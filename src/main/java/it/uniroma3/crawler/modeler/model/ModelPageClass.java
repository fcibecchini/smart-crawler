package it.uniroma3.crawler.modeler.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A ModelPageClass is a collection of pages with the same <i>page class schema</i> 
 * as the union of the individual page schemas, i.e.,
 * the union of the XPaths-to-link of the pages participating the collection.
 */
public class ModelPageClass implements Comparable<ModelPageClass> {
	private int id;
	private Set<Page> pages;
	private Set<XPath> schema;

	/**
	 * Constructs a new ModelPageClass with the given id
	 * @param id
	 */
	public ModelPageClass(int id) {
		this.id = id;
		this.pages = new HashSet<>();
		this.schema = new HashSet<>();
	}
	
	/**
	 * Constructs a new ModelPageClass with the given id
	 * and a List of {@link Pages}
	 * @param id identifier
	 * @param pages List of Pages
	 */
	public ModelPageClass(int id, List<Page> pages) {
		this(id);
		pages.forEach(this::addPageToClass);
	}
	
	public int getId() {
		return this.id;
	}
	
	/**
	 * Adds a {@link Page} to the current schema of this ModelPageClass. <br>
	 * The page schema of this page is added to the page class schema.
	 * @param p the Page to add
	 */
	public void addPageToClass(Page p) {
		if (pages.add(p))
			schema.addAll(p.getSchema());
	}
	
	/**
	 * 
	 * @return the set of XPaths of this schema
	 */
	public Set<XPath> getSchema() {
		return schema;
	}
	
	public Set<Page> getPages() {
		return pages;
	}
	
	/**
	 * Format a name for this ModelPageClass, consisting of the concatenation
	 * of at most three Page local-URLs belonging to this class	 
	 *  
	 * @return the class name as a three URLs concatenation
	 */
	public String name() {
		return pages.stream().limit(3)
		.map(Page::getUrl)
		.map(Paths::get)
		.map(url -> url.subpath((url.getNameCount()>2) ? 2 : 1, url.getNameCount()))
		.map(Path::toString)
		.reduce((u1,u2) -> u1+","+u2).get();
	}
	
	/**
	 * Collapses the specified ModelPageClass in the current one,
	 * adding all its pages and page schemas.
	 * @param candidate the ModelPageClass to collapse
	 */
	public void collapse(ModelPageClass candidate) {
		candidate.getPages().forEach(this::addPageToClass);
	}
	
	/**
	 * Returns true if the given Page belongs to the page collection
	 * @param page
	 * @return true if the page collection contains the page
	 */
	public boolean containsPage(Page page) {
		return pages.contains(page);
	}
	
	/**
	 * Returns the total number of outgoing URLs associated 
	 * with this ModelPageClass
	 * @return the count of URLs in this candidate page class
	 */
	public long outgoingURLs() {
		return pages.stream()
				.map(Page::getDiscoveredUrls)
				.flatMap(Set::stream)
				.distinct().count();
	}
	
	/**
	 * Returns the cardinality of the intersection between the page schema 
	 * of the given {@link Page} and the page class schema of this ModelPageClass
	 * @param p the Page
	 * @return the cardinality of the intersection
	 */
	public long schemaIntersectionSize(Page p) {
		Set<XPath> pageSchema = p.getSchema();
		return schema.stream().filter(pageSchema::contains).count();
	}
	
	/**
	 * Returns the cardinality of the difference between the page class schema 
	 * of this ModelPageClass and the page schema of the given {@link Page} 
	 * @param p the Page
	 * @return the XPaths difference
	 */
	public long schemaDifferenceSize(Page p) {
		Set<XPath> pageSchema = p.getSchema();
		return schema.stream().filter(xp -> !pageSchema.contains(xp)).count();
	}
	
	/**
	 * Returns the total number of {@link Page}s in this ModelPageClass
	 * @return the number of pages
	 */
	public int size() {
		return pages.size();
	}
	
	/**
	 * Returns the total number of XPaths in the page class schema of this
	 * ModelPageClass
	 * @return the number of XPaths
	 */
	public int schemaSize() {
		return schema.size();
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
	 * @param other the ModelPageClass to be compared
	 * @return the distance between this ModelPageClass and the other
	 */
	public double distance(ModelPageClass other) {
		Set<XPath> union = new HashSet<>();
		Set<XPath> diff1 = new HashSet<>();
		Set<XPath> diff2 = new HashSet<>();
		Set<XPath> unionDiff = new HashSet<>();

		union.addAll(getSchema());
		union.addAll(other.getSchema());
		
		diff1.addAll(getSchema());
		diff1.removeAll(other.getSchema());
				
		diff2.addAll(other.getSchema());
		diff2.removeAll(getSchema());
		
		unionDiff.addAll(diff1);
		unionDiff.addAll(diff2);
		
		return (double) unionDiff.size() / (double) union.size();
	}
	
	public int compareTo(ModelPageClass other) {
		return id - other.getId();
	}
	
	public String toString() {
		return id+": "+pages.toString();
	}
 
	public int hashCode() {
		return Objects.hash(id);
	}

	public boolean equals(Object obj) {
		ModelPageClass other = (ModelPageClass) obj;
		return id==other.getId();
	}
	
}
