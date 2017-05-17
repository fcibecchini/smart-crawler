package it.uniroma3.crawler.messages;

import it.uniroma3.crawler.model.CrawlURL;

public class OldURLMsg {
	private final CrawlURL curl;

	public OldURLMsg(CrawlURL curl) {
		this.curl = curl;
	}
	
	public CrawlURL getURL() {
		return curl;
	}
	
}
