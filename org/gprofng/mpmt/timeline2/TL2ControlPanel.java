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

package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnEvent;
import org.gprofng.mpmt.AnListener;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.control_panel.CPDetailsScrollPane;
import org.gprofng.mpmt.control_panel.CPToolbar;
import org.gprofng.mpmt.control_panel.CPToolbarHandler;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.filter.CustomFilterAction;
import org.gprofng.mpmt.filter.FilterClause;
import org.gprofng.mpmt.filter.RedoFilterAction;
import org.gprofng.mpmt.filter.RemoveAllFilterAction;
import org.gprofng.mpmt.filter.UndoFilterAction;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.settings.TimelineSetting;
import org.gprofng.mpmt.statecolors.AnColorChooser;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.statecolors.StackView;
import org.gprofng.mpmt.statecolors.StackViewState;
import org.gprofng.mpmt.statecolors.StateColorMap;
import org.gprofng.mpmt.timeline.events.DetailsIPC;
import org.gprofng.mpmt.timeline.events.ProfileEvent;
import org.gprofng.mpmt.timeline2.TL2DataSnapshot.*;
import org.gprofng.mpmt.timeline_common.TimelineCaliper;
import org.gprofng.mpmt.timeline_common.TimelinePanel;
import org.gprofng.mpmt.timeline_common.TimelineSelectionCaliperEvent;
import org.gprofng.mpmt.timeline_common.TimelineSelectionGenericEvent;
import org.gprofng.mpmt.timeline_common.TimelineSelectionHistoryEvent;
import org.gprofng.mpmt.timeline_common.TimelineSelectionListener;
import org.gprofng.mpmt.toolbar.ToolBarFiller;
import org.gprofng.mpmt.toolbar.ToolBarSeparator;
import org.gprofng.mpmt.util.gui.AnJPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * The TL2ControlPanel provides the visual controls for the Chart as well as the code that allows
 * user interaction with the chart.
 *
 * <p>The Panel contains the details display, and a toolbar with color and navigation controls.
 *
 * <p>The top level code for zooming panning, selection, and detail display is in ChartControlPanel.
 */
public class TL2ControlPanel extends AnJPanel
    implements AnListener,
        MouseListener,
        ListSelectionListener,
        CPToolbarHandler,
        AnChangeListener {

  // provided by user
  private final AnWindow window;
  private final TimelinePanel timeline;
  private final AnColorChooser color_chooser;
  private final StateColorMap clmap;
  // components
  private final TL2Drawer tl2_drawer;
  private final JComponent customComponentsPanel;
  private final CPToolbar cpToolbar;
  private final JPanel detailsPanel; // Details Panel top
  private JPanel detailsTextPanel;
  private JPanel stackViewPane; // Details Panel
  private StackView stack_view; // Details Panel
  private final EventDetails eventDetails; // Details Panel text, layout
  private CaliperZoom caliperZoom;
  private SetFilterVisibleTimeRangeOnly setFilterVisibleTimeRangeOnly;
  private SetFilterSelectedTimeRangeOnly setFilterSelectedTimeRangeOnly;
  private SetFilterExcludeBefore setFilterExcludeBefore;
  private SetFilterExcludeAfter setFilterExcludeAfter;
  private SetFilterSelectedRowsOnly setFilterSelectedRowsOnly;
  private SetFilterUnselectedRowsOnly setFilterUnselectedRowsOnly;
  private SetFilterMicrostate setFilterMicrostate;
  private SetFilterNotMicrostate setFilterNotMicrostate;
  private RemoveAllFilterAction removeAllFilters;
  private CustomFilterAction customFilter;
  private UndoFilterAction undoFilter;
  private RedoFilterAction redoFilter;
  private UndoZoomAction undoZoomAction;
  private RedoZoomAction redoZoomAction;
  private SelectNextUp selectNextUpAction;
  private SelectNextDown selectNextDownAction;
  private SelectNextLeft selectNextLeftAction;
  private SelectNextRight selectNextRightAction;
  private SelectFind selectFindAction;

  private final JComboBox byComboBox;
  private final JSpinner spinner;
  // state
  private boolean stackViewEnabled;

  /** Creates a new instance of TL2ControlPanel */
  public TL2ControlPanel(
      final AnWindow anWindow, final TimelinePanel timeline, final TL2Drawer tl2_drawer) {
    super();
    cpToolbar = new CPToolbar(this, true, true, true);
    cpToolbar.add(
        new ToolBarSeparator(7, 6),
        null,
        12); // FIXUP: hack to insert separator just before color chooser
    cpToolbar.add(AnWindow.getInstance().getSettingsAction().createActionButton());
    // Group Data By drop-down;
    JLabel groupLabel = new JLabel(AnLocale.getString("Group Data by:"));
    groupLabel.setDisplayedMnemonic(AnLocale.getString('y', "MN_Timeline_GroupDataby"));
    groupLabel.setFont(groupLabel.getFont().deriveFont(Font.PLAIN));
    groupLabel.setToolTipText(AnLocale.getString("Group data by..."));
    byComboBox = new JComboBox();
    groupLabel.setLabelFor(byComboBox);
    byComboBox.setFont(byComboBox.getFont().deriveFont(Font.PLAIN));
    TimelineSetting.EntityProp entProp;
    for (int ii = 0;
        null != (entProp = anWindow.getSettings().getTimelineSetting().getTl_entity_prop(ii));
        ii++) {
      byComboBox.addItem(entProp.prop_uname);
    }
    byComboBox.setToolTipText(groupLabel.getToolTipText());
    AnUtility.setAccessibleContext(byComboBox.getAccessibleContext(), byComboBox.getToolTipText());
    byComboBox.setPreferredSize(new Dimension(byComboBox.getPreferredSize().width, 19));
    byComboBox.setMaximumSize(byComboBox.getPreferredSize());
    byComboBox.setSelectedIndex(
        anWindow.getSettings().getTimelineSetting().getTl_entity_selected_btn());
    byComboBox.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            int index = byComboBox.getSelectedIndex();
            window
                .getSettings()
                .getTimelineSetting()
                .setTLGroupDataBySelectedButtonIndex(TL2ControlPanel.this, index);
          }

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    cpToolbar.add(new ToolBarSeparator(8, 10));
    cpToolbar.add(groupLabel);
    cpToolbar.add(new ToolBarFiller(2));
    cpToolbar.add(byComboBox);

    // Stack Depth
    if (true) { // YXXXX TBR stuff
      spinner = null;
    } else {
      JLabel spinnerLabel = new JLabel(AnLocale.getString("Stack Depth:"));
      spinnerLabel.setFont(spinnerLabel.getFont().deriveFont(Font.PLAIN));
      spinnerLabel.setToolTipText(AnLocale.getString("Maximum stack depth"));
      cpToolbar.add(new ToolBarFiller(7));
      cpToolbar.add(spinnerLabel);
      spinner = new JSpinner();
      spinner.setBorder(BorderFactory.createLineBorder(new Color(122, 138, 153)));
      spinner.setToolTipText(AnLocale.getString("Maximum stack depth"));
      spinner.setValue(anWindow.getSettings().getTimelineSetting().getTLStackDepth());
      spinner.addChangeListener(
          new ChangeListener() {
            final TL2Drawer drawer = tl2_drawer;

            @Override
            public void stateChanged(ChangeEvent e) {
              Object value = spinner.getValue();
              if (value instanceof Integer) {
                int sd = ((Integer) value).intValue();
                if (sd >= 0) {
                  window
                      .getSettings()
                      .getTimelineSetting()
                      .setTLStackDepth(TL2ControlPanel.this, sd);
                } else {
                  sd = window.getSettings().getTimelineSetting().getTLStackDepth();
                  spinner.setValue(sd);
                }
              }
            }
          });
      JComponent editor = ((JSpinner.DefaultEditor) spinner.getEditor());
      Dimension size = editor.getPreferredSize();
      editor.setPreferredSize(new Dimension(20, size.height));
      spinner.setPreferredSize(new Dimension(spinner.getPreferredSize().width, 19));
      spinner.setMaximumSize(spinner.getPreferredSize());
      cpToolbar.add(new ToolBarFiller(2));
      cpToolbar.add(spinner);
    }

    // this.cpFilterPanel = null;
    this.timeline = timeline;
    this.tl2_drawer = tl2_drawer;
    this.color_chooser = anWindow.getColorChooser();
    this.clmap = color_chooser.getColorMap();
    this.window = anWindow;
    this.customComponentsPanel = initializeCustomComponents();
    this.detailsPanel = initializeDetailComponents();
    //        overlaycursor = new Cursor(Cursor.HAND_CURSOR);

    initializeListeners(timeline);

    clmap.addAnListener(this); // YXXX leaks this in constructor
    eventDetails = new EventDetails(anWindow);
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
      case EXPERIMENTS_LOADED_FAILED:
      case EXPERIMENTS_LOADED:
      case FILTER_CHANGED:
      case FILTER_CHANGING:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SETTING_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
        break;
      case SETTING_CHANGING:
        AnSettingChangeEvent anSettingChangeEvent = (AnSettingChangeEvent) e.getSource();
        if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.TIMELINE) {
          //                    System.out.println("TL2ControlPanel getOriginalSource: " +
          // anSettingChangeEvent.getOriginalSource());
          if (anSettingChangeEvent.getOriginalSource() == TL2ControlPanel.this) {
            break;
          }
          //                    System.out.println("TL2ControlPanel getOriginalSource:
          // doIt...............");
          // Update states of Group Data and Stack Depth in control panel
          List new_tm = (List) anSettingChangeEvent.getNewValue();
          int new_entity_button_num = ((Integer) new_tm.get(1)).intValue();
          int new_stack_depth = ((Integer) new_tm.get(3)).intValue();
          //                    int new_stack_frame_pixels = ((Integer) new_tm.get(10)).intValue();
          updateDataGrouping(new_entity_button_num);
          updateStackDepth(new_stack_depth);
          //                    updateStackFramePixels(new_stack_frame_pixels);
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

  private void debug() {}

  public void edt_resetAll() {
    resetStackView();
    int sd = window.getSettings().getTimelineSetting().getTLStackDepth();
    if (spinner != null) {
      spinner.setValue(sd); // 18294015 workaround; TBR
    }
    if (cpToolbar != null) {
      cpToolbar.setVNumberOfLevels(TimelineDraw.TIMELINE_MAX_VZOOM_LEVEL);
      cpToolbar.setVZoomLevel(sd);
    }
  }

  // --- allow CP layout to access components
  public void updateStackDepth(int value) {
    if (spinner != null) {
      if (((Integer) spinner.getValue()) != value) {
        ChangeListener ch = spinner.getChangeListeners()[0];
        spinner.removeChangeListener(ch);
        spinner.setValue(value);
        spinner.addChangeListener(ch);
      }
    }
    if (cpToolbar != null) {
      cpToolbar.setVZoomLevel(value);
    }
  }

  public void updateDataGrouping(int index) {
    if (byComboBox != null) {
      if (byComboBox.getSelectedIndex() != index) {
        PopupMenuListener ch = byComboBox.getPopupMenuListeners()[0];
        byComboBox.removePopupMenuListener(ch);
        byComboBox.setSelectedIndex(index);
        byComboBox.addPopupMenuListener(ch);
      }
    }
  }

  public JPopupMenu getFilterPopup() {
    return createPopupMenu(true);
  }

  public JComponent getToolbar() {
    return cpToolbar;
  }

  public JComponent getCustomPanel() {
    return customComponentsPanel;
  }

  public JComponent getDetailsPanel() {
    return detailsPanel;
  }

  public JComponent getDetailsTextPanel() {
    if (detailsTextPanel == null) {
      detailsTextPanel = new AnJPanel();
    }
    return detailsTextPanel;
  }

  public JComponent getStackViewPane() {
    return stackViewPane;
  }

  /** Initialize visual components */
  private void initializeListeners(final TimelinePanel timeline) {
    timeline.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) { // YXXX overkill, nonspecific
            // book-keeping for zoom revert
            //                cpToolbar.enableRevertButton(true);
            int tmp;
            tmp = timeline.getZoomTimeLevelMax();
            cpToolbar.setNumberOfLevels(tmp);
            tmp = timeline.getZoomTimeLevel();
            cpToolbar.setZoomLevel(tmp);

            tmp = TimelineDraw.TIMELINE_MAX_VZOOM_LEVEL;
            cpToolbar.setVNumberOfLevels(tmp);
            tmp = window.getSettings().getTimelineSetting().getTLStackDepth();
            cpToolbar.setVZoomLevel(tmp);
          }
        });

    tl2_drawer.addSelectionListener(
        new TimelineSelectionListener() {
          @Override
          public void valueChanged(TimelineSelectionGenericEvent ge) {
            if (ge instanceof TimelineSelectionHistoryEvent) {
              TimelineSelectionHistoryEvent e = (TimelineSelectionHistoryEvent) ge;
              cpToolbar.enableZoomUndoButton(!e.atMin);
              cpToolbar.enableZoomRedoButton(!e.atMax);
              undoZoomAction.enableMenuItem(!e.atMin);
              redoZoomAction.enableMenuItem(!e.atMax);
            } else if (ge instanceof TimelineSelectionEvent) {
              TimelineSelectionEvent e = (TimelineSelectionEvent) ge;
              cpToolbar.enableNavigationButtons(e.left, e.right, e.up, e.down, e.find);
              setFilterMicrostate.enableMenuItem(e);
              setFilterNotMicrostate.enableMenuItem(e);
              enableNavigationMenuItems(e.left, e.right, e.up, e.down, e.find);
              showStack(e);
              setDetailText(e);
            } else if (ge instanceof TimelineSelectionCaliperEvent) {
              TimelineSelectionCaliperEvent e = (TimelineSelectionCaliperEvent) ge;
              cpToolbar.enableZoomToCaliperButton(!e.caliper.isSingleEvent());
              boolean enableRange = (e.caliper != null && !e.caliper.isSingleEvent());
              setFilterSelectedTimeRangeOnly.enableMenuItem(e.caliper != null);
              setFilterExcludeBefore.enableMenuItem(enableRange);
              setFilterExcludeAfter.enableMenuItem(enableRange);
              caliperZoom.enableMenuItem(enableRange);
            } else if (ge instanceof TimelineSelectionRowEvent) {
              TimelineSelectionRowEvent event = (TimelineSelectionRowEvent) ge;
              setFilterSelectedRowsOnly.enableMenuItem(event.rowsSelected);
              setFilterUnselectedRowsOnly.enableMenuItem(event.rowsSelected);
            }
          }
        });

    // mouse modes
    timeline.setRubberbandIs2D(false);

    // Init...
    initNavigationMenuItems();
    initFilterMenuItems();
    popupListener = new PopupListener();
    stackViewPopupListener = stack_view.new StackViewPopupListener(window);
    timeline.addMouseListener(popupListener);
  }

  // --- CPToolbarHandler API ---
  @Override
  public void selectFind() {
    timeline.zoomCenterSelection();
  }

  @Override
  public void selectFirst() {
    tl2_drawer.selectFirst();
  }

  @Override
  public void selectLeft() {
    tl2_drawer.selectLeft();
  }

  @Override
  public void selectRight() {
    tl2_drawer.selectRight();
  }

  @Override
  public void selectDown() {
    tl2_drawer.selectDown();
  }

  @Override
  public void selectUp() {
    tl2_drawer.selectUp();
  }

  public void selectLeft(boolean ctrl, boolean shift) {
    tl2_drawer.selectLeft(ctrl, shift);
  }

  public void selectRight(boolean ctrl, boolean shift) {
    tl2_drawer.selectRight(ctrl, shift);
  }

  public void selectDown(boolean ctrl, boolean shift) {
    tl2_drawer.selectDown(ctrl, shift);
  }

  public void selectUp(boolean ctrl, boolean shift) {
    tl2_drawer.selectUp(ctrl, shift);
  }

  @Override
  public void toggleMouseMode() {
    if (timeline.getMouseMode() == TimelinePanel.MouseDragMode.MOUSE_RUBBERBANDING_MODE) {
      timeline.setMouseMode(TimelinePanel.MouseDragMode.MOUSE_GRABBING_MODE);
    } else {
      timeline.setMouseMode(TimelinePanel.MouseDragMode.MOUSE_RUBBERBANDING_MODE);
    }
  }

  @Override
  public void zoomToCaliper() {
    timeline.zoomToCaliper();
  }

  @Override
  public void genericZoomToLevel(int n) {
    timeline.zoomTimeToLevel(n);
  }

  @Override
  public void genericZoomIn() {
    timeline.zoomTimeIn();
  }

  @Override
  public void genericZoomOut() {
    timeline.zoomTimeOut();
  }

  @Override
  public int getZoomLevelMax() {
    return 20; // YXXX prototype
    //        return timeline.getZoomTimeLevelMax();
  }

  @Override
  public void genericVZoomToLevel(int n) { // YXXX implement this?
    timeline.zoomHistoryAdd(); // needed because timeline isn't the owner of these settings
    window.getSettings().getTimelineSetting().setTLStackDepth(TL2ControlPanel.this, n);
    cpToolbar.setVZoomLevel(n); // YXXX fixup
  }

  @Override
  public void genericVZoomIn() {
    timeline.zoomHistoryAdd(); // needed because timeline isn't the owner of these settings
    int zl = window.getSettings().getTimelineSetting().getTLStackDepth();
    zl++;
    window.getSettings().getTimelineSetting().setTLStackDepth(TL2ControlPanel.this, zl);
    cpToolbar.setVZoomLevel(zl); // YXXX fixup
  }

  @Override
  public void genericVZoomOut() {
    timeline.zoomHistoryAdd(); // needed because timeline isn't the owner of these settings
    int zl = window.getSettings().getTimelineSetting().getTLStackDepth();
    zl--;
    if (zl < 0) {
      zl = 0; // YXXX fixup
    }
    window.getSettings().getTimelineSetting().setTLStackDepth(TL2ControlPanel.this, zl);
    cpToolbar.setVZoomLevel(zl); // YXXX fixup
  }

  @Override
  public void resetZoom() {
    timeline.resetZoom();
    // updateZoomSliders(); //YXXX remove this let widget update do it
  }

  @Override
  public void undoZoom() {
    timeline.undoZoom();
  }

  @Override
  public void redoZoom() {
    timeline.redoZoom();
  }

  @Override
  public void chooseColors() {
    color_chooser.setVisible(true);
  }

  /**
   * Shows Context Menu (reaction on Shift-F10)
   *
   * @param ev
   */
  @Override
  public void showContextMenu(ActionEvent ev) {
    int id = KeyEvent.VK_F10;
    long when = 0; // System.currentTimeMillis();
    int modifiers = InputEvent.SHIFT_MASK;
    int x = 120;
    int y = 40;
    int clickCount = 1;
    boolean popupTrigger = true;
    int button = MouseEvent.BUTTON1;
    MouseEvent me = null;
    me = new MouseEvent(timeline, id, when, modifiers, x, y, clickCount, popupTrigger, button);
    try {
      popupListener.maybeShowPopup(me);
    } catch (java.awt.IllegalComponentStateException icse) {
      // System.out.println("showContextMenu(): " + icse);
    }
  }

  /** Opens "Set Data Presentation" dialog */
  public void editProperties(String tabname) {
    window.getSettings().showDialog(window.getSettings().settingsTimelineIndex);
  }

  // --- used by menus ---
  // Changes timeline time zoom initiated by this class
  private void zoomTimeToLevel(int level) {
    timeline.zoomTimeToLevel(level);
  }

  // Change timeline process zoom initiated by this class
  private void zoomProcessToLevel(int level) {
    timeline.zoomProcessToLevel(level);
  }

  /** Initialize timeline specific components */
  // called by parent's initializeComponents()
  private JComponent initializeCustomComponents() { // YXXX uh, supposed to ovverride parent or not?
    // the component we're going to return
    JPanel customBoxesPanel = new AnJPanel();
    customBoxesPanel.setLayout(new BoxLayout(customBoxesPanel, BoxLayout.Y_AXIS));

    // --- YXXX....

    // customBoxesPanel.add(limiterPanel);

    // limiterPanel.revalidate();
    customBoxesPanel.revalidate();
    // return customBoxesPanel;
    return null;
  }

  /** Initialize component to show details */
  private JPanel initializeDetailComponents() {
    // the component we're going to return:
    JPanel tmpDetailsPanel = new AnJPanel();
    tmpDetailsPanel.setLayout(new BorderLayout());
    //        tmpDetailsPanel.setBorder(
    //                new TitledBorder(AnLocale.getString("Details")));

    // panel for text details
    detailsTextPanel = (JPanel) getDetailsTextPanel();
    detailsTextPanel.registerKeyboardAction(
        new Analyzer.HelpAction(AnVariable.HELP_TabsSummary),
        "help",
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    detailsTextPanel.setLayout(new BoxLayout(detailsTextPanel, BoxLayout.Y_AXIS));
    // horizontally-constrained scroll pane for detailsTextPanel
    CPDetailsScrollPane detailsScrollPane = new CPDetailsScrollPane(detailsTextPanel);
    detailsScrollPane.setBorder(BorderFactory.createEmptyBorder());
    //        tmpDetailsPanel.add(detailsScrollPane,BorderLayout.NORTH);

    // ---- call stack
    // panel for stack_view and stackEnableButton
    stackViewPane = new FocusPanel();
    stackViewPane.setBackground(Color.WHITE);
    stackViewPane.setVisible(false);

    // stack component
    stack_view = new StackView(clmap);
    stack_view.addMouseListener(this);
    stack_view.addListSelectionListener(this); // XXXmpview, why is this needed?
    stackViewEnabled = false;
    // YXXX selState=null;

    stackViewPane.revalidate();
    tmpDetailsPanel.add(stackViewPane, BorderLayout.CENTER);

    tmpDetailsPanel.revalidate();
    return tmpDetailsPanel;
  }

  class FocusPanel extends JPanel {
    @Override
    public void requestFocus() {
      if (stack_view != null) {
        stack_view.requestFocus();
      }
    }
  }

  /** Set the detail text */
  void setDetailText(TimelineSelectionEvent evt) {
    JPanel tmpPanel;
    JLabel lbl;
    final String title;

    if (evt == null) {
      title = AnLocale.getString("Nothing Selected");
      lbl = new JLabel(AnLocale.getString("Nothing selected"));
      tmpPanel = new AnJPanel();
      tmpPanel.add(lbl);
    } else {
      title = AnLocale.getString("Timelinexx");
      tmpPanel = eventDetails.setEvent(evt);
    }
    tmpPanel.revalidate();

    detailsTextPanel.removeAll();
    detailsTextPanel.add(tmpPanel);
    detailsTextPanel.revalidate();
    detailsTextPanel.repaint();
  }

  private void loadStacks() {
    //        mpi_drawer.getProvider().loadStacks();
    //        FunctionDetail fdet = new FunctionDetail(selState);
    //        long stackId=fdet.getStackId();
    // YXXX        showStack(stackId);
  }

  private void enableStackView() {
    // hide stackViewPane until data is ready
    stackViewPane.setVisible(false);
    stackViewPane.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(2, 2, 2, 2);
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    stackViewPane.add(stack_view, gridBagConstraints);
    stackViewPane.revalidate();
    stackViewPane.repaint();
    stackViewEnabled = true;
  }

  private void showStack(TimelineSelectionEvent e) {
    final long stack_id = (e == null) ? DetailsIPC.INVALID_STACK_ID : e.stackId;
    if (stack_id == DetailsIPC.INVALID_STACK_ID) {
      // hide stackview panel
      stackViewPane.setVisible(false);
    } else if (e.eventDetail == null) {
      // hide stackview panel
      stackViewPane.setVisible(false);
    } else if (stack_id == 0) {
      // we would show stacks, but stack ID is not known
      if (!stackViewEnabled) {
        // show user button that loads stack data
        stackViewPane.setVisible(true);
      } else {
        // no stack for this function
        stackViewPane.setVisible(false);
      }
    } else {
      // valid stack_id
      if (!stackViewEnabled) {
        // some other tab must have loaded stacks for us
        enableStackView();
      }
      updateStackView(e);
      stackViewPane.setVisible(true);
    }
    stackViewPane.revalidate();
  }

  public void resetStackView() {
    if (stack_view != null) {
      stack_view.reset();
    }
  }

  private static int desiredDistFromRoot = -1;

  private void updateStackView(final TimelineSelectionEvent e) {
    Vector<StackState> state_vec = (e == null) ? null : e.getStackFrames();
    if (state_vec == null || state_vec.isEmpty()) {
      stack_view.reset();
      return;
    }

    // stack_view(0) is leaf frame
    int prevSelIdx = stack_view.getLeadSelectionIndex();
    if (stack_view.isSelectionEmpty()) {
      prevSelIdx = -1;
    } else {
      prevSelIdx = stack_view.getLeadSelectionIndex();
    }
    final int prevMax = stack_view.getModel().getSize() - 1;

    // try to determine whether or not to change savedDistFromRoot
    if (prevSelIdx == -1) {
      // do nothing
    } else {
      int prevDistFromRoot = prevMax - prevSelIdx;
      if (prevDistFromRoot != desiredDistFromRoot) {
        // user probably made a selection
        if (prevSelIdx == 0) {
          desiredDistFromRoot = -1; // assume they want leaf to show
        } else {
          desiredDistFromRoot = prevDistFromRoot; // set new desired distance
        }
      }
    }

    // try to apply new desired to current stack
    final int selIndex;
    do {
      if (desiredDistFromRoot == -1) {
        selIndex = -1; // deselect and show leaf
        break;
      }
      final int newMax = state_vec.size() - 1;
      if (desiredDistFromRoot > newMax) {
        selIndex = -1; // deselect and show leaf
        break;
      }
      selIndex = newMax - desiredDistFromRoot;
    } while (false);

    clmap.addStates(state_vec);
    stack_view.setStates(state_vec, selIndex); // update stack in right-hand tab
    stack_view.setStackId(e.stackId);

    AnWindow.getInstance().setSubviewReadyToShow("timelineCallStackSubview", true);
  }

  // color change(s)
  @Override
  public void valueChanged(AnEvent e) {
    stack_view.repaint();
    timeline.repaint();
  }

  // Set selected function
  private void updateSummary(final StackState tmpstate) {
    final long sel_func;
    final long sel_pc;

    if (!(tmpstate instanceof StackViewState)) {
      System.err.println("XXXmpview timelistdisp: not StackViewState");
      return;
    }
    StackViewState state = (StackViewState) tmpstate;
    sel_func = state.getNumber();
    sel_pc = state.getPC();
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            long sel_obj = window.getSelectedObject().getObject(sel_func, sel_pc); // IPC!
            if (sel_obj != 0L) {
              window.getSelectedObject().setSelObj(sel_obj, AnDisplay.DSP_Timeline, 0);
              //            window.getSelectionManager().updateSelection(sel_obj,
              // AnDisplay.DSP_TIMELINE, 0, 1);
              AnWindow.getInstance()
                  .getViews()
                  .getCurrentViewDisplay()
                  .computeOnAWorkerThread(); // Update selection. FIXUP: Views that react on
                                             // selections should change selction on
                                             // SELECTION_CHANGED or SELECTED_OBJECT_CHANGED events
                                             // if not from own view
            }
          }
        },
        "Timelinecontrol update summary");
  }

  // The function selected has changed either by clicking inside the callstack
  // on the right or by clicking on an event in timeline
  // (MUST BE CALLED ON AWT THREAD)
  private void funcSelChanged(EventObject event) {
    final JList list;
    StackState state = null;

    // window.setSelectedInfoTab(eventPane); // TimelineDisp's eventPane

    if (event instanceof MouseEvent) {
      if (color_chooser.isVisible()) {
        color_chooser.setVisible(true);
      }
      if (((MouseEvent) event).getClickCount() > 1) {
        window.setSelectedView(AnDisplay.DSP_Source);
      }
    } else {
      if (color_chooser.isVisible()) {
        color_chooser.setVisible(true);
      }
    }

    // StackView or ChooserStateList
    if (event.getSource() instanceof StackView) {
      list = stack_view;
      state = (StackState) list.getSelectedValue();
    } else {
      // XXXmpview, figure this out...
      // System.err.println("XXXmpview MPItimelistdisp: eventsource unknown");
      return;
    }

    if (state == null) {
      return;
    }
    stack_view.setSelectedFunction(state.getNumber()); // XXXmpview may be redundant
    color_chooser.setSelectedFunction(state.getNumber());
    updateSummary(state);
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    if (SwingUtilities.isLeftMouseButton(event)) {
      funcSelChanged(event);
    }
    if (SwingUtilities.isRightMouseButton(event)) {
      stackViewPopupListener.maybeShowPopup(event);
    }
    return;
  }

  @Override
  public void mouseEntered(final MouseEvent event) {}

  @Override
  public void mouseExited(final MouseEvent event) {}

  @Override
  public void mousePressed(final MouseEvent event) {}

  @Override
  public void mouseReleased(final MouseEvent event) {}

  @Override
  public void valueChanged(final ListSelectionEvent event) {
    // Handles selection changes to RHS Callstack made via keyboard
    funcSelChanged(
        event); // YXXX unfortunately also picks up new stack selections and causes selected object
                // to change
  }

  // ---------- Context menu ---------- //
  // private JPopupMenu popup;
  private JMenuItem menuItemZoomHD;
  private JMenuItem menuItemScrollHL, menuItemScrollHR, menuItemScrollHPL, menuItemScrollHPR;
  private JMenuItem menuItemScrollVU, menuItemScorllVD;
  private PopupListener popupListener;
  private StackView.StackViewPopupListener stackViewPopupListener;

  // ------- Private classes and methods to implement popup menu items ------- //
  private void enableNavigationMenuItems(
      boolean left, boolean right, boolean up, boolean down, boolean center) {
    selectNextLeftAction.enableMenuItem(left);
    selectNextRightAction.enableMenuItem(right);
    selectNextUpAction.enableMenuItem(up);
    selectNextDownAction.enableMenuItem(down);
    selectFindAction.enableMenuItem(center);
  }

  private void enableZoomMenuItems(boolean hasHZoom, boolean hasVZoom) {
    if (menuItemZoomHD == null) {
      return;
    }
    menuItemZoomHD.setEnabled(hasHZoom);

    menuItemScrollHL.setEnabled(hasHZoom);
    menuItemScrollHR.setEnabled(hasHZoom);
    menuItemScrollHPL.setEnabled(hasHZoom);
    menuItemScrollHPR.setEnabled(hasHZoom);

    menuItemScrollVU.setEnabled(hasVZoom);
    menuItemScorllVD.setEnabled(hasVZoom);
  }

  private class PopupListener extends MouseAdapter implements ActionListener, PopupMenuListener {

    PopupListener() {}

    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    // public void mouseReleased(MouseEvent e) {
    //    maybeShowPopup(e);
    // }

    /**
     * Shows context menu
     *
     * @param MouseEvent e
     */
    private void maybeShowPopup(MouseEvent e) {
      // On Windows e.isPopupTrigger() returns false, so we have to apply a workaround
      if (e.isPopupTrigger()
          || ((System.getProperty("os.name").contains("Windows")) && (e.getButton() == 3))) {
        boolean hasHZoom =
            (tl2_drawer.getAbsoluteTimeDuration()
                != tl2_drawer.getTimeAxisReader().getTimeDuration());
        boolean hasVZoom =
            (tl2_drawer.getAbsoluteRowCount() != tl2_drawer.getDataAxisReader().getRowCount());
        enableZoomMenuItems(hasHZoom, hasVZoom);
        JPopupMenu popup = createPopupMenu(false);
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {}

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {}
  }

  /**
   * Creates popup menu Menu items: ---------------- Change Colors ---------------- Select Event ->
   * -> Select First -> Right -> Left -> Up -> Down -> Center Selection ---------------- Zoom -> ->
   * Increase -> Decrease -> Reset -> Previous (Back) ---------------- Scroll -> -> Center Selection
   * -> Right -> Left -> Up -> Down -> Page Right -> Page Left -> Previous (Back) ----------------
   * Add Filter: Include only events from visible time range Add Filter: Include only events from
   * selected time to end Add Filter: Include only events from beginning to selected time Add
   * Filter: Include only selected rows Add Filter: Include only unselected rows Remove All Filters
   * Manage Filters... ---------------- Properties ----------------
   *
   * <p>Returns PopupListener
   */
  private final int menuModifier = 0; // was ActionEvent.ALT_MASK;

  private JMenu newEventSelectSubMenu() {
    JMenu submenu1;
    JMenuItem menuItem;
    {
      // Add Select Next submenu
      submenu1 = new JMenu(AnLocale.getString("Select Event"));
      menuItem = selectNextUpAction.jmi;
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, menuModifier));
      submenu1.add(menuItem);
      menuItem = selectNextDownAction.jmi;
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, menuModifier));
      submenu1.add(menuItem);
      menuItem = selectNextLeftAction.jmi;
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, menuModifier));
      submenu1.add(menuItem);
      menuItem = selectNextRightAction.jmi;
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, menuModifier));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new SelectFirst());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, menuModifier));
      submenu1.add(menuItem);
    }
    return submenu1;
  }

  private JMenu newZoomSubMenu() {
    JMenu submenu1;
    JMenuItem menuItem;
    {
      // Add Zoom submenu
      submenu1 = new JMenu(AnLocale.getString("Zoom"));
      // Add Horizontal Zoom submenu
      submenu1.add(caliperZoom.jmi); // enabled dynamically

      // Zoom Undo/Redo/Reset
      menuItem = undoZoomAction.jmi;
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, menuModifier));
      submenu1.add(menuItem);
      menuItem = redoZoomAction.jmi;
      menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, ActionEvent.SHIFT_MASK));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new ResetHorizontalZoom());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, menuModifier));
      submenu1.add(menuItem);

      // Zoom Increase/Descrease
      menuItem = new JMenuItem(new IncreaseHorizontalZoom());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, menuModifier));
      submenu1.add(menuItem);
      menuItem = menuItemZoomHD = new JMenuItem(new DecreaseHorizontalZoom());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menuModifier));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new IncreaseVerticalZoom());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK));
      submenu1.add(menuItem);
      menuItem = menuItemZoomHD = new JMenuItem(new DecreaseVerticalZoom());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
      submenu1.add(menuItem);
    }
    return submenu1;
  }

  private JMenu newFindSubMenu() {
    JMenu submenu1;
    JMenuItem menuItem;
    {
      // Add Scroll submenu
      submenu1 = new JMenu(AnLocale.getString("Find"));
      menuItem = selectFindAction.jmi;
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuModifier));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new findTimeMarker());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_MASK));
      submenu1.add(menuItem);
    }
    return submenu1;
  }

  private JMenu newScrollSubMenu() {
    JMenu submenu1;
    JMenuItem menuItem;
    {
      // Add Scroll submenu
      submenu1 = new JMenu(AnLocale.getString("Scroll"));
      menuItem = menuItemScrollVU = new JMenuItem(new ScrollVerticalUp());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK));
      submenu1.add(menuItem);
      menuItem = menuItemScorllVD = new JMenuItem(new ScrollVerticalDown());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK));
      submenu1.add(menuItem);
      menuItem = menuItemScrollHL = new JMenuItem(new ScrollHorizontalLeft());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK));
      submenu1.add(menuItem);
      menuItem = menuItemScrollHR = new JMenuItem(new ScrollHorizontalRight());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK));
      submenu1.add(menuItem);
      menuItem = menuItemScrollHPL = new JMenuItem(new ScrollHorizontalPageLeft());
      menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
      submenu1.add(menuItem);
      menuItem = menuItemScrollHPR = new JMenuItem(new ScrollHorizontalPageRight());
      menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
      submenu1.add(menuItem);
    }
    return submenu1;
  }

  private JMenu newTimeRangeSubMenu() {
    JMenu submenu1;
    JMenuItem menuItem;
    {
      // add select tiime range items
      submenu1 = new JMenu(AnLocale.getString("Select Time Range"));
      menuItem = new JMenuItem(new SetTimeMarker());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, menuModifier));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new MoveTimeMarkerLeft());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, menuModifier));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new MoveTimeMarkerRight());
      menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, menuModifier));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new MoveTimeMarkerRangeLeft());
      menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.SHIFT_MASK));
      submenu1.add(menuItem);
      menuItem = new JMenuItem(new MoveTimeMarkerRangeRight());
      menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.SHIFT_MASK));
      submenu1.add(menuItem);
    }
    return submenu1;
  }

  private JPopupMenu createPopupMenu(boolean filterOnly) {
    JMenu submenu1;
    JMenuItem menuItem;
    JPopupMenu popup = new JPopupMenu();
    if (!filterOnly) {
      //            popup.add(caliperZoom.jmi); // enabled dynamically
      popup.add(newZoomSubMenu());
      popup.addSeparator();
    }

    popup.add(setFilterSelectedTimeRangeOnly.jmi);
    popup.add(setFilterExcludeBefore.jmi);
    popup.add(setFilterExcludeAfter.jmi);
    popup.add(setFilterVisibleTimeRangeOnly.jmi);
    popup.add(setFilterSelectedRowsOnly.jmi);
    popup.add(setFilterUnselectedRowsOnly.jmi);
    submenu1 = new JMenu(AnLocale.getString("Additional Filters"));
    submenu1.add(setFilterMicrostate.jmi);
    submenu1.add(setFilterNotMicrostate.jmi);
    submenu1.add(customFilter.jmi);
    popup.add(submenu1);
    if (filterOnly) {
      popup.addSeparator();
    }
    undoFilter.jmi.setEnabled(window.getFilters().canUndoLastFilter());
    popup.add(undoFilter.jmi);
    redoFilter.jmi.setEnabled(window.getFilters().canRedoLastFilter());
    popup.add(redoFilter.jmi);
    removeAllFilters.jmi.setEnabled(window.getFilters().canRemoveAllFilters());
    popup.add(removeAllFilters.jmi);
    // Add "Remove Filter" action
    if (window.getFilters().anyFilters()) {
      popup.add(window.getFilters().removeFilterMenuItem());
    }

    if (!filterOnly) {
      popup.addSeparator();
      {
        menuItem = new JMenuItem(new ChangeColors());
        menuItem.setAccelerator(KeyboardShortcuts.functionColorsActionShortcut);
        JMenuItem colorSubmenu = menuItem;
        popup.add(colorSubmenu);
      }
      popup.addSeparator();
      popup.add(newFindSubMenu());
      popup.add(newEventSelectSubMenu());
      popup.add(newTimeRangeSubMenu());
      popup.add(newScrollSubMenu());
      popup.addSeparator();
      // Add menu it em Properties
      menuItem = new JMenuItem(new ChangeDataPresentation());
      menuItem.setAccelerator(KeyboardShortcuts.settingsActionShortcut);
      popup.add(menuItem);
      //        popup.addSeparator();
    }

    return popup;
  }

  private void initNavigationMenuItems() {
    JMenuItem menuItem;

    undoZoomAction = new UndoZoomAction();
    menuItem = new JMenuItem(undoZoomAction);
    undoZoomAction.jmi = menuItem;
    menuItem.setEnabled(false);

    redoZoomAction = new RedoZoomAction();
    menuItem = new JMenuItem(redoZoomAction);
    redoZoomAction.jmi = menuItem;
    menuItem.setEnabled(false);

    selectNextUpAction = new SelectNextUp();
    menuItem = new JMenuItem(selectNextUpAction);
    selectNextUpAction.jmi = menuItem;
    menuItem.setEnabled(false);

    selectNextDownAction = new SelectNextDown();
    menuItem = new JMenuItem(selectNextDownAction);
    selectNextDownAction.jmi = menuItem;
    menuItem.setEnabled(false);

    selectNextLeftAction = new SelectNextLeft();
    menuItem = new JMenuItem(selectNextLeftAction);
    selectNextLeftAction.jmi = menuItem;
    menuItem.setEnabled(false);

    selectNextRightAction = new SelectNextRight();
    menuItem = new JMenuItem(selectNextRightAction);
    selectNextRightAction.jmi = menuItem;
    menuItem.setEnabled(false);

    selectFindAction = new SelectFind();
    menuItem = new JMenuItem(selectFindAction);
    selectFindAction.jmi = menuItem;
    menuItem.setEnabled(false);

    enableNavigationMenuItems(false, false, false, false, false);
  }

  private void initFilterMenuItems() {
    JMenuItem menuItem;

    TimelineCaliper caliper = timeline.getCaliperClone();
    final boolean enableSelFilters = (caliper != null);

    caliperZoom = new CaliperZoom();
    menuItem = new JMenuItem(caliperZoom);
    caliperZoom.jmi = menuItem;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    menuItem.setEnabled(enableSelFilters && !caliper.isSingleEvent());

    setFilterVisibleTimeRangeOnly = new SetFilterVisibleTimeRangeOnly();
    menuItem = new JMenuItem(setFilterVisibleTimeRangeOnly);
    setFilterVisibleTimeRangeOnly.jmi = menuItem;
    menuItem.setEnabled(true);

    setFilterSelectedTimeRangeOnly = new SetFilterSelectedTimeRangeOnly();
    menuItem = new JMenuItem(setFilterSelectedTimeRangeOnly);
    setFilterSelectedTimeRangeOnly.jmi = menuItem;
    menuItem.setEnabled(enableSelFilters && !caliper.isSingleEvent());

    setFilterExcludeAfter = new SetFilterExcludeAfter();
    menuItem = new JMenuItem(setFilterExcludeAfter);
    setFilterExcludeAfter.jmi = menuItem;
    menuItem.setEnabled(enableSelFilters);

    setFilterExcludeBefore = new SetFilterExcludeBefore();
    menuItem = new JMenuItem(setFilterExcludeBefore);
    setFilterExcludeBefore.jmi = menuItem;
    menuItem.setEnabled(enableSelFilters);

    final boolean hasRowSelections =
        (tl2_drawer.getRowBlockSelectList() != null
            && !tl2_drawer.getRowBlockSelectList().isEmpty());

    setFilterSelectedRowsOnly = new SetFilterSelectedRowsOnly();
    menuItem = new JMenuItem(setFilterSelectedRowsOnly);
    setFilterSelectedRowsOnly.jmi = menuItem;
    menuItem.setEnabled(hasRowSelections);

    setFilterUnselectedRowsOnly = new SetFilterUnselectedRowsOnly();
    menuItem = new JMenuItem(setFilterUnselectedRowsOnly);
    setFilterUnselectedRowsOnly.jmi = menuItem;
    menuItem.setEnabled(hasRowSelections);

    setFilterMicrostate = new SetFilterMicrostate();
    menuItem = new JMenuItem(setFilterMicrostate);
    setFilterMicrostate.jmi = menuItem;
    menuItem.setEnabled(false);

    setFilterNotMicrostate = new SetFilterNotMicrostate();
    menuItem = new JMenuItem(setFilterNotMicrostate);
    setFilterNotMicrostate.jmi = menuItem;
    menuItem.setEnabled(false);

    customFilter = new CustomFilterAction("Advanced Custom Filter...");
    menuItem = new JMenuItem(customFilter);
    customFilter.jmi = menuItem;
    menuItem.setEnabled(true);

    undoFilter = new UndoFilterAction();
    menuItem = new JMenuItem(undoFilter);
    undoFilter.jmi = menuItem;
    menuItem.setEnabled(window.getFilters().canUndoLastFilter());

    redoFilter = new RedoFilterAction();
    menuItem = new JMenuItem(redoFilter);
    redoFilter.jmi = menuItem;
    menuItem.setEnabled(window.getFilters().canRedoLastFilter());

    removeAllFilters = new RemoveAllFilterAction();
    menuItem = new JMenuItem(removeAllFilters);
    removeAllFilters.jmi = menuItem;
    menuItem.setEnabled(window.getFilters().canRemoveAllFilters());
  }

  class ContextMenuAction extends AbstractAction {

    final String name = AnLocale.getString("Context Menu");
    int id = KeyEvent.VK_F10;
    long when = 0; // System.currentTimeMillis();
    int modifiers = InputEvent.SHIFT_MASK;
    int x = 40;
    int y = 40;
    int clickCount = 1;
    boolean popupTrigger = true;
    int button = MouseEvent.BUTTON1;
    MouseEvent me = null;

    public ContextMenuAction() {
      super(AnLocale.getString("Context Menu"), null /* new ImageIcon("refresh.jpg") */);
      // A11YXXX : this is a hack, but I don't see a better way for now
      me = new MouseEvent(timeline, id, when, modifiers, x, y, clickCount, popupTrigger, button);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      try {
        popupListener.maybeShowPopup(me);
      } catch (java.awt.IllegalComponentStateException icse) {
        // System.out.println("ContextMenuAction.ActionPerformed(): " + icse);
      }
    }
  }

  /** Implements menu item: Zoom -> -> Horizontal -> -> Increase */
  class CaliperZoom extends AbstractAction {
    public JMenuItem jmi = null;

    public CaliperZoom() {
      super(AnLocale.getString("Zoom to Selected Time Range"), AnUtility.mtall_icon);
    }

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      zoomToCaliper();
    }
  }

  class IncreaseHorizontalZoom extends AbstractAction {

    public IncreaseHorizontalZoom() {
      super(AnLocale.getString("Increase Time Zoom"), null);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      genericZoomIn();
    }
  }

  class DecreaseHorizontalZoom extends AbstractAction {

    public DecreaseHorizontalZoom() {
      super(AnLocale.getString("Decrease Time Zoom"), null);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      genericZoomOut();
    }
  }

  class IncreaseVerticalZoom extends AbstractAction {

    public IncreaseVerticalZoom() {
      super(AnLocale.getString("Increase Vertical Zoom"), null);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      genericVZoomIn();
    }
  }

  class DecreaseVerticalZoom extends AbstractAction {

    public DecreaseVerticalZoom() {
      super(AnLocale.getString("Decrease Vertical Zoom"), null);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      genericVZoomOut();
    }
  }

  /** Implements menu item: Zoom -> -> Horizontal -> -> Reset */
  class ResetHorizontalZoom extends AbstractAction {

    public ResetHorizontalZoom() {
      super(AnLocale.getString("Reset Time Zoom"), null);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      zoomTimeToLevel(0);
    }
  }

  /** Implements menu item: Zoom -> -> Previous (Back) */
  class UndoZoomAction extends AbstractAction {

    public UndoZoomAction() {
      super(AnLocale.getString("Undo Zoom Action"), AnUtility.undo_icon);
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      undoZoom();
    }
  }

  class RedoZoomAction extends AbstractAction {

    public RedoZoomAction() {
      super(AnLocale.getString("Redo Zoom Action"), AnUtility.redo_icon);
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      redoZoom();
    }
  }

  // -------------- Change Colors ---------------- //
  /** Implements menu item: Change Colors */
  class ChangeColors extends AbstractAction {

    public ChangeColors() {
      super(AnLocale.getString("Function Colors..."), AnUtility.colr_icon);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      chooseColors();
    }
  }

  // -------------- Properties ---------------- //
  /** Implements menu item: Properties */
  class ChangeDataPresentation extends AbstractAction {

    public ChangeDataPresentation() {
      super(AnLocale.getString("Timeline Settings"), AnUtility.gear_icon);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      editProperties(AnLocale.getString("Timeline"));
    }
  }

  private static final String ADD_FILTER = AnLocale.getString("Add Filter: ");
  private static final String VISIBLE_RANGE_SHORT_NAME = AnLocale.getString("Visible Range");
  private static final String VISIBLE_RANGE_LONG_NAME =
      AnLocale.getString("Include only events intersecting visible time range");
  private static final String SELECTED_RANGE_SHORT_NAME =
      AnLocale.getString("Intersecting Time Range");
  private static final String SELECTED_RANGE_LONG_NAME =
      AnLocale.getString("Include only events intersecting selected time range");
  private static final String EXCLUDE_BEFORE_SHORT_NAME = AnLocale.getString("Starting in Range");
  private static final String EXCLUDE_BEFORE_LONG_NAME =
      AnLocale.getString("Include only events starting in selected time range");
  private static final String EXCLUDE_AFTER_SHORT_NAME = AnLocale.getString("Ending in Range");
  private static final String EXCLUDE_AFTER_LONG_NAME =
      AnLocale.getString("Include only events ending in selected time range");
  private static final String SELECTED_ROWS_SHORT_NAME = AnLocale.getString("Selected Rows");
  private static final String SELECTED_ROWS_LONG_NAME =
      AnLocale.getString("Include only selected rows");
  private static final String UNSELECTED_ROWS_SHORT_NAME = AnLocale.getString("Unselected Rows");
  private static final String UNSELECTED_ROWS_LONG_NAME =
      AnLocale.getString("Include only unselected rows");
  private static final String WITH_MSTATE_SHORT_NAME = AnLocale.getString("Thread State");
  private static final String WITH_MSTATE_LONG_NAME =
      AnLocale.getString("Include only events having the selected event's thread state");
  private static final String NOT_MSTATE_SHORT_NAME = AnLocale.getString("Not Thread State");
  private static final String NOT_MSTATE_LONG_NAME =
      AnLocale.getString("Include only events not having the selected event's thread state");

  private String getSelectedViewDisplayName() {
    String ret = "Timeline"; // fallback
    if (window != null) {
      ret = window.getSelectedView().getDisplayName();
    }
    return ret;
  }

  private void setClause(String shortName, String longName, String clause) {
    window
        .getFilters()
        .addClause(
            getSelectedViewDisplayName() + ": " + shortName,
            longName,
            clause,
            FilterClause.Kind.STANDARD);
  }
  // -------------- visible time range ---------------- //

  /** Implements menu item: Set Filter: visible time range */
  class SetFilterVisibleTimeRangeOnly extends AbstractAction {

    public JMenuItem jmi = null;

    public SetFilterVisibleTimeRangeOnly() {
      super(ADD_FILTER + VISIBLE_RANGE_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      long start = tl2_drawer.getTimeAxisReader().getTimeStart();
      long end = tl2_drawer.getTimeAxisReader().getTimeEnd();
      String clause = "!( (TSTAMP_HI < " + start + ") || (TSTAMP_LO > " + end + ") )";
      //            window.filter.showDialog();       // Show "Set Filter" dialog
      //            window.filter.setSelectedTab(1);  // Select advanced filter tab
      setClause(
          VISIBLE_RANGE_SHORT_NAME,
          VISIBLE_RANGE_LONG_NAME,
          clause); // Put the string in the text field
    }
  }

  /** Implements menu item: Set Filter: selected time range */
  class SetFilterSelectedTimeRangeOnly extends AbstractAction {

    public JMenuItem jmi = null;

    public SetFilterSelectedTimeRangeOnly() {
      super(ADD_FILTER + SELECTED_RANGE_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      TimelineCaliper caliper = timeline.getCaliperClone();
      if (caliper == null) {
        return;
      }
      final long start = caliper.getLowTime();
      final long end = caliper.getHighTime();
      String clause = "!( (TSTAMP_HI < " + start + ") || (TSTAMP_LO > " + end + ") )";
      //            window.filter.showDialog();       // Show "Set Filter" dialog
      //            window.filter.setSelectedTab(1);  // Select advanced filter tab
      setClause(
          SELECTED_RANGE_SHORT_NAME,
          SELECTED_RANGE_LONG_NAME,
          clause); // Put the string in the text field
    }
  }

  // -------------- exclude before ---------------- //
  /** Implements menu item: Set Filter: exclude before */
  class SetFilterExcludeBefore extends AbstractAction {

    public JMenuItem jmi = null;

    public SetFilterExcludeBefore() {
      super(ADD_FILTER + EXCLUDE_BEFORE_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      TimelineCaliper caliper = timeline.getCaliperClone();
      if (caliper == null) {
        return;
      }
      final long start = caliper.getLowTime();
      final long end = caliper.getHighTime();
      String clause = "( (TSTAMP_LO >= " + start + ") && (TSTAMP_LO <= " + end + ") )";
      //            window.filter.showDialog();       // Show "Set Filter" dialog
      //            window.filter.setSelectedTab(1);  // Select advanced filter tab
      setClause(
          EXCLUDE_BEFORE_SHORT_NAME,
          EXCLUDE_BEFORE_LONG_NAME,
          clause); // Put the string in the text field
    }
  }

  // -------------- exclude after ---------------- //
  /** Implements menu item: Set Filter: exclude after */
  class SetFilterExcludeAfter extends AbstractAction {

    public JMenuItem jmi = null;

    public SetFilterExcludeAfter() {
      super(ADD_FILTER + EXCLUDE_AFTER_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      TimelineCaliper caliper = timeline.getCaliperClone();
      if (caliper == null) {
        return;
      }
      final long start = caliper.getLowTime();
      final long end = caliper.getHighTime();
      String clause = "( (TSTAMP_HI >= " + start + ") && (TSTAMP_HI <= " + end + ") )";
      //            window.filter.showDialog();       // Show "Set Filter" dialog
      //            window.filter.setSelectedTab(1);  // Select advanced filter tab
      setClause(
          EXCLUDE_AFTER_SHORT_NAME,
          EXCLUDE_AFTER_LONG_NAME,
          clause); // Put the string in the text field
    }
  }

  // -------------- only selected rows ---------------- //

  /** Implements menu item: Set Filter: only selected rows */
  class SetFilterSelectedRowsOnly extends AbstractAction {

    public JMenuItem jmi = null;

    public SetFilterSelectedRowsOnly() {
      super(ADD_FILTER + SELECTED_ROWS_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      String clause = getSelRowsFilterClause(false);
      if (clause.length() != 0) {
        setClause(SELECTED_ROWS_SHORT_NAME, SELECTED_ROWS_LONG_NAME, clause);
      } // Put the string in the text field
    }
  }

  // -------------- only unselected rows ---------------- //
  /** Implements menu item: Set Filter: only unselected rows */
  class SetFilterUnselectedRowsOnly extends AbstractAction {

    public JMenuItem jmi = null;

    public SetFilterUnselectedRowsOnly() {
      super(ADD_FILTER + UNSELECTED_ROWS_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      String clause = getSelRowsFilterClause(true);
      if (clause.length() != 0) {
        setClause(UNSELECTED_ROWS_SHORT_NAME, UNSELECTED_ROWS_LONG_NAME, clause);
      } // Put the string in the text field
    }
  }

  private String getSelRowsFilterClause(boolean invert) {
    // Rules for generating row filter string:
    // if "group-by" is experiments
    //     filter by expid
    // else if (experiment master row selected)
    //     if (no subrows or all subrows selected)
    //         filter by expid
    //     else
    //         filter by selected subrows
    // else
    //     filter by selected subrows
    //
    //  This has special implications for Java:
    //  Example 1:
    //    filter by experiment two's master row and all user-mode threads
    //    Result: "EXPID==2"
    //    Switching to Machine Mode will show User and JVM threads
    //
    //  Example 2:
    //    filter by experiment two's user-mode threads *without* master row
    //    Result: "EXPID==2 && THRID IN (...)"
    //    Switching to Machine Mode will show only User Mode threads
    //
    //
    // Note: this function does not look at EXPGRID
    //        It also assumes that a given EXPID's blocks are contiguous
    //            (rows from one EXPID won't be interleaved with rows from some other EXPID)
    //
    List<Integer> blockSelList = tl2_drawer.getRowBlockSelectList();
    int nSelBlocks = blockSelList.size();
    if (nSelBlocks == 0) {
      return "";
    }
    List<EntityDefinitions> rowBlocks = tl2_drawer.getRowDefsByBlock();
    if (rowBlocks == null) {
      return ""; // data is in transition
    }
    int prevUserExpId = -1; // invalid value
    final String clauseInit = "( ";
    String clause = clauseInit;
    boolean experimentMasterRowSelected = false;
    boolean entIsExpid = false;
    String entPropIdName = null;
    final List<Integer> entityIds = new ArrayList();
    final List<Integer> expIdList = new ArrayList();
    for (int jj = 0; jj <= nSelBlocks /*one extra!*/; jj++) {
      final RowDefinition rowDef0;
      final int userExpId;
      if (jj < nSelBlocks) {
        int blockNum = blockSelList.get(jj);
        if (blockNum < 0 || blockNum >= rowBlocks.size()) {
          return ""; // weird
        }
        EntityDefinitions rowDefs = rowBlocks.get(blockNum);
        rowDef0 = rowDefs.entityRows.get(0);
        userExpId = rowDef0.getUserExpID();
      } else {
        rowDef0 = null;
        userExpId = -2;
      }
      if (jj == nSelBlocks || (jj > 0 && prevUserExpId != userExpId)) {
        // wrap up previous block

        boolean filterExclusivelyByExpid = false;
        if (entIsExpid) {
          // Group By Experiment
          filterExclusivelyByExpid = true;
        } else if (experimentMasterRowSelected) {
          if (entityIds.isEmpty()) {
            // Only Experiment master row selected
            filterExclusivelyByExpid = true;
          } else {
            ExperimentDefinitions tmp =
                tl2_drawer.event_drawer.getDataSnapshot().getExperimentDefinitions(prevUserExpId);
            if (tmp != null) {
              int numEntities = tmp.experimentEntities.size();
              if (numEntities - 1 == entityIds.size()) {
                // Master and all subrows selected
                filterExclusivelyByExpid = true;
              }
            } else {
              int kk = -1; // weird
            }
          }
        }

        if (filterExclusivelyByExpid) {
          // Only Experiment master row selected: filter exclusively by EXPID
          expIdList.add(prevUserExpId);
        } else {
          // generate a filter for selected  entities (e.g. THRID)
          if (!clause.equals(clauseInit)) {
            clause += " || ";
          }
          clause += "( EXPID==" + prevUserExpId + " && ";
          if (invert) {
            clause += "!";
            expIdList.add(prevUserExpId);
          }
          boolean propListStarted = false;
          for (Integer entId : entityIds) {
            if (!propListStarted) {
              clause += "( " + entPropIdName + " IN (";
            } else {
              clause += ",";
            }
            clause += entId;
            propListStarted = true;
          }
          clause += ") ) )";
        }
        experimentMasterRowSelected = false;
        entIsExpid = false;
        entPropIdName = null;
        entityIds.clear();
      }
      if (rowDef0 != null) {
        prevUserExpId = userExpId;
        Entity ent0 = rowDef0.getEntity();
        String tmpEntPropIdName = ent0.getPropIdName();
        if (tmpEntPropIdName.equalsIgnoreCase("EXPID")) {
          // no properties are needed for Experiment grouping
          entIsExpid = true;
        } else if (ent0.getPropId() == Settings.PROP_NONE) {
          // for rows that don't have an entity, like samples
          experimentMasterRowSelected = true;
        } else {
          int entId = ent0.getPropValue();
          if (entPropIdName == null) {
            entPropIdName = tmpEntPropIdName;
          } else if (!entPropIdName.equalsIgnoreCase(tmpEntPropIdName)) {
            int kk = 0; // weird!
          }
          entityIds.add(entId);
        }
      }
    }
    if (!expIdList.isEmpty()) {
      // now generate the 'simple' EXPID filters
      if (!clause.equals(clauseInit)) {
        clause += " || ";
      }
      if (invert) {
        clause += " !";
      }
      boolean firstTime = true;
      for (Integer tmpExpId : expIdList) {
        if (firstTime) {
          clause += "( EXPID in (" + tmpExpId;
          firstTime = false;
        } else {
          clause += "," + tmpExpId;
        }
      }
      clause += ") )";
    }
    clause += " )";
    return clause;
  }

  // -------------- from beginning to selected time ---------------- //
  /** Implements menu item: Set Filter: from beginning to selected time */
  class SetFilterMicrostate extends AbstractAction {

    public JMenuItem jmi = null;
    private int stateValue = -1;

    public SetFilterMicrostate() {
      super(/*ADD_FILTER +*/ WITH_MSTATE_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(TimelineSelectionEvent selEvent) {
      boolean enabled;
      if (selEvent != null
          && selEvent.eventDetail != null
          && selEvent.eventDetail instanceof ProfileEvent) {
        enabled = true;
        ProfileEvent pEvent = (ProfileEvent) selEvent.eventDetail;
        stateValue = pEvent.getState();
      } else {
        enabled = false;
        stateValue = -1;
      }
      if (null != jmi) {
        jmi.setEnabled(enabled);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      String clause = "(MSTATE == " + stateValue + ")";
      setClause(
          WITH_MSTATE_SHORT_NAME,
          WITH_MSTATE_LONG_NAME,
          clause); // Put the string in the text field
    }
  }

  // -------------- from beginning to selected time ---------------- //
  /** Implements menu item: Set Filter: from beginning to selected time */
  class SetFilterNotMicrostate extends AbstractAction {

    public JMenuItem jmi = null;
    private int stateValue = -1;

    public SetFilterNotMicrostate() {
      super(/*ADD_FILTER +*/ NOT_MSTATE_LONG_NAME /*, AnUtility.filtclause_icon*/);
    }

    public void enableMenuItem(TimelineSelectionEvent selEvent) {
      boolean enabled;
      if (selEvent != null
          && selEvent.eventDetail != null
          && selEvent.eventDetail instanceof ProfileEvent) {
        enabled = true;
        ProfileEvent pEvent = (ProfileEvent) selEvent.eventDetail;
        stateValue = pEvent.getState();
      } else {
        enabled = false;
        stateValue = -1;
      }
      if (null != jmi) {
        jmi.setEnabled(enabled);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      String clause = "(MSTATE != " + stateValue + ")";
      setClause(
          NOT_MSTATE_SHORT_NAME, NOT_MSTATE_LONG_NAME, clause); // Put the string in the text field
    }
  }

  // -------------- Select Event ---------------- //
  /** Implements menu item: -> Select First */
  class SelectFirst extends AbstractAction {

    public SelectFirst() {
      super(AnLocale.getString("Select a Visible Event"), new ImageIcon("sf.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      selectFirst();
    }
  }

  /** Implements menu item: -> Center Selection */
  class SelectFind extends AbstractAction {

    public SelectFind() {
      super(AnLocale.getString("Find (Recenter) Event Selection"));
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      selectFind();
    }
  }

  /** Implements menu item: -> Right */
  class SelectNextRight extends AbstractAction {

    public SelectNextRight() {
      super(AnLocale.getString("Right"), AnUtility.forw_icon);
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      selectRight();
    }
  }

  /** Implements menu item: -> Left */
  class SelectNextLeft extends AbstractAction {

    public SelectNextLeft() {
      super(AnLocale.getString("Left"), AnUtility.back_icon);
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      selectLeft();
    }
  }

  /** Implements menu item: -> Up */
  class SelectNextUp extends AbstractAction {

    public SelectNextUp() {
      super(AnLocale.getString("Up"), AnUtility.up_icon);
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      selectUp();
    }
  }

  /** Implements menu item: -> Down */
  class SelectNextDown extends AbstractAction {

    public SelectNextDown() {
      super(AnLocale.getString("Down"), AnUtility.down_icon);
    }

    public JMenuItem jmi = null;

    public void enableMenuItem(boolean val) {
      if (null != jmi) {
        jmi.setEnabled(val);
      }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      selectDown();
    }
  }

  // -------------- Scroll ---------------- //
  /** Implements menu item: Scroll -> -> Right */
  class ScrollHorizontalRight extends AbstractAction {

    public ScrollHorizontalRight() {
      super(AnLocale.getString("Right"), new ImageIcon("shr.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.panRight();
    }
  }

  /** Implements menu item: Scroll -> -> Left */
  class ScrollHorizontalLeft extends AbstractAction {

    public ScrollHorizontalLeft() {
      super(AnLocale.getString("Left"), new ImageIcon("shl.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.panLeft();
    }
  }

  /** Implements menu item: Scroll -> -> Page Right */
  class ScrollHorizontalPageRight extends AbstractAction {

    public ScrollHorizontalPageRight() {
      super(AnLocale.getString("Page Right"), new ImageIcon("shpr.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.pageRight();
    }
  }

  /** Implements menu item: Scroll -> -> Page Left */
  class ScrollHorizontalPageLeft extends AbstractAction {

    public ScrollHorizontalPageLeft() {
      super(AnLocale.getString("Page Left"), new ImageIcon("shpl.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.pageLeft();
    }
  }

  /** Implements menu item: Scroll -> -> Up */
  class ScrollVerticalUp extends AbstractAction {

    public ScrollVerticalUp() {
      super(AnLocale.getString("Up"), new ImageIcon("svu.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.panUp();
    }
  }

  /** Implements menu item: Scroll -> -> Down */
  class ScrollVerticalDown extends AbstractAction {

    public ScrollVerticalDown() {
      super(AnLocale.getString("Down"), new ImageIcon("svd.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.panDown();
    }
  }

  /** Implements menu item: Scroll -> -> Page Up */
  class ScrollVerticalPageUp extends AbstractAction {

    public ScrollVerticalPageUp() {
      super(AnLocale.getString("Page Up"), new ImageIcon("svpu.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.pageUp();
    }
  }

  /** Implements menu item: Scroll -> -> Page Down */
  class ScrollVerticalPageDown extends AbstractAction {

    public ScrollVerticalPageDown() {
      super(AnLocale.getString("Page Down"), new ImageIcon("svpd.jpg"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.pageDown();
    }
  }

  // -------------- Select Time Range ---------------- //
  class findTimeMarker extends AbstractAction {

    public findTimeMarker() {
      super(AnLocale.getString("Find Time Markers"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.findTimeMarker();
    }
  }

  /** Implements menu item: Select Time Range -> -> Set Time Marker at Center */
  class SetTimeMarker extends AbstractAction {

    public SetTimeMarker() {
      super(AnLocale.getString("Set Time Marker at Center"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.setTimeMarker();
    }
  }

  /** Implements menu item: Select Time Range -> -> Move Time Marker Left */
  class MoveTimeMarkerLeft extends AbstractAction {

    public MoveTimeMarkerLeft() {
      super(AnLocale.getString("Move Time Marker Left"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.moveTimeMarkerLeft();
    }
  }

  /** Implements menu item: Select Time Range -> -> Move Time Marker Right */
  class MoveTimeMarkerRight extends AbstractAction {

    public MoveTimeMarkerRight() {
      super(AnLocale.getString("Move Time Marker Right"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.moveTimeMarkerRight();
    }
  }

  /** Implements menu item: Select Time Range -> -> Adjust Selection Range Left */
  class MoveTimeMarkerRangeLeft extends AbstractAction {

    public MoveTimeMarkerRangeLeft() {
      super(AnLocale.getString("Adjust Time Range to the Left"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.moveTimeRangeToLeft();
    }
  }

  /** Implements menu item: Select Time Range -> -> Adjust Selection Range Right */
  class MoveTimeMarkerRangeRight extends AbstractAction {

    public MoveTimeMarkerRangeRight() {
      super(AnLocale.getString("Adjust Time Range to the Right"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      timeline.moveTimeRangeToRight();
    }
  }

  class ShowDynamicHelpPageAction extends AbstractAction {

    public ShowDynamicHelpPageAction() {
      super(AnLocale.getString("More Info"), new ImageIcon("help.gif"));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      ShowDynamicHelpPage();
    }
  }

  private void ShowDynamicHelpPage() {
    // HelpManager.getDefault().showDynaHelp("MPITimeline");
  }

  public TimelinePanel getTimelinePanel() {
    return timeline;
  }
}
