package indexer;

import java.util.*;


public class Posting {
    public String documentId;
    public int frequency;
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
        this.frequency = 0;
        this.positions = new ArrayList<>();
        this.anchor=new ArrayList<>();
        
    }
        // Constructor with frequency and positions
        public Posting(String documentId, int frequency, List<String> positions) {
            this.documentId = documentId;
            this.frequency = frequency;
            this.positions = positions;
            this.anchor = new ArrayList<>();
        }
    
        // Constructor with frequency, positions, and isAnchor
        public Posting(String documentId, int frequency, List<String> positions, boolean isAnchor) {
            this.documentId = documentId;
            this.frequency = frequency;
            this.positions = positions;
            this.anchor = new ArrayList<>();
            this.isAnchor = isAnchor;
        }
    }

