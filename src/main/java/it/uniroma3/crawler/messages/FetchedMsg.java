package it.uniroma3.crawler.messages;

public class FetchedMsg {
	
	private final int response;

	public FetchedMsg(int response) {
		this.response = response;
	}
	
	public int getResponse() {
		return this.response;
	}

}
