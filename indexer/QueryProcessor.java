import indexer.MongoUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.tartarus.snowball.ext.PorterStemmer;
import Rankers.Ranker;
import indexer.InvertedIndex;
import java.util.*;
import java.util.stream.Collectors;

public class QueryProcessor {
    private final MongoCollection<Document> collection; // initialize mongo collection
    private final PorterStemmer stemmer;  // steamming words for root form
    private final InvertedIndex index; // inverted index to store the words and their positions


    // constructor
    public QueryProcessor(InvertedIndex index) {
        this.collection = MongoUtil.getCollection();
        this.stemmer = new PorterStemmer();
        this.index = index;
    }
  // Processes a user query and retrieves ranked results
    public List<Map.Entry<String, Double>> processQuery(String query) {
        try {
            // Check for empty or null query
            if (query == null || query.trim().isEmpty()) {
                System.out.println("Query is empty or null.");
                return new ArrayList<>();
            }

            // Preprocess query
            query = query.toLowerCase();
            List<String> processedWords = new ArrayList<>();
            String[] words = query.split("\\s+");
            for (String word : words) {
                stemmer.setCurrent(word);
                stemmer.stem();
                processedWords.add(stemmer.getCurrent());
            }

            // Use Ranker to get ranked results
            Ranker ranker = new Ranker(index);
            String phrase = (query.startsWith("\"") && query.endsWith("\"")) ? query.substring(1, query.length() - 1) : "";
            ranker.rankQuery(processedWords, phrase);

            // The Ranker should return a sorted list of document IDs and scores
            // For now, assume Ranker returns a Map<String, Double>
            Map<String, Double> results = ranker.getResults();

            // Sort results by score descending
            return results.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());   // returns a sorted list of document IDs and scores
        } catch (Exception e) {
            System.err.println("Error processing query: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Performs a phrase search in the MongoDB collection (search for specific phrase )
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