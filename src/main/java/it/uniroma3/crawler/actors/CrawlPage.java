package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.XPathUtils.getAbsoluteInternalURLs;
import static it.uniroma3.crawler.util.XPathUtils.getFormParameters;
import static it.uniroma3.crawler.util.Commands.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.util.FileUtils;

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

		HtmlPage html = fetchUrl(url, msg.getForm(), msg.getParams(), msg.useJavaScript());
		setHtml(html);
		int code = (html!=null) ? 0 : 1;
		FetchedMsg response = (!msg.getUrl().equals(url)) ? new FetchedMsg(url, code) : new FetchedMsg(code);
		sender().tell(response, self());
	}
	
	private void save() {
		String directory = FileUtils.getPagesDirectory(domain);
		String path = savePage(html, directory, true);
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
			Map<String, List<String>> outLinks = getOutLinks(html, domain, msg.getNavXPaths(),
					msg.getFormXPaths());
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
	 
	private HtmlPage fetchUrl(String url, String form, List<NameValuePair> params, boolean js) {
		WebClient client = makeWebClient(js);
		HtmlPage page;
		try {
			if (form!=null) {
				HtmlPage formPage = getPage(url, client);
				WebRequest request = new WebRequest(new URL(url), HttpMethod.POST);
				request.setRequestParameters(getFormParameters(formPage, form));
				
				page = getPage(request, client);
				//getPage(request, client);
				//page = getPage(url, client);
			}
			else if (!params.isEmpty()) {
				WebRequest request = new WebRequest(new URL(url), HttpMethod.POST);
				request.setRequestParameters(params);
				
				page = getPage(request, client);
				// URL will change! (hopefully...)
				setURL(page.getUrl().toString());
			}
			else
				page = getPage(url, client);
		} catch (Exception e) {
			page = null;
		} finally {
			client.close();
		}
		return page;
	}
	
	private Map<String, List<String>> getOutLinks(HtmlPage html, String base, 
			List<String> xPaths, List<String> formXPaths) throws IOException {
		Map<String, List<String>> xpath2urls = new HashMap<>();
		for (String xp : xPaths) {
			xpath2urls.put(xp, getAbsoluteInternalURLs(html,xp,base));
		}
		for (String xp : formXPaths) {
			/* Save form Name/Value pairs for a future POST request
			 * as: "http://website.com/page>search=value;param=value2;..." */
			StringBuilder postRequest = new StringBuilder(url+">");
			List<NameValuePair> formParams = getFormParameters(html, xp);
			for (NameValuePair param : formParams) {
				postRequest.append(param.getName()+"="+param.getValue()+";");
			}
			xpath2urls.put(xp, Arrays.asList(postRequest.toString()));
		}
		
		return xpath2urls;
	}
	
	private List<String> getDataRecord(HtmlPage html, Map<String, DataType> dataTypes) {
		List<String> record = dataTypes.keySet().stream()
				.map(xp -> dataTypes.get(xp).extract(html, xp.split("\t")[1]))
				.collect(toList());
		return record;
	}
	
}
