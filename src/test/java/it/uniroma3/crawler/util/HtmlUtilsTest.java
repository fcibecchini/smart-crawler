package it.uniroma3.crawler.util;

import static org.junit.Assert.*;

import static java.util.stream.Collectors.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.NamedNodeMap;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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
		this.client = HtmlUtils.makeWebClient(true);
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
		NamedNodeMap linkAttributes = link.getAttributes();
		if (linkAttributes.getLength()>1) {
			for (int i=1; i<=linkAttributes.getLength()-1; i++) {
				org.w3c.dom.Node lattr = linkAttributes.item(i);
				String lAttrName = lattr.getNodeName();
				if (lAttrName.equals("id")) {
					String lattrValue = lattr.getNodeValue();
					return "//"+xpath+"[@"+lAttrName+"='"+lattrValue+"'"+"]";
				}
			}
		}
		xpath += "[not(@id)]";
		
		DomNode current=link.getParentNode();
		boolean stop = false;
		while (current.getNodeName()!="#document" && !stop) {
			String currentSection = current.getNodeName();
			NamedNodeMap attributes = current.getAttributes();
			if (attributes.getLength()>0 && !currentSection.equals("html")) {
				org.w3c.dom.Node attr = attributes.item(0);
				String attrName = attr.getNodeName();
				if (attrName.equals("id")) {
					stop = true;
					String attrValue = attr.getNodeValue();
					currentSection += "[@"+attrName+"='"+attrValue+"'"+"]";
				}
				else currentSection += "[@"+attrName+"]";
			}
			xpath = currentSection+"/"+xpath;
			current = current.getParentNode();
		}
		xpath = (stop) ? "//"+xpath : "/"+xpath;
		return xpath;
	}
	
	
	@SuppressWarnings("unchecked")
	private Page makePage(PageClassModel model, String base, HtmlPage page) {
		Page p = new Page(page.getUrl().toString(), model);
		
		List<HtmlAnchor> links = (List<HtmlAnchor>) XPathUtils.getByMatchingXPath(page, "//a");
		
		for (HtmlAnchor link : links) {
			String xpath = getXPath(link);
						
			String href = link.getHrefAttribute();
			if (isValid(base,href)) {
				if (!p.getUrl().equals(getAbsoluteURL(base,href)))
					p.updatePageSchema(xpath, href);
			}
		}
		return p;
	}
	
	public String getAbsoluteURL(String base, String relative) {
		try {
			URL baseUrl = new URL(base);
			URL url = new URL(baseUrl, relative);
			return url.toString();
		} catch (MalformedURLException e) {
			return "";
		}
	}
	
	
	@Test
	public void computeModelTest() {
		int fetchedPgs = 0;
		int classCounter = 1;
		String base = "http://www.pennyandsinclair.co.uk";
		String entry = "/";
		String sitename = base.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.", "_");
		
		final int MAX_PAGES = 100;
		final int n = 3;
		final double dt = 0.2;
		
		PageClassModel model = new PageClassModel();
		
		Queue<LinkCollection> queueQ = new PriorityQueue<>((lc1, lc2) -> lc2.compareTo(lc1));
		Set<LinkCollection> insertedCollections = new HashSet<>();
		
		// Feed queue with seed
		Set<String> lcSet = new HashSet<>();
		lcSet.add(entry);
		LinkCollection lcSeed = new LinkCollection(lcSet);
		Set<String> visitedUrls = new HashSet<>();
		
		queueQ.add(lcSeed);
		
		while (!queueQ.isEmpty()) {
			LinkCollection lcc = queueQ.poll();
			
			// Fetch n pages from lcc 
			
			Set<HtmlPage> fetchedW = new HashSet<>();
			int counter = 0;
			System.out.println("Parent Page: "+lcc.getParent()+", "+lcc.size()+" total links");
			for (String lcUrl : lcc.getLinks()) {
				try {
					
					//String urlToFetch = (lcUrl.contains(base)) ? lcUrl : base+lcUrl;
					String urlToFetch = getAbsoluteURL(base, lcUrl);
					if (!visitedUrls.contains(urlToFetch) && !urlToFetch.equals("")) {
						visitedUrls.add(urlToFetch);
						
						HtmlPage body = HtmlUtils.getPage(urlToFetch, client);
						fetchedW.add(body);
						System.out.println("Fetched: "+urlToFetch);
						Thread.sleep(1500); // wait..!!
						fetchedPgs++; 
						counter++;
						
					}
					if (counter==n) break;
				} catch (Exception e) {
					System.err.println("failed fetching: "+lcUrl);
				}
			}
			
			// Candidate class selection
			
			Set<CandidatePageClass> candidates = new HashSet<>();
			
			for (HtmlPage htmlPage : fetchedW) {
				Page page = makePage(model, base, htmlPage);
				
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
				boolean collapsed = false;
				for (CandidatePageClass cModel : model.getModel()) {
					if (cModel.distance(candidate) < dt) {
						cModel.collapse(candidate);
						collapsed = true;
						break;
					}
				}
				if (!collapsed) model.addFinalClass(candidate);
			}
			
			System.out.println(fetchedPgs);
			if (fetchedPgs>=MAX_PAGES) {
				System.out.println("END: Pages fetched: "+fetchedPgs+", still "+queueQ.size()+" link collections...");
				break;
			}
			
			// Update Queue
			
			Set<LinkCollection> newLinks = new HashSet<>();
			
			for (CandidatePageClass cl : orderedCandidates) {
				for (Page p : cl.getClassPages()) {
					for (String xp : p.getSchema()) {
						LinkCollection lCollection = new LinkCollection(p, p.getUrlsByXPath(xp));
						if (!insertedCollections.contains(lCollection)) {
							insertedCollections.add(lCollection);
							newLinks.add(lCollection); 
						}
					}
				}
			}
			
			queueQ.addAll(newLinks);
		}
		
		// Save Model
		
		try {
			FileWriter in = new FileWriter(sitename+"_model.txt");
			model.getModel().forEach(pc -> {
				try {in.write(pc.getName()+": "+pc.getClassPages().toString()+"\n");} 
				catch (IOException e) {}});
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
		
		// Save Page Classes and Class Links
		
		Map<CandidatePageClass, PageClass> cand2Pclass = new TreeMap<>((c1,c2) -> c1.compareTo(c2));
		model.getModel().forEach(cand -> cand2Pclass.put(cand, new PageClass(cand.getName())));
		
		//PageClass home = cand2Pclass.get(model.getCandidateFromUrl(entryPoint));
		
		for (CandidatePageClass candidate : model.getModel()) {
			PageClass src = cand2Pclass.get(candidate);
			for (String xpath : candidate.getClassSchema()) {
				Set<String> urls = candidate.getUrlsDiscoveredFromXPath(xpath);
				Map<CandidatePageClass, Long> dests = 
						urls.stream()
						.map(urll -> model.getCandidateFromUrl(urll))
						.filter(cc -> cc!=null)
						.collect(groupingBy(cc -> cc, counting()));
				
				if (!dests.isEmpty()) {
					CandidatePageClass dest1 = null;
					int max = 0;
					for (CandidatePageClass cpc : dests.keySet()) {
						long current = dests.get(cpc);
						if (current > max) dest1 = cpc;
					}
					PageClass dest = cand2Pclass.get(dest1);
					src.addPageClassLink(xpath, dest);
				}
			}
		}
		try {
			FileWriter in = new FileWriter(sitename+"_target.csv");
			in.write(base+"\n");
			cand2Pclass.keySet().stream()
			.map(k -> cand2Pclass.get(k))
			.forEach(pc -> {
				pc.getNavigationXPaths().stream().forEach( xp -> {
					try {in.write(pc.getName()+"\t"+"link"+"\t"+xp+"\t"+pc.getDestinationByXPath(xp).getName()+"\n");} 
					catch (IOException e) {}});});
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}

	}

}

//public void test() throws Exception {
//String base = "http://www.pennyandsinclair.co.uk";
//String entry = "/property/residential/for-rent/central-north-oxford/southmoor-road/101073006967";
//
//String urlToFetch = getAbsoluteURL(base, entry);
//HtmlPage body = HtmlUtils.getPage(urlToFetch, client);
//
//Page p = makePage(new PageClassModel(), base, body);
//
//}


//for (CandidatePageClass candidate : orderedCandidates) {
//PageClassModel modelNew = new PageClassModel();
//PageClassModel modelMerge = null;
//modelNew.addFinalClasses(model.getModel());
//modelNew.addFinalClass(candidate);
//double modelNewLength = modelNew.minimumLength();
//
//double mergeLength = Double.MAX_VALUE;
//for (CandidatePageClass cInModel : model.getModel()) {
//	CandidatePageClass tempClass = 
//			new CandidatePageClass(cInModel.getName(), base);
//	tempClass.collapse(candidate);
//	tempClass.collapse(cInModel);
//	
//	PageClassModel tempModel = new PageClassModel();
//	tempModel.addFinalClasses(model.getModel());
//	tempModel.removeClass(cInModel);
//	tempModel.addFinalClass(tempClass);
//	
//	double modelLength = tempModel.minimumLength();
//	if (mergeLength > modelLength) {
//		mergeLength = modelLength;
//		modelMerge = tempModel;
//	}
//}
//
//model.reset();
//if (mergeLength < modelNewLength)
//	model.addFinalClasses(modelMerge.getModel());
//else
//	model.addFinalClasses(modelNew.getModel());
//}




//Set<String> prova = page.getUrlsByXPath("//div[@id='fullDetailsHeader']/div[@class]/a");
//if (prova!=null) {
//	System.err.println(page.getUrl());
//	page.getSchema().forEach(xp -> System.err.println(xp+" -> "+page.getUrlsByXPath(xp)));
//	System.err.println(XPathUtils.getByMatchingXPath(htmlPage, "//a"));
//}


