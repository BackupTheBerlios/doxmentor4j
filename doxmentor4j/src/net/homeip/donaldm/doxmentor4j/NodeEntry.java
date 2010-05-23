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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.homeip.donaldm.httpdbase4j.DirItemInterface;
import net.homeip.donaldm.httpdbase4j.DirItemInterface.SORTBY;
import net.homeip.donaldm.httpdbase4j.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeEntry
//=====================
{
   final static private Logger logger = LoggerFactory.getLogger(NodeEntry.class);

   public final int UNKNOWN = 0;
   public final int LEAF = 1;
   public final int NODE = 2;
   
   public int type = 0;
   
   static public NodeEntry[] getEntries(Request request)
   //---------------------------------------------------
   {
      String s = request.getURI().getPath().trim();
      Request dirRequest = request;
      if ( (s.length() == 0) || (s.compareTo("/") == 0) ||
              (s.compareTo("/index.st") == 0) )
      {
         if (s.compareTo("/index.st") == 0)
            request = request.getDirRequest();
         request = request.getChildRequest("library");
      }
      else
         if (! request.isDirectory())
            request = request.getDirRequest();

      NodeEntry[] nodeEntries = null;
      TreeSet<DirItemInterface> files =null, dirs = null;      
      Iterator<DirItemInterface> it;
      StringBuffer text = new StringBuffer();
      NodeEntry nodeEntry = null;
      if (_isNode(request, text))
      {
         dirs = request.getDirListDirectories(SORTBY.NAME);
         if (dirs == null)
            nodeEntries = new NodeEntry[0];
         else
         {
            nodeEntries = new NodeEntry[dirs.size()];
            int j = 0;
            for (it = dirs.iterator(); it.hasNext();)
            {
               DirItemInterface dirItem = it.next();
               nodeEntry = null;
               try
               {
                  nodeEntry = new NodeEntry(request, dirItem);
                  nodeEntries[j++] = nodeEntry;
               }
               catch (Exception e)
               {                  
                  logger.error("Error creating index directory", e);
               }               
            }
         }         
      }
      else
      {
         if (_isLeaf(request, text))
         {
            nodeEntries = new NodeEntry[1];
            try
            {
               nodeEntry = new NodeEntry(request, text.toString());                  
               nodeEntries[0] = nodeEntry;
            }
            catch (Exception e)
            {
               logger.error("Error creating index directory", e);
            }               
         }
      }
      
      return nodeEntries;
   }
   
   static private boolean _isNode(Request request, StringBuffer text)
   //--------------------------------------------------------------
   {
      if (! request.isDirectory())
         request = request.getDirRequest();
      Request node = request.getChildRequest("NODE");
      if ( (node != null) && (node.exists()) )
         return readNodeText(node.getStream(), text);
      else
         return false;
   }
   
   static private boolean _isLeaf(Request request, StringBuffer text)
   //-------------------------------------------------------
   {
      if (! request.isDirectory())
         request = request.getDirRequest();
      Request node = request.getChildRequest("LEAF");
      if ( (node != null) && (node.exists()) )
         return readNodeText(node.getStream(), text);
      else
         return false;
   }
   
   static protected boolean readNodeText(InputStream is, StringBuffer text)
   //----------------------------------------------------------------------
   {
      BufferedReader reader = null; 
      String line = null;
      try
      {
         reader = new BufferedReader(new InputStreamReader(is));
         
         while ( (line = reader.readLine()) != null)
         {
            text.append(line);
            text.append("^^^");
         }
      } 
      catch (Exception e)
      {
         logger.error(e.getMessage(), e);
         return false;
      }
      finally
      {
         if (reader != null)
            try  { reader.close(); }  catch (Exception e) {}
      }
      return true;
   }   
   
   protected static String getLeafName(String text)
   //------------------------------------------
   {
      int p;
      String k, v, description =null;
      String[] lines = text.split("\\^\\^\\^"); 
      
      for (int i=0; i<lines.length; i++)
      {
         String line = lines[i].trim();
         if ( (line.length() == 0) || (line.startsWith("#")) )
            continue;
         p = line.indexOf("->");
         k = line.substring(0, p).trim();
         if (k.compareToIgnoreCase("name") == 0)
         {
            description = "";
            if (line.length() >= ++p)
               description = line.substring(p).trim();
            break;
         }
      }
      if (description == null) 
         description = "Invalid LEAF file: No name attribute";
      return description;
   }
   
   /**
    * Creates an Iterator for view directives in a LEAF file.
    * <B>NOTE:</b> The next() method can return null.
    * @param text A String containing the lines of the LEAF file separated by 
    * a '^^^' string
    * @return An Iterator over the view-> directives of the LEAF file
    */
   protected static Iterator<ViewInfo> viewIterator(final String text)
   //-----------------------------------------------------------------
   {
      return new Iterator<NodeEntry.ViewInfo>() 
      {
         int i =0;
         String[] lines = text.split("\\^\\^\\^"); 
         Pattern viewPattern = Pattern.compile("view[ \\t\\f]*->[ \\t\\f]*(.*)");
         Pattern csvPattern = Pattern.compile("\"([^\"]+?)\",?|([^,]+),?|,");
         Pattern stripQuotesPattern = Pattern.compile("\"(.*)\"");
         Map<String, String> matches = new HashMap<String, String>();
         
         @Override
         public boolean hasNext()
         //----------------------
         {
            return (i < lines.length);
         }

         @Override
         public ViewInfo next()
         //--------------------
         {            
            String line = lines[i].trim();            
            while ( (line.length() == 0) || (line.startsWith("#")) )
            {
               if (i < lines.length)
                  line = lines[++i].trim();
               else
                  return null;
            }
            
            Matcher matcher = viewPattern.matcher(line);
            while (! matcher.matches())
            {
               if (i < lines.length)
               {
                  line = lines[++i].trim();
                  matcher = viewPattern.matcher(line);
               }
               else
                  return null;
            }
            if (matcher.matches())
            {
               String value = matcher.group(1).trim();
               Matcher csvMatcher = csvPattern.matcher(value);
               matches.clear();
               while (csvMatcher.find())
               {
                  String match = csvMatcher.group();
                  if (match == null)  break;
                  if (match.endsWith(","))
                     match = match.substring(0, match.length() - 1).trim();
                  if (match.startsWith("\""))
                     match = match.substring(1, match.length() - 1).trim();
                  int p = match.indexOf('=');
                  if (p < 0) continue;
                  String k = match.substring(0, p).trim().toLowerCase();
                  String v = "";
                  if (match.length() >= ++p)
                     v = match.substring(p).trim();
                  Matcher m = stripQuotesPattern.matcher(v);
                  if (m.matches())
                     v = m.group(1);
                  matches.put(k, v);
               }
               String href = matches.get("href");
               String name = matches.get("description");               
               if (name == null) name = "View";
               String target = matches.get("target");
               boolean isIndexable = true, isDir =false;
               String yn = matches.get("dir");
               if (yn != null)
               {
                  yn = yn.toLowerCase();
                  if ( (yn.compareTo("yes") == 0) || (yn.compareTo("y") == 0) )
                     isDir = true;
               }
               
               String search = matches.get("search");
               if (search != null)
               {
                  yn = search.toLowerCase();
                  if ( (yn.compareTo("no") == 0) || (yn.compareTo("n") == 0) )
                     isIndexable = false;                     
               }
               else
                  isIndexable = false;
               i++;
               ViewInfo vi = new ViewInfo(name, href, target, isIndexable, isDir,
                                          search);
               return vi;
            }
            else
            {
               i++;
               return null;
            }
         }

         @Override
         public void remove()
         {
            throw new UnsupportedOperationException("Not supported.");
         }
      };

   }
   
   /*
    * A leaf file comprises one line with the name of the leaf in the format:
    * name->Name of leaf
    * and one or more view lines with the format:
    * view->href=url,description=Description of view,target=Target in anchor,
    *       search=yes/no,dir=yes/no
    * href=url specifies the url of the document to display
    * description=Description of view specifies a view specific description eg
    * HTML or PDF. If not specified then it defaults to the name specified in
    * the name-> line.
    * target=Target in anchor Specifies the target to use in the HTML anchor
    * eg a alphanumeric window name or one of _blank, _self, _parent or _top
    * search=yes/no Specify whether to create a search index for the document
    * specified by this view
    * dir=yes/no Specify a browseable directory, for example for browsing code
    * samples
    */
   private void processLeaf(Request request, String text, 
                            StringBuffer description)
   //----------------------------------------------------
   {      
      description.append(getLeafName(text));
      
      ArrayList<ViewInfo> views = new ArrayList<ViewInfo>();
      for (Iterator<ViewInfo> it = viewIterator(text); it.hasNext();)
      {
         ViewInfo vi = it.next();
         if (vi != null)
         {
            String href = vi.href;
            if (href != null)
            {               
               if (! href.trim().toLowerCase().startsWith("http://"))
               {
                  Request hrefReq = request.getChildRequest(href);
                  if ( (hrefReq.getName().toLowerCase().contains("dir.st")) || (hrefReq.exists()) )
                     href = hrefReq.getPath();
                  else
                  {
                     if (! hrefReq.exists())
                     {
                        vi.name = "<font color=\"red\">" + hrefReq.getAbsolutePath() + " Invalid" +
                                  "</font>";
                        href = "/dev/null";
                        logger.error(hrefReq.getAbsolutePath() + " does not exist");
                     }
                     else
                        href = null;
                  }
                  vi.href = href;
               }
            }
            String name = vi.name;
            if (name == null)
               name = m_description;
            if (vi.href != null)
               views.add(vi);
         }
      }
      m_views = views.toArray(m_views);      
   }
   
   public NodeEntry(Request request, DirItemInterface dir)
          throws FileNotFoundException 
   //-----------------------------------------------------
   {
      m_dir = dir;
      Request dirRequest = null;
      if (request.isDirectory())
      {
         dirRequest = request.getChildRequest(dir.getName());
         Request node = dirRequest.getChildRequest("NODE");
         type = NODE;
         if ( (node == null) || (! node.exists()) )
         {
            node = dirRequest.getChildRequest("LEAF");
            type = LEAF;
         }                        
         if ( (node == null) || (! node.exists()) )
         {
            m_description = dirRequest.getPath() + 
                            ": Invalid. Requires NODE or LEAF file";
            m_dir = null;
            //type = UNKNOWN;
            type = NODE;
            return;
         }
         StringBuffer sb = new StringBuffer();
         readNodeText(node.getStream(), sb);
         String text = sb.toString();
         if (type == NODE)
            m_description = text.replaceAll("\\^\\^\\^", "");
         else
         {
            StringBuffer description = new StringBuffer();
            processLeaf(dirRequest, text, description);
            m_description = description.toString();
         }
      }
      else
         throw new FileNotFoundException();
   }
   
   protected NodeEntry(Request request, String leafText)
   //------------------------------------------------
   {
      type = LEAF;
      StringBuffer description = new StringBuffer();
      processLeaf(request, leafText, description);      
      m_description = description.toString();
   }
   
   public String getPath()
   //-------------------
   {
      if (m_dir != null)
         return m_dir.getName();
      return "";
   }
   
   public String getDescription()
   //----------------------------
   {
      return m_description;
   }
    
   public boolean isNode() { return type == NODE; }
   
   public boolean isLeaf() { return type == LEAF; }
 
   public boolean isInjar() { return (DoxMentor4J.getApp().getArchiveFile() != null); }
   
   public ViewInfo[] getViews() { return m_views; }
   
   static public class ViewInfo
   //---------------------------
   {
      public String name;
      public String href;
      public String target = null;
      public String search = null;
      public boolean indexable = false;
      public boolean dir = false;
      public boolean valid = true;      
      
      @SuppressWarnings("deprecation")
      public ViewInfo(String name, String href, String target, 
                      boolean isIndexable, boolean isDir, String search)
      //----------------------------------------------------------------
      {
         this.name = name;
         if (href == null)
         {   
            this.href = "";
            this.valid = false;
         }
         else
            this.href = href;
         this.search = search;
         this.target = target;
         this.indexable = isIndexable;
         this.dir = isDir;
         if ( (this.dir) && (href != null) )
         {
            try
            {
               this.href = "/dir.st?" + URLEncoder.encode("dir", "UTF-8") + "=" + 
                       URLEncoder.encode(this.href, "UTF-8") + "&" + 
                       URLEncoder.encode("name", "UTF-8") + "=" + 
                       URLEncoder.encode(this.name, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
               this.href = "/dir.st?" + URLEncoder.encode("dir") + "=" + 
                       URLEncoder.encode(this.href) + "&" + 
                       URLEncoder.encode("name") + "=" + 
                       URLEncoder.encode(this.name);
            }      
            catch (Exception e)
            {
               logger.error(e.getMessage(), e);
            }
         }
            
      }
      
      public boolean isTargetexists() 
      { 
         return ( (target != null) && (target.length() > 0) ); 
      }      
   }
   
   private DirItemInterface m_dir =  null;
   private String m_description = "";   
   private ViewInfo[] m_views = new ViewInfo[0];
   
}