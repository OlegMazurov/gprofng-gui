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

import static org.gprofng.mpmt.AnDisplay.DSP_Callees;
import static org.gprofng.mpmt.AnDisplay.DSP_CallerCalleeSelf;
import static org.gprofng.mpmt.AnDisplay.DSP_Callers;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnSplitPaneInternal;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public final class CallerCalleesView extends FuncListDisp
    implements ExportSupport, AnChangeListener {

  private static final int CALLER_MIN_HEIGHT = 80;
  private static final int CALLEE_MIN_HEIGHT = 60;
  private AnTable caller, func_item, callee;
  private Object[][] caller_data = null;
  private Object[][] func_data = null;
  private Object[][] callee_data = null;
  private LocalProcessData caller_data_lpd;
  private LocalProcessData func_data_lpd;
  private LocalProcessData callee_data_lpd;
  private String acNameT1;
  private String acDescT1;
  private String acNameT2;
  private String acDescT2;
  private String acNameT3;
  private String acDescT3;
  private long[] ids_callers = null;
  private long[] ids_callees = null;
  private long[] ids_cstack = null;
  private String[] ids_names = null;
  private CC_CallStack cc_callstack = new CC_CallStack();
  private CallStackToolbarHandler CS_ControlPanel;
  private boolean singleFunctionMode = false;

  // Interface strings
  final String mlistStr = "MET_CALL";
  final String typeStrFunc = "FUNCTION";
  final String subtypeStr = "0";
  final String stab_callers = "CALLERS";
  final String stab_callees = "CALLEES";
  final String stab_self = "SELF";

  // Constructor
  public CallerCalleesView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Callers, 0, AnVariable.HELP_TabsCallersCallees);
    setAccessibility(AnLocale.getString("Callers and callees of the selected function"));
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
    //        System.out.println("CallerCalleesView stateChanged:  " + e.getType());
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
  public void requestFocus() {
    if (caller != null) {
      caller.requestFocus();
    }
  }

  @Override
  public JPanel getToolbarPanel() {
    return null;
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {
    final JPanel pcaller, pfunc_item, pcallee;
    JLabel label_callers, label_function, label_callees;
    JLabel label_callers_text, label_function_text, label_callees_text;
    final JSplitPane split_pane_1;
    final JSplitPane split_pane_2;
    Dimension msize;

    setLayout(new BorderLayout());

    // Create table names and descriptions
    acNameT1 = AnLocale.getString("Callers Table");
    acDescT1 = AnLocale.getString("Callers Table (shows functions that call selected function)");
    acNameT2 = AnLocale.getString("Self Function Table");
    acDescT2 = AnLocale.getString("Self Function Table (shows selected function)");
    acNameT3 = AnLocale.getString("Callees Table");
    acDescT3 =
        AnLocale.getString(
            "Callers Table (shows functions that are called from selected function)");

    // Create lables
    label_callers = new JLabel(AnUtility.calr_icon);
    label_function = new JLabel(AnUtility.func_icon);
    // label_function = new JLabel(AnUtility.attr_icon);
    label_callees = new JLabel(AnUtility.cale_icon);
    label_callers_text = new JLabel("1");
    label_function_text = new JLabel("2");
    label_callees_text = new JLabel("3");

    // Create tables
    caller =
        new AnTable(
            DSP_Callers,
            true,
            true,
            true,
            false,
            false,
            false,
            true,
            acNameT1,
            acDescT1,
            label_callers_text);
    table = caller;
    caller.setParent((AnDisplay) this);
    AccessibleContext context = caller.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Callers"));
    context.setAccessibleDescription(AnLocale.getString("Callers"));
    caller.addAnListener(new TableHandler());
    if (singleFunctionMode == true) {
      func_item =
          new AnTable(
              DSP_CallerCalleeSelf,
              false,
              false,
              true,
              true,
              false,
              true,
              false,
              acNameT2,
              acDescT2,
              label_function_text);
      func_item.setParent((AnDisplay) this);
      func_item.addAnListener(new TableHandler());
      context = func_item.getAccessibleContext();
      context.setAccessibleName(AnLocale.getString("Self function"));
      context.setAccessibleDescription(AnLocale.getString("Self function"));
      func_item.setHeader(caller);
    } else {
      func_item =
          new AnTable(
              DSP_CallerCalleeSelf,
              false,
              false,
              true,
              false,
              false,
              false,
              false,
              acNameT2,
              acDescT2,
              label_function_text);
      func_item.setParent((AnDisplay) this);
      func_item.addAnListener(new TableHandler());
      context = func_item.getAccessibleContext();
      context.setAccessibleName(AnLocale.getString("Stack Fragment"));
      context.setAccessibleDescription(AnLocale.getString("Stack Fragment"));
      func_item.setHeader(caller);
    }
    callee =
        new AnTable(
            DSP_Callees,
            false,
            false,
            true,
            false,
            true,
            false,
            false,
            acNameT3,
            acDescT3,
            label_callees_text);
    callee.setParent((AnDisplay) this);
    context = callee.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Callees"));
    context.setAccessibleDescription(AnLocale.getString("Callees"));
    callee.setHeader(caller);
    callee.addAnListener(new TableHandler());

    // Set header icons & tooltips
    pcaller = new JPanel(new BorderLayout());
    label_callers.setToolTipText(AnLocale.getString("Callers") + "  (Alt-1)");
    label_callers_text.setText("1");
    label_callers_text.setVisible(false);
    label_callers_text.setDisplayedMnemonic(KeyEvent.VK_1);
    context = label_callers.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Callers"));
    context.setAccessibleDescription(AnLocale.getString("Callers"));
    label_callers.setBorder(new EmptyBorder(40, AnVariable.sizeIcon, 0, AnVariable.sizeIcon));
    pcaller.add(label_callers_text, BorderLayout.WEST);
    pcaller.add(label_callers, BorderLayout.WEST);
    pcaller.add(caller, BorderLayout.CENTER);

    pfunc_item = new JPanel(new BorderLayout());
    JPanel pfunc_internal_panel = new JPanel(new BorderLayout());
    // Create Call Stack Builder Control Panel
    CS_ControlPanel = new CallStackToolbarHandler();
    JPanel pfunc_control_toolbar =
        CS_ControlPanel.initCallStackToolbarHandler(this); // new JToolBar();
    JLabel jl = new JLabel(AnLocale.getString("Call Stack Builder"));
    jl.setVisible(false);
    pfunc_control_toolbar.add(jl);
    pfunc_internal_panel.add(pfunc_control_toolbar, BorderLayout.NORTH);
    pfunc_internal_panel.add(func_item, BorderLayout.CENTER);
    if (singleFunctionMode == true) {
      label_function.setToolTipText(AnLocale.getString("Selected Function") + "  (Alt-2)");
    } else {
      label_function.setToolTipText(AnLocale.getString("Stack Fragment") + "  (Alt-2)");
    }
    label_function_text.setText("2");
    label_function_text.setVisible(false);
    label_function_text.setDisplayedMnemonic(KeyEvent.VK_2);
    context = label_function.getAccessibleContext();
    if (singleFunctionMode == true) {
      context.setAccessibleName(AnLocale.getString("Selected Function"));
      context.setAccessibleDescription(AnLocale.getString("Selected Function"));
    } else {
      context.setAccessibleName(AnLocale.getString("Stack Fragment"));
      context.setAccessibleDescription(AnLocale.getString("Stack Fragment"));
    }
    label_function.setBorder(AnVariable.iconBorder);
    pfunc_item.add(label_function_text, BorderLayout.WEST);
    pfunc_item.add(label_function, BorderLayout.WEST);
    // NM pfunc_item.add(func_item, BorderLayout.CENTER);
    pfunc_item.add(pfunc_internal_panel, BorderLayout.CENTER);

    pcallee = new JPanel(new BorderLayout());
    label_callees.setToolTipText(AnLocale.getString("Callees") + "  (Alt-3)");
    label_callees_text.setText("3");
    label_callees_text.setVisible(false);
    label_callees_text.setDisplayedMnemonic(KeyEvent.VK_3);
    context = label_callees.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Callees"));
    context.setAccessibleDescription(AnLocale.getString("Callees"));
    label_callees.setBorder(AnVariable.iconBorder);
    pcallee.add(label_callees_text, BorderLayout.WEST);
    pcallee.add(label_callees, BorderLayout.WEST);
    pcallee.add(callee, BorderLayout.CENTER);

    // Set the split pane
    msize = pcaller.getMinimumSize();
    msize.height = CALLER_MIN_HEIGHT;
    pcaller.setMinimumSize(msize);

    msize = pcallee.getMinimumSize();
    msize.height = CALLEE_MIN_HEIGHT;
    pcallee.setMinimumSize(msize);
    pfunc_item.setMinimumSize(msize);

    split_pane_2 = new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, pfunc_item, pcallee);
    split_pane_2.setContinuousLayout(true);
    split_pane_2.setDividerLocation(AnVariable.WIN_HEIGHT / 2);

    pcaller.setBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR_INSIDE));
    split_pane_1 = new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, pcaller, split_pane_2);
    split_pane_1.setContinuousLayout(true);
    split_pane_1.setDividerLocation(AnVariable.WIN_HEIGHT / 2);

    add(split_pane_1, BorderLayout.CENTER);
  }

  // Clear display
  @Override
  public void clear() {
    caller.removeAll();
    func_item.removeAll();
    callee.removeAll();
  }

  // Clear selections
  public void clearSelections() {
    caller.clearSelection();
    func_item.clearSelection();
    callee.clearSelection();
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

  private Object[][] getCurrentTotalMax() {
    Object[][] total_max = new Object[2][];
    total_max[0] = caller_data_lpd.getTotal();
    total_max[1] = MaximumValues[1];
    return total_max;
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String text = exportAsText(limit);
    return text;
  }

  private String exportAsText(Integer limit) {
    // Prepare text presentation of the table
    String sortedby = AnLocale.getString("sorted by metric:");
    String eol = "\n";
    String empty = "";
    String space = " ";
    String textImage = null;
    String separator = empty;
    String No = AnLocale.getString("No ");
    String title1 = caller.getAccessibleContext().getAccessibleName();
    String title2 = func_item.getAccessibleContext().getAccessibleName();
    String title3 = callee.getAccessibleContext().getAccessibleName();
    AnTable tbl = func_item;
    Object[][] total_max = getCurrentTotalMax();
    textImage = tbl.printTableHeader(title2, sortedby, total_max);
    boolean[] show_percentage = tbl.getPercentageArray();
    int[] maxh = tbl.getTableHeaderWidths();
    if (null == MaximumValues) {
      return textImage; // empty report
    }
    for (int i = 0; i < maxh.length - 1; i++) {
      int j = 0;
      if (i + 2 == maxh.length) {
        j = 1; // Cosmetic
      }
      for (; j < maxh[i]; j++) {
        separator += "=";
      }
    }
    separator += space; // Cosmetic
    int printLimit = limit != null ? limit : 0;
    tbl = caller;
    textImage += separator;
    if (tbl.getRowCount() == 0) {
      textImage += No;
    }
    textImage += title1 + eol;
    boolean last_only = false;
    boolean selected_only = false;
    textImage +=
        tbl.printTableContents(
            total_max, maxh, printLimit, show_percentage, last_only, selected_only);
    tbl = func_item;
    textImage += separator;
    textImage += title2 + eol;
    last_only = true;
    textImage +=
        tbl.printTableContents(
            total_max, maxh, printLimit, show_percentage, last_only, selected_only);
    tbl = callee;
    textImage += separator;
    if (tbl.getRowCount() == 0) {
      textImage += No;
    }
    textImage += title3 + eol;
    last_only = false;
    textImage +=
        tbl.printTableContents(
            total_max, maxh, printLimit, show_percentage, last_only, selected_only);
    return textImage;
  }

  /**
   * Sets column height for caller, stack fragment, and callee tables
   *
   * @param row_height
   */
  private void setHeightsAndScroll(int row_height) {
    // This should be done on AWT thread:
    final int common_row_height = row_height;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            // Set column height/width for caller/func/callee
            caller.setColumn(common_row_height);
            func_item.setColumn(common_row_height);
            callee.setColumn(common_row_height);
            if (singleFunctionMode == true) {
              caller.clearSelectedRow();
              callee.clearSelectedRow();
            }
            final int sc_pos = callee.getScroll();
            caller.setScroll(sc_pos);
            func_item.setScroll(sc_pos);
          }
        });
  }

  // Reset display
  @Override
  public void reset() {
    try {
      SwingUtilities.invokeAndWait(
          new Runnable() {
            @Override
            public void run() {
              caller.removeAllRows();
              func_item.removeAllRows();
              callee.removeAllRows();
            }
          });
    } catch (Exception exc) {
      System.out.println("CallDisp.reset() exception: " + exc);
      exc.printStackTrace();
    }
  }

  // Get ID of selected function
  public long getID() {
    return window.getSelectedObject().getSelObjV2("FUNCTION");
  }

  // Get current stack fragment
  public long[] getStack() {
    return cc_callstack.getIDs();
  }

  // Compute & update function list table
  @Override
  public synchronized void doCompute() {
    final int /*new_ind,*/ sort_ind, sc_pos;
    int name_len, len, row_height, height;
    Object[] raw_data_with_ids;
    final int[] caller_stype, func_stype, callee_stype;
    MetricLabel[] caller_label, func_label, callee_label;
    final int[][] caller_width, func_width, callee_width;
    AnObject obj, max_obj;
    final long[] cstack;
    boolean select_self = false;

    AnUtility.checkIfOnAWTThread(false);
    // Not selected
    if (!selected) {
      return;
    }

    caller_label = func_label = callee_label = null;
    if (null == cc_callstack) {
      return; // initialization is not finished yet
    }

    // R1 Reset stack if necessary
    if (cc_callstack.needReset()) {
      cc_callstack.reset();
      select_self = true;
      computed = false;
    }

    if (!computed) { // need re-compute
      sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock) // Required to initialize Functions list
      Object[] res = setFuncData(sel_func, type, subtype); // IPC
      String errstr = ((String[]) (res[1]))[0];
      // Update table
      if (errstr == null) {
        showWarning(window.getMsg(AnUtility.WARNING_MSG));
        table = caller;
        table.setViewport();

        // Update stack
        /*
         * The caller-callee display will honor selections
         * made in other displays.
         */
        cstack = cc_callstack.getIDs();

        // Update callers list
        raw_data_with_ids = /* window. */
            getTableData(mlistStr, stab_callers, typeStrFunc, subtypeStr, cstack);
        caller_data_lpd = new LocalProcessData(raw_data_with_ids);
        caller_data = caller_data_lpd.get_data();
        ids_callers = caller_data_lpd.get_ids();
        caller_stype = caller_data_lpd.get_types();

        // Update Callers
        // Pass callers_total_max to support percentage calculation
        Object[] callers_total_max =
            caller_data_lpd.getLocalTotalMax(); // XXX Do we need a special case; no callers?
        caller_label =
            getSettings()
                .getMetricsSetting()
                .getLabel(caller_data, callers_total_max, type, caller);

        name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
        names = getNames(type, 0);
        sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);

        // Update function item
        raw_data_with_ids = /* window. */
            getTableData(mlistStr, stab_self, typeStrFunc, subtypeStr, cstack);
        func_data_lpd = new LocalProcessData(raw_data_with_ids);
        func_data = func_data_lpd.get_data();
        if (func_data.length <= name_col) {
          // NM Hack against drop experiments
          return;
        }
        if (cstack.length == 1) {
          ids_cstack = func_data_lpd.get_ids();
          // NM fix API problem
          cc_callstack.setNames((String[]) func_data[name_col]);
        } else {
          // Now watch the hands :-)
          func_data = cc_callstack.recreateStackTable(raw_data_with_ids);
        }
        func_stype = func_data_lpd.get_types();

        // Update callees list
        raw_data_with_ids = /* window. */
            getTableData(mlistStr, stab_callees, typeStrFunc, subtypeStr, cstack);
        callee_data_lpd = new LocalProcessData(raw_data_with_ids);
        callee_data = callee_data_lpd.get_data();
        ids_callees = callee_data_lpd.get_ids();
        callee_stype = callee_data_lpd.get_types();

        // Update Stack Fragment
        if (caller_data[0].length < 1) {
          if (callee_data[0].length > 0) {
            callers_total_max = callee_data_lpd.getLocalTotalMax();
          } else {
            callers_total_max = func_data_lpd.getLocalTotalMax();
            // if ((null == MaximumValues) || (null == MaximumValues[0][0])) {
            //    callers_total_max = func_data_lpd.getLocalTotalMax();
            // } else {
            //    callers_total_max = new Object[2]; // ANOTHER HACK
            //    callers_total_max[0] = MaximumValues[0]; // ANOTHER HACK
            //    callers_total_max[1] = MaximumValues[1]; // ANOTHER HACK
            // }
          }
        }
        // Pass callers_total_max to support percentage calculation
        func_label =
            getSettings()
                .getMetricsSetting()
                .getLabel(func_data, callers_total_max, type, func_item);

        // Update Callees
        // Pass callers_total_max to support percentage calculation
        callee_label =
            getSettings()
                .getMetricsSetting()
                .getLabel(callee_data, callers_total_max, type, callee);

        {
          // Use SwingUtilities to update tables
          final MetricLabel[] label1 = caller_label;
          final MetricLabel[] label2 = func_label;
          final MetricLabel[] label3 = callee_label;
          AnUtility.dispatchOnSwingThread(
              new Runnable() {
                @Override
                public void run() {
                  caller.setData(label1, caller_data, names, caller_stype, 0, name_col, sort_ind);
                  func_item.setData(label2, func_data, names, func_stype, 0, name_col, sort_ind);
                  callee.setData(label3, callee_data, names, callee_stype, 0, name_col, sort_ind);
                }
              });
        }

      } else {
        window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);
        table.showMessage(errstr);
      }
    }

    // Set the column width // XXX Should all this staff be done on AWT thread?
    if (caller_label != null) {
      // Find the 'name' object which has the maximum width
      max_obj = caller_label[name_col].getMaxAnObject();
      name_len = caller.stringWidth(max_obj.toString());
      obj = func_label[name_col].getMaxAnObject();
      len = func_item.stringWidth(obj.toString());
      if (name_len < len) {
        name_len = len;
        max_obj = obj;
      }
      obj = callee_label[name_col].getMaxAnObject();
      len = callee.stringWidth(obj.toString());
      if (name_len < len) {
        // name_len = len; // assignment is never used
        max_obj = obj;
      }

      caller_label[name_col].setMaxAnObject(max_obj);
      func_label[name_col].setMaxAnObject(max_obj);
      callee_label[name_col].setMaxAnObject(max_obj);

      // Find the maximum row height
      row_height = caller.getRowHeight();
      height = func_item.getRowHeight();
      if (row_height < height) {
        row_height = height;
      }
      height = callee.getRowHeight();
      if (row_height < height) {
        row_height = height;
      }

      // Find the maximum width for each field
      caller_width = caller.getColumnWidth();
      func_width = func_item.getColumnWidth();
      callee_width = callee.getColumnWidth();

      for (int i = 0; i < caller_width.length; i++) {
        for (int j = 0; j < 4; j++) {
          if (caller_width[i][j] < func_width[i][j]) {
            caller_width[i][j] = func_width[i][j];
          }
          if (caller_width[i][j] < callee_width[i][j]) {
            caller_width[i][j] = callee_width[i][j];
          }

          func_width[i][j] = callee_width[i][j] = caller_width[i][j];
        }
      }

      // Set column height/width for caller/func/callee
      setHeightsAndScroll(row_height); // Use AWT thread
    }
    // Update caller table headers. Column width may have changed.
    caller.getTableModel().updateTableHeaders();

    // Update selection
    if (select_self && (ids_cstack.length > 0)) {
      cc_callstack.setSelectedObject(ids_cstack[0]);
      cc_callstack.saveSelection(ids_cstack[0], func_item, 0);
    }
    cc_callstack.computeSelection();

    // Update summary display
    window.getSelectionManager().updateSelection();

    computed = true;
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  // Find
  @Override
  public int find(final String str, final boolean next, boolean caseSensitive) {
    int find_row;
    final AnTable next_table;

    if (str == null) {
      return -1;
    }

    table.clearSelection();
    next_table = (table == caller) ? callee : caller;

    find_row = table.findAfter(str, next, caseSensitive);
    if (find_row == -1) {
      find_row = next_table.findBefore(str, next, caseSensitive);

      if (find_row == -1) {
        find_row = next_table.findAfter(str, next, caseSensitive);

        if (find_row == -1) {
          find_row = table.findBefore(str, next, caseSensitive);
        } else {
          table = next_table;
        }
      } else {
        table = next_table;
      }
    }

    if (find_row != -1) {
      table.setSelectedRow(find_row);
    }

    return find_row;
  }

  // Find name and select this raw
  public int findName(final String str, final boolean next, AnTable table, boolean caseSensitive) {
    int find_row = -1;
    AnTable next_table;

    if (str == null) {
      return -1;
    }
    next_table = table;
    if (null == table) {
      next_table = caller;
    }
    for (int i = 1; i <= 3; i++) {
      find_row = next_table.findAfter(str, next, caseSensitive);
      if (find_row == -1) {
        find_row = next_table.findBefore(str, next, caseSensitive);
      }
      if (find_row != -1) {
        clearSelections();
        // caller.clearSelection();
        // func_item.clearSelection();
        // callee.clearSelection();
        next_table.setSelectedRow(find_row);
        // NM table = next_table; //NM is it needed?
        break;
      }
      if (next_table == caller) {
        next_table = func_item;
      } else if (next_table == func_item) {
        next_table = callee;
      } else if (next_table == callee) {
        next_table = caller;
      }
    }
    return find_row;
  }

  // Sort
  @Override
  public void sort(final int index) {
    caller.sort(index);
    callee.sort(index);
  }

  // Check if "Back" action is available
  @Override
  public boolean isBackActionAvailable() {
    if (cc_callstack == null) {
      return false;
    }
    return cc_callstack.isBackActionAvailable();
  }

  // Check if "Forward" action is available
  @Override
  public boolean isForwardActionAvailable() {
    if (cc_callstack == null) {
      return false;
    }
    return cc_callstack.isForwardActionAvailable();
  }

  // Get function item data
  // private Object[] getFuncItem() {
  //     return getFuncList(DSP_SELF, 0);
  // }
  // Get callers data list
  // private Object[] getCallerList() {
  //     return getFuncList(DSP_CALLER, 0);
  // }
  // Get callees data list
  // private Object[] getCalleeList() {
  //     return getFuncList(DSP_CALLEE, 0);
  // }
  // Get table data
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

  class LocalProcessData {

    final String AccessToMaximumValues = "AccessToMaximumValues";
    private Object[][] LocalMaximumValues =
        null; // MaximumValues[0][col] = Total[col], MaximumValues[1][col] = Maximum[col]
    Object[][] total_max = null;
    private Object[] raw_data;
    private Object[][] data;
    private long[] ids;
    private int[] types;

    LocalProcessData(final Object[] raw_data_with_ids) {
      raw_data = raw_data_with_ids;
      final AnMetric[] mlist = getSettings().getMetricsSetting().getMetricListByDType(type);
      data = localProcessData(mlist, raw_data);
      ids = (long[]) raw_data[raw_data_with_ids.length - 1];
      //            types = (int[]) raw_data[raw_data_with_ids.length - 2];
      types = null; // Is not needed ?
      total_max = AnObject.updateMaxValues(data, null);
    }

    public long[] get_ids() {
      return ids;
    }

    public int[] get_types() {
      return types;
    }

    public Object[][] get_data() {
      return data;
    }

    /*
     * Returns MaximumValues (Total and Max values), calculated by processData()
     */
    public Object[] getTotalMax() {
      synchronized (AccessToMaximumValues) {
        return MaximumValues;
      }
    }

    /*
     * Returns local Maximum Values (Total and Max values), calculated by processData()
     */
    public Object[] getLocalTotalMax() {
      synchronized (AccessToMaximumValues) {
        return LocalMaximumValues;
      }
    }

    /*
     * Returns Total values
     */
    public Object[] getTotal() {
      Object[] total = null;
      Object[] raw_data_with_ids =
          getTableData(mlistStr, stab_callees, typeStrFunc, subtypeStr, null /*cstack*/);
      if (null == raw_data_with_ids) {
        return total;
      }
      final AnMetric[] mlist = getSettings().getMetricsSetting().getMetricListByDType(type);
      Object[][] processed_data = localProcessData(mlist, raw_data_with_ids);
      int len = processed_data.length;
      if (len > 0) {
        total = new Object[len];
        for (int i = 0; i < len; i++) {
          total[i] = processed_data[i][0];
        }
      }
      return total;
    }
  }

  // Listener for updating table
  private final class TableHandler implements AnListener {

    @Override
    public void valueChanged(final AnEvent event) {
      final AnTable src = (AnTable) event.getSource();
      final int loc, from, to, column, width;
      boolean doubleClick = false;

      int index = event.getValue();
      switch (event.getType()) {
        case AnEvent.EVT_SELECT: // Selecting
          changeSelection(src, index);
          break;
        case AnEvent.EVT_COPY_ALL: // Copy
          copyAll();
          break;
        case AnEvent.EVT_RESET: // Reset
          cc_callstack.reset();
          computed = false;
          computeOnAWorkerThread();
          break;
        case AnEvent.EVT_SET: // Set (re-center)
          if (src == caller) {
            if ((index >= 0) && (ids_callers.length - 1 >= index)) {
              cc_callstack.replace(ids_callers, index);
            }
          } else if (src == callee) {
            if ((index >= 0) && (ids_callees.length - 1 >= index)) {
              cc_callstack.replace(ids_callees, index);
            }
          } else if (src == func_item) {
            if ((index >= 0) && (ids_cstack.length - 1 >= index)) {
              cc_callstack.replace(ids_cstack, index);
            }
          }
          computed = false;
          computeOnAWorkerThread();
          break;
        case AnEvent.EVT_SETHEAD: // Set new head
          cc_callstack.setHead(index);
          computed = false;
          computeOnAWorkerThread();
          break;
        case AnEvent.EVT_SETTAIL: // Set new tail
          cc_callstack.setTail(index);
          computed = false;
          computeOnAWorkerThread();
          break;
        case AnEvent.EVT_COMPUTE: // Compute and Update
          // Default action for double click
          //                    double_click = true; //OM
          doubleClick = false; // NM
          if (src == caller) {
            cc_callstack.prepend(index);
            if (doubleClick) {
              if (ids_cstack.length == 2) {
                cc_callstack.replace(ids_cstack, 0);
              }
            }
          } else if (src == callee) {
            cc_callstack.append(index);
            if (doubleClick) {
              if (ids_cstack.length == 2) {
                cc_callstack.replace(ids_cstack, 1);
              }
            }
          } else if (src == func_item) {
            cc_callstack.remove(index);
            if (doubleClick) {
              if (ids_cstack.length == 2) {
                cc_callstack.replace(ids_cstack, 1);
              }
            }
          }
          computed = false;
          computeOnAWorkerThread();
          break;
        case AnEvent.EVT_UPDATE: // Updating
          if (index >= 0) {
            if (src == caller) {
              cc_callstack.prepend(index);
              if (doubleClick) { // OM
                if (ids_cstack.length == 2) { // OM
                  cc_callstack.replace(ids_cstack, 0); // OM
                } // OM
              } // OM
            } else if (src == callee) {
              cc_callstack.append(index);
              if (doubleClick) { // OM
                if (ids_cstack.length == 2) { // OM
                  cc_callstack.replace(ids_cstack, 1); // OM
                } // OM
              } // OM
            } else if (src == func_item) {
              if ((index == 0) && (ids_cstack.length > 1)) {
                cc_callstack.remove(index);
              }
              if ((index > 0) && (ids_cstack.length - 1 == index)) {
                cc_callstack.remove(index);
              }
              if ((index > 0) && (ids_cstack.length - 1 > index)) {
                // R1 cc_callstack.replace(ids_cstack, index);
                // do nothing
                break;
              }
            }
            computed = false;
            computeOnAWorkerThread();
            // } else { // Keep selected object (src == func_item)
          }
          // NM window.showSummary();
          break;
        case AnEvent.EVT_SORT: // Sorting
          getSettings()
              .getMetricsSetting()
              .setSortMetricByDType(this, ((Integer) event.getAux()).intValue(), type);
          break;
        case AnEvent.EVT_SCROLL: // Scrolling
          if (src == callee) {
            loc = event.getValue();

            caller.setScroll(loc);
            func_item.setScroll(loc);
          }
          break;
        case AnEvent.EVT_SWITCH: // Column switching
          from = event.getValue();
          to = ((Integer) event.getAux()).intValue();
          if (src == caller) {
            func_item.moveColumn(from, to);
            callee.moveColumn(from, to);
          }
          getSettings().getMetricsSetting().setMetricOrderByDType(this, from, to, type);
          break;
        case AnEvent.EVT_RESIZE: // Column resizing
          if (src == caller) {
            column = event.getValue();
            width = ((Integer) event.getAux()).intValue();

            func_item.setColumnWidth(column, width);
            callee.setColumnWidth(column, width);
          }
          break;
        case AnEvent.EVT_BACK: // Go back
          cc_callstack.gotoCallStackFrame(cc_callstack.StackHistoryIndex - 1);
          computed = false;
          computeOnAWorkerThread();
          break;
        case AnEvent.EVT_FORWARD: // Go forward
          cc_callstack.gotoCallStackFrame(cc_callstack.StackHistoryIndex + 1);
          computed = false;
          computeOnAWorkerThread();
          break;
      }
    }
  }

  // Listener for call stack toolbar actions
  private final class CallStackToolbarHandler implements ActionListener {

    private CallerCalleesView cd;
    private JPanel tb;
    public JButton btn_add = new JButton();
    public JButton btn_remove = new JButton();
    public JButton btn_set_head = new JButton();
    public JButton btn_set_center = new JButton();
    public JButton btn_set_tail = new JButton();
    public JButton btn_back = new JButton();
    public JButton btn_forward = new JButton();
    public boolean btn_append_enabled = false;

    public JPanel initCallStackToolbarHandler(CallerCalleesView calldisp) {
      cd = calldisp;
      tb = new JPanel();
      // btn_back = new JButton("Back");
      btn_back.setMnemonic(AnLocale.getString('B', "CallerCalleesBack"));
      btn_back.setIcon(AnUtility.goBackwardIcon);
      // btn_back.setText(AnLocale.getString("Back", "GO_BACK_IN_CALLSTACK_HISTORY"));
      btn_back.setToolTipText(AnLocale.getString("Go back in callstack history"));
      btn_back.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Go_Back(evt);
            }
          });
      tb.add(btn_back);
      // btn_forward = new JButton("Forward");
      btn_forward.setMnemonic(AnLocale.getString('o', "CallerCalleesForward"));
      btn_forward.setIcon(AnUtility.goForwardIcon);
      // btn_forward.setText(AnLocale.getString("Forward", "GO_FORWARD_IN_CALLSTACK_HISTORY"));
      btn_forward.setToolTipText(AnLocale.getString("Go forward in callstack history"));
      btn_forward.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Go_Forward(evt);
            }
          });
      tb.add(btn_forward);
      JLabel action_group = new JLabel("     ");
      tb.add(action_group);
      // btn_add = new JButton("Add");
      btn_add.setMnemonic(AnLocale.getString('A', "CallerCalleesAdd"));
      btn_add.setText(AnLocale.getString("Add", "ADD_SELECTED_FUNCTION_TO_CALLSTACK"));
      btn_add.setToolTipText(AnLocale.getString("Add selected function to the callstack"));
      btn_add.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Add_Function(evt);
            }
          });
      tb.add(btn_add);
      // btn_remove = new JButton("Remove");
      btn_remove.setMnemonic(AnLocale.getString('R', "CallerCalleesRemove"));
      btn_remove.setText(AnLocale.getString("Remove", "REMOVE_SELECTED_FUNCTION_FROM_CALLSTACK"));
      btn_remove.setToolTipText(AnLocale.getString("Remove selected function from the callstack"));
      btn_remove.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Remove_Function(evt);
            }
          });
      tb.add(btn_remove);
      JLabel set_group = new JLabel("     ");
      tb.add(set_group);
      // btn_set_head = new JButton("Set Head");
      btn_set_head.setMnemonic(AnLocale.getString('e', "CallerCalleesSetHead"));
      btn_set_head.setText(
          AnLocale.getString("Set Head", "SET_SELECTED_FUNCTION_AS_HEAD_OF_CALLSTACK"));
      btn_set_head.setToolTipText(
          AnLocale.getString("Set selected function as head of the callstack"));
      btn_set_head.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Set_Head(evt);
            }
          });
      tb.add(btn_set_head);
      // btn_set_center = new JButton("Set Center");
      btn_set_center.setMnemonic(AnLocale.getString('s', "CallerCalleesCenter"));
      btn_set_center.setText(
          AnLocale.getString("Set Center", "SET_SELECTED_FUNCTION_AS_CALLSTACK"));
      btn_set_center.setToolTipText(AnLocale.getString("Set selected function as new callstack"));
      btn_set_center.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Set_Center(evt);
            }
          });
      tb.add(btn_set_center);
      // btn_set_tail = new JButton("Set Tail");
      btn_set_tail.setMnemonic(AnLocale.getString('l', "CallerCalleesTail"));
      btn_set_tail.setText(
          AnLocale.getString("Set Tail", "SET_SELECTED_FUNCTION_AS_TAIL_OF_CALLSTACK"));
      btn_set_tail.setToolTipText(
          AnLocale.getString("Set selected function as tail of the callstack"));
      btn_set_tail.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
              CS_Set_Tail(evt);
            }
          });
      tb.add(btn_set_tail);
      return tb;
    }

    // Action performed
    @Override
    public void actionPerformed(final ActionEvent event) {
      final String cmd = event.getActionCommand();
      if (cmd.equals(AnLocale.getString("Add"))) {
        System.out.println("Add");
      }
    }

    // Actions
    public void CS_Add_Function(final ActionEvent event) {
      if (btn_append_enabled) {
        int index = cd.callee.getSelectedRow();
        // if (index < 0) return;
        // cd.cc_callstack.last_selected_table = callee;
        cd.cc_callstack.append(index);
        computed = false;
        computeOnAWorkerThread();
      } else {
        int index = cd.caller.getSelectedRow();
        // if (index < 0) return;
        // cd.cc_callstack.last_selected_table = caller;
        cd.cc_callstack.prepend(index);
        computed = false;
        computeOnAWorkerThread();
      }
    }

    /*
     * Remove
     */
    public void CS_Remove_Function(final ActionEvent event) {
      int index = cd.func_item.getSelectedRow();
      // if (0 == index) {
      // cd.cc_callstack.last_selected_table = caller;
      // } else {
      // cd.cc_callstack.last_selected_table = callee;
      // }
      cd.cc_callstack.remove(index);
      computed = false;
      computeOnAWorkerThread();
    }

    /*
     * Set Head
     */
    public void CS_Set_Head(final ActionEvent event) {
      int index = cd.func_item.getSelectedRow();
      // if (index <= 0) return;
      cd.cc_callstack.setHead(index);
      computed = false;
      computeOnAWorkerThread();
    }

    /*
     * Set Center
     */
    public void CS_Set_Center(final ActionEvent event) {
      cd.cc_callstack.setCenter();
      computed = false;
      computeOnAWorkerThread();
    }

    /*
     * Set Tail
     */
    public void CS_Set_Tail(final ActionEvent event) {
      int index = cd.func_item.getSelectedRow();
      cd.cc_callstack.setTail(index);
      computed = false;
      computeOnAWorkerThread();
    }

    /*
     * Back
     */
    public void CS_Go_Back(final ActionEvent event) {
      cd.cc_callstack.gotoCallStackFrame(cd.cc_callstack.StackHistoryIndex - 1);
      computed = false;
      computeOnAWorkerThread();
    }

    /*
     * Forward
     */
    public void CS_Go_Forward(final ActionEvent event) {
      cd.cc_callstack.gotoCallStackFrame(cd.cc_callstack.StackHistoryIndex + 1);
      computed = false;
      computeOnAWorkerThread();
    }

    /*
     * Enable Buttons
     */
    public void CS_Enable_Buttons(final AnTable table) {
      if (table == cd.caller) {
        btn_add.setEnabled(true);
        btn_append_enabled = false;
        btn_remove.setEnabled(false);
        btn_set_head.setEnabled(false);
        btn_set_center.setEnabled(true);
        btn_set_tail.setEnabled(false);
        btn_back.setEnabled(cd.cc_callstack.isBackActionAvailable());
        btn_forward.setEnabled(cd.cc_callstack.isForwardActionAvailable());
      }
      if (table == cd.func_item) {
        int rows = table.getRowCount();
        int selrow = table.getSelectedRow();
        btn_add.setEnabled(false);
        btn_append_enabled = false;
        btn_remove.setEnabled(false);
        if (rows > 1) {
          if ((selrow + 1 == rows) || (0 == selrow)) {
            btn_remove.setEnabled(true);
          }
        }
        if (selrow > 0) {
          btn_set_head.setEnabled(true);
        } else {
          btn_set_head.setEnabled(false);
        }
        btn_set_center.setEnabled(true);
        if ((selrow + 1 != rows) && (rows > 1)) {
          btn_set_tail.setEnabled(true);
        } else {
          btn_set_tail.setEnabled(false);
        }
        btn_back.setEnabled(cd.cc_callstack.isBackActionAvailable());
        btn_forward.setEnabled(cd.cc_callstack.isForwardActionAvailable());
      }
      if (table == cd.callee) {
        btn_add.setEnabled(true);
        btn_append_enabled = true;
        btn_remove.setEnabled(false);
        btn_set_head.setEnabled(false);
        btn_set_center.setEnabled(true);
        btn_set_tail.setEnabled(false);
        btn_back.setEnabled(cd.cc_callstack.isBackActionAvailable());
        btn_forward.setEnabled(cd.cc_callstack.isForwardActionAvailable());
      }
    }
  } // end of class

  /*
   * Change selection
   */
  @Override
  public void changeSelection(AnTable src, int index) {
    long ID = -1;
    if (index >= 0) {
      clearSelections();
      if (src == caller) {
        if (index < ids_callers.length) {
          ID = ids_callers[index];
        }
      } else if (src == callee) {
        if (index < ids_callees.length) {
          ID = ids_callees[index];
        }
      } else if (src == func_item) {
        if (index < ids_cstack.length) {
          ID = ids_cstack[index];
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
                cc_callstack.setSelectedObject(id); // NM Hack
                cc_callstack.saveSelection(id, at, ix); // NM Hack
                //                      window.computeFuncData(); // summary
                window.getSelectionManager().updateSelection();
                AnUtility.dispatchOnSwingThread(
                    new Runnable() {
                      @Override
                      public void run() {
                        //                                window.showSummary(); // AWT
                        CS_ControlPanel.CS_Enable_Buttons(at); // AWT
                      }
                    });
              }
            },
            "CallTree_thread");
      } else {
        CS_ControlPanel.CS_Enable_Buttons(src); // AWT
      }
    }
  }

  //    public CalledByCallsDisp getCallTab(final int ptype) { // FIXUP: REARCH
  //        switch(ptype){
  //            case AnDisplay.DSP_SOURCE:
  //                return call_tab_src;
  //            case AnDisplay.DSP_DISASM:
  //                return call_tab_dis;
  //            case AnDisplay.DSP_FUNCTION:
  //                return call_tab_func;
  //        }
  //        return null;
  //    }
  // CallStack actions
  private final class CC_CallStack {

    long[] ids;
    String[] names;
    long selectionTime = 0;
    String lastFilter = null;
    long last_ID = -1;
    AnTable last_selected_table = null;
    int last_selected_row = -1;
    String API_V2_FUNCTION_Str = "FUNCTION";
    Vector StackHistory = new Vector(0);
    int StackHistoryIndex = -1;

    private final class CC_CallStackFrame {

      long[] cstack;
      long selected_ID;
      AnTable selected_table;
      int selected_row;
    }

    public void append(int index) {
      // NM TODO: Replace ids_cstack with ids
      if (index >= ids_callees.length) {
        System.out.println("ERROR");
        return;
      }
      long ID = ids_callees[index];
      long[] new_cstack = new long[ids_cstack.length + 1];
      for (int i = 0; i < ids_cstack.length; i++) {
        new_cstack[i] = ids_cstack[i];
      }
      new_cstack[new_cstack.length - 1] = ID;
      ids_cstack = new_cstack;
      ids = new_cstack;

      String[] new_ids_names = new String[ids_names.length + 1];
      for (int i = 0; i < ids_names.length; i++) {
        new_ids_names[i] = ids_names[i];
      }
      new_ids_names[ids_names.length] = (String) callee_data[name_col][index];
      ids_names = new_ids_names;
      names = new_ids_names;

      // Save selection
      saveSelection(ID, func_item, ids.length - 1);
      saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
    }

    public void prepend(int index) {
      // NM TODO: Replace ids_cstack with ids
      if (index >= ids_callers.length) {
        System.out.println("ERROR");
        return;
      }
      long ID = ids_callers[index];
      // last_ID = ID;
      long[] new_cstack = new long[ids_cstack.length + 1];
      new_cstack[0] = ID;
      for (int i = 1; i < new_cstack.length; i++) {
        new_cstack[i] = ids_cstack[i - 1];
      }
      ids_cstack = new_cstack;
      ids = new_cstack;

      // NM TODO: Replace ids_names with names
      String[] new_ids_names = new String[ids_names.length + 1];
      for (int i = 0; i < ids_names.length; i++) {
        new_ids_names[i + 1] = ids_names[i];
      }
      new_ids_names[0] = (String) caller_data[name_col][index];
      ids_names = new_ids_names;
      names = new_ids_names;

      // Save selection
      saveSelection(ID, func_item, 0);
      saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
    }

    public void remove(int index) {
      if ((index == 0) && (ids.length > 1)) {
        // Remove top function
        long[] new_cstack = new long[ids.length - 1];
        for (int i = 1; i < ids.length; i++) {
          new_cstack[i - 1] = ids[i];
        }
        long ID = ids[0];
        ids = new_cstack;
        ids_cstack = ids; // TEMPORARY

        String[] new_names = new String[names.length - 1];
        for (int i = 1; i < names.length; i++) {
          new_names[i - 1] = names[i];
        }
        names = new_names;
        ids_names = names; // TEMPORARY

        // Save selection
        saveSelection(ID, caller, -1);
        saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
      }
      if ((index > 0) && (ids.length - 1 == index)) {
        // Remove bottom function
        long[] new_cstack = new long[ids.length - 1];
        for (int i = 0; i < new_cstack.length; i++) {
          new_cstack[i] = ids[i];
        }
        long ID = ids[ids.length - 1];
        ids = new_cstack;
        ids_cstack = ids; // TEMPORARY

        String[] new_names = new String[names.length - 1];
        for (int i = 0; i < new_names.length; i++) {
          new_names[i] = names[i];
        }
        names = new_names;
        ids_names = names; // TEMPORARY

        // Save selection
        saveSelection(ID, callee, -1);
        saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
      }
    }

    public void setHead(int index) {
      for (int i = 0; i < index; i++) {
        cc_callstack.remove(0);
      }
      long ID = ids[0];
      // Save selection
      saveSelection(ID, func_item, 0);
      saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
    }

    public void setTail(int index) {
      for (int i = ids.length - 1; i > index; i--) {
        cc_callstack.remove(i);
      }
      long ID = ids[ids.length - 1];
      // Save selection
      saveSelection(ID, func_item, ids.length - 1);
      saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
    }

    public void setCenter() {
      reset();
      AnUtility.checkIPCOnWrongThread(false);
      getIDs();
      AnUtility.checkIPCOnWrongThread(true);
    }

    public void replace(long[] stack, int index) {
      long ID = stack[index];
      long[] new_cstack = new long[1];
      new_cstack[0] = ID;
      ids = new_cstack;
      ids_cstack = ids; // TEMPORARY

      // Get name
      String fname = window.getObjNameV2(ID);
      String[] new_names = new String[1];
      new_names[0] = fname;
      names = new_names;
      ids_names = names; // TEMPORARY

      // Save selection
      saveSelection(ID, func_item, 0);
      saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
    }

    public void reset() {
      ids = null;
      names = null;
      // StackHistoryIndex = -1;
      // StackHistory = new Vector(0);
    }

    public Object[][] recreateStackTable(Object[] raw_data) {
      LocalProcessData func_data_new_lpd = new LocalProcessData(raw_data);
      Object[][] func_data_new = func_data_new_lpd.get_data();

      LocalProcessData func_data_from_dbe_lpd = new LocalProcessData(raw_data);
      Object[][] func_data_from_dbe = func_data_from_dbe_lpd.get_data();
      AnAddress[] anAddress;
      AnDouble[] anDouble;
      AnInteger[] anInteger;
      AnLong[] anLong;
      String[] anString = new String[ids.length];
      names = window.getObjNamesV2(ids);
      for (int i = 0; i < ids.length; i++) {
        anString[i] = names[i]; // Better to get names from DBE
      }
      // Set names
      func_data_new[name_col] = anString;
      // Set values
      for (int i = 0; i < func_data_new.length; i++) {
        if (func_data_new[i] instanceof String[]) { // Name
          func_data_new[i] = anString;
        } else if (func_data_new[i] instanceof AnDouble[]) {
          anDouble = new AnDouble[ids.length];
          for (int k = 0; k < ids.length; k++) {
            anDouble[k] = new AnDouble(0.0);
            anDouble[k].showZero(false);
          }
          func_data_new[i] = anDouble;
          if (func_data_from_dbe[i].length > 0) {
            func_data_new[i][ids.length - 1] = func_data_from_dbe[i][0];
            ((AnDouble) func_data_new[i][ids.length - 1]).showZero(true);
            // } else {
            // No data - show empty metrics
          }
        } else if (func_data_new[i] instanceof AnLong[]) {
          anLong = new AnLong[ids.length];
          for (int k = 0; k < ids.length; k++) {
            anLong[k] = new AnLong(0);
            anLong[k].showZero(false);
          }
          func_data_new[i] = anLong;
          if (func_data_from_dbe[i].length > 0) {
            func_data_new[i][ids.length - 1] = func_data_from_dbe[i][0];
            ((AnLong) func_data_new[i][ids.length - 1]).showZero(true);
            // } else {
            // No data - show empty metrics
          }
        } else if (func_data_new[i] instanceof AnInteger[]) {
          anInteger = new AnInteger[ids.length];
          for (int k = 0; k < ids.length; k++) {
            anInteger[k] = new AnInteger(0);
            anInteger[k].showZero(false);
          }
          func_data_new[i] = anInteger;
          if (func_data_from_dbe[i].length > 0) {
            func_data_new[i][ids.length - 1] = func_data_from_dbe[i][0];
            ((AnInteger) func_data_new[i][ids.length - 1]).showZero(true);
            // } else {
            // No data - show empty metrics
          }
        } else if (func_data_new[i] instanceof AnAddress[]) {
          anAddress = new AnAddress[ids.length];
          for (int k = 0; k < ids.length; k++) {
            anAddress[k] = new AnAddress(0);
            anAddress[k].showZero(false);
          }
          func_data_new[i] = anAddress;
          if (func_data_from_dbe[i].length > 0) {
            func_data_new[i][ids.length - 1] = func_data_from_dbe[i][0];
            ((AnAddress) func_data_new[i][ids.length - 1]).showZero(true);
            // } else {
            // No data - show empty metrics
          }
        }
      }

      return func_data_new;
    }

    private long[] getIDs() {
      if (ids == null) {
        // Get ID
        long ID = window.getSelectedObject().getSelObjV2(API_V2_FUNCTION_Str); // IPC
        long[] new_cstack = new long[1];
        new_cstack[0] = ID;
        ids = new_cstack;
        ids_cstack = ids; // TEMPORARY

        // Get name
        String fname = window.getObjNameV2(ID); // IPC
        String[] new_names = new String[1];
        new_names[0] = fname;
        names = new_names;
        ids_names = names; // TEMPORARY

        // Get Filter
        lastFilter = window.getFilterStr();

        // Save selection
        saveSelection(ID, func_item, 0);
        saveCallStackFrame(ids, last_ID, last_selected_table, last_selected_row);
      }
      return ids;
    }

    public void setIDs(long[] stack) {
      // NM TODO: Remove ids_cstack
      ids = stack;
      ids_cstack = ids; // TEMPORARY
    }

    public String[] getNames() {
      if (names == null) {
        getIDs();
      }
      return names;
    }

    public void setNames(String[] names) {
      // NM TODO: Remove ids_names
      this.names = names;
      ids_names = names; // TEMPORARY
    }

    public void saveSelection(long ID, AnTable table, int index) {
      last_ID = ID;
      last_selected_table = table;
      last_selected_row = index;
    }

    /*
     * Call Stack History implementation
     */
    /*
     * Saves Call Stack frame in StackHistory vector.
     * StackHistoryIndex points to current frame in StackHistory.
     * @Parameters:
     * cstack
     *
     */
    public void saveCallStackFrame(long[] cstack, long ID, AnTable table, int index) {
      CC_CallStackFrame frame = new CC_CallStackFrame();
      frame.cstack = new long[cstack.length];
      for (int i = 0; i < cstack.length; i++) {
        frame.cstack[i] = cstack[i];
      }
      frame.selected_ID = ID;
      frame.selected_table = table;
      frame.selected_row = index;
      StackHistoryIndex++;
      if (StackHistoryIndex >= 0) {
        if (StackHistoryIndex + 1 < StackHistory.size()) {
          for (int i = StackHistory.size() - 1; i > StackHistoryIndex; i--) {
            StackHistory.removeElementAt(i);
          }
        }
      } else {
        StackHistoryIndex = 0;
      }
      StackHistory.setSize(StackHistoryIndex + 1);
      boolean alreadySaved = false;
      if (StackHistory.elementAt(StackHistoryIndex) != null) {
        // Check if this frame is already saved
        CC_CallStackFrame f = (CC_CallStackFrame) StackHistory.elementAt(StackHistoryIndex);
        if (f.cstack != null) {
          int i = f.cstack.length;
          int j = frame.cstack.length;
          if (i == j) {
            alreadySaved = true;
            for (i = 0; i < j; i++) {
              if (f.cstack[i] != frame.cstack[i]) {
                alreadySaved = false;
                break;
              }
            }
          }
        }
      }
      if (!alreadySaved) {
        StackHistory.setElementAt(frame, StackHistoryIndex);
      } else {
        StackHistoryIndex--;
        StackHistory.setSize(StackHistoryIndex + 1); // ?
      }
    }

    /*
     * Implements "Back" and "Forward" actions.
     * StackHistoryIndex points to current frame in StackHistory.
     * - action "Back" goes to ( StackHistoryIndex - 1 ) frame
     * - action "Forward" goes to ( StackHistoryIndex + 1 ) frame
     * @parameters:
     * index of frame in StackHistory
     */
    public void gotoCallStackFrame(int index) {
      CC_CallStackFrame frame;
      if (index < 0) {
        return;
      }
      if (index >= StackHistory.size()) {
        return;
      }
      frame = (CC_CallStackFrame) StackHistory.elementAt(index);
      setIDs(frame.cstack);
      StackHistoryIndex = index;
      setSelectedObject(frame.selected_ID); // ?
      last_ID = frame.selected_ID;
      last_selected_table = frame.selected_table;
      last_selected_row = frame.selected_row;
      //          window.computeFuncData(); // summary
      window.getSelectionManager().updateSelection();
    }

    public boolean isBackActionAvailable() {
      if (StackHistoryIndex > 0) {
        return true;
      }
      return false;
    }

    public boolean isForwardActionAvailable() {
      if (StackHistoryIndex >= 0) {
        if (StackHistoryIndex + 1 < StackHistory.size()) {
          if (StackHistory.elementAt(StackHistoryIndex + 1) != null) {
            return true;
          }
        }
      }
      return false;
    }

    /*
     * Quick and durty hack to set selected object
     */
    public void setSelectedObject(long ID) {
      window.getSelectedObject().setSelObjV2(ID);
      selectionTime = window.getSelectedObject().getSelObjectSelectionTime();
      // last_ID = ID;
      last_selected_table = null;
      last_selected_row = -1;
    }

    /*
     * Quick and durty hack to show selected object
     */
    public void computeSelection() {
      long ID = window.getSelectedObject().getSelObjV2(API_V2_FUNCTION_Str);
      if (ID == last_ID) {
        if (last_selected_table != null) {
          if (last_selected_row >= 0) {
            try {
              last_selected_table.setSelectedRow(last_selected_row);
              clearSelections();
              last_selected_table.setSelectedRow(last_selected_row);
              CS_ControlPanel.CS_Enable_Buttons(last_selected_table);
              return;
            } catch (Exception e) {
              // sorry, not a lucky day
              last_selected_table = null;
              last_selected_row = -1;
            }
          } else {
            last_selected_row = updateSelection(last_selected_table);
            if (last_selected_row >= 0) {
              CS_ControlPanel.CS_Enable_Buttons(last_selected_table);
              return;
            }
            last_selected_table = null;
          }
        }
      }
      String fname = window.getObjNameV2(ID);
      findName(fname, true, null, true); // NM Hack, need FindID function
    }

    /*
     * Quick and durty hack to show selected object
     */
    public int updateSelection(AnTable table) {
      long ID = window.getSelectedObject().getSelObjV2(API_V2_FUNCTION_Str);
      String fname = window.getObjNameV2(ID);
      findName(fname, true, table, true);
      int index = table.getSelectedRow();
      return index;
    }

    /*
     * Quick and durty hack to check if reset is needed
     */
    public boolean needReset() {
      String fs = window.getFilterStr();
      if (fs != null) {
        if (!fs.equals(lastFilter)) {
          // Filter changed
          return true;
        }
      }
      if (selectionTime != window.getSelectedObject().getSelObjectSelectionTime()) {
        // Selected Object was changed in another tab
        return true;
      }
      return false;
    }
  }

  @Override
  public boolean exportLimitSupported() {
    return false;
  }

  @Override
  public List<Subview> getVisibleSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    list.add(window.getTimelineCallStackSubview());
    list.add(window.getIoCallStackSubview());
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    return list;
  }
}
