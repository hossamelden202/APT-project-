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
            
            
    
   
            globalIndex.merge(docIndex);
            System.out.println("Merging complete. TO: " + INDEX_FILE);
            
          
        }
          
             saveIndex(globalIndex);

    

    // Save to disk
   

        System.out.println("Indexing complete. " );
    }

    private static InvertedIndex loadExistingIndex() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(INDEX_FILE), InvertedIndex.class);
        } catch (IOException e) {
            return new InvertedIndex();
        }
    }

    private static void saveIndex(InvertedIndex globalIndex) {
        ObjectMapper mapper = new ObjectMapper();
        File f = new File(INDEX_FILE);
        int retries = 3;
        while (retries > 0) {
            try {
               
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, globalIndex);
                return;
            } catch (IOException e) {
                retries--;
                if (retries == 0) break;
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }
    

    private static InvertedIndex indexSingleDocument(String docId, String html) {
        Document doc = Jsoup.parse(html);
        Map<String, List<String>> outLinks = new HashMap<>();
        String docBodies ;     
        Integer doclength ; 
        String title = doc.title();
        String body = doc.body().text();
        String url=doc.select("link[rel=canonical]").attr("href");
         Map<String, List<String>> inLinks = new HashMap<>();

if(url.isEmpty())url="http://"+docId;

List<String> anchors=new ArrayList<>();
doc.select("a").forEach(a->{
String text=a.text();
if(!text.isEmpty())anchors.add(text);
});

String paragraph=doc.select("p").text();
InvertedIndex index=new InvertedIndex();
        List<String> words = Utils.tokenize(title + " " + body);
        docBodies=body;
        //////////////////////
        List<String> outLink = new ArrayList<>();
        doc.select("a[href]").forEach(a -> {
            String link = a.absUrl("href");
            if (!link.isEmpty()) outLink.add(link);
        });
        outLinks.put(docId, outLink);
    
        doclength=body.trim().split("\\s+").length;
        
        ////
        for (String fromDoc : outLinks.keySet()) {
            for (String toDoc : outLinks.get(fromDoc)) {
                inLinks
                    .computeIfAbsent(toDoc, k -> new ArrayList<>())
                    .add(fromDoc);
            }
        }
        ////////
        Map<String , Double>pagerank=computePageRank(outLinks,inLinks, 0.85, 20);
        ///////////////
        for (String word : words) {
            if (word.isEmpty() || stopWords.contains(word)) continue;

            stemmer.reset();
            stemmer.add(word.toCharArray(), word.length());
            stemmer.stem();
            String stemmed = stemmer.toString();

            String position = Utils.detectPosition(word, title, body);
            index.add(stemmed, docId, position,url,anchors,paragraph,docBodies, doclength,pagerank,outLinks,inLinks); 
  
        }
        // Extract outgoing links
            

        return index;
    }
    private static Map<String,Double> computePageRank( Map<String, List<String>> outLinks,Map<String, List<String>> inLinkss ,double dampingFactor, int iterations) {
        Map<String, Double> ranks = new HashMap<>();
        Set<String> allDocs = outLinks.keySet();
    
        // Initialize
        for (String doc : allDocs) {
            ranks.put(doc, 1.0);
        }
    
        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();
    
            for (String doc : allDocs) {
                double rankSum = 0.0;
                List<String> inLinks = inLinkss.getOrDefault(doc, new ArrayList<>());
    
                for (String inDoc : inLinks) {
                    int outSize = outLinks.getOrDefault(inDoc, new ArrayList<>()).size();
                    if (outSize > 0) {
                        rankSum += ranks.get(inDoc) / outSize;
                    }
                }
    
                double newRank = (1 - dampingFactor) + dampingFactor * rankSum;
                newRanks.put(doc, newRank);
            }
    
            ranks = newRanks;
        }
    
        return ranks;
    }
    
}
