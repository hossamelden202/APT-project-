package indexer;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Filters;
import java.util.*;
import java.util.stream.Collectors;
import Rankers.Ranker;

public class QueryProcessor {
    private final PorterStemmer stemmer;
    private final InvertedIndex index;

    public QueryProcessor() {
        this.stemmer = new PorterStemmer();
        this.index = new InvertedIndex();
    }

    public List<Map.Entry<String, Double>> processQuery(String query) {
        List<Map.Entry<String, Double>> finalResults = new ArrayList<>();

        try {
            if (query == null || query.trim().isEmpty()) {
                System.out.println("Query is empty or null.");
                return finalResults;
            }

            // Preprocess query: lowercase + stemming
            query = query.toLowerCase();
            List<String> processedWords = new ArrayList<>();
            String[] words = query.split("\\s+");
            for (String word : words) {
                stemmer.reset();
                for (char c : word.toCharArray()) {
                    stemmer.add(c);
                }
                stemmer.stem();
                processedWords.add(stemmer.toString());
            }

            // Connect to collections
            MongoCollection<Document> wordCollection = MongoUtil.getCollection("documents2");
            MongoCollection<Document> metaCollection = MongoUtil.getCollection("documents3");

            // Build the index
            index.index = new HashMap<>();

            for (String stemmedWord : processedWords) {
                List<Posting> postings = new ArrayList<>();
                FindIterable<Document> wordDocs = wordCollection.find(Filters.eq("w", stemmedWord));

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
                        double pagerank = metaDoc.getDouble("pR");
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
}