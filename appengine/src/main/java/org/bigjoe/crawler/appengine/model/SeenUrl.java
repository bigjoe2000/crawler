package org.bigjoe.crawler.appengine.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class SeenUrl {

	@Id
	private String url;
	@Parent
	private Key<Job> job;
	@Index
	private boolean done = false;
	
	public SeenUrl() {}
	
	public SeenUrl(Key<Job> job, String url) {
		this.url = url;
		this.job = job;
	}
	
	public static Key<SeenUrl> generateKey(Key<Job> job, String url) {
		return Key.create(job, SeenUrl.class, url);
	}
	
	public Key<SeenUrl> getKey() {
		return Key.create(this);
	}
	
	public void setDone() {
		done = true;
	}
	
	public boolean isDone() {
		return done;
	}
	
	public String getUrl() {
		return url;
	}
}
