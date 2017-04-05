package it.uniroma3.crawler.model;

import java.io.Serializable;

public class ClassLink implements Serializable {
	
	private static final long serialVersionUID = -8151270465503302385L;
	
	private String xpath;
	private PageClass destination;

	public ClassLink(String xpath, PageClass destination) {
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
		return "["+xpath+", "+destination.getName()+"]";
	}
	
}
