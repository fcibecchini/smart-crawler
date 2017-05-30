package it.uniroma3.crawler.settings;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

import akka.actor.Extension;

public class NodesSettings implements Extension {
	public final String[] nodes;
	
	public NodesSettings(Config config) {
		nodes = addresses(config.getObject("nodes"));
	}
	
	private String[] addresses(ConfigObject nodes) {
		String[] addr = new String[nodes.keySet().size()];
		int i=0;
		for (String k : nodes.keySet()) {
			Config node = nodes.toConfig().getConfig(k);
			String host = node.getString("host");
			int port = node.getInt("port");
			String system = node.getString("system");
			addr[i] = "akka://"+system+"@"+host+":"+port;
		}
		return addr;
	}
}
