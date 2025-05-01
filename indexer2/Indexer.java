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
import java.util.stream.Collectors;
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
                    String filenameWithoutExtension = file.getName();
                    int dotIndex = filenameWithoutExtension.lastIndexOf(".");
                    if (dotIndex != -1) {
                        filenameWithoutExtension = filenameWithoutExtension.substring(0,dotIndex);
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
// public static void info(String body,String &title,String &url)
// {


// }
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
    private static void savedocToMongo(String docId,String url,String titleUsed,String bodyUsed,Map<String,Double>pagerank,long doclength) {
        MongoCollection<org.bson.Document> collection = MongoUtil.getCollection();
        List<org.bson.Document> docs = new ArrayList<>();
    

                try {
                
    
                    String safeDocId = docId != null ? docId.replace(".", "_") : "AA";
                  // String safeUrl = url != null ? url.replace(".", "_") : "AA";
                    String safeTitle= titleUsed != null ? titleUsed.replace(".", "_") : "AA";
                    org.bson.Document doc = new org.bson.Document()
                        .append("did", safeDocId)

                        // .append("title",safeTitle)
                        // .append("body", bodyUsed)
                        .append("length",doclength)
                        .append("pR", pagerank);
                        // Uncomment if needed:
                        // .append("oL", posting.outLinks != null ? posting.outLinks : Collections.emptyList())
                        // .append("iL", posting.inLinks != null ? posting.inLinks : Collections.emptyList());
    
                    docs.add(doc);
                } catch (Exception e) {
                    System.err.println("Skipping invalid posting due to error: " + e.getMessage());
                    e.printStackTrace(); // Optional: detailed debugging
                }
            
        
    
        try {
            if (!docs.isEmpty()) collection.insertMany(docs);
            System.err.println("/n my doc ");
        } catch (Exception e) {
            System.err.println("MongoDB insertion error: " + e.getMessage());
            e.printStackTrace(); // Optional: detailed error
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
          Map<String, List<String>> outLinks = new HashMap<>();
        Map<String, List<String>> inLinks = new HashMap<>();     
     
    Document doc = Jsoup.parse(html);

    String title = doc.title();
    
    String body = doc.body().text();
    String url = doc.select("link[rel=canonical]").attr("href");
    if (url.isEmpty()) url = "http://" + docId;


  
        String paragraph = doc.select("p").text();
        
        List<String> words = Utils.tokenize(title + " " + body);
        List <String>words2 =Utils.tokenize(title);
        String docBodies = body;
       long doclength = body.trim().split("\\s+").length;

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

        Map<String, Double>  pagerank = computePageRank(outLinks, inLinks, 0.85, 20);

        InvertedIndex index = new InvertedIndex();
        String bodyUsed="";
        String titleUsed="";
        for(String word:words2)
        {
            if (word.isEmpty() || stopWords.contains(word)) continue;

            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();
                titleUsed+=stemmed;

        }
        for (String word : words) {
            if (word.isEmpty() || stopWords.contains(word)) continue;

            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();
                bodyUsed+=stemmed;
              //  System.err.println(paragraph);
               
               String para= extractWordsWithKeyword(body,word,25);
            String position = Utils.detectPosition(word, title, body);
            index.add(stemmed, docId, position, para);
        }
        
        savedocToMongo(docId,url,titleUsed,bodyUsed,pagerank,doclength);
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