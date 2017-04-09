package it.uniroma3.crawler.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.StringDataType;
import it.uniroma3.crawler.util.HtmlUtils;

public class StringDataTypeTest {
	private WebClient client;
	private HtmlPage page;
	
	@Before
	public void setUp() throws Exception {
		this.client = HtmlUtils.makeWebClient();
		this.page = client.getPage("http://www.proz.com/profile/1483207?sp_mode=corp_profile&summary=y");
	}

	@Test
	public void testExtract() {
		DataType stringType = new StringDataType(); 
		stringType.setXPath("//h1/text()");
		String string = stringType.extract(page);
		assertEquals("ViewGlobally", string);
	}
	
	@Test
	public void testExtract_notFound() {
		DataType stringType = new StringDataType(); 
		stringType.setXPath("//h1/a");
		String string = stringType.extract(page);
		assertEquals("", string);
	}

}
