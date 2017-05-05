package it.uniroma3.crawler.messages;

import java.util.List;

public class ExtractLinksMsg {
	
	private final String url;
	private final String htmlPath;
	private final String baseUrl;
	private final String mirror;
	private final List<String> navigationXPaths;
	
	public ExtractLinksMsg(String url, String htmlPath, String baseUrl, String mirror, List<String> navigationXPaths) {
		this.url = url;
		this.htmlPath = htmlPath;
		this.baseUrl = baseUrl;
		this.mirror = mirror;
		this.navigationXPaths = navigationXPaths;
	}
	
	public List<String> getNavXPaths() {
		return this.navigationXPaths;
	}
	
	public String getMirror() {
		return this.mirror;
	}

	public String getHtmlPath() {
		return htmlPath;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getUrl() {
		return url;
	}

}
