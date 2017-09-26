package it.uniroma3.crawler.modeler.model;

import java.util.TreeSet;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import it.uniroma3.crawler.model.PageClass;

import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

/**
 * A WebsiteModel is a collection of clustered pages ({@link ModelPageClass}) 
 * with a common page class schema.
 *
 */
public class WebsiteModel {
	private TreeSet<ModelPageClass> modelClasses;
	private Map<ModelPageClass,PageClass> model2Class;
	
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
	
	public void removeAll(Set<ModelPageClass> classes) {
		modelClasses.removeAll(classes);
	}
	
	/**
	 * Returns the ModelPageClass of this model with the given id
	 * @param id
	 * @return the ModelPageClass identified by this id, otherwise null
	 */
	public ModelPageClass getCandidateFromId(int id) {
		return modelClasses.stream().filter(c -> c.getId()==id).findAny().orElse(null);
	}
	
	/**
	 * Returns a reference to the ModelPageClass containing the
	 * given {@link Page}.
	 * @param page
	 * @return the CandidatePageClass of this model containing the page, 
	 * otherwise null
	 */
	public ModelPageClass getClassOfPage(Page page) {
		return modelClasses.stream().filter(c -> c.containsPage(page)).findAny().orElse(null);
	}
	
	public PageClass getPageClass(ModelPageClass c) {
		return model2Class.get(c);
	}
	
	/**
	 * Turns this WebsiteModel into a {@link PageClass} graph ready for crawling and storage.
	 * 
	 * @param conf Configuration parameters for this website 
	 * @return the root (homepage) PageClass of the navigation graph
	 */
	public PageClass toGraph(SeedConfig conf) {		
		model2Class = modelClasses.stream().collect(toMap(c->c, c->new PageClass(c.name(),conf)));
		buildLinks(conf);
		return getPageClass(modelClasses.first());
	}
	
	public void setPagesClassification() {
		for (ModelPageClass mpc : modelClasses) {
			PageClass pc = getPageClass(mpc);
			String desc = mpc.getPages().stream()
					.map(p -> pc.getName()+"\t"+p.getUrl()+"\t"+p.getTempFile()+"\n")
					.reduce(String::concat).orElse("");
			pc.setModelClassification(desc);
		}
	}
	
	private void buildLinks(SeedConfig conf) {
		modelClasses.forEach(c -> {
			PageClass src = getPageClass(c);
			c.getPages().stream().flatMap(p->p.getLinks().stream())
			.forEach(link -> {
				List<PageClass> dests = 
					link.getDestinations().stream()
					.map(this::getClassOfPage)
					.map(this::getPageClass)
					.collect(toList());
				link.linkToPageClass(src, dests);
			});
		});
	}
	
	public String toString() {
		return modelClasses.toString();
	}

}
