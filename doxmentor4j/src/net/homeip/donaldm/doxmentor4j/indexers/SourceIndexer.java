/*
 * DoxMentor4J - A standalone cross platform Web/Ajax based documentation library that 
 * is fully searchable and may be hosted in the file system, in an archive or 
 * embedded in the Java classpath.
 *
 * (C) Donald Munro 2007
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * http://www.gnu.org/copyleft/gpl.html
*/

package net.homeip.donaldm.doxmentor4j.indexers;

import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import java.io.Reader;
import java.util.Set;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

/**
 * Abstract base class for indexers for indexing computer languages
 */
abstract public class SourceIndexer extends Indexer implements Indexable, Cloneable
//=================================================================================
{   
   static public boolean USE_SOURCE_ANALYZER = false;

   public SourceIndexer() { }
      
   @Override
   public void setIndexWriter(IndexWriter indexWriter)
   //-------------------------------------------------------
   {
      if (! USE_SOURCE_ANALYZER)
      {
         super.setIndexWriter(null); // Don't index source on first pass
         return;
      }
      if ( (indexWriter == null) || (indexWriter.getAnalyzer() == null) || 
           (! (indexWriter.getAnalyzer() instanceof SourceCodeAnalyzer)) )
      {
         DoxMentor4J app = DoxMentor4J.getApp();
         java.io.File archiveFile = app.getArchiveFile();
         String dirName = app.getIndexDir();
         String archiveDirName = app.getArchiveIndexDir();
         if (indexWriter != null)
            try { indexWriter.close(); } catch (Exception e) {}
         Directory dir = null;
         try
         {
            if (archiveFile != null)
               IndexFactory.create(archiveFile, archiveDirName, dirName,
                                   new SourceIndexer.SourceCodeAnalyzer(),
                                   false);
            else
               IndexFactory.create(dirName,
                                   new SourceIndexer.SourceCodeAnalyzer(),
                                   false);
            m_indexWriter = IndexFactory.getWriter();
         }
         catch (Exception e)
         {
            logger().error("Error opening index directory " + dirName, e);
            m_indexWriter = null;
            return;
         }         
      }
      else
      {
         if (indexWriter.getAnalyzer() instanceof  SourceCodeAnalyzer)
            ((SourceCodeAnalyzer)indexWriter.getAnalyzer()).setStopSet();
         m_indexWriter = indexWriter;
      }
   }
   
   abstract public String[] getLanguageStopWords();
   
   public class SourceCodeAnalyzer extends StandardAnalyzer
   //======================================================
   {
      @SuppressWarnings("unchecked")
      private Set m_stopSet;
      
      public SourceCodeAnalyzer()
      //-------------------------
      {
         super(DoxMentor4J.LUCENE_VERSION);
         m_stopSet = StopFilter.makeStopSet(getLanguageStopWords());
      }
      
      public void setStopSet() 
      //----------------------
      { 
         m_stopSet = StopFilter.makeStopSet(getLanguageStopWords());
      }
      
      @Override
      public TokenStream tokenStream(String fieldName, Reader reader)
      //-------------------------------------------------------------
      {
         TokenStream result = super.tokenStream(fieldName, reader);
         result = new StopFilter(true, result, m_stopSet);
         return result;
      }
   }
}
