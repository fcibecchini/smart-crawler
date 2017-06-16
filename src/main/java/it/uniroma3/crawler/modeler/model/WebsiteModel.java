package it.uniroma3.crawler.modeler.model;

import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

import java.util.List;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

/**
 * A WebsiteModel is a collection of clustered pages ({@link ModelPageClass}) 
 * with a common page class schema.
 *
 */
public class WebsiteModel {
	private static final double C_U = 1;
	private static final double C_XP = 1;
	private static final double C_I = 0.8;
	private static final double C_MISS = 1;

	private TreeSet<ModelPageClass> modelClasses;
	private double cost;
	
	/**
	 * Constructs a new, empty Website model.
	 */
	public WebsiteModel() {
		this.modelClasses = new TreeSet<>();
	}
	
	/**
	 * Constructs a new WebsiteModel as a copy of the given WebsiteModel
	 * @param model the WebsiteModel to copy
	 */
	public WebsiteModel(WebsiteModel model) {
		this();
		modelClasses.addAll(model.getClasses());
	}
	
	public boolean addClass(ModelPageClass candidate) {
		return this.modelClasses.add(candidate);
	}
	
	public void removeClass(ModelPageClass candidate) {
		this.modelClasses.remove(candidate);
	}
	
	/**
	 * Substitute the current model page classes with the page classes
	 * from the specified model.
	 * @param other the WebsiteModel to copy
	 */
	public void copy(WebsiteModel other) {
		modelClasses.clear();
		modelClasses.addAll(other.getClasses());
	}
	
	/**
	 * True if this WebsiteModel does not contain any CandidatePageClass
	 * @return true if the model is empty
	 */
	public boolean isEmpty() {
		return this.modelClasses.isEmpty();
	}
	
	public TreeSet<ModelPageClass> getClasses() {
		return modelClasses;
	}
	
	/**
	 * Returns the ModelPageClass of this model with the given id
	 * @param id
	 * @return the ModelPageClass identified by this id, otherwise null
	 */
	public ModelPageClass getCandidateFromId(int id) {
		return modelClasses.stream()
				.filter(c -> c.getId()==id).findAny().orElse(null);
	}
	
	/**
	 * Returns a reference to the ModelPageClass containing the
	 * given {@link Page}.
	 * @param page
	 * @return the CandidatePageClass of this model containing the page, 
	 * otherwise null
	 */
	public ModelPageClass getClassOfPage(Page page) {
		return modelClasses.stream()
				.filter(c -> c.containsPage(page)).findAny().orElse(null);
	}
	
	/**
	 * Turns this WebsiteModel into a {@link PageClass} graph ready for crawling and storage.
	 * 
	 * @param conf Configuration parameters for this website 
	 * @return the root (homepage) PageClass of the navigation graph
	 */
	public PageClass toGraph(SeedConfig conf) {
		Map<Integer, PageClass> pageClasses = 
				modelClasses.stream().collect(toMap(
				c -> c.getId(), 
				c -> new PageClass(c.name(), conf)));
		
		for (ModelPageClass mpc : modelClasses) {
			for (Page p : mpc.getPages()) {
				PageClass src = pageClasses.get(getClassOfPage(p).getId());
				for (PageLink link : p.getLinks()) {
					List<PageClass> dests = 
						link.getDestinations().stream()
						.map(pdest -> pageClasses.get(getClassOfPage(pdest).getId()))
						.collect(Collectors.toList());
					link.linkToPageClass(src,dests);
				}
			}
		}
		return pageClasses.get(modelClasses.first().getId());
	}
	
	/**
	 * Calculates the Minimum Description Length cost of this model.<br>
	 * Model Cost is cached after the first invocation.
	 * @see <a href="https://en.wikipedia.org/wiki/Minimum_description_length">
	 * Minimum description length (MDL) principle</a>
	 * @return the MDL cost
	 */
	public double cost() {
		if (cost>0) 
			return cost;
		else {
			cost = calculateLength();
			return (cost>0) ? cost : Double.MAX_VALUE;
		}
	}
	
	private double calculateLength() {
		int modelCost = 0;
		for (ModelPageClass c : modelClasses) {
			modelCost += C_XP * c.schemaSize();
		}
		
		int dataCost = 0;
		for (ModelPageClass c : modelClasses) {
			for (Page p : c.getPages()) {
				int pageCost = 0;
				pageCost += C_U * p.urlsSize();
				pageCost += C_I * c.schemaIntersectionSize(p);
				pageCost += C_XP * p.schemaDifferenceSize(c);
				pageCost += C_MISS * c.schemaDifferenceSize(p);
				dataCost += pageCost;
			}
		}
		return modelCost+dataCost;
	}

}
