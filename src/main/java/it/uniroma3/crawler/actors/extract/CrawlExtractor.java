package it.uniroma3.crawler.actors.extract;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.actors.write.CrawlDataWriter;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.XPathUtils;

public class CrawlExtractor extends UntypedActor {
	private ActorRef crawlWriter;
	private String urlBase;
	
	public CrawlExtractor() {
		this.urlBase = CrawlController.getInstance().getUrlBase();
		this.crawlWriter = getContext().actorOf(Props.create(CrawlDataWriter.class), 
				"dataWriter"+getSelf().path().name());
	}
	
	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof CrawlURL) {
			CrawlURL cUrl = (CrawlURL) message;
			setOutLinks(cUrl);
			setDataRecord(cUrl);
			// send cUrl with inserted data to writer
			crawlWriter.tell(cUrl, getSelf());
		}
		else unhandled(message);
	}
	
	@SuppressWarnings("unchecked")
	private void setOutLinks(CrawlURL cUrl) {
		PageClass src = cUrl.getPageClass();
		List<String> navXPaths = src.getNavigationXPaths();
		for (String xPath : navXPaths) {
			List<HtmlAnchor> links = 
					(List<HtmlAnchor>) XPathUtils.getByMatchingXPath(cUrl.getPageContent(), xPath);
			for (HtmlAnchor anchor : links) {
				String link = anchor.getHrefAttribute();
				// TODO needs more checks...
				if (!link.contains("http")) {
					link = urlBase + link;
				}
				PageClass dest = src.getDestinationByXPath(xPath);
				
				cUrl.addOutLink(link, dest);
			}
		}
	}
	
	private void setDataRecord(CrawlURL cUrl) {
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
			
			cUrl.setRecord(record);
		}
	}
}
