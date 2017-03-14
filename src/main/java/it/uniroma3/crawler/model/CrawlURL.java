package it.uniroma3.crawler.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class CrawlURL {
	private URI url;
	private HtmlPage pageContent;
	private PageClass pageClass;
	private Map<String,PageClass> outLinks;
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
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrawlURL other = (CrawlURL) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
	
}
