package it.uniroma3.crawler.modeler;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;

public class StaticModelerTest {
	private WebsiteModeler modeler;

	@Before
	public void setUp() throws Exception {
		String file = System.class.getResource("/targets/target_test.csv").getPath();
		Website website = new Website("http://www.proz.com",3,0,false);
		modeler = new StaticModeler(website,2000,file);
		modeler.compute();
	}

	@Test
	public void testComputeModel_entryPointClass_name() {
		PageClass entry = modeler.getEntryPageClass();
		assertEquals("homepage", entry.getName());
	}
	
	@Test
	public void testComputeModel_entryPointClass_destination() {
		PageClass entry = modeler.getEntryPageClass();
		String xpath = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		assertEquals("companies", entry.getDestinationByXPath(xpath).getName());
	}
	
	@Test
	public void testComputeModel_testChain() {
		PageClass entry = modeler.getEntryPageClass();
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		PageClass detailsPageClass = entry.getDestinationByXPath(xpath1).getDestinationByXPath(xpath2);
		assertEquals("detailsPage", detailsPageClass.getName());
		assertTrue(detailsPageClass.isDataPage());
	}
	
	@Test
	public void testComputeModel_testEndPage() {
		PageClass entry = modeler.getEntryPageClass();
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		String xpath3 = "//tr/td[@align='right']/a";
		PageClass companiesPage = entry.getDestinationByXPath(xpath1);
		PageClass detailsPage = companiesPage.getDestinationByXPath(xpath2);
		PageClass profilePage = detailsPage.getDestinationByXPath(xpath3);
		assertEquals("profilePage", profilePage.getName());
		assertTrue(profilePage.isEndPage());
		assertTrue(profilePage.isDataPage());
	}
	
	@Test
	public void testComputeModel_testFindPageClass() {
		PageClass entry = modeler.getEntryPageClass();
		List<String> entryXPaths = entry.getNavigationXPaths();
		PageClass companies = entry.getDestinationByXPath(entryXPaths.get(0));
		List<String> companiesXPaths = companies.getNavigationXPaths();
		String next = companiesXPaths.get(0);
		String details = companiesXPaths.get(1);
		
		assertEquals("companies", companies.getName());
		assertEquals(companies, companies.getDestinationByXPath(next));
		assertEquals("detailsPage", companies.getDestinationByXPath(details).getName());
	}
	
	@Test
	public void testComputeModel_testDepthHierarchy() {
		PageClass entry = modeler.getEntryPageClass();
		PageClass companies = entry.getDestinationByXPath("//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']");
		PageClass detPage = companies.getDestinationByXPath("//tr/td[@colspan='2']//h2/a");
		PageClass profPage = detPage.getDestinationByXPath("//tr/td[@align='right']/a");
		
		assertEquals(0, entry.getDepth());
		assertEquals(1, companies.getDepth());
		assertEquals(2, detPage.getDepth());
		assertEquals(3, profPage.getDepth());
	}
	
	@Test
	public void getByName() {
		PageClass entry = modeler.getEntryPageClass();
		PageClass companies = entry.getDestinationByXPath("//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']");
		PageClass detPage = companies.getDestinationByXPath("//tr/td[@colspan='2']//h2/a");
		PageClass profPage = detPage.getDestinationByXPath("//tr/td[@align='right']/a");

		assertEquals(entry, modeler.getByName("homepage"));
		assertEquals(companies, modeler.getByName("companies"));
		assertEquals(detPage, modeler.getByName("detailsPage"));
		assertEquals(profPage, modeler.getByName("profilePage"));
		assertNull(modeler.getByName("test"));
	}
}
