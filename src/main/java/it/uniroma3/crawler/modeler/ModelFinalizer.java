package it.uniroma3.crawler.modeler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.toSet;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.model.CandidatePageClass;
import it.uniroma3.crawler.modeler.model.WebsiteModel;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class ModelFinalizer {
	private WebsiteModel model;	
	private Set<PageClass> classes;
	
	public ModelFinalizer(WebsiteModel model, SeedConfig sc) {
		this.model = model;
		this.classes = model.getClasses().stream()
				.map(c -> new PageClass(String.valueOf(c.getId()),sc))
				.collect(toSet());
	}
	
	public PageClass getRoot() {
		for (CandidatePageClass candidate : model.getClasses())					
			for (String xpath : candidate.getClassSchema()) 
				linksOfClass(candidate, xpath);
		return getPageClass(1);
	}

	private void linksOfClass(CandidatePageClass cand, String xpath) {
		PageClass src = getPageClass(cand.getId());
		List<String> urls = cand.getOrderedUrlsFromXPath(xpath);
		
		Map<String, CandidatePageClass> url2class = new HashMap<>();
		urls.forEach(u -> url2class.put(u, model.getClassOfURL(u)));
		
		long classes = url2class.values().stream()
				.filter(cc->cc!=null).distinct().count();
		
		if (classes>1) 
			addMenuXPaths(xpath, src, urls, url2class);
		else if (classes==1) 
			addXPath(xpath, src, urls, url2class);
	}

	private void addXPath(String xpath, PageClass src, List<String> urls, Map<String, CandidatePageClass> url2class) {
		CandidatePageClass cpcDest = url2class.values().stream()
				.filter(v -> v!=null).findAny().orElse(null);
		if (cpcDest != null) {
			PageClass dest = getPageClass(cpcDest.getId());
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
				PageClass dest = getPageClass(cpcDest.getId());
				for (int index : indexesOf(urls, url)) {
					String newXPath = "(" + xpath + ")[" + (index+1) + "]";
					src.addMenuLink(newXPath, dest);
				}
			}
		}
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
	
	private PageClass getPageClass(int id) {
		String name = String.valueOf(id);
		return classes.stream().filter(item -> item.getName().equals(name)).findAny().orElse(null);
	}

}
