package net.homeip.donaldm.doxmentor4j;

import de.schlichtherle.io.DefaultArchiveDetector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Callable;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.homeip.donaldm.doxmentor4j.indexers.IndexFactory;
import net.homeip.donaldm.doxmentor4j.indexers.SourceIndexer;
import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;

/**
 *
 * @author Donald Munro
 */
public class IndexThread implements Callable<Boolean>
//===================================================
{
   final static private Logger logger = LoggerFactory.getLogger(IndexThread.class);
   final static private int    NO_INDEX_THREADS = Runtime.getRuntime().availableProcessors() + 1;
   final static private Pattern LIST_PATTERN = Pattern.compile("\\[[ \t]*(.+)[ \t]*\\]");
   final static IndexFactory INDEXER_FACTORY = IndexFactory.getApp();

   private LinkedBlockingQueue<Runnable>  m_indexQueue      = null;
   private ExecutorService                m_indexExecutor   = null;
   private volatile boolean               stop              = false;
   private volatile boolean               busy              = false;
   
   final private List<DelayedIndexParams> m_indexLater      =
                                  Collections.synchronizedList(new ArrayList<DelayedIndexParams>());
   final private List<NamedFuture<Long>> m_futures = Collections.synchronizedList(new ArrayList<NamedFuture<Long>>());

   public boolean isBusy() { return busy; }

   public boolean isStop() { return stop; }

   public void setStop(boolean stop) { this.stop = stop; }
   
   @Override
   public Boolean call()
   // ------------------
   {
      busy = true;
      stop = false;
      m_indexQueue      = new LinkedBlockingQueue<Runnable>();
      m_indexExecutor = new ThreadPoolExecutor(NO_INDEX_THREADS, NO_INDEX_THREADS, 0L, TimeUnit.MILLISECONDS,
    		                                   m_indexQueue, new ThreadOptions("Indexer")); 
    	               //Executors.newFixedThreadPool(NO_INDEX_THREADS, new ThreadOptions("Indexer"));
      DoxMentor4J app = DoxMentor4J.getApp();
      java.io.File archiveFile = app.getArchiveFile();
      String archiveDirName = app.getArchiveDir();
      String indexDirName = app.getIndexDir();
      String archiveIndexDirName = app.getArchiveIndexDir();
      java.io.File homeDir = app.getHomeDir();

      try
      {
         if (archiveFile != null)
            IndexFactory.create(archiveFile, archiveIndexDirName, indexDirName, true, false);
         else
            IndexFactory.create(indexDirName, true, false);
      }
      catch (Exception e)
      {
         logger.error("Error creating index directory", e);
         stop = busy = false;
         return false;
      }

      if (archiveDirName != null)
      {
         File archiveDirectory = new File(archiveFile, archiveDirName);
         if ( (! archiveDirectory.exists()) ||
              (   (! archiveDirectory.isDirectory()) &&
                  (! archiveDirectory.isArchive()) ) )
         {
            stop = busy = false;
            return false;
         }
         // Recursively index archive
         final String home = archiveDirectory.getAbsoluteFile().toURI().toASCIIString();
         if (! _index(archiveDirectory, home))
         {
            stop = busy = false;
            return false;
         }
      }
      else
         if (homeDir != null)
         {
            // Recursively index filesystem
            final String home = homeDir.toURI().toASCIIString();
            if (! _index(homeDir, home))
            {
               stop = busy = false;
               return false;
            }
         }
         else
         {
            stop = busy = false;
            return false;
         }

      boolean ret = waitForThreads();

      // Delayed all SourceIndexer derived indexers so stop word based analyzer can be used in the
      // IndexWriter
      if ( (ret) && (m_indexLater != null) && (m_indexLater.size() > 0) )
      {
         IndexFactory.closeWriter();
         SourceIndexer.USE_SOURCE_ANALYZER = true;
         synchronized(m_indexLater)
         {
            for (DelayedIndexParams p : m_indexLater)
            {
               if (stop) break;
               String href = p.href;
               String ext = Utils.getExtension(href);
               Indexable indexer = INDEXER_FACTORY.getIndexer(ext);
               _indexDoc(indexer, href, p.fullHref, p.followLinks);
            }
         }

         ret = waitForThreads();
         IndexFactory.optimizeWriter();
         IndexFactory.closeWriter();
         IndexFactory.closeDirectory();
      }
      else
      {
         IndexFactory.optimizeWriter();
         IndexFactory.closeWriter();
         IndexFactory.closeDirectory();
      }

      stop = busy = false;
      return ret;
   }

   private boolean _index(final java.io.File dir, final String home)
   //---------------------------------------------------------
   {
      File leaf = new File(dir, "LEAF");
      // dir can be a java.io.File or a de.schlichtherle.io.File
      boolean isArchive = (dir instanceof de.schlichtherle.io.File);
      if (leaf.exists())
      {
         FileInputStream fis = null;
         try
         {
            fis = new FileInputStream(leaf);
            final String leafName = leaf.getAbsoluteFile().toURI().toASCIIString();
            processLeaf(fis, leafName, home, isArchive);
         }
         catch (Exception e)
         {
            logger.error("Exception indexing  " + leaf.getAbsolutePath(), e);
         }
      }
      else
      {
         java.io.File[] dirs = null;
         if (isArchive)
         {
            dirs = dir.listFiles(new FileFilter()
            {
               @Override public boolean accept(java.io.File f)
               //---------------------------------------------
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
                  @Override public boolean accept(java.io.File f) { return f.isDirectory(); }
               });
            }
            else
               return false;

         for (int i=0; i<dirs.length; i++)
         {
            if (stop) return false;
            if (! _index(dirs[i], home))
               return false;
         }
      }
      return true;
   }

   private void processLeaf(InputStream is, String fullPath, String homePath, boolean isJar)
   //---------------------------------------------------------------------------------------
   {
      StringBuffer text = new StringBuffer();
      java.io.File f = null;
      try
      {
         if (AjaxIndexer.URL_PATTERN.matcher(fullPath).matches())
            f = new java.io.File(new URI(fullPath));
         else
            f = new java.io.File(fullPath);
      }
      catch (Exception e)
      {
         logger.error("", e);
         return;
      }
      if (f.getName().equalsIgnoreCase("leaf"))
         f = f.getParentFile();
      if (f == null)
         f = new File("/");
      String relPath = Utils.getRelativePath(homePath, f.getAbsoluteFile().toURI().toASCIIString());
      if (NodeEntry.readNodeText(is, text))
      {
         Iterator<NodeEntry.ViewInfo> it;
         for (it=NodeEntry.viewIterator(text.toString(), relPath); it.hasNext();)
         {
            if (stop) return;
            NodeEntry.ViewInfo vi = it.next();
            if ( (vi != null) && (vi.indexable) )
               _indexView(vi, fullPath, homePath, isJar);
         }
      }
   }

   private void _indexView(NodeEntry.ViewInfo vi, String fullPathName, String homePath, boolean isJar)
   //------------------------------------------------------------------------------------------------
   {
      if ( (vi == null) || (vi.href == null) )
         return;      
      String href = vi.href.trim();
      int p;
      StringBuilder sb = new StringBuilder(href);
      String path = getIndexPath(homePath, fullPathName, sb);
      if (path == null) return;
      href = sb.toString();

      sb.setLength(0); sb.trimToSize();
      sb.append(fullPathName);
      String fullHref = getFullHref(href, homePath, sb);
      fullPathName = sb.toString();
      File fullPath = new File(fullPathName);

      Indexable indexer = null;
      String ext = Utils.getExtension(href);
      if ( (ext != null) && (! ext.isEmpty()) )
         indexer = INDEXER_FACTORY.getIndexer(ext);
      if (indexer != null)
      {
         try
         {
            boolean followLinks = (vi.search.compareToIgnoreCase("links") == 0);
            if (indexer instanceof SourceIndexer)
               m_indexLater.add(new DelayedIndexParams(href, fullHref, followLinks));
            else
               _indexDoc(indexer, href, fullHref, followLinks);
         }
         catch (Exception e)
         {
            logger.error(e.getMessage(), e);
            return;
         }
      }
      
      String fileList = "";
      // Handle list of files to be indexed inside [ ] square brackets or
      // wildcards.
      if (! vi.search.toLowerCase().matches("yes|y|no|n|links"))
      {
         Matcher matcher = LIST_PATTERN.matcher(vi.search);
         if ( (matcher.matches()) && (matcher.groupCount() >= 1) )
            fileList = matcher.group(1);
         else
            fileList = vi.search;
         StringTokenizer tok = new StringTokenizer(fileList);
         String s;
         while (tok.hasMoreTokens())
         {
            if (stop) return;
            href = tok.nextToken().trim();
            if (href.startsWith("/")) continue;
            indexFiles(path, fullPathName, homePath, href, isJar);
         }
      }
   }
   
   private void _indexDoc(final Indexable indexer, final String href, final String fullPath,
                          final boolean followLinks)
   //----------------------------------------------------------------------------------------
   {      
      Future<Long> f = null;
      try
      {
         f = m_indexExecutor.submit(new Callable<Long>()
         {
            @Override
            public Long call() throws Exception
            //---------------
            {
               URI uri = null;
               try
               {
                  Matcher m = AjaxIndexer.URL_PATTERN.matcher(fullPath);
                  String suri;
                  if (! m.matches())
                     suri = "file://" + Utils.canonizeFile(new java.io.File(fullPath)).getAbsolutePath();
                  else
                     suri = fullPath;
                  try
                  {
                     uri = new URI(suri);
                  }
                  catch (URISyntaxException e)
                  {
                     try { uri = new URI("file://" + fullPath); } catch (URISyntaxException _e) { logger.error("", suri); uri = null; }
                  }
                  if (uri != null)
                     return indexer.index(href, uri, followLinks);
                  else
                  {
                     logger.error("Invalid uri {}", fullPath);
                     return -1L;
                  }
               }
               catch (Exception ee)
               {
                  logger.error("Indexing error", ee);
                  return -1L;
               }
               finally
               {
                  Thread.currentThread().getId();
               }
            }
         });
      }
      catch (Exception e)
      {
         logger.error(e.getMessage(), e);
         f = null;
      }
      finally
      {
         NamedFuture<Long> future = new NamedFuture<Long>(f, href);
         if (future != null)
            m_futures.add(future);
      }
   }

   private File[] _wildcardFiles(String fullPath, String href, boolean isJar)
   //--------------------------------------------------------------------------
   {
      ArrayList<String> hrefs = new ArrayList<String>();
      String wildCard;
      int p = href.lastIndexOf("/");
      if ( (p++ >= 0) && (p < href.length()) )
         wildCard = href.substring(p);
      else
         wildCard = href;

      final Pattern pattern = Pattern.compile(Utils.wildcardToRegex(wildCard), Pattern.CASE_INSENSITIVE);
      File f = new File(fullPath);
      if (isJar)
      {
         if ( (! f.exists()) ||
              ( (! f.isDirectory()) && (! f.isArchive() ) ) )
            return new File[0];
      }
      else
         if ( (! f.exists()) || (! f.isDirectory()) )
            return new File[0];
      File[] files = f.listFiles(new FilenameFilter()
      {
         @Override
         public boolean accept(java.io.File dir, String name)
         {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) return true;
            dir = new File(dir, name);
            if (! dir.isDirectory()) return false;
            File ff = new File(dir, "LEAF");
            if (ff.exists()) return false;
            ff = new File(dir, "NODE");
            if (ff.exists()) return false;
            return true;
         }
      }, new DefaultArchiveDetector("zip"));
      return files;
   }

   private void indexFiles(final String path, final String fullPathName, final String homePath,
                           final String href, final boolean isJar)
   //-----------------------------------------------------------------------------------------------
   {
      File[] files;
      String newHref, newFullHref;
      if ( (href.indexOf("*") >= 0) || (href.indexOf("?") >= 0) )
         files = _wildcardFiles(fullPathName, href, isJar);
      else
      {
         files = new File[1];
         files[0] = new File(href);
      }
      for (File f : files)
      {
         if (f.isDirectory())
         {
            StringBuilder sb = new StringBuilder(href);
            String newPath = getIndexPath(homePath, f.getPath(), sb);
            if (newPath == null) continue;
            int p = href.lastIndexOf("/");
            String wildCard;
            if ( (p++ >= 0) && (p < href.length()) )
               wildCard = href.substring(p);
            else
               wildCard = href;
            newHref = f.getPath() + "/" +  wildCard;
            sb.setLength(0); sb.trimToSize();
            sb.append(f.getPath());
            newFullHref = getFullHref(newHref, homePath, sb);
            indexFiles(newPath, f.getPath(), homePath, newHref, isJar);
            logger.info("indexFiles({}, {}, {}, {}, {})",
                        new Object[] { newPath, f.getPath(), homePath, newHref, isJar} );
         }
         else
         {
            newHref = f.getName();
            newFullHref = fullPathName + "/" + newHref;
            newHref = path +
                 ( ( (! path.endsWith("/")) && (! f.getPath().startsWith("/")) )
                    ? "/" : "") + f.getName();
            String ext = Utils.getExtension(newHref);

            Indexable indexer = INDEXER_FACTORY.getIndexer(ext);
            if (indexer != null)
            {
               if (indexer instanceof SourceIndexer)
                  m_indexLater.add(new DelayedIndexParams(newHref, newFullHref, false));
               else
                  _indexDoc(indexer, newHref, newFullHref, false);
            }
         }
      }
   }

   private String getIndexPath(String homePath, String fullPathName, StringBuilder hrefBuf)
   //--------------------------------------------------------------------------------------
   {
      String href = hrefBuf.toString();
      String path = href;
      Matcher matcher = AjaxIndexer.URL_PATTERN.matcher(href);
      if (matcher.matches())
         return href;
      
      int p;
      if ( (! href.startsWith("/")) || (href.indexOf("dir.st") >= 0) )
      {
         path = Utils.getRelativePath(homePath, fullPathName);
         p = path.toUpperCase().indexOf("LEAF");
         if (p >= 0)
            path = path.substring(0, p);
         if (href.indexOf("dir.st") >= 0)
            href = path + ((path.endsWith("/")) ? "" : "/");
         else
            href = path + ((path.endsWith("/")) ? "" : "/") + href;
         hrefBuf.setLength(0); hrefBuf.trimToSize();
         hrefBuf.append(href);
      }
      else
      {
         p = href.lastIndexOf("/");
         path = href.substring(0, p+1);
      }
      return path;
   }

   private String getFullHref(String href, String homePath, StringBuilder sb)
   //-------------------------------------------------------------------------
   {
      Matcher matcher = AjaxIndexer.URL_PATTERN.matcher(href);
      if (matcher.matches())
         return href;
      int p = href.indexOf(homePath);
      int q = href.lastIndexOf('/');
      String fullHref = href, fullPathName = sb.toString();
      if (p < 0)
      {
         fullHref = homePath +
                 (((homePath.endsWith("/")) || (href.startsWith("/")))
                 ? "" : "/") + href;
         if (q >= 0)
            fullPathName = homePath +
                       (((homePath.endsWith("/")) || (href.startsWith("/")))
                           ? "" : "/") + href.substring(0, q);
         else
            fullPathName = homePath;
         sb.setLength(0); sb.trimToSize();
         sb.append(fullPathName);
      }
      return fullHref;
   }

   private boolean waitForThreads()
   //------------------------------
   {
      ArrayList<Future> finished = new ArrayList<Future>();
      while ( ( (m_futures.size() > 0) || (m_indexQueue.size() > 0) ) && (! stop) )
      {
         try
         {
            synchronized(m_futures)
            {
               for (NamedFuture<Long> future : m_futures)
               {
                  if (stop) break;
                  if (! future.isDone())
                  {
                     Long status = null;
                     try { status = future.get(500, TimeUnit.MILLISECONDS); } catch (Exception _e) { status = null; }
                     if ( (status != null) && (status == -1L) )
                        logger.error("Indexing failed for " + future.getName());
                  }
                  else
                     finished.add(future);
               }
            }
            m_futures.removeAll(finished);
            finished.clear();
         }
         catch (Exception ee)
         {
            logger.error("", ee);
         }
      }
      if (stop)
      {
         m_indexExecutor.shutdown();
         try { m_indexExecutor.awaitTermination(1, TimeUnit.MINUTES); } catch (Exception _e) {}
         stop = false;
         try { m_indexExecutor.shutdownNow(); } catch (Exception _e) { logger.error("", _e);  }
         return false;
      }
      return true;
   }

   private class DelayedIndexParams
   //==============================
   {
      public String href, fullHref;
      public boolean followLinks;

      public DelayedIndexParams(String href, String fullHref, boolean followLinks)
      //--------------------------------------------------------------------------
      {
         this.href = href;
         this.fullHref = fullHref;
         this.followLinks = followLinks;
      }
   }

   class NamedFuture<T> implements Future<T>
   //========================================
   {
      String name;
      Future<T> f;

      public NamedFuture(Future<T> f, String name)
      {
         this.f = f;
         this.name = name;
      }

      public String getName() { return name; }

      public void setName(String name) { this.name = name; }

      @Override public boolean cancel(boolean b) { return f.cancel(b); }

      @Override public boolean isCancelled() {return f.isCancelled();  }

      @Override public boolean isDone() { return f.isDone();  }

      @Override
      public T get() throws InterruptedException, ExecutionException
      {
         return f.get();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                                                       TimeoutException
      {
         return f.get(timeout, unit);
      }

      @Override
      public String toString()
      //----------------------
      {
         return "NamedFuture{" + "name=" + name  + ", Done = " + f.isDone() +
                ", Cancelled = " + f.isCancelled() + '}';
      }
   }
}
