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

import java.util.HashMap;
import net.homeip.donaldm.doxmentor4j.indexers.spi.Indexable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RubyIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(RubyIndexer.class);
   
   @Override public Logger logger() {return logger; }

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
      m_extensions = new HashMap<String, Void>()
      {{
          put("rb", null );
          put("ruby", null);
      }};
   }

   @Override public String[] getLanguageStopWords()
   //------------------------------------
   {
      return RUBY_STOP_WORDS;
   }
   
}
