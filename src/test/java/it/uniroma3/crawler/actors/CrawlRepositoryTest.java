package it.uniroma3.crawler.actors;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.actor.Props;
import static akka.pattern.PatternsCS.ask;
import akka.testkit.javadsl.TestKit;
import akka.testkit.TestActorRef;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.OutgoingLink;
import it.uniroma3.crawler.model.PageClass;

public class CrawlRepositoryTest {
	private static ActorSystem system;
	private static String baseurl;
	private static String csv;
	private static String mirror;
	private static boolean js;
	private static Props props;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create("testSystem");
		baseurl = "http://localhost:8081";
		csv = "repository-test.csv";
		mirror = "html/test_mirror";
		js = false;
		props = CrawlRepository.props(csv, js);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	    TestKit.shutdownActorSystem(system);
	    system = null;
		new File(mirror).delete();
	}
	
	@After
	public void tearDown() throws Exception {
		new File(csv).delete();
	}
	
	@Test
	public void testFetchUrl() throws Exception {
		FetchMsg fetch = new FetchMsg("http://localhost:8081",1);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, props, "repoA");
		
		final CompletableFuture<Object> future = 
				ask(repo, fetch, 4000).toCompletableFuture();
		
		FetchedMsg response = (FetchedMsg) future.get();
		
		assertEquals(0, response.getResponse());
	}
	
	@Test
	public void testFetchUrl_multiplePages() throws Exception {
		FetchMsg fetch1 = new FetchMsg(baseurl,1);
		FetchMsg fetch2 = new FetchMsg(baseurl+"/directory1.html",1);

		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, props, "repoB");
		
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
		FetchMsg fetch = new FetchMsg(baseurl,1);
		SaveMsg save = new SaveMsg(baseurl, "class1", mirror);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, props, "repoC");
		
		ask(repo, fetch, 4000);
		
		final CompletableFuture<Object> future = 
				ask(repo, save, 4000).toCompletableFuture();
		
		SavedMsg response = (SavedMsg) future.get();
		
		assertEquals(mirror+"/index.html", response.getFilePath());
		File pageFile = new File(mirror+"/index.html");
		assertTrue(pageFile.exists());
		
		pageFile.delete();
	}
	
	@Test
	public void testSaveUrl_multiplePages() throws Exception {
		PageClass details = new PageClass("details");
		String url1 = baseurl+"/detail2.html";
		String url2 = baseurl+"/detail3.html";

		FetchMsg fetch1 = new FetchMsg(url1,1);
		FetchMsg fetch2 = new FetchMsg(url2,1);
		
		SaveMsg save1 = new SaveMsg(url1, details.getName(), mirror);
		SaveMsg save2 = new SaveMsg(url2, details.getName(), mirror);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, props, "repoF");
		
		ask(repo, fetch1, 4000);
		ask(repo, fetch2, 4000);
		
		final CompletableFuture<Object> future1 = 
				ask(repo, save1, 4000).toCompletableFuture();

		final CompletableFuture<Object> future2 = 
				ask(repo, save2, 4000).toCompletableFuture();
		
		SavedMsg response1 = (SavedMsg) future1.get();
		SavedMsg response2 = (SavedMsg) future2.get();

		File pageFile1 = new File(response1.getFilePath());
		File pageFile2 = new File(response2.getFilePath());
		assertTrue(pageFile1.exists());
		assertTrue(pageFile2.exists());
		
		pageFile1.delete();
		pageFile2.delete();
	}
	
	@Test
	public void testExtractLinks() throws Exception {
		String url = baseurl+"/directory1.html";
		String file = mirror+"/directory1.html.html";
		String detail = "//div[@id='content']/ul/li/a[not(@id)]";
		String next = "//a[@id='page']";
		List<String> xpaths = new ArrayList<>();
		xpaths.add(detail);
		xpaths.add(next);
		
		FetchMsg fetch = new FetchMsg(url,1);
		SaveMsg save = new SaveMsg(url, "class1", mirror);
		ExtractLinksMsg extract = new ExtractLinksMsg(url, file, baseurl, mirror, xpaths);
		
		final TestActorRef<CrawlRepository> repo = 
				TestActorRef.create(system, props, "repoD");
		
		ask(repo, fetch, 4000);
		ask(repo, save, 4000);
		
		final CompletableFuture<Object> future = 
				ask(repo, extract, 4000).toCompletableFuture();
		
		ExtractedLinksMsg response = (ExtractedLinksMsg) future.get();
		Map<String, List<OutgoingLink>> xpath2urls = response.getLinks();
		
		List<String> urlsNext = xpath2urls.get(next).stream().map(ol -> ol.getUrl()).collect(toList());
		List<String> urlsDet = xpath2urls.get(detail).stream().map(ol -> ol.getUrl()).collect(toList());
		
		assertEquals(3, urlsDet.size());
		assertTrue(urlsDet.contains(baseurl+"/detail1.html"));
		assertTrue(urlsDet.contains(baseurl+"/detail2.html"));
		assertTrue(urlsDet.contains(baseurl+"/detail3.html"));
		
		assertEquals(1, urlsNext.size());
		assertEquals(baseurl+"/directory1next.html", urlsNext.get(0));
		
		new File(file).delete();
	}
	
	@Test
	public void testExtractDataRecord() throws Exception {
		String detail = baseurl+"/detail1.html";
		String file = mirror+"/detail1.html.html";
		String titleXPath = "//div[@id='content']/h1/text()";
		PageClass details = new PageClass("details");
		details.addData(titleXPath, "string");
		List<DataType> data = new ArrayList<>();
		data.add(details.getDataTypeByXPath(titleXPath));
		
		FetchMsg fetch = new FetchMsg(detail,1);
		SaveMsg save = new SaveMsg(detail, details.getName(), mirror);
		ExtractDataMsg extrData = new ExtractDataMsg(detail, file, baseurl, data);
		
		final TestActorRef<CrawlRepository> repo = TestActorRef.create(system, props, "repoE");
		
		ask(repo, fetch, 4000).toCompletableFuture();
		ask(repo, save, 4000).toCompletableFuture();
		
		final CompletableFuture<Object> future = 
				ask(repo, extrData, 4000).toCompletableFuture();

		String[] record = (String[]) future.get();
		
		assertNotNull(record);
		assertEquals(1, record.length);
		assertEquals("Detail page 1", record[0]);
		
		new File(file).delete();
	}
	


}
