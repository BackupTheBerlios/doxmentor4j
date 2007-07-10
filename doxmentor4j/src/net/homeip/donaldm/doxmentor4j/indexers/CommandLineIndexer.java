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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.homeip.donaldm.doxmentor4j.Utils;
import de.schlichtherle.io.File;

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
   
   public CommandLineIndexer()
   //-------------------------
   {
      
   }
   
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
   
   protected void setIndexer(Indexable indexer)
   //------------------------------------------
   {
      m_indexor = indexer;
   }
   
   public Object getData(InputStream is, String href, String fullPath, 
                         StringBuffer title, StringBuffer body)
   //--------------------------------------------------------------------
   {
      if (m_indexor == null)
         m_indexor = IndexFactory.getApp().getIndexer("html");
      String ext = Utils.getExtension(fullPath);
      if (m_indexor.supportsFileType(ext))
         return m_indexor.getData(is, href, fullPath, title, body);
      else
         return "";
   }
   
   public long index(String href, String fullPath, boolean followLinks,
                     Object... extraParams) throws IOException
   //------------------------------------------------------------------
   {
      if ( (m_extractorPath == null) || (! new File(m_extractorPath).exists()) )
      {
         System.err.println("Extract program executable not set: " + 
                            this.getClass().getName());
         return -1;
      }         
      if (m_indexWriter == null)
      {
         System.err.println("CLIndexer: index writer is null: " + 
                            this.getClass().getName());
         return -1;
      }
      if (m_indexor == null)
         m_indexor = IndexFactory.getApp().getIndexer("html");
      File archiveFile = new File(fullPath);
      boolean isJar = (archiveFile.getTopLevelArchive() != null);
      java.io.File tmpFileIn = null,  
                   tmpFileOut = File.createTempFile("clout", "." + 
                                             m_indexor.supportedFileTypes()[0]);
      String args = "";
      if (isJar)
      {         
         tmpFileIn = File.createTempFile("clin", "." + supportedFileTypes()[0]);
         File f = new File(tmpFileIn);
         if (! archiveFile.copyTo(f))
         {
            System.err.println("Error copying archive file " + 
                               archiveFile.getAbsolutePath() + 
                               " to temp file " + f.getAbsolutePath());
            f.delete();
            return -1;
         }
         if (m_extractorArgs != null) 
            args = m_extractorArgs.replaceAll("\\$s", tmpFileIn.getAbsolutePath());
      }
      else            
         if (m_extractorArgs != null) 
            args = m_extractorArgs.replaceAll("\\$s", fullPath);
      if (args.indexOf("$d") >= 0)
         args = args.replaceAll("\\$d", tmpFileOut.getAbsolutePath());
      if (args.indexOf("$D") >= 0)
      {
         tmpFileOut.delete();
         tmpFileOut.mkdirs();
         args = args.replaceAll("\\$D", tmpFileOut.getAbsolutePath());
         if ( (! tmpFileOut.exists()) || (! tmpFileOut.isDirectory()) )
            return -1;
      }         
      
      String command = String.format("%s %s", m_extractorPath, args);
      StringBuffer output = new StringBuffer();
      StringBuffer error = new StringBuffer();
      int status = CommandLineIndexer.exec(command, output, error);
      if ( ((tmpFileOut.isDirectory()) && (tmpFileOut.list().length == 0)) ||
           (! tmpFileOut.exists()) )
      {
         System.err.println(command + " failed:");
         System.err.println("Status = " + status);
         System.err.println(output.toString());
         System.err.println(error.toString());         
         return -1;
         
      }
      
      if (! tmpFileOut.isDirectory())
         return m_indexor.index(href, tmpFileOut.getAbsolutePath(), followLinks, 
                                extraParams);
      else
      {
         File d = new File(tmpFileOut);
         return _index(href, d, extraParams);            
      }
   }
   
   static public int exec(String command, StringBuffer stdout,
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
         e.printStackTrace();         
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
   
   protected Object clone() throws CloneNotSupportedException
   //--------------------------------------------------------
   {
      CommandLineIndexer klone = (CommandLineIndexer) super.clone();
      klone.m_extractorPath = m_extractorPath;
      klone.m_indexor = m_indexor;
      klone.m_isfollowLinks = m_isfollowLinks;
      return klone;
   }
   
   private long _index(String href, File fullPath, Object... extraParams) 
           throws IOException
   //--------------------------------------------------------------------
   {
      File[] dir = (File[]) fullPath.listFiles();
      long count =0, c;
      for (int i=0; i<dir.length; i++)
      {
         File f = dir[i];
         c = 0;
         if (f.isDirectory())
            c = _index(href, f, extraParams);
         else
         {
            Indexable indexor = m_indexor;
            String path = f.getAbsolutePath();
            String ext = Utils.getExtension(path);
            if (! indexor.supportsFileType(ext))
               indexor = IndexFactory.getApp().getIndexer(ext);
            if (indexor != null)
               c = indexor.index(href, path, false, extraParams);            
         }
         count += c;
      }
      return count;
   }
}
