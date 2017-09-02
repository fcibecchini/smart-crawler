package it.uniroma3.crawler.model;

import static java.util.stream.Collectors.toSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class CrawlURL implements Comparable<CrawlURL> {
	private URI url;
	private String formParameters;
	private PageClass pageClass;
	private Map<String, String> outLinks;
	private String[] record;
	
	public CrawlURL(String url, PageClass pageClass) throws URISyntaxException {
		this(URI.create(url), pageClass);
	}
	
	public CrawlURL(String url, String formP, PageClass pageClass) throws URISyntaxException {
		this(URI.create(url), pageClass);
		this.formParameters = formP;
	}
	
	public CrawlURL(URI uri, PageClass pageClass) {
		this.url = uri;
		this.pageClass = pageClass;
		this.outLinks = new HashMap<>();
	}
	
	public URI getUrl() {
		return url;
	}
	
	public String getStringUrl() {
		return url.toString();
	}
	
	public List<NameValuePair> getFormParameters() {
		List<NameValuePair> pairs = new ArrayList<>();
		if (formParameters!=null) {
			for (String pair : formParameters.split(";")) {
				String[] parts = pair.split("=");
				if (parts.length==2)
					pairs.add(new NameValuePair(parts[0], parts[1]));
			}
		}
		return pairs;
	}
	
	public String getDomain() {
		return pageClass.getDomain();
	}
	
	public PageClass getPageClass() {
		return this.pageClass;
	}

	public void addOutLink(String link, String pClass) {
		this.outLinks.putIfAbsent(link, pClass);
	}
	
	public Set<String> getOutLinks() {
		return outLinks.keySet().stream().collect(toSet());
	}
	
	public String getOutLinkPageClass(String link) {
		return outLinks.get(link);
	}
	
	public void setRecord(String[] record) {
		this.record = record;
	}
	
	public String[] getRecord() {
		return this.record;
	}
	
	public String getRelativeUrl() {
		String path = url.getPath();
		String query = url.getQuery();
		if (path==null || path.equals("/")) 
			return formatFormParameters();
		if (query==null) 
			return path+formatFormParameters();
		return path+query+formatFormParameters();
	}
	
	private String formatFormParameters() {
		if (formParameters==null) return "";
		return ">"+formParameters;
	}
	
	public int compareTo(CrawlURL c2) {
		int cmpPc = getPageClass().compareTo(c2.getPageClass());
		if (cmpPc!=0) return cmpPc;
		int cmpUrl = url.compareTo(c2.getUrl());
		if (cmpUrl!=0) return cmpUrl;
		if (formParameters==null && c2.formParameters!=null) return -1;
		if (formParameters!=null && c2.formParameters==null) return 1;
		return (formParameters!=null && c2.formParameters!=null) ? 
				formParameters.compareTo(c2.formParameters) : 0;
	}
	
	public String toString() {
		return "[URL: "+url.toString()+", PARAMS: "+formParameters+", "
				+ "CLASS: "+pageClass.getName()+"]";
	}
	
	public int hashCode() {
		return Objects.hash(url, formParameters, pageClass);
	}

	public boolean equals(Object obj) {
		if (obj instanceof CrawlURL) {
			CrawlURL other = (CrawlURL) obj;
			return Objects.equals(url, other.getUrl())
					&& Objects.equals(formParameters, other.formParameters)
					&& Objects.equals(pageClass, other.getPageClass());
		}
		return false;
	}
	
}
