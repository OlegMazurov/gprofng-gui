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
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.util.gui.AnSplitPaneInternal;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class AnCompDisp extends FuncListDisp implements ExportSupport {

  protected FuncListDisp oneDispPanel;
  private Component multiDispPanel;
  private boolean initialized, inCompute;
  private CardLayout cardLayout;
  //    private FuncListDisp[] funcDisps;
  private AnTable[] tables;
  private AnHotGap[] hotGps;
  private int[] groupIds;
  private JPanel[] jpanels;
  private Object[] raw_datas, raw_data_ids, src_types, met_labels, marks, marks_inc;
  private AnObject[][] cmp_totals;
  private long[] sel_objs;
  private int[] sel_indexes;
  private int focusedTable;
  private String[] str_errors;
  private final String help_id;

  //    private JPanel cards;
  public AnCompDisp(final AnWindow w, final int type, final String help_id) {
    super(w, type, 0, help_id);
    this.help_id = help_id;
    initialized = inCompute = false;
    cardLayout = null;
    multiDispPanel = null;
  }

  @Override
  protected void initComponents() {}

  @Override
  public void requestFocus() {
    if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
      if (oneDispPanel != null) {
        oneDispPanel.requestFocus();
      }
    } else {
      AnTable table = tables[focusedTable];
      if (table != null) {
        table.requestFocus();
      }
    }
  }

  public int getTableIndex(AnTable table) {
    int index = -1;
    for (AnTable at : tables) {
      index++;
      if (at == table) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String text = "";
    if ((null == tables) || (tables.length < 2)) { // Not comparison
      text = oneDispPanel.exportAsText(limit, format, delimiter); // exports the main table
    } else { // Comparison
      String sortedby = null; // Source and Disassembly are not sorted
      if (null == MaximumValues) {
        // Print error message and stack trace
        System.err.println("Internal Error: MaximumValues is null");
        Exception e = new Exception();
        e.printStackTrace();
        return text; // empty report
      }
      int printLimit = limit != null ? limit : 0;
      // Export tables
      for (int i = 0; i < tables.length; i++) {
        table = tables[i];
        if (i > 0) {
          text += "\n\n\n"; // Separator
        }
        int st = i + 1;
        AnObject[] totals = getTotals(type, st); // IPC call
        if (totals != null) {
          if (MaximumValues == null) {
            MaximumValues = new Object[2][totals.length];
            MaximumValues[1] = totals;
          }
          MaximumValues[0] = totals;
        }
        text += table.printTableHeader(sortedby, MaximumValues);
        text += table.printTableContents(MaximumValues, printLimit);
      }
    }
    return text;
  }

  @Override
  public JPopupMenu getFilterPopup() {
    if (oneDispPanel != null) {
      return oneDispPanel.getFilterPopup();
    }
    return null;
  }

  private static JPanel toolbarPanel =
      null; // Fixup: pass it to SourceDisp instead of being static!!!!

  public static JPanel getToolbarPanelInternal() {
    if (toolbarPanel == null) {
      toolbarPanel = new JPanel();
      toolbarPanel.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
      toolbarPanel.setLayout(new BorderLayout());
    }
    return toolbarPanel;
  }

  @Override
  public JPanel getToolbarPanel() {
    return getToolbarPanelInternal();
  }

  // Update view specific tool bar
  public void updateToolBar() {
    if ((jpanels.length == 1) && oneDispPanel != null) {
      oneDispPanel.updateToolBar();
    } else {
      AnUtility.dispatchOnSwingThread(
          new Runnable() {
            @Override
            public void run() {
              if (backForwardControls == null) {
                backForwardControls = new CompBackForwardControls();
              }
              backForwardControls.updateToolBarStatus();
              AnCompDisp.getToolbarPanelInternal().removeAll();
              AnCompDisp.getToolbarPanelInternal().add(backForwardControls, BorderLayout.CENTER);
              AnCompDisp.getToolbarPanelInternal().repaint();
            }
          });
    }
  }

  protected class CompBackForwardControls extends BackForwardControls {

    public CompBackForwardControls() {
      setOpaque(false);
      this.removeAll();
      setLayout(new GridBagLayout());
      GridBagConstraints gridBagConstraints;
      // Back button
      gridBagConstraints = new GridBagConstraints();
      backButton = new AnIconButton(AnUtility.goBackwardIcon);
      backButton.setMargin(new Insets(2, 4, 2, 4));
      backButton.setToolTipText(
          AnLocale.getString("Back")
              + AnUtility.keyStrokeToStringFormatted(KeyboardShortcuts.backwardActionShortcut));
      backButton.setEnabled(true);
      backButton.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              // Hook it up to back action;
              tables[focusedTable].goBack();
            }
          });
      // Backward
      KeyStroke keyStrokeBackward = KeyboardShortcuts.backwardActionShortcut;
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeBackward, keyStrokeBackward);
      getActionMap()
          .put(
              keyStrokeBackward,
              new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  backButton.doClick();
                }
              });
      add(backButton, gridBagConstraints);
      // Back button
      gridBagConstraints = new GridBagConstraints();
      forwardButton = new AnIconButton(AnUtility.goForwardIcon);
      forwardButton.setMargin(new Insets(2, 4, 2, 4));
      forwardButton.setToolTipText(
          AnLocale.getString("Forward")
              + AnUtility.keyStrokeToStringFormatted(KeyboardShortcuts.forwardActionShortcut));
      forwardButton.setEnabled(true);
      forwardButton.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              // Hook it up to forward action;
              tables[focusedTable].goForward();
            }
          });
      // Forward
      KeyStroke keyStrokeForward = KeyboardShortcuts.forwardActionShortcut;
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeForward, keyStrokeForward);
      getActionMap()
          .put(
              keyStrokeForward,
              new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  forwardButton.doClick();
                }
              });
      add(forwardButton, gridBagConstraints);
    }

    public void updateToolBarStatus() {
      backButton.setEnabled(tables[focusedTable].canGoBack());
      forwardButton.setEnabled(tables[focusedTable].canGoForward());
    }
  }

  private FuncListDisp newFuncListDisp(final int dsp, final String help_id) {
    FuncListDisp fd = null;
    if (AnDisplay.DSP_Source == dsp) {
      fd = new SourceDisp(window, dsp, help_id);
    } else if (AnDisplay.DSP_Disassembly == dsp) {
      fd = new DisasmDisp(window, dsp, help_id);
    } else {
      fd = new FuncListDisp(window, dsp, 0, help_id);
    }
    fd.setMinimumSize(new Dimension(0, 0)); // make the both parts to be 0ed
    return fd;
  }

  private void init() {
    if (cardLayout == null) {
      cardLayout = new CardLayout();
      setLayout(cardLayout);
      oneDispPanel = newFuncListDisp(type, help_id);
      add(oneDispPanel, ONE_DISP_PANEL);
    }
    if (multiDispPanel != null) {
      cardLayout.removeLayoutComponent(multiDispPanel);
    }

    String acName = AnLocale.getString("Source");
    String acDesc = AnLocale.getString("Show source for selected function");
    if ((type == AnDisplay.DSP_Disassembly) || (type == AnDisplay.DSP_DisassemblyV2)) {
      acName = AnLocale.getString("Disassembly");
      acDesc = AnLocale.getString("Show disassembled code for selected function");
    }
    JLabel acLabel = new JLabel(acName, JLabel.RIGHT);

    focusedTable = 0;
    groupIds = window.getGroupIds();
    int sz = groupIds.length;
    if (sz <= 0) {
      // no experiments loaded
      return;
    }
    tables = new AnTable[sz];
    hotGps = new AnHotGap[sz];
    jpanels = new JPanel[sz];
    for (int i = 0; i < sz; i++) {
      tables[i] =
          new AnTable(
              type == AnDisplay.DSP_Source ? AnDisplay.DSP_SourceV2 : AnDisplay.DSP_DisassemblyV2,
              true,
              true,
              false,
              false,
              true,
              true,
              (i == 0 ? true : false),
              acName,
              acDesc,
              acLabel);
      tables[i].setParent(this);
      tables[i].setGroupId(i);
      tables[i].addAnListener(new TableHandler(i));
      jpanels[i] = new JPanel(new BorderLayout());
      hotGps[i] = new AnHotGap(this, tables[i], 47); // FIXUP
      jpanels[i].add(tables[i], BorderLayout.CENTER);
      JPanel buttonPan = new JPanel(new GridBagLayout());
      buttonPan.setBackground(Color.WHITE);
      JButton upButton = new AnIconButton(AnUtility.goUpIcon);
      JButton downButton = new AnIconButton(AnUtility.goDownIcon);

      upButton.addActionListener(new CompDispButtonListener(i, false));
      downButton.addActionListener(new CompDispButtonListener(i, true));
      upButton.setToolTipText(
          AnLocale.getString("Previous Hot Line")
              + AnUtility.keyStrokeToStringFormatted(
                  KeyboardShortcuts.sourcePreviousHotLineActionShortcut));
      downButton.setToolTipText(
          AnLocale.getString("Next Hot Line")
              + AnUtility.keyStrokeToStringFormatted(
                  KeyboardShortcuts.sourceNextHotLineActionShortcut));
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridy = 0;
      buttonPan.add(upButton, gridBagConstraints);
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = GridBagConstraints.NORTH;
      gridBagConstraints.weighty = 1.0;
      buttonPan.add(downButton, gridBagConstraints);
      Dimension dim =
          new Dimension(AnHotGap.HOTGAP_WIDTH, 3 * (tables[i].getFont().getSize() + 3) + 2);
      buttonPan.setPreferredSize(dim);
      JPanel gapPan = new JPanel();
      gapPan.setLayout(new BorderLayout());
      gapPan.add(buttonPan, BorderLayout.NORTH);
      gapPan.add(hotGps[i]);
      gapPan.setBorder(
          BorderFactory.createMatteBorder(0, 1, 0, 0, AnEnvironment.SCROLLBAR_BORDER_COLOR));
      jpanels[i].add(gapPan, BorderLayout.EAST);
    }

    // Previous Hot Line
    KeyStroke keyStrokeUp = KeyboardShortcuts.sourcePreviousHotLineActionShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeUp, keyStrokeUp);
    getActionMap()
        .put(
            keyStrokeUp,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (focusedTable >= 0 && focusedTable < hotGps.length) {
                  hotGps[focusedTable].HG_Prev_Hot_Line();
                }
              }
            });
    // Next Hot Line
    KeyStroke keyStrokeDown = KeyboardShortcuts.sourceNextHotLineActionShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeDown, keyStrokeDown);
    getActionMap()
        .put(
            keyStrokeDown,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (focusedTable >= 0 && focusedTable < hotGps.length) {
                  hotGps[focusedTable].HG_Next_Hot_Line();
                }
              }
            });
    // Next NZ Line
    KeyStroke keyStrokeNextNZ = KeyboardShortcuts.sourceNextNonZeroLineActionShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeNextNZ, keyStrokeNextNZ);
    getActionMap()
        .put(
            keyStrokeNextNZ,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (focusedTable >= 0 && focusedTable < hotGps.length) {
                  hotGps[focusedTable].HG_Next_NZ_Line();
                }
              }
            });
    // Previous NZ Line
    KeyStroke keyStrokePreviousNZ = KeyboardShortcuts.sourcePreviousNonZeroLineActionShortcut;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokePreviousNZ, keyStrokePreviousNZ);
    getActionMap()
        .put(
            keyStrokePreviousNZ,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (focusedTable >= 0 && focusedTable < hotGps.length) {
                  hotGps[focusedTable].HG_Prev_NZ_Line();
                }
              }
            });

    table = tables[0];
    if (jpanels.length == 1) {
      add(jpanels[0], MULTI_DISP_PANEL);
      multiDispPanel = jpanels[0];
    } else if (jpanels.length > 1) {
      JSplitPane sp = null;
      for (int i = jpanels.length - 2; i >= 0; i--) {
        if (sp == null) {
          sp = new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, jpanels[i], jpanels[i + 1]);
        } else {
          sp = new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, jpanels[i], sp);
        }
        sp.setMinimumSize(new Dimension(0, 0));
        sp.setContinuousLayout(true);
        sp.setResizeWeight(0.5);
      }
      add(sp, MULTI_DISP_PANEL);
      multiDispPanel = sp;
    }

    getToolbarPanelInternal().removeAll();

    initialized = true;
  }

  private class CompDispButtonListener implements ActionListener {

    private int compIdx;
    private boolean findNext;

    public CompDispButtonListener(int idx, boolean next) {
      compIdx = idx;
      findNext = next;
    }

    public void actionPerformed(ActionEvent e) {
      focusedTable = compIdx;
      find(null, findNext, true);
      hotGps[compIdx].update();
    }
  }

  protected void addExperiment() {
    initialized = false;
    computed = false;
  }

  @Override
  public synchronized void doCompute() {
    AnUtility.checkIfOnAWTThread(false);
    if (!selected) {
      return;
    }
    if (!initialized) {
      init();
      if (!initialized) {
        return;
      }
    }
    if (inCompute) {
      return;
    }
    inCompute = true;
    //        cards.setPreferredSize(new Dimension(AnVariable.WIN_SIZE));
    //        cards.setPreferredSize(oneDispPanel.getPreferredSize());

    if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
      // XXXX oneDispPanel's computed should have been set through AnCompDisp.setComputed().
      // But it's not. So we set computed here.
      oneDispPanel.setComputed(computed);
      oneDispPanel.setSelected(true);
      //            oneDispPanel.doCompute(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
      oneDispPanel
          .computeOnAWorkerThread(); // 22131013 - Analyzer GUI: Drag-n-drop a table header can
      // cause a deadlock
      cardLayout.show(this, ONE_DISP_PANEL);
      computed = true;
      inCompute = false;
      return;
    }
    long sel_func = window.getSelectedObject().getSelObj(DSP_Functions, 0);
    if (sel_func == 0) {
      // Functions tab is not updated yet.
      // Let's try to update Functions tab
      window
          .getFunctionsView()
          .computeIfNeeded(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
      sel_func = window.getSelectedObject().getSelObj(DSP_Functions, 0);
    }
    long sel_obj = window.getSelectedObject().getSelObj(type, 0);
    //        sel_objs = getComparableObjsV2(sel_obj);
    sel_objs = getComparableObjsV2(sel_func);
    long[] cmpObjs = getComparableObjsV2(sel_func);
    oneDispPanel.setSelected(false);

    raw_datas = new Object[tables.length];
    raw_data_ids = new Object[tables.length];
    met_labels = new Object[tables.length];
    src_types = new Object[tables.length];
    str_errors = new String[tables.length];
    sel_indexes = new int[tables.length];
    marks = new Object[tables.length];
    marks_inc = new Object[tables.length];
    cmp_totals = new AnObject[tables.length][];
    MaximumValues = null;
    if (focusedTable >= tables.length) {
      focusedTable = 0;
    }
    for (int i = 0; i < tables.length; i++) {
      long cmpObj = cmpObjs[i];
      if (cmpObj == 0) {
        str_errors[i] = AnLocale.getString("No comparable object");
        continue;
      }

      int met_type = MetricsSetting.MET_COMMON | COMPARE_BIT | ((i + 1) << GROUP_ID_SHIFT);
      sel_objs[i] = dbeConvertSelObj(cmpObj, type);
      Object[] raw_data_with_ids = getFuncListV2(met_type, cmpObj, type, subtype);
      Object[] raw_marks = getHotMarks(type);
      Object[] raw_marks_inc = getHotMarksInc(type);
      int[][] hot_marks = new int[2][];
      hot_marks[0] = (int[]) raw_marks[0];
      hot_marks[1] = (int[]) raw_marks[1];
      int[][] hot_marks_inc = new int[2][];
      hot_marks_inc[0] = (int[]) raw_marks_inc[0];
      hot_marks_inc[1] = (int[]) raw_marks_inc[1];
      marks[i] = hot_marks;
      marks_inc[i] = hot_marks_inc;
      String errstr = window.getMsg(AnUtility.ERROR_MSG);
      if (errstr != null) {
        str_errors[i] = errstr;
        raw_datas[i] = null;
        marks[i] = null;
        marks_inc[i] = null;
        continue;
      }
      str_errors[i] = null;
      int last_ind = raw_data_with_ids.length - 1;
      Object[] metrics_data = (Object[]) raw_data_with_ids[last_ind];
      met_labels[i] = getSettings().getMetricList(metrics_data);
      long[] raw_data_id = (long[]) raw_data_with_ids[last_ind - 1];
      raw_data_ids[i] = raw_data_id;
      src_types[i] = raw_data_with_ids[last_ind - 2];
      final Object[][] data = get_data((AnMetric[]) met_labels[i], raw_data_with_ids);
      updateMaximumValues(data);
      raw_datas[i] = data;
      cmp_totals[i] = getTotals(type, i + 1); // IPC call
      int sel_ind = window.getSelectedObject().getSelIndex(sel_obj, type, subtype);
      if (sel_ind != -1) {
        sel_indexes[i] = sel_ind;
      } else {
        sel_indexes[i] = getSelIndex(raw_data_id, sel_objs[i]);
      }
    }

    SwingUtilities.invokeLater(
        new Runnable() { // 22131013 - Analyzer GUI: Drag-n-drop a table header can cause a deadlock
          @Override
          public void run() {
            update_tables();
          }
        });

    // Set toolbar and select subviews
    updateToolBar();

    computed = true;

    if (tailAction != null) {
      tailAction.tailFunction();
      tailAction = null;
    }

    inCompute = false;
  }

  private void update_tables() {
    long sel_obj = 0;
    for (int i = 0; i < tables.length; i++) {
      table = tables[i];
      table.setViewport();
      String errstr = str_errors[i];
      if (errstr != null) {
        window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);
        table.showMessage(errstr);
        hotGps[i].update();
        continue;
      }
      Object[][] raw_data = (Object[][]) raw_datas[i];
      int[][] mark = (int[][]) marks[i];
      int[][] mark_inc = (int[][]) marks_inc[i];
      int[] src_type = (int[]) src_types[i];
      if (cmp_totals[i] != null) {
        if (MaximumValues == null) {
          MaximumValues = new Object[2][cmp_totals[i].length];
          MaximumValues[1] = cmp_totals[i];
        }
        MaximumValues[0] = cmp_totals[i];
      }
      AnMetric[] mlist = (AnMetric[]) met_labels[i];
      MetricLabel[] label =
          getSettings().getMetricsSetting().getLabel(raw_data, MaximumValues, mlist, type, table);
      name_col = getSettings().getMetricsSetting().getNameColumnIndex(mlist);
      int sort_ind = getSettings().getMetricsSetting().getSortIndex(mlist);
      long sel = sel_objs[i];
      AnUtility.checkIPCOnWrongThread(false);
      String[] hdrContent = getNames(type, sel); // name column table header contents (?)
      AnUtility.checkIPCOnWrongThread(true);
      if (sel_obj == 0) {
        sel_obj = sel;
      }
      AnUtility.checkIPCOnWrongThread(false);
      table.setData(
          label,
          raw_data,
          hdrContent,
          src_type,
          sel_indexes[i],
          name_col,
          sort_ind,
          mark,
          mark_inc);
      AnUtility.checkIPCOnWrongThread(true);
      table.setSelectedRow(sel_indexes[i]);
      hotGps[i].update();
    }
    cardLayout.show(this, MULTI_DISP_PANEL);
    table = tables[0];
    updateSummary(sel_obj);
  }

  public void goToLine(final AnTable tbl, final int row) {
    for (int i = 0, sz = tables != null ? tables.length : 0; i < sz; i++) {
      if (tbl == tables[i]) {
        if (tables[i].getNavigationHistoryPool() != null) {
          if (type == DSP_Source || type == DSP_SourceV2) {
            tables[i].getNavigationHistoryPool().getHistory().goToSrcNew(row);
          } else if (type == DSP_Disassembly || type == DSP_DisassemblyV2) {
            tables[i].getNavigationHistoryPool().getHistory().goToDisNew(row);
          }
          updateToolBar();
        }
        goToLine(i, row);
      }
    }
  }

  private void goToLine(final int cmp_index, final int row) {
    table = tables[cmp_index];
    long[] raw_data_id = (long[]) raw_data_ids[cmp_index];
    if (raw_data_id == null) {
      return;
    }
    if (row < 0 || row >= raw_data_id.length) {
      return;
    }
    long sel_obj = raw_data_id[row];
    int sz_table = tables != null ? tables.length : 0;
    for (int i = 0; i < sz_table; i++) {
      tables[i].clearSelection();
    }
    tables[cmp_index].setSelectedRow(row);
    int selType = type;
    if (type == AnDisplay.DSP_Source) {
      selType = AnDisplay.DSP_SourceV2;
    } else if (type == AnDisplay.DSP_Disassembly) {
      selType = AnDisplay.DSP_DisassemblyV2;
    }
    window.getSelectedObject().setSelObj(sel_obj, selType, subtype);
    updateSummary(sel_obj);

    AnUtility.checkIPCOnWrongThread(false);
    long[] cmpObjs = getComparableObjsV2(sel_obj);
    AnUtility.checkIPCOnWrongThread(true);
    if (cmpObjs == null) {
      return;
    }
    for (int i = 0; i < sz_table; i++) {
      if (cmpObjs[i] != 0 && cmp_index != i) {
        long[] ids = (long[]) raw_data_ids[i];
        int sel_ind = getSelIndex(ids, cmpObjs[i]);
        if (sel_ind != -1) {
          long curRow =
              ((tables[i].srcRenderer != null) ? tables[i].srcRenderer : tables[i].disRenderer)
                  .getFunctionBaseRowWhenCompare(tables[i].getSelectedRow());
          long tgtRow =
              ((tables[i].srcRenderer != null) ? tables[i].srcRenderer : tables[i].disRenderer)
                  .getFunctionBaseRowWhenCompare(sel_ind);
          if (tgtRow != curRow) {
            tables[i].setSelectedRow(sel_ind);
          }
        }
      }
    }
  }

  private int getSelIndex(long[] raw_data_id, long sel_obj) {
    if (raw_data_id == null || sel_obj == 0) {
      return -1;
    }
    for (int i = 0; i < raw_data_id.length; i++) {
      if (raw_data_id[i] == sel_obj) {
        return i;
      }
    }
    return -1;
  }

  private void updateMaximumValues(final Object[][] data) {
    int nc = (data == null) ? 0 : data.length;
    // Updates MaximumValues (Total and Max values)
    // Note: Total is not used, so only Maximum is updated.
    if (MaximumValues == null) {
      MaximumValues = new Object[2][nc];
      AnAddress aa = new AnAddress(0);
      AnDouble ad = new AnDouble(0.0);
      AnInteger ai = new AnInteger(0);
      AnLong al = new AnLong(0);
      AnString as = new AnString("");
      for (int i = 0; i < nc; i++) {
        Object obj, arr = data[i];
        if (arr instanceof AnDouble[]) {
          obj = ad;
        } else if (arr instanceof AnInteger[]) {
          obj = ai;
        } else if (arr instanceof AnLong[]) {
          obj = al;
        } else if (arr instanceof AnAddress[]) {
          obj = aa;
        } else {
          obj = as;
        }
        MaximumValues[0][i] = obj;
        MaximumValues[1][i] = obj;
      }
    }
    for (int i = 0; i < nc; i++) {
      Object[] arr = data[i];
      Object obj = MaximumValues[1][i];
      int max_len = obj.toString().length();
      for (int k = 0; k < arr.length; k++) {
        int len = arr[k].toString().length();
        if (max_len < len) {
          max_len = len;
          obj = arr[k];
        }
      }
      MaximumValues[0][i] = obj;
      MaximumValues[1][i] = obj;
    }
  }

  // Listener for updating table
  private final class TableHandler implements AnListener {

    private int cmp_index;

    TableHandler(int ind) {
      super();
      cmp_index = ind;
    }

    public void valueChanged(final AnEvent event) {
      switch (event.getType()) {
        case AnEvent.EVT_SELECT:
          {
            table = tables[cmp_index];
            int row = event.getValue();
            focusedTable = cmp_index;
            boolean[] enabled = new boolean[tables.length];
            for (int i = 0; i < tables.length; i++) {
              enabled[i] = tables[i].getNavigationHistoryPool().getHistory().enabled;
              tables[i].getNavigationHistoryPool().getHistory().enabled = false;
            }
            int[] sel_rows = table.getSelectedRows();
            if (sel_rows.length > 1) { // multi-selection
              // Update Summary
              int len = sel_rows.length;
              long[] raw_data_id = (long[]) raw_data_ids[cmp_index];
              long[] multi_sel_objs = new long[len];
              for (int i = 0; i < len; i++) {
                multi_sel_objs[i] = raw_data_id[sel_rows[i]];
              }
              window.getSelectionManager().updateSelection(multi_sel_objs, type, subtype, 2);
              break;
            }
            goToLine(cmp_index, row);
            for (int i = 0; i < tables.length; i++) {
              tables[i].getNavigationHistoryPool().getHistory().enabled = enabled[i];
            }
            break;
          }
        case AnEvent.EVT_SORT: // Sorting
          break;
        case AnEvent.EVT_COPY_ALL: // Copy all lines
          copyAll();
          break;
        case AnEvent.EVT_COPY_SEL: // Copy selected lines
          copySelected();
          break;
        case AnEvent.EVT_SWITCH:
          { // Column switching
            table = tables[cmp_index];
            if (table != null) {
              table.columnsSaved = false;
            }
            int from = event.getValue();
            int to = ((Integer) event.getAux()).intValue();
            getSettings().getMetricsSetting().setMetricOrderByDType(this, from, to, type);
            break;
          }
      }
    }
  }

  protected void filterChanged() {
    //        System.out.println("AnCompDisp:filterChanged: " + this);
    if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
      if (oneDispPanel == null) {
        return;
      }
      if (oneDispPanel
          instanceof
          SourceDisp) { // FIXUP: REARCH: this is a mess. Just following previous code logic!
        ((SourceDisp) oneDispPanel).filterChanged();
      }
    }
  }

  public void clearHistory() {
    if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
      if (oneDispPanel == null) {
        return;
      }
      oneDispPanel.clearHistory();
    }
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  @Override
  public int find(final String str, final boolean next, boolean caseSensitive) {
    if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
      return oneDispPanel.find(str, next, caseSensitive);
    }
    AnTable tbl = tables[focusedTable];
    if (tbl != null) {
      int find_row = table.findAfter(str, next, caseSensitive);
      if (find_row == -1) {
        find_row = table.findBefore(str, next, caseSensitive);
      }
      if (find_row != -1) {
        goToLine(tbl, find_row);
        return find_row;
      }
    }
    return -1;
  }

  private long[] getComparableObjsV2(long sel_obj) {
    synchronized (IPC.lock) {
      window.IPC().send("getComparableObjsV2");
      window.IPC().send(0);
      window.IPC().send(sel_obj);
      window.IPC().send(type);
      return (long[]) window.IPC().recvObject();
    }
  }

  protected Object[] getFuncListV2(
      final int met_type, final long sel_func, final int type, final int subtype) {
    synchronized (IPC.lock) {
      window.IPC().send("getFuncListV2");
      window.IPC().send(0);
      window.IPC().send(met_type);
      window.IPC().send(sel_func);
      window.IPC().send(type);
      window.IPC().send(subtype);
      return (Object[]) window.IPC().recvObject();
    }
  }

  protected long dbeConvertSelObj(final long obj, final int type) {
    synchronized (IPC.lock) {
      window.IPC().send("dbeConvertSelObj");
      window.IPC().send(obj);
      window.IPC().send(type);
      return window.IPC().recvLong();
    }
  }

  protected void updateSummary(final long sel_obj) {
    //        final Object[] data;
    //        long[] objs = new long[1];
    //        objs[0] = sel_obj;
    //        synchronized (IPC.lock) {
    //            window.IPC().send("getSummaryV2");
    //            window.IPC().send(window.getWindowID());
    //            window.IPC().send(objs);
    //            window.IPC().send(type);
    //            window.IPC().send(subtype);
    //            data = (Object[]) window.IPC().recvObject();
    //        }
    //        AnUtility.dispatchOnSwingThread(new Runnable() {
    //            @Override
    //            public void run() {
    //                window.getSummaryPanel().updateSummary(data, type);
    //            }
    //        });

    window.getSelectionManager().updateSelection(sel_obj, type, subtype, 2);
  }

  @Override
  public List<ExportFormat> getSupportedExportFormats() {
    List<ExportFormat> formats = new ArrayList<ExportFormat>();
    formats.add(ExportFormat.TEXT);
    formats.add(ExportFormat.JPG);
    return formats;
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
    list.add(window.getHeapCallStackSubview());
    if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
      if (type == AnDisplay.DSP_Source) {
        list.add(window.getCalledByCallsSrcSubview());
      } else if (type == AnDisplay.DSP_Disassembly) {
        list.add(window.getCalledByCallsDisSubview());
      }
    }
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    return list;
  }

  private final String MULTI_DISP_PANEL = "multiDispPanel";
  private final String ONE_DISP_PANEL = "oneDispPanel";
  private final int COMPARE_BIT = 1 << 8;
  private final int GROUP_ID_SHIFT = 16;
}
