package it.uniroma3.crawler.model;

import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@NodeEntity(label="url")
public class UrlDataType extends DataType {

	@Override
	public String extract(HtmlPage page, String xpath) {
		final List<?> nodes = page.getByXPath(xpath);
		if (nodes.isEmpty()) return "";
		HtmlAnchor targetSite = (HtmlAnchor) nodes.get(0);
		return targetSite.getHrefAttribute();
	}

}
