package it.uniroma3.crawler.model;

public abstract class DataType {
	private String xpath;
	private String content;
	
	public String getXPath() {
		return this.xpath;
	}

	public void setXPath(String xpath) {
		this.xpath = xpath;
	}

	public void setDataContent(String content) {
		this.content = content;
	}
	
	public String getDataContent() {
		return this.content;
	}
	
	public abstract String extract(Object object);
	
}
