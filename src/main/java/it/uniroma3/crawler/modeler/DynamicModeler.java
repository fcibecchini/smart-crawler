package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.HtmlUtils.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static it.uniroma3.crawler.util.XPathUtils.getAbsoluteURLs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.AbstractLoggingActor;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.model.ModelPageClass;
import it.uniroma3.crawler.modeler.model.LinkCollection;
import it.uniroma3.crawler.modeler.model.Page;
import it.uniroma3.crawler.modeler.model.WebsiteModel;
import it.uniroma3.crawler.modeler.model.XPath;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import it.uniroma3.crawler.util.Commands;
import it.uniroma3.crawler.util.FileUtils;
import it.uniroma3.crawler.util.HtmlUtils;
import scala.concurrent.duration.Duration;

public class DynamicModeler extends AbstractLoggingActor {
	public static final short FETCH=2, POLL=3, CLUSTER=4, 
			UPDATE=5, XPATH_FINER=6, XPATH_COARSER=7, FINALIZE=8;
	
	private SeedConfig conf; // website configuration

	/**
	 * Model of this website
	 */
	private final WebsiteModel model = new WebsiteModel();
	
	private WebClient client;
	
	/**
	 * the queue of discovered Link Collections
	 */
	private Queue<LinkCollection> queue =
			new PriorityQueue<>((l1,l2) -> l1.densestFirst(l2,model));
	
	/**
	 * map of visited pages
	 */
	private Map<String,Page> visitedURLs = new HashMap<>();
	
	/**
	 * set of already visited LinkCollection for duplicate detection
	 */
	private Set<LinkCollection> visitedColl = new HashSet<>();
	
	/**
	 * total number of fetched URLs
	 */
	private int totalFetched;
	
	/**
	 * current id of last created {@link CandidateClassPage}
	 */
	private int id;
	
	/**
	 * current list of new pages from latest outgoing links
	 */
	private List<Page> newPages = new ArrayList<>();
	
	/**
	 * last polled LinkCollection
	 */
	private LinkCollection collection;
	
	/**
	 * current queue of outgoing links being fetched
	 */
	private Queue<String> links;
	
	/**
	 * current list of candidates clusters
	 */
	private List<ModelPageClass> candidates;
	
	/**
	 * a reference to the ModelPageClass id where
	 * the last singleton newPage was added.
	 */
	private int lastSingletonId;
	
	/**
	 * counter of how many times a singleton newPage
	 * was added to the same ModelPageClass
	 */
	private int singletonCounter;
	
	/**
	 * max number of links to fetch per {@link LinkCollection}
	 */
	private int max = 3;
	
	/**
	 * current number of links fetched in the current collection
	 */
	private int fetched;
		
	private boolean pause;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(SeedConfig.class, this::start)
		.matchEquals(POLL, msg -> poll())
		.matchEquals(FETCH, msg -> fetch())
		.matchEquals(CLUSTER, msg -> cluster())
		.matchEquals(XPATH_FINER, msg -> changeXPath(true))
		.matchEquals(XPATH_COARSER, msg -> changeXPath(false))
		.matchEquals(UPDATE, msg -> update())
		.matchEquals(FINALIZE, msg -> finalizeModel())
		.build();
	}
	
	public void start(SeedConfig sc) {
		conf = sc;
		client = makeWebClient(sc.javascript);

		// Feed queue with seed
		queue.add(new LinkCollection(Arrays.asList(conf.site)));
		self().tell(POLL, self());
	}
	
	public void poll() {
		if (queue.isEmpty()) 
			self().tell(FINALIZE, self());
		else {
			if (collection!=null && collection.fetchAll()) {
				collection.setFetchAll(false);
				max = collection.size();
			}
			else 
				collection = queue.poll();
			
			int size = collection.size();
			log().info("Parent Page: "+collection.getPage()+", "+size+" links");
			List<String> group = collection.getLinks();
			links = new LinkedList<>();
			if (size<=max)
				links.addAll(group);
			else {
				links.add(group.get(0));
				links.add(group.get((size-1)/2));
				links.add(group.get(size-1));
			}
			
			if (pause) 
				context().system().scheduler().scheduleOnce(
						Duration.create(conf.wait, TimeUnit.MILLISECONDS), 
						self(), FETCH, context().dispatcher(), self());
			else self().tell(FETCH, self());		
			
			/* reset */
			newPages.clear();
			pause = false;
		}
	}
	
	public void fetch() {
		if (!links.isEmpty() && fetched < max) {
			String url = links.poll();
			if (isValidURL(conf.site, url)) {
				try {
					Page page = visitedURLs.get(url);
					if (page!=null) {
						log().info("Loaded: "+url);
						page.setLoaded();
					}
					else if (totalFetched<conf.modelPages) {
						page = new Page(url, getPage(url, client));
						visitedURLs.put(url,page);
						pause = true;
						totalFetched++;
						log().info("Fetched: "+url);
					}
					else {
						self().tell(FINALIZE, self());
						return;
					}
					newPages.add(page);
					fetched++;
				} catch (Exception e) {
					log().warning("Failed fetching: "+url+", "+e.getMessage());
				}
			}
			else log().info("Rejected URL: "+url);
			
			self().tell(FETCH, self());
		}
		else if (fetched>0) {
			/* reset values */
			max = 3;
			fetched = 0;
			self().tell(CLUSTER, self());
		}
		else self().tell(POLL, self());
	}
	
	public void cluster() {
		candidates = clusterPages(newPages);
		inspect(candidates);
	}
	
	/* 
	 * Candidate classes selection
	 * Collapse classes with similar structure
	 */
	private List<ModelPageClass> clusterPages(List<Page> pages) {
		List<ModelPageClass> candidates =
			pages.stream()
			.collect(groupingBy(Page::getSchema)).values().stream()
			.map(groupedPages -> new ModelPageClass((++id),groupedPages))
			.sorted((c1,c2) -> c2.size()-c1.size())
			.collect(toList());
		
		Set<ModelPageClass> deleted = new HashSet<>();
		for (int i = 0; i < candidates.size(); i++) {
			for (int j = candidates.size() - 1; j > i; j--) {
				ModelPageClass ci = candidates.get(i);
				ModelPageClass cj = candidates.get(j);
				if (!deleted.contains(ci) && !deleted.contains(cj)) {
					if (ci.distance(cj) < 0.2) {
						ci.collapse(cj);
						deleted.add(cj);
					}
				}
			}
		}
		candidates.removeAll(deleted);
		return candidates;
	}
	
	/*
	 * Inspect the candidates and take actions on the base of:
	 * - Number of newPages fetched
	 * - Number of clusters created
	 */
	private void inspect(List<ModelPageClass> candidates) {
		if (newPages.size()>=3) {
			if (newPages.size()==3 && candidates.size()==1) {
				collection.setList();
				self().tell(UPDATE, self());
			}
			else if (newPages.size()==3 && candidates.size()==2) {
				if (!collection.isFinest())
					self().tell(XPATH_FINER, self());
				else {
					collection.setList();
					self().tell(UPDATE, self());
				}
			}
			else if (candidates.size()>=3) {
				if (collection.isMenu())
					self().tell(UPDATE, self());
				else {
					collection.setFetchAll(true);
					collection.setMenu();
					//queue.add(collection);
					//max = collection.size();
					self().tell(POLL, self());
					log().info("MENU: FETCHING ALL URLS IN LINK COLLECTION...");
				}
			}
		} 
		else if (newPages.size()==2) {
			if (candidates.size()==1) {
				collection.setList();
				self().tell(UPDATE, self());
			}
			else {
				collection.setMenu();
				self().tell(UPDATE, self());
			}
		} 
		else if (newPages.size()==1) {
			collection.setSingleton();
			self().tell(UPDATE, self());
		}
		else // empty
			self().tell(POLL, self());
	}
	
	public void update() {
		List<ModelPageClass> toRemove = new ArrayList<>();
		for (ModelPageClass c : candidates) {
			for (Page p : c.getClassPages()) {
				/* If there are already classified pages in candidates
				 * we should skip the update phase for this cluster,
				 * merging new fetched pages. */
				if (p.isLoaded()) {
					ModelPageClass mpc = model.getClassOfPage(p);
					/* while we are sure that this page was already downloaded,
					 * we must also check that it was classified */
					if (mpc!=null) { 
						mpc.collapse(c); // merge new fetched pages, if any
						toRemove.add(c);
					}
					break;
				}
			}
		}
		candidates.removeAll(toRemove);
		
		updateModel(candidates);
		
		if (setPageLinks(collection)) {
			queue.addAll(getLinkCollections(newPages));
			self().tell(POLL, self());
		}
		else self().tell(XPATH_COARSER, self());
	}
	
	/*
	 * Set the Page Links between the current collection Parent page
	 * and the newPages links
	 * Returns false if an XPath refinement is required
	 */
	private boolean setPageLinks(LinkCollection collection) {
		boolean saved = true;
		
		Page page = collection.getPage();
		
		// seed does not have a parent page
		if (page!=null) { 
			String xpath = collection.getXPath().get();
			if (collection.isList()) {
				page.addListLink(xpath, newPages);
			}
			else if (collection.isMenu()) {
				page.addMenuLink(xpath, newPages);
			}
			else if (collection.isSingleton()) {
				int modelClassId = model.getClassOfPage(newPages.get(0)).getId();
				if (modelClassId==lastSingletonId && singletonCounter==3
						&& !collection.isCoarsest()) {
					singletonCounter = 0;
					saved = false;
				}
				else {
					if (modelClassId==lastSingletonId) 
						singletonCounter++;
					page.addSingleLink(xpath, newPages);
				}
			}
		}
		return saved;
	}
	
	/*
	 * Changes the current LinkCollection XPath version
	 * until it founds different links
	 */
	public void changeXPath(boolean finer) {		
		Page page = collection.getPage();
		String url = page.getUrl();
		XPath xp = collection.getXPath();
		String version = xp.get();
		boolean found = false;

		try {
			HtmlPage html;
			if (page.getTempFile()==null) {
				html = getPage(url, client);
				String directory = FileUtils.getTempDirectory(conf.site);
				String path = HtmlUtils.savePage(html,directory,false);
				page.setTempFile(path);
			} else
				html = HtmlUtils.restorePageFromFile(page.getTempFile(), url);

			LinkCollection newCol = null;
			while (!found && !((finer) ? xp.finer() : xp.coarser()).isEmpty()) {
				List<String> links = getAbsoluteURLs(html, xp.get(), url);
				if (!links.equals(collection.getLinks())) {
					newCol = new LinkCollection(page, new XPath(xp), links);
					queue.add(newCol);
					log().info("Refined XPath: "+xp.getDefault()+" -> "+xp.get());
					found=true;
				}
			}

		} catch (Exception e) {
			log().warning("Failed refinement of XPath: "+e.getMessage());
		}
		
		if (!found) {
			collection.getXPath().setVersion(version);
			if (finer) 
				collection.setFinest();
			else 
				collection.setCoarsest();
			queue.add(collection);
		}
		
		self().tell(POLL, self());
	}
	
	/*
	 * Update Model merging candidates to existing classes
	 * or creating new ones.
	 */
	private void updateModel(List<ModelPageClass> candidates) {
		for (ModelPageClass candidate : candidates) {
			WebsiteModel merged = minimumModel(candidate);
			WebsiteModel mNew = new WebsiteModel(model);
			mNew.addClass(candidate);			
			model.copy((merged.cost() < mNew.cost()) ? merged : mNew);
		}
	}
	
	/*
	 * Returns the Merged Model with minimum length cost
	 */
	private WebsiteModel minimumModel(ModelPageClass candidate) {
		WebsiteModel minimum = new WebsiteModel();
		for (ModelPageClass c : model.getClasses()) {
			WebsiteModel temp = new WebsiteModel(model);
			temp.removeClass(c);

			ModelPageClass union = new ModelPageClass(c.getId());
			union.collapse(candidate);
			union.collapse(c);
			temp.addClass(union);

			if (minimum.cost()>temp.cost())
				minimum = temp;
		}
		return minimum;
	}
	
	/*
	 * Constructs new Link Collections from newPages
	 */
	private Set<LinkCollection> getLinkCollections(List<Page> pages) {		
		Set<LinkCollection> newLinks = new HashSet<>();
		for (Page p : pages) {
			for (XPath xp : p.getXPaths()) {
				LinkCollection lc = 
					new LinkCollection(p, new XPath(xp), p.getURLsByXPath(xp));
				if (visitedColl.add(lc))
					newLinks.add(lc);
			}
		}
		return newLinks;
	}
	
	public void finalizeModel() {
		log().info("FINALIZING MODEL...");
		client.close();
		if (!model.isEmpty()) {
			PageClass root = model.toGraph(conf);
			root.setHierarchy();
			FileUtils.clearTempDirectory(conf.site);
			log().info("END");
			context().parent().tell(root, self());
		}
		else {
			log().info("MODELING FAILED");
			context().parent().tell(Commands.STOP, self());
		}
	}

}
