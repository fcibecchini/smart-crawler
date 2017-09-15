package it.uniroma3.crawler.messages;

import java.util.ArrayList;
import java.util.List;

public class ExtractedDataMsg {
	
	private final List<String> record;
	
	public ExtractedDataMsg() {
		this.record = new ArrayList<>();
	}
	
	public ExtractedDataMsg(List<String> record) {
		this.record = record;
	}
	
	public List<String> getRecord() {
		return record;
	}

}
