package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.XPathUtils.*;
import static it.uniroma3.crawler.util.Commands.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
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
	private final static int MAX = 3;
	
	private SeedConfig conf; // website configuration

	private WebsiteModel model;
	private WebClient client;
	
	private Queue<LinkCollection> queue; // queue of discovered Links Collections
	private Set<String> visitedURLs; // set of visited URLs for duplicate removal
	private Set<LinkCollection> visitedColl; // A set of already visited link collections
	private int totalFetched; // total number of fetched pages
	private int classes; // total number of classes created
	
	private Map<String, HtmlPage> htmlPages; // map{url -> HtmlPage} of currently fetched pages
	private LinkCollection collection; // current collection being fetched
	private TreeSet<String> links; // current Links Set being fetched
	private int max; // max to-fetch URLs per collection
	private int fetched; // current number of URLs fetched in collection
	
	public DynamicModeler() {
		max = MAX;
		model = new WebsiteModel();
		visitedURLs = new HashSet<>();
		queue = new PriorityQueue<>();
		visitedColl = new HashSet<>();
		htmlPages = new HashMap<>();
	}
	
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
		queue.add(new LinkCollection(conf.site));
		self().tell(POLL, self());
	}
	
	private void poll() {
		collection = queue.poll();
		log().info("Parent Page: "+collection.getParent()+", "+collection.size()+" total links");
		links = new TreeSet<>(collection.getLinks());
		self().tell(FETCH, self());
	}
	
	private void fetch() {
		if (!links.isEmpty() && fetched < max) {
			String url = links.pollFirst();
			String u = transformURL(url);
			if (visitedURLs.contains(u)||u.isEmpty()||!isValidURL(conf.site, u))
				self().tell(FETCH, self()); // try next..
			else {
				try {
					HtmlPage body = getPage(url, client);
					htmlPages.put(url, body);
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
			/* restore default values */
			fetched = 0;
			max = MAX;
			self().tell(UPDATE, self());
		}
	}
	
	private void update() {
		if (!htmlPages.isEmpty()) {
			Set<Page> newPages = makePages();
			List<CandidatePageClass> candidates = clusterPages(newPages);
			// fetch all links if they are from different classes
			if (candidates.size() > 1 && !links.isEmpty()) {
				log().info("MENU DETECTED: FETCHING ALL URLS IN LINK COLLECTION...");
				queue.add(collection);
				max = collection.size();
				totalFetched -= newPages.size(); // reset counter
			}
			else {
				updateModel(candidates);
				queue.addAll(newLinks(newPages));
				newPages.forEach(p -> visitedURLs.add(transformURL(p.getUrl())));
			}
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
	private List<CandidatePageClass> clusterPages(Set<Page> pages) {
		List<CandidatePageClass> candidates =
			pages.stream()
			.collect(groupingBy(Page::getSchema)).values().stream()
			.map(p -> new CandidatePageClass("class"+(++classes),p))
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
	private void updateModel(List<CandidatePageClass> cands) {
		for (CandidatePageClass candidate : cands) {
			WebsiteModel modelNew = new WebsiteModel();
			WebsiteModel modelMerge = null;
			modelNew.addFinalClasses(model.getModel());
			modelNew.addFinalClass(candidate);
			double modelNewLength = modelNew.minimumLength();

			double mergeLength = Double.MAX_VALUE;
			for (CandidatePageClass cInModel : model.getModel()) {
				CandidatePageClass tempClass = new CandidatePageClass(cInModel.getName());
				tempClass.collapse(candidate);
				tempClass.collapse(cInModel);

				WebsiteModel tempModel = new WebsiteModel();
				tempModel.addFinalClasses(model.getModel());
				tempModel.removeClass(cInModel);
				tempModel.addFinalClass(tempClass);

				double modelLength = tempModel.minimumLength();
				if (mergeLength > modelLength) {
					mergeLength = modelLength;
					modelMerge = tempModel;
				}
			}

			model.reset();
			if (mergeLength < modelNewLength)
				model.addFinalClasses(modelMerge.getModel());
			else
				model.addFinalClasses(modelNew.getModel());
		}
	}
	
	private Set<LinkCollection> newLinks(Set<Page> pages) {
		Set<LinkCollection> newLinks = new HashSet<>();
		
		for (Page p : pages) {
			for (String xp : p.getSchema()) {
				LinkCollection lc = new LinkCollection(p, xp, p.getUrlsByXPath(xp));
				if (!visitedColl.contains(lc)) {
					visitedColl.add(lc);
					newLinks.add(lc);
				}
			}
		}
		return newLinks;
	}
	
	private Set<Page> makePages() {
		Set<Page> newPages = new HashSet<>();
		for (String pageUrl : htmlPages.keySet()) {
			HtmlPage htmlPage = htmlPages.get(pageUrl);
			Page page = makePage(htmlPage, pageUrl);
			newPages.add(page);
		}
		htmlPages.clear();
		return newPages;
	}
	
	private Page makePage(HtmlPage page, String pageUrl) {
		Page p = new Page(pageUrl, model);
		for (HtmlAnchor link : page.getAnchors()) {
			String xpath = getXPathTo(link);
			String url = getAbsoluteURL(pageUrl, link.getHrefAttribute());
			p.updatePageSchema(xpath, url);
		}
		return p;
	}

}
