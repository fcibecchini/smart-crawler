package it.uniroma3.crawler.messages;

public class SaveCacheMsg {
	
	private final String url, pClass, filePath;
	
	public SaveCacheMsg(String url, String pClass, String filePath) {
		this.url = url;
		this.pClass = pClass;
		this.filePath = filePath;
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
