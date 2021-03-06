package it.uniroma3.crawler.modeler.model;

import java.util.Set;
import java.util.stream.Stream;

import it.uniroma3.crawler.model.PageClass;
import static it.uniroma3.crawler.util.FileUtils.getRelativeUrl;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A ModelPageClass is a collection of pages with the same <i>page class schema</i> 
 * as the union of the individual page schemas, i.e.,
 * the union of the XPaths-to-link and XPath-to-labels of the pages participating the collection.
 */
public class ModelPageClass implements Comparable<ModelPageClass> {
	private int id;
	private Set<Page> pages;

	/**
	 * Constructs a new ModelPageClass with the given id
	 * @param id
	 */
	public ModelPageClass(int id) {
		this.id = id;
		this.pages = new HashSet<>();
	}
	
	/**
	 * Constructs a new ModelPageClass with the given id
	 * and a List of {@link Pages}
	 * @param id identifier
	 * @param pages List of Pages
	 */
	public ModelPageClass(int id, List<Page> pages) {
		this(id);
		this.pages.addAll(pages);
	}
	
	/**
	 * Constructs a new ModelPageClass as the union of
	 * the two given ModelPageClasses. <br>
	 * @param c1
	 * @param c2
	 */
	public ModelPageClass(ModelPageClass c1, ModelPageClass c2) {
		this(c1.getId());
		collapse(c1);
		collapse(c2);
	}
	
	public int getId() {
		return this.id;
	}
	
	/**
	 * 
	 * @return the set of XPaths of this class
	 */
	public Set<XPath> getSchema() {
		return Stream.concat(linksStream(),textsStream()).collect(toSet());
	}
	
	/**
	 * 
	 * @return the set of XPaths-to-link of this class
	 */
	public Set<XPath> getLinkSchema() {
		return linksStream().collect(toSet()); 
	}

	private Stream<XPath> linksStream() {
		return pages.stream().flatMap(p->p.getLinkSchema().stream());
	}
	
	/**
	 * 
	 * @return the set of XPaths-to-label of this class
	 */
	public Set<XPath> getLabelSchema() {
		return textsStream().collect(toSet());
	}
	
	private Stream<XPath> textsStream() {
		return pages.stream().flatMap(p->p.getLabelSchema().stream()).distinct()
				.filter(this::isLabelXPath);
	}
	
	private boolean isLabelXPath(XPath xp) {
		List<Set<String>> wList = 
			pages.stream().map(p->p.getLabels(xp)).filter(xpp->xpp!=null).collect(toList());
		return wList.size()>=2 && wList.stream().distinct().count()==1;
	}

	/**
	 * @return the pages of this class
	 */
	public Set<Page> getPages() {
		return pages;
	}
	
	/**
	 * Format a name for this ModelPageClass, consisting of the concatenation
	 * of at most three Page titles belonging to this class	 
	 *  
	 * @return the class name as a three titles concatenation
	 */
	public String name() {
		return id+"_"+pages.stream().limit(3)
				.map(p -> (p.getTitle().isEmpty()) ? getRelativeUrl(p.getUrl()) : p.getTitle())
				.map(t -> (t.length()<=20) ? t : t.substring(0, 20))
				.reduce((t1,t2)->t1+", "+t2).orElse("");
	}
	
	/**
	 * Collapses the specified ModelPageClass in the current one,
	 * adding all its pages and page schemas.
	 * @param candidate the ModelPageClass to collapse
	 */
	public void collapse(ModelPageClass candidate) {
		pages.addAll(candidate.getPages());
	}
	
	/**
	 * Returns true if the given Page belongs to the page collection
	 * @param page
	 * @return true if the page collection contains the page
	 */
	public boolean containsPage(Page page) {
		return pages.contains(page);
	}
	
	public void buildLinks(PageClass src, WebsiteModel model) {
		pages.forEach(p -> p.buildLinks(src, model));
	}
	
	/**
	 * Returns the total number of outgoing URLs associated 
	 * with this ModelPageClass
	 * @return the count of URLs in this candidate page class
	 */
	public long outgoingURLs() {
		return pages.stream().flatMap(p->p.getDiscoveredUrls().stream()).distinct().count();
	}
	
	/**
	 * Returns the total number of {@link Page}s in this ModelPageClass
	 * @return the number of pages
	 */
	public int size() {
		return pages.size();
	}
	
	public void removePage(Page p) {
		pages.remove(p);
	}
	
	public String classDescription() {
		String name = name();
		return pages.stream().map(p -> name+"\t"+p.getUrl()+"\t"+p.getTempFile()+"\n")
				.reduce(String::concat).orElse("");
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
