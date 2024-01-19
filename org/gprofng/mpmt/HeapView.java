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

import static org.gprofng.mpmt.AnDisplay.DSP_Heap;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;

import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.statecolors.StackView;
import org.gprofng.mpmt.statecolors.StackViewState;
import org.gprofng.mpmt.statecolors.StateColorMap;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnSplitPaneInternal;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class HeapView extends FuncListDisp
    implements AnListener,
        MouseListener,
        AdjustmentListener,
        ListSelectionListener,
        AnChangeListener {

  private boolean inCompute = false;
  //    protected AnTable table; // Don't use your own local copy. Use the 'table' from
  // FuncListDisp.

  private JScrollPane errorPane = null;
  private Object[][] tableDataCalStk;
  private long[] dataCalStkIds = null;
  private JPanel statPanel;
  private AnJScrollPane statScrollPane;

  private String[][] statisticsData;

  private JComponent eventPane;
  private JComponent stackViewPane;
  private StackView stack_view; // Right-hand Tab's Event Stack Display

  private final StateColorMap clmap;
  private int stackRowId;

  private final int HEAPSIZE = 0;
  private final int ALLOC = 1;
  private final int LEAK = 2;

  private boolean updateIfNotSort;

  private StackView.StackViewPopupListener stackViewPopupListener;

  /** Keeps last warning to avoid showing the same warning message many times */
  // Constructor
  public HeapView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Heap, 0, AnVariable.HELP_TabsHeap);
    clmap = window.getColorChooser().getColorMap();
    clmap.addAnListener(this);
    initTab();

    stackViewPopupListener = stack_view.new StackViewPopupListener(window);

    setAccessibility(AnLocale.getString("Heap"));
    AnEventManager.getInstance().addListener(this);

    updateIfNotSort = true;
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
  public JPanel getToolbarPanel() {
    return null;
  }

  private void initTab() {

    // Event Pane
    eventPane = new FocusPanel();
    eventPane.setLayout(new BorderLayout());
    TitledBorder titledBorder = new TitledBorder(AnLocale.getString("Heap Activity"));
    titledBorder.setBorder(new LineBorder(eventPane.getBackground()));

    // Event Pane: Call Stack
    stack_view = new StackView(clmap);
    stack_view.addMouseListener(this);
    stack_view.addListSelectionListener(this);

    stackViewPane = new JPanel();
    stackViewPane.setBackground(Color.WHITE);
    stackViewPane.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(2, 2, 2, 2);
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    stackViewPane.add(stack_view, gridBagConstraints);
    stackViewPane.setVisible(false);
    stackViewPane.revalidate();
    stackViewPane.repaint();

    eventPane.add(stackViewPane, BorderLayout.CENTER);
    eventPane.revalidate();
  }

  class FocusPanel extends JPanel {
    @Override
    public void requestFocus() {
      if (stack_view != null) {
        stack_view.requestFocus();
      }
    }
  }

  @Override
  public JPopupMenu getFilterPopup() {
    return table.initPopup(true);
  }

  // tab to populate right-side info area
  public JComponent getStackInfoTabComp() {
    return eventPane;
  }

  // name for tab in right-side info area
  public String getStackInfoTabName() {
    return AnLocale.getString("Call Stack - Heap");
  }

  public JComponent getStackViewPane() {
    return stackViewPane;
  }

  @Override
  public void setSelected(final boolean set) {

    super.setSelected(set);

    // Show right pane if we get selected
    // if (set) {
    //    window.showRightPane();
    // }
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {

    setLayout(new BorderLayout());

    String acName = null;
    String acDesc = null;
    JLabel acLabel = null;

    acName = AnLocale.getString("HeapActivity");
    acDesc = AnLocale.getString("Show heap activity for selected application");
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

    statPanel = new JPanel();
    statPanel.setLayout(new GridBagLayout());
    statPanel.setBackground(Color.white);

    statScrollPane = new AnJScrollPane(statPanel);
    statScrollPane.setPreferredSize(new Dimension(100, 100));

    JSplitPane sp = new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, table, statScrollPane);
    sp.setResizeWeight(.75);

    add(sp, BorderLayout.CENTER);
  }

  // Compute & update function list table
  @Override
  public void doCompute() {
    AnUtility.checkIfOnAWTThread(false);
    MetricLabel[] label;
    final String pwarnstr;

    // Not selected
    if (!selected) {
      return;
    }

    // XXXX show error when compare is on, needs simplified,
    // but the situation is different for different views
    if (window.getSettings().getCompareModeSetting().comparingExperiments()) {
      JLabel error_label = new JLabel();
      error_label.setHorizontalAlignment(JLabel.CENTER);
      error_label.getAccessibleContext().setAccessibleName(AnLocale.getString("Error message"));
      error_label
          .getAccessibleContext()
          .setAccessibleDescription(AnLocale.getString("Error message"));
      JViewport error_view = new JViewport();
      error_view.setView(error_label);
      String msg = AnLocale.getString("Not available when comparing experiments");
      error_label.setText(msg);
      if (errorPane == null) {
        errorPane = new AnJScrollPane();
        add(errorPane, BorderLayout.CENTER);
      }
      errorPane.setViewportView(error_view);
      errorPane.setVisible(true);
      statScrollPane.setVisible(false);
      return;
    }

    if (errorPane != null) {
      errorPane.setVisible(false);
    }

    statScrollPane.setVisible(true);

    if (inCompute) {
      return;
    } else {
      inCompute = true;
    }

    int typeForPresentation = type;

    if (!computed) { // need re-compute
      reset();
      tableDataCalStk = null;
      dataCalStkIds = null;
      stack_view.reset();

      table.setViewport();
      final AnMetric[] mlist = getSettings().getMetricsSetting().getMetricListByDType(type);
      Object[] raw_data_with_ids =
          window.getTableDataV2("MET_HEAP", "ALL", "HEAPCALLSTACK", "" + subtype, null);
      tableDataCalStk = localProcessData(mlist, raw_data_with_ids);
      dataCalStkIds = current_ids;
      if (updateIfNotSort) {
        statisticsData = getHeapStatistics();
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              // @Override
              @Override
              public void run() {
                statPanel.removeAll();
                displayStatisticsData(statisticsData);
              }
            });
      }
      updateIfNotSort = true;

      names = getNames(type, 0); // IPC call, don't do on AWT

      AnUtility.dispatchOnSwingThread(
          new Runnable() {
            // @Override
            @Override
            public void run() {
              MetricLabel[] label =
                  getSettings().getMetricsSetting().getLabel(tableDataCalStk, null, type, table);
              name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
              int sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
              table.setData(label, tableDataCalStk, names, null, 0, name_col, sort_ind);
              table.setSelectedRow(stackRowId);
              table.showSelectedRow();
              window.showSubview(window.getHeapCallStackSubview(), true);
              if (table.getTable().getColumnCount() == 1) {
                table.showMessage(new NoMetricsSelectedPanel(type));
                inCompute = false;
                return;
              }
            }
          });
    }
    //        else if (!isUpdated()) {
    //            Object[] gtm = getTotalMax();
    //            label = getSettings().getMetricsSetting().getLabel(table.getTableData(), gtm,
    // typeForPresentation, table);
    //
    //            if (label != null) {
    //                table.setLabel(label);
    //            }
    //        }
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          // @Override
          @Override
          public void run() {
            if (stackRowId >= dataCalStkIds.length) {
              stackRowId = dataCalStkIds.length - 1;
            }
            long id = dataCalStkIds[stackRowId];
            int[] rows = table.getSelectedRows();
            if (rows != null) {
              long[] ids = getSelectedIds(rows, DSP_Heap);
              window.getSelectionManager().updateSelection(ids, DSP_Heap, subtype, 1);
            }
            updateStackView(id);
          }
        });

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

  public int getDisplayMode() {
    return DSP_Heap;
  }

  public long[] getSelectedIds(int selectedIndices[], int displayMode) { // FIXUP: REARCH
    long[] ids = null;

    if (selectedIndices.length >= 1) {
      ids = new long[selectedIndices.length];
      for (int i = 0; i < selectedIndices.length; i++) {
        ids[i] = dataCalStkIds[selectedIndices[i]];
      }
    }

    return ids;
  }

  private String exportStatisticsData(final String[][] raw_data) {
    if (raw_data == null) {
      return null;
    }

    StringBuilder strBuild = new StringBuilder();
    strBuild.append("\n");
    if (!AnWindow.getInstance().getFilters().anyFilters()) {
      strBuild.append(AnLocale.getString("Heap Data Statistics For <Total>"));
      strBuild.append("\n\n");
    } else {
      strBuild.append(AnLocale.getString("Heap Data Statistics For <Total> (filters on)"));
      strBuild.append("\n\n");
    }

    if (raw_data[HEAPSIZE].length > 1) {
      strBuild.append(raw_data[HEAPSIZE][0]);
      strBuild.append("\n");
      for (int i = 1; i < raw_data[HEAPSIZE].length; i++) {
        strBuild.append(raw_data[HEAPSIZE][i]);
        strBuild.append("\t\t\t");
        i++;
        strBuild.append(raw_data[HEAPSIZE][i]);
        strBuild.append("\n");
      }
      strBuild.append("\n");
    }

    if (raw_data[ALLOC].length > 1) {
      strBuild.append(raw_data[ALLOC][0]);
      strBuild.append("\n");
      strBuild.append(AnLocale.getString("Allocation Size Range"));
      strBuild.append("\t\t");
      strBuild.append(AnLocale.getString("Allocations"));
      strBuild.append("\n");

      for (int i = 1; i < raw_data[ALLOC].length; i++) {
        if (raw_data[ALLOC][i].trim().indexOf('-') > 0) {
          strBuild.append("  ");
        }
        strBuild.append(raw_data[ALLOC][i]);
        if (raw_data[ALLOC][i].trim().indexOf('-') > 0) {
          strBuild.append("\t\t\t");
        } else {
          strBuild.append("\t");
        }
        i++;
        strBuild.append(raw_data[ALLOC][i]);
        strBuild.append("\n");
      }
      strBuild.append("\n");
    }

    if (raw_data[LEAK].length > 1) {
      strBuild.append(raw_data[LEAK][0]);
      strBuild.append("\n");
      strBuild.append(AnLocale.getString("Leak Size Range"));
      strBuild.append("\t\t\t");
      strBuild.append(AnLocale.getString("Leaks"));
      strBuild.append("\n");

      for (int i = 1; i < raw_data[LEAK].length; i++) {
        if (raw_data[LEAK][i].trim().indexOf('-') > 0) {
          strBuild.append("  ");
        }
        strBuild.append(raw_data[LEAK][i]);
        if (raw_data[LEAK][i].trim().indexOf('-') > 0) {
          strBuild.append("\t\t\t");
        } else {
          strBuild.append("\t\t");
        }
        i++;
        strBuild.append(raw_data[LEAK][i]);
        strBuild.append("\n");
      }
      strBuild.append("\n");
    }

    return strBuild.toString();
  }

  private void displayStatisticsData(final String[][] raw_data) {
    if (raw_data == null) {
      return;
    }

    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    Color color = new Color(240, 240, 240);

    if (!AnWindow.getInstance().getFilters().anyFilters()) {
      JLabel label = new JLabel(AnLocale.getString("Heap Data Statistics For <Total>"));
      label.setFont(table.getFont().deriveFont(Font.BOLD));
      label.setOpaque(true);
      label.setBackground(color);
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.insets.left = 0;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(label, gridBagConstraints);
    } else {
      JLabel label =
          new JLabel(AnLocale.getString("Heap Data Statistics For <Total> (filters on)"));
      label.setFont(table.getFont().deriveFont(Font.BOLD));
      label.setOpaque(true);
      label.setBackground(color);
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.insets.left = 0;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(label, gridBagConstraints);
    }
    gridBagConstraints.insets.bottom = 0;

    if (raw_data[HEAPSIZE].length > 1) {
      JPanel heapSizePanel = new JPanel();
      heapSizePanel.setLayout(new GridBagLayout());
      heapSizePanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsHeapSize = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[HEAPSIZE][0]);
      label1.setFont(table.getFont().deriveFont(Font.BOLD));
      label1.setOpaque(true);
      label1.setBackground(color);
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.gridwidth = 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.insets.bottom = 0;
      gridBagConstraints.insets.top = 0;
      gridBagConstraints.insets.left = 0;
      statPanel.add(label1, gridBagConstraints);

      gridBagConstraintsHeapSize.gridwidth = 1;

      for (int i = 1; i < raw_data[HEAPSIZE].length; i++) {
        JLabel label2 = new JLabel(raw_data[HEAPSIZE][i]);
        label2.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsHeapSize.weightx = 1;
        gridBagConstraintsHeapSize.weighty = 0;
        gridBagConstraintsHeapSize.gridx = 0;
        gridBagConstraintsHeapSize.gridy = gridBagConstraintsHeapSize.gridy + 1;
        gridBagConstraintsHeapSize.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsHeapSize.fill = GridBagConstraints.NONE;
        gridBagConstraintsHeapSize.insets.left = 0;
        heapSizePanel.add(label2, gridBagConstraintsHeapSize);

        i++;
        JLabel label3 = new JLabel(raw_data[HEAPSIZE][i]);
        label3.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsHeapSize.weightx = 1;
        gridBagConstraintsHeapSize.weighty = 0;
        gridBagConstraintsHeapSize.gridx = gridBagConstraintsHeapSize.gridx + 1;
        gridBagConstraintsHeapSize.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsHeapSize.fill = GridBagConstraints.NONE;
        gridBagConstraintsHeapSize.insets.left = 170;
        heapSizePanel.add(label3, gridBagConstraintsHeapSize);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(heapSizePanel, gridBagConstraints);
    }

    if (raw_data[ALLOC].length > 1) {
      JPanel allocPanel = new JPanel();
      allocPanel.setLayout(new GridBagLayout());
      allocPanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsAlloc = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[ALLOC][0]);
      label1.setFont(table.getFont().deriveFont(Font.BOLD));
      label1.setOpaque(true);
      label1.setBackground(color);
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.gridwidth = 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.insets.bottom = 0;
      gridBagConstraints.insets.top = 0;
      gridBagConstraints.insets.left = 0;
      statPanel.add(label1, gridBagConstraints);

      gridBagConstraintsAlloc.gridwidth = 1;

      JLabel label2 = new JLabel(AnLocale.getString("Allocation Size Range"));
      label2.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsAlloc.weightx = 1;
      gridBagConstraintsAlloc.weighty = 0;
      gridBagConstraintsAlloc.gridx = 0;
      gridBagConstraintsAlloc.gridy = 1;
      gridBagConstraintsAlloc.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsAlloc.fill = GridBagConstraints.NONE;
      gridBagConstraintsAlloc.insets.bottom = 1;
      gridBagConstraintsAlloc.insets.top = 0;
      gridBagConstraintsAlloc.insets.left = 0;
      allocPanel.add(label2, gridBagConstraintsAlloc);

      JLabel label3 = new JLabel(AnLocale.getString("Allocations"));
      label3.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsAlloc.weightx = 1;
      gridBagConstraintsAlloc.weighty = 0;
      gridBagConstraintsAlloc.gridx = gridBagConstraintsAlloc.gridx + 1;
      gridBagConstraintsAlloc.gridy = 1;
      gridBagConstraintsAlloc.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsAlloc.fill = GridBagConstraints.NONE;
      gridBagConstraintsAlloc.insets.bottom = 1;
      gridBagConstraintsAlloc.insets.top = 0;
      gridBagConstraintsAlloc.insets.left = 120;
      allocPanel.add(label3, gridBagConstraintsAlloc);

      for (int i = 1; i < raw_data[ALLOC].length; i++) {
        JLabel label4 = new JLabel(raw_data[ALLOC][i]);
        label4.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsAlloc.weightx = 1;
        gridBagConstraintsAlloc.weighty = 0;
        gridBagConstraintsAlloc.gridx = 0;
        gridBagConstraintsAlloc.gridy = gridBagConstraintsAlloc.gridy + 1;
        gridBagConstraintsAlloc.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsAlloc.fill = GridBagConstraints.NONE;
        gridBagConstraintsAlloc.insets.bottom = 0;
        gridBagConstraintsAlloc.insets.top = 0;

        if (raw_data[ALLOC][i].trim().indexOf('-') > 0) {
          gridBagConstraintsAlloc.insets.left = 5;
        } else {
          gridBagConstraintsAlloc.insets.left = 0;
        }
        allocPanel.add(label4, gridBagConstraintsAlloc);

        i++;
        JLabel label5 = new JLabel(raw_data[ALLOC][i]);
        label5.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsAlloc.weightx = 1;
        gridBagConstraintsAlloc.weighty = 0;
        gridBagConstraintsAlloc.gridx = gridBagConstraintsAlloc.gridx + 1;
        gridBagConstraintsAlloc.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsAlloc.fill = GridBagConstraints.NONE;
        gridBagConstraintsAlloc.insets.left = 120;
        gridBagConstraintsAlloc.insets.bottom = 0;
        allocPanel.add(label5, gridBagConstraintsAlloc);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(allocPanel, gridBagConstraints);
    }

    if (raw_data[LEAK].length > 1) {
      JPanel leakPanel = new JPanel();
      leakPanel.setLayout(new GridBagLayout());
      leakPanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsLeak = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[LEAK][0]);
      label1.setFont(table.getFont().deriveFont(Font.BOLD));
      label1.setOpaque(true);
      label1.setBackground(color);
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.gridwidth = 1;
      gridBagConstraints.insets.bottom = 0;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      statPanel.add(label1, gridBagConstraints);

      gridBagConstraintsLeak.gridwidth = 1;

      JLabel label2 = new JLabel(AnLocale.getString("Leak Size Range"));
      label2.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsLeak.weightx = 1;
      gridBagConstraintsLeak.weighty = 0;
      gridBagConstraintsLeak.gridx = 0;
      gridBagConstraintsLeak.gridy = 1;
      gridBagConstraintsLeak.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsLeak.fill = GridBagConstraints.NONE;
      gridBagConstraintsLeak.insets.bottom = 1;
      leakPanel.add(label2, gridBagConstraintsLeak);

      JLabel label3 = new JLabel(AnLocale.getString("Leaks"));
      label3.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsLeak.weightx = 1;
      gridBagConstraintsLeak.weighty = 0;
      gridBagConstraintsLeak.gridx = gridBagConstraintsLeak.gridx + 1;
      gridBagConstraintsLeak.gridy = 1;
      gridBagConstraintsLeak.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsLeak.fill = GridBagConstraints.NONE;
      gridBagConstraintsLeak.insets.bottom = 1;
      gridBagConstraintsLeak.insets.left = 135;
      leakPanel.add(label3, gridBagConstraintsLeak);

      for (int i = 1; i < raw_data[LEAK].length; i++) {
        JLabel label4 = new JLabel(raw_data[LEAK][i]);
        label4.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsLeak.weightx = 1;
        gridBagConstraintsLeak.weighty = 0;
        gridBagConstraintsLeak.gridx = 0;
        gridBagConstraintsLeak.gridy = gridBagConstraintsLeak.gridy + 1;
        gridBagConstraintsLeak.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsLeak.fill = GridBagConstraints.NONE;
        gridBagConstraintsLeak.insets.bottom = 0;
        gridBagConstraintsLeak.insets.top = 0;

        if (raw_data[LEAK][i].trim().indexOf('-') > 0) {
          gridBagConstraintsLeak.insets.left = 5;
        } else {
          gridBagConstraintsLeak.insets.left = 0;
        }
        leakPanel.add(label4, gridBagConstraintsLeak);

        i++;
        JLabel label5 = new JLabel(raw_data[LEAK][i]);
        label5.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsLeak.weightx = 1;
        gridBagConstraintsLeak.weighty = 0;
        gridBagConstraintsLeak.gridx = gridBagConstraintsLeak.gridx + 1;
        gridBagConstraintsLeak.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsLeak.fill = GridBagConstraints.NONE;
        gridBagConstraintsLeak.insets.left = 135;
        gridBagConstraintsLeak.insets.bottom = 0;
        leakPanel.add(label5, gridBagConstraintsLeak);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(leakPanel, gridBagConstraints);
    }

    // The last component. It will push everything up.
    JLabel label = new JLabel("");
    gridBagConstraints.weightx = 1;
    gridBagConstraints.weighty = 1;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    statPanel.add(label, gridBagConstraints);

    statPanel.invalidate();
    statPanel.validate();
    statPanel.repaint();
  }

  public void updateStackView(long stackId) {
    if (stackId == 0 /*null*/) {
      stack_view.reset(); // update stack in right-hand tab
      return; // Request for details about uninstrumented code is not supported yet.
    }
    AnUtility.checkIPCOnWrongThread(false);
    long[] pcs = window.getStackPCs(stackId); // IPC!
    String[] pcnames = window.getStackNames(stackId); // IPC!
    long[] funcs = window.getStackFunctions(stackId); // IPC!
    String[] fnames = window.getFuncNames(funcs); // IPC!
    AnUtility.checkIPCOnWrongThread(true);
    Vector<StackState> state_vec = new Vector<StackState>();

    int i;
    for (i = 0; i < funcs.length; i++) {
      StackViewState svstate = new StackViewState(fnames[i], pcnames[i], funcs[i], pcs[i]);
      state_vec.add(svstate);
    }

    final int selected_frame = funcs.length - 1;
    clmap.addStates(state_vec);
    stack_view.setStates(state_vec, selected_frame); // update stack in right-hand tab
    stack_view.setStackId(stackId);
    stackViewPane.setVisible(true);
    stackViewPane.revalidate();

    AnWindow.getInstance().setSubviewReadyToShow("heapCallStackSubview", true);
  }

  // Listener for updating table
  private final class TableHandler implements AnListener {

    @Override
    public void valueChanged(final AnEvent event) {

      switch (event.getType()) {
        case AnEvent.EVT_SELECT: // Selecting
          stackRowId = event.getValue();
          computed = false;
          AnUtility.dispatchOnSwingThread(
              new Runnable() {
                // @Override
                @Override
                public void run() {
                  long id = dataCalStkIds[stackRowId];
                  int[] rows = table.getSelectedRows();
                  if (rows != null) {
                    long[] ids = getSelectedIds(rows, DSP_Heap);
                    window.getSelectionManager().updateSelection(ids, DSP_Heap, subtype, 1);
                  }
                  updateStackView(id);
                }
              });

          break;
        case AnEvent.EVT_SORT: // Sorting
          // save current scroll location
          updateIfNotSort = false;
          getSettings()
              .getMetricsSetting()
              .setSortMetricByDType(this, ((Integer) event.getAux()).intValue(), type);
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
          int from = event.getValue();
          int to = ((Integer) event.getAux()).intValue();
          getSettings().getMetricsSetting().setMetricOrderByDType(this, from, to, type);
          break;
        case AnEvent.EVT_UPDATE:
          //                    setUpdated(false);
          break;
        case AnEvent.EVT_COMPUTE:
          computed = false;
          break;
      }
    }
  }

  // color change(s)
  @Override
  public void valueChanged(AnEvent e) {
    //        setUpdated(false);
    stack_view.repaint();
  }

  // Set selected function
  private void updateSummary(final StackState tmpstate) {
    if (!(tmpstate instanceof StackViewState)) {
      System.err.println("XXXmpview heapactivitydisp: not StackViewState");
      return;
    }
    StackViewState state = (StackViewState) tmpstate;

    final long sel_func;
    final long sel_pc;

    sel_func = state.getNumber();
    sel_pc = state.getPC();
    long sel_obj = window.getSelectedObject().getObject(sel_func, sel_pc);

    if (sel_obj != 0L) {
      window.getSelectedObject().setSelObj(sel_obj, AnDisplay.DSP_Heap, 0);
      // lazy computing may be needed for if Summary is not been selected
      if (window.getViews().getCurrentViewDisplay() == window.getHeapView()) {
        int[] rows = table.getSelectedRows();
        if (rows != null) {
          long[] ids = getSelectedIds(rows, getDisplayMode());
          window.getSelectionManager().updateSelection(ids, getDisplayMode(), 0, 1);
        }
      } else {
        AnWindow.getInstance()
            .getViews()
            .getCurrentViewDisplay()
            .computeOnAWorkerThread(); // Update selection. FIXUP: Views that react on selections
        // should change selction on SELECTION_CHANGED or
        // SELECTED_OBJECT_CHANGED events if not from own view
      }
    }
  }

  @Override
  public void adjustmentValueChanged(final AdjustmentEvent e) {
    revalidate();
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    if (SwingUtilities.isLeftMouseButton(event)) {
      funcChanged(event);
    }
    if (SwingUtilities.isRightMouseButton(event)) {
      stackViewPopupListener.maybeShowPopup(event);
    }
  }

  @Override
  public void mouseEntered(final MouseEvent event) {}

  @Override
  public void mouseExited(final MouseEvent event) {}

  @Override
  public void mousePressed(final MouseEvent event) {}

  @Override
  public void mouseReleased(final MouseEvent event) {}

  // Will be needed if I want to update summary tab based on selection
  @Override
  public void valueChanged(final ListSelectionEvent event) {
    funcChanged(event);
  }

  // (MUST BE CALLED ON AWT THREAD)
  public void funcChanged(EventObject event) {
    final JList list;
    StackState state = null;

    if (event instanceof MouseEvent) {
      if (window.getColorChooser().isVisible()) {
        window.getColorChooser().setVisible(true);
      }
      if (((MouseEvent) event).getClickCount() > 1) {
        window.setSelectedView(AnDisplay.DSP_Source);
      }
    } else {
      if (window.getColorChooser().isVisible()) {
        window.getColorChooser().setVisible(true);
      }
    }

    if (event.getSource() instanceof StackView) {
      list = stack_view;
      state = (StackState) list.getSelectedValue();
    } else {
      // XXXmpview, figure this out...
      // System.err.println("XXXmpview heapactivitydisp: eventsource unknown");
      return;
    }

    if (state == null) {
      return;
    }
    stack_view.setSelectedFunction(state.getNumber()); // is this redundant?
    window.getColorChooser().setSelectedFunction(state.getNumber());
    updateSummary(state);
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  // Find
  @Override
  public int find(final String str, final boolean next, boolean caseSensitive) {
    int find_row;

    if ((str == null) || (str.length() == 0)) {
      return -1;
    }

    find_row = table.findAfter(str, next, caseSensitive);
    if (find_row == -1) {
      find_row = table.findBefore(str, next, caseSensitive);
    }

    // Update summary display
    if (find_row != -1) {
      goToLine(table, find_row);
    }

    return find_row;
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String text = super.exportAsText(limit, format, delimiter); // exports the main table
    // Export lower panel if format is TEXT
    if (format == ExportFormat.TEXT && (limit == null || limit >= 0)) {
      statisticsData = getHeapStatistics();
      text += exportStatisticsData(statisticsData);
    }
    return text;
  }

  // Native methods from liber_dbe.so
  private String[][] getHeapStatistics() {
    synchronized (IPC.lock) {
      window.IPC().send("getHeapStatistics");
      window.IPC().send(0);
      return (String[][]) window.IPC().recvObject();
    }
  }

  @Override
  public List<Subview> getVisibleSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    list.add(window.getHeapCallStackSubview());
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    list.add(window.getHeapCallStackSubview());
    return list;
  }

  @Override
  public void goToLine(final AnTable tbl, final int row) {
    tbl.setSelectedRow(row);
    int[] rows = table.getSelectedRows();
    if (rows != null) {
      long[] ids = getSelectedIds(rows, getDisplayMode());
      window.getSelectionManager().updateSelection(ids, getDisplayMode(), subtype, 1);
      long id = dataCalStkIds[row];
      updateStackView(id);
    }
  }
}
