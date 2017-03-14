package it.uniroma3.crawler;

import java.net.URI;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import it.uniroma3.crawler.target.CrawlTarget;
import it.uniroma3.crawler.actors.fetch.CrawlFetcher;
import it.uniroma3.crawler.actors.frontier.*;
import it.uniroma3.crawler.actors.schedule.CrawlLinkScheduler;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlController {
    private static CrawlController instance = null;
    private CrawlTarget target;
    private ActorRef frontier, fetcher, scheduler;
    private ActorSystem system;
    private long waitTime;
    private int rndTime;
    private int maxPages;

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
    
    public ActorRef getFrontier() {
    	return this.frontier;
    }
    
    public ActorRef getScheduler() {
    	return this.scheduler;
    }
    
    public int getMaxPages() {
    	return this.maxPages;
    }
    
    public String getUrlBase() {
    	return this.target.getUrlBase().toString();
    }
    
    public void setTarget(String config, long waitTime, int rndTime, int maxPages) {
    	this.waitTime = waitTime;
    	this.rndTime = rndTime;
    	this.maxPages = maxPages;
    	this.target = new CrawlTarget(config, waitTime, rndTime);
    	this.target.initCrawlingTarget();
    }
    
    public void startCrawling() {
    	PageClass entry = target.getEntryPageClass();
    	URI base = target.getUrlBase();
    	CrawlURL entryPoint = CrawlURLFactory.getCrawlUrl(base.toString(), entry);
    	startSystem(entryPoint);
    }
    
    public void startSystem(CrawlURL entryPoint) {
    	system = ActorSystem.create("CrawlSystem");
    	frontier = system.actorOf(BreadthFirstUrlFrontier.props(maxPages), "frontier");
    	scheduler = system.actorOf(Props.create(CrawlLinkScheduler.class), "linkScheduler");
    	fetcher = system.actorOf(Props.create(CrawlFetcher.class), "fetcher");
    	
    	final Inbox inbox = Inbox.create(system);
    	inbox.send(frontier, entryPoint);
    	inbox.send(fetcher, "Start");
    }
    
    public void stopSystem() {
    	final Inbox inbox = Inbox.create(system);
    	inbox.send(fetcher, "Stop");
    	inbox.send(scheduler, "Stop");
    	inbox.send(frontier, "Stop");
    	system.terminate();
    }
    
}
