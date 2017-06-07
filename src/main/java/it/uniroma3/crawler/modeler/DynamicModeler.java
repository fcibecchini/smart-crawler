package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.HtmlUtils.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static it.uniroma3.crawler.util.XPathUtils.getAbsoluteURLs;

import java.io.File;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import it.uniroma3.crawler.model.ClassLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.model.ModelPageClass;
import it.uniroma3.crawler.modeler.model.LinkCollection;
import it.uniroma3.crawler.modeler.model.Page;
import it.uniroma3.crawler.modeler.model.PageLink;
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
	
	/**
	 * current number of pages saved due to an XPath version change
	 */
	private int saved;
	
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
		client.getOptions().setDownloadImages(false);

		// Feed queue with seed
		queue.add(new LinkCollection(Arrays.asList(conf.site)));
		self().tell(POLL, self());
	}
	
	public void poll() {
		collection = queue.poll();
		log().info("Parent Page: "+collection.getParent()+", "+collection.size()+" total links");
		
		int size = collection.size();
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
		fetched = 0;
		pause = false;
	}
	
	public void fetch() {
		if (!links.isEmpty() && fetched < max) {
			String url = links.poll();
			String u = transformURL(url);
			if (!u.isEmpty() && isValidURL(conf.site, u)) {
				try {
					Page page = visitedURLs.get(u);
					if (page!=null) 
						log().info("Loaded: "+u);
					else if (totalFetched<conf.modelPages) {
						page = new Page(u, getPage(u, client));
						visitedURLs.put(u,page);
						pause = true;
						totalFetched++;
						log().info("Fetched: "+u);
					}
					else {
						self().tell(FINALIZE, self());
						return;
					}
					newPages.add(page);
					fetched++;
				} catch (Exception e) {
					log().warning("Failed fetching: "+u+", "+e.getMessage());
				}
			}
			self().tell(FETCH, self());
		}
		else {
			/* reset max */
			max = 3;
			self().tell(CLUSTER, self());
		}
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
				if (fetched<collection.size()) {
					log().info("MENU: FETCHING ALL URLS IN LINK COLLECTION...");
					queue.add(collection);
					max = collection.size();
					self().tell(POLL, self());
				}
				else {
					collection.setMenu();
					self().tell(UPDATE, self());
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
		updateModel(candidates);
		
		if (setPageLinks(collection)) {
			queue.addAll(getLinkCollections(newPages));
			self().tell((queue.isEmpty()) ? FINALIZE: POLL, self());
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
		
		Page parent = collection.getParent();
		
		// seed does not have a parent page
		if (parent!=null) { 
			String xpath = collection.getCurrentXPath();
			if (collection.isList()) {
				parent.addListLink(xpath, newPages.get(0));
			}
			else if (collection.isMenu()) {
				for (int i=0;i<newPages.size();i++) {
					String menuXPath = "(" + xpath + ")[" + (i+1) + "]";
					parent.addMenuLink(menuXPath, newPages.get(i));
				}
			}
			else if (collection.isSingleton()) {
				Page single = newPages.get(0);
				int modelClassId = model.getClassOfPage(single).getId();
				if (modelClassId==lastSingletonId && singletonCounter==3
						&& !collection.isCoarsest()) {
					singletonCounter = 0;
					saved = false;
				}
				else {
					if (modelClassId==lastSingletonId) 
						singletonCounter++;
					parent.addSingleLink(xpath, single);
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
		Page parent = collection.getParent();
		String parentUrl = parent.getUrl();
		XPath xp = collection.getXPath();
		String version = xp.get();
				
		try {
			HtmlPage html;
			if (parent.getTempFile()==null) {
				html = getPage(parentUrl, client);
				String path = FileUtils.getWriteDir("temp", conf.site)
						+"/temp"+(++saved);
				html.save(new File(path));
				parent.setTempFile(path+".html");
			} else
				html = HtmlUtils.restorePageFromFile(parent.getTempFile(), 
						URI.create(parentUrl));
			
			LinkCollection newCol = null;
			while (!((finer) ? xp.finer() : xp.coarser()).isEmpty()) {
				List<String> urls = getAbsoluteURLs(html, xp.get(), parentUrl);
				if (!urls.equals(collection.getLinks())) {
					newCol = new LinkCollection(parent,xp,urls);
					queue.add(newCol);
					log().info("Refined XPath: "+xp.getDefault()+" -> "+xp.get());
					break;
				}
			}
			if (newCol==null) {
				collection.getXPath().setVersion(version);
				if (finer) 
					collection.setFinest();
				else 
					collection.setCoarsest();
				queue.add(collection);
			}

		} catch (Exception e) {
			queue.add(collection);
			log().warning("Failed refinement of XPath: "+e.getMessage());
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
				LinkCollection lc = new LinkCollection(p, xp, p.getURLsByXPath(xp));
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
			PageClass root = modelToGraph();
			root.setHierarchy();
			
			try {
				Files.walk(Paths.get(FileUtils.getWriteDir("temp", conf.site)), 
					FileVisitOption.FOLLOW_LINKS)
			    .sorted(Comparator.reverseOrder())
			    .map(Path::toFile)
			    .forEach(File::delete);
			} catch (Exception e) {}
			
			log().info("END");
			context().parent().tell(root, self());
		}
		else {
			log().info("MODELING FAILED");
			context().parent().tell(Commands.STOP, self());
		}
	}
	
	/*
	 * Input: the final WebsiteModel of ModelPageClass
	 * Output: the PageClass graph
	 */
	private PageClass modelToGraph() {
		Set<PageClass> classes = model.getClasses().stream()
				.map(c -> new PageClass(String.valueOf(c.getId()),conf))
				.collect(toSet());
		
		for (ModelPageClass mpc : model.getClasses()) {
			PageClass src = getPageClass(mpc.getId(), classes);
			for (Page p : mpc.getClassPages()) {
				for (PageLink link : p.getLinks()) {
					String xp = link.getXpath();
					ClassLink classLink = src.getLink(xp);
					if (classLink==null) {
						PageClass dest = getPageClass(
								model.getClassOfPage(link.getDest()).getId(), 
								classes);
						src.addLink(xp, dest, link.getType());	
					} 
					else if (link.isList() && classLink.isSingleton())
						classLink.setTypeList();
				}
			}
		}
		return getPageClass(1,classes);
	}
	
	private PageClass getPageClass(int id, Set<PageClass> classes) {
		String name = String.valueOf(id);
		return classes.stream()
			.filter(item -> item.getName().equals(name))
			.findAny().orElse(null);
	}

}
