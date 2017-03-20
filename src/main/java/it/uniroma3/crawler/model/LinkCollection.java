package it.uniroma3.crawler.model;

import java.util.HashSet;
import java.util.Set;

public class LinkCollection {
	private Page parent;
	private Set<String> links;
	
	public LinkCollection(Page parent, Set<String> links) {
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
		double prob = (double) links.size() / (double) parent.getDiscoveredUrls().size();
		int size = (int) (prob*1000);
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
