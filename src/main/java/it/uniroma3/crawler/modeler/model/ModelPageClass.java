package it.uniroma3.crawler.modeler.model;

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
	private Set<Page> classPages;
	private Set<String> classSchema;

	/**
	 * Constructs a new ModelPageClass with the given id
	 * @param id
	 */
	public ModelPageClass(int id) {
		this.id = id;
		this.classPages = new HashSet<>();
		this.classSchema = new HashSet<>();
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
	
	public Set<Page> getClassPages() {
		return classPages;
	}
	
	/**
	 * Format a name for this ModelPageClass, consisting in the concatenation
	 * of at most three Page local-URLs belonging to this class, 
	 * plus the id of this class.
	 * 
	 * @return the class name as a three urls concatenation
	 */
	public String name() {
		return classPages.stream().limit(3)
		.map(p -> Paths.get(p.getUrl()))
		.map(url -> { 
			int max = url.getNameCount();
			return url.subpath((max>2) ? 2 : 1, max).toString();
		})
		.reduce((u1,u2) -> u1+","+u2).get();
	}
	
	/**
	 * Collapses the specified ModelPageClass in the current one,
	 * adding all its pages and page schemas.
	 * @param candidate the ModelPageClass to collapse
	 */
	public void collapse(ModelPageClass candidate) {
		candidate.getClassPages().forEach(this::addPageToClass);
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
	 * with this ModelPageClass
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
	 * of the given {@link Page} and the page class schema of this ModelPageClass
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
	 * of this ModelPageClass and the page schema of the given {@link Page} 
	 * @param p the Page
	 * @return the XPaths difference
	 */
	public long schemaDifferenceSize(Page p) {
		return classSchema.stream()
				.filter(xp -> !p.getSchema().contains(xp))
				.count();
	}
	
	/**
	 * Returns the total number of {@link Page}s in this ModelPageClass
	 * @return the number of pages
	 */
	public int size() {
		return classPages.size();
	}
	
	/**
	 * Returns the total number of XPaths in the page class schema of this
	 * ModelPageClass
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
	 * @param other the ModelPageClass to be compared
	 * @return the distance between this ModelPageClass and the other
	 */
	public double distance(ModelPageClass other) {
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
	
	public int compareTo(ModelPageClass other) {
		return id - other.getId();
	}
	
	public String toString() {
		return id+": "+classPages.toString();
	}
 
	public int hashCode() {
		return Objects.hash(id);
	}

	public boolean equals(Object obj) {
		ModelPageClass other = (ModelPageClass) obj;
		return id==other.getId();
	}
	
}
