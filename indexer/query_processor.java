import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.*;

public class QueryProcessor {
    private MongoCollection<Document> collection;

    public QueryProcessor() {
        this.collection = MongoUtil.getCollection();
    }

    public Map<String, Double> processQuery(String query) {
        Map<String, Double> results = new HashMap<>();
        PorterStemmer stemmer = new PorterStemmer();

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
                word = stemmer.getCurrent();

                // Query MongoDB for the stemmed word
                for (Document doc : collection.find(Filters.eq("w", word))) {
                    String docId = doc.getString("dId");
                    results.put(docId, results.getOrDefault(docId, 0.0) + 1.0);
                }
            }
        }

        return results;
    }

    private Map<String, Double> performPhraseSearch(String phrase) {
        Map<String, Double> results = new HashMap<>();
        List<String> words = Arrays.asList(phrase.split("\\s+"));
        PorterStemmer stemmer = new PorterStemmer();

        // Stem each word in the phrase
        for (int i = 0; i < words.size(); i++) {
            stemmer.setCurrent(words.get(i));
            stemmer.stem();
            words.set(i, stemmer.getCurrent());
        }

        // Query MongoDB for documents containing the exact phrase
        for (Document doc : collection.find(Filters.and(
                Filters.eq("w", words.get(0)),
                Filters.in("pos", words)
        ))) {
            String docId = doc.getString("dId");
            results.put(docId, results.getOrDefault(docId, 0.0) + 1.0);
        }

        return results;
    }
}