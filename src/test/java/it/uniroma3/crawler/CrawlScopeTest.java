package it.uniroma3.crawler;

import static org.junit.Assert.*;

import java.util.Queue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.uniroma3.crawler.page.PageClass;
import it.uniroma3.crawler.scope.CrawlScope;

public class CrawlScopeTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCrawlScope() {
		CrawlScope scope = new CrawlScope("scope.csv");
		Queue<PageClass> pageTypes = scope.getPages();
		assertEquals(pageTypes.size(), 4);
		PageClass homePage = pageTypes.poll();
		PageClass companies = pageTypes.poll();
		PageClass detailsPage = pageTypes.poll();
		PageClass profilePage = pageTypes.poll();
		
		assertEquals(scope.getUrlBase().toString(), "http://www.proz.com/");
		
		assertEquals(homePage.getName(), "homepage");
		assertEquals(companies.getName(), "companies");
		assertEquals(detailsPage.getName(), "detailsPage");
		assertEquals(profilePage.getName(), "profilePage");
		
		assertEquals(homePage.getXPaths().size(), 1);
		assertEquals(detailsPage.getXPaths().size(), 8);
		assertTrue(detailsPage.getXPaths().containsKey("//img[@alt='logo']"));
		assertEquals(detailsPage.getXPaths().get("//img[@alt='logo']").get(0), "logo");
		assertEquals(detailsPage.getXPaths().get("//img[@alt='logo']").get(1), "null");
		
	}

}
