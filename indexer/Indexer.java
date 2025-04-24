package indexer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class Indexer {
    private static final String INDEX_FILE = "indexer/index.json";
    private static final String STOP_WORDS_FILE = "indexer/stopwords.txt";
    private static final String HTML_FOLDER = "data/crawled_pages";

    private static Set<String> stopWords;
    private static PorterStemmer stemmer = new PorterStemmer();

    public static void main(String[] args) throws Exception {
        stopWords = Utils.loadStopWords(STOP_WORDS_FILE);
        InvertedIndex globalIndex = loadExistingIndex();

        File folder = new File(HTML_FOLDER);
        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".html")) continue;
            System.out.println("Indexing: " + file.getName());

        Path path = file.toPath();
      byte[] bytes = Files.readAllBytes(path);
      String html = new String(bytes, StandardCharsets.UTF_8);
            InvertedIndex docIndex = indexSingleDocument(file.getName(), html);
            globalIndex.merge(docIndex);saveIndex(globalIndex);
        }
          
    for (String fromDoc : globalIndex.outLinks.keySet()) {
        for (String toDoc : globalIndex.outLinks.get(fromDoc)) {
            globalIndex.inLinks
                .computeIfAbsent(toDoc, k -> new ArrayList<>())
                .add(fromDoc);
        }
    }

    computePageRank(globalIndex, 0.85, 20);

    // Save to disk
    saveIndex(globalIndex);

        System.out.println("Indexing complete. Saved to " + INDEX_FILE);
    }

    private static InvertedIndex loadExistingIndex() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(INDEX_FILE), InvertedIndex.class);
        } catch (IOException e) {
            return new InvertedIndex();
        }
    }

    private static void saveIndex(InvertedIndex index) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(INDEX_FILE), index);//make it readable idented
    }

    private static InvertedIndex indexSingleDocument(String docId, String html) {
        Document doc = Jsoup.parse(html);
        String title = doc.title();
        String body = doc.body().text();
        String url=doc.select("link[rel=canonical]").attr("href");
if(url.isEmpty())url="http://"+docId;

List<String> anchors=new ArrayList<>();
doc.select("a").forEach(a->{
String text=a.text();
if(!text.isEmpty())anchors.add(text);
});

String paragraph=doc.select("p").text();
InvertedIndex index=new InvertedIndex();
        List<String> words = Utils.tokenize(title + " " + body);
        index.docBodies.put(docId, body);
        for (String word : words) {
            if (word.isEmpty() || stopWords.contains(word)) continue;

            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();

            String position = Utils.detectPosition(word, title, body);
            index.add(stemmed, docId, position,url,anchors,paragraph);
        }
        // Extract outgoing links
    List<String> outLinks = new ArrayList<>();
    doc.select("a[href]").forEach(a -> {
        String link = a.absUrl("href");
        if (!link.isEmpty()) outLinks.add(link);
    });
    index.outLinks.put(docId, outLinks);
    index.doclength.put(docId, body.trim().split("\\s+").length);
        return index;
    }
    private static void computePageRank(InvertedIndex index, double dampingFactor, int iterations) {
        Map<String, Double> ranks = new HashMap<>();
        Set<String> allDocs = index.outLinks.keySet();
    
        // Initialize
        for (String doc : allDocs) {
            ranks.put(doc, 1.0);
        }
    
        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();
    
            for (String doc : allDocs) {
                double rankSum = 0.0;
                List<String> inLinks = index.inLinks.getOrDefault(doc, new ArrayList<>());
    
                for (String inDoc : inLinks) {
                    int outSize = index.outLinks.getOrDefault(inDoc, new ArrayList<>()).size();
                    if (outSize > 0) {
                        rankSum += ranks.get(inDoc) / outSize;
                    }
                }
    
                double newRank = (1 - dampingFactor) + dampingFactor * rankSum;
                newRanks.put(doc, newRank);
            }
    
            ranks = newRanks;
        }
    
        index.pagerank = ranks;
    }
    
}
