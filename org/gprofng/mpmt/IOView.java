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

import static org.gprofng.mpmt.AnDisplay.DSP_IO;
import static org.gprofng.mpmt.AnDisplay.DSP_IOCallStacks;
import static org.gprofng.mpmt.AnDisplay.DSP_IOFileDescriptors;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;

import org.gprofng.analyzer.AnEnvironment;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class IOView extends FuncListDisp
    implements AnListener,
        MouseListener,
        AdjustmentListener,
        ListSelectionListener,
        AnChangeListener {

  private boolean inCompute = false;
  private ButtonControlPanel buttonPanel;
  private JScrollPane errorPane = null;
  private Object[][] tableDataFile;
  private Object[][] tableDataVfd;
  private Object[][] tableDataCalStk;
  private long[] dataFileIds = null;
  private long[] dataVfdIds = null;
  private long[] dataCalStkIds = null;
  private JRadioButton actButton;
  private JRadioButton detButton;
  private JRadioButton calStkButton;
  private JPanel statPanel;
  private AnJScrollPane statScrollPane;

  private String[][] statisticsData;
  private final int WRITE = 0;
  private final int READ = 1;
  private final int OTHER = 2;
  private final int ERROR = 3;

  private JComponent eventPane;
  private JComponent stackViewPane;
  private StackView stack_view; // Right-hand Tab's Event Stack Display

  private final StateColorMap clmap;
  private int fileRowId;
  private int vfdRowId;
  private int stackRowId;

  private boolean updateIfNotSort;

  private StackView.StackViewPopupListener stackViewPopupListener;

  /** Keeps last warning to avoid showing the same warning message many times */
  // Constructor
  public IOView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_IO, 0, AnVariable.HELP_TabsIOActivity);
    clmap = window.getColorChooser().getColorMap();
    clmap.addAnListener(this);
    initTab();

    stackViewPopupListener = stack_view.new StackViewPopupListener(window);

    setAccessibility(AnLocale.getString("IO"));
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
    //        System.out.println("IOView stateChanged:  " + e.getType());
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
    TitledBorder titledBorder = new TitledBorder(AnLocale.getString("I/O Activity"));
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
    return AnLocale.getString("Call Stack - I/O");
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

    acName = AnLocale.getString("IOActivity");
    acDesc = AnLocale.getString("Show I/O activity for selected application");
    if (acName != null) {
      acLabel = new JLabel(acName, JLabel.RIGHT);
      AnUtility.setAccessibleContext(acLabel);
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

    JSplitPane sp = new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, table, statScrollPane);
    sp.setResizeWeight(.75);

    add(sp, BorderLayout.CENTER);
    buttonPanel = new ButtonControlPanel();
    add(buttonPanel, BorderLayout.NORTH);
  }

  class ButtonControlPanel extends JPanel {

    int sort_ind;
    MetricLabel[] label;

    public ButtonControlPanel() {
      JPanel displayPanel;
      String acName;
      String acDesc;
      fileRowId = 0;
      vfdRowId = 0;
      stackRowId = 0;

      setLayout(new FlowLayout(FlowLayout.LEFT));
      setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
      displayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      displayPanel.setMinimumSize(new Dimension(20, 20)); // So window can resize properly
      displayPanel.setBorder(null);
      ButtonGroup group = new ButtonGroup();
      JLabel aggrlabel = new JLabel(AnLocale.getString("Aggregate I/O Data By:"));
      AnUtility.setAccessibleContext(aggrlabel);
      displayPanel.add(aggrlabel);
      // Accessibility
      acName = AnLocale.getString("Aggregate I/O Data By");
      acDesc = AnLocale.getString("Select Aggregate I/O Data By");
      aggrlabel.getAccessibleContext().setAccessibleName(acName);
      aggrlabel.getAccessibleContext().setAccessibleDescription(acDesc);

      actButton = new JRadioButton(AnLocale.getString("File Name"));
      actButton.setMnemonic(AnLocale.getString('A', "IOViewFileNameMN"));
      actButton.setSelected(true);
      group.add(actButton);
      displayPanel.add(actButton);
      // Accessibility
      acName = AnLocale.getString("Aggregate I/O Data By: File Name");
      acDesc = AnLocale.getString("Set Aggregate I/O Data BY: File Name");
      actButton.getAccessibleContext().setAccessibleName(acName);
      actButton.getAccessibleContext().setAccessibleDescription(acDesc);

      detButton = new JRadioButton(AnLocale.getString("File Descriptor"));
      detButton.setMnemonic(AnLocale.getString('l', "IOViewFileDescriptorMN"));
      detButton.setSelected(false);
      group.add(detButton);
      displayPanel.add(detButton);
      // Accessibility
      acName = AnLocale.getString("Aggregate I/O Data By: File Descriptor");
      acDesc = AnLocale.getString("Set Aggregate I/O Data BY: File Descriptor");
      detButton.getAccessibleContext().setAccessibleName(acName);
      detButton.getAccessibleContext().setAccessibleDescription(acDesc);

      calStkButton = new JRadioButton(AnLocale.getString("Call Stack"));
      calStkButton.setMnemonic(AnLocale.getString('s', "IOViewCallStackMN"));
      calStkButton.setSelected(false);
      group.add(calStkButton);
      displayPanel.add(calStkButton);
      // Accessibility
      acName = AnLocale.getString("Aggregate I/O Data By: Call Stack");
      acDesc = AnLocale.getString("Set Aggregate I/O Data BY: Call Stack");
      calStkButton.getAccessibleContext().setAccessibleName(acName);
      calStkButton.getAccessibleContext().setAccessibleDescription(acDesc);

      add(displayPanel);

      actButton.addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              if (actButton.isSelected()) {
                Object[] gtm = getTotalMax();
                label = getSettings().getMetricsSetting().getLabel(tableDataFile, gtm, type, table);
                name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
                AnUtility.checkIPCOnWrongThread(false);
                names = getNames(type, 0); // IPC - FIXUP: don't do this on AWT
                AnUtility.checkIPCOnWrongThread(true);
                sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
                table.setData(label, tableDataFile, names, null, 0, name_col, sort_ind);
                table.setSelectedRow(fileRowId);
                table.showSelectedRow();

                int[] rows = table.getSelectedRows();
                if (rows != null) {
                  long[] ids = getSelectedIds(rows, DSP_IO);
                  window.getSelectionManager().updateSelection(ids, DSP_IO, subtype, 1);
                }
              }
              window.showSubview(window.getIoCallStackSubview(), false);
            }
          });

      detButton.addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              if (detButton.isSelected()) {
                Object[] gtm = getTotalMax();
                label = getSettings().getMetricsSetting().getLabel(tableDataVfd, gtm, type, table);
                name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
                AnUtility.checkIPCOnWrongThread(false);
                names = getNames(type, 0); // IPC - FIXUP: don't do this on AWT
                AnUtility.checkIPCOnWrongThread(true);
                sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
                table.setData(label, tableDataVfd, names, null, 0, name_col, sort_ind);
                table.setSelectedRow(vfdRowId);
                table.showSelectedRow();

                int[] rows = table.getSelectedRows();
                if (rows != null) {
                  long[] ids = getSelectedIds(rows, DSP_IOFileDescriptors);
                  window
                      .getSelectionManager()
                      .updateSelection(ids, DSP_IOFileDescriptors, subtype, 1);
                }
              }
              window.showSubview(window.getIoCallStackSubview(), false);
            }
          });

      calStkButton.addItemListener(
          new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              if (calStkButton.isSelected()) {
                Object[] gtm = getTotalMax();
                label =
                    getSettings().getMetricsSetting().getLabel(tableDataCalStk, gtm, type, table);
                name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
                AnUtility.checkIPCOnWrongThread(false);
                names = getNames(type, 0); // IPC - FIXUP: don't do this on AWT
                AnUtility.checkIPCOnWrongThread(true);
                sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
                table.setData(label, tableDataCalStk, names, null, 0, name_col, sort_ind);
                table.setSelectedRow(stackRowId);
                table.showSelectedRow();

                if (stackRowId >= dataCalStkIds.length) {
                  stackRowId = dataCalStkIds.length - 1;
                }
                long id = dataCalStkIds[stackRowId];
                int[] rows = table.getSelectedRows();
                if (rows != null) {
                  long[] ids = getSelectedIds(rows, DSP_IOCallStacks);
                  window.getSelectionManager().updateSelection(ids, DSP_IOCallStacks, subtype, 1);
                }
                updateStackView(id);
              }
              window.showSubview(window.getIoCallStackSubview(), true);
            }
          });
    }
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
      AnUtility.setAccessibleContext(error_label);
      if (errorPane == null) {
        errorPane = new AnJScrollPane();
        add(errorPane, BorderLayout.CENTER);
      }
      errorPane.setViewportView(error_view);
      errorPane.setVisible(true);
      buttonPanel.setVisible(false);
      statScrollPane.setVisible(false);
      return;
    }

    if (errorPane != null) {
      errorPane.setVisible(false);
    }

    buttonPanel.setVisible(true);
    statScrollPane.setVisible(true);

    if (inCompute) {
      return;
    } else {
      inCompute = true;
    }

    //        int typeForPresentation = type;
    if (!computed) { // need re-compute
      reset();
      tableDataFile = null;
      dataFileIds = null;
      tableDataVfd = null;
      dataVfdIds = null;
      tableDataCalStk = null;
      dataCalStkIds = null;
      stack_view.reset();

      table.setViewport();
      final AnMetric[] mlist = getSettings().getMetricsSetting().getMetricListByDType(type);
      Object[] raw_data_with_ids =
          window.getTableDataV2("MET_IO", "ALL", "IOACTFILE", "" + subtype, null);
      tableDataFile = localProcessData(mlist, raw_data_with_ids);
      dataFileIds = current_ids;
      if (updateIfNotSort) {
        statisticsData = getIOStatistics();
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

      raw_data_with_ids = window.getTableDataV2("MET_IO", "ALL", "IOACTVFD", "" + subtype, null);
      tableDataVfd = localProcessData(mlist, raw_data_with_ids);
      dataVfdIds = current_ids;

      raw_data_with_ids = window.getTableDataV2("MET_IO", "ALL", "IOCALLSTACK", "" + subtype, null);
      tableDataCalStk = localProcessData(mlist, raw_data_with_ids);
      dataCalStkIds = current_ids;
      names = getNames(type, 0); // IPC call, don't do on AWT

      AnUtility.dispatchOnSwingThread(
          new Runnable() {
            // @Override
            @Override
            public void run() {
              if (actButton.isSelected()) {
                MetricLabel[] label =
                    getSettings().getMetricsSetting().getLabel(tableDataFile, null, type, table);
                name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
                int sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
                table.setData(label, tableDataFile, names, null, 0, name_col, sort_ind);
                table.setSelectedRow(fileRowId);
                table.showSelectedRow();
              } else if (detButton.isSelected()) {
                MetricLabel[] label =
                    getSettings().getMetricsSetting().getLabel(tableDataVfd, null, type, table);
                name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
                int sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
                table.setData(label, tableDataVfd, names, null, 0, name_col, sort_ind);
                table.setSelectedRow(vfdRowId);
                table.showSelectedRow();
              } else if (calStkButton.isSelected()) {
                MetricLabel[] label =
                    getSettings().getMetricsSetting().getLabel(tableDataCalStk, null, type, table);
                name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
                int sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(type);
                table.setData(label, tableDataCalStk, names, null, 0, name_col, sort_ind);
                table.setSelectedRow(stackRowId);
                table.showSelectedRow();
              }
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
    //            label = getSettings().getMetricsSetting().getLabel(table.getTableData(),
    //                    gtm, typeForPresentation, table);
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
            if (actButton.isSelected()) {
              int[] rows = table.getSelectedRows();
              if (rows != null) {
                long[] ids = getSelectedIds(rows, DSP_IO);
                window.getSelectionManager().updateSelection(ids, DSP_IO, subtype, 1);
              }
            } else if (detButton.isSelected()) {
              int[] rows = table.getSelectedRows();
              if (rows != null) {
                long[] ids = getSelectedIds(rows, DSP_IOFileDescriptors);
                window
                    .getSelectionManager()
                    .updateSelection(ids, DSP_IOFileDescriptors, subtype, 1);
              }
            } else if (calStkButton.isSelected()) {
              if (stackRowId >= dataCalStkIds.length) {
                stackRowId = dataCalStkIds.length - 1;
              }
              long id = dataCalStkIds[stackRowId];
              int[] rows = table.getSelectedRows();
              if (rows != null) {
                long[] ids = getSelectedIds(rows, DSP_IOCallStacks);
                window.getSelectionManager().updateSelection(ids, DSP_IOCallStacks, subtype, 1);
              }
              updateStackView(id);
            }
          }
        });

    computed = true;
    //        setUpdated(true);

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
    int displayMode = DSP_IO;

    if (detButton.isSelected()) {
      displayMode = DSP_IOFileDescriptors;
    } else if (calStkButton.isSelected()) {
      displayMode = DSP_IOCallStacks;
    }

    return displayMode;
  }

  public long[] getSelectedIds(int selectedIndices[], int displayMode) { // FIXUP: REARCH
    long[] ids = null;

    if (selectedIndices.length >= 1) {
      ids = new long[selectedIndices.length];
      for (int i = 0; i < selectedIndices.length; i++) {
        if (displayMode == DSP_IO) {
          ids[i] = dataFileIds[selectedIndices[i]];
        } else if (displayMode == DSP_IOFileDescriptors) {
          ids[i] = dataVfdIds[selectedIndices[i]];
        } else {
          ids[i] = dataCalStkIds[selectedIndices[i]];
        }
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
      strBuild.append(AnLocale.getString("I/O Data Statistics For <Total>"));
      strBuild.append("\n\n");
    } else {
      strBuild.append(AnLocale.getString("I/O Data Statistics For <Total> (filters on)"));
      strBuild.append("\n\n");
    }

    if (raw_data[WRITE].length > 1) {
      strBuild.append(raw_data[WRITE][0]);
      strBuild.append("\n");
      strBuild.append(AnLocale.getString("I/O Size Range"));
      strBuild.append("\t\t\t");
      strBuild.append(AnLocale.getString("Write Calls"));
      strBuild.append("\n");

      for (int i = 1; i < raw_data[WRITE].length; i++) {
        if (raw_data[WRITE][i].trim().indexOf('-') > 0) {
          strBuild.append("  ");
        }
        strBuild.append(raw_data[WRITE][i]);
        if (raw_data[WRITE][i].trim().indexOf('-') > 0) {
          strBuild.append("\t\t\t");
        } else {
          strBuild.append("\t\t");
        }
        i++;
        strBuild.append(raw_data[WRITE][i]);
        strBuild.append("\n");
      }
      strBuild.append("\n");
    }

    if (raw_data[READ].length > 1) {
      strBuild.append(raw_data[READ][0]);
      strBuild.append("\n");
      strBuild.append(AnLocale.getString("I/O Size Range"));
      strBuild.append("\t\t\t");
      strBuild.append(AnLocale.getString("Read Calls"));
      strBuild.append("\n");

      for (int i = 1; i < raw_data[READ].length; i++) {
        if (raw_data[READ][i].trim().indexOf('-') > 0) {
          strBuild.append("  ");
        }
        strBuild.append(raw_data[READ][i]);
        if (raw_data[READ][i].trim().indexOf('-') > 0) {
          strBuild.append("\t\t\t");
        } else {
          strBuild.append("\t\t");
        }
        i++;
        strBuild.append(raw_data[READ][i]);
        strBuild.append("\n");
      }
      strBuild.append("\n");
    }

    if (raw_data[OTHER].length > 1) {
      strBuild.append(raw_data[OTHER][0]);
      strBuild.append("\n");
      for (int i = 1; i < raw_data[OTHER].length; i++) {
        strBuild.append(raw_data[OTHER][i]);
        strBuild.append("\t\t");
        i++;
        strBuild.append(raw_data[OTHER][i]);
        strBuild.append("\n");
      }
      strBuild.append("\n");
    }

    if (raw_data[ERROR].length > 1) {
      strBuild.append(raw_data[ERROR][0]);
      strBuild.append("\n");
      for (int i = 1; i < raw_data[ERROR].length; i++) {
        strBuild.append(raw_data[ERROR][i]);
        strBuild.append("\t\t");
        i++;
        strBuild.append(raw_data[ERROR][i]);
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
      JLabel label = new JLabel(AnLocale.getString("I/O Data Statistics For <Total>"));
      AnUtility.setAccessibleContext(label);
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
      JLabel label = new JLabel(AnLocale.getString("I/O Data Statistics For <Total> (filters on)"));
      AnUtility.setAccessibleContext(label);
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

    if (raw_data[WRITE].length > 1) {
      JPanel writePanel = new JPanel();
      writePanel.setLayout(new GridBagLayout());
      writePanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsWrite = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[WRITE][0]);
      AnUtility.setAccessibleContext(label1);
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

      gridBagConstraintsWrite.gridwidth = 1;

      JLabel label2 = new JLabel(AnLocale.getString("I/O Size Range"));
      AnUtility.setAccessibleContext(label2);
      label2.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsWrite.weightx = 1;
      gridBagConstraintsWrite.weighty = 0;
      gridBagConstraintsWrite.gridx = 0;
      gridBagConstraintsWrite.gridy = 1;
      gridBagConstraintsWrite.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsWrite.fill = GridBagConstraints.NONE;
      gridBagConstraintsWrite.insets.bottom = 1;
      gridBagConstraintsWrite.insets.top = 0;
      gridBagConstraintsWrite.insets.left = 0;
      writePanel.add(label2, gridBagConstraintsWrite);

      JLabel label3 = new JLabel(AnLocale.getString("Write Calls"));
      AnUtility.setAccessibleContext(label3);
      label3.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsWrite.weightx = 1;
      gridBagConstraintsWrite.weighty = 0;
      gridBagConstraintsWrite.gridx = gridBagConstraintsWrite.gridx + 1;
      gridBagConstraintsWrite.gridy = 1;
      gridBagConstraintsWrite.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsWrite.fill = GridBagConstraints.NONE;
      gridBagConstraintsWrite.insets.bottom = 1;
      gridBagConstraintsWrite.insets.top = 0;
      gridBagConstraintsWrite.insets.left = 120;
      writePanel.add(label3, gridBagConstraintsWrite);

      for (int i = 1; i < raw_data[WRITE].length; i++) {
        JLabel label4 = new JLabel(raw_data[WRITE][i]);
        AnUtility.setAccessibleContext(label4);
        label4.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsWrite.weightx = 1;
        gridBagConstraintsWrite.weighty = 0;
        gridBagConstraintsWrite.gridx = 0;
        gridBagConstraintsWrite.gridy = gridBagConstraintsWrite.gridy + 1;
        gridBagConstraintsWrite.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsWrite.fill = GridBagConstraints.NONE;
        gridBagConstraintsWrite.insets.bottom = 0;
        gridBagConstraintsWrite.insets.top = 0;

        if (raw_data[WRITE][i].trim().indexOf('-') > 0) {
          gridBagConstraintsWrite.insets.left = 5;
        } else {
          gridBagConstraintsWrite.insets.left = 0;
        }
        writePanel.add(label4, gridBagConstraintsWrite);

        i++;
        JLabel label5 = new JLabel(raw_data[WRITE][i]);
        AnUtility.setAccessibleContext(label5);
        label5.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsWrite.weightx = 1;
        gridBagConstraintsWrite.weighty = 0;
        gridBagConstraintsWrite.gridx = gridBagConstraintsWrite.gridx + 1;
        gridBagConstraintsWrite.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsWrite.fill = GridBagConstraints.NONE;
        gridBagConstraintsWrite.insets.left = 120;
        gridBagConstraintsWrite.insets.bottom = 0;
        writePanel.add(label5, gridBagConstraintsWrite);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(writePanel, gridBagConstraints);
    }

    if (raw_data[READ].length > 1) {
      JPanel readPanel = new JPanel();
      readPanel.setLayout(new GridBagLayout());
      readPanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsRead = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[READ][0]);
      AnUtility.setAccessibleContext(label1);
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

      gridBagConstraintsRead.gridwidth = 1;

      JLabel label2 = new JLabel(AnLocale.getString("I/O Size Range"));
      AnUtility.setAccessibleContext(label2);
      label2.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsRead.weightx = 1;
      gridBagConstraintsRead.weighty = 0;
      gridBagConstraintsRead.gridx = 0;
      gridBagConstraintsRead.gridy = 1;
      gridBagConstraintsRead.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsRead.fill = GridBagConstraints.NONE;
      gridBagConstraintsRead.insets.bottom = 1;
      readPanel.add(label2, gridBagConstraintsRead);

      JLabel label3 = new JLabel(AnLocale.getString("Read Calls"));
      AnUtility.setAccessibleContext(label3);
      label3.setFont(table.getFont().deriveFont(Font.BOLD));
      gridBagConstraintsRead.weightx = 1;
      gridBagConstraintsRead.weighty = 0;
      gridBagConstraintsRead.gridx = gridBagConstraintsRead.gridx + 1;
      gridBagConstraintsRead.gridy = 1;
      gridBagConstraintsRead.anchor = GridBagConstraints.LINE_START;
      gridBagConstraintsRead.fill = GridBagConstraints.NONE;
      gridBagConstraintsRead.insets.bottom = 1;
      gridBagConstraintsRead.insets.left = 120;
      readPanel.add(label3, gridBagConstraintsRead);

      for (int i = 1; i < raw_data[READ].length; i++) {
        JLabel label4 = new JLabel(raw_data[READ][i]);
        AnUtility.setAccessibleContext(label4);
        label4.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsRead.weightx = 1;
        gridBagConstraintsRead.weighty = 0;
        gridBagConstraintsRead.gridx = 0;
        gridBagConstraintsRead.gridy = gridBagConstraintsRead.gridy + 1;
        gridBagConstraintsRead.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsRead.fill = GridBagConstraints.NONE;
        gridBagConstraintsRead.insets.bottom = 0;
        gridBagConstraintsRead.insets.top = 0;

        if (raw_data[READ][i].trim().indexOf('-') > 0) {
          gridBagConstraintsRead.insets.left = 5;
        } else {
          gridBagConstraintsRead.insets.left = 0;
        }
        readPanel.add(label4, gridBagConstraintsRead);

        i++;
        JLabel label5 = new JLabel(raw_data[READ][i]);
        AnUtility.setAccessibleContext(label5);
        label5.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsRead.weightx = 1;
        gridBagConstraintsRead.weighty = 0;
        gridBagConstraintsRead.gridx = gridBagConstraintsRead.gridx + 1;
        gridBagConstraintsRead.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsRead.fill = GridBagConstraints.NONE;
        gridBagConstraintsRead.insets.left = 120;
        gridBagConstraintsRead.insets.bottom = 0;
        readPanel.add(label5, gridBagConstraintsRead);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(readPanel, gridBagConstraints);
    }

    if (raw_data[OTHER].length > 1) {
      JPanel otherPanel = new JPanel();
      otherPanel.setLayout(new GridBagLayout());
      otherPanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsOther = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[OTHER][0]);
      AnUtility.setAccessibleContext(label1);
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

      gridBagConstraintsOther.gridwidth = 1;

      for (int i = 1; i < raw_data[OTHER].length; i++) {
        JLabel label2 = new JLabel(raw_data[OTHER][i]);
        AnUtility.setAccessibleContext(label2);
        label2.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsOther.weightx = 1;
        gridBagConstraintsOther.weighty = 0;
        gridBagConstraintsOther.gridx = 0;
        gridBagConstraintsOther.gridy = gridBagConstraintsOther.gridy + 1;
        gridBagConstraintsOther.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsOther.fill = GridBagConstraints.NONE;
        gridBagConstraintsOther.insets.left = 0;
        otherPanel.add(label2, gridBagConstraintsOther);

        i++;
        JLabel label3 = new JLabel(raw_data[OTHER][i]);
        AnUtility.setAccessibleContext(label3);
        label3.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsOther.weightx = 1;
        gridBagConstraintsOther.weighty = 0;
        gridBagConstraintsOther.gridx = gridBagConstraintsOther.gridx + 1;
        gridBagConstraintsOther.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsOther.fill = GridBagConstraints.NONE;
        gridBagConstraintsOther.insets.left = 175;
        otherPanel.add(label3, gridBagConstraintsOther);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(otherPanel, gridBagConstraints);
    }

    if (raw_data[ERROR].length > 1) {
      JPanel errorPanel = new JPanel();
      errorPanel.setLayout(new GridBagLayout());
      errorPanel.setBackground(Color.WHITE);
      GridBagConstraints gridBagConstraintsError = new GridBagConstraints();

      JLabel label1 = new JLabel(raw_data[ERROR][0]);
      AnUtility.setAccessibleContext(label1);
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

      gridBagConstraintsError.gridwidth = 1;

      for (int i = 1; i < raw_data[ERROR].length; i++) {
        JLabel label2 = new JLabel(raw_data[ERROR][i]);
        AnUtility.setAccessibleContext(label2);
        label2.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsError.weightx = 1;
        gridBagConstraintsError.weighty = 0;
        gridBagConstraintsError.gridx = 0;
        gridBagConstraintsError.gridy = gridBagConstraintsError.gridy + 1;
        gridBagConstraintsError.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsError.fill = GridBagConstraints.NONE;
        gridBagConstraintsError.insets.left = 0;
        errorPanel.add(label2, gridBagConstraintsError);

        i++;
        JLabel label3 = new JLabel(raw_data[ERROR][i]);
        AnUtility.setAccessibleContext(label3);
        label3.setFont(table.getFont().deriveFont(Font.PLAIN));
        gridBagConstraintsError.weightx = 1;
        gridBagConstraintsError.weighty = 0;
        gridBagConstraintsError.gridx = gridBagConstraintsError.gridx + 1;
        gridBagConstraintsError.anchor = GridBagConstraints.LINE_START;
        gridBagConstraintsError.fill = GridBagConstraints.NONE;
        gridBagConstraintsError.insets.left = 175;
        errorPanel.add(label3, gridBagConstraintsError);
      }
      gridBagConstraints.weightx = 1;
      gridBagConstraints.weighty = 0;
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = gridBagConstraints.gridy + 1;
      gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
      gridBagConstraints.fill = GridBagConstraints.REMAINDER;
      gridBagConstraints.insets.bottom = 10;
      statPanel.add(errorPanel, gridBagConstraints);
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
    AnUtility.checkIPCOnWrongThread(false);
    stack_view.setStates(
        state_vec,
        selected_frame); // update stack in right-hand tab //  FIXUP: don't do this on AWT
    AnUtility.checkIPCOnWrongThread(true);
    stack_view.setStackId(stackId);
    stackViewPane.setVisible(true);
    stackViewPane.revalidate();

    AnWindow.getInstance().setSubviewReadyToShow("ioCallStackSubview", true);
  }

  // Listener for updating table
  private final class TableHandler implements AnListener {

    @Override
    public void valueChanged(final AnEvent event) {

      switch (event.getType()) {
        case AnEvent.EVT_SELECT: // Selecting
          if (actButton.isSelected()) {
            fileRowId = event.getValue();
            computed = false;
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  // @Override
                  @Override
                  public void run() {
                    int[] rows = table.getSelectedRows();
                    if (rows != null) {
                      long[] ids = getSelectedIds(rows, DSP_IO);
                      window.getSelectionManager().updateSelection(ids, DSP_IO, subtype, 1);
                    }
                  }
                });
          } else if (detButton.isSelected()) {
            vfdRowId = event.getValue();
            ;
            computed = false;
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  // @Override
                  @Override
                  public void run() {
                    int[] rows = table.getSelectedRows();
                    if (rows != null) {
                      long[] ids = getSelectedIds(rows, DSP_IOFileDescriptors);
                      window
                          .getSelectionManager()
                          .updateSelection(ids, DSP_IOFileDescriptors, subtype, 1);
                    }
                  }
                });
          } else if (calStkButton.isSelected()) {
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
                      long[] ids = getSelectedIds(rows, DSP_IOCallStacks);
                      window
                          .getSelectionManager()
                          .updateSelection(ids, DSP_IOCallStacks, subtype, 1);
                    }
                    updateStackView(id);
                  }
                });
          }

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
      System.err.println("XXXmpview ioactivitydisp: not StackViewState");
      return;
    }
    StackViewState state = (StackViewState) tmpstate;

    final long sel_func;
    final long sel_pc;

    sel_func = state.getNumber();
    sel_pc = state.getPC();
    long sel_obj = window.getSelectedObject().getObject(sel_func, sel_pc);

    if (sel_obj != 0L) {
      window.getSelectedObject().setSelObj(sel_obj, AnDisplay.DSP_IO, 0);
      // lazy computing may be needed for if Summary is not been selected
      if (window.getViews().getCurrentViewDisplay() == window.getIOView()) {
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
    } else if (window.getColorChooser().isVisible()) {
      window.getColorChooser().setVisible(true);
    }

    if (event.getSource() instanceof StackView) {
      list = stack_view;
      state = (StackState) list.getSelectedValue();
    } else {
      // XXXmpview, figure this out...
      // System.err.println("XXXmpview ioactivitydisp: eventsource unknown");
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
      statisticsData = getIOStatistics();
      text += exportStatisticsData(statisticsData);
    }
    return text;
  }

  // Native methods from liber_dbe.so
  private String[][] getIOStatistics() {
    synchronized (IPC.lock) {
      window.IPC().send("getIOStatistics");
      window.IPC().send(0);
      return (String[][]) window.IPC().recvObject();
    }
  }

  @Override
  public List<Subview> getVisibleSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    if (calStkButton.isSelected()) {
      list.add(window.getIoCallStackSubview());
    }
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    if (calStkButton.isSelected()) {
      list.add(window.getIoCallStackSubview());
    }
    return list;
  }

  @Override
  public void goToLine(final AnTable tbl, final int row) {
    tbl.setSelectedRow(row);
    int[] rows = table.getSelectedRows();
    if (rows != null) {
      long[] ids = getSelectedIds(rows, getDisplayMode());
      window.getSelectionManager().updateSelection(ids, getDisplayMode(), subtype, 1);
      if (calStkButton.isSelected()) {
        long id = dataCalStkIds[row];
        updateStackView(id);
      }
    }
  }
}
