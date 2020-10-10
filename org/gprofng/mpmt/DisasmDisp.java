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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnTable.SelObjInfo;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class DisasmDisp extends SourceDisp {

  private int[] src_type;
  private int prevScroll = -1;
  private boolean inCompute = false;
  private int my_src_type = AnTable.AT_DIS;
  private int my_src_type_only = AnTable.AT_DIS_ONLY;
  /** Keeps last warning to avoid showing the same warning message many times */
  private String lastWarning = null;

  private long lastSelObj = 0;

  // Constructor
  public DisasmDisp(final AnWindow window, final int type, final String help_id) {
    super(window, type, help_id);
  }

  // Constructor
  public DisasmDisp(final AnWindow window, final int type, final String help_id, final int ptype) {
    super(window, type, help_id, ptype);
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {

    setLayout(new BorderLayout());
    String acName = null;
    String acDesc = null;
    JLabel acLabel = null;

    acName = AnLocale.getString("Disassembly");
    acDesc = AnLocale.getString("Show disassemled code for selected function");
    if (acName != null) {
      acLabel = new JLabel(acName, JLabel.RIGHT);
    }
    table =
        new AnTable(type, true, true, can_sort, false, true, true, true, acName, acDesc, acLabel);
    table.setParent(this);
    if (acLabel != null) {
      acLabel.setVisible(false);
      //            acLabel.setDisplayedMnemonic(acName.charAt(0));
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

    sel_obj = window.getSelectedObject().getSelObj(type, subtype);
    // sel_func = window.getSelObj(DSP_FUNCTION, 0);
    // if (-1 == sel_func) {
    long lastSelFunc = sel_func;
    sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
    // (AnVariable.mainFlowLock)
    if (!forceCompute
        && parent_type != AnDisplay.DSP_SourceDisassembly
        && lastSelFunc == sel_func
        && lastSelObj == sel_obj) {
      computed = true;
    }

    if (forceCompute) {
      forceCompute = false;
    }
    // }

    if (!computed) { // need re-compute

      lastSelObj = sel_obj;
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
        table_data =
            localProcessData(
                mlist, raw_data); // first index is for column, second index is for rows
        src_type = (int[]) raw_data[raw_data.length - 1]; // AnTable.AT_SRC, DIS, QUOTE, etc.
        String[] hdrContent = getNames(type, 0); // name column table header contents (?)
        label = getSettings().getMetricsSetting().getLabel(table_data, null, type, table);
        name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
        sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
        int func_ind = (new_ind == -1) ? 0 : new_ind;
        new_ind = window.getSelectedObject().getSelIndex(sel_obj, type, subtype);
        new_ind = (new_ind == -1) ? func_ind : new_ind;
        table.setData(
            label, table_data, hdrContent, src_type, new_ind, name_col, sort_ind, marks, marks_inc);

        if (sel_func == 0) {
          window.getSelectedObject().setSelObj(new_ind, type, subtype);
        }
      } else {
        window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);
        table.showMessage(errstr);
      }
    }

    // Update selected row
    new_ind = window.getSelectedObject().getSelIndex(sel_obj, type, subtype);
    if (new_ind == -1) {
      new_ind = table.getSelectedRow();
      table.showSelectedRow();
      window.getSelectedObject().setSelObj(new_ind, type, subtype);
      // if (-1 == sel_func) { // Do we need sel_func?
      sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
      // }
    } else {
      table.setSelectedRow(new_ind);
    }

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
      if (lastSelSrc == sel_src) {
        table.setSelectedRow(lastFilterSelection.lineno);
        // XXX we should not call setSelObj when we go to the Source tab
        window.getSelectedObject().setSelObj(lastFilterSelection.lineno, type, subtype);
      }
      lastFilterSelection = null;
      lastSelSrc = -1;
    }

    window
        .getCalledByCallsDisassemblyView()
        .doCompute(); // inside doCompute, on worker thread and synchronized
    // (AnVariable.mainFlowLock)

    // Set toolbar and select subviews
    updateToolBar();

    if (tailAction != null) {
      tailAction.tailFunction();
      tailAction = null;
    }
    inCompute = false;
  }

  // Is current selected tab?
  public void setSelected(final boolean set) {
    selected = set;
    window.getCalledByCallsDisassemblyView().setSelected(set);
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    // Update Total value
    AnObject[] totals = getTotals(type, subtype); // IPC call
    if (totals != null) {
      if (MaximumValues == null) {
        MaximumValues = new Object[2][totals.length];
        MaximumValues[1] = totals;
      }
      MaximumValues[0] = totals;
    }
    // Call super.exportAsText
    String text = super.exportAsText(limit, format, delimiter);
    return text;
  }

  // Set current func/load-object in 'selected func' & summary display
  private void updateSummary(final int new_ind) {
    // Use "AWT-EventQueue-0" thread to avoid race conditions
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
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
        });
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
            window.getCalledByCallsDisassemblyView().setComputed(false);
            window.getCalledByCallsDisassemblyView().computeOnAWorkerThread();
            updateSummary(from);
            if (parent_type == DSP_SourceDisassembly) {
              window.getSourceDisassemblyView().syncHalf(AnTable.AT_SRC);
            }
            //                        if ((type == DSP_SOURCE) || (type == DSP_DISASM)) {
            //                            // fixme, xxxx to make MARTY and RDT people happy, Do not
            // pop Summary Tab to top for Source or Disasm
            //                        } else {
            //                            window.showSummary();
            //                        }
          } else // in other cases (eg. AT_SRC_ONLY), just update summary
          {
            window.getCalledByCallsDisassemblyView().setComputed(false);
            window.getCalledByCallsDisassemblyView().computeOnAWorkerThread();
            updateSummary(from);
          }

          if (parent_type == DSP_SourceDisassembly) {
            updateToolBar();
          }

          break;
        case AnEvent.EVT_SORT: // Sorting
          // save current scroll location
          prevScroll = table.getScroll();
          //                    int typeForPresentation = isOmpInxObj() ? AnDisplay.DSP_FUNCTION :
          // type;
          //                    getSettings().updateSortList(((Integer) event.getAux()).intValue(),
          // typeForPresentation);
          int func_sort_col = getFuncSortColumn(((Integer) event.getAux()).intValue());
          getSettings()
              .getMetricsSetting()
              .setSortMetricByDType(this, func_sort_col, AnDisplay.DSP_Functions);
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

  /*
   * Class to render source code for simple syntax highlighting
   */
  static class DisRenderer extends SrcRenderer {

    private final String[] BRANCH_INSTRS = {
      "goto", // branch for java byte code
      "goto_w",
      "jsr",
      "jsr_w",
      "if_acmpeq",
      "if_acmpne",
      "if_icmpeq",
      "if_icmpne",
      "if_icmplt",
      "if_icmpge",
      "if_icmpgt",
      "if_icmple",
      "ifeq",
      "ifne",
      "iflt",
      "ifge",
      "ifgt",
      "ifle",
      "ifnonnull",
      "ifnull",
      "fba", // float-point branch on SPARC
      "fbn",
      "fbu",
      "fbg",
      "fbug",
      "fbl",
      "fbul",
      "flg",
      "fbne",
      "fbe",
      "fbue",
      "fbge",
      "fbuge",
      "fble",
      "fbule",
      "fbo",
      "cbn", // coprocessor branch on SPARC
      "cb123",
      "cb12",
      "cb13",
      "cb1",
      "cb23",
      "cb2",
      "cb3",
      "cba",
      "cb0",
      "cb03",
      "cb02",
      "cb023",
      "cb01",
      "cb013",
      "cb012",
      "cwbne", // compare and branch on SPARC
      "cwbe",
      "cwbg",
      "cwble",
      "cwbge",
      "cwbl",
      "cwbgu",
      "cwbleu",
      "cwbcc",
      "cwbcs",
      "cwbpos",
      "cwbneg",
      "cwbvc",
      "cwbvs",
      "cxbne",
      "cxbe",
      "cxbg",
      "cxble",
      "cxbge",
      "cxbl",
      "cxbgu",
      "cxbleu",
      "cxbcc",
      "cxbcs",
      "cxbpos",
      "cxbneg",
      "cxbvc",
      "cxbvs",
      "jmpl", // jump on SPARC
      "jmp",
      "bne", // integer branch on SPARC
      "ba",
      "b",
      "be",
      "bn",
      "bg",
      "ble",
      "blu",
      "bgeu",
      "bge",
      "bgu",
      "bleu",
      "bcc",
      "bcs",
      "bpos",
      "bneg",
      "bvc",
      "bvs",
      "brz",
      "brlez",
      "brlz",
      "brnz",
      "brgz",
      "brgez",
      "bz",
      "bnz",
      "bl"
    };

    private final String[] X86_BRANCH_INSTRS = {
      "jmp", // branch on x86
      "jno",
      "jb",
      "jnae",
      "jc",
      "jnb",
      "jae",
      "jnc",
      "jz",
      "je",
      "jnz",
      "jne",
      "jbe",
      "jna",
      "jnbe",
      "ja",
      "js",
      "jns",
      "jp",
      "jpe",
      "jnp",
      "jpo",
      "jl",
      "jnge",
      "jnl",
      "jge",
      "jle",
      "jng",
      "jnle",
      "jg",
      "jo",
      "jmpf",
      "loopnz",
      "loopne",
      "loopz",
      "loope",
      "loop",
      "jecxz",
      "jrcxz",
      "jmpe",
      "syscall",
      "sysenter",
      "sysexit",
      "rsm",
      "loadall"
    };

    private static final String SPLIT_PATTERN = "[(),:* \\[\\]]";
    public static final int AT_DIS = 3;
    protected static final int AT_DIS_ONLY = 8;
    protected int maxDisCachedNumber =
        1; // XXXX don't cache disasm view data since there is no consistant row to content map
    protected int caretLinePosition = -1;
    protected String previousSelectedReg = "";
    protected Integer previousSelectedFunction = -1;
    protected HashMap<Integer, HashMap<String, ArrayList<Integer>>> regsInFunction =
        new HashMap<
            Integer,
            HashMap<
                String,
                ArrayList<Integer>>>(); // stores for each reg which rows it's in for each function
    protected HashMap<Integer, DisTextMarker> disRendered =
        null; // stores cached DisTextMarker of each line
    protected HashMap<Integer, DisTextPane> disPaneCreated =
        null; // stores cached DisTextPane of each line
    protected HashMap<Long, HashMap<Integer, DisTextMarker>> disRenderedMap =
        new HashMap<
            Long,
            HashMap<
                Integer,
                DisTextMarker>>(); // stores cached disRendered of each selected object that has
    // been shown in Source View before
    protected HashMap<Long, HashMap<Integer, DisTextPane>> disPaneCreatedMap =
        new HashMap<Long, HashMap<Integer, DisTextPane>>();

    protected HashMap<String, Integer> addrToRow = new HashMap<String, Integer>();
    protected HashMap<Integer, String> rowToAddr = new HashMap<Integer, String>();

    protected HashMap<String, Boolean> addrIsBranch = new HashMap<String, Boolean>();

    protected boolean isJavaByteCode = false;
    protected boolean isSparc = false;
    protected boolean isX86 = false;

    protected boolean reachedMemLimit = false;
    protected int numRows = 0;

    protected final HashMap<String, Boolean> branchInstrMap;
    protected final HashMap<String, Boolean> x86BranchInstrMap;

    public DisRenderer(AnTable table) {
      super(table);
      branchInstrMap = new HashMap<String, Boolean>();
      for (int i = 0; i < BRANCH_INSTRS.length; i++) {
        branchInstrMap.put(BRANCH_INSTRS[i], Boolean.TRUE);
      }
      x86BranchInstrMap = new HashMap<String, Boolean>();
      for (int i = 0; i < X86_BRANCH_INSTRS.length; i++) {
        x86BranchInstrMap.put(X86_BRANCH_INSTRS[i], Boolean.TRUE);
      }
    }

    public boolean regEquals(String r0, String r1) {
      if ((r0 == null && r1 != null) || (r1 == null && r0 != null)) {
        return false;
      } else if (r0 == null && r1 == null) {
        return true;
      }
      String reg0 = r0.replaceFirst("%", "");
      String reg1 = r1.replaceFirst("%", "");
      if (!isSparc) {
        if (reg0.matches("[gG][0-7]") || reg0.matches("%[oO][0-7]")) {
          isSparc = true;
        }
      }
      if (isSparc) {
        if (reg0.equalsIgnoreCase(reg1)) {
          return true;
        }
      }
      if (reg0.equalsIgnoreCase(reg1)) {
        return true;
      } else {
        if (reg0.matches("[rR]([89]|[1][0-6])[bwdBWD]?")
            && reg1.matches("[rR]([89]|[1][0-6])[bwdBWD]?")) {
          String number0 = reg0.replaceFirst("[rR]", "");
          number0 = number0.replaceFirst("[bwdBWD]", "");
          String number1 = reg1.replaceFirst("[rR]", "");
          number1 = number1.replaceFirst("[bwdBWD]", "");
          return number0.equalsIgnoreCase(number1);
        } else if (reg0.matches("[erER]?[a-dA-D][xX]|[a-dA-D][hlHL]")
            && reg1.matches("[erER]?[a-dA-D][xX]|[a-dA-D][hlHL]")) {
          String number0 = reg0.replaceFirst("[erER]", "");
          number0 = number0.replaceFirst("[xhlXHL]", "");
          String number1 = reg1.replaceFirst("[erER]", "");
          number1 = number1.replaceFirst("[xhlXHL]", "");
          return number0.equalsIgnoreCase(number1);
        } else if (reg0.matches("[erER]?(SI|si|DI|di|BP|bp|SP|sp)|(SI|si|DI|di|BP|bp|SP|sp)[lL]")
            && reg1.matches("[erER]?(SI|si|DI|di|BP|bp|SP|sp)|(SI|si|DI|di|BP|bp|SP|sp)[lL]")) {
          String number0 = reg0.replaceFirst("[erER]", "");
          number0 = number0.replaceFirst("[lL]", "");
          String number1 = reg1.replaceFirst("[erER]", "");
          number1 = number1.replaceFirst("[lL]", "");
          return number0.equalsIgnoreCase(number1);
        } else if (reg0.matches("[xyXY](MM|mm)([0-9]|[1][0-5])")
            && reg1.matches("[xyXY](MM|mm)([0-9]|[1][0-5])")) {
          String number0 = reg0.replaceFirst("[xyXY](MM|mm)", "");
          String number1 = reg1.replaceFirst("[xyXY](MM|mm)", "");
          return number0.equalsIgnoreCase(number1);
        }
        return false;
      }
    }

    public String regFullName(final String r) {
      if (r == null) {
        return r;
      }
      if (!isSparc) {
        if (r.matches("%[gG][0-7]") || r.matches("%[oO][0-7]")) {
          isSparc = true;
        }
      }
      if (isSparc) {
        return r;
      }
      String reg = r.replaceFirst("%", "");
      if (reg.matches("[rR]([89]|[1][0-6])[bwdBWD]?")) {
        String number = reg.replaceFirst("[rR]", "");
        number = number.replaceFirst("[bwdBWD]", "");
        return "%r" + number;
      } else if (reg.matches("[erER]?[a-dA-D][xX]|[a-dA-D][hlHL]")) {
        isX86 = true;
        String number = reg.replaceFirst("[erER]", "");
        number = number.replaceFirst("[xhlXHL]", "");
        return "%r" + number + "x";
      } else if (reg.matches("[erER]?(SI|si|DI|di|BP|bp|SP|sp)|(SI|si|DI|di|BP|bp|SP|sp)[lL]")) {
        String number = reg.replaceFirst("[erER]", "");
        number = number.replaceFirst("[lL]", "");
        return "%r" + number;
      } else if (reg.matches("[xyXY](MM|mm)([0-9]|[1][0-5])")) {
        isX86 = true;
        String number = reg.replaceFirst("[xyXY](MM|mm)", "");
        return "%ymm" + number;
      }
      return r;
    }

    protected void renderDis(
        final Object[][] table_data, final int[] src_type, final long sel_instr) {
      tableData = table_data;
      srcTypeData = src_type;
      previousSelectedReg = "";
      previousSelectedFunction = -1;

      // initialize or store new cached data for the selected object
      // cache disasm data per function since we don't have a unique id for disasm data in dbe
      if (disRenderedMap.size()
          >= maxDisCachedNumber) { // clear data cache before it gets too large
        disRenderedMap.clear();
        disPaneCreatedMap.clear();
      }
      disRendered = disRenderedMap.get(sel_instr);
      disPaneCreated = disPaneCreatedMap.get(sel_instr);
      if (disRendered == null || sel_instr == 0) {
        if (disRendered == null) {
          disRendered = new HashMap<Integer, DisTextMarker>();
        }
        disRendered.clear();

        regsInFunction.clear();
        addrToRow.clear();
        rowToAddr.clear();
        addrIsBranch.clear();
        isJavaByteCode = false;
        isSparc = false;
        isX86 = false;
        numRows = table_data[fl_table.getNameCol()].length;
        for (int i = 0; i < numRows; i++) {
          Object value = table_data[fl_table.getNameCol()][i];
          int type = src_type[i] < 0 ? (-src_type[i]) : src_type[i];
          if (value instanceof String && (type == AT_DIS || type == AT_DIS_ONLY)) {
            if (AnMemoryManager.getInstance().checkMemoryThreshold()) {
              reachedMemLimit = true;
              break;
            }
            DisTextMarker dm = new DisTextMarker(i, ((String) value));
            disRendered.put(i, dm);
          }
        }
        Set<Integer> rows = disRendered.keySet();
        if (reachedMemLimit) {
          rows = new TreeSet<Integer>();
          for (int i = 0; i < numRows; i++) {
            Object value = table_data[fl_table.getNameCol()][i];
            int type = src_type[i] < 0 ? (-src_type[i]) : src_type[i];
            if (value instanceof String && (type == AT_DIS || type == AT_DIS_ONLY)) {
              rows.add(i);
            }
          }
        }
        if (rows != null) {
          for (final Integer r : rows) {
            if (calleeInfo != null && calleeInfo.get(r) != null) {
              continue; // skip callsite instruction
            }
            DisTextMarker dm = disRendered.get(r);
            if (reachedMemLimit && dm == null) {
              dm = newTextMarker(r);
              dm.renderCode(r, true);
            }
            if (dm.addrPositions != null && !dm.addrPositions.isEmpty()) {
              Set<String> addrs = dm.addrPositions.keySet();
              if (addrs.size() > 1) {
                continue;
              }
              for (final String addr : addrs) {
                Integer addrRow = addrToRow.get(addr);
                addrIsBranch.put(addr, true);
                String targetFunc = addrRow == null ? null : getFunction(addrRow.intValue());
                String curFunc = getFunction(r.intValue());
                if (targetFunc == null
                    || curFunc == null
                    || (!curFunc.equalsIgnoreCase(targetFunc))) {
                  dm.replaceMarkerStyle("jmp_back", "jmp_outside");
                  dm.replaceMarkerStyle("jmp_other", "jmp_outside");
                }
              }
            }
          }
        }

        // render block start addresses
        renderBlocks(0, table_data[fl_table.getNameCol()].length, false);

        disRenderedMap.put(sel_instr, disRendered);
      }
      if (disPaneCreated == null || sel_instr == 0) {
        if (disPaneCreated == null) {
          disPaneCreated = new HashMap<Integer, DisTextPane>();
        }
        disPaneCreated.clear();
        disPaneCreatedMap.put(sel_instr, disPaneCreated);
      }
      if (calleeInfo != null) {
        for (final Integer callsite : calleeInfo.keySet()) {
          DisTextMarker dm = disRendered.get(callsite);
          if (dm != null) {
            dm.isCallSite = true;
          }
        }
      }
      // add new obj to history if it's different from current
      if (fl_table.getNavigationHistoryPool().getHistory().enabled) {
        int row = fl_table.getSelectedRow();
        DisTextMarker dm = disRendered.get(row);
        if (dm == null) {
          dm = newTextMarker(row);
        }
        if (dm != null) {
          String baseAddrStr = getRowToAddr(row, row);
          long fun_id = -1;
          if (functionId != null && row > 0 && row <= functionId.size()) {
            fun_id = functionId.get(row);
          }
          if (fun_id == 0) {
            fun_id = fl_table.getAnParent().window.getSelectedObject().getSelObjV2("FUNCTION");
          }
          SelObjInfo cur_so = fl_table.getNavigationHistoryPool().getHistory().getCurrent();
          if (cur_so == null
              || cur_so.id != fun_id
              || (cur_so.name != null && !cur_so.name.equalsIgnoreCase(baseAddrStr))) {
            SelObjInfo new_so = new SelObjInfo(fun_id, row, baseAddrStr);
            fl_table.getNavigationHistoryPool().getHistory().goToNew(new_so);
          } else {
            fl_table.getNavigationHistoryPool().getHistory().newAdded = false;
          }
        }
      }
    }

    private void renderBlocks(
        final Integer startRow, final Integer endRow, final boolean allocateMemroy) {
      if (allocateMemroy) {
        for (Integer r = startRow; r < endRow; r++) {
          Object value = tableData[fl_table.getNameCol()][r];
          int type = srcTypeData[r] < 0 ? (-srcTypeData[r]) : srcTypeData[r];
          if (!(value instanceof String && (type == AT_DIS || type == AT_DIS_ONLY))) {
            continue;
          }
          if (calleeInfo != null && calleeInfo.get(r) != null) {
            continue; // skip callsite instruction
          }
          DisTextMarker dm = disRendered.get(r);
          DisTextPane dt = disPaneCreated == null ? null : disPaneCreated.get(r);
          if (dm == null) {
            dm = new DisTextMarker(r, (String) value);
            disRendered.put(r, dm);
          }
          if (dt == null) { // only create JTextPanel when it needs to be shown
            if (dm != null) {
              dm.renderCode(r, true);
            }
            if (dm != null && calleeInfo.get(r) != null) {
              dm.isCallSite = true;
            }
          }
          if (dm.addrPositions != null && !dm.addrPositions.isEmpty()) {
            Set<String> addrs = dm.addrPositions.keySet();
            if (addrs.size() > 1) {
              continue;
            }
            for (final String addr : addrs) {
              Integer addrRow = getAddrToRow(addr, r);
              addrIsBranch.put(addr, true);
              String targetFunc = addrRow == null ? null : getFunction(addrRow.intValue());
              String curFunc = getFunction(r.intValue());
              if (targetFunc == null
                  || curFunc == null
                  || (!curFunc.equalsIgnoreCase(targetFunc))) {
                dm.replaceMarkerStyle("jmp_back", "jmp_outside");
                dm.replaceMarkerStyle("jmp_other", "jmp_outside");
              }
            }
          }
        }
      }
      // render block start addresses
      if (!isJavaByteCode) {
        int funcStart = 0;
        int seenCB = 0;
        for (int i = startRow; i < endRow; i++) {
          Object value = tableData[fl_table.getNameCol()][i];
          int type = srcTypeData[i] < 0 ? (-srcTypeData[i]) : srcTypeData[i];
          if (type == AnTable.AT_FUNC) {
            funcStart = 1;
            seenCB = 0;
          }
          if (value instanceof String && (type == AT_DIS || type == AT_DIS_ONLY)) {
            DisTextMarker dm = disRendered.get(i);
            DisTextPane dt = disPaneCreated == null ? null : disPaneCreated.get(i);
            if (dm == null && allocateMemroy) {
              dm = new DisTextMarker(i, (String) value);
              disRendered.put(i, dm);
            }
            if (dt == null && allocateMemroy) { // only create JTextPanel when it needs to be shown
              if (dm != null) {
                dm.renderCode(i, true);
              }
              if (dm != null && calleeInfo.get(i) != null) {
                dm.isCallSite = true;
              }
            }
            if (i > 0) {
              int type_pre = srcTypeData[i - 1] < 0 ? (-srcTypeData[i - 1]) : srcTypeData[i - 1];
              if (type_pre == AnTable.AT_FUNC) {
                funcStart = 1;
                seenCB = 0;
              }
            }
            if (dm != null) {
              //                            if (funcStart == 1
              //                                    && !dm.isBranchTargetMarker
              //                                    && !dm.isNop) {
              //                                funcStart = 0;
              //                                dm.replaceMarkerStyle("address",
              // "block_start_address");
              //                            } else if (((!isSparc && seenCB == 1)
              //                                    || (isSparc && seenCB == 2))
              //                                    && (!dm.isBranchTargetMarker
              //                                    && !dm.isNop)) {
              //                                seenCB = 0;
              //                                dm.replaceMarkerStyle("address",
              // "block_start_address");
              //                            } else {
              String addr = dm.getAddr();
              Boolean isBranch = addrIsBranch.get(addr);
              if (isBranch != null && isBranch == true) {
                dm.replaceMarkerStyle("address", "block_start_address");
              }
              //                            }
              if (isSparc && seenCB == 1) {
                seenCB = 2;
              }
              if (dm.hasCall || dm.hasBranch || dm.isRet) {
                seenCB = 1;
              }
            }
          }
        }
      }
    }

    /*private void renderFunction(final String content, final JComponent cmp, final Integer baseRow) {
        if (baseRow != null) {
            HashMap<String, ArrayList<Integer>> regRows = regsInFunction.get(baseRow);
            if (regRows == null) {
                regRows = new HashMap<String, ArrayList<Integer>>();
                regsInFunction.put(baseRow, regRows);

                for (int i = baseRow + 1; i < tableData[fl_table.getNameCol()].length; i++) {
                    int type = srcTypeData[i] < 0 ? (-srcTypeData[i]) : srcTypeData[i];
                    if (type == AnTable.AT_FUNC || i - baseRow > 100) {
                        break;
                    }
                    Object value = tableData[fl_table.getNameCol()][i];
                    if (value instanceof String && (type == AT_DIS || type == AT_DIS_ONLY)) {
                        DisTextMarker dm = disRendered.get(i);
                        DisTextPane dt = disPaneCreated.get(i);
                        if (dm == null) {
                            dm = new DisTextMarker(i, (String) value);
                            disRendered.put(i, dm);
                        }
                        if (dt == null) { // only create JTextPanel when it needs to be shown
                            if (dm != null) {
                                dm.renderCode(i, true);
                            }
                            if (dm != null && calleeInfo.get(i) != null) {
                                dm.isCallSite = true;
                            }
                            dt = new DisTextPane((String) value, cmp, dm);
                            disPaneCreated.put(i, dt);
                        }
                        if (dm == null) {
                            return;
                        }
                        Set<String> regs = dm.regPositions.keySet();
                        if (regs != null) {
                            for (final String r : regs) {
                                ArrayList<Integer> list = regRows.get(regFullName(r));
                                if (list == null) {
                                    list = new ArrayList<Integer>();
                                    regRows.put(regFullName(r), list);
                                }
                                if (!list.contains(i)) {
                                    list.add(i);
                                }
                            }
                        }
                    }
                }
            }
        }
    }*/

    public DisTextPane getDisTextPane(final String content, final JComponent cmp, final int row) {
      Integer function = getFunctionBaseRowWhenCompare(row);
      if (functionCallerCalleeAdded.get(function) == null) {
        addFunctionByRow(function);
      }
      // renderFunction(content, cmp, function);
      DisTextMarker dm = disRendered.get(row);
      if (dm == null) {
        renderBlocks(
            row - 100 > 0 ? row - 100 : 0,
            row + 100 > tableData[fl_table.getNameCol()].length
                ? tableData[fl_table.getNameCol()].length
                : row + 100,
            true);
      }
      dm = disRendered.get(row);
      DisTextPane dt = disPaneCreated.get(row);
      if (dt == null) { // only create JTextPanel when it needs to be shown
        if (dm != null) {
          dm.renderCode(row, true);
        }
        if (dm != null && calleeInfo.get(row) != null) {
          dm.isCallSite = true;
        }
        dt = new DisTextPane(content, cmp, dm);
        disPaneCreated.put(row, dt);
      }
      final int[] anno_set =
          fl_table.getAnParent().window.getSettings().getSourceDisassemblySetting().get();
      boolean scope_only = false;
      if (anno_set.length > 8) { // To accomodate older configurations that don't have scope...
        scope_only = (anno_set[8] != 0);
      }
      if (scope_only || (function != null && function.equals(previousSelectedFunction))) {
        dt.updateRegSelection(previousSelectedReg);
      }
      HashMap<String, ArrayList<Integer>> regRows = regsInFunction.get(function);
      if (regRows == null) {
        regRows = new HashMap<String, ArrayList<Integer>>();
        regsInFunction.put(function, regRows);
      }
      if (dm != null) {
        Set<String> regs = dm.regPositions.keySet();
        if (regs != null) {
          for (final String r : regs) {
            if (scope_only) {
              for (Map.Entry entry : regsInFunction.entrySet()) {
                HashMap<String, ArrayList<Integer>> regMap =
                    (HashMap<String, ArrayList<Integer>>) entry.getValue();
                ArrayList<Integer> list = regMap.get(regFullName(r));
                if (list == null) {
                  list = new ArrayList<Integer>();
                  regMap.put(regFullName(r), list);
                }
                if (!list.contains(row)) {
                  list.add(row);
                }
              }
            } else {
              ArrayList<Integer> list = regRows.get(regFullName(r));
              if (list == null) {
                list = new ArrayList<Integer>();
                regRows.put(regFullName(r), list);
              }
              if (!list.contains(row)) {
                list.add(row);
              }
            }
          }
        }
      }
      return dt;
    }

    protected final class DisTextPane extends JTextPane {

      private DisTextMarker textMarker = null;
      private String selectedReg = null;

      private UnderlineHighlighter highlighter = new UnderlineHighlighter(AnVariable.OPCODE_COLOR);
      private Highlighter.HighlightPainter painter =
          highlighter.new UnderlineHighlightPainter(AnVariable.OPCODE_COLOR);

      public DisTextPane(final String content, final JComponent cmp, final DisTextMarker tm) {
        textMarker = tm;
        setEditable(false);

        setForeground(cmp.getForeground());
        setBackground(cmp.getBackground());

        setHighlighter(highlighter);

        Style style1 = addStyle("opcode", null);
        StyleConstants.setForeground(style1, AnVariable.OPCODE_COLOR);
        Style style2 = addStyle("oprandreg", null);
        StyleConstants.setForeground(style2, AnVariable.OPRANDREG_COLOR);
        Style style3 = addStyle("oprandvalue", null);
        StyleConstants.setForeground(style3, AnVariable.OPRANDVALUE_COLOR);
        Style style4 = addStyle("address", null);
        // StyleConstants.setBold(style4, true);

        Style style5 = addStyle("selected", null);
        StyleConstants.setBackground(style5, AnVariable.REGSEL_COLOR);
        Style style6 = addStyle("defaultbackground", null);
        StyleConstants.setBackground(style6, cmp.getBackground());

        Style style7 = addStyle("jmp_other", null);
        StyleConstants.setUnderline(style7, true);
        StyleConstants.setForeground(style7, AnVariable.JMPOTHER_COLOR);

        Style style8 = addStyle("jmp_back", null);
        StyleConstants.setUnderline(style8, true);
        StyleConstants.setForeground(style8, AnVariable.JMPBACK_COLOR);

        Style style9 = addStyle("jmp_outside", null);
        StyleConstants.setUnderline(style9, true);
        StyleConstants.setForeground(style9, AnVariable.JMPOUTSIDE_COLOR);

        Style style10 = addStyle("line_no", null);
        // StyleConstants.setBold(style10, true);
        StyleConstants.setUnderline(style10, true);

        Style style11 = addStyle("callsite", null);
        StyleConstants.setUnderline(style11, true);
        StyleConstants.setBold(style11, true);

        Style style12 = addStyle("line_no_bold", null);
        // StyleConstants.setForeground(style12, AnVariable.LINENO_COLOR_BOLD);
        StyleConstants.setBold(style12, true);
        StyleConstants.setUnderline(style12, true);

        Style style13 = addStyle("block_start_address", null);
        StyleConstants.setBold(style13, true);

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
            highlighter.addHighlight(textMarker.opcodeBegin, textMarker.opcodeEnd, painter);
          }
        } catch (BadLocationException e) {
        }

        final MouseMotionListener[] ls = getMouseMotionListeners();
        for (final MouseMotionListener l : ls) {
          removeMouseMotionListener(l);
        }

        DisTextPaneMouseAdapter ma = new DisTextPaneMouseAdapter(cmp, this);
        DisTextPaneMouseMotionAdapter mam = new DisTextPaneMouseMotionAdapter(cmp, this);
        DisTextPaneMouseWheelAdapter maw = new DisTextPaneMouseWheelAdapter(cmp, this);
        SrcTextPaneKeyAdapter ka = new SrcTextPaneKeyAdapter(cmp);
        this.addMouseListener(ma);
        this.addMouseMotionListener(mam);
        this.addMouseWheelListener(maw);
        addKeyListener(ka);

        this.setDragEnabled(false);
        // this.setFont(cmp.getFont());
        this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, cmp.getFont().getSize()));
        this.setBorder(AnVariable.labelBorder);

        this.setForeground(cmp.getForeground());
        this.setBackground(cmp.getBackground());
        this.setOpaque(false);
        setEditable(false);
      }

      public void updateRegSelection(String reg) {
        if (selectedReg == null) {
          selectedReg = reg;
        } else if (regEquals(selectedReg, reg)) {
          return;
        } else {
          // remove previous highlightings
          if (textMarker == null) {
            return;
          }
          ArrayList<Position> list =
              textMarker.regPositions == null
                  ? null
                  : textMarker.regPositions.get(regFullName(selectedReg));
          if (list != null) {
            StyledDocument doc = getStyledDocument();
            for (int i = 0; i < list.size(); i++) {
              int start = list.get(i).start;
              int length = list.get(i).length;
              doc.setCharacterAttributes(start, length, getStyle("oprandreg"), true);
            }
          }
          selectedReg = reg;
        }
        if (textMarker == null) {
          return;
        }
        ArrayList<Position> list =
            textMarker.regPositions == null ? null : textMarker.regPositions.get(regFullName(reg));
        if (list != null) {
          StyledDocument doc = getStyledDocument();
          for (int i = 0; i < list.size(); i++) {
            int start = list.get(i).start;
            int length = list.get(i).length;
            doc.setCharacterAttributes(start, length, getStyle("selected"), true);
          }
        }
      }

      public String getReg(int pos) {
        return textMarker.regMap.get(pos);
      }

      public String getAddr(int pos) {
        return textMarker.addrMap.get(pos);
      }

      public String getLineNo(int pos) {
        return textMarker.lineNoMap.get(pos);
      }
    }

    public final class Position {

      int start;
      int length;

      public Position(int s, int l) {
        start = s;
        length = l;
      }
    }

    protected final class DisTextPaneMouseMotionAdapter extends MouseMotionAdapter {

      private Component parentComponent;
      private JTextPane pane;

      public DisTextPaneMouseMotionAdapter(JComponent cmp, JTextPane p) {
        pane = p;
        parentComponent = cmp;
      }

      public void mouseMoved(MouseEvent e) {}
    }

    protected final class DisTextPaneMouseWheelAdapter implements MouseWheelListener {

      private Component parentComponent;
      private JTextPane pane;

      public DisTextPaneMouseWheelAdapter(JComponent cmp, JTextPane p) {
        pane = p;
        parentComponent = cmp;
      }

      public void mouseWheelMoved(MouseWheelEvent e) {
        propagateToTable(e, true, parentComponent);
      }
    }

    protected final class DisTextPaneMouseAdapter extends MouseAdapter {

      private Component parentComponent;
      private JTextPane pane;

      public DisTextPaneMouseAdapter(JComponent cmp, JTextPane p) {
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

    protected final class DisTextMarker {

      public HashMap<String, ArrayList<Position>> regPositions =
          new HashMap<String, ArrayList<Position>>();
      public HashMap<Integer, String> regMap = new HashMap<Integer, String>();

      public HashMap<String, ArrayList<Position>> addrPositions =
          new HashMap<String, ArrayList<Position>>();
      public HashMap<Integer, String> addrMap = new HashMap<Integer, String>();

      public HashMap<String, ArrayList<Position>> lineNoPositions =
          new HashMap<String, ArrayList<Position>>();
      public HashMap<Integer, String> lineNoMap = new HashMap<Integer, String>();

      public int opcodeBegin = -1;
      public int opcodeEnd = -1;

      public boolean isCallSite = false;
      public boolean hasCall = false;
      public boolean hasBranch = false;

      public boolean isBranchTargetMarker = false;
      public boolean isRet = false;
      public boolean isNop = false;

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
      private int curRow = -1;

      public DisTextMarker(final int row, final String content) {
        text = content;
        curRow = row;
        renderCode(row, false);
      }

      public DisTextMarker(final int row, final String content, final boolean doWork) {
        text = content;
        curRow = row;
        if (doWork) {
          renderCode(row, false);
        }
      }

      public int getRow() {
        return curRow;
      }

      public String getAddr() {
        String[] words = text.split(SPLIT_PATTERN);
        int numWords = 0;
        for (int i = 0; i < words.length; i++) {
          if (words[i].length() == 0) {
          } else {
            numWords++;
            if (numWords == 2) {
              return addrStripZero(words[i]);
            }
          }
        }
        return null;
      }

      public void replaceMarkerStyle(String oldStyle, String newStyle) {
        for (int i = 0; i < marker.size(); i++) {
          if (marker.get(i).style.equalsIgnoreCase(oldStyle)) {
            marker.get(i).style = newStyle;
          }
        }
      }

      public String addrStripZero(final String addr) {
        if (addr == null) {
          return addr;
        }
        int i = 0;
        for (i = 0; i < addr.length(); i++) {
          if (addr.charAt(i) != '0') {
            break;
          }
        }
        if (i == addr.length()) {
          i--;
        }
        String stripped = addr.substring(i);
        if (!isJavaByteCode) {
          return stripped;
        } else {
          try {
            long funcBase =
                getFunctionBaseRowWhenCompare(curRow)
                    * 100; // XXXX ugly assumption that the longest java bytecode instruction is 100
            // bytes
            BigInteger addrValue = new BigInteger(stripped, 16);
            BigInteger fBase = BigInteger.valueOf(funcBase);
            addrValue = fBase.add(addrValue);
            return addrValue.toString(16);
          } catch (NumberFormatException e) {
            return stripped;
          }
        }
      }

      public void renderCode(final int row, final boolean allocateMemory) {
        String[] words = text.split(SPLIT_PATTERN);
        int numWords = 0;
        int curPos = 0;
        int hexPos = -1;
        int hexIdx = -1;
        boolean seenOpcode = false;
        boolean seenCall = false;
        boolean seenBranch = false;
        String curAddr = null;
        for (int i = 0; i < words.length; i++) {
          if (words[i].length() == 0) {
          } else {
            numWords++;
            if (numWords == 1 && allocateMemory) {
              if (calleeInfo != null && calleeInfo.get(row) != null) { // it's a callsite
                marker.add(new Highlighter(curPos, words[i].length(), "line_no_bold"));
              } else {
                marker.add(new Highlighter(curPos, words[i].length(), "line_no"));
              }
              if (!words[i].equals("?")) {
                for (int temp = curPos; temp < curPos + words[i].length(); temp++) {
                  lineNoMap.put(temp, words[i]);
                }
              }
            } else if (numWords == 2) {
              if (!allocateMemory) {
                marker.add(new Highlighter(curPos, words[i].length(), "address"));
              }
              if (!isJavaByteCode) {
                if (words[i].equalsIgnoreCase(
                    "00000000")) { // XXXX this is ugly, need a better solution to detect java byte
                  // code
                  isJavaByteCode = true;
                }
              }
              curAddr = addrStripZero(words[i]);
              if (!words[i].equals("?") && !allocateMemory) {
                addrToRow.put(curAddr, row);
                rowToAddr.put(row, curAddr);
              }
            } else if (numWords == 3) {
              if (words[i].length() == 2
                  && words[i].matches("[0-9a-fA-F][0-9a-fA-F]")
                  && !(curPos + words[i].length() + 1 < text.length()
                      && text.charAt(curPos + words[i].length()) == ','
                      && text.charAt(curPos + words[i].length() + 1) != ' ')) {
                // if it seems like a 2 digit hex number we skip it unless there is no opcode found
                numWords--;
                hexPos = curPos;
                hexIdx = i;
              } else if ((words[i].length() >= 2
                      && words[i].matches("[0-9._a-zA-Z]*")
                      && !words[i].matches("0x[0-9a-fA-F]*"))
                  || (words[i].length() == 1 && words[i].matches("[a-zA-Z]"))) {
                if (!allocateMemory) {
                  marker.add(new Highlighter(curPos, words[i].length(), "opcode"));
                }
                opcodeBegin = (opcodeBegin == -1) ? curPos : opcodeBegin;
                opcodeEnd =
                    (opcodeEnd < (curPos + words[i].length()))
                        ? (curPos + words[i].length())
                        : opcodeEnd;
                seenOpcode = true;
                if (words[i].equalsIgnoreCase("call")) { // call for x86 and sparc
                  seenCall = true;
                  hasCall = true;
                } else if (branchInstrMap.get(words[i].toLowerCase()) != null
                    || (!isSparc && x86BranchInstrMap.get(words[i].toLowerCase()) != null)) {
                  seenBranch = true;
                  hasBranch = true;
                }
                if ((curPos + words[i].length() + 1 < text.length()
                        && text.charAt(curPos + words[i].length()) == ','
                        && text.charAt(curPos + words[i].length() + 1) != ' ')
                    || ((words[i].equalsIgnoreCase("lock") // lock prefix for x86
                            || words[i].equalsIgnoreCase("rep") // rep* prefix of x86
                            || words[i].equalsIgnoreCase("repe")
                            || words[i].equalsIgnoreCase("repz")
                            || words[i].equalsIgnoreCase("repne")
                            || words[i].equalsIgnoreCase("repnz"))
                        && curPos + words[i].length() + 1 < text.length()
                        && text.charAt(curPos + words[i].length()) == ' '
                        && text.charAt(curPos + words[i].length() + 1) != ' ')) {
                  numWords--;
                }
                if (words[i].toLowerCase().contains("ret")) {
                  isRet = true;
                } else if (words[i].equalsIgnoreCase("nop")) {
                  isNop = true;
                }
              } else if (words[i].equalsIgnoreCase("<branch")
                  && words[i + 1].equalsIgnoreCase("target>")) {
                isBranchTargetMarker = true;
              }
            } else if (words[i].charAt(0) == '$') {
              if (!allocateMemory) {
                marker.add(new Highlighter(curPos, words[i].length(), "oprandvalue"));
              }
            } else if (words[i].charAt(0) == '%') {
              if (!allocateMemory) {
                marker.add(new Highlighter(curPos, words[i].length(), "oprandreg"));
              }
              if (allocateMemory) {
                addRegPosition(words[i], curPos, words[i].length());
                for (int temp = curPos; temp < curPos + words[i].length(); temp++) {
                  regMap.put(temp, words[i]);
                }
              } else if (!isSparc && !isX86) {
                regFullName(words[i]); // detect whether it's sparc
              }
            } else if (calleeInfo != null && calleeInfo.get(row) != null) { // it's a callsite
              if (words[i].charAt(0) == '0'
                  && words[i].length() > 2
                  && words[i].charAt(1) == 'x'
                  && ((curPos - 2 >= 0
                          && curPos + words[i].length() + 1 < text.length()
                          && text.charAt(curPos - 2) == '[' // [ addr ] or [ addr, .-offset] for x86
                          && (text.charAt(curPos + words[i].length() + 1) == ']'
                              || text.charAt(curPos + words[i].length() + 1) == ' '))
                      || ((seenCall || seenBranch)
                          && curPos - 1 >= 0
                          && text.charAt(curPos - 1) != '*'))) {
                String style = "callsite";
                String newAddr = words[i].substring(2);
                String realAddr = addrStripZero(newAddr);
                marker.add(new Highlighter(curPos, words[i].length(), style));
                addAddrPosition(realAddr, curPos, words[i].length());
                if (allocateMemory) {
                  for (int temp = curPos; temp < curPos + words[i].length(); temp++) {
                    addrMap.put(temp, realAddr);
                  }
                }
                hasBranch = true;
              }
            } else if (!seenCall
                && words[i].charAt(0) == '0'
                && words[i].length() > 2
                && words[i].charAt(1) == 'x'
                && ((curPos - 2 >= 0
                        && curPos + words[i].length() + 1 < text.length()
                        && text.charAt(curPos - 2) == '[' // [ addr ] or [ addr, .-offset] for x86
                        && (text.charAt(curPos + words[i].length() + 1) == ']'
                            || text.charAt(curPos + words[i].length() + 1) == ' '))
                    || (seenBranch
                        && (curPos - 2 < 0
                            || text.charAt(curPos - 2) != ',' // ingnore midle operand
                            || curPos + words[i].length() >= text.length()
                            || text.charAt(curPos + words[i].length()) != ',')
                        && curPos - 1 >= 0
                        && text.charAt(curPos - 1) != '*'))) {
              String newAddr = words[i].substring(2);
              String realAddr = addrStripZero(newAddr);
              BigInteger realAddrNum = new BigInteger(realAddr, 16);
              BigInteger curAddrNum = new BigInteger(curAddr, 16);
              String style = null;
              if (realAddrNum.compareTo(curAddrNum) > 0) {
                style = "jmp_other";
              } else {
                style = "jmp_back";
              }
              if (!allocateMemory) {
                marker.add(new Highlighter(curPos, words[i].length(), style));
              }
              addAddrPosition(realAddr, curPos, words[i].length());
              if (allocateMemory) {
                for (int temp = curPos; temp < curPos + words[i].length(); temp++) {
                  addrMap.put(temp, realAddr);
                }
              }
            }
          }
          if (i < words.length - 1) {
            curPos += words[i].length() + 1;
          }
        }
        if (!seenOpcode && hexPos != -1) {
          marker.add(new Highlighter(hexPos, words[hexIdx].length(), "opcode"));
          opcodeBegin = (opcodeBegin == -1) ? curPos : opcodeBegin;
          opcodeEnd =
              (opcodeEnd < (curPos + words[hexIdx].length()))
                  ? (curPos + words[hexIdx].length())
                  : opcodeEnd;
          seenOpcode = true;
          seenBranch = false;
          int i = hexIdx;
          if (words[i].equalsIgnoreCase("ba") // branch on SPARC
              || words[i].equalsIgnoreCase("be")
              || words[i].equalsIgnoreCase("b")) {
            seenBranch = true;
            hasBranch = true;
          }
          for (i = hexIdx + 1; i < words.length; i++) {
            if (words[i].length() != 0
                && words[i].charAt(0) == '0'
                && words[i].length() > 2
                && words[i].charAt(1) == 'x'
                && seenBranch) {
              String newAddr = words[i].substring(2);
              String realAddr = addrStripZero(newAddr);
              BigInteger realAddrNum = new BigInteger(realAddr, 16);
              BigInteger curAddrNum = new BigInteger(curAddr, 16);
              String style = null;
              if (realAddrNum.compareTo(curAddrNum) > 0) {
                style = "jmp_other";
              } else {
                style = "jmp_back";
              }
              if (!allocateMemory) {
                marker.add(new Highlighter(curPos, words[i].length(), style));
              }
              addAddrPosition(realAddr, curPos, words[i].length());
              if (allocateMemory) {
                for (int temp = curPos; temp < curPos + words[i].length(); temp++) {
                  addrMap.put(temp, realAddr);
                }
              }
              break;
            }
          }
        }
      }

      private void addRegPosition(String reg, int start, int length) {
        ArrayList<Position> positions = regPositions.get(regFullName(reg));
        if (positions == null) {
          positions = new ArrayList<Position>();
          regPositions.put(regFullName(reg), positions);
        }
        positions.add(new Position(start, length));
      }

      private void addAddrPosition(String realAddr, int start, int length) {
        ArrayList<Position> positions = addrPositions.get(realAddr);
        if (positions == null) {
          positions = new ArrayList<Position>();
          if (!realAddr.equals("?")) {
            addrPositions.put(realAddr, positions);
          }
        }
        positions.add(new Position(start, length));
      }
    }

    private void clearSelectedRegs() {
      final int[] anno_set =
          fl_table.getAnParent().window.getSettings().getSourceDisassemblySetting().get();
      boolean scope_only = false;
      if (anno_set.length > 8) { // To accomodate older configurations that don't have scope...
        scope_only = (anno_set[8] != 0);
      }
      if (scope_only) {
        for (Map.Entry entry : regsInFunction.entrySet()) {
          HashMap<String, ArrayList<Integer>> oldMap =
              (HashMap<String, ArrayList<Integer>>) entry.getValue();
          ArrayList<Integer> oldList = null;
          if (oldMap != null) {
            oldList = oldMap.get(regFullName(previousSelectedReg));
          }
          if (oldList != null) {
            for (final int r : oldList) {
              DisTextPane oldDt = disPaneCreated.get(r);
              if (oldDt != null) {
                oldDt.updateRegSelection(null);
              }
            }
          }
        }
        previousSelectedFunction = -1;
        previousSelectedReg = "";
        return;
      }
      HashMap<String, ArrayList<Integer>> oldMap = regsInFunction.get(previousSelectedFunction);
      ArrayList<Integer> oldList = null;
      if (oldMap != null) {
        oldList = oldMap.get(regFullName(previousSelectedReg));
      }
      if (oldList != null) {
        for (final int r : oldList) {
          DisTextPane oldDt = disPaneCreated.get(r);
          if (oldDt != null) {
            oldDt.updateRegSelection(null);
          }
        }
      }
      previousSelectedFunction = -1;
      previousSelectedReg = "";
    }

    protected void updateSelectedRegs(int row) {
      DisTextPane dt = disPaneCreated.get(row);
      if (dt != null) {
        String reg = dt.getReg(caretLinePosition);
        if (reg != null) {
          final int[] anno_set =
              fl_table.getAnParent().window.getSettings().getSourceDisassemblySetting().get();
          boolean scope_only = false;
          if (anno_set.length > 8) { // To accomodate older configurations that don't have scope...
            scope_only = (anno_set[8] != 0);
          }
          if (scope_only) {
            if (!regEquals(reg, previousSelectedReg)) {
              clearSelectedRegs();
              Integer function = -1;
              for (Map.Entry entry : regsInFunction.entrySet()) {
                HashMap<String, ArrayList<Integer>> regMap =
                    (HashMap<String, ArrayList<Integer>>) entry.getValue();
                ArrayList<Integer> newList = regMap.get(regFullName(reg));
                if (newList != null) {
                  for (final int r : newList) {
                    DisTextPane newDt = disPaneCreated.get(r);
                    if (newDt != null) {
                      newDt.updateRegSelection(reg);
                    }
                  }
                }
                function = (Integer) entry.getKey();
              }
              previousSelectedReg = reg;
              previousSelectedFunction = function;
            }
          } else {
            Integer function = getFunctionBaseRowWhenCompare(row);
            if (function != null
                && (!regEquals(reg, previousSelectedReg)
                    || !function.equals(previousSelectedFunction))) {
              clearSelectedRegs();
              ArrayList<Integer> newList = regsInFunction.get(function).get(regFullName(reg));
              if (newList != null) {
                for (final int r : newList) {
                  DisTextPane newDt = disPaneCreated.get(r);
                  if (newDt != null) {
                    newDt.updateRegSelection(reg);
                  }
                }
              }
              previousSelectedReg = reg;
              previousSelectedFunction = function;
            }
          }
        } else {
          clearSelectedRegs();
        }
      } else {
        clearSelectedRegs();
      }

      fl_table.repaint();
    }

    protected boolean goToAddr(int row) {
      DisTextPane dt = disPaneCreated.get(row);
      if (dt != null) {
        String addr = dt.getAddr(caretLinePosition);
        if (addr == null
            && calleeInfo != null
            && calleeInfo.get(row) != null
            && calleeInfo.get(row).size() > 0
            && (caretLinePosition >= dt.textMarker.opcodeBegin
                && caretLinePosition < dt.textMarker.opcodeEnd)) {
          for (final String key : dt.textMarker.addrPositions.keySet()) {
            addr = key;
          }
        }
        if (addr != null) {
          Integer targetRowObj = getAddrToRow(addr, row);
          if (targetRowObj != null) {
            int targetRow = targetRowObj.intValue();
            // add new obj to history if it's different from current
            fl_table.getNavigationHistoryPool().getHistory().goToDisNew(row);
            fl_table.getNavigationHistoryPool().getHistory().enabled = false;
            fl_table.setSelectedRow(targetRow);
            fl_table.getNavigationHistoryPool().getHistory().enabled = true;
            fl_table.fireAnEvent(new AnEvent(fl_table, AnEvent.EVT_SELECT, targetRow, null));
            // add new obj to history if it's different from current
            fl_table.getNavigationHistoryPool().getHistory().goToDisNew(targetRow);
            fl_table.getAnParent().updateToolBar();
            return true;
          }
        }
      }
      return false;
    }

    protected void goToCallee(int row, MouseEvent e) {
      if (calleeInfo == null || calleeInfo.get(row) == null) {
        return;
      }
      DisTextPane dt = disPaneCreated.get(row);
      if (dt != null) {
        String addr = dt.getAddr(caretLinePosition);
        if (addr != null
            || (caretLinePosition >= dt.textMarker.opcodeBegin
                && caretLinePosition < dt.textMarker.opcodeEnd)) {
          if (calleeInfo.get(row).size() > 1) {
            // pop up context menu
            JPopupMenu popup = fl_table.getMenuListener().initPopup(e);
            if (popup != null) {
              popup.show(e.getComponent(), e.getX(), e.getY());
              boolean pressDown = false;
              for (int i = 0;
                  i < 3;
                  i++) { // XXXX ugly assumption that the "show callee dis" is the 3rd item
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
          } else {
            long funcId = calleeInfo.get(row).get(0).id;
            // add new obj to history if it's different from current
            fl_table.getNavigationHistoryPool().getHistory().goToDisNew(row);
            // change selected object
            fl_table.getAnParent().window.getSelectedObject().setSelObjV2(funcId);
            fl_table.getAnParent().setComputed(false);
            fl_table.getAnParent().computeOnAWorkerThread();
            fl_table.updateAnTable(AnTable.STR_ACTION_SHOW_CALLEE_DISASM);
          }
        }
      }
    }

    protected void goToSrcLine(int row) {
      DisTextPane dt = disPaneCreated.get(row);
      if (dt != null) {
        String ln = dt.getLineNo(caretLinePosition);
        if (ln != null) {
          int targetRow = getRowByLineNo(Integer.parseInt(ln.trim()));
          if (targetRow != -1) {
            // add new obj to history if it's different from current
            fl_table.getNavigationHistoryPool().getHistory().goToDisNew(row);
            fl_table.getNavigationHistoryPool().getHistory().enabled = false;
            fl_table.setSelectedRow(targetRow);
            fl_table.getNavigationHistoryPool().getHistory().enabled = true;
            fl_table.fireAnEvent(new AnEvent(fl_table, AnEvent.EVT_SELECT, targetRow, null));
            // add new obj to history if it's different from current
            fl_table.getNavigationHistoryPool().getHistory().goToDisNew(targetRow);
            fl_table.getAnParent().updateToolBar();
          }
        }
      }
    }

    protected DisTextMarker newTextMarker(int i) {
      DisTextMarker dm = null;
      if (tableData == null
          || fl_table == null
          || srcTypeData == null
          || fl_table.getNameCol() >= tableData.length
          || i >= tableData[fl_table.getNameCol()].length
          || i < 0) {
        return dm;
      }
      Object value = tableData[fl_table.getNameCol()][i];
      int type = srcTypeData[i] < 0 ? (-srcTypeData[i]) : srcTypeData[i];
      if (value instanceof String && (type == AT_DIS || type == AT_DIS_ONLY)) {
        dm = new DisTextMarker(i, ((String) value), false);
      }
      return dm;
    }

    public Integer getAddrToRow(String addr) {
      int base_row = fl_table.getSelectedRow();
      return getAddrToRow(addr, base_row);
    }

    public Integer getAddrToRow(String addr, int base_row) {
      Integer row = addrToRow.get(addr);
      if (row != null || !reachedMemLimit) {
        return row;
      } else {
        // find row
        row = null;

        DisTextMarker dm = disRendered.get(base_row);
        if (dm == null) {
          dm = newTextMarker(base_row);
        }
        String base_addr = dm.getAddr();
        BigInteger baseAddrNum = new BigInteger(base_addr, 16);
        BigInteger curAddrNum = new BigInteger(addr, 16);
        if (baseAddrNum.compareTo(curAddrNum) > 0) {
          for (int i = base_row; i > 0; i--) {
            dm = disRendered.get(i);
            if (dm == null) {
              dm = newTextMarker(i);
            }
            if (dm != null) {
              String cur_addr = dm.getAddr();
              if (cur_addr.equalsIgnoreCase(addr)) {
                return i;
              }
            }
          }
        } else if (baseAddrNum.compareTo(curAddrNum) < 0) {
          for (int i = base_row; i < numRows; i++) {
            dm = disRendered.get(i);
            if (dm == null) {
              dm = newTextMarker(i);
            }
            if (dm != null) {
              String cur_addr = dm.getAddr();
              if (cur_addr.equalsIgnoreCase(addr)) {
                return i;
              }
            }
          }
        } else {
          return base_row;
        }
        if (row == null) {
          if (baseAddrNum.compareTo(curAddrNum) < 0) {
            for (int i = base_row; i > 0; i--) {
              dm = disRendered.get(i);
              if (dm == null) {
                dm = newTextMarker(i);
              }
              if (dm != null) {
                String cur_addr = dm.getAddr();
                if (cur_addr.equalsIgnoreCase(addr)) {
                  return i;
                }
              }
            }
          } else if (baseAddrNum.compareTo(curAddrNum) > 0) {
            for (int i = base_row; i < numRows; i++) {
              dm = disRendered.get(i);
              if (dm == null) {
                dm = newTextMarker(i);
              }
              if (dm != null) {
                String cur_addr = dm.getAddr();
                if (cur_addr.equalsIgnoreCase(addr)) {
                  return i;
                }
              }
            }
          }
        }
        return row;
      }
    }

    public String getRowToAddr(int row) {
      int base_row = fl_table.getSelectedRow();
      return getRowToAddr(row, base_row);
    }

    public String getRowToAddr(int row, int base_row) {
      String addr = rowToAddr.get(row);
      if (addr != null || !reachedMemLimit) {
        return addr;
      } else {
        // find addr
        addr = null;

        DisTextMarker dm = disRendered.get(base_row);
        if (dm == null) {
          dm = newTextMarker(base_row);
        }
        if (base_row > row) {
          for (int i = base_row; i > 0; i--) {
            dm = disRendered.get(i);
            if (dm == null) {
              dm = newTextMarker(i);
            }
            if (dm != null) {
              int cur_row = dm.getRow();
              if (cur_row == row) {
                return dm.getAddr();
              }
            }
          }
        } else if (base_row < row) {
          for (int i = base_row; i < numRows; i++) {
            dm = disRendered.get(i);
            if (dm == null) {
              dm = newTextMarker(i);
            }
            if (dm != null) {
              int cur_row = dm.getRow();
              if (cur_row == row) {
                return dm.getAddr();
              }
            }
          }
        } else {
          return dm == null ? null : dm.getAddr();
        }
        return addr;
      }
    }
  }
}
