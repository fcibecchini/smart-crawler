package it.uniroma3.crawler.model;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.UrlDataType;
import it.uniroma3.crawler.util.HtmlUtils;

public class UrlDataTypeTest {
	private static WebClient client;
	private static HtmlPage page;
	
	@BeforeClass
	public static void setUp() throws Exception {
		client = HtmlUtils.makeWebClient(false);
		page = HtmlUtils.getPage("http://localhost:8081",client);
		client.close();
	}

	@Test
	public void testExtract() {
		DataType urlType = new UrlDataType(); 
		String website = urlType.extract(page, "//div[@id='link']/a");
		assertEquals("http://www.external-link.test", website);
	}

}
