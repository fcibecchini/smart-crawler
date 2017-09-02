package it.uniroma3.crawler.actors;

import static it.uniroma3.crawler.util.Commands.*;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.Props;
import static akka.pattern.PatternsCS.ask;
import akka.testkit.javadsl.TestKit;
import akka.testkit.TestActorRef;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.PageClass;

public class CrawlRepositoryTest {
	private static ActorSystem system;
	private static String domain;
	private static String mirror;
	private static boolean js;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem", 
				ConfigFactory.parseString("nodes { repository0 { host = \"127.0.0.1\", "
						+ "port = 2552, system = \"CrawlSystem\" } }"));
		domain = "http://localhost:8081";
		mirror = "html/localhost:8081_mirror";
		js = false;
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	    Files.deleteIfExists(Paths.get(mirror));
	}
	
	@After
	public void tearDown() throws Exception {
		Files.deleteIfExists(Paths.get("src/main/resources/repository/localhost:8081.csv"));
	}
	
	@Test
	public void testFetchFormUrl() throws Exception {
		FetchMsg fetch = new FetchMsg("https://olfatheque.com/olfatheque.login",
				null, new ArrayList<>(), "login", "https://olfatheque.com", 0 ,true);
		
		SaveMsg save = new SaveMsg("https://olfatheque.com/olfatheque.login");
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repo0");
		
		ask(repo, fetch, Integer.MAX_VALUE);
		ask(repo, save, Integer.MAX_VALUE);
		
		final CompletableFuture<Object> future = 
				ask(repo,
						new ExtractLinksMsg("https://olfatheque.com/olfatheque.login", 
								new ArrayList<>(),
								Arrays.asList("//form[@id=\"connexion_form\"],"
										+ "./div/div/input[@type=\"text\"]:"
										+ "yakazafuda@matchpol.net,"
										+ "./div/div/input[@type=\"password\"]:"
										+ "amadeus"))
						, Integer.MAX_VALUE).toCompletableFuture();
		
		ExtractedLinksMsg response = (ExtractedLinksMsg) future.get();
		Map<String, List<String>> xpath2urls = response.getLinks();
		
		System.out.println(xpath2urls);
	}
	
	@Test
	public void testFetchUrl() throws Exception {
		FetchMsg fetch = new FetchMsg(domain,"home",domain,0,js);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repoA");
		
		final CompletableFuture<Object> future = 
				ask(repo, fetch, 4000).toCompletableFuture();
		
		FetchedMsg response = (FetchedMsg) future.get();
		
		assertEquals(0, response.getResponse());
	}
	
	@Test
	public void testFetchUrl_multiplePages() throws Exception {
		FetchMsg fetch1 = new FetchMsg(domain,"home",domain,0,js);
		FetchMsg fetch2 = new FetchMsg(domain+"/directory1.html","dir1",domain,1,js);

		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repoB");
		
		final CompletableFuture<Object> future1 = 
				ask(repo, fetch1, 4000).toCompletableFuture();
		final CompletableFuture<Object> future2 = 
				ask(repo, fetch2, 4000).toCompletableFuture();
		
		FetchedMsg response1 = (FetchedMsg) future1.get();
		FetchedMsg response2 = (FetchedMsg) future2.get();
		
		assertEquals(0, response1.getResponse());
		assertEquals(0, response2.getResponse());
	}
	
	@Test
	public void testSaveUrl() throws Exception {
		FetchMsg fetch = new FetchMsg(domain,"home",domain,0,js);
		SaveMsg save = new SaveMsg(domain);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repoC");
		
		ask(repo, fetch, 4000);
		
		final CompletableFuture<Object> future = 
				ask(repo, save, 4000).toCompletableFuture();
		
		Short response = (Short) future.get();
		
		assertTrue(SAVED==response);
		File pageFile = new File(mirror+"/index.html");
		File img = new File(mirror+"/index/fake.jpg");
		File indexDir = new File(mirror+"/index");
		
		assertTrue(pageFile.exists());
		assertTrue(indexDir.exists());
		assertTrue(img.exists());
		
		pageFile.delete();
		img.delete();
		indexDir.delete();
	}
	
	@Test
	public void testSaveUrl_multiplePages() throws Exception {
		PageClass details = new PageClass("details",domain);
		String url1 = domain+"/detail2.html";
		String url2 = domain+"/detail3.html";

		FetchMsg fetch1 = new FetchMsg(url1,details.getName(),domain,0,js);
		FetchMsg fetch2 = new FetchMsg(url2,details.getName(),domain,0,js);
		
		SaveMsg save1 = new SaveMsg(url1);
		SaveMsg save2 = new SaveMsg(url2);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repoF");
		
		ask(repo, fetch1, 4000);
		ask(repo, fetch2, 4000);
		
		final CompletableFuture<Object> future1 = 
				ask(repo, save1, 4000).toCompletableFuture();

		final CompletableFuture<Object> future2 = 
				ask(repo, save2, 4000).toCompletableFuture();
		
		Short response1 = (Short) future1.get();
		Short response2 = (Short) future2.get();

		assertTrue(SAVED==response1);
		assertTrue(SAVED==response2);
		new File(mirror+"/detail2.html.html").delete();
		new File(mirror+"/detail3.html.html").delete();
	}
	
	@Test
	public void testExtractLinks() throws Exception {
		String url = domain+"/directory1.html";
		String file = mirror+"/directory1.html.html";
		String detail = "//div[@id='content']/ul/li/a[not(@id)]";
		String next = "//a[@id='page']";
		List<String> xpaths = new ArrayList<>();
		xpaths.add(detail);
		xpaths.add(next);
		
		FetchMsg fetch = new FetchMsg(url,"dir1",domain,0,js);
		SaveMsg save = new SaveMsg(url);
		ExtractLinksMsg extract = new ExtractLinksMsg(url, xpaths);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repoD");
		
		ask(repo, fetch, 4000);
		ask(repo, save, 4000);
		
		final CompletableFuture<Object> future = 
				ask(repo, extract, 4000).toCompletableFuture();
		
		ExtractedLinksMsg response = (ExtractedLinksMsg) future.get();
		Map<String, List<String>> xpath2urls = response.getLinks();
		
		List<String> urlsNext = xpath2urls.get(next);
		List<String> urlsDet = xpath2urls.get(detail);
		
		assertEquals(3, urlsDet.size());
		assertTrue(urlsDet.contains(domain+"/detail1.html"));
		assertTrue(urlsDet.contains(domain+"/detail2.html"));
		assertTrue(urlsDet.contains(domain+"/detail3.html"));
		
		assertEquals(1, urlsNext.size());
		assertEquals(domain+"/directory1next.html", urlsNext.get(0));
		
		new File(file).delete();
	}
	
	@Test
	public void testExtractDataRecord() throws Exception {
		String detail = domain+"/detail1.html";
		String file = mirror+"/detail1.html.html";
		String titleXPath = "//div[@id='content']/h1/text()";
		PageClass details = new PageClass("details",domain);
		details.addData(titleXPath, "string");
		
		FetchMsg fetch = new FetchMsg(detail,details.getName(),domain,0,js);
		SaveMsg save = new SaveMsg(detail);
		ExtractDataMsg extrData = new ExtractDataMsg(detail, details.xPathToData());
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, Props.create(CrawlRepository.class), "repoE");
		
		ask(repo, fetch, 4000).toCompletableFuture();
		ask(repo, save, 4000).toCompletableFuture();
		
		final CompletableFuture<Object> future = 
				ask(repo, extrData, 4000).toCompletableFuture();

		ExtractedDataMsg msg = (ExtractedDataMsg) future.get();
		List<String> record = msg.getRecord();
		
		assertNotNull(record);
		assertEquals(1, record.size());
		assertEquals("Detail page 1", record.get(0));
		
		new File(file).delete();
	}

}
