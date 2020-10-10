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

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.overview.OverviewPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

public class MiniFuncListDisp extends FuncListDisp {

  private int[] src_type;

  public MiniFuncListDisp(final AnWindow window, final int type, final String help_id) {
    super(window, type, 0, help_id);
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {

    // System.err.println("XXX In FLD initComponents()");
    setLayout(new BorderLayout());
    String acName = null;
    String acDesc = null;
    JLabel acLabel = null;

    switch (type) {
      case AnDisplay.DSP_Functions:
        acName = AnLocale.getString("Functions");
        acDesc = AnLocale.getString("Show function list");
        break;
    }
    if (acName != null) {
      acLabel = new JLabel(acName, JLabel.RIGHT);
    }
    table =
        new AnTable(
            DSP_MiniFunctions,
            true,
            true,
            false,
            false,
            true,
            true,
            false,
            acName,
            acDesc,
            acLabel); // HERE
    table.setParent(this);
    if (acLabel != null) {
      acLabel.setVisible(false);
      acLabel.setDisplayedMnemonic(acName.charAt(0));
      table.add(acLabel);
    }
    table.addAnListener(new TableHandler());
    table.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    add(table, BorderLayout.CENTER);
  }

  //    static int count = 0;

  @Override
  public void doCompute() {
    //        System.out.println("MiniFuncListDisp - doCompute " + count++ + selected + " " +
    // computed + " " + type);
    final long sel_obj;
    int new_ind;
    final int sort_ind;
    final Object[] raw_data;
    final String errstr, pwarnstr;
    final MetricLabel[] label;

    AnUtility.checkIfOnAWTThread(false);
    // Not selected
    if (!selected) {
      return;
    }
    if (inCompute) { // NM TEMPORARY should be synchronized
      return;
    } else {
      inCompute = true;
    }
    // System.err.println("XXX In FLD doCompute - 1 ()");
    //        window.setBusyCursor(true);

    // System.err.println("XXX In FLD doCompute - 3 ()");
    sel_obj = window.getSelectedObject().getSelObj(type, subtype);
    if (can_sort) { // Function/LINE/PC
      sel_func = sel_obj;
    } else { // Annotated src/dis
      sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock) // Required to initialize Functions list
    }

    // Special code for Index tabs and for OpenMP tabs
    int typeForPresentation = type;
    String typeStr = "INDEXOBJ";
    String subtypeStr = "" + subtype;
    String modeStr = "ALL";
    String mlistStr = "MET_INDX";
    if (isOmpInxObj()) {
      mlistStr = "MET_NORMAL";
      typeForPresentation = AnDisplay.DSP_Functions;
    }

    if (!computed) { // need re-compute

      // System.err.println("XXX In FLD doCompute - 4 ()");
      reset();
      //            window.setBusyCursor(true);

      // new_ind = setFuncData(sel_func, type, subtype);
      window
          .getFunctionsView()
          .computeIfNeeded(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
      errstr = window.getMsg(AnUtility.ERROR_MSG);

      // Update table
      if (errstr == null) {

        // System.err.println("XXX In FLD doCompute - 5 ()");
        table.setViewport();
        Object[][] table_data;
        raw_data = getFuncListMini(type, subtype);
        if ((raw_data == null) || (raw_data.length < 1)) {
          String txt = "IPC ERROR (?) in MiniFuncListDisp: raw_data is null or empty";
          AnLog.log(txt);
          inCompute = false;
          return; // No data
        }
        final AnMetric[] mlist =
            getSettings().getMetricsSetting().getMetricListByDType(typeForPresentation);
        table_data =
            localProcessData(
                mlist, raw_data); // first index is for column, second index is for rows
        // System.err.println("XXX after processData table_data.length = " + table_data.length + ";
        // row_length " + table_data[0].length );
        src_type = (int[]) raw_data[raw_data.length - 1]; // AnTable.AT_SRC, DIS, QUOTE, etc.
        String[] hdrContent =
            getNames(typeForPresentation, 0); // name column table header contents (?)
        label =
            getSettings()
                .getMetricsSetting()
                .getLabel(table_data, null, typeForPresentation, table);
        name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(getMetricMType());
        sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(typeForPresentation);
        new_ind = 0;
        table.setData(label, table_data, hdrContent, src_type, new_ind, name_col, sort_ind);
        table.repaint();
      } else {
        // System.err.println("XXX In FLD doCompute - 6 ()");
        window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);
        table.showMessage(errstr);
      }
      OverviewPanel.getInstance().previewChanged();
    }

    //        table.requestFocus();

    computed = true;
    //        pstatstr = window.getMsg(AnUtility.PSTAT_MSG);
    pwarnstr = window.getMsg(AnUtility.PWARN_MSG);
    //        if (pstatstr != null) {
    //            window.appendLog(pstatstr);
    //        }
    if (pwarnstr != null) {
      window.showProcessorWarning(pwarnstr);
    }

    inCompute = false;
  }

  // Listener for updating table
  private final class TableHandler implements AnListener {

    @Override
    public void valueChanged(final AnEvent event) {
      final int from;
      final int to;
      int stype = 0;
      final boolean can_nav;

      switch (event.getType()) {
        case AnEvent.EVT_SELECT: // Selecting
          break;
        case AnEvent.EVT_SORT: // Sorting
          break;
        case AnEvent.EVT_COPY_ALL: // Copy all lines
          copyAll();
          break;
        case AnEvent.EVT_COPY_SEL: // Copy selected lines
          copySelected();
          break;
        case AnEvent.EVT_SWITCH: // Column switching
          if (table != null) {
            table.columnsSaved = false;
          }
          from = event.getValue();
          to = ((Integer) event.getAux()).intValue();
          getSettings().getMetricsSetting().setMetricOrderByDType(this, from, to, getMetricMType());
          break;
      }
    }
  }

  private Object[] getFuncListMini(final int type, final int subtype) {
    synchronized (IPC.lock) {
      window.IPC().send("getFuncListMini");
      window.IPC().send(0);
      window.IPC().send(type);
      window.IPC().send(subtype);
      return (Object[]) window.IPC().recvObject();
    }
  }
}
