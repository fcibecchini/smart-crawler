package it.uniroma3.crawler.messages;

public class SaveMsg {
	private final String url;
	
	public SaveMsg(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

}
