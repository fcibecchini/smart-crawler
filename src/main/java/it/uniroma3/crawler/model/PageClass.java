package it.uniroma3.crawler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

import static java.util.stream.Collectors.toList;

@NodeEntity
public class PageClass {
	private Long id;
	
	private String name;
	private String website;
	private int depth;
	private int version;
	
	@Transient private int waitTime;
	@Transient private int randomPause;
	@Transient private int maxFetchTries;
	@Transient private boolean javascript;
	
	@Relationship(type="CLASS_LINK", direction=Relationship.OUTGOING)
	private Set<ClassLink> links;
	
	@Relationship(type="DATA_LINK", direction=Relationship.OUTGOING)
	private List<DataLink> dataLinks;
	
	public PageClass() {
		this.links = new HashSet<>();
		this.dataLinks = new ArrayList<>();
	}
	
	public PageClass(String name, SeedConfig conf) {
		this(name, conf.site);
		this.waitTime = conf.wait;
		this.randomPause = conf.randompause;
		this.maxFetchTries = conf.maxfailures;
		this.javascript = conf.javascript;
	}
	
	public PageClass(String name, String website) {
		this();
		this.name = name;
		this.website = website;
	}
	
    public Long getId() {
        return id;
    }

	public String getName() {
		return this.name;
	}
	
	public String getDomain() {
		return website;
	}

	public void setMaxFetchTries(int maxFetchTries) {
		this.maxFetchTries = maxFetchTries;
	}

	public void setRandomPause(int randomPause) {
		this.randomPause = randomPause;
	}

	public void setJavascript(boolean javascript) {
		this.javascript = javascript;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLinks(Set<ClassLink> links) {
		this.links = links;
	}
	
	public Set<ClassLink> getLinks() {
		return links;
	}

	public void setDataLinks(List<DataLink> dataLinks) {
		this.dataLinks = dataLinks;
	}

	public void setWebsite(String website) {
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
	
	public void setVersion(int version) {
		this.version = version;
	}
	
	public int getVersion() {
		return this.version;
	}

	public boolean useJavaScript() {
		return javascript;
	}
	
	public int maxTries() {
		return maxFetchTries;
	}
	
	public int getPause() {
		return randomPause;
	}
	
	public Stream<PageClass> classLinks() {
		return links.stream().map(ClassLink::getDestination);
	}
	
	public PageClass getDescendant(String name) {
		Queue<PageClass> queue = new LinkedList<>();
		Set<PageClass> visited = new HashSet<>();
		
		PageClass current = null;
		queue.add(this);
		while ((current = queue.poll()) != null) {
			if (current.getName().equals(name))
				return current;
			current.classLinks()
			.filter(pc -> !visited.contains(pc))
			.forEach(queue::add);
			visited.add(current);
		}
		return null;
	}
	
	public void setHierarchy() {
		Queue<PageClass> queue = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		visited.add(name);
		queue.add(this);
		
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			int depth = current.getDepth();
			
			current.classLinks()
			.filter(pc -> visited.add(pc.getName()))
			.forEach(pc -> {
				queue.add(pc); 
				pc.setDepth(depth+1);
			});
		}
	}
	
	public void setGraphVersion(int version) {
		Queue<PageClass> queue = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		visited.add(name);
		queue.add(this);
		
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			current.setVersion(version);
			current.classLinks()
			.filter(pc -> visited.add(pc.getName()))
			.forEach(queue::add);
		}
	}
	
	public PageClass getDestinationByXPath(String xpath) {
		if (!links.isEmpty())
			return links.stream()
				.filter(l -> l.getXPath().equals(xpath))
				.map(ClassLink::getDestination)
				.findAny().orElse(null);
		return null;
	}
	
	public DataType getDataTypeByXPath(String xpath) {
		if (!dataLinks.isEmpty())
			return this.dataLinks.stream()
				.filter(link -> link.getXPath().equals(xpath))
				.map(DataLink::getDataType)
				.findAny().orElse(null);
		return null;
	}
	
	public Map<String, DataType> xPathToData() {
		Map<String, DataType> map = new HashMap<>();
		dataLinks.stream().forEach(l -> map.put(l.getXPath(), l.getDataType()));
		return map;
	}
	
	public List<String> getNavigationXPaths() {
		return this.links.stream().map(l -> l.getXPath()).collect(toList());
	}
	
	public List<String> getMenuXPaths() {
		return this.links.stream()
				.filter(ClassLink::isMenu)
				.map(ClassLink::getXPath).collect(toList());
	}
	
	public List<String> getListXPaths() {
		return this.links.stream()
				.filter(ClassLink::isList)
				.map(ClassLink::getXPath).collect(toList());
	}
	
	public List<String> getSingletonXPaths() {
		return this.links.stream()
				.filter(ClassLink::isSingleton)
				.map(ClassLink::getXPath).collect(toList());
	}
	
	public List<String> getDataXPaths() {
		return this.dataLinks.stream().map(DataLink::getXPath).collect(toList());
	}
	
	/**
	 * Adds a ClassLink to this PageClass, without any specified type.
	 * @param xpath the XPath
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addPageClassLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest);
		return this.links.add(link);
	}
	
	/**
	 * Adds a Menu item ClassLink to this PageClass.
	 * @param xpath the XPath leading to the menu item
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addMenuLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest);
		link.setTypeMenu();
		return links.add(link);
	}
	
	/**
	 * Adds a Menu item ClassLink to this PageClass. 
	 * Note that the specified XPath must link the whole menu, not just one item.
	 * The index parameter specify the Menu item index which leads to the given destination
	 * PageClass. 
	 * @param xpath the XPath leading to the menu
	 * @param index the menu item index
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addMenuLink(String xpath, int index, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest, index);
		link.setTypeMenu();
		return links.add(link);
	}
	
	/**
	 * Adds a List ClassLink to this PageClass
	 * @param xpath the XPath
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addListLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest);
		link.setTypeList();
		return links.add(link);
	}
	
	/**
	 * Adds a Singleton ClassLink to this PageClass
	 * @param xpath the XPath
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addSingletonLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest);
		link.setTypeSingleton();
		return links.add(link);
	}
	
	/**
	 * Returns true if this PageClass has a ClassLink of any type (Menu,List,Singleton) 
	 * identified by the specified xpath.
	 * @param xp the xpath
	 * @return true if there is a ClassLink with this xpath
	 */
	public boolean hasLink(String xp) {
		return links.stream().anyMatch(
				l -> l.getXPath().equals(xp) || l.getMenuXPath().equals(xp));
	}
	
	/**
	 * Returns true if the specified XPath leads to a Menu.
	 * @param xp the xpath
	 * @return true if this xpath identifies a menu
	 */
	public boolean hasMenuLink(String xp) {
		return links.stream().anyMatch(l -> l.getMenuXPath().equals(xp));
	}
	
	/**
	 * Returns true if this PageClass has a ClassLink of of type List 
	 * identified by the specified xpath.
	 * @param xp the xpath
	 * @return true if there is a List ClassLink with this xpath
	 */
	public boolean hasListLink(String xp) {
		return links.stream().filter(ClassLink::isList)
				.anyMatch(l -> l.getXPath().equals(xp));
	}
	
	/**
	 * Returns true if this PageClass has a ClassLink of type Singleton
	 * identified by the specified xpath.
	 * @param xp the xpath
	 * @return true if there is a Singleton ClassLink with this xpath
	 */
	public boolean hasSingleLink(String xp) {
		return links.stream().filter(ClassLink::isSingleton)
				.anyMatch(l -> l.getXPath().equals(xp));
	}
	
	/**
	 * Removes a single ClassLink identified by this XPath
	 * @param xp the xpath
	 */
	public void removeLink(String xp) {
		links.stream()
			.filter(l -> l.getXPath().equals(xp))
			.findAny().ifPresent(links::remove);
	}
	
	/**
	 * Removes all the ClassLinks leading to menu items identified by this XPath
	 * @param xp the xpath leading to the menu
	 */
	public void removeMenuLink(String xp) {
		List<ClassLink> toRemove = 
			links.stream().filter(l -> l.getMenuXPath().equals(xp)).collect(Collectors.toList());
		links.removeAll(toRemove);
	}
	
	public boolean addData(String xpath, String type, String fieldName) {
		DataType data = makeDataType(xpath, type);
		if (data==null) return false;
		data.setName(fieldName);
		return dataLinks.add(new DataLink(this, xpath, data));
	}
	
	public boolean addData(String xpath, String type) {
		DataType data = makeDataType(xpath, type);
		if (data==null) return false;
		return dataLinks.add(new DataLink(this, xpath, data));
	}
	
	private DataType makeDataType(String xpath, String type) {
		String name = type.substring(0, 1).toUpperCase()+type.substring(1);
		String className = "it.uniroma3.crawler.model."+name+"DataType";
		try {
			DataType dataType = (DataType) Class.forName(className).newInstance();
			return dataType;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	public boolean isEndPage() {
		return this.links.isEmpty();
	}
	
	public boolean isDataPage() {
		return !this.dataLinks.isEmpty();
	}
	
	public int compareTo(PageClass pc2) {
		int cmpdepth = depth - pc2.getDepth();
		if (cmpdepth!=0) return cmpdepth;
		int cmpname = name.compareTo(pc2.getName());
		if (cmpname!=0) return cmpname;
		return website.compareTo(pc2.getDomain());
	}
	
	public String toString() {
		return name;
	}
	
	public int hashCode() {
		return Objects.hash(name, depth, website);
	}

	public boolean equals(Object obj) {
		PageClass other = (PageClass) obj;
		return Objects.equals(name, other.getName())
			&& depth==other.getDepth()
			&& website.equals(other.getDomain());
	}

}
