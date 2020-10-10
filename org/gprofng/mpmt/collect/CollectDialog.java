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

package org.gprofng.mpmt.collect;

import org.gprofng.mpmt.AnDialog;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JFrame;

public final class CollectDialog extends AnDialog implements CollectPanel.Provider {

  private final CollectPanel collectPanel;
  private final Collector collector;
  private int prof_type = Collector.PROFILE_APPLICATION;

  // Constructor
  public CollectDialog(
      final AnWindow awindow, final JFrame frame, int profiling_type, String title) {
    super(
        awindow,
        frame, /*CollectPanel.title*/
        title,
        false,
        CollectPanel.aux,
        CollectPanel.mnemonic,
        getHelpID(profiling_type),
        false /* not modal */);
    final JButton buttons[] = new JButton[] {ok, apply, aux_actions[0], aux_actions[1], close};

    close_on_enter = false;
    try {
      if (Analyzer.getInstance().IPC_started == false) {
        Analyzer.getInstance().startIPC();
      }
    } catch (Exception e) {
      // Should never come here
      System.err.println("analyzer: Exception in CollectDialog");
      e.printStackTrace();
      System.exit(1);
    }
    collector = new Collector(window);
    prof_type = profiling_type;
    collector.setProfilingType(prof_type);
    AnUtility.checkIPCOnWrongThread(false);
    if (prof_type == Collector.SYSTEM_PROFILING) {
      // profile kernel
      JButton buttons2[] = new JButton[] {ok, apply, aux_actions[0], aux_actions[1], close};
      collectPanel = new CollectPanel(collector, this, this, window, buttons2);
    } else if (prof_type == Collector.PROFILE_RUNNING_APPLICATION) {
      // profile running application
      JButton buttons1[] = new JButton[] {ok, apply, aux_actions[0], aux_actions[1], close};
      collectPanel = new CollectPanel(collector, this, this, window, buttons1);
    } else {
      // profile new application
      collectPanel = new CollectPanel(collector, this, this, window, buttons);
    }
    AnUtility.checkIPCOnWrongThread(true);
    setAccessory(collectPanel.work_panel);
  }

  private static String getHelpID(int profiling_type) {
    if (profiling_type == Collector.PROFILE_APPLICATION) {
      return AnVariable.HELP_CollectDialog;
    } else if (profiling_type == Collector.PROFILE_RUNNING_APPLICATION) {
      return AnVariable.HELP_CollectRunningDialog;
    } else if (profiling_type == Collector.SYSTEM_PROFILING) {
      return AnVariable.HELP_CollectKernelDialog;
    } else {
      return AnVariable.HELP_CollectDialog;
    }
  }

  // Constructor
  public CollectDialog(final AnWindow awindow, final JFrame frame) {
    this(awindow, frame, 0, CollectPanel.title);
  }

  public void doDialog() {
    collectPanel.loadExpDefaults();
    setVisible(true);
  }

  public void doDialog(String[] arguments) {
    collectPanel.loadExpDefaults();
    if ((arguments != null) && (arguments.length > 0)) {
      collectPanel.target.setValue(arguments[0]);
      String args = "";
      for (int i = 1; i < arguments.length; i++) {
        if (i > 1) {
          args += " ";
        }
        args += arguments[i];
      }
      collectPanel.args.setValue(args);
    }
    setVisible(true);
  }

  @Override
  public void setTitleStr(final String title) {
    setTitle(title);
  }

  public void loadExpDefaults() {
    collectPanel.loadExpDefaults();
  }

  // Action performed; run collect
  @Override
  public void actionPerformed(final ActionEvent event) {
    collectPanel.actionPerformed(event);
  }

  @Override
  public void setVisible(boolean aFlag) {
    if (collectPanel != null) {
      collectPanel.closeHWCDialogs();
    }
    super.setVisible(aFlag);
  }
}
