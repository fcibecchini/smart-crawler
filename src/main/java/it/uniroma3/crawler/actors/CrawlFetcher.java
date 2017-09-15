package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;
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
import it.uniroma3.crawler.model.PageClass;
import scala.concurrent.duration.Duration;

public class CrawlFetcher extends AbstractLoggingActor {
	private final int id;
	private final ActorRef cache;
	private int failures;
	
	static public class ResultMsg {
		private final CrawlURL curl;
		private final String url;
		private final int responseCode;
		
		public ResultMsg(CrawlURL curl, String url, int resp) {
			this.curl = curl;
			this.url = url;
			this.responseCode = resp;
		}
		
		public CrawlURL getCurl() {
			return this.curl;
		}
		
		public String getUrl() {
			return this.url;
		}
		
		public int getResponseCode() {
			return this.responseCode;
		}
	}
	
	public CrawlFetcher() {
		this.id = Integer.parseInt(self().path().name().replace("fetcher", ""));
		String cacheName = "cache" + id;
		this.cache = context().actorOf(Props.create(CrawlCache.class), cacheName);
		this.failures = 0;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> context().parent().tell(NEXT, self()))
		.match(CrawlURL.class, this::fetchRequest)
		.match(ResultMsg.class, this::fetchHandle)
		.build();
	}
	
	private void fetchRequest(CrawlURL curl) {
		String url = curl.getStringUrl();
		PageClass pClass = curl.getPageClass();
		boolean js = curl.getPageClass().useJavaScript();
		ActorSelection repository = context().actorSelection(REPOSITORY);
		
		CompletableFuture<Object> future = 
				ask(repository, 
					new FetchMsg(url, pClass.getForm(), curl.getFormParameters(),
							pClass.getName(), curl.getDomain(),id,js), 
					10000).toCompletableFuture();
		CompletableFuture<ResultMsg> result = future.thenApply(v -> {
			FetchedMsg msg = (FetchedMsg) future.join();
			return new ResultMsg(curl, msg.getUrl(), msg.getResponse());
		});
		pipe(result, context().dispatcher()).to(self());
	}
	
	private void fetchHandle(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		String url = curl.getStringUrl();
		String newUrl = msg.getUrl();
		
		if (msg.getResponseCode()==0) {			
			failures = 0; // everything went ok

			if (newUrl!=null)
				log().info("Page reached = "+newUrl); // url has changed
			else 
				log().info("Page reached = "+url);
			
			// send cUrl to cache for further processing
			cache.tell(curl, self());
			// request next cUrl to Frontier
			context().parent().tell(NEXT, self());
		}
		else {
			log().warning("HTTP REQUEST: FAILED "+url);
			failures++;
			if (failures <= curl.getPageClass().maxTries()) {
				log().warning("HTTP REQUEST: TRY AGAIN...");
				self().tell(curl, self());
			}
			else {
				failures = 0;
				// Stop crawlPage actor
				context().actorSelection(REPOSITORY)
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
