package it.uniroma3.crawler.settings;

import akka.actor.Extension;

import java.util.Map;
import static java.util.stream.Collectors.toMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

public class CrawlerSettings implements Extension {
	public final Map<String,SeedConfig> seeds;
	public final int fetchers;
	public final int pages;
	public final int frontierheap;
	
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
		
		seeds = sites.keySet().stream().collect(toMap(s->s, s -> seedConfig(s,config)));
		fetchers = config.getInt("crawler.crawling.fetchers");
		pages = config.getInt("crawler.crawling.pages");
		frontierheap = config.getInt("crawler.crawling.frontierheap");
	}
	
	private SeedConfig seedConfig(String site, Config config) {
		String key = site.replaceAll("://|.", "\"$0\"");
		String file = config.getString("crawler.modeler."+key+".file");
		int pages = config.getInt("crawler.modeler."+key+".pages");
		boolean js = config.getBoolean("crawler.modeler."+key+".javascript");
		int wait = config.getInt("crawler.modeler."+key+".wait");
		int random = config.getInt("crawler.modeler."+key+".randompause");
		int failures = config.getInt("crawler.modeler."+key+".maxfailures");
		return new SeedConfig(file,pages,js,wait,random,failures);
	}

}