package it.uniroma3.crawler.actors.frontier;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import it.uniroma3.crawler.actors.fetch.CrawlFetcher;
import it.uniroma3.crawler.model.CrawlURL;
import scala.concurrent.duration.Duration;

public class BreadthFirstUrlFrontier extends UntypedActor implements UrlFrontier  {
	private Queue<CrawlURL> urlsQueue;
	private final ActorRef fetcher;

	public BreadthFirstUrlFrontier() {
		this.urlsQueue = new PriorityQueue<>(
				(CrawlURL c1, CrawlURL c2) -> c1.compareTo(c2));
		this.fetcher = getContext().actorOf(Props.create(CrawlFetcher.class), "fetcher");
	}

	@Override
	public CrawlURL next() {
		return urlsQueue.poll();
	}

	@Override
	public void scheduleUrl(CrawlURL crawUrl) {
		urlsQueue.add(crawUrl);
	}

	@Override
	public boolean isEmpty() {
		return urlsQueue.isEmpty();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message.equals("next")) {
			if (!isEmpty()) {
			// handle request from fetcher for next url to be processed
			long wait = urlsQueue.peek().getPageClass().getWaitTime();
			getContext().system().scheduler().scheduleOnce(Duration
					.create(wait, TimeUnit.MILLISECONDS),
					  fetcher, next(), getContext().system().dispatcher(), null);
			}
			else { // frontier is empty, we must wait (?)
				getContext().system().scheduler().scheduleOnce(Duration
						.create(2000, TimeUnit.MILLISECONDS),
						  getSelf(), "next", getContext().system().dispatcher(), null);
			}
		}
		else if (message.equals("start crawling")) {
			fetcher.tell(next(), getSelf());
		}
		else if (message instanceof CrawlURL) {
			// store the received url
			CrawlURL cUrl = (CrawlURL) message;
			scheduleUrl(cUrl);
		}
		
		else unhandled(message);
	}

}
