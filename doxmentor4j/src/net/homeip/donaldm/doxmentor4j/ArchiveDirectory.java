package net.homeip.donaldm.doxmentor4j;

import de.schlichtherle.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.SimpleFSLockFactory;

/**
 * Implements a Lucene Directory within an archive. As files in a archive cannot 
 * have random access, this implementation 'cheats' by using a proxy and copying 
 * the archive index to a temporary directory and working with the temporary 
 * directory as normal using the proxy FSDirectory. When close is called the 
 * index directory is copied back to the archive. Obviously any index updates 
 * will be lost in the event of a crash.
 * @author Donald Munro
 */
public class ArchiveDirectory extends Directory
//---------------------------------------------
{
   protected File                      m_archiveDir = null;
   protected java.io.File              mTempDirectory = null;
   protected boolean                   m_updated = false;
   protected boolean                   m_isReadOnly = false;
   
   private FSDirectory                 mLuceneDirectory = null;
   private SimpleFSLockFactory         m_lockFactory = null;
   
   private ArchiveDirectory(java.io.File archiveFile, boolean isCreate) throws IOException
   //-------------------------------------------------------------------
   {
      m_archiveDir = new File(archiveFile);
      _copyToTemp(archiveFile, isCreate);
      m_lockFactory = new SimpleFSLockFactory(mTempDirectory);
      mLuceneDirectory =  FSDirectory.open(mTempDirectory, m_lockFactory);
   }
   
   private ArchiveDirectory(java.io.File archiveFile, String archiveDir, boolean isCreate)
          throws IOException
   //------------------------------------------------------------------
   {
      m_archiveDir = new File(archiveFile, archiveDir);
      _copyToTemp(archiveFile, isCreate);
      m_lockFactory = new SimpleFSLockFactory(mTempDirectory);
      mLuceneDirectory =  FSDirectory.open(mTempDirectory, m_lockFactory);
   }
   
   public static Directory getDirectory(java.io.File archiveFile, String archiveDir, boolean isCreate)
                 throws IOException                              
   //------------------------------------------------------------------------------------------------
   {
      if ( (archiveFile == null) || (! archiveFile.exists()) || 
           (! archiveFile.canRead()) || (! archiveFile.canWrite()) )
         throw new IOException("Cannot open " + ((archiveFile == null) ? "null" 
                                                : archiveFile.getAbsolutePath())
                               + "for reading and writing");
      if (archiveDir == null)
         return new ArchiveDirectory(archiveFile, isCreate);
      else
         return new ArchiveDirectory(archiveFile, archiveDir, isCreate);
      
      
   }   
   
   public java.io.File getTempDirectory() { return mTempDirectory; }
   
   public void setReadOnly(boolean b) { m_isReadOnly = b; }
   
   public boolean getReadOnly() { return m_isReadOnly; }
      
   @Override
   public boolean fileExists(String fileName)
   //-----------------------------------------
   {
      return mLuceneDirectory.fileExists(fileName);
   }
   
   @Override
   public long fileModified(String fileName)
   //---------------------------------------
   {
      return mLuceneDirectory.fileModified(fileName);
   }
   
   @Override
   public void touchFile(String fileName)
   //------------------------------------
   {
      if (m_isReadOnly) 
         return;
      mLuceneDirectory.touchFile(fileName);
   }
   
   @Override
   public void deleteFile(String fileName) throws IOException
   //--------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      mLuceneDirectory.deleteFile(fileName);
   }
      
   @Override
   public long fileLength(String fileName)
   //-------------------------------------
   {
      return mLuceneDirectory.fileLength(fileName);
   }
   
   @Override
   public IndexOutput createOutput(String fileName) throws IOException
   //------------------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      return mLuceneDirectory.createOutput(fileName);
   }
   
   @Override
   public IndexInput openInput(String fileName) throws IOException
   //--------------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      return mLuceneDirectory.openInput(fileName);
   }

   @Override
   public IndexInput openInput(String arg0, int arg1) throws IOException
   //-------------------------------------------------------------------
   {
       return mLuceneDirectory.openInput(arg0, arg1);
   }

   @Override
   public String getLockID()
   //-----------------------
   {
       return mLuceneDirectory.getLockID();
   }

   @Override
   public String[] listAll() throws IOException
   //------------------------------------------
   {
      return mLuceneDirectory.listAll();
   }



   @Override
   public String toString()
   {
      return "ArchiveDirectory{" + "archiveDir=" + m_archiveDir +
             ", tempDirectory=" + mTempDirectory + ", updated=" + m_updated +
             ", isReadOnly=" + m_isReadOnly + '}';
   }
   
   @Override
   public void close()
   //-----------------
   {
      mLuceneDirectory.close();
      if (m_updated)
      {
         try  
         {
            _copyFromTemp();
         }
         catch (Exception e)
         {
            e.printStackTrace(System.err);
         }
      }
   }
   
   @Override
   public Lock makeLock(String name)
   //-------------------------------
   {
      return m_lockFactory.makeLock(name);
   }
   
   
   @Override
   public void clearLock(String name) throws IOException
   //---------------------------------------------------
   {
      if (m_lockFactory != null)
         m_lockFactory.clearLock(name);
   }

   public static java.io.File tempDir(java.io.File archiveFile, boolean isCreate)
   //----------------------------------------------------------------------------
   {
      MessageDigest md5 = null;
      String filename;
      try
      {  md5 = MessageDigest.getInstance("MD5");
         md5.update(archiveFile.getName().getBytes());
         filename = Utils.bytesToBinHex(md5.digest());
      }
      catch (Exception e)
      {
         filename = archiveFile.getName() + ".tmp";
      }
         
      java.io.File tmp = new java.io.File(System.getProperty("java.io.tmpdir", "."));
      java.io.File tmpDir = new java.io.File(tmp, filename);
      if (tmpDir.isFile())
         tmpDir.delete();
      if ( (tmpDir.isDirectory()) && (isCreate) )
         Utils.deleteDir(tmpDir);
      tmpDir.mkdirs();
      return tmpDir;
   }

   private void _copyToTemp(java.io.File archiveFile, boolean isCreate) throws IOException
   //--------------------------------------------------------------------------------------
   {
      java.io.File tmpDir = tempDir(archiveFile, isCreate);
      mTempDirectory = tmpDir;
      if ( (! mTempDirectory.exists()) || (! mTempDirectory.isDirectory()) )
         throw new IOException("Error creating temporary directory " + 
                               mTempDirectory.getAbsolutePath());
      if ( (isCreate) && (! m_archiveDir.archiveCopyAllTo(mTempDirectory)) )
         throw new IOException("Error copying content from archive directory " +
                               m_archiveDir.getAbsolutePath() + 
                               " to temporary directory " + 
                               mTempDirectory.getAbsolutePath());
              
      
   }
   
   private void _copyFromTemp() throws IOException
   //--------------------------
   {
      if (! m_archiveDir.archiveCopyAllFrom(mTempDirectory))
         throw new IOException("Error copying content from temp directory " +
                               mTempDirectory.getAbsolutePath() +
                               " to archive directory " + 
                               m_archiveDir.getAbsolutePath());
                               
                               
   }
}
