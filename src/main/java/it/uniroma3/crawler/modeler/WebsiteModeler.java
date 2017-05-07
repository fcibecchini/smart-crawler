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
	private int wait;		
	
	public WebsiteModeler(Website website, int wait) {
		this.website = website;
		this.wait = wait;
	}
	
	public Website getWebsite() {
		return this.website;
	}

	public int getWait() {
		return wait;
	}

	public Logger getLogger() {
		return this.log;
	}
	
	public PageClass compute() {
		PageClass root = computeModel();
		setHierarchy(root);
		return root;
	}
	
	protected abstract PageClass computeModel();

	private void setHierarchy(PageClass root) {
		Queue<PageClass> queue = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		visited.add(root);
		queue.add(root);
		root.setDepth(0);
		
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			int depth = current.getDepth();
			
			current.classLinks().stream()
			.filter(pc -> !visited.contains(pc))
			.forEach(pc -> {
				visited.add(pc); 
				queue.add(pc); 
				pc.setDepth(depth+1);});
		}
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
