package it.uniroma3.crawler.actors;

import static akka.pattern.PatternsCS.ask;
import static akka.pattern.PatternsCS.pipe;
import static it.uniroma3.crawler.util.Commands.*;

import java.util.concurrent.CompletableFuture;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.messages.SaveMsg;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlCache extends AbstractLoggingActor {
	private final int id;
	private final ActorRef extractor;
	
	static public class ResultMsg {
		private final CrawlURL curl;
		private final Short code;
		
		public ResultMsg(CrawlURL curl, Short code) {
			this.curl = curl;
			this.code = code;
		}
		
		public CrawlURL getCurl() {
			return this.curl;
		}
		
		public Short getCode() {
			return this.code;
		}
	}

	public CrawlCache() {
		this.id = Integer.parseInt(self().path().name().replace("cache", ""));
		String extract = "extractor" + id;
		this.extractor = context().actorOf(Props.create(CrawlExtractor.class), extract);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, this::requestSave)
		.match(ResultMsg.class, this::sendSavedCurl)
		.build();
	}
	
	private void requestSave(CrawlURL curl) {
		String url = curl.getStringUrl();
		ActorSelection repository = context().actorSelection(REPOSITORY);
		
		CompletableFuture<Object> future = 
				ask(repository, 
				new SaveMsg(url), 
				10000).toCompletableFuture();
		
		CompletableFuture<ResultMsg> result = future.thenApply(v -> {
			Short code = (Short) future.join();
			return new ResultMsg(curl,code);
		});
		pipe(result, context().dispatcher()).to(self());
	}
	
	private void sendSavedCurl(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		if (msg.getCode()==SAVED)
			extractor.tell(curl, self());
		else {
			// Stop crawlPage actor
			context().actorSelection(REPOSITORY)
			.tell(new StopMsg(curl.getStringUrl()), self());
		}
	}

}
