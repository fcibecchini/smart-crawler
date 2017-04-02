package it.uniroma3.crawler.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
				Set<String> urls = candidate.getUrlsDiscoveredFromXPath(xpath);
				
				Set<CandidatePageClass> destClasses = urls.stream()
						.map(u -> getCandidateFromUrl(u))
						.filter(cc -> cc!=null)
						.collect(Collectors.toSet());
				if (!destClasses.isEmpty()) {
					CandidatePageClass destClass = destClasses.stream()
					.max((c1,c2) -> (int)c1.discoveredUrlsSize()-(int)c2.discoveredUrlsSize())
					.get();
					PageClass dest = cand2Pclass.get(destClass);
					src.addPageClassLink(xpath, dest);
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

}
