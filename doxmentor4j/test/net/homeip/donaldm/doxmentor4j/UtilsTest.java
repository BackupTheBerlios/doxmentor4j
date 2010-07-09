/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.homeip.donaldm.doxmentor4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author root
 */
public class UtilsTest {

    public UtilsTest() {
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
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

   /**
    * Test of deleteDir method, of class Utils.
    */
   @Test
   public void testDeleteDir() throws IOException
   {
      System.out.println("deleteDir");
      File dir = File.createTempFile("dir", ".tmp");
      dir.delete();
      dir.mkdirs();
      assertTrue(dir.isDirectory());
      boolean expResult = true;
      boolean result = Utils.deleteDir(dir);
      assertEquals(expResult, result);
      assertFalse(dir.exists());
   }

   /**
    * Test of wildcardToRegex method, of class Utils.
    */
   @Test
   public void testWildcardToRegex()
   {
      System.out.println("wildcardToRegex");
      String wildcard = "*.*";
      String expResult = "^.*\\..*$";
      String result = Utils.wildcardToRegex(wildcard);
      System.out.println(result);
      assertEquals(expResult, result);
   }

   /**
    * Test of getExtension method, of class Utils.
    */
   @Test
   public void testGetExtension()
   {
      System.out.println("getExtension");
      String path = "/tmp/test.1.txt";
      String expResult = "txt";
      String result = Utils.getExtension(path);
      assertEquals(expResult, result);
   }

   /**
    * Test of findFile method, of class Utils.
    */
   @Test
   public void testFindFile() throws IOException
   {
      System.out.println("findFile");
      File f = File.createTempFile("test", ".tmp");
      f.createNewFile();
      String name = f.getName();
      String[] path = { System.getProperty("user.home"), System.getProperty("user.dir"),
                        System.getProperty("java.io.tmpdir") };
      File expResult = f;
      File result = Utils.findFile(name, path);
      assertEquals(expResult, result);
      assertEquals(f, result);
   }

   
   /**
    * Test of indexOfIgnoreCase method, of class Utils.
    */
   @Test
   public void testIndexOfIgnoreCase()
   {
      System.out.println("indexOfIgnoreCase");
      String string = "123Test456";
      String substring = "test";
      int expResult = 3;
      int result = Utils.indexOfIgnoreCase(string, substring);
      assertEquals(expResult, result);
      result = Utils.indexOfIgnoreCase(string, substring, 2);
      assertEquals(expResult, result);
      expResult = -1;
      result = Utils.indexOfIgnoreCase(string, substring, 4);
      assertEquals(expResult, result);
   }

   /**
    * Test of startsWithIgnoreCase method, of class Utils.
    */
   @Test
   public void testStartsWithIgnoreCase()
   {
      System.out.println("startsWithIgnoreCase");
      String string = "Test123";
      String substring = "test";
      boolean expResult = true;
      boolean result = Utils.startsWithIgnoreCase(string, substring);
      assertEquals(expResult, result);
   }


   /**
    * Test of getRelativePath method, of class Utils.
    */
   @Test
   public void testGetRelativePath()
   {
      System.out.println("getRelativePath");
      String basePath = "/usr/home";
      String fullPath = "/usr/home/test/1";
      String expResult = "test/1";
      String result = Utils.getRelativePath(basePath, fullPath);
      assertEquals(expResult, result);
   }

}