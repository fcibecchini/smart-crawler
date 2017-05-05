package it.uniroma3.crawler.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
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
	
	public static String getWebResponse(String url, WebClient client) throws Exception {
		final HtmlPage entry = client.getPage(url);
		return entry.getWebResponse().getContentAsString();
	}
	
	public static String getXmlPage(String url, WebClient client) throws Exception {
		final HtmlPage entry = client.getPage(url);
		return entry.asXml();
	}
	
	public static HtmlPage restorePage(String htmlSrc, URI url) throws Exception {
		StringWebResponse response = new StringWebResponse(htmlSrc, url.toURL());
		WebClient client = makeWebClient();
		HtmlPage page = HTMLParser.parseHtml(response, client.getCurrentWindow());
		client.close();
		return page;
	}
	
	public static HtmlPage restorePageFromFile(String path, URI url) throws Exception {
		String src = new String(Files.readAllBytes(Paths.get(path.replaceFirst("^/(.:/)", "$1"))), Charset.forName("UTF-8"));
		return restorePage(src, url);
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
	
	public static URL getURL(String base, String relative) {
		try {
			URL baseUrl = new URL(base);
			URL url = new URL(baseUrl, relative);
			return url;
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	public static String getCachedURL(String root, URL url) {
		String file = url.getPath();
		if (url.getQuery() != null) file += transformURLQuery(url.getQuery());
		String result = new File(root + file + ".html").toURI().toString();
		return result;
	}
	
	public static String getCachedURL(String root, String base, String relative) {
		String result;
		try {
			URL baseUrl = new URL(base);
			URL url = new URL(baseUrl, relative);
			result = getCachedURL(root, url);
		} catch (MalformedURLException e) {
			result = "";
		}
		return result;
	}
	
	public static String transformURLQuery(String queryString) {
		String subPath = "";
		String[] query = queryString.split("&");
		for (String param : query) {
			subPath += param.replaceAll("=|%", "_");
		}
		return subPath;
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
