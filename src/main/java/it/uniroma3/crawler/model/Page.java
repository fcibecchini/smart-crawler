package it.uniroma3.crawler.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Page {
	private String url;
	private Map<String, List<String>> xpath2Urls;
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
		this.xpath2Urls.putIfAbsent(xpath, new ArrayList<>());
		this.xpath2Urls.get(xpath).add(url);
	}
	
	public Set<String> getSchema() {
		return this.xpath2Urls.keySet().stream()
				.sorted((x1,x2) -> 
					xpath2Urls.get(x2).size() - xpath2Urls.get(x1).size())
				.collect(toSet());
	}
	
	public Set<String> getUrlsByXPath(String xpath) {
		List<String> urls = this.xpath2Urls.get(xpath);
		if (urls != null)
			return urls.stream().collect(toSet());
		return null;
	}
	
	public List<String> getUrlsListByXPath(String xpath) {
		return this.xpath2Urls.get(xpath);
	}
	
	public void removeXPathFromSchema(String xpath) {
		this.xpath2Urls.remove(xpath);
	}
	
	public int getUrlIndex(String xpath, String url) {
		return this.xpath2Urls.get(xpath).indexOf(url);
	}
	
	public Set<String> getDiscoveredUrls() {
		return this.xpath2Urls.keySet().stream()
				.map(k -> xpath2Urls.get(k))
				.flatMap(List::stream)
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
