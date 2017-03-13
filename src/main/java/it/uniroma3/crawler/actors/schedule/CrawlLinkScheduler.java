package it.uniroma3.crawler.actors.schedule;

import java.util.Random;

import akka.actor.UntypedActor;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlLinkScheduler extends UntypedActor {
	private Random random;
	private CrawlController controller;
	
	public CrawlLinkScheduler() {
		this.random = new Random();
		this.controller = CrawlController.getInstance();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof CrawlURL) {
			CrawlURL newCUrl = (CrawlURL) message;
			long waitTime = 
					controller.getWaitTime() + 
					random.nextInt(controller.getRoundTime());
			newCUrl.getPageClass().setWaitTime(waitTime);
			// send to Frontier
			controller.getFrontier().tell(newCUrl, getSelf());
		}
		else unhandled(message);
	}
}
