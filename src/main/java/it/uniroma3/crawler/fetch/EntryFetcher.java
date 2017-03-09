package it.uniroma3.crawler.fetch;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import it.uniroma3.crawler.page.PageClass;
import it.uniroma3.crawler.util.HtmlUtils;
import it.uniroma3.crawler.util.XPathUtils;
import it.uniroma3.crawler.writer.Writer;

public class EntryFetcher extends UntypedActor {
	
	public static Props props(final String url, final Queue<PageClass> classes) {
		return Props.create(new Creator<EntryFetcher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EntryFetcher create() throws Exception {
				return new EntryFetcher(url, classes);
			}
		});
	}
	final String urlBase;
	final Queue<PageClass> pageTypes; 
	final Logger log;
	final WebClient webClient;
	
	public EntryFetcher(String url, Queue<PageClass> classes) {
		this.urlBase = url;
		this.pageTypes = classes;
		this.log = Logger.getLogger(EntryFetcher.class.getName());
		this.webClient = HtmlUtils.makeWebClient();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof PageClass) {
			// got the entrypoint
			
			HtmlPage homePoint = HtmlUtils.getPage(urlBase, webClient, log);
			PageClass homePageClass = (PageClass) message;
			Map<String, List<String>> xPathsMap = homePageClass.getXPaths();
			String xPath = xPathsMap.keySet().stream().findFirst().get();
			String entryUrl = urlBase + XPathUtils.getUniqueByXPath(homePoint, xPath).getTextContent();
			String destination = xPathsMap.get(xPath).get(1);
			
			Iterator<PageClass> pageTypesIt = pageTypes.iterator();
			List<ActorRef> actorRefs = new LinkedList<>();

			// Build Page Class actors
			while (pageTypesIt.hasNext()) {
				PageClass current = pageTypesIt.next();
				actorRefs.add(getContext().actorOf(MainFetcher.props(current, urlBase), current.getName()));
			}
			
			// Writer
			actorRefs.add(getContext().actorOf(Writer.props("./result.csv"), "writer"));
			
			// Send actor references to themselves
			actorRefs.stream().forEach(t -> t.tell(actorRefs, getSelf()));
			
			// First page crawl
			ActorRef target = actorRefs.stream()
					.filter(t -> t.path().name().equals(destination))
					.collect(Collectors.toList()).get(0);
			target.tell(entryUrl, getSelf());
			
			webClient.close();
			
		}
		else unhandled(message);
		
	}

}
