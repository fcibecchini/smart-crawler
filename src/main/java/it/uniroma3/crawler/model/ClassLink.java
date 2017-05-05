package it.uniroma3.crawler.model;

public class ClassLink {
		
	public final static int SINGLETON = 1;
	public final static int LIST = 2;
	public final static int MENU = 3;
	
	private String xpath;
	private int type;
	private PageClass destination;

	public ClassLink(String xpath, PageClass destination) {
		this.xpath = xpath;
		this.destination = destination;
	}
	
	public ClassLink(String xpath, int type, PageClass destination) {
		this(xpath, destination);
		this.type = type;
	}
	
	public String getXPath() {
		return xpath;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return this.type;
	}

	public PageClass getDestination() {
		return destination;
	}
	
	public String toString() {
		return "["+xpath.toString()+", "+destination.getName()+"]";
	}
	
}
