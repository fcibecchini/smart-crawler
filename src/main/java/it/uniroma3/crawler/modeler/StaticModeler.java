package it.uniroma3.crawler.modeler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.csvreader.CsvReader;

import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;

public class StaticModeler extends WebsiteModeler {
	private static final char DELIMITER = '\t';
	private String configFile;
	
	public StaticModeler(Website website, int wait, String configFile) {
		super(website,wait);
		this.configFile = configFile;
	}

	@Override
	protected PageClass computeModel() {
		PageClass root = null;
		try {
			CsvReader reader = new CsvReader(configFile, DELIMITER);
			reader.readRecord(); // skip url base
			
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
					else getLogger().warning("Could not find "+reader.get(3));
				}
				else pageSrc.addData(xpath, type);
			}
			
		} catch (FileNotFoundException e) {
			getLogger().severe("Could not find target configuration file");
			return null;
		} catch (IOException e) {
			getLogger().severe("Could not read target configuration file");
			return null;
		} catch (IllegalArgumentException ie) {
			getLogger().severe("Not a valid url base");
			return null;
		}
		
		return root;
	}

	private Set<PageClass> getPageClasses() throws IOException {
		Website website = getWebsite();
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(configFile, DELIMITER);
		reader.readRecord(); // skip url base
		while (reader.readRecord()) {
			PageClass pClass1 = new PageClass(reader.get(0),website,getWait());
			PageClass pClass2 = new PageClass(reader.get(3),website,getWait());
			pageClasses.add(pClass1);
			pageClasses.add(pClass2);
		}
		return pageClasses;
	}
	
	private PageClass getPageClass(Set<PageClass> pClasses, String name) {
		return pClasses.stream().filter(pc -> pc.getName().equals(name)).findAny().orElse(null);
	}
	
}
