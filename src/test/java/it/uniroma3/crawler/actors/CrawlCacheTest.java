package it.uniroma3.crawler.actors;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import it.uniroma3.crawler.actors.CrawlCache;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import static it.uniroma3.crawler.util.HtmlUtils.*;

public class CrawlCacheTest {
	private static String directory;
	private static WebClient client;
	private static ActorSystem system;
	private CrawlURL curl;

	/*
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		directory = "html/site_test";
		system = ActorSystem.create("testSystem");
		client = makeWebClient();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestKit.shutdownActorSystem(system);
	    system = null;
	    client.close();
		//deleteAllFiles();
	}
	
	
	private static void deleteAllFiles() throws IOException {
		Path dir = Paths.get(directory);
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
		   @Override
		   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		       Files.delete(file);
		       return FileVisitResult.CONTINUE;
		   }

		   @Override
		   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		       Files.delete(dir);
		       return FileVisitResult.CONTINUE;
		   }
		});
	}

	@Test
	public void testSavePageMirror_homepage() throws Exception {
		final Props props = CrawlCache.props(directory,new ArrayList<ActorRef>());
		final TestActorRef<CrawlCache> cacheRef = TestActorRef.create(system, props, "cache1");
		
		PageClass pClass = new PageClass("testClass");
		curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", pClass);
		curl.setResponse(getWebResponse(curl.getStringUrl(), client));

		akka.pattern.Patterns.ask(cacheRef, curl, 3000);
		
		HtmlPage response = restorePageFromFile(curl.getFilePath(), curl.getUrl());
		
		assertNull(curl.getResponse());
		assertEquals(directory+"/index.html", curl.getFilePath());
		assertEquals("Homepage", response.getTitleText());
	}
	
	@Test
	public void testSavePageMirror_remotePage() throws Exception {
		final Props props = CrawlCache.props(directory,new ArrayList<ActorRef>());
		final TestActorRef<CrawlCache> cacheRef = TestActorRef.create(system, props, "cache2");
		
		PageClass pClass = new PageClass("testClass");
		curl = CrawlURLFactory.getCrawlUrl("http://www.ansa.it/sito/notizie/economia/index.shtml", pClass);
		curl.setResponse(getWebResponse(curl.getStringUrl(), client));

		akka.pattern.Patterns.ask(cacheRef, curl, 3000);
		
		HtmlPage response = restorePageFromFile(curl.getFilePath(), curl.getUrl());
		
		assertNull(curl.getResponse());
		assertEquals(directory+"/sito/notizie/economia/index.shtml.html", curl.getFilePath());
		assertEquals("Economia- ANSA.it", response.getTitleText());
	}
	*/
}
