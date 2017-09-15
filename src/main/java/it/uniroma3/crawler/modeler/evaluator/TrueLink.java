package it.uniroma3.crawler.modeler.evaluator;

public class TrueLink {
	private String type;
	private TrueClass dest;
	
	public TrueLink(String type, TrueClass dest) {
		this.type = type;
		this.dest = dest;
	}
	
	public String getType() {
		return type;
	}
	
	public TrueClass getDestination() {
		return dest;
	}

}
