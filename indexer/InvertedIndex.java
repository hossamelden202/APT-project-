package indexer;

import java.util.*;

public class InvertedIndex {
    public Map<String, List<Posting>> index;
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
    }

    public void add(String word, String docId, String position,String url,List<String> anchor,String paragrph, String docBodies , Integer doclength,Map<String, Double> pagerank,Map<String, List<String>> outLinks,Map<String, List<String>> inLinks) {
        
        index.putIfAbsent(word, new ArrayList<>());//unique adding (prevent overwritting)
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
            existing.get().positions.add(position);
            existing.get().url=url;
existing.get().paragraph=paragrph;
existing.get().anchor=new ArrayList<>(anchor);
////////////////////////////
existing.get().docBodies= docBodies;
existing.get().doclength=doclength;
existing.get().inLinks=inLinks;
existing.get().outLinks=outLinks;
existing.get().pagerank=pagerank;



if(anchor.contains(word))
existing.get().isAnchor=true;
else existing.get().isAnchor=false;
        } else {
            Posting p = new Posting(docId);
            p.frequency_body = 1;
            p.frequency_head= 1;
            p.positions.add(position);
            postings.add(p);
            p.url = url;
            p.paragraph = paragrph;

p.docBodies= docBodies;
p.doclength=doclength;
p.inLinks=inLinks;
p.outLinks=outLinks;
p.pagerank=pagerank;
if(anchor.contains(word))
p.isAnchor=true;
else p.isAnchor=false;
            p.anchor = new ArrayList<>(anchor);
        }
      
  
    }

    public void merge(InvertedIndex other) {
        for (String word : other.index.keySet()) {
            this.index.putIfAbsent(word, new ArrayList<>());
            this.index.get(word).addAll(other.index.get(word));
        }

    }
}
