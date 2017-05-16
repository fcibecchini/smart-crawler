package it.uniroma3.crawler.messages;

import it.uniroma3.crawler.model.CrawlURL;

public class InitCrawling {
	private final CrawlURL curl;

	public InitCrawling(CrawlURL curl) {
		this.curl = curl;
	}
	
	public CrawlURL getURL() {
		return curl;
	}

}
