package it.uniroma3.crawler;

import java.util.Queue;

import it.uniroma3.crawler.page.PageClass;
import it.uniroma3.crawler.scope.CrawlScope;

public class Crawler {
	private String scopeFile;
	
	public static void main(String[] args) {
		Crawler crawler = new Crawler("scope.csv");
		crawler.crawl();
	}
	
	public Crawler(String config) {
		this.scopeFile = config;
	}
	
	public void crawl() {
		CrawlScope scope = new CrawlScope(this.scopeFile);
		Queue<PageClass> pageTypes = scope.getPages();
		PageClass homePage = pageTypes.poll();
		CrawlController controller = CrawlController.getInstance();
		controller.fetchRequests(scope.getUrlBase().toString(), homePage, pageTypes);
		
	}	
	

}
