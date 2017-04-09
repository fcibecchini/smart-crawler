package it.uniroma3.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import it.uniroma3.crawler.actors.extract.CrawlExtractor;
import it.uniroma3.crawler.actors.fetch.CrawlFetcher;
import it.uniroma3.crawler.actors.frontier.*;
import it.uniroma3.crawler.actors.schedule.CrawlLinkScheduler;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.DynamicModeler;
import it.uniroma3.crawler.modeler.StaticModeler;
import it.uniroma3.crawler.modeler.WebsiteModeler;

public class CrawlController {
	private static CrawlController instance = null;
    private WebsiteModeler modeler;
    private ActorRef frontier, scheduler;
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

	public void setRoundTime(int rndTime) {
		this.rndTime = rndTime;
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}
    
    public ActorRef getFrontier() {
    	return this.frontier;
    }
    
    public ActorRef getScheduler() {
    	return this.scheduler;
    }
    
    public String getUrlBase() {
    	return this.modeler.getUrlBase().toString();
    }
    
    public String getBaseDirectory() {
    	String baseUrlString = this.modeler.getUrlBase().toString();
		return baseUrlString.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.", "_");
    }
    
    private Properties getProperties(String fileName) {
    	try (InputStream stream = Files.newInputStream(Paths.get(fileName))) {
        	Properties config = new Properties();
            config.load(stream);
            return config;
        } catch (IOException ie) {
        	return null;
        }
    }
    
    private void setModeler(WebsiteModeler modeler) {
    	this.modeler = modeler;
    }
    
    private WebsiteModeler chooseModeler(Properties prop) {
    	String config = prop.getProperty("modelfile");
    	WebsiteModeler modeler = null;
    	
    	if (!config.equals("null")) {
    		modeler = new StaticModeler(config);
    	} 
    	else {
    		String seed = prop.getProperty("modelseed");
    		int maxPages = new Integer(prop.getProperty("modelpages"));
    		boolean js = new Boolean(prop.getProperty("javascript"));
    		try {
    			modeler = new DynamicModeler(URI.create(seed), maxPages, waitTime, js);
    		} catch (Exception e) {
    			System.err.println("modelseed is not a valid URI");
    		}
    	}
    	return modeler;
    }
    
    public void startCrawling(ActorSystem system) {
    	Properties prop = getProperties("config.properties");
    	this.waitTime = new Long(prop.getProperty("wait"));
    	this.rndTime = new Integer(prop.getProperty("randompause"));
    	
    	WebsiteModeler modeler = chooseModeler(prop);
    	if (modeler==null) system.terminate();
    	else {
	    	setModeler(modeler);
	    	PageClass entryClass = modeler.computeModel();
	    	entryClass.setWaitTime(waitTime);
	    	URI base = modeler.getUrlBase();
	    	CrawlURL entryPoint = CrawlURLFactory.getCrawlUrl(base.toString(), entryClass);
	    	startSystem(system, entryPoint, 
	    			new Integer(prop.getProperty("fetchers")), 
	    			new Integer(prop.getProperty("pages")), 
	    			new Integer(prop.getProperty("maxfailures")), 
	    			new Integer(prop.getProperty("failuretime")),
	    			new Boolean(prop.getProperty("javascript")));
    	}
    }
    
    private void startSystem(
    		ActorSystem system, 
    		CrawlURL entryPoint, 
    		int n, 
    		int pages, 
    		int maxFails,
    		int time,
    		boolean useJavascript) {
    	
    	/* Init. System Actors*/
    	
    	frontier = system.actorOf(BFSFrontier.props(pages), "frontier");
    	scheduler = system.actorOf(Props.create(CrawlLinkScheduler.class), "linkScheduler");
    	
    	List<ActorRef> extractors = new ArrayList<>();
    	for (PageClass pClass : modeler.getClasses()) {
    		extractors.add(system.actorOf(Props.create(CrawlExtractor.class), pClass.getName()));
    	}
    	
    	List<ActorRef> fetchers = new ArrayList<>();
    	for (int i=0; i<n; i++) {
    		fetchers.add(system.actorOf(CrawlFetcher.props(extractors,maxFails,time,useJavascript), "fetcher"+(i+1)));
    	}

    	final Inbox inbox = Inbox.create(system);
    	inbox.send(frontier, entryPoint);
    	for (ActorRef fetcher : fetchers) {
    		inbox.send(fetcher, "Start");
    	}
    }
    
}
