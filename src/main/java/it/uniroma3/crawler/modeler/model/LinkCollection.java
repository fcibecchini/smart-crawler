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
	private XPath xpath;
	private List<String> links;
	private short type;
	private boolean isFinest, isCoarsest;
	
	/**
	 * Constructs a new LinkCollection with the given group of link.
	 * @param links the outgoing links
	 */
	public LinkCollection(List<String> links) {
		this.links = Collections.unmodifiableList(links);
	}
	
	/**
	 * Constructs a new LinkCollection containing the 
	 * outgoing links of the specified page related to the given XPath 
	 * @param parent the parent {@link Page} which contains the outgoing links
	 * @param xpath the {@link XPath} that references this link collection
	 * @param urls the outgoing links
	 */
	public LinkCollection(Page parent, XPath xpath, List<String> urls) {
		this(urls);
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
	 * @return the current XPath version referencing this collection
	 */
	public String getCurrentXPath() {
		return xpath.get();
	}
	
	/**
	 * 
	 * @return the XPath instance of this Link Collection
	 */
	public XPath getXPath() {
		return xpath;
	}
	
	/**
	 * 
	 * @return the number of links in this collection
	 */
	public int size() {
		return links.size();
	}
	
	public void setList() {
		type=1;
	}
	
	public void setMenu() {
		type=2;
	}
	
	public void setSingleton() {
		type=3;
	}
	
	public boolean isList() {
		return type==1;
	}
	
	public boolean isMenu() {
		return type==2;
	}
	
	public boolean isSingleton() {
		return type==3;
	}
	
	public boolean isFinest() {
		return isFinest;
	}

	public void setFinest() {
		this.isFinest = true;
	}

	public boolean isCoarsest() {
		return isCoarsest;
	}

	public void setCoarsest() {
		this.isCoarsest = true;
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
	 * Note that link collections in singleton {@link ModelPageClass} 
	 * are always less than the others (they are assigned top priority).
	 * @param o the LinkCollection to compare
	 * @param model the {@link WebsiteModel} of clustered pages to retrieve the current
	 * page class of the page
	 */
	public int densestFirst(LinkCollection o, WebsiteModel model) {
		ModelPageClass c1 = model.getClassOfPage(parent);
		ModelPageClass c2 = model.getClassOfPage(o.getParent());
		
		if (c1.size()==1 && c2.size()>1) return -1;
		if (c1.size()>1 && c2.size()==1) return 1;
		
		double ratio1 = (double) size() / (double) c1.outgoingURLs();
		double ratio2 = (double) o.size() / (double) c2.outgoingURLs();
		
		if (ratio1 > ratio2) return -1;
		if (ratio1 < ratio2) return 1;
		return 0;
	}
	
	public String toString() {
		String desc = (parent!=null) ? parent.getUrl() : "entryPoint";
		return desc+" "+getCurrentXPath()+" -> "+ links.toString();
	}
	
	public int hashCode() {
		return Objects.hash(getCurrentXPath(), links);
	}

	public boolean equals(Object obj) {
		LinkCollection other = (LinkCollection) obj;
		return Objects.equals(getCurrentXPath(), other.getCurrentXPath())
			&& Objects.equals(links, other.getLinks());
	}

}
