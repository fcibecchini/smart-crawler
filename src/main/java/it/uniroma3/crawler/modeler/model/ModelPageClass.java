package it.uniroma3.crawler.modeler.model;

import java.util.Set;

import static java.util.stream.Collectors.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A CandidatePageClass is a collection of pages with the same <i>page class schema</i> 
 * as the union of the individual page schemas, i.e.,
 * the union of the XPaths-to-link of the pages participating the collection.
 */
public class CandidatePageClass implements Comparable<CandidatePageClass> {
	private int id;
	private Set<Page> classPages;
	private Set<String> classSchema;

	/**
	 * Constructs a new CandidatePageClass with the given id
	 * @param id
	 */
	public CandidatePageClass(int id) {
		this.id = id;
		this.classPages = new HashSet<>();
		this.classSchema = new HashSet<>();
	}
	
	/**
	 * Constructs a new CandidatePageClass with the given id
	 * and a List of {@link Pages}
	 * @param id identifier
	 * @param pages List of Pages
	 */
	public CandidatePageClass(int id, List<Page> pages) {
		this(id);
		pages.forEach(this::addPageToClass);
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Adds a {@link Page} to the current schema of this CandidatePageClass. <br>
	 * The page schema of this page is added to the page class schema.
	 * @param p the Page to add
	 */
	public void addPageToClass(Page p) {
		p.getSchema().forEach(classSchema::add);
		classPages.add(p);
	}
	
	/**
	 * Adds an XPath to the page class schema.
	 * @param xpath
	 * @return true if this schema did not already contain the xpath
	 */
	public boolean addXPathToSchema(String xpath) {
		return classSchema.add(xpath);
	}
	
	public Set<String> getClassSchema() {
		return classSchema;
	}
	
	public void setClassSchema(Set<String> schema) {
		classSchema.addAll(schema);
	}
	
	public Set<Page> getClassPages() {
		return classPages;
	}
	
	/**
	 * Collapses the specified CandidatePageClass in the current one,
	 * adding all its pages and page schemas.
	 * @param candidate the CandidatePageClass to collapse
	 */
	public void collapse(CandidatePageClass candidate) {
		candidate.getClassPages().forEach(this::addPageToClass);
	}
	
	/**
	 * Returns true if this page class schema contains the specified XPath
	 * @param xpath
	 * @return true if this page class schema contains the XPath
	 */
	public boolean containsXPath(String xpath) {
		return classSchema.contains(xpath);
	}
	
	/**
	 * Returns true if the given URL belongs to the page collection
	 * @param url
	 * @return true if the page collection contains the url
	 */
	public boolean containsPage(String url) {
		return classPages.stream().anyMatch(p -> p.getUrl().equals(url));
	}
	
	/**
	 * Returns true if the given Page belongs to the page collection
	 * @param page
	 * @return true if the page collection contains the page
	 */
	public boolean containsPage(Page page) {
		return classPages.contains(page);
	}
	
	/**
	 * Returns the total number of outgoing URLs associated 
	 * with this CandidatePageClass
	 * @return the count of URLs in this candidate page class
	 */
	public long outgoingURLs() {
		return classPages.stream()
				.map(Page::getDiscoveredUrls)
				.flatMap(Set::stream)
				.distinct().count();
	}
	
	/**
	 * Returns the cardinality of the intersection between the page schema 
	 * of the given {@link Page} and the page class schema of this CandidatePageClass
	 * @param p the Page
	 * @return the cardinality of the intersection
	 */
	public long schemaIntersectionSize(Page p) {
		return classSchema.stream()
				.filter(xp -> p.getSchema().contains(xp))
				.count();
	}
	
	/**
	 * Returns the cardinality of the difference between the page class schema 
	 * of this CandidatePageClass and the page schema of the given {@link Page} 
	 * @param p the Page
	 * @return the XPaths difference
	 */
	public long schemaDifferenceSize(Page p) {
		return classSchema.stream()
				.filter(xp -> !p.getSchema().contains(xp))
				.count();
	}
	
	/**
	 * Returns a Set of outgoing URLs from the pages of this
	 * CandidatePageClass matched by the given XPath
	 * @param xpath
	 * @return a Set of outgoing URLs matched by the XPath
	 */
	public Set<String> getUrlsDiscoveredFromXPath(String xpath) {
		return classPages.stream()
				.map(p -> p.getURLsByXPath(xpath))
				.flatMap(List::stream).collect(toSet());
	}
	
	/**
	 * Returns a List of outgoing URLs matched by the given XPath
	 * with the max number of URLs among all the Pages of this CandidatePageClass.
	 * @param xpath
	 * @return bigger List of outgoing URLs matched by the given XPath
	 */
	public List<String> getOrderedUrlsFromXPath(String xpath) {
		return classPages.stream()
				.map(p -> p.getURLsByXPath(xpath))
				.max((l1,l2) -> l1.size() - l2.size())
				.get();
	}
	
	/**
	 * Returns the total number of {@link Page}s in this CandidatePageClass
	 * @return the number of pages
	 */
	public int size() {
		return classPages.size();
	}
	
	/**
	 * Returns the total number of XPaths in the page class schema of this
	 * CandidatePageClass
	 * @return the number of XPaths
	 */
	public int schemaSize() {
		return classSchema.size();
	}
	
	/**
	 * The distance between the page class schemas is defined as
	 * the normalized cardinality of the symmetric 
	 * set difference between the two schemas.<br>
	 * Namely, let Gi and Gj be the schemas of groups i and j; then:<br>
	 * distance(Gi,Gj) = |(Gi-Gj) U (Gj-Gi)| / |(Gi U Gj)| <br>
	 * Note that if Gi = Gj (identical schemas), then distance(Gi,Gj) = 0;<br>
	 * conversely, if Gi ∩ Gj = empty (the schemas are disjoint), 
	 * then distance(Gi,Gj) = 1
	 * @param other the CandidatePageClass to be compared
	 * @return the distance between this CandidatePageClass and the other
	 */
	public double distance(CandidatePageClass other) {
		Set<String> union = new HashSet<>();
		Set<String> diff1 = new HashSet<>();
		Set<String> diff2 = new HashSet<>();
		Set<String> unionDiff = new HashSet<>();

		union.addAll(classSchema);
		union.addAll(other.getClassSchema());
		
		diff1.addAll(classSchema);
		diff1.removeAll(other.getClassSchema());
				
		diff2.addAll(other.getClassSchema());
		diff2.removeAll(classSchema);
		
		unionDiff.addAll(diff1);
		unionDiff.addAll(diff2);
		
		return (double) unionDiff.size() / (double) union.size();
	}
	
	public int compareTo(CandidatePageClass other) {
		return id - other.getId();
	}
	
	public String toString() {
		return id+": "+classPages.toString();
	}
 
	public int hashCode() {
		return Objects.hash(id);
	}

	public boolean equals(Object obj) {
		CandidatePageClass other = (CandidatePageClass) obj;
		return id==other.getId();
	}
	
}
