package it.uniroma3.crawler.modeler.model;

import java.util.List;

import it.uniroma3.crawler.model.PageClass;

public class ListPageLink extends PageLink {

	public ListPageLink(String xp, List<Page> destinations) {
		super(xp, destinations);
	}

	@Override
	public void linkToPageClass(PageClass src, List<PageClass> dests) {		
		String xpath = getXpath();
		if (!src.hasLink(xpath)) {
			src.addListLink(xpath, dests.get(0));
		}
		else if (src.hasSingleLink(xpath)) {
			src.removeLink(xpath);
			src.addListLink(xpath, dests.get(0));
		}
		else if (src.hasMenuLink(xpath)) {
			src.removeMenuLink(xpath);
			src.addListLink(xpath, dests.get(0));
		}
	}

}
