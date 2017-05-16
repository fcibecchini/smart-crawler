package it.uniroma3.crawler.actors;

import java.io.FileWriter;
import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//
//import static java.util.stream.Collectors.toMap;
//import static java.util.stream.Collectors.toSet;
//import static java.util.stream.Collectors.toList;
//
//import com.csvreader.CsvReader;
//import it.uniroma3.crawler.model.OutgoingLink;

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
	
	/*
	private void resolveUrls(ResolveLinksMsg msg) {
		ExtractedLinksMsg result = new ExtractedLinksMsg(resolve(msg));
		sender().tell(result, self());
	}
	
	private Map<String, List<OutgoingLink>> resolve(ResolveLinksMsg msg) {
		Map<String, List<String>> xp2urls = msg.getLinks();
		Set<OutgoingLink> inCache = cachedURLs(xp2urls);
		
		return xp2urls.keySet().stream()
			.collect(toMap(Function.identity(), 
					xp -> xp2urls.get(xp).stream()
					.map(u -> get(inCache, u)).collect(toList())));
	}
	
	private OutgoingLink get(Set<OutgoingLink> inCache, String url) {
		return inCache.stream().filter(ol -> ol.getUrl().equals(url)).findAny().get();
	}
	
	private Set<OutgoingLink> cachedURLs(Map<String, List<String>> xp2urls) {
		Set<OutgoingLink> cached = 
				xp2urls.values().stream().flatMap(List::stream)
				.map(u -> new OutgoingLink(u)).collect(toSet());
				
		try {
			CsvReader reader = new CsvReader(csv, '\t');
			while (reader.readRecord()) {
				for (OutgoingLink outl : cached) {
					if (reader.get(0).equals(outl.getUrl()))
						outl.setCachedFile(reader.get(2));
				}
			}
		} catch (IOException ie) {
			log().warning("Could not read URL-PAGECLASS-FILEPATH Cache");
		}
		return cached;
	}
	*/

}
