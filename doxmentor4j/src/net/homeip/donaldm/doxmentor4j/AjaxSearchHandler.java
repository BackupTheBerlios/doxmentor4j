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


package net.homeip.donaldm.doxmentor4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.StringTokenizer;

import net.homeip.donaldm.httpdbase4j.CloneableHeaders;
import net.homeip.donaldm.httpdbase4j.Http;
import net.homeip.donaldm.httpdbase4j.HttpResponse;
import net.homeip.donaldm.httpdbase4j.Postable;
import net.homeip.donaldm.httpdbase4j.Request;
import net.homeip.donaldm.doxmentor4j.indexers.IndexFactory;
import net.homeip.donaldm.doxmentor4j.indexers.Indexable;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

public class AjaxSearchHandler implements Postable
//================================================
{
   static protected int HITS_PER_PAGE = 10;
   
   private Logger logger = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");         

   public AjaxSearchHandler() 
   //------------------------
   {
   }

   public Object onHandlePost(long id, HttpExchange ex, Request request,
                              HttpResponse r, java.io.File dir, 
                              Object... extraParameters)
   //------------------------------------------------------------------
   {
      String html = "<p><center><h1>Query returned no hits</h1></center></p>";
      CloneableHeaders postHeaders = request.getPOSTParameters();
      String searchText = postHeaders.getFirst("search");
      String page = postHeaders.getFirst("page");      
      int pageNo;
      try { pageNo = Integer.parseInt(page); } catch (Exception e) {pageNo = 1;}
      if (pageNo < 0) pageNo = 1;
      if ( (searchText == null) || (searchText.trim().length() == 0) )
         html = "<p><center><h1>Please enter search text before clicking search" +
                 "</h1></center></p>";
      else
         html = search(searchText, pageNo-1);
        
      
      r.setBody(html);
      r.setMimeType(Http.MIME_HTML);
      r.addHeader("Cache-Control", "no-cache");
      r.addHeader("Content-Length", Integer.toString(html.length()));
      return r;
   }
   
   private final int DISPLAY_WINDOW_SIZE = 10;
   
   private String search(String searchText, int pageNo)
   //--------------------------------------------------
   {
      DoxMentor4J app = DoxMentor4J.getApp();
      java.io.File archiveFile = app.getArchiveFile();
      String indexDirName = app.getIndexDir();
      String archiveIndexDirName = app.getArchiveIndexDir();
      if ( (   (indexDirName == null) || (indexDirName.trim().length() == 0)) &&
           (   (archiveIndexDirName == null) || 
               (archiveIndexDirName.trim().length() == 0)) )
         return errorMessage("Search index not defined in configuration file");
      if (indexDirName != null)
         indexDirName = indexDirName.trim();
      if (archiveIndexDirName != null)
         archiveIndexDirName = archiveIndexDirName.trim();
      Directory directory = null;
      IndexReader indexReader = null;
      Searcher searcher = null;
      StringBuffer html = new StringBuffer();
      try
      {
         try
         {
            if (archiveFile != null)
               IndexFactory.create(archiveFile, archiveIndexDirName, indexDirName, false);
            else
               IndexFactory.create(indexDirName, false);
            directory = IndexFactory.getDirectory();
            if (directory == null)
               return errorMessage("Could not open search index directory");
            indexReader = IndexReader.open(directory);
         }
         catch (Exception e)
         {
            if (logger != null)
               logger.error("Could not open search index directory", e);
            else
               e.printStackTrace(System.err);
            return errorMessage("Could not open search index directory " + "<br>" 
                                + e.getMessage());
         }
         searcher = new IndexSearcher(indexReader);
         Analyzer analyzer = new StandardAnalyzer();
         QueryParser parser = new QueryParser("contents", analyzer);
         Query query = null;
         try
         {
            query = parser.parse(searchText);
         }
         catch (ParseException e)
         {
            if (logger != null)
               logger.error("Error parsing search text (" + searchText + ")", e);
            else
               e.printStackTrace(System.err);
            return errorMessage("Error parsing search text (" + searchText + ")<br>"
                    + e.getMessage());
         }
         Hits hits = searcher.search(query);
         int count = hits.length();
         if (count == 0)
            html.append("<div style=\"vertical-align: middle; \"><p>Query " +
                    "returned no hits</p></div>");
         else
         {
            int pages = count / HITS_PER_PAGE;
            if ((count % HITS_PER_PAGE) != 0)
               pages++;
            int startHit = pageNo*HITS_PER_PAGE;
            int endHit = Math.min(startHit + HITS_PER_PAGE, hits.length());
            html.append("<div style=\"vertical-align: top; \">");
            html.append("<p width=\"100%\" align=\"right\" class=\"search\">");
            html.append(String.format("Page %d/%d (Hits %d-%d of %d)", pageNo+1,
                    pages, startHit, endHit, hits.length()));
            html.append("</div>");
            html.append("<div style=\"vertical-align: middle; \">");
            for (int i = startHit; i < endHit; i++)
            {
               Document doc = hits.doc(i);
               String title = doc.get("title");
               if (title == null)
                  title = "<i>Title undefined</i>";
               String href = doc.get("path");
               if (href != null)
               {
                  html.append(String.format("<a href=\"%s\" class=\"search\">" +
                          "%s&nbsp;(%s)</a>",
                          href, title, href));
                  String fragment = getFragment(href, searchText);
                  if (fragment.length() > 0)
                     html.append(fragment);
                  html.append("<hr>");
               }
            }
            html.append("</div>");
            html.append("<div>");
            html.append("<p width=\"100%\" align=\"center\" class=\"search\">");
            
            // The javascript search function page no. is 1 based while pageNo
            // is 0 based.
            if (pageNo > 0)
            {               
               html.append("<font size=\"+1\"><a class=\"search\" " +
                       "href=\"javascript:search('" + searchText + "'," + pageNo
                       + ")\">Previous</a></font>&nbsp;&nbsp;");
            }
            int startPage = Math.max(0, pageNo - DISPLAY_WINDOW_SIZE/2);
            int endPage = Math.min(pages, pageNo + DISPLAY_WINDOW_SIZE/2);
            for (int p=startPage; p<endPage; p++)
            {
               if (p != pageNo)
                  html.append("<a href=\"javascript:search('" + searchText + 
                          "'," +  (p+1) + ")\" " + "class=\"search\">" + (p+1) + 
                          "</a>&nbsp;");
               else
                  html.append((p+1) + "&nbsp;");
            }
            if ((pageNo+1) < pages)
               html.append("<font size=\"+1\"><a class=\"search\" " +
                       "href=\"javascript:search('" + searchText + "'," + 
                       (pageNo + 2) + ")\"> Next</a></font>");
            html.append("</div>");
         }
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error("Error creating search query (" + searchText + ")", e);
         else
            e.printStackTrace(System.err);
         return errorMessage("Error creating search query (" + searchText + ")<br>"
                 + e.getMessage());
      }


      finally
      {
         if (searcher != null) try { searcher.close(); } catch (Exception e) {}
         if (indexReader != null) try { indexReader.close(); } catch (Exception e) {}
         if (directory != null) try { directory.close(); } catch (Exception e){}
      }
      return html.toString();
   }
   
   
   private String getFragment(String href, String searchText)
   //--------------------------------------------------------
   {
      DoxMentor4J app = DoxMentor4J.getApp();
      java.io.File archiveFile = app.getArchiveFile();
      String archiveDir = app.getArchiveDir();
      File home = null;
      InputStream is = null;
      BufferedReader br = null;
      if (archiveFile != null)
         home = new File(archiveFile, archiveDir);
      else
         home = new File(app.getHomeDir());
      File f;
      if (home != null)
      {
         f = new File(home, href);
         try                    
         {  
            is = new FileInputStream(f);
         }
         catch (Exception e)
         {
            if (logger != null)
               logger.error("Error opening " + f.getAbsolutePath(), e);
            else
               e.printStackTrace(System.err);
            return "Error opening " + f.getAbsolutePath() + " (" + 
                   e.getMessage() + ")";
         }
      }
      else         
         return "";

      
      String ext = Utils.getExtension(href);
      Indexable indexer = IndexFactory.getApp().getIndexer(ext);
      StringBuffer sb = new StringBuffer();
      if (indexer != null)
      {
         StringBuffer title = new StringBuffer();
         StringBuffer body = new StringBuffer();
         if (indexer.getData(is, href, f.getAbsolutePath(), null, body) != null)
         {
            String s = body.toString();            
            body.setLength(0);
            StringTokenizer tok = new StringTokenizer(searchText);
            while (tok.hasMoreTokens())
            {
               String t = tok.nextToken();
               int p = Utils.indexOfIgnoreCase(s, t);
               if (p >= 0)
               {
                  sb.append(extract(s, p, t));
                  sb.append("<BR>");
               }
            }
         }
      }
      return sb.toString();
   }
   
   private final int MAX_DISPLAY_CHARS = 512;
   
   private String extract(String s, int p, String t)
   //-----------------------------------------------
   {
      int i = p-1, sentences = 0;
      String pre ="", suf ="";
      while (i >= 0)
      {
         char ch = s.charAt(i--); 
         if ( (ch == '.') && (sentences++ == 1) ) break;
         if ((p - i) > MAX_DISPLAY_CHARS/2) break;
         pre = ch + pre;
      }
      i = p; sentences = 0;
      while (i < s.length())
      {
         char ch = s.charAt(i++);
         if ( (ch == '.') && (sentences++ == 1) ) break;
         if ((i - p) > MAX_DISPLAY_CHARS/2) break;
         suf = suf + ch;
      }
      s = pre + suf;
      s = s.replaceAll("\n\r", "<BR>");
      s = s.replaceAll("\r\n", "<BR>");
      s = s.replaceAll("\r", "<BR>");
      s = s.replaceAll("\n", "<BR>");
      s = s.replaceAll(t, "<font color=\"red\">" + t + "</font>");
      s = "<p class=\"search\">" + s + "</p>";
      return s;
   }
   
   private String errorMessage(String message)
   //-----------------------------------------
   {
      return "<font color=\"red\"><center><h1>" + message + "</h1></center></font>";
   }
}
