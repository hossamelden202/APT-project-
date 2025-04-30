package indexer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.mongodb.client.MongoCollection;


import java.util.concurrent.*;
import java.util.*;
import java.nio.file.*;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Indexer {
    private static final String INDEX_FILE = "indexer/index.json";
    private static final String STOP_WORDS_FILE = "indexer/stopwords.txt";
    private static final String HTML_FOLDER = "data/test";
    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static Set<String> stopWords;
    private static PorterStemmer stemmer = new PorterStemmer();
    private static final ConcurrentLinkedQueue<InvertedIndex> queue = new ConcurrentLinkedQueue<>();
    private static int totalFiles = 0;
    private static final Object progressLock = new Object();

    public static void main(String[] args) throws Exception {
        stopWords = Utils.loadStopWords(STOP_WORDS_FILE);
        InvertedIndex globalIndex = new InvertedIndex();
        File folder = new File(HTML_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".html"));
        totalFiles = files.length;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        int[] indexedCount = {0}; // use array for mutable counter

        for (File file : files) {
            executor.submit(() -> {
                try {
                    Path path = file.toPath();
                    byte[] bytes = Files.readAllBytes(path);
                    String html = new String(bytes, StandardCharsets.UTF_8);
                    
                    InvertedIndex singleDocIndex = indexSingleDocument(file.getName(), html);
                    queue.add(singleDocIndex);

                    synchronized (progressLock) {
                        indexedCount[0]++;
                        double progress = (indexedCount[0] * 100.0) / totalFiles;
                        System.out.printf("\rIndexing Progress: %.2f%%", progress);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to process: " + file.getName());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(100, TimeUnit.HOURS);

        System.out.println("\nMerging all indices...");
        while (!queue.isEmpty()) {
            globalIndex.merge(queue.poll());
        }

        System.out.println("Saving to MongoDB...");
        saveToMongo(globalIndex);

        System.out.println("Indexing and saving complete.");
    }

    private static void saveToMongo(InvertedIndex index) {
        MongoCollection<org.bson.Document> collection = MongoUtil.getCollection();
        List<org.bson.Document> docs = new ArrayList<>();
    
        for (Map.Entry<String, List<Posting>> entry : index.index.entrySet()) {
            String word = entry.getKey();
            if (word == null || word.isEmpty() || stopWords.contains(word)) continue;
    
            List<Posting> postings = entry.getValue();
            if (postings == null) continue;
    
            for (Posting posting : postings) {
                try {
                    if (posting == null) continue;
    
                    String safeDocId = posting.documentId != null ? posting.documentId.replace(".", "_") : "unknown_doc";
                    String safeUrl = posting.url != null ? posting.url.replace(".", "_") : "unknown_url";
    
                    org.bson.Document doc = new org.bson.Document()
                        .append("w", word)
                        .append("dId", safeDocId)
                        .append("fh", posting.frequency_head)
                        .append("fb", posting.frequency_body)
                        
                        .append("pos", posting.positions != null ? posting.positions : Collections.emptyList())
                        .append("url", safeUrl)
                        .append("p", posting.paragraph != null ? posting.paragraph : "")
                        .append("a", posting.anchor != null ? posting.anchor : "")
                        .append("isA", posting.isAnchor)
                        .append("docB", posting.docBodies != null ? posting.docBodies : "")
                        .append("docl", posting.doclength != null ? posting.doclength : 0)
                        .append("pR", posting.pagerank != null ? posting.pagerank : 0.0);
                        // Uncomment if needed:
                        // .append("oL", posting.outLinks != null ? posting.outLinks : Collections.emptyList())
                        // .append("iL", posting.inLinks != null ? posting.inLinks : Collections.emptyList());
    
                    docs.add(doc);
                } catch (Exception e) {
                    System.err.println("Skipping invalid posting due to error: " + e.getMessage());
                    e.printStackTrace(); // Optional: detailed debugging
                }
            }
        }
    
        try {
            if (!docs.isEmpty()) collection.insertMany(docs);
        } catch (Exception e) {
            System.err.println("MongoDB insertion error: " + e.getMessage());
            e.printStackTrace(); // Optional: detailed error
        }
    }
    
    

    private static InvertedIndex indexSingleDocument(String docId, String html) {
        Document doc = Jsoup.parse(html);
        Map<String, List<String>> outLinks = new HashMap<>();
        Map<String, List<String>> inLinks = new HashMap<>();
        String title = doc.title();
        String body = doc.body().text();
        String url = doc.select("link[rel=canonical]").attr("href");
        if (url.isEmpty()) url = "http://" + docId;
        List<String> anchors = new ArrayList<>();
        doc.select("a").forEach(a -> {
            String text = a.text();
            if (!text.isEmpty()) anchors.add(text);
        });
        String paragraph = doc.select("p").text();
        List<String> words = Utils.tokenize(title + " " + body);
        String docBodies = body;
        int doclength = body.trim().split("\\s+").length;

        List<String> outLink = new ArrayList<>();
        doc.select("a[href]").forEach(a -> {
            String link = a.absUrl("href");
            if (!link.isEmpty()) outLink.add(link);
        });
        outLinks.put(docId, outLink);

        for (String fromDoc : outLinks.keySet()) {
            for (String toDoc : outLinks.get(fromDoc)) {
                inLinks.computeIfAbsent(toDoc, k -> new ArrayList<>()).add(fromDoc);
            }
        }

        Map<String, Double> pagerank = computePageRank(outLinks, inLinks, 0.85, 20);

        InvertedIndex index = new InvertedIndex();
        for (String word : words) {
            if (word.isEmpty() || stopWords.contains(word)) continue;

            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();

            String position = Utils.detectPosition(word, title, body);
            index.add(stemmed, docId, position, url, anchors, paragraph, docBodies, doclength, pagerank, outLinks, inLinks);
        }
        return index;
    }

    private static Map<String, Double> computePageRank(Map<String, List<String>> outLinks, Map<String, List<String>> inLinks, double dampingFactor, int iterations) {
        Map<String, Double> ranks = new HashMap<>();
        Set<String> allDocs = outLinks.keySet();
        for (String doc : allDocs) ranks.put(doc, 1.0);

        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();
            for (String doc : allDocs) {
                double rankSum = 0.0;
                for (String inDoc : inLinks.getOrDefault(doc, new ArrayList<>())) {
                    int outDegree = outLinks.getOrDefault(inDoc, new ArrayList<>()).size();
                    if (outDegree > 0) {
                        rankSum += ranks.get(inDoc) / outDegree;
                    }
                }
                newRanks.put(doc, (1 - dampingFactor) + dampingFactor * rankSum);
            }
            ranks = newRanks;
        }
        return ranks;
    }
}
