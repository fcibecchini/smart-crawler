package it.uniroma3.crawler.model;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class LinkCollection implements Comparable<LinkCollection> {
	private Page parent;
	private String xpath;
	private Set<String> links;
	
	public LinkCollection(String seed) {
		this.links = new TreeSet<>();
		this.links.add(seed);
	}
	
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
	
	public boolean isEmpty() {
		return this.links.isEmpty();
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
		return 0;
	}
	
	public String toString() {
		String desc = (parent!=null) ? parent.getUrl() : "entryPoint";
		return desc+" -> "+ getLinks().toString();
	}
	
	public int hashCode() {
		return links.hashCode() + ((xpath == null) ? 0 : xpath.hashCode());
	}

	public boolean equals(Object obj) {
		LinkCollection other = (LinkCollection) obj;
		if (!Objects.equals(getXPath(), other.getXPath()))
			return false;
		return Objects.equals(getLinks(), other.getLinks());
	}

}
