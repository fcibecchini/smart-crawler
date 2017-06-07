package it.uniroma3.crawler.model;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Website implements Comparable<Website>{
	private Long id;
	@Index(unique=true, primary=true) private String domain;
	
	@Relationship(type="MODEL_LINK", direction=Relationship.OUTGOING)
	private SortedSet<ModelLink> models;
	
	public Website() {
		this.models = new TreeSet<>();
	}
	
	public Website(String domain) {
		this();
		this.domain = domain;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setModels(SortedSet<ModelLink> models) {
		this.models = models;
	}
	
	public boolean addModel(PageClass root, long timestamp) {
		int v = (!models.isEmpty()) ? models.last().getVersion()+1 : 1;
		return models.add(new ModelLink(this,root,timestamp,v));
	}
	
	public PageClass getNewestModel() {
		return models.last().getRoot();
	}
	
	public int size() {
		return models.size();
	}

	@Override
	public int hashCode() {
		return domain.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		Website other = (Website) obj;
		return Objects.equals(domain, other.getDomain());
	}

	@Override
	public int compareTo(Website other) {
		return domain.compareTo(other.getDomain());
	}
}
