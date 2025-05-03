package indexer;
import java.util.*;
public class Posting {
    
    // public String word;
    public String documentId;
    public int frequency_body;
    public int frequency_head;
    // public List<String> positions;
    public String paragraph;
    ///////////////////////////////////
    public  String docBodies;      // full text of documents
    public  Integer doclength;     // number of words per doc
public Map<String, List<String>> outLinks;
public Map<String, List<String>> inLinks;

    public Posting(String documentId) {
        this.documentId = documentId;
        this.frequency_body = 0;
        this.frequency_head = 0;
        
    }
        public Posting(String documentId, int frequency_head,int frequency_body) {
            this.documentId = documentId;
            this.frequency_head = frequency_head;
            this.frequency_body = frequency_body;
        }
        
    }       
        
