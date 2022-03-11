package org.bigjoe.crawler.appengine.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bigjoe.crawler.appengine.model.FoundImage;
import org.bigjoe.crawler.appengine.model.Job;
import org.bigjoe.crawler.appengine.model.SeenUrl;
import org.bigjoe.crawler.appengine.util.ObjHelper;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

@WebServlet(name = "JobServlet", value = "/job/*")
public class JobServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String action = req.getPathInfo();
		if (action == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		switch (req.getPathInfo()) {
		case "/list": {
			JsonWriter writer = new JsonWriter(resp.getWriter());
			writer.beginArray();
			for (Job job : ObjHelper.query(Job.class)) {
				GSON.toJson(job, Job.class, writer);
			}
			writer.endArray();
			return;
		}
		case"/get": {
			Job job = ObjHelper.load(Job.generateKey(req.getParameter("url")));
			if (job == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			JsonWriter writer = new JsonWriter(resp.getWriter());
			writer.beginObject();
			writer.name("job");
			GSON.toJson(job, Job.class, writer);
			writer.name("images");
			writer.beginArray();
			for (FoundImage image : ObjHelper.query(FoundImage.class).ancestor(job)) {
				GSON.toJson(image, FoundImage.class, writer);
			}
			writer.endArray();
			writer.endObject();
			writer.close();
			return;
		}
		case "/checkJob": {
			String url = req.getParameter("url");
			Job job = ObjHelper.load(Job.generateKey(url));
			if (job == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if (!job.isRunning())
				return;
			if (ObjHelper.query(SeenUrl.class).ancestor(job).filter("done", false).count() <= 0) {
				job.shutdown();
				ObjHelper.save(job);
				return;
			}
			enqueueCheck(url);
			return;
		}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String url = req.getParameter("url");
		Job job = ObjHelper.load(Job.generateKey(url));
		switch (req.getPathInfo()) {
		case "/create": {
			int level = Integer.parseInt(req.getParameter("level"));
			int maxPages = Integer.parseInt(req.getParameter("maxPages"));
			if (job != null) {
				resp.sendError(HttpServletResponse.SC_CONFLICT, "That crawl already exists");
				return;
			}
			job = new Job(url, level, maxPages);
			ObjHelper.save(job);
			ObjHelper.save(new SeenUrl(job.getKey(), url));
			QueueFactory.getQueue("crawl").add(TaskOptions.Builder.withUrl("/crawl")
					.method(Method.POST)
					.param("job", job.getUrl())
					.param("url", url)
					.param("level", "" + level));
			resp.getWriter().print(GSON.toJson(job));
			enqueueCheck(url);
			return;			
		}
		case "/delete": {
			ObjHelper.delete(job);
			return;
		}
		}
	}

	public void enqueueCheck(String url) {
		QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/job/checkJob")
				.method(Method.GET)
				.param("url",  url)
				.countdownMillis(60 * 1000));
	}
}
