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


import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.homeip.donaldm.doxmentor4j.Utils;
import de.schlichtherle.io.File;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import net.homeip.donaldm.httpdbase4j.Httpd;

/**
 * A base class for indexers using command line programs to extract 
 */ 
abstract public class CommandLineIndexer extends Indexer 
         implements Indexable, Cloneable
//=====================================================================
{
   protected String  m_extractorPath = null;
   
   /**
    * Use $s as placeholder for source file and $d as placeholder for
    * destination file or $D as placeholder for destination directory. Default
    * assumed to be "$s $d".
    */
   protected String  m_extractorArgs = "$s $d";
      
   protected Indexable m_indexor = null;
   
   protected boolean m_isfollowLinks = false;

   abstract protected boolean getExtractor(boolean isFindDefault);

   public CommandLineIndexer() { }
   
   public CommandLineIndexer(String extractorPath, Indexable indexor)
   //-------------------------------------------------------
   {
      m_extractorPath = extractorPath;
      m_indexor = indexor;
   }
   
   public CommandLineIndexer(String extractorPath, String extractorArgs)
   //-----------------------------------------------------------
   {
      m_extractorPath = extractorPath;
      m_extractorArgs = extractorArgs;
   }
   
   public CommandLineIndexer(String extractorPath, String extractorArgs, 
                             Indexable indexor)
   //-------------------------------------------------------
   {
      m_extractorPath = extractorPath;
      m_extractorArgs = extractorArgs;
      m_indexor = indexor;
   }

   public CommandLineIndexer(String extractorPath, String extractorArgs, 
                    Indexable indexor, boolean isfollowLinks)
   //-------------------------------------------------------
   {
      m_extractorPath = extractorPath;
      m_indexor = indexor;
      m_extractorArgs = extractorArgs;
      m_isfollowLinks = isfollowLinks;
   }
   
   protected void setExtractorPath(String extractorPath) 
   //----------------------------------------------------
   { 
      m_extractorPath = extractorPath;
   }
   
   protected void setExtractorArgs(String extractorArgs)
   //------------------------------------------
   {
      m_extractorArgs = extractorArgs;
   }
   
   protected void setIndexer(Indexable indexer) { m_indexor = indexer; }

   protected Indexable getIndexer() { return m_indexor; }

   @Override
   public Reader getText(URI uri, int page, StringBuilder title)
          throws FileNotFoundException, MalformedURLException, IOException
   //-----------------------------------------------------------------------
   {
      if (m_indexor == null)
         m_indexor = IndexFactory.getApp().getIndexer("txt");
      if (m_extractorPath == null)
         getExtractor(true); // Overridden getExtractor should also set m_indexor
      if ( (m_extractorPath == null) || (! new File(m_extractorPath).exists()) )
      {
         logger().error("Extract program executable {} not set or could not be found", m_extractorPath);
         return null;
      }

      String scheme = uri.getScheme();
      if ( (scheme != null) && (! scheme.equalsIgnoreCase("file")) )
      {
         logger().error("Cannot extract index text from a remote file");
         return null;
      }
      java.io.File tmpFileOut = extract(uri);
      if (! tmpFileOut.isDirectory())
         return getText(tmpFileOut, title);
      else
      {
         java.io.File[] files = tmpFileOut.listFiles();
         java.io.File tmpAllFiles = mergeFiles(files, title, null);
         Utils.deleteDir(tmpFileOut);
         return new FileReader(tmpAllFiles);
      }
   }

   @Override
   public long index(String href, URI uri, boolean followLinks, Object... extraParams) throws IOException
   //---------------------------------------------------------------------------------------
   {
      if (m_indexor == null)
         m_indexor = IndexFactory.getApp().getIndexer("txt");
      if (m_extractorPath == null)
         getExtractor(true);
      if ( (m_extractorPath == null) || (! new File(m_extractorPath).exists()) )
      {
         logger().error("Extract program executable not set or could not be found");
         return -1;
      }
      String scheme = uri.getScheme();
      if ( (scheme != null) && (! scheme.equalsIgnoreCase("file")) )
      {
         logger().error("Cannot index text from a remote file");
         return -1;
      }
      if (m_indexWriter == null)
      {
         logger().error("Index writer is null");
         return -1;
      }
      java.io.File tmpFileOut = extract(uri);
           
      if (! tmpFileOut.isDirectory())
         return m_indexor.index(href, tmpFileOut.toURI(), followLinks, extraParams);
      else
      {
         java.io.File[] files = tmpFileOut.listFiles();
         java.io.File tmpAllFiles = mergeFiles(files, null, null);
         Utils.deleteDir(tmpFileOut);
         return m_indexor.index(href, tmpAllFiles.toURI(), false, extraParams);
      }
   }

   protected Reader getText(java.io.File f, StringBuilder title)
             throws FileNotFoundException, MalformedURLException, IOException
   //--------------------------------------------------
   {
      if (! m_indexor.supportsFileType(Utils.getExtension(f.getName())))
         return null;
      return m_indexor.getText(f.toURI(), -1, title);
   }

   private java.io.File mergeFiles(java.io.File[] files, StringBuilder title, BufferedWriter writer)
   //------------------------------------------------------------------------------------------------
   {
      BufferedReader reader = null;
      java.io.File tmpFile = null;
      try
      {
         if (writer == null)
         {
            tmpFile = File.createTempFile("clac", ".tmp");
            tmpFile.delete();
            writer = new BufferedWriter(new FileWriter(tmpFile));
         }
         for (java.io.File file : files)
         {
            if (file.isDirectory())
            {
               mergeFiles(file.listFiles(), title, writer);
               continue;
            }
            if (! file.isFile()) continue;
            try { reader = new BufferedReader(getText(file, title)); } catch (Exception _e) { reader = null; }
            if (reader != null)
            {
               int ch;
               while ( (ch = reader.read()) != -1)
                  writer.write(ch);
               try { reader.close(); reader = null; } catch (Exception _e) {}
            }
         }
         try { writer.close(); writer = null; } catch (Exception _e) {}
         tmpFile.deleteOnExit();
         return tmpFile;
      }
      catch (Exception e)
      {
         logger().error("", e);
         return null;
      }
      finally
      {
         if (reader != null)
            try { reader.close(); } catch (Exception _e) {}
         if (writer != null)
            try { writer.close(); } catch (Exception _e) {}
      }
   }

   protected java.io.File extract(URI uri)
   //---------------------------------------------
   {
      String ext = null;
      if (m_indexor != null)
         ext = m_indexor.supportedFileTypes()[0];
      if ( (ext == null) || (ext.trim().isEmpty()) )
         ext = ".tmp";
      else
         ext = "." + ext;
      File archiveFile = Utils.uri2Archive(uri);
      if (archiveFile == null)
         archiveFile = new File(uri.getPath());
      boolean isJar = (archiveFile.getTopLevelArchive() != null);
      java.io.File tmpFileIn = null,  tmpFileOut = null;
      try { tmpFileOut = File.createTempFile("clout", ext); } catch (Exception _e) { tmpFileOut = new java.io.File(archiveFile.getName() + ext); }
      String args = "";
      if (isJar)
      {
         try { tmpFileIn = File.createTempFile("clin", ".tmp"); } catch (Exception _e) { tmpFileOut = new java.io.File(archiveFile.getName() + ".in"); }
         File f = new File(tmpFileIn);
         if (! archiveFile.copyTo(f))
         {
            System.err.println("Error copying archive file " +
                               archiveFile.getAbsolutePath() +
                               " to temp file " + f.getAbsolutePath());
            f.delete();
            return null;
         }
         if (m_extractorArgs != null)
            args = m_extractorArgs.replaceAll("\\$s", tmpFileIn.getAbsolutePath());
      }
      else
         if (m_extractorArgs != null)
            args = m_extractorArgs.replaceAll("\\$s", uri.getPath());
      if (args.indexOf("$d") >= 0)
         args = args.replaceAll("\\$d", tmpFileOut.getAbsolutePath());
      if (args.indexOf("$D") >= 0)
      {
         tmpFileOut.delete();
         tmpFileOut.mkdirs();
         args = args.replaceAll("\\$D", tmpFileOut.getAbsolutePath());
         if ( (! tmpFileOut.exists()) || (! tmpFileOut.isDirectory()) )
         {
            if (tmpFileIn != null)
               tmpFileIn.delete();
            return null;
         }
      }

      String command = String.format("%s %s", m_extractorPath, args);
      StringBuffer output = new StringBuffer();
      StringBuffer error = new StringBuffer();
      int status = exec(command, output, error);
      if ( ((tmpFileOut.isDirectory()) && (tmpFileOut.list().length == 0)) ||
           (! tmpFileOut.exists()) )
      {
         logger().error(command + " failed: Status = " + status + " output: " +
                        output.toString() + Httpd.EOL + error.toString());
         if (tmpFileIn != null)
            tmpFileIn.delete();
         return null;
      }
      return tmpFileOut;
   }

   public int exec(String command, StringBuffer stdout,
           StringBuffer stderr)           
   // -----------------------------------------------------------------
   {
      int            status = -1;
      BufferedReader input  = null, error  = null;
      Process        p = null;
      
      try
      {
         Runtime r = Runtime.getRuntime();
         
         if (r != null)
         {
            p = r.exec(command);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            String line;            
            if (stdout != null) while ((line = input.readLine()) != null)
               stdout.append(line);
            error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            if (stderr != null) while ((line = error.readLine()) != null)
               stderr.append(line);
            p.waitFor();
            status = p.exitValue();
         }
      }
      catch (Exception e)
      {
         logger().error(command, e);
         return -1;
      }
      finally
      {
         if (input != null)
         {
            try { input.close(); } catch (Exception e) {}            
            input = null;
         }
         
         if (error != null)
         {
            try { error.close(); } catch (Exception e) {}            
            error = null;
         }
         
         if (p != null)
         {
            try { p.destroy(); } catch (Exception e) {}
         }
      }
      
      return status;
   }
   
   @Override
   public Object clone() throws CloneNotSupportedException
   //--------------------------------------------------------
   {
      CommandLineIndexer klone = (CommandLineIndexer) super.clone();
      return klone;
   }
}
