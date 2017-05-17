package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlModeler extends AbstractLoggingActor {
	private String site;
	private ActorRef modeler, sender;

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(ModelMsg.class, this::computeModel)
		.match(PageClass.class, rootClass -> sender.tell(rootClass, self()))
		.matchEquals(STOP, msg -> {
			context().stop(modeler);
			context().stop(self());
		})
		.build();
	}
	
	public void computeModel(ModelMsg msg) {
		sender = sender();
		site = msg.getSite();
		SeedConfig sc = msg.getSeedConfig();
		
		modeler = chooseModeler(site,sc);
		context().watch(modeler);
		modeler.tell(START, self());
	}
    
    private ActorRef chooseModeler(String site, SeedConfig sc) {
    	Website website = new Website(site, sc.maxfailures, sc.randompause, sc.javascript);
    	if (!sc.file.equals("null"))
    		return context().actorOf(StaticModeler.props(website, sc.wait, sc.file), "static");
    	else 
    		return context().actorOf(DynamicModeler.props(website, sc.wait, sc.pages), "dynamic");
    }
    
}
