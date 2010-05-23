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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxtIndexer extends Indexer implements Indexable, Cloneable
//=====================================================================
{   
   final static private Logger logger = LoggerFactory.getLogger(TxtIndexer.class);
   
   @Override public Logger logger() {return logger; }

   public TxtIndexer()
   //-----------------
   {
      EXTENSIONS = new String[] { "txt", "asc" };
   }   
   
}
