package indexer;

import java.util.*;


public class Posting {
    public String documentId;
    public int frequency_body;
    public int frequency_head;
    public List<String> positions;
    public  String url;
    public String paragraph;
    public List<String>anchor;
    public boolean isAnchor; 
    ///////////////////////////////////
    public  String docBodies;      // full text of documents
    public  Integer doclength;     // number of words per doc
    public Map<String, Double> pagerank;       // PageRank values
public Map<String, List<String>> outLinks;
public Map<String, List<String>> inLinks;
    public Posting(String documentId) {
        this.documentId = documentId;
        this.frequency_body = 0;
        this.frequency_head = 0;
        this.positions = new ArrayList<>();
        this.anchor=new ArrayList<>();
        
    }
        // Constructor with frequency and positions
        // public Posting(String documentId, int frequency, List<String> positions) {
        //     this.documentId = documentId;
        //     this.frequency = frequency;
        //     this.positions = positions;
        //     this.anchor = new ArrayList<>();
        // }
    
        // Constructor with frequency, positions, and isAnchor
        public Posting(String documentId, int frequency_head,int frequency_body, List<String> positions, boolean isAnchor) {
            this.documentId = documentId;
            this.frequency_head = frequency_head;
            this.frequency_body = frequency_body;
            this.positions = positions;
            this.anchor = new ArrayList<>();
            this.isAnchor = isAnchor;
        }
    }

