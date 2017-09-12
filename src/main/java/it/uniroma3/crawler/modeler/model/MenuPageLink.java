package it.uniroma3.crawler.modeler.model;

import java.util.List;
import static java.util.stream.Collectors.toList;

import it.uniroma3.crawler.model.PageClass;

public class MenuPageLink extends PageLink {
	List<String> hrefs;

	public MenuPageLink(String xpath, List<Page> destinations) {
		super(xpath, destinations);
		this.hrefs = destinations.stream().map(p -> p.getHref()).collect(toList());
	}

	@Override
	public void linkToPageClass(PageClass src, List<PageClass> dests) {
		String xpath = getXpath();
		if (!src.hasLink(xpath)) {
			for (int i=0; i<dests.size(); i++) {
				src.addMenuLink(xpath, hrefs.get(i), dests.get(i));
			}
		}
		else if (src.hasSingleLink(xpath)) {
			src.removeLink(xpath);
			for (int i=0; i<dests.size(); i++) {
				src.addMenuLink(xpath, hrefs.get(i), dests.get(i));
			}
		}
	}

}
