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


package org.gprofng.mpmt.guitesting;

import org.gprofng.mpmt.AnWindow;

/*
 * Miscellenuous support for automated GUI testing
 */

public class GUITesting {
  private static final String GUI_TESTING_ENVVAR = "SP_ANALYZER_GUI_TESTING";
  private static final String GUI_TESTING_EXTRA = "SP_ANALYZER_GUI_TESTING_EXTRA";
  private static final String GUI_RUNNING_UNDER_NB = "SP_RUNNING_UNDER_NB";
  private static GUITesting instance = null;

  private boolean enableGUITestingExtra = false;
  private boolean runningUnderGUITesting = false;
  private boolean runningUnderNB = false;

  public GUITesting() {

    String guiTestingExtra = System.getenv(GUI_TESTING_EXTRA);
    if (guiTestingExtra == null) {
      guiTestingExtra = System.getProperty(GUI_TESTING_EXTRA);
    }
    enableGUITestingExtra = guiTestingExtra != null;

    String guiTesting = System.getenv(GUI_TESTING_ENVVAR);
    if (guiTesting == null) {
      guiTesting = System.getProperty(GUI_TESTING_ENVVAR);
    }
    runningUnderGUITesting = guiTesting != null;

    String underNB = System.getenv(GUI_RUNNING_UNDER_NB);
    if (underNB == null) {
      underNB = System.getProperty(GUI_RUNNING_UNDER_NB);
    }
    runningUnderNB = underNB != null;

    if (enableGUITestingExtra) {
      System.out.println("GUITesting: SP_ANALYZER_GUI_TESTING_EXTRA: " + enableGUITestingExtra);
    }
    if (runningUnderGUITesting) {
      System.out.println("GUITesting: SP_ANALYZER_GUI_TESTING: " + runningUnderGUITesting);
    }
    if (runningUnderNB) {
      System.out.println("GUITesting: SP_RUNNING_UNDER_NB: " + runningUnderNB);
    }
  }

  public static GUITesting getInstance() {
    if (instance == null) {
      instance = new GUITesting();
    }
    return instance;
  }

  /**
   * @return the runningUnderGUITesting
   */
  public boolean isRunningUnderGUITesting() {
    return runningUnderGUITesting;
  }

  /**
   * @return the runningUnderNB
   */
  public boolean isRunningUnderNB() {
    return runningUnderNB;
  }

  /**
   * @return the enableGUITestingExtra
   */
  public boolean guiTestingExtraEnabled() {
    return enableGUITestingExtra;
  }

  public String filter(String in) {
    String out = in;
    out = out.replaceAll("\\(found as .*\\)", "(found as <FILTERED>)");
    out = out.replaceAll("...... file: /.*/", "...... file: <FILTERED>/"); // Source or Object file
    out = out.replaceAll("Stack 0x.*", "Stack 0x<FILTERED>"); // I/O view
    out = out.replaceAll("Process Id.*", "Process Id <FILTERED>"); // I/O subview
    out = out.replaceAll("Time of peak.*", "Time of peak <FILTERED>"); // I/O subview
    out = out.replaceAll("Total time.*", "Total time <FILTERED>"); // I/O subview
    //        out = out.replaceAll("Heap size bytes.*", "Heap size bytes <FILTERED>");  // I/O
    // subview
    //        out = out.replaceAll("Total bytes.*", "Total bytes <FILTERED>");  // I/O subview
    out =
        out.replaceAll(
            "Instruction freq.*", "Instruction freq... <FILTERED>"); // instruction Frequency
    return out;
  }

  /**
   * Dump contents of main view. Used in unit tests.
   *
   * @return
   */
  public String dumpMainview() {
    return dumpMainview(0);
  }
  /**
   * Dump contents of main view. Used in unit tests.
   *
   * @param maxLines limit output to top maxLines lines. 0 meaning all lines.
   * @return
   */
  public String dumpMainview(int maxLines) {
    AnWindow window = AnWindow.getInstance();
    String txt = window.dumpMainview(maxLines);
    txt = filter(txt);
    return txt;
  }

  /**
   * Dump contents of all visible sub views. Used in unit tests.
   *
   * @return
   */
  public String dumpSubviews() {
    AnWindow window = AnWindow.getInstance();
    String txt = window.dumpSubviews();
    txt = filter(txt);
    return txt;
  }

  /**
   * Dump visibility of main view and sub views. Used in unit tests.
   *
   * @return
   */
  public String dumpViewsVisibility() {
    AnWindow window = AnWindow.getInstance();
    return window.dumpViewsVisibility();
  }

  public String dumpAvailableViews() {
    AnWindow window = AnWindow.getInstance();
    String txt = window.getViewsPanel().dumpAvailableViews();
    return txt;
  }
}
