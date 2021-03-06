package it.uniroma3.crawler.model;

import java.util.Objects;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type="CLASS_LINK")
public class ClassLink {
	private Long id;
	private String xpath;
	private String type;
	@StartNode private PageClass source;
	@EndNode private PageClass destination;
	
	public ClassLink() {}

	public ClassLink(PageClass source, String xpath, PageClass destination) {
		this.source = source;
		this.xpath = xpath;
		this.destination = destination;
		this.type = "";
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
	
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setSource(PageClass source) {
		this.source = source;
	}
	
	public void setTypeList() {
		this.type = "list";
	}
	
	public void setTypeForm() {
		this.type = "form";
	}
	
	public String getType() {
		return this.type;
	}
	
	public boolean isList() {
		return type.equals("list");
	}
	
	public boolean isSingleton() {
		return !type.isEmpty() && !(isList() || isForm());
	}
	
	public boolean isForm() {
		return type.equals("form");
	}
	
	public void setDestination(PageClass destination) {
		this.destination = destination;
	}

	public PageClass getDestination() {
		return destination;
	}
	
	public String toString() {
		return source.getName()+"\tlink\t"+xpath+"\t"+destination.getName()+"\t"+type+"\n";
	}
	
	public int hashCode() {
		return Objects.hash(xpath,type);
	}
	
	public boolean equals(Object other) {
		ClassLink o = (ClassLink) other;
		return Objects.equals(xpath, o.getXPath()) 
				&& (Objects.equals(type, o.getType()) || (isSingleton() && o.isSingleton()));
	}
	
}
