package it.uniroma3.crawler.writer;

import java.nio.charset.Charset;

import com.csvreader.CsvWriter;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

@Deprecated
public class Writer extends UntypedActor {
	
	public static Props props(String fileName) {
		return Props.create(new Creator<Writer>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Writer create() throws Exception {
				return new Writer(fileName);
			}
		});
	}

	final CsvWriter writer;
	private Integer count;
	
	public Writer(String fileName) {
		this.writer = new CsvWriter(fileName, '\t', Charset.forName("UTF-8"));
		this.count = 0;
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof String[]) {
			String[] record = (String[]) message;
			this.writer.writeRecord(record);
			count++;
			if (count==3) {
				this.writer.close();
				getContext().stop(getSelf());
			}
		}
		else unhandled(message);
	}

}
