package it.uniroma3.crawler.messages;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class FetchMsg {
	private final String url;
	private final List<NameValuePair> params;
	private final String form;
	private final String pclass;
	private final String domain;
	private final int id;
	private final boolean js;
		
	public FetchMsg(String url, String pclass, String domain, int id, boolean js) {
		this.url = url;
		this.form = null;
		this.params = new ArrayList<>();
		this.pclass = pclass;
		this.domain = domain;
		this.id = id;
		this.js = js;
	}
	
	public FetchMsg(String url, String form, 
			List<NameValuePair> params, String pclass, String domain, int id, boolean js) {
		this.url = url;
		this.form = form;
		this.params = params;
		this.pclass = pclass;
		this.domain = domain;
		this.id = id;
		this.js = js;
	}
	
	public int getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}
	
	public String getForm() {
		return form;
	}

	public List<NameValuePair> getParams() {
		return params;
	}

	public String getPageClass() {
		return pclass;
	}
	
	public String getDomain() {
		return domain;
	}

	public boolean useJavaScript() {
		return js;
	}

}
