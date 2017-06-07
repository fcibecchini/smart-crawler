package it.uniroma3.crawler.modeler.model;

/**
 * A PageLink is a link from one {@link Page} instance to another 
 * via an XPath version.<br> Links can point to Lists, Menu items or singletons.
 *
 */
public class PageLink {
	private String xp;
	private Page dest;
	private int type;
	
	public PageLink(String xp, Page dest, int type) {
		this.xp = xp;
		this.dest = dest;
		this.type = type;
	}

	public String getXpath() {
		return xp;
	}
	
	public int getType() {
		return type;
	}

	public Page getDest() {
		return dest;
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

}
