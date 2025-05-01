package indexer;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class MongoUtil {
    private static final String CONNECTION_STRING = "mongodb://hossammohamed04:OFz3mQnyfbBFemPC@ac-nhliflv-shard-00-00.icdzc2j.mongodb.net:27017,ac-nhliflv-shard-00-01.icdzc2j.mongodb.net:27017,ac-nhliflv-shard-00-02.icdzc2j.mongodb.net:27017/?ssl=true&replicaSet=atlas-wrksbv-shard-0&authSource=admin&retryWrites=true&w=majority&appName=Cluster0";
    private static final String DATABASE_NAME = "indexerdb";
    private static final String COLLECTION_NAME = "documents2";
    private static MongoDatabase database;

    static {
        try {
            MongoClient client = MongoClients.create(CONNECTION_STRING);
            database = client.getDatabase(DATABASE_NAME);
            System.out.println("Successfully connected to MongoDB");
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static MongoCollection<Document> getCollection() {
        return database.getCollection(COLLECTION_NAME).withWriteConcern(WriteConcern.UNACKNOWLEDGED);
    }
}
