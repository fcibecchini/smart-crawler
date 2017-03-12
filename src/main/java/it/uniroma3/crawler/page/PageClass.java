package it.uniroma3.crawler.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class PageClass {
	private String name;
	private Map<String,List<String>> xPathNavigation;
	
	public PageClass(String name) {
		this.name = name;
		this.xPathNavigation = new HashMap<>();
	}
	
	public String getName() {
		return this.name;
	}
	
	public Map<String,List<String>> getXPaths() {
		return this.xPathNavigation;
	}
	
	public void add(String xpath, String functionName, String destinationPage) {
		List<String> values = new ArrayList<>();
		values.add(functionName);
		values.add(destinationPage);
		this.xPathNavigation.put(xpath, values);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PageClass other = (PageClass) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
