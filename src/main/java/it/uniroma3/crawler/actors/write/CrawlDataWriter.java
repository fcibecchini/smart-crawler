package it.uniroma3.crawler.actors.write;

import java.io.File;
import java.nio.charset.Charset;

import com.csvreader.CsvWriter;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.UntypedActor;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlDataWriter extends UntypedActor {
	private CsvWriter csvWriter;
	private File directory;
	private int counter;
	
	public CrawlDataWriter() {
		this.csvWriter = new CsvWriter
				("./result.csv", '\t', Charset.forName("UTF-8"));
		this.counter = 0;
		this.directory = new File("html/");
		directory.mkdir();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof String[]) {
			String[] record = (String[]) message;
			this.csvWriter.writeRecord(record);
		}
		else if (message instanceof CrawlURL) {
			CrawlURL cUrl = (CrawlURL) message;
			HtmlPage page = cUrl.getPageContent();
			page.save(new File(getPageFileName(cUrl)));
		}
		else unhandled(message);
	}
	
	private String getPageFileName(CrawlURL cUrl) {
		return directory + cUrl.getPageClass().getName() + ++counter+".html";
	}

}
