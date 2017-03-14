package it.uniroma3.crawler.actors.frontier;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import it.uniroma3.crawler.CrawlController;
import it.uniroma3.crawler.model.CrawlURL;
import scala.concurrent.duration.Duration;

public class BreadthFirstUrlFrontier extends UntypedActor implements UrlFrontier  {
	private final static String NEXT = "next";
	private Queue<CrawlURL> urlsQueue;
	private final int maxPages;
	private int pageCount;
	private ActorRef requester;
	private boolean isEmpty, isStopping;
	
	  public static Props props(final int maxPages) {
		    return Props.create(new Creator<BreadthFirstUrlFrontier>() {
		      private static final long serialVersionUID = 1L;
		 
		      @Override
		      public BreadthFirstUrlFrontier create() throws Exception {
		        return new BreadthFirstUrlFrontier(maxPages);
		      }
		    });
		  }

	public BreadthFirstUrlFrontier(int maxPages) {
		this.urlsQueue = new PriorityQueue<>(
				(CrawlURL c1, CrawlURL c2) -> c1.compareTo(c2));
		this.maxPages = maxPages;
		this.isEmpty = false;
		this.isStopping = false;
		this.pageCount = 0;
	}

	@Override
	public CrawlURL next() {
		pageCount++;
		return urlsQueue.poll();
	}

	@Override
	public void scheduleUrl(CrawlURL crawUrl) {
		urlsQueue.add(crawUrl);
	}

	@Override
	public boolean isEmpty() {
		return urlsQueue.isEmpty();
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (pageCount>maxPages) {
			if (!isStopping) {
				isStopping = true;
				CrawlController.getInstance().stopSystem();
			}
			// job is done..
		}
		else if (message.equals(NEXT)) {
			if (!isEmpty()) {
			// handle request for next url to be processed
			long wait = urlsQueue.peek().getPageClass().getWaitTime();
			getContext().system().scheduler().scheduleOnce(Duration
					.create(wait, TimeUnit.MILLISECONDS),
					  getSender(), next(), getContext().system().dispatcher(), getSelf());
			}
			else {
				isEmpty = true;
				// sender will be informed when 
				// a new CrawlURL is available
				requester = getSender(); 
			}
		}
		else if (message instanceof CrawlURL) {
			// store the received url
			CrawlURL cUrl = (CrawlURL) message;
			scheduleUrl(cUrl);
			if (isEmpty && requester!=null) { 
				// request next CrawlURL as if it was 
				// requested by the original fetcher
				getSelf().tell(NEXT, requester);
				isEmpty = false;
				requester = null;
			}
		}
		else if (message.equals("Stop")) {
			context().system().stop(getSelf());
		}
		
		else unhandled(message);
	}
	
}
