package it.uniroma3.crawler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.stream.Collectors.toList;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.AttributeConverter;

@RelationshipEntity(type="MENU_LINK")
public class MenuItem {
	
	public static class ItemConverter implements 
			AttributeConverter<Map<String, List<String>>, List<String>> {

		public List<String> toGraphProperty(Map<String, List<String>> value) {
			return value.values().stream().flatMap(List::stream).distinct().collect(toList());
		}
		public Map<String, List<String>> toEntityAttribute(List<String> value) {
			return new HashMap<>();
		}
	}
	
	private Long id;
	
	@Property(name="anchors")
	@Convert(ItemConverter.class)
	private Map<String,List<String>> page2hrefs;
	
	@StartNode private Menu menuSource;
	@EndNode private PageClass destination;

	public MenuItem() {
		this.page2hrefs = new HashMap<>();
	}
	
	public MenuItem(Menu menu, PageClass dest) {
		this();
		this.menuSource = menu;
		this.destination = dest;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Menu getMenuSource() {
		return menuSource;
	}

	public void setMenuSource(Menu menuSource) {
		this.menuSource = menuSource;
	}

	public PageClass getDestination() {
		return destination;
	}

	public void setDestination(PageClass destination) {
		this.destination = destination;
	}

	public void setHrefs(Map<String,List<String>> page2hrefs) {
		this.page2hrefs = page2hrefs;
	}
	
	public boolean isMutable() {
		if (page2hrefs.size()<=1) return false;
		Iterator<List<String>> it = page2hrefs.values().iterator();
		return Collections.disjoint(it.next(), it.next());
	}
	 
	public List<String> getXPaths() {
		return page2hrefs.values().stream().flatMap(List::stream).distinct()
				.map(this::getXpath).collect(toList());
	}
	
	public void addHref(String sourceUrl, String href) {
		this.page2hrefs.computeIfAbsent(sourceUrl, k -> new ArrayList<>()).add(href);
	}
	
	public void collapse(MenuItem other) {
		other.page2hrefs.entrySet()
		.forEach(e -> e.getValue().forEach(h -> this.addHref(e.getKey(), h)));
	}
	
	public int size() {
		return (int) page2hrefs.values().stream().flatMap(List::stream).distinct().count();
	}
	
	public String getXpath(String href) {
		return (!href.matches("[0-9]+")) ? hrefXPath(href) : indexXPath(href);
	}
	
	private String hrefXPath(String href) {
		return menuSource.getXpath()+"[@href=\""+href+"\"]";
	}
	
	private String indexXPath(String index) {
		return "("+menuSource.getXpath()+")["+index+"]";
	}
	
	public String getRecord(String href) {
		return menuSource.getClassName()+"\t"+
				"link\t"+
				href+"\t"+
				destination.getName()+"\t"+
				"menu\t"+
				menuSource.getXpath()+"\t"+
				menuSource.getType()+"\n";
	}
	
	public String toString() {
		return page2hrefs.values().stream()
			.flatMap(List::stream).distinct()
			.map(this::getRecord)
			.reduce(String::concat).get();
	}
	
	@Override
	public int hashCode() {
		return destination.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MenuItem)) return false;
		MenuItem other = (MenuItem) obj;
		return Objects.equals(destination, other.destination);
	}
	
}
