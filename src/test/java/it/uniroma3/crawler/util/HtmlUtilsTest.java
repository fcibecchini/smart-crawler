package it.uniroma3.crawler.util;
import static org.junit.Assert.*;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
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
	
	private boolean externalDomain(String base, String href) {
		return (href.contains("http") && !href.contains(base));
	}
	
	private Set<String> getSchema(Map<String, Set<String>> xpath2Links) {
		return xpath2Links.keySet().stream()
				.sorted((x1, x2) -> xpath2Links.get(x2).size() - xpath2Links.get(x1).size())
				.collect(toSet());
	}
	
	private List<Set<String>> getLinksCollections(Map<String, Set<String>> xpath2Links) {
		return xpath2Links.keySet().stream()
				.map(xpath2Links::get).collect(toList());
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Set<String>> extractSchema(String base, Set<String> visitedUrls, HtmlPage page) {
		Map<String,Set<String>> xpath2Links = new HashMap<>();

		List<HtmlAnchor> links = (List<HtmlAnchor>) 
				XPathUtils.getByMatchingXPath(page, "//a");
		
		for (HtmlAnchor link : links) {
			String xpath = "a";
			DomNode current=link.getParentNode();
			boolean stop = false;
			while (current.getNodeName()!="#document" && !stop) {
				String currentSection = current.getNodeName();
				NamedNodeMap attributes = current.getAttributes();
				if (attributes.getLength()>0 && !currentSection.equals("html")) {
					org.w3c.dom.Node attr = attributes.item(0);
					String attrName = attr.getNodeName();
					if (attrName.equals("id")) 	stop = true;
					String attrValue = attr.getNodeValue();
					currentSection += "[@"+attrName+"='"+attrValue+"'"+"]";
				}
				xpath = currentSection+"/"+xpath;
				current = current.getParentNode();
			}
			xpath = "//"+xpath;
			String href = link.getHrefAttribute();

			if (!href.contains("javascript") && !externalDomain(base,href) 
					&& !visitedUrls.contains(href)) {
				visitedUrls.add(href);
				if (!xpath2Links.containsKey(xpath)) 
					xpath2Links.put(xpath, new HashSet<>());
				xpath2Links.get(xpath).add(href);
			}
		}
		return xpath2Links;
	}
	
	private double distance(String g1, String g2, 
			Map<String, List<Map<String, Set<String>>>> group2schema) {

		Set<String> g1Schema = getSchema(group2schema.get(g1).get(0));
		Set<String> g2Schema = getSchema(group2schema.get(g2).get(0));

		Set<String> union = new HashSet<>(g1Schema);
		union.addAll(g2Schema);

		Set<String> diff1 = new HashSet<>(union);
		Set<String> diff2 = new HashSet<>(union);
		diff1.removeAll(g2Schema);
		diff2.removeAll(g1Schema);

		Set<String> unionDiff = new HashSet<>(diff1);
		unionDiff.addAll(diff2);

		return unionDiff.size() / union.size();
	}
	
	@Test
	public void computeModelTest() {
		int test = 0;
		int c = 1;
		String base = "http://www.ansa.it";
		String url = "/sito/notizie/topnews/index.shtml";
		
		int n = 10;
		double dt = 0.2;
		
		Set<String> lcSeed = new HashSet<>();
		Set<String> visitedUrls = new HashSet<>();
		Queue<Set<String>> queueQ = new PriorityQueue<>((lc1, lc2) -> lc2.size() - lc1.size());
		
		// Map<String, List<Map<String, Set<String>>>> = page classes model
		// String = page class name
		// List<Map<String, Set<String>>> = xpath 2 links Collection for each page added to the class...
		// Map<String, Set<String>> = xpath 2 links Collection
		// String = xpath
		// Set<String> = Links Collection [url1,url2,..]
		Map<String, List<Map<String, Set<String>>>> model = new HashMap<>();
		
		Map<String, Set<String>> modelClass2PageUrls = new HashMap<>();
		
		visitedUrls.add(url);
		lcSeed.add(url);
		queueQ.add(lcSeed);
		
		while (!queueQ.isEmpty()) {
			Set<String> lc = queueQ.poll();
			Set<HtmlPage> fetchedW = new HashSet<>();
			int counter = 0;
			for (String lcUrl : lc) {
				try {
					HtmlPage body = HtmlUtils.getPage(base+lcUrl, client);
					fetchedW.add(body);
					Thread.sleep(1000); // wait..!!
					counter++;
					if (counter==n) break;
				} catch (Exception e) {}
			}
			
			// Candidate class selection
			
			Map<String, List<Map<String, Set<String>>>> group2schema = new HashMap<>();
			for (HtmlPage page : fetchedW) {
				Map<String, Set<String>> schema2Lc = extractSchema(base, visitedUrls, page);
				Set<String> pageSchema = getSchema(schema2Lc);
				boolean foundMatch = false;
				for (String group : group2schema.keySet()) {
					Set<String> schema = getSchema(group2schema.get(group).get(0));
					if (pageSchema.stream().allMatch(sc -> schema.contains(sc))) {
						// add page to the current Group
						group2schema.get(group).add(schema2Lc);
						foundMatch = true;
						
						if (!modelClass2PageUrls.containsKey(group)) 
							modelClass2PageUrls.put(group, new HashSet<>());
						modelClass2PageUrls.get(group).add(page.getUrl().toString());
						
						break;
					}	
				}
				if (!foundMatch) { // new Group
					String groupName = "class"+(c++);
					group2schema.put(groupName, new ArrayList<>());
					group2schema.get(groupName).add(schema2Lc);
					
					if (!modelClass2PageUrls.containsKey(groupName)) 
						modelClass2PageUrls.put(groupName, new HashSet<>());
					modelClass2PageUrls.get(groupName).add(page.getUrl().toString());

				}
			}
			List<String> ordGroups = group2schema.keySet().stream()
			.sorted((co1,co2) -> group2schema.get(co2).size() - group2schema.get(co1).size())
			.collect(toList());
			
			/* Collapse similar groups */
			
			Map<String, List<Map<String, Set<String>>>> class2Schema = new HashMap<>();
			class2Schema.putAll(group2schema);
			
			Set<String> keysToRemove = new HashSet<>();
			for (int i=0;i<ordGroups.size();i++) {
				for (int j=ordGroups.size()-1; j>i; j--) {
					String gi = ordGroups.get(i);
					String gj = ordGroups.get(j);
					if (distance(gi, gj, group2schema) < dt) {
						class2Schema.get(gi).addAll(group2schema.get(gj));						
						modelClass2PageUrls.get(gi).addAll(modelClass2PageUrls.get(gj));
					}
				}
			}
			
			keysToRemove.stream().forEach(k -> {class2Schema.remove(k);modelClass2PageUrls.remove(k);});
			
			// Model update phase 
			//TODO
//			for (String candidate : class2Schema.keySet()) {
//				for (String pClass : model.keySet()) {
//					Map<String, List<Map<String, Set<String>>>> tempModel = new HashMap<>();
//					tempModel.putAll(model);
//					tempModel.get(pClass).addAll(class2Schema.get(candidate));
//					
//				}
//			}
			
			model.putAll(class2Schema);
			
			// Insert discovered links Collections into queue
			queueQ.addAll(class2Schema.keySet().stream()
			.map(key -> class2Schema.get(key))
			.flatMap(List::stream)
			.map(xpath2Links -> getLinksCollections(xpath2Links))
			.flatMap(List::stream)
			.distinct().collect(toList()));
			
			System.out.println(++test);
			if (test==20) {
				System.out.println("Break");
				break;
			}
			
		}
		
		for (String pClass : model.keySet()) {
			System.out.println(pClass+": "+modelClass2PageUrls.get(pClass));
			Map<String, Set<String>> mergedSchema = new HashMap<>();
			model.get(pClass).forEach(schemaMap -> mergedSchema.putAll(schemaMap));
			System.out.println("SCHEMAS AND LINKS");
			mergedSchema.keySet()
			.forEach(k -> {
				System.out.println("\t"+k+": "); 
				System.out.println("\t"+mergedSchema.get(k));
				});
		}
		

	}
	
	//TODO
	private double MDL(Map<String, List<Map<String, Set<String>>>> model,
			Map<String, Set<String>> modelClass2PageUrls) {
		
		/* Encoding the model */
		/* Number of the xpaths for each class */
		long modelCost = 0;
		for (String pClass : model.keySet()) {
			modelCost += model.get(pClass).stream()
					.map(mymap -> mymap.keySet())
					.flatMap(Set::stream)
					.distinct().count();
		}
		
		/* Encoding the data with the help of the model */
		long dataCost = 0;
		
		
		return modelCost+dataCost;
	}

}
