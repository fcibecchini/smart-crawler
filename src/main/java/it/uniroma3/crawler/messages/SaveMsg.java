package it.uniroma3.crawler.messages;

public class SaveMsg {
	
	private final String url;
	private final String pClass, domain;
	
	public SaveMsg(String url, String pClass, String domain) {
		this.url = url;
		this.pClass = pClass;
		this.domain = domain;
	}
	
	public String getDomain() {
		return this.domain;
	}
	
	public String getPageClass() {
		return pClass;
	}

	public String getUrl() {
		return url;
	}

}
