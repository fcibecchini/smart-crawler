package it.uniroma3.crawler.modeler.model;

import java.util.Set;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CandidatePageClass implements Comparable<CandidatePageClass> {
	private String name;
	private Set<Page> classPages;
	private Set<String> classSchema;

	public CandidatePageClass(String name) {
		this.name = name;
		this.classPages = new HashSet<>();
		this.classSchema = new HashSet<>();
	}
	
	public CandidatePageClass(String name, List<Page> pages) {
		this(name);
		pages.forEach(this::addPageToClass);
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addPageToClass(Page p) {
		p.getSchema().forEach(classSchema::add);
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
		c2.getClassPages().forEach(this::addPageToClass);
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
			if (urls!=null) 
				temp.addAll(urls);
			
		}
		return temp;
	}
	
	public List<String> getOrderedUrlsFromXPath(String xpath) {
		try {
			return classPages.stream()
					.map(p -> p.getUrlsListByXPath(xpath))
					.filter(l -> l!=null)
					.max((l1,l2) -> l1.size() - l2.size())
					.get();
		} catch (NoSuchElementException e) {
			return new ArrayList<>();
		}
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
	
	public int compareTo(CandidatePageClass other) {
		int thisRank = new Integer(getName().replace("class", ""));
		int otherRank = new Integer(other.getName().replace("class", ""));
		return thisRank - otherRank;
	}
	
	public String toString() {
		return getName()+" "+classPages.toString();
	}
 
	public int hashCode() {
		final int prime = 31;
		return prime + name.hashCode();
	}

	public boolean equals(Object obj) {
		CandidatePageClass other = (CandidatePageClass) obj;
		return Objects.equals(name, other.getName());
	}
	
}
