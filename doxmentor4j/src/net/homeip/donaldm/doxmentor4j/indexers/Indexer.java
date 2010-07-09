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

import java.io.Reader;
import java.net.URI;
import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import java.io.IOException;

import net.homeip.donaldm.doxmentor4j.AjaxIndexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Map;
import net.homeip.donaldm.doxmentor4j.Utils;
import org.apache.lucene.store.AlreadyClosedException;

abstract public class Indexer implements Indexable, Cloneable
//===========================================================
{            
   protected Map<String, Void> m_extensions;
   
   protected IndexWriter m_indexWriter = null;
   
   public Indexer() {}
   
   public Indexer(IndexWriter indexWriter)
   //-------------------------------------
   {
      m_indexWriter = indexWriter;
   }

   abstract public Logger logger();

   @Override
   public Object clone() throws CloneNotSupportedException
   //--------------------------------------------------------
   {
      Indexer klone = (Indexer) super.clone();
      return klone;
   }

   @Override public String[] supportedFileTypes() { return m_extensions.keySet().toArray(new String[0]);  }

   @Override
   public void addFileType(String ext)
   //---------------------------------
   {
      if (ext == null) return;
      int p = ext.trim().indexOf('.');
      if (p >= 0)
         ext = ext.substring(p+1);         
      m_extensions.put(ext.toLowerCase(), null);
   }

   @Override
   public boolean supportsFileType(String ext)
   //-----------------------------------------------
   {
      if (ext == null) return false;
      int p = ext.trim().indexOf('.');
      if (p >= 0)
         ext = ext.substring(p+1);
      return m_extensions.containsKey(ext.toLowerCase());
   }
   
   @Override
   public void setIndexWriter(IndexWriter indexWriter) { m_indexWriter = indexWriter; }
   
   public IndexWriter getIndexWriter() { return m_indexWriter; }

   protected String getUriName(URI uri)
   //----------------------------------
   {
      String href = uri.getPath();
      if (href == null) return "";
      int q = href.length();
      if ( (href.endsWith(File.separator)) || (href.endsWith("/")) )
         q = href.length() - 2;
      int p = href.lastIndexOf(File.separatorChar, q);
      if (p < 0)
         p = href.lastIndexOf('/', q);
      return (((p >= 0) && (++p < href.length()))
               ? href.substring(p) : href);
   }

   @Override
   public Reader getText(URI uri, int page, StringBuilder title)
          throws FileNotFoundException, MalformedURLException, IOException
   //-----------------------------------------------------------
   {
      if (title != null)
         title.append(getUriName(uri));
      String scheme = uri.getScheme();
      if ( (scheme == null) || (scheme.equalsIgnoreCase("file")) )
      {
         File f = Utils.uri2Archive(uri);
         if (f == null)
            f = new File(uri.getPath());
         return new FileReader(f);
      }
      else if (scheme.equalsIgnoreCase("http"))
         return new InputStreamReader(uri.toURL().openStream());
      else
         throw new UnsupportedOperationException(scheme);
   }
   
   /*
    * Defaults to text indexer
    */ 
   @Override
   public long index(String href, URI uri, boolean followLinks, Object... extraParams)
          throws IOException
   //------------------------------------------------------------------
   {
      if (m_indexWriter == null) return -1;
      long count =0;
            
      Reader reader = null;
      try
      {         
         StringBuilder title = new StringBuilder();
         if ( (reader = getText(uri, -1, title)) != null)
         {            
            Document doc = new Document();
            doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
            doc.add(new Field("title", title.toString(), 
                              Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("contents", reader, Field.TermVector.WITH_POSITIONS_OFFSETS));
            doc.add(new Field("page", "-1", Field.Store.YES, Field.Index.NO));
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
         logger().error("Indexing document " + uri.toString(), e);
         return -1;
      }
      finally
      {
         if (reader != null)
            try { reader.close(); } catch (Exception _e) {}
      }

      return count;
   }
   
   protected boolean addDocument(Document doc)
   //-----------------------------------------
   {
      for (int i=0; i<2; i++)
      {
         try
         {
            m_indexWriter.addDocument(doc);
            return true;
         }
         catch (AlreadyClosedException e)
         {
            if (i > 0)
               logger().error("Error adding Lucene Document.", e);
            m_indexWriter = IndexFactory.getWriter();
            continue;
         }
         catch (Exception e)
         {
            logger().error("Error adding Lucene Document.", e);
            return false;
         }
      }
      return false;
   }
   
}
