package it.uniroma3.crawler.modeler;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.csvreader.CsvReader;

import akka.actor.AbstractLoggingActor;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.persistence.PageClassService;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import static it.uniroma3.crawler.util.Commands.STOP;

public class ModelerService extends AbstractLoggingActor {
	private static final String CSV_PATH = "src/main/resources/targets/";
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(SeedConfig.class, this::load)
		.match(PageClass.class, this::save)
		.build();
	}
	
	private void save(PageClass root) {
		PageClassService serv = new PageClassService();
		serv.saveModel(root, root.getDomain());
		context().parent().tell(STOP, self());
	}
	
	private void load(SeedConfig conf) {
		PageClass root = (conf.file==null) ? loadDB(conf) : loadCSV(conf);
		context().parent().tell(root, self());
		context().parent().tell(STOP, self());
	}
	
	private PageClass loadDB(SeedConfig conf) {
		PageClassService serv = new PageClassService();
		PageClass root = serv.getModel(conf.site);
		setConf(root,conf);
		return root;
	}
	
	private PageClass loadCSV(SeedConfig conf) {
		PageClass root = null;
		try {
			CsvReader reader = new CsvReader(CSV_PATH+conf.file, '\t');
			
			Set<PageClass> classes = getPageClasses(conf);
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
								case "form":
									pageSrc.addFormLink(xpath, pageDest);
									break;
							}
						}
						else pageSrc.addPageClassLink(xpath, pageDest);
					}
				}
				else if (type.equals("form")) {
					pageSrc.setForm(xpath);
				}
				else {
					String fieldName = reader.get(3);
					if (fieldName == null)
						pageSrc.addData(xpath, type);
					else pageSrc.addData(xpath, type, fieldName);
				}
			}
			reader.close();
		} catch (IOException e) {
			return null;
		}
		root.setHierarchy();
		return root;
	}

	private Set<PageClass> getPageClasses(SeedConfig conf) 
			throws IOException {
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(CSV_PATH+conf.file, '\t');
		while (reader.readRecord()) {
			PageClass pClass1 = new PageClass(reader.get(0),conf);
			PageClass pClass2 = new PageClass(reader.get(3),conf);
			pageClasses.add(pClass1);
			pageClasses.add(pClass2);
		}
		reader.close();
		return pageClasses;
	}
	
	private PageClass getPageClass(Set<PageClass> pClasses, String name) {
		return pClasses.stream().filter(pc -> pc.getName().equals(name)).findAny().orElse(null);
	}
	
	private void setConf(PageClass root, SeedConfig conf) {
		Queue<PageClass> queue = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		
		queue.add(root);	
		PageClass current = null;
		while ((current = queue.poll()) != null) {
			current.setWaitTime(conf.wait);
			current.setRandomPause(conf.randompause);
			current.setMaxFetchTries(conf.maxfailures);
			current.setJavascript(conf.javascript);
			
			current.classLinks()
			.filter(pc -> !visited.contains(pc.getName()))
			.forEach(queue::add);
			visited.add(current.getName());
		}

	}

}
