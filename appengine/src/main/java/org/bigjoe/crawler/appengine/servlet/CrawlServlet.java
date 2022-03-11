package org.bigjoe.crawler.appengine.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bigjoe.crawler.appengine.model.FoundImage;
import org.bigjoe.crawler.appengine.model.Job;
import org.bigjoe.crawler.appengine.model.SeenUrl;
import org.bigjoe.crawler.appengine.util.ObjHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(name = "CrawlServlet", value = "/crawl/*")
public class CrawlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(CrawlServlet.class.getSimpleName());
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Queue queue = null;

	@Override
	public void init() throws ServletException {
		queue = QueueFactory.getQueue("crawl");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String url = req.getParameter("url");
		String jobId = req.getParameter("job");
		int level = Integer.parseInt(req.getParameter("level"));
		Job job = ObjHelper.load(Job.generateKey(jobId));
		if (job == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found");
			return;
		}
		// Yes, we could inadvertently crawl too many pages by only doing this query once.
		int urlsCount = ObjHelper.query(SeenUrl.class).ancestor(job).count();
		SeenUrl crawlUrl = ObjHelper.load(SeenUrl.generateKey(job.getKey(), url));
		if (crawlUrl.isDone()) {
			log.info("Page was already crawled");
			return;
		}
		try {
			Document doc = Jsoup.connect(url).get();
	
			for (Element el : doc.select("img")) {
				ObjHelper.save(new FoundImage(job.getKey(), el.absUrl("src")));
			}
				
			for (Element el : doc.select("a")) {
				String found = el.absUrl("href");
				try {
					if (job.getDomain().equals(new URL(found).getHost())) {
						SeenUrl seen = new SeenUrl(job.getKey(), found);
						if (!ObjHelper.exists(seen.getKey())) {
							ObjHelper.save(seen);
							queue.add(TaskOptions.Builder.withUrl("/crawl")
									.method(Method.POST)
									.param("job", jobId)
									.param("level", "" + (level - 1))
									.param("url", seen.getUrl()));
							if (++urlsCount >= job.getMaxPages())
								return;
						}
					}
				} catch (IOException e) {
	
				}
			}
		} catch (Exception e) {
			log.info("We got an exception, marking this page as done anyway:" + e.getMessage());
			
		}
		crawlUrl.setDone();
		ObjHelper.save(crawlUrl);
		return;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doGet(req, resp);
	}

}
