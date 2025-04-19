package indexer;

import java.util.*;

public class InvertedIndex {
    public Map<String, List<Posting>> index;
    public String urls;
    public String paragraphs;
    public Map<String, List<String>> anchors = new HashMap<>();
    public InvertedIndex() {
        index = new HashMap<>();
    }

    public void add(String word, String docId, String position,String url,List<String> anchor,String paragrph) {
        index.putIfAbsent(word, new ArrayList<>());//unique adding (prevent overwritting)
        List<Posting> postings = index.get(word);
      //(take null or value) 
       Optional<Posting> existing = postings.stream()
                .filter(p -> p.documentId.equals(docId)).findFirst();
//equlavent not equal null
        if (existing.isPresent()) {
            existing.get().frequency++;
            existing.get().positions.add(position);
            existing.get().url=url;
existing.get().paragraph=paragrph;
existing.get().anchor=new ArrayList<>(anchor);

if(anchor.contains(word))
existing.get().isAnchor=true;
else existing.get().isAnchor=false;
        } else {
            Posting p = new Posting(docId);
            p.frequency = 1;
            p.positions.add(position);
            postings.add(p);
            p.url = url;
            p.paragraph = paragrph;
            
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
