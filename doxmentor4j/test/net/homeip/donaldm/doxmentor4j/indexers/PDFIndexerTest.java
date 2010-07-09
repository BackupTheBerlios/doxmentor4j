package net.homeip.donaldm.doxmentor4j.indexers;

import java.io.IOException;
import java.io.File;
import java.io.Reader;
import java.net.URI;
import net.homeip.donaldm.doxmentor4j.DoxMentor4J;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import util.TestUtils;
import static org.junit.Assert.*;

public class PDFIndexerTest
{

   public PDFIndexerTest()
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

   
   /**
    * Test of getText method, of class PDFIndexer.
    */
   @Test
   public void testGetText() throws Exception
   {
      System.out.println("getText");
      File pdfFile = genPDF();
      URI uri = pdfFile.toURI();
      StringBuilder title = null;
      PDFIndexer instance = new PDFIndexer();

      Reader reader = instance.getText(uri, -1, title);
      pdfFile.delete();
      StringBuilder sb = new StringBuilder();
      int ch;
      while ( (ch = reader.read()) != -1)
         sb.append((char) ch);
      reader.close();
      System.out.println(sb.toString());
      for (int i=0; i<sb.length(); i++)
      {
         //Character Ch = Character.valueOf(sb.charAt(i));
         //System.out.print(Ch.toString());
         System.out.println((int) sb.charAt(i));
      }
      System.out.println();
      assertEquals("Page 1 \nPage 2\n", sb.toString());

      pdfFile = genPDF();
      uri = pdfFile.toURI();
      reader = instance.getText(uri, 1, title);
      sb.setLength(0); sb.trimToSize();
      while ( (ch = reader.read()) != -1)
         sb.append((char) ch);
      reader.close();
      assertEquals("Page 1 \n", sb.toString());
   }

   /**
    * Test of index method, of class PDFIndexer.
    */
   @Test
   public void testIndex() throws Exception
   {
      System.out.println("index");
      File pdfFile = genPDF();
      String href = pdfFile.getPath();
      URI uri = pdfFile.toURI();
      Directory directory = null;
      IndexWriter indexWriter = null;
      try
      {
         directory = new RAMDirectory();
//         File dir = new File("/tmp/dir");
//         Http.deleteDir(dir);
//         directory = FSDirectory.open(dir);
         indexWriter = new IndexWriter(directory, new StandardAnalyzer(DoxMentor4J.LUCENE_VERSION),
                                       true, IndexWriter.MaxFieldLength.UNLIMITED);
         indexWriter.setInfoStream(System.out);
         PDFIndexer instance = new PDFIndexer();
         instance.setIndexWriter(indexWriter);
         long expResult = 1L;
         long result = instance.index(href, uri, false);
         assertEquals(expResult, result);
         if (indexWriter != null)
         {
            try { indexWriter.commit();} catch (Exception _e) {}
            try { indexWriter.close(); indexWriter = null; } catch (Exception _e) {}
         }
         Document[] docs = TestUtils.getHits(directory, "contents", 10, "page");
         assertEquals(2, docs.length);
         docs = TestUtils.getHits(directory, "contents", 10, "page", "1");
         assertEquals(1, docs.length);
         assertEquals("1", docs[0].getField("page").stringValue());

         docs = TestUtils.getHits(directory, "contents", 10, "page", "2");
         assertEquals(1, docs.length);
         assertEquals("2", docs[0].getField("page").stringValue());
      }
      finally
      {
         if (indexWriter != null)
            try { indexWriter.close(); } catch (Exception _e) {}
         if (directory != null)
            try { directory.close(); } catch (Exception _e) {}         
         pdfFile.delete();
      }
   }

   protected File genPDF() throws IOException, COSVisitorException
   //--------------------------------------------------------------
   {
      PDDocument document = new PDDocument();
      PDDocumentInformation info = new PDDocumentInformation();
      info.setTitle("Title");
      document.setDocumentInformation(info);
		PDPage page = new PDPage();
		document.addPage( page );
      PDFont font = PDType1Font.HELVETICA_BOLD;
      PDPageContentStream contentStream = new PDPageContentStream(document, page);
      contentStream.beginText();
		contentStream.setFont( font, 12 );
		contentStream.moveTextPositionByAmount( 100, 700 );
		contentStream.drawString( "Page 1 " );
		contentStream.endText();
      contentStream.close();

      page = new PDPage();
		document.addPage( page );
      contentStream = new PDPageContentStream(document, page);
      contentStream.beginText();
		contentStream.setFont( font, 12 );
		contentStream.moveTextPositionByAmount( 100, 700 );
		contentStream.drawString( "Page 2" );
		contentStream.endText();
      contentStream.close();

      File pdfFile = File.createTempFile("tmp", ".pdf");
      pdfFile.delete();
      document.save(pdfFile.getAbsolutePath());
		document.close();
      return pdfFile;
   }
}
