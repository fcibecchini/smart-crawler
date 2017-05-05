package it.uniroma3.crawler.modeler;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;

import org.junit.After;
import org.junit.Test;

import it.uniroma3.crawler.model.PageClass;

public class DynamicModelerTest {
	private final static String TEST_SITE = "http://localhost:8081";

	@Test
	public void testComputeModel() throws Exception {
		URI entry = new URI(TEST_SITE);
		String toDirectory = "(//ul[@id='menu']/li/a)[1]";
		String toNext = "//a[@id='page']";
		WebsiteModeler targetFromWebsite = new DynamicModeler(entry, 200, 0, false);

		PageClass home = targetFromWebsite.computeModel();
		PageClass directory1 = home.getDestinationByXPath(toDirectory);

		assertEquals("class1", home.getName());
		assertEquals(0, home.getDepth());
		assertTrue(home.getMenuXPaths().contains(toDirectory));

		assertEquals(1, directory1.getDepth());
		assertEquals(directory1, directory1.getDestinationByXPath(toNext));
		assertTrue(directory1.getListXPaths().size() > 0);		
	}
	
	@After
	public void tearDown() {
		assertTrue(new File("src/main/resources/targets/localhost_8081_target.csv").delete());
	}

}
