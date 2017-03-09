package it.uniroma3.crawler.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class HtmlUtils {
	
	public static WebClient makeWebClient() {
		final WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setCssEnabled(false);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		return webClient;
	}
	 
	public static HtmlPage getPage(String url, WebClient client, Logger log) throws Exception {
		final HtmlPage entry = client.getPage(url);
		log.info("Page reached = "+url+" - "+entry.getTitleText());
		return entry;
	}

}
