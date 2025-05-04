package indexer;
import java.util.*;
                   public  class  doc{
                    public String documentId;
                
            public  String url;
            public String  Title ;
            public  String body;
            public  Double pagerank;
            public long doclength;
            

                public doc(String documentId,String url,String  Title ,String body, Double  pagerank,long doclength)
            {
                this.documentId = documentId;
                    this.Title=Title;
                    this.body=body;
                    this.doclength=doclength;
                    this.pagerank=pagerank;
                    this.url=url;
            }

    }

