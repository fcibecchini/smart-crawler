package it.uniroma3.crawler.factories;

import java.net.URI;
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
		return factory.create(url, pClass);
	}
	
	public static CrawlURL getCrawlUrl(URI url, PageClass pClass) {
		return factory.create(url, pClass);
	}
	
	public static CrawlURL copy(CrawlURL curl) {
		return factory.create(curl.getUrl(), curl.getPageClass());
	}
	
	private CrawlURL create(String url, PageClass pClass) {
		try {
			CrawlURL crawlUrl = new CrawlURL(url, pClass);
	        if (logger.isLoggable(Level.FINE)) {
	            logger.fine("URL "+url+" INITIALIZED");
	        }
			return crawlUrl;
		} catch (URISyntaxException | IllegalArgumentException e) {
	        if (logger.isLoggable(Level.WARNING)) {
	            logger.warning("URL "+url+" COULD NOT BE INITIALIZED");
	        }
			return null;
		}
	}
	
	private CrawlURL create(URI url, PageClass pClass) {
		CrawlURL crawlUrl = new CrawlURL(url, pClass);
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("URL " + url + " INITIALIZED");
		}
		return crawlUrl;
	}
}
