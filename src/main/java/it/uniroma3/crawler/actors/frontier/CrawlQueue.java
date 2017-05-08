package it.uniroma3.crawler.actors.frontier;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.util.List;

import static it.uniroma3.crawler.factories.CrawlURLFactory.getCrawlUrl;

import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;

public class CrawlQueue {
	private static Logger log = Logger.getLogger(CrawlQueue.class.getName());
	private final static String STORAGE = "src/main/resources/storage/queue.csv";
	private final static String TEMP = STORAGE+"~";

	private int maxsize;
	private Map<String,Set<String>> visited;
	private Map<String,PageClass> rootClasses;
	private PriorityQueue<CrawlURL> maxPriorityUrls;
	private PriorityQueue<CrawlURL> minPriorityUrls;
		
	public CrawlQueue(int max) {
		this.maxsize = max;
		this.visited = new HashMap<>();
		this.rootClasses = new HashMap<>();
		this.maxPriorityUrls = new PriorityQueue<>();
		this.minPriorityUrls = new PriorityQueue<>((c1,c2) -> c2.compareTo(c1));
	}
	
	public CrawlURL next() {
		if (maxPriorityUrls.isEmpty()) {
			List<CrawlURL> loaded = retrieve(maxsize);
			maxPriorityUrls.addAll(loaded);
			minPriorityUrls.addAll(loaded);
		}
		CrawlURL next = maxPriorityUrls.poll();
		minPriorityUrls.remove(next);
		return next;
	}
	
	public boolean add(CrawlURL curl) {
		String domain = curl.getDomain();
		if (visited.get(domain)==null) {
			visited.put(domain, new HashSet<>());
			rootClasses.put(domain, curl.getPageClass());
		}
		Set<String> visitedUrls = visited.get(domain);
		URI url = curl.getUrl();
		String cs = checksum(url.getPath()+url.getQuery());
		if (!visitedUrls.contains(cs)) {
			visitedUrls.add(cs);
			
			if (maxPriorityUrls.size()<maxsize) {
				maxPriorityUrls.add(curl);
				minPriorityUrls.add(curl);
			}
			else {
				CrawlURL toStore = curl;
				CrawlURL last = minPriorityUrls.peek();
				if (curl.compareTo(last)<=-1) {
					maxPriorityUrls.remove(last);
					maxPriorityUrls.add(curl);
					minPriorityUrls.poll();
					minPriorityUrls.add(curl);
					toStore = last;
				}
				store(toStore);
			}
			return true;
		}
		return false;
	}
	
	public int size() {
		return maxPriorityUrls.size();
	}
	
	public boolean contains(CrawlURL curl) {
		return maxPriorityUrls.contains(curl) 
				&& minPriorityUrls.contains(curl);
	}
	
	public boolean isEmpty() {
		return maxPriorityUrls.isEmpty()
				&& minPriorityUrls.isEmpty();
	}
	
	public boolean deleteStorage() {
		try {
			return Files.deleteIfExists(Paths.get(STORAGE));
		} catch (IOException e) {
			return false;
		}
	}
	
	private void store(CrawlURL curl) {
		PageClass pc = curl.getPageClass();
		
		String url = curl.getStringUrl();
		String site = curl.getDomain();
		String name = pc.getName();
		String depth = String.valueOf(pc.getDepth());
		
		String[] toWrite = new String[]{url,site,name,depth};
		try {
			Path stor = Paths.get(STORAGE);
			CsvWriter writer = new CsvWriter(new FileWriter(TEMP), '\t');
			boolean written = false;
			if (Files.exists(stor)) {
				CsvReader reader = new CsvReader(STORAGE, '\t');
				while (reader.readRecord()) {
					String[] current = reader.getValues();
					if (!written && compare(toWrite,current)<=-1) {
						writer.writeRecord(toWrite);
						written = true;
					}
					writer.writeRecord(current);
				}
				reader.close();
			}
			if (!written) 
				writer.writeRecord(toWrite);
			writer.flush();
			writer.close();			
			Files.move(Paths.get(TEMP), stor, REPLACE_EXISTING);
		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot store CURL to Queue Storage");
		}
	}
	
	private List<CrawlURL> retrieve(int quantity) {
		List<CrawlURL> curls = new ArrayList<>();
		int count = quantity;
		try {
			CsvWriter writer = new CsvWriter(new FileWriter(TEMP), '\t');
			CsvReader reader = new CsvReader(STORAGE, '\t');
			while (reader.readRecord()) {
				String url = reader.get(0);
				String website = reader.get(1);
				String name = reader.get(2);
				if (count>0) {
					PageClass pclass = rootClasses.get(website).getDescendant(name);
					curls.add(getCrawlUrl(url,pclass));
					count--;
				}
				else 
					writer.writeRecord(new String[]{url,website,name});
			}
			reader.close();
			writer.flush();
			writer.close();
			Files.move(Paths.get(TEMP), Paths.get(STORAGE), REPLACE_EXISTING);
		} catch (IOException ie) {
			log.log(Level.SEVERE, "Cannot retrieve CURL from Queue Storage");
		}
		return curls;
	}
	
	private int compare(String[] record1, String[] record2) {
		int cmp1 = Integer.valueOf(record1[3]) - Integer.valueOf(record2[3]);
		if (cmp1!=0) return cmp1;
		int cmp2 = record1[1].compareTo(record2[1]);
		if (cmp2!=0) return cmp2;
		int cmp3 = record1[2].compareTo(record2[2]);
		if (cmp3!=0) return cmp3;	
		return record1[0].compareTo(record2[0]);
	}
	
    private String checksum(String input)  {
    	MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA");
	        final byte[] checkSumBytes = md.digest(input.getBytes());
	        final String result = new String(checkSumBytes);
	        return result;
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Cannot make URL checksum");
			return null;
		}
    }
}
