package org.bigjoe.crawler.appengine.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class FoundImage {

	@Id
	private String url;
	@Parent
	private Key<Job> job;
	
	public FoundImage() {}
	
	public FoundImage(Key<Job> job, String url) {
		this.job = job;
		this.url = url;
	}
}
