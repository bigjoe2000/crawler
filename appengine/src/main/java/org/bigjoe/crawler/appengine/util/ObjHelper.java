package org.bigjoe.crawler.appengine.util;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;

import org.bigjoe.crawler.appengine.model.FoundImage;
import org.bigjoe.crawler.appengine.model.Job;
import org.bigjoe.crawler.appengine.model.SeenUrl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.util.Closeable;


@WebListener
@WebFilter("/*")
public class ObjHelper implements ServletContextListener, Filter {
	private static Logger log = Logger.getLogger(ObjHelper.class.getName());

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		register();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
	
	public static void register() {
		log.info("Registering Objectify entities");
		ObjectifyService.register(Job.class);
		ObjectifyService.register(SeenUrl.class);
		ObjectifyService.register(FoundImage.class);
	}

	public static <T> LoadType<T> query(Class<T> clazz) {
		return ofy().load().type(clazz);
	}

	public static <T> T load(Key<T> key) {
		return ofy().load().key(key).now();
	}

	public static <T> Key<T> save(T obj) {
		return ofy().save().entity(obj).now();
	}
	
	public static <T> boolean exists(Key<T> key) {
		return ofy().load().filterKey(key).count() > 0;
	}

	public static void delete(Job job) {
		ofy().delete().keys(ofy().load().type(FoundImage.class).ancestor(job).keys().list()).now();
		ofy().delete().keys(ofy().load().type(SeenUrl.class).ancestor(job).keys().list()).now();
		ofy().delete().entity(job).now();
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		log.info("doFilter");
		try (Closeable closeable = ObjectifyService.begin()) {
			ObjHelper.register();
			chain.doFilter(request, response);
		}
		log.info("Done filter");
		
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}
