package it.uniroma3.crawler.modeler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import it.uniroma3.crawler.model.CandidatePageClass;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.model.WebsiteModel;

public class ModelFinalizer {
	private WebsiteModel model;
	private Website website;
	private TreeSet<PageClass> pClasses;
	private int wait;
	
	public ModelFinalizer(WebsiteModel model, Website website, int wait) {
		this.model = model;
		this.website = website;
		this.wait = wait;
	}
	
	public TreeSet<PageClass> makePageClasses() {
		this.pClasses = initPageClasses();
		
		for (CandidatePageClass candidate : model.getModel())					
			for (String xpath : candidate.getClassSchema()) 
				linksOfClass(candidate, xpath);			

		return pClasses;
	}

	private void linksOfClass(CandidatePageClass cand, String xpath) {
		PageClass src = getPageClass(cand.getName());
		List<String> urls = cand.getOrderedUrlsFromXPath(xpath);
		
		Map<String, CandidatePageClass> url2class = new HashMap<>();
		urls.forEach(u -> url2class.put(u, model.getClassOfURL(u)));
		
		long classes = url2class.values().stream().filter(cc->cc!=null).distinct().count();
		
		if (classes>1) 
			addMenuXPaths(xpath, src, urls, url2class);
		else if (classes==1) 
			addXPath(xpath, src, urls, url2class);
	}

	private void addXPath(String xpath, PageClass src, List<String> urls, Map<String, CandidatePageClass> url2class) {
		CandidatePageClass cpcDest = url2class.values().stream()
				.filter(v -> v!=null).findAny().orElse(null);
		if (cpcDest != null) {
			PageClass dest = getPageClass(cpcDest.getName());
			if (urls.size()>1) 
				src.addListLink(xpath, dest);
			else 
				src.addSingletonLink(xpath, dest);
		}
	}

	private void addMenuXPaths(String xpath, PageClass src, List<String> urls, Map<String, CandidatePageClass> url2class) {
		for (String url : url2class.keySet()) {
			CandidatePageClass cpcDest = url2class.get(url);
			if (cpcDest != null) {
				PageClass dest = getPageClass(cpcDest.getName());
				for (int index : indexesOf(urls, url)) {
					String newXPath = "(" + xpath + ")[" + (index+1) + "]";
					src.addMenuLink(newXPath, dest);
				}
			}
		}
	}	
	
	private PageClass getPageClass(String name) {
		return pClasses.stream().filter(item -> item.getName().equals(name)).findAny().orElse(null);
	}
	
	private List<Integer> indexesOf(List<String> urls, String url) {
		List<Integer> indexes = new ArrayList<>();
		int i = 0;
		for (String u : urls) {
			if (u.equals(url)) indexes.add(i);
			i++;
		}
		return indexes;
	}
	
	private TreeSet<PageClass> initPageClasses() {
		TreeSet<PageClass> pClasses = new TreeSet<>(
				(p1,p2) -> {
					int r1 = new Integer(p1.getName().replace("class", ""));
					int r2 = new Integer(p2.getName().replace("class", ""));
					return r1-r2;
				});
		model.getModel().forEach(cand -> {
			PageClass pc = new PageClass(cand.getName(),website,wait);
			pClasses.add(pc);});
		return pClasses;
	}

}
