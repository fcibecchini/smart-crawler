package it.uniroma3.crawler.actors;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import it.uniroma3.crawler.actors.CrawlLinkScheduler;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlLinkSchedulerTest {
	private static ActorSystem system;

	/*
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestKit.shutdownActorSystem(system);
	    system = null;
	}

	
	@Test
	public void testUpdateWaitTime() {
		final Props props = CrawlLinkScheduler.props("http://localhost:8081");
		final TestActorRef<CrawlLinkScheduler> schedulerRef = TestActorRef.create(system, props, "scheduler1");
		
		PageClass home = new PageClass("home");
		PageClass result = new PageClass("result", 0);
		PageClass table = new PageClass("table", 0);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", home);
		
		curl.addOutLink("http://localhost:8081/directory1.html", result);
		curl.addOutLink("http://localhost:8081/directory1next.html", result);
		curl.addOutLink("http://localhost:8081/table.html", table);

		akka.pattern.Patterns.ask(schedulerRef, curl, 3000); // schedule new urls
		
		assertTrue(result.getWaitTime()!=0);
		assertTrue(table.getWaitTime()!=0);
	}
	
	@Test
	public void testFetchedUrls() {
		final Props props = CrawlLinkScheduler.props("http://localhost:8081");
		final TestActorRef<CrawlLinkScheduler> schedulerRef = TestActorRef.create(system, props, "scheduler2");
		CrawlLinkScheduler scheduler = schedulerRef.underlyingActor();
		
		PageClass home = new PageClass("home");
		PageClass result = new PageClass("result", 0);
		PageClass table = new PageClass("table", 0);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", home);
		
		curl.addOutLink("http://localhost:8081/directory1.html", result);
		curl.addOutLink("http://localhost:8081/directory1next.html", result);
		curl.addOutLink("http://localhost:8081/table.html", table);

		akka.pattern.Patterns.ask(schedulerRef, curl, 3000); // schedule new urls
		
		assertTrue(scheduler.getFetchedUrls().contains("http://localhost:8081/table.html"));
		assertTrue(scheduler.getFetchedUrls().contains("http://localhost:8081/directory1.html"));
		assertTrue(scheduler.getFetchedUrls().contains("http://localhost:8081/directory1next.html"));
	}
	*/

}
