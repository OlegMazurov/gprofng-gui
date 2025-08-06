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

package org.gprofng.mpmt.timeline2;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnEvent;
import org.gprofng.mpmt.AnListener;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.guitesting.GUITesting;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.settings.TimelineSetting;
import org.gprofng.mpmt.timeline_common.TimelinePanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

/**
 * Derived from MPITimelineDisp.java
 *
 * <p>computed==true: -> use current TL2DataSnapshot, but always refetch rows updated==true: -> use
 * current TL2DataSnapshot and reuse row data if possible (repaint will handle fetching row data
 * from a TL2DataSnapshot)
 */
public final class TimelineView extends AnDisplay implements ExportSupport, AnChangeListener {
  // Created and owned by this class:
  private final MetaExperiment metaExperiment;

  // components
  private JPanel callStackPane; // right hand tab pane
  private JPanel detailsInfoTabComp; // details
  private TL2ControlPanel controlPanel; // control panel in rhTabPane
  private JPanel leftTabPanel; // left tab panel
  private TimelinePanel timelinePanel; // generic timeline framework
  private TL2Drawer drawer; // timeline data + drawer
  // private MPITLDrawer drawer; // timeline data + drawer

  // computing state
  private boolean fetchingNewExperiments = false;

  // Constructor
  public TimelineView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_Timeline, AnVariable.HELP_TabsTimeline);

    // listen for color chooser changes
    window
        .getColorChooser()
        .getColorMap()
        .addAnListener(
            new AnListener() {
              @Override
              public void valueChanged(AnEvent e) {
                repaintTLDrawer();
              }
            });

    metaExperiment =
        new MetaExperiment(window, this); // YXXX potential race; leaks this in constructor
    instantiateTimelineDrawer(); // creates timelinePanel, drawer, etc.

    setAccessibility(AnLocale.getString("Timeline"));
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
    //        System.out.println("TimelineView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_NEW:
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
        computed = false;
        fetchingNewExperiments = true;
        if (e.getType() == AnChangeEvent.Type.EXPERIMENTS_LOADING_NEW) {
          edt_resetAll(); // YXXX includes resetting anWindow.getColorChooser() colors
        } else {
          edt_resetExperiments();
        }
        break;
      case EXPERIMENTS_LOADED_FAILED:
        // Nothing
        break;
      case EXPERIMENTS_LOADED:
        computed = false;
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
        edt_clearRows(); // clear previous row data, stops background IPC
        computed = false;
        break;
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
        break;
      case SETTING_CHANGING:
      case SETTING_CHANGED:
        AnSettingChangeEvent anSettingChangeEvent = (AnSettingChangeEvent) e.getSource();
        switch (anSettingChangeEvent.getType()) {
          case VIEW_MODE:
          case LIBRARY_VISIBILITY:
          case FORMAT:
            if (e.getType() == AnChangeEvent.Type.SETTING_CHANGING) {
              edt_clearRows(); // clear previous row data, stops background IPC
            }
            computed = false;
            if (selected) {
              computeOnAWorkerThread();
            }
            break;
          case TIMELINE: // stack height, group-by, which properties to show
            if (e.getType() == AnChangeEvent.Type.SETTING_CHANGING) {
              break; // these can happen quickly; just handle CHANGED event
            }
            List newSettings = (List) anSettingChangeEvent.getNewValue();
            boolean[] ret = edt_settingsChanged(newSettings);
            boolean newExperimentFetchNeeded = ret[0];
            boolean drawNeeded = ret[1];
            if (newExperimentFetchNeeded) {
              computed = false;
              if (selected) {
                computeOnAWorkerThread();
              }
            } else if (drawNeeded) {
              repaintTLDrawer();
            }
            break;
          default:
            break;
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
  public void requestFocus() {
    if (timelinePanel != null) {
      timelinePanel.requestFocus();
    }
  }

  @Override
  public JPopupMenu getFilterPopup() {
    return controlPanel.getFilterPopup();
  }

  protected void initComponentsXXXmpview() {}

  // Is current selected tab?
  @Override
  public void setSelected(final boolean set) { // called from AWT thread
    final JSplitPane splitPane; // YXXX RE_FACTOR

    super.setSelected(set);
    //        enableAction(set);

    // Show right pane if we get selected
    //	if (set) {
    //	    window.showRightPane();
    //	}
  }

  // tab to populate right-side info area
  public JComponent getStackInfoTabComp() {
    return callStackPane;
  }

  public JComponent getDetailsInfoTabComp() {
    return detailsInfoTabComp;
  }

  // name for tab in right-side info area
  public String getStackInfoTabName() {
    return AnLocale.getString("Call Stack - Timeline"); // YXXX javadoc
  }

  // name for tab in right-side info area
  public String getDetailsInfoTabName() {
    return AnLocale.getString("Selection Details"); // YXXX javadoc
  }

  class FocusPanel extends JPanel {
    @Override
    public void requestFocus() {
      if (controlPanel != null) {
        controlPanel.getStackViewPane().requestFocus();
      }
    }
  }

  // Minimal initialize of GUI components. Should not do IPC.
  // NOTE: called by super() init; other class variables not initted yet(!)
  @Override
  protected void initComponents() {
    this.setLayout(new BorderLayout());

    // right hand tab
    callStackPane = new FocusPanel();
    callStackPane.setLayout(new BorderLayout());
    detailsInfoTabComp = new JPanel();
    detailsInfoTabComp.setLayout(new BorderLayout());

    // main tab
    leftTabPanel = new JPanel();
    leftTabPanel.setLayout(new BorderLayout());

    // YXXX ally also set in AnWindow.java...
    String accessibleName = AnLocale.getString("Timeline");
    leftTabPanel.getAccessibleContext().setAccessibleName(accessibleName);
    leftTabPanel.getAccessibleContext().setAccessibleDescription(accessibleName);
    leftTabPanel.setToolTipText(accessibleName);

    this.add(leftTabPanel, BorderLayout.CENTER);
    this.revalidate();

    String ks_string;
    KeyStroke ks_def;

    ks_def = KeyboardShortcuts.contextMenuActionShortcut;
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_def);
    this.getActionMap()
        .put(
            ks_def,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.showContextMenu(ev);
                }
              }
            });

    ks_string = "FIRST";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectFirst();
                }
              }
            });

    ks_string = "ENTER";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel.getTimelinePanel()) {
                  controlPanel.getTimelinePanel().zoomToCaliper();
                }
              }
            });

    ks_string = "CENTERTIMESEL";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.findTimeMarker();
                }
              }
            });

    ks_string = "CENTEREVENTSEL";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectFind();
                }
              }
            });

    // --- plain arrows
    ks_string = "LEFT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectLeft();
                }
              }
            });

    ks_string = "RIGHT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectRight();
                }
              }
            });

    ks_string = "UP";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectUp();
                }
              }
            });

    ks_string = "DOWN";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectDown();
                }
              }
            });

    // --- shift ctrl arrows
    ks_string = "SHIFTCTRLLEFT";
    ks_def =
        KeyStroke.getKeyStroke(
            KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.pageLeft();
                }
              }
            });

    ks_string = "SHIFTCTRLRIGHT";
    ks_def =
        KeyStroke.getKeyStroke(
            KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.pageRight();
                }
              }
            });

    ks_string = "SHIFTCTRLUP";
    ks_def =
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.pageUp();
                }
              }
            });

    ks_string = "SHIFTCTRLDOWN";
    ks_def =
        KeyStroke.getKeyStroke(
            KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.pageDown();
                }
              }
            });

    // --- shift arrows
    ks_string = "SHIFTLEFT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectLeft(false, true);
                }
              }
            });

    ks_string = "SHIFTRIGHT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectRight(false, true);
                }
              }
            });

    ks_string = "SHIFTUP";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectUp(false, true);
                }
              }
            });

    ks_string = "SHIFTDOWN";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.selectDown(false, true);
                }
              }
            });

    // --- ctrl arrows
    ks_string = "CTRLLEFT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.stepLeft();
                }
              }
            });

    ks_string = "CTRLRIGHT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.stepRight();
                }
              }
            });

    ks_string = "CTRLUP";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.stepUp();
                }
              }
            });

    ks_string = "CTRLDOWN";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.stepDown();
                }
              }
            });

    // --- zoom
    ks_string = "ZOOMIN";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.genericZoomIn();
                }
              }
            });

    ks_string = "ZOOMINE";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.genericZoomIn();
                }
              }
            });

    ks_string = "ZOOMOUT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.genericZoomOut();
                }
              }
            });
    if (true) { // Thomas' race condition needs to be sorted out first
      // --- height
      ks_string = "VZOOMIN";
      ks_def =
          KeyStroke.getKeyStroke(
              KeyEvent.VK_EQUALS, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK, false);
      this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
      this.getActionMap()
          .put(
              ks_string,
              new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                  int sd = window.getSettings().getTimelineSetting().getTLStackDepth() + 1;
                  window.getSettings().getTimelineSetting().setTLStackDepth(TimelineView.this, sd);
                }
              });

      ks_string = "VZOOMINE";
      ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK, false);
      this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
      this.getActionMap()
          .put(
              ks_string,
              new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                  int sd = window.getSettings().getTimelineSetting().getTLStackDepth() + 1;
                  window.getSettings().getTimelineSetting().setTLStackDepth(TimelineView.this, sd);
                }
              });

      ks_string = "VZOOMOUT";
      ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_MASK, false);
      this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
      this.getActionMap()
          .put(
              ks_string,
              new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                  int sd = window.getSettings().getTimelineSetting().getTLStackDepth() - 1;
                  if (sd >= 0) {
                    window
                        .getSettings()
                        .getTimelineSetting()
                        .setTLStackDepth(TimelineView.this, sd);
                  }
                }
              });
    }
    ks_string = "ZOOMRESET";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_0, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.resetZoom();
                }
              }
            });

    ks_string = "ZOOMUNDO";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.undoZoom();
                }
              }
            });

    ks_string = "ZOOMREDO";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != controlPanel) {
                  controlPanel.redoZoom();
                }
              }
            });

    ks_string = "CENTERTIMEMARKER";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.setTimeMarker();
                }
              }
            });

    ks_string = "MOVETIMEMARKERLEFT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.moveTimeMarkerLeft();
                }
              }
            });

    ks_string = "MOVETIMEMARKERRIGHT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.moveTimeMarkerRight();
                }
              }
            });

    ks_string = "ADJUSTSELECTIONRANGELEFT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.moveTimeRangeToLeft();
                }
              }
            });

    ks_string = "ADJUSTSELECTIONRANGERIGHT";
    ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.SHIFT_MASK, false);
    this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def, ks_string);
    this.getActionMap()
        .put(
            ks_string,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent ev) {
                if (null != timelinePanel) {
                  timelinePanel.moveTimeRangeToRight();
                }
              }
            });

    //        ks_string = "COLORCHOOSER";
    //        ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, false);
    //        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def,
    // ks_string);
    //        this.getActionMap().put(ks_string, new AbstractAction() {
    //            @Override
    //            public void actionPerformed(ActionEvent ev) {
    //                window.getColorChooser().setVisible(true);
    //            }
    //        });
    //
    //        ks_string = "TIMELINESETTINGS";
    //        ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false);
    //        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def,
    // ks_string);
    //        this.getActionMap().put(ks_string, new AbstractAction() {
    //            @Override
    //            public void actionPerformed(ActionEvent ev) {
    //                window.getSettings().showDialog(window.getSettings().settingsTimelineIndex);
    //            }
    //        });
  }

  /**
   * Initialize the TimelinePanel and TL2ControlPanel objects and add them to the eventPane. Called
   * only from edt_threadDoCompute()
   */
  private void instantiateTimelineDrawer() { // called from AWT thread
    drawer = new TL2Drawer(metaExperiment.getDataFetcher());
    drawer
        .getDataAxisMaster()
        .setAbsRowMinHeights(drawer.getAbsoluteRowCount(), 100); // just some value to init calc

    timelinePanel =
        new TimelinePanel(
            drawer,
            TimelinePanel.MouseDragMode.MOUSE_GRABBING_MODE,
            TimelinePanel.MouseDragMode.MOUSE_SELECTION_MODE,
            TimelinePanel.MouseDragMode.MOUSE_GRABBING_MODE);
    drawer.setParent(timelinePanel); // YXXX yuck

    controlPanel = new TL2ControlPanel(window, timelinePanel, drawer);
    detailsInfoTabComp.add(controlPanel.getDetailsTextPanel());
    callStackPane.add(controlPanel.getDetailsPanel(), BorderLayout.CENTER);
    callStackPane.revalidate();

    JPanel toolbarPanel = new JPanel(new GridBagLayout());
    toolbarPanel.setMinimumSize(new Dimension(10, 10));
    toolbarPanel.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.weightx = 1.0;
    toolbarPanel.add(controlPanel.getToolbar(), gridBagConstraints);
    leftTabPanel.add(toolbarPanel, BorderLayout.NORTH);

    if (GUITesting.getInstance().isRunningUnderGUITesting()) {
      // Use a fixed size panel for GUI testing
      Dimension dim = new Dimension(820, 750);
      timelinePanel.setPreferredSize(dim);
      timelinePanel.setMinimumSize(dim);
      timelinePanel.setMaximumSize(dim);
      timelinePanel.setSize(dim);
      JPanel tmpPanel = new JPanel();
      tmpPanel.add(timelinePanel);
      timelinePanel.setBorder(BorderFactory.createLineBorder(Color.darkGray, 1));
      leftTabPanel.add(tmpPanel, BorderLayout.CENTER);
    } else {
      leftTabPanel.add(timelinePanel, BorderLayout.CENTER);
    }
    leftTabPanel.revalidate();
  }

  @Override
  public void doCompute() { // called by "Display_thread"
    AnUtility.checkIfOnAWTThread(false);
    if (!selected) {
      return;
    }
    if (!computed) {
      metaExperiment.ipcUpdateRows(); // IPC!
    }
    timelinePanel.repaint();
    computed = true;
  }

  // presentation settings delivered via AnSettingChangeEvent.Type.TIMELINE //can be AWT thread
  public boolean[] edt_settingsChanged(List new_tm) {
    // presentation:timeline settings were changed
    final String new_entity_prop_name;
    final int new_entity_button_num;
    final Settings.TLStack_align new_stack_align;
    final int new_vzoom_level;
    final int new_stack_frame_pixels;
    final long new_tldata_hidden_mask; // unchecked tldata boxes
    final List<String> new_tldata_cnames; // (samples, ...)
    final List<String> new_tldata_unames; // (samples, ...)
    final int new_tldata_names_version;
    final boolean tldata_show_states, tldata_show_charts;

    //                    final ArrayList new_tm;// populated by Presentation.java
    //                    new_tm = (ArrayList) event.getAux();
    new_entity_prop_name = (String) new_tm.get(0);
    new_entity_button_num = ((Integer) new_tm.get(1)).intValue();
    new_stack_align = (Settings.TLStack_align) new_tm.get(2);
    new_vzoom_level = ((Integer) new_tm.get(3)).intValue();
    new_tldata_hidden_mask = ((Long) new_tm.get(4)).longValue();
    new_tldata_cnames = (List<String>) new_tm.get(5);
    new_tldata_unames = (List<String>) new_tm.get(6);
    new_tldata_names_version = ((Integer) new_tm.get(7)).intValue();
    tldata_show_states = ((Boolean) new_tm.get(8)).booleanValue();
    tldata_show_charts = ((Boolean) new_tm.get(9)).booleanValue();
    new_stack_frame_pixels = ((Integer) new_tm.get(10)).intValue();

    TimelineSetting.EntityProp entityProp =
        AnWindow.getInstance()
            .getSettings()
            .getTimelineSetting()
            .getTl_entity_prop(new_entity_button_num);
    if (entityProp == null) {
      return new boolean[] {false, false}; // weird
    }

    // set up next snapshot
    boolean drawNeeded =
        edt_updatePresentationDrawingOptions(
            new_stack_align, new_vzoom_level, new_stack_frame_pixels);
    boolean fetchNeeded =
        metaExperiment.edt_updatePresentationFetchOptions(
            entityProp,
            new_tldata_hidden_mask,
            tldata_show_states,
            tldata_show_charts,
            new_tldata_cnames,
            new_tldata_unames,
            new_tldata_names_version);
    return new boolean[] {fetchNeeded, drawNeeded};
  }

  private boolean edt_updatePresentationDrawingOptions( // AWT only!
      final Settings.TLStack_align new_stack_align,
      final int new_vzoom_level,
      final int new_stack_frame_pixels) {
    boolean updated = false;
    if (drawer.event_drawer.getStackAlign() != new_stack_align) {
      drawer.event_drawer.setStackAlign(new_stack_align);
      updated = true;
    }
    if (drawer.event_drawer.getVZoomLevel() != new_vzoom_level) {
      drawer.event_drawer.setVZoomLevel(new_vzoom_level);
      drawer.event_drawer.edt_recalcRowOffsets();
      if (timelinePanel != null) {
        timelinePanel.edt_revalidateAll(false);
      }
      updated = true;
    }
    if (drawer.event_drawer.getStackFramePixels() != new_stack_frame_pixels) {
      drawer.event_drawer.setStackFramePixels(new_stack_frame_pixels);
      updated = true;
    }
    return updated;
  }

  // YXXX not @Override?
  public void setCPUIdleColor(final boolean set, final Color color) { // YXXX this is retarded
    drawer.event_drawer.setCPUIdleColor(set, color);
    repaintTLDrawer();
  }

  public void repaintTLDrawer() { // Called from data fetcher worker thread
    if (selected) {
      timelinePanel.repaint();
    }
  }

  // Reset row data, stops background IPC for row fetching (force a new data snapshot, recheck
  // experiments)
  private void edt_clearRows() { // e.g. filters, user/expert/machine mode, TL presentation
    // create a new TL2DataSnapshot because row definitions may have changed
    metaExperiment.resetCurrentActivity(); // triggers repaint
  }

  // Clear display
  private void edt_resetExperiments() { // e.g. add/drop experiments
    controlPanel.resetStackView(); // FIXUP: RE_ARCH
    metaExperiment.resetCurrentActivity(); // triggers repaint
    timelinePanel.edt_resetExperiments();
  }

  private void edt_resetAll() { // e.g. Open Experiment
    controlPanel.edt_resetAll();
    metaExperiment.edt_resetAll(); // resets TL2DataSnapshot. Also triggers repaint
    drawer.edt_resetAll();
    timelinePanel.edt_resetAll(); // may depend on TL2DataSnapshot being reset
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    if (GUITesting.getInstance().isRunningUnderGUITesting()) {
      System.out.println("timelinePanel dimensions: " + timelinePanel.getSize());
    }
    StringBuilder buf = new StringBuilder();
    //        buf.append(controlPanel.exportAsText(limit));
    //        buf.append(timelinePanel.exportAsText(limit));
    buf.append(drawer.event_drawer.exportAsText(limit));
    return buf.toString();
  }

  @Override
  public java.util.List<ExportFormat> getSupportedExportFormats() {
    java.util.List<ExportFormat> formats = new ArrayList<>();
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return false;
  }

  @Override
  public List<Subview> getVisibleSubviews() {
    List<Subview> list = new ArrayList<>();
    list.add(window.getSelectedDetailsSubviewTimeLine());
    list.add(window.getTimelineCallStackSubview());
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList<>();
    list.add(window.getSelectedDetailsSubviewTimeLine());
    list.add(window.getTimelineCallStackSubview());
    return list;
  }
}
