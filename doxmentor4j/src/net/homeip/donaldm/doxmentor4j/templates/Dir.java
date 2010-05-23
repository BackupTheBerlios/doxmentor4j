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
package net.homeip.donaldm.doxmentor4j.templates;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.TreeSet;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import net.homeip.donaldm.httpdbase4j.CloneableHeaders;
import net.homeip.donaldm.httpdbase4j.DirItemInterface;
import net.homeip.donaldm.httpdbase4j.DirItemInterface.SORTBY;
import net.homeip.donaldm.httpdbase4j.FileRequest;
import net.homeip.donaldm.httpdbase4j.ArchiveRequest;
import net.homeip.donaldm.httpdbase4j.Request;
import net.homeip.donaldm.httpdbase4j.TemplatableAdapter;
import org.antlr.stringtemplate.StringTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Donald Munro
 */
public class Dir extends TemplatableAdapter
//-------------------------------------------
{
   final static private Logger logger = LoggerFactory.getLogger(Dir.class);

   public Dir()
   //----------
   {
   }

   private void setTemplateAttributes(StringTemplate template, Request request)
   //-------------------------------------------------------------------------
   {
      template.setAttribute("injar", (DoxMentor4J.getApp().getArchiveFile() != null));
      CloneableHeaders getMap = request.getGETParameters();
      String dir = "", name = "";
      if (getMap != null)
         dir = getMap.getFirst("dir");
      if (dir == null)
      {
         logger.error("Invalid request to dir.st (" + getClass().getName() + ") dir arg was null in" +
                 "request " + request.toString());
         template.setAttribute("dirEntries", new String[0]);
         return;
      }
      template.setAttribute("title", "Directory Listing " + dir);
      template.setAttribute("caption", "Directory Listing " + name);
      if (dir.length() > 0)
      {
         Request dirRequest;
         try
         {
            Request dirReq = request.getDirRequest();
            if (request instanceof ArchiveRequest)
               dirRequest = new ArchiveRequest((ArchiveRequest) dirReq, dir);
            else
               dirRequest = new FileRequest((FileRequest) dirReq, dir);
         }
         catch (Exception e)
         {
            dirRequest = null;
            template.setAttribute("caption", "Error generating directory "
                                             + e.getMessage());
         }
         if ( (dirRequest == null) || (! dirRequest.exists()) )
         {
            String s;
            if (dirRequest != null)
            {
               logger.info("Invalid directory view request: " + dirRequest.getAbsolutePath());               
               s = "<font color=\"red\">Invalid directory view request: " + dirRequest.getAbsolutePath()
                   + "</font>";
            }
            else
               s = "Null directory view request: " + request.getPath();
            template.setAttribute("dirEntries.{href,name}", "", s);
            return;
         }
         TreeSet<DirItemInterface> dirSet = dirRequest.getDirListDirectories(SORTBY.NAME);
         _setupListTemplate(dirSet, dirRequest, template, true);
         dirSet = dirRequest.getDirListFiles(SORTBY.NAME);
         if (!_setupListTemplate(dirSet, dirRequest, template, false))
            template.setAttribute("dirEntries", new String[0]);
      }
      else
         template.setAttribute("dirEntries", new String[0]);
   }

   @Override
   public File templateFile(StringTemplate template, Request request,
                            StringBuffer mimeType, File dir)
   //----------------------------------------------------------------
   {
      setTemplateAttributes(template, request);
      return super.templateFile(template, request, mimeType, dir);
   }

   @Override
   public String templateString(StringTemplate template, Request request,
                                StringBuffer mimeType)
   //--------------------------------------------------------------------
   {
      setTemplateAttributes(template, request);
      return super.templateString(template, request, mimeType);
   }

   @Override
   public InputStream templateStream(StringTemplate template, Request request,
                                     StringBuffer mimeType)
   //-------------------------------------------------------------------------
   {
      setTemplateAttributes(template, request);
      return super.templateStream(template, request, mimeType);
   }

   @SuppressWarnings("deprecation")
   private boolean _setupListTemplate(TreeSet<DirItemInterface> dirSet,
                                      Request dirRequest, StringTemplate template,
                                      boolean isDirs)
   //-------------------------------------------------------------------------
   {
      if (dirSet == null)
         return false;

      for (Iterator<DirItemInterface> it = dirSet.iterator(); it.hasNext();)
      {
         DirItemInterface dirItem = it.next();
         String path = dirItem.getName();
         int p = path.lastIndexOf('/');
         if ((isDirs) && (p > 0))
            p = path.lastIndexOf('/', p - 1);
         if (p < 0)
         {
            p = path.lastIndexOf(File.separatorChar);
            if ((isDirs) && (p > 0))
               p = path.lastIndexOf(File.separatorChar, p - 1);
         }
         String name;
         if ((p >= 0) && (path.length() > ++p))
            name = path.substring(p);
         else
            name = path;
         if (name.compareToIgnoreCase("LEAF") == 0)
            continue;
         Request fileRequest;
         try
         {
            if (dirRequest instanceof ArchiveRequest)
               fileRequest = new ArchiveRequest((ArchiveRequest) dirRequest, path);
            else
               fileRequest = new FileRequest((FileRequest) dirRequest, path);
         }
         catch (Exception e)
         {
            fileRequest = null;
            logger.error("Error creating Request", e);
            continue;
         }

         String href = fileRequest.getPath();
         if (isDirs)
         {
            try
            {
               href = "/dir.st?" + URLEncoder.encode("dir", "UTF-8") + "="
                      + URLEncoder.encode(href, "UTF-8") + "&"
                      + URLEncoder.encode("name", "UTF-8") + "="
                      + URLEncoder.encode(name, "UTF-8");
            }
            catch (Exception e)
            {
               href = "/dir.st?" + URLEncoder.encode("dir") + "="
                      + URLEncoder.encode(href) + "&"
                      + URLEncoder.encode("name") + "="
                      + URLEncoder.encode(name);
            }
         }

         template.setAttribute("dirEntries.{href,name}", href, name);
      }
      return true;
   }
}
