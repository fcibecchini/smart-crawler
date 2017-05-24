package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.uniroma3.crawler.actors.frontier.CrawlFrontier;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.CrawlModeler;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlController extends AbstractLoggingActor {
	private CrawlerSettings set;
	private int frontiers;
	
	public CrawlController() {
    	set = Settings.SettingsProvider.get(context().system());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> startCrawling())
		.matchEquals(STOP, msg -> stop())
		.match(PageClass.class, pclass -> {
			context().stop(sender());
			if (!set.modelOnly) initFrontier(pclass);
		})
		.build();
	}
    
    private void startCrawling() {
    	if (!set.modelOnly)
    		context().watch(context().actorOf(Props.create(CrawlRepository.class), "repository"));
    	for (String site : set.seeds.keySet()) {
        	String name = site.replace("://", "_");
    		SeedConfig conf = set.seeds.get(site);
    		context().actorOf(CrawlModeler.props(site, conf), "modeler_"+name);
    	}
    }
    
    private void initFrontier(PageClass root) {
    	ActorRef frontier = createFrontier(root);
    	context().watch(frontier);
    	frontiers++;
    	frontier.tell(START, self());
    }
    
    private ActorRef createFrontier(PageClass pclass) {
    	String name = pclass.getDomain().replace("://", "_");
		ActorRef frontier = context().actorOf(
				CrawlFrontier.props(set.fetchers, set.pages, set.frontierheap, pclass), 
    			"frontier_"+name);
    	return frontier;
    }
    
    private void stop() {
    	context().unwatch(sender());
    	context().stop(sender());
    	frontiers--; 
    	if (frontiers==0)
    		context().system().terminate();
    }
    
}
