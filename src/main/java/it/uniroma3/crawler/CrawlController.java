package it.uniroma3.crawler;

import java.net.URI;
import java.util.Queue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import it.uniroma3.crawler.target.CrawlTarget;
import it.uniroma3.crawler.actors.fetch.CrawlFetcher;
import it.uniroma3.crawler.actors.frontier.FIFOUrlFrontier;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlController {
    private static CrawlController instance = null;
    private CrawlTarget target;
    private ActorRef frontier;
    private long waitTime;
    private int rndTime;

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
    
    public long getWaitTime() {
    	return this.waitTime;
    }
    
    public int getRoundTime() {
    	return this.rndTime;
    }
    
    public void setTarget(String config, long waitTime, int roundTime) {
    	this.waitTime = waitTime;
    	this.rndTime = roundTime;
    	this.target = new CrawlTarget(config, waitTime, roundTime);
    }
    
    public void startCrawling() {
    	PageClass entry = target.getEntryPageClass();
    	URI base = target.getUrlBase();
    	CrawlURL entryPoint = CrawlURLFactory.getCrawlUrl(base.toString(), entry);
    	
    	final ActorSystem system = ActorSystem.create("CrawlSystem");
    	frontier = system.actorOf(Props.create(FIFOUrlFrontier.class));
    	final ActorRef fetcher = system.actorOf(Props.create(CrawlFetcher.class));
    	
    	final Inbox inbox = Inbox.create(system);
    	inbox.send(frontier, entryPoint);
    	inbox.send(fetcher, "start crawling");
    	
    }
    
    public ActorRef getFrontier() {
    	return this.frontier;
    }
    
    public String getUrlBase() {
    	return this.target.getUrlBase().toString();
    }
    
    @Deprecated
    public void fetchRequests(String urlBase, 
    		it.uniroma3.crawler.page.PageClass homePage, Queue<it.uniroma3.crawler.page.PageClass> classes) {
    	// ActorSystem is a heavy object: create only one per application
    	final ActorSystem system = ActorSystem.create("CrawlSystem");
    	final ActorRef entryFetcher = system.actorOf(it.uniroma3.crawler.fetch.EntryFetcher.props(urlBase, classes), "entryFetcher");
    	
    	final Inbox inbox = Inbox.create(system);
    	inbox.send(entryFetcher, homePage);
    	
    }

}
