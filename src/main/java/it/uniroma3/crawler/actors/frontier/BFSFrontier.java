package it.uniroma3.crawler.actors.frontier;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
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
	private CrawlQueue queue;
 	private Queue<ActorRef> requesters;
	private Random random;
	private int maxPages;
	private int pageCount;
	private boolean isEnding;
	
	static class InnerProps implements Creator<BFSFrontier> {
		private int fetchers, maxPages, inMemory;
		
		public InnerProps(int fetchers, int max, int inMemory) {
			this.fetchers = fetchers;
			this.maxPages = max;
			this.inMemory = inMemory;
		}

		@Override
		public BFSFrontier create() throws Exception {
			return new BFSFrontier(fetchers, maxPages, inMemory);
		}	
	}
		
	public static Props props(int fetchers, int maxPages, int inMemory) {
		return Props.create(BFSFrontier.class, new InnerProps(fetchers,maxPages,inMemory));
	}
	
	private BFSFrontier() {
		this.requesters = new LinkedList<>();
		this.random = new Random();
		this.pageCount = 0;
		this.isEnding = false;
	}

	public BFSFrontier(int fetchers, int maxPages, int inMemory) {
		this();
		this.queue = new CrawlQueue(inMemory);
		this.maxPages = maxPages;
		createFetchers(fetchers);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, this::store)
		.matchEquals(NEXT, msg -> retrieve())
		.matchEquals(START, msg -> context().actorSelection("*").tell(msg, self()))
		.matchEquals(STOP, msg -> context().system().stop(self()))
		.build();
	}
	
	private void store(CrawlURL curl) {
		if (pageCount>=maxPages) 
			terminate();
		else {
			if (queue.add(curl) && !requesters.isEmpty()) { 
				// send request for next CURL from requester
				self().tell(NEXT, requesters.poll());
			}
		}
	}
	
	private void retrieve() {
		if (pageCount>=maxPages) terminate();
		else {
			if (!queue.isEmpty()) {
				// handle request for next url to be processed
				CrawlURL next = queue.next();
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
	
	private void createFetchers(int n) {
		for (int i=1;i<n+1;i++) {
			ActorRef child = 
					context().actorOf(Props.create(CrawlFetcher.class), "fetcher"+i);
			context().watch(child);
		}
	}
	
	@Override
	public void postStop() {
		queue.deleteStorage();
		context().system().terminate();
	}
	
}
