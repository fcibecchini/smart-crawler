package it.uniroma3.crawler.util;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
	
	/**
	 * Builds a XPath matching the specified {@link HtmlAnchor}, starting from a unique element
	 * in the current DOM. Unique elements are {@link DomNode} with an "id" attribute
	 * and the HTML root node.<br>
	 * For the anchor node, the resulting XPath includes all its attributes names, 
	 * or the id name and value only if present. <br>
	 * For following nodes, it includes the first attribute name only if present.<br>
	 * For instance, the following Anchor and DOM:
	 * <pre>
	 * {@code <a href="/detail1.html">}<br>
	 * {@code
	 * <html>
	 * <body>
	 *  <div id="main">
	 *    <div id="site_content">
	 *      <div id="content">
	 *        <ul>
	 *         <li><a href="/detail1.html">Detail page 1</a></li>
	 *         <li><a href="/detail2.html">Detail page 2</a></li>
	 *         <li><a href="/detail3.html">Detail page 3</a></li>
	 *        </ul>
	 *      </div>
	 *    </div>
	 *  </div>
	 * </body>
	 * </html>
	 * }
	 * </pre>
	 * will produce: {@code //div[@id='content']/ul/li/a}
	 * @param link the HTML anchor
	 * @return a String XPath matching the anchor
	 */
	public static String getXPathTo(HtmlAnchor link) {
		String anchor = anchor(link);
		if (anchor.startsWith("//")) 
			return anchor;
		
		ArrayDeque<String> stack = new ArrayDeque<>();
		stack.push(anchor);
		DomNode node = link.getParentNode();
		boolean stop = false;
		while (node.getNodeName()!="#document" && !stop) {
			StringBuilder query = new StringBuilder(node.getNodeName());
			NamedNodeMap attrs = node.getAttributes();
			if (attrs.getLength()>0 && !query.equals("html")) {
				Node attr = attrs.item(0);
				String name = attr.getNodeName();
				if (name.equals("id")) {
					stop = true;
					query.append("[@"+name+"='"+attr.getNodeValue()+"'"+"]");
				}
				else 
					query.append("[@"+name+"]");
			}
			stack.push(query.toString());
			node = node.getParentNode();
		}
		
		StringBuilder xpath = new StringBuilder("//");
		while (!stack.isEmpty()) {
			xpath.append(stack.pop());
			if (!stack.isEmpty())
				xpath.append("/");
		}
		return xpath.toString();
	}
	
	private static String anchor(HtmlAnchor link) {
		NamedNodeMap attrs = link.getAttributes();
		int n = attrs.getLength();
		if (n<=1) return "a";
		
		Queue<String> query = new LinkedList<>();
		for (int i=0; i<=n-1; i++) {
			Node attr = attrs.item(i);
			String name = attr.getNodeName();
			if (name.equals("id"))
				return "//a[@"+name+"='"+attr.getNodeValue()+"']";
			else if (!name.equals("href") && !name.contains(":"))
				query.add("@"+name);
		}
		if (query.isEmpty()) return "a";
		
		StringBuilder anchor = new StringBuilder("a[");
		while (!query.isEmpty()) {
			anchor.append(query.poll());
			if (!query.isEmpty())
				anchor.append(" and ");
		}
		return anchor+"]";
	}
	
}
