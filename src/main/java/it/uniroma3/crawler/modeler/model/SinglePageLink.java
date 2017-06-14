package it.uniroma3.crawler.modeler.model;

import java.util.List;

import it.uniroma3.crawler.model.PageClass;

public class SinglePageLink extends PageLink {

	public SinglePageLink(String xpath, List<Page> destinations) {
		super(xpath, destinations);
	}

	@Override
	public void linkToPageClass(PageClass src, List<PageClass> dests) {		
		String xpath = getXpath();
		if (!src.hasLink(xpath)) {
			src.addSingletonLink(xpath, dests.get(0));
		}
	}

}
