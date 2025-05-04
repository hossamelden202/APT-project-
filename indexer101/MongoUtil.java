package indexer;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class MongoUtil {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "indexerdb";
    private static final String COLLECTION_NAME = "documents2";
        private static final String COLLECTION_NAME2 = "documents3";
    private static MongoDatabase database;

    static {
        try {
            MongoClient client = MongoClients.create(CONNECTION_STRING);

            database = client.getDatabase(DATABASE_NAME);
            System.out.println("Successfully connected to MongoDB");
        } catch (Exception e) {
            //System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public static MongoCollection<Document> getCollection() {
        return database.getCollection(COLLECTION_NAME).withWriteConcern(WriteConcern.UNACKNOWLEDGED);
    }
    public static MongoCollection<Document> getCollection2() {
        return database.getCollection(COLLECTION_NAME2).withWriteConcern(WriteConcern.UNACKNOWLEDGED);
    }
}
