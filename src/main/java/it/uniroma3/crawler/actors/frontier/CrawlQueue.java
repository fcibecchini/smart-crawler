package it.uniroma3.crawler.actors.frontier;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import static it.uniroma3.crawler.factories.CrawlURLFactory.getCrawlUrl;
import it.uniroma3.crawler.model.CrawlURL;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.util.FileUtils;

/**
 * A CrawlQueue is a queue of priority-ordered {@link CrawlURL} elements <b>of the same Host</b> with a fixed-size
 * in-memory capacity. 
 * <br>
 * When the in-memory side of the CrawlQueue is full, the exceeding CrawlURL elements are stored 
 * on a persistent-side file queue. 
 */
public class CrawlQueue {
	private static Logger log = Logger.getLogger(CrawlQueue.class.getName());
	private String storage;
	private String temp;

	private int max;
	private int sizeStorage;
	private Set<String> visited;
	private PageClass root;
	private TreeSet<CrawlURL> urls; // discovered URLs 
	
	/**
	 * Constructs a new CrawlQueue with the given maximum in-memory capacity<br>
	 * and the specified root {@link PageClass}. The initialized queue contains
	 * the seed of the corresponding web site.
	 * @param max the max number of elements that can be stored in memory
	 * @param root the root PageClass of a web site
	 */
	public CrawlQueue(int max, PageClass root) {
		this.max = max;
		this.visited = new HashSet<>();
		this.urls = new TreeSet<>();
		this.root = root;
		this.add(getCrawlUrl(root.getDomain(), root));
		this.storage = "src/main/resources/storage/queue_"
				+FileUtils.normalizeURL(root.getDomain())+".csv";
		this.temp = storage+"~";
	}
	
	/**
	 * Retrieves the next top-priority (in memory) {@link CrawlURL} from this queue.
	 * <br>
	 * Due to the low-efficient implementation of the persistent-side of this queue, 
	 * there could be elements more important then the one returned in the persistent-side. 
	 * @return the (in memory) top-priority CrawlURL, or null if the queue is empty
	 */
	public CrawlURL next() {
		if (urls.isEmpty() && sizeStorage>0) 
			dequeue(max);		
		CrawlURL next = urls.pollFirst();
		return next;
	}
	
	/**
	 * Adds the given {@link CrawlURL} to this queue if it has not been visited yet. 
	 * @param curl CrawlURL
	 * @return true if the CrawlURL was added to this queue, false if it was already visited
	 */
	public boolean add(CrawlURL curl) {
		if (visited.add(checksum(curl.getRelativeUrl()))) {
			addToQueue(curl);
			return true;
		}
		return false;
	}
	
	/**
	 * Convenience method to add a CrawlURL to this queue given a URL and a PageClass name.
	 * <br> The PageClass must be reachable from the root PageClass of this queue.
	 * @param url the URL to add
	 * @param className the PageClass name 
	 * @return true if the resulting CrawlURL was added to this queue, false if it was already visited
	 */
	public boolean add(String url, String className) {
		PageClass pclass = root.getDescendant(className);
		CrawlURL curl = getCrawlUrl(url, pclass);
		return (curl!=null) ? this.add(curl) : false;
	}
	
	/**
	 * Convenience method to re-add a previously visited CrawlURL to the queue.
	 * @param curl CrawlURL
	 */
	public void recover(CrawlURL curl) {
		addToQueue(curl);
	}
	
	/**
	 * Returns the total number of elements in the queue
	 * @return the size of this queue
	 */
	public int size() {
		return urls.size()+sizeStorage;
	}
	
	/**
	 * Returns true if no element is present
	 * @return true if the queue is empty
	 */
	public boolean isEmpty() {
		return size()==0;
	}
	
	/**
	 * Deletes the current Storage file if exists
	 * @return true if the Storage was deleted, false otherwise
	 */
	public boolean deleteStorage() {
		try {
			return Files.deleteIfExists(Paths.get(storage));
		} catch (IOException e) {
			return false;
		}
	}
	
	private void addToQueue(CrawlURL curl) {
		if (urls.size()<max)
			urls.add(curl);
		else {
			CrawlURL toStore = curl;
			CrawlURL last = urls.last();
			if (curl.compareTo(last)<=-1) {
				urls.pollLast();
				urls.add(curl);
				toStore = last;
			}
			enqueue(toStore);
		}
	}
	 
	/**
	 * Appends the given {@link CrawlURL} to the persistent-side queue.
	 * The priority order is not preserved
	 * @param curl the CrawlURL
	 */
	private void enqueue(CrawlURL curl) {		
		try {
			CsvWriter writer = new CsvWriter(new FileWriter(storage, true), '\t');
			writer.write(curl.getRelativeUrl());
			writer.write(curl.getPageClass().getName());
			writer.endRecord();
			writer.flush();
			writer.close();	
			sizeStorage++;
		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot store CURL to Queue Storage");
		}
	}
	
	/**
	 * Fills the queue with the given number of URLs from the persistent-side of the queue 
	 * <br><br>
	 * Note that there is no warranty that the given dequeued URLs are the most relevant
	 * with respect to the entire persistent-side queue
	 * @param quantity number of urls to retrieve
	 */
	private void dequeue(int quantity) {
		int count = quantity;
		try {
			CsvWriter writer = new CsvWriter(new FileWriter(temp), '\t');
			CsvReader reader = new CsvReader(storage, '\t');
			while (reader.readRecord()) {
				String relativeURL = reader.get(0);
				String name = reader.get(1);
				if (count>0) {
					PageClass pclass = root.getDescendant(name);
					String url = root.getDomain() + relativeURL;
					urls.add(getCrawlUrl(url,pclass));
					count--;
				}
				else writer.writeRecord(reader.getValues());
			}
			reader.close();
			writer.flush();
			writer.close();
			Files.move(Paths.get(temp), Paths.get(storage), REPLACE_EXISTING);
			
			sizeStorage -= (quantity-count);
		} catch (IOException ie) {
			log.log(Level.SEVERE, "Cannot retrieve CURL from Queue Storage: "+ie.getMessage());
		}
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
