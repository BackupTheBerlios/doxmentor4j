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

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Utils
{
   public static boolean deleteDir(File dir)
   //---------------------------------------
   {  // a symbolic link has a different canonical path than its actual path,
      // unless it's a link to itself
      File candir;
      try {	candir = dir.getCanonicalFile(); } catch (IOException e) { return false; }        
      if (!candir.equals(dir.getAbsoluteFile())) 
         return false;

      File[] files = candir.listFiles();
      if (files != null) 
      {	
         for (int i = 0; i < files.length; i++) 
         {  File file = files[i];
            boolean deleted = file.delete();
            if (! deleted) 
            if (file.isDirectory()) 
            deleteDir(file);
         }
      }
   return dir.delete();  
   }
   
   public static String wildcardToRegex(String wildcard)
   //---------------------------------------------------
   {
      StringBuffer s = new StringBuffer(wildcard.length());
      s.append('^');
      for (int i = 0, is = wildcard.length(); i < is; i++)
      {
         char c = wildcard.charAt(i);
         switch(c)
         {
         case '*':
            s.append(".*");
            break;
         case '?':
            s.append(".");
            break;
            // escape special regexp-characters
         case '(': case ')': case '[': case ']': case '$':
         case '^': case '.': case '{': case '}': case '|':
         case '\\':
            s.append("\\");
            s.append(c);
            break;
         default:
            s.append(c);
            break;
         }
      }
      s.append('$');
      return(s.toString());
   }
   
   static private Pattern m_extPattern = Pattern.compile(".+\\.(.+)$");
   
   public static String getExtension(String path)
   //--------------------------------------------
   {
      int p = path.lastIndexOf(File.separatorChar);
      if (p < 0)
         p = path.lastIndexOf('/');
      if ( (p++ >= 0) && (p < path.length()) )
         path = path.substring(p);
      p = path.indexOf("?");  
      if (p < 0)
         p = path.indexOf("&");  
      if (p > 0)
         path = path.substring(0, p);
      Matcher matcher = m_extPattern.matcher(path);
      String ext = "";
      if (matcher.matches())
         ext = matcher.group(1);
      return ext;
   }
   
   static public File findFile(String name, String ...path)
   //----------------------------------------------------
   {
      for (String dirName : path)
      {
         File dir = new File(dirName);
         if ( (! dir.exists()) || (! dir.isDirectory()) ) continue;
         File f = new File(dir, name);
         if (f.exists()) return f;
      }
      return null;
   }

   public static int indexOfIgnoreCase(String string, String substring, 
                                       int fromIndex)
   //------------------------------------------------------------------
   {
      for(int i = fromIndex; i < string.length(); i++)
      {
         if (startsWithIgnoreCase(string, substring, i))
            return i;
      }
      return -1;
   }

   public static int indexOfIgnoreCase(String string, String substring)
   //------------------------------------------------------------------
   {
      return indexOfIgnoreCase(string, substring, 0);
   }
   
   public static boolean startsWithIgnoreCase(String string, String substring, 
                                              int fromIndex)
   //-------------------------------------------------------------------------
   {
      if ((fromIndex < 0) || ((fromIndex + substring.length()) > string.length()))
         return false;
      for(int i = 0; i < substring.length(); i++)
         if (Character.toUpperCase(string.charAt(fromIndex + i)) != Character.toUpperCase(substring.charAt(i)))
            return false;
      return true;
   }
   
   public static boolean startsWithIgnoreCase(String string, String substring)
   //-------------------------------------------------------------------------
   {
      return startsWithIgnoreCase(string, substring, 0);
   }

   private static File _getLogDir(File logDir)
   //-----------------------------------------
   {
      String logFileName = "DoxMentor4J.log";
      File logFile = null;
      if (logDir == null)
         logDir = new File(".");
      if (! logDir.exists())
         logDir.mkdirs();
      if (logDir.exists())
         logFile = new File(logDir, logFileName);
      else
      {
         logDir = new File(".");
         logFile = new File(logFileName);
      }
      boolean isCreate = false;
      if (! logFile.exists())
      {
         try { logFile.createNewFile(); } catch (Exception e) {}
         isCreate = true;
      }
      if (! logFile.canWrite())
      {
         File tmpDir = new File(System.getProperty("java.io.tmpdir"));
         logDir = new File(tmpDir, "DoxMentor4J-Log");
         logFile = new File(logDir, logFileName);
         System.out.println("Logging to " + logDir.getAbsolutePath());
      }          
      if (isCreate)
         logFile.delete();
      return logDir;
   }
}
