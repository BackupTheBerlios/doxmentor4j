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


public class PhpIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(PhpIndexer.class);
   
   @Override public Logger logger() {return logger; }

   private static final String[] PHP_STOP_WORDS =
   {
      "and", "E_PARSE", "old_function", "$argv", "E_ERROR", "or", "as", 
      "E_WARNING", "parent", "$argc", "eval", "PHP_OS", "break", "exit",
      "$PHP_SELF", "case", "extends", "PHP_VERSION", "cfunction", "FALSE", 
      "print", "class", "for", "require", "continue", "foreach",  "require_once",
      "declare", "function", "return", "default", "static", "do", "switch",
      "die", "stdClass", "echo", "$this", "else", "TRUE", "elseif", "var",
      "empty", "if", "xor", "enddeclare", "include", "virtual", "endfor",
      "include_once", "while", "endforeach", "global",  "__FILE__", "endif",
      "list", "__LINE__", "endswitch", "new", "__sleep", "endwhile", "not",
      "__wakeup",  "E_ALL", "NULL"
   };
   
   public PhpIndexer()
   //---------------
   {
      // Allows .h files to be indexed with CppIndexer
      EXTENSIONS = new String[] { "php", "php4", "php5" }; 
   }

   @Override public String[] getLanguageStopWords()
   //------------------------------------
   {
      return PHP_STOP_WORDS;
   }
   
}
