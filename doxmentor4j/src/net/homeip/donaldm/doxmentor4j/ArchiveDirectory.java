package net.homeip.donaldm.doxmentor4j;

import de.schlichtherle.io.File;
import java.io.IOException;
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
public class ArchiveDirectory extends FSDirectory
//---------------------------------------------
{
   protected File                      m_archiveDir = null;
   protected File                      m_tempDirectory = null;
   protected boolean                   m_updated = false;
   protected boolean                   m_isReadOnly = false;
   
   private FSDirectory                 m_directory = null;
   private SimpleFSLockFactory         m_lockFactory = null;
   
   private ArchiveDirectory(java.io.File archiveFile) throws IOException
   //-------------------------------------------------------------------
   {
      m_archiveDir = new File(archiveFile);
      _copyToTemp();
      m_lockFactory = new SimpleFSLockFactory(m_tempDirectory);
      m_directory =  FSDirectory.getDirectory(m_tempDirectory, m_lockFactory);
   }
   
   private ArchiveDirectory(java.io.File archiveFile, String archiveDir)
          throws IOException
   //------------------------------------------------------------------
   {
      m_archiveDir = new File(archiveFile, archiveDir);
      _copyToTemp();
      m_lockFactory = new SimpleFSLockFactory(m_tempDirectory);
      m_directory =  FSDirectory.getDirectory(m_tempDirectory, m_lockFactory);
   }
   
   public static Directory getDirectory(java.io.File archiveFile,
                                               String archiveDir)
                 throws IOException                              
   //-------------------------------------------------------------------
   {
      if ( (archiveFile == null) || (! archiveFile.exists()) || 
           (! archiveFile.canRead()) || (! archiveFile.canWrite()) )
         throw new IOException("Cannot open " + ((archiveFile == null) ? "null" 
                                                : archiveFile.getAbsolutePath())
                               + "for reading and writing");
      if (archiveDir == null)
         return new ArchiveDirectory(archiveFile);
      else
         return new ArchiveDirectory(archiveFile, archiveDir);      
      
      
   }   
   
   public File getTempDirectory() { return m_tempDirectory; }
   
   public void setReadOnly(boolean b) { m_isReadOnly = b; }
   
   public boolean getReadOnly() { return m_isReadOnly; }
   
   @Override
   public String[] list() 
   //--------------------
   {      
      return m_directory.list();
   }
   
   @Override
   public boolean fileExists(String fileName)
   //-----------------------------------------
   {
      return m_directory.fileExists(fileName);
   }
   
   @Override
   public long fileModified(String fileName)
   //---------------------------------------
   {
      return m_directory.fileModified(fileName);
   }
   
   @Override
   public void touchFile(String fileName)
   //------------------------------------
   {
      if (m_isReadOnly) 
         return;
      m_directory.touchFile(fileName);
   }
   
   @Override
   public void deleteFile(String fileName) throws IOException
   //--------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      m_directory.deleteFile(fileName);
   }
   
   @SuppressWarnings("deprecation")
   @Override
   public synchronized void renameFile(String from, String to) throws IOException
   //----------------------------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      m_directory.renameFile(from, to);
   }
   
   @Override
   public long fileLength(String fileName)
   //-------------------------------------
   {
      return m_directory.fileLength(fileName);
   }
   
   @Override
   public IndexOutput createOutput(String fileName) throws IOException
   //------------------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      return m_directory.createOutput(fileName);
   }
   
   @Override
   public IndexInput openInput(String fileName) throws IOException
   //--------------------------------------------------------------
   {
      if (m_isReadOnly) 
         throw new IOException("Read only Directory");
      m_updated = true;
      return m_directory.openInput(fileName);
   }

   @Override
   public IndexInput openInput(String arg0, int arg1) throws IOException
   //-------------------------------------------------------------------
   {
       return m_directory.openInput(arg0, arg1);
   }

   @Override
   public String getLockID()
   //-----------------------
   {
       return m_directory.getLockID();
   }

   @Override
   public java.io.File getFile()
   //---------------------------
   {
       return m_directory.getFile();
   }

   @Override
   public String toString()
   //----------------------
   {
      StringBuffer sb = new StringBuffer();
      sb.append(m_directory.toString());
      sb.append(System.getProperty("line.separator"));
      sb.append("Archive Directory: ");
      sb.append(m_archiveDir.getAbsolutePath());
      sb.append(System.getProperty("line.separator"));
      sb.append("Temporary Directory: ");
      sb.append(m_tempDirectory.getAbsolutePath());
      sb.append(System.getProperty("line.separator"));
      sb.append("Updated: ");
      sb.append(m_updated);
      return sb.toString();
   }
   
   @Override
   public void close()
   //-----------------
   {
      m_directory.close();
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

   
   private void _copyToTemp() throws IOException
   //-------------------------------------------
   {
      File tmp = new File(File.createTempFile("TMPDIR", ".tmp"));
      tmp.delete();
      tmp.deleteAll();
      tmp.mkdirs();
      m_tempDirectory = tmp;
      if ( (! m_tempDirectory.exists()) || (! m_tempDirectory.isDirectory()) )
         throw new IOException("Error creating temporary directory " + 
                               m_tempDirectory.getAbsolutePath());
      if (! m_archiveDir.archiveCopyAllTo(m_tempDirectory))
         throw new IOException("Error copying content from archive directory " +
                               m_archiveDir.getAbsolutePath() + 
                               " to temporary directory " + 
                               m_tempDirectory.getAbsolutePath());
              
      
   }
   
   private void _copyFromTemp() throws IOException
   //--------------------------
   {
      if (! m_archiveDir.archiveCopyAllFrom(m_tempDirectory))
         throw new IOException("Error copying content from temp directory " +
                               m_tempDirectory.getAbsolutePath() + 
                               " to archive directory " + 
                               m_archiveDir.getAbsolutePath());
                               
                               
   }
}
