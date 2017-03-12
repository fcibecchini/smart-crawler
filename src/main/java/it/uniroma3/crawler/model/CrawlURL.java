package it.uniroma3.crawler.model;

import java.net.URI;
import java.net.URISyntaxException;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class CrawlURL {
	private URI url;
	private HtmlPage pageContent;
	private PageClass pageClass;
	
	public CrawlURL(String url, PageClass pageClass) throws URISyntaxException {
		this.url = URI.create(url);
		this.pageClass = pageClass;
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
	
}
