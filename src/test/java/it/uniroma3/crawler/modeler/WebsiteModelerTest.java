package it.uniroma3.crawler.modeler;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.WebSiteModeler;

public class WebsiteModelerTest {
	private final static String TEST_SITE = "http://localhost:8081";
	private WebSiteModeler targetFromFile;

	@Before
	public void setUp() throws Exception {
		String file = WebsiteModelerTest.class.getResource("/targets/target_test.csv").getPath();
		this.targetFromFile = new WebSiteModeler(file);
	}

	@Test
	public void testInitCrawlingTarget_entryPointClass_name() {
		targetFromFile.initCrawlingTarget();
		PageClass entry = targetFromFile.getEntryPageClass();
		assertEquals(entry.getName(), "homepage");
	}
	
	@Test
	public void testInitCrawlingTarget_entryPointClass_destination() {
		targetFromFile.initCrawlingTarget();
		PageClass entry = targetFromFile.getEntryPageClass();
		String xpath = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		assertEquals(entry.getDestinationByXPath(xpath).getName(), "companies");
	}
	
	@Test
	public void testInitCrawlingTarget_testChain() {
		targetFromFile.initCrawlingTarget();
		PageClass entry = targetFromFile.getEntryPageClass();
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		PageClass detailsPageClass = entry.getDestinationByXPath(xpath1).getDestinationByXPath(xpath2);
		assertEquals(detailsPageClass.getName(), "detailsPage");
		assertTrue(detailsPageClass.isDataPage());
	}
	
	@Test
	public void testInitCrawlingTarget_testEndPage() {
		targetFromFile.initCrawlingTarget();
		PageClass entry = targetFromFile.getEntryPageClass();
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
		targetFromFile.initCrawlingTarget();
		PageClass entry = targetFromFile.getEntryPageClass();
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
		targetFromFile.initCrawlingTarget();
		PageClass entry = targetFromFile.getEntryPageClass();
		PageClass companies = entry.getDestinationByXPath("//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']");
		PageClass detPage = companies.getDestinationByXPath("//tr/td[@colspan='2']//h2/a");
		PageClass profPage = detPage.getDestinationByXPath("//tr/td[@align='right']/a");
		
		assertEquals(entry.getDepth(), 0);
		assertEquals(companies.getDepth(), 1);
		assertEquals(detPage.getDepth(), 2);
		assertEquals(profPage.getDepth(), 3);
	}
	
	@Test
	public void testComputeWebsiteTarget() {
		try {
			URI entry = new URI(TEST_SITE);
			String toDirectory = "(//ul[@id='menu']/li/a[not(@id)])[1]";
			String toNext = "//a[@id='page']";
			WebSiteModeler targetFromWebsite = new WebSiteModeler(entry, false);
			
			PageClass home = targetFromWebsite.computeModel(200, 3, 0.2, 0);
			PageClass directory1 = home.getDestinationByXPath(toDirectory);
			
			assertEquals(home.getName(), "class1");
			assertEquals(home.getDepth(), 0);
			
			assertEquals(directory1.getDepth(), 1);
			assertEquals(directory1.getDestinationByXPath(toNext), directory1);

		}
		catch (URISyntaxException e) {}
	}
	

}
