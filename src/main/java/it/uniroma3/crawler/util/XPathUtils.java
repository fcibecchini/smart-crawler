package it.uniroma3.crawler.util;

import static it.uniroma3.crawler.util.HtmlUtils.isValidURL;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

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
		if (nodes==null) return nodes;
		/*
		assertNotNull(nodes);		
		assertFalse(nodes.isEmpty());
		/*for(Object node : nodes) {
			assertNotNull(node);
		}*/
		return nodes;
	}
	
	public static List<HtmlAnchor> getAnchors(HtmlPage page, String xpath) {
		final List<HtmlAnchor> anchors = page.getByXPath(xpath);
		return anchors;
	}
	
	public static String submitForm(HtmlPage page, String formXPath) throws IOException {
		String[] xpaths = formXPath.split(",");
		HtmlForm form = (HtmlForm) page.getByXPath(xpaths[0]).get(0);		
		for (int i=1;i<xpaths.length;i++) {
			String[] input = xpaths[i].split(":");
			if (input.length>1) {
				HtmlInput textInput = (HtmlInput) form.getByXPath(input[0]).get(0);
				textInput.setValueAttribute(input[1].replaceAll("\"", ""));
			}
			else {
				HtmlButton button = (HtmlButton) form.getByXPath(input[0]).get(0);
				return button.click().getUrl().toExternalForm();
			}
		}
		return "";
	}
	
	/**
	 * Fills a form from the HTML page, returning a list of (Name-Value) pairs to be used
	 * in a HTTP POST request.
	 * @param page the HtmlPage
	 * @param formXPath a composed XPath containing the form location and how to fill it
	 * @return a list of Name Value pairs
	 * @throws IOException
	 */
	public static List<NameValuePair> getFormParameters(HtmlPage page, String formXPath) throws IOException {
		String[] xpaths = formXPath.split(",");
		HtmlForm form = (HtmlForm) page.getByXPath(xpaths[0]).get(0);
		
		/* Internal API... */
		List<NameValuePair> list = new ArrayList<>(form.getParameterListForSubmit(null));
		
		for (int i=1;i<xpaths.length;i++) {
			String[] input = xpaths[i].split(":");
			HtmlInput textInput = (HtmlInput) form.getByXPath(input[0]).get(0);
			list.add(new NameValuePair(textInput.getNameAttribute(), input[1]));
		}
		return list;
	}
	
	/**
	 * Evaluates the specified XPath-to-link in the HtmlPage specified, 
	 * returning the matching absolute URLs, resolved with the given absolute URL.<br>
	 * URLs not in the same domain as the URL given are also omitted.
	 * @param page the html page containing the DOM
	 * @param xpath the xpath-to-link
	 * @param url the URL to resolve the matching anchors
	 * @return the List of absolute URLs matched by this XPath
	 */
	public static List<String> getAbsoluteInternalURLs(
			HtmlPage page, 
			String xpath, 
			String url) {
		return getAnchors(page, xpath).stream()
				.map(HtmlAnchor::getHrefAttribute)
				.filter(l -> isValidURL(url, l))
				.map(href -> getAbsoluteURL(url, href))
				.collect(toList());
	}
	
	/**
	 * Evaluates the specified XPath-to-link in the HtmlPage specified, 
	 * returning the matching absolute URLs, resolved with the given absolute URL.
	 * @param page the html page containing the DOM
	 * @param xpath the xpath-to-link
	 * @param url the URL to resolve the matching anchors
	 * @return the List of absolute URLs matched by this XPath
	 */
	public static List<String> getAbsoluteURLs(
			HtmlPage page, 
			String xpath, 
			String url) {
		return getAnchors(page, xpath).stream()
				.map(HtmlAnchor::getHrefAttribute)
				.map(href -> getAbsoluteURL(url, href))
				.collect(toList());
	}
	
	private static String getAbsoluteURL(String base, String relative) {
		try {
			String url = new URL(new URL(base), relative).toString();
			return (url.endsWith("/")) ? url.substring(0, url.length()-1) : url;
		} catch (MalformedURLException e) {
			return "";
		}
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
