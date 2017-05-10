package it.uniroma3.crawler.modeler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import it.uniroma3.crawler.model.PageClass;

public interface WebsiteModeler {
	
	public PageClass compute();
		
	public default void setHierarchy(PageClass root) {
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
}
