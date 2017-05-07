package it.uniroma3.crawler.modeler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;

public abstract class WebsiteModeler {
	private Logger log = Logger.getLogger(WebsiteModeler.class.getName());

	private Website website;
	private PageClass entryClass;
	private int wait;		
	
	public WebsiteModeler(Website website, int wait) {
		this.website = website;
		this.wait = wait;
	}
	
	public Website getWebsite() {
		return this.website;
	}
	
	public PageClass getEntryPageClass() {
		return this.entryClass;
	}

	public int getWait() {
		return wait;
	}

	public Logger getLogger() {
		return this.log;
	}
	
	public PageClass compute() {
		entryClass = computeModel();
		setHierarchy();
		return entryClass;
	}
	
	protected abstract PageClass computeModel();

	private void setHierarchy() {
		Queue<PageClass> queue = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		visited.add(entryClass);
		queue.add(entryClass);
		entryClass.setDepth(0);
		
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			int depth = current.getDepth();
			
			current.children().stream()
			.filter(pc -> !visited.contains(pc))
			.forEach(pc -> {
				visited.add(pc); 
				queue.add(pc); 
				pc.setDepth(depth+1);});
		}
	}
	
	public PageClass getByName(String name) {
		Queue<PageClass> queue = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		visited.add(entryClass);
		queue.add(entryClass);
		
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			if (current.getName().equals(name))
				return current;
			
			current.children().stream()
			.filter(pc -> !visited.contains(pc))
			.forEach(pc -> {visited.add(pc); queue.add(pc);});
		}
		return null;
	}

	@Override
	public int hashCode() {
		return website.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		WebsiteModeler other = (WebsiteModeler) obj;
		return Objects.equals(website, other.getWebsite());
	}
}
