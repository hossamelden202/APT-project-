package crawler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VisitedURLs {
    private final Set<String> visited = ConcurrentHashMap.newKeySet();

    public void add(String url) {
        visited.add(url);
    }

    public boolean contains(String url) {
        return visited.contains(url);
    }
}
