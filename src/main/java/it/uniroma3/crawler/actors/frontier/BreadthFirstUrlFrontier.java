package it.uniroma3.crawler.actors.frontier;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import it.uniroma3.crawler.model.CrawlURL;
import scala.concurrent.duration.Duration;

public class BreadthFirstUrlFrontier extends UntypedActor implements UrlFrontier  {
	private final static String NEXT = "next";
	private Queue<CrawlURL> urlsQueue;
	private Queue<ActorRef> requesters;
	private final int maxPages;
	private int pageCount;
	private boolean isEnding;
	
	public static Props props(final int maxPages) {
		return Props.create(new Creator<BreadthFirstUrlFrontier>() {
			private static final long serialVersionUID = 1L;

			@Override
			public BreadthFirstUrlFrontier create() throws Exception {
				return new BreadthFirstUrlFrontier(maxPages);
			}
		});
	}

	public BreadthFirstUrlFrontier(int maxPages) {
		this.urlsQueue = new PriorityQueue<>(
				(CrawlURL c1, CrawlURL c2) -> c1.compareTo(c2));
		this.requesters = new LinkedList<>();
		this.maxPages = maxPages;
		this.pageCount = 0;
		this.isEnding = false;
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
	
	public boolean terminalCondition() {
		return pageCount==maxPages;
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (terminalCondition() && !isEnding) {
				context().system().scheduler().scheduleOnce(
					Duration.create(5000, TimeUnit.MILLISECONDS), 
					getSelf(), "Stop", context().system().dispatcher(), null);
				isEnding = true;
				// job is done..
		}
		else if (message.equals(NEXT) && !terminalCondition()) {
			if (!isEmpty()) {
			// handle request for next url to be processed
			pageCount++;
			long wait = urlsQueue.peek().getPageClass().getWaitTime();
			getContext().system().scheduler().scheduleOnce(Duration
					.create(wait, TimeUnit.MILLISECONDS),
					  getSender(), next(), getContext().system().dispatcher(), getSelf());
			}
			else {
				// sender will be informed when 
				// a new CrawlURL is available
				requesters.add(getSender()); 
			}
		}
		else if (message instanceof CrawlURL && !terminalCondition()) {
			// store the received url
			CrawlURL cUrl = (CrawlURL) message;
			scheduleUrl(cUrl);
			if (!requesters.isEmpty()) { 
				// request next CrawlURL as if it was 
				// requested by the original fetcher
				getSelf().tell(NEXT, requesters.poll());
			}
		}
		else if (message.equals("Stop")) {
			context().system().stop(getSelf());
		}
		else unhandled(message);
	}
	
	@Override
	public void postStop() {
		context().system().terminate();
	}
	
}
