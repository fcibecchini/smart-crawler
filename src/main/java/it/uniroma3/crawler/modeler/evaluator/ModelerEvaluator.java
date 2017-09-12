package it.uniroma3.crawler.modeler.evaluator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.util.ByteString;
import it.uniroma3.crawler.model.ClassLink;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.modeler.model.ModelPageClass;
import it.uniroma3.crawler.modeler.model.Page;
import it.uniroma3.crawler.modeler.model.WebsiteModel;

public class ModelerEvaluator extends AbstractLoggingActor {
	private TrueModel goldenModel;
	private WebsiteModel computedModel;
	private String domain;
	private Map<String,Integer> matrix;
	private Map<ModelPageClass,Double> cohesions;
	private Map<ModelPageClass,Double> purities;
	private int trueClassesPages;
	private double fmeasure, cohesion, purity, linksFmeasure;
		
	public static Props props(ByteString msg) {
		return Props.create(ModelerEvaluator.class, () -> new ModelerEvaluator(msg));
	}
	
	public ModelerEvaluator(ByteString msg) {
		this.goldenModel = loadGoldenModel(msg);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(WebsiteModel.class, this::evaluateModel)
		.build();
	}
	
	public void evaluateModel(WebsiteModel model) {
		log().info("START Model evaluation");
		
		loadStatistics(model);
		calculateFmeasure();
		calculateCohesion();
		calculatePurity();
		calculateLinksFMeasure();
		
		sender().tell(ByteString.fromString(getStatistics()), context().parent());
				
		log().info("END evaluation");
	}
	
	private String getStatistics() {
		StringBuilder build = new StringBuilder();
		
		build.append("WEBSITE\tF-MEASURE\tCOHESION\tPURITY\tLINKS_F-MEASURE\t#PAGES\n");
		build.append(getWebsiteStatistics()+"\n");
		
		build.append("PAGECLASS\tCLASS_SIZE\tCLASS_LINKS\tCOHESION\tPURITY\n");
		computedModel.getClasses().forEach(c -> build.append(getModelClassStatistics(c)+"\n"));
		
		build.append("GOLDEN_CLASS\t"
				+ "PAGECLASS\t"
				+ "CLASS_SIZE\t"
				+ "CLASS_LINKS\t"
				+ "CLUSTER_PRECISION\t"
				+ "CLUSTER_RECALL\t"
				+ "CLUSTER_F-MEASURE\t"
				+ "LINKS_PRECISION\t"
				+ "LINKS_RECALL\t"
				+ "LINKS_F-MEASURE\n");
		goldenModel.getClasses().forEach(c -> build.append(c.getStatistics()+"\n"));
		
		return build.toString();
	}
	
	private String getWebsiteStatistics() {
		DecimalFormat df = new DecimalFormat("#.##");
		return domain+"\t"+
				df.format(fmeasure)+"\t"+
				df.format(cohesion)+"\t"+
				df.format(purity)+"\t"+
				df.format(linksFmeasure)+"\t"+
				trueClassesPages;
	}
	
	private String getModelClassStatistics(ModelPageClass mpc) {
		DecimalFormat df = new DecimalFormat("#.##");
		String name = mpc.getPageClass().getName();
		int size = mpc.size();
		int linksSize = mpc.getPageClass().getLinks().size();
		Double cohesion = cohesions.get(mpc);
		Double purity = purities.get(mpc);
		String cs = (cohesion!=null) ? df.format(cohesion) : "-";
		String ps = (purity!=null) ? df.format(purity) : "-";
		return name+"\t"+size+"\t"+linksSize+"\t"+cs+"\t"+ps;
	}
	
	private void loadStatistics(WebsiteModel model) {
		computedModel = model;
		domain = model.getClasses().first().getPageClass().getDomain();
		matrix = countMatrix();
		setTrueClassesSize();
		trueClassesPages = goldenModel.getClasses().stream()
				.mapToInt(tc -> tc.size())
				.reduce(Integer::sum).getAsInt();
	}
	
	private void calculateFmeasure() {
		for (TrueClass tc : goldenModel.getClasses()) {
			double fm=-1, p=-1, r=-1;
			ModelPageClass computed=null;
			for (ModelPageClass mpc : computedModel.getClasses()) {
				double precision = precision(tc,mpc);
				double recall = recall(tc,mpc);
				double fmeasure = fMeasure(precision,recall);
				if (fmeasure>fm) {
					fm = fmeasure;
					p = precision;
					r = recall;
					computed = mpc;
				}
			}
			tc.setComputedClass(computed);
			tc.setPrecision(p);
			tc.setRecall(r);
			tc.setFmeasure(fm);
			fmeasure += fm*((double) tc.size() / (double)trueClassesPages);
		}
	}
	
	private void calculateCohesion() {
		cohesions = new HashMap<>();
		double totalSize = 0;
		for (ModelPageClass mpc : computedModel.getClasses()) {
			double cohesion = 0;
			boolean found = false;
			for (TrueClass tClass : goldenModel.getClasses()) {
				double p = precision(tClass,mpc);
				if (p>0) found=true;
				cohesion += (p==0) ? 0 : p*Math.log(p);
			}
			cohesion = (cohesion==0) ? 0 : -cohesion;
			if (found) {
				cohesions.put(mpc, cohesion);
				totalSize+=mpc.size();
			}
		}

		for (ModelPageClass mpc : computedModel.getClasses()) {
			Double c = cohesions.get(mpc);
			if (c!=null) cohesion += c * ((double)mpc.size() / (double)totalSize);
		}
		
	}
	
	private void calculatePurity() {
		purities = new HashMap<>();
		double totalSize = 0;
		for (ModelPageClass mpc : computedModel.getClasses()) {
			double max = 0;
			for (TrueClass tClass : goldenModel.getClasses()) {
				int count = matrix.get(tClass.getName()+"\t"+mpc.getId());
				if (count>max)
					max = count;
			}
			double purity = max / (double) mpc.size();
			if (purity>0) {
				purities.put(mpc, purity);
				totalSize+=mpc.size();
			}
		}

		for (ModelPageClass mpc : computedModel.getClasses()) {
			Double p = purities.get(mpc); 
			if (p!=null) purity += p * ((double)mpc.size() / (double)totalSize);
		}
	}
	
	/*
	 * Map["i,j"] => count of elements from true class i assigned to computed class j
	 */
	private Map<String, Integer> countMatrix() {
		Map<String, Integer> matrix = new TreeMap<>();
		for (TrueClass tClass : goldenModel.getClasses()) {
			for (ModelPageClass mpc : computedModel.getClasses()) {
				int count=0;
				for (Page p : mpc.getPages()) {
					boolean belong = tClass.belongsToClass(p.getUrl());
					if (belong)
						count++;
				}
				matrix.put(tClass.getName()+"\t"+mpc.getId(), count);
			}
		}
		return matrix;
	}
	
	private void calculateLinksFMeasure() {
		double totalLinksSize = 0;
		for (TrueClass tc : goldenModel.getClasses()) {
			totalLinksSize += tc.getLinks().size();
		}

		for (TrueClass tc : goldenModel.getClasses()) {
			double size = tc.getLinks().size();
			if (size>0) {
				setLinksPrecisionAndRecall(tc);
				linksFmeasure += tc.getLinksFmeasure() * (size / totalLinksSize);
			}
		}
	}
	
	private void setLinksPrecisionAndRecall(TrueClass tc) {
		double count = 0;
		PageClass pc = tc.getComputedClass().getPageClass();
		List<ClassLink> pLinks = new ArrayList<>(pc.getLinks());
		int pLinksSize = pLinks.size();
		List<TrueLink> tLinks = tc.getLinks();
		for (TrueLink tl : tLinks) {
			String type = tl.getType();
			PageClass dest = tl.getDestination().getComputedClass().getPageClass();
			ClassLink current=null;
			for (ClassLink pl : pLinks) {
				if ((type.equals(pl.getType()) || (type.equals("singleton") && pl.isSingleton()))
						&& dest.equals(pl.getDestination())) {
					count++;
					current=pl;
					break;
				}
			}
			if(current!=null) pLinks.remove(current);
		}
		double p = (pLinksSize>0) ? count / (double) pLinksSize : 0;
		double r = count / (double) tLinks.size();
		double f = fMeasure(p,r);
		tc.setLinksPrecision(p);
		tc.setLinksRecall(r);
		tc.setLinksFmeasure(f);
	}
	
	private void setTrueClassesSize() {
		for (TrueClass tc : goldenModel.getClasses()) {
			int sum = 0;
			for (ModelPageClass mpcc : computedModel.getClasses()) {
				sum += matrix.get(tc.getName()+"\t"+mpcc.getId());
			}
			tc.setSize(sum);
		}
	}
	
	private double precision(TrueClass tc, ModelPageClass mpc) {
		double count = matrix.get(tc.getName()+"\t"+mpc.getId());
		double size = mpc.size();
		return (count==0) ? 0 : count / size;
	}
	
	private double recall(TrueClass tc, ModelPageClass mpc) {
		double count = matrix.get(tc.getName()+"\t"+mpc.getId());
		double size = tc.size();
		return (count==0) ? 0 : count / size;
	}
	
	private double fMeasure(double precision, double recall) {
		double sum = precision+recall;
		if (sum==0) return 0;
		return (2*precision*recall) / sum;
	}
	
    private TrueModel loadGoldenModel(ByteString msg) {
    	String[] csv = msg.utf8String().split("\n");
		Queue<String> queue = new LinkedList<>(Arrays.asList(csv));
		
		TrueModel tModel = new TrueModel();
		while (!queue.isEmpty()) {
			String line = queue.poll();
			String[] fields = line.split("\t");
			String name = fields[0];
			if (name.isEmpty())
				break;
			TrueClass tClass = new TrueClass(name);
			String[] patterns = fields[1].split(",");
			for (String pattern : patterns) {
				tClass.addPattern(pattern);
			}
			tModel.addClass(tClass);
		}

		while (!queue.isEmpty()) {
			String line = queue.poll();
			String[] fields = line.split("\t");
			TrueClass src = tModel.getByName(fields[0]);
			String type = fields[1];
			TrueClass dest = tModel.getByName(fields[2]);
			src.addLink(type, dest);
		}
		return tModel;
    }

}
