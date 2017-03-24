package it.uniroma3.crawler.actors.schedule;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import akka.actor.UntypedActor;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlLinkScheduler extends UntypedActor {
	private Random random;
	private CrawlController controller;
	private Set<String> fetchedUrls;
	
	public CrawlLinkScheduler() {
		this.random = new Random();
		this.controller = CrawlController.getInstance();
		this.fetchedUrls = new HashSet<>();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof CrawlURL) {
			CrawlURL oldCUrl = (CrawlURL) message;
			List<CrawlURL> newCUrls = extractCrawlURLs(oldCUrl);
			// dummy wait time update...
			updateWaitTime(newCUrls);
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
	
	private void updateWaitTime(List<CrawlURL> newCUrls) {
		long waitTime = controller.getWaitTime();
		int randTime = controller.getRoundTime();
		newCUrls.stream()
				.map(curl -> curl.getPageClass())
				.forEach(pclass -> pclass.setWaitTime(waitTime + random.nextInt(randTime)));
	}
	
	private void schedule(List<CrawlURL> newCUrls) {
		newCUrls.stream()
			.filter(curl -> !fetchedUrls.contains(curl.getUrl().toString()))
			.forEach(curl -> { 
					getSender().tell(curl, getSelf());
					fetchedUrls.add(curl.getUrl().toString());
				});
	}
}
