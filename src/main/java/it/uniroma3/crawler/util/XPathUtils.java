package it.uniroma3.crawler.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.gargoylesoftware.htmlunit.html.*;

public class XPathUtils {
	
	public static boolean isMatchingXPath(HtmlPage page, String xpath) {
		return !page.getByXPath(xpath).isEmpty();
	}

	public static HtmlAnchor getAnchorByXPath(HtmlPage page, String xpath) {
		final List<?> anchors = page.getByXPath(xpath);
		assertNotNull(anchors);
		if (anchors.isEmpty()) return null;
		HtmlAnchor firstAnchor = (HtmlAnchor) anchors.get(0);
		assertNotNull(firstAnchor);
		return firstAnchor;
	}

	public static HtmlAnchor getUniqueAnchorByXPath(HtmlPage page, String xpath) {
		final List<?> anchors = page.getByXPath(xpath);
		assertNotNull(anchors);
		assertFalse(anchors.isEmpty());
		HtmlAnchor firstAnchor = (HtmlAnchor) anchors.get(0);
		assertNotNull(firstAnchor);
		return firstAnchor;
	}

	public static DomNode getFirstByXPath(HtmlPage page, String xpath) {
		return (DomNode) getByMatchingXPath(page, xpath).get(0);
	}

	public static DomNode getUniqueByXPath(HtmlPage page, String xpath) {
		final List<?> nodes = page.getByXPath(xpath);
		assertEquals(1, nodes.size());
		return (DomNode) nodes.get(0);
	}

	public static String getUniqueByXPathString(HtmlPage page, String xpath) {
		final List<?> nodes = page.getByXPath(xpath);
		assertEquals(1, nodes.size());
		final DomNode domNode = (DomNode) nodes.get(0);
		return domNode.getTextContent();
	}
	
	public static List<?> getByMatchingXPath(HtmlPage page, String xpath) {
		final List<?> nodes = page.getByXPath(xpath);
		assertNotNull(nodes);
		assertFalse(nodes.isEmpty());
		for(Object node : nodes) {
			assertNotNull(node);
		}
		return nodes;
	}
	
	/*
	public static HtmlPage setInputValue(HtmlPage page, String xpath, String value) {
		HtmlInput input = (HtmlInput) getUniqueByXPath(page,xpath);
		return (HtmlPage) input.setValueAttribute(value);
	}
	*/
	
	public static HtmlPage selectOption(HtmlPage page, String xpath, String value) {
		HtmlSelect select = (HtmlSelect) getUniqueByXPath(page, xpath);
		HtmlOption option = (HtmlOption) select.getOptionByValue(value);
		return select.setSelectedAttribute(option,true);
	}

	public static HtmlPage checkRadio(HtmlPage page, String xpath) {
		HtmlRadioButtonInput radio = (HtmlRadioButtonInput) getUniqueByXPath(page, xpath);
		return (HtmlPage) radio.setChecked(true);
	}

	public static HtmlPage setTextArea(HtmlPage page, String xpath, String value) {
		HtmlTextArea area = (HtmlTextArea) getUniqueByXPath(page, xpath);
		area.setText(value);
		return page;	
	}

	public static String extractByXPath(HtmlPage page, String xpath) {
		return extractByXPath(page, xpath, null);
	}

	public static String extractByXPath(HtmlPage page, String xpath, String defaultValue) {
		final List<?> nodes = page.getByXPath(xpath);
		return extractStringValue(nodes, defaultValue);
	}

	public static String extractStringValue(final Object value) {
		return extractStringValue(Collections.singletonList(value), null);
	}
	public static String extractStringValue(final Object value, String defaultValue) {
		return extractStringValue(Collections.singletonList(value), defaultValue);
	}

	public static String extractStringValue(final List<?> nodes,
			String defaultValue) {
		if (nodes.isEmpty()) return defaultValue;
		StringBuilder result = new StringBuilder();
		for(Object node : nodes) {
			result.append(node.toString());
			result.append(" ");
		}
		String value = result.toString().trim();
		if (value.isEmpty()) return defaultValue;
		return StringEscapeUtils.escapeCsv(value.replaceAll("(\\s)+", " ")).trim();
	}
		
	
}