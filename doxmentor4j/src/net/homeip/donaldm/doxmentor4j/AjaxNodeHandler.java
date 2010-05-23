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

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.TreeSet;
import net.homeip.donaldm.httpdbase4j.CloneableHeaders;
import net.homeip.donaldm.httpdbase4j.DirItemInterface;
import net.homeip.donaldm.httpdbase4j.DirItemInterface.SORTBY;
import net.homeip.donaldm.httpdbase4j.FileRequest;
import net.homeip.donaldm.httpdbase4j.Http;
import net.homeip.donaldm.httpdbase4j.HttpResponse;
import net.homeip.donaldm.httpdbase4j.Httpd;
import net.homeip.donaldm.httpdbase4j.ArchiveRequest;
import net.homeip.donaldm.httpdbase4j.ArchiveStringTemplateGroup;
import net.homeip.donaldm.httpdbase4j.Postable;
import net.homeip.donaldm.httpdbase4j.Request;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxNodeHandler implements Postable
//==============================================     
{
   final static private Logger logger = LoggerFactory.getLogger(AjaxNodeHandler.class);

   private final int UNKNOWN= 0;
   private final int LEAF = 1;
   private final int NODE = 2;
   
   private int type = UNKNOWN;
   
   private StringTemplateGroup m_templateGroup = null;
   
   private Httpd m_httpd = null;
   
   private java.io.File m_homeDir = null;
   
   private java.io.File m_archiveFile = null;
   
   private String m_archiveDir = null;   
   
   public AjaxNodeHandler()
   //----------------------
   {
      DoxMentor4J app = DoxMentor4J.getApp();
      m_httpd = app.getHttpd();
      m_archiveFile = app.getArchiveFile();
      m_archiveDir = app.getArchiveDir();
      m_homeDir = app.getHomeDir();
   }
   
   @Override
   public Object onHandlePost(long id, HttpExchange ex, Request request,
           HttpResponse r, java.io.File docsDir,
           Object... extraParameters)
   //-----------------------------------------------------------------
   {      
      CloneableHeaders postHeaders = request.getPOSTParameters();
      String dir = postHeaders.getFirst("value");
      if (dir == null) return null;            
      Request req = null;
      int p = -1;
      
      try
      {
         if ( (m_archiveFile != null) || (m_archiveDir != null) )
         {            
            p = dir.indexOf(m_archiveDir);
            if (p >= 0)
               dir = dir.substring(p+m_archiveDir.length());
            dir = dir.trim();
            req = new ArchiveRequest(m_httpd, ex, m_archiveFile, m_archiveDir);
            if (dir.length() > 0)
               req = req.getChildRequest(dir);
            if (m_templateGroup == null)
               m_templateGroup = new ArchiveStringTemplateGroup("AjaxNodeHandlerCP",
                                                            m_archiveFile, 
                                                            m_archiveDir);
         }
         else
            if (m_homeDir != null)
            {
               String dirName = new File(dir).getAbsolutePath();
               String homeName = m_homeDir.getAbsolutePath();
               p = dirName.indexOf(homeName);
               if (p >= 0)
                  dirName = dirName.substring(p+homeName.length());
               if ( (dirName.startsWith("/")) || 
                    (dirName.startsWith(File.separator)) )
               {
                  if (dirName.length() > 1)
                     dirName = dirName.substring(1);
               }
               req = new FileRequest(m_httpd, ex, m_homeDir, new File(dirName));
               if (m_templateGroup == null)
                  m_templateGroup = new StringTemplateGroup("AjaxNodeHandler",
                                                   m_homeDir.getAbsolutePath());
            }
      }
      catch (Exception e)
      {
        logger.error(e.getMessage(), e);
      }
      if (req == null)
         return null;
      String html = "<li>Error generating content</li>";
      try
      {
         StringTemplate st = null;
         if (m_templateGroup.getName().compareTo("AjaxNodeHandlerCP") == 0)
            st = m_templateGroup.getInstanceOf("templates/li");
         else
            st = m_templateGroup.getInstanceOf("/templates/li");
         st.setAttribute("injar",(DoxMentor4J.getApp().getArchiveFile() != null));
         st.setAttribute("nodeEntries", NodeEntry.getEntries(req));
         html = st.toString();
         
      }
      catch (Exception e)
      {
         logger.error(e.getMessage(), e);
         html = "<li>Error generating content " + e.getMessage() + "</li>";
      }
      r.setBody(html);
      r.setMimeType(Http.MIME_HTML);
      r.addHeader("Cache-Control", "no-cache");
      r.addHeader("Pragma", "no-cache");
      r.addHeader("Expires","-1");
      r.addHeader("Content-Length", Integer.toString(html.length()));
      return r;
   }
   
   private DirItemInterface getNodeId(Request req)
   //---------------------------------------------
   {
      Iterator<DirItemInterface> i;
      TreeSet<DirItemInterface> files = req.getDirListFiles(SORTBY.NAME);
      if (files != null)
      {
         for (i = files.iterator(); i.hasNext();)
         {
            DirItemInterface dirItem = i.next();
            if (dirItem.getName().trim().compareToIgnoreCase("LEAF") == 0)
            {
               type = LEAF;
               return dirItem;
            }
            if (dirItem.getName().trim().compareToIgnoreCase("NODE") == 0)
            {
               type = NODE;
               return dirItem;
            }
         }
      }
      return null;
   }
   
   private String readNodeFile(DirItemInterface di)
   //-------------------------------------------
   {
      BufferedReader reader = null;
      StringBuffer sb = new StringBuffer();
      String line = null;
      try
      {
         reader = new BufferedReader(new InputStreamReader(
                 di.getStream()));
         
         while ( (line = reader.readLine()) != null)
            sb.append(line);
      }
      catch (Exception e)
      {
         logger.error(e.getMessage(), e);
      }
      finally
      {
         if (reader != null)
            try
            { reader.close(); }
            catch (Exception e)
            {}
      }
      return sb.toString();
   }
   }


