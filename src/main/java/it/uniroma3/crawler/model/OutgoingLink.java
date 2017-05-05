package it.uniroma3.crawler.model;

import java.util.Objects;

public class OutgoingLink {
	
	private String url;
	private String cachedFile;
	
	public OutgoingLink(String url) {
		this.url = url;
	}
	
	public OutgoingLink(String url, String cachedFile) {
		this(url);
		this.cachedFile = cachedFile;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCachedFile() {
		return cachedFile;
	}

	public void setCachedFile(String cachedFile) {
		this.cachedFile = cachedFile;
	}

	public int hashCode() {
		return url.hashCode();
	}

	public boolean equals(Object obj) {
		OutgoingLink other = (OutgoingLink) obj;
		return Objects.equals(url, other.getUrl());
	}

}
