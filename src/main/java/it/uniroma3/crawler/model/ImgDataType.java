package it.uniroma3.crawler.model;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.annotation.NodeEntity;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.util.XPathUtils;

@NodeEntity(label="img")
public class ImgDataType extends DataType {

	@Override
	public String extract(HtmlPage page, String xpath) {
		String extracted = XPathUtils.extractByXPath(page, xpath);
		if (extracted != null)
			return StringUtils.substringBefore(
					StringUtils.substringAfterLast(extracted,"/"),">")
					.replaceAll("\"", "");
		return "";
	}

}
