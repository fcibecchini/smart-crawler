package it.uniroma3.crawler.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

import java.util.HashMap;

public class Page {
	private String url;
	private Map<String, Set<String>> xpath2Urls;
	private PageClassModel model;
	
	public Page(String url, PageClassModel model) {
		this.model = model;
		this.url = url;
		this.xpath2Urls = new HashMap<>();
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public void updatePageSchema(String xpath, String url) {
		this.xpath2Urls.putIfAbsent(xpath, new HashSet<>());
		this.xpath2Urls.get(xpath).add(url);
	}
	
	public Set<String> getSchema() {
		return this.xpath2Urls.keySet().stream()
				.sorted((x1,x2) -> 
					xpath2Urls.get(x2).size() - xpath2Urls.get(x1).size())
				.collect(toSet());
	}
	
	public Set<String> getUrlsByXPath(String xpath) {
		return this.xpath2Urls.get(xpath);
	}
	
	public Set<String> getDiscoveredUrls() {
		return this.xpath2Urls.keySet().stream()
				.map(k -> xpath2Urls.get(k))
				.flatMap(Set::stream)
				.distinct().collect(toSet());
	}
	
	public Set<String> newXPaths(CandidatePageClass c) {
		return getSchema().stream()
				.filter(xp -> !c.getClassSchema().contains(xp))
				.collect(toSet());
	}
	
	public CandidatePageClass getCurrentCluster() {
		return model.getCandidateFromUrl(getUrl());
	}
	
	public String toString() {
		return this.url;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		Page other = (Page) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
	
}
