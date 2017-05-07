package it.uniroma3.crawler.settings;

import akka.actor.Extension;

import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

public class CrawlerSettings implements Extension {
	public final Map<String,SeedConfig> seeds;
	public final int fetchers;
	public final int pages;
	
	public static class SeedConfig {
		public final String file;
		public final int pages;
		public final boolean javascript;
		public final int wait;
		public final int randompause;
		public final int maxfailures;
		
		public SeedConfig(String file, int pages, boolean js, int wait,
				int pause, int maxfailures) {
			this.file = file;
			this.pages = pages;
			this.javascript = js;
			this.wait = wait;
			this.randompause = pause;
			this.maxfailures = maxfailures;
		}
	}

	public CrawlerSettings(Config config) {
		ConfigObject sites = config.getObject("crawler.modeler");
		
		seeds = getSeeds(config,sites);
		fetchers = config.getInt("crawler.crawling.fetchers");
		pages = config.getInt("crawler.crawling.pages");
	}
	
	private Map<String,SeedConfig> getSeeds(Config config, ConfigObject sites) {
		Map<String,SeedConfig> temp = new HashMap<>();
		for (String site : sites.keySet()) {
			temp.put(site, getSeedConfig(site,config));
		}
		return temp;
	}
	
	private SeedConfig getSeedConfig(String site, Config config) {
		String file = config.getString("crawler.modeler."+site+".file");
		int pages = config.getInt("crawler.modeler."+site+".pages");
		boolean js = config.getBoolean("crawler.modeler."+site+".javascript");
		int wait = config.getInt("crawler.modeler."+site+".wait");
		int random = config.getInt("crawler.modeler."+site+".randompause");
		int failures = config.getInt("crawler.modeler."+site+".maxfailures");
		return new SeedConfig(file,pages,js,wait,random,failures);
	}

}