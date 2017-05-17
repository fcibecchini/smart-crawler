package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.REPOSITORY;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import it.uniroma3.crawler.messages.StoreURLMsg;
import it.uniroma3.crawler.messages.OldURLMsg;
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
		ActorSelection frontier = context().actorSelection("../../../..");
		ActorSelection repository = context().actorSelection(REPOSITORY);
		
		frontier.tell(new OldURLMsg(curl), self());
		// stop crawlPage actor for this URL
		repository.tell(new StopMsg(curl.getStringUrl()), self());
				
		curl.getOutLinks().stream()
		.map(link -> new StoreURLMsg(link, curl.getOutLinkPageClass(link)))
		.forEach(newcurl -> frontier.tell(newcurl, self()));
		
	}

}
