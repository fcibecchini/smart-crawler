package it.uniroma3.crawler.modeler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import akka.actor.AbstractLoggingActor;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.persistence.PageClassService;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

import static it.uniroma3.crawler.util.FileUtils.normalizeURL;
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
		long timestamp = System.currentTimeMillis();
		saveCSV(root, timestamp);
		new PageClassService().saveModel(root, timestamp);
		context().parent().tell(STOP, self());
	}
	
	private void load(SeedConfig conf) {
		PageClass root = loadCSV(conf);
		context().parent().tell(root, self());
		context().parent().tell(STOP, self());
	}
	
	private void saveCSV(PageClass root, long timestamp) {
		String normUrl = normalizeURL(root.getDomain());
		try {
			int version = saveWebsiteCSV(root.getDomain(), normUrl, timestamp);
			saveModelCSV(root, normUrl, version);
		} catch (IOException e) {
			log().error("IOException while saving CSV Model: "+e.getMessage());
		}
	}
	
	private void saveModelCSV(PageClass root, String normalizedUrl, int version) 
			throws IOException {
		String file = CSV_PATH+normalizedUrl+"_target_"+version+".csv";
		FileWriter writer = new FileWriter(file,true);
		
		Queue<PageClass> queue = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		PageClass current = null;
		queue.add(root);
		while ((current = queue.poll()) != null) {
			writer.write(current.toString());
			current.classLinks().filter(pc -> visited.add(pc.getName())).forEach(queue::add);
		}
		writer.close();
	}
	
	private int saveWebsiteCSV(String domain, String normalizedDomain, long timestamp) 
			throws IOException {
		String file = CSV_PATH+normalizedDomain+"_website.csv";
		int version;
		boolean writeHeader = !Files.exists(Paths.get(file));
		CsvWriter writer = new CsvWriter(new FileWriter(file, true), '\t');
		if (writeHeader) {
			writer.writeRecord(new String[] { domain });
			writer.writeRecord(new String[] { "version", "timestamp", "date" });
			version = 1;
		} else
			version = lastModelVersion(file) + 1;
		String date = Instant.ofEpochMilli(timestamp).toString();
		writer.writeRecord(new String[] { String.valueOf(version), 
				String.valueOf(timestamp), date });
		writer.close();
		return version;
	}
	
	private int lastModelVersion(String websiteFile) throws IOException {
		int version = 0;
		CsvReader websiteReader = new CsvReader(websiteFile, '\t');
		websiteReader.readRecord();
		websiteReader.readRecord();
		while (websiteReader.readRecord())
			version++;
		return version;
	}
	
	private PageClass loadCSV(SeedConfig conf) {
		PageClass root = null;
		try {
			String file = conf.file;
			if (file==null) {
				String normUrl = normalizeURL(conf.site);
				String websiteFile = CSV_PATH+normUrl+"_website.csv";
				file = normUrl+"_target_"+lastModelVersion(websiteFile)+".csv";
			}
			CsvReader reader = new CsvReader(CSV_PATH+file, '\t');
			
			Set<PageClass> classes = getPageClasses(conf, file);
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
								case "form":
									pageSrc.addFormLink(xpath, pageDest);
									break;
								default:
									pageSrc.addSingletonLink(xpath, subtype, pageDest);
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
			log().error("IOException while loading CSV Model: "+e.getMessage());
			return new PageClass();
		}
		root.setHierarchy();
		return root;
	}

	private Set<PageClass> getPageClasses(SeedConfig conf, String file) 
			throws IOException {
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(CSV_PATH+file, '\t');
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

}
