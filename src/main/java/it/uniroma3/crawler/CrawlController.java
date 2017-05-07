package it.uniroma3.crawler;

import static it.uniroma3.crawler.factories.CrawlURLFactory.getCrawlUrl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import it.uniroma3.crawler.actors.CrawlRepository;
import it.uniroma3.crawler.actors.frontier.BFSFrontier;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.OutgoingLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.modeler.DynamicModeler;
import it.uniroma3.crawler.modeler.StaticModeler;
import it.uniroma3.crawler.modeler.WebsiteModeler;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlController {
	private static final String HTML = "html";
	private static CrawlController instance = null;
    
    private CrawlController() {}

    public static synchronized CrawlController getInstance() {
        if (instance==null) instance = new CrawlController();
        return instance;
    }
    
    public void startCrawling(ActorSystem system) {
    	CrawlerSettings s = Settings.SettingsProvider.get(system);
    	
    	List<CrawlURL> entryPoints = new ArrayList<>();
    	
    	for (String site : s.seeds.keySet()) {
    		SeedConfig seedConfig = s.seeds.get(site);
	    	WebsiteModeler modeler = chooseModeler(site, seedConfig);
	    	entryPoints.add(makeEntryPoint(site, modeler));
		    makeDirectories(site);
    	}
    	
    	startSystem(system, entryPoints, s.fetchers, s.pages, s.frontierheap);
    }
    
    private boolean makeDirectories(String site) {
    	String writeDir = FileUtils.getWriteDir(HTML, site);
    	return new File(writeDir).mkdirs() && new File(writeDir+"_mirror").mkdir();
    }
    
    private WebsiteModeler chooseModeler(String site, SeedConfig sc) {
    	Website website = new Website(site, sc.maxfailures, sc.randompause, sc.javascript);
    	if (!sc.file.equals("null"))
    		return new StaticModeler(website, sc.wait, sc.file);
    	else 
    		return new DynamicModeler(website, sc.wait, sc.pages);
    }
    
    private OutgoingLink seedLink(String site) {
    	OutgoingLink entry = new OutgoingLink(site);
    	String file = FileUtils.getMirror(HTML, site)+"/index.html";
    	if (new File(file).exists()) entry.setCachedFile(file);
    	return entry;
    }
    
    private CrawlURL makeEntryPoint(String site, WebsiteModeler modeler) {
    	PageClass entryClass = modeler.compute();
    	return getCrawlUrl(seedLink(site), entryClass);
    }
    
    private void startSystem(ActorSystem system, 
    		List<CrawlURL> entries, int n, int pages, int heap) {
    	/* Init. System Actors*/
    	system.actorOf(CrawlRepository.props("repository.csv"), "repository");
    	ActorRef frontier = system.actorOf(BFSFrontier.props(n, pages, heap), "frontier");

    	final Inbox inbox = Inbox.create(system);
    	inbox.send(frontier, entries.get(0)); // for now use just 1 site
    	inbox.send(frontier, "start");
    }
    
}
