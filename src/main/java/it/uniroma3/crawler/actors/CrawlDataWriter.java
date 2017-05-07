package it.uniroma3.crawler.actors;

import java.io.FileWriter;
import java.io.IOException;

import com.csvreader.CsvWriter;

import akka.actor.AbstractLoggingActor;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlDataWriter extends AbstractLoggingActor {
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(CrawlURL.class, this::write)
		.build();
	}
	
	private void write(CrawlURL curl) {
		PageClass src = curl.getPageClass();
		String dir = FileUtils.getWriteDir("html", src.getDomain());
		String[] record = curl.getRecord();
		if (record!=null) saveRecord(record, src.getName(), dir);
		
		//TODO
		//saveWARC(curl)
	}
	
	private void saveRecord(String[] record, String className, String dir) {
		String output = dir+"/"+className+".csv";
		try {
			CsvWriter writer = new CsvWriter(new FileWriter(output, true), '\t');
			writer.writeRecord(record);
			writer.close();
		} catch (IOException e) {
			log().error("Can't save record to csv");
		}
	}

}
