package it.uniroma3.crawler.messages;

public class StoreURLMsg {
	private final String url;
	private final String pclass;
	
	public StoreURLMsg(String url, String pclass) {
		this.url = url;
		this.pclass = pclass;
	}
	
	public String getURL() {
		return url;
	}
	
	public String getPageClass() {
		return pclass;
	}

}
