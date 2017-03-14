package it.uniroma3.crawler.model;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.util.XPathUtils;

public class StringDataType extends DataType {
	
	@Override
	public String extract(Object object) {
		HtmlPage page = (HtmlPage) object;
		String string = XPathUtils.extractByXPath(page, getXPath());
		return string.replaceAll("\"", "");
	}

}
