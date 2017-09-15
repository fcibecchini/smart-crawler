package it.uniroma3.crawler.messages;

import java.util.Map;

import it.uniroma3.crawler.model.DataType;

public class ExtractDataMsg {
	
	private final String url;
	private final Map<String,DataType> data;
	
	public ExtractDataMsg(String url, Map<String,DataType> data) {
		this.url = url;
		this.data = data;
	}
	
	public Map<String,DataType> getData() {
		return this.data;
	}
	
	public String getUrl() {
		return url;
	}
}
