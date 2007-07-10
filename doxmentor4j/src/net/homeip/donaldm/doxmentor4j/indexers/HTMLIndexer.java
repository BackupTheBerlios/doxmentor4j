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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.homeip.donaldm.doxmentor4j.AjaxIndexer;
import net.homeip.donaldm.doxmentor4j.Utils;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.HTMLElementName;
import au.id.jericho.lib.html.Segment;
import au.id.jericho.lib.html.Source;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

public class HTMLIndexer extends Indexer implements Indexable, Cloneable
//======================================================================
{      
   private Logger logger = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");         

   public HTMLIndexer() throws IOException
   //-------------------------------------
   {
      EXTENSIONS = new String[] 
                     { "html", "htm", "ht", "asp", "xml", "xhttp" };
   }   
   
   public Object getData(InputStream is, String href, String fullPath, 
                         StringBuffer title, StringBuffer body)
   //-------------------------------------------------------------------
   {
      BufferedInputStream bis = null;
      Source source = null;
      try
      {
         if (is instanceof BufferedInputStream)
            bis = (BufferedInputStream) is;
         else
            bis = new BufferedInputStream(is);

         String s;
         source = new Source(bis);
         source.setLogger(null);
         source.fullSequentialParse();         
         try { bis.close(); bis = null; } catch (Exception e) {}
         Element el = source.findNextElement(0, HTMLElementName.TITLE);
         if ( (el != null) && (title != null) )
         {
            s = CharacterReference.decodeCollapseWhiteSpace(el.getContent());
            if ( (s == null) || (s.length() == 0) )
               s = href;
            title.append(s);
         }

         el = source.findNextElement(0,HTMLElementName.BODY);
         Segment seg = ((el == null) ? source : el.getContent());
         if (body != null)
            body.append(seg.getTextExtractor().setIncludeAttributes(true).toString());
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
         if (bis != null)
            try { bis.close(); } catch (Exception e) {}
      }
      return source;
   }
   
   @SuppressWarnings("unchecked")
   public long index(String href, String fullPath, boolean followLinks,
                     Object... extraParams) 
                  throws IOException
   //-------------------------------------------------------------------
   {
      if (m_indexWriter == null)
      {         
         if (logger != null)
            logger.error("HTMLIndexer: index writer is null");
         else
            System.err.println("HTMLIndexer: index writer is null");
         return -1;
      }
      
      Map<String, Object> duplicates = null;      
      if ( (extraParams == null) || (extraParams.length == 0) )
         duplicates = new HashMap<String, Object>();
      else
         duplicates = (Map<String, Object>) extraParams[0];
      
      if (duplicates.containsKey(href.trim()))
         return 0;      
      duplicates.put(href.trim(), null);
      
      long count =0, c =0;      
      Source source = null;
      InputStream is = null;
      try
      {         
         try
         {        
            is = new FileInputStream(fullPath);
         }
         catch (Exception e)
         {            
            e.printStackTrace(System.err);
            if (logger != null)
               logger.error("Error opening " + fullPath, e);
            else
            {
               System.out.println("Error opening " + fullPath + ": " + e.getMessage());
               e.printStackTrace(System.err);
            }
            is = null;
            return -1;
         }
         if (is != null)
         {
            StringBuffer title = new StringBuffer();
            StringBuffer body = new StringBuffer();
            source = (Source) getData(is, href, fullPath, title, body);
            if (is != null)
            {
               try { is.close(); } catch (Exception e) {}
               is = null;
            }
            if (source != null)
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
            else
            {
               if (logger != null)
                  logger.error("HTMLIndexer: Error parsing html: " + fullPath);
               else
                  System.err.println("Error parsing html: " + fullPath);
            }
         }
         if ( (followLinks) &&  (source != null) )
         {            
            List<Element> linkElements=source.findAllElements(HTMLElementName.A);
            for (Iterator<Element> it=linkElements.iterator(); it.hasNext();)
            {
               Element linkElement = it.next();
               String linkHref = linkElement.getAttributeValue("href");
               if (linkHref == null) continue;
               linkHref = linkHref.trim();
               String s = linkHref.toLowerCase();
               if ( (s.startsWith("http:/")) || (s.startsWith("/")) || 
                    (s.startsWith("mailto:")) || (s.startsWith("#")) || 
                    (s.startsWith("ftp:/")) )
                  continue;
               
               int p = linkHref.indexOf("#");
               if (p >= 0)
                  linkHref = linkHref.substring(0, p); 
               linkHref = linkHref.trim();
               if (linkHref.length() == 0)
                  continue;
               
               String newHref = href;
               p = href.lastIndexOf('/');               
               if (p > 0)
                  newHref = href.substring(0, p) + "/" + linkHref;
               p = newHref.indexOf('?');
               if (p >= 0)
                  newHref = newHref.substring(0, p);
               boolean mustNorm = ( (newHref.indexOf("..") >= 0) || 
                                    (newHref.indexOf(".") >= 0) );
               if (mustNorm)
               {   
                  File f = new File(newHref);
                  newHref = f.getNormalizedPath();
               }   
               
               if (duplicates.containsKey(newHref))
                  continue;
               String newPath = fullPath;
               p = newPath.lastIndexOf('/');
               if (p > 0)
                  newPath = newPath.substring(0, p) + "/" + linkHref;
               if (mustNorm)
               {   
                  File f = new File(newPath);
                  newPath = f.getNormalizedPath();
               }   
               
               p = newPath.indexOf('?');
               if (p >= 0)
                  newPath = newPath.substring(0, p);
               String ext = Utils.getExtension(linkHref);
               if (! supportsFileType(ext))
               {
                  Indexable altIndexer = IndexFactory.getApp().getIndexer(ext);
                  if (altIndexer != null)
                  {
                     c = altIndexer.index(newHref, newPath, true);
                     if (c > 0) count += c;
                  }
               }
               else
               {
                  c = index(newHref, newPath, true, duplicates);
                  if (c > 0) count += c;
               }
            }            
            linkElements.clear(); linkElements = null;
         }
      }
      catch (Exception e)
      {         
         if (logger != null)
            logger.error("Indexing document " + fullPath, e);
         else
            e.printStackTrace(System.err);
         return ((count == 0) ? -1 : count);
      }


      finally
      {
         if (is != null)
            try { is.close(); } catch (Exception e) {}
         if (source != null)
         {
            source.clearCache();
            source = null;
         }
      }
      
      return count;      
   }   
}
