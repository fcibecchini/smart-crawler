package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.XPathUtils.getAnchors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.OutgoingLink;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlPage extends AbstractLoggingActor {
	private HtmlPage html;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(FetchMsg.class, this::fetch)
		.match(SaveMsg.class, this::save)
		.match(ExtractLinksMsg.class, this::extract)
		.match(ExtractDataMsg.class, this::extract)			
		.build();
	}
	
	private void fetch(FetchMsg msg) {
		HtmlPage html = fetchUrl(msg.getUrl(), msg.useJavaScript());
		setHtml(html);
		int code = (html!=null) ? 0 : 1;
		sender().tell(new FetchedMsg(code), self());
	}
	
	private void save(SaveMsg msg) {
		String htmlPath = getFileUrlPath(html, FileUtils.getMirror("html", msg.getDomain()));
		try {
			savePageMirror(html, htmlPath);
			setHtml(null);
			sender().tell(new SavedMsg(htmlPath), self());
			context().parent()
			.tell(new SaveCacheMsg(msg.getDomain(),msg.getUrl(),msg.getPageClass(),htmlPath), 
					ActorRef.noSender());
		} catch (IOException e) {
			//TODO: improve exception handling
			sender().tell(new SavedMsg(""), self());
			setHtml(null);
			log().warning("save: IOException while saving page: "+msg.getUrl()+" "+e.getMessage());
		}
	}
	
	private void extract(ExtractLinksMsg msg) {
		try {
			String baseUrl = msg.getBaseUrl();
			HtmlPage html = restorePageFromFile(msg.getHtmlPath(), URI.create(baseUrl));
			Map<String, List<OutgoingLink>> outLinks = getOutLinks(html, baseUrl, msg.getNavXPaths());
			//ResolveLinksMsg question = new ResolveLinksMsg(outLinks);
			//context().parent().tell(question, sender());
			sender().tell(new ExtractedLinksMsg(outLinks), self());
		} catch (Exception e) {
			//TODO: improve exception handling
			sender().tell(new ExtractedLinksMsg(), self());
			log().warning("extract: Exception while restoring HtmlPage: "+msg.getHtmlPath()+" "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void extract(ExtractDataMsg msg) {
		try {
			HtmlPage html = restorePageFromFile(msg.getHtmlPath(), URI.create(msg.getBaseUrl()));
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
	
	private void savePageMirror(HtmlPage html, String path) throws IOException {
		File pathFile = new File(path);
		html.save(pathFile);
	}
	
	private String getFileUrlPath(HtmlPage html, String directory) {
		URL url = html.getUrl();
		String path = directory + url.getPath();
		String query;
		if ((query = url.getQuery()) != null)
			path += transformURLQuery(query);
		if (path.lastIndexOf("/") == path.length() - 1)
			path += "index";
		path += ".html";
		return path;
	}
	
	private Map<String, List<OutgoingLink>> getOutLinks(HtmlPage html, String base, List<String> xPaths) {		
		return xPaths.stream().distinct()
		.collect(toMap(Function.identity(), 
					   xp -> getAnchors(html, xp).stream()
					  .map(a -> a.getHrefAttribute())
					  .filter(l -> isValidURL(base, l))
					  .map(l -> new OutgoingLink(getAbsoluteURL(base, l)))
					  .collect(toList())));
	}
	
	private List<String> getDataRecord(HtmlPage html, Collection<DataType> dataTypes) {		
		List<String> record = dataTypes.stream().map(dt -> dt.extract(html)).collect(toList());
		return record;
	}
}
