package it.uniroma3.crawler.util;
import static org.junit.Assert.*;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.NamedNodeMap;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import it.uniroma3.crawler.util.HtmlUtils;
import it.uniroma3.crawler.util.XPathUtils;
import it.uniroma3.crawler.model.PageClass;

public class HtmlUtilsTest {
	private WebClient client;
	
	
	@Before
	public void setUp() {
		this.client = HtmlUtils.makeWebClient();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testXPathInfer() throws Exception {
		HtmlPage ansa = HtmlUtils.getPage("http://www.proz.com/translation-companies/", client);
		List<HtmlAnchor> links = (List<HtmlAnchor>) 
				XPathUtils.getByMatchingXPath(ansa, "//a");
		
		Map<String,HashSet<String>> xpaths = new HashMap<>();
		
		for (HtmlAnchor link : links) {
			String xpath = "a";
			DomNode current=link.getParentNode();
			boolean stop = false;
			while (current.getNodeName()!="#document" && !stop) {
				String currentSection = current.getNodeName();
				NamedNodeMap attributes = current.getAttributes();
				if (attributes.getLength()>0 
						&& !currentSection.equals("html")) {
					org.w3c.dom.Node attr = attributes.item(0);
					String attrName = attr.getNodeName();
					String attrValue = attr.getNodeValue();
					currentSection += 
							"[@"+attrName+"='"+attrValue+"'"+"]";
					if (attrName.equals("id")) stop = true;
				}
				xpath = currentSection+"/"+xpath;
				current = current.getParentNode();
			}
			xpath = "//"+xpath;
			if (!xpaths.containsKey(xpath)) 
				xpaths.put(xpath, new HashSet<>());
			String href = link.getHrefAttribute();
			if (!href.contains("javascript") && !href.equals("/"))
				xpaths.get(xpath).add(href);	
		}
		
		List<String> finalXpaths = xpaths.keySet().parallelStream()
		.filter(xp -> xpaths.get(xp).size()>0)
		.sorted((x1, x2) -> xpaths.get(x2).size() - xpaths.get(x1).size())
		.collect(toList());
		
		finalXpaths.forEach(xp -> 
			System.out.println(
					xpaths.get(xp).size()+" "+xp+" -> "+xpaths.get(xp)));

	}

}
