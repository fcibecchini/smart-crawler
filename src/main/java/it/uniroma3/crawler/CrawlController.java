package it.uniroma3.crawler;

import java.util.Queue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import it.uniroma3.crawler.fetch.EntryFetcher;
import it.uniroma3.crawler.page.PageClass;

public class CrawlController {
    private static CrawlController instance = null;
	private static final int RND_PAUSE = 1000;
	private static final int PAUSE = 2000;

    private CrawlController() {}

    public static synchronized CrawlController getInstance() {
        if (instance == null) {
        	instance = new CrawlController();
        }
        return instance;
    }
    
    public void fetchRequests(String urlBase, PageClass homePage, Queue<PageClass> classes) {
    	// ActorSystem is a heavy object: create only one per application
    	final ActorSystem system = ActorSystem.create("CrawlSystem");
    	final ActorRef entryFetcher = system.actorOf(EntryFetcher.props(urlBase, classes), "entryFetcher");
    	
    	final Inbox inbox = Inbox.create(system);
    	inbox.send(entryFetcher, homePage);
    	
    }

}
