package it.uniroma3.crawler.util;

import java.net.MalformedURLException;
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
	
	public static WebClient makeWebClient(boolean javascript) {
		final WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(javascript);
		if (javascript) {
			//webClient.waitForBackgroundJavaScript(10000);
			webClient.getOptions().setThrowExceptionOnScriptError(false);
		}
		webClient.getOptions().setCssEnabled(false);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		return webClient;
	}
	 
	public static HtmlPage getPage(String url, WebClient client) throws Exception {
		final HtmlPage entry = client.getPage(url);
		return entry;
	}
	
	public static String getAbsoluteURL(String base, String relative) {
		try {
			URL baseUrl = new URL(base);
			URL url = new URL(baseUrl, relative);
			return url.toString();
		} catch (MalformedURLException e) {
			return "";
		}
	}
	
	public static String transformURL(String url) {
		String newUrl = url.toLowerCase();
		if (newUrl.endsWith("/")) 
			newUrl = newUrl.substring(0, newUrl.length()-1);
		return newUrl;
	}
	
	public static boolean isValidURL(String base, String href) {
		if (href.startsWith("http") && !href.startsWith(base))
			return false;
		if (href.contains("javascript") 
				|| href.contains("crawler") 
				|| href.contains("@") 
				|| href.contains("#"))
			return false;
		return true;
	}

}
