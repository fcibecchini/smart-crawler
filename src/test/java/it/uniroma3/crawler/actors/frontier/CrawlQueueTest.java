package it.uniroma3.crawler.actors.frontier;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.csvreader.CsvReader;

import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;

import static it.uniroma3.crawler.factories.CrawlURLFactory.getCrawlUrl;

public class CrawlQueueTest {
	private String storage;
	private CrawlQueue queue;
	private PageClass pclass, pclass2, pclass3;
	
	@Before
	public void setUp() {
		storage = "src/main/resources/storage/queue.csv";
		Website website = new Website("http://localhost",0,0,false);
		pclass = new PageClass("class1",website);
		pclass2 = new PageClass("class2",website);
		pclass3 = new PageClass("class3",website);
		pclass.setDepth(0);
		pclass2.setDepth(1);
		pclass3.setDepth(2);
		pclass.addPageClassLink("//a", pclass2);
		pclass2.addPageClassLink("//a", pclass3);
	}
	
	@After
	public void tearDown() throws IOException {
		queue.deleteStorage();
	}
	
	@Test
	public void testIsEmpty() {
		queue = new CrawlQueue(1);
		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/test",pclass);
		
		assertTrue(queue.isEmpty());
		queue.add(curl1);
		queue.add(curl2);
		assertFalse(queue.isEmpty());
	}
	
	@Test
	public void testSize() {
		queue = new CrawlQueue(2);
		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/test2",pclass);
		CrawlURL curl3 = getCrawlUrl("http://localhost/test3",pclass);
		CrawlURL curl4 = getCrawlUrl("http://localhost/test4",pclass);
		CrawlURL curl5 = getCrawlUrl("http://localhost/test5",pclass);
		
		queue.add(curl1);
		queue.add(curl2);
		queue.add(curl3);
		queue.add(curl4);
		queue.add(curl5);
		assertEquals(5, queue.size());
		queue.next();
		queue.next();
		assertEquals(3, queue.size());
		queue.next();
		assertEquals(2, queue.size());
	}

	@Test
	public void testAdd_allInMemory() {
		queue = new CrawlQueue(2);

		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/test",pclass);
		
		queue.add(curl1);
		queue.add(curl2);
		
		assertEquals(queue.size(), 2);
	}
	
	@Test
	public void testAdd_duplticateUrls() {
		queue = new CrawlQueue(2);

		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl3 = getCrawlUrl("http://localhost/",pclass);

		queue.add(curl1);
		queue.add(curl2);
		assertEquals(queue.size(), 1);

		queue.next();
		queue.add(curl3);
		assertEquals(queue.size(), 0);
	}
	
	@Test
	public void testAdd_urlsWithQuery() {
		queue = new CrawlQueue(4);

		CrawlURL curl1 = getCrawlUrl("http://localhost/directory",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/directory?query=true&test=1",pclass);
		CrawlURL curl3 = getCrawlUrl("http://localhost/directory#fragment",pclass);

		assertTrue(queue.add(curl1));
		assertTrue(queue.add(curl2));
		assertFalse(queue.add(curl3));
	}
	
	@Test
	public void testAdd_storeOnFile() throws IOException {
		queue = new CrawlQueue(2);

		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/test",pclass);
		CrawlURL curl3 = getCrawlUrl("http://localhost/directory",pclass);
		
		queue.add(curl1);
		queue.add(curl2);
		queue.add(curl3);
		
		CsvReader reader = new CsvReader(storage, '\t');
		reader.readRecord();
		assertEquals(curl2.getRelativeUrl(), reader.get(0));
		assertEquals(pclass.getName(), reader.get(1));
		reader.close();
	}
	
	@Test
	public void testNext_allInMemory() {
		queue = new CrawlQueue(2);

		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/test",pclass2);
		CrawlURL curl3 = getCrawlUrl("http://localhost/directory",pclass2);
		
		queue.add(curl2);
		queue.add(curl1);
		
		assertEquals(curl1,queue.next());
		assertEquals(curl2,queue.next());
		
		queue.add(curl3);
		
		assertEquals(curl3,queue.next());
	}
	
	@Test
	public void testNext_retrieveFromFile() throws IOException {
		queue = new CrawlQueue(1);

		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/test",pclass2);
		CrawlURL curl3 = getCrawlUrl("http://localhost/directory",pclass);
		CrawlURL curl4 = getCrawlUrl("http://localhost/index",pclass3);
		
		queue.add(curl1);
		queue.add(curl2);
		queue.add(curl4);
		queue.add(curl3);
		
		/* When max=1 policy is FIFO */
		assertEquals(curl1,queue.next());
		assertEquals(curl2,queue.next());
		assertEquals(curl4,queue.next());
		assertEquals(curl3,queue.next());
	}
	
	@Test
	public void testNext_retrieveFromFile2() {
		queue = new CrawlQueue(4);

		CrawlURL curl1 = getCrawlUrl("http://localhost",pclass);
		CrawlURL curl2 = getCrawlUrl("http://localhost/2",pclass2);
		CrawlURL curl3 = getCrawlUrl("http://localhost/3",pclass);
		CrawlURL curl4 = getCrawlUrl("http://localhost/4",pclass3);
		
		CrawlURL curl5 = getCrawlUrl("http://localhost/5",pclass3);
		CrawlURL curl6 = getCrawlUrl("http://localhost/6",pclass3);
		CrawlURL curl7 = getCrawlUrl("http://localhost/7",pclass3);
		CrawlURL curl8 = getCrawlUrl("http://localhost/8",pclass3);
		
		CrawlURL curl9 = getCrawlUrl("http://localhost/9",pclass2);
		CrawlURL curl10 = getCrawlUrl("http://localhost/10",pclass2);
		CrawlURL curl11 = getCrawlUrl("http://localhost/11",pclass2);
		CrawlURL curl12 = getCrawlUrl("http://localhost/122",pclass);

		queue.add(curl1);
		queue.add(curl2);
		queue.add(curl4);
		queue.add(curl3);
		
		queue.add(curl5);
		queue.add(curl6);
		queue.add(curl7);
		queue.add(curl8);
		
		queue.add(curl9);
		queue.add(curl10);
		queue.add(curl11);
		queue.add(curl12);
		
		assertEquals(curl1, queue.next());
		assertEquals(curl12, queue.next());
		assertEquals(curl3, queue.next());
		assertEquals(curl10, queue.next());
	}

}
