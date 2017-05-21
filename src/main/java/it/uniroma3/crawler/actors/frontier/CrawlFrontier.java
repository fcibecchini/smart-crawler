package it.uniroma3.crawler.actors.frontier;

import static it.uniroma3.crawler.util.Commands.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import it.uniroma3.crawler.actors.CrawlDataWriter;
import it.uniroma3.crawler.actors.CrawlFetcher;
import it.uniroma3.crawler.messages.StoreURLMsg;
import it.uniroma3.crawler.messages.OldURLMsg;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import scala.concurrent.duration.Duration;

public class CrawlFrontier extends AbstractPersistentActor  {
	LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private CrawlQueue queue;
	private ActorRef writer;
	private Random random;
	private int maxPages;
	private int pageCount;
	private boolean isEnding;
	
	static class InnerProps implements Creator<CrawlFrontier> {
		private static final long serialVersionUID = 1L;
		private PageClass pclass;
		private int fetchers, maxPages, size;
		
		public InnerProps(int fetchers, int max, int size, PageClass pclass) {
			this.fetchers = fetchers;
			this.maxPages = max;
			this.size = size;
			this.pclass = pclass;
		}

		@Override
		public CrawlFrontier create() throws Exception {
			return new CrawlFrontier(fetchers, maxPages, size, pclass);
		}	
	}
		
	public static Props props(int fetchers, int maxPages, int size, PageClass pclass) {
		return Props.create(CrawlFrontier.class, new InnerProps(fetchers,maxPages,size, pclass));
	}
	
	static class CompletedURL {
		public final String url;
		public CompletedURL(String url) {
			this.url = url;
		}
	}
	
	private CrawlFrontier() {
		this.random = new Random();
		this.pageCount = 0;
		this.isEnding = false;
	}

	public CrawlFrontier(int fetchers, int maxPages, int size, PageClass pclass) {
		this();
		this.queue = new CrawlQueue(size,pclass);
		this.queue.deleteStorage();
		this.maxPages = maxPages;
		this.writer = context().actorOf(Props.create(CrawlDataWriter.class), "writer");
		createFetchers(fetchers);
	}
	
	@Override
	public String persistenceId() {
		return self().path().name();
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
		.matchEquals(NEXT, n -> queue.next())
		.match(StoreURLMsg.class, ev -> queue.add(ev.getURL(), ev.getPageClass()))
		.match(CompletedURL.class, ev -> queue.removeProcessed(ev.url))
		.match(RecoveryCompleted.class, ev -> queue.recoverInProcessURLs())
		.build();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> context().actorSelection("*").tell(msg, self()))
		.matchEquals(NEXT, n -> retrieve())
		.match(StoreURLMsg.class, this::store)
		.match(OldURLMsg.class, this::handleOldCURL)
		.build();
	}
	
	private void store(StoreURLMsg msg) {
		if (pageCount>=maxPages) {
			terminate();
			return;
		}
		
		persist(msg, (StoreURLMsg ev) -> {
			boolean added = queue.add(ev.getURL(), ev.getPageClass());
			if (added) context().system().eventStream().publish(NEW_URL);
		});
	}
		
	private void retrieve() {
		if (pageCount>=maxPages) {
			terminate();
			return;
		}
		
		if (queue.isEmpty()) {
			context().system().eventStream().subscribe(sender(), Short.class);
			return;
		}
		
		// handle request for next url to be processed
		persist(NEXT, n -> {
			CrawlURL next = queue.next();
			//TODO: update page class wait time somehow
			PageClass pClass = next.getPageClass();
			long wait = pClass.getWaitTime() + random.nextInt(pClass.getPause());
			context().system().scheduler().scheduleOnce(
					Duration.create(wait, TimeUnit.MILLISECONDS),
					sender(), next, context().dispatcher(), self());
			pageCount++;
			log.info(""+pageCount);
		});
	}
	
	private void handleOldCURL(OldURLMsg msg) {
		CrawlURL curl = msg.getURL();
		writer.tell(curl, self());
		CompletedURL event = new CompletedURL(curl.getStringUrl());
		persist(event, (CompletedURL ev) -> queue.removeProcessed(ev.url));
	}
	
	private void terminate() {
		if (!isEnding) {
			context().system().scheduler().scheduleOnce(
					Duration.create(60, TimeUnit.SECONDS), 
					context().parent(), STOP, context().dispatcher(), self());
			isEnding = true; // job is done..
			log.info("Max Page Count "+pageCount+" reached: ending...");
		}
	}
	
	private void createFetchers(int n) {
		for (int i=0;i<n;i++) {
			context().watch(context().actorOf(Props.create(CrawlFetcher.class), "fetcher"+i));
		}
	}
	
}
