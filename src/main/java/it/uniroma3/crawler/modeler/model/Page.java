package it.uniroma3.crawler.modeler.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import static it.uniroma3.crawler.util.XPathUtils.getRelativeURLs;
import static it.uniroma3.crawler.util.XPathUtils.getTextNodes;
import static it.uniroma3.crawler.util.XPathUtils.getByMatchingXPath;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A Page is a web page represented as a subset of the XPaths-to-link in the corresponding 
 * DOM tree representation, along with the referenced URLs themselves.
 *
 */
public class Page {
	private String url;
	private String href;
	private String tempFile;
	private Set<LinkCollection> linkCollections;
	private Map<XPath, Integer> textXPaths;
	private Set<String> urls;
	private List<PageLink> links;
	private boolean loaded, classified;
	
	/**
	 * Constructs a new Page identified by the given URL and 
	 * with a page schema inferred from the {@link HtmlPage}.<br>
	 * @param url the URL of the page
	 * @param html the HtmlPage
	 */
	public Page(String url, HtmlPage html) {
		this.url = url;
		this.linkCollections = pageSchema(html);
		this.textXPaths = textSchema(html);
		this.links = new ArrayList<>();
	}
	
	/**
	 * Returns this web Page URL
	 * @return the URL
	 */
	public String getUrl() {
		return this.url;
	}
	
	public String getHref() {
		return this.href;
	}
	
	public void setHref(String href) {
		this.href = href;
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
	public void addListLink(String xp, List<Page> destinations) {
		links.add(new ListPageLink(xp,destinations));
	}
	
	/**
	 * Adds a link of type Menu to the given XPath and destination Page
	 * 
	 * @param xp the xpath
	 * @param dest the destination Page as a menu item
	 */
	public void addMenuLink(String xp, List<Page> destinations) {
		MenuPageLink ml = new MenuPageLink(xp, destinations);
		ml.setSourceUrl(url);
		links.add(ml);
	}
	
	/**
	 * Adds a link of type Singleton to the given XPath and destination Page
	 * 
	 * @param xp the xpath
	 * @param text the anchor text of this link
	 * @param dest the Singleton destination Page
	 */
	public void addSingleLink(String xp, String text, List<Page> destinations) {
		links.add(new SinglePageLink(xp,text,destinations));
	}
	
	/**
	 * 
	 * @return all the {@link PageLink}s of this Page
	 */
	public List<PageLink> getLinks() {
		return links;
	}
	
	public void setLoaded() {
		loaded = true;
	}
	
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Test and <b>Set</b> if a page was classified in the model.
	 * @return true if this page was classified in the model, false otherwise
	 */
	public boolean classified() {
		boolean cl = classified;
		if (!cl) classified = true;
		return cl;
	}
	
	/**
	 * @return true if this page was classified in the model, false otherwise
	 */
	public boolean isClassified() {
		return classified;
	}
	
	/*
	 * INTERNAL API 
	 * Groups outgoing URLs by XPaths-to-link to build the page schema.
	 */
	private Set<LinkCollection> pageSchema(HtmlPage html) {
		this.urls = new HashSet<>();
		Set<LinkCollection> collections = new HashSet<>();
		Set<XPath> xpaths = html.getAnchors().stream().map(XPath::new).collect(toSet());
		for (XPath xp : xpaths) {
			List<String> urls = getRelativeURLs(html,xp.getDefault());
			if (!urls.isEmpty()) {
				this.urls.addAll(urls);
				LinkCollection lc = new LinkCollection(this,xp,urls);
				collections.add(lc);
			}
		}
		return collections;
	}
	
	private Map<XPath, Integer> textSchema(HtmlPage html) {
		Map<XPath, Integer> textXPaths = new HashMap<>();
		Set<XPath> xpaths = getTextNodes(html,16).stream().map(XPath::new).collect(toSet());
		for (XPath xp : xpaths) {
			int texts = getByMatchingXPath(html, xp.getDefault()).size();
			textXPaths.put(xp, texts);
		}
		return textXPaths;
	}
	
	/**
	 * Returns the <i>page schema</i> of this Page.<br>
	 * A page schema is an abstraction of a page consisting of a set 
	 * of DOM {@link XPath}s.<br> A page schema is computed simply from the 
	 * page's DOM tree by considering  only the set of paths starting 
	 * from the root (or from a tag with an ID value) and ending in link and text tags.<br>
	 * Schema could change over time due to XPaths granularity updates.
	 * @return the page schema
	 */
	public Set<XPath> getSchema() {
		Set<XPath> xpaths = linkCollections.stream().map(LinkCollection::getXPath).collect(toSet());
		xpaths.addAll(textXPaths.keySet());
		return xpaths;
	}
	
	/**
	 * Returns the <i>default page schema</i> of this Page, based on {@link XPath#getDefault} 
	 * <br>
	 * See also {@link Page#getSchema}
	 * @return the default page schema
	 */
	public Set<String> getDefaultSchema() {
		Set<String> xpaths = linkCollections.stream().map(LinkCollection::getXPath)
				.map(XPath::getDefault).collect(toSet());
		textXPaths.keySet().forEach(xp -> xpaths.add(xp.getDefault()));
		return xpaths;
	}
	
	public Set<LinkCollection> getLinkCollections() {
		return linkCollections;
	}
	
	/**
	 * Returns a collection of unique outgoing URLs of this Page.
	 * @return the outgoing URLs set
	 */
	public Set<String> getDiscoveredUrls() {
		return urls;
	}
	
	/**
	 * Returns the total number of outgoing URLs of this Page.
	 * @return the number of outgoing URLs
	 */
	public int urlsSize() {
		return urls.size();
	}
	
	/**
	 * Returns the number of elements matched by the given XPath in this page.
	 * @param path the XPath
	 * @return number of elements matched
	 */
	public int getXPathFrequency(XPath path) {
		Integer freq = textXPaths.get(path);
		return (freq!=null) ? freq : linkCollections.stream()
				.filter(lc -> lc.getXPath().equals(path))
				.map(LinkCollection::size)
				.findFirst().orElse(0);
	}
	
	public boolean containsXPath(XPath path) {
		return textXPaths.containsKey(path) || 
				linkCollections.stream().anyMatch(lc -> lc.getXPath().equals(path));
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
