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


public class PyIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
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

   public String[] getLanguageStopWords()
   //------------------------------------
   {
      return PY_STOP_WORDS;
   }
   
}
