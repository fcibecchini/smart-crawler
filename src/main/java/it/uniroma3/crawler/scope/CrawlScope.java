package it.uniroma3.crawler.scope;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import com.csvreader.CsvReader;

import it.uniroma3.crawler.page.PageClass;

@Deprecated
public class CrawlScope {
	private static final char DELIMITER = '\t';
	private URI urlBase;
	private Queue<PageClass> pageTypes;
	
	public CrawlScope(String config) {
		pageTypes = initScope(config);
	}
	
	public Queue<PageClass> getPages() {
		return this.pageTypes;
	}
	
	public URI getUrlBase() {
		return this.urlBase;
	}
	
	private Queue<PageClass> initScope(String config) {
		return parseScopeFile(config);
	}
	
	private Queue<PageClass> parseScopeFile(String config) {
		Queue<PageClass> pageTypes = new LinkedList<>();
		try {
			CsvReader reader = new CsvReader(config, DELIMITER);
			reader.readRecord();
			String urlBase = reader.get(0);
			this.urlBase = URI.create(urlBase);

			PageClass pc = null;
			while (reader.readRecord()) {
				String pageSrc = reader.get(0);
				String functionName = reader.get(1);
				String xPath = reader.get(2);
				String pageDst = reader.get(3);
				
				List<PageClass> temp = pageTypes.stream().filter(page -> page.getName().equals(pageSrc)).collect(Collectors.toList());
				if (temp.isEmpty()) {
					pc = new PageClass(pageSrc);
					pageTypes.add(pc);
				}
				else 
					pc = temp.get(0);
				pc.add(xPath, functionName, pageDst);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Could not find scope configuration file");
		} catch (IOException e) {
			System.err.println("Could not read scope configuration file");
		} catch (IllegalArgumentException ie) {
			System.err.println("Not a valid url base");
		}
		return pageTypes;
	}

}
