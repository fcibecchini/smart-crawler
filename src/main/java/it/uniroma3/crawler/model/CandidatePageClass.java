package it.uniroma3.crawler.model;

import java.util.Set;

import static java.util.stream.Collectors.*;

import java.util.HashSet;

public class CandidatePageClass {
	private String baseUrl;
	private String name;
	private Set<Page> classPages;
	private Set<String> classSchema;

	public CandidatePageClass(String name) {
		this.name = name;
		this.classPages = new HashSet<>();
		this.classSchema = new HashSet<>();
	}
	
	public CandidatePageClass(String name, String base) {
		this(name);
		this.baseUrl = base;
	}
	
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addPageToClass(Page p) {
		p.getSchema().forEach(xp -> classSchema.add(xp));
		this.classPages.add(p);
	}
	
	public boolean addXPathToSchema(String xpath) {
		return this.classSchema.add(xpath);
	}
	
	public Set<String> getClassSchema() {
		return this.classSchema;
	}
	
	public void setClassSchema(Set<String> schema) {
		this.classSchema.addAll(schema);
	}
	
	public Set<Page> getClassPages() {
		return this.classPages;
	}
	
	public void collapse(CandidatePageClass c2) {
		c2.getClassPages().forEach(p -> this.addPageToClass(p));
	}
	
	public boolean containsXPath(String xpath) {
		return this.classSchema.contains(xpath);
	}
	
	public boolean containsPage(String url) {
		return classPages.stream().anyMatch(p -> p.getUrl().equals(url));
	}
	
	public long discoveredUrlsSize() {
		return classPages.stream()
				.map(p -> p.getDiscoveredUrls())
				.flatMap(Set::stream)
				.distinct().count();
	}
	
	public Set<String> insersectXPaths(Page p) {
		return getClassSchema().stream()
				.filter(xp -> p.getSchema().contains(xp))
				.collect(toSet());
	}
	
	public Set<String> missingXPaths(Page p) {
		return getClassSchema().stream()
				.filter(xp -> !p.getSchema().contains(xp))
				.collect(toSet());
	}
	
	public Set<String> getUrlsDiscoveredFromXPath(String xpath) {
		Set<String> temp = new HashSet<>();
		for (Page p : classPages) {
			Set<String> urls = p.getUrlsByXPath(xpath);
			if (urls!=null) {
				for (String url : urls) {
					String newUrl = (!url.contains("http")) ? baseUrl+url : url;
					temp.add(newUrl);
				}
			}
		}
		return temp;
	}
	
	public int size() {
		return classPages.size();
	}
	
	public double distance(CandidatePageClass other) {
		Set<String> union = new HashSet<>();
		Set<String> diff1 = new HashSet<>();
		Set<String> diff2 = new HashSet<>();
		Set<String> unionDiff = new HashSet<>();

		union.addAll(this.getClassSchema());
		union.addAll(other.getClassSchema());
		
		diff1.addAll(this.getClassSchema());
		diff1.removeAll(other.getClassSchema());
				
		diff2.addAll(other.getClassSchema());
		diff2.removeAll(this.getClassSchema());
		
		unionDiff.addAll(diff1);
		unionDiff.addAll(diff2);
		
		return (double) unionDiff.size() / (double) union.size();

	}
	
	public String toString() {
		return getName();
	}
 
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		CandidatePageClass other = (CandidatePageClass) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
