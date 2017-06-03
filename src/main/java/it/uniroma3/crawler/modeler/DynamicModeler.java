package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.Commands.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.AbstractLoggingActor;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.model.CandidatePageClass;
import it.uniroma3.crawler.modeler.model.LinkCollection;
import it.uniroma3.crawler.modeler.model.Page;
import it.uniroma3.crawler.modeler.model.WebsiteModel;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;
import scala.concurrent.duration.Duration;

public class DynamicModeler extends AbstractLoggingActor {
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
	 * set of visited URLs for duplicate detection
	 */
	private Set<String> visitedURLs = new HashSet<>(); 
	
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
	 * max number of links to fetch per {@link LinkCollection}
	 */
	private int max = 3;
	
	/**
	 * current number of links fetched in the current collection
	 */
	private int fetched;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(SeedConfig.class, this::start)
		.matchEquals(POLL, msg -> poll())
		.matchEquals(FETCH, msg -> fetch())
		.matchEquals(UPDATE, msg -> update())
		.matchEquals(FINALIZE, msg -> finalizeModel())
		.build();
	}
	
	private void start(SeedConfig sc) {
		conf = sc;
		client = makeWebClient(sc.javascript);

		// Feed queue with seed
		queue.add(new LinkCollection(Arrays.asList(conf.site)));
		self().tell(POLL, self());
	}
	
	private void poll() {
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
		self().tell(FETCH, self());
	}
	
	private void fetch() {
		if (!links.isEmpty() && fetched < max) {
			String url = links.poll();
			String u = transformURL(url);
			if (visitedURLs.contains(u)||u.isEmpty()||!isValidURL(conf.site, u))
				self().tell(FETCH, self()); // try next..
			else {
				try {
					HtmlPage body = getPage(url, client);
					newPages.add(new Page(url, body.getAnchors()));
					totalFetched++; 
					fetched++;
					log().info("Fetched: " + url);
					context().system().scheduler().scheduleOnce(
						Duration.create(conf.wait, TimeUnit.MILLISECONDS), 
						self(), FETCH, context().dispatcher(), self());
				} catch (Exception e) {
					log().warning("Failed fetching: " + url);
					self().tell(FETCH, self());
				}
			}
		} else {
			/* reset default */
			max = 3;
			self().tell(UPDATE, self());
		}
	}
	
	private void update() {
		if (!newPages.isEmpty()) {
			List<CandidatePageClass> candidates = clusterPages(newPages);
			// fetch all links if they are from different classes
			if (candidates.size() > 1 && fetched<collection.size()) {
				log().info("MENU DETECTED: FETCHING ALL URLS IN LINK COLLECTION...");
				queue.add(collection);
				max = collection.size();
				totalFetched -= newPages.size();
			}
			else {
				updateModel(candidates);
				queue.addAll(newLinks(newPages));
				newPages.forEach(p -> visitedURLs.add(transformURL(p.getUrl())));
			}
			/* reset */
			newPages.clear();
			fetched = 0;
		}
		if (!queue.isEmpty() && totalFetched < conf.modelPages)
			self().tell(POLL, self());
		else
			self().tell(FINALIZE, self());
	}
	
	private void finalizeModel() {
		log().info("FINALIZING MODEL...");
		client.close();
		if (!model.isEmpty()) {
			ModelFinalizer finalizer = new ModelFinalizer(model, conf);
			PageClass root = finalizer.getRoot();
			root.setHierarchy();
			log().info("END");
			context().parent().tell(root, self());
		}
		else {
			log().info("MODELING FAILED");
			context().parent().tell(STOP, self());
		}
	}
	
	// Candidate class selection
	// Collapse classes with similar structure
	private List<CandidatePageClass> clusterPages(List<Page> pages) {
		List<CandidatePageClass> candidates =
			pages.stream()
			.collect(groupingBy(Page::getSchema)).values().stream()
			.map(groupedPages -> new CandidatePageClass((++id),groupedPages))
			.sorted((c1,c2) -> c2.size()-c1.size())
			.collect(toList());
		
		Set<CandidatePageClass> deleted = new HashSet<>();
		for (int i = 0; i < candidates.size(); i++) {
			for (int j = candidates.size() - 1; j > i; j--) {
				CandidatePageClass ci = candidates.get(i);
				CandidatePageClass cj = candidates.get(j);
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
	
	// Update Model
	private void updateModel(List<CandidatePageClass> candidates) {
		for (CandidatePageClass candidate : candidates) {
			WebsiteModel merged = minimumModel(candidate);
			WebsiteModel mNew = new WebsiteModel(model);
			mNew.addClass(candidate);			
			model.copy((merged.cost() < mNew.cost()) ? merged : mNew);
		}
	}
	
	// Merged Model with minimum cost
	private WebsiteModel minimumModel(CandidatePageClass candidate) {
		WebsiteModel minimum = new WebsiteModel();
		for (CandidatePageClass c : model.getClasses()) {
			WebsiteModel temp = new WebsiteModel(model);
			temp.removeClass(c);

			CandidatePageClass union = new CandidatePageClass(c.getId());
			union.collapse(candidate);
			union.collapse(c);
			temp.addClass(union);

			if (minimum.cost()>temp.cost())
				minimum = temp;
		}
		return minimum;
	}
	
	private Set<LinkCollection> newLinks(List<Page> pages) {
		Set<LinkCollection> newLinks = new HashSet<>();
		for (Page p : pages) {
			for (String xp : p.getSchema()) {
				LinkCollection lc = new LinkCollection(p, xp);
				if (!visitedColl.contains(lc)) {
					visitedColl.add(lc);
					newLinks.add(lc);
				}
			}
		}
		return newLinks;
	}

}
