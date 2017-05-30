package it.uniroma3.crawler.modeler.model;

import java.util.Set;
import java.util.TreeSet;

public class WebsiteModel {
	private static final double C_U = 1;
	private static final double C_XP = 1;
	private static final double C_I = 0.8;
	private static final double C_MISS = 1;

	private Set<CandidatePageClass> modelClasses;
	
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
	
	public boolean isEmpty() {
		return this.modelClasses.isEmpty();
	}
	
	public Set<CandidatePageClass> getModel() {
		return modelClasses;
	}
	
	public CandidatePageClass getCandidateFromName(String name) {
		return modelClasses.stream()
				.filter(c -> c.getName().equals(name)).findAny().orElse(null);
	}
	
	public CandidatePageClass getClassOfURL(String url) {
		return modelClasses.stream()
				.filter(c -> c.containsPage(url)).findAny().orElse(null);
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
