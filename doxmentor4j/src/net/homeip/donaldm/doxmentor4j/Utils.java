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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   
   public static Logger _createXMLLogger(File logDir, boolean isDebug)
   //-----------------------------------------------------------------
   {
      if (logDir != null)
         System.setProperty("log.dir", logDir.getAbsolutePath() + 
                                       File.separatorChar);
      Logger logger = null;
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      try 
      {
         JoranConfigurator configurator = new JoranConfigurator();
         configurator.setContext(lc);
         lc.shutdownAndReset();
         if (isDebug)
            configurator.doConfigure("logback-debug.xml");
         else
            configurator.doConfigure("logback.xml");
         logger =  LoggerFactory.getLogger("net.homeip.donaldm.doxmentor4j");         
      } 
      catch (JoranException e) 
      {
         System.err.println("Error opening Logging XML configuration file " + 
                             ((isDebug) ? "logback-debug.xml" : "logback.xml") +
                             ". (" + e.getMessage() + ")" +
                             ". Using internal log configuration.");
         return null;
      }
      return logger;
   }
   
   public static Logger createLogger(File logDir, boolean isDebug)
   //----------------------------------------------------------------------
   {
      logDir = _getLogDir(logDir);      
      ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) 
                                             _createXMLLogger(logDir, isDebug);
      if (logger != null)
         return logger;
      
      LoggerContext context =  (ch.qos.logback.classic.LoggerContext) 
                                    org.slf4j.LoggerFactory.getILoggerFactory();
      PatternLayout layout = null;
      String logFileName = "DoxMentor4J.log";
      File logFile = null;
      if ( (logDir != null) && (! logDir.exists()) ) 
         logDir.mkdirs();
      if ( (logDir != null) && logDir.exists() )
         logFile = new File(logDir, logFileName);
      else
         logFile = new File(logFileName);
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
      context.shutdownAndReset();
      layout = new PatternLayout();
      layout.setPattern("%d{dd MMM yyyy HH:mm:ss} %-5logger{40} %-8level " +
              "[%thread]: Ver: " + DoxMentor4J.getVersion() + " %n%message%n");
      layout.setContext(context);         
      if (isDebug)
      {
         ConsoleAppender<LoggingEvent> c = new ConsoleAppender<LoggingEvent>();
         c.setContext(context);
         c.setLayout(layout);
         c.setName("console");
         c.setTarget("System.err");            

         RollingFileAppender<LoggingEvent> a = new RollingFileAppender<LoggingEvent>();
         a.setContext(context);
         if (logDir.exists()) 
            a.setFile(logDir.getAbsolutePath() + File.separator + 
                                 logFileName);
         else
            a.setFile(logFileName);
         a.setAppend(true);
         a.setName("fileappender");            
         a.setLayout(layout);
         a.setAppend(true);
         TimeBasedRollingPolicy p = new TimeBasedRollingPolicy();            
         p.setContext(context);
         p.setParent(a);                    
         if (logDir.exists())       
            p.setFileNamePattern(logDir.getAbsolutePath() + File.separator + 
                                 logFileName +  ".%d{yyyy-MM-dd}.zip");
         else
            p.setFileNamePattern(logFileName + ".%d{yyyy-MM-dd}.zip");            

         a.setRollingPolicy(p);
         p.start();
         layout.start();
         c.start();
         a.start();
         logger = context.getLogger(ch.qos.logback.classic.LoggerContext.ROOT_NAME);
         logger.addAppender(a);
         logger.addAppender(c);
      }
      else
      {
         RollingFileAppender<LoggingEvent> a = new RollingFileAppender<LoggingEvent>();
         a.setContext(context);
         if (logDir.exists()) 
            a.setFile(logDir.getAbsolutePath() + File.separator + 
                                 logFileName);
         else
            a.setFile(logFileName);
         a.setAppend(true);
         a.setName("fileappender");            
         a.setLayout(layout);
         a.setAppend(true);
         TimeBasedRollingPolicy p = new TimeBasedRollingPolicy();            
         p.setContext(context);
         p.setParent(a);                    
         if (logDir.exists())       
            p.setFileNamePattern(logDir.getAbsolutePath() + File.separator + 
                                 logFileName +  ".%d{yyyy-MM-dd}.zip");
         else
            p.setFileNamePattern(logFileName + ".%d{yyyy-MM-dd}.zip");            

         a.setRollingPolicy(p);
         p.start();
         layout.start();
         a.start();
         logger = context.getLogger(ch.qos.logback.classic.LoggerContext.ROOT_NAME);
         logger.addAppender(a);
      }

      return logger;         
   }
   
   static public void stopLogger()
   //-----------------------------
   {
      LoggerContext context =  (ch.qos.logback.classic.LoggerContext) 
                                    org.slf4j.LoggerFactory.getILoggerFactory();
      if (context != null)
         context.stop();
   }
}
