package it.uniroma3.crawler.modeler;

import static it.uniroma3.crawler.util.Commands.*;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.remote.RemoteScope;
import it.uniroma3.crawler.messages.ModelMsg;
import it.uniroma3.crawler.model.PageClass;
import it.uniroma3.crawler.settings.CrawlerSettings.SeedConfig;

public class CrawlModeler extends AbstractActor {
	private boolean crawl;

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(ModelMsg.class, this::start)
		.match(PageClass.class, this::sendAndSave)
		.matchEquals(STOP, s -> context().stop(self()))
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
			ActorRef service = 
					context().actorOf(Props.create(ModelerService.class),"service");
			context().watch(service);
			service.tell(conf, self());
		}
	}
	
	private void sendAndSave(PageClass root) {
		if (crawl) context().parent().tell(root, self());
		if (sender().path().name().equals("dynamic")) {
			ActorRef service = 
					context().actorOf(Props.create(ModelerService.class),"service");
			context().watch(service);
			service.tell(root, self());
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
    
}
