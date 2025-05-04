package indexer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.*;
import java.nio.file.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.mongodb.client.MongoCollection;

import indexer.InvertedIndex;
import indexer.pagerank;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Indexer {
    private static final String INDEX_FILE = "indexer/index.json";
    private static final String STOP_WORDS_FILE = "indexer/stopwords.txt";
    private static final String HTML_FOLDER = "data/run1";
    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static Set<String> stopWords;
    private static final PorterStemmer stemmer = new PorterStemmer();
    private static final ConcurrentLinkedQueue<InvertedIndex> queue = new ConcurrentLinkedQueue<>();
    private static int totalFiles = 0;
    private static final Object progressLock = new Object();

    private static Map<String, Double> pagerankScores;

    public static void main(String[] args) throws Exception {
        stopWords = Utils.loadStopWords(STOP_WORDS_FILE);
        InvertedIndex globalIndex = new InvertedIndex();
        File folder = new File(HTML_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".html"));
        if (files == null) return;
        totalFiles = files.length;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        int[] indexedCount = {0};
        System.out.println("Total files: " + totalFiles);
        System.out.println("Reading PageRank scores...");
        //pagerank p1 =new pagerank();
       // pagerankScores = p1.calc_rank();
        // for(int i=0; i<750; i++)
        // {
        //     String docId = (String) pagerankScores.keySet().toArray()[i];
        //     System.out.println("Doc ID: " + i + ", PageRank Score: " + pagerankScores.get(docId));
        // }
        System.out.println("PageRank scores read successfully.");
        for (File file : files) 
        {
            executor.submit(() -> {
                try {
                    Path path = file.toPath();
                    byte[] bytes = Files.readAllBytes(path);
                    String html = new String(bytes, StandardCharsets.UTF_8);
                    String filenameWithoutExtension = file.getName();
                    int dotIndex = filenameWithoutExtension.lastIndexOf(".");
                    if (dotIndex != -1) {
                        filenameWithoutExtension = filenameWithoutExtension.substring(0, dotIndex);
                    }

                    InvertedIndex singleDocIndex = indexSingleDocument(filenameWithoutExtension, html);
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
                    String safeDocId = posting.documentId != null ? posting.documentId.replace(".", "_") : "AA";
                    org.bson.Document doc = new org.bson.Document()
                        .append("w", word)
                        .append("dId", safeDocId)
                        .append("fh", posting.frequency_head)
                        .append("fb", posting.frequency_body)
                        .append("p", posting.paragraph);
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
            String saveurl= url != null ? url.replace(".", "_") : "AA";
            org.bson.Document doc = new org.bson.Document()
                .append("did", safeDocId)
                .append("length", doclength)
                .append("pR", pagerank)
                .append("url", saveurl);
                
            docs.add(doc);
        } catch (Exception e) {
            System.err.println("Skipping invalid posting due to error: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            if (!docs.isEmpty()) collection.insertMany(docs);
            System.err.println("\n my doc ");
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
        return result.toString();
    }

    private static InvertedIndex indexSingleDocument(String docId, String html) {

        Document doc = Jsoup.parse(html);
        String title = doc.title();
        String body = doc.body().text();
        String url = doc.select("link[rel=canonical]").attr("href");
        if (url.isEmpty()) 
        url = "http://" + docId;
        String paragraph = doc.select("p").text();

        List<String> words = Utils.tokenize(title + " " + body);
        List<String> words2 = Utils.tokenize(title);
        long doclength = body.trim().split("\\s+").length;

        InvertedIndex index = new InvertedIndex();
        String bodyUsed = "";
        String titleUsed = "";

        for (String word : words2) {
            if (word.isEmpty() || stopWords.contains(word)) continue;
            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            titleUsed += stemmer.toString();
        }

        for (String word : words) {
            if (word.isEmpty() || stopWords.contains(word)) continue;
            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();
            bodyUsed += stemmed;
            String para = extractWordsWithKeyword(body, word, 25);
            String position = Utils.detectPosition(word, title, body);
            index.add(stemmed, docId, position, para);
        }
        // for(int i=0;i<750;i++)
        // {
        //     String docId1 = (String) pagerankScores.keySet().toArray()[i];
        //     System.out.println("Doc ID: " + i + ", PageRank Score: " + pagerankScores.get(docId1));
        // }
        double pagerank = pagerankScores.getOrDefault(docId, 0.0);
        System.out.println("Doc ID: " + docId + ", PageRank Score: " + pagerank);
        savedocToMongo(docId, url, titleUsed, bodyUsed, pagerank, doclength);
        return index;
    }

}
