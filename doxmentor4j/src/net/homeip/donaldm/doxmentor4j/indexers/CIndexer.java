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


public class CIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(CIndexer.class);
   
   @Override public Logger logger() {return logger; }

   private static final String[] C_STOP_WORDS =
   {
      "auto", "const", "double", "float", "int", "short", "struct", "unsigned",
      "break", "continue", "else", "for", "long", "signed", "switch", "void",
      "case", "default", "enum", "goto", "register", "sizeof",  "typedef",
      "volatile", "char", "do", "extern", "if", "return", "static", "union",
      "while"
              
   };
   
   public CIndexer()
   //---------------
   {
      // Allows .h files to be indexed with CppIndexer
      EXTENSIONS = new String[] { "c" }; 
   }

   @Override
   public String[] getLanguageStopWords()
   //------------------------------------
   {
      return C_STOP_WORDS;
   }
   
}
