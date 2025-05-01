package crawler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebCrawler {
    private final URLFrontier frontier;
    private final VisitedURLs visited;
    private final int numThreads;
    private final int maxPages;

    public WebCrawler(String[] seeds, int numThreads, int maxPages) {
        this.frontier = new URLFrontier();
        this.visited = new VisitedURLs();
        this.numThreads = numThreads;
        this.maxPages = maxPages;

        for (String seed : seeds) {
            frontier.add(seed);
        }
    }

    public void startCrawling() {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        RobotsTxtHandler robots = new RobotsTxtHandler();
        PageCounter counter = new PageCounter(maxPages);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(new CrawlerThread(frontier, visited, robots, counter));
        }

        executor.shutdown();
    }

    public static void main(String[] args) {
        String[] seeds = {
            "https://example.com",
            "https://www.w3schools.com/",
            "https://info.cern.ch/",
            "https://en.wikipedia.org/wiki/Web_crawler",
            "https://www.mozilla.org/en-US/",
            "https://developer.mozilla.org/en-US/docs/Web",
            "https://www.geeksforgeeks.org/",
            "https://www.javatpoint.com/",
            "https://www.tutorialspoint.com/index.htm",
            "https://openai.com/",
            "https://www.gnu.org/",
            "https://www.khanacademy.org/",
            "https://www.codecademy.com/",
            "https://en.wikibooks.org/wiki/Main_Page",
            "https://www.britannica.com/",
            "https://www.python.org/",
            "https://www.java.com/en/",
            "https://www.stackoverflow.com/",
            "https://www.oracle.com/java/",
            "https://cs50.harvard.edu/"
        };
        
        int threadCount = 5;
        int maxPages = 100;

        new WebCrawler(seeds, threadCount, maxPages).startCrawling();
    }
}
