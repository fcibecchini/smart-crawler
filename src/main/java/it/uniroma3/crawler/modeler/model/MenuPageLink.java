package it.uniroma3.crawler.modeler.model;

import java.util.List;
import static java.util.stream.Collectors.toList;

import it.uniroma3.crawler.model.PageClass;

public class MenuPageLink extends PageLink {
	private String sourceUrl;
	private List<String> hrefs;

	public MenuPageLink(String xpath, List<Page> destinations) {
		super(xpath, destinations);
		this.hrefs = destinations.stream().map(p -> p.getHref()).collect(toList());
	}
	
	public void setSourceUrl(String url) {
		this.sourceUrl = url;
	}

	@Override
	public void linkToPageClass(PageClass src, List<PageClass> dests) {
		String xpath = getXpath();
		if (!src.hasLink(xpath) || src.hasMenuLink(xpath)) {
			src.addMenu(sourceUrl, xpath, hrefs, dests);
		}
		else if (src.hasSingleLink(xpath)) {
			src.removeLink(xpath);
			src.addMenu(sourceUrl, xpath, hrefs, dests);
		}
	}

}
