package Ranker;
import java.util.*;
import indexer.InvertedIndex;
import indexer.Posting;
public class RankerTest {
public static void main(String[] args) {
        // Create a mock inverted index
        InvertedIndex index = new InvertedIndex();

        // Sample documents
        index.docBodies.put("doc1", "This is the best search engine ever.");
        index.docBodies.put("doc2", "Search engines are tools to find information.");
        index.docBodies.put("doc3", "Another engine to compare with the best search engines.");

        // Page ranks
        index.pagerank.put("doc1", 2.0);
        index.pagerank.put("doc2", 1.0);
        index.pagerank.put("doc3", 3.0);

        // Document lengths
        index.doclength.put("doc1", 7);
        index.doclength.put("doc2", 8);
        index.doclength.put("doc3", 9);

        // Anchors
        index.anchors.put("doc1", Arrays.asList("top search engine"));
        index.anchors.put("doc2", Arrays.asList("find info"));
        index.anchors.put("doc3", Arrays.asList("search engines"));

        // Build index with dummy data
        index.index.put("best", Arrays.asList(
                new Posting("doc1", 1, Arrays.asList("body")),
                new Posting("doc3", 1, Arrays.asList("body"), true)
        ));
        index.index.put("search", Arrays.asList(
                new Posting("doc1", 1, Arrays.asList("title")),
                new Posting("doc2", 1, Arrays.asList("heading")),
                new Posting("doc3", 1, Arrays.asList("body"), true)
        ));
        index.index.put("engines", Arrays.asList(
                new Posting("doc2", 1, Arrays.asList("body")),
                new Posting("doc3", 1, Arrays.asList("body"))
        ));

        // Create ranker and rank query
        ranker r = new ranker(index);
        r.rankQuery("best search engines");
}
}
