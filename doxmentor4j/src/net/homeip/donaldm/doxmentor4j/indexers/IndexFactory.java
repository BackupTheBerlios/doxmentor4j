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
import java.io.File;
import java.io.IOException;
import net.homeip.donaldm.doxmentor4j.ArchiveDirectory;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.store.NoLockFactory;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("unchecked")
public class IndexFactory
//=========================
{   
   final static private Logger logger = LoggerFactory.getLogger(IndexFactory.class);

   private static final class SingletonHolder
   {
      static final IndexFactory singleton = new IndexFactory();      
   }

   protected static Directory mLuceneDirectory = null;

   protected static File mDirectory = null;

   protected static Analyzer m_analyzer = new StandardAnalyzer(DoxMentor4J.LUCENE_VERSION);
   
   protected static IndexWriter m_indexWriter = null;
        
   public static IndexFactory getApp()
   //---------------------------------
   {
      return SingletonHolder.singleton;
   }
   
   private IndexFactory()
   //--------------------
   {       
   }

   public static Directory getDirectory() { return mLuceneDirectory; }

   public Indexable getIndexer(String extension)
   //-------------------------------------------
   {
     if ( (extension == null) || (extension.isEmpty()) )
        return null;
      Lookup.Result<Indexable> indexors = Lookup.getDefault().lookupResult(Indexable.class);
      for (Indexable indexable : indexors.allInstances())
      {
         if (! indexable.supportsFileType(extension)) continue;
         Indexer indexer = null;
         try
         {
            Indexer ind = (Indexer) (((Indexer) indexable).clone());
            indexer = ind;
         }
         catch (Exception e)
         {
            logger.warn("Error cloning " + indexable + " using uncloned instance");
            try { indexer = (Indexer) indexable; } catch (Exception _e) { indexer = null; logger.error("", _e);}
         }
         if (indexer != null)
         {
            indexer.setIndexWriter(m_indexWriter);
            return indexer;
         }
      }
      logger.error("Could not find indexor class for extension " + extension);
      return null;
   }
      
   public static Analyzer getAnalyzer() { return m_analyzer; }
   
   static public void create(File archiveFile, String archiveIndexDirName, 
                             String indexDirName, boolean isCreate) 
                 throws IOException
   //---------------------------------------------------------------------------
   {
      create(archiveFile, archiveIndexDirName, indexDirName, 
             new StandardAnalyzer(DoxMentor4J.LUCENE_VERSION), isCreate);
   }
   
   static public void create(File archiveFile, String archiveIndexDirName, 
                             String indexDirName, Analyzer analyzer, 
                             boolean isCreate) 
                 throws IOException
   //---------------------------------------------------------------------------
   {
      if (m_indexWriter != null)
         try { m_indexWriter.close(); } catch (Exception e) {}
      boolean isClosed = false;
      if (mLuceneDirectory == null)
         isClosed = true;
      else
         try { mLuceneDirectory.close(); isClosed = true; } catch (Exception e) {}
      m_analyzer = analyzer;
      if (archiveIndexDirName != null)
      {
         if ( (! isClosed)  && (mLuceneDirectory != null) )
            ((ArchiveDirectory) mLuceneDirectory).clearLock(IndexWriter.WRITE_LOCK_NAME);
         mLuceneDirectory = ArchiveDirectory.getDirectory(archiveFile, archiveIndexDirName, isCreate);
         mDirectory = ((ArchiveDirectory) mLuceneDirectory).getTempDirectory();
      }
      else if (indexDirName != null)
      {
         java.io.File f = new File(indexDirName);
         if ( (f.exists()) && (! f.isDirectory()) )
            f.delete();
         if (! f.exists())
            f.mkdirs();
         File ff;
         try { ff = File.createTempFile("tst", ".tmp", f); } catch (Exception _e) { ff = null; }
         if ( (ff == null) || (! ff.exists()) )
         {
            System.out.println("Read-Only media for " + indexDirName + ". Disabling locks");
            logger.info("Read-Only media for " + indexDirName + ". Disabling locks");
            if (isClosed)
            {
               mLuceneDirectory = FSDirectory.open(f, NoLockFactory.getNoLockFactory());
               mDirectory = f;
            }
         }
         else
         {
            if (ff != null)
               ff.delete();
            if (isClosed)
            {
               mLuceneDirectory = FSDirectory.open(f);
               mDirectory = f;
            }
         }
      }
      else
         throw new IOException("Index directory invalid");
      m_indexWriter = new IndexWriter(mLuceneDirectory, m_analyzer, isCreate,
                                      IndexWriter.MaxFieldLength.UNLIMITED);
   }      
   
   static public void create(String indexDirName, boolean isCreate) throws IOException
   //---------------------------------------------------------------------------
   {
      create(indexDirName, new StandardAnalyzer(DoxMentor4J.LUCENE_VERSION), isCreate);
   }
   
   static public void create(String indexDirName, Analyzer analyzer, boolean isCreate) 
                      throws IOException
   //---------------------------------------------------------------------------
   {
      if (m_indexWriter != null)
         try { m_indexWriter.close(); } catch (Exception e) { logger.error("", e); }      
      m_analyzer = analyzer;
      java.io.File f = new File(indexDirName);
      if ( (f.exists()) && (! f.isDirectory()) )
         f.delete();
      if (! f.exists())
         f.mkdirs();
      if (mLuceneDirectory != null)
      {
         if ( (mDirectory != null) && (! mDirectory.equals(f)) )
         {
            try { mLuceneDirectory.close(); } catch (Exception e) { logger.error("", e); }
            mLuceneDirectory = null;
         }
      }
      File ff;
      try { ff = File.createTempFile("tst", ".tmp", f); } catch (Exception _e) { ff = null; }
      if ( (ff == null) || (! ff.exists()) )
      {
         logger.info("Read-Only media for " + indexDirName + ". Disabling locks");
         if (mLuceneDirectory == null)
         {
            mLuceneDirectory = FSDirectory.open(f, NoLockFactory.getNoLockFactory());
            mDirectory = f;
         }
      }
      else
      {
         if (ff != null)
            ff.delete();
         if (mLuceneDirectory == null)
         {
            mLuceneDirectory = FSDirectory.open(f);
            mDirectory = f;
         }
      }
      m_indexWriter = new IndexWriter(mLuceneDirectory, m_analyzer, isCreate,
                                      IndexWriter.MaxFieldLength.UNLIMITED);
   }      

   public static boolean optimizeWriter()
   //----------------------------------------------------
   { 
      if (m_indexWriter == null) return false;
      try
      {
         m_indexWriter.optimize();
      }
      catch (Exception e)
      {
         logger.error("Error optimizing Lucene index directory", e);
         return false;
      }
      return true;
   }
  
   public static boolean flushWriter()
   //----------------------------------------------------
   { 
      if (m_indexWriter == null) return false;
      try
      {
         m_indexWriter.commit();
      }
      catch (Exception e)
      {
         logger.error("Error optimizing Lucene index directory", e);
         return false;
      }
      return true;
   }

   public static boolean closeWriter()
   //----------------------------------------------------
   { 
      if (m_indexWriter == null) return false;
      try
      {
         m_indexWriter.close();
         m_indexWriter = null;
      }
      catch (Exception e)
      {
         logger.error("Error closing Lucene index directory", e);
         return false;
      }      
      return true;
   }
   
   public static IndexWriter getWriter() { return m_indexWriter; }
}
