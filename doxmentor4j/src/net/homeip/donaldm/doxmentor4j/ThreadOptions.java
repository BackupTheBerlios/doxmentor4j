package net.homeip.donaldm.doxmentor4j;

import java.util.concurrent.ThreadFactory;

public class ThreadOptions implements ThreadFactory
//=======================================================
{
   String name;
   int priority = Thread.NORM_PRIORITY;
   boolean isDaemon = true;

   public ThreadOptions(String name) { this.name = name; }

   public ThreadOptions(String name, int priority)
   //---------------------------------------------------
   { this.name = name;
     this.priority = priority;
   }

   public ThreadOptions(String name, int priority, boolean isDaemon)
   //---------------------------------------------------
   { this.name = name;
     this.priority = priority;
     this.isDaemon = isDaemon;
   }

   @Override
   public Thread newThread(Runnable r)
   //---------------------------------
   {
     Thread t = new Thread(r);
     t.setDaemon(isDaemon);
     t.setName(name);
     t.setPriority(priority);
     return t;
   }
}
