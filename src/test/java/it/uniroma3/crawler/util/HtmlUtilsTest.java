package it.uniroma3.crawler.util;
import static org.junit.Assert.*;

import static java.util.stream.Collectors.*;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

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
		this.client = HtmlUtils.makeWebClient();
	}
	
	private boolean externalDomain(String base, String href) {
		return (href.contains("http") && !href.contains(base));
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
			if (!href.contains("javascript") && !externalDomain(base,href)) {
				p.updatePageSchema(xpath, href);
			}
		}
		return p;
	}

	
	@Test
	public void computerModelTest() {
		int test = 0;
		int c = 1;
		String base = "http://www.ansa.it";
		String url = "/";
		
		int n = 5;
		double dt = 0.2;
		
		Queue<LinkCollection> queueQ = new PriorityQueue<>(
				(lc1, lc2) -> lc2.relativeSize() - lc1.relativeSize());
		Set<String> visitedUrls = new HashSet<>();
		
		PageClassModel model = new PageClassModel();
		
		// Feed queue with seed
		Set<String> lcSet = new HashSet<>();
		lcSet.add(url);
		LinkCollection lcSeed = new LinkCollection(null, lcSet);
		
		queueQ.add(lcSeed);
		
		while (!queueQ.isEmpty()) {
			Set<String> lc = queueQ.poll().getLinks();
			
			Set<HtmlPage> fetchedW = new HashSet<>();
			int counter = 0;
			for (String lcUrl : lc) {
				try {
					String urlToFetch = (lcUrl.contains(base)) ? lcUrl : base+lcUrl;
					HtmlPage body = HtmlUtils.getPage(urlToFetch, client);
					fetchedW.add(body);
					Thread.sleep(1000); // wait..!!
					counter++;
					if (counter==n) break;
				} catch (Exception e) {}
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
					CandidatePageClass newClass = new CandidatePageClass("class"+(c++));
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
			
			List<CandidatePageClass> finalCandidates = null;
			if (orderedCandidates.size()>1) {
				finalCandidates = 
						orderedCandidates.stream()
						.filter(cc -> cc.getClassPages().size() > 2)
						.collect(toList());
			}
			else 
				finalCandidates = orderedCandidates;
			
			// Update Model
			for (CandidatePageClass candidate : finalCandidates) {
				PageClassModel modelNew = new PageClassModel();
				PageClassModel modelMerge = null;
				modelNew.addFinalClasses(model.getModel());
				modelNew.addFinalClass(candidate);
				double modelNewLength = modelNew.minimumLength();
				
				double minLength = Double.MAX_VALUE;
				for (CandidatePageClass cInModel : model.getModel()) {
					CandidatePageClass tempClass = 
							new CandidatePageClass(candidate.getName()+cInModel.getName());
					tempClass.setClassSchema(cInModel.getClassSchema());
					tempClass.collapse(candidate);
					tempClass.collapse(cInModel);
					
					PageClassModel tempModel = new PageClassModel();
					tempModel.addFinalClasses(model.getModel());
					tempModel.removeClass(cInModel);
					tempModel.addFinalClass(tempClass);
					
					double modelLength = tempModel.minimumLength();
					if (minLength > modelLength) {
						minLength = modelLength;
						modelMerge = tempModel;
					}
				}
				
				model.reset();
				if (minLength < modelNewLength)
					model.addFinalClasses(modelMerge.getModel());
				else
					model.addFinalClasses(modelNew.getModel());
			}
			
			System.out.println(++test);
//			if (test==10) {
//				System.out.println("Break");
//				break;
//			}
			
			// Update Queue
			Set<LinkCollection> newLinkCollections = new HashSet<>();
			
			for (CandidatePageClass cl : orderedCandidates) {
				for (Page p : cl.getClassPages()) {
					for (String xp : p.getSchema()) {
						Set<String> urlCollection = 
								p.getUrlsByXPath(xp)
								.stream().filter(urll -> !visitedUrls.contains(urll))
								.collect(toSet());
						if (!urlCollection.isEmpty()) {
							visitedUrls.addAll(urlCollection);
							LinkCollection lCollection = new LinkCollection(p, urlCollection);
							newLinkCollections.add(lCollection); 
						}
					}
				}
			}
			
			queueQ.addAll(newLinkCollections);
		}
		
		try {
			FileWriter in = new FileWriter("page_class_model.txt");
			for (CandidatePageClass pClass : model.getModel()) {
				in.write(pClass.getName()+": "+pClass.getClassPages().toString()+"\n");
				in.write("SCHEMAS AND LINKS\n");
				for (String xpath : pClass.getClassSchema()) {
					Set<String> urls = pClass.getUrlsDiscoveredFromXPath(xpath);
					in.write("\t"+xpath+"\n");
					in.write("\t"+urls+"\n");
				}
			}
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
	}

}
