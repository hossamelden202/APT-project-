package indexer;
import java.util.*;
                   public  class  doc{
                    public String documentId;
                
            public  String url;
            public String  Title ;
            public  String body;
            public Map<String, Double> pagerank =new HashMap<>();
            public long doclength;
            

                public doc(String documentId,String url,String  Title ,String body, Map<String, Double>  pagerank,long doclength)
            {
                this.documentId = documentId;
                    this.Title=Title;
                    this.body=body;
                    this.doclength=doclength;
                    this.pagerank=pagerank;
                    this.url=url;
            }

    }

