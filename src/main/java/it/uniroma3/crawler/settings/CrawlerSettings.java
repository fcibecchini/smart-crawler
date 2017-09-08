package it.uniroma3.crawler.settings;

import akka.actor.Extension;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

public class CrawlerSettings implements Extension {
	public final List<SeedConfig> seeds;
	public final int fetchers;
	public final int pages;
	public final int frontierheap;
	
	public static class SeedConfig {
		public String site;
		public final String file;
		public final String goldenModel;
		public final int modelPages;
		public final boolean javascript,crawl;
		public final int wait;
		public final int randompause;
		public final int maxfailures;
				
		public SeedConfig(String site, String file, String goldenModel, int pages, 
				boolean js, int wait, int pause, int maxfailures, 
				boolean crawl) {
			this.site = site;
			this.file = file;
			this.goldenModel = goldenModel;
			this.modelPages = pages;
			this.javascript = js;
			this.wait = wait;
			this.randompause = pause;
			this.maxfailures = maxfailures;
			this.crawl = crawl;
		}
	}

	public CrawlerSettings(Config config) {
		ConfigObject sites = config.getObject("crawler.modeler");
		seeds = sites.keySet().stream().map(s -> conf(s,config)).collect(toList());
		fetchers = config.getInt("crawler.crawling.fetchers");
		pages = config.getInt("crawler.crawling.pages");
		frontierheap = config.getInt("crawler.crawling.frontierheap");
	}
	
	private SeedConfig conf(String site, Config conf) {
		String key = site.replaceAll("://|.", "\"$0\"");
		String filep = "crawler.modeler."+key+".static.file";
		String goldenModelp = "crawler.modeler."+key+".golden";
		String pagesp = "crawler.modeler."+key+".dynamic.pages";
		String jsp = "crawler.modeler."+key+".javascript";
		String waitp = "crawler.modeler."+key+".wait";
		String randomp = "crawler.modeler."+key+".randompause";
		String failuresp = "crawler.modeler."+key+".maxfailures";
		String crawlp = "crawler.modeler."+key+".crawl";
		
		String file = (conf.hasPath(filep)) ? conf.getString(filep) : null;
		String goldenModel = (conf.hasPath(goldenModelp)) ? conf.getString(goldenModelp) : null;
		int pages = (conf.hasPath(pagesp)) ? conf.getInt(pagesp) : 0;
		boolean js = (conf.hasPath(jsp)) ? conf.getBoolean(jsp) : false;
		int wait = (conf.hasPath(waitp)) ? conf.getInt(waitp) : 2000;
		int random = (conf.hasPath(randomp)) ? conf.getInt(randomp) : 1000;
		int failures = (conf.hasPath(failuresp)) ? conf.getInt(failuresp) : 1;
		boolean crawl = (conf.hasPath(crawlp)) ? conf.getBoolean(crawlp) : false;
		
		return new SeedConfig(site,file,goldenModel,pages,js,wait,random,failures,crawl);
	}

}