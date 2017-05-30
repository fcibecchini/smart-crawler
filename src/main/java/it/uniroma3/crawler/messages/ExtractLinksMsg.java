package it.uniroma3.crawler.messages;

import java.util.List;

public class ExtractLinksMsg {
	
	private final String url;
	private final List<String> navigationXPaths;
	
	public ExtractLinksMsg(String url, List<String> navigationXPaths) {
		this.url = url;
		this.navigationXPaths = navigationXPaths;
	}
	
	public List<String> getNavXPaths() {
		return this.navigationXPaths;
	}

	public String getUrl() {
		return url;
	}

}
