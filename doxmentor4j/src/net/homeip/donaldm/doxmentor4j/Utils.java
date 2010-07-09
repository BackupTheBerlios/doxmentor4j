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
import java.net.URI;
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
   
   static private Pattern EXTENSION_PATTERN = Pattern.compile(".+\\.(.+)$");
   
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
      Matcher matcher = EXTENSION_PATTERN.matcher(path);
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

   public static String uriName(URI uri)
   //-----------------------------------
   {
      String path = uri.getPath();
      if (path == null)
         return null;
      int slash = path.lastIndexOf('/');
      if (slash >= 0)
         return path.substring(slash+1);
      else
         return path;
   }

   /**
    * Converts a uri to a zip archive to a TrueZip File. Always returns the innermost
    * zip file ie if there are zip files inside zip files the innermost one is returned
    * @param uri The uri to convert
    * @return A truezip de.schlichtherle.io.File of the innermost zip archive or null if url
    * does not reference a zip archive.
    */
   public static de.schlichtherle.io.File uri2Archive(URI uri)
   //---------------------------------------------------------
   {
      de.schlichtherle.io.File archive = null, f = null;
      String s = uri.getPath();
      int p = s.toLowerCase().indexOf(".zip");
      while (p >= 0)
      {
         p = p + 4;
         if (archive == null)
            archive = new de.schlichtherle.io.File(s.substring(0, p));
         else
            archive = new de.schlichtherle.io.File(archive, s.substring(0, p));
         if (s.length() >= (p+2))
         {
            s = s.substring(p+1);
            f = new de.schlichtherle.io.File(archive, s);
         }
         else
            s = "";
         p = s.toLowerCase().indexOf(".zip");
      }
      return f;
   }

   static public java.io.File uri2File(URI uri)
   //---------------------------------------------
   {
      java.io.File tmpFile = null;
      String ext;
      try { ext = "." + getExtension(uriName(uri)); } catch (Exception _e) { ext = ".tmp"; }
      try { tmpFile = java.io.File.createTempFile("tmp", ext); } catch (IOException e) { tmpFile = new File(uriName(uri) + ext); }
      tmpFile.delete();
      String scheme = uri.getScheme();
      if ( (scheme == null) || (scheme.equalsIgnoreCase("file")) )
      {
         de.schlichtherle.io.File f = uri2Archive(uri);
         if (f != null)
         {
            f.copyTo(tmpFile);
            tmpFile.deleteOnExit();
            return tmpFile;
         }
         else
         {
            f = new de.schlichtherle.io.File(uri.getPath());
            return f;
         }
//         f.isArchive(); f.isDirectory();
//         if (f.isEntry())
//         {
//
//            f.copyTo(tmpPdf);
//            return tmpPdf;
//         }
//         else
//            return f;
      }
      else
         return null;
   }

   static public String getRelativePath(String basePath, String fullPath)
   //-------------------------------------------------------------------
   {
      int p = fullPath.indexOf(basePath);
      if (p >= 0)
         fullPath = fullPath.substring(p+basePath.length());
      if ( (fullPath.startsWith("/")) || (fullPath.startsWith(File.separator)) )
      {
         if (fullPath.length() > 1)
            fullPath = fullPath.substring(1);
      }
      return fullPath;
   }

   public static File canonizeFile(final java.io.File f)
   //---------------------------------------------------
   {
      File ff = null;
      try
      {
         ff = f.getCanonicalFile();
      }
      catch (Exception e)
      {
         ff = f.getAbsoluteFile();
      }
      return ff;
   }

   final static char[] HEXTAB = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                  'A', 'B', 'C', 'D', 'E', 'F' };

   public static String bytesToBinHex(byte[] data) { return bytesToBinHex(data, 0, data.length); }

   public static String bytesToBinHex(byte[] data, int offset, int len)
   //------------------------------------------------------------------
   {
      StringBuilder sbuf = new StringBuilder();
      sbuf.setLength(len << 1);

      int nPos = 0;

      int nC = offset + len;

      while (offset < nC)
      {
         sbuf.setCharAt(nPos++, HEXTAB[(data[offset] >> 4) & 0x0f]);
         sbuf.setCharAt(nPos++, HEXTAB[data[offset++] & 0x0f]);
      }
      return sbuf.toString();
   }

}
