package it.uniroma3.crawler.actors.extract;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.actors.schedule.CrawlLinkScheduler;
import it.uniroma3.crawler.actors.write.CrawlDataWriter;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.XPathUtils;

public class CrawlExtractor extends UntypedActor {
	private ActorRef crawlWriter, linkScheduler;
	private CrawlController controller;
	
	public CrawlExtractor() {
		this.controller = CrawlController.getInstance();
		this.crawlWriter = getContext().actorOf(Props.create(CrawlDataWriter.class));
		this.linkScheduler = getContext().actorOf(Props.create(CrawlLinkScheduler.class));
	}
	
	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof CrawlURL) {
			CrawlURL cUrl = (CrawlURL) message;
			crawlWriter.tell(cUrl, getSelf());
			extractAndSendLinks(cUrl);
			extractAndSendData(cUrl);
		}
		else unhandled(message);
	}
	
	@SuppressWarnings("unchecked")
	private void extractAndSendLinks(CrawlURL cUrl) {
		PageClass src = cUrl.getPageClass();
		List<String> navXPaths = src.getNavigationXPaths();
		for (String xPath : navXPaths) {
			List<HtmlAnchor> links = 
					(List<HtmlAnchor>) XPathUtils.getByMatchingXPath(cUrl.getPageContent(), xPath);
			for (HtmlAnchor link : links) {
				String url = controller.getUrlBase() + link.getHrefAttribute();
				PageClass dest = src.getDestinationByXPath(xPath);
				CrawlURL newCUrl = CrawlURLFactory.getCrawlUrl(url, dest);
				// send new crawl url to scheduler for further processing
				linkScheduler.tell(newCUrl, getSelf());
			}
		}
	}
	
	private void extractAndSendData(CrawlURL cUrl) {
		PageClass src = cUrl.getPageClass();
		if (src.isDataPage()) {
			List<String> dataXPaths = src.getDataXPaths();
			List<String> values = new ArrayList<>();
			for (String xPath : dataXPaths) {
				DataType data = src.getDataTypeByXPath(xPath);
				// handle this value on the base of its type
				String value = data.extract(cUrl.getPageContent());
				values.add(value);
			}
			String[] record = values.toArray(new String[dataXPaths.size()]);
			// send record to writer
			crawlWriter.tell(record, getSelf());
		}

	}
}
