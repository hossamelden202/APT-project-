import indexer.MongoUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.*;

public class QueryProcessor {
    private final MongoCollection<Document> collection;

    public QueryProcessor() {
        this.collection = MongoUtil.getCollection();
    }

    public Map<String, Double> processQuery(String query) {
        Map<String, Double> results = new HashMap<>();
        PorterStemmer stemmer = new PorterStemmer();

        try {
            // Check for empty or null query
            if (query == null || query.trim().isEmpty()) {
                System.out.println("Query is empty or null.");
                return results;
            }

            // Preprocess query
            query = query.toLowerCase();

            // Check if the query contains quotation marks
            if (query.startsWith("\"") && query.endsWith("\"")) {
                String phrase = query.substring(1, query.length() - 1);
                results = performPhraseSearch(phrase);
            } else {
                String[] words = query.split("\\s+");
                for (String word : words) {
                    // Stem the word
                    stemmer.setCurrent(word);
                    stemmer.stem();
                    String stemmedWord = stemmer.getCurrent();

                    // Query MongoDB for the stemmed word
                    for (Document doc : collection.find(Filters.eq("w", stemmedWord))) {
                        String docId = doc.getString("dId");
                        double score = doc.getString("w").equals(word) ? 1.0 : 0.5; // Exact match gets higher score
                        results.put(docId, results.getOrDefault(docId, 0.0) + score);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing query: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private Map<String, Double> performPhraseSearch(String phrase) {
        Map<String, Double> results = new HashMap<>();
        List<String> words = Arrays.asList(phrase.split("\\s+"));
        PorterStemmer stemmer = new PorterStemmer();

        try {
            // Stem each word in the phrase
            List<String> stemmedWords = new ArrayList<>();
            for (String word : words) {
                stemmer.setCurrent(word);
                stemmer.stem();
                stemmedWords.add(stemmer.getCurrent());
            }

            // Query MongoDB for documents containing the first word
            for (Document doc : collection.find(Filters.eq("w", stemmedWords.get(0)))) {
                List<Integer> positions = doc.getList("pos", Integer.class);
                String docId = doc.getString("dId");

                // Check if the phrase exists in the correct order
                if (positions != null && isPhraseInOrder(stemmedWords, docId, positions)) {
                    results.put(docId, results.getOrDefault(docId, 0.0) + 1.0);
                }
            }
        } catch (Exception e) {
            System.err.println("Error performing phrase search: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private boolean isPhraseInOrder(List<String> words, String docId, List<Integer> positions) {
        try {
            // Fetch positions for all words in the phrase in a single query
            Map<String, List<Integer>> wordPositions = new HashMap<>();
            List<Document> docs = collection.find(Filters.and(Filters.in("w", words), Filters.eq("dId", docId))).into(new ArrayList<>());

            for (Document doc : docs) {
                String word = doc.getString("w");
                List<Integer> pos = doc.getList("pos", Integer.class);
                wordPositions.put(word, pos);
            }

            // Check if the words appear in order
            for (int startPos : positions) {
                boolean inOrder = true;
                int currentPos = startPos;

                for (int i = 1; i < words.size(); i++) {
                    List<Integer> nextWordPositions = wordPositions.get(words.get(i));
                    if (nextWordPositions == null || !nextWordPositions.contains(++currentPos)) {
                        inOrder = false;
                        break;
                    }
                }

                if (inOrder) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking phrase order: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}