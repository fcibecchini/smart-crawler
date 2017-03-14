package it.uniroma3.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import it.uniroma3.crawler.target.CrawlTarget;
import it.uniroma3.crawler.actors.extract.CrawlExtractor;
import it.uniroma3.crawler.actors.fetch.CrawlFetcher;
import it.uniroma3.crawler.actors.frontier.*;
import it.uniroma3.crawler.actors.schedule.CrawlLinkScheduler;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlController {
	private static CrawlController instance = null;
    private CrawlTarget target;
    private ActorRef frontier, scheduler;
    private String config;
    private long waitTime;
    private int rndTime, maxPages, numberOfFetchers;

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
    
	public void setConfig(String config) {
		this.config = config;
	}

	public void setRoundTime(int rndTime) {
		this.rndTime = rndTime;
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	public void setMaxPages(int maxPages) {
		this.maxPages = maxPages;
	}
	
	public void setNumberOfFetchers(int n) {
		this.numberOfFetchers = n;
	}
    
    public ActorRef getFrontier() {
    	return this.frontier;
    }
    
    public ActorRef getScheduler() {
    	return this.scheduler;
    }
    
    public String getUrlBase() {
    	return this.target.getUrlBase().toString();
    }
    
    private void loadProperties() {
    	Properties prop = new Properties();
    	InputStream input = null;
    	try {
    		input = new FileInputStream("config.properties");
    		prop.load(input);
        	setConfig(prop.getProperty("config"));
        	setNumberOfFetchers(new Integer(prop.getProperty("fetchers")));
        	setWaitTime(new Long(prop.getProperty("wait")));
        	setRoundTime(new Integer(prop.getProperty("randompause")));
        	setMaxPages(new Integer(prop.getProperty("pages")));
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	} finally {
    		if (input != null) {
    			try {
    				input.close();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}
    	}
    }
    
    private void setTarget(String config, long waitTime, int rndTime, int maxPages) {
    	this.target = new CrawlTarget(config, waitTime, rndTime);
    	this.target.initCrawlingTarget();
    }
    
    public void startCrawling(ActorSystem system) {
    	loadProperties();
    	setTarget(config, waitTime, rndTime, maxPages);
    	PageClass entry = target.getEntryPageClass();
    	URI base = target.getUrlBase();
    	CrawlURL entryPoint = CrawlURLFactory.getCrawlUrl(base.toString(), entry);
    	startSystem(system, entryPoint);
    }
    
    private void startSystem(ActorSystem system, CrawlURL entryPoint) {
    	/* Init. System Actors*/
    	
    	frontier = system.actorOf(BreadthFirstUrlFrontier.props(maxPages), "frontier");
    	scheduler = system.actorOf(Props.create(CrawlLinkScheduler.class), "linkScheduler");
    	
    	List<ActorRef> extractors = new ArrayList<>();
    	for (PageClass pClass : target.getClasses()) {
    		extractors.add(system.actorOf(Props.create(CrawlExtractor.class), pClass.getName()));
    	}
    	
    	List<ActorRef> fetchers = new ArrayList<>();
    	for (int i=0; i<numberOfFetchers; i++) {
    		fetchers.add(system.actorOf(CrawlFetcher.props(extractors), "fetcher"+(i+1)));
    	}

    	final Inbox inbox = Inbox.create(system);
    	inbox.send(frontier, entryPoint);
    	for (ActorRef fetcher : fetchers) {
    		inbox.send(fetcher, "Start");
    	}
    }
    
}
