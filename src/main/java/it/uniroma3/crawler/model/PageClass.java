package it.uniroma3.crawler.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class PageClass {
	private String name;
	private Website website;
	private int depth;
	private int waitTime;
	
	private Collection<ClassLink> links;
	private Collection<DataType> dataTypes;
	
	public PageClass(String name, Website website, int waitTime) {
		this(name,website);
		this.waitTime = waitTime;
	}
	
	public PageClass(String name, Website website) {
		this.name = name;
		this.website = website;
		this.links = new ArrayList<>();
		this.dataTypes = new ArrayList<>();
	}
		
	public Collection<DataType> getDataTypes() {
		return dataTypes;
	}

	public String getName() {
		return this.name;
	}
	
	public Website getWebsite() {
		return website;
	}
	
	public String getDomain() {
		return website.getDomain();
	}

	public void setWebsite(Website website) {
		this.website = website;
	}

	public long getWaitTime() {
		return this.waitTime;
	}
	
	public void setWaitTime(int time) {
		this.waitTime = time;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public int getDepth() {
		return this.depth;
	}
	
	public boolean useJavaScript() {
		return website.isJavascript();
	}
	
	public int maxTries() {
		return website.getMaxFetchTries();
	}
	
	public int getPause() {
		return website.getPause();
	}
	
	public Set<PageClass> classLinks() {
		return links.stream().map(l -> l.getDestination()).collect(Collectors.toSet());
	}
	
	public PageClass getDescendant(String name) {
		Queue<PageClass> queue = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		visited.add(this);
		queue.add(this);
		
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			if (current.getName().equals(name))
				return current;
			current.classLinks().stream()
			.filter(pc -> !visited.contains(pc))
			.forEach(pc -> {visited.add(pc); queue.add(pc);});
		}
		return null;
	}
	
	public PageClass getDestinationByXPath(String xpath) {
		if (!links.isEmpty())
			return links.stream()
				.filter(l -> l.getXPath().equals(xpath))
				.map(l -> l.getDestination())
				.findAny().orElse(null);
		return null;
	}
	
	public DataType getDataTypeByXPath(String xpath) {
		if (!dataTypes.isEmpty())
			return this.dataTypes.stream()
				.filter(d -> d.getXPath().equals(xpath))
				.findAny().orElse(null);
		return null;
	}
	
	public List<String> getNavigationXPaths() {
		return this.links.stream().map(l -> l.getXPath()).collect(Collectors.toList());
	}
	
	public List<String> getMenuXPaths() {
		return this.links.stream()
				.filter(l -> l.getType()==ClassLink.MENU)
				.map(l -> l.getXPath())
				.collect(Collectors.toList());
	}
	
	public List<String> getListXPaths() {
		return this.links.stream()
				.filter(l -> l.getType()==ClassLink.LIST)
				.map(l -> l.getXPath())
				.collect(Collectors.toList());
	}
	
	public List<String> getSingletonXPaths() {
		return this.links.stream()
				.filter(l -> l.getType()==ClassLink.SINGLETON)
				.map(l -> l.getXPath())
				.collect(Collectors.toList());
	}
	
	public List<String> getDataXPaths() {
		return this.dataTypes.stream().map(dt -> dt.getXPath()).collect(Collectors.toList());
	}
	
	public boolean addPageClassLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(xpath, dest);
		return this.links.add(link);
	}
	
	public boolean addMenuLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(xpath, ClassLink.MENU, dest);
		return this.links.add(link);
	}
	
	public boolean addListLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(xpath, ClassLink.LIST, dest);
		return this.links.add(link);
	}
	
	public boolean addSingletonLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(xpath, ClassLink.SINGLETON, dest);
		return this.links.add(link);
	}
	
	public boolean addData(String xpath, String type) {
		String name = type.substring(0, 1).toUpperCase()+type.substring(1);
		try {
			DataType dataType = (DataType) Class.forName("it.uniroma3.crawler.model."+name+"DataType").newInstance();
			dataType.setXPath(xpath);
			return dataTypes.add(dataType);
		} catch (InstantiationException e) {
			System.err.println("Data Type instantiation failed");
			return false;
		} catch (IllegalAccessException e) {
			System.err.println("Data Type illegal access");
			return false;
		} catch (ClassNotFoundException e) {
			System.err.println("Data Type not found "+type+" "+xpath);
			return false;
		}
	}
	
	public boolean isEndPage() {
		return this.links.isEmpty();
	}
	
	public boolean isDataPage() {
		return !this.dataTypes.isEmpty();
	}
	
	public int compareTo(PageClass pc2) {
		int cmpdepth = this.getDepth() - pc2.getDepth();
		if (cmpdepth!=0) return cmpdepth;
		int cmpws = this.getWebsite().compareTo(pc2.getWebsite());
		if (cmpws!=0) return cmpws;
		return this.getName().compareTo(pc2.getName());
	}
	
	public String toString() {
		return name+", "+getDomain();
	}
	
	public int hashCode() {
		return name.hashCode() + website.hashCode();
	}

	public boolean equals(Object obj) {
		PageClass other = (PageClass) obj;
		return Objects.equals(name, other.getName())
			&& Objects.equals(website, other.getWebsite());
	}

}
