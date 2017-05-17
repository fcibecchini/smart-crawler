package it.uniroma3.crawler.messages;

import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class ModelMsg {
	private final String site;
	private final SeedConfig seedConfig;
	
	public ModelMsg(String site, SeedConfig seed) {
		this.site = site;
		this.seedConfig = seed;
	}

	public String getSite() {
		return site;
	}

	public SeedConfig getSeedConfig() {
		return seedConfig;
	}

}
