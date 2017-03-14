package it.uniroma3.crawler.actors.write;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.csvreader.CsvWriter;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import it.uniroma3.crawler.actors.schedule.CrawlLinkScheduler;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlDataWriter extends UntypedActor {
	private CsvWriter csvWriter;
	private File directory;
	private int counter;
	private ActorRef linkScheduler;
	
	public CrawlDataWriter() {
		this.csvWriter = new CsvWriter
				("./result.csv", '\t', Charset.forName("UTF-8"));
		this.counter = 0;
		this.directory = new File("html/");
		directory.mkdir();
		this.linkScheduler = getContext().actorOf(Props.create(CrawlLinkScheduler.class));
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof CrawlURL) {
			CrawlURL cUrl = (CrawlURL) message;
			HtmlPage page = cUrl.getPageContent();
			String pageClassName = cUrl.getPageClass().getName();
			String[] record = cUrl.getRecord();
			// send cUrl to scheduler
			linkScheduler.tell(cUrl, getSelf());
			// save data of interest
			savePage(page, pageClassName);
			saveRecord(record);
		}
		else unhandled(message);
	}
	
	private void savePage(HtmlPage page, String pageClassName) throws IOException {
		page.save(new File(getPageFileName(pageClassName)));
	}
	
	private void saveRecord(String[] record) throws IOException {
		if (record!=null) csvWriter.writeRecord(record);
	}
	
	private String getPageFileName(String pageClassName) {
		return directory + pageClassName + ++counter+".html";
	}

}
