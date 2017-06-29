package it.uniroma3.crawler.modeler.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import static it.uniroma3.crawler.util.HtmlUtils.isValidURL;

/**
 * A LinkCollection is an immutable group of links with uniform layout and 
 * presentation properties.<br> Pages reached from the same 
 * collection are assumed to form a uniform class of pages.<br>
 * Note that there could be situations where this assumption is violated.<br>
 * A link collection may be singleton.
 */
public class LinkCollection {
	private Page page;
	private XPath xpath;
	private List<String> links;
	private short type;
	private boolean refinable, finer;
	private int maxFetches;
	
	/**
	 * Constructs a new LinkCollection containing the 
	 * outgoing links of the specified page related to the given XPath 
	 * @param parent the parent {@link Page} which contains the outgoing links
	 * @param xpath the {@link XPath} that references this link collection
	 * @param urls the outgoing links
	 */
	public LinkCollection(Page parent, XPath xpath, List<String> urls) {
		this.page = parent;
		this.xpath = xpath;
		this.links = Collections.unmodifiableList(urls);
		this.maxFetches = 3;
		this.refinable = true;
	}
	
	/**
	 * @return the links group
	 */
	public List<String> getLinks() {
		return links;
	}
	
	public void setLinks(List<String> links) {
		this.links = Collections.unmodifiableList(links);
	}
	
	/**
	 * Returns the Parent Page of this Link Collection.
	 * @return the parent page
	 */
	public Page getPage() {
		return page;
	}
	
	/**
	 * 
	 * @return the XPath instance of this Link Collection
	 */
	public XPath getXPath() {
		return xpath;
	}
	
	/**
	 * Set this LinkCollection xpath
	 * @param xpath
	 */
	public void setXPath(XPath xpath) {
		this.xpath = xpath;
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
	
	public boolean isRefinable() {
		return refinable;
	}

	public void setRefinable(boolean ref) {
		this.refinable = ref;
	}

	public boolean isFiner() {
		return finer;
	}

	public void setFiner(boolean finer) {
		this.finer = finer;
	}
	
	public int getMaxFetches() {
		return maxFetches;
	}
	
	public void setMaxFetches(int n) {
		this.maxFetches = n;
	}
	
	/**
	 * Returns a subset of the links of this collection consisting of valid links that
	 * must be fetched.
	 * @param base the base URL to retrieve valid internal URLs
	 * @return the subset links group to fetch
	 */
	public Queue<String> getLinksToFetch(String base) {
		Queue<String> linksToFetch = new LinkedList<>();
		int size = size();
		if (size<=maxFetches)
			links.stream().filter(l->isValidURL(base,l)).forEach(linksToFetch::add);
		else {
			int start=0, middle=(size-1)/2, end=size-1;
			int i = start;
			while (i<size) {
				String link = links.get(i);
				if (isValidURL(base, link)) {
					linksToFetch.add(link);
					break;
				}
				i++;
			}
			i = (i<middle) ? middle : i+1;
			while (i<size) {
				String link = links.get(i);
				if (isValidURL(base, link)) {
					linksToFetch.add(link);
					break;
				}
				i++;
			}
			int lastIndex = i;
			i = (i<end) ? end : 0;
			while (i>lastIndex) {
				String link = links.get(i);
				if (isValidURL(base, link)) {
					linksToFetch.add(link);
					break;
				}
				i--;
			}
		}
		return linksToFetch;
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
		ModelPageClass c1 = model.getClassOfPage(page);
		ModelPageClass c2 = model.getClassOfPage(o.getPage());
		
		if (c1.size()==1 && c2.size()>1) return -1;
		if (c1.size()>1 && c2.size()==1) return 1;
		
		double ratio1 = (double) size() / (double) c1.outgoingURLs();
		double ratio2 = (double) o.size() / (double) c2.outgoingURLs();
		
		if (ratio1 > ratio2) return -1;
		if (ratio1 < ratio2) return 1;
		return 0;
	}
	
	public String toString() {
		return page.getUrl()+" "+xpath.get()+" -> "+ links.toString();
	}
	
	public int hashCode() {
		return Objects.hash(page, xpath, links);
	}

	public boolean equals(Object obj) {
		LinkCollection other = (LinkCollection) obj;
		return Objects.equals(page, other.getPage())
			&& Objects.equals(xpath, other.getXPath())
			&& Objects.equals(links, other.getLinks());
	}

}
