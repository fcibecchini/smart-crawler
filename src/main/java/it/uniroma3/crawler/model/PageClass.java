package it.uniroma3.crawler.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PageClass implements Serializable {
	private static final long serialVersionUID = 20919679521227239L;
	
	private String name;
	private int depth;
	private long waitTime;
	
	private transient Collection<PageClassLink> links;
	private transient Collection<DataType> dataTypes;
	
	public PageClass(String name, long waitTime) {
		this(name);
		this.waitTime = waitTime;
	}
	
	public PageClass(String name) {
		this.name = name;
		this.links = new ArrayList<>();
		this.dataTypes = new ArrayList<>();
	}
	
	public String getName() {
		return this.name;
	}
	
	public long getWaitTime() {
		return this.waitTime;
	}
	
	public void setWaitTime(long time) {
		this.waitTime = time;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public int getDepth() {
		return this.depth;
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
	
	public List<String> getDataXPaths() {
		return this.dataTypes.stream().map(dt -> dt.getXPath()).collect(Collectors.toList());
	}
	
	public boolean addPageClassLink(String xpath, PageClass dest) {
		PageClassLink link = new PageClassLink(xpath, dest);
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
		return this.getDepth() - pc2.getDepth();
	}
	
	public String toString() {
		return name+": "+links.toString();
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject((links.isEmpty()) ? null : links);
		out.writeObject((dataTypes.isEmpty()) ? null : dataTypes);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		Collection<PageClassLink> classLinks = 
				(Collection<PageClassLink>) in.readObject();
		links = (classLinks != null) ? classLinks : new ArrayList<>();
		Collection<DataType> dTypes = 
				(Collection<DataType>) in.readObject();
		dataTypes = (dTypes != null) ? dTypes : new ArrayList<>();
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

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
