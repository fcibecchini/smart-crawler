package it.uniroma3.crawler.messages;

public class SaveCacheMsg {
	
	private final String domain, url, pClass, filePath;
	
	public SaveCacheMsg(String domain, String url, String pClass, String filePath) {
		this.domain = domain;
		this.url = url;
		this.pClass = pClass;
		this.filePath = filePath;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public String getPageClass() {
		return pClass;
	}

}
