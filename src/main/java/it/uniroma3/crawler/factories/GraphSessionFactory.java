package it.uniroma3.crawler.factories;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class GraphSessionFactory {
    private final static SessionFactory sessionFactory = 
    		new SessionFactory("it.uniroma3.crawler.model");
    private final static GraphSessionFactory factory = new GraphSessionFactory();
    
    private GraphSessionFactory() {}

    public static GraphSessionFactory getInstance() {
        return factory;
    }

    public Session getSession() {
        return sessionFactory.openSession();
    }
}
