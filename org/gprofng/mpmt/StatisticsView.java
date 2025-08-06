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

import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnTextIcon;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public final class StatisticsView extends AnDisplay implements ExportSupport, AnChangeListener {

  private boolean experimentAdded = false;

  private static final DecimalFormat statis_fmt = new DecimalFormat("0.###");
  private static final Object syncFormat = new Object();

  private AnTree tree;

  // Constructor
  public StatisticsView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Statistics, AnVariable.HELP_TabsStatistics);
    setAccessibility(AnLocale.getString("Statistics"));
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
    //        System.out.println("StatisticsView stateChanged:  " + e.getType());
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
        experimentAdded = true;
        if (selected) {
          computeOnAWorkerThread();
        }
        break;
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case FILTER_CHANGING:
      case FILTER_CHANGED:
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
        if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEW_MODE) {
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
  public void requestFocus() {
    if (tree != null) {
      tree.requestFocus();
    }
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {
    setLayout(new BorderLayout());

    tree = new AnTree(AnLocale.getString("Experiments"), 1);
    tree.getAccessibleContext().setAccessibleName(AnLocale.getString("Experiments"));
    tree.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Experiments"));
    add(new AnJScrollPane(tree), BorderLayout.CENTER);
  }

  // Append added experiment
  private void addExperiment() {
    AnTextIcon[] exp_list;
    int last;

    exp_list = window.getExperimentList();

    if (exp_list == null) {
      return;
    }

    // Add the extra node "<Sum across selected experiments>" if needed &
    // Get the last index
    last = tree.addExtra(AnLocale.getString("<Sum across selected experiments>"));

    // Add experiment into tree
    tree.addNodes(exp_list, last);
  }

  // Compute & update Statistics display
  @Override
  public void doCompute() {
    Object[] ov_data, st_data;
    String errstr;
    int size;

    AnUtility.checkIfOnAWTThread(false);
    // Not selected
    if (!selected) {
      return;
    }

    if (experimentAdded) {
      addExperiment();
      experimentAdded = false;
    }

    if (!computed) { // need re-compute
      // Get data
      ov_data = getStatisOverviewList();
      errstr = window.getMsg(AnUtility.ERROR_MSG);
      if ((ov_data != null) && (errstr == null)) {
        st_data = getStatisList();
        errstr = window.getMsg(AnUtility.ERROR_MSG);
        if ((st_data != null) && (errstr == null)) {
          size = ov_data.length;
          for (int i = 1; i < size; i++) {
            tree.setContent(i - 1, getContent(ov_data, st_data, i));
          }
        }
      }

      showError(errstr);
    }

    tree.setSelectionRow(0);

    computed = true;
  }

  @Override
  public java.util.List<ExportSupport.ExportFormat> getSupportedExportFormats() {
    java.util.List<ExportSupport.ExportFormat> formats = new ArrayList<>();
    //        formats.add(ExportSupport.ExportFormat.TEXT);
    //        formats.add(ExportSupport.ExportFormat.HTML);
    //        formats.add(ExportSupport.ExportFormat.CSV);
    formats.add(ExportSupport.ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return true;
  }

  // Set container for one experiment element
  private JPanel getContent(Object[] ov_data, Object[] st_data, int index) {
    JPanel panel, inter_panel;
    AnList ov_list, st_list;

    inter_panel = new JPanel();
    inter_panel.setLayout(new BoxLayout(inter_panel, BoxLayout.X_AXIS));

    // Overview panel
    ov_list = processOVData(ov_data, index);
    ov_list.getAccessibleContext().setAccessibleName(AnLocale.getString("Overview"));
    ov_list.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Overview"));
    ov_list.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    inter_panel.add(ov_list);

    inter_panel.add(Box.createHorizontalStrut(4));

    // Statistics panel
    st_list = processSTData(st_data, index);
    st_list.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    inter_panel.add(st_list);
    st_list.getAccessibleContext().setAccessibleName(AnLocale.getString("Statistics"));
    st_list.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Statistics"));

    panel = new JPanel(new BorderLayout(2, 2));
    panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    panel.add(
        AnUtility.getHeader(AnLocale.getString("Execution statistics for entire program:")),
        BorderLayout.NORTH);
    panel.add(inter_panel, BorderLayout.CENTER);

    return panel;
  }

  // Process Overview data
  private static AnList processOVData(Object[] data, int index) {
    AnList ov_list;
    String[] label;
    double[] value;
    int size, columns;
    int i;
    double total;
    AnDouble dvalue;
    String dstr;

    // Compute overview data
    ov_list = new AnList(false);
    label = (String[]) data[0];
    value = (double[]) data[index];
    size = label.length;

    // Get total
    total = 0.0;
    for (i = 5; i < size; i++) {
      total += value[i];
    }

    columns = (new AnDouble(total)).toString().length() + AnDouble.quote_space.length();

    // Write Overview header
    for (i = 0; i < 5; i++) {
      dvalue = new AnDouble(value[i]);
      dstr = (dvalue.doubleValue() < 0.0) ? AnLocale.getString("N/A") : dvalue.toString();
      if (label[i].equals(AnLocale.getString("Total LWP Time (sec.)"))) {
        total = dvalue.doubleValue();
      }
      ov_list.add(
          AnUtility.getItem(label[i] + ":"),
          AnUtility.getNumber(dstr + AnDouble.quote_space, columns));
    }

    // Write Overview values
    ov_list.add((JComponent) null, (JComponent) null);
    ov_list.add(AnUtility.getItem(AnLocale.getString("Process Times (sec.):")), null);

    for (i = 5; i < size; i++) {
      double percent = total == 0.0 ? total : (value[i] / total) * 100;
      ov_list.add(
          AnUtility.getItem(label[i] + ":"),
          AnUtility.getNumber((new AnDouble(value[i])).toPercentQuote(percent), columns));
    }

    return ov_list;
  }

  // Process Statistics data
  private static AnList processSTData(Object[] data, int index) {
    AnList st_list;
    String[] label;
    String[] value_str;
    double[] value;
    int size, len, columns;
    int i;

    // Write statistics data
    st_list = new AnList(false);
    label = (String[]) data[0];
    value = (double[]) data[index];
    size = label.length;

    value_str = new String[size];
    columns = 0;
    for (i = 0; i < size; i++) {
      synchronized (syncFormat) { // begin critical section: format is not thread safe
        value_str[i] = statis_fmt.format(value[i]);
      } // end of critical section
      len = value_str[i].length();
      if (columns < len) {
        columns = len;
      }
    }

    for (i = 0; i < size; i++) {
      st_list.add(AnUtility.getItem(label[i] + ":"), AnUtility.getNumber(value_str[i], columns));
    }

    return st_list;
  }

  // Native methods from liber_dbe.so
  private Object[] getStatisOverviewList() {
    synchronized (IPC.lock) {
      window.IPC().send("getStatisOverviewList");
      window.IPC().send(0);
      return (Object[]) window.IPC().recvObject();
    }
  }

  private Object[] getStatisList() {
    synchronized (IPC.lock) {
      window.IPC().send("getStatisList");
      window.IPC().send(0);
      return (Object[]) window.IPC().recvObject();
    }
  }
}
