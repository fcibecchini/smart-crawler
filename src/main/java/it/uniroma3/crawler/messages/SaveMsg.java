package it.uniroma3.crawler.messages;

public class SaveMsg {
	
	private final String url;
	private final String pClass, mirror;
	
	public SaveMsg(String url, String pClass, String mirror) {
		this.url = url;
		this.pClass = pClass;
		this.mirror = mirror;
	}
	
	public String getMirror() {
		return this.mirror;
	}
	
	public String getPageClass() {
		return pClass;
	}

	public String getUrl() {
		return url;
	}

}
