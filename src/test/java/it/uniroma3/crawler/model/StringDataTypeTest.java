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
		stringType.setXPath("//h1/text()");
		String string = stringType.extract(page);
		assertEquals("Detail page 1", string);
	}
	
	@Test
	public void testExtract_notFound() {
		DataType stringType = new StringDataType(); 
		stringType.setXPath("//h1/a");
		String string = stringType.extract(page);
		assertEquals("", string);
	}

}
