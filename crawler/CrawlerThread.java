package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

public class CrawlerThread implements Runnable {
    private final URLFrontier frontier;
    private final VisitedURLs visited;
    private final RobotsTxtHandler robots;
    private final PageCounter counter;

    public CrawlerThread(URLFrontier frontier, VisitedURLs visited,
                         RobotsTxtHandler robots, PageCounter counter) {
        this.frontier = frontier;
        this.visited = visited;
        this.robots = robots;
        this.counter = counter;
    }

    @Override
    public void run() {
        while (!counter.isLimitReached()) {
            String url = frontier.getNext();
            if (url == null) break;

            if (visited.contains(url)) continue;

            try {
                URL normalized = new URL(url);
                String host = normalized.getHost();

                if (!robots.isAllowed(url)) continue;

                Document doc = Jsoup.connect(url).get();
                if (!counter.incrementIfBelowLimit()) break;

                visited.add(url);
                saveHTML(url, doc.html());

                Elements links = doc.select("a[href]");
                for (var link : links) {
                    String absUrl = link.absUrl("href");
                    if ((absUrl.endsWith(".html") || absUrl.endsWith(".htm")) && !visited.contains(absUrl)) {
                        frontier.add(absUrl);
                    }
                }

            } catch (Exception e) {
                System.err.println("Error crawling: " + url + " -> " + e.getMessage());
            }
        }
    }

    private void saveHTML(String url, String html) throws Exception {
        String safeName = url.replaceAll("[^a-zA-Z0-9]", "_");
        File file = new File("data/crawled_pages/" + safeName + ".html");
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(html);
        }
    }
}
