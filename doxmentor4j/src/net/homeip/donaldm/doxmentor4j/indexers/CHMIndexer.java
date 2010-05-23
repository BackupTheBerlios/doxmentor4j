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

import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import java.io.File;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import net.homeip.donaldm.doxmentor4j.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CHMIndexer extends CommandLineIndexer implements Indexable, Cloneable
//========================================================================
{   
   final static private Logger logger = LoggerFactory.getLogger(CHMIndexer.class);
   
   public CHMIndexer()
   //-----------------
   {
      EXTENSIONS = new String[] { "chm" };                        
   }
   
   public CHMIndexer(String extractorPath, String extractorArgs)
   //-----------------------------------------------------------
   {
      super(extractorPath, extractorArgs);
      EXTENSIONS = new String[] { "chm" };
      setIndexer(IndexFactory.getApp().getIndexer("html"));      
   }

   @Override public Logger logger() {return logger; }

   @Override
   protected boolean getExtractor()
   //-----------------------------
   {
      File chmext = null;
      String s = DoxMentor4J.getApp().getCHMExtractor();
      if (s != null)
      {
         chmext = new File(s);
         if (! chmext.exists())
         {
            logger.error("CHMIndexer: Extractor " + s + " does not exist");
            chmext = null;
         }
      }
      s = DoxMentor4J.getApp().getCHMArgs();
      if ( (s != null) && (chmext != null) )
      {
         setExtractorPath(chmext.getAbsolutePath());
         setExtractorArgs(s);
         setIndexer(IndexFactory.getApp().getIndexer("html"));
         return true;
      }
      if ( (s != null) && (chmext == null) )
         logger.warn("CHMIndexer: Extractor args " + s +
                     " but no extractor specified. Attempting to find default extractors");

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
         chmext = Utils.findFile("hh.exe", drive+"windows", drive+"windows\\system",
                           drive+"windows\\system32",
                           drive+"Program Files\\HTML Help Workshop");
         if (chmext != null)
         {
            setExtractorPath(chmext.getAbsolutePath());
            setExtractorArgs("-decompile $D $s ");
            setIndexer(IndexFactory.getApp().getIndexer("html"));
            return true;
         }
         else
         {
            s = "CHMIndexer: Could not find hh.exe for extracting " +
                       ".chm files. Searched: " +  drive+"windows;" + drive+
                        "windows\\system;" + drive+"windows\\system32;" +
                        drive+"Program Files\\HTML Help Workshop";
            logger.error(s);
            return false;
         }

      }
      else if (! os.startsWith("OS/2"))
         {
            chmext = Utils.findFile("chmdump", "/bin", "/usr/bin", "/usr/local/bin",
                              "/usr/sbin", "/usr/local/sbin", "/opt/bin");
            if (chmext == null)
            {
               chmext = Utils.findFile("chmextract", "/bin", "/usr/bin",
                                       "/usr/local/bin", "/usr/sbin",
                                       "/usr/local/sbin", "/opt/bin");
               if (chmext == null)
                  chmext = Utils.findFile("extract_chmLib", "/bin", "/usr/bin",
                                          "/usr/local/bin", "/usr/sbin",
                                          "/usr/local/sbin", "/opt/bin");
            }
            if (chmext != null)
            {
               setExtractorPath(chmext.getAbsolutePath());
               setExtractorArgs("$s $D");
               setIndexer(IndexFactory.getApp().getIndexer("html"));
               return true;
            }
            else
            {
               s = "CHMIndexer: Could not find a chm extractor. Searched for"
                        + " chmdump, chmextract and extract_chmLib in /bin:" +
                          "/usr/bin:/usr/local/bin:/usr/sbin:/usr/local/sbin:/opt/bin";
               logger.error(s);
               return false;
            }
         }
      return false;
   }
}
