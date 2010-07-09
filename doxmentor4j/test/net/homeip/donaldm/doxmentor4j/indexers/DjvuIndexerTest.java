package net.homeip.donaldm.doxmentor4j.indexers;

import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import util.TestUtils;
import static org.junit.Assert.*;

public class DjvuIndexerTest
{
   public DjvuIndexerTest()
   {
   }

   @BeforeClass
   public static void setUpClass() throws Exception
   {
   }

   @AfterClass
   public static void tearDownClass() throws Exception
   {
   }

   @Before
   public void setUp()
   {
   }

   @After
   public void tearDown()
   {
   }

   @Test
   public void testCommandLineGetText() throws Exception
   {
      System.out.println("Command line getText");
      URI uri = genDjvu().toURI();
      StringBuilder title = null;
      DjvuIndexer instance = new DjvuIndexer();
      assertTrue(instance.getExtractor(true));
      Reader djvuReader = instance.getText(uri, -1, title);
      Reader txtReader = new FileReader(new File("test-data", "bottou-98.txt"));
      int ch, ch2;
      while ( (ch = djvuReader.read()) != -1)
      {
         ch2 = txtReader.read();
         assertEquals(ch2, ch);
      }      
//      StringBuilder sb = new StringBuilder();
//      while ( (ch = djvuReader.read()) != -1)
//         sb.append((char) ch);
//      djvuReader.close();
//      assertEquals("High Quality Document Image Compression with DjVu", sb.substring(0, 48));
   }

   @Test
   public void testGetText() throws Exception
   {
      System.out.println("getText");
      URI uri = genDjvu().toURI();
      StringBuilder title = null;
      DjvuIndexer instance = new DjvuIndexer();
      instance.setExtractorPath("garbage");
      instance.setExtractorArgs("trash");
      Reader djvuReader = instance.getText(uri, -1, title);
      Reader txtReader = new FileReader(new File("test-data", "bottou-98.txt"));
      int ch, ch2, c = 0;
      StringBuilder sb = new StringBuilder();
      while ( (ch = djvuReader.read()) != -1)
      {
         sb.append((char) ch);
         ch2 = txtReader.read();
         if (ch > 255)
            continue;
         if (ch2 > 255)
            continue;
         if (ch2 != ch)
         {
            System.out.print(c + ":");
            if (c > 100)
               c -= 100;
            else if (c > 50)
               c -= 50;
            else if (c >= 10)
               c -= 10;
            System.out.println(sb.toString().substring(c));
         }
         assertEquals(ch2, ch);
         c++;
      }
      djvuReader.close();
      txtReader.close();

      djvuReader = instance.getText(uri, 0, title);
      sb.setLength(0); sb.trimToSize();
      while ( (ch = djvuReader.read()) != -1)
         sb.append((char) ch);
      djvuReader.close();
      String s = sb.toString();
      assertEquals("High Quality Document Image Compression with DjVu", s.substring(0, 49));

   }

   @Test
   public void testCommandLineIndex() throws Exception
   {
      System.out.println("Command line index");
      File f = genDjvu();
      URI uri = f.toURI();
      DjvuIndexer instance = new DjvuIndexer();
      assertTrue(instance.getExtractor(true));
      String href = f.getPath();
      Directory directory = null;
      IndexWriter indexWriter = null;
      try
      {
         directory = new RAMDirectory();
         indexWriter = new IndexWriter(directory, new StandardAnalyzer(DoxMentor4J.LUCENE_VERSION),
                                       true, IndexWriter.MaxFieldLength.UNLIMITED);
         indexWriter.setInfoStream(System.out);
         instance.setIndexWriter(indexWriter);
         instance.getIndexer().setIndexWriter(indexWriter);
         long result = instance.index(href, uri, false);
         assertEquals(1L, result);
         if (indexWriter != null)
         {
            try { indexWriter.commit();} catch (Exception _e) {}
            try { indexWriter.close(); indexWriter = null; } catch (Exception _e) {}
         }
         Document[] docs = TestUtils.getHits(directory, "contents", 10, "abstract");
         assertEquals(1, docs.length);
      }
      finally
      {
         if (indexWriter != null)
            try { indexWriter.close(); } catch (Exception _e) {}
         if (directory != null)
            try { directory.close(); } catch (Exception _e) {}
         f.delete();
      }
   }

   @Test
   public void testIndex() throws Exception
   {
      System.out.println("index");
      File f = genDjvu();
      URI uri = f.toURI();
      DjvuIndexer instance = new DjvuIndexer();
      instance.setExtractorPath("garbage");
      instance.setExtractorArgs("trash");
      String href = f.getPath();
      Directory directory = null;
      IndexWriter indexWriter = null;
      try
      {
         directory = new RAMDirectory();
         indexWriter = new IndexWriter(directory, new StandardAnalyzer(DoxMentor4J.LUCENE_VERSION),
                                       true, IndexWriter.MaxFieldLength.UNLIMITED);
         indexWriter.setInfoStream(System.out);
         instance.setIndexWriter(indexWriter);
         instance.getIndexer().setIndexWriter(indexWriter);
         long result = instance.index(href, uri, false);
         assertEquals(1L, result);
         if (indexWriter != null)
         {
            try { indexWriter.commit();} catch (Exception _e) {}
            try { indexWriter.close(); indexWriter = null; } catch (Exception _e) {}
         }

         Document[] docs = TestUtils.getHits(directory, "contents", 10, "abstract");
         assertEquals(1, docs.length);
         assertEquals("1", docs[0].getField("page").stringValue());

         docs = TestUtils.getHits(directory, "contents", 10, "basic", "ideas");
         assertEquals(1, docs.length);
         assertEquals("4", docs[0].getField("page").stringValue());
      }
      finally
      {
         if (indexWriter != null)
            try { indexWriter.close(); } catch (Exception _e) {}
         if (directory != null)
            try { directory.close(); } catch (Exception _e) {}
         f.delete();
      }
   }

   private File genDjvu() throws IOException
   //--------------------
   {
      File f = new File("test-data", "bottou-98.djvu");
      assertTrue(f.exists());
      File djvuFile = File.createTempFile("tmp", ".djvu");
      djvuFile.delete();
      de.schlichtherle.io.File.cp(f, djvuFile);
      assertTrue(djvuFile.exists());
      return djvuFile;
   }
}
