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
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import java.io.IOException;
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
import java.io.StringReader;
import java.util.regex.Matcher;

public class HTMLIndexer extends Indexer implements Indexable, Cloneable
//======================================================================
{      
   final static private Logger logger = LoggerFactory.getLogger(HTMLIndexer.class);

   @Override public Logger logger() {return logger; }

   public HTMLIndexer() throws IOException
   //-------------------------------------
   {
      m_extensions = new HashMap<String, Void>()
      {{
          put("html", null );
          put("htm", null);
          put("ht", null);
          put("asp", null);
          put("xhttp", null);
      }};
   }

   @Override
   public Reader getText(URI uri, int page, StringBuilder title) throws FileNotFoundException,
                                                                        MalformedURLException,
                                                                        IOException
   //-------------------------------------------------------------------------------------------
   {
      Source source = null;
      java.io.File tmpHtml = null;
      java.io.FileInputStream fis = null;
      try
      {
         String s;
         tmpHtml = Utils.uri2File(uri);
         if (tmpHtml != null)
         {
            fis = new java.io.FileInputStream(tmpHtml);
            source = new Source(fis);
         }
         else
            source = new Source(uri.toURL());
         source.setLogger(null);
         source.fullSequentialParse();         
         Element el = source.findNextElement(0, HTMLElementName.TITLE);
         if ( (el != null) && (title != null) )
         {
            s = CharacterReference.decodeCollapseWhiteSpace(el.getContent());
            if ( (s == null) || (s.length() == 0) )
               s = uri.getPath();
            title.append(s);
         }

         el = source.findNextElement(0,HTMLElementName.BODY);
         Segment seg = ((el == null) ? source : el.getContent());
         return new StringReader(seg.getTextExtractor().setIncludeAttributes(true).toString());
      }
      catch (Exception e)
      {
         logger.error("Reading document data " + uri.toString(), e);
         return null;
      }
      finally
      {
         if (source != null)
            source.clearCache();
         if (fis != null)
            try { fis.close(); } catch (Exception _e) {}
         if ( (tmpHtml != null) &&
              (tmpHtml.getAbsolutePath().toLowerCase().indexOf(System.getProperty("java.io.tmpdir")) >= 0) )
            tmpHtml.delete();
      }
   }

   @Override
   public long index(String href, URI uri, boolean followLinks, Object... extraParams) throws IOException
   //-----------------------------------------------------------------------------------------------------
   {
      if (m_indexWriter == null)
      {         
         logger.error("HTMLIndexer: index writer is null");
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
      Reader reader = null;
      java.io.File tmpHtml = null;
      java.io.FileInputStream fis = null;
      try
      {                  
         StringBuilder title = new StringBuilder();
         reader = getText(uri, -1, title);
         if (reader != null)
         {
            Document doc = new Document();
            doc.add(new Field("path", href, Field.Store.YES, Field.Index.NO));
            doc.add(new Field("title", title.toString(), Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("contents", reader, Field.TermVector.WITH_POSITIONS_OFFSETS));            
            doc.add(new Field("page", "-1", Field.Store.YES, Field.Index.NO));
            try
            {
               if (addDocument(doc))
               {
                  AjaxIndexer.incrementCount();
                  count++;
               }
               else
                  return -1;
            }
            finally
            {
               try { reader.close(); reader = null; } catch (Exception _e) {}
            }
            doc = null;
         }
         else
         {
            logger.error("HTMLIndexer: Error parsing html: " + uri.toString());
            return -1L;
         }

         if (followLinks)
         {            
            tmpHtml = Utils.uri2File(uri);
            if (tmpHtml != null)
            {
               fis = new java.io.FileInputStream(tmpHtml);
               source = new Source(fis);            
            }
            else
               source = new Source(uri.toURL());
            if (source == null)
               return 1L;
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
               if (duplicates.containsKey(newHref))
                  continue;

               boolean mustNorm = ( (newHref.indexOf("..") >= 0) || 
                                    (newHref.indexOf(".") >= 0) );                              

               String uriPath = uri.getPath();
               p = uriPath.lastIndexOf('/');
               if (p >= 0)
                  uriPath = uriPath.substring(0, p);
               URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                                    uriPath + "/" + linkHref, uri.getQuery(), uri.getFragment());
               if (mustNorm)
               {
                  Matcher matcher = AjaxIndexer.URL_PATTERN.matcher(href);
                  if (matcher.matches())
                  {
                     newUri = newUri.normalize();
                     newHref = newUri.toASCIIString();
                  }
                  else
                  {
                     File f = new File(newHref);
                     newHref = f.getNormalizedPath();
                  }
               }
               if (duplicates.containsKey(newHref))
                  continue;
               String ext = Utils.getExtension(linkHref);
               if (! supportsFileType(ext))
               {
                  Indexable altIndexer = IndexFactory.getApp().getIndexer(ext);
                  if (altIndexer != null)
                  {
                     c = altIndexer.index(newHref, newUri, true);
                     if (c > 0) count += c;
                  }
               }
               else
               {
                  c = index(newHref, newUri, true, duplicates);
                  if (c > 0) count += c;
               }
            }            
            linkElements.clear(); linkElements = null;
         }
      }
      catch (Exception e)
      {         
         logger.error("Indexing document " + uri.toString(), e);
         return ((count == 0) ? -1 : count);
      }
      finally
      {
         if (source != null)
         {
            source.clearCache();
            source = null;
         }
         if (reader != null)
            try { reader.close();  } catch (Exception _e) {}
         if (fis != null)
            try { fis.close(); } catch (Exception _e) {}
         if ( (tmpHtml != null) &&
              (tmpHtml.getAbsolutePath().toLowerCase().indexOf(System.getProperty("java.io.tmpdir")) >= 0) )
            tmpHtml.delete();
      }
      
      return count;      
   }   
}
