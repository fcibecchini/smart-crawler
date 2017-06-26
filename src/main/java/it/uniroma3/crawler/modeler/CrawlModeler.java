package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.STOP;

import java.io.IOException;
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
import it.uniroma3.crawler.util.FileUtils;

public class CrawlModeler extends AbstractLoggingActor {
	private boolean crawl;
	private int children;

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(ModelMsg.class, this::start)
		.match(PageClass.class, this::sendAndSave)
		.matchEquals(STOP, s -> {
			context().unwatch(sender());
			context().stop(sender());
			children--;
			if (children==0) context().stop(self());
		})
		.build();
	}
	
	private void start(ModelMsg msg) {
		SeedConfig conf = msg.getConf();
		String addr = msg.getAddress();
		crawl = conf.crawl;
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
			
			sendGolden(root.getDomain());
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
    
    private void sendGolden(String domain) {
    	String name = FileUtils.normalizeURL(domain);
    	Path file = Paths.get("src/main/resources/golden/"+name+".csv");
		if (Files.exists(file)) {
			try {
				byte[] byteFile = Files.readAllBytes(file);
				ByteString msg = ByteString.fromArray(byteFile);
				sender().tell(msg,self());
			} catch (IOException e) {
				log().warning("IOException: "+e.getMessage());
				self().tell(STOP, sender());
			}
		}
		else self().tell(STOP, sender());
    }
    
}
