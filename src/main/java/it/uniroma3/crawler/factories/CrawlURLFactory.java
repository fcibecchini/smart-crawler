package it.uniroma3.crawler.factories;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlURLFactory {
	private final static CrawlURLFactory factory = new CrawlURLFactory();
    private static Logger logger =
            Logger.getLogger(CrawlURLFactory.class.getName());
	
	private CrawlURLFactory() {}
	
	public static CrawlURL getCrawlUrl(String url, PageClass pClass) {
		return CrawlURLFactory.factory.create(url, pClass);
	}
	
	private CrawlURL create(String url, PageClass pClass) {
		try {
			CrawlURL crawlUrl = new CrawlURL(url, pClass);
	        if (logger.isLoggable(Level.FINE)) {
	            logger.fine("URL "+url+" INITIALIZED");
	        }
			return crawlUrl;
		} catch (URISyntaxException e) {
	        if (logger.isLoggable(Level.WARNING)) {
	            logger.fine("URL "+url+" COULD NOT BE INITIALIZED");
	        }
			return null;
		}
		
		
	}

}
