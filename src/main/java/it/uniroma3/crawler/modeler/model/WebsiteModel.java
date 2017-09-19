package it.uniroma3.crawler.modeler.model;

import java.util.TreeSet;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

/**
 * A WebsiteModel is a collection of clustered pages ({@link ModelPageClass}) 
 * with a common page class schema.
 *
 */
public class WebsiteModel {
	private TreeSet<ModelPageClass> modelClasses;
	
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
		buildLinks(conf);
		collapsePageClasses();
		buildLinks(conf);
		return modelClasses.first().getPageClass();
	}
	
	public void setPagesClassification() {
		for (ModelPageClass mpc : modelClasses) {
			PageClass pc = mpc.getPageClass();
			String desc = mpc.getPages().stream()
					.map(p -> p.getUrl()+"\t"+p.getTempFile()+"\t"+pc.getName()+"\n")
					.reduce(String::concat).orElse("");
			pc.setModelClassification(desc);
		}
	}
	
	private void collapsePageClasses() {		
		List<ModelPageClass> classList = new ArrayList<>(modelClasses);
		List<ModelPageClass> toRemove = new ArrayList<>();
		
		for (int i = 0; i < classList.size(); i++) {
			for (int j = classList.size() - 1; j > i; j--) {
				ModelPageClass c1 = classList.get(i);
				ModelPageClass c2 = classList.get(j);
				PageClass p1 = c1.getPageClass();
				PageClass p2 = c2.getPageClass();
				if (!toRemove.contains(c1) && !toRemove.contains(c2)) {
					if (p1.distance(p2)<0.2 || p1.isSubSet(p2)) {
						c1.collapse(c2);
						toRemove.add(c2);
					}
				}
			}
		}
		modelClasses.removeAll(toRemove);
	}
	
	private void buildLinks(SeedConfig conf) {
		modelClasses.forEach(c -> c.setPageClass(new PageClass(c.name(),conf)));
		for (ModelPageClass mpc : modelClasses) {
			PageClass src = mpc.getPageClass();
			for (Page p : mpc.getPages()) {
				for (PageLink link : p.getLinks()) {
					List<PageClass> dests = 
						link.getDestinations().stream()
						.map(d -> getClassOfPage(d).getPageClass()).collect(toList());
					link.linkToPageClass(src, dests);
				}
			}
		}
	}
	
	public String toString() {
		return modelClasses.toString();
	}

}
