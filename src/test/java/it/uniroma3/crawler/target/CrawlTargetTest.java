package it.uniroma3.crawler.target;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.target.CrawlTarget;

public class CrawlTargetTest {
	private String configFile;
	private CrawlTarget target;

	@Before
	public void setUp() throws Exception {
		this.configFile = "scope.csv";
		this.target = new CrawlTarget(configFile);
	}

	@Test
	public void testInitCrawlingTarget_entryPointClass_name() {
		target.initCrawlingTarget();
		PageClass entry = target.getEntryPageClass();
		assertEquals(entry.getName(), "homepage");
	}
	
	@Test
	public void testInitCrawlingTarget_entryPointClass_destination() {
		target.initCrawlingTarget();
		PageClass entry = target.getEntryPageClass();
		String xpath = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		assertEquals(entry.getDestinationByXPath(xpath).getName(), "companies");
	}
	
	@Test
	public void testInitCrawlingTarget_testChain() {
		target.initCrawlingTarget();
		PageClass entry = target.getEntryPageClass();
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		PageClass detailsPageClass = entry.getDestinationByXPath(xpath1).getDestinationByXPath(xpath2);
		assertEquals(detailsPageClass.getName(), "detailsPage");
		assertTrue(detailsPageClass.isDataPage());
	}
	
	@Test
	public void testInitCrawlingTarget_testEndPage() {
		target.initCrawlingTarget();
		PageClass entry = target.getEntryPageClass();
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		String xpath3 = "//tr/td[@align='right']/a";
		PageClass companiesPage = entry.getDestinationByXPath(xpath1);
		PageClass detailsPage = companiesPage.getDestinationByXPath(xpath2);
		PageClass profilePage = detailsPage.getDestinationByXPath(xpath3);
		assertEquals(profilePage.getName(), "profilePage");
		assertTrue(profilePage.isEndPage());
		assertTrue(profilePage.isDataPage());
	}
	
	@Test
	public void testInitCrawlingTarget_testFindPageClass() {
		target.initCrawlingTarget();
		PageClass entry = target.getEntryPageClass();
		List<String> entryXPaths = entry.getNavigationXPaths();
		PageClass companies = entry.getDestinationByXPath(entryXPaths.get(0));
		List<String> companiesXPaths = companies.getNavigationXPaths();
		String next = companiesXPaths.get(0);
		String details = companiesXPaths.get(1);
		
		assertEquals(companies.getName(), "companies");
		assertEquals(companies, companies.getDestinationByXPath(next));
		assertEquals(companies.getDestinationByXPath(details).getName(), "detailsPage");
	}
	
	@Test
	public void testInitCrawlingTarget_testDepthHierarchy() {
		target.initCrawlingTarget();
		PageClass entry = target.getEntryPageClass();
		PageClass companies = entry.getDestinationByXPath("//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']");
		PageClass detPage = companies.getDestinationByXPath("//tr/td[@colspan='2']//h2/a");
		PageClass profPage = detPage.getDestinationByXPath("//tr/td[@align='right']/a");
		
		assertEquals(entry.getDepth(), 0);
		assertEquals(companies.getDepth(), 1);
		assertEquals(detPage.getDepth(), 2);
		assertEquals(profPage.getDepth(), 3);
	}

}
