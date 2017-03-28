package it.uniroma3.crawler.util;

import static org.junit.Assert.*;

import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;

import it.uniroma3.crawler.factories.CrawlURLFactory;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class PersistenceTest {
	private final static String DBNAME = "file.db";
	private final static String QUEUE = "crawls_queue";
	
	private DB db;

	@Before
	public void setUp() throws Exception {
		this.db = DBMaker
		        .fileDB(DBNAME)
		        .fileMmapEnable()
		        .make();
	}
	
	
	@Test
	public void testCrawlURLQueue() {		
		PageClass pClass1 = new PageClass("one");
		PageClass pClass2 = new PageClass("two");
		pClass1.setDepth(0);
		pClass1.setWaitTime(0);
		pClass2.setDepth(1);
		pClass2.setWaitTime(1000);
		pClass1.addPageClassLink("//a[0]", pClass2);
		String url1 = "http://foo.com";
		String url2 = "http://foo.com/section";
		CrawlURL curl1 = CrawlURLFactory.getCrawlUrl(url1, pClass1);
		CrawlURL curl2 = CrawlURLFactory.getCrawlUrl(url2, pClass2);
		curl1.addOutLink(url2, pClass2);
		
		try {
			NavigableSet<CrawlURL> curlsQueue = getQueue(QUEUE);
			
			curlsQueue.add(curl2);
			curlsQueue.add(curl1);
			
			db.commit(); // commit changes
						
			NavigableSet<CrawlURL> persistedQueue = getQueue(QUEUE);
			
			CrawlURL persistedCurl1 = persistedQueue.pollFirst();
			CrawlURL persistedCurl2 = persistedQueue.pollFirst();
			
			assertEquals(persistedCurl1, curl1);
			assertEquals(persistedCurl2, curl2);
			
			assertEquals(pClass1, persistedCurl1.getPageClass());
			assertEquals(pClass2, persistedCurl2.getPageClass());

			assertEquals(pClass2, persistedCurl1.getOutLinkPageClass(url2));
			assertEquals(
					persistedCurl1.getPageClass().getDestinationByXPath("//a[0]"),
					pClass2);

		} finally {
			db.close();
		}
	}
	
	private NavigableSet<CrawlURL> getQueue(String name) {
		return db
				.treeSet(name)
		        .serializer((GroupSerializer<CrawlURL>) Serializer.JAVA)
		        .open();
	}

}
