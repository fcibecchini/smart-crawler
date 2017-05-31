package it.uniroma3.crawler.modeler.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import static it.uniroma3.crawler.util.HtmlUtils.getAbsoluteURL;
import static it.uniroma3.crawler.util.XPathUtils.getXPathTo;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;

/**
 * A Page is a web page represented as a subset of the XPaths-to-link in the corresponding 
 * DOM tree representation, along with the referenced URLs themselves.
 *
 */
public class Page {
	private String url;
	private Map<String, List<String>> xpathToURLs;
	
	/**
	 * Constructs a new Page identified by the given URL and 
	 * with a page schema inferred from the List of outgoing HtmlAnchors.<br>
	 * @param url the URL of the page
	 * @param anchors the outgoing {@link HtmlAnchor}s
	 */
	public Page(String url, List<HtmlAnchor> anchors) {
		this.url = url;
		this.xpathToURLs = pageSchema(anchors);
	}
	
	/**
	 * Returns this web Page URL
	 * @return the URL
	 */
	public String getUrl() {
		return this.url;
	}
	
	/**
	 * INTERNAL API: group outgoing URLs by XPaths-to-link 
	 * to build the page schema.
	 * @param anchors
	 */
	private Map<String, List<String>> pageSchema(List<HtmlAnchor> anchors) {
		return anchors.stream()
			.collect(groupingBy(a -> getXPathTo(a),
				mapping(a->getAbsoluteURL(url, a.getHrefAttribute()), toList())));
	}
	
	/**
	 * Returns the <i>page schema</i> of this Page.<br>
	 * A page schema is an abstraction of a page consisting of a set 
	 * of DOM XPaths-to-link.<br> A page schema is computed simply from the 
	 * page's DOM tree by considering  only the set of paths starting 
	 * from the root (or from a tag with an ID value) and ending in link tags.
	 * @return the page schema
	 */
	public Set<String> getSchema() {
		return xpathToURLs.keySet();
	}
	
	/**
	 * Returns the List of URLs referenced by the given XPath.
	 * @param xpath
	 * @return the URLs
	 */
	public List<String> getURLsByXPath(String xpath) {
		List<String> urls = xpathToURLs.get(xpath);
		return (urls!=null) ? urls : new ArrayList<>();
	}
	
	/**
	 * Returns the index of the URL referenced by the given XPath.
	 * @param xpath
	 * @param url
	 * @return the index of the URL
	 */
	public int getUrlIndex(String xpath, String url) {
		return getURLsByXPath(xpath).indexOf(url);
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
	 * and the specified {@link CandidatePageClass} schema.
	 * @param candidate
	 * @return the cardinality of the difference between the two schemas
	 */
	public long schemaDifferenceSize(CandidatePageClass candidate) {
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
