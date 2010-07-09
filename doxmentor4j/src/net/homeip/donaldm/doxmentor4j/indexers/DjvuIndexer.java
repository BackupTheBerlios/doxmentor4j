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

import com.lizardtech.djvu.Codec;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.text.DjVuText;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import net.homeip.donaldm.doxmentor4j.AjaxIndexer;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import net.homeip.donaldm.doxmentor4j.Utils;
import org.apache.lucene.document.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DjvuIndexer extends CommandLineIndexer implements Indexable, Cloneable
//=================================================================================
{   
   final static private Logger logger = LoggerFactory.getLogger(DjvuIndexer.class);
   
   public DjvuIndexer()
   //--------------------
   {
      m_extensions = new HashMap<String, Void>()
      {{
          put("djvu", null );
          put("djv", null);
          put("dejavu", null);
      }};
      m_indexor = new TxtIndexer();
   }
   
   public DjvuIndexer(String extractorPath, String extractorArgs)
   //-----------------------------------------------------------
   {
      super(extractorPath, extractorArgs);
      m_extensions = new HashMap<String, Void>()
      {{
          put("djvu", null );
          put("djv", null);
          put("dejavu", null);
      }};
      m_indexor = new TxtIndexer();
   }
   
   @Override public Logger logger() {return logger; }

   @Override
   protected boolean getExtractor(boolean isFindDefault)
   //---------------------------------------------------
   {
      if ( (m_extractorPath != null) && (m_extractorArgs != null) )
         return true;
      java.io.File djvuext = null;
      String s = DoxMentor4J.getApp().getDJVUExtractor();
      if (s != null)
      {
         djvuext = new java.io.File(s);
         if (! djvuext.exists())
         {
            logger.error("DjvuIndexer: Extractor " + s + " does not exist");
            djvuext = null;
         }
      }
      s = DoxMentor4J.getApp().getDJVUArgs();
      if ( (s != null) && (djvuext != null) )
      {
         setExtractorPath(djvuext.getAbsolutePath());
         setExtractorArgs(s);
         setIndexer(IndexFactory.getApp().getIndexer("txt"));
         return true;
      }
      if (isFindDefault)
      {
         logger.warn("No extractor and/or extractor args specified. Attempting to find default extractor");

         // Try to get working extractor per OS
         String os = System.getProperty("os.name").toLowerCase();

         if (os.startsWith("windows"))
         {
            String drive = System.getProperty("user.home");
            if (drive.length() > 3)
               drive = drive.substring(0, 3);
            else
               if (drive.length() < 3)
                  drive = drive + "\\";
            djvuext = Utils.findFile("djvutxt.exe", drive+"windows",
                                       drive+"windows\\system",
                                       drive+"windows\\system32");
            if (djvuext != null)
            {
               setExtractorPath(djvuext.getAbsolutePath());
               setExtractorArgs("$s $d");
               setIndexer(IndexFactory.getApp().getIndexer("txt"));
               return true;
            }
            else
            {
               s = "DjvuIndexer: Could not find djvutxt.exe for extracting "
                        + ".djvu files. Searched: " +  drive+"windows;" + drive+
                           "windows\\system;" + drive+"windows\\system32";
               logger.error(s);
               return false;
            }
         }
         else if (! os.startsWith("OS/2"))
            {
               djvuext = Utils.findFile("djvutxt", "/bin", "/usr/bin", "/usr/local/bin",
                                 "/usr/sbin", "/usr/local/sbin", "/opt/bin");
               if (djvuext != null)
               {
                  setExtractorPath(djvuext.getAbsolutePath());
                  setExtractorArgs("$s $d");
                  setIndexer(IndexFactory.getApp().getIndexer("txt"));
                  return true;
               }
               else
               {
                  s = "DjvuIndexer: Could not find a djvu extractor." +
                             " Searched for djvutxt in /bin:/usr/bin:" +
                             "/usr/local/bin:/usr/sbin:/usr/local/sbin:/opt/bin";
                  logger.error(s);
                  return false;
               }
            }
      }
      return false;
   }

   @Override
   public Reader getText(URI uri, int pageNo, StringBuilder title) throws FileNotFoundException,
                                                                        MalformedURLException,
                                                                        IOException
   //------------------------------------------------------------------------------------------
   {
      // If command line extractor specified then use it else try using a native Java implementation
      Reader reader = null;
      Writer writer = null;
      if (getExtractor(false))
      {
         reader = super.getText(uri, pageNo, title);
         if (reader != null)
            return reader;
      }

      Document djvu = new Document();
      DjVuPage page = null;
      java.io.File tmpDjvu = null;
      java.io.FileInputStream fis = null;
      try
      {
         tmpDjvu = Utils.uri2File(uri);
         if (tmpDjvu != null)
         {
            fis = new java.io.FileInputStream(tmpDjvu);
            djvu.read(fis);
         }
         else
            djvu.read(uri.toURL());
         int no = djvu.size(); //djvu.getDjVmDir().get_pages_num();
         djvu.setAsync(false);
         java.io.File f = java.io.File.createTempFile("djvu", ".txt");
         writer = new FileWriter(f);
         if (pageNo >= 0)
         {
            page = getPage(djvu, pageNo);
            if (page != null)
            {
               Codec codec = page.getText();
               if (codec instanceof DjVuText)
               {
                  DjVuText text = (DjVuText) codec;
                  if (! text.isImageData())
                     writer.write(text.toString());
               }
            }
         }
         else
         {
            for (int pg=0; pg<no; pg++)
            {
               page = null;
               page = getPage(djvu, pg);
               if (page == null)
                  continue;
               Codec codec = page.getText();
               if (codec instanceof DjVuText)
               {
                  DjVuText text = (DjVuText) codec;
                  if (! text.isImageData())
                  {
                     writer.write(text.toString());
                     writer.write('\n'); writer.write(0X0C);
                  }
               }
            }
         }
         try { writer.close(); writer = null; } catch (Exception _e) {}
         f.deleteOnExit();
         return new FileReader(f);
      }
      catch (Exception e)
      {
         logger.error("", e);
         return null;
      }
      finally
      {
         if (fis != null)
            try { fis.close(); } catch (Exception _e) {}
         if ( (tmpDjvu != null) &&
              (tmpDjvu.getAbsolutePath().toLowerCase().indexOf(System.getProperty("java.io.tmpdir")) >= 0) )
            tmpDjvu.delete();
         djvu = null;
      }
   }

   @Override
   public long index(String href, URI uri, boolean followLinks, Object... extraParams) throws IOException
   //-------------------------------------------------------------------------------------------------------------
   {
      // If command line extractor specified then use it else try using a native Java implementation
      if (getExtractor(false))
      {
         long ret = super.index(href, uri, followLinks, extraParams);
         if (ret >= 0)
            return ret;
      }

      Document djvu = new Document();
      java.io.File tmpDjvu = null;
      java.io.FileInputStream fis = null;
      long count =0;
      try
      {
         tmpDjvu = Utils.uri2File(uri);
         if (tmpDjvu != null)
         {
            fis = new java.io.FileInputStream(tmpDjvu);
            djvu.read(fis);
         }
         else
            djvu.read(uri.toURL());
         int no = djvu.size(); //djvu.getDjVmDir().get_pages_num();
         djvu.setAsync(false);
         DjVuPage page = null;         
         for (int pg=0; pg<no; pg++)
         {
            page = null;
            page = getPage(djvu, pg);
            if (page == null)
               break;
            Codec codec = page.getText();
            if (codec instanceof DjVuText)
            {
               DjVuText text = (DjVuText) codec;
               if (! text.isImageData())
               {                                    
                  org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
                  doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
                  doc.add(new Field("title", uri.getPath(),  Field.Store.YES, Field.Index.ANALYZED));
                  doc.add(new Field("contents", text.toString(), Field.Store.NO, Field.Index.ANALYZED,
                                    Field.TermVector.WITH_POSITIONS_OFFSETS));
                  doc.add(new Field("page", Integer.toString(pg+1), Field.Store.YES, Field.Index.NO));
                  if (addDocument(doc))
                     AjaxIndexer.incrementCount();
                  else
                     logger.error("Error adding page " + pg + " of " + uri.toString());
               }
               text = null;
            }
            if ( (pg % 50) == 0 )
            {
               System.runFinalization();
               System.gc();
            }
         }
         return 1L;
      }
      catch (Exception e)
      {
         logger.error("", e);
         return ((count == 0) ? -1 : count);
      }
      finally
      {
         if (fis != null)
            try { fis.close(); } catch (Exception _e) {}
         if ( (tmpDjvu != null) &&
              (tmpDjvu.getAbsolutePath().toLowerCase().indexOf(System.getProperty("java.io.tmpdir")) >= 0) )
            tmpDjvu.delete();
         djvu = null;
         System.gc();
      }
   }

   private DjVuPage getPage(Document djvu, int pg)
   //---------------------------------------------
   {
      DjVuPage page = null;
      try
      {
         page = djvu.getPage(pg, 1, true);
      }
      catch (OutOfMemoryError _e)
      {
         page = null;
         System.runFinalization();
         System.gc();
         try
         {
            page = djvu.getPage(pg, 1, true);
         }
         catch (OutOfMemoryError _ex)
         {
            page = null;
            AjaxIndexer.setError(_ex.getMessage());
            logger.error("", _ex);
            return null;
         }
         catch (Exception _ex)
         {
            logger.error("", _ex);
            AjaxIndexer.setError(_ex.getMessage());
            logger.error("", _ex);
            return null;
         }
      }
      catch (Exception _e)
      {
         logger.error("", _e);
         AjaxIndexer.setError(_e.getMessage());
         return null;
      }
      return page;
   }
}
