package it.uniroma3.crawler.messages;

import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class ModelMsg {
	private final SeedConfig conf;
	private final String address;
	
	public ModelMsg(SeedConfig conf, String address) {
		this.conf = conf;
		this.address = address;
	}
	
	public ModelMsg(SeedConfig conf) {
		this.conf = conf;
		this.address = "";
	}

	public String getAddress() {
		return address;
	}

	public SeedConfig getConf() {
		return conf;
	}

}
