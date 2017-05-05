package it.uniroma3.crawler;

import java.io.File;
import java.net.URI;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import it.uniroma3.crawler.actors.CrawlRepository;
import it.uniroma3.crawler.actors.frontier.*;
import static it.uniroma3.crawler.factories.CrawlURLFactory.getCrawlUrl;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.OutgoingLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.DynamicModeler;
import it.uniroma3.crawler.modeler.StaticModeler;
import it.uniroma3.crawler.modeler.WebsiteModeler;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlController {
	private static final String ROOT_HTML_DIRECTORY = "html";
	private static CrawlController instance = null;
    private WebsiteModeler modeler;
    
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
    
    public WebsiteModeler getModeler() {
    	return this.modeler;
    }
    
    public void setModeler(WebsiteModeler modeler) {
    	this.modeler = modeler;
    }
    
    public void setPageClassesWaitTime(long wait) {
    	modeler.getClasses().forEach(pc -> pc.setWaitTime(wait));
    }
    
    public boolean makeDirectories(WebsiteModeler mod) {
    	String website = mod.getUrlBase().toString();
    	String writeDir = FileUtils.getWriteDir(ROOT_HTML_DIRECTORY, website);
    	return new File(writeDir).mkdirs() && new File(writeDir+"_mirror").mkdir();
    }
    
    private WebsiteModeler chooseModeler(CrawlerSettings settings, long waitTime) {
    	String config = settings.modelfile;
    	WebsiteModeler modeler = null;
    	
    	if (!config.equals("null")) {
    		modeler = new StaticModeler(config);
    	} 
    	else {
    		String seed = settings.modelseed;
    		int maxPages = new Integer(settings.modelpages);
    		boolean js = new Boolean(settings.javascript);
    		try {
    			modeler = new DynamicModeler(URI.create(seed), maxPages, waitTime, js);
    		} catch (Exception e) {
    			System.err.println("modelseed is not a valid URI");
    		}
    	}
    	return modeler;
    }
    
    public OutgoingLink seed(WebsiteModeler modeler) {
    	String website = modeler.getUrlBase().toString();
    	OutgoingLink entry = new OutgoingLink(website);
    	String file = FileUtils.getMirror(ROOT_HTML_DIRECTORY, website)+"/index.html";
    	if (new File(file).exists()) entry.setCachedFile(file);
    	return entry;
    }
    
    private CrawlURL makeEntryPoint(long waitTime) {
    	PageClass entryClass = modeler.computeModel();
    	setPageClassesWaitTime(waitTime);
    	return getCrawlUrl(seed(modeler), entryClass);
    }
    
    public void startCrawling(ActorSystem system, CrawlerSettings settings) {
    	long defaultWaitTime = new Long(settings.wait);
    	
    	WebsiteModeler modeler = chooseModeler(settings, defaultWaitTime);
    	if (modeler==null) system.terminate();
    	else {
	    	setModeler(modeler);
	    	CrawlURL entryPoint = makeEntryPoint(defaultWaitTime);
	    	makeDirectories(modeler);
	    	startSystem(system, entryPoint, settings);
    	}
    }
    
    private void startSystem(ActorSystem system, CrawlURL entryPoint, CrawlerSettings s) {
    	
    	/* Init. System Actors*/
    	boolean js = new Boolean(s.javascript);
    	system.actorOf(CrawlRepository.props("repository.csv", js), "repository");
    	
    	ActorRef frontier = system.actorOf(BFSFrontier.props(s), "frontier");

    	final Inbox inbox = Inbox.create(system);
    	inbox.send(frontier, entryPoint);
    	inbox.send(frontier, "start");
    }
    
}
