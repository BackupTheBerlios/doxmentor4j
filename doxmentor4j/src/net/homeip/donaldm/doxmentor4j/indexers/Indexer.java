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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import net.homeip.donaldm.doxmentor4j.AjaxIndexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

abstract public class Indexer implements Indexable, Cloneable
//===========================================================
{         
   
   protected String[] EXTENSIONS = null;
   
   protected IndexWriter m_indexWriter = null;

   private Logger logger = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");
   
   public Indexer() {}
   
   public Indexer(IndexWriter indexWriter)
   //-------------------------------------
   {
      m_indexWriter = indexWriter;
   }
   
   public String[] supportedFileTypes()
   //----------------------------------
   {
      return EXTENSIONS;
   }
   
   public boolean supportsFileType(String ext)
   //-----------------------------------------------
   {
      ext = ext.trim();
      if (ext.startsWith(".")) ext = ext.substring(1);
      for (int i=0; i<EXTENSIONS.length; i++)
         if (ext.compareToIgnoreCase(EXTENSIONS[i]) == 0)
            return true;
      return false;
   }
   
   public void setIndexWriter(IndexWriter indexWriter)
   //------------------------------------------------
   {
      m_indexWriter = indexWriter;
   }
   
   public IndexWriter getIndexWriter() { return m_indexWriter; }
   
   protected Object clone() throws CloneNotSupportedException
   //--------------------------------------------------------
   {
      Indexer klone = (Indexer) super.clone();
      klone.m_indexWriter = m_indexWriter;
      return klone;
   }

   public Object getData(InputStream is, String href, String fullPath, 
                         StringBuffer title, StringBuffer body)
   //--------------------------------------------------------------------
   {
      BufferedReader br = null;
      StringWriter sw = new StringWriter();
      try
      {
         href = href.trim();
         int q = href.length(); 
         if ( (href.endsWith(File.separator)) || (href.endsWith("/")) )
            q = href.length() - 2;
         int p = href.lastIndexOf(File.separatorChar, q);
         if (p < 0)
            p = href.lastIndexOf('/', q);
         String s = (((p >= 0) && (++p < href.length())) 
                           ? href.substring(p) : href);
         if (title != null)
            title.append(s);
         br = new BufferedReader(new InputStreamReader(is));
         char[] buffer = new char[4096];
         int cb =-1; 
         while ( (cb=br.read(buffer)) >= 0) 
            sw.write(buffer, 0, cb);
         sw.flush();
         if (body != null) 
            body.append(sw.getBuffer());
         if (body.length() == 0)
            return null;
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error("Reading document data " + fullPath, e);
         else
            e.printStackTrace(System.err);
         return null;
      }
      finally
      {
         if (br != null) try { br.close(); } catch (Exception e) {}
         if (sw != null) try { sw.close(); } catch (Exception e) {}

      }
      return Boolean.TRUE;
   }
   
   /*
    * Defaults to text indexer
    */ 
   public long index(String href, String fullPath, boolean followLinks,
                     Object... extraParams) throws IOException
   //------------------------------------------------------------------
   {
      if (m_indexWriter == null) return -1;
      long count =0;
            
      InputStream is = null;      
      try
      {
         is = new FileInputStream(fullPath);
         if (is == null) return -1;
         
         StringBuffer title = new StringBuffer();
         StringBuffer body = new StringBuffer();
         if (getData(is, href, fullPath, title, body) != null)
         {
            Document doc = new Document();
            doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
            doc.add(new Field("title", title.toString(), 
                              Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("contents", body.toString(), 
                              Field.Store.NO, Field.Index.TOKENIZED));
            if (addDocument(doc))
            {
               count++;
               AjaxIndexer.incrementCount();
            }
            else
               return -1;
            doc = null;
         }                 
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error("Indexing document " + fullPath, e);
         else
            e.printStackTrace(System.err);
         return -1;
      }

      return count;
   }
   
   protected boolean addDocument(Document doc)
   //-----------------------------------------
   {
      try
      {
         m_indexWriter.addDocument(doc);
      }
      catch (Exception e)
      {                  
         if (logger != null)
            logger.error(this.getClass().getName() + 
                         ": Error adding Lucene Document.", e);
         else
            e.printStackTrace(System.err);
      }
      finally
      {
         doc = null;
      }
      return true;
   }
   
}
