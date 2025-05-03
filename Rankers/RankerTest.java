package Rankers;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import indexer.InvertedIndex;
import indexer.Posting;
import java.util.*;

public class RankerTest {

    public static void main(String[] args) {
        String connection = "mongodb://moayman20001:OFz3mQnyfbBFemPC@ac-nhliflv-shard-00-00.icdzc2j.mongodb.net:27017,ac-nhliflv-shard-00-01.icdzc2j.mongodb.net:27017,ac-nhliflv-shard-00-02.icdzc2j.mongodb.net:27017/?ssl=true&replicaSet=atlas-wrksbv-shard-0&authSource=admin&retryWrites=true&w=majority&appName=Cluster0";
        try (MongoClient mongoClient = MongoClients.create(connection)) {
            MongoDatabase db = mongoClient.getDatabase("indexerdb");

            // Your collections
            MongoCollection<Document> wordCollection = db.getCollection("documents2");
            MongoCollection<Document> metaCollection = db.getCollection("documents3");
            String queryWord = "car"; // Example word from your data
            InvertedIndex index = new InvertedIndex();
            index.index = new HashMap<>();
            List<Posting> postings = new ArrayList<>();
            // Query inverted index for the word
            FindIterable<Document> wordDocs = wordCollection.find(Filters.eq("w", queryWord));
            Document firstDoc = wordDocs.first();
            if (firstDoc == null) {
                System.out.println("No documents found for the word: " + queryWord);
                return;
            }
            for (Document doc : wordDocs) {
                System.out.println(doc.toJson());
            }
            for (Document doc : wordDocs) {
                String docId = doc.getString("dId");
                int fh = doc.getInteger("fh", 0);
                int fb = doc.getInteger("fb", 0);
                String paragraph = doc.getString("p");

                // Create posting
                Posting posting = new Posting(docId,fh,fb);
                postings.add(posting);

                index.docBodies.put(docId, paragraph);

                // Query doc metadata
                Document metaDoc = metaCollection.find(Filters.eq("did", docId)).first();
                if (metaDoc != null) {
                    double pagerank = metaDoc.getDouble("pR");
                    Long docSize = metaDoc.containsKey("length") ? metaDoc.getLong("length") : 1;
                    index.pagerank.put(docId, pagerank);
                    index.doclength.put(docId, docSize);
                }
            }

            if (postings.isEmpty()) {
                System.out.println("No postings found for the word: " + queryWord);
                return;
            }

            // Add to index
            index.index.put(queryWord, postings);
            // Run ranker
            Ranker ranker = new Ranker(index);
            ranker.rankQuery(queryWord);
        }
    }
}