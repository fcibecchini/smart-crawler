package it.uniroma3.crawler.model;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.util.XPathUtils;

public class StringDataType extends DataType {
	
	@Override
	public String extract(Object object) {
		HtmlPage page = (HtmlPage) object;
		String extracted = XPathUtils.extractByXPath(page, getXPath());
		if (extracted != null) return extracted.replaceAll("\"", "");
		return "";
	}

}
