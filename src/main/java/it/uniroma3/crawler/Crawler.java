package it.uniroma3.crawler;

import static it.uniroma3.crawler.util.Commands.*;

import java.util.logging.Logger;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
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
		
		ActorRef controller = system.actorOf(Props.create(CrawlController.class), "controller");
		final Inbox inbox = Inbox.create(system);
		inbox.send(controller, START);
		try {
			Await.result(system.whenTerminated(), Duration.Inf());
			logger.fine("Terminating crawling");
		} catch (Exception e) {
			logger.warning("Failed while waiting termination");
		}
	}
}
