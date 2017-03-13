package it.uniroma3.crawler.fetch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import it.uniroma3.crawler.page.PageClass;
import it.uniroma3.crawler.util.HtmlUtils;
import it.uniroma3.crawler.util.XPathUtils;

@Deprecated
public class MainFetcher extends UntypedActor {
	
	public static Props props(PageClass pageClass, String urlBase) {
		return Props.create(new Creator<MainFetcher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public MainFetcher create() throws Exception {
				return new MainFetcher(pageClass, urlBase);
			}
		});
	}
	
	final String urlBase;
	final PageClass pageClass;
	final ThreadLocal<WebClient> webClient;
	final Logger log;
	private List<ActorRef> fetchers;
	
	public MainFetcher(PageClass pageClass, String urlBase) {
		this.urlBase = urlBase;
		this.pageClass = pageClass;
		this.log = Logger.getLogger(MainFetcher.class.getName());
		this.webClient = new ThreadLocal<WebClient>() {
			@Override
			protected WebClient initialValue() {
				return 	HtmlUtils.makeWebClient();
			}
		};
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		
		// handle http request
		if (message instanceof String) {
			String url = (String) message;
			HtmlPage page = HtmlUtils.getPage(url, webClient.get(), log);
			Map<String, List<String>> xPath2Dest = pageClass.getXPaths();
			
			List<String> xPathNavigation = xPath2Dest.keySet().stream()
					.filter(xPath -> !xPath2Dest.get(xPath).get(1).equals("null"))
					.collect(Collectors.toList());
			if (!xPathNavigation.isEmpty()) sendRequests(page, xPath2Dest, xPathNavigation);
			
			List<String> xPathValues = xPath2Dest.keySet().stream()
			.filter(xPath -> xPath2Dest.get(xPath).get(1).equals("null"))
			.collect(Collectors.toList());
			
			if (!xPathValues.isEmpty()) {
				String[] record = extractRecord(page, url, xPath2Dest, xPathValues);
				getDestActor("writer").tell(record, getSelf());
			}
		}
		else if (message instanceof List) {
			if (this.fetchers==null) this.fetchers = (List<ActorRef>) message;
		}
		
		else unhandled(message);

	}
	
	private void sendRequests(HtmlPage page, Map<String, List<String>> xPath2Dest, List<String> xPathNavigation) {
		for (String xPath : xPathNavigation) {
			List<String> values = xPath2Dest.get(xPath);
			String dest = values.get(1);

			List<HtmlAnchor> requests = (List<HtmlAnchor>) XPathUtils.getByMatchingXPath(page, xPath);
			for (HtmlAnchor req : requests) {
				String finalUrl = urlBase + req.getHrefAttribute();
				getDestActor(dest).tell(finalUrl, getSelf());
			}
		}
	}

	private String[] extractRecord(HtmlPage page, String url, Map<String, List<String>> xPath2Dest, List<String> xPathValues) {
		final String[] values = new String[xPathValues.size()];
		values[0] = url;
		for (int i=1; i<values.length; i++) {
			values[i] = XPathUtils.extractByXPath(page, xPathValues.get(i));
		}
		log.fine("Extracted tuple "+Arrays.asList(values));
		return values;
	}
	
	private ActorRef getDestActor(String name) {
		return fetchers.stream()
				.filter(t -> t.path().name().equals(name))
				.collect(Collectors.toList()).get(0);
	}

}
