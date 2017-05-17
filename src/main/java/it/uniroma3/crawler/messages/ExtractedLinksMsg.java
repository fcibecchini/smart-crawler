package it.uniroma3.crawler.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtractedLinksMsg {
	
	private final Map<String, List<String>> outLinks;
	
	public ExtractedLinksMsg() {
		this.outLinks = new HashMap<>();
	}
	
	public ExtractedLinksMsg(Map<String, List<String>> outLinks) {
		this.outLinks = outLinks;
	}
	
	public Map<String, List<String>> getLinks() {
		return this.outLinks;
	}

}
