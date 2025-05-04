package Rankers;
import indexer.InvertedIndex;
import indexer.Posting;
import java.util.*;
import java.util.stream.Collectors;
public class Ranker {
InvertedIndex index; //change according to the process wheather you will take it from index or from query search result
public Ranker(InvertedIndex index) {
this.index = index;
    }
    public  Map<String, Double> rankQuery(List<String> query, String phrase) {
        //i dont know who make query preprocessing but i will assume that query is already preprocessed and person who did this should do same as in index
        Map<String,Boolean> doc_phrase= new HashMap<>();
        Map<String, Scorecomponents> docComponents = new HashMap<>();
        String[] words = query.toArray(new String[0]);  // Convert List to array        int total_docs= 6000;
        int total_docs_with_word=0;
        int total_docs=6000;
        String word;
        int freq;
        String docid;
        Posting calc_D;
        List<Posting>postings;
        Double TF_IDF=0.0;
       // Double total=0.0;
        Double w1=0.5;//weight of TF_IDF
        Double w2=0.3;//weight of page rank
        Double w3=0.2;//weight of phrase matching
        //i suggest this values as if want to focus on get most relevant documents then TF_IDF should be more weighted than page rank and phrase matching 

        Long doc_size;//edit this to fit
        int weight_head=2;
        double idf=0.0;
        double minPR = Double.MAX_VALUE, maxPR = Double.MIN_VALUE;
        for(int i=0;i<words.length;i++)
        {     word=words[i];
            postings=index.index.get(word);
            if(postings !=null)
            {  
                Map<String, Double> tfidfPerDoc = new HashMap<>();
                double minTFIDF = Double.MAX_VALUE;
                double maxTFIDF = Double.MIN_VALUE;
                total_docs_with_word=postings.size();
                if(total_docs_with_word>0)
                {
                    idf = Math.log((double) (total_docs)/(total_docs_with_word));
                }
                for(int j=0;j<postings.size();j++)
                {    
                    calc_D=postings.get(j);
                    freq=(calc_D.frequency_body)+(weight_head*calc_D.frequency_head);
                    docid=calc_D.documentId;
                    double tf=freq;
                    doc_size=index.doclength.getOrDefault(docid,1L);
                    if(doc_size>0)
                    {
                        tf= (tf/doc_size);
                    }
                    TF_IDF = tf * idf;
                    tfidfPerDoc.put(docid, TF_IDF);
                    minTFIDF = Math.min(minTFIDF, TF_IDF);
                    maxTFIDF = Math.max(maxTFIDF, TF_IDF);
                    double pagerank = index.pagerank.getOrDefault(docid, 1.0);
                    minPR = Math.min(minPR, pagerank);
                    maxPR = Math.max(maxPR, pagerank);
                    double phraseMatch = phrase_matching(phrase, docid, doc_phrase);
                    docComponents.putIfAbsent(docid, new Scorecomponents());
                    Scorecomponents comp = docComponents.get(docid);
                    //comp.tfidf += TF_IDF;
                    comp.pagerank = pagerank; 
                    comp.phraseMatch += phraseMatch;
                }
                for (Map.Entry<String, Double> entry : tfidfPerDoc.entrySet()) 
                {
                    String docid2 = entry.getKey();
                    double normTFIDF = entry.getValue();
                    if(maxTFIDF != minTFIDF) 
                    { 
                        normTFIDF = (entry.getValue() - minTFIDF) / (maxTFIDF - minTFIDF);
                    }
                    Scorecomponents comp = docComponents.get(docid2);
                    comp.tfidf += normTFIDF; // Add normalized TF-IDF to the score components
                }
            }
        }
        //*************if it takes along to calc make it normalize over all words */
        Map<String, Double> finalscores = new HashMap<>();
        for (Map.Entry<String, Scorecomponents> entry : docComponents.entrySet()) 
        {
            String docid1 = entry.getKey();
            Scorecomponents comp = entry.getValue();
            double normTfIdf = comp.tfidf;
            double normPR = comp.pagerank;
            if (maxPR != minPR) 
            {
                normPR = (comp.pagerank - minPR) / (maxPR - minPR);
            } 
            double totalScore = w1 * normTfIdf + w2 * normPR + w3 * comp.phraseMatch;
            //System.out.printf("Doc: %-10s | TF-IDF: %.4f | PageRank: %.4f | PhraseMatch: %.2f \n",
               //             docid1, normTfIdf, normPR, comp.phraseMatch);
            finalscores.put(docid1, totalScore);
        }
        
        // Overwrite finalscores with the sorted version
        finalscores = finalscores.entrySet()
            .stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // sort descending
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        

        return finalscores; // Return the final scores for all documents
}


    private Double phrase_matching(String query,String docId,Map<String,Boolean> doc_phrase) 
    {
        // Check if the document contains the exact phrase
        String docText = index.docBodies.get(docId);
        if (docText != null && docText.toLowerCase().contains(query.toLowerCase())) //to work query should be preprocessed like in indexer
        {   if(doc_phrase.get(docId) == null) 
            {
                doc_phrase.put(docId, true); // Mark the document as containing the phrase
                return 5.0;
            }
            else
            {
                return 0.0; // Already counted
            }
        }
        else
        {
            return 0.0; 
        }
    }

    static class Scorecomponents {
        double tfidf = 0.0;
        double pagerank = 0.0;
        double phraseMatch = 0.0;
    }


    // public static void main(String[] args) {
    //     InvertedIndex index = new InvertedIndex();
    //     // TODO: Load or build your index here
    //     ranker ranker = new ranker(index);
    //     ranker.rankQuery("best search engines");
    // }
}
