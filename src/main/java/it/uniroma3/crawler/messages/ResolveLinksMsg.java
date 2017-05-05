package it.uniroma3.crawler.messages;

import java.util.List;
import java.util.Map;

public class ResolveLinksMsg {
	
	private final Map<String, List<String>> outLinks;
	
	public ResolveLinksMsg(Map<String, List<String>> outLinks) {
		this.outLinks = outLinks;
	}
	
	public Map<String, List<String>> getLinks() {
		return this.outLinks;
	}
}
