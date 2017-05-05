package it.uniroma3.crawler.messages;

public class SavedMsg {
	
	private final String filePath;

	public SavedMsg(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePath() {
		return filePath;
	}

}
