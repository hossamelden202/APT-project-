package indexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.*;
import java.nio.file.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.client.MongoCollection;
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
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });

        totalFiles = files.length;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        final int[] indexedCount = {0};

        for (final File file : files) {
            executor.submit(new Runnable() {
                public void run() {
                    try {
                        Path path = file.toPath();
                        byte[] bytes = Files.readAllBytes(path);
                        String html = new String(bytes, StandardCharsets.UTF_8);
                        String filename = file.getName();
                        int dotIndex = filename.lastIndexOf(".");
                        if (dotIndex != -1) filename = filename.substring(0, dotIndex);

                        InvertedIndex docIndex = indexSingleDocument(filename, html);
                        queue.add(docIndex);

                        synchronized (progressLock) {
                            indexedCount[0]++;
                            double progress = (indexedCount[0] * 100.0) / totalFiles;
                            System.out.printf("\rIndexing Progress: %.2f%%", progress);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to process: " + file.getName());
                    }
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

                    String safeDocId = posting.documentId != null ? posting.documentId.replace(".", "_") : "AA";
                    org.bson.Document doc = new org.bson.Document()
                        .append("w", word)
                        .append("dId", safeDocId)
                        .append("fh", posting.frequency_head)
                        .append("fb", posting.frequency_body)
                        .append("p", posting.paragraph != null ? posting.paragraph : "");

                    docs.add(doc);
                } catch (Exception e) {
                    System.err.println("Skipping invalid posting due to error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        try {
            if (!docs.isEmpty()) collection.insertMany(docs);
        } catch (Exception e) {
            System.err.println("MongoDB insertion error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void savedocToMongo(String docId, String url, String titleUsed, String bodyUsed, Double pagerank, long doclength) {
        MongoCollection<org.bson.Document> collection = MongoUtil.getCollection2();
        List<org.bson.Document> docs = new ArrayList<>();

        try {
            String safeDocId = docId != null ? docId.replace(".", "_") : "AA";
            String safeTitle = titleUsed != null ? titleUsed.replace(".", "_") : "AA";
            org.bson.Document doc = new org.bson.Document()
                .append("did", safeDocId)
                .append("length", doclength)
                .append("pR", pagerank);
            docs.add(doc);
        } catch (Exception e) {
            System.err.println("Skipping invalid doc due to error: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            if (!docs.isEmpty()) collection.insertMany(docs);
            System.err.println("\nDocument saved.");
        } catch (Exception e) {
            System.err.println("MongoDB insertion error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String extractWordsWithKeyword(String text, String keyword, int maxWords) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (int i = 0; i < words.length; i++) {
            if (words[i].toLowerCase().contains(keyword.toLowerCase())) {
                int start = Math.max(0, i - maxWords / 2);
                int end = Math.min(words.length, i + maxWords / 2);
                for (int j = start; j < end && count < maxWords; j++) {
                    result.append(words[j]).append(" ");
                    count++;
                }
                break;
            }
        }
        return result.toString().trim();
    }

    private static InvertedIndex indexSingleDocument(String docId, String html) {
        Map<String, List<String>> outLinks = new HashMap<>();
        Map<String, List<String>> inLinks = new HashMap<>();

        Document doc = Jsoup.parse(html);
        String title = doc.title();
        String body = doc.body().text();
        String url = doc.select("link[rel=canonical]").attr("href");
        if (url.isEmpty()) url = "http://" + docId;

        String paragraph = doc.select("p").text();
        List<String> words = Utils.tokenize(title + " " + body);
        List<String> titleWords = Utils.tokenize(title);
        long docLength = body.trim().split("\\s+").length;

        List<String> outLink = new ArrayList<>();
        Elements links = doc.select("a[href]");
        for (Element a : links) {
            String link = a.absUrl("href");
            if (!link.isEmpty()) outLink.add(link);
        }
        outLinks.put(docId, outLink);

        for (String fromDoc : outLinks.keySet()) {
            for (String toDoc : outLinks.get(fromDoc)) {
                if (!inLinks.containsKey(toDoc)) inLinks.put(toDoc, new ArrayList<String>());
                inLinks.get(toDoc).add(fromDoc);
            }
        }

        Double pagerank = computePageRank(docId, outLinks, inLinks, 0.85, 20);

        InvertedIndex index = new InvertedIndex();
        StringBuilder titleUsed = new StringBuilder();
        StringBuilder bodyUsed = new StringBuilder();

        for (String word : titleWords) {
            if (word.isEmpty() || stopWords.contains(word)) continue;
            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            titleUsed.append(stemmer.toString());
        }

        for (String word : words) {
            if (word.isEmpty() || stopWords.contains(word)) continue;
            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();
            bodyUsed.append(stemmed);

            String para = extractWordsWithKeyword(body, word, 25);
            String position = Utils.detectPosition(word, title, body);
            index.add(stemmed, docId, position, para);
        }

        savedocToMongo(docId, url, titleUsed.toString(), bodyUsed.toString(), pagerank, docLength);
        return index;
    }

    private static Double computePageRank(String d, Map<String, List<String>> outLinks, Map<String, List<String>> inLinks, double dampingFactor, int iterations) {
        Map<String, Double> ranks = new HashMap<>();
        Set<String> allDocs = outLinks.keySet();
        for (String doc : allDocs) ranks.put(doc, 1.0);

        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();
            for (String doc : allDocs) {
                double rankSum = 0.0;
                List<String> inDocList = inLinks.get(doc);
                if (inDocList != null) {
                    for (String inDoc : inDocList) {
                        int outDegree = outLinks.getOrDefault(inDoc, new ArrayList<String>()).size();
                        if (outDegree > 0) {
                            rankSum += ranks.get(inDoc) / outDegree;
                        }
                    }
                }
                newRanks.put(doc, (1 - dampingFactor) + dampingFactor * rankSum);
            }
            ranks = newRanks;
        }
        return ranks.get(d);
    }
}
