package it.uniroma3.crawler.actors;

import static akka.pattern.PatternsCS.ask;
import static akka.pattern.PatternsCS.pipe;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.CrawlURL;
import scala.concurrent.duration.Duration;

public class CrawlFetcher extends AbstractLoggingActor {
	private final static String NEXT = "next", START = "start";
	private final int id, MAX_FAILURES, TIME_TO_WAIT;
	private final ActorRef cache;
	private int failures;
	
	static public class ResultMsg {
		private final CrawlURL curl;
		private final int responseCode;
		
		public ResultMsg(CrawlURL curl, int resp) {
			this.curl = curl;
			this.responseCode = resp;
		}
		
		public CrawlURL getCurl() {
			return this.curl;
		}
		
		public int getResponseCode() {
			return this.responseCode;
		}
	}
		
	public static Props props(int maxFailures, int time, boolean js) {
		return Props.create(CrawlFetcher.class, () -> new CrawlFetcher(maxFailures, time, js));
	}
	
	public CrawlFetcher(int maxFailures, int time, boolean js) {
		this.id = Integer.parseInt(self().path().name().replace("fetcher", ""));
		String cacheName = "cache" + id;
		this.cache = context().actorOf(Props.create(CrawlCache.class), cacheName);
		this.failures = 0;
		MAX_FAILURES = maxFailures;
		TIME_TO_WAIT = time;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> context().parent().tell(NEXT, getSelf()))
		.match(CrawlURL.class, this::fetchRequest)
		.match(ResultMsg.class, this::fetchHandle)
		.build();
	}
	
	private void fetchRequest(CrawlURL curl) {
		if (!curl.isCached()) {
			String url = curl.getStringUrl();
			ActorSelection repository = context().actorSelection("/user/repository");
	
			CompletableFuture<Object> future = 
					ask(repository, new FetchMsg(url,id), 4000).toCompletableFuture();
			CompletableFuture<ResultMsg> result = future.thenApply(v -> {
				FetchedMsg msg = (FetchedMsg) future.join();
				return new ResultMsg(curl, msg.getResponse());
			});
			pipe(result, context().dispatcher()).to(self());
		}
		else { //synchronous fetch handling since curl is cached
			fetchHandle(new ResultMsg(curl, 0));
		}
	}
	
	private void fetchHandle(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		String url = curl.getStringUrl();
		
		if (msg.getResponseCode()==0) {
			log().info("Page reached = "+url);
			
			failures = 0; // everything went ok

			// send cUrl to cache for further processing
			cache.tell(curl, self());
			
			// request next cUrl to Frontier
			context().parent().tell(NEXT, self());
		}
		else {
			log().warning("HTTP REQUEST: FAILED "+url);
			failures++;
			if (failures <= MAX_FAILURES) {
				log().warning("HTTP REQUEST: TRY AGAIN...");
				self().tell(curl, self());
			}
			else {
				failures = 0;
				// Stop crawlPage actor
				context().actorSelection("/user/repository")
				.tell(new StopMsg(curl.getStringUrl()), self());
				
				log().info("TRYING NEXT URL");
				context().parent().tell(NEXT, self());
			}
		}
	}
	
	private void waitAndRequestNext(int time) {
		// wait time befor requesting
		log().warning("HTTP REQUEST: WAIT FOR "+time+" minutes");
		context().system().scheduler().scheduleOnce(Duration
				.create(time, TimeUnit.MILLISECONDS),
				sender(), NEXT, context().dispatcher(), self());

	}
	
	private boolean isValidResponse(String page) {
		return page.contains("html");
	}
	
}
