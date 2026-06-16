package indexer;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Filters;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import Rankers.Ranker;

public class QueryProcessor {
    private final PorterStemmer stemmer;
    private final InvertedIndex index;
    private final Set<String> stopWords;

    public QueryProcessor() {
        this.stemmer = new PorterStemmer();
        this.index = new InvertedIndex();
        this.stopWords = loadStopWords("indexer/stopwords.txt"); // Adjust path if needed
    }

    private Set<String> loadStopWords(String filePath) {
        Set<String> stopWords = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] words = line.split(",");
                for (String word : words) {
                    stopWords.add(word.trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading stop words: " + e.getMessage());
        }
        return stopWords;
    }

    public List<Map.Entry<String, Double>> processQuery(String query) {
        List<Map.Entry<String, Double>> finalResults = new ArrayList<>();

        try {
            if (query == null || query.trim().isEmpty()) {
                System.out.println("Query is empty or null.");
                return finalResults;
            }

            // Preprocess query: lowercase + stemming + stop word filtering
            query = query.toLowerCase();
            List<String> processedWords = new ArrayList<>();
            String[] words = query.split("\\s+");
            for (String word : words) {
                if (stopWords.contains(word)) continue; // Skip stop words
                stemmer.reset();
                for (char c : word.toCharArray()) {
                    stemmer.add(c);
                }
                stemmer.stem();
                processedWords.add(stemmer.toString());
            }

            // Connect to collections
            MongoCollection<Document> wordCollection = MongoUtil.getCollection(); // stores word data doucement ids , frequencies , paragraghes
            MongoCollection<Document> metaCollection = MongoUtil.getCollection2(); // stores metadata page rank document lenghth urls

            // Build the index
            index.index = new HashMap<>();

            for (String stemmedWord : processedWords) {
                List<Posting> postings = new ArrayList<>();
                FindIterable<Document> wordDocs = wordCollection.find(Filters.eq("w", stemmedWord)).limit(400);

                for (Document doc : wordDocs) {
                    String docId = doc.getString("dId");
                    int fh = doc.getInteger("fh", 0);
                    int fb = doc.getInteger("fb", 0);
                    String paragraph = doc.getString("p");

                    postings.add(new Posting(docId, fh, fb));
                    index.docBodies.putIfAbsent(docId, paragraph);

                    // Get metadata for the document
                    Document metaDoc = metaCollection.find(Filters.eq("did", docId)).first();
                    if (metaDoc != null) {
                        Object prValue = metaDoc.get("pR");
                        double pagerank = 0.0;
                        if (prValue instanceof Double) {
                            pagerank = (Double) prValue;
                        } else if (prValue instanceof String) {
                            try {
                                pagerank = Double.parseDouble((String) prValue);
                            } catch (NumberFormatException e) {
                                pagerank = 0.0; // fallback if string is not a valid double
                            }
                        } else if (prValue instanceof Integer) {
                            pagerank = ((Integer) prValue).doubleValue();
                        }

                        long docSize = metaDoc.containsKey("length") ? metaDoc.getLong("length") : 1;
                        index.pagerank.put(docId, pagerank);
                        index.doclength.put(docId, docSize);
                        index.url.put(docId, metaDoc.getString("url"));
                    }
                }

                if (!postings.isEmpty()) {
                    index.index.put(stemmedWord, postings);
                }
            }

            // Rank the query
            String phrase = (query.startsWith("\"") && query.endsWith("\"")) ?
                    query.substring(1, query.length() - 1) : "";

            Ranker ranker = new Ranker(index);
            Map<String, Double> resultMap = ranker.rankQuery(processedWords, phrase);
            for (Map.Entry<String, Double> entry : resultMap.entrySet()) {
                String docId = entry.getKey();
                double score = entry.getValue();
                String url = index.url.get(docId);

                System.out.println("Doc ID: " + docId);
                System.out.println("URL: " + url);
                System.out.println("Score: " + score);
                System.out.println("-----------------------------");
            }
            // Sort results by score in descending order
            finalResults = resultMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error processing query: " + e.getMessage());
            e.printStackTrace();
        }

        return finalResults;
    }

public static void main(String[] args) {
    QueryProcessor processor = new QueryProcessor();
    processor.processQuery(args.length > 0 ? args[0] : "");
}
}