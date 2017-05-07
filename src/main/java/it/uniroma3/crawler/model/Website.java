package it.uniroma3.crawler.model;

import java.util.Objects;

public class Website implements Comparable<Website>{
	private String domain;
	private int maxFetchTries;
	private int randomPause;
	private boolean javascript;
	
	public Website(String domain, int maxFetchTries, int pause, boolean js) {
		this.domain = domain;
		this.maxFetchTries = maxFetchTries;
		this.randomPause = pause;
		this.javascript = js;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public int getMaxFetchTries() {
		return maxFetchTries;
	}

	public void setMaxFetchTries(int maxFailures) {
		this.maxFetchTries = maxFailures;
	}
	
	public int getPause() {
		return randomPause;
	}

	public void sePause(int pause) {
		this.randomPause = pause;
	}

	public boolean isJavascript() {
		return javascript;
	}

	public void setJavascript(boolean javascript) {
		this.javascript = javascript;
	}

	@Override
	public int hashCode() {
		return domain.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		Website other = (Website) obj;
		return Objects.equals(domain, other.getDomain());
	}

	@Override
	public int compareTo(Website other) {
		return domain.compareTo(other.getDomain());
	}
}
