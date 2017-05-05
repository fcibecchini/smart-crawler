package it.uniroma3.crawler.actors.frontier;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class BFSFrontierTest {
	private static ActorSystem system;

	/*
	@BeforeClass
	public static void setUp() throws Exception {
		system = ActorSystem.create("testSystem");
	}
	
	@AfterClass
	public static void cleanUp() throws Exception {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	
	@Test
	public void testScheduleUrl() throws Exception {
		final Props props = BFSFrontier.props(1);
		final TestActorRef<BFSFrontier> frontierRef = TestActorRef.create(system, props, "frontier1");
		BFSFrontier frontier = frontierRef.underlyingActor();		
		
		PageClass pClass = new PageClass("testClass");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://test.com", pClass);
		
		akka.pattern.Patterns.ask(frontierRef, curl, 3000);

		assertFalse(frontier.isEmpty());
	}
	
	@Test
	public void testTerminalCondition() throws Exception {
		final int maxPages = 1;
		final Props props = BFSFrontier.props(maxPages);
		final TestActorRef<BFSFrontier> frontierRef = TestActorRef.create(system, props, "frontier2");
		BFSFrontier frontier = frontierRef.underlyingActor();		

		PageClass pClass = new PageClass("testClass");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://test.com", pClass);
		
		akka.pattern.Patterns.ask(frontierRef, curl, 3000);
		akka.pattern.Patterns.ask(frontierRef, "next", 3000);
		
		akka.pattern.Patterns.ask(frontierRef, "next", 3000);
		
		assertTrue(frontier.isEnding());
	}
	
	@Test
	public void testNext() throws Exception {
		final int maxPages = 1;
		final Props props = BFSFrontier.props(maxPages);
		final TestActorRef<BFSFrontier> frontierRef = TestActorRef.create(system, props, "frontier3");
		BFSFrontier frontier = frontierRef.underlyingActor();		

		PageClass pClass = new PageClass("testClass");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://test.com", pClass);
		
		akka.pattern.Patterns.ask(frontierRef, curl, 3000);
		akka.pattern.Patterns.ask(frontierRef, "next", 3000);
		
		assertTrue(frontier.isEmpty());
	}


	*/
}
