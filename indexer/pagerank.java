package indexer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class pagerank {
    
    private static Map<String, List<String>> outLinks = new HashMap<>();
    private static Map<String, List<String>> inLinks = new HashMap<>();
    private static final String HTML_FOLDER = "data_final8"; // Path to the folder containing HTML files
    private static int totalFiles = 0;

    public static void main(String[] args) {
        File folder = new File(HTML_FOLDER);
        File[] txtFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        if (txtFiles == null) return;
    
        totalFiles = txtFiles.length;
        System.out.println("Total .txt files: " + totalFiles);
    
        Map<String, Double> pagerankScores = readFile(txtFiles);
    
        System.out.println("Outlinks: " + outLinks.size());
        System.out.println("Inlinks: " + inLinks.size());
        System.out.println("Computing PageRank scores...");
    
        try (PrintWriter writer = new PrintWriter("pagerank_scores_data_final8.csv")) {
            writer.println("DocID,PageRank");
            for (Map.Entry<String, Double> entry : pagerankScores.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
            System.out.println("PageRank scores successfully written to pagerank_scores.csv");
        } catch (IOException e) {
            System.err.println("Error writing to pagerank_scores_total.csv: " + e.getMessage());
        }
    }
    

    private static Map<String, Double> computePageRanks(
        Map<String, List<String>> outLinks,
        Map<String, List<String>> inLinks,
        double dampingFactor,
        int iterations
    ) {
        Set<String> allDocs = new HashSet<>();
        allDocs.addAll(outLinks.keySet());
        allDocs.addAll(inLinks.keySet());

        int N = allDocs.size();
        Map<String, Double> ranks = new HashMap<>();
        for (String doc : allDocs) {
            ranks.put(doc, 1.0 / N);  // Initialize to 1/N
        }

        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();
            double danglingSum = 0.0;

            for (String doc : allDocs) {
                if (outLinks.getOrDefault(doc, new ArrayList<>()).isEmpty()) {
                    danglingSum += ranks.getOrDefault(doc, 0.0);
                }
            }

            for (String doc : allDocs) {
                double rankSum = 0.0;
                for (String inDoc : inLinks.getOrDefault(doc, new ArrayList<>())) {
                    int outDegree = outLinks.getOrDefault(inDoc, new ArrayList<>()).size();
                    if (outDegree > 0) {
                        rankSum += ranks.getOrDefault(inDoc, 0.0) / outDegree;
                    }
                }
                double danglingContribution = dampingFactor * danglingSum / N;
                double newRank = (1 - dampingFactor) / N + dampingFactor * rankSum + danglingContribution;
                newRanks.put(doc, newRank);
            }

            ranks = newRanks;
        }

        return ranks;
    }

    private static Map<String, Double> readFile(File[] txtFiles) {
        if (txtFiles == null || txtFiles.length == 0) {
            System.out.println("No .txt files found in the directory.");
            return null;
        }
    
        Set<String> validDocIds = new HashSet<>();
        int downloadCount = 0; // Counter for downloaded pages
    
        for (File txtFile : txtFiles) {
            try {
                List<String> lines = Files.readAllLines(txtFile.toPath(), StandardCharsets.UTF_8);
                if (lines.isEmpty()) continue;
    
                String rawLink = lines.get(0).trim(); // URL from the .txt file
                String docId = normalizeLink(rawLink);
                File htmlFile = new File(txtFile.getParent(), docId + ".html");
    
                String html;
    
                if (!htmlFile.exists()) {
                    System.out.println("Downloading HTML for: " + rawLink);
                    try {
                        Document downloaded = Jsoup.connect(rawLink).get();
                        html = downloaded.html();
    
                        // Save downloaded HTML
                        Files.writeString(htmlFile.toPath(), html, StandardCharsets.UTF_8);
                        downloadCount++; // Increment download counter
                    } catch (IOException e) {
                        System.err.println("Failed to download: " + rawLink + " — " + e.getMessage());
                        continue;
                    }
                } else {
                    html = Files.readString(htmlFile.toPath(), StandardCharsets.UTF_8);
                }
    
                validDocIds.add(docId);
                Document doc = Jsoup.parse(html);
    
                List<String> outLinkList = new ArrayList<>();
                for (org.jsoup.nodes.Element a : doc.select("a[href]")) {
                    String href = a.attr("href").trim();
                    String linkedDocId = normalizeLink(href.replaceAll("\\.html$", "")); // Normalize and strip .html
    
                    outLinkList.add(linkedDocId);
                    inLinks.computeIfAbsent(linkedDocId, k -> new ArrayList<>()).add(docId);
                }
    
                outLinks.put(docId, outLinkList);
    
            } catch (IOException e) {
                System.err.println("Error reading " + txtFile.getName() + ": " + e.getMessage());
            }
        }
    
        System.out.println("Downloaded pages: " + downloadCount);
        System.out.println("Normalized Document IDs:");
        validDocIds.forEach(System.out::println);
    
        Map<String, Double> pagerankScores = computePageRanks(outLinks, inLinks, 0.85, 20);
        pagerankScores.keySet().retainAll(validDocIds); // Only keep valid pages
    
        // Re-normalize PageRank to sum to 1 over only valid pages
        double total = pagerankScores.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> normalizedRanks = new HashMap<>();
        for (Map.Entry<String, Double> entry : pagerankScores.entrySet()) {
            double normalized = entry.getValue() / total;
            // Optionally scale for readability:
            // normalized *= 100000;
            normalizedRanks.put(entry.getKey(), normalized);
        }
    
        return normalizedRanks;
    }
    
    

    private static String normalizeLink(String link) {
        // Remove "http://" or "https://"
        link = link.replaceAll("https?://", "");

        // Handle cases like "http___" and replace "___" with "_"
        link = link.replaceAll("^_+", ""); // Remove any leading underscores (if there's any)
        link = link.replaceAll("___", "_"); // Replace '___' with a single underscore

        // Remove file extensions like .html or any other unwanted characters
        link = link.replaceAll("\\.html$", "");

        // Replace any remaining non-alphanumeric characters with "_"
        link = link.replaceAll("[^a-zA-Z0-9_]", "_");

        return link;
    }
}
