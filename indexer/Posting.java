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
    public Posting(String documentId) {
        this.documentId = documentId;
        this.frequency = 0;
        this.positions = new ArrayList<>();
        this.anchor=new ArrayList<>();
        
    }
}
