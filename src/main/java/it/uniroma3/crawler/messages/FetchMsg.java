package it.uniroma3.crawler.messages;

public class FetchMsg {
	private final String url;
	private final int id;
	private final boolean js;
		
	public FetchMsg(String url, int id, boolean js) {
		this.url = url;
		this.id = id;
		this.js = js;
	}
	
	public int getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public boolean useJavaScript() {
		return js;
	}

}
