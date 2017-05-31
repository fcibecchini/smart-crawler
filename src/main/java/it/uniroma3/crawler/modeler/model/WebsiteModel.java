package it.uniroma3.crawler.modeler.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * A WebsiteModel is a collection of clustered pages ({@link CandidatePageClass}) 
 * with a common page class schema.
 *
 */
public class WebsiteModel {
	private static final double C_U = 1;
	private static final double C_XP = 1;
	private static final double C_I = 0.8;
	private static final double C_MISS = 1;

	private Set<CandidatePageClass> modelClasses;
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
	
	public boolean addClass(CandidatePageClass candidate) {
		return this.modelClasses.add(candidate);
	}
	
	public void removeClass(CandidatePageClass candidate) {
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
	
	public Set<CandidatePageClass> getClasses() {
		return modelClasses;
	}
	
	/**
	 * Returns the CandidatePageClass of this model with the given id
	 * @param id
	 * @return the CandidatePageClass identified by this id, otherwise null
	 */
	public CandidatePageClass getCandidateFromId(int id) {
		return modelClasses.stream()
				.filter(c -> c.getId()==id).findAny().orElse(null);
	}
	
	/**
	 * Returns a reference to the CandidatePageClass containing a {@link Page} 
	 * identified with the given URL.
	 * @param url the URL of the page
	 * @return the CandidatePageClass of this model containing the URL, 
	 * otherwise null
	 */
	public CandidatePageClass getClassOfURL(String url) {
		return modelClasses.stream()
				.filter(c -> c.containsPage(url)).findAny().orElse(null);
	}
	
	/**
	 * Returns a reference to the CandidatePageClass containing the
	 * given {@link Page}.
	 * @param page
	 * @return the CandidatePageClass of this model containing the page, 
	 * otherwise null
	 */
	public CandidatePageClass getClassOfPage(Page page) {
		return modelClasses.stream()
				.filter(c -> c.containsPage(page)).findAny().orElse(null);
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
		for (CandidatePageClass c : modelClasses) {
			modelCost += C_XP * c.schemaSize();
		}
		
		int dataCost = 0;
		for (CandidatePageClass c : modelClasses) {
			for (Page p : c.getClassPages()) {
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
