package it.uniroma3.crawler.modeler;

import static org.junit.Assert.*;

import org.junit.After;
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

public class DynamicModelerTest {
	static ActorSystem system;

	@BeforeClass
	public static void setup() {
		system = ActorSystem.create("ModelerTest");
	}

	@AfterClass
	public static void tearDownAfterClass() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}
	
	@After
	public void tearDown()  {
//		PageClassService serv = new PageClassService();
//		serv.deleteModel(site, 1);
	}
	
	@Test
	public void testDynamicModeler_localhost() {
		String site = "http://localhost:8081";		
		
		TestKit parent = new TestKit(system);
		SeedConfig conf = new SeedConfig(site,null,null,10,false,0,0,1,true,false);
		ActorRef modeler = parent.childActorOf(Props.create(CrawlModeler.class));
		modeler.tell(new ModelMsg(conf), parent.getRef());
		
		PageClass home = parent.expectMsgClass(parent.duration("60 seconds"), PageClass.class);
		
		String toDirectory = "(//ul[@id=\"menu\"]/li/a)[1]";
		String toNext = "//a[@id=\"page\"]";
		PageClass directory1 = home.getDestinationByXPath(toDirectory);
		
		assertEquals(0, home.getDepth());
		assertTrue(home.getMenuXPaths().contains(toDirectory));

		assertEquals(1, directory1.getDepth());
		assertEquals(directory1, directory1.getDestinationByXPath(toNext));
		assertTrue(directory1.getListXPaths().size() > 0);

	}
	
	/*@Test
	public void testXPath_finer() {
		String site = "http://localhost:8082";	

		TestKit parent = new TestKit(system);
		SeedConfig conf = new SeedConfig(site,null,null,21,false,0,0,1,true);
		ActorRef modeler = parent.childActorOf(Props.create(CrawlModeler.class));
		modeler.tell(new ModelMsg(conf), parent.getRef());
		
		PageClass home = parent.expectMsgClass(parent.duration("60 seconds"), PageClass.class);

	}*/

}
