package it.uniroma3.crawler.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.model.DataType;
import it.uniroma3.crawler.model.ImgDataType;
import it.uniroma3.crawler.util.HtmlUtils;

public class ImgDataTypeTest {
	private WebClient client;
	private HtmlPage page;
	
	@Before
	public void setUp() throws Exception {
		this.client = HtmlUtils.makeWebClient();
		this.page = client.getPage("http://www.proz.com/profile/1483207?sp_mode=corp_profile&summary=y");
	}

	@Test
	public void testExtract() {
		DataType imgType = new ImgDataType(); 
		imgType.setXPath("//img[@alt='logo']");
		String logo = imgType.extract(page);
		assertEquals(logo, "1483207_r55bf38586a5d1.jpg");
	}

}
