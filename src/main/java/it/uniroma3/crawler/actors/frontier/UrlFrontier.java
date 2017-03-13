package it.uniroma3.crawler.actors.frontier;

import it.uniroma3.crawler.model.CrawlURL;

public interface UrlFrontier {
	
	public CrawlURL next();
	
	public void scheduleUrl(CrawlURL crawUrl);
	
	public boolean isEmpty();
	
}
