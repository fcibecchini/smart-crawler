package it.uniroma3.crawler.actors;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.actors.CrawlExtractor;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.StaticModeler;
import it.uniroma3.crawler.modeler.WebsiteModeler;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlExtractorTest {
	private static ActorSystem system;
	private static WebClient client;
	private static CrawlController controller;
	private static WebsiteModeler modeler;
	private File curlFile;

	/*
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
		modeler = new StaticModeler("");
		controller = CrawlController.getInstance();
		controller.setModeler(modeler);
		client = HtmlUtils.makeWebClient();
	}
	
	@Before
	public void setUp() {
		curlFile = new File("test.html");
	}
	
	@After
	public void tearDown() {
		curlFile.delete();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestKit.shutdownActorSystem(system);
	    system = null;
	    client.close();
	}

	
	@Test
	public void testSetOutLinks_listNotEmpty() throws Exception {
		String urlBase = "http://localhost:8081";
		modeler.setUrlBase(URI.create(urlBase));
		
		final Props props = CrawlExtractor.props(urlBase,controller.getMirror());
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor1");
		
		PageClass result = new PageClass("result");
		PageClass detail = new PageClass("detail");
		result.addPageClassLink("//div[@id='content']/ul/li/a[not(@id)]", detail);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/directory1.html", result);
		HtmlUtils.getPage(curl.getStringUrl(), client).save(curlFile);
		curl.setFilePath(curlFile.getPath());
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract links
		
		List<String> outLinks = curl.getOutLinks();
				
		for (String link : outLinks) {
			assertTrue(link.contains("http://localhost:8081/detail"));
			assertEquals(detail, curl.getOutLinkPageClass(link));
		}
		assertEquals(3, outLinks.size());
	}
	
	@Test
	public void testSetOutLinks_listEmpty() throws Exception {
		String urlBase = "http://localhost:8081";
		modeler.setUrlBase(URI.create(urlBase));
		
		final Props props = CrawlExtractor.props(urlBase,controller.getMirror());
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor2");
		
		PageClass result = new PageClass("result");
		PageClass detail = new PageClass("detail");
		result.addPageClassLink("//test/a", detail);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/directory1.html", result);
		HtmlUtils.getPage(curl.getStringUrl(), client).save(curlFile);
		curl.setFilePath(curlFile.getPath());
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract links
		
		List<String> outLinks = curl.getOutLinks();
		
		assertEquals(0, outLinks.size());
	}
	
	@Test
	public void testSetOutLinks_cachedPage() throws Exception {	
		String urlBase = "http://www.pennyandsinclair.co.uk";
		modeler.setUrlBase(URI.create(urlBase));
		
		final Props props = CrawlExtractor.props(urlBase, controller.getMirror());
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor3");
		
		PageClass home = new PageClass("home");
		PageClass result = new PageClass("result");
		home.addPageClassLink("(//div[@id='wrap']/header/div[@class]/div[@class]/div[@class]/nav/div/ul/li/div[@class]/ul/li/a[@target])[17]", result);
		CrawlURL curl = CrawlURLFactory.getCrawlUrl(controller.getCachedEntryPoint(), home);
		curl.setFilePath(curl.getUrl().getPath());
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract links
		
		List<String> outLinks = curl.getOutLinks();
		String link = outLinks.get(0);
		
		String url = new File("html/pennyandsinclair_co_uk_mirror/searchcategory_1listingtype_6statusids_1,2,6,7,8obc_Priceobd_Descendingofficeids_4,5,6,7.html").toURI().toString();
		assertEquals(url, link);
		assertEquals(result, curl.getOutLinkPageClass(link));
		assertEquals(1, outLinks.size());
	}
	
	@Test
	public void testSetDataRecord() throws Exception {
		String urlBase = "http://localhost:8081";
		modeler.setUrlBase(URI.create(urlBase));
		
		final Props props = CrawlExtractor.props(urlBase,controller.getMirror());
		final TestActorRef<CrawlExtractor> extractorRef = TestActorRef.create(system, props, "extractor4");
		
		PageClass table = new PageClass("table");		
		table.addData("//table/tbody/tr/td[not(a)]/text()", "string");
		
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/table.html", table);
		HtmlUtils.getPage(curl.getStringUrl(), client).save(curlFile);
		curl.setFilePath(curlFile.getPath());
		
		akka.pattern.Patterns.ask(extractorRef, curl, 3000); // extract data
		
		String[] record = curl.getRecord();
		
		assertNotNull(record);
		assertEquals(1, record.length);
		assertTrue(record[0].contains("Item 1"));
	}
	*/
	
}
