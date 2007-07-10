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

import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.index.IndexWriter;

public interface Indexable
//========================
{
   /**
    * 
    * @return A String array listing all the file types (extensions) supported
    * by the Indexable implementation.
    */
   public String[] supportedFileTypes();   
   
   public boolean supportsFileType(String ext);
   
   public void setIndexWriter(IndexWriter indexWriter);
   
   public Object getData(InputStream is, String href, String fullPath,
                         StringBuffer title, StringBuffer body);
   
   /**
    * Indexes the contents of is to index directory indexDir
    * @param href The path to store as the location of the file/resource
    * @param fullPath Path to the file to be indexed.
    * @param followLinks If true attempt to index linked files that have a 
    * relative path
    * @param extraParams Extra implementation defined parameters.
    * @return The number of successfully indexed files else -1.
    * @throws java.io.IOException 
    */
   public long index(String href, String fullPath, boolean followLinks,
                        Object ...extraParams) 
                  throws IOException;   
}
