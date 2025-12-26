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

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.statuspanel.StatusHandleFactory;
import org.gprofng.mpmt.statuspanel.StatusLabelValueHandle;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 * @author tpreisle
 */
public class AnMemoryManager {

  private static final long MB = 1024 * 1024;
  private static AnMemoryManager instance = null;
  private static StatusLabelValueHandle memoryStatusHandle = null;

  private static long max = 0;
  private static long total = 0;
  private static long free = 0;
  private static long leftThreshold = 0;
  private static long visibleThreshold = 0;
  private static long used = 0;
  private static long left = 0;

  private static boolean warned = false;

  public AnMemoryManager() {}

  public static AnMemoryManager getInstance() {
    if (instance == null) {
      instance = new AnMemoryManager();
    }
    if (memoryStatusHandle == null
        && AnWindow.getInstance() != null
        && AnWindow.getInstance().getStatusPanel() != null) {
      memoryStatusHandle =
          StatusHandleFactory.createStatusLabelValue(
              AnWindow.getInstance(),
              null,
              null,
              AnLocale.getString("Available Memory"),
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  System.err.println("---------------Memory Usage");
                  System.err.println(statusText());
                }
              });
      memoryStatusHandle.setVisible(false);
    }
    return instance;
  }

  public boolean checkMemoryThreshold() {
    max = Runtime.getRuntime().maxMemory();
    total = Runtime.getRuntime().totalMemory();
    free = Runtime.getRuntime().freeMemory();
    leftThreshold = Math.max(max / 8, 20 * MB);
    visibleThreshold = max / 2;
    used = total - free;
    left = max - used;

    if (left < visibleThreshold) {
      return true;
    }

    return false;
  }

  public void checkMemoryUsage() {
    max = Runtime.getRuntime().maxMemory();
    total = Runtime.getRuntime().totalMemory();
    free = Runtime.getRuntime().freeMemory();
    leftThreshold = Math.max(max / 8, 20 * MB);
    visibleThreshold = max / 2;
    used = total - free;
    left = max - used;

    if (left < leftThreshold && !warned) {
      warning();
      warned = true;
    }
    if (!(left < leftThreshold)) {
      warned = false;
    }

    if (memoryStatusHandle != null) {
      if ((used / MB) > visibleThreshold) {
        memoryStatusHandle.setVisible(true);
      }
      if (memoryStatusHandle.isVisible()) {
        final String labelValueText = (used / MB + "/" + max / MB + " MB");
        //            System.err.println(labelValueText);
        final String labelToolTip = statusTT();
        final StatusLabelValueHandle.Mode mode =
            left < leftThreshold
                ? StatusLabelValueHandle.Mode.ERROR
                : StatusLabelValueHandle.Mode.DEFAULT;
        final ImageIcon icon = left < leftThreshold ? AnUtility.warningIcon : null;

        SwingUtilities.invokeLater(
            new Runnable() {
              @Override
              public void run() {
                memoryStatusHandle.update(labelValueText, mode);
                memoryStatusHandle.updateToolTip(labelToolTip);
                memoryStatusHandle.update(icon);
              }
            });
      }
    }
  }

  private void warning() {
    String warning = "============================> LOW MEMORY WARNING";
    String status = statusText();
    StringBuilder stacktrace = new StringBuilder();

    StackTraceElement[] se = new Exception().getStackTrace();
    stacktrace.append(Thread.currentThread().getName());
    stacktrace.append("\n");
    for (StackTraceElement se1 : se) {
      stacktrace.append(se1.toString());
      stacktrace.append("\n");
    }

    AnLog.log(warning);
    AnLog.log(status);
    AnLog.log(stacktrace.toString());
    System.err.println(warning); // FIXUP: should be removed
    System.err.println(status); // FIXUP: should be removed
  }

  private static String statusTT() {
    StringBuilder buf = new StringBuilder();
    buf.append("<html>");
    buf.append("<b>Memory Usage</b> (used/available): " + used / MB + "/" + max / MB + " MB");
    buf.append("<br>");
    buf.append("<b>max</b>: " + max / MB + " MB");
    buf.append("<br>");
    buf.append("<b>total</b>: " + total / MB + " MB");
    buf.append("<br>");
    buf.append("<b>free</b>: " + free / MB + " MB");
    buf.append("<br>");
    buf.append("<b>used</b>: " + used / MB + " MB");
    buf.append("<br>");
    buf.append("<b>leftThreshold</b>: " + leftThreshold / MB + " MB");
    buf.append("<br>");
    buf.append("<b>visibleThreshold</b>: " + visibleThreshold / MB + " MB");
    buf.append("<br>");
    buf.append("<b>left</b>: " + left / MB + " MB");
    buf.append("<br>");
    buf.append("</html>");
    return buf.toString();
  }

  private static String statusText() {
    StringBuilder buf = new StringBuilder();
    buf.append("max: " + max / MB + " MB");
    buf.append("\n");
    buf.append("total: " + total / MB + " MB");
    buf.append("\n");
    buf.append("free: " + free / MB + " MB");
    buf.append("\n");
    buf.append("used: " + used / MB + " MB");
    buf.append("\n");
    buf.append("leftThreshold: " + leftThreshold / MB + " MB");
    buf.append("\n");
    buf.append("left: " + left / MB + " B");
    return buf.toString();
  }
}
