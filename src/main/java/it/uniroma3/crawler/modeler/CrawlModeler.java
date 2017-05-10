package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.OutgoingLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlModeler extends AbstractLoggingActor {
	private String HTML;
	private String site;
	private ActorRef modeler, sender;

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(ModelMsg.class, this::computeModel)
		.match(PageClass.class, this::entryPoint)
		.matchEquals(STOP, msg -> {
			context().stop(modeler);
			context().stop(self());
		})
		.build();
	}
	
	public void computeModel(ModelMsg msg) {
		sender = sender();
		HTML = msg.getRootDirectory();
		site = msg.getSite();
		SeedConfig sc = msg.getSeedConfig();
		
		modeler = chooseModeler(site,sc);
		context().watch(modeler);
		modeler.tell(START, self());
	}
	
	public void entryPoint(PageClass entryClass) {
		CrawlURL entry = CrawlURLFactory.getCrawlUrl(seedLink(), entryClass);
		sender.tell(entry, self());
	}
    
    private ActorRef chooseModeler(String site, SeedConfig sc) {
    	Website website = new Website(site, sc.maxfailures, sc.randompause, sc.javascript);
    	if (!sc.file.equals("null"))
    		return context().actorOf(StaticModeler.props(website, sc.wait, sc.file), "Static");
    	else 
    		return context().actorOf(DynamicModeler.props(website, sc.wait, sc.pages), "Dynamic");
    }
    
    private OutgoingLink seedLink() {
    	OutgoingLink entry = new OutgoingLink(site);
    	String file = FileUtils.getMirror(HTML, site)+"/index.html";
    	if (Files.exists(Paths.get(file))) entry.setCachedFile(file);
    	return entry;
    }
    
}
