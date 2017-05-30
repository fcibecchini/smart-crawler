package it.uniroma3.crawler.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.remote.RemoteScope;
import it.uniroma3.crawler.messages.*;
import it.uniroma3.crawler.settings.AddressSettings;

import static it.uniroma3.crawler.util.Commands.SAVE;

import scala.Option;

public class CrawlRepository extends AbstractActor {
	private final ActorRef csvCache;
	private final String[] nodes;
	
	public CrawlRepository() {
		this.csvCache = context().actorOf(Props.create(CrawlUrlClass.class), "csvcache");
		context().watch(csvCache);
		this.nodes = AddressSettings.SettingsProvider.get(context().system()).nodes;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(FetchMsg.class, msg -> 
			findOrCreate(msg.getUrl(), msg.getId()).forward(msg, context()))
		.match(SaveMsg.class, msg -> 
			find(msg.getUrl()).tell(SAVE, sender()))
		.match(ExtractLinksMsg.class, msg -> 
			find(msg.getUrl()).forward(msg, context()))
		.match(ExtractDataMsg.class, msg -> 
			find(msg.getUrl()).forward(msg, context()))
		.match(SaveCacheMsg.class, 
			msg -> csvCache.forward(msg, context()))
		.match(StopMsg.class, this::stopChildPage)
		.build();
	}
	
	private void stopChildPage(StopMsg msg) {
		ActorRef child = find(msg.getUrl());
		context().unwatch(child);
		context().stop(child);
	}
	
	private ActorRef findOrCreate(String url, int id) {
		ActorRef child = find(url);
		if (child==null) child = create(url, id);
		return child;
	}
	
	private ActorRef find(String url) {
		String name = formatUrl(url);
		Option<ActorRef> option = context().child(name);
		ActorRef ref = (option.isEmpty()) ? null : option.get();
		return ref;
	}
	
	private ActorRef create(String url, int id) {	
		String name = formatUrl(url);
		Address addr = AddressFromURIString.parse(nodes[id]);
		Props props = Props.create(CrawlPage.class).withDeploy(new Deploy(new RemoteScope(addr)));
		ActorRef child = context().actorOf(props, name);		
		context().watch(child);
		return child;
	}
	
	private String formatUrl(String id) {
		return id.replaceAll("/", "_").replaceAll("\\?", "-");
	}
}
