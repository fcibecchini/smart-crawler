package it.uniroma3.crawler.messages;

import it.uniroma3.crawler.model.CrawlURL;

public class OldCrawlURL {
	private final CrawlURL curl;

	public OldCrawlURL(CrawlURL curl) {
		this.curl = curl;
	}
	
	public CrawlURL getURL() {
		return curl;
	}
	
}
