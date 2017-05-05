package it.uniroma3.crawler.messages;

public class StopMsg  {
	
	private final String url;
		
	public StopMsg(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return this.url;
	}
	
}
