/* Copyright (C) 2022-2025 Free Software Foundation

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

import org.gprofng.mpmt.IPC.AnIPCException;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.util.gui.AnUtility;

/** AnFileSystemView - File System View for remote experiments */
public class AnFileSystemView extends FileSystemView {

  private AnWindow anWindow = null;
  private AnChooser anChooser = null;
  private static final String SLASH = "/";
  private static final String DOUBLE_SLASH = "//";
  private static final char SLASH_CHAR = '/';
  private static final char BACKSLASH_CHAR = '\\';
  private static final char COLUMN_CHAR = ':';

  private static int ThreadID = 0;
  private File[] roots = null;

  static AnFileSystemView remoteFileSystemView = null;
  static boolean useSystemExtensionsHiding = false;

  private final boolean DONT_USE_LS = false;

  public static AnFileSystemView getFileSystemView() {
    useSystemExtensionsHiding =
        UIManager.getDefaults().getBoolean("FileChooser.useSystemExtensionHiding");
    UIManager.addPropertyChangeListener(
        new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("lookAndFeel")) {
              useSystemExtensionsHiding =
                  UIManager.getDefaults().getBoolean("FileChooser.useSystemExtensionHiding");
            }
          }
        });
    if (remoteFileSystemView == null) {
      remoteFileSystemView = new RemoteFileSystemView();
    }
    return remoteFileSystemView;
  }

  /**
   * Returns all root partitions on this system. For example, on Windows, this would be the
   * "Desktop" folder, while on DOS this would be the A: through Z: drives.
   *
   * @return 
   */
  @Override
  public File[] getRoots() {
    if (null != roots) {
      return roots;
    }
    roots = new AnFile[1];
    AnFile root = new AnFile(SLASH);
    roots[0] = root; // createFileSystemRoot(root);
    return roots;
  }

  /**
   * Is dir the root of a tree in the file system, such as a drive or partition. Example: Returns
   * true for "C:\" on Windows 98.
   *
   * @param dir a <code>File</code> object representing a directory
   * @return <code>true</code> if <code>f</code> is a root of a filesystem
   * @see #isRoot
   * @since 1.4
   */
  @Override
  public boolean isFileSystemRoot(File dir) {
    if (dir != null) {
      String p = dir.getAbsolutePath();
      if (SLASH.equals(p)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the list of shown (i.e.not hidden) files.
   * Use IPC_getFiles to get the list of remote files
   * 
   * @param directory
   * @param isFileHidingEnabled
   * @return 
   */
  @Override
  public synchronized File[] getFiles(File directory, boolean isFileHidingEnabled) {
    // Temporary fix for Windows
    String path = slashifyPath(directory.getPath());
    AnFile dir = new AnFile(path);
    dir.isDirectoryFlag = true;
    dir.attributesReady = true;
    ThreadID++;
    ArrayList<AnFile> anFiles = new ArrayList<>();
    String dirname = dir.getAbsolutePath(); // dir.getCanonicalPath();
    try {
      String filenames;
      if (DONT_USE_LS) {
        filenames = IPC_getFiles(dirname, "/bin/ls -aF");
      } else {
        String cmd = "/usr/bin/ls -";
        if (isFileHidingEnabled) {
          cmd += "a";
        }
        filenames = AnUtility.getRemoteOutput(cmd + "F1 " + dirname);
      }
//      AnLog.log(String.format("getFiles: %d %s\n%s", ThreadID, path, filenames));
      while (filenames.length() > 0) {
        int j = filenames.indexOf("\n");
        if (j <= 0) {
          break;
        }
        String fn = filenames.substring(0, j);
        filenames = filenames.substring(j + 1);
        char sym = fn.charAt(j - 1);
        if (sym == '/' || sym == '*' || sym == '@') {
          fn = fn.substring(0, j - 1);
        }
        if (fn.equals(".") || fn.equals("..") ||
            (!isFileHidingEnabled && fn.startsWith("."))) {
          continue;
        }
        AnFile f = new AnFile(fn);
        anFiles.add(f);
        f.executableFlag = sym == '*';
        f.setAttributes(path, sym == '/', true);
      }
    } catch (AnIPCException e) {
      AnLog.log("AnFileSystemView.getFiles: Thread ID=" + ThreadID + " " + e);
    } catch (Exception e) {
    }
    return anFiles.toArray(new File[anFiles.size()]);
  }

  /**
   * Process File Attributes
   *
   * @param dir
   * @param file
   * @param filename
   * @param fattr
   * @return
   */
  private boolean processFileAttributes(
      String /*AnFile*/ dir, AnFile file, String filename, String fattr) {
    int j = fattr.indexOf("\n");
    if (j > 0) {
      fattr = fattr.substring(0, j);
    }
    if (fattr.endsWith(filename)) {
      if (fattr.startsWith("d")) {
        file.setAttributes(dir, true, true);
      }
      if (fattr.startsWith("-")) {
        file.setAttributes(dir, false, true);
      }
      file.attributesReady = true;
      return true;
    }
    return false;
  }

  /**
   * Get file attributes and saves it in AnFile
   *
   * @param file
   * @return true if no exceptions happened
   */
  public boolean updateFileAttributes(AnFile file) {
    return updateFileAttributes(null, file);
  }

  /**
   * Get file attributes and saves it in AnFile
   *
   * @param dir
   * @param file
   * @return true if no exceptions happened
   */
  public boolean updateFileAttributes(AnFile dir, AnFile file) {
    if (file.getAttributesReady()) {
      return true;
    }
    String LS_CMD = "/bin/ls -dl ";
    try {
      String dirname = null;
      if (dir != null) {
        dirname = dir.getCanonicalPath();
      }
      String filename = file.getPath();
      // Try to get this info from gp-display-text
      String fullfilename = filename;
      if (!fullfilename.startsWith(SLASH)) {
        if (null != dirname) {
          if (!dirname.endsWith(SLASH)) {
            dirname += SLASH;
          }
          // Temporary fix for Windows
          dirname = slashifyPath(dirname);
          fullfilename = dirname + filename;
        } else {
          fullfilename = SLASH + filename; // BUG!
        }
      }
      String fattr = IPC_getFileAttributes(fullfilename, LS_CMD);
      if (null != fattr) {
        if (fattr.length() > 0) {
          if (processFileAttributes(dirname, file, fullfilename, fattr)) {
            return true;
          }
        }
        fattr = null;
      }
      if (null == fattr) {
        file.existsFlag = false;
        file.isDirectoryFlag = false;
        file.attributesReady = true;
      }
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return false;
  }

  /**
   * Update File Attributes if the name matches
   *
   * @param files
   * @param filename
   * @param index
   * @param dir
   * @return true if updated
   */
  private boolean updateFile(AnFile[] files, String filename, int index, /*AnFile*/ String dir) {
    boolean isDir = false;
    boolean exists = true;
    if (index >= files.length) {
      return false;
    }
    String s = files[index].getName(); // NM was files[index].getPath();
    if (filename.endsWith(SLASH)) {
      isDir = true;
      filename = filename.substring(0, filename.length() - 1);
      if (filename.compareTo(s) == 0) {
        files[index].setAttributes(dir, isDir, exists);
        return true;
      }
      return false;
    }
    if (filename.compareTo(s) == 0) { // Regular file
      files[index].setAttributes(dir, isDir, exists);
      return true;
    } else if (filename.endsWith("@")) { // Link
      String fs = filename.substring(0, filename.length() - 1);
      if (fs.compareTo(s) == 0) {
        isDir = true;
        files[index].setAttributes(dir, isDir, exists);
        return true;
      }
    } else if (filename.endsWith("*")) {
      String fs = filename.substring(0, filename.length() - 1);
      if (fs.compareTo(s) == 0) {
        isDir = false;
        files[index].setAttributes(dir, isDir, exists);
        return true;
      }
    }
    return false;
  }

  /**
   * Internal function to process 'ls -aF' output and update file attributes
   *
   * @param files
   * @param filenames
   * @return
   */
  private boolean processFileAttributes(String dir, AnFile[] files,
      String filenames) {
    int index = 0;
    String fn = filenames;
    while (filenames.length() > 0) {
      int j = filenames.indexOf("\n");
      if (j > 0) {
        fn = filenames.substring(0, j);
        filenames = filenames.substring(j + 1);
      } else {
        break;
      }
      if (updateFile(files, fn, index, dir)) {
        if (index < files.length - 1) {
          index++;
        }
        continue;
      }
      // Search through all files
      int i;
      for (i = 0; i < files.length; i++) {
        if (updateFile(files, fn, i, dir)) {
          break;
        }
      }
      if (i < files.length - 1) {
        index = i + 1;
      }
    }
    return true;
  }

  /**
   * Name of a file, directory, or folder as it would be displayed in a system file browser. Example
   * from Windows: the "M:\" directory displays as "CD-ROM (M:)"
   *
   * <p>The default implementation gets information from the ShellFolder class.
   *
   * @param f a <code>File</code> object
   * @return the file name as it would be displayed by a native file chooser
   * @see JFileChooser#getName
   * @since 1.4
   */
  @Override
  public String getSystemDisplayName(File f) {
    if (f == null) {
      return null;
    }
    return f.getName();
  }

  /** Creates a new folder with a default folder name. */
  @Override
  public File createNewFolder(File containingDir) throws IOException {
    // anWindow.createDirectory(dirname);
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Sets AnWindows.
   * @param a
   */
  public void setAnWindow(AnWindow a) {
    anWindow = a;
  }

  /** Gets AnWindows.
   * @return
   */
  public AnWindow getAnWindow() {
    return (anWindow);
  }

  public void setAnChooser(AnChooser c) {
    anChooser = c;
  }

  /**
   * Get File Attributes via IPC call
   *
   * @param filename
   * @param format
   * @return String
   */
  public String IPC_getFileAttributes(final String filename, final String format) {
    IPCContext fsvContext =
        IPCContext.newCurrentContext(
            AnLocale.getString("File Chooser"), IPCContext.Scope.SYSTEM, false, anWindow);
    fsvContext.setCancellable(true);
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, fsvContext);
    ipcHandle.append("getFileAttributes");
    ipcHandle.append(filename);
    ipcHandle.append(format);
    IPCResult ipcResult = ipcHandle.sendRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      anChooser.cancelChooser();
    }
    String result = ipcResult.getString(); // blocking
    if (result == null) {
      System.err.println("Error: ipcHandle: " + ipcHandle);
    }
    anWindow.getSystemProgressPanel().setProgressCancelContext(null);
    return result;
  }

  /**
   * Get Files via IPC call
   *
   * @param dirname
   * @param format
   * @return String
   */
  private synchronized String IPC_getFiles(final String dirname, final String format) {
    IPCContext fsvContext =
        IPCContext.newCurrentContext(
            AnLocale.getString("File Chooser"), IPCContext.Scope.SYSTEM, false, anWindow);
    fsvContext.setCancellable(true);
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, fsvContext);
    ipcHandle.append("getFiles");
    ipcHandle.append(dirname);
    ipcHandle.append(format);
    IPCResult ipcResult = ipcHandle.sendRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      anChooser.cancelChooser();
    }
    String result = ipcResult.getString(); // blocking
    anWindow.getSystemProgressPanel().setProgressCancelContext(null);
    //        System.out.println("--------------------------- \'" + dirname + "\' \'" + format +
    // "\'");
    //        System.out.println("IPC_getFiles: \n" + result);
    return result;
  }

  public static String slashifyPath(String path) {
    String s = path;
    if (null == s) {
      return null;
    }
    // Temporary code for Windows
    if (s.length() >= 3) {
      if ((s.charAt(1) == COLUMN_CHAR) && (s.charAt(2) == BACKSLASH_CHAR)) {
        // Special case
        if (s.length() > 3) {
          s = SLASH + s.substring(3);
        } else {
          s = SLASH;
        }
      }
    }
    // Replace all \ characters with / characters
    s = s.replace(BACKSLASH_CHAR, SLASH_CHAR);
    // Replace double slashes with single slash
    int n = s.indexOf(DOUBLE_SLASH);
    while (n >= 0) {
      s = s.substring(0, n) + s.substring(n + 1);
      n = s.indexOf(DOUBLE_SLASH);
    }
    return s;
  }
} // End of AnFileSystemView

/** Remote FileSystemView */
class RemoteFileSystemView extends AnFileSystemView {

  private static final String SLASH = "/";
  private static final char SLASH_CHAR = '/';
  private AnFile root = null;
  private AnFile home = null;

  /** Returns a File object constructed from the given path string. */
  @Override
  public File createFileObject(String path) {
    AnFile f = new AnFile(path);
    updateFileAttributes(f);
    return f;
  }

  /** Returns a File object constructed from the given path string. */
  public File createFileObject(File parent, String fileName) {
    String path = parent.getPath();
    if (path.endsWith(SLASH)) {
      path = path + fileName;
    } else {
      path = path + SLASH + fileName;
    }
    return createFileObject(path);
  }

  /** Creates a new folder with a default folder name. */
  @Override
  public File createNewFolder(File containingDir) throws IOException {
    if (containingDir == null) {
      throw new IOException("Containing directory is null:");
    }
    String res = null;
    AnFile newFolder = null;
    if (containingDir instanceof AnFile) {
      String defaultFolderName = UIManager.getString("FileChooser.other.newFolder");
      newFolder = (AnFile) containingDir;
      String folderName = newFolder.getAbsolutePath() + SLASH + defaultFolderName;
      newFolder = new AnFile(folderName);
      // System.out.println("Create New Folder: " + newFolder.getAbsolutePath());
      res = getAnWindow().createDirectories(newFolder.getAbsolutePath());
    }
    if (null == newFolder) {
      throw new IOException("Create New Folder is not implemented yet.");
    }
    if (null != res && res.length() > 0) {
      throw new IOException("Create New Folder failed: " + res);
    }
    return newFolder;
  }

  /**
   * @param parent a <code>File</code> object representing a directory or special folder
   * @param fileName a name of a file or folder which exists in <code>parent</code>
   * @return a File object. This is normally constructed with <code>new
   * File(parent, fileName)</code> except when parent and child are both special folders, in which
   *     case the <code>File</code> is a wrapper containing a <code>ShellFolder</code> object.
   * @since 1.4
   */
  @Override
  public File getChild(File parent, String fileName) {
    return createFileObject(parent, fileName);
  }

  @Override
  public File getHomeDirectory() {
    if (null == home) {
      String HomeDir = AnWindow.getInstance().getHomeDir();
      if (HomeDir != null) {
        home = new AnFile(HomeDir);
        return home;
      }
    } else {
      return home;
    }
    if (null == root) {
      root = new AnFile(SLASH);
    }
    return (root);
  }

  /**
   * Return the user's default starting directory for the file chooser.
   *
   * @return a <code>File</code> object representing the default starting folder
   * @since 1.4
   */
  @Override
  public File getDefaultDirectory() {
    if (null == root) {
      root = new AnFile(SLASH);
    }
    return (root);
  }

  /**
   * Returns the parent directory of <code>dir</code>.
   *
   * @param dir the <code>File</code> being queried
   * @return the parent directory of <code>dir</code>, or <code>null</code> if <code>dir</code> is
   *     <code>null</code>
   */
  @Override
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
   * On Windows, a file can appear in multiple folders, other than its parent directory in the
   * filesystem. Folder could for example be the "Desktop" folder which is not the same as
   * file.getParentFile().
   *
   * @param folder a <code>File</code> object repesenting a directory or special folder
   * @param file a <code>File</code> object
   * @return <code>true</code> if <code>folder</code> is a directory or special folder and contains
   *     <code>file</code>.
   * @since 1.4
   */
  @Override
  public boolean isParent(File folder, File file) {
    if (folder == null || file == null) {
      return false;
    } // NM  else if (folder instanceof ShellFolder) {
    File parent = file.getParentFile();
    if (parent != null && parent.equals(folder)) {
      return true;
    }
    File[] children = getFiles(folder, false);
    for (int i = 0; i < children.length; i++) {
      if (file.equals(children[i])) {
        return true;
      }
    }
    return false;
    // NM } else {
    // NM     return folder.equals(file.getParentFile());
    // NM }
  }

  /**
   * Used by UI classes to decide whether to display a special icon for a computer node, e.g. "My
   * Computer" or a network server.
   *
   * <p>The default implementation has no way of knowing, so always returns false.
   *
   * @param dir a directory
   * @return <code>false</code> always
   * @since 1.4
   */
  @Override
  public boolean isComputerNode(File dir) {
    // return ShellFolder.isComputerNode(dir);
    return false;
  }

  /**
   * Checks if <code>f</code> represents a real directory or file as opposed to a special folder
   * such as <code>"Desktop"</code>. Used by UI classes to decide if a folder is selectable when
   * doing directory choosing.
   *
   * @param f a <code>File</code> object
   * @return <code>true</code> if <code>f</code> is a real file or directory.
   * @since 1.4
   */
  @Override
  public boolean isFileSystem(File f) {
    if (f.isDirectory()) {
      return true;
    }
    return false;
  }

  /* Normalize the given pathname, whose length is len, starting at the given
  offset; everything before this offset is already normal. */
  private String normalize(String pathname, int len, int off) {
    if (len == 0) return pathname;
    int n = len;
    while ((n > 0) && (pathname.charAt(n - 1) == SLASH_CHAR)) n--;
    if (n == 0) return SLASH;
    StringBuilder sb = new StringBuilder(pathname.length());
    if (off > 0) sb.append(pathname.substring(0, off));
    char prevChar = 0;
    for (int i = off; i < n; i++) {
      char c = pathname.charAt(i);
      if ((prevChar == SLASH_CHAR) && (c == SLASH_CHAR)) continue;
      sb.append(c);
      prevChar = c;
    }
    return sb.toString();
  }

  /* Check that the given pathname is normal.  If not, invoke the real
  normalizer on the part of the pathname that requires normalization.
  This way we iterate through the whole pathname string only once. */
  public String normalize(String pathname) {
    int n = pathname.length();
    char prevChar = 0;
    for (int i = 0; i < n; i++) {
      char c = pathname.charAt(i);
      if ((prevChar == SLASH_CHAR) && (c == SLASH_CHAR)) return normalize(pathname, n, i - 1);
      prevChar = c;
    }
    if (prevChar == SLASH_CHAR) return normalize(pathname, n, n - 1);
    return pathname;
  }

  public int prefixLength(String pathname) {
    if (pathname.length() == 0) return 0;
    return (pathname.charAt(0) == SLASH_CHAR) ? 1 : 0;
  }
} // End of RemoteFileSystemView
