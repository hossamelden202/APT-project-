package Rankers;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import indexer.InvertedIndex;
import indexer.Posting;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RankerTest {

    public static void main(String[] args) {
        String connection ="you wonot need it any more ";

        try (MongoClient mongoClient = MongoClients.create(connection)) {
            MongoDatabase db = mongoClient.getDatabase("indexerdb");

            MongoCollection<Document> wordCollection = db.getCollection("documents2");
            MongoCollection<Document> metaCollection = db.getCollection("documents3");

            String queryString = "car";
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
                    index.index.put(queryWord, postings);
                }
            }

            Ranker ranker = new Ranker(index);
            Map<String, Double> result = ranker.rankQuery(queryWords, queryString);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("htmlpages.txt"))) 
            {
                for (Map.Entry<String, Double> entry : result.entrySet()) {
                    String docId = entry.getKey();
                    double score = entry.getValue();
                    String url = index.url.get(docId);
                    
                    System.out.println("Doc ID: " + docId);
                    System.out.println("URL: " + url);
                    System.out.println("Score: " + score);
                    System.out.println("-----------------------------");

                    writer.write("Doc ID: " + docId);
                    writer.newLine();
                    writer.write("URL: " + url);
                    writer.newLine();
                    writer.write("Score: " + score);
                    writer.newLine();
                    writer.write("-----------------------------");
                    writer.newLine();
                }
                System.out.println("Results written to htmlpages.txt");
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
        }

            
        }
    }
