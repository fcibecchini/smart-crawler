package it.uniroma3.crawler.modeler.evaluator;

import java.util.ArrayList;
import java.util.List;

public class TrueModel {
	private List<TrueClass> classes;

	public TrueModel() {
		classes = new ArrayList<>();
	}
	
	public void addClass(TrueClass tClass) {
		classes.add(tClass);
	}
	
	public List<TrueClass> getClasses() {
		return classes;
	}
	
	public TrueClass getByName(String name) {
		return classes.stream().filter(tc -> tc.getName().equals(name)).findAny().get();
	}
	
	public int size() {
		return classes.size();
	}

}
