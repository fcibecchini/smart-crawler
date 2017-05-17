package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.uniroma3.crawler.actors.frontier.CrawlFrontier;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.CrawlModeler;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlController extends AbstractLoggingActor {	
	private int fetchers, pages, heapSize, frontiers;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> startCrawling())
		.matchEquals(STOP, msg -> stop())
		.match(PageClass.class, pclass -> {
			sender().tell(STOP, self());
			initFrontier(pclass);
		})
		.build();
	}
    
    private void startCrawling() {
    	CrawlerSettings s = Settings.SettingsProvider.get(context().system());
    	fetchers = s.fetchers;
    	pages = s.pages;
    	heapSize = s.frontierheap;
    	
    	ActorRef repository = context().actorOf(Props.create(CrawlRepository.class), "repository");
    	context().watch(repository);

    	for (String site : s.seeds.keySet()) {
        	String name = site.replace("://", "_");
    		SeedConfig conf = s.seeds.get(site);
    		ActorRef modeler = context().actorOf(Props.create(CrawlModeler.class), 
    				"modeler_"+name);
    		modeler.tell(new ModelMsg(site,conf), self());
    	}
    }
    
    private void initFrontier(PageClass root) {
    	ActorRef frontier = createFrontier(root);
    	frontier.tell(START, self());
    }
    
    private ActorRef createFrontier(PageClass pclass) {
    	String name = pclass.getDomain().replace("://", "_");
		ActorRef frontier = context().actorOf(
				CrawlFrontier.props(fetchers, pages, heapSize, pclass), 
    			"frontier_"+name);
    	context().watch(frontier);
    	frontiers++;
    	return frontier;
    }
    
    private void stop() {
    	context().stop(sender());
    	frontiers--; 
    	if (frontiers==0)
    		context().system().terminate();
    }
    
}
