/* Copyright (C) 2022-2024 Free Software Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses>.  */

package org.gprofng.mpmt;

import org.gprofng.mpmt.ipc.IPCLogger;
import java.io.File;
import java.io.IOException;

/** AnFile extends File, overwrites method exists() */
public class AnFile extends File {
  public static final char separatorChar = '/';
  boolean attributesReady = false;
  boolean isDirectoryFlag = true;
  boolean existsFlag = true;
  private String directory = null;
  private String fullpath = null;
  private static final String SLASH = "/";
  private static final String DOT = ".";

  /**
   * Default constructor: expects full path
   *
   * @param pathname
   */
  public AnFile(String pathname) {
    super(pathname);
    if (Analyzer.getInstance().remoteConnection == null) {
      File f = new File(pathname);
      if (f.exists()) {
        if (!f.isDirectory()) {
          isDirectoryFlag = false;
        }
      } else {
        existsFlag = false;
        isDirectoryFlag = false;
      }
      fullpath = f.getAbsolutePath();
      directory = f.getParent();
      attributesReady = true;
      return;
    }
    pathname = AnFileSystemView.slashifyPath(pathname);
    if (pathname.equals(SLASH)) {
      fullpath = SLASH;
      directory = SLASH; // NM TEMPORARY
      return; // Special case: leave directory = null
    }
    if (pathname.startsWith(SLASH)) {
      fullpath = pathname;
      int next = pathname.lastIndexOf(SLASH);
      if (next > 0) {
        directory = pathname.substring(0, next + 1); // include last /
      } else {
        directory = SLASH;
      }
    } else {
      if (Analyzer.getInstance().remoteConnection != null) {
        AnWindow aw = AnWindow.getInstance();
        directory = aw.getCurrentRemoteDirectory();
        if (directory == null) {
          directory = DOT; // NM TEMPORARY
        }
        if (!directory.endsWith(SLASH)) {
          directory += SLASH;
        }
      } else {
        directory = SLASH; // ? BUG!
      }
      fullpath = directory + pathname;
    }
    fullpath = AnFileSystemView.slashifyPath(fullpath);
    // Now let's check if everything is correct
    int error = 0;
    if (fullpath.equals(SLASH)) {
      // System.err.println("AnFile: error = 1; fullpath="+fullpath);
      IPCLogger.logTrace("\n" + "AnFile: error = 1; fullpath=" + fullpath);
      error = 1; // should not come here
    }
    if (null == directory) {
      // System.err.println("AnFile: error = 2; fullpath="+fullpath+" directory="+directory);
      IPCLogger.logTrace(
          "\n" + "AnFile: error = 2; fullpath=" + fullpath + " directory=" + directory);
      error = 2; // should not come here
    }
    if (directory.endsWith(SLASH)) {
      String fn = directory + getName();
      if (!fullpath.equals(fn)) {
        // System.err.println("AnFile: error = 3; fullpath="+fullpath+" directory="+directory+"
        // fn="+fn);
        // Exception e = new Exception();
        // e.printStackTrace();
        IPCLogger.logTrace(
            "\n"
                + "AnFile: error = 3; fullpath="
                + fullpath
                + " directory="
                + directory
                + " fn="
                + fn);
        error = 3;
      }
    } else {
      // System.err.println("AnFile: error = 4; fullpath="+fullpath+" directory="+directory);
      IPCLogger.logTrace(
          "\n" + "AnFile: error = 4; fullpath=" + fullpath + " directory=" + directory);
      error = 4;
      String fn = directory + SLASH + getName();
      if (!fullpath.equals(fn)) {
        // System.err.println("AnFile: error = 5; fullpath="+fullpath+" directory="+directory);
        IPCLogger.logTrace(
            "\n" + "AnFile: error = 5; fullpath=" + fullpath + " directory=" + directory);
        error = 5;
      }
    }
  }

  /**
   * Constructor: expects full directory path and short file name
   *
   * @param dir
   * @param name
   */
  public AnFile(String dir, String name) {
    this(dir + SLASH + name);
  }

  @Override
  public File[] listFiles() {
    File[] ret;
    if (Analyzer.getInstance().remoteConnection == null) {
      ret = super.listFiles();
    } else {
      ret =
          AnFileSystemView.getFileSystemView()
              .getFiles(this, false); // FIXUP: how tp pass isFileHidingEnabled?
    }

    return ret;
  }

  /**
   * Checks if remote file exists.
   *
   * @return
   */
  @Override
  public boolean exists() {
    if (!attributesReady) {
      updateAttributes();
    }
    return existsFlag;
  }

  /**
   * Checks if remote file is a directory.
   *
   * @return
   */
  @Override
  public boolean isDirectory() {
    if (!attributesReady) {
      updateAttributes();
    }
    return isDirectoryFlag;
  }

  /**
   * Checks if remote file is a regular file.
   *
   * @return
   */
  @Override
  public boolean isFile() {
    if (!attributesReady) {
      updateAttributes();
    }
    return !isDirectoryFlag;
  }

  /**
   * Check if file path is absolute
   *
   * @return
   */
  @Override
  public boolean isAbsolute() {
    String pathname = getPath();
    if (pathname.startsWith(SLASH)) {
      return true;
    }
    return false;
  }

  /**
   * Gets path
   *
   * @return path
   */
  @Override
  public String getPath() {
    return this.fullpath;
  }

  /**
   * Returns the absolute form of this abstract pathname. Equivalent to <code>
   * new&nbsp;AnFile(this.{@link #getAbsolutePath})</code>.
   *
   * @return The absolute abstract pathname denoting the same file or directory as this abstract
   *     pathname
   */
  @Override
  public File getAbsoluteFile() {
    if (Analyzer.getInstance().remoteConnection == null) { // Optimization
      return super.getAbsoluteFile();
    }
    // NM why not return this?
    if (!attributesReady) {
      updateAttributes();
    }
    String absPath = getAbsolutePath();
    AnFile af = new AnFile(absPath);
    af.setAttributes(directory, isDirectoryFlag, existsFlag);
    return (af);
  }

  @Override
  public File getCanonicalFile() throws IOException {
    if (Analyzer.getInstance().remoteConnection == null) { // Optimization
      return super.getCanonicalFile();
    }
    return (getAbsoluteFile());
  }

  @Override
  public String getCanonicalPath() /* throws IOException */ {
    return (getAbsolutePath());
  }

  /**
   * Returns the absolute pathname string of this abstract pathname.
   *
   * @return The absolute pathname string denoting the same file or directory as this abstract
   *     pathname
   */
  @Override
  public String getAbsolutePath() {
    String pathname = getPath();
    if (pathname.startsWith(SLASH)) {
      // Temporary fix for Windows
      pathname = AnFileSystemView.slashifyPath(pathname);
      return pathname;
    }
    if (null == directory) {
      // System.err.println("getAbsolutePath: error: directory=null fullpath="+fullpath); // BUG
      IPCLogger.logTrace("\n" + "getAbsolutePath: error: directory=null fullpath=" + fullpath);
      return super.getAbsolutePath(); // This is not AnFile, which means this is the bug!
    }
    if (!directory.endsWith(SLASH)) {
      directory = directory + SLASH; // BUG!
    }
    pathname = directory + pathname;
    // Temporary fix for Windows
    pathname = AnFileSystemView.slashifyPath(pathname);
    return pathname;
  }

  /**
   * Returns the pathname string of this abstract pathname's parent, or <code>null</code> if this
   * pathname does not name a parent directory.
   *
   * <p>The <em>parent</em> of an abstract pathname consists of the pathname's prefix, if any, and
   * each name in the pathname's name sequence except for the last. If the name sequence is empty
   * then the pathname does not name a parent directory.
   *
   * @return The pathname string of the parent directory named by this abstract pathname, or <code>
   *     null</code> if this pathname does not name a parent
   */
  @Override
  public String getParent() {
    if (Analyzer.getInstance().remoteConnection == null) { // Optimization
      return super.getParent();
    }
    String parent = directory;
    if (null == parent) {
      // System.err.println("AnFile: error: getParent(); fullpath="+fullpath+"
      // directory="+directory); // BUG
      IPCLogger.logTrace(
          "\n" + "AnFile: error: getParent(); fullpath=" + fullpath + " directory=" + directory);
      return null;
    }
    if (parent.length() > 1) {
      if (parent.endsWith(SLASH)) {
        parent = parent.substring(0, parent.length() - 1);
      }
    }
    return parent;
  }

  /**
   * Returns the abstract pathname of this abstract pathname's parent, or <code>null</code> if this
   * pathname does not name a parent directory.
   *
   * <p>The <em>parent</em> of an abstract pathname consists of the pathname's prefix, if any, and
   * each name in the pathname's name sequence except for the last. If the name sequence is empty
   * then the pathname does not name a parent directory.
   *
   * @return The abstract pathname of the parent directory named by this abstract pathname, or
   *     <code>null</code> if this pathname does not name a parent
   * @since 1.2
   */
  @Override
  public File getParentFile() {
    if (fullpath == null) {
      fullpath = SLASH;
    }
    if (fullpath.equals(SLASH)) {
      // Special case to make MetalFileChooser happy: return null
      return null;
    }
    String p = getParent();
    if (p == null) {
      return null;
    }
    AnFile af = new AnFile(p); // directory
    return (af);
    // return new AnFile(p, this.prefixLength);
  }

  /**
   * Returns the parent directory of <code>dir</code>.
   *
   * @param dir the <code>File</code> being queried
   * @return the parent directory of <code>dir</code>, or <code>null</code> if <code>dir</code> is
   *     <code>null</code>
   */
  public File getParentDirectory(File dir) {
    if (dir == null || !dir.exists()) {
      return null;
    }
    String psf = dir.getParent();
    if (psf == null) {
      return null;
    }
    AnFile pdir = new AnFile(dir.getParent());
    return pdir;
  }

  /**
   * Gets path
   *
   * @return path
   */
  public String getDirectoryName() {
    return this.directory;
  }

  /**
   * Renames the file denoted by this abstract pathname.
   *
   * <p>Many aspects of the behavior of this method are inherently platform-dependent: The rename
   * operation might not be able to move a file from one filesystem to another, it might not be
   * atomic, and it might not succeed if a file with the destination abstract pathname already
   * exists. The return value should always be checked to make sure that the rename operation was
   * successful.
   *
   * <p>Note that the {@link java.nio.file.Files} class defines the {@link java.nio.file.Files#move
   * move} method to move or rename a file in a platform independent manner.
   *
   * @param dest The new abstract pathname for the named file
   * @return <code>true</code> if and only if the renaming succeeded; <code>false</code> otherwise
   * @throws SecurityException If a security manager exists and its <code>{@link
   *          java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write
   *     access to either the old or new pathnames
   * @throws NullPointerException If parameter <code>dest</code> is <code>null</code>
   */
  @Override
  public boolean renameTo(File dest) {
    if (dest == null) {
      // throw new NullPointerException();
      return false;
    }
    //        if (this.isInvalid() || dest.isInvalid()) {
    //            return false;
    //        }
    boolean ret = false;
    if (dest instanceof AnFile) {
      AnFile newFile = (AnFile) dest;
      String srcName = getAbsolutePath();
      String destName = newFile.getAbsolutePath();
      // System.out.println("Rename to: " + newFile.getAbsolutePath());
      // ret = AnWindow.getInstance().renameFile(srcName, destName); // Not implemented yet
      String res = AnWindow.getInstance().createDirectories(destName);
      if (null == res || res.length() <= 0) { // created
        res = AnWindow.getInstance().deleteFile(srcName);
        if (null == res || res.length() <= 0) { // deleted
          ret = true;
        }
      }
    }
    return ret;
  }

  /**
   * Sets flag isDirectoryFlag
   *
   * @param directory
   * @param isDirectory
   * @param exists
   */
  public void setAttributes(String directory, boolean isDirectory, boolean exists) {
    if (null != directory) {
      if (!directory.endsWith(SLASH)) {
        directory = directory + SLASH; // BUG!
      }
      this.directory = directory;
    }
    isDirectoryFlag = isDirectory;
    existsFlag = exists;
    attributesReady = true;
  }

  /**
   * Gets flag attributesReady
   *
   * @return boolean attributesReady
   */
  public boolean getAttributesReady() {
    return attributesReady;
  }

  /** Update attributes of this remote file */
  public void updateAttributes() {
    if (!attributesReady) {
      if (Analyzer.getInstance().remoteConnection != null) {
        AnWindow aw = AnWindow.getInstance();
        AnFileSystemView afsv = null;
        AnChooser ac = null; // aw.getAnChooserRemote();
        if (null != ac) {
          afsv = ac.getAnFileSystemView();
        } else {
          afsv = AnFileSystemView.getFileSystemView();
          if (null != afsv) {
            afsv.setAnWindow(aw);
          }
        }
        if (null != afsv) {
          afsv.updateFileAttributes(this);
          attributesReady = true;
        }
      }
    }
  }

  /**
   * Tests this abstract pathname for equality with the given object. Returns <code>true</code> if
   * and only if the argument is not <code>null</code> and is an abstract pathname that denotes the
   * same file or directory as this abstract pathname. Whether or not two abstract pathnames are
   * equal depends upon the underlying system. On UNIX systems, alphabetic case is significant in
   * comparing pathnames; on Microsoft Windows systems it is not.
   *
   * @param obj The object to be compared with this abstract pathname
   * @return <code>true</code> if and only if the objects are the same; <code>false</code> otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if ((obj != null) && (obj instanceof AnFile)) {
      return compareTo((AnFile) obj) == 0;
    }
    return super.equals(obj);
  }

  /**
   * Compares two abstract pathnames lexicographically. The ordering defined by this method depends
   * upon the underlying system. On UNIX systems, alphabetic case is significant in comparing
   * pathnames; on Microsoft Windows systems it is not.
   *
   * @param pathname The abstract pathname to be compared to this abstract pathname
   * @return Zero if the argument is equal to this abstract pathname, a value less than zero if this
   *     abstract pathname is lexicographically less than the argument, or a value greater than zero
   *     if this abstract pathname is lexicographically greater than the argument
   * @since 1.2
   */
  @Override
  public int compareTo(File pathname) {
    if ((pathname != null) && (pathname instanceof AnFile)) {
      return ((pathname.getPath()).compareTo(this.getPath()));
    }
    return super.compareTo(pathname);
  }

  /**
   * Deletes the file or directory denoted by this abstract pathname. If this pathname denotes a
   * directory, then the directory must be empty in order to be deleted.
   *
   * @return <code>true</code> if and only if the file or directory is successfully deleted; <code>
   *     false</code> otherwise
   */
  @Override
  public boolean delete() {
    if (!exists()) {
      return true;
    }
    if (Analyzer.getInstance().remoteConnection != null) {
      AnWindow aw = AnWindow.getInstance();
      String error = aw.deleteFile(fullpath);
      if (error != null && error.length() > 0) {
        return false;
      }
    } else {
      return super.delete();
    }
    attributesReady = false;
    return true;
  }

  /**
   * Creates the directory named by this abstract pathname, including any necessary but nonexistent
   * parent directories. Note that if this operation fails it may have succeeded in creating some of
   * the necessary parent directories.
   *
   * @return <code>true</code> if and only if the directory was created, along with all necessary
   *     parent directories; <code>false</code> otherwise
   */
  @Override
  public boolean mkdirs() {
    if (exists()) {
      return false;
    }
    if (Analyzer.getInstance().remoteConnection != null) {
      AnWindow aw = AnWindow.getInstance();
      String error = aw.createDirectories(fullpath);
      if (error != null && error.length() > 0) {
        return false;
      }
    } else {
      return super.mkdirs();
    }
    attributesReady = false;
    return true;
  }
}
