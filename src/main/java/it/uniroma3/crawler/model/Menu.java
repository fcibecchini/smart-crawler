package it.uniroma3.crawler.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toList;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

@NodeEntity
public class Menu implements Comparable<Menu> {
	private final static String FIXED = "fixed", MUTABLE = "mutable";
	
	private Long id;	
	private String xpath;
	private String type;
	@Transient private String className;
	
	@Relationship(type="MENU_LINK", direction=Relationship.OUTGOING)
	private Set<MenuItem> items;
	
	public Menu() {
		this.items = new HashSet<>();
	}
	
	public Menu(String className, String xpath) {
		this();
		this.className = className;
		this.xpath = xpath;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void setType() {
		this.type = (items.stream().anyMatch(MenuItem::isMutable)) ? MUTABLE : FIXED;		
	}
	
	public boolean isMutable() {
		return MUTABLE.equals(type);
	}
	
	public boolean isFixed() {
		return FIXED.equals(type);
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
	
	public String getXpath() {
		return this.xpath;
	}
	
	public List<String> getMenuXPaths() {
		return items.stream().map(MenuItem::getXPaths).flatMap(List::stream).collect(toList());
	}
	
	public PageClass getDestination(String xpath) {
		return items.stream().filter(i -> i.getXPaths().contains(xpath))
				.map(MenuItem::getDestination).findFirst().orElse(null);
	}
	
	public void setItems(Set<MenuItem> items) {
		this.items = items;
	}
	
	public int size() {
		return items.stream().mapToInt(MenuItem::size).sum();
	}
	
	public Set<ClassLink> toClassLinks() {
		return items.stream().map(i -> i.getXPaths().stream().map(xp -> {
			ClassLink cl = new ClassLink();
			cl.setXpath(xp);
			cl.setType("menu");
			cl.setDestination(i.getDestination());
			return cl;
		})).flatMap(s -> s).collect(toSet());
	}
	
	public void addItem(String sourceUrl, String href, PageClass dest) {
		MenuItem item = items.stream().filter(i -> i.getDestination().equals(dest))
				.findFirst().orElse(new MenuItem(this,dest));
		item.addHref(sourceUrl, href);
		items.add(item);
	}
	
	public String toString() {
		return items.stream().map(i -> i.toString()).reduce(String::concat).get();
	}
	
	@Override
	public int hashCode() {
		return xpath.hashCode();
	}
	
	@Override
	public int compareTo(Menu other) {
		return xpath.compareTo(other.xpath);
	}
	
	@Override
	public boolean equals(Object obj) {
		Menu other = (Menu) obj;
		return Objects.equals(xpath, other.xpath);
	}

}
