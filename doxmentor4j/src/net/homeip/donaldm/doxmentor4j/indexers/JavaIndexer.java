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

public class JavaIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   final static private Logger logger = LoggerFactory.getLogger(JavaIndexer.class);
   
   @Override public Logger logger() {return logger; }

   private static final String[] JAVA_STOP_WORDS =
   {
    "public","private","protected","interface",
    "abstract","implements","extends","null", "new",
    "switch","case", "default" ,"synchronized" ,
    "do", "if", "else", "break","continue","this",
    "assert" ,"for","instanceof", "transient",
    "final", "static" ,"void","catch","try",
    "throws","throw","class", "finally","return",
    "const" , "native", "super","while", "import",
    "package" ,"true", "false" 
   };
   
   public JavaIndexer() 
   //-----------------
   {
      // gloss over groovy specific keywords
      m_extensions = new HashMap<String, Void>()
      {{
          put("java", null );
          put("groovy", null);
      }};
   }   
   
   @Override
   public String[] getLanguageStopWords()
   {
      return JAVA_STOP_WORDS;
   }
}
