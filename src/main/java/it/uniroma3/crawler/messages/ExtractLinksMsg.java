package it.uniroma3.crawler.messages;

import java.util.ArrayList;
import java.util.List;

public class ExtractLinksMsg {
	
	private final String url;
	private final List<String> navigationXPaths;
	private final List<String> formXPaths;
	
	public ExtractLinksMsg(String url, List<String> navigationXPaths) {
		this.url = url;
		this.navigationXPaths = navigationXPaths;
		this.formXPaths = new ArrayList<>();
	}
	
	public ExtractLinksMsg(String url, List<String> navigationXPaths, List<String> formXPaths) {
		this.url = url;
		this.navigationXPaths = navigationXPaths;
		this.formXPaths = formXPaths;
	}
	
	public List<String> getNavXPaths() {
		return this.navigationXPaths;
	}
	
	public List<String> getFormXPaths() {
		return formXPaths;
	}

	public String getUrl() {
		return url;
	}

}
