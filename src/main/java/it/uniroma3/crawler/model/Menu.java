package it.uniroma3.crawler.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;

import static java.util.stream.Collectors.toList;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Menu implements Comparable<Menu> {
	private final static String FIXED = "fixed", MUTABLE = "mutable";
	
	private Long id;	
	private String xpath;
	private String type;
	
	@Relationship(type="MENU_LINK", direction=Relationship.OUTGOING)
	private List<MenuItem> items;
	
	public Menu() {
		this.items = new ArrayList<>();
	}
	
	public Menu(String className, String xpath) {
		this();
		this.xpath = xpath;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void setType() {
		this.type = (items.stream().anyMatch(MenuItem::isMutable)) ? MUTABLE : FIXED;
		if (isMutable()) {
			int index = 1;
			for (MenuItem it : items) {
				index = it.useIndexes(index);
			}
		}
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
	
	public Set<String> getMenuKey() {
		Set<String> key = items.stream().map(i -> i.getDestination().getName()).collect(toSet());
		key.add(xpath);
		return key;
	}
	
	public void setItems(List<MenuItem> items) {
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
		.findFirst().orElseGet(() -> {MenuItem i = new MenuItem(this,dest);items.add(i);return i;});
		item.addHref(sourceUrl, href);
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
