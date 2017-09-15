package it.uniroma3.crawler.settings;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

public class AddressSettings extends AbstractExtensionId<NodesSettings> 
implements ExtensionIdProvider{
	
	public final static AddressSettings SettingsProvider = new AddressSettings();

	private AddressSettings() {}

	@Override
	public NodesSettings createExtension(ExtendedActorSystem system) {
		return new NodesSettings(system.settings().config());
	}

	@Override
	public AddressSettings lookup() {
		return SettingsProvider;
	}

}
