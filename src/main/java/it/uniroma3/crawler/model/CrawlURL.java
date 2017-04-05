package it.uniroma3.crawler.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class CrawlURL implements Comparable<CrawlURL>, Serializable {
	
	private static final long serialVersionUID = 5388473492348802891L;
		
	private transient URI url;
	
	private PageClass pageClass;
	
	private HtmlPage pageContent;
	
	private transient Map<String,PageClass> outLinks;
	
	private String[] record;
	
	public CrawlURL(String url, PageClass pageClass) throws URISyntaxException {
		this.url = URI.create(url);
		this.pageClass = pageClass;
		this.outLinks = new HashMap<>();
		this.record = null;
	}
	
	public URI getUrl() {
		return url;
	}
	
	public String getStringUrl() {
		return url.toString();
	}

	public HtmlPage getPageContent() {
		return pageContent;
	}
	
	public void setPageContent(HtmlPage pageContent) {
		this.pageContent = pageContent;
	}
	
	public PageClass getPageClass() {
		return this.pageClass;
	}
	
	public boolean addOutLink(String url, PageClass pClass) {
		if (outLinks.containsKey(url)) 
			return false;
		this.outLinks.put(url, pClass);
		return true;
	}
	
	public List<String> getOutLinks() {
		return outLinks.keySet().stream().collect(Collectors.toList());
	}
	
	public PageClass getOutLinkPageClass(String url) {
		return outLinks.get(url);
	}
	
	public void setRecord(String[] record) {
		this.record = record;
	}
	
	public String[] getRecord() {
		return this.record;
	}
	
	public int compareTo(CrawlURL c2) {
		return this.getPageClass().compareTo(c2.getPageClass());
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeUTF(url.toString());
		out.writeObject((outLinks.isEmpty()) ? null : outLinks);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		url = URI.create(in.readUTF());
		Map<String, PageClass> outs = (Map<String, PageClass>) in.readObject();
		outLinks = (outs != null) ? outs : new HashMap<>();
	}
	
	public String toString() {
		return "[URL: "+url.toString()+", CLASS: "+pageClass.getName()+"]";
	}
	
	public int hashCode() {
		return url.hashCode();
	}

	public boolean equals(Object obj) {
		CrawlURL other = (CrawlURL) obj;
		return Objects.equals(url, other.getUrl());
	}
	
}
