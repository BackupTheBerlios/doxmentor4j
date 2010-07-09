package util;

import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;


public class TestUtils 
//====================
{
   static public Document[] getHits(Directory directory, String fieldName, int maxHits, String ... terms)
          throws IOException
   //------------------------------------------------------------------------------------------------
   {
      IndexSearcher searcher = null;
      try
      {
         searcher = new IndexSearcher(directory);
         BooleanQuery bq = new BooleanQuery();
         for (String term : terms)
         {
            Term t = new Term(fieldName, term);
            Query query = new TermQuery(t);
            bq.add(query, Occur.MUST);
         }

         TopDocs hits = searcher.search(bq, maxHits);
         Document[] docs = new Document[hits.totalHits];
         int i = 0;
         for (ScoreDoc hit : hits.scoreDocs)
            docs[i++] = searcher.doc(hit.doc);
         return docs;
      }
      catch (Exception e)
      {
         return null;
      }
      finally
      {
         if (searcher != null)
            try { searcher.close(); } catch (Exception _e) {}
      }
   }
}
