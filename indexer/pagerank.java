package indexer;

import java.io.File;
import java.io.IOException;
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
    private static final String HTML_FOLDER = "data/total";
    private static int totalFiles = 0;
   public  Map<String, Double> calc_rank()
   {
    File folder = new File(HTML_FOLDER);
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".html"));
    if (files == null) return null ;
    totalFiles = files.length;
    System.out.println("Total files: " + totalFiles);
    Map<String, Double> pagerankScores=read_file(files);
    System.out.println("Outlinks: " + outLinks.size());
    System.out.println("Inlinks: " + inLinks.size());
    System.out.println("Computing PageRank scores...");
    // for(int i = 0; i < totalFiles; i++) {
    //     String docId = (String) pagerankScores.keySet().toArray()[i];
    //     System.out.println("Doc ID: " + docId + ", PageRank: " + pagerankScores.get(docId));
    //     System.out.println("Outlinks: " + outLinks.get(docId));
    //     System.out.println("Inlinks: " + inLinks.get(docId));
    // }
    System.out.println("PageRank scores computed successfully.");
    return pagerankScores;
    // for(int i=0; i<5000; i++){
    //     String docId = (String) pagerankScores.keySet().toArray()[i];
    //     System.out.println("Doc ID: " + docId + ", PageRank Score: " + pagerankScores.get(docId));
    // }
}

private  static Map<String, Double> computePageRanks(
        Map<String, List<String>> outLinks,
        Map<String, List<String>> inLinks,
        double dampingFactor,
        int iterations
    ) 
    {
        Set<String> allDocs = new HashSet<>();
        allDocs.addAll(outLinks.keySet());
        allDocs.addAll(inLinks.keySet());
    
        Map<String, Double> ranks = new HashMap<>();
        for (String doc : allDocs) {
            ranks.put(doc, 1.0);
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
                double danglingContribution = dampingFactor * danglingSum / allDocs.size();
                newRanks.put(doc, (1 - dampingFactor) + dampingFactor * rankSum + danglingContribution);
            }
            ranks = newRanks;
        }
        
        return ranks;
    }
private static Map<String, Double> read_file(File[] files) {
    if (files == null || files.length == 0) {
        System.out.println("No files found in the directory.");
        return null;
    }

    // 1. Collect valid document IDs from file names
    Set<String> validDocIds = Arrays.stream(files)
        .map(f -> f.getName().replaceAll("\\.html$", ""))
        .collect(Collectors.toSet());

    for (File file : files) {
        try {
            Path path = file.toPath();
            byte[] bytes = Files.readAllBytes(path);
            String html = new String(bytes, StandardCharsets.UTF_8);
            String docId = file.getName().replaceAll("\\.html$", "");
            Document doc = Jsoup.parse(html);

            List<String> outLinkList = new ArrayList<>();

            for (org.jsoup.nodes.Element a : doc.select("a[href]")) {
                String link = a.attr("href").trim();

                // Handle local links only
                if (link.endsWith(".html")) {
                    String linkedDocId = link.replaceAll("\\.html$", "");

                    if (validDocIds.contains(linkedDocId)) {
                        outLinkList.add(linkedDocId);
                        inLinks.computeIfAbsent(linkedDocId, k -> new ArrayList<>()).add(docId);
                    }
                }
            }
            outLinks.put(docId, outLinkList);
        } catch (IOException e) {
            System.err.println("Failed to extract links from: " + file.getName());
        }
    }
    Map<String, Double> pagerankScores = computePageRanks(outLinks, inLinks, 0.85, 20);
    // After PageRank calculation is done:
    pagerankScores.keySet().retainAll(validDocIds);
    return pagerankScores;

}

}
