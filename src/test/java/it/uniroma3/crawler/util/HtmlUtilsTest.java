package it.uniroma3.crawler.util;

import static org.junit.Assert.*;

import static java.util.stream.Collectors.*;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.NamedNodeMap;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

import it.uniroma3.crawler.util.HtmlUtils;
import it.uniroma3.crawler.util.XPathUtils;
import it.uniroma3.crawler.model.CandidatePageClass;
import it.uniroma3.crawler.model.LinkCollection;
import it.uniroma3.crawler.model.Page;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.PageClassModel;

public class HtmlUtilsTest {
	private WebClient client;
	
	@Before
	public void setUp() {
		this.client = HtmlUtils.makeWebClient();
	}
	
	private boolean isValid(String base, String href) {
		if (href.startsWith("http") && !href.startsWith(base))
			return false;
		if (href.contains("javascript") 
				|| href.contains("crawler") 
				|| href.contains("@") 
				|| href.contains("#"))
			return false;
		return true;
	}
	
	public String getXPath(HtmlAnchor link) {
		String xpath = "a";
		DomNode current=link.getParentNode();
		boolean stop = false;
		while (current.getNodeName()!="#document" && !stop) {
			String currentSection = current.getNodeName();
			NamedNodeMap attributes = current.getAttributes();
			if (attributes.getLength()>0 && !currentSection.equals("html")) {
				org.w3c.dom.Node attr = attributes.item(0);
				String attrName = attr.getNodeName();
				if (attrName.equals("id")) 	stop = true;
				String attrValue = attr.getNodeValue();
				currentSection += "[@"+attrName+"='"+attrValue+"'"+"]";
			}
			xpath = currentSection+"/"+xpath;
			current = current.getParentNode();
		}
		xpath = "//"+xpath;
		return xpath;
	}
	
	@SuppressWarnings("unchecked")
	private Page makePage(String base, HtmlPage page) {
		Page p = new Page(page.getUrl().toString());
		
		List<HtmlAnchor> links = (List<HtmlAnchor>) XPathUtils.getByMatchingXPath(page, "//a");
		
		for (HtmlAnchor link : links) {
			String xpath = getXPath(link);
			String href = link.getHrefAttribute();
			if (isValid(base,href)) {
				p.updatePageSchema(xpath, href);
			}
		}
		return p;
	}

	
	public void foo() {
		String base = "http://www.pennyandsinclair.co.uk";
		String url = "/search?officeids=8&obc=Price&obd=Descending";
		
		try {
			HtmlPage p = HtmlUtils.getPage(base+url, client);
			System.out.println(p.getUrl());
		}
		 catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Test
	public void computerModelTest() {
		int fetchedPgs = 0;
		int classCounter = 1;
		String base = "http://www.ansa.it";
		String sitename = base.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.", "_");
		String url = "/";
		
		final int MAX_PAGES = 300;
		final int n = 5;
		final double dt = 0.2;
		
		PageClassModel model = new PageClassModel();
		
		Queue<LinkCollection> queueQ = new PriorityQueue<>((lc1, lc2) -> lc2.compareTo(lc1));
		Set<String> visitedUrls = new HashSet<>();
		
		
		// Feed queue with seed
		Set<String> lcSet = new HashSet<>();
		lcSet.add(url);
		LinkCollection lcSeed = new LinkCollection(lcSet);
		
		queueQ.add(lcSeed);
		
		while (!queueQ.isEmpty()) {
			LinkCollection lcc = queueQ.poll();
			
			Set<HtmlPage> fetchedW = new HashSet<>();
			int counter = 0;
			for (String lcUrl : lcc.getLinks()) {
				try {
					String urlToFetch = (lcUrl.contains(base)) ? lcUrl : base+lcUrl;
					if (!visitedUrls.contains(urlToFetch)) {
						HtmlPage body = HtmlUtils.getPage(urlToFetch, client);
						fetchedW.add(body);
						visitedUrls.add(urlToFetch);
						System.out.println("Fetched: "+urlToFetch);
						Thread.sleep(1500); // wait..!!
						fetchedPgs++; 
						counter++;
						if (counter==n) break;
					}
				} catch (Exception e) {
					System.err.println("failed fetching: "+lcUrl);
				}
			}
			
			// Candidate class selection
			
			Set<CandidatePageClass> candidates = new HashSet<>();
			
			for (HtmlPage htmlPage : fetchedW) {
				Page page = makePage(base, htmlPage);
				CandidatePageClass group = candidates.stream()
						.filter(cand -> cand.getClassSchema().equals(page.getSchema()))
						.findAny().orElse(null);
				if (group != null)
					group.addPageToClass(page);
				else {
					CandidatePageClass newClass = new CandidatePageClass("class"+(classCounter++), base);
					page.getSchema().forEach(xp -> newClass.addXPathToSchema(xp));
					newClass.addPageToClass(page);
					candidates.add(newClass);
				}
			}
			
			List<CandidatePageClass> orderedCandidates = candidates.stream()
					.sorted((cc1, cc2) -> cc2.getClassPages().size() - cc1.getClassPages().size())
					.collect(toList());
			
			Set<CandidatePageClass> toRemove = new HashSet<>();
			for (int i = 0; i < orderedCandidates.size(); i++) {
				for (int j = orderedCandidates.size() - 1; j > i; j--) {
					CandidatePageClass ci = orderedCandidates.get(i);
					CandidatePageClass cj = orderedCandidates.get(j);
					if (ci.distance(cj) < dt) {
						ci.collapse(cj);
						toRemove.add(cj);
					}
				}
			}
			
			orderedCandidates.removeAll(toRemove);

			
			// Update Model
			
			for (CandidatePageClass candidate : orderedCandidates) {
				PageClassModel modelNew = new PageClassModel();
				PageClassModel modelMerge = null;
				modelNew.addFinalClasses(model.getModel());
				modelNew.addFinalClass(candidate);
				double modelNewLength = modelNew.minimumLength();
				
				double mergeLength = Double.MAX_VALUE;
				for (CandidatePageClass cInModel : model.getModel()) {
					CandidatePageClass tempClass = 
							new CandidatePageClass(cInModel.getName(), base);
					tempClass.collapse(candidate);
					tempClass.collapse(cInModel);
					
					PageClassModel tempModel = new PageClassModel();
					tempModel.addFinalClasses(model.getModel());
					tempModel.removeClass(cInModel);
					tempModel.addFinalClass(tempClass);
					
					double modelLength = tempModel.minimumLength();
					if (mergeLength > modelLength) {
						mergeLength = modelLength;
						modelMerge = tempModel;
					}
				}
				
				model.reset();
				if (mergeLength < modelNewLength)
					model.addFinalClasses(modelMerge.getModel());
				else
					model.addFinalClasses(modelNew.getModel());
			}
			
			System.out.println(fetchedPgs);
			if (fetchedPgs>=MAX_PAGES) {
				System.out.println("END: Pages fetched -> "+fetchedPgs);
				break;
			}
			
			// Update Queue
			
			Set<LinkCollection> newLinkCollections = new HashSet<>();
			
			for (CandidatePageClass cl : orderedCandidates) {
				for (Page p : cl.getClassPages()) {
					for (String xp : p.getSchema()) {
						LinkCollection lCollection = new LinkCollection(model, p, p.getUrlsByXPath(xp));
						newLinkCollections.add(lCollection); 
					}
				}
			}
			
			queueQ.addAll(newLinkCollections);
		}
		
		// Save Model
		
		try {
			FileWriter in = new FileWriter(sitename+"_model.txt");
			for (CandidatePageClass pClass : model.getModel()) {
				in.write(pClass.getName()+": "+pClass.getClassPages().toString()+"\n");
//				in.write("SCHEMAS AND LINKS\n");
//				for (String xpath : pClass.getClassSchema()) {
//					Set<String> urls = pClass.getUrlsDiscoveredFromXPath(xpath);
//					in.write("\t"+xpath+"\n");
//					in.write("\t"+urls+"\n");
//				}
			}
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
		
		// Save Page Classes and Class Links
		
		Map<CandidatePageClass, PageClass> cand2Pclass = new HashMap<>();
		model.getModel().forEach(cand -> cand2Pclass.put(cand, new PageClass(cand.getName())));
		
		//PageClass home = cand2Pclass.get(model.getCandidateFromUrl(entryPoint));
		
		for (CandidatePageClass candidate : model.getModel()) {
			PageClass src = cand2Pclass.get(candidate);
			for (String xpath : candidate.getClassSchema()) {
				Set<String> urls = candidate.getUrlsDiscoveredFromXPath(xpath);
				CandidatePageClass dest1 = 
						urls.stream()
						.map(urll -> model.getCandidateFromUrl(urll))
						.filter(cc -> cc!=null)
						.findAny().orElse(null);
				if (dest1!=null) {
					PageClass dest = cand2Pclass.get(dest1);
					src.addPageClassLink(xpath, dest);
				}
			}
		}
		try {
			FileWriter in = new FileWriter(sitename+"_target.csv");
			cand2Pclass.keySet().stream()
			.map(k -> cand2Pclass.get(k))
			.forEach(pc -> {
				pc.getNavigationXPaths().stream()
					.forEach( xp -> {
						try {in.write(pc.getName()+"\t"+"link"+"\t"+xp+"\t"+pc.getDestinationByXPath(xp).getName()+"\n");} 
						catch (IOException e) {}});});
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}

	}

}
