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

import net.homeip.donaldm.httpdbase4j.Request;
import net.homeip.donaldm.httpdbase4j.TemplatableAdapter;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import net.homeip.donaldm.doxmentor4j.NodeEntry;

import org.antlr.stringtemplate.StringTemplate;

public class Index extends TemplatableAdapter
//-------------------------------------------
{
   
   public Index()
   //----------------------
   {      
   }
   
   private void setTemplateAttributes(StringTemplate template, Request request)
   //-------------------------------------------------------------------------
   {      
      template.setAttribute("searchable", DoxMentor4J.getApp().isSearchable());
      template.setAttribute("indexable", DoxMentor4J.getApp().isIndexable());
      template.setAttribute("injar",(DoxMentor4J.getApp().getArchiveFile() != null));
      template.setAttribute("nodeEntries", NodeEntry.getEntries(request));
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
}
