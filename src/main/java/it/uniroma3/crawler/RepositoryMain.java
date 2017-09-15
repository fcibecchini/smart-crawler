package it.uniroma3.crawler;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;

public class RepositoryMain {

	public static void main(String[] args) {
		if (args.length<1) {
			System.out.println("Start CrawlRepository remote ActorSystem:\n"
					+ "Usage: <id> (ActorSystem id number)");
			System.exit(1);
		}
		String id = args[0];
		Config conf = ConfigFactory.load("repository"+id);
		ActorSystem.create("RepositorySystem"+id, conf);
	}

}
