package it.uniroma3.crawler.model;

import org.apache.commons.lang3.StringUtils;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.util.XPathUtils;

public class ImgDataType extends DataType {

	@Override
	public String extract(Object object) {
		HtmlPage page = (HtmlPage) object;
		String logo = StringUtils.substringBefore(
				StringUtils.substringAfterLast(
						XPathUtils.extractByXPath(page, getXPath()),"/"),">");
		return logo.replaceAll("\"", "");
	}

}
