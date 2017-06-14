package it.uniroma3.crawler.modeler.model;

import java.util.ArrayList;
import java.util.List;

import it.uniroma3.crawler.model.PageClass;

/**
 * A PageLink is a link from one {@link Page} instance to an other (possible multiple) Page 
 * via an XPath version.<br> Links can point to Lists, Menus or a Singleton.
 *
 */
public abstract class PageLink {
	private String xpath;
	private List<Page> destinations;
	
	public PageLink(String xpath, List<Page> destinations) {
		this.xpath = xpath;
		this.destinations = new ArrayList<>(destinations);
	}

	public String getXpath() {
		return xpath;
	}
	
	public List<Page> getDestinations() {
		return destinations;
	}
	
	/**
	 * Links a source PageClass to a list of destination PageClass, according to
	 * the nature of this PageLink and the nature of current PageClass links.
	 * @param src the source PageClass
	 * @param dests the List of destination PageClasses. Can be a singleton.
	 */
	public abstract void linkToPageClass(PageClass src, List<PageClass> dests);

}
