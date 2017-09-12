package it.uniroma3.crawler.modeler.model;

import java.util.List;

import it.uniroma3.crawler.model.PageClass;

public class SinglePageLink extends PageLink {
	private String text;

	public SinglePageLink(String xpath, List<Page> destinations) {
		super(xpath, destinations);
	}
	
	public SinglePageLink(String xpath, String text, List<Page> destinations) {
		super(xpath, destinations);
		this.text = text;
	}

	@Override
	public void linkToPageClass(PageClass src, List<PageClass> dests) {		
		String xpath = getXpath();
		if (!src.hasLink(xpath)) {
			src.addSingletonLink(xpath, text, dests.get(0));
		}
	}

}
