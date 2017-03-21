package it.uniroma3.crawler.model;

import java.util.HashSet;
import java.util.Set;

public class LinkCollection {
	private PageClassModel model;
	private Page parent;
	private Set<String> links;
	
	public LinkCollection(PageClassModel model, Page parent, Set<String> links) {
		this.model = model;
		this.parent = parent;
		this.links = new HashSet<>();
		this.links.addAll(links);
	}
	
	public Set<String> getLinks() {
		return this.links;
	}
	
	public Page getParent() {
		return this.parent;
	}
	
	public int relativeSize() {
		int size;
		CandidatePageClass cluster = model.getCandidateFromUrl(parent.getUrl());
		double prob = (double) links.size() / (double) cluster.discoveredUrlsSize();
		size = (int) (prob*10000);
		if (cluster.size()==1) 
			size *= 10;
		return size;
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
