package org.bigjoe.crawler.jetty;

import java.io.IOException;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@WebServlet(urlPatterns = { "/*" }, loadOnStartup = 1)
public class CrawlingServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String url = req.getParameter("url");
		int level;
		try {
			level = Integer.parseInt(req.getParameter("level"));
		} catch (NumberFormatException e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "level parameter is not an integer");
			return;
		}
		if (url == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "url parameter is required");
			return;
		}
		try {
			Crawler crawler = new Crawler(url, level);
			crawler.crawl();
			resp.getWriter().print(String.join(",", crawler.images));
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public static class Crawler {
		final String domain;
		final int level;
		final String url;
		ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		KeySetView<String, Boolean> urlsCrawled = ConcurrentHashMap.newKeySet();
		Queue<Future<?>> futures = new LinkedBlockingQueue<Future<?>>();
		public KeySetView<String, Boolean> images = ConcurrentHashMap.newKeySet();

		public Crawler(String url, int level) throws IOException {
			this.level = level;
			this.url = url;
			domain = new URL(url).getHost();
		}

		public void crawl() throws Exception {
			futures.add(executor.submit(new Worker(url, level)));
			Future<?> future;
			while ((future = futures.poll()) != null) {
				future.get();
			}
		}

		public class Worker implements Runnable {
			final String url;
			final int level;

			public Worker(String url, int level) {
				this.url = url;
				this.level = level;
			}

			
			public void run() {
				try {
					Document doc = Jsoup.connect(url).get();
					for (Element el : doc.select("img")) {
						images.add(el.absUrl("src"));
					}
					for (Element el : doc.select("a")) {
						String foundUrl = el.absUrl("href");
						try {
							if (domain.equals(new URL(foundUrl).getHost()) && urlsCrawled.add(foundUrl))
								futures.add(executor.submit(new Worker(foundUrl, level - 1)));
						} catch (IOException e) {
						}
					}
				} catch (IOException e) {
				}
			}
		}
	}
}
