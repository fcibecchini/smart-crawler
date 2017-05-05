package it.uniroma3.crawler.settings;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

public class Settings extends AbstractExtensionId<CrawlerSettings> 
						implements ExtensionIdProvider {
	
	public final static Settings SettingsProvider = new Settings();

	private Settings() {}

	public Settings lookup() {
		return Settings.SettingsProvider;
	}

	public CrawlerSettings createExtension(ExtendedActorSystem system) {
		return new CrawlerSettings(system.settings().config());
	}
}
