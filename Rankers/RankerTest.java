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

            MongoCollection<Document> wordCollection = db.getCollection("documents2");
            MongoCollection<Document> metaCollection = db.getCollection("documents3");

            String queryString = "car engine speed";
            List<String> queryWords = Arrays.asList(queryString.split("\\s+"));

            InvertedIndex index = new InvertedIndex();
            index.index = new HashMap<>();

            for (String queryWord : queryWords) {
                List<Posting> postings = new ArrayList<>();
                FindIterable<Document> wordDocs = wordCollection.find(Filters.eq("w", queryWord));
                for (Document doc : wordDocs) {
                    String docId = doc.getString("dId");
                    int fh = doc.getInteger("fh", 0);
                    int fb = doc.getInteger("fb", 0);
                    String paragraph = doc.getString("p");

                    postings.add(new Posting(docId, fh, fb));
                    index.docBodies.putIfAbsent(docId, paragraph);

                    Document metaDoc = metaCollection.find(Filters.eq("did", docId)).first();
                    if (metaDoc != null) {
                        double pagerank = metaDoc.getDouble("pR");
                        long docSize = metaDoc.containsKey("length") ? metaDoc.getLong("length") : 1;
                        index.pagerank.put(docId, pagerank);
                        index.doclength.put(docId, docSize);
                    }
                }
                if (!postings.isEmpty()) {
                    index.index.put(queryWord, postings);
                }
            }
            Ranker ranker = new Ranker(index);
            ranker.rankQuery(queryWords,queryString);
        }
    }
}