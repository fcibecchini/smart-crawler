package it.uniroma3.crawler.modeler;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import it.uniroma3.crawler.model.PageClass;

public abstract class WebsiteModeler {
	private URI urlBase;
	private PageClass entryClass;
	private Set<PageClass> pClasses;
		
	private Logger log;
	
	public WebsiteModeler() {
		this.log = Logger.getLogger(WebsiteModeler.class.getName());
	}
	
	public WebsiteModeler(URI urlBase) {
		this();
		this.urlBase = urlBase;
	}
	
	public URI getUrlBase() {
		return this.urlBase;
	}
	
	public void setUrlBase(URI base) {
		this.urlBase = base;
	}
	
	public PageClass getEntryPageClass() {
		return this.entryClass;
	}
	
	public void setEntryPageClass(PageClass entry) {
		this.entryClass = entry;
	}
	
	public Logger getLogger() {
		return this.log;
	}
	
	public Set<PageClass> getClasses() {
		return this.pClasses;
	}
	
	public void setClasses(Set<PageClass> pClasses) {
		this.pClasses = pClasses;
	}
	
	public abstract PageClass computeModel();

	public void setHierarchy() {
		Queue<PageClass> queue = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		visited.add(entryClass);
		queue.add(entryClass);
		PageClass current, next = null;
		entryClass.setDepth(0);
		while ((current = queue.poll()) != null) {
			if (!current.isEndPage()) {
				for (String xpath : current.getNavigationXPaths()) {
					next = current.getDestinationByXPath(xpath);
					// avoid page class loops
					if (!visited.contains(next)) {
						visited.add(next);
						queue.add(next);
						next.setDepth(current.getDepth()+1);
					}
				}
			}
		}
	}
}
