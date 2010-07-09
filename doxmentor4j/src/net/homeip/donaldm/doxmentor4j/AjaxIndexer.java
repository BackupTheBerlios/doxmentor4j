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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.homeip.donaldm.httpdbase4j.CloneableHeaders;
import net.homeip.donaldm.httpdbase4j.Http;
import net.homeip.donaldm.httpdbase4j.HttpResponse;
import net.homeip.donaldm.httpdbase4j.Postable;
import net.homeip.donaldm.httpdbase4j.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class AjaxIndexer implements Postable
//==========================================
{    
   final static private Logger logger = LoggerFactory.getLogger(AjaxIndexer.class);

   static final public Pattern URL_PATTERN = Pattern.compile("\\w+://.+");

   /**
    * Executor for the entire indexing process. Only one allowed.
    */ 
   private ExecutorService                m_executor        = null;
   
   private IndexThread                    m_thread          = new IndexThread();
   private Future<Boolean>                m_task            = null;
            
   static private AtomicLong              m_count           = new AtomicLong(0);
      
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
      m_executor = Executors.newSingleThreadExecutor(new ThreadOptions("AjaxIndexer"));
   }

   public boolean index()
   //--------------------
   {
      stop(0);
      m_task = null;
      try
      {
         m_task = m_executor.submit(m_thread);
      }
      catch (Exception e)
      {
         logger.error(e.getMessage(), e);
         return false;
      }
      return true;
   }
           
   protected boolean stop(int timeout)
   //---------------------------------
   {
      m_thread.setStop(true);
      Boolean b = true;
      if ( (m_task != null) && (! m_task.isDone()) )      
      {
         try
         {
            b = m_task.get(timeout, TimeUnit.MILLISECONDS);
         }
         catch (Exception e)
         {
            b = false;
            m_task.cancel(true);
         }
      }
      return b;      
   }
      
   public static void incrementCount()
   //--------------------------------------
   {
      if (m_count != null)
         m_count.incrementAndGet();
   }       
   
   public static void addCount(long n)
   //---------------------------------
   {
      if (m_count != null)
         m_count.addAndGet(n);
   }

   private int stopCount = 0;

   @Override
   public Object onHandlePost(long id, HttpExchange ex, Request request,
                              HttpResponse r, java.io.File dir, 
                              Object... extraParameters)
   //-------------------------------------------------------------------
   {
      String html;
      CloneableHeaders postHeaders = request.getPOSTParameters();
      if (postHeaders.containsKey("start"))
      {
         if (m_thread.isBusy())
            html = "Indexing already in progress";
         else
         {
            stopCount = 0;
            if (! index())
               html = "<b> Indexing failed </b>";
            else
               html = "<b> Indexing Started </b>";
         }   
      }
      else if (postHeaders.containsKey("stop"))
      {
         if (m_thread.isBusy())
         {
            m_thread.setStop(true);
            html = "<b>Stopping index threads.</b>";
         }
         else
            html = "";
      }
      else
      {   
         if (m_thread.isBusy())
         {
            if (m_thread.isStop())
            {
               html = "<b>Stopping index threads.</b>";
               if (stopCount++ > 10) // Ajax updater calls POST handler every 2 seconds so this is 20 seconds
                  m_task.cancel(true);
            }
            else
               html = "<b>" + m_count + "</b> files indexed";
         }
         else
         {
            boolean b = false;
            try { b = m_task.get(); } catch (Exception e) {}
            if (b)
               html = "Complete. <b>" + m_count + "</b> files indexed";
            else
               html = "Errors occurred. <b>" + m_count + "</b> files indexed";
            if ( (error != null) && (! error.isEmpty()) )
               html += " <font color=\"red\">" + error + "</font>";
         }
      }
      r.setBody(html);
      r.setMimeType(Http.MIME_HTML);
      r.addHeader("Cache-Control", "no-cache");
      r.addHeader("Pragma", "no-cache");
      r.addHeader("Expires", "Sat, 26 Jul 1997 05:00:00 GMT");
      r.addHeader("Content-Length", Integer.toString(html.length()));
      return r;
   }

   static private String error = null;

   public static void setError(String message) { error = message; }
}
