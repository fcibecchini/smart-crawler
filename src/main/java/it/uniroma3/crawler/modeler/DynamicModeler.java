package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.XPathUtils.*;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.CandidatePageClass;
import it.uniroma3.crawler.model.LinkCollection;
import it.uniroma3.crawler.model.Page;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.model.WebsiteModel;

public class DynamicModeler extends WebsiteModeler {
	private WebsiteModel model;
	private int maxPages;
	
	private WebClient client;
	private Set<String> visitedURLs;
	private int fetched;
	private int classCounter;
	private boolean allLinksFetched;
	
	public DynamicModeler(Website website, int wait, int maxPages) {
		super(website,wait);
		
		this.maxPages = maxPages;
		this.client = makeWebClient(website.isJavascript());
		
		this.model = new WebsiteModel();
		this.visitedURLs = new HashSet<>();
		this.classCounter = 1;
		this.fetched = 0;
		this.allLinksFetched = false;
	}

	@Override
	protected PageClass computeModel() {
		String domain = getWebsite().getDomain();
		
		final int n = 3;
		int max = n;
		Queue<LinkCollection> queue = new PriorityQueue<>();
		
		// A set of already visited link collections
		Set<LinkCollection> visitedColl = new HashSet<>(); 
		
		// Feed queue with seed
		queue.add(new LinkCollection(domain));
		
		while (!queue.isEmpty() && fetched<maxPages) {
			LinkCollection coll = queue.poll();
			
			Map<String, HtmlPage> url2HtmlPage = fetchLinks(coll, max);
			max = n; // restore default n
			
			Set<Page> newPages = makePages(url2HtmlPage);
			List<CandidatePageClass> candidates = clusterPages(newPages);
			
			// fetch all links if they are from different classes
			if (candidates.size() > 1 && !allLinksFetched) {
				getLogger().info("MENU DETECTED: FETCHING ALL URLS IN LINK COLLECTION...");
				queue.add(coll);
				max = coll.size();
			}
			else {
				updateModel(candidates);
				queue.addAll(newLinks(newPages, visitedColl));
				newPages.forEach(p -> visitedURLs.add(transformURL(p.getUrl())));
			}
		}
				
		// Transform candidates into Page Classes and Class Links
		ModelFinalizer finalizer = new ModelFinalizer(model, getWebsite(), getWait());
		TreeSet<PageClass> pClasses = finalizer.makePageClasses();
		
		// Log and save model to filesystem
		logModel(pClasses, "src/main/resources/targets");
		
		return pClasses.first();
	}
	
	// Fetch at most MAX links from collection
	private Map<String, HtmlPage> fetchLinks(LinkCollection coll, int max) {
		Map<String, HtmlPage> url2HtmlPage = new HashMap<>();
		
		getLogger().info("Parent Page: "+coll.getParent()+", "+coll.size()+" total links");
		
		TreeSet<String> links = new TreeSet<>(coll.getLinks());
		int counter = 0;
		while (!links.isEmpty() && counter<max) {
			String lcUrl = links.pollFirst();
			try {
				// normalize url for validity checks
				String normalizedUrl = transformURL(lcUrl);
				if (!visitedURLs.contains(normalizedUrl) 
						&& !normalizedUrl.equals("") 
						&& isValidURL(transformURL(getWebsite().getDomain()),normalizedUrl)) {
					
					//visitedURLs.add(normalizedUrl);
					HtmlPage body = getPage(lcUrl, client);
					url2HtmlPage.put(lcUrl, body);
					fetched++; 
					counter++;
					
					getLogger().info("Fetched: "+lcUrl);
					Thread.sleep(getWait());
				}
			} catch (Exception e) {
				getLogger().warning("failed fetching: "+lcUrl);
			}
		}
		allLinksFetched = (links.isEmpty()) ? true : false;
		
		return url2HtmlPage;
	}
	
	// Candidate class selection
	// Collapse classes with similar structure
	private List<CandidatePageClass> clusterPages(Set<Page> pages) {
				
		Set<CandidatePageClass> candidates = new HashSet<>();
		
		for (Page page : pages) {

			CandidatePageClass group = candidates.stream()
					.filter(cand -> cand.getClassSchema().equals(page.getSchema()))
					.findAny().orElse(null);
			if (group != null)
				group.addPageToClass(page);
			else {
				CandidatePageClass newClass = 
						new CandidatePageClass("class"+(classCounter++), getWebsite().getDomain());
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
				if (ci.distance(cj) < 0.2) {
					ci.collapse(cj);
					toRemove.add(cj);
				}
			}
		}
		orderedCandidates.removeAll(toRemove);

		return orderedCandidates;
	}
	
	// Update Model
	private void updateModel(List<CandidatePageClass> cands) {
		for (CandidatePageClass candidate : cands) {
			WebsiteModel modelNew = new WebsiteModel();
			WebsiteModel modelMerge = null;
			modelNew.addFinalClasses(model.getModel());
			modelNew.addFinalClass(candidate);
			double modelNewLength = modelNew.minimumLength();

			double mergeLength = Double.MAX_VALUE;
			for (CandidatePageClass cInModel : model.getModel()) {
				CandidatePageClass tempClass = 
						new CandidatePageClass(cInModel.getName(), getWebsite().getDomain());
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
	}
	
	private void logModel(Set<PageClass> pClasses, String dir) {
//		String normalBase = transformURL(base);
		String sitename = 
				getWebsite().getDomain()
				.replaceAll("http[s]?://(www.)?|/", "").replaceAll("\\.|:", "_");
//		String schema = dir+"/"+sitename+"_class_schema.txt";
		String target = dir+"/"+sitename+"_target.csv";
		
		new File(dir).mkdir();
		
		try {
//			FileWriter schemaFile = new FileWriter(schema);
//			for (CandidatePageClass cpc : model.getModel()) {
//				schemaFile.write(cpc.getName()+": "+
//						cpc.getClassPages().toString().replace(normalBase, "")+"\n");
//				for (String xp : cpc.getClassSchema()) {
//					schemaFile.write(xp+" -> "+cpc.getUrlsDiscoveredFromXPath(xp)+"\n");
//				}
//				schemaFile.write("\n");
//			}
//			schemaFile.close();

			FileWriter targetFile = new FileWriter(target);
			targetFile.write(getWebsite().getDomain()+"\n");
			for (PageClass pc : pClasses) {
				for (String xp : pc.getMenuXPaths()) {
					targetFile.write(
							pc.getName()+"\t"+"link"+"\t"+xp+"\t"+
							pc.getDestinationByXPath(xp).getName()+"\t"+"menu"+"\n");
				}
				for (String xp : pc.getListXPaths()) {
					targetFile.write(
							pc.getName()+"\t"+"link"+"\t"+xp+"\t"+
							pc.getDestinationByXPath(xp).getName()+"\t"+"list"+"\n");
				}				
				for (String xp : pc.getSingletonXPaths()) {
					targetFile.write(
							pc.getName()+"\t"+"link"+"\t"+xp+"\t"+
							pc.getDestinationByXPath(xp).getName()+"\t"+"singleton"+"\n");
				}
			}
			targetFile.close();
		} 
		catch (FileNotFoundException e) {
			getLogger().severe("File not found while logging model");
		} 
		catch (IOException e) {
			getLogger().severe("IOException while logging model");
		}
		
	}
	
	private Set<LinkCollection> newLinks(Set<Page> pages, Set<LinkCollection> visited) {
		Set<LinkCollection> newLinks = new HashSet<>();
		
		for (Page p : pages) {
			for (String xp : p.getSchema()) {
				LinkCollection lCollection = new LinkCollection(p, xp, p.getUrlsByXPath(xp));
				if (!visited.contains(lCollection)) {
					visited.add(lCollection);
					newLinks.add(lCollection);
				}
			}
		}
		return newLinks;
	}
	
	private Set<Page> makePages(Map<String, HtmlPage> url2HtmlPage) {
		Set<Page> newPages = new HashSet<>();
		for (String pageUrl : url2HtmlPage.keySet()) {
			HtmlPage htmlPage = url2HtmlPage.get(pageUrl);
			Page page = makePage(htmlPage, pageUrl);
			newPages.add(page);
		}
		return newPages;
	}
	
	@SuppressWarnings("unchecked")
	private Page makePage(HtmlPage page, String pageUrl) {
		Page p = new Page(pageUrl, model);
		List<HtmlAnchor> links = (List<HtmlAnchor>) getByMatchingXPath(page, "//a");
		for (HtmlAnchor link : links) {
			String href = link.getHrefAttribute();
			String url = getAbsoluteURL(pageUrl, href);
			p.updatePageSchema(getXPathTo(link), url);
		}
		return p;
	}

}
