package it.uniroma3.crawler.actors;

import java.io.FileWriter;
import java.io.IOException;

import com.csvreader.CsvWriter;

import akka.actor.AbstractLoggingActor;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.util.FileUtils;

public class CrawlUrlClass extends AbstractLoggingActor {
	private final static String DIRECTORY = "src/main/resources/repository/";

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(SaveCacheMsg.class, this::writeCache)
		.build();
	}
	
	private void writeCache(SaveCacheMsg msg) {
		try {
			String csv = DIRECTORY+FileUtils.normalizeURL(msg.getDomain())+".csv";
			CsvWriter writer = new CsvWriter(new FileWriter(csv, true), '\t');
			writer.write(msg.getUrl());
			writer.write(msg.getPageClass());
			writer.write(msg.getFilePath());
			writer.endRecord();
			writer.flush();
		} catch (IOException e) {
			log().warning("Could not write URL-PAGECLASS-FILEPATH Cache");
		}
	}

}
