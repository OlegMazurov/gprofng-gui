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
import org.gprofng.mpmt.AnTable.SelObjInfo;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;

public class SourceDisp extends FuncListDisp {

  private int[] src_type;
  // private boolean can_comp;
  private int prevScroll = -1;
  private boolean inCompute = false;
  private int my_src_type = AnTable.AT_SRC;
  private int my_src_type_only = AnTable.AT_SRC_ONLY;
  /** Keeps last warning to avoid showing the same warning message many times */
  private String lastWarning = null;

  protected SelObjInfo lastFilterSelection = null;
  protected long lastSelSrc = -1;
  protected long lastSelFunc = -1;

  // Constructor
  public SourceDisp(final AnWindow window, final int type, final String help_id) {
    super(window, type, 0, help_id);
  }

  // Constructor
  public SourceDisp(final AnWindow window, final int type, final String help_id, final int ptype) {
    super(window, type, 0, help_id);
    parent_type = ptype;
  }

  // Constructor
  public SourceDisp(
      final AnWindow window,
      final int type,
      final int subtype,
      final String help_id,
      final int ptype) {
    super(window, type, subtype, help_id);
    parent_type = ptype;
  }

  // Initialize GUI components
  protected void initComponents() {

    setLayout(new BorderLayout());
    String acName = null;
    String acDesc = null;
    JLabel acLabel = null;

    acName = AnLocale.getString("Source");
    acDesc = AnLocale.getString("Show source for selected function");
    if (acName != null) {
      acLabel = new JLabel(acName, JLabel.RIGHT);
    }
    table =
        new AnTable(type, true, true, can_sort, false, true, true, true, acName, acDesc, acLabel);
    table.setParent(this);
    if (acLabel != null) {
      acLabel.setVisible(false);
      acLabel.setDisplayedMnemonic(acName.charAt(0));
      table.add(acLabel);
    }
    table.addAnListener(new TableHandler());
    add(table, BorderLayout.CENTER);

    HotGapPanel hotGapPanel = new HotGapPanel(this);
    table.setHotGapPanel(hotGapPanel);
    add(hotGapPanel, BorderLayout.EAST);
    warningPane = new JPanel();
    warningPane.setVisible(false);
    //        warningPane.setBorder(AnVariable.textBorder);
    warningPane.setBorder(new LineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    add(warningPane, BorderLayout.SOUTH);
  }

  // Clear display
  @Override
  public void clear() {
    if (table != null) {
      table.removeAll();
    }
  }

  @Override
  public void clearHistory() {
    if (table != null) {
      table.clearHistory();
    }
  }

  public int getParentType() {
    return this.parent_type;
  }

  // Compute & update function list table
  @Override
  public void doCompute() {
    final long sel_obj;
    int new_ind;
    final int sort_ind;
    final Object[] raw_data;
    final String pwarnstr;
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

    if (lastFilterSelection != null) {
      window.getSelectedObject().setSelObjV2(lastFilterSelection.id);
    }

    if (!forceCompute
        && parent_type != AnDisplay.DSP_SourceDisassembly
        && table.srcRenderer.functionIdToRow != null) {
      long src_obj = window.getSelectedObject().getSelObj(DSP_SourceSelectedObject, 0);
      long funcId = window.getSelectedObject().getSelObjV2("FUNCTION");
      Integer targetRow = table.srcRenderer.functionIdToRow.get(funcId);
      if (targetRow != null && src_obj == sel_src && src_obj != 0) {
        int targetFuncRow = (targetRow - 1 >= 0) ? (targetRow - 1) : 0;
        long cur_sel_obj = window.getSelectedObject().getSelObj(type, subtype);
        int sel_ind = window.getSelectedObject().getSelIndex(cur_sel_obj, type, subtype);
        if (sel_ind != -1) {
          window.getSelectedObject().setSelObj(sel_ind, type, subtype);
          targetFuncRow = sel_ind;
          boolean recordHistory = table.getNavigationHistoryPool().getHistory().enabled;
          if (recordHistory) {
            table.getNavigationHistoryPool().getHistory().enabled = false;
          }
          table.setSelectedRow(targetFuncRow);
          if (recordHistory) {
            table.getNavigationHistoryPool().getHistory().enabled = true;
          }
          table.fireAnEvent(new AnEvent(table, AnEvent.EVT_SELECT, targetFuncRow, null));
          table.srcRenderer.tryAddNewObj();
          table.getAnParent().updateToolBar();
          inCompute = false;
          computed = true;

          window
              .getCalledByCallsSourceView()
              .doCompute(); // inside doCompute, on worker thread and synchronized
          // (AnVariable.mainFlowLock)
          lastSelFunc = table.srcRenderer.getFunctionBaseRowWhenCompare(targetFuncRow);
          if (tailAction != null) {
            tailAction.tailFunction();
            tailAction = null;
          }
          return;
        }
      }
    }

    if (forceCompute) {
      forceCompute = false;
    }

    sel_obj = window.getSelectedObject().getSelObj(type, subtype);
    // sel_func = window.getSelObj(DSP_FUNCTION, 0);
    // if (-1 == sel_func) {
    sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
    // (AnVariable.mainFlowLock)
    // }

    if (!computed) { // need re-compute

      reset();
      Object[] res = setFuncData(sel_func, type, subtype);
      new_ind = (int) ((long[]) (res[0]))[0];
      String errstr = ((String[]) (res[1]))[0];
      long sf_id = ((long[]) (res[0]))[1];
      String sf_name = ((String[]) (res[1]))[1];
      // Update table
      if (errstr == null) {
        updateWarningPanel();
        table.setViewport();
        Object[][] table_data;
        raw_data = getFuncList(type, subtype);
        Object[] raw_marks = getHotMarks(type);
        Object[] raw_marks_inc = getHotMarksInc(type);
        int[][] marks = new int[2][];
        marks[0] = (int[]) raw_marks[0];
        marks[1] = (int[]) raw_marks[1];
        int[][] marks_inc = new int[2][];
        marks_inc[0] = (int[]) raw_marks_inc[0];
        marks_inc[1] = (int[]) raw_marks_inc[1];
        final AnMetric[] mlist = getSettings().getMetricsSetting().getMetricListByDType(type);
        table_data = localProcessData(mlist, raw_data);
        // first index is for column, second index is for rows
        src_type = (int[]) raw_data[raw_data.length - 1]; // AT_SRC, DIS, QUOTE, etc.
        String[] hdrContent = getNames(type, 0); // name column table header contents (?)
        label = getSettings().getMetricsSetting().getLabel(table_data, null, type, table);
        name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
        sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
        new_ind = (new_ind == -1) ? 0 : new_ind;

        int sel_ind = window.getSelectedObject().getSelIndex(sel_obj, type, subtype);
        if (sel_ind == -1) {
          // XXX we should not call setSelObj when we go to the Source tab
          window
              .getSelectedObject()
              .setSelObj(new_ind, type, subtype); // set SelObj DSP_SRC_FILE before renderSrc()
          sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
          // (AnVariable.mainFlowLock)
        }
        table.setData(label, table_data, hdrContent, src_type, new_ind,
            name_col, sort_ind, marks, marks_inc);

        if (sel_func == 0) {
          // XXX we should not call setSelObj when we go to the Source tab
          window.getSelectedObject().setSelObj(new_ind, type, subtype);
        }
      } else {
        window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);

        if (sf_name == null || sf_name.startsWith("(")) { // FIXUP: need better API
          table.showMessage(errstr);
        } else {
          table.showMessage(new CannotFindFilePanel(errstr, sf_name, sf_id));
        }
        //                table.showMessage(errstr, sf_name, sf_id);
        computed = true;
        inCompute = false;
        return;
      }
    }

    // Update selected row
    new_ind = window.getSelectedObject().getSelIndex(sel_obj, type, subtype);
    if (new_ind == -1) {
      new_ind = table.getSelectedRow();
      if (new_ind == -1) {
        new_ind = 0;
      }
      table.showSelectedRow();
      // if (-1 == sel_func) { // Do we need sel_func?
      sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
      // }
    } else {
      table.setSelectedRow(new_ind);
    }
    // XXX we should not call setSelObj when we go to the Source tab
    window.getSelectedObject().setSelObj(new_ind, type, subtype);

    // scroll to previous location after re-sort
    if (prevScroll > 0) {
      table.setScroll(prevScroll);
      prevScroll = -1;
    }
    updateGap();
    // Update summary display
    if (parent_type != AnDisplay.DSP_SourceDisassembly) {
      updateSummary(new_ind);
    }

    computed = true;
    //        pstatstr = window.getMsg(AnUtility.PSTAT_MSG);
    pwarnstr = window.getMsg(AnUtility.PWARN_MSG);
    //        if (pstatstr != null) {
    //            window.appendLog(pstatstr);
    //        }
    if (pwarnstr != null) {
      window.showProcessorWarning(pwarnstr);
    }

    if (lastFilterSelection != null) {
      if (lastSelSrc == sel_src && lastFilterSelection.lineno != -1) {
        table.setSelectedRow(lastFilterSelection.lineno);
        // XXX we should not call setSelObj when we go to the Source tab
        window.getSelectedObject().setSelObj(lastFilterSelection.lineno, type, subtype);
      }
      lastFilterSelection = null;
      lastSelSrc = -1;
    }

    // Set toolbar and select subviews
    updateToolBar();

    window
        .getCalledByCallsSourceView()
        .doCompute(); // inside doCompute, on worker thread and synchronized
    // (AnVariable.mainFlowLock)

    lastSelFunc = table.srcRenderer.getFunctionBaseRowWhenCompare(new_ind);

    if (tailAction != null) {
      tailAction.tailFunction();
      tailAction = null;
    }
    inCompute = false;
  }

  protected BackForwardControls getBackForwardControls() {
    if (backForwardControls == null) {
      backForwardControls = new BackForwardControls();
    }
    backForwardControls.updateToolBarStatus();
    return backForwardControls;
  }

  @Override
  public void updateToolBar() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            if (backForwardControls == null) {
              backForwardControls = new BackForwardControls();
            }
            backForwardControls.updateToolBarStatus();
            if (parent_type == AnDisplay.DSP_SourceDisassembly) {
              if (type == AnDisplay.DSP_Disassembly) {
                SourceDisassemblyView.getToolbarPanelInternal().removeAll();
                SourceDisassemblyView.getToolbarPanelInternal()
                    .add(backForwardControls, BorderLayout.CENTER);
                SourceDisassemblyView.getToolbarPanelInternal().repaint();
              }
            } else if (parent_type == AnDisplay.DSP_DualSource) {
              DualSourceView.getToolbarPanelInternal().removeAll();
              DualSourceView.getToolbarPanelInternal()
                  .add(backForwardControls, BorderLayout.CENTER);
              DualSourceView.getToolbarPanelInternal().repaint();
            } else {
              AnCompDisp.getToolbarPanelInternal().removeAll();
              AnCompDisp.getToolbarPanelInternal().add(backForwardControls, BorderLayout.CENTER);
              AnCompDisp.getToolbarPanelInternal().repaint();
            }
          }
        });
  }

  @Override
  public void syncSrcDisWin() {
    if (parent_type == DSP_SourceDisassembly) {
      if (type == DSP_Source) {
        window.getSourceDisassemblyView().focusInHalf(AnTable.AT_SRC);
        window.getSourceDisassemblyView().syncHalf(AnTable.AT_DIS);
      } else if (type == DSP_Disassembly) {
        window.getSourceDisassemblyView().focusInHalf(AnTable.AT_DIS);
        window.getSourceDisassemblyView().syncHalf(AnTable.AT_SRC);
      }
    }
  }

  // Set current func/load-object in 'selected func' & summary display
  private void updateSummary(final int new_ind) {
    int[] rows = table.getSelectedRows();
    if (null != rows) {
      if (rows.length > 1) {
        // Special case for multiselection: pass only lines with my_src_type
        int len = 0;
        for (int i = 0; i < rows.length; i++) {
          int k = rows[i];
          if (k < src_type.length) {
            int stype = src_type[k];
            if (stype < 0) {
              stype = -stype;
            }
            if ((stype == my_src_type) || (stype == my_src_type_only)) {
              len++;
            }
          }
        }
        if ((len > 0) && (len != rows.length)) {
          long[] filtered_rows = new long[len];
          for (int i = 0, m = 0; i < rows.length; i++) {
            int k = rows[i];
            int stype = src_type[k];
            if (stype < 0) {
              stype = -stype;
            }
            if ((stype == my_src_type) || (stype == my_src_type_only)) {
              filtered_rows[m] = rows[i];
              m++;
            }
          }
          window.getSelectionManager().updateSelection(filtered_rows, type, subtype, 1);
          return;
        }
      }
      window.getSelectionManager().updateSelection(rows, type, subtype, 1);
      //  } else { // no source
    }
  }

  @Override
  public void goToLine(final AnTable tbl, final int row) {
    AnDisplay anDisplay = null;
    if (tbl.getNavigationHistoryPool() != null) {
      if (type == DSP_Source) {
        tbl.getNavigationHistoryPool().getHistory().goToSrcNew(row);
        anDisplay = window.getCalledByCallsSourceView();
      } else if (type == DSP_Disassembly) {
        tbl.getNavigationHistoryPool().getHistory().goToDisNew(row);
        anDisplay = window.getCalledByCallsDisassemblyView();
      }
      updateToolBar();
    }
    tbl.setSelectedRow(row);
    window.getSelectedObject().setSelObj(row, type, subtype);
    long curSelFunc = window.getSelectedObject().getSelObjV2("FUNCTION");
    if (curSelFunc != lastSelFunc || curSelFunc == 0) {
      lastSelFunc = curSelFunc;
      anDisplay.setComputed(false);
      anDisplay.computeOnAWorkerThread();
    }
    updateSummary(row);
    syncSrcDisWin();
  }

  // Listener for updating table
  private final class TableHandler implements AnListener {

    public void valueChanged(final AnEvent event) {
      final int from;
      final int to;
      int stype = 0;
      final boolean can_nav;

      switch (event.getType()) {
        case AnEvent.EVT_SELECT: // Selecting
          from = event.getValue();

          if (can_sort) {
            can_nav = true;
          } else {
            stype = src_type[from];
            if (stype < 0) {
              stype = -stype;
            }

            can_nav =
                (stype == AnTable.AT_SRC)
                    || (stype == AnTable.AT_SRC_ONLY)
                    || (stype == AnTable.AT_FUNC)
                    || (stype == AnTable.AT_DIS)
                    || (stype == AnTable.AT_QUOTE);
          }

          if ((type == DSP_Source)) {
            deselectRaceStack(subtype); // FIXUP: REARCH
            window.getSourceDisassemblyView().focusInHalf(AnTable.AT_SRC);
          }

          if ((type == DSP_SourceDisassembly) || (type == DSP_Disassembly)) {
            if (stype == AnTable.AT_DIS) {
              window.getSourceDisassemblyView().focusInHalf(AnTable.AT_DIS);
            } else { // SOURCE
              window.getSourceDisassemblyView().focusInHalf(AnTable.AT_SRC);
            }
          }

          // Set selected object & Update summary display
          if (can_nav) {
            window.getSelectedObject().setSelObj(from, type, subtype);
            long curSelFunc = window.getSelectedObject().getSelObjV2("FUNCTION");
            if (curSelFunc != lastSelFunc || curSelFunc == 0) {
              lastSelFunc = curSelFunc;
              window.getCalledByCallsSourceView().setComputed(false);
              window.getCalledByCallsSourceView().computeOnAWorkerThread();
            }
            updateSummary(from);
            if (parent_type == DSP_SourceDisassembly) {
              window.getSourceDisassemblyView().syncHalf(AnTable.AT_DIS);
            }
            //                        if ((type == DSP_SOURCE) || (type == DSP_DISASM)) {
            //                            // fixme, xxxx to make MARTY and RDT people happy, Do not
            // pop Summary Tab to top for Source or Disasm
            //                        } else {
            //                            window.showSummary();
            //                        }
          } else // in other cases (eg. AnTable.AT_SRC_ONLY), just update summary
          {
            long curSelFunc = window.getSelectedObject().getSelObjV2("FUNCTION");
            if (curSelFunc != lastSelFunc || curSelFunc == 0) {
              lastSelFunc = curSelFunc;
              window.getCalledByCallsSourceView().setComputed(false);
              window.getCalledByCallsSourceView().computeOnAWorkerThread();
            }
            updateSummary(from);
          }

          //                    if (parent_type == DSP_SOURCE_DISASM) { // XXXX don't update toolbar
          // for src in src&dis view
          //                        updateToolBar();
          //                    }
          break;
        case AnEvent.EVT_SORT: // Sorting
          // save current scroll location
          prevScroll = table.getScroll();
          int functionsColumn = getFuncSortColumn(((Integer) event.getAux()).intValue());
          getSettings()
              .getMetricsSetting()
              .setSortMetricByDType(this, functionsColumn, AnDisplay.DSP_Functions);
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
          getSettings().getMetricsSetting().setMetricOrderByDType(this, from, to, type);
          break;
      }
    }
  }

  protected int getFuncSortColumn(int srcdis_sort_ind) {
    int ret = -1;
    AnMetric[] func_mlist =
        getSettings().getMetricsSetting().getMetricListByMType(MetricsSetting.MET_NORMAL);
    AnMetric[] srcdis_mlist =
        getSettings().getMetricsSetting().getMetricListByMType(MetricsSetting.MET_SRCDIS);
    if (srcdis_sort_ind < 0 || srcdis_sort_ind >= srcdis_mlist.length) {
      return -1;
    }
    for (int ind = 0; ind < func_mlist.length; ind++) {
      if (srcdis_mlist[srcdis_sort_ind].getUserName().equals(func_mlist[ind].getUserName())) {
        ret = ind;
        break;
      }
    }
    if (ret == -1) {
      ret = getSettings().getMetricsSetting().getSortColumnByDType(AnDisplay.DSP_Functions);
    }
    return ret;
  }

  // Is current selected tab?
  @Override
  public void setSelected(final boolean set) {
    selected = set;
    window.getCalledByCallsSourceView().setSelected(set);
  }

  //    @Override
  //    public String exportAsText(Integer limit, ExportSupport.ExportFormat format, Character
  // delimiter) {
  //        // Update Total value
  //        AnObject[] totals = getTotals(type, subtype); // IPC call
  //        if (totals != null) {
  //            if (MaximumValues == null) {
  //                MaximumValues = new Object[2][totals.length];
  //                MaximumValues[1] = totals;
  //            }
  //            MaximumValues[0] = totals;
  //        }
  //        // Call super.exportAsText
  //        String text = super.exportAsText(limit, format, delimiter);
  //        return text;
  //    }
  public void filterChanged() {
    long id = window.getSelectedObject().getSelObjV2("FUNCTION");
    int cur_line = table.getSelectedRow();
    lastFilterSelection = new SelObjInfo(id, cur_line, "");
    lastSelSrc = sel_src;
    forceCompute = true;
  }

  /*
   * Class to render source code for simple syntax highlighting
   */
  static class SrcRenderer {

    // Fortran keywords (see CR 7206640)
    private final String[] FORTRAN95_KEYWORDS = {
      "assign",
      "backspace",
      "block",
      "data",
      "call",
      "close",
      "common",
      "continue",
      "data",
      "dimension",
      "do",
      "else",
      "if",
      "end",
      "endfile",
      "endif",
      "entry",
      "equivalence",
      "external",
      "format",
      "function",
      "goto",
      "implicit",
      "inquire",
      "intrinsic",
      "open",
      "parameter",
      "pause",
      "print",
      "program",
      "read",
      "return",
      "rewind",
      "rewrite",
      "save",
      "stop",
      "subroutine",
      "then",
      "write",
      "allocate",
      "allocatable",
      "case",
      "contains",
      "cycle",
      "deallocate",
      "elsewhere",
      "exit",
      "include",
      "interface",
      "intent",
      "module",
      "namelist",
      "nullify",
      "only",
      "operator",
      "optional",
      "pointer",
      "private",
      "procedure",
      "public",
      "result",
      "recursive",
      "select",
      "sequence",
      "target",
      "use",
      "while",
      "where",
      "elemental",
      "forall",
      "pure",
      "integer",
      "real",
      "complex",
      "logical",
      "character",
      "double",
      "precision",
      "type",
      "kind",
      "in",
      "out",
      "inout",
      "blockdata",
      "doubleprecision",
      "elseif",
      "elsewhere",
      "endblockdata",
      "enddo",
      "endforall",
      "endfunction",
      "endinterface",
      "endmodule",
      "endprogram",
      "endselect",
      "endsubroutine",
      "endtype",
      "endwhere",
      "selectcase",
      "selecttype"
    };
    private final String[] JAVA_KEYWORDS = {
      "abstract",
      "assert",
      "boolean",
      "break",
      "byte",
      "case",
      "catch",
      "char",
      "class",
      "const",
      "continue",
      "default",
      "do",
      "double",
      "else",
      "enum",
      "extends",
      "false",
      "final",
      "finally",
      "float",
      "for",
      "goto",
      "if",
      "implements",
      "import",
      "instanceof",
      "int",
      "interface",
      "long",
      "native",
      "new",
      "null",
      "package",
      "private",
      "protected",
      "public",
      "return",
      "short",
      "static",
      "strictfp",
      "super",
      "switch",
      "synchronized",
      "this",
      "throw",
      "throws",
      "true",
      "transient",
      "try",
      "void",
      "volatile",
      "while"
    };
    private final String[] C_KEYWORDS = {
      "auto",
      "_Bool",
      "break",
      "case",
      "char",
      "_Complex",
      "const",
      "continue",
      "default",
      "do",
      "double",
      "else",
      "enum",
      "extern",
      "float",
      "for",
      "goto",
      "if",
      "_Imaginary",
      "inline",
      "int",
      "long",
      "register",
      "restrict",
      "return",
      "short",
      "signed",
      "sizeof",
      "static",
      "struct",
      "switch",
      "typedef",
      "union",
      "unsigned",
      "void",
      "volatile",
      "while"
    };
    private final String[] CPP_KEYWORDS = {
      "alignas",
      "alignof",
      "and",
      "and_eq",
      "asm",
      "auto",
      "bitand",
      "bitor",
      "bool",
      "break",
      "case",
      "catch",
      "char",
      "char16_t",
      "char32_t",
      "class",
      "compl",
      "const",
      "constexpr",
      "const_cast",
      "continue",
      "decltype",
      "default",
      "delete",
      "do",
      "double",
      "dynamic_cast",
      "else",
      "enum",
      "explicit",
      "export",
      "extern",
      "false",
      "float",
      "for",
      "friend",
      "goto",
      "if",
      "inline",
      "int",
      "long",
      "mutable",
      "namespace",
      "new",
      "noexcept",
      "not",
      "not_eq",
      "nullptr ",
      "operator",
      "or",
      "or_eq",
      "private",
      "protected",
      "public",
      "register",
      "reinterpret_cast",
      "return",
      "short",
      "signed",
      "sizeof",
      "static",
      "static_assert",
      "static_cast",
      "struct",
      "switch",
      "template",
      "this",
      "thread_local",
      "throw",
      "true",
      "try",
      "typedef",
      "typeid",
      "typename",
      "union",
      "unsigned",
      "using",
      "virtual",
      "void",
      "volatile",
      "wchar_t",
      "while",
      "xor",
      "xor_eq"
    };
    private final String SPLIT_PATTERN = "[@$~!%&();,#<>*/= +-?:.|\\[\\]{}^]";
    protected final HashMap<String, Boolean> fortran95KeywordMap;
    protected final HashMap<String, Boolean> javaKeywordMap;
    protected final HashMap<String, Boolean> cKeywordMap;
    protected final HashMap<String, Boolean> cppKeywordMap;

    protected int caretLinePosition = -1;
    protected AnTable fl_table;

    public SrcRenderer(AnTable table) {
      fl_table = table;
      fortran95KeywordMap = new HashMap<String, Boolean>();
      javaKeywordMap = new HashMap<String, Boolean>();
      cKeywordMap = new HashMap<String, Boolean>();
      cppKeywordMap = new HashMap<String, Boolean>();
      for (int i = 0; i < FORTRAN95_KEYWORDS.length; i++) {
        fortran95KeywordMap.put(FORTRAN95_KEYWORDS[i], Boolean.TRUE);
      }
      for (int i = 0; i < JAVA_KEYWORDS.length; i++) {
        javaKeywordMap.put(JAVA_KEYWORDS[i], Boolean.TRUE);
      }
      for (int i = 0; i < C_KEYWORDS.length; i++) {
        cKeywordMap.put(C_KEYWORDS[i], Boolean.TRUE);
      }
      for (int i = 0; i < CPP_KEYWORDS.length; i++) {
        cppKeywordMap.put(CPP_KEYWORDS[i], Boolean.TRUE);
      }
    }

    protected String srcType = "c";
    protected Object[][] tableData = null;
    protected int[] srcTypeData = null;
    protected boolean srcInComment = false;
    protected HashMap<Integer, SrcTextMarker> srcRendered =
        null; // stores cached SrcTextMarker of each line
    protected HashMap<Integer, SrcTextPane> srcPaneCreated =
        null; // stores cached SrcTextPane of each line
    protected HashMap<Long, HashMap<Integer, SrcTextMarker>> srcRenderedMap =
        new HashMap<
            Long,
            HashMap<
                Integer,
                SrcTextMarker>>(); // stores cached srcRendered of each selected object that has
    // been shown in Source View before
    protected HashMap<Long, HashMap<Integer, SrcTextPane>> srcPaneCreatedMap =
        new HashMap<
            Long,
            HashMap<
                Integer,
                SrcTextPane>>(); // stores cached srcPaneCreated of each selected object that has
    // been shown in Source View before
    protected int maxSrcCachedNumber =
        15; // XXXX cacheing asumes the content will always be the same for the same DSP_SRC_FILE
    // object
    protected HashMap<Long, HashMap<Integer, String>> functionRenderedMap =
        new HashMap<Long, HashMap<Integer, String>>();
    protected HashMap<Integer, String> functionRendered = null; // a map from row to function name
    protected HashMap<Long, HashMap<Integer, Boolean>> functionCallerCalleeAddedMap =
        new HashMap<Long, HashMap<Integer, Boolean>>();
    protected HashMap<Integer, Boolean> functionCallerCalleeAdded =
        null; // a map from row to whether it's been added caller-callee info
    protected HashMap<Long, HashMap<Long, ArrayList<SelObjInfo>>> callerInfoMap =
        new HashMap<Long, HashMap<Long, ArrayList<SelObjInfo>>>();
    protected HashMap<Long, ArrayList<SelObjInfo>> callerInfo =
        null; // a map from function id to its callers' ids
    protected HashMap<Long, HashMap<Integer, ArrayList<SelObjInfo>>> calleeInfoMap =
        new HashMap<Long, HashMap<Integer, ArrayList<SelObjInfo>>>();
    protected HashMap<Integer, ArrayList<SelObjInfo>> calleeInfo =
        null; // a map from call site row to callees' ids
    protected HashMap<Long, ArrayList<Long>> functionIdMap = new HashMap<Long, ArrayList<Long>>();
    protected ArrayList<Long> functionId = null; // a vector of function id of each row or null
    protected HashMap<Long, HashMap<Long, Integer>> functionIdToRowMap =
        new HashMap<Long, HashMap<Long, Integer>>();
    protected HashMap<Long, Integer> functionIdToRow = null; // a map from function id to row
    protected HashMap<Long, HashMap<Integer, Integer>> lineNoToRowMap =
        new HashMap<Long, HashMap<Integer, Integer>>();
    protected HashMap<Integer, Integer> lineNoToRow = null; // a map from line number to row
    protected HashMap<Long, HashMap<Integer, Integer>> newRowToOldRowMap =
        new HashMap<Long, HashMap<Integer, Integer>>();
    protected HashMap<Integer, Integer> newRowToOldRow = null; // a map from new row to old row

    protected ArrayList<Integer> rowToFuncBaseRow = null;

    protected int old_viewmode = -1;

    protected Object[] getFuncCalleeInfo(final int type, final int[] rows, final int groupId) {
      synchronized (IPC.lock) {
        fl_table.getAnParent().window.IPC().send("getFuncCalleeInfo");
        fl_table.getAnParent().window.IPC().send(0);
        fl_table.getAnParent().window.IPC().send(type);
        fl_table.getAnParent().window.IPC().send(rows);
        fl_table.getAnParent().window.IPC().send(groupId);
        return (Object[]) fl_table.getAnParent().window.IPC().recvObject();
      }
    }

    protected Object[] getFuncCallerInfo(final int type, final int[] rows, final int groupId) {
      synchronized (IPC.lock) {
        fl_table.getAnParent().window.IPC().send("getFuncCallerInfo");
        fl_table.getAnParent().window.IPC().send(0);
        fl_table.getAnParent().window.IPC().send(type);
        fl_table.getAnParent().window.IPC().send(rows);
        fl_table.getAnParent().window.IPC().send(groupId);
        return (Object[]) fl_table.getAnParent().window.IPC().recvObject();
      }
    }

    protected long[] getFuncId(final int type, final int begin, final int length) {
      synchronized (IPC.lock) {
        fl_table.getAnParent().window.IPC().send("getFuncId");
        fl_table.getAnParent().window.IPC().send(0);
        fl_table.getAnParent().window.IPC().send(type);
        fl_table.getAnParent().window.IPC().send(begin);
        fl_table.getAnParent().window.IPC().send(length);
        return (long[]) fl_table.getAnParent().window.IPC().recvObject();
      }
    }

    public int mapRow(int newRow) {
      return newRowToOldRow.get(newRow) == null ? -1 : newRowToOldRow.get(newRow);
    }

    // Process the raw data from mixed Primary/Object to pure Object array.
    // Creates and updates MaximumValues.
    public Object[][] localProcessData(final Object[] raw_data) {
      // raw_data has one vector for each column of metric values.
      // In addition, it has a vector of dbe row identifiers (HistItem->obj->id)
      int i;
      if (raw_data == null) {
        return null;
      }

      final int nc = raw_data.length - 1; // number of metric columns
      if (nc < 0) {
        return null;
      }
      final Object[][] data = new Object[nc][];

      for (i = 0; i < nc; i++) {
        if (raw_data[i] instanceof double[]) {
          data[i] = AnDouble.toArray((double[]) raw_data[i]);
        } else if (raw_data[i] instanceof int[]) {
          data[i] = AnInteger.toArray((int[]) raw_data[i]);
        } else if (raw_data[i] instanceof long[]) {
          if ((((long[]) raw_data[i]).length == 0)
              || !AnAddress.isAddress(((long[]) raw_data[i])[0])) {
            data[i] = AnLong.toArray((long[]) raw_data[i]);
          } else {
            data[i] = AnAddress.toArray((long[]) raw_data[i]);
          }
        } else {
          data[i] = (Object[]) raw_data[i];
        }
      }
      return data;
    }

    protected void releaseMap(HashMap map, int count) {
      Set keys = map.keySet();
      int i = 0;
      for (Object key : keys) {
        map.put(key, null);
        i++;
        if (i >= count) {
          break;
        }
      }
    }

    protected void renderSrc(
        final String[] hdrContent,
        final Object[][] table_data,
        final int[] src_type,
        final long sel_src,
        final int view_mode) {
      tableData = table_data;
      srcTypeData = src_type;
      if (hdrContent.length <= 0) {
        return;
      }
      // set src type for source view
      if (hdrContent[0].startsWith("Source File")) {
        int dotPos = hdrContent[0].lastIndexOf('.');
        if (dotPos != -1) {
          srcType = hdrContent[0].substring(dotPos + 1);
          if (srcType.matches("[0-9]*\\)?")) {
            int dotPos2 = hdrContent[0].lastIndexOf('.', dotPos - 1);
            if (dotPos2 != -1) {
              srcType = hdrContent[0].substring(dotPos2 + 1, dotPos);
            }
          }
          srcType = srcType.replaceFirst("\\)", "");
          srcType = srcType.replaceFirst("_[a-zA-Z0-9\\-]*", "");
          int dummyPos = srcType.indexOf(" (not found");
          if (dummyPos != -1) {
            srcType = srcType.substring(0, dummyPos);
          }
        }
      }

      // initialize or store new cached data for the selected object
      if (srcRenderedMap.size() >= maxSrcCachedNumber
          || old_viewmode != view_mode) { // clear data cache before it gets too large
        releaseMap(srcRenderedMap, maxSrcCachedNumber / 3);
        releaseMap(srcPaneCreatedMap, maxSrcCachedNumber / 3);
        releaseMap(functionRenderedMap, maxSrcCachedNumber / 3);
        releaseMap(functionIdMap, maxSrcCachedNumber / 3);
        releaseMap(functionIdToRowMap, maxSrcCachedNumber / 3);
        releaseMap(lineNoToRowMap, maxSrcCachedNumber / 3);
        releaseMap(newRowToOldRowMap, maxSrcCachedNumber / 3);
        releaseMap(callerInfoMap, maxSrcCachedNumber / 3);
        releaseMap(calleeInfoMap, maxSrcCachedNumber / 3);
      }
      if (old_viewmode != view_mode) {
        old_viewmode = view_mode;
      }
      srcRendered = srcRenderedMap.get(sel_src);
      functionRendered = functionRenderedMap.get(sel_src);
      functionCallerCalleeAdded = functionCallerCalleeAddedMap.get(sel_src);
      srcPaneCreated = srcPaneCreatedMap.get(sel_src);
      functionId = functionIdMap.get(sel_src);
      functionIdToRow = functionIdToRowMap.get(sel_src);
      lineNoToRow = lineNoToRowMap.get(sel_src);
      newRowToOldRow = newRowToOldRowMap.get(sel_src);
      callerInfo = callerInfoMap.get(sel_src);
      calleeInfo = calleeInfoMap.get(sel_src);
      boolean isRenew = false;
      if (srcRendered == null || lineNoToRow == null || newRowToOldRow == null || sel_src == 0) {
        isRenew = true;
      }
      if (srcRendered == null) {
        srcRendered = new HashMap<Integer, SrcTextMarker>();
      }
      if (functionRendered == null) {
        functionRendered = new HashMap<Integer, String>();
      }
      if (functionCallerCalleeAdded == null) {
        functionCallerCalleeAdded = new HashMap<Integer, Boolean>();
      }
      if (functionId == null) {
        functionId = new ArrayList<Long>();
      }
      if (functionIdToRow == null) {
        functionIdToRow = new HashMap<Long, Integer>();
      }
      if (lineNoToRow == null) {
        lineNoToRow = new HashMap<Integer, Integer>();
      }
      if (newRowToOldRow == null) {
        newRowToOldRow = new HashMap<Integer, Integer>();
      }
      if (callerInfo == null) {
        callerInfo = new HashMap<Long, ArrayList<SelObjInfo>>();
      }
      if (calleeInfo == null) {
        calleeInfo = new HashMap<Integer, ArrayList<SelObjInfo>>();
      }
      if (isRenew) {
        srcRendered.clear();
        lineNoToRow.clear();
        newRowToOldRow.clear();

        for (int i = 0; i < table_data[fl_table.getNameCol()].length; i++) {
          Object value = table_data[fl_table.getNameCol()][i];
          int type = src_type[i] < 0 ? (-src_type[i]) : src_type[i];
          if (value instanceof String && (type == AnTable.AT_SRC || type == AnTable.AT_SRC_ONLY)) {
            SrcTextMarker sm = new SrcTextMarker(i, ((String) value));
            srcRendered.put(i, sm);
            newRowToOldRow.put(i, i);
          }
        }

        srcRenderedMap.put(sel_src, srcRendered);
        lineNoToRowMap.put(sel_src, lineNoToRow);
        newRowToOldRowMap.put(sel_src, newRowToOldRow);
      }

      if (srcPaneCreated == null || sel_src == 0) {
        if (srcPaneCreated == null) {
          srcPaneCreated = new HashMap<Integer, SrcTextPane>();
        }
        srcPaneCreated.clear();
        srcPaneCreatedMap.put(sel_src, srcPaneCreated);
      }

      // add function ids and caller callee info
      functionRendered.clear();
      functionCallerCalleeAdded.clear();
      functionId.clear();
      functionIdToRow.clear();
      callerInfo.clear();
      calleeInfo.clear();
      AnUtility.checkIPCOnWrongThread(false);
      long[] rawData = getFuncId(fl_table.getAnParent().type, 0, src_type.length);
      AnUtility.checkIPCOnWrongThread(true);
      long lastFuncId = 0;
      if (rawData != null) {
        for (int i = 0; i < rawData.length; i++) {
          long id = rawData[i];
          if (id == 0) {
            int start = i;
            int end = i + 4;
            boolean found = false;
            for (int k = start; k < end && k < rawData.length; k++) {
              int type = src_type[k] < 0 ? (-src_type[k]) : src_type[k];
              if (type == AnTable.AT_FUNC) {
                found = true;
                break;
              }
            }
            id = found ? 0 : lastFuncId;
          }
          functionId.add(id);
          int type = src_type[i] < 0 ? (-src_type[i]) : src_type[i];
          if (type == AnTable.AT_FUNC) {
            functionIdToRow.put(rawData[i], i);
            lastFuncId = rawData[i];
          }
        }
      }

      rowToFuncBaseRow = new ArrayList<Integer>();
      int baseRow = 0;
      for (int i = 0; i < table_data[fl_table.getNameCol()].length; i++) {
        Object value = table_data[fl_table.getNameCol()][i];
        int type = src_type[i] < 0 ? (-src_type[i]) : src_type[i];
        if (type == AnTable.AT_FUNC) {
          functionRendered.put(i, (String) value);
          baseRow = i;
        }
        rowToFuncBaseRow.add(baseRow);
      }

      functionRenderedMap.put(sel_src, functionRendered);
      functionCallerCalleeAddedMap.put(sel_src, functionCallerCalleeAdded);
      functionIdMap.put(sel_src, functionId);
      functionIdToRowMap.put(sel_src, functionIdToRow);
      callerInfoMap.put(sel_src, callerInfo);
      calleeInfoMap.put(sel_src, calleeInfo);

      tryAddNewObj();
    }

    public void tryAddNewObj() {
      // add new obj to history if it's different from current
      if (fl_table.getNavigationHistoryPool().getHistory().enabled
          && (fl_table.getType() == AnDisplay.DSP_Source
              || fl_table.getType() == AnDisplay.DSP_SourceV2)) {
        int row = fl_table.getSelectedRow();
        SrcTextMarker sm = srcRendered.get(mapRow(row));
        if (sm != null) {
          int lineNo = sm.lineNo;
          long fun_id = -1;
          if (functionId != null && row >= 0 && row < functionId.size()) {
            fun_id = functionId.get(row);
          }
          if (fun_id == 0) {
            fun_id = fl_table.getAnParent().window.getSelectedObject().getSelObjV2("FUNCTION");
          }
          SelObjInfo cur_so = fl_table.getNavigationHistoryPool().getHistory().getCurrent();
          if (cur_so == null || cur_so.id != fun_id || cur_so.lineno != lineNo) {
            SelObjInfo new_so = new SelObjInfo(fun_id, lineNo, "");
            fl_table.getNavigationHistoryPool().getHistory().goToNew(new_so);
          } else {
            fl_table.getNavigationHistoryPool().getHistory().newAdded = false;
          }
        }
      }
    }

    public void addFunctionByRow(int row) {
      if (row < 0 || row >= srcTypeData.length) {
        return;
      }
      int type = srcTypeData[row] < 0 ? (-srcTypeData[row]) : srcTypeData[row];
      if (type != AnTable.AT_FUNC) {
        return;
      }
      int[] funcIds = new int[1];
      funcIds[0] = row;
      String functionName =
          (tableData == null) ? null : (String) tableData[fl_table.getNameCol()][row];
      functionCallerCalleeAdded.put(row, true);
      AnUtility.checkIPCOnWrongThread(false);
      Object[] calleeData = getFuncCalleeInfo(fl_table.getType(), funcIds, fl_table.getGroupId());
      Object[] callerData = getFuncCallerInfo(fl_table.getType(), funcIds, fl_table.getGroupId());
      AnUtility.checkIPCOnWrongThread(true);
      if (calleeData != null
          && callerData != null
          && tableData != null
          && calleeData.length > 0
          && callerData.length > 0) {
        addFunction(row, functionName, (Object[]) calleeData[0], (Object[]) callerData[0]);
      }
    }

    public void addFunction(int row, String function, Object[] calleeData, Object[] callerData) {

      if (calleeData == null) {
        return;
      }
      String[] names = (String[]) calleeData[calleeData.length - 1];

      Object[][] return_data = localProcessData(calleeData);
      if (return_data.length != 2) {
        return; // error happens here
      }

      for (int i = 0; i < return_data[0].length; i++) {
        Integer callsite = ((AnInteger) return_data[0][i]).toInteger();
        if (callsite == null) {
          continue;
        }
        Long calleeId = ((AnLong) return_data[1][i]).toLong();
        String funcName = names[i];
        SelObjInfo fi = new SelObjInfo(calleeId, -1, funcName);
        ArrayList<SelObjInfo> callees = calleeInfo.get(callsite);
        if (callees == null) {
          callees = new ArrayList<SelObjInfo>();
          callees.add(fi);
          calleeInfo.put(callsite, callees);
          if (fl_table.getType() == AnDisplay.DSP_Source
              || fl_table.getType() == AnDisplay.DSP_SourceV2) {
            SrcTextMarker sm = srcRendered.get(mapRow(callsite));
            if (sm != null) {
              sm.isCallSite = true;
            }
          }
        } else {
          callees.add(fi);
        }
      }

      if (callerData == null) {
        return;
      }
      names = (String[]) callerData[callerData.length - 1];

      return_data = localProcessData(callerData);
      if (return_data.length != 2) {
        return; // error happens here
      }

      if (functionId == null || row < 0 || row >= functionId.size()) {
        return;
      }
      Long funcId = functionId.get(row);
      for (int i = 0; i < return_data[0].length; i++) {
        Long callerId = ((AnLong) return_data[0][i]).toLong();
        Integer lineNo = ((AnInteger) return_data[1][i]).toInteger();
        if (callerId == null) {
          continue;
        }
        String funcName = names[i];
        SelObjInfo fi = new SelObjInfo(callerId, lineNo, funcName);

        ArrayList<SelObjInfo> callers = callerInfo.get(funcId);
        if (callers == null) {
          callers = new ArrayList<SelObjInfo>();
          callers.add(fi);
          callerInfo.put(funcId, callers);
        } else {
          callers.add(fi);
        }
      }
    }

    protected String getFunctionWhenCompare(int row) {
      int funcRow;
      if (rowToFuncBaseRow == null) {
        funcRow = -1;
      } else if (rowToFuncBaseRow.get(row) == null) {
        funcRow = -1;
      } else {
        funcRow = rowToFuncBaseRow.get(row);
      }
      return functionRendered.get(funcRow);
    }

    protected int getFunctionBaseRowWhenCompare(int row) {
      if (rowToFuncBaseRow == null) {
        return 0;
      }
      try {
        if (rowToFuncBaseRow.get(row) == null) { //
          return 0;
        }
        return rowToFuncBaseRow.get(row);
      } catch (ArrayIndexOutOfBoundsException e) {
        // This can be a bug or a race condition
        return 0;
      }
    }

    public String getFunction(int row) {
      if (fl_table
          .getAnParent()
          .window
          .getSettings()
          .getCompareModeSetting()
          .comparingExperiments()) {
        return getFunctionWhenCompare(row);
      }
      if (functionId == null || row < 0 || row >= functionId.size()) {
        return null;
      }
      int funcRow = -1;
      Long id = functionId.get(row);
      if (id != null) {
        Integer funcR = functionIdToRow.get(id);
        if (funcR != null) {
          funcRow = funcR.intValue();
        }
      }
      return functionRendered.get(funcRow);
    }

    public int getFunctionBaseRow(int row) {
      if (fl_table
          .getAnParent()
          .window
          .getSettings()
          .getCompareModeSetting()
          .comparingExperiments()) {
        return getFunctionBaseRowWhenCompare(row);
      }
      if (functionId == null || row < 0 || row >= functionId.size()) {
        return 0;
      }
      int funcRow = 0;
      Long id = functionId.get(row);
      if (id != null) {
        Integer funcR = functionIdToRow.get(id);
        if (funcR != null) {
          funcRow = funcR.intValue();
        }
      }
      return funcRow;
    }

    public int getNextCallsite(int row) {
      int callsite = -1;
      if (calleeInfo == null) {
        return callsite;
      }
      for (int i = 0; i < calleeInfo.size(); i++) {
        if (calleeInfo.get(row + i) != null) {
          callsite = row + i;
          break;
        }
      }
      return callsite;
    }

    public int getRowByLineNo(int lineNo) {
      Integer result = lineNoToRow.get(lineNo);
      int ret = -1;
      if (result != null) {
        ret = result.intValue();
      }
      return ret;
    }

    public SrcTextPane getSrcTextPane(final String content, final JComponent cmp, final int row) {
      if (functionCallerCalleeAdded.get(getFunctionBaseRow(row)) == null) {
        addFunctionByRow(getFunctionBaseRow(row));
      }
      int lineBegin = content.indexOf('.');
      int lineNo = -1;
      if (lineBegin != -1) {
        lineNo = Integer.parseInt(content.substring(0, lineBegin).trim());
      }
      Integer oldRowObj =
          lineNoToRow == null
              ? null
              : lineNoToRow.get(
                  lineNo); // lineNoToRow is the one to one mapping between line number and the
      // index in srcRendered & srcPaneCreated
      int oldRow = oldRowObj == null ? 0 : oldRowObj;
      SrcTextMarker sm = srcRendered.get(oldRow);
      SrcTextPane st = srcPaneCreated.get(oldRow);
      boolean oldIsCallSite = false;
      if (oldRow != row) {
        newRowToOldRow.put(row, oldRow);
        if (calleeInfo != null && calleeInfo.get(row) == null && sm != null) {
          oldIsCallSite = sm.isCallSite;
          sm.isCallSite = false;
        }
      }
      if (st == null
          || !st.getText()
              .equalsIgnoreCase(content)) { // only create JTextPanel when it needs to be shown
        st = new SrcTextPane(content, cmp, sm);
        srcPaneCreated.put(oldRow, st);
      } else {
        if (oldRow != row && sm != null && oldIsCallSite && !sm.isCallSite) {
          st.unSetCallSiteStyle();
        }
      }
      return st;
    }

    // A JTextPane for src/dis table cell
    protected final class SrcTextPane extends JTextPane {

      private String text;
      private SrcTextMarker textMarker = null;

      private UnderlineHighlighter highlighter = new UnderlineHighlighter(Color.black);
      private Highlighter.HighlightPainter painter =
          highlighter.new UnderlineHighlightPainter(Color.black);

      public SrcTextPane(final String content, final JComponent cmp, final SrcTextMarker tm) {
        text = content;
        textMarker = tm;

        setFont(cmp.getFont());
        setBorder(AnVariable.labelBorder);

        setForeground(cmp.getForeground());
        setBackground(cmp.getBackground());

        setHighlighter(highlighter);

        Style style1 = addStyle("keyword", null);
        StyleConstants.setForeground(style1, AnVariable.KEYWORD_COLOR);
        Style style2 = addStyle("comment", null);
        StyleConstants.setForeground(style2, AnVariable.COMMENT_COLOR);
        Style style3 = addStyle("directive", null);
        StyleConstants.setForeground(style3, AnVariable.DIRECTIVE_COLOR);
        Style style4 = addStyle("string", null);
        StyleConstants.setForeground(style4, AnVariable.STRING_COLOR);
        Style style5 = addStyle("lineno", null);
        StyleConstants.setForeground(style5, AnVariable.LINENO_COLOR);
        Style style6 = addStyle("lineno_bold", null);
        StyleConstants.setForeground(style6, AnVariable.LINENO_COLOR_BOLD);
        StyleConstants.setBold(style6, true);

        StyledDocument doc = getStyledDocument();
        try {
          doc.insertString(0, content, null);
          if (textMarker == null) {
            return;
          }
          for (int i = 0; i < textMarker.marker.size(); i++) {
            int from = textMarker.marker.get(i).from;
            int length = textMarker.marker.get(i).length;
            String style = textMarker.marker.get(i).style;
            doc.setCharacterAttributes(from, length, getStyle(style), true);
          }
          if (textMarker.isCallSite) {
            highlighter.addHighlight(textMarker.codeBegin, textMarker.codeEnd, painter);
            for (int i = 0; i < textMarker.marker.size(); i++) {
              int from = textMarker.marker.get(i).from;
              int length = textMarker.marker.get(i).length;
              String style = textMarker.marker.get(i).style;
              if (style.equals("lineno")) {
                doc.setCharacterAttributes(from, length, getStyle("lineno_bold"), true);
                break;
              }
            }
          }
        } catch (BadLocationException e) {
          // System.err.println("BadLocationException: " + e);
          // e.printStackTrace();
        }

        final MouseMotionListener[] ls = getMouseMotionListeners();
        for (final MouseMotionListener l : ls) {
          removeMouseMotionListener(l);
        }

        SrcTextPaneMouseAdapter ma = new SrcTextPaneMouseAdapter(cmp, this);
        SrcTextPaneMouseMotionAdapter mam = new SrcTextPaneMouseMotionAdapter(cmp, this);
        SrcTextPaneMouseWheelAdapter maw = new SrcTextPaneMouseWheelAdapter(cmp, this);
        SrcTextPaneKeyAdapter ka = new SrcTextPaneKeyAdapter(cmp);
        this.addMouseListener(ma);
        this.addMouseMotionListener(mam);
        this.addMouseWheelListener(maw);
        addKeyListener(ka);

        this.setDragEnabled(false);
        this.setFont(cmp.getFont());
        this.setBorder(AnVariable.labelBorder);

        this.setForeground(cmp.getForeground());
        this.setBackground(cmp.getBackground());
        this.setOpaque(false);
        setEditable(false);
      }

      public void unSetCallSiteStyle() {
        StyledDocument doc = getStyledDocument();
        if (textMarker == null) {
          return;
        }

        Highlighter.Highlight[] hilites = highlighter.getHighlights();
        for (int i = 0; i < hilites.length; i++) {
          if (hilites[i].getPainter() instanceof UnderlineHighlighter.UnderlineHighlightPainter) {
            highlighter.removeHighlight(hilites[i]);
          }
        }
        for (int i = 0; i < textMarker.marker.size(); i++) {
          int from = textMarker.marker.get(i).from;
          int length = textMarker.marker.get(i).length;
          String style = textMarker.marker.get(i).style;
          if (style.equals("lineno_bold")) {
            doc.setCharacterAttributes(from, length, getStyle("lineno"), true);
            break;
          }
        }
      }

      public String getText() {
        return text;
      }
    }

    protected void propagateToTable(
        MouseEvent e, boolean changeSource, final Component parentComponent) {

      if (JTextPane.class.isAssignableFrom(e.getSource().getClass())) {
        int oldX = e.getX();
        int oldY = e.getY();
        Point p = e.getPoint();
        SwingUtilities.convertPointToScreen(p, e.getComponent());
        e.translatePoint(-oldX, -oldY);
        SwingUtilities.convertPointFromScreen(p, parentComponent);
        e.translatePoint(p.x, p.y);
        if (changeSource) {
          e.setSource(parentComponent);
        }
        parentComponent.dispatchEvent(e);
      }
    }

    protected final class SrcTextPaneKeyAdapter extends KeyAdapter {

      private Component parentComponent;

      public SrcTextPaneKeyAdapter(JComponent cmp) {
        parentComponent = cmp;
      }

      public void propagateToTable(KeyEvent e) {
        e.setSource(parentComponent);
        parentComponent.dispatchEvent(e);
      }

      @Override
      public void keyTyped(KeyEvent e) {
        propagateToTable(e);
      }

      @Override
      public void keyPressed(KeyEvent e) {
        propagateToTable(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        propagateToTable(e);
      }
    }

    protected final class SrcTextPaneMouseMotionAdapter extends MouseMotionAdapter {

      private Component parentComponent;
      private JTextPane pane;

      public SrcTextPaneMouseMotionAdapter(JComponent cmp, JTextPane p) {
        pane = p;
        parentComponent = cmp;
      }

      public void mouseMoved(MouseEvent e) {}
    }

    protected final class SrcTextPaneMouseWheelAdapter implements MouseWheelListener {

      private Component parentComponent;
      private JTextPane pane;

      public SrcTextPaneMouseWheelAdapter(JComponent cmp, JTextPane p) {
        pane = p;
        parentComponent = cmp;
      }

      public void mouseWheelMoved(MouseWheelEvent e) {
        propagateToTable(e, true, parentComponent);
      }
    }

    protected final class SrcTextPaneMouseAdapter extends MouseAdapter {

      private Component parentComponent;
      private JTextPane pane;

      public SrcTextPaneMouseAdapter(JComponent cmp, JTextPane p) {
        pane = p;
        parentComponent = cmp;
      }

      public void mouseClicked(MouseEvent e) {
        propagateToTable(e, true, parentComponent);
      }

      public void mousePressed(MouseEvent e) {
        caretLinePosition = pane.getCaretPosition();
      }

      public void mouseReleased(MouseEvent e) {}

      public void mouseEntered(MouseEvent e) {}

      public void mouseExited(MouseEvent e) {}
    }
    // A marker of the style for JTextPane for src/dis table cell

    protected final class SrcTextMarker {

      protected final class Highlighter {

        public int from;
        public int length;
        public String style;

        public Highlighter(int f, int l, String s) {
          from = f;
          length = l;
          style = s;
        }
      }

      public ArrayList<Highlighter> marker = new ArrayList<Highlighter>();
      private String text = "";
      private String type = "";
      private int curRow = -1;
      public int lineNo = -1;

      public int codeBegin = 0;
      public int codeEnd = 0;

      public boolean isCallSite = false;

      public SrcTextMarker(int row, final String content) {
        curRow = row;
        if (srcType.equalsIgnoreCase("f") || srcType.equalsIgnoreCase("for")) {
          type = "fortranfixed";
        } else if (srcType.equalsIgnoreCase("f90") || srcType.equalsIgnoreCase("f95")) {
          type = "fortranfree";
        } else if (srcType.equalsIgnoreCase("java")) {
          type = "java";
        } else if (srcType.equalsIgnoreCase("cpp")
            || srcType.equalsIgnoreCase("cc")
            || srcType.equalsIgnoreCase("cxx")
            || srcType.equalsIgnoreCase("hpp")
            || srcType.equalsIgnoreCase("hh")
            || srcType.equalsIgnoreCase("hxx")) {
          type = "cpp";
        } else if (srcType.equalsIgnoreCase("c")
            || srcType.equalsIgnoreCase("h")
            || srcType.equalsIgnoreCase("inc")) {
          type = "c";
        }
        text = content;
        renderCode();
      }

      public void renderDirective(int startPos, int endPos) {
        if (endPos <= startPos) {
          return;
        }

        int directiveEnd = endPos;
        int isSpace = 0;
        boolean nonSpaceSeen = false;
        for (int i = startPos; i < endPos; i++) {
          if (text.charAt(i) != ' ' && text.charAt(i) != '\t' && text.charAt(i) != '#') {
            nonSpaceSeen = true;
          }
          if (nonSpaceSeen
              && (text.charAt(i) == ' '
                  || text.charAt(i) == '\t'
                  || text.charAt(i) == '"'
                  || text.charAt(i) == '<')) {
            directiveEnd = i;
            isSpace = (text.charAt(i) == ' ' || text.charAt(i) == '\t') ? 1 : 0;
            break;
          }
        }

        marker.add(new Highlighter(startPos, directiveEnd - startPos, "directive"));
        if (text.substring(startPos, directiveEnd).equals("#include")) {
          marker.add(new Highlighter(directiveEnd + isSpace, endPos, "string"));
        }
      }

      public void renderComment(int startPos, int endPos) {
        if (endPos <= startPos) {
          return;
        }
        marker.add(new Highlighter(startPos, endPos - startPos, "comment"));
      }

      public void renderQuote(int startPos, int endPos) {
        if (endPos <= startPos) {
          return;
        }
        marker.add(new Highlighter(startPos, endPos - startPos, "string"));
      }

      // render code that is not multiline comment in /**/
      public void renderNonMLComment(int startPos, int endPos) {
        if (endPos <= startPos) {
          return;
        }

        boolean seenQuote = false;
        int start = -1;
        int end = 0;
        for (int i = startPos; i < endPos; i++) {
          if (text.charAt(i) == '"') {
            boolean isQuote = true;
            int k = i - 1;
            int slashNum = 0;
            while (k >= 0 && text.charAt(k) == '\\') {
              slashNum++;
              k--;
            }
            if (slashNum % 2 != 0) {
              isQuote = false;
            }
            if (isQuote) {
              if (seenQuote) {
                end = i + 1;
                renderQuote(start, end);
              } else {
                start = i;
                renderNonQuoteComment(end, start);
              }
              seenQuote = !seenQuote;
            }
          }
        }
        if (seenQuote) {
          renderQuote(start, endPos);
        } else {
          renderNonQuoteComment(end, endPos);
        }
      }

      // render non comment conde that is not inside ""
      public void renderNonQuoteComment(int startPos, int endPos) {
        if (endPos <= startPos) {
          return;
        }

        int startDirective = startPos;
        boolean foundDirective = false;
        if (type.equals("c") || type.equals("cpp")) {
          while (startDirective < endPos) {
            if (text.charAt(startDirective) == '#') {
              foundDirective = true;
              break;
            }
            startDirective++;
          }
        }
        if (foundDirective && text.charAt(startDirective) == '#') {
          renderDirective(startDirective, endPos);
        } else {
          String code = text.substring(startPos, endPos);
          String[] words = code.split(SPLIT_PATTERN);
          int curPos = startPos;
          for (int i = 0; i < words.length; i++) {
            if (words[i].length() == 0) {
            } else {
              if (type.equals("c")) {
                if (cKeywordMap.get(words[i]) != null) {
                  marker.add(new Highlighter(curPos, words[i].length(), "keyword"));
                }
              } else if (type.equals("cpp")) {
                if (cppKeywordMap.get(words[i]) != null) {
                  marker.add(new Highlighter(curPos, words[i].length(), "keyword"));
                }
              } else if (type.equals("java")) {
                if (javaKeywordMap.get(words[i]) != null) {
                  marker.add(new Highlighter(curPos, words[i].length(), "keyword"));
                }
              } else if (type.equals("fortranfixed") || type.equals("fortranfree")) {
                if (fortran95KeywordMap.get(words[i].toLowerCase()) != null) {
                  marker.add(new Highlighter(curPos, words[i].length(), "keyword"));
                }
              }
            }
            if (i < words.length - 1) {
              curPos += words[i].length() + 1;
            }
          }
        }
      }

      // render code before which /* is not seen
      public void renderCode(int start, int end) {
        if (start >= end) {
          return;
        }

        codeBegin = start;
        codeEnd = end;
        while (codeBegin != end) {
          if (text.charAt(codeBegin) != ' ') {
            break;
          }
          codeBegin++;
        }
        if (type.equals("fortranfixed")) {
          if ((text.charAt(start) == 'c' || text.charAt(start) == 'C' || text.charAt(start) == '*')
              && start + 1 < end
              && text.charAt(start + 1) != '$') {
            renderComment(start, end);
            return;
          }
        }
        boolean seenQuote = false;
        boolean seenCommentStart = false;
        int startPos = -1;
        int endPos = start;
        int tail = 1;
        if (type.equals("fortranfixed") || type.equals("fortranfree")) {
          tail = 0;
        }
        for (int i = start; i < end - tail; i++) {
          if (text.charAt(i) == '"') {
            boolean isQuote = true;
            int k = i - 1;
            int slashNum = 0;
            while (k >= 0 && text.charAt(k) == '\\') {
              slashNum++;
              k--;
            }
            if (slashNum % 2 != 0) {
              isQuote = false;
            }
            if (isQuote && !seenCommentStart) {
              seenQuote = !seenQuote;
            }
          }
          if (type.equals("fortranfixed") || type.equals("fortranfree")) {
            if (text.charAt(i) == '!' && i + 1 < end && text.charAt(i + 1) != '$') {
              if (!seenQuote && !seenCommentStart) {
                renderNonMLComment(endPos, i);
                renderComment(i, end);
                return;
              }
            }
          } else if (type.equals("c") || type.equals("cpp") || type.equals("java")) {
            if (text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
              if (!seenQuote && !seenCommentStart) {
                seenCommentStart = true;
                startPos = i;
                renderNonMLComment(endPos, startPos);
                i++;
                continue;
              }
            }
            if (text.charAt(i) == '*'
                && text.charAt(i + 1) == '/'
                && (i == 0 || text.charAt(i - 1) != '/')) {
              if (!seenQuote && seenCommentStart) {
                seenCommentStart = false;
                endPos = i + 2;
                renderComment(startPos, endPos);
                i++;
                continue;
              }
            }
            if (text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
              if (!seenQuote && !seenCommentStart) {
                renderNonMLComment(endPos, i);
                renderComment(i, end);
                return;
              }
            }
          }
        }
        if (seenCommentStart) {
          renderComment(startPos, end);
          srcInComment = true;
        } else {
          renderNonMLComment(endPos, end);
        }
      }

      public void renderCode() {
        int lineBegin = text.indexOf('.');
        if (lineBegin == -1) {
          lineBegin = 0;
        } else {
          lineNo = Integer.parseInt(text.substring(0, lineBegin).trim());
          lineNoToRow.put(lineNo, curRow);
          lineBegin += 2;
        }

        marker.add(new Highlighter(0, lineBegin, "lineno"));

        if (type.equals("fortranfixed") || type.equals("fortranfree")) {
          // assume fortran has no comment inside /**/
          renderCode(lineBegin, text.length());
          return;
        }
        // detect comments inside /**/ or after //
        if (srcInComment) {
          String[] nonQuote = text.split("\"");
          int commentEndPos = 0;
          boolean foundEnd = false;
          for (int i = 0; i < nonQuote.length; i += 2) { // only look at "*/" outside pairs of "\""
            if (i > 0) {
              commentEndPos += nonQuote[i - 2].length() + nonQuote[i - 1].length() + 2;
            }
            int idx = nonQuote[i].indexOf("*/");
            if (idx != -1) {
              commentEndPos += idx + 2;
              foundEnd = true;
              break;
            }
          }
          if (foundEnd) {
            renderComment(lineBegin, commentEndPos);
            srcInComment = false;
            renderCode(commentEndPos, text.length());
          } else {
            renderComment(lineBegin, text.length());
          }
          return;
        }
        renderCode(lineBegin, text.length());
      }
    }

    protected class UnderlineHighlighter extends DefaultHighlighter {

      // Painter used for this highlighter
      protected Highlighter.HighlightPainter painter;

      public UnderlineHighlighter(Color c) {
        painter = new UnderlineHighlightPainter(c);
      }

      // Convenience method to add a highlight with
      // the default painter.
      public Object addHighlight(int p0, int p1) throws BadLocationException {
        return addHighlight(p0, p1, painter);
      }

      public void setDrawsLayeredHighlights(boolean newValue) {
        // Illegal if false - we only support layered highlights
        if (newValue == false) {
          throw new IllegalArgumentException("UnderlineHighlighter only draws layered highlights");
        }
        super.setDrawsLayeredHighlights(true);
      }

      // Painter for underlined highlights
      public class UnderlineHighlightPainter extends LayeredHighlighter.LayerPainter {

        public UnderlineHighlightPainter(Color c) {
          color = c;
        }

        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
          // Do nothing: this method will never be called
        }

        public Shape paintLayer(
            Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
          g.setColor(color == null ? c.getSelectionColor() : color);

          Rectangle alloc = null;
          if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
            if (bounds instanceof Rectangle) {
              alloc = (Rectangle) bounds;
            } else {
              alloc = bounds.getBounds();
            }
          } else {
            try {
              Shape shape =
                  view.modelToView(
                      offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
              alloc = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
            } catch (BadLocationException e) {
              // System.err.println("BadLocationException: " + e);
              // e.printStackTrace();
              return null;
            }
          }

          FontMetrics fm = c.getFontMetrics(c.getFont());
          int baseline = alloc.y + alloc.height - fm.getDescent();
          int lowShift = 1;
          g.drawLine(alloc.x, baseline + lowShift, alloc.x + alloc.width, baseline + lowShift);

          return alloc;
        }

        protected Color color; // The color for the underline
      }
    }

    protected void goToCallee(int row, MouseEvent e) {
      if (calleeInfo == null || calleeInfo.get(row) == null) {
        return;
      }
      SrcTextPane dt = srcPaneCreated.get(mapRow(row));
      if (dt != null) {
        if (caretLinePosition >= dt.textMarker.codeBegin
            && caretLinePosition < dt.textMarker.codeEnd) {
          ArrayList<SelObjInfo> callees = calleeInfo.get(row);
          if (callees.size() > 1) {
            // pop up context menu
            JPopupMenu popup = fl_table.getMenuListener().initPopup(e);
            if (popup != null) {
              popup.show(e.getComponent(), e.getX(), e.getY());
              boolean pressDown = false;
              for (int i = 0;
                  i < 3;
                  i++) { // XXXX ugly assumption that the "show callee source" is the 3rd item
                if (!popup.getComponent(i).isEnabled()) {
                  continue;
                }
                popup.dispatchEvent(
                    new KeyEvent(popup, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, '\0'));
                pressDown = true;
              }
              if (pressDown) {
                popup.dispatchEvent(
                    new KeyEvent(popup, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_RIGHT, '\0'));
              }
            }
          } else if (callees.size() == 1) {
            long funcId = callees.get(0).id;
            Integer targetRow = functionIdToRow.get(funcId);

            Object progressBarHandle =
                fl_table
                    .getAnParent()
                    .window
                    .getSystemProgressPanel()
                    .progressBarStart(AnLocale.getString("Callee"));
            // add new obj to history if it's different from current
            fl_table.getNavigationHistoryPool().getHistory().goToSrcNew(row);
            // change selected object
            fl_table.getAnParent().window.getSelectedObject().setSelObjV2(funcId);
            if (targetRow != null) {
              int targetFuncRow = (targetRow - 1 >= 0) ? (targetRow - 1) : 0;
              fl_table.getNavigationHistoryPool().getHistory().enabled = false;
              fl_table.setSelectedRow(targetFuncRow);
              fl_table.getNavigationHistoryPool().getHistory().enabled = true;
              fl_table.fireAnEvent(new AnEvent(fl_table, AnEvent.EVT_SELECT, targetFuncRow, null));
              tryAddNewObj();
              fl_table.getAnParent().updateToolBar();
            } else {
              fl_table.getAnParent().setComputed(false);
              fl_table.getAnParent().computeOnAWorkerThread();
            }
            fl_table.updateAnTable(AnTable.STR_ACTION_SHOW_CALLEE_SOURCE);
            fl_table
                .getAnParent()
                .window
                .getSystemProgressPanel()
                .progressBarStop(progressBarHandle);
          }
        }
      }
    }
  }
}
