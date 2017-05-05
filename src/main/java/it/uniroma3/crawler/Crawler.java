package it.uniroma3.crawler;

import java.util.logging.Logger;

import akka.actor.ActorSystem;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class Crawler {
	
	public static void main(String[] args) {
		Crawler crawler = new Crawler();
		crawler.crawl();
	}
		
	public void crawl() {
		final Logger logger = Logger.getLogger(Crawler.class.getName());
		final ActorSystem system = ActorSystem.create("CrawlSystem");
		CrawlerSettings settings = Settings.SettingsProvider.get(system);
		CrawlController.getInstance().startCrawling(system, settings);
		try {
			Await.result(system.whenTerminated(), Duration.Inf());
			logger.fine("Terminating crawling");
		} catch (Exception e) {
			logger.warning("Failed while waiting termination");
		}
	}
}
