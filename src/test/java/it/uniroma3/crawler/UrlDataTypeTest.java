package it.uniroma3.crawler;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.UrlDataType;
import it.uniroma3.crawler.util.HtmlUtils;

public class UrlDataTypeTest {
	private WebClient client;
	private HtmlPage page;
	
	@Before
	public void setUp() throws Exception {
		this.client = HtmlUtils.makeWebClient();
		this.page = client.getPage("http://www.proz.com/profile/41164?sp_mode=corp_profile&summary=y");
	}

	@Test
	public void testExtract() {
		DataType urlType = new UrlDataType(); 
		urlType.setXPath("//div[@class='rdbx']/div[@class='contact_column']/a[@target]");
		String website = urlType.extract(page);
		assertEquals(website, "http://www.polilingua.com");
	}

}
