package it.uniroma3.crawler;

import java.net.URI;
import java.util.Queue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.target.CrawlTarget;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.fetch.EntryFetcher;

public class CrawlController {
    private static CrawlController instance = null;
    private CrawlTarget target;

    private CrawlController() {}

    public static CrawlController getInstance() {
        if (instance == null) {
        	synchronized (CrawlController.class) {
        		if (instance == null)
        			instance = new CrawlController();
        	}
        }
        return instance;
    }
    
    public void setTarget(String config, long waitTime, int roundTime) {
    	this.target = new CrawlTarget(config, waitTime, roundTime);
    }
    
    public void startCrawling() {
    	// TODO
    	PageClass entry = target.getEntryPageClass();
    	URI base = target.getUrlBase();
    	CrawlURL entryPoint = CrawlURLFactory.getCrawlUrl(base.toString(), entry);
    	
    }
    
    @Deprecated
    public void fetchRequests(String urlBase, 
    		it.uniroma3.crawler.page.PageClass homePage, Queue<it.uniroma3.crawler.page.PageClass> classes) {
    	// ActorSystem is a heavy object: create only one per application
    	final ActorSystem system = ActorSystem.create("CrawlSystem");
    	final ActorRef entryFetcher = system.actorOf(EntryFetcher.props(urlBase, classes), "entryFetcher");
    	
    	final Inbox inbox = Inbox.create(system);
    	inbox.send(entryFetcher, homePage);
    	
    }

}
