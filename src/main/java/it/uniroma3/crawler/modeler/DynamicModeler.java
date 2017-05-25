package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.HtmlUtils.*;
import static it.uniroma3.crawler.util.XPathUtils.*;
import static it.uniroma3.crawler.util.Commands.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.csvreader.CsvWriter;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.Creator;
import it.uniroma3.crawler.model.CandidatePageClass;
import it.uniroma3.crawler.model.LinkCollection;
import it.uniroma3.crawler.model.Page;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.model.Website;
import it.uniroma3.crawler.model.WebsiteModel;
import it.uniroma3.crawler.util.FileUtils;
import scala.concurrent.duration.Duration;

public class DynamicModeler extends AbstractLoggingActor implements WebsiteModeler {
	private final static String LOG = "src/main/resources/targets";
	private final static int MAX = 3;
	private CsvWriter csv;
	
	private Website website;
	private WebsiteModel model;
	private int wait;
	private int maxPages; // max number of pages used to infer the web site model
	private WebClient client;
	private Set<String> visitedURLs; // set of visited URLs for duplicate removal
	private int fetched; // total number of fetched pages
	private int classes; // total number of created classes
	
	private Queue<LinkCollection> queue; // queue of discovered Links Collections
	private Set<LinkCollection> visitedColl; // A set of already visited link collections
	private Map<String, HtmlPage> htmlPages; // map{url -> HtmlPage} of currently fetched pages
	private LinkCollection collection; // current collection being fetched
	private TreeSet<String> links; // current Links Set being fetched
	private int max; // max URLs fetched per collection
	private int counter; // current number of URLs fetched
	
	static class InnerProps implements Creator<DynamicModeler> {
		private static final long serialVersionUID = 1L;
		private Website website;
		private int wait, maxPages;
		
		public InnerProps(Website website, int wait, int maxPages) {
			this.website = website;
			this.wait = wait;
			this.maxPages = maxPages;
		}

		@Override
		public DynamicModeler create() throws Exception {
			return new DynamicModeler(website, wait, maxPages);
		}	
	}
	
	public static Props props(Website website, int wait, int maxPages) {
		return Props.create(DynamicModeler.class, new InnerProps(website,wait,maxPages));
	}
	
	public DynamicModeler(Website website, int wait, int maxPages) {
		this.website = website;
		this.wait = wait;
		this.maxPages = maxPages;
		client = makeWebClient(website.isJavascript());
		
		model = new WebsiteModel();
		visitedURLs = new HashSet<>();
		max = MAX;
		queue = new PriorityQueue<>();
		visitedColl = new HashSet<>();
		htmlPages = new HashMap<>();
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.matchEquals(START, msg -> start())
		.matchEquals(POLL, msg -> poll())
		.matchEquals(FETCH, msg -> fetch())
		.matchEquals(UPDATE, msg -> update())
		.matchEquals(FINALIZE, msg -> finalizeModel())
		.build();
	}
	
	private void start() {
		// Feed queue with seed
		queue.add(new LinkCollection(website.getDomain()));
		self().tell(POLL, self());
	}
	
	private void poll() {
		collection = queue.poll();
		log().info("Parent Page: "+collection.getParent()+", "+collection.size()+" total links");
		links = new TreeSet<>(collection.getLinks());
		self().tell(FETCH, self());
	}
	
	private void fetch() {
		if (!links.isEmpty() && counter < max) {
			String url = links.pollFirst();
			String normURL = transformURL(url);
			if (visitedURLs.contains(normURL) || normURL.isEmpty() || !isValidURL(website.getDomain(), normURL))
				self().tell(FETCH, self()); // try next..
			else {
				try {
					HtmlPage body = getPage(url, client);
					htmlPages.put(url, body);
					fetched++;
					counter++;
					log().info("Fetched: " + url);
					context().system().scheduler().scheduleOnce(
						Duration.create(wait, TimeUnit.MILLISECONDS), 
						self(), FETCH, context().dispatcher(), self());
				} catch (Exception e) {
					log().warning("Failed fetching: " + url);
					self().tell(FETCH, self());
				}
			}
		} else {
			/* restore default values */
			counter = 0;
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
				fetched -= newPages.size(); // reset counter
			}
			else {
				updateModel(candidates);
				queue.addAll(newLinks(newPages));
				newPages.forEach(p -> visitedURLs.add(transformURL(p.getUrl())));
			}
		}
		if (!queue.isEmpty() && fetched < maxPages)
			self().tell(POLL, self());
		else
			self().tell(FINALIZE, self());
	}
	
	private void finalizeModel() {
		log().info("FINALIZING MODEL...");
		client.close();
		if (!model.isEmpty()) {
			PageClass root = compute();
			log().info("END");
			context().parent().tell(root, self());
		}
		else {
			log().info("MODELING FAILED");
			context().parent().tell(STOP, self());
		}
	}
	
	public PageClass compute() {
		// Transform candidates into Page Classes and Class Links
		ModelFinalizer finalizer = new ModelFinalizer(model, website, wait);
		TreeSet<PageClass> pClasses = finalizer.makePageClasses();
		
		// Log and save model to filesystem
		logModel(pClasses);
		
		PageClass root = pClasses.first();
		setHierarchy(root);
		return root;
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
	
	private void logModel(Set<PageClass> pClasses) {
		String target = LOG+"/"+FileUtils.normalizeURL(website.getDomain())+"_target.csv";
		
		try {
			Path dirPath = Paths.get(LOG);
			if (!Files.exists(dirPath)) Files.createDirectory(dirPath);

			csv = new CsvWriter(new FileWriter(target),'\t');
			pClasses.forEach(pc -> {
				pc.getMenuXPaths().forEach(xp -> writeRow(pc,xp,"menu"));
				pc.getListXPaths().forEach(xp -> writeRow(pc,xp,"list"));
				pc.getSingletonXPaths().forEach(xp -> writeRow(pc,xp,"singleton"));});
			csv.close();
		} 
		catch (IOException e) {
			log().error("IOException :"+e.getMessage());
		}
	}
	
	private void writeRow(PageClass pc, String xp, String type) {
		try {
			csv.write(pc.getName());
			csv.write("link");
			csv.write(xp);
			csv.write(pc.getDestinationByXPath(xp).getName());
			csv.write(type);
			csv.endRecord();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	private Set<LinkCollection> newLinks(Set<Page> pages) {
		Set<LinkCollection> newLinks = new HashSet<>();
		
		for (Page p : pages) {
			for (String xp : p.getSchema()) {
				LinkCollection lCollection = new LinkCollection(p, xp, p.getUrlsByXPath(xp));
				if (!visitedColl.contains(lCollection)) {
					visitedColl.add(lCollection);
					newLinks.add(lCollection);
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
