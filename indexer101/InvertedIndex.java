package indexer;

import java.util.*;

public class InvertedIndex {
    public Map<String, List<Posting>> index;
    public Map<String, List<doc>> index2;
    public String urls;
    public String paragraphs;
    public Map<String, List<String>> anchors = new HashMap<>();
    //************************************************************************* */
//     public String docBodies ;      // full text of documents
//     public  Integer doclength ;     // number of words per doc
//     public Map<String, Double> pagerank = new HashMap<>();       // PageRank values
// public Map<String, List<String>> outLinks = new HashMap<>();
// public Map<String, List<String>> inLinks = new HashMap<>();

    public InvertedIndex() {
        index = new HashMap<>();
        index2 = new HashMap<>();
    }

    public void add(String word, String docId, String position,String paragrph){
        index.putIfAbsent(word, new ArrayList<>());//unique adding (prevent overwritting)
        // System.out.println("mine mine :"+ docId);
        List<Posting> postings = index.get(word);
      //(take null or value) 
       Optional<Posting> existing = postings.stream()
                .filter(p -> p.documentId.equals(docId)).findFirst();
//equlavent not equal null
        if (existing.isPresent()) {
            if(position=="head")
            existing.get().frequency_head++;
            else if(position=="body")
            existing.get().frequency_body++;

existing.get().paragraph=paragrph;




        } else {
            Posting p = new Posting(docId);
            p.frequency_body = 1;
            p.frequency_head= 1;
            
            postings.add(p);
            
            p.paragraph = paragrph;


        }
  
    }


    public void merge(InvertedIndex other) {
        for (String word : other.index.keySet()) {
            this.index.putIfAbsent(word, new ArrayList<>());
            this.index.get(word).addAll(other.index.get(word));

        }

    }
}
