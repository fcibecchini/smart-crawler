package it.uniroma3.crawler.actors.fetch;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlFetcherTest {
	private static ActorSystem system;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	    JavaTestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	public void testFetchCrawlURL_validPage() throws Exception {
		final Props props = CrawlFetcher.props(
				new ArrayList<ActorRef>(),0,0,false);
		final TestActorRef<CrawlFetcher> fetcherRef = TestActorRef.create(system, props, "fetcher1");
		
		PageClass pClass = new PageClass("testClass");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", pClass);
		
		akka.pattern.Patterns.ask(fetcherRef, curl, 3000); // fetch this CrawlURL

		HtmlPage response = curl.getPageContent();
		assertNotNull(response);
		assertEquals(response.getUrl(), curl.getUrl().toURL());
		assertEquals(response.getTitleText(), "Homepage");
	}
	
	@Test
	public void testFetchCrawlURL_unvalidPage() throws Exception {
		final int maxAttempts = 3; // try to re-fetch a url max 3 times 
		final Props props = CrawlFetcher.props(
				new ArrayList<ActorRef>(),maxAttempts,0,false);
		final TestActorRef<CrawlFetcher> fetcherRef = TestActorRef.create(system, props, "fetcher2");
		
		PageClass pClass = new PageClass("testClass");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/null", pClass);
		
		akka.pattern.Patterns.ask(fetcherRef, curl, 3000); // fetch this CrawlURL
		
		HtmlPage response = curl.getPageContent();
		assertNull(response);
	}

}
