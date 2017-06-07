package it.uniroma3.crawler.persistence;

import java.util.HashMap;

import org.neo4j.ogm.session.Session;

import it.uniroma3.crawler.factories.GraphSessionFactory;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;

public class PageClassService {
	private Session session = GraphSessionFactory.getInstance().getSession();
	
	public void saveModel(PageClass pclass, String domain) {
		Website site = session.load(Website.class, domain, -1);
		if (site==null) site = new Website(domain);
		site.addModel(pclass, System.currentTimeMillis());
		session.save(site);
	}
	
	public PageClass getModel(String domain) {      
		Website site = session.load(Website.class, domain, -1);
        PageClass root = site.getNewestModel();
        return root;
	}
	
	public void deleteModel(String domain, int n) {
		//TODO: delete all website... or model, based on a query:
		// version = 2, version = 30 ...
		String query = "MATCH (w:Website)-[ml]-(root) "
				+ "WHERE w.domain = { domain } "
				+ "WITH root "
				+ "ORDER BY ml.timestamp "
				+ "LIMIT { n } "
				+ "MATCH (root)-[r:CLASS_LINK|DATA_LINK*0..]->(d) "
				+ "DETACH DELETE root,r,d";
		HashMap<String,Object> params = new HashMap<>();
		params.put("domain", domain);
		params.put("n", n);
		session.query(query, params);
	}

}
