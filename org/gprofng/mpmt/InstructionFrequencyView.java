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

import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;

import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;

public final class InstructionFrequencyView extends AnDisplay
    implements ExportSupport, AnChangeListener {

  private static final Font table_font;
  private AnUtility.AnTextArea inst_log;

  static {
    JTable jtable = new JTable();
    Font org_font = jtable.getFont();

    table_font = new Font("Monospaced", org_font.getStyle(), org_font.getSize());
  }

  // Constructor
  public InstructionFrequencyView() {
    super(
        AnWindow.getInstance(),
        AnDisplay.DSP_InstructionFrequency,
        AnVariable.HELP_WelcomeAnalyzer);
    setAccessibility(AnLocale.getString("Instruction Frequency"));
    AnEventManager.getInstance().addListener(this);
  }

  /**
   * AnChangeEvent handler. Events are always dispatched on AWT thread. Requirements to an event
   * handler like this one: o When it done handling the event and returns, all it's states need to
   * be *consistent* and *well-defined*. It cannot dispatch that or part of that work to a different
   * (worker) thread and not wait for it to finish. o It cannot spend significant time handling the
   * event. Significant work like doCompute needs to be dispatched to a worker thread.
   *
   * @param e the event
   */
  @Override
  public void stateChanged(AnChangeEvent e) {
    //        System.out.println("InstructionFrequencyView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        computed = false;
        clear();
        clearHistory();
        break;
      case EXPERIMENTS_LOADED_FAILED:
        // Nothing
        break;
      case EXPERIMENTS_LOADED:
        addExperiment();
        if (selected) {
          computeOnAWorkerThread();
        }
        break;
      case FILTER_CHANGED:
        computed = false;
        if (selected) {
          computeOnAWorkerThread();
        }
        break;
      case FILTER_CHANGING:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case SETTING_CHANGING:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
        break;
      case SETTING_CHANGED:
        AnSettingChangeEvent anSettingChangeEvent = (AnSettingChangeEvent) e.getSource();
        if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEW_MODE
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.FORMAT
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.COMPARE_MODE
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.LIBRARY_VISIBILITY
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.METRICS) {
          computed = false;
          if (selected) {
            computeOnAWorkerThread();
          }
        }
        break;
      case DEBUG:
        debug();
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    StringBuilder buf = new StringBuilder();
    buf.append(getAnDispTab().getTName());
    buf.append("\n");
    buf.append(inst_log.getText());
    return buf.toString();
  }

  @Override
  public List<ExportFormat> getSupportedExportFormats() {
    List<ExportFormat> formats = new ArrayList<ExportFormat>();
    formats.add(ExportFormat.TEXT);
    //        formats.add(ExportFormat.HTML);
    //        formats.add(ExportFormat.CSV);
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return false;
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {
    setLayout(new BorderLayout());
    inst_log = new AnUtility.AnTextArea(null, false);
    inst_log.setFont(table_font);
    add(new AnJScrollPane(inst_log), BorderLayout.CENTER);
  }

  // Clear the text area on add experiment
  private void addExperiment() {
    inst_log.setText("");
  }

  // Compute & update Statistics display
  @Override
  public void doCompute() {
    String[] inst_data;
    int size;

    AnUtility.checkIfOnAWTThread(false);
    // Not selected
    if (!selected) {
      return;
    }

    if (!computed) { // need re-compute
      // Get data
      inst_log.setText("");
      inst_data = getIfreqData();
      if (inst_data != null) {
        size = inst_data.length;
        for (int i = 0; i < size; i++) {
          inst_log.append(AnUtility.trimNewLine(inst_data[i]) + "\n");
        }
      }
    }

    computed = true;
  }

  // Native methods from liber_dbe.so
  private String[] getIfreqData() {
    synchronized (IPC.lock) {
      window.IPC().send("getIfreqData");
      window.IPC().send(0);
      return (String[]) window.IPC().recvObject();
    }
  }
}
