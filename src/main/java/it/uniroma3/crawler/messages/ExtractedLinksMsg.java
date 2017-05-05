package it.uniroma3.crawler.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniroma3.crawler.model.OutgoingLink;

public class ExtractedLinksMsg {
	
	private final Map<String, List<OutgoingLink>> outLinks;
	
	public ExtractedLinksMsg() {
		this.outLinks = new HashMap<>();
	}
	
	public ExtractedLinksMsg(Map<String, List<OutgoingLink>> outLinks) {
		this.outLinks = outLinks;
	}
	
	public Map<String, List<OutgoingLink>> getLinks() {
		return this.outLinks;
	}

}
