package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.csvreader.CsvReader;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.Creator;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;

public class StaticModeler extends AbstractLoggingActor implements WebsiteModeler {
	private static final char DELIMITER = '\t';
	
	private Website website;
	private int wait;
	private String configFile;
	
	static class InnerProps implements Creator<StaticModeler> {
		private static final long serialVersionUID = 1L;
		
		private Website website;
		private int wait;
		private String config;
		
		public InnerProps(Website website, int wait, String config) {
			this.website = website;
			this.wait = wait;
			this.config = config;
		}

		@Override
		public StaticModeler create() throws Exception {
			return new StaticModeler(website, wait, config);
		}	
	}
	
	public static Props props(Website website, int wait, String config) {
		return Props.create(StaticModeler.class, new InnerProps(website,wait,config));
	}
	
	public StaticModeler(Website website, int wait, String configFile) {
		this.website = website;
		this.wait = wait;
		this.configFile = configFile;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> start())
		.build();
	}
	
	private void start() {
		PageClass root = compute();
		context().parent().tell(root, self());
	}

	@Override
	public PageClass compute() {
		PageClass root = null;
		try {
			CsvReader reader = new CsvReader(configFile, DELIMITER);
			
			Set<PageClass> classes = getPageClasses();
			while (reader.readRecord()) {
				PageClass pageSrc = getPageClass(classes, reader.get(0));
				if (root==null) root = pageSrc;
				String type = reader.get(1);
				String xpath = reader.get(2);
				PageClass pageDest = getPageClass(classes, reader.get(3));
				if (type.equals("link")) {
					if (pageDest!=null) {
						String subtype = reader.get(4);
						if (subtype!=null) {
							switch(subtype) {
								case "menu":
									pageSrc.addMenuLink(xpath, pageDest);
									break;
								case "list":
									pageSrc.addListLink(xpath, pageDest);
									break;
								case "singleton":
									pageSrc.addSingletonLink(xpath, pageDest);
									break;
							}
						}
						else pageSrc.addPageClassLink(xpath, pageDest);
					}
					else log().warning("Could not find "+reader.get(3));
				}
				else pageSrc.addData(xpath, type);
			}
			reader.close();
		} catch (IOException e) {
			log().error("Could not read target configuration file");
			return null;
		}
		setHierarchy(root);
		return root;
	}

	private Set<PageClass> getPageClasses() throws IOException {
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(configFile, DELIMITER);
		while (reader.readRecord()) {
			PageClass pClass1 = new PageClass(reader.get(0),website,wait);
			PageClass pClass2 = new PageClass(reader.get(3),website,wait);
			pageClasses.add(pClass1);
			pageClasses.add(pClass2);
		}
		reader.close();
		return pageClasses;
	}
	
	private PageClass getPageClass(Set<PageClass> pClasses, String name) {
		return pClasses.stream().filter(pc -> pc.getName().equals(name)).findAny().orElse(null);
	}

}
