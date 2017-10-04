package it.uniroma3.crawler.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.net.InternetDomainName;

/**
 * Utility class providing useful methods to handle web pages
 *
 */
public class HtmlUtils {
	
	/**
	 * Wrapper method that creates a web client instance
	 * with JavaScript and CSS support disabled.
	 * @return the web client
	 */
	public static WebClient makeWebClient() {
		return makeWebClient(false);
	}
	
	/**
	 * Wrapper method that creates a web client instance 
	 * with JavaScript support enabled/disabled as specified 
	 * and CSS support disabled.
	 * @param javascript true to enable JavaScript support
	 * @return the web client
	 */
	public static WebClient makeWebClient(boolean javascript) {
		final WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(javascript);
		if (javascript) {
			//webClient.waitForBackgroundJavaScript(10000);
			webClient.getOptions().setThrowExceptionOnScriptError(false);
		}
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getCookieManager().setCookiesEnabled(false);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		return webClient;
	}
	
	/**
	 * Wrapper for {@link WebClient#getPage}.
	 * @param url the URL to fetch
	 * @param client the WebWindow to load the result of the request into
	 * @return the page returned by the server
	 * @throws Exception if an IO error occurs
	 */
	public static HtmlPage getPage(String url, WebClient client) throws Exception {
		final HtmlPage entry = client.getPage(url);
		return entry;
	}
	
	/**
	 * Wrapper for {@link WebClient#getPage(WebRequest)}.
	 * @param request the WebRequest
	 * @param client the WebWindow to load the result of the request into
	 * @return the page returned by the server
	 * @throws Exception if an IO error occurs
	 */
	public static HtmlPage getPage(WebRequest request, WebClient client) throws Exception {
		final HtmlPage entry = client.getPage(request);
		return entry;
	}
	
	/**
	 * Produces a {@link HtmlPage} object from the given stored HTML file 
	 * with the given URL.
	 * @param path the path to the html file
	 * @param url the URL that this should be associated with
	 * @return the loaded HtmlPage
	 * @throws IOException if an IO error occurs
	 */
	public static HtmlPage restorePageFromFile(String path, String url) 
			throws IOException {
		String src = new String(Files.readAllBytes(
				Paths.get(path.replaceFirst("^/(.:/)", "$1"))), 
				StandardCharsets.ISO_8859_1);
		StringWebResponse response = new StringWebResponse(src, new URL(url));
		WebClient client = makeWebClient();
		HtmlPage page = HTMLParser.parseHtml(response, client.getCurrentWindow());
		client.close();
		return page;
	}
	
	/**
	 * Saves the specified {@link HtmlPage} into the given directory.
	 * The file path will be the same as the URL path, starting from the 
	 * specified root directory location.
	 * @param html the web page to save
	 * @param directory the root directory location in which the file will be saved
	 * @param images true to save the web page images
	 * @return The absolute path where the file has been stored, or an empty string
	 * if a IO error occurs while saving the page.
	 */
	public static String savePage(HtmlPage html, String directory, boolean images) {
		URL url = html.getUrl();
		StringBuilder pathBuild = new StringBuilder(directory + url.getPath());
		String query;
		if ((query = url.getQuery()) != null)
			pathBuild.append(transformURLQuery(query));
		if (pathBuild.lastIndexOf("/") == pathBuild.length() - 1)
			pathBuild.append("index");
		pathBuild.append(".html");
		String path = pathBuild.toString();
		File file = new File(path);
		try {
			if (images) {
				try {
					html.save(file);
				} catch (IOException e) {
					String originTitle = restorePageFromFile(path, url.toString()).getTitleText();
					String newTitle = html.getTitleText();
					if (!originTitle.equals(newTitle)) {
						path = path.replace(".html", html.getTitleText()).concat(".html");
						html.save(new File(path));
					}
					else return "";
				}
			}
			else
				FileUtils.writeStringToFile(file, html.asXml(), 
						Charset.forName("UTF-8"));
		} catch (IOException e) {
			return "";
		}
		return path;
	}
	
	private static String transformURLQuery(String queryString) {
		String[] query = queryString.split("&");
		StringBuilder subPath = new StringBuilder();
		for (String param : query) {
			subPath.append(param.replaceAll("=|%", "_"));
		}
		return subPath.toString();
	}
	
	/**
	 * Returns true if the given href is a valid internal URL with respect to
	 * the given base url. 
	 * @param base the base url
	 * @param href the anchor href
	 * @return true if the url is valid
	 */
	public static boolean isValidURL(String base, String href) {
		if (href.contains(".jpg")) return false;
		if (href.startsWith("http") || href.startsWith("//")) {
			try {
				String domain1 = 
				InternetDomainName.from(new URL(href).getHost()).topPrivateDomain().toString();
				String domain2 = 
				InternetDomainName.from(new URL(base).getHost()).topPrivateDomain().toString();
				if (!domain1.equals(domain2)) 
					return false;
			} catch (Exception e) {
				/* If any exception occurs while resolving domains, 
				 * check if they are localhost urls 
				 */
				if (!(base.contains("localhost") && href.contains("localhost")))
					return false;
			}
		}
		return !href.contains("javascript") 
				&& !href.contains("crawler") 
				&& !href.contains("@") 
				&& !href.contains("#");
	}

}
