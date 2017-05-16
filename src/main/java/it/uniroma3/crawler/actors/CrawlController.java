package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.uniroma3.crawler.actors.frontier.CrawlFrontier;
import it.uniroma3.crawler.messages.InitCrawling;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.modeler.CrawlModeler;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlController extends AbstractLoggingActor {
	private final static String HTML = "html";
	private int frontiers;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> startCrawling())
		.matchEquals(STOP, msg -> stop())
		.match(CrawlURL.class, this::sendToFrontier)
		.build();
	}
    
    private void startCrawling() {
    	CrawlerSettings s = Settings.SettingsProvider.get(context().system());
    	
    	ActorRef repository = context().actorOf(Props.create(CrawlRepository.class), "repository");
    	context().watch(repository);

    	for (String site : s.seeds.keySet()) {
        	String name = site.replace("://", "_");
    		SeedConfig conf = s.seeds.get(site);
    		ActorRef modeler = context().actorOf(Props.create(CrawlModeler.class), 
    				"CrawlModeler_"+name);
    		ActorRef frontier = context().actorOf(CrawlFrontier.props(s.fetchers, s.pages, s.frontierheap), 
        			"frontier_"+name);
        	context().watch(frontier);
        	frontiers++;
    		modeler.tell(new ModelMsg(HTML,site,conf), self());
    	}
    }
    
    private void sendToFrontier(CrawlURL curl) {
    	sender().tell(STOP, self());
    	String frontierName = curl.getDomain().replace("://", "_");
    	ActorRef frontier = context().child("frontier_"+frontierName).get();
    	frontier.tell(new InitCrawling(curl), self());
    }
    
    private void stop() {
    	context().stop(sender());
    	frontiers--; 
    	if (frontiers==0)
    		context().system().terminate();
    }
    
}
