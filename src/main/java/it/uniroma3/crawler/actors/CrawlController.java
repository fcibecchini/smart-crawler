package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;

import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.remote.RemoteScope;
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
    	initModels(set.seeds);
    }
    
    private void initModels(Map<String,SeedConfig> sites) {
		Config nodes = ConfigFactory.load("nodes");
		Map<Integer,Address> addrs = addresses(nodes);
		int n = addrs.size();
		int i = 0;
    	for (String site : sites.keySet()) {
        	String name = site.replace("://", "_");
    		SeedConfig conf = sites.get(site);
    		Address addr = addrs.get(i);
    		Props props = CrawlModeler.props(site, conf)
    				.withDeploy(new Deploy(new RemoteScope(addr)));
    		context().actorOf(props, "modeler_"+name);
    		i = (i++) % n;
    	}
    }
    
	private Map<Integer, Address> addresses(Config nodes) {
		Map<Integer, Address> addr = new HashMap<>();
		ConfigObject obj = nodes.getObject("nodes");
		int i=0;
		for (String k : obj.keySet()) {
			Config conf = nodes.getConfig("nodes."+k);
			String host = conf.getString("host");
			int port = conf.getInt("port");
			String protocol = conf.getString("protocol");
			String system = conf.getString("system");
			addr.put(i++, new Address(protocol, system, host, port));
		}
		return addr;
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
