package it.uniroma3.crawler.actors.schedule;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.factories.CrawlURLFactory;
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
			CrawlURL oldCUrl = (CrawlURL) message;
			List<CrawlURL> newCUrls = extractCrawlURLs(oldCUrl);
			// dummy wait time update...
			setWaitTime(newCUrls);
			// send to Frontier
			schedule(newCUrls);
		}
		else unhandled(message);
	}
	
	private List<CrawlURL> extractCrawlURLs(CrawlURL cUrl) {
		return cUrl.getOutLinks().stream()
			.map(url -> 
				CrawlURLFactory.getCrawlUrl(url, cUrl.getOutLinkPageClass(url)))
			.collect(Collectors.toList());
	}
	
	private void setWaitTime(List<CrawlURL> newCUrls) {
		long waitTime = controller.getWaitTime();
		int randTime = controller.getRoundTime();
		newCUrls.stream().forEach(cUrl -> cUrl.getPageClass().setWaitTime(waitTime + random.nextInt(randTime)));
	}
	
	private void schedule(List<CrawlURL> newCUrls) {
		ActorRef frontier = controller.getFrontier();
		newCUrls.stream().forEach(curl -> frontier.tell(curl, getSelf()));
	}
}
