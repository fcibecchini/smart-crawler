package it.uniroma3.crawler.messages;

public class FetchMsg {
	private final String url;
	private final String pclass;
	private final String domain;
	private final int id;
	private final boolean js;
		
	public FetchMsg(String url, String pclass, String domain, int id, boolean js) {
		this.url = url;
		this.pclass = pclass;
		this.domain = domain;
		this.id = id;
		this.js = js;
	}
	
	public int getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}
	
	public String getPageClass() {
		return pclass;
	}
	
	public String getDomain() {
		return domain;
	}

	public boolean useJavaScript() {
		return js;
	}

}
