package it.uniroma3.crawler.settings;

import akka.actor.Extension;
import com.typesafe.config.Config;

public class CrawlerSettings implements Extension {
	public final String modelfile;
	public final String	modelseed;
	public final int modelpages;
	public final int fetchers;
	public final int wait;
	public final int randompause;
	public final int maxfailures;
	public final int failuretime;
	public final int pages;
	public final boolean javascript;

	public CrawlerSettings(Config config) {
		modelfile = config.getString("crawler.modeler.file");
		modelseed = config.getString("crawler.modeler.seed");
		modelpages = config.getInt("crawler.modeler.pages");
		
		fetchers = config.getInt("crawler.crawling.fetchers");
		wait = config.getInt("crawler.crawling.wait");
		randompause = config.getInt("crawler.crawling.randompause");
		maxfailures = config.getInt("crawler.crawling.maxfailures");
		failuretime = config.getInt("crawler.crawling.failuretime");
		pages = config.getInt("crawler.crawling.pages");
		javascript = config.getBoolean("crawler.crawling.javascript");
	}

}