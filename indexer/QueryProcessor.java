// import indexer.MongoUtil;
// import com.mongodb.client.MongoCollection;
// import com.mongodb.client.model.Filters;
// import org.bson.Document;
// import org.tartarus.snowball.ext.PorterStemmer;

// import java.util.*;

// public class QueryProcessor {
//     private MongoCollection<Document> collection;

//     public QueryProcessor() {
//         this.collection = MongoUtil.getCollection();
//     }

//     public Map<String, Double> processQuery(String query) {
//         Map<String, Double> results = new HashMap<>();
//         PorterStemmer stemmer = new PorterStemmer();

//         try {
//             // Check for empty or null query
//             if (query == null || query.trim().isEmpty()) {
//                 System.out.println("Query is empty or null.");
//                 return results;
//             }

//             // Preprocess query
//             query = query.toLowerCase();

//             // Check if the query contains quotation marks
//             if (query.startsWith("\"") && query.endsWith("\"")) {
//                 String phrase = query.substring(1, query.length() - 1);
//                 results = performPhraseSearch(phrase);
//             } else {
//                 String[] words = query.split("\\s+");
//                 for (String word : words) {
//                     // Stem the word
//                     stemmer.setCurrent(word);
//                     stemmer.stem();
//                     word = stemmer.getCurrent();

//                     // Query MongoDB for the stemmed word
//                     for (Document doc : collection.find(Filters.eq("w", word))) {
//                         String docId = doc.getString("dId");
//                         results.put(docId, results.getOrDefault(docId, 0.0) + 1.0);
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("Error processing query: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return results;
//     }

//     private Map<String, Double> performPhraseSearch(String phrase) {
//         Map<String, Double> results = new HashMap<>();
//         List<String> words = Arrays.asList(phrase.split("\\s+"));
//         PorterStemmer stemmer = new PorterStemmer();

//         try {
//             // Stem each word in the phrase
//             for (int i = 0; i < words.size(); i++) {
//                 stemmer.setCurrent(words.get(i));
//                 stemmer.stem();
//                 words.set(i, stemmer.getCurrent());
//             }

//             // Query MongoDB for documents containing the exact phrase
//             for (Document doc : collection.find(Filters.eq("w", words.get(0)))) {
//                 List<Integer> positions = doc.getList("pos", Integer.class);
//                 String docId = doc.getString("dId");

//                 // Check if the phrase exists in the correct order
//                 if (positions != null && isPhraseInOrder(words, positions)) {
//                     results.put(docId, results.getOrDefault(docId, 0.0) + 1.0);
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("Error performing phrase search: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return results;
//     }

//     private boolean isPhraseInOrder(List<String> words, List<Integer> positions) {
//         for (int startPos : positions) {
//             boolean inOrder = true;
//             int currentPos = startPos;

//             for (int i = 1; i < words.size(); i++) {
//                 String nextWord = words.get(i);
//                 List<Integer> nextWordPositions = collection.find(Filters.eq("w", nextWord))
//                         .map(doc -> doc.getList("pos", Integer.class))
//                         .first();

//                 if (nextWordPositions == null || !nextWordPositions.contains(++currentPos)) {
//                     inOrder = false;
//                     break;
//                 }
//             }

//             if (inOrder) {
//                 return true;
//             }
//         }

//         return false;
//     }
// }  