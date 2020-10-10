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

import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnSplitPaneInternal;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;

/** Dual Source / Disassembly tab */
public final class SourceDisassemblyView extends AnDisplay
    implements ExportSupport, AnChangeListener {

  private JScrollPane errorPane = null;
  private SourceDisp upperSourcePanel;
  private DisasmDisp lowerSourcePanel;
  private JSplitPane splitPane;
  private boolean inCompute = false;
  private boolean initialized;
  private int focusedPart;

  /** Creates a new instance of SrcDisamDisp */
  public SourceDisassemblyView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_SourceDisassembly, AnVariable.HELP_TabsSrcDisassm);
    focusedPart = 1;
    initialized = false;
    setAccessibility(AnLocale.getString("Source/Disassembly"));
    AnEventManager.getInstance().addListener(this);
  }

  @Override
  public void requestFocus() {
    if (upperSourcePanel != null) {
      upperSourcePanel.requestFocus();
    }
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
    //        System.out.println("SourceDisassemblyView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        setComputed(false);
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
        if (selected) {
          filterChanged();
        }
        setComputed(false);
        if (selected) {
          computeOnAWorkerThread();
        }
        break;
      case SOURCE_FINDING_CHANGED:
        setComputed(false);
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
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.SRC_DIS
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.SEARCH_PATH
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.PATH_MAP
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.LIBRARY_VISIBILITY
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.METRICS) {
          setComputed(false);
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
  public void setComputed(boolean set) {
    super.setComputed(set);
    upperSourcePanel.setComputed(set);
    lowerSourcePanel.setComputed(set);
  }

  @Override
  public JPopupMenu getFilterPopup() {
    return upperSourcePanel.getFilterPopup();
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String text;
    text = upperSourcePanel.exportAsText(limit, format, delimiter);
    text = text + lowerSourcePanel.exportAsText(limit, format, delimiter);
    return text;
  }

  @Override
  protected void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    upperSourcePanel =
        new SourceDisp(
            window,
            AnDisplay.DSP_Source,
            null,
            AnDisplay.DSP_SourceDisassembly); // AnVariable.HELP_TabsSource);

    lowerSourcePanel =
        new DisasmDisp(
            window,
            AnDisplay.DSP_Disassembly,
            null,
            AnDisplay.DSP_SourceDisassembly); // AnVariable.HELP_TabsDisassembly);
    lowerSourcePanel.table.setWrapMetricNames(false);

    // make the both parts to be 0ed
    Dimension minDim = new Dimension(0, 0);
    upperSourcePanel.setMinimumSize(minDim);
    lowerSourcePanel.setMinimumSize(minDim);

    splitPane =
        new AnSplitPaneInternal(JSplitPane.VERTICAL_SPLIT, upperSourcePanel, lowerSourcePanel);

    // the both parts are equals
    splitPane.setResizeWeight(0.5);

    // put the split bar in the middle
    splitPane.setDividerLocation(0.5);

    //
    //        splitPane.setOneTouchExpandable(true);
    add(splitPane);
  }

  @Override
  public synchronized void doCompute() {
    AnUtility.checkIfOnAWTThread(false);
    if (!selected) {
      return; // Not selected
    }
    if (window
        .getSettings()
        .getCompareModeSetting()
        .comparingExperiments()) { // XXXX show error when compare is on, needs simplified, but the
      // situation is different for different views
      reset();
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
      if (splitPane != null) {
        splitPane.setVisible(false);
      }
      return;
    }
    if (errorPane != null) {
      errorPane.setVisible(false);
    }
    if (splitPane != null) {
      splitPane.setVisible(true);
    }
    if (inCompute) {
      return; // Weak synchronization
    }
    inCompute = true;

    if (!initialized) {
      initialized = true;
      splitPane.setDividerLocation(0.5);
    }

    if (true) // cur_pc_2 != parent.getSelPC(2) )
    {
      // System.err.println("Update low");
      lowerSourcePanel.setSelected(true);
      lowerSourcePanel.doCompute(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
    }

    if (true) // cur_pc_1 != parent.getSelPC(1) )
    {
      upperSourcePanel.setSelected(true);
      upperSourcePanel.doCompute(); // inside doCompute, on worker thread and synchronized
      // (AnVariable.mainFlowLock)
    }

    computed = true;
    inCompute = false;
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
    if (focusedPart == 1 && upperSourcePanel != null) {
      upperSourcePanel.updateToolBar();
    } else if (focusedPart == 2 && lowerSourcePanel != null) {
      lowerSourcePanel.updateToolBar();
    }
  }

  /** Remember which part is active (has focus) to continue search */
  public void focusInHalf(int stype) {
    if (stype == AnTable.AT_SRC) {
      focusedPart = 1;
    }
    if (stype == AnTable.AT_DIS) {
      focusedPart = 2;
    }
  }

  public void syncHalf(int stype) {
    if (focusedPart == 2 && stype == AnTable.AT_SRC) {
      upperSourcePanel.computeOnAWorkerThread();
    }
    if (focusedPart == 1 && stype == AnTable.AT_DIS) {
      lowerSourcePanel.computeOnAWorkerThread();
    }
  }

  //    @Override
  public void filterChanged() {
    //        System.out.println("SourceDisassemblyView:filterChanged: " + this);
    if (upperSourcePanel == null || lowerSourcePanel == null) {
      return;
    }
    upperSourcePanel.filterChanged();
    lowerSourcePanel.filterChanged();
  }

  @Override
  public void clearHistory() {
    if (upperSourcePanel == null || lowerSourcePanel == null) {
      return;
    }
    upperSourcePanel.clearHistory();
    lowerSourcePanel.clearHistory();
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  /** Find */
  @Override
  public int find(final String str, final boolean next, boolean caseSensitive) {
    int value = -1;
    if (focusedPart == 1) {
      value = upperSourcePanel.find(str, next, caseSensitive);
    } else if (focusedPart == 2) {
      value = lowerSourcePanel.find(str, next, caseSensitive);
    }
    return value;
  }

  @Override
  public java.util.List<ExportFormat> getSupportedExportFormats() {
    java.util.List<ExportFormat> formats = new ArrayList<ExportFormat>();
    formats.add(ExportFormat.JPG);
    formats.add(ExportFormat.TEXT);
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
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    return list;
  }
}
