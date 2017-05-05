package it.uniroma3.crawler.actors;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.csvreader.CsvReader;
import com.gargoylesoftware.htmlunit.WebClient;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import it.uniroma3.crawler.actors.CrawlDataWriter;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlDataWriterTest {
	private static ActorSystem system;
	private static WebClient client;
	private final static String SITE_DIRECTORY = "html/test";
	private final static String PATH = "html/"+SITE_DIRECTORY+"/";
	private final static String RECORD_FILENAME = "record_test";
	private static File csvFile;
	private File curlFile;

	/*
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
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
		new File(PATH).delete();
		csvFile.delete();
	}
	
	
	@Test
	public void testSavePage() throws Exception {
		final Props props = CrawlDataWriter.props(RECORD_FILENAME, SITE_DIRECTORY);
		final TestActorRef<CrawlDataWriter> writerRef = TestActorRef.create(system, props, "writer1");
		
		PageClass home = new PageClass("home");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", home);
		HtmlUtils.getPage(curl.getStringUrl(), client).save(curlFile);
		curl.setFilePath(curlFile.getPath());
		
		akka.pattern.Patterns.ask(writerRef, curl, 3000); // save page
		
		File savedPage = new File(SITE_DIRECTORY+"/"+home.getName()+"/"+home.getName()+"_1.html");
		
		assertTrue(savedPage.exists());
		assertTrue(savedPage.length()>0);
		
		assertTrue(savedPage.delete());
		assertTrue(savedPage.getParentFile().delete());
		assertTrue(savedPage.getParentFile().getParentFile().delete());
	}
	
	@Test
	public void testSaveRecord() throws Exception {
		final Props props = CrawlDataWriter.props(RECORD_FILENAME, SITE_DIRECTORY);
		final TestActorRef<CrawlDataWriter> writerRef = TestActorRef.create(system, props, "writer2");
		
		PageClass home = new PageClass("home");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", home);
		
		String[] record = {"test1","test2","test3"};
		curl.setRecord(record);
		
		akka.pattern.Patterns.ask(writerRef, curl, 3000); // save page and record
		
		csvFile = new File(RECORD_FILENAME+".csv");
		
		assertTrue(csvFile.exists());
		
		CsvReader reader = new CsvReader(RECORD_FILENAME+".csv", '\t');
		while (reader.readRecord()) {
			assertTrue(reader.get(0).equals(record[0]));
			assertTrue(reader.get(1).equals(record[1]));
			assertTrue(reader.get(2).equals(record[2]));
		}
		reader.close();
	}
	
	@Test
	public void testSavePage_cachedPage() throws Exception {
		final Props props = CrawlDataWriter.props(RECORD_FILENAME, SITE_DIRECTORY);
		final TestActorRef<CrawlDataWriter> writerRef = TestActorRef.create(system, props, "writer3");
		
		PageClass home = new PageClass("home");
		String url = new File("html/pennyandsinclair_co_uk_mirror/index.html").toURI().toString();
		CrawlURL curl = CrawlURLFactory.getCrawlUrl(url, home);
		curl.setFilePath(curl.getUrl().getPath());
		
		akka.pattern.Patterns.ask(writerRef, curl, 3000); // save page
		
		File savedPage = new File(SITE_DIRECTORY+"/"+home.getName()+"/"+home.getName()+"_1.html");
		
		assertTrue(savedPage.exists());
		assertTrue(savedPage.length()>0);
		
		assertTrue(savedPage.delete());
		assertTrue(savedPage.getParentFile().delete());
		assertTrue(savedPage.getParentFile().getParentFile().delete());
	}
	 */
}
