package it.uniroma3.crawler.model;

import java.util.Objects;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type="MODEL_LINK")
public class ModelLink implements Comparable<ModelLink> {
	@GraphId private Long id;
	@Property private int version;
	@Property private long timestamp;
	@StartNode private Website site;
	@EndNode private PageClass root;
	
	public ModelLink() {}
	
	public ModelLink(Website site, PageClass root, long timestamp, int v) {
		this.site = site;
		this.root = root;
		this.timestamp = timestamp;
		this.version = v;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Website getSite() {
		return site;
	}

	public void setSite(Website site) {
		this.site = site;
	}

	public PageClass getRoot() {
		return root;
	}

	public void setRoot(PageClass root) {
		this.root = root;
	}
	
	public int getVersion() {
		return version;
	}
	
	public void setVersion(int v) {
		this.version = v;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(site, root, timestamp);
	}
	
	@Override
	public boolean equals(Object obj) {
		ModelLink other = (ModelLink) obj;
		return Objects.equals(site, other.getSite())
				&& Objects.equals(root, other.getRoot())
				&& Objects.equals(version, other.getVersion());
	}

	@Override
	public int compareTo(ModelLink o) {
		int cmp0 = (int) (version - o.getVersion());
		if (cmp0!=0) return cmp0;
		int cmp1 = site.compareTo(o.getSite());
		if (cmp1!=0) return cmp1;
		return root.compareTo(o.getRoot());
	}
	
}
