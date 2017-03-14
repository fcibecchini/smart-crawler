package it.uniroma3.crawler;

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
		CrawlController controller = CrawlController.getInstance();
		controller.setTarget(scopeFile, 1500, 1000, 5);
		controller.startCrawling();
	}
}
