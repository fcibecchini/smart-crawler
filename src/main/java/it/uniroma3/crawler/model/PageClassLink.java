package it.uniroma3.crawler.model;

public class PageClassLink {
	private String xpath;
	private PageClass destination;

	public PageClassLink(String xpath, PageClass destination) {
		this.xpath = xpath;
		this.destination = destination;
	}
	
	public String getXPath() {
		return xpath;
	}

	public PageClass getDestination() {
		return destination;
	}
	
	public String toString() {
		return "["/*+xpath+*/+", "+destination.getName()+"]";
	}
	
}
