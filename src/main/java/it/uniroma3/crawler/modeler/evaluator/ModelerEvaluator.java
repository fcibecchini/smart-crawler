package it.uniroma3.crawler.modeler.evaluator;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static it.uniroma3.crawler.util.FileUtils.normalizeURL;
import static it.uniroma3.crawler.util.Commands.STOP;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
	private Map<String,Integer> class2size;
	private Map<ModelPageClass,Double> cohesions;
	private Map<ModelPageClass,Double> purities;
	private int pages;
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
		
		saveToFile();
		//System.out.println(getStatistics());
		
		log().info("END evaluation");
		context().parent().tell(STOP, self());
	}
	
	private void saveToFile() {
		try {
			File file = new File("src/main/resources/evaluations/"+normalizeURL(domain)+".csv");
			writeStringToFile(file, getStatistics(), Charset.forName("UTF-8"));
		} catch (IOException ie) {
			log().warning("IOException while printing Website Statistics: "+ie.getMessage());
		}
	}
	
	private String getStatistics() {
		StringBuilder build = new StringBuilder();
		
		build.append("WEBSITE\tF-MEASURE\tCOHESION\tPURITY\tLINKS_F-MEASURE\t#PAGES\n");
		build.append(getWebsiteStatistics()+"\n");
		
		build.append("PAGECLASS\tCOHESION\tPURITY\n");
		computedModel.getClasses().forEach(c -> build.append(getModelClassStatistics(c)+"\n"));
		
		build.append("GOLDEN_CLASS\tPAGECLASS\t"
				+ "CLUSTER_PRECISION\tCLUSTER_RECALL\tCLUSTER_F-MEASURE\t"
				+ "LINKS_PRECISION\tLINKS_RECALL\tLINKS_F-MEASURE\n");
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
				pages;
	}
	
	private String getModelClassStatistics(ModelPageClass mpc) {
		DecimalFormat df = new DecimalFormat("#.##");
		String name = mpc.getPageClass().getName();
		return name+"\t"+df.format(cohesions.get(mpc))+"\t"+df.format(purities.get(mpc));
	}
	
	private void loadStatistics(WebsiteModel model) {
		computedModel = model;
		domain = model.getClasses().first().getPageClass().getDomain();
		matrix = countMatrix();
		class2size = getSizes();
		pages = matrix.values().stream().reduce(Integer::sum).get();
	}
	
	private void calculateFmeasure() {
		for (TrueClass tClass : goldenModel.getClasses()) {
			double fm=0, p=0, r=0;
			ModelPageClass computed=null;
			for (ModelPageClass mpc : computedModel.getClasses()) {
				double precision = precision(tClass,mpc);
				double recall = recall(tClass,mpc);
				double fmeasure = fMeasure(precision,recall);
				if (fmeasure>fm) {
					fm = fmeasure;
					p = precision;
					r = recall;
					computed = mpc;
				}
			}
			tClass.setComputedClass(computed);
			tClass.setPrecision(p);
			tClass.setRecall(r);
			tClass.setFmeasure(fm);
			fmeasure += fm*((double) class2size.get(tClass.getName()) / (double)pages);
		}
	}
	
	private void calculateCohesion() {
		cohesions = new HashMap<>();
		for (ModelPageClass mpc : computedModel.getClasses()) {
			String id = String.valueOf(mpc.getId());
			double cohesion = 0;
			for (TrueClass tClass : goldenModel.getClasses()) {
				double p = precision(tClass,mpc);
				cohesion += (p==0) ? 0 : p*Math.log(p);
			}
			cohesion = (cohesion==0) ? 0 : -cohesion;
			cohesions.put(mpc, cohesion);
			this.cohesion += cohesion * ((double)class2size.get(id) / (double)pages);
		}
	}
	
	private void calculatePurity() {
		purities = new HashMap<>();
		for (ModelPageClass mpc : computedModel.getClasses()) {
			String id = String.valueOf(mpc.getId());
			double max = 0;
			for (TrueClass tClass : goldenModel.getClasses()) {
				int count = matrix.get(tClass.getName()+"\t"+id);
				if (count>max)
					max = count;
			}
			double purity = max / (double) class2size.get(id);
			purities.put(mpc, purity);
			this.purity += purity * ((double)class2size.get(id) / (double)pages);
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
					if (tClass.belongsToClass(p.getUrl()))
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
				linksFmeasure += tc.getFmeasure() * (size / totalLinksSize);
			}
		}
	}
	
	private void setLinksPrecisionAndRecall(TrueClass tc) {
		double count = 0;
		PageClass pc = tc.getComputedClass().getPageClass();
		Set<ClassLink> pLinks = pc.getLinks();
		List<TrueLink> tLinks = tc.getLinks();
		for (TrueLink tl : tLinks) {
			String type = tl.getType();
			PageClass dest = tl.getDestination().getComputedClass().getPageClass();
			for (ClassLink pl : pLinks) {
				if (type.equals(pl.getType()) && dest.equals(pl.getDestination())) {
					count++;
					break;
				}
			}
		}
		double p = count / (double) pLinks.size();
		double r = count / (double) tLinks.size();
		double f = fMeasure(p,r);
		tc.setLinksPrecision(p);
		tc.setLinksRecall(r);
		tc.setLinksFmeasure(f);
	}
	
	private Map<String, Integer> getSizes() {
		Map<String, Integer> matrix = new TreeMap<>();
		for (TrueClass tc : goldenModel.getClasses()) {
			matrix.put(tc.getName(), sizeOfClass(tc));
		}
		for (ModelPageClass mpc : computedModel.getClasses()) {
			matrix.put(String.valueOf(mpc.getId()), sizeOfClass(mpc));
		}
		return matrix;
	}
	
	private double precision(TrueClass tc, ModelPageClass mpc) {
		double count = matrix.get(tc.getName()+"\t"+mpc.getId());
		double size = class2size.get(String.valueOf(mpc.getId()));
		return count / size ;
	}
	
	private double recall(TrueClass tc, ModelPageClass mpc) {
		double count = matrix.get(tc.getName()+"\t"+mpc.getId());
		double size = class2size.get(tc.getName());
		return count / size;
	}
	
	private int sizeOfClass(TrueClass tc) {
		String name = tc.getName();
		int sum = 0;
		for (ModelPageClass mpcc : computedModel.getClasses()) {
			sum += matrix.get(name+"\t"+mpcc.getId());
		}
		return sum;
	}
	
	private int sizeOfClass(ModelPageClass mpc) {
		int id = mpc.getId();
		int sum = 0;
		for (TrueClass tcc : goldenModel.getClasses()) {
			sum += matrix.get(tcc.getName()+"\t"+id);
		}
		return sum;
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
