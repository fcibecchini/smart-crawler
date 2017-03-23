package it.uniroma3.crawler.util;

import java.net.URL;
import java.util.logging.Level;

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
	
	public static WebClient makeWebClient(boolean useJs) {
		final WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(useJs);
		webClient.getOptions().setCssEnabled(false);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		return webClient;
	}
	 
	public static HtmlPage getPage(String url, WebClient client) throws Exception {
		//final HtmlPage entry = client.getPage(url.replaceAll("(\\&|\\=)", "\\\\$1"));
		final HtmlPage entry = client.getPage(url.replaceAll("(\\?|\\&|\\=)", "\\\\$1"));
		return entry;
	}
	
	public static HtmlPage getPage(URL url, WebClient client) throws Exception {
		final HtmlPage entry = client.getPage(url);
		return entry;
	}

}
