package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlModeler extends AbstractLoggingActor {
	
	static class InnerProps implements Creator<CrawlModeler> {
		private static final long serialVersionUID = 1L;
		private String site;
		private SeedConfig conf;
		
		public InnerProps(String site, SeedConfig conf) {
			this.site = site;
			this.conf = conf;
		}

		@Override
		public CrawlModeler create() throws Exception {
			return new CrawlModeler(site,conf);
		}	
	}
		
	public static Props props(String site, SeedConfig conf) {
		return Props.create(CrawlModeler.class, new InnerProps(site, conf));
	}
	
	public CrawlModeler(String site, SeedConfig conf) {
		ActorRef modeler = chooseModeler(site,conf);
		context().watch(modeler);
		modeler.tell(START, self());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(PageClass.class, rootClass -> context().parent().tell(rootClass, self()))
		.matchEquals(STOP, s -> context().stop(self()))
		.build();
	}
    
    private ActorRef chooseModeler(String site, SeedConfig sc) {
    	Website website = new Website(HtmlUtils.transformURL(site), sc.maxfailures, sc.randompause, sc.javascript);
    	if (!sc.file.equals("null"))
    		return context().actorOf(StaticModeler.props(website, sc.wait, sc.file), "static");
    	else 
    		return context().actorOf(DynamicModeler.props(website, sc.wait, sc.pages), "dynamic");
    }
    
}
