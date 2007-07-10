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


public class RubyIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   private static final String[] RUBY_STOP_WORDS = 
   {      
      "alias", "and", "BEGIN", "begin", "break", "case", "class", "def", 
      "defined", "do", "else", "elsif", "END", "end", "ensure", "false", "for",
      "if", "in", "module", "next", "nil", "not", "or", "redo", "rescue", 
      "retry", "return", "self", "super", "then", "true", "undef", "unless",
      "until", "when", "while", "yield"              
   };
   
   public RubyIndexer()
   //---------------
   {
      // Allows .h files to be indexed with CppIndexer
      EXTENSIONS = new String[] { "rb", "ruby" }; 
   }

   public String[] getLanguageStopWords()
   //------------------------------------
   {
      return RUBY_STOP_WORDS;
   }
   
}
