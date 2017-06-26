package it.uniroma3.crawler.modeler.evaluator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import it.uniroma3.crawler.modeler.model.ModelPageClass;

public class TrueClass {
	private String name;
	private List<Pattern> patterns;
	private List<TrueLink> links;
	
	private ModelPageClass computedClass;
	
	private int size;
	private double precision, recall, fmeasure;
	private double linksPrecision, linksRecall, linksFmeasure;
	
	public TrueClass(String name) {
		this.name = name;
		this.patterns = new ArrayList<>();
		this.links = new ArrayList<>();
	}
	
	public String getName() {
		return this.name;
	}
	
	public List<TrueLink> getLinks() {
		return links;
	}
	
	public void addPattern(String string) {
		Pattern pattern = Pattern.compile(string);
		patterns.add(pattern);
	}
	
	public void addLink(String type, TrueClass dest) {
		links.add(new TrueLink(type, dest));
	}
	
	public boolean belongsToClass(String url) {
		return patterns.stream().anyMatch(p -> p.matcher(url).find());
	}
	
	public void setComputedClass(ModelPageClass mpc) {
		this.computedClass = mpc;
	}
	
	public ModelPageClass getComputedClass() {
		return computedClass;
	}
	
	public void setPrecision(double p) {
		this.precision = p;
	}
	
	public void setRecall(double r) {
		this.recall = r;
	}
	
	public void setFmeasure(double fmeasure) {
		this.fmeasure = fmeasure;
	}
	
	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getFmeasure() {
		return fmeasure;
	}

	public double getLinksPrecision() {
		return linksPrecision;
	}

	public void setLinksPrecision(double linksPrecision) {
		this.linksPrecision = linksPrecision;
	}

	public double getLinksRecall() {
		return linksRecall;
	}

	public void setLinksRecall(double linksRecall) {
		this.linksRecall = linksRecall;
	}

	public double getLinksFmeasure() {
		return linksFmeasure;
	}

	public void setLinksFmeasure(double linksFmeasure) {
		this.linksFmeasure = linksFmeasure;
	}

	public int size() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getStatistics() {
		DecimalFormat df = new DecimalFormat("#.##");
		String format1 = name+"\t"+computedClass.getPageClass().getName();
		String format2 = computedClass.size()+"\t"+computedClass.getPageClass().getLinks().size();
		String format3 = df.format(precision)+"\t"+df.format(recall)+"\t"+df.format(fmeasure);
		String format4 = (links.size()>0) ?
			df.format(linksPrecision)+"\t"+df.format(linksRecall)+"\t"+df.format(linksFmeasure)
			: "-\t-\t-";
		return format1+"\t"+format2+"\t"+format3+"\t"+format4;
	}

	public int hashCode() {
		return name.hashCode();
	}
	
	public boolean equals(Object other) {
		TrueClass o = (TrueClass) other;
		return Objects.equals(getName(), o.getName());
	}

}
