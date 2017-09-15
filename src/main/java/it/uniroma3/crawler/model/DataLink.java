package it.uniroma3.crawler.model;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type="DATA_LINK")
public class DataLink {
	@GraphId private Long id;
	@Property private String xpath;
	@StartNode private PageClass source;
	@EndNode private DataType dataType;
	
	public DataLink() {}

	public DataLink(PageClass source, String xpath, DataType dataType) {
		this.source = source;
		this.xpath = xpath;
		this.dataType = dataType;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public String getXPath() {
		return xpath;
	}

	public void setXPath(String xpath) {
		this.xpath = xpath;
	}

	public PageClass getSource() {
		return source;
	}

	public void setSource(PageClass source) {
		this.source = source;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}
	
}
 