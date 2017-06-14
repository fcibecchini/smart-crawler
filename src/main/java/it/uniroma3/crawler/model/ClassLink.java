package it.uniroma3.crawler.model;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Transient;

@RelationshipEntity(type="CLASS_LINK")
public class ClassLink {
	private Long id;
	private String xpath;
	private String type;
	@Transient private String menuXPath;
	@StartNode private PageClass source;
	@EndNode private PageClass destination;
	
	public ClassLink() {}

	public ClassLink(PageClass source, String xpath, PageClass destination) {
		this.source = source;
		this.xpath = xpath;
		this.destination = destination;
		this.menuXPath = "";
	}
	
	public ClassLink(PageClass source, String xpath, PageClass destination, int index) {
		this(source,"("+xpath+")["+index+"]",destination);
		this.menuXPath = xpath;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getXPath() {
		return xpath;
	}
	
	public String getMenuXPath() {
		return menuXPath;
	}
	
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setSource(PageClass source) {
		this.source = source;
	}

	public void setTypeMenu() {
		this.type = "menu";
	}
	
	public void setTypeList() {
		this.type = "list";
	}
	
	public void setTypeSingleton() {
		this.type = "singleton";
	}
	
	public String getType() {
		return this.type;
	}
	
	public boolean isMenu() {
		return type.equals("menu");
	}
	
	public boolean isList() {
		return type.equals("list");
	}
	
	public boolean isSingleton() {
		return type.equals("singleton");
	}

	public PageClass getDestination() {
		return destination;
	}
	
	public String toString() {
		return "["+xpath.toString()+", "+destination.getName()+"]";
	}
	
}
