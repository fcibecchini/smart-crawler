package it.uniroma3.crawler.messages;

public class FetchedMsg {
	
	private final int response;
	private final String url;

	public FetchedMsg(int response) {
		this.response = response;
		this.url = null;
	}
	
	public FetchedMsg(String url, int response) {
		this.response = response;
		this.url = url;
	}
	
	public int getResponse() {
		return this.response;
	}

	public String getUrl() {
		return url;
	}

}
