package it.uniroma3.crawler.actors.write;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.csvreader.CsvReader;
import com.gargoylesoftware.htmlunit.WebClient;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.HtmlUtils;

public class CrawlDataWriterTest {
	private static ActorSystem system;
	private static WebClient client;
	private final static String SITE_DIRECTORY = "test";
	private final static String PATH = "html/"+SITE_DIRECTORY+"/";
	private final static String RECORD_FILENAME = "record_test";
	private File savedPage, csvFile;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
		client = HtmlUtils.makeWebClient();
	}
	
	@After
	public void tearDown() throws Exception {
		if (csvFile!=null && csvFile.exists()) csvFile.delete();
		savedPage.delete();
		savedPage.getParentFile().delete();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	    JavaTestKit.shutdownActorSystem(system);
	    system = null;
	    client.close();
		new File(PATH).delete();
	}

	@Test
	public void testSavePage() throws Exception {
		final Props props = CrawlDataWriter.props(RECORD_FILENAME, SITE_DIRECTORY);
		final TestActorRef<CrawlDataWriter> writerRef = TestActorRef.create(system, props, "writer1");
		
		PageClass home = new PageClass("home");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", home);
		curl.setPageContent(HtmlUtils.getPage(curl.getStringUrl(), client));
		
		akka.pattern.Patterns.ask(writerRef, curl, 3000); // save page
		
		savedPage = new File(PATH+home.getName()+"/"+home.getName()+"_1.html");
		
		assertTrue(savedPage.exists());
		assertTrue(savedPage.length()!=0);
	}
	
	@Test
	public void testSaveRecord() throws Exception {
		final Props props = CrawlDataWriter.props(RECORD_FILENAME, SITE_DIRECTORY);
		final TestActorRef<CrawlDataWriter> writerRef = TestActorRef.create(system, props, "writer2");
		
		PageClass home = new PageClass("home");
		CrawlURL curl = CrawlURLFactory.getCrawlUrl("http://localhost:8081/", home);
		curl.setPageContent(HtmlUtils.getPage(curl.getStringUrl(), client));
		String[] record = {"test1","test2","test3"};
		curl.setRecord(record);
		
		akka.pattern.Patterns.ask(writerRef, curl, 3000); // save page and record
		
		savedPage = new File(PATH+home.getName()+"/"+home.getName()+"_1.html");
		csvFile = new File(RECORD_FILENAME+".csv");
		
		assertTrue(savedPage.exists());
		assertTrue(savedPage.length()!=0);
		assertTrue(csvFile.exists());
		
		CsvReader reader = new CsvReader(RECORD_FILENAME+".csv", '\t');
		while (reader.readRecord()) {
			assertTrue(reader.get(0).equals(record[0]));
			assertTrue(reader.get(1).equals(record[1]));
			assertTrue(reader.get(2).equals(record[2]));
		}
	}

}
