package it.uniroma3.crawler.messages;

public class FetchMsg {
	private final String url;
	private final int id;
		
	public FetchMsg(String url, int id) {
		this.url = url;
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

}
