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

package org.gprofng.mpmt.overview;

import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.MiniFuncListDisp;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.swing.ToolTipManager;

public final class OverviewView extends AnDisplay implements ExportSupport, AnChangeListener {

  private MiniFuncListDisp miniFuncListDisp = null;
  private String lastSortMetrics = null;
  private OverviewPanel overviewPanel;
  private boolean metricsHaveChanged = false;
  private int defaultToolTipDismissDelay = 0;
  private static final int ToolTipDismissDelay = 10000;
  private Thread miniFuncListDispComputeThread;
  private Object miniFuncListDispComputeThreadLock = new Object();

  // Constructor
  public OverviewView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Overview, AnVariable.HELP_Overview);
    setAccessibility(AnLocale.getString("Overview"));
    AnEventManager.getInstance().addListener(this);
    miniFuncListDisp = constructMiniFuncListDisp();
    defaultToolTipDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
  }

  @Override
  public void requestFocus() {
    overviewPanel.requestFocus(); // To change body of generated methods, choose Tools | Templates.
  }

  public String getShortDescr() {
    return "Overview short description...";
  }

  public String getLongDescr() {
    return "<HTML><b>Overview View</b><br>Overview long"
               + " description......<br>..............................................<br>..............................................</HTML>";
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
    //        System.out.println("OverviewView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        setComputed(false);
        clear();
        clearHistory();
        String[][] groups = (String[][]) e.getSource();
        if (groups.length > 0) {
          overviewPanel.initLoadingExperiments(groups, true);
        }
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
        setComputed(false);
        if (selected) {
          computeOnAWorkerThread();
        }
        miniFuncListDisp.setComputed(false);
        if (selected) {
          computeMiniFuncListDisp();
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
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.LIBRARY_VISIBILITY
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.COMPARE_MODE) {
          setComputed(false);
          if (selected) {
            computeOnAWorkerThread();
          }
          miniFuncListDisp.setComputed(false);
          if (selected) {
            computeMiniFuncListDisp();
          }
        }
        if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.METRICS) {
          if (anSettingChangeEvent.getOriginalSource() != this) {
            metricsHaveChanged = true;
            if (selected) {
              computeOnAWorkerThread();
            }
          }
          miniFuncListDisp.setComputed(false);
          if (selected) {
            computeMiniFuncListDisp();
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

  private void computeMiniFuncListDisp() {
    synchronized (miniFuncListDispComputeThreadLock) {
      if (miniFuncListDispComputeThread != null && miniFuncListDispComputeThread.isAlive()) {
        miniFuncListDispComputeThread.interrupt();
      }
      miniFuncListDispComputeThread =
          AnUtility.dispatchOnAWorkerThread(
              new Runnable() {
                @Override
                public void run() {
                  boolean interrupted = false;
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException ie) {
                    interrupted = true;
                  }
                  ;
                  if (!interrupted) {
                    miniFuncListDisp.computeOnAWorkerThread();
                  }
                }
              },
              "computeMiniFuncListDisp");
    }
  }

  private MiniFuncListDisp constructMiniFuncListDisp() {
    MiniFuncListDisp miniFLD =
        new MiniFuncListDisp(window, AnDisplay.DSP_Functions, AnVariable.HELP_Overview); //
    Dimension dim = new java.awt.Dimension(100, overviewPanel.getFont().getSize() * 4 + 20);
    miniFLD.setPreferredSize(dim);
    AccessibleContext context = miniFLD.getAccessibleContext();
    context.setAccessibleName(AnLocale.getString("Functions"));
    context.setAccessibleDescription(AnLocale.getString("List of functions"));

    return miniFLD;
  }

  public MiniFuncListDisp getMiniFuncListDisp() {
    return miniFuncListDisp;
  }

  private void saveLastSortMetrics() {
    lastSortMetrics =
        AnWindow.getInstance().getSettings().getMetricsSetting().getSortColumnMetricDisplayName(0);
  }

  private void restoreLastSortMetrics() {
    if (lastSortMetrics != null) {
      String sortMetrics =
          AnWindow.getInstance()
              .getSettings()
              .getMetricsSetting()
              .getSortColumnMetricDisplayName(0);
      if (sortMetrics != null && !sortMetrics.equals(lastSortMetrics)) {
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .setSortColumnName(lastSortMetrics, 0);
      }
    }
  }

  @Override
  public void setSelected(boolean set) {
    if (isSelected() == set) {
      return;
    }
    super.setSelected(set);
    if (set) {
      saveLastSortMetrics();
      ToolTipManager.sharedInstance().setDismissDelay(ToolTipDismissDelay);
    } else {
      restoreLastSortMetrics();
      ToolTipManager.sharedInstance().setDismissDelay(defaultToolTipDismissDelay);
    }

    miniFuncListDisp.setSelected(set);
  }

  @Override
  public void setComputed(boolean set) {
    super.setComputed(set);
    miniFuncListDisp.setComputed(set);
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {
    setLayout(new BorderLayout());
    overviewPanel = new OverviewPanel(window, this);
    add(overviewPanel, BorderLayout.CENTER);
  }

  // Compute & update Statistics display
  @Override
  public void doCompute() {
    AnUtility.checkIfOnAWTThread(false);
    if (!selected) {
      return;
    }
    if (computed) {
      if (metricsHaveChanged) {
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              @Override
              public void run() {
                overviewPanel.updateMetricTree(
                    AnWindow.getInstance()
                        .getSettings()
                        .getMetricsSetting()
                        .getMetricStates()
                        .getMetricStateList());
              }
            });
        metricsHaveChanged = false;
      }
    }
    if (!computed) {
      overviewPanel.initLoadingExperiments(AnWindow.getInstance().getExperimentGroups(), false);
      overviewPanel.refreshMetricTree();
      saveLastSortMetrics();
    }
    computeMiniFuncListDisp();
    computed = true;
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    StringBuilder buf = new StringBuilder();
    buf.append(overviewPanel.dump());
    buf.append(miniFuncListDisp.exportAsText(0, format, delimiter));
    return buf.toString();
  }

  @Override
  public List<ExportFormat> getSupportedExportFormats() {
    List<ExportFormat> formats = new ArrayList<ExportFormat>();
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return false;
  }
}
