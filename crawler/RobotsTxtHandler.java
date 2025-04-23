package crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class RobotsTxtHandler {
    private final Set<String> disallowed = new HashSet<>();

    public boolean isAllowed(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String base = url.getProtocol() + "://" + url.getHost();
            if (disallowed.contains(base)) return false;

            URL robotsURL = new URL(base + "/robots.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(robotsURL.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Disallow:")) {
                    String[] parts = line.split(" ");
                    if (parts.length > 1) {
                        String path = parts[1].trim();
                        if (url.getPath().startsWith(path)) {
                            disallowed.add(base);
                            return false;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return true;
    }
}
