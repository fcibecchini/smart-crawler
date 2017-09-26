package it.uniroma3.crawler.modeler.util;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import it.uniroma3.crawler.model.ClassLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.model.ModelPageClass;
import it.uniroma3.crawler.modeler.model.Page;
import it.uniroma3.crawler.modeler.model.WebsiteModel;
import it.uniroma3.crawler.modeler.model.XPath;

import static java.util.stream.Stream.concat;

/**
 * Utility class to calculate the MDL cost of a WebsiteModel
 *
 */
public class ModelCostCalculator {
	
	/**
	 * Weight for encoding of URLs
	 */
	private final double C_U = 1;
		
	/**
	 * Weight for encoding of XPaths indexes
	 */
	private final double C_I = 0.8;
	
	/**
	 * Weight for encoding of XPaths
	 */
	private final double C_XP = 1;
	
	/**
	 * map of inverse document frequency value for each XPath 
	 */
	private Map<XPath,Double> xpath2IDF;
	
	private Map<Page,Set<XPath>> page2schema;
	
	/**
	 * Constructs a Model cost Calculator based on the given collection of Pages
	 * @param pages
	 */
	public ModelCostCalculator(Collection<Page> pages) {
		this.page2schema = pages.stream().collect(toMap(p->p, Page::getSchema));
		this.xpath2IDF = page2schema.values().stream().flatMap(Set::stream).distinct()
				.collect(toMap(xp->xp, this::idf));
	}
	
	private double idf(XPath xp) {
		double df = page2schema.values().stream().filter(s->s.contains(xp)).count();
		return Math.log(page2schema.size() / df);
	}

	/**
	 * Calculates the Minimum Description Length cost of this {@link WebsiteModel}. <br>
	 * cost of model = cost of the encoding of each {@link ModelPageClass} schema <br>
	 * cost of data = cost of encoding of each {@link Page} of each class <br>
	 * MDL = cost of model + cost of data
	 * @param model the website model
	 * @return the MDL cost
	 */
	public double cost(WebsiteModel model) {
		return model.getClasses().stream().mapToDouble(this::cost).sum();
	}
	
	/**
	 * Calculates the cost of encoding of a {@link ModelPageClass}.
	 * @param c the ModelPageClass
	 * @return the class cost
	 */
	public double cost(ModelPageClass c) {
		Set<XPath> links = c.getLinkSchema();
		Set<XPath> labels = c.getLabelSchema();
		return xpathsCost(links.stream(), c, C_XP) + xpathsCost(labels.stream(), c, C_XP) +
				c.getPages().stream().mapToDouble(p -> pageCost(p,c,links,labels)).sum();
	}
	
	/**
	 * Page cost function for an instance p of Class C <br>
	 * <br>
	 * indexesLinks = 	XPaths-to-links of p in C's schema <br>
	 * indexesLabels = 	XPaths-to-labels of p in C's schema <br>
	 * urlsSize = 		URLs in p <br>
	 * missingLinks =  	XPaths-to-link present in C but missing in p <br>
	 * missingLabels = 	XPaths-to-labels present in C but missing in p <br>
	 * <br>
	 * @return cost(p|c)
	 */
	public double pageCost(Page p, ModelPageClass c, Set<XPath> cLinks, Set<XPath> cLabels) {
		Set<XPath> pLinks = p.getLinkSchema();
		Set<XPath> pLabels = p.getLabelSchema();
		
		double indexesLinks = xpathsCost(pLinks.stream(), c, C_I);
		double missingLinks = xpathsCost(cLinks.stream().filter(xp -> !pLinks.contains(xp)), 
				c, C_XP);
		
		double indexesLabels = xpathsCost(pLabels.stream().filter(cLabels::contains), c, C_I);
		double missingLabels = xpathsCost(cLabels.stream().filter(xp -> !pLabels.contains(xp)), 
				c, C_XP);
		
		return indexesLinks + C_U*p.urlsSize() + missingLinks + indexesLabels + missingLabels;
	}
	
	private double xpathsCost(Stream<XPath> xpaths, ModelPageClass c, double w) {
		return xpaths.mapToDouble(xp -> w/(tfIdf(xp,c)+1)).sum();
	}
	
	/**
	 * Calculates the XPath Frequency in a Class - Inverse Page Frequency of the XPath.
	 * @param xp the XPath
	 * @param c the ModelPageClass
	 * @return the tf-idf
	 */
	public double tfIdf(XPath xp, ModelPageClass c) {
		double tf = 
			c.getPages().stream().filter(p->page2schema.get(p).contains(xp)).count() / c.size();
		return tf * xpath2IDF.get(xp);
	}
		
	/**
	 * The distance between the page class schemas is defined as
	 * the normalized cardinality of the <b>weighted</b> symmetric 
	 * set difference between the two schemas.<br>
	 * Namely, let Gi and Gj be the weighted schemas of groups i and j; then:<br>
	 * <b>distance(Gi,Gj) = [w(Gi-Gj) + w(Gj-Gi)] / w(Gi U Gj)</b> <br>
	 * Note that if Gi = Gj (identical schemas), then distance(Gi,Gj) = 0;<br>
	 * conversely, if Gi ∩ Gj = empty (the schemas are disjoint), 
	 * then distance(Gi,Gj) = 1 <br>
	 * <b>weight(G) = sum of w(xp) for each xp in Gi<br>
	 * w(xp) = tf-idf(xp,c1) + tf-idf(xp,c2)</b>
	 * @return the weighted distance between the two classes
	 * @see {@link ModelCostCalculator#tfIdf(XPath, ModelPageClass)}
	 */
	public double weightedDistance(ModelPageClass c1, ModelPageClass c2) {
		Set<XPath> schema1 = c1.getSchema();
		Set<XPath> schema2 = c2.getSchema();
		
		double diff = schema1.stream().filter(xp->!schema2.contains(xp))
				.mapToDouble(xp -> 1+tfIdf(xp,c1)).sum() +
					  schema2.stream().filter(xp->!schema1.contains(xp))
				.mapToDouble(xp -> 1+tfIdf(xp,c2)).sum();
		double inter = schema1.stream().filter(schema2::contains)
				.mapToDouble(xp -> 2+tfIdf(xp,c1)+tfIdf(xp,c2)).sum();
		
		return diff/(diff+inter);
	}
	
	/**
	 * The distance between the page class schemas is defined as
	 * the normalized cardinality of the symmetric 
	 * set difference between the two schemas.<br>
	 * Namely, let Gi and Gj be the schemas of groups i and j; then:<br>
	 * distance(Gi,Gj) = |(Gi-Gj) U (Gj-Gi)| / |(Gi U Gj)| <br>
	 * Note that if Gi = Gj (identical schemas), then distance(Gi,Gj) = 0;<br>
	 * conversely, if Gi ∩ Gj = empty (the schemas are disjoint), 
	 * then distance(Gi,Gj) = 1
	 * @param c1
	 * @param c2
	 * @return the distance between the two classes
	 */
	public static double distance(ModelPageClass c1, ModelPageClass c2) {
		Set<XPath> schema1 = c1.getSchema();
		Set<XPath> schema2 = c2.getSchema();
		return (differenceSize(schema1,schema2)+differenceSize(schema2,schema1)) /
				unionSize(schema1,schema2);
	}
	
	public static double distanceLinks(PageClass c1, PageClass c2) {
		Set<ClassLink> links1 = c1.getAllLinks();
		Set<ClassLink> links2 = c2.getAllLinks();
		return (differenceSize(links1,links2)+differenceSize(links2,links1)) /
				unionSize(links1,links2);
	}
	
	public static boolean isSubSet(PageClass c1, PageClass c2) {
		Set<ClassLink> links1 = c1.getAllLinks();
		Set<ClassLink> links2 = c2.getAllLinks();
		return !links1.isEmpty() && !links2.isEmpty() && 
				(links1.containsAll(links2) || links2.containsAll(links1));
	}
	
	private static double unionSize(Set<?> s1, Set<?> s2) {
		return concat(s1.stream(), s2.stream()).distinct().count();
	}
	
	private static double differenceSize(Set<?> s1, Set<?> s2) {
		return s1.stream().filter(xp->!s2.contains(xp)).count();
	}
 
}
