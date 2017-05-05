package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.factories.CrawlURLFactory.getCrawlUrl;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import it.uniroma3.crawler.messages.StopMsg;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlLinkScheduler extends AbstractActor {
 	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, this::handleCURLS)
		.build();
	}
	
	private void handleCURLS(CrawlURL curl) {
		ActorSelection frontier = context().actorSelection("/user/frontier");
		ActorSelection repository = context().actorSelection("/user/repository");
		
		curl.getOutLinks().stream()
		.map(ol -> getCrawlUrl(ol, curl.getOutLinkPageClass(ol)))
		.forEach(newcurl -> frontier.tell(newcurl, self()));
		
		// CrawlURL lifecycle ended, stop crawlPage actor
		repository.tell(new StopMsg(curl.getStringUrl()), self());
		
	}

}
