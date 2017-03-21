package it.uniroma3.crawler.model;

import java.util.HashSet;
import java.util.Set;

public class LinkCollection {
	private CandidatePageClass cluster;
	private Page parent;
	private Set<String> links;
	
	public LinkCollection(Set<String> links) {
		this.links = new HashSet<>();
		this.links.addAll(links);
	}
	
	public LinkCollection(PageClassModel model, Page parent, Set<String> links) {
		this(links);
		this.parent = parent;
		this.cluster = model.getCandidateFromUrl(parent.getUrl());
	}
	
	public Set<String> getLinks() {
		return this.links;
	}
	
	public Page getParent() {
		return this.parent;
	}
	
	public CandidatePageClass getCluster() {
		return this.cluster;
	}
	
	public int compareTo(LinkCollection other) {
		double thisProb = (double) getLinks().size() / (double) getCluster().discoveredUrlsSize();
		double otherProb = (double) other.getLinks().size() / (double) other.getCluster().discoveredUrlsSize();
		
		if (getCluster().size()==1 && other.getCluster().size()>1) return 1;
		if (getCluster().size()>1 && other.getCluster().size()==1) return -1;
		if (thisProb > otherProb) return 1;
		if (thisProb < otherProb) return -1;
		else return 0;
	}
	
	public String toString() {
		String desc = (parent!=null) ? parent.getUrl() : "entryPoint";
		return desc+" -> "+ getLinks().toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((links == null) ? 0 : links.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LinkCollection other = (LinkCollection) obj;
		if (links == null) {
			if (other.links != null)
				return false;
		} else if (!links.equals(other.links))
			return false;
		return true;
	}


}
