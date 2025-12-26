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

package org.gprofng.analyzer;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.ExperimentPickList;
import org.gprofng.mpmt.ExperimentPickListElement;
import org.gprofng.mpmt.ExperimentPickLists;
import org.gprofng.mpmt.guitesting.GUITesting;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.persistence.UserPrefPersistence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/** Main class for standalone analyzer */
public final class AnMain {

  private static JOptionPane importOldSettingPane = null;
  private static final int JVM_MAJOR = 1;
  private static final int JVM_MINOR = 8;
  /* The size of the fonts in the UI - 0 pt. The value can be changed
  by command-line argument -fontsize <size> */
  private static int uiFontSize = 0;

  /**
   * Parses the command line arguments
   *
   * @param args command line arguments
   * @return array of arguments without the fontsize flag and that value
   */
  private static String[] processArgs(final String[] args) {
    ArrayList<String> argsExp = new ArrayList<>();
    String argvOrig;

    for (int i = 0; i < args.length; i++) {
      argvOrig = args[i];
      if (argvOrig.equals("-f") || argvOrig.equals("--fontsize")) {
        try {
          uiFontSize = Integer.parseInt(args[++i]);
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println(AnLocale.getString("Font size expected, using default font size..."));
        } catch (NumberFormatException e2) {
          System.out.println(
              AnLocale.getString("Bad format of the font size, using default font size..."));
        }
      } else if (argvOrig.equals("-c") || argvOrig.equals("--compare")) {
        Analyzer.compareMode = true;
      } else if (argvOrig.equals("-u") || argvOrig.equals("--userdir")) {
        if (i >= (args.length - 1)) {
          System.out.println(AnLocale.getString("Not enough arguments, exiting..."));
          System.exit(1);
        }
        String userdir = args[++i];
        UserPref.setUserDir(userdir);
      } else if (argvOrig.startsWith("--bindir=")) {
        UserPref.binDirFromCommandLine = argvOrig.substring(argvOrig.indexOf("=") + 1);
      } else if (argvOrig.startsWith("--datadir=")) {
        UserPref.dataDirFromCommandLine = argvOrig.substring(argvOrig.indexOf("=") + 1);
      } else if (argvOrig.startsWith("--gprofngdir=")) {
        UserPref.gprofngdir = argvOrig.substring(argvOrig.indexOf("=") + 1);
      } else if (argvOrig.equals("--verbose")) {
        UserPref.verbose = true;
      } else {
        argsExp.add(argvOrig);
        // This argument is an experiment name, or a name of a binary to profile
        // Don't parse target's options - pass them down
        for (int j = i + 1; j < args.length; j++) {
          argsExp.add(args[j]);
        }
        break;
      }
    }
    if (UserPref.verbose) {
      for (int i = 0; i < args.length; i++) {
        AnLog.log(String.format("%2d %s", i, args[i]));
      }
    }
    return argsExp.toArray(new String[argsExp.size()]);
  }

  // sets Fonts for the value of -fontsize key
  private static void setFontSize(int size) {
    // Set all font size to new size
    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof FontUIResource) {
        FontUIResource font = (FontUIResource) value;
        UIManager.put(key, new FontUIResource(font.deriveFont((float) size)));
      }
    }
  }

  // Show Java version warning
  private static void warningVersion() {
    final String rec_ver, msg;
    final int sel;
    final Object[] options =
        new Object[] {AnLocale.getString("Continue"), AnLocale.getString("Exit")};

    rec_ver = AnLocale.getString("J2SE[tm] ") + JVM_MAJOR + "." + JVM_MINOR;
    msg =
        AnLocale.getString("The J2SE[tm] ")
            + Analyzer.jvm_ver
            + AnLocale.getString(" found at ")
            + Analyzer.jvm_home
            + AnLocale.getString(" should not be used by the gprofng tools.\n")
            + rec_ver
            + AnLocale.getString(" is recommended.\n")
            + AnLocale.getString("Use the -j option to specify a path to ")
            + rec_ver
            + ".";

    sel =
        JOptionPane.showOptionDialog(
            null,
            msg,
            AnLocale.getString("Warning: Java Runtime Environment Version"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1]);

    if ((sel == JOptionPane.CLOSED_OPTION) || (sel == JOptionPane.NO_OPTION)) {
      System.exit(0);
    }
  }

  public static void main(final String[] args) {
    //    AnMemoryManager.getInstance().checkMemoryUsage();
    AnEnvironment.setLookAndFeel();
    Analyzer analyzer = new Analyzer();
    int major = 0;
    int minor = 0;
    int n1, n2;
    String ver = Analyzer.jvm_ver;
    if ((n1 = ver.indexOf('.')) != -1) {
      major = Integer.parseInt(ver.substring(0, n1));
      if ((n2 = ver.indexOf('.', n1 + 1)) != -1) {
        minor = Integer.parseInt(ver.substring(n1 + 1, n2));
      }
    } else {
      // Check if it is java 9
      if (ver.startsWith("9")) {
        major = 1;
        minor = 9;
      }
    }
    if (major == 0 || (major == 1 && minor < 8) || (major > 1 && major < 8)) {
      warningVersion();
    }
    ToolTipManager.sharedInstance().setInitialDelay(250);

    final String[] argsNew = processArgs(args);
    File fromDirFile = null;
    File toDirFile = null;
    if (!GUITesting.getInstance().isRunningUnderGUITesting()) {
      String actualUserDirPath = UserPref.getAnalyzerDirPath();
      String actualInitFilePath = UserPref.getAnalyzerInitFilePath();
      if (actualUserDirPath.equals(UserPref.getDefaultAnalyzerDirPath())) {
        if (!new File(actualInitFilePath).exists()) {
          for (String userDir : UserPref.getOldUserDirs()) {
            String userDirPath = UserPref.getAnalyzerDirPath(userDir);
            String initFilePath = UserPref.getAnalyzerInitFilePath(userDir);
            if (new File(initFilePath).exists()) {
              fromDirFile = new File(userDirPath);
              toDirFile = new File(actualUserDirPath);
              try {
                // FIXUP: should use copyDirectory from AnUtility but it doesn't run this early due
                // to class loader issue
                UserPref.copyDirectory(fromDirFile, toDirFile);
              } catch (IOException e) {
                e.printStackTrace(); // FIXUP: what to do?
              }
              String msg =
                  AnLocale.getString(
                      "Settings created by a previous version of gprofng were found on your system"
                          + " at %s.\n"
                          + "They have been imported into %s.");
              String formattedMsg = String.format(msg, userDirPath, actualUserDirPath);
              //              JOptionPane.showMessageDialog(null, formattedMsg,
              // AnLocale.getString("Import Settings"), JOptionPane.PLAIN_MESSAGE);
              importOldSettingPane = new JOptionPane(formattedMsg, JOptionPane.PLAIN_MESSAGE);
              break;
            }
          }
        }
      }

      // Read user settings
      new UserPrefPersistence()
          .restoreSettings(UserPref.getAnalyzerInitFilePath(), UserPref.getInstance());

      // Copy those config files that are beeing used (from the picklists)
      if (fromDirFile != null && toDirFile != null) {
        ExperimentPickLists pickLists = UserPref.getInstance().getExperimentsPicklists();
        if (pickLists != null) {
          Map<String, ExperimentPickList> picklistMap = pickLists.getPicklistMap();
          if (picklistMap != null) {
            Set<String> hosts = picklistMap.keySet();
            if (hosts != null) {
              for (String host : hosts) {
                ExperimentPickList experimentPickList = picklistMap.get(host);
                if (experimentPickList != null) {
                  List<ExperimentPickListElement> elements =
                      experimentPickList.getRecentExperiments();
                  if (elements != null) {
                    for (ExperimentPickListElement element : elements) {
                      String mangledFileNameNoTimestamp =
                          UserPref.getMangledFileName(element.getPath(), host, false)
                              + UserPref.configurationSuffix;
                      String fromConfigPath =
                          fromDirFile
                              + "/"
                              + UserPref.configDirName
                              + "/"
                              + UserPref.configAsWhenClosedName
                              + "/"
                              + mangledFileNameNoTimestamp;
                      String asClosedDir =
                          toDirFile
                              + "/"
                              + UserPref.configDirName
                              + "/"
                              + UserPref.configAsWhenClosedName;
                      new File(asClosedDir).mkdirs();
                      String toConfigPath = asClosedDir + "/" + mangledFileNameNoTimestamp;
                      try {
                        UserPref.copyFile(new File(fromConfigPath), new File(toConfigPath));
                      } catch (IOException ioe) {
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    if (uiFontSize > 0) {
      setFontSize(uiFontSize);
    }

    analyzer.initMetricColors();

    SwingUtilities.invokeLater(
        () -> {
          try {
            analyzer.initAnalyzer(argsNew);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  public static void showImportOldSettingsDialog() {
    if (importOldSettingPane != null) {
      final JDialog importOldSettingDialog =
          importOldSettingPane.createDialog(AnLocale.getString("Import Settings"));
      importOldSettingDialog.setModal(false);
      importOldSettingDialog.setVisible(true);
      importOldSettingDialog.requestFocus();
      importOldSettingPane = null;
    }
  }
}
