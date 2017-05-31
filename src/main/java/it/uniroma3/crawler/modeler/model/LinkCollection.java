package it.uniroma3.crawler.modeler.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A LinkCollection is an immutable group of links with uniform layout and 
 * presentation properties.<br> Pages reached from the same 
 * collection are assumed to form a uniform class of pages.<br>
 * Note that there could be situations where this assumption is violated.<br>
 * A link collection may be singleton.
 */
public class LinkCollection {
	private Page parent;
	private String xpath;
	private List<String> links;
	
	/**
	 * Constructs a new LinkCollection with the given group of link.
	 * @param seed the web site homepage link
	 */
	public LinkCollection(List<String> links) {
		this.links = Collections.unmodifiableList(links);
	}
	
	/**
	 * Constructs a new LinkCollection containing the 
	 * outgoing links of the specified page related to the given XPath 
	 * @param parent the parent {@link Page} which contains the outgoing links
	 * @param xpath the XPath that references this link collection
	 */
	public LinkCollection(Page parent, String xpath) {
		this(parent.getURLsByXPath(xpath));
		this.xpath = xpath;
		this.parent = parent;
	}
	
	/**
	 * @return the links group
	 */
	public List<String> getLinks() {
		return links;
	}
	
	/**
	 * Returns the Parent Page of this Link Collection.
	 * @return the parent page
	 */
	public Page getParent() {
		return parent;
	}
	
	/**
	 * 
	 * @return the XPath referencing this collection
	 */
	public String getXPath() {
		return xpath;
	}
	
	/**
	 * 
	 * @return the number of links in this collection
	 */
	public int size() {
		return links.size();
	}
	
	/**
	 * Compares this LinkCollection with the specified LinkCollection for order 
	 * with a <i>densest-first</i> strategy. 
	 * Returns a negative integer, zero, or a positive integer as this 
	 * LinkCollection is less than, equal to, or greater than the 
	 * specified object, according to this strategy<br><br>
	 * A LinkCollection is less than another (has higher priority) 
	 * if it has more instances relative to the total 
	 * number of outgoing links for its cluster than the other.<br>
	 * Note that link collections in singleton {@link CandidatePageClass} 
	 * are always less than the others (they are assigned top priority).
	 * @param o the LinkCollection to compare
	 * @param model the {@link WebsiteModel} of clustered pages to retrieve the current
	 * page class of the page
	 */
	public int densestFirst(LinkCollection o, WebsiteModel model) {
		int size1 = model.getClassOfPage(parent).size();
		int size2 = model.getClassOfPage(o.getParent()).size();
		if (size1==1 && size2>1) return -1;
		if (size1>1 && size2==1) return 1;

		double ratio1 = size() / model.getClassOfPage(parent).outgoingURLs();
		double ratio2 = o.size() / model.getClassOfPage(o.getParent()).outgoingURLs();
		
		if (ratio1 > ratio2) return -1;
		if (ratio1 < ratio2) return 1;
		return 0;
	}
	
	public String toString() {
		String desc = (parent!=null) ? parent.getUrl() : "entryPoint";
		return desc+" -> "+ links.toString();
	}
	
	public int hashCode() {
		return Objects.hash(xpath, links);
	}

	public boolean equals(Object obj) {
		LinkCollection other = (LinkCollection) obj;
		return Objects.equals(xpath, other.getXPath())
			&& Objects.equals(links, other.getLinks());
	}

}
