package it.uniroma3.crawler.messages;

import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class ModelMsg {
	private final String rootDirectory;
	private final String site;
	private final SeedConfig seedConfig;
	
	public ModelMsg(String root, String site, SeedConfig seed) {
		this.rootDirectory = root;
		this.site = site;
		this.seedConfig = seed;
	}

	public String getRootDirectory() {
		return rootDirectory;
	}

	public String getSite() {
		return site;
	}

	public SeedConfig getSeedConfig() {
		return seedConfig;
	}

}
