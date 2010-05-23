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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import net.homeip.donaldm.doxmentor4j.AjaxIndexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;
import org.pdfbox.util.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schlichtherle.io.FileInputStream;

public class PDFIndexer extends Indexer implements Indexable, Cloneable
//=====================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(PDFIndexer.class);

   @Override public Logger logger() {return logger; }

   private PDFTextStripper m_stripper = null;
              
   public PDFIndexer()
   //-----------------
   {
      EXTENSIONS = new String[] { "pdf" };
   }

   @Override
   public Object getData(InputStream is, String href, String fullPath, 
                         StringBuffer title, StringBuffer body)
   //-------------------------------------------------------------------
   {
      BufferedInputStream bis = null;
      PDDocument pdf = null;
      try
      {
         if (is instanceof BufferedInputStream)
            bis = (BufferedInputStream) is;
         else
            bis = new BufferedInputStream(is);
         pdf = PDDocument.load(bis);
         PDDocumentInformation pdfInfo = pdf.getDocumentInformation();
         String s = pdfInfo.getTitle();
         if ( (s == null) || (s.length() == 0) )
            s = href;
         if (title != null)
            title.append(s);
         StringWriter writer = new StringWriter();
         if (m_stripper == null)
            m_stripper = new PDFTextStripper();
         else
            m_stripper.resetEngine();
         m_stripper.writeText(pdf, writer);
         if (body != null)
            body.append(writer.getBuffer().toString());
      }
      catch (Exception e)
      {
         logger.error("Error extracting PDF text from " + fullPath, e);
         return null;
      }
      finally
      {
         if (bis != null)
            try { bis.close(); } catch (Exception e) {}
      }
      return pdf;
   }

   @Override
   public long index(String href, String fullPath, boolean followLinks,
                     Object... extraParams) throws IOException
   //------------------------------------------------------------------
   {
      if (m_indexWriter == null)
      {
         logger.error("PDFIndexer: index writer is null");
         return -1;
      }
      long count =0, c =0;
      PDDocument pdf = null;
      InputStream is = null;
      try
      {
         is = new FileInputStream(fullPath);         
         if (is == null) return ((count == 0) ? -1 : count);
         
         StringBuffer title = new StringBuffer();
         StringBuffer body = new StringBuffer();
         pdf = (PDDocument) getData(is, href, fullPath, title, body);
         if (pdf != null)
         {                  
            Document doc = new Document();
            doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
            doc.add(new Field("title", title.toString(), 
                              Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("contents", body.toString(), 
                              Field.Store.NO, Field.Index.TOKENIZED));
            if (addDocument(doc))
            {
               AjaxIndexer.incrementCount();
               count++;
            }
            else
               return -1;
            doc = null;
         }
      }
      catch (Exception e)
      {
         logger.error("Error indexing PDF text from " + fullPath, e);
         return ((count == 0) ? -1 : count);
      }

      finally
      {
         if (is != null)
            try { is.close(); } catch (Exception e) {}
         if (pdf != null)
         {
            pdf.close();
            pdf = null;
         }
         
      }
      return count;
   }
   
}
