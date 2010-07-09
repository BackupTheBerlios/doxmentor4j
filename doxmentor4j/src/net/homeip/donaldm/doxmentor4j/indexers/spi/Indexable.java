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

package net.homeip.donaldm.doxmentor4j.indexers.spi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
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

   public void addFileType(String ext);

   public void setIndexWriter(IndexWriter indexWriter);

   /**
    * Returns the text data for an indexable resource.
    * @param uri The @see java.net.URI URI of the resource
    * @param page If the resource supports pages then this specifies the page no
    * to get text for. If the resource does not support pages then all data is
    * returned. If page is -1 then all data is returned.
    * @param title A previously allocated StringBuilder into which the title
    * of the resource is copied if the resource supports titles. Can be null.
    * @return A Reader for the text of the resource. The caller should close
    * the Reader when done.
    */
   public Reader getText(URI uri, int page, StringBuilder title)
          throws FileNotFoundException, MalformedURLException, IOException;
   
   /**
    * Indexes the contents of uri 
    * @param uri The @see java.net.URI URI of the resource
    * @param followLinks If true attempt to index linked files that have a 
    * relative path
    * @param extraParams Extra implementation defined parameters.
    * @return The number of successfully indexed files else -1.
    * @throws java.io.IOException 
    */
   public long index(String href, URI uri, boolean followLinks, Object ...extraParams)
               throws IOException;   
}
