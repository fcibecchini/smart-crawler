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
		newCUrls.stream().forEach(
				cUrl -> cUrl.getPageClass().setWaitTime(
						controller.getWaitTime() + random.nextInt(controller.getRoundTime())));
	}
	
	private void schedule(List<CrawlURL> newCUrls) {
		ActorRef frontier = controller.getFrontier();
		newCUrls.stream().forEach(curl -> frontier.tell(curl, getSelf()));
	}
}
