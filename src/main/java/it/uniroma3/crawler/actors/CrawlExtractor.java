package it.uniroma3.crawler.actors;

import static akka.pattern.PatternsCS.ask;
import static akka.pattern.PatternsCS.pipe;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static it.uniroma3.crawler.util.Commands.REPOSITORY;

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
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlExtractor extends AbstractLoggingActor {
	private final int id;
	private final ActorRef scheduler;
	
	static public class ResultMsg {
		private final CrawlURL curl;
		private final Map<String, List<String>> outLinks;
		private final List<String> record;
		
		public ResultMsg(CrawlURL curl, Map<String, List<String>> links, List<String> record) {
			this.curl = curl;
			this.outLinks = links;
			this.record = record;
		}
		
		public CrawlURL getCurl() {
			return this.curl;
		}
		
		public Map<String, List<String>> getLinks() {
			return this.outLinks;
		}
		
		public List<String> getRecord() {
			return this.record;
		}
	}
	
	public CrawlExtractor() {
		this.id = Integer.parseInt(self().path().name().replace("extractor", ""));
		String sched = "scheduler" + id;
		this.scheduler = context().actorOf(Props.create(CrawlLinkScheduler.class), sched);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, this::extract)
		.match(ResultMsg.class, this::forward)
		.build();
	}
	
	private void extract(CrawlURL curl) {
		PageClass src = curl.getPageClass();

		String url = curl.getStringUrl();
		String htmlPath = curl.getFilePath();
		String mirror = FileUtils.getMirror("html", src.getDomain());

		ActorSelection repository = context().actorSelection(REPOSITORY);
		CompletableFuture<Object> links, data;

		if (!src.isEndPage())
			links = ask(repository,
					new ExtractLinksMsg(url, 
							htmlPath, 
							src.getDomain(), 
							mirror, 
							src.getNavigationXPaths()), 4000)
							.toCompletableFuture();
		else
			links = completedFuture(new ExtractedLinksMsg());

		if (src.isDataPage())
			data = ask(repository, 
					new ExtractDataMsg(url, 
					htmlPath, 
					src.getDomain(), 
					src.getDataTypes()), 4000)
					.toCompletableFuture();
		else
			data = completedFuture(new ExtractedDataMsg());

		CompletableFuture<ResultMsg> result = allOf(links, data).thenApply(v -> {
			ExtractedLinksMsg msg1 = (ExtractedLinksMsg) links.join();
			ExtractedDataMsg msg2 = (ExtractedDataMsg) data.join();
			return new ResultMsg(curl, msg1.getLinks(), msg2.getRecord());
		});

		pipe(result, context().dispatcher()).to(self());
	}
	
	private void forward(ResultMsg msg) {
		CrawlURL curl = msg.getCurl();
		CrawlURL copy = copy(curl);
		
		copy.setFilePath(curl.getFilePath());
		
		Map<String, List<String>> links = msg.getLinks();
		PageClass src = curl.getPageClass();
		for (String xPath : links.keySet()) {
			for (String link : links.get(xPath)) {
				PageClass dest = src.getDestinationByXPath(xPath);
				copy.addOutLink(link, dest.getName());
			}
		}
		
		List<String> recordList = msg.getRecord();
		if (!recordList.isEmpty()) {
			String[] record = msg.getRecord().toArray(new String[recordList.size()]);
			copy.setRecord(record);
		}

		scheduler.tell(copy, self());
	}
	
}
