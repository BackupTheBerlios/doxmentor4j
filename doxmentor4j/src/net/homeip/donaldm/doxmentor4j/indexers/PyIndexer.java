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


public class PyIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(PyIndexer.class);
   
   @Override public Logger logger() {return logger; }

   private static final String[] PY_STOP_WORDS =
   {
      "and", "del", "from", "not", "while", "elif", "global", "or", "with",     
      "assert", "else", "if", "pass", "yield", "break", "except", "import",
      "print", "class", "exec", "in", "raise", "continue", "finally", "is", 
      "return", "def", "for", "lambda", "try", "None", "as", "with"
   };
   
   public PyIndexer()
   //---------------
   {
      // Allows .h files to be indexed with CppIndexer
      EXTENSIONS = new String[] { "py", "python" }; 
   }

   @Override public String[] getLanguageStopWords()
   //------------------------------------
   {
      return PY_STOP_WORDS;
   }
   
}
