package it.uniroma3.crawler.target;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import com.csvreader.CsvReader;

import it.uniroma3.crawler.model.PageClass;

public class CrawlTarget {
	private static final char DELIMITER = '\t';
	private URI urlBase;
	private String configFile;
	private PageClass entryClass;
	private HashSet<PageClass> pClasses;
	
	public CrawlTarget(String configFile) {
		this.configFile = configFile;
	}
	
	public URI getUrlBase() {
		return this.urlBase;
	}
	
	public PageClass getEntryPageClass() {
		return this.entryClass;
	}
	
	public HashSet<PageClass> getClasses() {
		return this.pClasses;
	}
	
	public void initCrawlingTarget() {
		try {
			CsvReader reader = new CsvReader(configFile, DELIMITER);
			reader.readRecord();
			String urlBase = reader.get(0);
			this.urlBase = URI.create(urlBase);
			
			this.pClasses = getPageClasses();
			while (reader.readRecord()) {
				PageClass pageSrc = getPageClass(pClasses, reader.get(0));
				String type = reader.get(1);
				String xpath = reader.get(2);
				PageClass pageDest = getPageClass(pClasses, reader.get(3));
				if (type.equals("link") && pageDest!=null) {
					pageSrc.addLink(xpath, pageDest);
				}
				else {
					pageSrc.addData(xpath, type);
				}
			}
			// set depth hierarchy for page classes
			setHierarchy();
			
		} catch (FileNotFoundException e) {
			System.err.println("Could not find target configuration file");
		} catch (IOException e) {
			System.err.println("Could not read target configuration file");
		} catch (IllegalArgumentException ie) {
			System.err.println("Not a valid url base");
		}
	}
	
	private HashSet<PageClass> getPageClasses() throws IOException {
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(configFile, DELIMITER);
		reader.readRecord(); // skip url base
		while (reader.readRecord()) {
			PageClass pClass = new PageClass(reader.get(0));
			if (entryClass==null) entryClass = pClass;
			pageClasses.add(pClass);
		}
		return pageClasses;
	}
	
	private PageClass getPageClass(HashSet<PageClass> pClasses, String name) {
		return pClasses.stream().filter(pc -> pc.getName().equals(name)).findAny().orElse(null);
	}
	
	private void setHierarchy() {
		Queue<PageClass> classes = new LinkedList<>();
		classes.add(entryClass);
		PageClass current, next = null;
		int depth = 0;
		while (!classes.isEmpty()) {
			current = classes.poll();
			current.setDepth(depth);
			if (!current.isEndPage()) {
				for (String xpath : current.getNavigationXPaths()) {
					next = current.getDestinationByXPath(xpath);
					// avoid page class loops
					if (!current.equals(next)) classes.add(next);
				}
			}
			depth++;
		}
	}

}
