package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.*;
import static org.apache.commons.io.FileUtils.writeStringToFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.remote.RemoteScope;
import akka.util.ByteString;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlModeler extends AbstractLoggingActor {	
	private static final String GOLDEN_PATH = "src/main/resources/golden/";
	private static final String EVALUATION_PATH = "src/main/resources/evaluations/";
	
	private boolean crawl;
	private int children;
	private String goldenModel;

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(ModelMsg.class, this::start)
		.match(PageClass.class, this::sendAndSave)
		.match(ByteString.class, this::saveEvaluation)
		.matchEquals(STOP, s -> stopChild(sender()))
		.build();
	}
	
	private void start(ModelMsg msg) {
		SeedConfig conf = msg.getConf();
		String addr = msg.getAddress();
		crawl = conf.crawl;
		goldenModel = conf.goldenModel;
		if (conf.modelPages>0) {
			ActorRef dynamic = dynamicModeler(conf, addr);
			context().watch(dynamic);
			dynamic.tell(conf, self());
		}
		else {
			ActorRef service = context().actorOf(Props.create(ModelerService.class),"service");
			context().watch(service);
			service.tell(conf, self());
		}
		children++;
	}
	
	private void sendAndSave(PageClass root) {
		if (crawl) context().parent().tell(root, self());
		if (sender().path().name().equals("dynamic")) {
			ActorRef service = context().actorOf(Props.create(ModelerService.class),"service");
			context().watch(service);
			service.tell(root, self());
			children++;
			
			sendGolden();
		}
	}
    
    private ActorRef dynamicModeler(SeedConfig sc, String addr) {
    	Props p = Props.create(DynamicModeler.class);
    	if (!addr.isEmpty()) {
    		Address a = AddressFromURIString.parse(addr);
    		p = p.withDeploy(new Deploy(new RemoteScope(a)));
    	}
    	return context().actorOf(p, "dynamic");
    }
    
    private void sendGolden() {
    	if (goldenModel!=null) {
			try {
		    	Path file = Paths.get(GOLDEN_PATH+goldenModel);
				byte[] byteFile = Files.readAllBytes(file);
				ByteString msg = ByteString.fromArray(byteFile);
				sender().tell(msg,self());
			} catch (IOException e) {
				log().warning("Could not read golden model for Evaluation: "+e.getMessage());
				stopChild(sender());
			}
    	}
		else stopChild(sender());
    }
    
    private void saveEvaluation(ByteString stats) {
		try {
			File file = new File(EVALUATION_PATH+goldenModel);
			writeStringToFile(file, stats.utf8String(), Charset.forName("UTF-8"));
		} catch (IOException ie) {
			log().warning("IOException while printing Evaluation: "+ie.getMessage());
		}
		stopChild(sender());
    }
    
    private void stopChild(ActorRef ref) {
    	context().unwatch(ref);
		context().stop(ref);
		if (--children==0) {
			if (!crawl) context().parent().tell(SAVED, self());
			context().stop(self());
		}
    }
    
}
