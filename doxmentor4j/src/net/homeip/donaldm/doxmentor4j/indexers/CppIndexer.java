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


public class CppIndexer extends SourceIndexer implements Indexable, Cloneable
//============================================================================
{
   private static final String[] CPP_STOP_WORDS = 
   {
      "auto", "const", "double", "float", "int", "short", "struct", "unsigned",
      "break", "continue", "else", "for", "long", "signed", "switch", "void",
      "case", "default", "enum", "goto", "register", "sizeof",  "typedef",
      "volatile", "char", "do", "extern", "if", "return", "static", "union",
      "while", "asm", "dynamic_cast", "namespace", "reinterpret_cast", "try",
      "bool", "explicit", "new", "static_cast", "typeid", "catch", "false",
      "operator", "template", "typename", "class", "friend", "private", "this",
      "using", "const_cast", "inline", "public", "throw", "virtual", "delete",
      "mutable", "protected", "true", "wchar_t"     
   };
   
   public CppIndexer()
   //---------------
   {
      EXTENSIONS = new String[] { "c++", "cpp", "cxx", "cc", "h", "hpp", "h++" }; 
   }

   public String[] getLanguageStopWords()
   //------------------------------------
   {
      return CPP_STOP_WORDS;
   }
   
}
