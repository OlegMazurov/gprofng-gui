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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.util.gui.AnSplitPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

public class CalledByCallsDisp extends FuncListDisp {

  private static final int CALLER_MIN_HEIGHT = 120;
  private static final int CALLEE_MIN_HEIGHT = 60;
  private AnTable caller, func_item, callee;
  private Object[][] caller_data, callee_data;
  private Object[][] caller_sort_data, callee_sort_data;
  private LocalProcessData caller_data_lpd;
  private LocalProcessData callee_data_lpd;
  private String acNameT1;
  private String acDescT1;
  private String acNameT3;
  private String acDescT3;
  private long[] ids_callers = null;
  private long[] ids_callees = null;
  private long[] ids_cstack = null;
  private String[] ids_names = null;
  //        private CC_CallStack cc_callstack;
  private int sort_ind;
  JLabel calledByCallsLabel;
  String[] names_caller, names_callee;
  //    private Object[][] MaximumValues = null;
  protected long sel_func;
  // Constructor

  public CalledByCallsDisp(
      final AnWindow awindow, final int type, final String help_id, final int ptype) {
    super(awindow, type, 0, help_id);
    caller_data = callee_data = null;
    parent_type = ptype;
    sel_func = 0;
  }

  private Object[] getTableData(
      final String mlistStr,
      final String modeStr,
      final String typeStr,
      final String subtypeStr,
      final long[] cstack) {
    // long t = System.currentTimeMillis();
    Object[] data = window.getTableDataV2(mlistStr, modeStr, typeStr, subtypeStr, cstack);
    // t = System.currentTimeMillis() - t;
    // System.out.println("DEBUG: CallerCalleesDisp.getTableData(): time="+t+" mls.");
    return data;
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    StringBuilder buf = new StringBuilder();
    buf.append(calledByCallsLabel.getText());
    buf.append(" ");
    buf.append(names_caller[1]);
    buf.append("\n");
    buf.append(caller.printTableContents(null, 0));

    buf.append(calledByCallsLabel.getText());
    buf.append(" ");
    buf.append(names_callee[1]);
    buf.append("\n");
    buf.append(callee.printTableContents(null, 0));

    return buf.toString();
  }

  public AnTable getCallerTable() {
    return caller;
  }

  public AnTable getCalleeTable() {
    return callee;
  }

  class LocalProcessData {

    final String AccessToMaximumValues = "AccessToMaximumValues";

    // Process the raw data from mixed Primary/Object to pure Object array.
    // Creates or updates MaximumValues.
    public Object[][] localProcessData(final Object[] raw_data) {
      int i;
      // Default values for MaximumValues
      AnAddress aa = new AnAddress(0);
      AnDouble ad = new AnDouble(0.0);
      AnInteger ai = new AnInteger(0);
      AnLong al = new AnLong(0);
      AnString as = new AnString("");
      final int nc = raw_data.length - 1;
      final Object[][] data = new Object[nc][];

      synchronized (AccessToMaximumValues) {
        MaximumValues = new Object[2][nc];

        for (i = 0; i < nc; i++) {
          if (raw_data[i] instanceof double[]) {
            data[i] = AnDouble.toArray((double[]) raw_data[i]);
            MaximumValues[1][i] = ad;
          } else if (raw_data[i] instanceof int[]) {
            data[i] = AnInteger.toArray((int[]) raw_data[i]);
            MaximumValues[1][i] = ai;
          } else if (raw_data[i] instanceof long[]) {
            if ((((long[]) raw_data[i]).length == 0)
                || !AnAddress.isAddress(((long[]) raw_data[i])[0])) {
              data[i] = AnLong.toArray((long[]) raw_data[i]);
              MaximumValues[1][i] = al;
            } else {
              data[i] = AnAddress.toArray((long[]) raw_data[i]);
              MaximumValues[1][i] = aa;
            }
          } else {
            data[i] = (Object[]) raw_data[i];
            MaximumValues[1][i] = as;
          }
          this.updateTotalMax(data[i], i);
        }

        for (i = 0; i < nc; i++) {
          // make sure there are no null elements in MaximumValues[1]
          if (MaximumValues[0][i] == null) {
            MaximumValues[0][i] = ad;
          }
          if (MaximumValues[1][i] == null) {
            MaximumValues[1][i] = ad;
          }
        }
      }
      return data;
    }

    /*
     * Updates MaximumValues (Total and Max values)
     * Note: Total is not used, so only Maximum is updated.
     */
    void updateTotalMax(Object[] table_column, int index) {
      synchronized (AccessToMaximumValues) {
        double d0 = 0;
        for (int i = 0; i < table_column.length; i++) {
          if (MaximumValues[0][index] != null) {
            d0 = ((AnDouble) (MaximumValues[0][index])).doubleValue();
          }
          if ((table_column[i] instanceof AnDouble)) {
            double d = ((AnDouble) table_column[i]).doubleValue();
            d = d + d0;
            MaximumValues[0][index] = new AnDouble(d);
          }
          if ((table_column[i] instanceof AnInteger)) {
            double d = ((AnInteger) table_column[i]).doubleValue();
            d = d + d0;
            MaximumValues[0][index] = new AnDouble(d);
          }
          if ((table_column[i] instanceof AnLong)) {
            double d = ((AnLong) table_column[i]).doubleValue();
            d = d + d0;
            MaximumValues[0][index] = new AnDouble(d);
          }

          if (MaximumValues[1][index] == null) {
            MaximumValues[1][index] = table_column[i];
            continue;
          }

          int j = MaximumValues[1][index].toString().length();
          int k = table_column[i].toString().length();
          if (j < k) {
            MaximumValues[1][index] = table_column[i];
          }
        }
      }
    }

    /*
     * Returns MaximumValues (Total and Max values), calculated by processData()
     */
    public Object[] getTotalMax() {
      synchronized (AccessToMaximumValues) {
        return MaximumValues;
      }
    }
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {
    final JPanel pcaller;
    final JPanel pcallee;
    final JSplitPane splitPane1;
    Dimension msize;

    setLayout(new BorderLayout());

    // Create table names and descriptions
    acNameT1 = AnLocale.getString("Callers Table");
    acDescT1 = AnLocale.getString("Callers Table (shows functions that call selected function)");
    acNameT3 = AnLocale.getString("Callees Table");
    acDescT3 =
        AnLocale.getString(
            "Callers Table (shows functions that are called from selected function)");

    // Create lables
    calledByCallsLabel = new JLabel();
    Font monoSpacedFont = new Font("Monospaced", Font.BOLD, calledByCallsLabel.getFont().getSize());
    calledByCallsLabel.setFont(monoSpacedFont);
    AnUtility.setAccessibleContext(
        calledByCallsLabel.getAccessibleContext(), AnLocale.getString("Selected function"));

    // Create tables
    caller =
        new AnTable(
            DSP_MiniCaller,
            true,
            false,
            true,
            false,
            true,
            false,
            false,
            acNameT1,
            acDescT1,
            null); // HERE
    table = caller;
    caller.setParent((AnDisplay) this);
    AccessibleContext context = caller.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Callers"));
    context.setAccessibleDescription(AnLocale.getString("Callers"));
    caller.addAnListener(new TabTableHandler());
    caller.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    callee =
        new AnTable(
            DSP_MiniCallee,
            true,
            false,
            true,
            false,
            true,
            false,
            false,
            acNameT3,
            acDescT3,
            null); // HERE
    callee.setParent((AnDisplay) this);
    context = callee.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Callees"));
    context.setAccessibleDescription(AnLocale.getString("Callees"));
    callee.setHeader(caller);
    callee.addAnListener(new TabTableHandler());
    callee.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // Set header icons & tooltips
    pcaller = new JPanel(new BorderLayout());
    pcaller.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 1, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    pcaller.add(caller, BorderLayout.CENTER);

    pcallee = new JPanel(new BorderLayout());
    pcallee.setBorder(
        BorderFactory.createMatteBorder(1, 1, 0, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    pcallee.add(callee, BorderLayout.CENTER);

    // Set the split pane
    msize = pcaller.getMinimumSize();
    msize.height = CALLER_MIN_HEIGHT;
    pcaller.setMinimumSize(msize);

    msize = pcallee.getMinimumSize();
    msize.height = CALLEE_MIN_HEIGHT;
    pcallee.setMinimumSize(msize);
    msize.height = 100;
    msize.width = 50;

    splitPane1 = new AnSplitPane(JSplitPane.HORIZONTAL_SPLIT, pcaller, pcallee);
    splitPane1.setContinuousLayout(true);
    splitPane1.setResizeWeight(0.5);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.insets = new Insets(3, 0, 2, 0);
    topPanel.add(calledByCallsLabel, gridBagConstraints);
    add(topPanel, BorderLayout.NORTH);
    add(splitPane1, BorderLayout.CENTER);
    this.setPreferredSize(msize);
  }

  // Compute & update function list table
  @Override
  public synchronized void doCompute() {
    AnUtility.checkIfOnAWTThread(false);
    final int sc_pos;
    int name_len, len, row_height, height, i, j;
    Object[] raw_data;
    Object[] raw_data_with_ids;
    final int[] caller_stype, callee_stype;
    MetricLabel[] caller_label, callee_label;
    MetricLabel[] caller_sort_label, callee_sort_label;
    final int[][] caller_width, callee_width;
    AnObject obj, max_obj;
    final String mlistStr = "MET_CALL";
    final String typeStrCaller;
    switch (parent_type) {
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_Lines:
        typeStrCaller = "LINE";
        break;
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_PCs:
        typeStrCaller = "INSTR";
        break;
      default:
        typeStrCaller = "FUNCTION";
        break;
    }

    final String typeStr = "FUNCTION";
    final String subtypeStr;
    switch (parent_type) {
      case AnDisplay.DSP_Lines:
      case AnDisplay.DSP_PCs:
        subtypeStr = "1";
        break;
      default:
        subtypeStr = "0";
        break;
    }
    final String stab_callers = "CALLERS";
    final String stab_callees = "CALLEES";
    final String stab_self = "SELF";
    final long[] cstack;

    // Not selected
    if (!selected) {
      return;
    }

    caller_label = callee_label = null;

    long func_obj = window.getSelectedObject().getSelObj(DSP_Functions, 0);

    if (sel_func == 0 || sel_func != func_obj) {
      computed = false;
      sel_func = func_obj;
    }
    if (!computed) { // need re-compute
      // sel_func = getFuncObj(); // Required to initialize Functions list
      // setFuncData(sel_func, type, subtype);

      table = caller;
      table.setViewport();

      // Update stack
      /*
       * The caller-callee display will honor selections
       * made in other displays.
       */
      long ID = window.getSelectedObject().getSelObjV2("FUNCTION");
      long[] new_cstack = new long[1];
      new_cstack[0] = ID;
      cstack = new_cstack;
      ids_cstack = cstack; // TEMPORARY

      // Get name
      String fname = window.getObjNameV2(ID);
      String[] new_names = new String[1];
      new_names[0] = fname;
      ids_names = new_names; // TEMPORARY

      // Update callers list
      raw_data_with_ids = /* window. */
          getTableData(mlistStr, stab_callers, typeStrCaller, subtypeStr, cstack);
      raw_data = new Object[raw_data_with_ids.length];
      for (i = 0; i < raw_data.length; i++) {
        raw_data[i] = raw_data_with_ids[i];
      }
      raw_data[raw_data.length - 1] = null;
      ids_callers = (long[]) raw_data_with_ids[raw_data_with_ids.length - 1];

      caller_data_lpd = new LocalProcessData();
      caller_data = caller_data_lpd.localProcessData(raw_data);
      caller_stype = (int[]) raw_data[raw_data.length - 1];
      caller_label =
          getSettings()
              .getMetricsSetting()
              .getLabel(
                  caller_data, caller_data_lpd.getTotalMax(/*DSP_CALLER, subtype*/ ), type, caller);
      name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(getMetricMType());
      names = getNames(type, 0);

      sort_ind = getSortCol();
      if (sort_ind == name_col) {
        sort_ind = (name_col == 0) ? 1 : 0;
      }

      if (sort_ind >= caller_data.length) {
        sort_ind = 0;
      }
      if (caller_data.length > 1) {
        caller_sort_data = new Object[2][];
        caller_sort_label = new MetricLabel[2];
        caller_sort_data[0] = caller_data[sort_ind];
        caller_sort_label[0] = caller_label[sort_ind];
        caller_sort_data[1] = caller_data[name_col];
        caller_sort_label[1] = caller_label[name_col];
      } else {
        caller_sort_data = caller_data;
        caller_sort_label = caller_label;
      }

      // Update callees list
      raw_data_with_ids = /* window. */
          getTableData(mlistStr, stab_callees, typeStr, subtypeStr, cstack);
      raw_data = new Object[raw_data_with_ids.length];
      for (i = 0; i < raw_data.length; i++) {
        raw_data[i] = raw_data_with_ids[i];
      }
      raw_data[raw_data.length - 1] = null;
      ids_callees = (long[]) raw_data_with_ids[raw_data_with_ids.length - 1];

      callee_data_lpd = new LocalProcessData();
      callee_data = callee_data_lpd.localProcessData(raw_data);
      callee_stype = (int[]) raw_data[raw_data.length - 1];

      /* TEMPORARY changes to support percentage calculation */
      // Have to do it here, because Total was not calculated earlier
      callee_label =
          getSettings()
              .getMetricsSetting()
              .getLabel(
                  callee_data, callee_data_lpd.getTotalMax(/*DSP_CALLEE, subtype*/ ), type, callee);

      if (callee_data.length > 1) {
        callee_sort_data = new Object[2][];
        callee_sort_label = new MetricLabel[2];
        callee_sort_data[0] = callee_data[sort_ind];
        callee_sort_label[0] = callee_label[sort_ind];
        callee_sort_data[1] = callee_data[name_col];
        callee_sort_label[1] = callee_label[name_col];
      } else {
        callee_sort_data = callee_data;
        callee_sort_label = callee_label;
      }

      // Update function name
      String[] funcNames = ids_names;
      String funcName = AnLocale.getString("Selected function");
      if (funcNames != null && funcNames.length > 0) {
        funcName = (String) funcNames[0];
      }

      //                int index = funcName.indexOf('(');
      //                if (index > 0) {
      //                    funcName = funcName.substring(0, index);
      //                }
      calledByCallsLabel.setText(funcName);

      names_caller = new String[names.length];
      names_callee = new String[names.length];
      if (names.length > 1) {
        names_caller[0] = funcName;
        names_callee[0] = funcName;
        names_caller[1] = AnLocale.getString("is called by");
        names_callee[1] = AnLocale.getString("calls");
      }

      int sort_col = getSortCol();
      if (sort_col == name_col) {
        sort_col = 1;
      } else {
        sort_col = 0;
      }

      //            if (SwingUtilities.isEventDispatchThread()) {
      //                caller.setData(caller_sort_label, caller_sort_data, names_caller,
      // caller_stype,
      //                        0, (caller_data.length > 1) ? 1 : name_col, sort_col);
      //                callee.setData(callee_sort_label, callee_sort_data, names_callee,
      // callee_stype,
      //                        0, (callee_data.length > 1) ? 1 : name_col, sort_col);
      //                caller.repaint();
      //                callee.repaint();
      //            } else {
      {
        // Use SwingUtilities to update tables
        final MetricLabel[] label1 = caller_sort_label;
        final MetricLabel[] label3 = callee_sort_label;
        //                try {
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              @Override
              public void run() {
                int sort_col = getSortCol();
                if (sort_col == name_col) {
                  sort_col = 1;
                } else {
                  sort_col = 0;
                }
                getCallerTable()
                    .setData(
                        label1,
                        caller_sort_data,
                        names_caller,
                        caller_stype,
                        0,
                        (caller_data.length > 1) ? 1 : name_col,
                        sort_col);
                getCalleeTable()
                    .setData(
                        label3,
                        callee_sort_data,
                        names_callee,
                        callee_stype,
                        0,
                        (callee_data.length > 1) ? 1 : name_col,
                        sort_col);
                getCallerTable().repaint();
                getCalleeTable().repaint();
              }
            });
        //                } catch (Exception exc) {
        //                    System.out.println("CallDisp.recompute() exception: " + exc);
        //                    exc.printStackTrace();
        //                }
      }
    }

    // Set the column width
    if (caller_label != null) {
      // Find the 'name' object which has the maximum width
      max_obj = caller_label[name_col].getMaxAnObject();
      name_len = caller.stringWidth(max_obj.toString());
      obj = callee_label[name_col].getMaxAnObject();
      len = callee.stringWidth(obj.toString());
      if (name_len < len) {
        // name_len = len; // assignment is never used
        max_obj = obj;
      }

      caller_label[name_col].setMaxAnObject(max_obj);
      callee_label[name_col].setMaxAnObject(max_obj);

      // Find the maximum row height
      row_height = caller.getRowHeight();
      height = callee.getRowHeight();
      if (row_height < height) {
        row_height = height;
      }

      // Find the maximum width for each field
      caller_width = caller.getColumnWidth();
      callee_width = callee.getColumnWidth();

      for (i = 0; i < caller_width.length; i++) {
        for (j = 0; j < 4; j++) {
          if (caller_width[i][j] < callee_width[i][j]) {
            caller_width[i][j] = callee_width[i][j];
          }
        }
      }

      // Set column height/width for caller/func/callee
      caller.setColumn(row_height);
      callee.setColumn(row_height);

      caller.clearSelectedRow();
      callee.clearSelectedRow();

      sc_pos = callee.getScroll();
      caller.setScroll(sc_pos);
    }

    computed = true;
  }

  private int getSortCol() {
    int ret = -1;
    int func_sort_ind =
        getSettings().getMetricsSetting().getSortColumnByDType(AnDisplay.DSP_Functions);
    AnMetric[] func_mlist =
        getSettings().getMetricsSetting().getMetricListByMType(MetricsSetting.MET_NORMAL);
    AnMetric[] cc_mlist =
        getSettings().getMetricsSetting().getMetricListByMType(MetricsSetting.MET_CALL);
    for (int ind = 0; ind < cc_mlist.length; ind++) {
      if (cc_mlist[ind].getUserName().equals(func_mlist[func_sort_ind].getUserName())) {
        ret = ind;
        break;
      }
    }
    if (ret == -1) {
      ret = getSettings().getMetricsSetting().getSortColumnByDType(type);
    }
    return ret;
  }

  // Is current selected tab?
  public void setSelected(final boolean set) {
    selected = set;
  }

  // Listener for updating table
  private final class TabTableHandler implements AnListener {

    @Override
    public void valueChanged(final AnEvent event) {
      final AnTable src = (AnTable) event.getSource();
      final int loc, from, to, column, width;
      boolean double_click = false;

      int index = event.getValue();
      switch (event.getType()) {
        case AnEvent.EVT_SELECT: // Selecting
          // changeSelection(src, index);
          break;
        case AnEvent.EVT_COMPUTE: // Compute and Update
          changeSelection(src, index);
          break;
        case AnEvent.EVT_SORT: // Sorting
          getSettings()
              .getMetricsSetting()
              .setSortMetricByDType(this, getSortCol(), getMetricMType());
          break;
        case AnEvent.EVT_COPY_ALL: // Copy all lines
          copyAll();
          break;
        case AnEvent.EVT_COPY_SEL: // Copy selected lines
          copySelected();
          break;
        case AnEvent.EVT_SCROLL: // Scrolling
          if (src == getCalleeTable()) {
            loc = event.getValue();

            getCallerTable().setScroll(loc);
            func_item.setScroll(loc);
          }
          break;
        case AnEvent.EVT_SWITCH: // Column switching
          from = event.getValue();
          to = ((Integer) event.getAux()).intValue();
          getSettings().getMetricsSetting().setMetricOrderByDType(this, from, to, getMetricMType());
          break;
        case AnEvent.EVT_RESIZE: // Column resizing
          if (src == getCallerTable()) {
            column = event.getValue();
            width = ((Integer) event.getAux()).intValue();

            func_item.setColumnWidth(column, width);
            getCalleeTable().setColumnWidth(column, width);
          }
          break;
      }
    }
  }

  @Override
  public void changeSelection(AnTable src, int index) {
    long ID = -1;
    if (index >= 0) {
      //                clearSelections();
      if (src == caller) {
        if (index < ids_callers.length) {
          ID = ids_callers[index];
        }
      } else if (src == callee) {
        if (index < ids_callees.length) {
          ID = ids_callees[index];
        }
      }
      if (ID > 0) {
        src.setSelectedRow(index); // AWT
        // cc_callstack.setSelectedObject(ID); //NM Hack
        // cc_callstack.saveSelection(ID, src, index); //NM Hack
        // window.computeFuncData(); //NM Update Summary
        // window.showSummary(); // AWT
        final long id = ID;
        final int ix = index;
        final AnTable at = src;
        AnUtility.dispatchOnAWorkerThread(
            new Runnable() {
              @Override
              public void run() {
                if (at == getCallerTable()
                    && (parent_type == AnDisplay.DSP_Source
                        || parent_type == AnDisplay.DSP_Disassembly)) {
                  window.getSelectedObject().setSelObjWithEvent(id, DSP_MiniCaller, 0);
                } else {
                  window.getSelectedObject().setSelObjV2WithEvent(id);
                }
                //                        //window.computeFuncData(); //NM Update Summary
                //                        switch (parent_type) {
                //                            case AnDisplay.DSP_SOURCE:
                //                                window.computeSrcData();
                //                                break;
                //                            case AnDisplay.DSP_DISASM:
                //                                window.computeDisData();
                //                                break;
                //                            case AnDisplay.DSP_FUNCTION:
                //                                window.computeFuncData();
                //                                break;
                //                            default:
                //                                break;
                //                        }

              }
            },
            "CallTree_thread");
      }
    }
  }
}
