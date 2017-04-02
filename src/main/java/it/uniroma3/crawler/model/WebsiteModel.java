package it.uniroma3.crawler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class WebsiteModel {
	private static final double C_U = 1;
	private static final double C_XP = 1;
	private static final double C_I = 0.8;
	private static final double C_MISS = 1;

	Set<CandidatePageClass> modelClasses;
	
	public WebsiteModel() {
		this.modelClasses = new TreeSet<>();
	}
	
	public boolean addFinalClass(CandidatePageClass c) {
		return this.modelClasses.add(c);
	}
	
	public void addFinalClasses(Set<CandidatePageClass> classes) {
		this.modelClasses.addAll(classes);
	}
	
	public void removeClass(CandidatePageClass cpClass) {
		this.modelClasses.remove(cpClass);
	}
	
	public void reset() {
		this.modelClasses.clear();
	}
	
	public Set<CandidatePageClass> getModel() {
		return modelClasses;
	}
	
	public CandidatePageClass getCandidateFromName(String name) {
		return modelClasses.stream()
				.filter(c -> c.getName().equals(name)).findAny().orElse(null);
	}
	
	public CandidatePageClass getCandidateFromUrl(String url) {
		return modelClasses.stream()
				.filter(c -> c.containsPage(url)).findAny().orElse(null);
	}
	
	public List<PageClass> makePageClasses() {
		List<PageClass> pClasses = new ArrayList<>();
		
		Map<CandidatePageClass, PageClass> cand2Pclass = new TreeMap<>();
		getModel().forEach(cand -> 
			cand2Pclass.put(cand, new PageClass(cand.getName())));
				
		for (CandidatePageClass candidate : getModel()) {
			PageClass src = cand2Pclass.get(candidate);
			pClasses.add(src);
						
			for (String xpath : candidate.getClassSchema()) {
				List<String> urls = candidate.getOrderedUrlsFromXPath(xpath);
				Map<String, CandidatePageClass> url2class = new HashMap<>();
				urls.forEach(u -> url2class.put(u, getCandidateFromUrl(u)));
				
				// menu..?
				long size = url2class.values().stream().distinct().count();
				if (size>1) {
					for (String url : url2class.keySet()) {
						CandidatePageClass cpcDest = url2class.get(url);
						if (cpcDest != null) {
							PageClass dest = cand2Pclass.get(cpcDest);
							for (int index : indexesOf(urls, url)) {
								String newXPath = "(" + xpath + ")[" + (index+1) + "]";
								src.addPageClassLink(newXPath, dest);
							}
						}
					}
				}
				else if (size==1) {
					CandidatePageClass cpcDest = url2class.values().stream()
							.filter(v -> v!=null).findAny().orElse(null);
					if (cpcDest != null) {
						PageClass dest = cand2Pclass.get(cpcDest);
						src.addPageClassLink(xpath, dest);
					}
				}			
			}
		}
		
		return pClasses;
	}
	
	public double minimumLength() {
		int modelCost = 0;
		for (CandidatePageClass c : modelClasses) {
			modelCost += C_XP * c.getClassSchema().size();
		}
		
		int dataCost = 0;
		for (CandidatePageClass c : modelClasses) {
			for (Page p : c.getClassPages()) {
				int pageCost = 0;
				pageCost += C_U * p.getDiscoveredUrls().size();
				pageCost += C_I * c.insersectXPaths(p).size();
				pageCost += C_XP * p.newXPaths(c).size();
				pageCost += C_MISS * c.missingXPaths(p).size();
				dataCost += pageCost;
			}
		}
		
		return modelCost+dataCost;
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

}
