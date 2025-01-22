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

import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnSplitPaneInternal;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public final class DualSourceView extends AnDisplay implements ExportSupport, AnChangeListener {
  private FuncListDisp upperSourcePanel;
  private FuncListDisp lowerSourcePanel;
  private JSplitPane splitPane;
  private int focusedPart;

  DualSourceView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_DualSource, AnVariable.HELP_TabsRaceDualSrc);
    focusedPart = 0;
    setAccessibility(AnLocale.getString("Dual Sources"));
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
    //        System.out.println("DualSourceView stateChanged:  " + e.getType());
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
  protected boolean supportsFindText() {
    return true;
  }

  // Find
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

  public void focusInHalf(int ahalf) {
    focusedPart = ahalf;
  }

  protected void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    upperSourcePanel =
        new SourceDisp(
            window,
            AnDisplay.DSP_Source,
            1,
            null,
            AnDisplay.DSP_DualSource); // AnVariable.HELP_TabsSource);

    lowerSourcePanel =
        new SourceDisp(
            window,
            AnDisplay.DSP_Source,
            2,
            null,
            AnDisplay.DSP_DualSource); // AnVariable.HELP_TabsSource);
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

    add(splitPane);
  }

  @Override
  public void doCompute() {
    long sel_obj;
    final long sel_func = 0;

    AnUtility.checkIfOnAWTThread(false);
    // Not selected
    if (!selected) {
      return;
    }

    if (focusedPart == 0) {
      splitPane.setDividerLocation(0.5);
      focusedPart = 1;
    }

    boolean selObjWasChanged = false;
    boolean wasPreviousSel = window.getSelectedObject().wasASelDone();
    int savedSelType = window.getSelectedObject().getSelType();
    int savedSelSubType = window.getSelectedObject().getSelSubType();
    long savedSelInd = window.getSelectedObject().getSelIndex();

    if (wasRaceAccessUpdated(2)) {
      if ((getSelPC(2) != 0)
          && (sel_obj = window.getSelectedObject().getObject(getSelPC(2), sel_func)) != 0L) {
        lowerSourcePanel.setSelected(true);
        window.getSelectedObject().setSelObj(sel_obj, DSP_Races /*type*/, subtype);
        selObjWasChanged = true;
        lowerSourcePanel.doCompute(); // inside doCompute, on worker thread and synchronized
        // (AnVariable.mainFlowLock)
      }
    }

    if (wasRaceAccessUpdated(1)) {
      if ((getSelPC(1) != 0)
          && (sel_obj = window.getSelectedObject().getObject(getSelPC(1), sel_func)) != 0L) {
        upperSourcePanel.setSelected(true);
        window.getSelectedObject().setSelObj(sel_obj, DSP_Races /*type*/, subtype);
        selObjWasChanged = true;
        upperSourcePanel.doCompute(); // inside doCompute, on worker thread and synchronized
        // (AnVariable.mainFlowLock)
      }
    }

    if (wasPreviousSel && selObjWasChanged) {
      window.getSelectedObject().setSelObj(savedSelInd, savedSelType, savedSelSubType);
    }

    computed = true;
  }

  private boolean wasRaceAccessUpdated_1 = false;
  private boolean wasRaceAccessUpdated_2 = false;
  private long pc_1;
  private long pc_2;

  public boolean wasRaceAccessUpdated(int aHalf) {
    boolean retValue = false;

    if (aHalf == 1) {
      retValue = wasRaceAccessUpdated_1;
      wasRaceAccessUpdated_1 = false;
    } else if (aHalf == 2) {
      retValue = wasRaceAccessUpdated_2;
      wasRaceAccessUpdated_2 = false;
    }

    return retValue;
  }

  public void setWasRaceAccessUpdated(int id, boolean value) {
    if (id == 1) {
      wasRaceAccessUpdated_1 = value;
    } else {
      wasRaceAccessUpdated_2 = value;
    }
  }

  public void setSelPC(int id, long value) {
    if (id == 1) {
      pc_1 = value;
    } else {
      pc_2 = value;
    }
  }

  public long getSelPC(int id) {
    if (id == 1) {
      return pc_1;
    } else {
      return pc_2;
    }
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String upperText = upperSourcePanel.exportAsText(limit, format, delimiter);
    String lowerText = lowerSourcePanel.exportAsText(limit, format, delimiter);
    return upperText + lowerText;
  }

  @Override
  public java.util.List<ExportFormat> getSupportedExportFormats() {
    java.util.List<ExportFormat> formats = new ArrayList<ExportFormat>();
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
    return list;
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
}
