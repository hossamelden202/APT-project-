package indexer;

import java.util.*;

public class InvertedIndex {
    public Map<String, List<Posting>> index;

    public InvertedIndex() {
        index = new HashMap<>();
    }

    public void add(String word, String docId, String position) {
        index.putIfAbsent(word, new ArrayList<>());//unique adding (prevent overwritting)
        List<Posting> postings = index.get(word);
      //(take null or value) 
       Optional<Posting> existing = postings.stream()
                .filter(p -> p.documentId.equals(docId)).findFirst();
//equlavent not equal null
        if (existing.isPresent()) {
            existing.get().frequency++;
            existing.get().positions.add(position);
        } else {
            Posting p = new Posting(docId);
            p.frequency = 1;
            p.positions.add(position);
            postings.add(p);
        }
    }

    public void merge(InvertedIndex other) {
        for (String word : other.index.keySet()) {
            this.index.putIfAbsent(word, new ArrayList<>());
            this.index.get(word).addAll(other.index.get(word));
        }
    }
}
