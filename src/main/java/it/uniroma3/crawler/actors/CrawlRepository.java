package it.uniroma3.crawler.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.japi.Creator;
import akka.remote.RemoteScope;
import it.uniroma3.crawler.messages.*;
import scala.Option;

public class CrawlRepository extends AbstractActor {
	private final ActorRef csvCache;
	
	static class InnerProps implements Creator<CrawlRepository> {
		private String csv;
		
		public InnerProps(String csv) {
			this.csv = csv;
		}

		@Override
		public CrawlRepository create() throws Exception {
			return new CrawlRepository(csv);
		}
		
	}
	
	public static Props props(String csv) {
		return Props.create(CrawlRepository.class, new InnerProps(csv));
	}
	
	public CrawlRepository(String csvFile) {
		this.csvCache = context().actorOf(CrawlUrlClass.props(csvFile), "csvcache");
		context().watch(csvCache);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(FetchMsg.class, msg -> 
			findOrCreate(msg.getUrl()).forward(msg, context()))
		.match(SaveMsg.class, msg -> 
			find(msg.getUrl()).forward(msg, context()))
		.match(ExtractLinksMsg.class, msg -> 
			findOrCreate(msg.getUrl()).forward(msg, context()))
		.match(ExtractDataMsg.class, msg -> 
			findOrCreate(msg.getUrl()).forward(msg, context()))
		.match(SaveCacheMsg.class, 
			msg -> csvCache.forward(msg, context()))
		.match(ResolveLinksMsg.class,
			msg -> csvCache.forward(msg, context()))
		.match(StopMsg.class, this::stopChildPage)
		.build();
	}
	
	private void stopChildPage(StopMsg msg) {
		ActorRef child = find(msg.getUrl());
		context().unwatch(child);
		context().stop(child);
	}
	
	private ActorRef findOrCreate(String url) {
		ActorRef child = find(url);
		if (child==null) child = create(url);
		return child;
	}
	
	private ActorRef find(String url) {
		String name = formatUrl(url);
		Option<ActorRef> option = context().child(name);
		return (option.isEmpty()) ? null : option.get();
	}
	
	private ActorRef create(String url) {
		/* TODO: Map each Id to a different host */
		//Address addr = new Address("akka.tcp", "CrawlSystem", "host", 2552);
		String name = formatUrl(url);
		ActorRef child = context().actorOf(Props.create(CrawlPage.class), name);
				//.withDeploy(new Deploy(new RemoteScope(addr))), name);
		context().watch(child);
		return child;
	}
	
	private String formatUrl(String id) {
		return id.replaceAll("/", "_").replaceAll("\\?", "-");
	}
}
