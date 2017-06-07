package it.uniroma3.crawler.modeler.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import static it.uniroma3.crawler.util.XPathUtils.getAbsoluteURLs;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Page is a web page represented as a subset of the XPaths-to-link in the corresponding 
 * DOM tree representation, along with the referenced URLs themselves.
 *
 */
public class Page {
	private String url;
	private String tempFile;
	private Map<XPath, List<String>> xpathToURLs;
	private List<PageLink> links;
	
	/**
	 * Constructs a new Page identified by the given URL and 
	 * with a page schema inferred from the {@link HtmlPage}.<br>
	 * @param url the URL of the page
	 * @param html the HtmlPage
	 */
	public Page(String url, HtmlPage html) {
		this.url = url;
		this.xpathToURLs = new HashMap<>();
		pageSchema(html);
		this.links = new ArrayList<>();
	}
	
	/**
	 * Returns this web Page URL
	 * @return the URL
	 */
	public String getUrl() {
		return this.url;
	}
	
	/**
	 * 
	 * @return the file path where this page has been saved, if any
	 */
	public String getTempFile() {
		return tempFile;
	}
	
	/**
	 * Sets a file path where this page has been saved.
	 * @param path the file path
	 */
	public void setTempFile(String path) {
		this.tempFile = path;
	}
	
	/**
	 * Adds a link of type List to the given XPath and destination Page
	 * 
	 * @param xp the xpath
	 * @param dest one of the Pages in the list
	 */
	public void addListLink(String xp, Page dest) {
		links.add(new PageLink(xp,dest,1));
	}
	
	/**
	 * Adds a link of type Menu to the given XPath and destination Page
	 * 
	 * @param xp the xpath
	 * @param dest the destination Page as a menu item
	 */
	public void addMenuLink(String xp, Page dest) {
		links.add(new PageLink(xp,dest,2));
	}
	
	/**
	 * Adds a link of type Singleton to the given XPath and destination Page
	 * 
	 * @param xp the xpath
	 * @param dest the Singleton destination Page
	 */
	public void addSingleLink(String xp, Page dest) {
		links.add(new PageLink(xp,dest,3));
	}
	
	/**
	 * 
	 * @return all the {@link PageLink}s of this Page
	 */
	public List<PageLink> getLinks() {
		return links;
	}

	/*
	 * INTERNAL API 
	 * Groups outgoing URLs by XPaths-to-link to build the page schema. 
	 * Grouping is defined over the XPath.getDefault() version of the XPaths.
	 * 
	 */
	private void pageSchema(HtmlPage html) {
		html.getAnchors().stream().map(XPath::new).distinct()
			.forEach(xp ->
				xpathToURLs.put(xp, getAbsoluteURLs(html, xp.getDefault(), url)));
	}
	
	/**
	 * Returns the <i>page schema</i> of this Page.<br>
	 * A page schema is an abstraction of a page consisting of a set 
	 * of DOM XPaths-to-link.<br> A page schema is computed simply from the 
	 * page's DOM tree by considering  only the set of paths starting 
	 * from the root (or from a tag with an ID value) and ending in link tags.<br>
	 * The precise, static version of the XPaths used for the page class
	 * schema definition is the one defined by {@link XPath#getDefault()}.
	 * @return the page schema
	 */
	public Set<String> getSchema() {
		return xpathToURLs.keySet().stream()
				.map(XPath::getDefault).collect(toSet());
	}
	
	/**
	 * 
	 * @return the XPaths-to-link of this page
	 */
	public Set<XPath> getXPaths() {
		return xpathToURLs.keySet();
	}
	
	public void printSchema() {
		xpathToURLs.forEach((xp,l) -> System.out.println(xp.getDefault()+" "+l));
	}
	
	/**
	 * Returns the List of URLs referenced by the given {@link XPath#getDefault()} 
	 * version.
	 * @param xpath the XPath
	 * @return the outgoing URLs
	 */
	public List<String> getURLsByXPath(XPath xpath) {
		List<String> urls = xpathToURLs.get(xpath);
		return (urls!=null) ? urls : new ArrayList<>();
	}
	
	/**
	 * Returns a collection of unique outgoing URLs of this Page.
	 * @return the outgoing URLs set
	 */
	public Set<String> getDiscoveredUrls() {
		return xpathToURLs.values().stream()
				.flatMap(List::stream)
				.distinct().collect(toSet());
	}
	
	/**
	 * Returns the total number of outgoing URLs of this Page.
	 * @return the number of outgoing URLs
	 */
	public long urlsSize() {
		return xpathToURLs.values().stream()
				.flatMap(List::stream)
				.distinct().count();
	}
	
	/**
	 * Returns the cardinality of the difference between this Page schema
	 * and the specified {@link ModelPageClass} schema.
	 * @param candidate
	 * @return the cardinality of the difference between the two schemas
	 */
	public long schemaDifferenceSize(ModelPageClass candidate) {
		return getSchema().stream()
				.filter(xp -> !candidate.getClassSchema().contains(xp))
				.count();
	}
	
	public String toString() {
		return url;
	}

	public int hashCode() {
		return url.hashCode();
	}

	public boolean equals(Object obj) {
		Page other = (Page) obj;
		return Objects.equals(url, other.getUrl());
	}
	
}
