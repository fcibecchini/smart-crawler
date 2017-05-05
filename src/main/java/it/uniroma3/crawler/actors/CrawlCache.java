package it.uniroma3.crawler.actors;

import static akka.pattern.PatternsCS.ask;
import static akka.pattern.PatternsCS.pipe;
import static it.uniroma3.crawler.factories.CrawlURLFactory.copy;

import java.util.concurrent.CompletableFuture;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.messages.SaveMsg;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlCache extends AbstractLoggingActor {
	private final int id;
	private final ActorRef extractor;
	
	static public class ResultMsg {
		private final CrawlURL curl;
		private final String path;
		
		public ResultMsg(CrawlURL curl, String path) {
			this.curl = curl;
			this.path = path;
		}
		
		public CrawlURL getCurl() {
			return this.curl;
		}
		
		public String getPath() {
			return this.path;
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
		.match(CrawlURL.class, this::save)
		.match(ResultMsg.class, this::sendSavedCurl)
		.build();
	}
	
	private void save(CrawlURL curl) {
		if (!curl.isCached()) 
			requestSave(curl);
		else 
			extractor.tell(curl, self());
	}
	
	private void requestSave(CrawlURL curl) {
		String url = curl.getStringUrl();
		PageClass src = curl.getPageClass();
		String mirror = FileUtils.getMirror("html", src.getWebsite());
		ActorSelection repository = context().actorSelection("/user/repository");
		
		CompletableFuture<Object> future = 
				ask(repository, 
				new SaveMsg(url, src.getName(), mirror), 
				10000).toCompletableFuture();
		
		CompletableFuture<ResultMsg> result = future.thenApply(v -> {
			SavedMsg msg = (SavedMsg) future.join();
			return new ResultMsg(curl, msg.getFilePath());
		});
		pipe(result, context().dispatcher()).to(self());
	}
	
	private void sendSavedCurl(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		String path = msg.getPath();
		if (!path.isEmpty()) {
			CrawlURL copy = copy(curl);
			copy.setFilePath(path);
			// send cUrl to extractor for further processing
			extractor.tell(copy, self());
		}
		else {
			// Stop crawlPage actor
			context().actorSelection("/user/repository")
			.tell(new StopMsg(curl.getStringUrl()), self());
		}
	}

}
