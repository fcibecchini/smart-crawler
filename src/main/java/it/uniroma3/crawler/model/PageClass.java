package it.uniroma3.crawler.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

import static java.util.stream.Collectors.toList;

@NodeEntity
public class PageClass implements Comparable<PageClass> {
	private Long id;
	
	private String name;
	private String form;
	private String website;
	private int depth;
	private int version;
	
	@Transient private int waitTime;
	@Transient private int randomPause;
	@Transient private int maxFetchTries;
	@Transient private boolean javascript;
	@Transient private SortedSet<PageClass> descendants;
	
	@Relationship(type="CLASS_LINK", direction=Relationship.OUTGOING)
	private Set<ClassLink> links;
	
	@Relationship(type="DATA_LINK", direction=Relationship.OUTGOING)
	private List<DataLink> dataLinks;
	
	@Relationship(type="MENU", direction=Relationship.OUTGOING)
	private Set<Menu> menus;
	
	public PageClass() {
		this.links = new HashSet<>();
		this.dataLinks = new ArrayList<>();
		this.menus = new HashSet<>();
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
	
	public void setMenus(Set<Menu> menus) {
		this.menus = menus;
	}

	public void setWebsite(String website) {
		this.website = website;
	}
	
	public String getForm() {
		return this.form;
	}
	
	public void setForm(String form) {
		this.form = form;
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
	
	public int linksSize() {
		return links.size() + menus.stream().mapToInt(Menu::size).sum();
	}
	
	public Set<ClassLink> getAllLinks() {
		Set<ClassLink> links = new HashSet<>(this.links);
		menus.forEach(m -> links.addAll(m.toClassLinks()));
		return links;
	}
	
	public PageClass getDescendant(String name) {
		if (descendants==null) setHierarchy();
		return descendants.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
	}
	
	public SortedSet<PageClass> getDescendants() {
		return descendants;
	}
	
	public void setHierarchy() {
		Map<String,PageClass> name2class = new HashMap<>();
		name2class.put(name, this);
		Queue<PageClass> queue = new LinkedList<>(Arrays.asList(this));
		while (!queue.isEmpty()) {
			PageClass current = queue.poll();
			current.getAllLinks().stream().map(ClassLink::getDestination)
			.filter(p -> name2class.putIfAbsent(p.getName(), p)==null)
			.forEach(p -> {p.setDepth(current.getDepth()+1); queue.add(p);});
		}
		this.descendants = new TreeSet<>(name2class.values());
	}
	
	public void setGraphVersion(int version) {
		descendants.forEach(p -> p.setVersion(version));
	}
	
	public void setMenusTypes() {
		descendants.forEach(pc -> pc.menus.forEach(Menu::setType));
	}
	
	public PageClass getDestinationByXPath(String xpath) {
		return links.stream().filter(l -> l.getXPath().equals(xpath))
				.map(l -> l.getDestination()).findFirst()
				.orElseGet(() -> 
					menus.stream().map(m -> m.getDestination(xpath))
					.filter(d -> d!=null).findFirst().orElse(null));
	}
	
	public DataType getDataTypeByXPath(String xpath) {
		return this.dataLinks.stream()
				.filter(link -> link.getXPath().equals(xpath))
				.map(DataLink::getDataType).findFirst().orElse(null);
	}
	
	public Map<String, DataType> xPathToData() {
		Map<String, DataType> map = new TreeMap<>();
		int i = 0;
		for (DataLink l : dataLinks) {
			map.put(i+"\t"+l.getXPath(), l.getDataType());
			i++;
		}
		return map;
	}
	
	public String[] getDataFieldNames() {
		String[] fields = new String[dataLinks.size()+1];
		fields[0] = "URL";
		int i = 1;
		for (DataLink l : dataLinks) {
			String name = l.getDataType().getName();
			if (name!=null) 
				fields[i] = name;
			else 
				return new String[0]; // no header
			i++;
		}
		return fields;
	}
	
	public List<String> getNavigationXPaths() {
		List<String> navList = this.links.stream()
				.filter(l -> !l.isForm()).map(l -> l.getXPath()).collect(toList());
		navList.addAll(getMenuXPaths());
		return navList;
	}
	
	public List<String> getFormXPaths() {
		return this.links.stream()
				.filter(ClassLink::isForm)
				.map(ClassLink::getXPath).collect(toList());
	}
	
	public List<String> getMenuXPaths() {
		return menus.stream().flatMap(m -> m.getMenuXPaths().stream()).collect(toList());
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
	 * Adds a Menu item to this PageClass.
	 * @param href the href leading to the menu item
	 * @param menuXPath the xpath that identifies the menu
	 * @param type the menu type (fixed or mutable)
	 * @param dest the destination PageClass
	 */
	public void loadMenuLink(String href, String menuXPath, String type, PageClass dest) {
		String key = menuXPath, anchor = href;		
		if (menuXPath.isEmpty() && href.startsWith("(")) {
			int index = href.lastIndexOf(")");			
			key = href.substring(1, index);
			anchor = href.substring(index+1).replaceAll("\\[([0-9]+)\\]", "$1");
		}
		if (key!=null) {
			Menu menu = findOrCreateMenu(key);
			menu.addItem("", anchor, dest);
			menu.setType((type!=null) ? type : "");
		}
	}
	
	/**
	 * Adds a Menu to this PageClass identified by the given XPath.
	 * The hrefs list specifies the items of this menu.
	 * @param xpath the XPath leading to the menu
	 * @param href the hrefs that identify the menu elements
	 * @param dests the destination PageClasses
	 */
	public void addMenu(String sourceUrl, String xpath, List<String> hrefs, List<PageClass> dests) {
		Menu menu = findOrCreateMenu(xpath);
		for (int i=0; i<dests.size(); i++) {
			menu.addItem(sourceUrl, hrefs.get(i), dests.get(i));
		}
	}
	
	private Menu findOrCreateMenu(String xpath) {
		return menus.stream().filter(m -> m.getXpath().equals(xpath)).findFirst()
				.orElseGet(() -> { Menu m = new Menu(name, xpath); menus.add(m); return m; });
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
	 * @param text the anchor text of this link
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addSingletonLink(String xpath, String text, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest);
		link.setType((text.isEmpty()) ? "singleton" : text);
		return links.add(link);
	}
	
	/**
	 * Adds a ClassLink, reachable through a Form, to this PageClass
	 * @param xpath the composed XPath (how to reach the Form, what and how to fill it)
	 * @param dest the destination PageClass
	 * @return true if the Link was added
	 */
	public boolean addFormLink(String xpath, PageClass dest) {
		ClassLink link = new ClassLink(this, xpath, dest);
		link.setTypeForm();
		return links.add(link);
	}
	
	/**
	 * Returns true if this PageClass has a ClassLink of any type (Menu,List,Singleton) 
	 * identified by the specified xpath.
	 * @param xp the xpath
	 * @return true if there is a ClassLink with this xpath
	 */
	public boolean hasLink(String xp) {
		return links.stream().anyMatch(l -> l.getXPath().equals(xp)) || hasMenuLink(xp);
	}
	
	/**
	 * Returns true if the specified XPath leads to a Menu.
	 * @param xp the xpath
	 * @return true if this xpath identifies a menu
	 */
	public boolean hasMenuLink(String xp) {
		return menus.stream().anyMatch(m -> m.getXpath().equals(xp));
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
	 * Removes a link identified by this XPath
	 * @param xp the xpath
	 */
	public void removeLink(String xp) {
		if (!links.removeIf(l -> l.getXPath().equals(xp))) removeMenuLink(xp);
	}
	
	/**
	 * Removes all the links leading to menu items identified by this XPath
	 * @param xp the xpath leading to the menu
	 */
	public void removeMenuLink(String xp) {
		menus.removeIf(m -> m.getXpath().equals(xp));
	}
	
	public void changeDestinations(PageClass oldClass, PageClass newClass) {
		links.stream().filter(l -> l.getDestination().equals(oldClass))
		.forEach(l -> l.setDestination(newClass));
		menus.forEach(m -> m.changeDestination(oldClass, newClass));
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
		return this.links.isEmpty() && this.menus.isEmpty();
	}
	
	public boolean isDataPage() {
		return !this.dataLinks.isEmpty();
	}
	
	public double distance(PageClass other) {
		Set<ClassLink> links = getAllLinks();
		Set<ClassLink> otherLinks = other.getAllLinks();

		Set<ClassLink> union = new HashSet<>();
		Set<ClassLink> diff1 = new HashSet<>();
		Set<ClassLink> diff2 = new HashSet<>();
		Set<ClassLink> unionDiff = new HashSet<>();
		
		union.addAll(links);
		union.addAll(otherLinks);
		
		diff1.addAll(links);
		diff1.removeAll(otherLinks);

		diff2.addAll(otherLinks);
		diff2.removeAll(links);
		
		unionDiff.addAll(diff1);
		unionDiff.addAll(diff2);
		
		return (double) unionDiff.size() / (double) union.size();
	}
	
	public boolean isSubSet(PageClass other) {
		Set<ClassLink> links = getAllLinks();
		Set<ClassLink> otherLinks = other.getAllLinks();
		return !links.isEmpty() && ! otherLinks.isEmpty() && 
				(links.containsAll(otherLinks) || otherLinks.containsAll(links));
	}
	
	public int compareTo(PageClass pc2) {
		int cmpdepth = depth - pc2.getDepth();
		if (cmpdepth!=0) return cmpdepth;
		int cmpname = name.compareTo(pc2.getName());
		if (cmpname!=0) return cmpname;
		return website.compareTo(pc2.getDomain());
	}
	
	public String toString() {
		return links.stream().map(ClassLink::toString).reduce(String::concat).orElse("")+
			menus.stream().map(Menu::toString).reduce(String::concat).orElse("");
	}
	
	public int hashCode() {
		return Objects.hash(name, depth, website);
	}

	public boolean equals(Object obj) {		
		if (!(obj instanceof PageClass)) return false;
		PageClass other = (PageClass) obj;
		return Objects.equals(name, other.getName())
			&& depth==other.getDepth()
			&& website.equals(other.getDomain());
	}

}
