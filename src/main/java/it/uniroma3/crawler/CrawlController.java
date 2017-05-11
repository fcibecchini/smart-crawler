package it.uniroma3.crawler;

import static it.uniroma3.crawler.util.Commands.*;

import java.util.concurrent.TimeUnit;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.uniroma3.crawler.actors.CrawlRepository;
import it.uniroma3.crawler.actors.frontier.CrawlFrontier;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.modeler.CrawlModeler;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import scala.concurrent.duration.Duration;

public class CrawlController extends AbstractLoggingActor {
	private final static String HTML = "html";
	
	private boolean started;
	
	public CrawlController() {
		this.started = false;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> startCrawling())
		.matchEquals(STOP, 	msg -> stopSystem())
		.matchEquals(END_SYSTEM, msg -> context().system().terminate())
		.match(CrawlURL.class, curl -> {
	    	sender().tell(STOP, self());
			sendToFrontier(curl);})
		.build();
	}
    
    private void startCrawling() {
    	CrawlerSettings s = Settings.SettingsProvider.get(context().system());
    	
    	ActorRef repository = context().actorOf(CrawlRepository.props("repository.csv"), "repository");
    	ActorRef frontier = context().actorOf(CrawlFrontier.props(s.fetchers, s.pages, s.frontierheap), 
    			"frontier");
    	context().watch(repository);
    	context().watch(frontier);

    	for (String site : s.seeds.keySet()) {
        	String name = site.replace("://", "_");
    		SeedConfig conf = s.seeds.get(site);
    		ActorRef modeler = context().actorOf(Props.create(CrawlModeler.class), 
    				"CrawlModeler_"+name);
    		modeler.tell(new ModelMsg(HTML,site,conf), self());
    	}
    }
    
    private void sendToFrontier(CrawlURL curl) {
    	ActorRef frontier = context().child("frontier").get();
    	frontier.tell(curl, self());
    	if (!started) {
    		frontier.tell(START, self());
    		started = true;
    	}
    }
    
    private void stopSystem() {
		context().system().scheduler().scheduleOnce(
				Duration.create(60, TimeUnit.SECONDS), 
				self(), END_SYSTEM, context().dispatcher(), null);
    }
    
}
