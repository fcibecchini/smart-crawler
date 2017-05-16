package it.uniroma3.crawler;

import static it.uniroma3.crawler.util.Commands.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import it.uniroma3.crawler.actors.CrawlController;

public class MasterMain {
	
	public static void main(String[] args) {
		MasterMain crawler = new MasterMain();
		crawler.crawl();
	}
		
	public void crawl() {
		Config conf = ConfigFactory.load("master");
		final ActorSystem system = ActorSystem.create("CrawlSystem",conf);
		ActorRef controller = system.actorOf(Props.create(CrawlController.class), "controller");
		final Inbox inbox = Inbox.create(system);
		inbox.send(controller, START);
	}
}
