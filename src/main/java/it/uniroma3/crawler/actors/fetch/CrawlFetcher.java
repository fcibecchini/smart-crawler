package it.uniroma3.crawler.actors.fetch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.actors.extract.CrawlExtractor;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlFetcher extends UntypedActor {
	private WebClient webClient;
	private Logger log;
	private List<ActorRef> extractors;
	private CrawlController controller;
	
	public CrawlFetcher() {
		this.webClient = HtmlUtils.makeWebClient();
		this.log = Logger.getLogger(CrawlFetcher.class.getName());
		this.extractors = new ArrayList<>();
		this.controller = CrawlController.getInstance();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message.equals("start crawling")) {
			controller.getFrontier().tell("next", getSelf());
		}
		
		else if (message instanceof CrawlURL) {
			CrawlURL cUrl = (CrawlURL) message;
			String url = cUrl.getStringUrl();
			HtmlPage body = HtmlUtils.getPage(url, webClient, log);
			cUrl.setPageContent(body);
			String pClassName = cUrl.getPageClass().getName();
			
			ActorRef extractor = getExtractorRef(pClassName);
			if (extractor==null) {
				extractor = getContext()
						.actorOf(Props.create(CrawlExtractor.class), pClassName);
				extractors.add(extractor);
			}
			
			// send cUrl to extractor for further processing
			extractor.tell(cUrl, getSelf());
		}
		else unhandled(message);

	}
	
	private ActorRef getExtractorRef(String name) {
		return extractors.stream()
				.filter(e -> e.path().name().equals(name))
				.findAny().orElse(null);
	}
}
