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

package org.gprofng.mpmt.welcome;

import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public final class WelcomeView extends AnDisplay implements ExportSupport, AnChangeListener {

  private final WelcomePanel welcomePanel;

  // Constructor
  public WelcomeView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Welcome, AnVariable.HELP_Welcome);
    setLayout(new BorderLayout());
    welcomePanel = new WelcomePanel();
    add(welcomePanel, BorderLayout.CENTER);
    setAccessibility(AnLocale.getString("Welcome View"));
    AnEventManager.getInstance().addListener(this);
  }

  @Override
  public void requestFocus() {
    welcomePanel.requestFocus();
  }

  // Initialize GUI components
  @Override
  protected void initComponents() {}

  public String getShortDescr() {
    return "Welcome short description...";
  }

  public String getLongDescr() {
    return "<HTML><b>Welcome View</b><br>Welcome long"
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
    //        System.out.println("WelcomeDisp stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        computed = false;
        welcomePanel.enableActions(false);
        break;
      case EXPERIMENTS_LOADED_FAILED:
        welcomePanel.enableActions(true);
        break;
      case EXPERIMENTS_LOADED:
        if (selected) {
          computeOnAWorkerThread();
        }
        welcomePanel.enableActions(true);
        break;
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
        computed = false;
        if (selected) {
          computeOnAWorkerThread();
        }
        break;
      case FILTER_CHANGING:
      case FILTER_CHANGED:
      case SETTING_CHANGED:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case SETTING_CHANGING:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
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
  public void doCompute() {
    //        System.out.println("WelcomeDisp:doCompute: " + selected + " " + computed);
    AnUtility.checkIfOnAWTThread(false);
    if (!selected) {
      // Not selected
      return;
    }
    if (!computed) {
      // Not computed
      welcomePanel.doCompute();
      computed = true;
    }
  }

  @Override
  public List<ExportFormat> getSupportedExportFormats() {
    List<ExportFormat> formats = new ArrayList<>();
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return false;
  }
}
