package it.uniroma3.crawler.actors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.createDirectories;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		String[] record = curl.getRecord();
		if (record!=null) {
						
			String[] newRec = new String[record.length+1];
			newRec[0] = curl.getStringUrl();
			for (int i=0;i<record.length;i++) {
				newRec[i+1] = record[i];
			}
			
			String output = FileUtils.getRecordDirectory(src.getDomain());
			Path dir = Paths.get(output);
	    	if (!exists(dir)) {
				try {
					createDirectories(dir);
				} catch (IOException e) {
					log().error("Can't create output directory");
					return;
				}
	    	}
			String file = dir+"/"+src.getName()+".csv";

			if (!exists(Paths.get(file))) // save file header
				saveRecord(src.getDataFieldNames(), file);
			saveRecord(newRec, file);
			
			//TODO
			//saveWARC(curl)
		}
	}
	
	private void saveRecord(String[] record, String file) {
		try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true),
				StandardCharsets.UTF_8)) {
			if (record.length==2 && record[1].contains("\t"))
				writeMultipleRecords(out, record);
			else
				writeSingleRecord(out, record);
		} catch (IOException e) {
			log().error("Can't save record to csv");
		}
	}
	
	private void writeSingleRecord(Writer csvOutput, String[] record) 
			throws IOException {
		int len = record.length;
		for (int i=0;i<len;i++) {
			csvOutput.write(record[i]);
			if (i<len-1)
				csvOutput.write("\t");
		}
		if (len>0) csvOutput.write("\n");
		csvOutput.close();
	}
	
	private void writeMultipleRecords(Writer csvOutput, String[] records) 
			throws IOException {
		for (String r : records[1].split("\t")) {
			csvOutput.write(records[0]+"\t"+r);
			csvOutput.write("\n");
		}
		csvOutput.close();
	}

}
