package it.uniroma3.crawler.util;

import static org.junit.Assert.*;

import static java.util.stream.Collectors.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
		this.client = HtmlUtils.makeWebClient(false);
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
		String anchorQuery = "";
		NamedNodeMap linkAttributes = link.getAttributes();
		if (linkAttributes.getLength()>1) { // escape anchors with href only
			for (int i=0; i<=linkAttributes.getLength()-1; i++) {
				org.w3c.dom.Node lattr = linkAttributes.item(i);
				String lAttrName = lattr.getNodeName();
				if (!lAttrName.equals("href")) {
					if (lAttrName.equals("id")) {
						String lattrValue = lattr.getNodeValue();
						return "//"+xpath+"[@"+lAttrName+"='"+lattrValue+"'"+"]";
					}
					else anchorQuery = "@"+lAttrName+" and ";
				}
			}
		}
		xpath += "["+anchorQuery+"not(@id)]";
		
		DomNode current=link.getParentNode();
		boolean stop = false;
		while (current.getNodeName()!="#document" && !stop) {
			String currentQuery = current.getNodeName();
			NamedNodeMap attributes = current.getAttributes();
			if (attributes.getLength()>0 && !currentQuery.equals("html")) {
				org.w3c.dom.Node attr = attributes.item(0);
				String attrName = attr.getNodeName();
				if (attrName.equals("id")) {
					stop = true;
					String attrValue = attr.getNodeValue();
					currentQuery += "[@"+attrName+"='"+attrValue+"'"+"]";
				}
				else currentQuery += "[@"+attrName+"]";
			}
			xpath = currentQuery+"/"+xpath;
			current = current.getParentNode();
		}
		xpath = (stop) ? "//"+xpath : "/"+xpath;
		return xpath;
	}
	
	
	@SuppressWarnings("unchecked")
	private Page makePage(PageClassModel model, String base, HtmlPage page, String pageUrl) {
		Page p = new Page(pageUrl, model);
		List<HtmlAnchor> links = (List<HtmlAnchor>) XPathUtils.getByMatchingXPath(page, "//a");
		for (HtmlAnchor link : links) {
			String href = link.getHrefAttribute();
			String url = getAbsoluteURL(pageUrl, href);
			p.updatePageSchema(getXPath(link), url);
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
	
	private String transformURL(String url) {
		String newUrl = url.toLowerCase();
		if (newUrl.endsWith("/")) 
			newUrl = newUrl.substring(0, newUrl.length()-1);
		return newUrl;
	}
	
	@Test
	public void computeModelTest() {
		String base = "http://localhost:8081";
		String entry = "/";
		String sitename = base.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.|:", "_");
		
		int fetchedPgs = 0, classCounter = 1;
		
		final int MAX_PAGES = 200;
		final double dt = 0.2;
		int n = 3;
		
		// Final model of discovered clusters
		PageClassModel model = new PageClassModel();
		
		// Current queue of link collections to expand
		Queue<LinkCollection> queueQ = new PriorityQueue<>();
		
		// A set of already visited link collections
		Set<LinkCollection> insertedCollections = new HashSet<>(); 
		
		// A set of already visited urls
		Set<String> visitedUrls = new HashSet<>();
		
		// A set of Pages with some edited xpath: to be used at the end to edit their cluster schema
		Set<Page> editedPages = new HashSet<>();

		// Feed queue with seed
		Set<String> lcSet = new HashSet<>();
		lcSet.add(base+entry);
		LinkCollection lcSeed = new LinkCollection(lcSet);
		queueQ.add(lcSeed);
		
		
		// Main while, fetch n links from next link collection
		while (!queueQ.isEmpty() && fetchedPgs<MAX_PAGES) {
			LinkCollection currentCollection = queueQ.poll();

			Map<String, HtmlPage> url2HtmlPage = new HashMap<>();
			
			System.out.println("Parent Page: "+currentCollection.getParent()+", "+currentCollection.size()+" total links");
			
			TreeSet<String> links = (TreeSet<String>) currentCollection.getLinks();
			int counter = 0;
			while (!links.isEmpty() && counter<n) {
				String lcUrl = links.pollFirst();
				
				try {
					// normalize url for validity checks
					String transformedUrl = transformURL(lcUrl);
					if (!visitedUrls.contains(transformedUrl) 
							&& !transformedUrl.equals("") 
							&& isValid(base,transformedUrl)) {
						visitedUrls.add(transformedUrl);
						HtmlPage body = HtmlUtils.getPage(lcUrl, client);
						url2HtmlPage.put(lcUrl, body);
						System.out.println("Fetched: "+lcUrl);
						//Thread.sleep(1500); // wait..!!
						fetchedPgs++; 
						counter++;
					}
				} catch (Exception e) {
					System.err.println("failed fetching: "+lcUrl);
				}
			}
			n = 3; // restore default n
			
			
			// Candidate class selection
			
			Set<CandidatePageClass> candidates = new HashSet<>();
			Set<Page> newPages = new HashSet<>();
			
			for (String pageUrl : url2HtmlPage.keySet()) {
				HtmlPage htmlPage = url2HtmlPage.get(pageUrl);
				Page page = makePage(model, base, htmlPage, pageUrl);
				newPages.add(page);

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
			
			// Collapse classes with similar structure
			
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
			
			// Edit parent page xpath if links are from different classes (i.e. list of links is a menu)
			if (orderedCandidates.size() > 1) {
				System.out.println("MENU DETECTED...");
				if (links.isEmpty()) {
					Page parent = currentCollection.getParent();
					String parentXPath = currentCollection.getXPath();
					System.out.println("EDITING XPATHS TO MATCH "+newPages);
					for (Page child : newPages) {
						int index = parent.getUrlIndex(parentXPath, child.getUrl());
						String newXPath = "(" + parentXPath + ")[" + (index+1) + "]";
						parent.updatePageSchema(newXPath, child.getUrl());
					}
					parent.removeXPathFromSchema(parentXPath); // remove old xpath
					editedPages.add(parent); // save page with new xpaths for later schema edit
				}
				else {
					links.addAll(url2HtmlPage.keySet());
					for (String visitedUrl : url2HtmlPage.keySet()) {
						visitedUrls.remove(transformURL(visitedUrl));
					}					
					n = links.size();
					queueQ.add(currentCollection);
					System.out.println("FETCHING ALL URLS IN LIST");
					continue; // fetch all the collection to expand the menu
				}
			}

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
			
			// Update Queue
			
			Set<LinkCollection> newLinks = new HashSet<>();
						
			for (Page p : newPages) {	
				for (String xp : p.getSchema()) {
					LinkCollection lCollection = new LinkCollection(p, xp, p.getUrlsByXPath(xp));
					if (!insertedCollections.contains(lCollection)) {
						insertedCollections.add(lCollection);
						newLinks.add(lCollection);
					}
				}
			}
			
			queueQ.addAll(newLinks);
		}
		
		// Model has been completed, we must update the cluster schema with the previously edited pages
		for (Page edited : editedPages) {
			edited.getCurrentCluster().addPageToClass(edited); // save new xpaths to Cluster schema
		}
		
		// Save Model
		
		File directory = new File("sites_navigation");
		directory.mkdir();
		
		try {
			FileWriter in = new FileWriter(directory.toString()+"/"+sitename+"_model.txt");
			model.getModel().forEach(pc -> {
				try {in.write(pc.getName()+": "+pc.getClassPages().toString()+"\n");} 
				catch (IOException e) {}});
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
		
		// Save Page Classes and Class Links
		
		Map<CandidatePageClass, PageClass> cand2Pclass = new TreeMap<>();
		model.getModel().forEach(cand -> cand2Pclass.put(cand, new PageClass(cand.getName())));
		
		//PageClass home = cand2Pclass.get(model.getCandidateFromUrl(entryPoint));
		
		for (CandidatePageClass candidate : model.getModel()) {
			PageClass src = cand2Pclass.get(candidate);
						
			for (String xpath : candidate.getClassSchema()) {
				Set<String> urls = candidate.getUrlsDiscoveredFromXPath(xpath);
				
				Set<CandidatePageClass> destClasses = urls.stream()
						.map(u -> model.getCandidateFromUrl(u))
						.filter(cc -> cc!=null)
						.collect(toSet());
				if (!destClasses.isEmpty()) {
					CandidatePageClass destClass = destClasses.stream()
					.max((c1,c2) -> (int)c1.discoveredUrlsSize()-(int)c2.discoveredUrlsSize())
					.get();
					PageClass dest = cand2Pclass.get(destClass);
					src.addPageClassLink(xpath, dest);
				}				
			}
		}

		try {
			FileWriter in = new FileWriter(directory.toString()+"/"+sitename+"_target.csv");
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
		
		
		try {
			FileWriter in = new FileWriter(directory.toString()+"/"+sitename+"_class_schema.csv");
			in.write(base+"\n");
			model.getModel().forEach(cl -> {
					try {
						in.write(cl.getName()+"\n");
						for (String xp : cl.getClassSchema()) {
							in.write(xp+"\n");
						}
						in.write("\n");
					} 
					catch (IOException e) {}});
			in.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}


	}

}


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