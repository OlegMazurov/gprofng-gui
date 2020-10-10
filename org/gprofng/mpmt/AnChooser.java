/* Copyright (C) 2022 Free Software Foundation

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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.ipc.IPCLogger;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileSystemView;

public class AnChooser extends JFileChooser implements PropertyChangeListener {

  private static final int EXP_NOT = 0;
  private static final int EXP_NORMAL = 1;
  private static final int EXP_GROUP = 2;
  private static final int EXP_BAD = 3;
  private static final int EXP_OLD = 4;
  public static final int EXP_CHOOSER = 0;
  public static final int EXP_GROUP_CHOOSER = 1;
  public static final int TARGET_CHOOSER = 2;
  public static final int DIR_CHOOSER = 3;
  public static final int DIR_FILE_CHOOSER = 4;
  private static String startDir = Analyzer.getInstance().getWorkingDirectory();
  private static final String open_text = AnLocale.getString("Open");
  private static final String add_text = AnLocale.getString("Add");
  private static final String set_text = AnLocale.getString("Set");
  private final int chooser_type;
  private final AnWindow window;
  private final Component comp;
  private JPanel previewPanel;
  private AnList previewList;
  private ConfigurationPanel anChooserExtraPanel;
  private String title_str, approve_button_text;
  private static AnFileSystemView anFileSystemView = null;
  private AnFile currentRemoteDirectory = null;
  private final String SLASH = "/";

  /**
   * Common constructor. Used by all constructors in this class.
   *
   * @param comp
   * @param chooser_type
   * @param window
   * @param title
   * @param st_dir
   * @param rhost
   * @param fsv
   */
  public AnChooser(
      final Component comp,
      final int chooser_type,
      final AnWindow window,
      final String title,
      final String st_dir,
      String rhost,
      FileSystemView fsv) {
    super(st_dir, fsv);
    this.chooser_type = chooser_type;
    this.window = window;
    this.comp = comp;
    this.anFileSystemView = (AnFileSystemView) fsv;
    initAnChooser(chooser_type, title, rhost, fsv);
  }

  /**
   * Common constructor. Used by all constructors in remote mode.
   *
   * @param comp
   * @param chooser_type
   * @param window
   * @param restart
   * @param st_dir
   * @param rhost
   * @param fsv
   */
  public AnChooser(
      final Component comp,
      final int chooser_type,
      final AnWindow window,
      final String title,
      final AnFile st_dir,
      String rhost,
      FileSystemView fsv) {
    super(st_dir, fsv);
    this.chooser_type = chooser_type;
    this.window = window;
    this.comp = comp;
    this.anFileSystemView = (AnFileSystemView) fsv;
    initAnChooser(chooser_type, title, rhost, fsv);
    if (Analyzer.getInstance().remoteConnection != null) {
      setCurrentDirectory(st_dir);
    }
  }

  /**
   * Initialize AnChooser
   *
   * @param chooser_type
   * @param title
   * @param rhost
   * @param fsv
   */
  private void initAnChooser(
      final int chooser_type, final String title, String rhost, FileSystemView fsv) {

    setFileHidingEnabled(false);
    setFileView(new ExpView());

    if (chooser_type != EXP_CHOOSER) {
      approve_button_text = set_text;
    }
    if (chooser_type == EXP_CHOOSER || chooser_type == EXP_GROUP_CHOOSER) {
      setFileSelectionMode(FILES_AND_DIRECTORIES);
      setFileFilter(new ExpFilter());
      initComponents();
    } else if (chooser_type == TARGET_CHOOSER) {
      setFileSelectionMode(FILES_ONLY);
    } else if (chooser_type == DIR_FILE_CHOOSER) {
      setFileSelectionMode(FILES_AND_DIRECTORIES);
    } else { // chooser_type == DIR_CHOOSER
      setFileSelectionMode(DIRECTORIES_ONLY);
    }
    if (chooser_type == EXP_CHOOSER) {
      setMultiSelectionEnabled(true);
      if (null != title) {
        String newtitle = title;
        if (rhost != null) {
          newtitle = title + "   " + rhost + "";
        }
        setDialogTitle(newtitle);
        if (newtitle.startsWith(AnLocale.getString("Add Experiment"))) {
          approve_button_text = add_text;
        } else {
          approve_button_text = open_text;
        }
        setApproveButtonText(approve_button_text);
      }
    }
    addPropertyChangeListener(this);
  }

  /**
   * This constructor is used by CollectPanel
   *
   * @param comp
   * @param chooser_type
   * @param window
   * @param restart
   * @param directory
   */
  public AnChooser(
      final Component comp, final int chooser_type, final AnWindow window, final String directory) {
    this(comp, chooser_type, window, null /* title */, directory, null, null);
  }

  /**
   * This constructor is used by AnWindow for local file system
   *
   * @param comp
   * @param awindow
   * @param title
   * @param restart
   */
  public AnChooser(final Component comp, final AnWindow awindow, final String title) {
    this(comp, awindow, title, null, null);
  }

  /**
   * This constructor is used by AnWindow for remote file system
   *
   * @param comp
   * @param awindow
   * @param title
   * @param title
   * @param rhost
   * @param fsv
   */
  public AnChooser(
      final Component comp,
      final AnWindow awindow,
      final String title,
      String rhost,
      FileSystemView fsv) {
    this(comp, EXP_CHOOSER, awindow, title, startDir, rhost, fsv);
  }

  @Override
  public final void setDialogTitle(final String title) {
    super.setDialogTitle(title);
    title_str = title;
  }

  //    @Override
  //    public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
  //        if (chooser_type == EXP_CHOOSER || chooser_type == EXP_GROUP) {
  //            setControlButtonsAreShown(false);
  //            AnDialog2 anDialog2 = new AnDialog2(parent, getDialogTitle());
  //            anDialog2.getOKButton().setText(getApproveButtonText());
  //            JPanel panel = new JPanel();
  //            panel.setLayout(new BorderLayout());
  //            panel.add(this, BorderLayout.CENTER);
  //            anDialog2.setCustomPanel(panel);
  //            anDialog2.setVisible(true);
  //            AnDialog2.Status status = anDialog2.getStatus();
  //            if (status == AnDialog2.Status.OK) {
  //                return AnChooser.APPROVE_OPTION;
  //            }
  //            else {
  //                return AnChooser.CANCEL_OPTION;
  //            }
  //        }
  //        else {
  //            setControlButtonsAreShown(true);
  //            return(super.showDialog(parent, approveButtonText)); //To change body of generated
  // methods, choose Tools | Templates.
  //        }
  //    }
  /**
   * Get AnFileSystemView
   *
   * @return anFileSystemView
   */
  public final AnFileSystemView getAnFileSystemView() {
    return anFileSystemView;
  }

  /**
   * Returns the current directory.
   *
   * @return the current directory
   * @see #setCurrentDirectory
   */
  @Override
  public File getCurrentDirectory() {
    if (Analyzer.getInstance().remoteConnection != null) { // NM
      if (null == currentRemoteDirectory) {
        AnFile af = new AnFile(SLASH);
        af.setAttributes(SLASH, true, true); // NM Is it correct??
        currentRemoteDirectory = af;
      }
      // Temporary fix for Windows
      String dirname = currentRemoteDirectory.getPath();
      if (dirname.length() >= 3) {
        if ((dirname.charAt(1) == ':') && (dirname.charAt(2) == '\\')) {
          if (dirname.length() > 3) {
            dirname = SLASH + dirname.substring(3);
          } else {
            dirname = SLASH;
          }
          AnFile af = new AnFile(dirname);
          af.setAttributes(dirname, true, true); // NM Is it correct??
          currentRemoteDirectory = af;
        }
      }
      return currentRemoteDirectory;
    }
    return super.getCurrentDirectory();
  }

  /**
   * Sets the current directory.
   *
   * @param dir
   */
  public void setCurrentDirectory(File curdir) {
    if (Analyzer.getInstance().remoteConnection == null) {
      super.setCurrentDirectory(curdir);
      return;
    }
    // NM Temporary implementation for remote case
    if (curdir == null) {
      return;
    }
    AnFile oldValue = currentRemoteDirectory;
    String s = curdir.getPath();
    // Temporary code for Windows
    s = AnFileSystemView.slashifyPath(s);
    AnFile dir = new AnFile(s);
    // Update attributes
    if (anFileSystemView != null) {
      anFileSystemView.updateFileAttributes(dir);
    } else { // anFileSystemView can be null only during the first call from super
      currentRemoteDirectory = dir; // just set currentRemoteDirectory and return
      return;
    }

    if (dir != null && !dir.exists()) { // ???
      // dir = currentRemoteDirectory; // why not return?
      return;
    }
    // if (dir == null) {
    //    dir = getFileSystemView().getDefaultDirectory();
    // }
    if (currentRemoteDirectory != null) {
      /* Verify the toString of object */
      if (currentRemoteDirectory.equals(dir)) {
        return;
      }
    }

    AnFile prev = null;
    while (!isTraversable(dir) && prev != dir) {
      prev = new AnFile(dir.getParent());
      if (prev == null) {
        return;
      }
      dir = prev;
      // dir = getFileSystemView().getParentDirectory(dir);
    }
    currentRemoteDirectory = dir;
    if (s.startsWith("//")) {
      prev = null; // BUG
    }
    // System.err.println("AnChooser.setCurrentDirectory:"+currentRemoteDirectory);
    try {
      firePropertyChange(DIRECTORY_CHANGED_PROPERTY, oldValue, currentRemoteDirectory);
    } catch (NullPointerException npe) {
      // CR 7199013, 7200045
      // System.err.println("AnChooser.setCurrentDirectory:274: NPE CR 7200045 File=" +
      // currentRemoteDirectory.getAbsolutePath());
      IPCLogger.logTrace(
          "\n"
              + "AnChooser.setCurrentDirectory:274: NPE CR 7200045 File="
              + currentRemoteDirectory.getAbsolutePath());
      // npe.printStackTrace();
      currentRemoteDirectory = oldValue;
    }
  }

  /** Returns a list of selected files if the file chooser is set to allow multiple selection. */
  public File[] getSelectedAnFiles() {
    File[] files = getSelectedFiles();
    if (Analyzer.getInstance().remoteConnection == null) {
      return files;
    }
    if (anFileSystemView != null) {
      for (int i = 0; i < files.length; i++) {
        if (files[i] instanceof AnFile) {
          continue;
        }
        // Replace File with AnFile
        String file_name = files[i].getAbsolutePath();
        AnFile af = new AnFile(file_name);
        anFileSystemView.updateFileAttributes(af);
        files[i] = (File) af;
      }
    }
    return files;
  }

  /**
   * Returns the selected file. This can be set either by the programmer via <code>setFile</code> or
   * by a user action, such as either typing the filename into the UI or selecting the file from a
   * list in the UI.
   *
   * @see #setSelectedFile
   * @return the selected file
   */
  public File getSelectedAnFile() {
    File file = getSelectedFile();
    if (Analyzer.getInstance().remoteConnection == null) {
      return file;
    }
    if ((file != null) && (anFileSystemView != null)) {
      if (file instanceof AnFile) {
        return file;
      }
      String file_name = file.getName();
      if (null == file_name) {
        return null;
      }
      if (file_name.startsWith(SLASH)) {
        AnFile af = new AnFile(file_name);
        anFileSystemView.updateFileAttributes(af);
        file = (File) af;
      } else {
        AnFile dir = (AnFile) getCurrentDirectory();
        AnFile af = new AnFile(dir.getPath(), file_name);
        anFileSystemView.updateFileAttributes(dir, af);
        file = (File) af /*.getAbsoluteFile() */;
      }
    }
    return file;
  }

  // Initialize GUI components
  private void initComponents() {
    final JLabel label;
    final JScrollPane scrollPane;

    // Add list for preview
    previewList = new AnList(true);
    scrollPane = new AnJScrollPane(previewList);
    scrollPane.setBorder(BorderFactory.createLineBorder(AnEnvironment.SCROLLBAR_BORDER_COLOR));

    // Add label for preview
    label = new JLabel(AnLocale.getString("Experiment Preview:"));
    label.setBorder(AnVariable.labelBorder);
    label.setDisplayedMnemonic(AnLocale.getString('E', "MNEM_EXPERIMENT_PREVIEW"));
    label.setLabelFor(previewList);
    AccessibleContext context = previewList.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Experiment Preview"));
    context.setAccessibleDescription(AnLocale.getString("Experiment Preview"));
    context = label.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Experiment Preview"));
    context.setAccessibleDescription(AnLocale.getString("Experiment Preview"));
    previewPanel = new JPanel(new GridBagLayout()); // no ANJPanel
    previewPanel.setPreferredSize(new Dimension(AnVariable.FVIEW_SIZE));
    GridBagConstraints gridBagConstraints;
    int gridy = 0;

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(6, 6, 0, 0);
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    previewPanel.add(label, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(12, 6, 0, 0);
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    previewPanel.add(scrollPane, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(12, 6, 0, 0);
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    previewPanel.add(anChooserExtraPanel = new ConfigurationPanel(true), gridBagConstraints);
    anChooserExtraPanel.setVisible(false);

    setAccessory(previewPanel);
  }

  // override createDialog() to workaround JDK accessibility bug
  @Override
  protected final JDialog createDialog(final Component parent) {
    final JDialog dialog = super.createDialog(parent);
    final AccessibleContext context = dialog.getAccessibleContext();
    context.setAccessibleName(title_str);
    context.setAccessibleDescription(title_str);
    return dialog;
  }

  // Listener for file chooser
  @Override
  public final void propertyChange(final PropertyChangeEvent evt) {
    AnUtility.dispatchOnAWorkerThread(
        () -> {
          PropertyChangeInternal(evt);
        },
        "PropertyChangeThread");
    // Run this on a thread so that the AWT does not hang when fileChooser is waiting for a bad
    // remote file
  }

  private final void PropertyChangeInternal(final PropertyChangeEvent evt) {
    final File file;
    final String[] exp_info;
    String header, value;
    final int type, size;
    final Dimension msize;

    if (!evt.getPropertyName().equals(SELECTED_FILE_CHANGED_PROPERTY)) {
      return;
    }

    file = getSelectedAnFile();
    if (file == null) {
      return;
    }

    type = getExperimentType(file);

    if ((chooser_type != DIR_CHOOSER) && (chooser_type != DIR_FILE_CHOOSER)) {
      if (type == EXP_NOT && file.isDirectory()) {
        setApproveButtonText(open_text);
      } else {
        setApproveButtonText(approve_button_text);
      }
    }

    if (previewList == null) {
      return;
    }
    previewList.removeAll();
    anChooserExtraPanel.setVisible(false);

    if ((type == EXP_NORMAL) || (type == EXP_GROUP)) {
      // Get experiment preview
      exp_info = window.getExpPreview(file.getAbsolutePath());
      size = exp_info.length;
      anChooserExtraPanel.initConfigurationChoices(file);
      anChooserExtraPanel.setWorkingDirectory(file.getParent());
      anChooserExtraPanel.setVisible(true);

      for (int i = 0; i < size; ) {
        header = exp_info[i++];
        value = exp_info[i++];

        previewList.add(
            AnUtility.getItem(header + ":"),
            value.indexOf('\n') == -1 ? AnUtility.getText(value, 0) : AnUtility.getTextArea(value));
      }

      //            msize = previewList.getMaximumSize();
      //            msize.height = previewList.getPreferredSize().height;
      //            previewList.setMaximumSize(msize);
    }

    previewList.repaint();
    previewList.invalidate();
    previewPanel.validate();
  }

  public List<String> getExperiments() {
    final List<String> list = new ArrayList<String>();
    String errstr = "";
    final File[] files = getSelectedAnFiles();
    if (files != null) {
      // Get file list
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        int type = getExperimentType(file);
        if ((type == EXP_NORMAL) || (type == EXP_GROUP)) {
          String path = file.getAbsolutePath();
          List<String> exp_list = AnUtility.getGroupList(path);
          if (exp_list.size() > 1) {
            list.add(path);
          } else {
            list.addAll(exp_list);
          }
        } else {
          if (!errstr.equals("")) {
            errstr += "\n";
          }
          if (type == EXP_OLD) {
            errstr += AnLocale.getString("Old experiment is not supported:");
          } else if (type == EXP_BAD) {
            errstr += AnLocale.getString("Bad experiment:");
          } else {
            errstr += AnLocale.getString("Not a valid experiment:");
          }
          errstr += " " + file;
        }
      }
    }

    // Show error mesg
    if (!errstr.equals("")) {
      AnUtility.showMessage(comp, errstr, JOptionPane.ERROR_MESSAGE);
      window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);
    }
    return list;
  }

  // Get experiment type
  public static int getExperimentType(final File file) {
    final String file_name;

    if (file == null) {
      return EXP_NOT;
    }

    file_name = file.getName();

    if (file_name.endsWith(".er")) {
      if (file.isDirectory()) {
        return EXP_NORMAL;
      } else if (file.isFile()) {
        return EXP_OLD;
      } else {
        return EXP_NOT;
      }
    } else if (file_name.endsWith(".erg") && file.isFile()) {
      return EXP_GROUP;
    } else {
      return EXP_NOT;
    }
  }

  public String getWorkingDirectory() {
    String wd = null;
    if (anChooserExtraPanel != null) {
      wd = anChooserExtraPanel.getWorkingDirectory();
    }
    return wd;
  }

  public String getConfiguration() {
    String configuration = null;
    if (anChooserExtraPanel != null) {
      configuration = anChooserExtraPanel.getConfiguration();
    }
    return configuration;
  }

  public boolean alwaysUseThisConfiguration() {
    boolean always = false;
    if (anChooserExtraPanel != null) {
      always = anChooserExtraPanel.alwaysUseThisConfiguration();
    }
    return always;
  }

  // Called by the UI when the user hits the Approve button
  @Override
  public final void approveSelection() {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            ApproveSelectionInternal();
          }
        },
        "ApproveSelectionThread");
    // Run this on a thread so that the AWT does not hang when fileChooser is waiting for a bad
    // remote file
  }

  private final void ApproveSelectionInternal() {
    File[] files = null;
    final /*NM final*/ File file;
    final int type;

    if (chooser_type == EXP_CHOOSER) {
      files = getSelectedAnFiles();
      if (files.length == 0) {
        return;
      }
      file = files[0];
    } else {
      file = getSelectedAnFile();
      if (file == null) {
        return;
      }
    }

    /*NM        if (anFileSystemView != null) { // TEMPORARY! should be in getSelectedAnFiles()
    for (int i = 0; i < files.length; i++) {
    String file_name = files[i].getName();
    AnFile af = new AnFile(file_name);
    File dir = this.getCurrentDirectory();
    anFileSystemView.getFileAttributes(dir, af);
    files[i] = (File) af.getAbsoluteFile();
    }
    file = files[0];
    } */
    type = getExperimentType(file);
    String fullpath = file.getAbsolutePath();
    // Non-experiment directory been selected
    if ((chooser_type != DIR_CHOOSER)
        && (chooser_type != DIR_FILE_CHOOSER)
        && file.isDirectory()
        && type == EXP_NOT) {
      if (chooser_type == EXP_CHOOSER) {
        startDir = fullpath;
      }
      if (Analyzer.getInstance().remoteConnection != null) { // NM
        final AnFile af = new AnFile(fullpath);
        // setCurrentDirectory(af); // causes NPE
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              // @Override
              @Override
              public void run() {
                try {
                  setCurrentDirectory(af);
                } catch (IllegalArgumentException ex) {
                  // no exceptions expected here
                }
              }
            });
        return;
      } // NM
      // setCurrentDirectory(file);
      AnUtility.dispatchOnSwingThread(
          () -> {
            try {
              setCurrentDirectory(file);
            } catch (IllegalArgumentException ex) {
              // no exceptions expected here
            }
          } // @Override
          );
      return;
    } else {
      if (chooser_type == EXP_CHOOSER) {
        startDir = getCurrentDirectory().getAbsolutePath();
      }
    }

    boolean done = true;
    if (chooser_type == EXP_GROUP_CHOOSER) {
      if (type != EXP_GROUP) {
        AnUtility.showMessage(
            comp,
            AnLocale.getString("Not a valid experiment group: ") + file,
            JOptionPane.ERROR_MESSAGE);
        done = false;
      }
    } else if (chooser_type == TARGET_CHOOSER) {
      if (!Analyzer.getInstance().isRemote()) {
        if (!file.isFile() /* AnUtility.isTarget(file) */) {
          AnUtility.showMessage(
              comp, AnLocale.getString("Not a valid target: ") + file, JOptionPane.ERROR_MESSAGE);
          done = false;
        }
      } else {
        // no check in remote mode
        /* NM This code is not ready yet
        // setCurrentDirectory(file);
        final AnFile af = new AnFile(fullpath);
        start_dir = af.getDirectoryName();
        window.dispatchOnSwingThread(new Runnable() {
        // @Override
        public void run() {
        try {
        setCurrentDirectory(file.getParentFile());
        //start_dir = getCurrentDirectory().getAbsolutePath();
        } catch (Exception ex) {
        // no exceptions expected here
        ex.printStackTrace();
        }
        }
        });
        */
      }
    }

    if (done) {
      super.approveSelection();
    }
  }

  public final void cancelChooser() {
    super.cancelSelection();
  }

  // Called by the UI when the user chooses the Cancel button.
  @Override
  public final void cancelSelection() {
    if (chooser_type == EXP_CHOOSER) {
      startDir = getCurrentDirectory().getAbsolutePath();
    }
    /* NM This code does not work (see CR 16617347)
    final CancelContext cancelContext = window.getProgressCancelContext();
    if (cancelContext != null) {
    window.dispatchOnWorkerThread(new Runnable() {
    public void run() {
    cancelContext.getCancellable().cancel();
    // actual cancel will be called by the thread receiving
    // the cancel for the getFiles IPC request
    }
    }, "CancelTaskThread");
    }
    */
    super.cancelSelection();
  }

  // File filter for experiment
  final class ExpFilter extends javax.swing.filechooser.FileFilter {

    @Override
    public boolean accept(final File file) {
      final boolean accepted;
      final int type;

      type = getExperimentType(file);

      switch (chooser_type) {
        case EXP_GROUP_CHOOSER:
          accepted = (type == EXP_GROUP) || (type == EXP_NOT && file.isDirectory());
          break;
        case TARGET_CHOOSER:
          accepted = (type == EXP_NOT && file.isDirectory()) || AnUtility.isTarget(file);
          break;
        default: // EXP_CHOOSER
          accepted = (type != EXP_NOT) || file.isDirectory();
          break;
      }
      return (accepted);
    }

    @Override
    public String getDescription() {
      switch (chooser_type) {
        case EXP_GROUP_CHOOSER:
          return AnLocale.getString("Analyzer Experiment Group (*.erg)");
        case TARGET_CHOOSER:
          return AnLocale.getString("Analyzer Target (executable files)");
        default: // EXP_CHOOSER
          return AnLocale.getString("Analyzer Experiment (*.er, *.erg)");
      }
    }
  }
  // File view for experiment

  private final class ExpView extends javax.swing.filechooser.FileView {

    @Override
    public Icon getIcon(final File file) {
      if (chooser_type == TARGET_CHOOSER) {
        if (!file.isFile()) {
          return getExperimentIcon(file);
        }
        switch (AnUtility.getMimeFormat(file)) {
          case AnUtility.MIME_ELF_EXECUTABLE:
            return AnUtility.elf_icon;
          case AnUtility.MIME_JAVA_CLASS_FILE:
            return AnUtility.cls_icon;
          case AnUtility.MIME_JAR_FILE:
            return AnUtility.jar_icon;
          default:
            return getExperimentIcon(file);
        }
      } else {
        return getExperimentIcon(file);
      }
    }

    private Icon getExperimentIcon(final File file) {
      switch (getExperimentType(file)) {
        case EXP_NORMAL:
          return AnUtility.expt_icon;
        case EXP_GROUP:
          return AnUtility.egrp_icon;
        case EXP_OLD:
          return AnUtility.eold_icon;
        case EXP_BAD:
          return AnUtility.ebad_icon;
        default:
          return null;
      }
    }

    @Override
    public final Boolean isTraversable(final File file) {
      return getExperimentType(file) == EXP_NOT && file.isDirectory();
    }

    // Compatible with old JDK
    @Override
    public String getName(final File f) {
      return null;
    }

    @Override
    public String getDescription(final File f) {
      return null;
    }

    @Override
    public String getTypeDescription(final File f) {
      return null;
    }
  }
}
