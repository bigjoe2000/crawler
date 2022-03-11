package org.bigjoe.crawler.appengine.model;

import java.net.MalformedURLException;
import java.net.URL;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class Job {

	@Id
	String url;
	String domain;
	boolean running = true;
	int maxPages = 1000;
	int level = 4;
	
	public Job() {}
	
	public Job(String url, int level, int maxPages) throws MalformedURLException {
		this.url = url;
		domain = new URL(url).getHost();
		this.maxPages = maxPages;
		this.level = level;
	}
	
	public static Key<Job> generateKey(String url) {
		return Key.create(Job.class, url);
	}
	
	public String getUrl() {
		return url;
	}
	
	public Key<Job> getKey() {
		return Key.create(this);
	}
	
	public String getDomain() {
		return domain;
	}
	
	public int getMaxPages() {
		return maxPages;
	}

	public boolean isRunning() {
		return running;
	}

	public void shutdown() {
		this.running = false;
	}
}
