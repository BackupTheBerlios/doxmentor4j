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

import java.io.File;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import net.homeip.donaldm.doxmentor4j.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DjvuIndexer extends CommandLineIndexer implements Indexable, Cloneable
//=================================================================================
{   
   private Logger logger = LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");
   
   public DjvuIndexer()
   //--------------------
   {
      EXTENSIONS = new String[] { "djvu", "dejavu" };
      
      File djvuext = null;
      String s = DoxMentor4J.getApp().getDJVUExtractor();
      if (s != null)
      {   
         djvuext = new File(s);
         if (! djvuext.exists())
         {
            if (logger != null)
               logger.error("DjvuIndexer: Extractor " + s + " does not exist");
            else
               System.err.println("DjvuIndexer: Extractor " + s + " does not exist");
            djvuext = null;            
         }
      }
      s = DoxMentor4J.getApp().getDJVUArgs();
      if ( (s != null) && (djvuext != null) )
      {   
         setExtractorPath(djvuext.getAbsolutePath());
         setExtractorArgs(s);
         setIndexer(IndexFactory.getApp().getIndexer("txt"));
         return;
      }
      if ( (s != null) && (djvuext == null) )
      {
         if (logger != null)
            logger.error("DjvuIndexer: Extractor args " + s + 
                         " but no extractor specified");
         else
            System.err.println("DjvuIndexer: Extractor args " + s + 
                               " but no extractor specified");
      }
         
      // Try to get working extractor per OS
      String os = System.getProperty("os.name").toLowerCase();
      
      if (os.startsWith("windows"))
      {
         String drive = System.getProperty("user.home");
         if (drive.length() > 3) 
            drive = drive.substring(0, 3);
         else
            if (drive.length() < 3) 
               drive = drive + "\\";
         djvuext = Utils.findFile("djvutxt.exe", drive+"windows", 
                                    drive+"windows\\system", 
                                    drive+"windows\\system32");
         if (djvuext != null)
         {
            setExtractorPath(djvuext.getAbsolutePath());
            setExtractorArgs("$s $d");
            setIndexer(IndexFactory.getApp().getIndexer("txt"));
         }
         else
         {
            s = "DjvuIndexer: Could not find djvutxt.exe for extracting " 
                     + ".djvu files. Searched: " +  drive+"windows;" + drive+
                        "windows\\system;" + drive+"windows\\system32";
            if (logger != null)
               logger.error(s);
            else
               System.err.println(s);
         }
      }
      else
         if (! os.startsWith("OS/2"))
         {
            djvuext = Utils.findFile("djvutxt", "/bin", "/usr/bin", "/usr/local/bin",
                              "/usr/sbin", "/usr/local/sbin", "/opt/bin");
            if (djvuext != null)
            {
               setExtractorPath(djvuext.getAbsolutePath());
               setExtractorArgs("$s $d");
               setIndexer(IndexFactory.getApp().getIndexer("txt"));
            }
            else
            {
               s = "DjvuIndexer: Could not find a djvu extractor." +
                          " Searched for djvutxt in /bin:/usr/bin:" +
                          "/usr/local/bin:/usr/sbin:/usr/local/sbin:/opt/bin";
               if (logger != null)
                  logger.error(s);
               else
                  System.err.println(s);
            }
         }      
   }
   
   public DjvuIndexer(String extractorPath, String extractorArgs)
   //-----------------------------------------------------------
   {
      super(extractorPath, extractorArgs);
      EXTENSIONS = new String[] { "djvu", "dejavu" };
      setIndexer(IndexFactory.getApp().getIndexer("txt"));      
   }
   
   
   
}
