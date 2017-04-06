package it.uniroma3.crawler.actors.extract;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlExtractorTest {
	private static ActorSystem system;
	private static String urlBase;
	private static WebClient client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
		urlBase = "http://localhost:8081";
		client = HtmlUtils.makeWebClient();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	    JavaTestKit.shutdownActorSystem(system);
	    system = null;
	    client.close();
	}

	@Test
	public void testSetOutLinks_listNotEmpty() throws Exception {
		final Props props = CrawlExtractor.props(urlBase);
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor1");
		
		PageClass result = new PageClass("result");
		PageClass detail = new PageClass("detail");
		result.addPageClassLink("//div[@id='content']/ul/li/a[not(@id)]", detail);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/directory1.html", result);
		curl.setPageContent(HtmlUtils.getPage(curl.getStringUrl(), client));
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract links
		
		List<String> outLinks = curl.getOutLinks();
				
		for (String link : outLinks) {
			assertTrue(link.contains("http://localhost:8081/detail"));
			assertEquals(curl.getOutLinkPageClass(link), detail);
		}
		assertEquals(outLinks.size(), 3);
	}
	
	@Test
	public void testSetOutLinks_listEmpty() throws Exception {
		final Props props = CrawlExtractor.props(urlBase);
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor2");
		
		PageClass result = new PageClass("result");
		PageClass detail = new PageClass("detail");
		result.addPageClassLink("//test/a", detail);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/directory1.html", result);
		curl.setPageContent(HtmlUtils.getPage(curl.getStringUrl(), client));
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract links
		
		List<String> outLinks = curl.getOutLinks();
		
		assertEquals(outLinks.size(), 0);
	}
	
	@Test
	public void testSetDataRecord() throws Exception {
		final Props props = CrawlExtractor.props(urlBase);
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor3");
		
		PageClass table = new PageClass("table");		
		table.addData("//table/tbody/tr/td[not(a)]/text()", "string");
		
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/table.html", table);
		curl.setPageContent(HtmlUtils.getPage(curl.getStringUrl(), client));
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract data
		
		String[] record = curl.getRecord();
		
		assertNotNull(record);
		assertEquals(record.length, 1);
		assertTrue(record[0].contains("Item 1"));
	}

}
