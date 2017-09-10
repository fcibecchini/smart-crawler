package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.uniroma3.crawler.actors.frontier.CrawlFrontier;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.CrawlModeler;
import it.uniroma3.crawler.settings.AddressSettings;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import it.uniroma3.crawler.util.FileUtils;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlController extends AbstractLoggingActor {
	private CrawlerSettings set;
	private int frontiers, models;
	
	public CrawlController() {
    	set = Settings.SettingsProvider.get(context().system());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> startCrawling())
		.matchEquals(STOP, msg -> stop())
		.matchEquals(SAVED, msg -> {if (--models==0) context().system().terminate();})
		.match(PageClass.class, this::initFrontier)
		.build();
	}
    
    private void startCrawling() {
    	context().watch(context().actorOf(Props.create(CrawlRepository.class),
    			"repository"));
    	initModels();
    }
    
    private void initModels() {
    	String[] nodes = AddressSettings.SettingsProvider
    			.get(context().system()).nodes;
		int n = nodes.length;
		int i = 0;
		frontiers = (int) set.seeds.stream().filter(c -> c.crawl).count();
		/* Check if this session will only produce models without any actual crawling */
		if (frontiers==0) models = set.seeds.size();
    	for (SeedConfig conf : set.seeds) {
        	String name = FileUtils.normalizeURL(conf.site);
    		ActorRef modeler = 
    			context().actorOf(Props.create(CrawlModeler.class), "modeler_"+name);
    		modeler.tell(new ModelMsg(conf, nodes[i]), self());
    		i = (i+1) % n;
    	}
    }
    
    private void initFrontier(PageClass root) {
    	ActorRef frontier = createFrontier(root);
    	context().watch(frontier);
    	frontier.tell(START, self());
    }
    
    private ActorRef createFrontier(PageClass pclass) {
    	String name = FileUtils.normalizeURL(pclass.getDomain());
		ActorRef frontier = context().actorOf(
				CrawlFrontier.props(set.fetchers, set.pages, set.frontierheap, pclass), 
    			"frontier_"+name);
    	return frontier;
    }
    
    private void stop() {
    	context().unwatch(sender());
    	context().stop(sender());
    	if (--frontiers==0)
    		context().system().terminate();
    }
    
}
