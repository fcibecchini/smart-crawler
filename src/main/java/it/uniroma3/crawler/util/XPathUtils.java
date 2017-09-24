package it.uniroma3.crawler.util;

import static it.uniroma3.crawler.util.HtmlUtils.isValidURL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
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

	public static DomNode getFirstByXPath(HtmlPage page, String xpath) {
		return getByMatchingXPath(page, xpath).get(0);
	}

	public static Optional<DomNode> getUniqueByXPath(HtmlPage page, String xpath) {
		final List<DomNode> nodes = getByMatchingXPath(page, xpath);
		return (nodes.size()==1) ? Optional.of(nodes.get(0)) : Optional.empty();
	}
	
	public static List<DomNode> getByMatchingXPath(HtmlPage page, String xpath) {
		return page.getByXPath(xpath);
	}
	
	public static List<HtmlAnchor> getAnchors(HtmlPage page, String xpath) {
		final List<HtmlAnchor> anchors = page.getByXPath(xpath);
		return anchors;
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
	public static List<String> getAbsoluteInternalURLs(HtmlPage page, String xpath, String url) {
		List<String> hrefs = getRelativeURLs(page, xpath);
		hrefs.removeIf(l -> !isValidURL(url, l));
		return getAbsoluteURLs(url, hrefs);
	}
	
	/**
	 * Evaluates the specified XPath-to-link in the HtmlPage specified, 
	 * returning the matching absolute URLs, resolved with the given absolute URL.
	 * @param page the html page containing the DOM
	 * @param xpath the xpath-to-link
	 * @param url the URL to resolve the matching anchors
	 * @return the List of absolute URLs matched by this XPath
	 */
	public static List<String> getAbsoluteURLs(HtmlPage page, String xpath, String url) {
		return getAbsoluteURLs(url, getRelativeURLs(page, xpath));
	}
	
	/**
	 * Evaluates the specified XPath-to-link in the HtmlPage specified, 
	 * returning the matching anchors.
	 * @param page the html page containing the DOM
	 * @param xpath the xpath-to-link
	 * @return the List of anchors matched by this XPath
	 */
	public static List<String> getRelativeURLs(HtmlPage page, String xpath) {
		return getAnchors(page, xpath).stream().map(a -> a.getHrefAttribute()).collect(toList());
	}
	
	/**
	 * Resolve a List of relative URLs into absolute URLs
	 * @param url the URL to resolve the matching anchors
	 * @param hrefs the anchors hrefs
	 * @return the resolved URLs
	 */
	public static List<String> getAbsoluteURLs(String url, List<String> hrefs) {
		return hrefs.stream().map(href -> getAbsoluteURL(url, href)).collect(toList());
	}
	
	/**
	 * Resolve a relative URL into an absolute URL
	 * @param base the URL to resolve the relative one
	 * @param relative
	 * @return the resolved URL
	 */
	public static String getAbsoluteURL(String base, String relative) {
		try {
			String url = new URL(new URL(base), relative).toString();
			return (url.endsWith("/")) ? url.substring(0, url.length()-1) : url;
		} catch (MalformedURLException e) {
			return "";
		}
	}
	
	public static String getAnchorText(HtmlPage page, String xpath) {
		return formatCsv(getAnchors(page, xpath).get(0).getTextContent());
	}
	
	/**
	 * Returns a Set of single words contained in nodes of the DOM.
	 * @param page the html page
	 * @param lengthLimit limit of the word length
	 * @return the matching strings
	 */
	public static Set<String> getSingleWords(HtmlPage page, int lengthLimit) {
		final String findTexts = 
				"//*[not(self::a) and not(parent::a)][text()]"
				+ "[string-length(normalize-space(text()))>0 and"
				+ " string-length(normalize-space(text()))<"+lengthLimit+"]"
				+ "[not(contains(normalize-space(text()),' '))]";
		return getByMatchingXPath(page,findTexts).stream()
				.map(n -> formatCsv(n.getTextContent())).collect(toSet());
	}
	
	public static HtmlPage setInputValue(HtmlPage page, String xpath, String value) {
		getUniqueByXPath(page,xpath).ifPresent(n -> {
			HtmlInput input = (HtmlInput) n;
			input.setValueAttribute(value);
		});
		return page;
	}
	
	public static HtmlPage selectOption(HtmlPage page, String xpath, String value) {
		return getUniqueByXPath(page, xpath).map(n -> {
			HtmlSelect select = (HtmlSelect) n;
			HtmlOption option = (HtmlOption) select.getOptionByValue(value);
			return (HtmlPage) select.setSelectedAttribute(option,true);
		}).orElse(page);
	}

	public static HtmlPage checkRadio(HtmlPage page, String xpath) {
		return getUniqueByXPath(page, xpath).map(n -> {
			HtmlRadioButtonInput radio = (HtmlRadioButtonInput) n;
			return (HtmlPage) radio.setChecked(true);
		}).orElse(page);
	}

	public static HtmlPage setTextArea(HtmlPage page, String xpath, String value) {
		getUniqueByXPath(page, xpath).ifPresent(n -> {
			HtmlTextArea area = (HtmlTextArea) n;
			area.setText(value);
		});
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
		return formatCsv(value);
	}
	
	private static String formatCsv(String s) {
		return (s!=null) ? StringEscapeUtils.escapeCsv(s.replaceAll("(\\s)+", " ")).trim() : "";
	}
	
}
