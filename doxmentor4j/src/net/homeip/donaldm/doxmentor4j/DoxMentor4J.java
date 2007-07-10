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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import net.homeip.donaldm.httpdbase4j.FileHttpd;
import net.homeip.donaldm.httpdbase4j.FileStringTemplateHandler;
import net.homeip.donaldm.httpdbase4j.Httpd;
import net.homeip.donaldm.httpdbase4j.ArchiveHttpd;
import net.homeip.donaldm.httpdbase4j.ArchiveStringTemplateHandler;
import net.homeip.donaldm.httpdbase4j.StringTemplateHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import de.schlichtherle.io.File;

public class DoxMentor4J
//======================
{

   private static final String               VERSION = "0.1";
   
   public static final String                EOL = System.getProperty("line.separator");
   
   protected static Thread                   m_mainThread = null;
   
   private Httpd                             m_httpd = null;

   private java.io.File                      m_homeDir = null;

   private java.io.File                      m_templateDir = null;

   private java.io.File                      m_archiveFile = null;

   private File                              m_archivePath = null;

   private String                            m_archiveDirName = null;

   private int                               m_port = 8088;

   private String                            m_searchIndexDir = null;
   
   private String                            m_archiveSearchIndexDir = null;

   private boolean                           m_isSearchable = false;

   private boolean                           m_isIndexable = false;

   private String                            m_chmExtractor = null;

   private String                            m_chmArgs = null;

   private String                            m_djvuExtractor = null;

   private String                            m_djvuArgs = null;

   private File                              m_logDir = null;

   final static private String               m_defaultHome = 
                                    "resources" + File.separatorChar + "htdocs";
   
   final static private String               m_defaultIndex = 
                                    "resources" + File.separatorChar + "index";
   
   final static private String instructions = 
"Arguments:" +  EOL + 
"==========" +  EOL + 
"DoxMentor4J config-file" + EOL + 
"DoxMentor4J -h or --help" + EOL + 
"config-file Specify the configuration properties file. " + EOL + 
"            Default ./doxmentor4j.properties" + EOL + 
"-h or --help                           Display this help screen" + EOL + 
"Configuration File" + EOL +
"==================" + EOL +
"port=http-port   TCP port for standard HTTP (default 80)" + EOL +
"home=homedir     File system home directory containng docs " + EOL +
"archivefile=file Separate .jar, .zip, .tar, .tar.gz or .tar.bz2  file " + EOL + 
"                 containing docs." + EOL +
"archivedir=dir   Directory in archive  containg docs. dir is a directory"+ EOL +
"                 in a jar in the classpath or in the archive specified in " + EOL + 
"                 archivedir=. Default resources/htdocs" + EOL +
"search=yes|no    If yes then a search bar will be displayed enabling a " +
"                 full text search of those documents marked as being " + EOL + 
"                 searchable in their LEAF file. The Lucene jar file " + EOL +
"                 (lucene-core.jar) must be in the class path" + EOL + 
"index=yes|no     If yes then reindexing of the search index is allowed by" + EOL + 
"                 pressing a re-index button" + EOL + 
"indexdir=dir     Specifies the directory where the Lucene search index is stored." + EOL +
"archiveindexdir  Specifies the directory within the archive where the Lucene " + EOL +
"                 search index is stored." + EOL +
"chmext=path      Specifies the full path to the executable to be used to extract html files " + EOL +
"                 from a .chm file. If not specified then the CHMIndexer constructor will " + EOL + 
"                 attempt to find such an executable (On Windows it will attempt to find " + EOL +
"                 hh.exe and in Linux it will search for chmdump, chmextract and extract_chmLib)" + EOL +
"chmargs=args     Specifies the arguments to send to the chmext executable specified above" + EOL + 
"                 Use the following substitution strings: %s = source file, %d = destination file" + EOL + 
"                 %D = destination directory." + EOL +            
"djvuext=path     Specifies the full path to the executable to be used to extract text files " + EOL +
"                 from a .djvu file. If not specified then the DjvuIndexer constructor will " + EOL + 
"                 attempt to find such an executable (On Windows it will attempt to find " + EOL +
"                 djvutxt.exe and in Linux it will search for djvutxt)" + EOL +
"djvuargs=args    Specifies the arguments to send to the djvuext executable specified above" + EOL + 
"                 Use the following substitution strings: %s = source file, %d = destination file" + EOL + 
"                 %D = destination directory." + EOL +     
"logdir=dir       Directory to write log files to. Default log in current directory of application." + EOL + 
"Directory Structure" + EOL + "=================" + EOL + 
"Directories in the DoxMentor4J web server home directory must have the following structure" + EOL + 
"The documentation directories are defined to be located under library in the server home." + EOL + 
"Directories are either nodes or leaves. Nodes contain other directories but do not contain " + EOL + 
"content. Leaves contain the content and may contain sub-directories with content which do" + EOL + 
"not need to be identified as a node or a leaf." + EOL + 
"To Identify a directory as a node it must contain a file titled NODE. The contents of the file" + EOL + 
"should be a single line of text identifying the node which will be displayed in the tree on " + EOL + 
"the client browser." + EOL + 
"To identify as directory as a leaf it should contain a file titled LEAF with the following contents:" + EOL + 
"name->Name of the leaf which is displayed in the browser tree " + EOL + 
"One or more view directives where the format is:" + EOL + 
"view->href=\"content anchor\",description=\"description\",search=yes|no|[list]|links" + EOL + 
"href specifies the link to the content document (which can be any format for which your" +
"browser has a mime type mapping to an application although currently only txt,html,pdf," + EOL + 
"djvu and chm can be indexed (specified using using search). The href should prefarably" + EOL + 
"be relative to the current (leaf) directory. eg view->href=html/index.htm" + EOL + 
"The description is the text which will be displayed in the browser tree for this view" + EOL + 
"search specifies whether the document should be indexed for a full text search using Lucene" + EOL +
"If yes or y then the document specified in href will be indexed (if the doucment type is " +
"supported).  If no then the documents will not be searchable. " + EOL + 
"If search=links and the document is an html file then an attempt will be made to " + EOL + 
"recursively index all documents that are linked to the first document specified" + EOL + 
"in href (including other non-html but supported types)." + EOL + 
"If search=[list of space separated files] eg " + EOL + 
"   search=[book.html api.html] " + EOL + 
"or search=[wildcard] eg search=[html/*.html]" + EOL +
"then the files referenced in the list will be indexed for searching." + EOL + 
"The Lucene jar file (lucene-core.jar) must be in the class path if using search";
   
   private static final class SingletonHolder
   {
      static final DoxMentor4J singleton = new DoxMentor4J();      
   }

   public static DoxMentor4J getApp()
   //---------------------------------
   {
      return SingletonHolder.singleton;
   }   

   static public String getVersion() { return VERSION; }

   public boolean isSearchable() { return m_isSearchable; }
   
   public boolean isIndexable() { return m_isIndexable; }
   
   public String getIndexDir() { return m_searchIndexDir; }
   
   public String getArchiveIndexDir() { return m_archiveSearchIndexDir; }
   
   public void setIndexDir(String searchIndexDir) { m_searchIndexDir = searchIndexDir; }
   
   public void setArchiveIndexDir(String searchIndexDir) 
   //---------------------------------------------------
   { 
      m_archiveSearchIndexDir = searchIndexDir; 
   }
   
   private DoxMentor4J()
   //-------------------
   {            
   }
   
   public java.io.File getHomeDir()  { return m_homeDir; }

   public Httpd getHttpd() { return m_httpd; } 
   
   protected void setHomeDir(java.io.File homeDir)
   //-------------------------------------
   {
      this.m_homeDir = homeDir;
   }

   public java.io.File getTemplateDir()  { return m_templateDir; }

   protected void setTemplateDir(java.io.File templateDir)
   //---------------------------------------------
   {
      this.m_templateDir = templateDir;
   }

   public java.io.File getArchiveFile()  { return m_archiveFile;  }

   protected void setArchiveFile(java.io.File archiveFile) 
   //---------------------------------------------------------
   { 
      m_archiveFile = archiveFile;  
      m_archivePath = new File(m_archiveFile, 
                           (m_archiveDirName == null) ? "/" : m_archiveDirName);
   }   

   public String getArchiveDir() {  return m_archiveDirName;  }
   
   public String getArchivePath() { return m_archivePath.getAbsolutePath(); }

   public String getCHMExtractor() { return m_chmExtractor; }
   
   public String getCHMArgs() { return m_chmArgs; }
   
   public String getDJVUExtractor() { return m_djvuExtractor; }
   
   public String getDJVUArgs() { return m_djvuArgs; }
   
   protected void setArchiveDir(String archiveDir)
   //-----------------------------------------------
   {
      this.m_archiveDirName = archiveDir;
   }

   public int getPort() { return m_port;  }

   protected void setPort(int port)
   //------------------------------
   {
      this.m_port = port;
   }
   
   protected boolean portConfig(java.io.File propertiesFile, Properties properties)
   //----------------------------------------------------------------------
   {
      if (propertiesFile == null)
      {
         m_port = 8088;
         return true;
      }
      try
      {
         m_port = Integer.parseInt(properties.getProperty("port", "80"));
      }
      catch (Exception e)
      {
         System.err.println(propertiesFile + ": port= must be an integer");
         return false;
      }
      
      return true;
   }
   
   protected boolean dirConfig(java.io.File propertiesFile, Properties properties)
   //---------------------------------------------------------------------
   {
      String userHome = ((System.getProperty("user.home") == null) 
                           ? "." 
                           : System.getProperty("user.home")) + File.separator;
      if (propertiesFile == null)
      {  // Assumes content in classpath if no home specified
         m_homeDir = null;
         m_archiveFile = null; 
         return true;
      }   
      String home = properties.getProperty("home", null);
      m_archiveDirName = properties.getProperty("archivedir", null);
      String archiveFile = properties.getProperty("archivefile", null);
      if ( (home == null) && (archiveFile == null) && (m_archiveDirName == null) )
      {
         m_homeDir = new File(m_defaultHome);
         return true;
      }   
      if (home == null)
      {
         if (archiveFile != null)
         {
            m_archiveFile = new java.io.File(archiveFile);
            if (! m_archiveFile.exists())
            {
               System.err.println(propertiesFile.getName() + 
                                  ": Archive file " + m_archiveFile.getAbsolutePath() + 
                                  " not found.");
               return false;
            }   
            if (m_archiveDirName == null)
               m_archiveDirName = "/";
            m_archivePath = new File(m_archiveFile, m_archiveDirName);
         }
         else
            m_archiveFile = null;
      }
      else
      {            
         if ( (m_archiveDirName != null) || (archiveFile != null) )
         {
            System.err.println(propertiesFile.getName() + 
                               ": Either home= or archivedir= and/or " +
                               "archivefile= must be specified.");
            System.out.println(instructions);
            return false;
         }   
         m_homeDir = new java.io.File(home);
      }                        
      return true;
   }
   
   protected boolean indexConfig(java.io.File propertiesFile, Properties properties)
   //-----------------------------------------------------------------------
   {
      if (propertiesFile == null)
      {
         m_archiveSearchIndexDir = m_defaultIndex;
         return true;
      }   
      m_searchIndexDir = properties.getProperty("indexdir", null);
      m_archiveSearchIndexDir = properties.getProperty("archiveindexdir", null);
      if ( (m_searchIndexDir == null) && (m_archiveSearchIndexDir == null) )
      {
         if (m_homeDir != null)
         {
            if (m_homeDir.getPath().compareTo(m_defaultHome) == 0)
               m_searchIndexDir = m_defaultIndex;
            else
               m_searchIndexDir = System.getProperty("java.io.tmpdir") + 
                                  File.separatorChar + "DoxMentor4JIndex";
         }   
         else
            if (m_archiveFile != null)
               m_archiveSearchIndexDir = File.separator;
      }
      return true;
   }   
     
   protected boolean xtractConfig(java.io.File propertiesFile, Properties properties)
   //-----------------------------------------------------------------------
   {
      if (propertiesFile == null)
      {
         m_chmExtractor = m_chmArgs = m_djvuExtractor = m_djvuArgs = null;
         return true;
      }   
      m_chmExtractor = properties.getProperty("chmext", null); 
      m_chmArgs = properties.getProperty("chmargs", null); 
      m_djvuExtractor = properties.getProperty("chmext", null); 
      m_djvuArgs = properties.getProperty("chmargs", null); 
      return true;
   }
   
   
   protected boolean config(java.io.File propertiesFile)
   //------------------------------------------
   {      
      Properties properties = new Properties();
      try 
      {
         if (propertiesFile == null)
            propertiesFile = new java.io.File("doxmentor4j.properties");
         if (propertiesFile.exists())
            properties.load(new java.io.FileInputStream(propertiesFile));
         else
            propertiesFile = null;
      }
      catch (Exception e)
      {
         System.err.println("Error reading from properties file " + 
                            propertiesFile.getAbsolutePath() + ". " + 
                            e.getMessage());
         return false;
      }
      
      if (! portConfig(propertiesFile, properties))
         return false;
      if (! dirConfig(propertiesFile, properties))
         return false;
       String s = properties.getProperty("search", "y").trim();
       if (s.substring(0,1).compareToIgnoreCase("y") == 0)
          m_isSearchable = true;       
       
       m_logDir = new File(properties.getProperty("logdir", "log").trim());
       if (m_logDir.exists())
       {
          if (m_logDir.isDirectory())
             Utils.deleteDir(m_logDir);
          else
             m_logDir.delete();          
       }
       m_logDir.mkdirs();       
       
       if (m_isSearchable)
       {
          if (! indexConfig(propertiesFile, properties))
            return false;
          if (! xtractConfig(propertiesFile, properties))
            return false;
       }
       
       if (! m_isSearchable)
          m_isIndexable = false;
       else
       {
         s = properties.getProperty("index", "y").trim();
         if (s.substring(0,1).compareToIgnoreCase("y") == 0)
             m_isIndexable = true;
       }
      
      return true;
   }
   
   protected synchronized boolean start() 
             throws FileNotFoundException, IOException, NoSuchFieldException
   //------------------------------------------------------------------------
   {
      StringTemplateHandler stHandler = null;
      boolean isDebug = false;
      File indexDir = null;
      
      if (System.getProperty("_DEBUG_") != null)
         isDebug = true;
      
      Logger logger = Utils.createLogger(m_logDir, isDebug);      
      
      // Assume content in classpath if no home specified
      if ( (m_archiveFile == null) && (m_homeDir == null) )
      {
         // Assume content based at resources/htdocs in a jar (or zip) in the classpath
         if (m_archiveDirName == null)
            m_archiveDirName = m_defaultHome;
         if (isDebug)
            ArchiveHttpd.addToClassPath("dist/DoxMentor4J.jar");

         try
         {
            m_httpd = new ArchiveHttpd(m_archiveDirName, 10, 15);
         }
         catch (FileNotFoundException e)
         {  // Try / in classpath eg a zip in the classpath
            System.err.println("Could not find " + m_archiveDirName + 
                               " in classpath. Trying " + File.separator);
            m_archiveDirName = File.separator;
            m_httpd = new ArchiveHttpd(m_archiveDirName, 10, 15);
         }
         m_archiveFile = ((ArchiveHttpd)m_httpd).getArchiveFile();
         if (m_archiveFile == null)
         {
            System.err.println("Error opening archive dir");
            if (logger != null)
               logger.error("DoxMentor4J.start(): Error opening archive dir");
            return false;
         }   
         m_archivePath = new File(m_archiveFile, m_archiveDirName);
         stHandler = new ArchiveStringTemplateHandler(m_httpd, 
                                           "net.homeip.donaldm.doxmentor4j.templates",
                                           m_archiveFile, m_archiveDirName);
      }
      else
         if (m_archiveFile != null)
         {   
            if (m_archiveDirName == null)
               m_archiveDirName = File.separator;
            m_archivePath = new File(m_archiveFile, m_archiveDirName);
            m_httpd = new ArchiveHttpd(m_archiveFile, m_archiveDirName, 10, 15);
            stHandler = new ArchiveStringTemplateHandler(m_httpd, 
                                           "net.homeip.donaldm.doxmentor4j.templates",
                                           m_archiveFile, m_archiveDirName);
         }
         else
            if (m_homeDir != null)
            {
               m_httpd = new FileHttpd(m_homeDir, 10, 15);   
               stHandler = new FileStringTemplateHandler(m_httpd, 
                                              "net.homeip.donaldm.doxmentor4j.templates");
            }
      if (isDebug)
      {
         stHandler.setDebug(true);
         m_httpd.setCaching(false);
         m_httpd.setVerbose(true);
      }
      m_httpd.addHandler(".st", stHandler);      
      m_httpd.setLogger(logger);
      m_httpd.addDefaultFile("index.html");
      
      AjaxNodeHandler nodeHandler = new AjaxNodeHandler();
      m_httpd.addPostHandler("/node", nodeHandler);  
      
      AjaxSearchHandler searchHandler = new AjaxSearchHandler();
      m_httpd.addPostHandler("/search", searchHandler);  
         
      if (m_isIndexable)
      {
         AjaxIndexer indexHandler = AjaxIndexer.getIndexer();
         m_httpd.addPostHandler("/indexer", indexHandler);      
      }
      
      boolean b = m_httpd.start(m_port, "/");  
      if (b)
      {
         Logger l = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");
         //Logger l = Utils.getLogger();
         if (l != null)
            l.info("DoxMentor4J started: " + new Date().toString());         
      }
      synchronized (m_mainThread)
      {
         try { m_mainThread.wait(); } catch (Exception e) { e.printStackTrace(System.err);}
      }  
      return b;
   }

   static private void shutdown()
   //---------------------
   {
      DoxMentor4J app = DoxMentor4J.getApp();
      if ( (app.m_httpd != null) && (app.m_httpd.isStarted()) )
         app.m_httpd.stop(1);
      Utils.stopLogger();
   }
   
   public static void main(String[] args)
   //------------------------------------
   {
      java.io.File propertiesFile = null;
      DoxMentor4J app = DoxMentor4J.getApp();
            
      if (args.length > 0)
      {         
         if (args[0].trim().startsWith("-h"))
         {
            System.out.println(instructions);
            return;
         }
         propertiesFile = new java.io.File(args[0]);
         if (! propertiesFile.exists())
         {
            System.err.println("Property file " + 
                               propertiesFile.getAbsolutePath() +" not found");
            System.out.println(instructions);
            return;
         }
      }
      if (! app.config(propertiesFile))
      {
         System.out.println(instructions);
         return;
      }
      boolean ok = false;
      Runtime.getRuntime().addShutdownHook(new Thread() 
      {
         public void run() 
         {
            shutdown();
         }
      });
      m_mainThread = Thread.currentThread();
      MySignalHandler signalHandler = new MySignalHandler(app);
      Signal.handle(new Signal("INT"), signalHandler);
      Signal.handle(new Signal("TERM"), signalHandler);
      if (System.getProperty("os.name").toLowerCase().indexOf("windows") != 0)
         Signal.handle(new Signal("HUP"), signalHandler);
      try
      {
         ok = app.start();    
      }
      catch (Exception e)
      {
         System.err.println(e.getMessage());
         ok = false;
      }
      if (! ok)
         System.err.println("DoxMentor4J: Startup error");
   }   
   
   private boolean _copyIndex(String src, String dest)
   //-------------------------------------------------
   {
      src = src.trim();
      if (Utils.startsWithIgnoreCase(src, "jar://"))
         src = src.substring(6);
      File f = new File(m_archiveFile, src);
      if (! f.exists())
         return false;
      File destDir = new File(dest);
      return f.copyAllTo(destDir);
   }
   
   static final class MySignalHandler implements SignalHandler
   //=========================================================
   {    
      SignalHandler     m_oldHandler = null;
      DoxMentor4J       m_doxMentor = null;
      
      public MySignalHandler(DoxMentor4J doxMentor)
      //----------------------------------------------------
      {
         m_doxMentor = doxMentor;
      }
      
      public void handle(Signal sig)
      // ---------------------------
      {
         System.out.println("Signal " + sig.toString() + " called");
         System.out.println("Please wait - shutting down");         
         if (m_doxMentor.m_httpd.isStarted())
            m_doxMentor.m_httpd.stop(5);
         synchronized (m_mainThread)
         {
            m_mainThread.notifyAll();
         }
         

// Chain back to previous handler, if one exists
// if ( oldHandler != SIG_DFL && oldHandler != SIG_IGN )
// m_oldHandler.handle(sig);
//         catch (Exception e)
//         {
//            System.err.println("Signal handler failed, reason " + e);
//         }
      }      
   }   
}
