package it.uniroma3.crawler.model;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.ImgDataType;
import it.uniroma3.crawler.util.HtmlUtils;

public class ImgDataTypeTest {
	private static WebClient client;
	private static HtmlPage page;
	
	@BeforeClass
	public static void setUp() throws Exception {
		client = HtmlUtils.makeWebClient(false);
		page = client.getPage("http://localhost:8081");
		client.close();
	}

	@Test
	public void testExtract() {
		DataType imgType = new ImgDataType(); 
		imgType.setXPath("//img[@alt='logo']");
		String logo = imgType.extract(page);
		assertEquals("fake.jpg", logo);
	}

}
