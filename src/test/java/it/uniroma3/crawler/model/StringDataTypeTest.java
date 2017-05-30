package it.uniroma3.crawler.model;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.StringDataType;
import it.uniroma3.crawler.util.HtmlUtils;

public class StringDataTypeTest {
	private static WebClient client;
	private static HtmlPage page;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		client = HtmlUtils.makeWebClient(false);
		page = HtmlUtils.getPage("http://localhost:8081/detail1.html",client);
		client.close();
	}

	@Test
	public void testExtract() {
		DataType stringType = new StringDataType(); 
		String string = stringType.extract(page,"//h1/text()");
		assertEquals("Detail page 1", string);
	}
	
	@Test
	public void testExtract_notFound() {
		DataType stringType = new StringDataType(); 
		String string = stringType.extract(page, "ul/ul/li/a/text()");
		assertEquals("", string);
	}

}
