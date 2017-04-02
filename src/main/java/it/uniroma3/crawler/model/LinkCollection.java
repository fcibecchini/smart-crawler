package it.uniroma3.crawler.model;

import java.util.Set;
import java.util.TreeSet;

public class LinkCollection implements Comparable<LinkCollection> {
	private Page parent;
	private String xpath;
	private Set<String> links;
	
	public LinkCollection(Set<String> links) {
		this.links = new TreeSet<>();
		this.links.addAll(links);
	}
	
	public LinkCollection(Page parent, String xpath, Set<String> links) {
		this(links);
		this.xpath = xpath;
		this.parent = parent;
	}
	
	public Set<String> getLinks() {
		return this.links;
	}
	
	public Page getParent() {
		return this.parent;
	}
	
	public CandidatePageClass getCluster() {
		return this.parent.getCurrentCluster();
	}
	
	public String getXPath() {
		return this.xpath;
	}
	
	public int size() {
		return this.links.size();
	}
	
	public int compareTo(LinkCollection other) {
		if (getCluster().size()==1 && other.getCluster().size()>1) return -1;
		if (getCluster().size()>1 && other.getCluster().size()==1) return 1;

		double thisProb = (double) getLinks().size() / (double) getCluster().discoveredUrlsSize();
		double otherProb = (double) other.getLinks().size() / (double) other.getCluster().discoveredUrlsSize();
		
		if (thisProb > otherProb) return -1;
		if (thisProb < otherProb) return 1;
		else return 0;
	}
	
	public String toString() {
		String desc = (parent!=null) ? parent.getUrl() : "entryPoint";
		return desc+" -> "+ getLinks().toString();
	}
	
	public int hashCode() {
		return links.hashCode()
				+ ((parent == null) ? 0 : parent.hashCode())
				+ ((xpath == null) ? 0 : xpath.hashCode());
	}

	public boolean equals(Object obj) {
		LinkCollection other = (LinkCollection) obj;
		if (parent == null) {
			if (other.getParent() != null)
				return false;
		} 
		else if (!parent.equals(other.getParent()))
			return false;
		if (xpath == null) {
			if (other.getXPath() != null)
				return false;
		} 
		else if (!xpath.equals(other.xpath))
			return false;
		return links.equals(other.getLinks());
	}

}
