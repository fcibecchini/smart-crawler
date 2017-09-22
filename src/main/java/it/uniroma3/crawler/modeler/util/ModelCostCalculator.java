package it.uniroma3.crawler.modeler.util;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import it.uniroma3.crawler.model.ClassLink;
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
	 * Weight for encoding a schema XPath that is not in p
	 */
	private final double C_MISS = 1;
	
	/**
	 * map of inverse document frequency value for each XPath 
	 */
	private Map<XPath,Double> xpath2IDF;
	
	/**
	 * Constructs a Model cost Calculator based on the given collection of Pages
	 * @param pages
	 */
	public ModelCostCalculator(Collection<Page> pages) {
		this.xpath2IDF = pages.stream().flatMap(p -> p.getSchema().stream()).distinct()
				.collect(toMap(xp->xp, xp -> Math.log(pages.size() / df(xp,pages))));
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
		return model.getClasses().stream().mapToDouble(c -> {
			Set<XPath> classSchema = c.getSchema();
			return classSchema.size()+
				c.getPages().stream().mapToDouble(p -> pageCost(p,c,classSchema)).sum();
		}).sum();
	}
	
	/**
	 * Page cost function for an instance p of Class C <br>
	 * <br>
	 * indexes = weighted sum of XPaths of p in C's schema <br>
	 * urlsSize = sum of URLs in p <br>
	 * missing =  sum of XPaths present in C but missing in p <br>
	 * <br>
	 * weight(xp|c) = tf-idf score of an XPath of C (the more the score, the less the weight) 
	 * @param p the page
	 * @param classSchema the schema of the class of p
	 * @return cost(p|c)
	 */
	public double pageCost(Page p, ModelPageClass c, Set<XPath> classSchema) {
		Set<XPath> pageSchema = p.getSchema();
		double indexes = pageSchema.stream().mapToDouble(xp -> C_I*pageWeight(xp,p)).sum();
		double missing = classSchema.stream().filter(xp -> !pageSchema.contains(xp))
				.mapToDouble(xp -> C_MISS*classWeight(xp,c)).sum();		
		return indexes + C_U*p.urlsSize() + missing;
	}

	/**
	 * Calculates the weight of an XPath of a Page following a tf-idf scoring function
	 * @param xp the XPath
	 * @param p the Page
	 * @return weight(xp|p)
	 */
	public double pageWeight(XPath xp, Page p) {
		double score = p.getXPathFrequency(xp) * idf(xp);
		return (score==0) ? 1 : 1/score;
	}
	
	/**
	 * Calculates the weight of an XPath of a Class following a tf-idf scoring function
	 * @param xp the XPath
	 * @param c the ModelPageClass
	 * @return weight(xp|c)
	 */
	public double classWeight(XPath xp, ModelPageClass c) {
		double score = tfIdf(xp,c);
		return (score==0) ? 1 : 1/score;
	}
	
	/**
	 * Calculates the XPath Frequency in a Class - Inverse Page Frequency of the XPath.
	 * @param xp the XPath
	 * @param c the ModelPageClass
	 * @return the tf-idf
	 */
	public double tfIdf(XPath xp, ModelPageClass c) {
		return c.xPathFrequency(xp) * idf(xp);
	}
	
	/**
	 * Calculates the inverse Page frequency of an XPath
	 * @param xp the XPath
	 * @return the inverse Page frequency
	 */
	public double idf(XPath xp) {
		return xpath2IDF.get(xp);
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
		
	public double weightedDistance(ModelPageClass c1, ModelPageClass c2) {
		Set<XPath> schema1 = c1.getSchema();
		Set<XPath> schema2 = c2.getSchema();
		
		double diff = schema1.stream().filter(xp->!schema2.contains(xp))
				.mapToDouble(xp -> tfIdf(xp,c1)).sum() +
				schema2.stream().filter(xp->!schema1.contains(xp))
				.mapToDouble(xp -> tfIdf(xp,c2)).sum();
		
		double union = concat(schema1.stream(), schema2.stream()).distinct()
				.mapToDouble(xp -> tfIdf(xp,c1)+tfIdf(xp,c2)).sum();
					
		return (diff>0 && union>0) ? diff/union : 0;
	}
	
	public static double distanceLinks(ModelPageClass c1, ModelPageClass c2) {
		Set<ClassLink> links1 = c1.getPageClass().getAllLinks();
		Set<ClassLink> links2 = c2.getPageClass().getAllLinks();
		return (differenceSize(links1,links2)+differenceSize(links2,links1)) /
				unionSize(links1,links2);
	}
	
	public static boolean isSubSet(ModelPageClass c1, ModelPageClass c2) {
		Set<ClassLink> links1 = c1.getPageClass().getAllLinks();
		Set<ClassLink> links2 = c2.getPageClass().getAllLinks();
		return !links1.isEmpty() && !links2.isEmpty() && 
				(links1.containsAll(links2) || links2.containsAll(links1));
	}
	
	private static double unionSize(Set<?> s1, Set<?> s2) {
		return concat(s1.stream(), s2.stream()).distinct().count();
	}
	
	private static double differenceSize(Set<?> s1, Set<?> s2) {
		return s1.stream().filter(xp->!s2.contains(xp)).count();
	}
	
	private static double df(XPath xp, Collection<Page> pages) {
		return pages.stream().filter(p -> p.containsXPath(xp)).count();
	}
	
	/*
	private double printN(XPath xp, ModelPageClass c) {
		double f = c.xPathFrequency(xp);
		double t = f*idf(xp);
		System.out.println(f+"*"+idf(xp)+" = "+t+" "+xp.get());
		return t;
	}
	
	private double printN2(XPath xp, ModelPageClass c1, ModelPageClass c2) {
		double f1 = c1.xPathFrequency(xp);
		double t1 = f1*idf(xp);
		
		double f2 = c2.xPathFrequency(xp);
		double t2 = f2*idf(xp);
		
		double res = t1+t2;
		
		System.out.println(f1+"*"+idf(xp)+" = "+t1+", "+
				f2+"*"+idf(xp)+" = "+t2+" -> "+res+" "+xp.get());
		return res;
	}
	*/
 
}
