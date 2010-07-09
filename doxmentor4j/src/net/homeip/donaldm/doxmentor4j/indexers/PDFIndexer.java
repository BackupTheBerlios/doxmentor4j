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


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;

import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import net.homeip.donaldm.doxmentor4j.AjaxIndexer;
import net.homeip.donaldm.doxmentor4j.Utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

public class PDFIndexer extends Indexer implements Indexable, Cloneable
//=====================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(PDFIndexer.class);

   @Override public Logger logger() {return logger; }
              
   public PDFIndexer()
   //-----------------
   {
      m_extensions = new HashMap<String, Void>()
      {{
          put("pdf", null );
      }};
   }

   @Override
   public Reader getText(URI uri, int page, StringBuilder title) throws FileNotFoundException,
                                                                        MalformedURLException,
                                                                        IOException
   //-----------------------------------------------------------------------------------------
   {
      FileWriter writer = null;
      PDDocument pdf = null;
      PDFTextStripper stripper = null;
      java.io.File tmpPdf = null;
      try
      {         
         tmpPdf = Utils.uri2File(uri);
         if (tmpPdf != null)
            pdf = PDDocument.load(tmpPdf.getAbsolutePath(), true);
         else
            pdf = PDDocument.load(uri.toURL(), true);
         PDDocumentInformation pdfInfo = pdf.getDocumentInformation();
         String s = pdfInfo.getTitle();
         if ( (s == null) || (s.length() == 0) )
            s = uri.getPath();
         if (title != null)
            title.append(s);
         stripper = new PDFTextStripper();
         if (page >= 0)
         {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
         }
         else
         {
            stripper.setStartPage(1);
            stripper.setEndPage(pdf.getNumberOfPages());
         }
         java.io.File f = java.io.File.createTempFile("pdf", ".tmp");
         writer = new FileWriter(f);
         stripper.writeText(pdf, writer);
         try { writer.close(); writer = null; } catch (Exception _e) {}
         stripper.resetEngine();
         return new FileReader(f);
      }
      finally
      {
         if (stripper != null)
            try { stripper.resetEngine(); } catch (Exception _e) {}
         if (pdf != null)
            try { pdf.close(); } catch (Exception _e) {}
         if (writer != null)
            try { writer.close(); } catch (Exception _e) {}
         if ( (tmpPdf != null) &&
              (tmpPdf.getAbsolutePath().toLowerCase().indexOf(System.getProperty("java.io.tmpdir")) >= 0) )
            tmpPdf.delete();
      }
   }

   @Override
   public long index(String href, URI uri, boolean followLinks, Object... extraParams) throws IOException
   //-----------------------------------------------------------------------------------------------------
   {
      if (m_indexWriter == null)
      {
         logger.error("PDFIndexer: index writer is null");
         return -1;
      }
      PDDocument pdf = null;
      PDFTextStripper stripper = null;
      Reader reader = null;
      Writer writer = null;
      java.io.File tmpPdf = null;
      try
      {                  
         tmpPdf = Utils.uri2File(uri);
         if (tmpPdf != null)
            pdf = PDDocument.load(tmpPdf.getAbsolutePath(), true);
         else
            pdf = PDDocument.load(uri.toURL(), true);
         PDDocumentInformation pdfInfo = pdf.getDocumentInformation();
         String title = pdfInfo.getTitle();
         if ( (title == null) || (title.isEmpty()) )
            title = uri.getPath();
         stripper = new PDFTextStripper();
         int noPages = pdf.getNumberOfPages();         
         stripper.setSuppressDuplicateOverlappingText(false);         
         if (noPages != PDDocument.UNKNOWN_NUMBER_OF_PAGES)
         {
            for (int page=1; page<=noPages; page++)
            {
               stripper.setStartPage(page);
               stripper.setEndPage(page);
               writer = new StringWriter();
               stripper.writeText(pdf, writer);
               reader = new StringReader(writer.toString());
               Document doc = new Document();
               doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
               doc.add(new Field("title", title.toString(), Field.Store.YES, Field.Index.ANALYZED));
               doc.add(new Field("contents", reader, Field.TermVector.WITH_POSITIONS_OFFSETS));
               doc.add(new Field("page", Integer.toString(page), Field.Store.YES, Field.Index.NO));
               if (addDocument(doc))
                  AjaxIndexer.incrementCount();
               try { writer.close(); writer = null; } catch (Exception _e) {}
               try { reader.close(); reader = null; } catch (Exception _e) {}
               if ((page % 50) == 0)
               {
                  try { System.runFinalization(); System.gc(); } catch (Exception _e) {}
               }
            }
         }
         else
         {
            java.io.File f = java.io.File.createTempFile("pdf", ".tmp");
            writer = new FileWriter(f);
            stripper.writeText(pdf, writer);
            try { writer.close(); writer = null; } catch (Exception _e) {}
            reader = new FileReader(f);
            Document doc = new Document();
            doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
            doc.add(new Field("title", title.toString(), Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("contents", reader, Field.TermVector.WITH_POSITIONS_OFFSETS));
            doc.add(new Field("page", "-1", Field.Store.YES, Field.Index.NO));
            if (addDocument(doc))
               AjaxIndexer.incrementCount();
            try { reader.close(); reader = null; } catch (Exception _e) {}
            try { System.runFinalization(); System.gc(); } catch (Exception _e) {}
         }
         return 1;
      }
      catch (Exception e)
      {
         logger.error("Error indexing PDF text from " + uri.toString(), e);
         return -1;
      }
      finally
      {
         if (stripper != null)
            try { stripper.resetEngine(); } catch (Exception _e) {}
         if (pdf != null)
            try { pdf.close(); } catch (Exception _e) {}
         if (writer != null)
            try { writer.close(); } catch (Exception _e) {}
         if (reader != null)
            try { reader.close(); } catch (Exception _e) {}
         if ( (tmpPdf != null) &&
              (tmpPdf.getAbsolutePath().toLowerCase().indexOf(System.getProperty("java.io.tmpdir")) >= 0) )
            tmpPdf.delete();
      }
   }
}
