package it.uniroma3.crawler.model;

import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class UrlDataType extends DataType {

	@Override
	public String extract(Object object) {
		HtmlPage page = (HtmlPage) object;
		final List<?> nodes = page.getByXPath(getXPath());
		if (nodes.isEmpty()) return null;
		HtmlAnchor targetSite = (HtmlAnchor) nodes.get(0);
		return targetSite.getHrefAttribute();
	}

}
