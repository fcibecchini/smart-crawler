package it.uniroma3.crawler.modeler;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class StaticModelerTest {
	private static PageClass root;	
	static ActorSystem system;

	@BeforeClass
	public static void setup() {
		String file = System.class.getResource("/targets/target_test.csv").getPath();
		system = ActorSystem.create("ModelerTest");
		TestKit parent = new TestKit(system);
		String site = "http://www.proz.com";
		SeedConfig conf = new SeedConfig(site,file,0,false,2000,1000,2,true);
		ActorRef modeler = parent.childActorOf(Props.create(CrawlModeler.class));
		modeler.tell(new ModelMsg(conf), parent.getRef());
		root = parent.expectMsgClass(parent.duration("60 seconds"), PageClass.class);
	}

	@AfterClass
	public static void tearDownAfterClass() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}

	@Test
	public void testComputeModel_entryPointClass_name() {
		assertEquals("homepage", root.getName());
	}
	
	@Test
	public void testComputeModel_entryPointClass_destination() {
		String xpath = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		assertEquals("companies", root.getDestinationByXPath(xpath).getName());
	}
	
	@Test
	public void testComputeModel_testChain() {
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		PageClass detailsPageClass = root.getDestinationByXPath(xpath1).getDestinationByXPath(xpath2);
		assertEquals("detailsPage", detailsPageClass.getName());
		assertTrue(detailsPageClass.isDataPage());
	}
	
	@Test
	public void testComputeModel_testEndPage() {
		String xpath1 = "//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']";
		String xpath2 = "//tr/td[@colspan='2']//h2/a";
		String xpath3 = "//tr/td[@align='right']/a";
		PageClass companiesPage = root.getDestinationByXPath(xpath1);
		PageClass detailsPage = companiesPage.getDestinationByXPath(xpath2);
		PageClass profilePage = detailsPage.getDestinationByXPath(xpath3);
		assertEquals("profilePage", profilePage.getName());
		assertTrue(profilePage.isEndPage());
		assertTrue(profilePage.isDataPage());
	}
	
	@Test
	public void testComputeModel_testFindPageClass() {
		List<String> entryXPaths = root.getNavigationXPaths();
		PageClass companies = root.getDestinationByXPath(entryXPaths.get(0));
		List<String> companiesXPaths = companies.getNavigationXPaths();
		String next = companiesXPaths.get(0);
		String details = companiesXPaths.get(1);
		
		assertEquals("companies", companies.getName());
		assertEquals(companies, companies.getDestinationByXPath(next));
		assertEquals("detailsPage", companies.getDestinationByXPath(details).getName());
	}
	
	@Test
	public void testComputeModel_testDepthHierarchy() {
		PageClass companies = root.getDestinationByXPath("//li[@class='dropdown menu-jobs-directories']//a[text()='Companies']");
		PageClass detPage = companies.getDestinationByXPath("//tr/td[@colspan='2']//h2/a");
		PageClass profPage = detPage.getDestinationByXPath("//tr/td[@align='right']/a");
		
		assertEquals(0, root.getDepth());
		assertEquals(1, companies.getDepth());
		assertEquals(2, detPage.getDepth());
		assertEquals(3, profPage.getDepth());
	}
}
