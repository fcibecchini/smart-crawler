package it.uniroma3.crawler.target;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;

import com.csvreader.CsvReader;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import static it.uniroma3.crawler.util.HtmlUtils.*;

import it.uniroma3.crawler.model.CandidatePageClass;
import it.uniroma3.crawler.model.LinkCollection;
import it.uniroma3.crawler.model.Page;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.WebsiteModel;
import it.uniroma3.crawler.util.XPathUtils;

public class CrawlTarget {
	private static final char DELIMITER = '\t';
	private URI urlBase;
	private String configFile;
	private PageClass entryClass;
	private HashSet<PageClass> pClasses;
	
	private WebClient client;
	
	private Logger log;
	
	public CrawlTarget() {
		this.log = Logger.getLogger(CrawlTarget.class.getName());
	}
	
	public CrawlTarget(URI urlBase, boolean useJavaScript) {
		this();
		this.urlBase = urlBase;
		this.client = makeWebClient(useJavaScript);
	}
	
	public CrawlTarget(String configFile) {
		this();
		this.configFile = configFile;
	}
	
	public URI getUrlBase() {
		return this.urlBase;
	}
	
	public PageClass getEntryPageClass() {
		return this.entryClass;
	}
	
	public HashSet<PageClass> getClasses() {
		return this.pClasses;
	}
	
	public PageClass computeModel(int maxPages, int n, double dt, long waitTime) {
		String base = urlBase.toString();
		
		int fetchedPgs = 0;
		int classCounter = 1;
		int maxIterationPages = n;
		
		// Final model of discovered clusters
		WebsiteModel model = new WebsiteModel();
		
		// Current queue of link collections to expand
		Queue<LinkCollection> queueQ = new PriorityQueue<>();
		
		// A set of already visited link collections
		Set<LinkCollection> insertedCollections = new HashSet<>(); 
		
		// A set of already visited urls
		Set<String> visitedUrls = new HashSet<>();
		
		// A set of Pages with some edited xpath: 
		// to be used at the end to edit their cluster schema
		Set<Page> editedPages = new HashSet<>();

		// Feed queue with seed
		Set<String> lcSet = new HashSet<>();
		lcSet.add(base);
		LinkCollection lcSeed = new LinkCollection(lcSet);
		queueQ.add(lcSeed);
		
		// Main while, fetch n links from next link collection
		while (!queueQ.isEmpty() && fetchedPgs<maxPages) {
			LinkCollection currentCollection = queueQ.poll();

			Map<String, HtmlPage> url2HtmlPage = new HashMap<>();
			
			log.info("Parent Page: "+currentCollection.getParent()+", "+currentCollection.size()+" total links");
			
			TreeSet<String> links = (TreeSet<String>) currentCollection.getLinks();
			int counter = 0;
			while (!links.isEmpty() && counter<maxIterationPages) {
				String lcUrl = links.pollFirst();
				
				try {
					// normalize url for validity checks
					String transformedUrl = transformURL(lcUrl);
					if (!visitedUrls.contains(transformedUrl) 
							&& !transformedUrl.equals("") 
							&& isValidURL(base,transformedUrl)) {
						visitedUrls.add(transformedUrl);
						HtmlPage body = getPage(lcUrl, client);
						url2HtmlPage.put(lcUrl, body);
						fetchedPgs++; 
						counter++;
						
						log.info("Fetched: "+lcUrl);
						Thread.sleep(waitTime);
					}
				} catch (Exception e) {
					log.warning("failed fetching: "+lcUrl);
				}
			}
			maxIterationPages = n; // restore default n
			
			
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
			
			// Edit parent page xpath if links are from different classes 
			// (i.e. list of links is a menu)
			
			if (orderedCandidates.size() > 1) {
				log.info("MENU DETECTED...");
				if (links.isEmpty()) {
					Page parent = currentCollection.getParent();
					String parentXPath = currentCollection.getXPath();
					log.info("EDITING XPATHS TO MATCH "+newPages);
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
					maxIterationPages = links.size();
					queueQ.add(currentCollection);
					log.info("FETCHING ALL URLS IN LIST");
					continue; // fetch all the collection to expand the menu
				}
			}

			// Update Model
			
			for (CandidatePageClass candidate : orderedCandidates) {
				WebsiteModel modelNew = new WebsiteModel();
				WebsiteModel modelMerge = null;
				modelNew.addFinalClasses(model.getModel());
				modelNew.addFinalClass(candidate);
				double modelNewLength = modelNew.minimumLength();

				double mergeLength = Double.MAX_VALUE;
				for (CandidatePageClass cInModel : model.getModel()) {
					CandidatePageClass tempClass = new CandidatePageClass(cInModel.getName(), base);
					tempClass.collapse(candidate);
					tempClass.collapse(cInModel);

					WebsiteModel tempModel = new WebsiteModel();
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
		
		// Model completed, we must update the cluster schema 
		// with the previously edited pages
		editedPages.forEach(p -> p.getCurrentCluster().addPageToClass(p));
		
		// Transform candidates into Page Classes and Class Links
		List<PageClass> pClasses = model.makePageClasses();
		entryClass = pClasses.get(0);
		
		// Set classes hierarchy
		setHierarchy();
		
		// Log and save model to filesystem
		logModel(model, pClasses, "sites_navigation");
		
		return entryClass;
	}
	
	private void logModel(WebsiteModel model, List<PageClass> pClasses, String dir) {
		String base = urlBase.toString();
		String sitename = base.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.|:", "_");
		
		new File(dir).mkdir();
		
		try {
			FileWriter modelFile = new FileWriter(dir+"/"+sitename+"_model.txt");
			FileWriter schemaFile = new FileWriter(dir+"/"+sitename+"_class_schema.csv");
			FileWriter targetFile = new FileWriter(dir+"/"+sitename+"_target.csv");

			for (CandidatePageClass cpc : model.getModel()) {
				modelFile.write(cpc.getName()+": "+cpc.getClassPages().toString()+"\n");
				
				schemaFile.write(cpc.getName()+"\n");
				for (String xp : cpc.getClassSchema()) {
					schemaFile.write(xp+"\n");
				}
				schemaFile.write("\n");
			}
			modelFile.close();
			schemaFile.close();

			targetFile.write(base+"\n");
			for (PageClass pc : pClasses) {
				for (String xp : pc.getNavigationXPaths()) {
					targetFile.write(pc.getName()+"\t"+"link"+"\t"+xp+"\t"+pc.getDestinationByXPath(xp).getName()+"\n");
				}
			}
			targetFile.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
		
	}
	
	public void initCrawlingTarget() {
		try {
			CsvReader reader = new CsvReader(configFile, DELIMITER);
			reader.readRecord();
			String urlBase = reader.get(0);
			this.urlBase = URI.create(urlBase);
			
			this.pClasses = getPageClasses();
			while (reader.readRecord()) {
				PageClass pageSrc = getPageClass(pClasses, reader.get(0));
				String type = reader.get(1);
				String xpath = reader.get(2);
				PageClass pageDest = getPageClass(pClasses, reader.get(3));
				if (type.equals("link")) {
					if (pageDest!=null)
						pageSrc.addPageClassLink(xpath, pageDest);
					else
						log.warning("Could not find "+reader.get(3));
				}
				else {
					pageSrc.addData(xpath, type);
				}
			}
			// set depth hierarchy for page classes
			setHierarchy();
			
		} catch (FileNotFoundException e) {
			log.severe("Could not find target configuration file");
		} catch (IOException e) {
			log.severe("Could not read target configuration file");
		} catch (IllegalArgumentException ie) {
			log.severe("Not a valid url base");
		}
	}
	
	private HashSet<PageClass> getPageClasses() throws IOException {
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(configFile, DELIMITER);
		reader.readRecord(); // skip url base
		while (reader.readRecord()) {
			PageClass pClass1 = new PageClass(reader.get(0));
			PageClass pClass2 = new PageClass(reader.get(3));
			if (entryClass==null) entryClass = pClass1;
			pageClasses.add(pClass1);
			pageClasses.add(pClass2);
		}
		return pageClasses;
	}
	
	private PageClass getPageClass(HashSet<PageClass> pClasses, String name) {
		return pClasses.stream().filter(pc -> pc.getName().equals(name)).findAny().orElse(null);
	}
	
	private void setHierarchy() {
		Queue<PageClass> classes = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		classes.add(entryClass);
		PageClass current, next = null;
		entryClass.setDepth(0);
		while ((current = classes.poll()) != null) {
			if (!current.isEndPage()) {
				for (String xpath : current.getNavigationXPaths()) {
					next = current.getDestinationByXPath(xpath);
					// avoid page class loops
					if (!visited.contains(next)) {
						visited.add(next);
						classes.add(next);
						next.setDepth(current.getDepth()+1);
					}
				}
			}
		}
	}
	
	private String getXPath(HtmlAnchor link) {
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
	private Page makePage(WebsiteModel model, String base, HtmlPage page, String pageUrl) {
		Page p = new Page(pageUrl, model);
		List<HtmlAnchor> links = (List<HtmlAnchor>) XPathUtils.getByMatchingXPath(page, "//a");
		for (HtmlAnchor link : links) {
			String href = link.getHrefAttribute();
			String url = getAbsoluteURL(pageUrl, href);
			p.updatePageSchema(getXPath(link), url);
		}
		return p;
	}
	
}
