package it.uniroma3.crawler.model;

import java.util.HashSet;
import java.util.Set;

public class PageClassModel {
	private static final double C_U = 1;
	private static final double C_XP = 1;
	private static final double C_I = 0.8;
	private static final double C_MISS = 1;

	Set<CandidatePageClass> modelClasses;
	
	public PageClassModel() {
		this.modelClasses = new HashSet<>();
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
	
	public double distance(CandidatePageClass c1, CandidatePageClass c2) {
		Set<String> union = new HashSet<>();
		Set<String> diff1 = new HashSet<>();
		Set<String> diff2 = new HashSet<>();
		Set<String> unionDiff = new HashSet<>();

		union.addAll(c1.getClassSchema());
		union.addAll(c2.getClassSchema());
		
		diff1.addAll(c1.getClassSchema());
		diff1.removeAll(c2.getClassSchema());
		
		diff2.addAll(c2.getClassSchema());
		diff2.removeAll(c1.getClassSchema());
		
		unionDiff.addAll(diff1);
		unionDiff.addAll(diff2);
		
		return unionDiff.size() / union.size();
	}

}
