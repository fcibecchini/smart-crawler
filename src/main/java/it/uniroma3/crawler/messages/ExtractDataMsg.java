package it.uniroma3.crawler.messages;

import java.util.Collection;

import it.uniroma3.crawler.model.DataType;

public class ExtractDataMsg {
	
	private final String url;
	private final String htmlPath;
	private final String baseUrl;
	private final Collection<DataType> data;
	
	public ExtractDataMsg(String url, String htmlPath, String baseUrl, Collection<DataType> data) {
		this.url = url;
		this.htmlPath = htmlPath;
		this.baseUrl = baseUrl;
		this.data = data;
	}
	
	public Collection<DataType> getData() {
		return this.data;
	}
	
	public String getHtmlPath() {
		return htmlPath;
	}

	public String getBaseUrl() {
		return baseUrl;
	}
	
	public String getUrl() {
		return url;
	}
}
