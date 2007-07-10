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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import net.homeip.donaldm.doxmentor4j.ArchiveDirectory;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("unchecked")
public class IndexFactory
//=========================
{   
   private static final class SingletonHolder
   {
      static final IndexFactory singleton = new IndexFactory();      
   }

   private static Map<String, Indexable> m_IndexerMap = 
                                                new HashMap<String, Indexable>();
   
   protected static Directory m_directory = null;
   
   protected static Analyzer m_analyzer = new StandardAnalyzer();
   
   protected static IndexWriter m_indexWriter = null;
  
   private static Logger logger = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");
   
   static
   {      
      ClassLoader loader = DoxMentor4J.getApp().getClass().getClassLoader();
      InputStream is = loader.getResourceAsStream(
              "net/homeip/donaldm/doxmentor4j/indexers/");
      if (is != null)
      {
         BufferedReader br = new BufferedReader(new InputStreamReader(is));
         String klassName;
         try
         { klassName = br.readLine(); }
         catch (Exception e)
         { klassName = null; }
         while (klassName != null)
         {
            int p = klassName.indexOf(".class");
            if (p >= 0)
               klassName = klassName.substring(0, p);
            Indexable instance = null;
            try
            {
               Class klass = Class.forName("net.homeip.donaldm.doxmentor4j.indexers." +
                       klassName);
               instance = (Indexable) klass.newInstance();
               instance.setIndexWriter(m_indexWriter);
            }
            catch (Exception e)
            {
               try
               { klassName = br.readLine(); }
               catch (Exception ee)
               { klassName = null; }
               continue;
            }
            if (instance != null)
            {
               String[] extensions = instance.supportedFileTypes();
               for (int i=0; i<extensions.length; i++)
                  m_IndexerMap.put(extensions[i], instance);
            }
            try
            { klassName = br.readLine(); }
            catch (Exception ee)
            { klassName = null; }
         }
      }
   }
   
   public static IndexFactory getApp()
   //---------------------------------
   {
      return SingletonHolder.singleton;
   }
   
   private IndexFactory()
   //--------------------
   {       
   }
   
   public Indexable getIndexer(String extension)
   //-------------------------------------------
   {
      extension = extension.trim().toLowerCase();
      if (extension.startsWith("."))
         extension = extension.substring(1);
      Indexer o = (Indexer) m_IndexerMap.get(extension);
      if (o == null) return null;
      Indexable c = null;
      try
      {
         c = (Indexable) o.clone();
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error("Exception cloning " + o.getClass().getName(), e);
         else
         {
            System.err.println("Exception cloning " + o.getClass().getName());
            e.printStackTrace(System.err);
         }
         return null;
      }
      ((Indexer) c).setIndexWriter(m_indexWriter);
      return c;
   }
   
   public static Directory getDirectory() { return m_directory; }
   
   public static Analyzer getAnalyzer() { return m_analyzer; }
   
   static public void create(File archiveFile, String archiveIndexDirName, 
                             String indexDirName, boolean isCreate) 
                 throws IOException
   //---------------------------------------------------------------------------
   {
      create(archiveFile, archiveIndexDirName, indexDirName, 
             new StandardAnalyzer(), isCreate);
   }
   
   static public void create(File archiveFile, String archiveIndexDirName, 
                             String indexDirName, Analyzer analyzer, 
                             boolean isCreate) 
                 throws IOException
   //---------------------------------------------------------------------------
   {
      if (m_indexWriter != null)
         try { m_indexWriter.close(); } catch (Exception e) {}
      if (m_directory != null)
         try { m_directory.close(); } catch (Exception e) {}
      m_analyzer = analyzer;
      if (archiveIndexDirName != null)
         m_directory = ArchiveDirectory.getDirectory(archiveFile, 
                                                     archiveIndexDirName);
      else
         if (indexDirName != null)
         {
            java.io.File f = new File(indexDirName);
            if (! f.exists())
               f.mkdirs();
            if (! f.canWrite())
            {
               System.out.println("Read-Only media for " + indexDirName + 
                                  ". Disabling locks");
               FSDirectory.setDisableLocks(true);
            }
            m_directory = FSDirectory.getDirectory(indexDirName);            
         }
         else
            throw new IOException("Index directory invalid");
      m_indexWriter = new IndexWriter(m_directory, m_analyzer, isCreate);
   }      
   
   static public void create(String indexDirName, boolean isCreate) 
                 throws IOException
   //---------------------------------------------------------------------------
   {
      create(indexDirName, new StandardAnalyzer(), isCreate);
   }
   
   static public void create(String indexDirName, Analyzer analyzer, 
                             boolean isCreate) 
                 throws IOException
   //---------------------------------------------------------------------------
   {
      if (m_indexWriter != null)
         try { m_indexWriter.close(); } catch (Exception e) {}
      if (m_directory != null)
         try { m_directory.close(); } catch (Exception e) {}
      m_analyzer = analyzer;
      java.io.File f = new File(indexDirName);
      if (! f.exists())
         f.mkdirs();
      if (! f.canWrite())
      {
         System.out.println("Read-Only media for " + indexDirName + 
                            ". Disabling locks");
         FSDirectory.setDisableLocks(true);
      }
      m_directory = FSDirectory.getDirectory(indexDirName);      
      m_indexWriter = new IndexWriter(m_directory, m_analyzer, isCreate);
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
         if (logger != null)
            logger.error("Error optimizing Lucene index directory", e);
         else
         {
            System.err.println("Error optimizing Lucene index directory");
            e.printStackTrace(System.err);
         }
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
         m_indexWriter.flush();
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error("Error optimizing Lucene index directory", e);
         else
         {
            System.err.println("Error optimizing Lucene index directory");
            e.printStackTrace(System.err);
         }
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
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error("Error closing Lucene index directory", e);
         else
         {
            System.err.println("Error closing Lucene index directory");
            e.printStackTrace(System.err);
         }
         return false;
      }
      m_indexWriter = null;
      return true;
   }
   
   public static IndexWriter getWriter() { return m_indexWriter; }
}
