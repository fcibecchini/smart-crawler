package it.uniroma3.crawler.actors.write;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.csvreader.CsvWriter;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.model.CrawlURL;

public class CrawlDataWriter extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private CsvWriter csvWriter;
	private File directory;
	private int counter;
	private CrawlController controller;
	
	public CrawlDataWriter() {
		String fileName = "./result"+getSelf().path().name()+".csv";
		this.csvWriter = new CsvWriter(fileName, '\t', Charset.forName("UTF-8"));
		this.counter = 0;
		this.directory = new File("html/");
		directory.mkdir();
		this.controller = CrawlController.getInstance();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof CrawlURL) {
			CrawlURL cUrl = (CrawlURL) message;
			HtmlPage page = cUrl.getPageContent();
			String pageClassName = cUrl.getPageClass().getName();
			String[] record = cUrl.getRecord();
			// send cUrl to scheduler
			controller.getScheduler().forward(cUrl, getContext());
			// save data of interest
			savePage(page, pageClassName);
			saveRecord(record);
		}
		else unhandled(message);
	}
	
	private void savePage(HtmlPage page, String pageClassName) {
		try {
			page.save(new File(directory.toString() + "/"+ getPageFileName(pageClassName)));
		} catch (IOException e) {
			log.error("Can't save Html page");
		}
	}
	
	private void saveRecord(String[] record) {
		if (record!=null) {
			try {
				csvWriter.writeRecord(record);
			} catch (IOException e) {
				log.error("Can't save record to csv");
			}
		}
	}
	
	private String getPageFileName(String pageClassName) {
		return pageClassName + ++counter+".html";
	}
	
	@Override
	public void postStop() {
		this.csvWriter.close();
	}

}
