package it.uniroma3.crawler.modeler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.csvreader.CsvReader;

import it.uniroma3.crawler.model.PageClass;

public class StaticModeler extends WebsiteModeler {
	private static final char DELIMITER = '\t';
	private String configFile;
	
	public StaticModeler(String configFile) {
		super();
		this.configFile = configFile;
	}

	@Override
	public PageClass computeModel() {
		try {
			CsvReader reader = new CsvReader(configFile, DELIMITER);
			reader.readRecord();
			String urlBase = reader.get(0);
			setUrlBase(URI.create(urlBase));
			
			setClasses(getPageClasses(urlBase));
			while (reader.readRecord()) {
				PageClass pageSrc = getPageClass(getClasses(), reader.get(0));
				String type = reader.get(1);
				String xpath = reader.get(2);
				PageClass pageDest = getPageClass(getClasses(), reader.get(3));
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
			// set depth hierarchy for page classes
			setHierarchy();
			
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
		
		return getEntryPageClass();
	}

	private Set<PageClass> getPageClasses(String website) throws IOException {
		HashSet<PageClass> pageClasses = new HashSet<>();
		CsvReader reader = new CsvReader(configFile, DELIMITER);
		reader.readRecord(); // skip url base
		while (reader.readRecord()) {
			PageClass pClass1 = new PageClass(reader.get(0));
			pClass1.setWebsite(website);
			PageClass pClass2 = new PageClass(reader.get(3));
			pClass2.setWebsite(website);
			if (getEntryPageClass()==null) setEntryPageClass(pClass1);
			pageClasses.add(pClass1);
			pageClasses.add(pClass2);
		}
		return pageClasses;
	}
	
	private PageClass getPageClass(Set<PageClass> pClasses, String name) {
		return pClasses.stream().filter(pc -> pc.getName().equals(name)).findAny().orElse(null);
	}
	
}
