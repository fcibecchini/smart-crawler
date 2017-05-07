package it.uniroma3.crawler.actors.frontier;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import it.uniroma3.crawler.actors.CrawlFetcher;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import scala.concurrent.duration.Duration;

public class BFSFrontier extends AbstractLoggingActor  {
	private final static String NEXT = "next"; 
	private final static String	START = "start"; 
	private final static String	STOP = "stop";
	private Queue<CrawlURL> urlsQueue;
	private Set<String> visitedUrls;
 	private Queue<ActorRef> requesters;
	private Random random;
	private int maxPages;
	private int pageCount;
	private boolean isEnding;
	
	static class InnerProps implements Creator<BFSFrontier> {
		private int fetchers, maxPages;
		
		public InnerProps(int fetchers, int max) {
			this.fetchers = fetchers;
			this.maxPages = max;
		}

		@Override
		public BFSFrontier create() throws Exception {
			return new BFSFrontier(fetchers, maxPages);
		}	
	}
		
	public static Props props(int fetchers, int max) {
		return Props.create(BFSFrontier.class, new InnerProps(fetchers,max));
	}
	
	private BFSFrontier() {
		this.urlsQueue = new PriorityQueue<>();
		this.visitedUrls = new HashSet<>();
		this.requesters = new LinkedList<>();
		this.random = new Random();
		this.pageCount = 0;
		this.isEnding = false;
	}

	public BFSFrontier(int fetchers, int maxPages) {
		this();
		this.maxPages = maxPages;
		createFetchers(fetchers);
	}

	public CrawlURL next() {
		return urlsQueue.poll();
	}

	public void scheduleUrl(CrawlURL curl) {
		String url = curl.getStringUrl();
		if (!visitedUrls.contains(url)) {
			visitedUrls.add(url);
			urlsQueue.add(curl);
		}
	}

	public boolean isEmpty() {
		return urlsQueue.isEmpty();
	}
	
	public boolean isEnding() {
		return isEnding;
	}
	
	public boolean end() {
		return pageCount==maxPages;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, curl -> { if (!end()) store(curl); else terminate();})
		.matchEquals(NEXT, msg -> {if (!end()) retrieve(); else terminate();})
		.matchEquals(START, this::start)
		.matchEquals(STOP, msg -> context().system().stop(self()))
		.build();
	}
	
	private void store(CrawlURL curl) {
		// store the received url
		scheduleUrl(curl);
		if (!requesters.isEmpty()) { 
			// request next CrawlURL as if it was 
			// requested by the original fetcher
			self().tell(NEXT, requesters.poll());
		}
	}
	
	private void retrieve() {
		if (!isEmpty()) {
			// handle request for next url to be processed
			CrawlURL next = next();
			if (next.isCached())
				sender().tell(next, self());
			else {
				//TODO: update page class wait time somehow
				PageClass pClass = next.getPageClass();
				long wait = pClass.getWaitTime() + random.nextInt(pClass.getPause());
				context().system().scheduler().scheduleOnce(
						Duration.create(wait, TimeUnit.MILLISECONDS),
						sender(), next, context().dispatcher(),self());
			}
			pageCount++;
		}
		else {
			// sender will be informed when 
			// a new CrawlURL is available
			requesters.add(sender()); 
		}
	}
	
	private void terminate() {
		if (!isEnding) {
			context().system().scheduler().scheduleOnce(
					Duration.create(60, TimeUnit.SECONDS), 
					self(), STOP, context().dispatcher(), null);
			isEnding = true; // job is done..
			log().info("Reached "+pageCount+" pages: ending actor system");
		}
	}
	
	private void start(String msg) {
		context().actorSelection("*").tell(msg, self());
	}
	
	private void createFetchers(int n) {
		for (int i=1;i<n+1;i++) {
			ActorRef child = 
					context().actorOf(Props.create(CrawlFetcher.class), "fetcher"+i);
			context().watch(child);
		}
	}
	
	@Override
	public void postStop() {
		context().system().terminate();
	}
	
}
