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

import java.io.FileFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.homeip.donaldm.httpdbase4j.CloneableHeaders;
import net.homeip.donaldm.httpdbase4j.Http;
import net.homeip.donaldm.httpdbase4j.HttpResponse;
import net.homeip.donaldm.httpdbase4j.Httpd;
import net.homeip.donaldm.httpdbase4j.Postable;
import net.homeip.donaldm.httpdbase4j.Request;
import net.homeip.donaldm.doxmentor4j.indexers.IndexFactory;
import net.homeip.donaldm.doxmentor4j.indexers.Indexable;
import net.homeip.donaldm.doxmentor4j.indexers.SourceIndexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

public class AjaxIndexer implements Postable
//==========================================
{    
   /**
    * Executor for the entire indexing process. Only one allowed.
    */ 
   private ExecutorService                m_executor        = null;
   
   /**
    * Threadpool executor for individual directory index.
    */
   private ExecutorService                m_indexExecutor   = null;
   
   private Callable<Boolean>              m_thread          = null;
   private FutureTask<Boolean>            m_task            = null;   
   private volatile boolean               m_stop            = false;
   private volatile boolean               m_busy            = false;

   private ArrayList<DelayedIndexParams>  m_indexLater      = null;
   
   private Logger logger = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");         

   final protected int                    MAX_INDEX_THREADS = 3;
   
   static private AtomicLong              m_count           = null;
   
   private static final class SingletonHolder
   {
      static final AjaxIndexer singleton = new AjaxIndexer();      
   }

   public static AjaxIndexer getIndexer()
   //---------------------------------
   {
      return SingletonHolder.singleton;
   }   
   
   private AjaxIndexer() 
   //------------------
   {    
      m_executor = Executors.newSingleThreadExecutor(
                                       new DaemonThreadFactory("AjaxIndexer"));      
   }

   public boolean index()
   //--------------------
   {
      m_thread = new Callable<Boolean>()
      {
         public Boolean call()
         // ------------------
         {
            m_busy = true;
            m_stop = false;
            m_count = new AtomicLong(0);             
            m_indexExecutor = Executors.newFixedThreadPool(MAX_INDEX_THREADS,
                                    new DaemonThreadFactory("Indexer"));
            DoxMentor4J app = DoxMentor4J.getApp();
            Httpd httpd = app.getHttpd();
            java.io.File archiveFile = app.getArchiveFile();
            String archiveDirName = app.getArchiveDir();
            String indexDirName = app.getIndexDir();
            String archiveIndexDirName = app.getArchiveIndexDir();
            java.io.File homeDir = app.getHomeDir();            
            
            try
            {
               if (archiveFile != null)
                  IndexFactory.create(archiveFile, archiveIndexDirName, 
                                      indexDirName, true);
               else
                  IndexFactory.create(indexDirName, true);
            }
            catch (Exception e)
            {
               if (logger != null)
                  logger.error("Error creating index directory", e);
               else
                  e.printStackTrace(System.err);
               m_stop = m_busy = false;
               return false;
            }
                        
            if (archiveDirName != null)
            {
               File archiveDirectory = new File(archiveFile, archiveDirName);
               if ( (! archiveDirectory.exists()) || 
                    (   (! archiveDirectory.isDirectory()) && 
                        (! archiveDirectory.isArchive()) ) )
               {
                  m_stop = m_busy = false;
                  return false;
               }               
               if (! _index(archiveDirectory, archiveDirectory.getAbsolutePath()))
               {
                  m_stop = m_busy = false;
                  return false;
               }
            }
            else
               if (homeDir != null)
               {
                  if (! _index(homeDir, homeDir.getPath()))
                  {
                     m_stop = m_busy = false;
                     return false;
                  }
               }
               else
               {
                  m_stop = m_busy = false;
                  return false;
               }            
            
            m_indexExecutor.shutdown();
            try
            {        
               m_indexExecutor.awaitTermination(5, TimeUnit.MINUTES);
            }
            catch (Exception e)
            {
               try { m_indexExecutor.shutdownNow(); } catch (Exception ee) {}
            }                        
            
            if (m_indexLater != null)
            {               
               IndexFactory.closeWriter();
               
               m_indexExecutor = Executors.newFixedThreadPool(MAX_INDEX_THREADS,
                                    new DaemonThreadFactory("Indexer"));
               IndexFactory indexContainer = IndexFactory.getApp();
               SourceIndexer.setUserSourceAnalyzer(true);
               for (DelayedIndexParams p : m_indexLater)
               {
                  String href = p.href;
                  String ext = Utils.getExtension(href);                  
                  Indexable indexer = indexContainer.getIndexer(ext);
                  _indexDoc(indexer, href, p.fullHref, p.followLinks);
               }
               
               m_indexExecutor.shutdown();
               try
               {        
                  m_indexExecutor.awaitTermination(5, TimeUnit.MINUTES);
               }
               catch (Exception e)
               {
                  try { m_indexExecutor.shutdownNow(); } catch (Exception ee) {}
               }
               IndexFactory.optimizeWriter();               
               IndexFactory.closeWriter();               
            }
            else
            {
               IndexFactory.optimizeWriter();
               IndexFactory.closeWriter();
            }
            
            m_stop = m_busy = false;
            return true;
         }
      };
      m_task = new FutureTask<Boolean>(m_thread);
      try
      {
         m_executor.submit(m_task);
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error(e.getMessage(), e);
         else
            e.printStackTrace(System.err);
         return false;
      }
      return true;
   }
           
   protected boolean stop(int timeout)
   //---------------------------------
   {
      m_stop = true;
      if ( (m_task != null) && (! m_task.isDone()) )      
         m_task.cancel(true);
      Boolean b;
      try
      {
         b = m_task.get(timeout, TimeUnit.MILLISECONDS);
      }
      catch (Exception e)
      {
         b = false;
      }
      return b;      
   }
   
   private boolean _index(java.io.File dir, String home)
   //---------------------------------------------------
   {
      File leaf = new File(dir, "LEAF");
      boolean isArchive = (dir instanceof de.schlichtherle.io.File);
      if (leaf.exists())
      {
         FileInputStream fis = null;
         try
         {
            fis = new FileInputStream(leaf);
            processLeaf(fis, leaf.getAbsolutePath(), home, isArchive);
         }
         catch (Exception e)
         {
            if (logger != null)
               logger.error("Exception indexing  " + leaf.getAbsolutePath(), e);
            else
               e.printStackTrace(System.err);
         }
      }
      else
      {         
         java.io.File[] dirs = null;
         if (isArchive)
         { 
            dirs = dir.listFiles(new FileFilter() 
            {
               public boolean accept(java.io.File f)
               {
                  File file = (File) f;
                  return ( (file.isDirectory()) || (file.isArchive()) );
               }
            });
         }
         else
            if (dir instanceof java.io.File)
            {
               dirs = dir.listFiles(new FileFilter() 
               {
                  public boolean accept(java.io.File f)
                  {
                     return f.isDirectory();
                  }
               });
            }
            else
               return false;
            
         for (int i=0; i<dirs.length; i++)
         {
            if (m_stop) return false;
            if (! _index(dirs[i], home))
               return false;         
         }
      }
      return true;
   }
   
   private void processLeaf(InputStream is, String fullPath, String homePath,
                            boolean isJar)
   //------------------------------------------------------------------------
   {
      StringBuffer text = new StringBuffer();
      if (NodeEntry.readNodeText(is, text))
      {         
         Iterator<NodeEntry.ViewInfo> it;
         for (it=NodeEntry.viewIterator(text.toString());
              it.hasNext();)
         {
            if (m_stop) return;
            NodeEntry.ViewInfo vi = it.next();
            if ( (vi != null) && (vi.indexable) )
               _indexView(vi, fullPath, homePath, isJar);
         }
      }
   }
   
   static private Pattern m_listPattern = 
                                    Pattern.compile("\\[[ \t]*(.+)[ \t]*\\]");
   
   private void _indexView(NodeEntry.ViewInfo vi, String fullPath, 
                           String homePath, boolean isJar)
   //---------------------------------------------------------------
   {
      String href = vi.href.trim();
      if ( (href != null) &&
              (href.toLowerCase().startsWith("http://")) )
      {
         if (logger != null)
            logger.error("Cannot index remote file" + href);
         else
            System.err.println("Cannot index remote file" + href);         
         return;
      }
      int p;
      String path = href;
      if ( (! href.startsWith("/")) || (href.indexOf("dir.st") >= 0) )
      {
         p = fullPath.indexOf(homePath);
         if (p >= 0)
            path = fullPath.substring(p+homePath.length());
         else
            path = "/";
         p = path.toUpperCase().indexOf("LEAF");
         if (p >= 0)
            path = path.substring(0, p);
         if (href.indexOf("dir.st") >= 0)
            href = path + ((path.endsWith("/")) ? "" : "/");
         else
            href = path + ((path.endsWith("/")) ? "" : "/") + href;
      }
      else
      {
         p = href.lastIndexOf("/");
         path = href.substring(0, p+1);
      }
      p = href.indexOf(homePath);
      int q = href.lastIndexOf('/');
      String fullHref = href;
      if (p < 0)
      {
         fullHref = homePath +
                 (((homePath.endsWith("/")) || (href.startsWith("/")))
                 ? "" : "/") + href;
         if (q >= 0)
            fullPath = homePath +
                       (((homePath.endsWith("/")) || (href.startsWith("/")))
                           ? "" : "/") + href.substring(0, q);
         else
            fullPath = homePath;
      }

      String ext = Utils.getExtension(href);
      IndexFactory indexContainer = IndexFactory.getApp();
      Indexable indexer = indexContainer.getIndexer(ext);
      try
      {
         if (indexer != null)
         {
            boolean followLinks = (vi.search.compareToIgnoreCase("links") == 0);
            if (indexer instanceof SourceIndexer)
            {
               if (m_indexLater == null)
                  m_indexLater = new ArrayList<DelayedIndexParams>();
               m_indexLater.add(new DelayedIndexParams(href, fullHref, 
                                                       followLinks));               
            }
            else
               _indexDoc(indexer, href, fullHref, followLinks);
         }
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error(e.getMessage(), e);
         else
            e.printStackTrace(System.err);
         return;
      }
      String fileList = "";
      System.out.println(vi.search);
      // Handle list of files to be indexed inside [ ] square brackets or
      // wildcards.
      if (! vi.search.toLowerCase().matches("yes|y|no|n|links")) 
      {         
         Matcher matcher = m_listPattern.matcher(vi.search);
         if ( (matcher.matches()) && (matcher.groupCount() >= 1) )
            fileList = matcher.group(1);
         else 
            fileList = vi.search;
         StringTokenizer tok = new StringTokenizer(fileList);
         String s;
         while (tok.hasMoreTokens())
         {
            href = tok.nextToken().trim();
            if (href.startsWith("/")) continue;
            String[] hrefs;
            if ( (href.indexOf("*") >= 0) || (href.indexOf("?") >= 0) )
               hrefs = _wildcardFiles(fullPath, href, isJar);
            else
            {
               hrefs = new String[1];
               hrefs[0] = href;
            }
            for (int i=0; i<hrefs.length; i++)
            {
               href = hrefs[i];
               fullHref = fullPath + "/" + href;
               href = path + 
                    ( ( (! path.endsWith("/")) && (! hrefs[i].startsWith("/")) )
                       ? "/" : "") + hrefs[i];               
               ext = Utils.getExtension(href);
               indexer = indexContainer.getIndexer(ext);
               if (indexer != null)
               {
                  if (indexer instanceof SourceIndexer)
                  {
                     if (m_indexLater == null)
                        m_indexLater = new ArrayList<DelayedIndexParams>();
                     m_indexLater.add(new DelayedIndexParams(href, fullHref, 
                                                             false));
                  }
                  else                        
                     _indexDoc(indexer, href, fullHref, false);
               }
            }
         }
      }
         
      
   }
   
   public static void incrementCount()
   //--------------------------------------
   {
      m_count.incrementAndGet();
   }       
   
   public static void addCount(long n)
   //---------------------------------
   {
      m_count.addAndGet(n);
   }
   
   private void _indexDoc(final Indexable indexer, final String href, 
                             final String fullPath, final boolean followLinks)
   //------------------------------------------------------------------
   {
      try
      {
         m_indexExecutor.execute(new Runnable() 
         {
            public void run()
            {
               try
               {
                  indexer.index(href, fullPath, followLinks);
               }
               catch (Exception e)
               {  
                  if (logger != null)
                     logger.error("Indexing error", e);
                  else
                     e.printStackTrace(System.err);
               }
            }
         });
      }
      catch (Exception e)
      {
         if (logger != null)
            logger.error(e.getMessage(), e);
         else
            e.printStackTrace(System.err);
      }
   }
   
   private String[] _wildcardFiles(String fullPath, String href, boolean isJar)
   //--------------------------------------------------------------------------
   {
      ArrayList<String> hrefs = new ArrayList<String>();         
      String wildCard;
      int p = href.lastIndexOf("/");
      if ( (p++ >= 0) && (p < href.length()) )
         wildCard = href.substring(p);
      else
         wildCard = href;

      Pattern pattern = Pattern.compile(Utils.wildcardToRegex(wildCard), 
                                            Pattern.CASE_INSENSITIVE);          
      File f = new File(fullPath);
      if (isJar)
      {
         if ( (! f.exists()) || 
              ( (! f.isDirectory()) && (! f.isArchive() ) ) )
            return new String[0];
      }
      else
         if ( (! f.exists()) || (! f.isDirectory()) )
            return new String[0];
      String[] files = f.list();
      for (int i=0; i<files.length; i++)
      {
         String name = files[i];
         Matcher matcher = pattern.matcher(name);
         if (matcher.matches())
            hrefs.add(name);                
      }

      String[] a = new String[0];
      return hrefs.toArray(a);
   }
   
   public Object onHandlePost(long id, HttpExchange ex, Request request,
                              HttpResponse r, java.io.File dir, 
                              Object... extraParameters)
   //-------------------------------------------------------------------
   {
      String html;
      CloneableHeaders postHeaders = request.getPOSTParameters();
      if (postHeaders.containsKey("start"))
      {
         if (m_busy)
            html = "Indexing already in progress";
         else
         {
            if (! index())
               html = "<b> Indexing failed </b>";
            else
               html = "<b> Indexing Started </b>";
         }   
      }
      else
      {   
         if (m_busy)
            html = "<b>" + m_count + "</b> files indexed";            
         else
         {
            boolean b = false;
            try { b = m_task.get(); } catch (Exception e) {}
            if (b)
               html = "Complete. <b>" + m_count + "</b> files indexed";
            else
               html = "Errors occurred. <b>" + m_count + "</b> files indexed";
         }
      }
      r.setBody(html);
      r.setMimeType(Http.MIME_HTML);
      r.addHeader("Cache-Control", "no-cache");
      r.addHeader("Content-Length", Integer.toString(html.length()));
      return r;
   }   

   public class DaemonThreadFactory implements ThreadFactory
   //========================================================
   {
      String name = "Indexer";
      
      public DaemonThreadFactory(String name) { this.name = name; }
      
      
      public Thread newThread(Runnable r)
      {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(name);
        return t;
      }
   }
   
   private class DelayedIndexParams
   {
      public String href, fullHref;
      public boolean followLinks;
      
      public DelayedIndexParams(String href, String fullHref, boolean followLinks)
      {
         this.href = href;
         this.fullHref = fullHref;
         this.followLinks = followLinks;
      }
   }
}
