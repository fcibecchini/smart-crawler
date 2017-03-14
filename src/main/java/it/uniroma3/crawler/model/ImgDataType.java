package it.uniroma3.crawler.model;

import org.apache.commons.lang3.StringUtils;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.util.XPathUtils;

public class ImgDataType extends DataType {

	@Override
	public String extract(Object object) {
		HtmlPage page = (HtmlPage) object;
		String extracted = XPathUtils.extractByXPath(page, getXPath());
		if (extracted != null)
			return StringUtils.substringBefore(
					StringUtils.substringAfterLast(extracted,"/"),">")
					.replaceAll("\"", "");
		return "";
	}

}
