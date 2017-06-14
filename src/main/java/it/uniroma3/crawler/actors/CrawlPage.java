package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.XPathUtils.getAbsoluteInternalURLs;
import static it.uniroma3.crawler.util.Commands.*;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.util.FileUtils;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlPage extends AbstractLoggingActor {
	private String url;
	private String pclass;
	private String domain;
	private HtmlPage html;
	private String htmlPath;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(FetchMsg.class, this::fetch)
		.matchEquals(SAVE, msg -> save())
		.match(ExtractLinksMsg.class, this::extract)
		.match(ExtractDataMsg.class, this::extract)			
		.build();
	}
	
	private void fetch(FetchMsg msg) {
		setURL(msg.getUrl());
		setClass(msg.getPageClass());
		setDomain(msg.getDomain());

		HtmlPage html = fetchUrl(url, msg.useJavaScript());
		setHtml(html);
		int code = (html!=null) ? 0 : 1;
		sender().tell(new FetchedMsg(code), self());
	}
	
	private void save() {
		String directory = FileUtils.getPagesDirectory(domain);
		String path = HtmlUtils.savePage(html, directory, true);
		if (!path.isEmpty()) {
			setHtmlPath(path);
			sender().tell(SAVED, self());
			context().parent().tell(new SaveCacheMsg(domain,url,pclass,path), 
				ActorRef.noSender());
		}
		else {
			//TODO: improve exception handling
			sender().tell(ERROR, self());
			log().warning("save: IOException while saving page: "+url);
		}
		setHtml(null);
	}
	
	private void extract(ExtractLinksMsg msg) {
		try {
			HtmlPage html = restorePageFromFile(htmlPath, domain);
			Map<String, List<String>> outLinks = getOutLinks(html, domain, msg.getNavXPaths());
			sender().tell(new ExtractedLinksMsg(outLinks), self());
		} catch (Exception e) {
			//TODO: improve exception handling
			sender().tell(new ExtractedLinksMsg(), self());
			log().warning("extract: Exception while restoring HtmlPage: "+htmlPath+" "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void extract(ExtractDataMsg msg) {
		try {
			HtmlPage html = restorePageFromFile(htmlPath, domain);
			List<String> record = getDataRecord(html, msg.getData());
			sender().tell(new ExtractedDataMsg(record), self());
		} catch (Exception e) {
			//TODO: improve exception handling
			sender().tell(new ExtractedDataMsg(), self());
			log().warning("extract: Exception while restoring HtmlPage: "+e.getMessage());
		}
	}
	
	private void setHtml(HtmlPage html) {
		this.html = html;
	}
	
	private void setHtmlPath(String htmlPath) {
		this.htmlPath = htmlPath;
	}
	
	private void setDomain(String domain) {
		this.domain = domain;
	}
	
	private void setURL(String url) {
		this.url = url;
	}
	
	private void setClass(String pclass) {
		this.pclass = pclass;
	}
	 
	private HtmlPage fetchUrl(String url, boolean js) {
		WebClient client = makeWebClient(js);
		HtmlPage page;
		try {
			page = getPage(url, client);
		} catch (Exception e) {
			page = null;
		} finally {
			client.close();
		}
		return page;
	}
	
	private Map<String, List<String>> getOutLinks(HtmlPage html, String base, List<String> xPaths) {		
		return xPaths.stream().distinct()
		.collect(toMap(Function.identity(), 
					   xp -> getAbsoluteInternalURLs(html,xp,base)));
	}
	
	private List<String> getDataRecord(HtmlPage html, Map<String, DataType> dataTypes) {
		List<String> record = dataTypes.keySet().stream()
				.map(xp -> dataTypes.get(xp).extract(html, xp))
				.collect(toList());
		return record;
	}
	
}
