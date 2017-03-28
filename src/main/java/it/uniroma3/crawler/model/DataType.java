package it.uniroma3.crawler.model;

import java.io.Serializable;

public abstract class DataType implements Serializable {
	
	private static final long serialVersionUID = -1685843286195454597L;
	
	private String xpath;
	
	public String getXPath() {
		return this.xpath;
	}

	public void setXPath(String xpath) {
		this.xpath = xpath;
	}
	
    /* Extract the expected value with this DataType XPath
     * An HtmlPage should be expected as the input parameter 
     * */
	public abstract String extract(Object object);
	
}
