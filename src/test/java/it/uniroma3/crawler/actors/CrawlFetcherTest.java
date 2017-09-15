package it.uniroma3.crawler.actors;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import it.uniroma3.crawler.actors.CrawlFetcher;
import it.uniroma3.crawler.actors.frontier.CrawlFrontier;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.ModelerService;
import it.uniroma3.crawler.settings.CrawlerSettings;
import it.uniroma3.crawler.settings.Settings;

public class CrawlFetcherTest {
	private static ActorSystem system;
	private static CrawlController controller;
	private static PageClass entryClass;

//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//		system = ActorSystem.create("testSystem");
//		controller = CrawlController.getInstance();
//		
//		String file = CrawlFetcherTest.class.getResource("/targets/localhost_target.csv").getPath();
//		modeler = new StaticModeler(file);
//		controller.setModeler(modeler);
//		entryClass = modeler.computeModel();
//		controller.makeDirectories(modeler);
//		
//		Props props = CrawlRepository.props("repository.csv", false);
//		TestActorRef.create(system, props, "repository");
//	}
//
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//	    TestKit.shutdownActorSystem(system);
//	    system = null;
//	}
//
//	
//	@Test
//	public void testFetchCrawlURL_validPage() throws Exception {
//		final Props props = CrawlFetcher.props(0,0,false,controller.getWriteDir(),controller.getMirror());
//		final TestActorRef<CrawlFetcher> fetcherRef = TestActorRef.create(system, props, "fetcher1");
//		
//		CrawlURL curl = CrawlURLFactory.getCrawlUrl(controller.getUrlBase(), entryClass);
//		
//		ask(fetcherRef, curl, 3000); // fetch this CrawlURl
//	}
	
//	  @Test
//	  //TODO
//	  public void testIt() {
//		new TestKit(system) {
//			{
//				CrawlerSettings s = Settings.SettingsProvider.get(system);
//				controller.setPageClassesWaitTime(2000);
//
//				CrawlURL curl = CrawlURLFactory.getCrawlUrl(controller.seed(modeler), entryClass);
//				final ActorRef frontier = system.actorOf(BFSFrontier.props(s), "frontier");
//
//				final TestKit probe = new TestKit(system);
//
//				within(duration("35 seconds"), () -> {
//					frontier.tell(curl, getRef());
//					frontier.tell("start", getRef());
//					
//					expectNoMsg();
//					return null;
//				});
//			}
//		};
//	}
	
	/*
	@Test
	public void testFetchCrawlURL_unvalidPage() throws Exception {
		final int maxAttempts = 3; // try to re-fetch a url max 3 times 
		final Props props = CrawlFetcher.props(maxAttempts,0,false,controller.getWriteDir(),controller.getMirror());
		final TestActorRef<CrawlFetcher> fetcherRef = TestActorRef.create(system, props, "fetcher2");
		
		PageClass pClass = new PageClass("testClass");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/null", pClass);
		
		ask(fetcherRef, curl, 3000); // fetch this CrawlURL
		
	}
	
	@Test
	public void testFetchCrawlURL_cachedPage() throws Exception {
		final Props props = CrawlFetcher.props(0,0,false,controller.getWriteDir(),controller.getMirror());
		final TestActorRef<CrawlFetcher> fetcherRef = TestActorRef.create(system, props, "fetcher3");
				
		CrawlURL curl = CrawlURLFactory.getCrawlUrl(controller.getCachedUrlBase(), entryClass);
				
		ask(fetcherRef, curl, 3000); // fetch this CrawlURL

	}*/

}
