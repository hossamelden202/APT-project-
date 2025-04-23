package crawler;

import java.util.concurrent.ConcurrentLinkedQueue;

public class URLFrontier {
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public void add(String url) {
        queue.add(url);
    }

    public String getNext() {
        return queue.poll();
    }
}
