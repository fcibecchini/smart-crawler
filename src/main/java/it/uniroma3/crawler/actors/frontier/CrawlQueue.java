package it.uniroma3.crawler.actors.frontier;

import it.uniroma3.crawler.model.CrawlURL;

import org.mapdb.DB;

public class CrawlQueue implements UrlFrontier {
	private DB db;

	@Override
	public CrawlURL next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void scheduleUrl(CrawlURL crawUrl) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

}
