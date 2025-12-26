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

import static org.gprofng.mpmt.AnDisplay.DSP_Functions;
import static org.gprofng.mpmt.AnDisplay.DSP_Lines;
import static org.gprofng.mpmt.AnDisplay.DSP_PCs;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnTable.SelObjInfo;
import org.gprofng.mpmt.export.Export;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.settings.ViewsSetting;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;

public class FuncListDisp extends AnDisplay implements ExportSupport {

  // public AnHotGap gap; // defined in AnDisplay
  protected AnTable table;
  protected long sel_func, sel_src;
  protected int name_col;
  protected String[] names;
  private int[] src_type;
  private boolean can_comp;
  private int prevScroll = -1;
  protected long[] current_ids;
  protected boolean ignoreSelected = false;
  protected boolean updateSummary = true;
  /** Keeps last warning to avoid showing the same warning message many times */
  private String lastWarning = null;

  protected JPanel warningPane;

  // Constructor
  public FuncListDisp(
      final AnWindow window, final int type, final int subtype, final String help_id) {
    super(window, type, subtype, help_id);
    sel_func = 0;
    sel_src = 0;
    name_col = 0;
    can_comp = false;
    current_ids = null;
  }

  @Override
  public void requestFocus() {
    if (table != null) {
      table.requestFocus();
    }
  }

  @Override
  public JPopupMenu getFilterPopup() {
    if (table != null) {
      return table.initPopup(true);
    } else {
      return null;
    }
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
      case AnDisplay.DSP_Source:
        acName = AnLocale.getString("Source");
        acDesc = AnLocale.getString("Show source for selected function");
        break;
      case AnDisplay.DSP_IO:
      case AnDisplay.DSP_IOFileDescriptors:
      case AnDisplay.DSP_IOCallStacks:
        acName = AnLocale.getString("IOActivity");
        acDesc = AnLocale.getString("Show I/O activity for selected application");
        break;
      case AnDisplay.DSP_Heap:
        acName = AnLocale.getString("HeapActivity");
        acDesc = AnLocale.getString("Show Heap activity for selected application");
        break;
      case AnDisplay.DSP_Disassembly:
        acName = AnLocale.getString("Disasm");
        acDesc = AnLocale.getString("Show assembly for selected function");
        break;
      case AnDisplay.DSP_Lines:
        acName = AnLocale.getString("Hot Lines");
        acDesc = AnLocale.getString("Show hot lines");
        break;
      case AnDisplay.DSP_PCs:
        acName = AnLocale.getString("PC");
        acDesc = AnLocale.getString("Show hot PCs");
        break;
      case AnDisplay.DSP_IndexObject:
        // XXX: This is not a good idea to use subtype,
        //      but we don't have a better API yet.
        if ((0 <= subtype) && (9 >= subtype)) {
          switch (subtype) {
            case 0: // Threads
              acName = AnLocale.getString("Threads");
              acDesc = acName;
              break;
            case 1: // CPUs
              acName = AnLocale.getString("CPUs");
              acDesc = acName;
              break;
            case 2: // Samples
              acName = AnLocale.getString("Samples");
              acDesc = acName;
              break;
            case 3: // Seconds
              acName = AnLocale.getString("Seconds");
              acDesc = acName;
              break;
            case 4: // Processes
              acName = AnLocale.getString("Processes");
              acDesc = acName;
              break;
            case 5: // ExperimentIDs
              acName = AnLocale.getString("ExperimentIDs");
              acDesc = acName;
              break;
            case 6: // Data Size
              acName = AnLocale.getString("Data Size");
              acDesc = acName;
              break;
            case 7: // Duration
              acName = AnLocale.getString("Duration");
              acDesc = acName;
              break;
            case 8: // OpenMP Parallel Region
              acName = AnLocale.getString("OpenMP Parallel Region");
              acDesc = acName;
              break;
            case 9: // OpenMP Tasks
              acName = AnLocale.getString("OpenMP Tasks");
              acDesc = acName;
              break;
          }
        }
        break;
    }
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
    //        if (type == DSP_DataLayout) {
    //            HotGapPanel hotGapPanel = createHotGapPanel();
    //            table.setHotGapPanel(hotGapPanel);
    //            add(hotGapPanel, BorderLayout.EAST);
    //        }
    switch (type) {
      case AnDisplay.DSP_Source:
        warningPane = new JPanel();
        warningPane.setVisible(false);
        warningPane.setBorder(new LineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR));
        add(warningPane, BorderLayout.SOUTH);
        break;
    }
  }

  private static JPanel toolbarPanel = null; // Fixup: XXXX ugly design

  public void updateToolBar() {
    if (type != AnDisplay.DSP_Functions) {
      return;
    }
    if (backForwardControls == null) {
      backForwardControls = new BackForwardControls();
    }
    backForwardControls.updateToolBarStatus();

    FuncListDisp.getToolbarPanelInternal().removeAll();
    FuncListDisp.getToolbarPanelInternal().add(backForwardControls, BorderLayout.CENTER);
    FuncListDisp.getToolbarPanelInternal().repaint();
  }

  protected BackForwardControls backForwardControls = null;
  protected JPanel backForwardControlsPanel = null;

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

  /**
   * @return the settings
   */
  public Settings getSettings() {
    return AnWindow.getInstance().getSettings();
  }

  protected class BackForwardControls extends JPanel {

    protected AnIconButton backButton;
    protected AnIconButton forwardButton;

    public BackForwardControls() {
      setOpaque(false);
      setLayout(new GridBagLayout());
      GridBagConstraints gridBagConstraints;
      // Back button
      gridBagConstraints = new GridBagConstraints();
      backButton = new AnIconButton(AnUtility.goBackwardIcon);
      backButton.setMargin(new Insets(2, 0, 2, 2));
      backButton.setToolTipText(
          AnLocale.getString("Back")
              + AnUtility.keyStrokeToStringFormatted(KeyboardShortcuts.backwardActionShortcut));
      backButton.setEnabled(true);
      backButton.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              // Hook it up to back action;
              table.goBack();
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
              table.goForward();
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
      if (backButton != null) {
        backButton.setEnabled(table.canGoBack());
      }
      if (forwardButton != null) {
        forwardButton.setEnabled(table.canGoForward());
      }
    }
  }

  public void tryAddNewObj(int row) {
    // add new obj to history if it's different from current
    if (table.getNavigationHistoryPool().getHistory().enabled
        && table.getType() == AnDisplay.DSP_Functions) {
      SelObjInfo cur_so = table.getNavigationHistoryPool().getHistory().getCurrent();
      if (cur_so == null || cur_so.lineno != row) {
        SelObjInfo new_so = new SelObjInfo(0, row, "");
        table.getNavigationHistoryPool().getHistory().goToNew(new_so);
      } else {
        table.getNavigationHistoryPool().getHistory().newAdded = false;
      }
    }
  }

  // Clear display
  @Override
  public void clear() {
    if (table != null) {
      table.removeAll();
    }
  }

  /** Copy all lines to the system clipboard */
  protected void copyAll() {
    String text = exportAsText(null, ExportFormat.TEXT, null);
    AnUtility.copyToClipboard(text);
  }

  /** Copy selected lines to the system clipboard */
  protected void copySelected() {
    String sortedby = AnLocale.getString("sorted by metric:");
    String textImage = table.printTableHeader(sortedby, MaximumValues);
    int printLimit = 0;
    textImage += table.printSelectedTableContents(MaximumValues, printLimit);
    AnUtility.copyToClipboard(textImage);
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String sortedby = AnLocale.getString("sorted by metric:");
    String textImage = "";
    if (format == ExportFormat.TEXT) {
      textImage = table.printTableHeader(sortedby, MaximumValues);
      if (null == MaximumValues) {
        return textImage; // empty report
      }
      int printLimit = limit != null ? limit : 0;
      textImage += table.printTableContents(MaximumValues, printLimit);
    } else if (format == ExportFormat.CSV || format == ExportFormat.HTML) {
      // FIXUP: code shold be moved form gp-display-text to GUI
      AnUtility.checkIPCOnWrongThread(false);
      Export.setLimit(limit);
      Export.setFormat(format, delimiter);
      String fake_printer = "-";
      textImage = Export.printData(type, subtype, fake_printer, null); // IPC call
      AnUtility.checkIPCOnWrongThread(true);
    }
    return textImage;
  }

  // Get new selected func index & compare to the old one
  // Note: must be called from doCompute
  protected final long getFuncObj() {
    final long func_obj;
    final long src_obj;

    // System.err.println("XXX In FLD getFuncObj()");
    // need Total/Maximum from func-list data
    window
        .getFunctionsView()
        .computeIfNeeded(); // on worker thread and synchronized (AnVariable.mainFlowLock)
    func_obj = window.getSelectedObject().getSelObj(DSP_Functions, 0);

    if (sel_func != func_obj) {
      computed = false;
      if (type == DSP_Source) {
        src_obj = window.getSelectedObject().getSelObj(DSP_SourceSelectedObject, 0);
        if (src_obj != sel_src) {
          sel_src = src_obj;
        }
      }
    } else if (type != DSP_Source) {
      computed = false;
    } else {
      src_obj = window.getSelectedObject().getSelObj(DSP_SourceSelectedObject, 0);
      if (src_obj != sel_src) {
        computed = false;
        sel_src = src_obj;
      }
    }

    return func_obj;
  }

  private String getTabName() {
    List<ViewsSetting.View> indx_tablist =
        AnWindow.getInstance().getSettings().getViewsSetting().getIndexViews();
    if (type == AnDisplay.DSP_IndexObject && subtype < indx_tablist.size()) {
      AnWindow.AnDispTab adt = indx_tablist.get(subtype).getAnDispTab();
      return adt.getTCmd();
    }
    return ""; // NO18N
  }

  protected int getMetricMType() {
    if (isOmpInxObj()) {
      return AnDisplay.DSP_Functions; // FIXUP: this is UGLY!!!!!!!!!!!
    } else {
      return type;
    }
  }

  protected boolean isOmpInxObj() {
    if (type == AnDisplay.DSP_IndexObject) {
      String cmd = getTabName();
      if (cmd.equalsIgnoreCase("OMP_preg") // NO18N
          || cmd.equalsIgnoreCase("OMP_task")) { // NO18N
        return true;
      }
    }
    return false;
  }

  // Compute & update function list table
  @Override
  public void doCompute() {
    final long sel_obj;
    int new_ind = -1;
    boolean reset_new_ind = false;
    final int sort_ind;
    final Object[] raw_data;
    final String pwarnstr;
    final MetricLabel[] label;

    AnUtility.checkIfOnAWTThread(false);
    // Not selected
    if (!selected && !ignoreSelected) {
      return;
    }
    if (inCompute) { // NM TEMPORARY should be synchronized
      return;
    } else {
      inCompute = true;
    }
    // System.err.println("XXX In FLD doCompute - 1 ()");
    //        window.setBusyCursor(true);

    if (can_comp) {
      // System.err.println("XXX In FLD doCompute - 2 ()");
      sel_obj = window.getSelectedObject().getSelObj(DSP_Lines, 0);
      sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
      can_comp = false;
      computed = false;
    } else {
      // System.err.println("XXX In FLD doCompute - 3 ()");
      sel_obj = window.getSelectedObject().getSelObj(type, subtype);
      if (can_sort) { // Function/LINE/PC
        sel_func = sel_obj;
      } else { // Annotated src/dis
        sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
        // (AnVariable.mainFlowLock)
      }
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

      Object[] res = setFuncData(sel_func, type, subtype);
      new_ind = (int) ((long[]) (res[0]))[0];
      String errstr = ((String[]) (res[1]))[0];
      // Update table
      if (errstr == null) {

        // System.err.println("XXX In FLD doCompute - 5 ()");
        switch (type) {
          case AnDisplay.DSP_Source:
            updateWarningPanel();
            break;
          default:
            String warning = window.getMsg(AnUtility.WARNING_MSG);
            if (null != warning) {
              if (!(warning.equalsIgnoreCase(lastWarning))) {
                lastWarning = warning;
                showWarning(lastWarning);
              }
            }
            break;
        }
        table.setViewport();
        Object[][] table_data;
        int raw_data_length = -1;
        final AnMetric[] mlist =
            getSettings().getMetricsSetting().getMetricListByDType(typeForPresentation);
        final IPCResult ipcr_getNames = SendRequest_getNames(typeForPresentation, 0); // ASYNC IPC
        if (type == AnDisplay.DSP_IndexObject) { // Use new API
          Object[] raw_data_with_ids =
              window.getTableDataV2(mlistStr, modeStr, typeStr, subtypeStr, null);
          table_data = localProcessData(mlist, raw_data_with_ids);
          src_type = null;
        } else { // Use old API
          new_ind = -1; // reset new_ind for old API
          raw_data = getFuncList(type, subtype);
          if (raw_data != null && raw_data.length > 0) {
            raw_data_length = raw_data.length;
            table_data = localProcessData(mlist, raw_data);
            // first index is for column, second index is for rows:
            src_type = (int[]) raw_data[raw_data.length - 1]; // AT_SRC, DIS, QUOTE, etc.
          } else {
            // Should never happen but it does sometimes if you quickly select/deselect metrics in
            // overview // Changdao?
            String txt = "IPC ERROR (?) in FuncListDisp: raw_data is null or empty";
            AnLog.log(txt);
            if (tailAction != null) {
              tailAction.tailFunction();
              tailAction = null;
            }
            String[] hdrContent = ipcr_getNames.getStrings(); // To exclude the reply from the queue
            inCompute = false;
            return;
          }
        }

        // String[] hdrContent = getNames(typeForPresentation, 0); // name column table header
        // contents (?) // SYNC IPC
        String[] hdrContent = ipcr_getNames.getStrings();
        label = getSettings().getMetricsSetting()
                .getLabel(table_data, null, typeForPresentation, table);
        name_col = getSettings().getMetricsSetting().getNameColumnIndexByDType(getMetricMType());
        sort_ind = getSettings().getMetricsSetting().getSortColumnByDType(typeForPresentation);
        new_ind = (new_ind == -1) ? 0 : new_ind;
        if (!(window.getSelectedObject().wasASelDone())) {
          if ((new_ind < 1) && (raw_data_length > 1)) {
            // Change selected object to first function
            reset_new_ind = true;
            new_ind = 1;
          }
        }
        table.setData(label, table_data, hdrContent, src_type, new_ind, name_col, sort_ind);
        if (table.getTable().getColumnCount() == 1) {
          // Show "No Metrics Selected" instaed of only name column in table for certain views
          if (MetricsSetting.dtype2mtype(type) == MetricsSetting.MET_DATA) {
            table.showMessage(new NoMetricsSelectedPanel(type));
            inCompute = false;
            return;
          }
        }

        if ((sel_func == 0) || (reset_new_ind)) {
          window.getSelectedObject().setSelObj(new_ind, typeForPresentation, subtype); // SYNC IPC
        }
      } else {
        // System.err.println("XXX In FLD doCompute - 6 ()");
        window.getExperimentsView().appendLog(AnLocale.getString("Error: ") + errstr);
        table.showMessage(errstr);
      }
    }

    // Update selected row
    if (!reset_new_ind) {
      new_ind = window.getSelectedObject().getSelIndex(sel_obj, type, subtype); // SYNC IPC
    }
    if (new_ind == -1) {
      new_ind = table.getSelectedRow();
      if (type == AnDisplay.DSP_Functions) {
        tryAddNewObj(new_ind);
      }
      table.showSelectedRow();
      window.getSelectedObject().setSelObj(new_ind, type, subtype); // SYNC IPC
      sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
    } else {
      if (type == AnDisplay.DSP_Functions) {
        tryAddNewObj(new_ind);
      }
      table.setSelectedRow(new_ind);
    }

    // scroll to previous location after re-sort
    if (prevScroll > 0) {
      table.setScroll(prevScroll);
      prevScroll = -1;
    }
    updateGap();
    // Update summary display
    if (updateSummary) {
      updateSummary(new_ind);
    }

    //        pstatstr = window.getMsg(AnUtility.PSTAT_MSG);
    pwarnstr = window.getMsg(AnUtility.PWARN_MSG); // SYNC IPC
    //        if (pstatstr != null) {
    //            window.appendLog(pstatstr);
    //        }
    if (pwarnstr != null) {
      window.showProcessorWarning(pwarnstr);
    }

    FuncListDisp.getToolbarPanelInternal().removeAll();
    if (type == AnDisplay.DSP_Functions
        && window.getViews().getCurrentViewDisplay() == this) { // FIXUP: REARCH
      // Set toolbar and select subviews
      updateToolBar();
      window
          .getCalledByCallsFunctionsView()
          .doCompute(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
    }

    computed = true;
    ignoreSelected = false;
    updateSummary = true;

    if (tailAction != null) {
      tailAction.tailFunction();
      tailAction = null;
    }

    inCompute = false;
  }

  // show warning panel
  public void updateWarningPanel() {

    String warning = window.getMsg(AnUtility.WARNING_MSG);
    if (null != warning) {
      if (!(warning.equalsIgnoreCase(lastWarning))) {
        warningPane.removeAll();
        lastWarning = warning;
        String temp[] = lastWarning.split("[`']");
        String text = " ";
        for (int i = 0; i < temp.length; i += 2) {
          String temp2[] = temp[i].split("[ ]");
          for (int j = 0; j < temp2.length; j++) {
            if (!temp2[j].startsWith("Warning") && temp2[j].length() > 0) {
              text = text + temp2[j].replaceAll("[ ]", "") + " ";
            }
          }
        }

        JLabel lb = new JLabel(text, JLabel.LEFT);
        Font newLabelFont = new Font(lb.getFont().getName(), Font.PLAIN, lb.getFont().getSize());
        warningPane.setLayout(new BorderLayout());

        JLabel wlb = new JLabel(AnLocale.getString(" Warning:"), JLabel.LEFT); // I18N
        lb.setFont(newLabelFont);
        JLabel ilb = new JLabel(AnUtility.warningIcon, JLabel.LEFT);
        JPanel msgPanel = new JPanel();
        msgPanel.setOpaque(false);
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.X_AXIS));
        msgPanel.add(ilb);
        msgPanel.add(wlb);
        msgPanel.add(lb);

        warningPane.add(msgPanel, BorderLayout.WEST);
        warningPane.setToolTipText(lastWarning);
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        JButton btn = new JButton(AnUtility.removeFocusedIcon);
        Dimension dm =
            new Dimension(
                AnUtility.removeFocusedIcon.getIconWidth() + 1,
                AnUtility.removeFocusedIcon.getIconHeight() + 1);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(dm);
        btn.setMaximumSize(dm);
        btn.setPreferredSize(dm);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setSize(dm);
        panel.add(btn);

        btn.addActionListener(
            new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                warningPane.setVisible(false);
              }
            });

        warningPane.add(panel, BorderLayout.EAST);
        warningPane.setBackground(AnVariable.WARNINGPANEL_COLOR);
        // showWarning(lastWarning);
        warningPane.setVisible(true);
      }
    } else {
      warningPane.setVisible(false);
    }
  }

  // Set current func/load-object in 'selected func' & summary display
  private void updateSummary(final int new_ind) {
    //        sel_func = can_sort ? new_ind : getFuncObj();
    //        window.updateSummary(new_ind, type, subtype);

    int[] rows = null;
    if (table != null) {
      rows = table.getSelectedRows();
    }
    if (rows != null && rows.length > 1) {
      window.getSelectionManager().updateSelection(rows, type, subtype, 1);
    } else {
      window.getSelectionManager().updateSelection(new_ind, type, subtype, 1);
    }
  }

  // Update gap panel
  protected final void updateGap() {
    if (gap != null) {
      gap.update();
    }
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  // Find
  @Override
  public int find(final String str, final boolean next, boolean caseSensitive) {
    int find_row;

    if ((str == null)
        && (type != DSP_Source)
        && (type != DSP_Disassembly)
        && (type != DSP_DualSource)
        && (type != DSP_DataLayout)) {
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

  public void goToLine(final AnTable tbl, final int row) {
    final boolean can_nav;

    if (can_sort) {
      can_nav = true;
    } else {
      int stype = tbl.getSrcType(row);
      can_nav =
          (stype == AnTable.AT_SRC) || (stype == AnTable.AT_DIS) || (stype == AnTable.AT_QUOTE);
    }
    tbl.setSelectedRow(row);

    // Set selected object & Update summary display
    if (can_nav) {
      AnUtility.dispatchOnAWorkerThread(
          new Runnable() {
            @Override
            public void run() {
              window.getSelectedObject().setSelObj(row, type, subtype);
              if (type == AnDisplay.DSP_Functions) {
                if (tbl.getNavigationHistoryPool() != null) {
                  tbl.getNavigationHistoryPool().getHistory().goToFuncNew(row);
                  updateToolBar();
                }
                window.getCalledByCallsFunctionsView().setComputed(false);
                window.getCalledByCallsFunctionsView().computeOnAWorkerThread();
              }
              updateSummary(row);
            }
          },
          "funcdisplist selection");
    }
  }

  // Is current selected tab?
  public void setSelected(final boolean set) {
    selected = set;
    if (type == AnDisplay.DSP_Functions) {
      window.getCalledByCallsFunctionsView().setSelected(set);
    }
  }

  // Sort
  public void sort(final int index) {
    if (table != null) {
      table.sort(index);
    }
  }

  protected Object[][] MaximumValues = null;
  int totalColumn = -1;

  protected Object[][] localProcessData(final AnMetric[] mlist, final Object[] raw_data) {
    final Object[][] data = get_data(mlist, raw_data);
    if (data == null) {
      return null;
    }
    if ((raw_data.length > mlist.length)
        && (raw_data[mlist.length] != null)
        && (raw_data[mlist.length] instanceof long[])) {
      current_ids = (long[]) raw_data[mlist.length]; // per-row HistItem->obj->id
    }

    // XXXX (Mez) Neew to remove MaximumValues from FuncListDisp
    MaximumValues = AnObject.updateMaxValues(data, MaximumValues);

    return data;
  }

  protected Object[][] get_data(final AnMetric[] mlist, final Object[] raw_data) {
    // raw_data has one vector for each column of metric values.
    // In addition, it has a vector of dbe row identifiers (HistItem->obj->id)
    if ((raw_data == null)
        || (mlist == null)
        || (mlist.length == 0)
        || (mlist.length > raw_data.length)) {
      return null;
    }

    // "Bugs 16624403 and 19539622" (leave this string intact for searches)
    // look for the row that has name==<Total>
    int row_with_Total = -1;
    for (int i = 0; i < mlist.length; i++) {
      AnMetric m = mlist[i];
      if (!m.isNameMetric()) {
        continue;
      }
      Object[] d = (Object[]) raw_data[i];
      for (int j = 0; j < d.length; j++) {
        if (d[j].toString().equals("<Total>")) {
          // FOUND IT!!!
          row_with_Total = j;
          break;
        }
      }
      break;
    }

    final Object[][] data = new Object[mlist.length][];
    for (int i = 0; i < mlist.length; i++) {
      AnMetric m = mlist[i];
      switch (m.get_valtype()) {
        case AnMetric.VT_LLONG:
        case AnMetric.VT_ULLONG:
          if (raw_data[i] instanceof long[]) {
            data[i] = AnLong.toArray((long[]) raw_data[i]);
          } else {
            data[i] = AnDouble.toArray((double[]) raw_data[i]);
          }
          break;
        case AnMetric.VT_ADDRESS:
          if (raw_data[i] instanceof long[]) {
            data[i] = AnAddress.toArray((long[]) raw_data[i]);
          } else {
            data[i] = AnDouble.toArray((double[]) raw_data[i]);
          }
          break;
        case AnMetric.VT_DOUBLE:
          data[i] = AnDouble.toArray((double[]) raw_data[i]);
          break;
        case AnMetric.VT_LABEL:
          data[i] = (Object[]) raw_data[i];
          break;
        default:
          System.err.println(
              String.format(
                  "Error in localProcessData: unrecognized get_valtype(): %d", m.get_valtype()));
          data[i] = (Object[]) raw_data[i];
          break;
      }
      CompareMode mode = m.getCompareMode();
      if (mode == CompareMode.CMP_DELTA) {
        switch (m.get_valtype()) {
          case AnMetric.VT_LLONG: // EUGENE why not also other types like VT_INT?
          case AnMetric.VT_ULLONG:
          case AnMetric.VT_ADDRESS:
          case AnMetric.VT_DOUBLE:
            Object[] row = data[i];
            for (int j = 0, jsz = (row == null) ? 0 : row.length; j < jsz; j++) {
              ((AnObject) row[j]).showSign(true);
            }
            break;
        }
      }
      if (mode == CompareMode.CMP_RATIO) {
        switch (m.get_valtype()) {
          case AnMetric.VT_LLONG: // EUGENE why not also other types like VT_INT?
          case AnMetric.VT_ULLONG:
          case AnMetric.VT_DOUBLE:
            Object[] row = data[i];
            for (int j = 0, jsz = (row == null) ? 0 : row.length; j < jsz; j++) {
              ((AnObject) row[j]).showXtimes(true);
            }
            break;
        }
      }
      // "Bugs 16624403 and 19539622" (leave this string intact for searches)
      if (m.getUserName().equals("Block Covered %") || m.getUserName().equals("Instr Covered %")) {
        if (row_with_Total >= 0) {
          // to blank this item out, set to 0 and set showZero(false)
          data[i][row_with_Total] = new AnLong((long) 0);
          ((AnObject) data[i][row_with_Total]).showZero(false);
        }
      }
    }
    return data;
  }

  protected AnObject[] getTotals(int type, int subtype) {
    AnUtility.checkIPCOnWrongThread(false);
    Object[] raw_data = dbeGetTotals(type, subtype);
    AnUtility.checkIPCOnWrongThread(true);
    if ((raw_data == null) || (raw_data.length != 2)) {
      return null;
    }
    Object[] data = (Object[]) raw_data[1];
    AnMetric[] mlist = getSettings().getMetricList((Object[]) raw_data[0]);
    if ((mlist == null) || (mlist.length == 0) || (mlist.length != data.length)) {
      return null;
    }

    AnObject[] res = new AnObject[mlist.length];
    for (int i = 0; i < mlist.length; i++) {
      AnMetric m = mlist[i];
      switch (m.get_valtype()) {
        case AnMetric.VT_LLONG:
        case AnMetric.VT_ULLONG:
          res[i] = new AnLong(((long[]) data[i])[0]);
          break;
        case AnMetric.VT_ADDRESS:
          res[i] = new AnAddress(((long[]) data[i])[0]);
          break;
        case AnMetric.VT_DOUBLE:
          res[i] = new AnDouble(((double[]) data[i])[0]);
          break;
        case AnMetric.VT_LABEL:
          res[i] = new AnDouble((double) 0);
          break;
        default:
          System.err.println(
              String.format("Error in getTotals: unrecognized get_valtype(): %d", m.get_valtype()));
          res[i] = new AnDouble((double) 0);
          break;
      }
    }
    return res;
  }

  /*
   * Returns MaximumValues (Total and Max values), calculated by processData()
   */
  public Object[] getTotalMax() {
    return MaximumValues;
  }

  //    // Create the filter clause based on the selected objects
  //    public String createFilterClause() {
  //        int[] selected_indices = table.selected_indices;
  //        if (isOmpInxObj()) { // Use new API
  //            String clause = "";
  //            if (selected_indices.length <= 0) {
  //                return clause;
  //            }
  //            String IDs = "";
  //            for (int i = 0; i < selected_indices.length; i++) {
  //                long id = current_ids[selected_indices[i]];
  //                IDs = IDs + id;
  //                if (i + 1 < selected_indices.length) {
  //                    IDs = IDs + ",";
  //                }
  //            }
  //            clause = "(" + getTabName() + " IN (" + IDs + "))";
  //            return clause;
  //        }
  //        String clause = composeFilterClause(type, subtype, selected_indices);
  //        return clause;
  //    }
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
            AnUtility.dispatchOnAWorkerThread(
                new Runnable() {
                  @Override
                  public void run() {
                    window.getSelectedObject().setSelObj(from, type, subtype);
                    if (type == AnDisplay.DSP_Functions) {
                      window.getCalledByCallsFunctionsView().setComputed(false);
                      window.getCalledByCallsFunctionsView().computeOnAWorkerThread();
                    }
                    updateSummary(from);
                  }
                },
                "funclistdisp selection");
          } else // in other cases (eg. AT_SRC_ONLY), just update summary
          {
            // updateSummary(from);
            if (type == AnDisplay.DSP_Functions) {
              window.getCalledByCallsFunctionsView().setComputed(false);
              window.getCalledByCallsFunctionsView().computeOnAWorkerThread();
            }
            updateSummary(from);
          }

          break;
        case AnEvent.EVT_SORT: // Sorting
          // save current scroll location
          prevScroll = table.getScroll();
          getSettings()
              .getMetricsSetting()
              .setSortMetricByDType(this, ((Integer) event.getAux()).intValue(), getMetricMType());
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

  protected void deselectRaceStack(int subtype) { // FIXUP: REARCH
    // System.err.println("The selection is done in Source # " + subtype);
    if (subtype == 0) {
      return;
    }

    window.getDualSourceView().focusInHalf(subtype);
  }

  // Native methods from liber_dbe.so
  protected Object[] setFuncData(final long sel_func, final int type, final int subtype) {
    synchronized (IPC.lock) {
      window.IPC().send("setFuncDataV2");
      window.IPC().send(0);
      window.IPC().send(sel_func);
      window.IPC().send(type);
      window.IPC().send(subtype);
      return (Object[]) window.IPC().recvObject();
    }
  }

  //    protected void dbe_archive(final long ids[], final String locations[]) {
  //        synchronized (IPC.lock) {
  //            window.IPC().send("dbe_archive");
  //            window.IPC().send(ids);
  //            window.IPC().send(locations);
  //            window.IPC().recvVoid();
  //        }
  //    }
  //
  //    protected void dbeSetLocations(final String fnames[], final String locations[]) {
  //        synchronized (IPC.lock) {
  //            window.IPC().send("dbeSetLocation");
  //            window.IPC().send(fnames);
  //            window.IPC().send(locations);
  //            window.IPC().recvVoid();
  //        }
  //    }
  //
  //    protected Object[] dbeResolvedWith_setpath(final String path) {
  //        // obj[0] - list of old pathes
  //        // obj[1] - list of new pathes
  //        // obj[2] - list of ids
  //        synchronized (IPC.lock) {
  //            window.IPC().send("dbeResolvedWith_setpath");
  //            window.IPC().send(path);
  //            return (Object[]) window.IPC().recvObject();
  //       }
  //    }
  //
  //    protected Object[] dbeResolvedWith_pathmap(final String old_prefix, final String new_prefix)
  // {
  //        // obj[0] - list of old pathes
  //        // obj[1] - list of new pathes
  //        // obj[2] - list of ids
  //        synchronized (IPC.lock) {
  //            window.IPC().send("dbeResolvedWith_pathmap");
  //            window.IPC().send(old_prefix);
  //            window.IPC().send(new_prefix);
  //            return (Object[]) window.IPC().recvObject();
  //        }
  //    }
  protected Object[] getFuncList(final int type, final int subtype) {
    synchronized (IPC.lock) {
      window.IPC().send("getFuncList");
      window.IPC().send(0);
      window.IPC().send(type);
      window.IPC().send(subtype);
      return (Object[]) window.IPC().recvObject();
    }
  }

  protected Object[] dbeGetTotals(int dsptype, int subtype) {
    synchronized (IPC.lock) {
      IPC ipc = window.IPC();
      ipc.send("dbeGetTotals");
      ipc.send(0);
      ipc.send(dsptype);
      ipc.send(subtype);
      return (Object[]) ipc.recvObject();
    }
  }

  protected Object[] getHotMarks(final int type) {
    synchronized (IPC.lock) {
      window.IPC().send("getHotMarks");
      window.IPC().send(0);
      window.IPC().send(type);
      return (Object[]) window.IPC().recvObject();
    }
  }

  protected Object[] getHotMarksInc(final int type) {
    synchronized (IPC.lock) {
      window.IPC().send("getHotMarksInc");
      window.IPC().send(0);
      window.IPC().send(type);
      return (Object[]) window.IPC().recvObject();
    }
  }

  protected String[] getNames(final int type, final long sel_obj) {
    synchronized (IPC.lock) {
      window.IPC().send("getNames");
      window.IPC().send(0);
      window.IPC().send(type);
      window.IPC().send(sel_obj);
      return (String[]) window.IPC().recvObject();
    }
  }

  /**
   * Send request to get Names. Non-blocking IPC call. Caller should call ipcResult.getStrings() to
   * get the result
   *
   * @return IPCResult
   */
  public static IPCResult SendRequest_getNames(final int type, final long sel_obj) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getNames");
    ipcHandle.append(0);
    ipcHandle.append(type);
    ipcHandle.append(sel_obj);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getStrings() // blocking
    return ipcResult;
  }

  protected String composeFilterClause(
      final int type, final int subtype, final int[] selected_ids) {
    synchronized (IPC.lock) {
      window.IPC().send("composeFilterClause");
      window.IPC().send(0);
      window.IPC().send(type);
      window.IPC().send(subtype);
      window.IPC().send(selected_ids);
      return window.IPC().recvString();
    }
  }

  @Override
  public List<ExportFormat> getSupportedExportFormats() {
    List<ExportFormat> formats = new ArrayList<>();
    formats.add(ExportFormat.TEXT);
    formats.add(ExportFormat.HTML);
    formats.add(ExportFormat.CSV);
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return true;
  }

  @Override
  public List<Subview> getVisibleSubviews() {
    List<Subview> list = new ArrayList<>();
    list.add(window.getSelectedDetailsSubview());
    if (type == DSP_Functions || type == DSP_Lines || type == DSP_PCs) {
      list.add(window.getTimelineCallStackSubview());
      list.add(window.getIoCallStackSubview());
      list.add(window.getHeapCallStackSubview());
    }
    if (type == DSP_Functions) {
      if (!window.getSettings().getCompareModeSetting().comparingExperiments()) {
        list.add(window.getCalledByCallsFuncSubview());
      }
    }
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList<>();
    list.add(window.getSelectedDetailsSubview());
    return list;
  }

  @Override
  public void setComputed(boolean set) {
    super.setComputed(set);
    if (table != null) { // possible major change to table, so rebuild columns
      table.columnsSaved = false;
    }
  }
}
