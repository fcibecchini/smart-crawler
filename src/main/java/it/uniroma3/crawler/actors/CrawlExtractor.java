package it.uniroma3.crawler.actors;

import static akka.pattern.PatternsCS.ask;
import static akka.pattern.PatternsCS.pipe;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static it.uniroma3.crawler.factories.CrawlURLFactory.copy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.OutgoingLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlExtractor extends AbstractLoggingActor {
	private final int id;
	private final ActorRef crawlWriter, scheduler;
	
	static public class ResultMsg {
		private final CrawlURL curl;
		private final Map<String, List<OutgoingLink>> outLinks;
		private final String[] record;
		
		public ResultMsg(CrawlURL curl, Map<String, List<OutgoingLink>> links, String[] record) {
			this.curl = curl;
			this.outLinks = links;
			this.record = record;
		}
		
		public CrawlURL getCurl() {
			return this.curl;
		}
		
		public Map<String, List<OutgoingLink>> getLinks() {
			return this.outLinks;
		}
		
		public String[] getRecord() {
			return this.record;
		}
	}
	
	public CrawlExtractor() {
		this.id = Integer.parseInt(self().path().name().replace("extractor", ""));
		String writer = "writer" + id;
		String sched = "scheduler" + id;
		this.crawlWriter = context().actorOf(Props.create(CrawlDataWriter.class), writer);
		this.scheduler = context().actorOf(Props.create(CrawlLinkScheduler.class), sched);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, this::extract)
		.match(ResultMsg.class, this::examine)
		.build();
	}
	
	private void extract(CrawlURL curl) {
		PageClass src = curl.getPageClass();

		if (src.isDataPage() || !src.isEndPage()) {
			String url = curl.getStringUrl();
			String htmlPath = curl.getFilePath();
			String mirror = FileUtils.getMirror("html", src.getWebsite());
			
			ActorSelection repository = context().actorSelection("/user/repository");
			CompletableFuture<Object> links, data;
			
			if (!src.isEndPage())
				links = ask(repository,
						new ExtractLinksMsg(url, htmlPath, src.getWebsite(), mirror, src.getNavigationXPaths()), 
						4000).toCompletableFuture();
			else
				links = completedFuture(new ExtractedLinksMsg());
			
			if (src.isDataPage() && !curl.isCached())
				data = ask(repository, 
					   new ExtractDataMsg(url, htmlPath, src.getWebsite(), src.getDataTypes()), 
					   4000).toCompletableFuture();
			else
				data = completedFuture(null);
			
			CompletableFuture<ResultMsg> result = allOf(links, data).thenApply(v -> {
				ExtractedLinksMsg msg1 = (ExtractedLinksMsg) links.join();
				String[] msg2 = (String[]) data.join();
				return new ResultMsg(curl, msg1.getLinks(), msg2);
			});

			pipe(result, context().dispatcher()).to(self());
		}
	}
	
	private void examine(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		if (msg.getLinks().isEmpty() && msg.getRecord()==null)
			// nothing to do, stop crawlPage actor
			context().actorSelection("/user/repository")
			.tell(new StopMsg(curl.getStringUrl()), self());
		else
			forward(msg);
	}
	
	private void forward(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		PageClass src = curl.getPageClass();
		CrawlURL copy = copy(curl);
		
		copy.setCached(curl.isCached());
		copy.setFilePath(curl.getFilePath());
		Map<String, List<OutgoingLink>> links = msg.getLinks();
		for (String xPath : links.keySet()) {
			for (OutgoingLink link : links.get(xPath)) {
				PageClass dest = src.getDestinationByXPath(xPath);
				copy.addOutLink(link, dest);
			}
		}
		
		/*
		 * FIXME: we don't know if the crawlWriter handled this message before the failure even if we correctly cached the page.
		 * We could loose some extracted data because of this assumption.
		 */
		String[] record = msg.getRecord();
		if (record!=null) {
			copy.setRecord(msg.getRecord());
			crawlWriter.tell(copy, self());
		}
		if (!links.isEmpty()) scheduler.tell(copy, self());
	}
	
}
