package it.uniroma3.crawler.model;

import org.neo4j.ogm.annotation.NodeEntity;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

@NodeEntity
public abstract class DataType {
	private Long id;
	private String name;
	
	public DataType() {}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Extracts the expected value type with the specified XPath
	 * @param page the {@link HtmlPage} 
	 * @param xpath
	 * @return the String record
	 */
	public abstract String extract(HtmlPage page, String xpath);
	
}
