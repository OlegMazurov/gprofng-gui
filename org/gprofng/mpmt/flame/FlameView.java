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

package org.gprofng.mpmt.flame;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnAction;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnEvent;
import org.gprofng.mpmt.AnListener;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.KeyboardShortcuts;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.filter.CustomFilterAction;
import org.gprofng.mpmt.filter.FilterClause;
import org.gprofng.mpmt.filter.RedoFilterAction;
import org.gprofng.mpmt.filter.RemoveAllFilterAction;
import org.gprofng.mpmt.filter.UndoFilterAction;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.settings.MetricsSetting.MetricCheckBox;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.settings.ViewModeSetting;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.statecolors.StateColorMap;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class FlameView extends AnDisplay implements ExportSupport, AnChangeListener, AnListener {

  private static final String ADD_FILTER = AnLocale.getString("Add Filter: ");
  private static final String FILTER_SELECTED_BRANCH_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected branch");
  private static final String FILTER_SELECTED_BRANCH_SHORT_NAME =
      AnLocale.getString("Selected Branch");
  private static final String FILTER_NOT_SELECTED_BRANCH_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected branch");
  private static final String FILTER_NOT_SELECTED_BRANCH_SHORT_NAME =
      AnLocale.getString("Not Selected Branch");
  private static final String FILTER_SELECTED_FUNCTION_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected function");
  private static final String FILTER_SELECTED_FUNCTION_SHORT_NAME =
      AnLocale.getString("Selected Function");
  private static final String FILTER_SELECTED_LEAF_LONG_NAME =
      AnLocale.getString("Include only stacks with the selected function as leaf");
  private static final String FILTER_SELECTED_LEAF_SHORT_NAME = AnLocale.getString("Selected Leaf");
  private static final String STR_ACTION_UNDO_FILTER = AnLocale.getString("Undo Filter Action");
  private static final String STR_ACTION_REDO_FILTER = AnLocale.getString("Redo Filter Action");
  private static final String STR_ACTION_REMOVE_ALL_FILTERS =
      AnLocale.getString("Remove All Filters");
  private static final String STR_ACTION_CUSTOM_FILTER =
      ADD_FILTER + AnLocale.getString("Advanced Custom Filter...");

  // Data
  private int rowCount = -1;
  private FlameData flameData = null;
  private Object flameDataLock = new Object();
  private MainThread mainThread = null;

  // GUI
  private FlamePanel flamePanel;
  private JScrollPane mainScrollPane;
  private JPanel mainToolbarPanel;
  private FlameToolBar flameToolBar;
  private JPanel detailsPanel;
  private JLabel detailsLabel;

  // States
  private boolean mainThreadKilled = false;
  private AnMetric anMetricToBeUsed = null;
  private boolean supportedExperiments = true;

  // Actions
  private AnAction undoSetBaseAction;
  private AnAction redoSetBaseAction;
  private AnAction resetBaseAction;
  private AnAction setBaseAction;
  private AnAction upOneAction;
  private AnAction downOneAction;
  private AnAction leftOneAction;
  private AnAction rightOneAction;

  // Debug
  //    private static Date beginDate = new Date();
  // Constructor
  public FlameView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_CallFlame, AnVariable.HELP_CallFlameChart);
    setAccessibility(AnLocale.getString("Flame Graph"));
    AnEventManager.getInstance().addListener(this);
    AnWindow.getInstance().getColorChooser().getColorMap().addAnListener(this);

    KeyStroke ks = KeyboardShortcuts.contextMenuActionShortcut;
    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, ks);
    getActionMap()
        .put(
            ks,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                flamePanel.showPopup(getFlameData().getSelectedFlameBlock());
              }
            });
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
    //        System.out.println("CallTreeView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
        computed = false;
        initView();
        break;
      case EXPERIMENTS_LOADED_FAILED:
        // Nothing
        break;
      case EXPERIMENTS_LOADED:
        handleSupportedExperiments();
        if (selected) {
          computeOnAWorkerThread();
        }
        break;
      case FILTER_CHANGED:
        interruptMainThread();
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
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.LIBRARY_VISIBILITY
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.METRICS) {
          if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.METRICS) {
            Object o = anSettingChangeEvent.getOriginalSource();
            if (o instanceof MetricsSetting.MetricCheckBox) {
              anMetricToBeUsed = ((MetricCheckBox) o).getAnMetric();
            } else {
              anMetricToBeUsed = null;
            }
          }
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
  public void valueChanged(AnEvent e) {
    flamePanel.functionColorsChanged();
  }

  @Override
  public JPopupMenu getFilterPopup() {
    if (getFlameData() != null) {
      JPopupMenu popup = new JPopupMenu();
      for (JComponent jmi : getFilterMenuList(getFlameData().getSelectedFlameBlock())) {
        popup.add(jmi);
      }
      return popup;
    } else {
      return null;
    }
  }

  protected FlameToolBar getFlameToolBar() {
    return flameToolBar;
  }

  private void initActions() {
    undoSetBaseAction =
        new AnAction(
            AnLocale.getString("Undo Set As Base"),
            AnUtility.undo_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (undoSetBaseAction.isEnabled()) {
                  flameToolBar.undoSetBaseStack();
                }
              }
            });
    undoSetBaseAction.setKeyboardShortCut(
        this, "FLAME_BACKSPACE", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));

    redoSetBaseAction =
        new AnAction(
            AnLocale.getString("Redo Set As Base"),
            AnUtility.redo_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (redoSetBaseAction.isEnabled()) {
                  flameToolBar.redoSetBaseStack();
                }
              }
            });
    redoSetBaseAction.setKeyboardShortCut(
        this,
        "FLAME_SHIFT_BACKSPACE",
        KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_DOWN_MASK));

    resetBaseAction =
        new AnAction(
            AnLocale.getString("Reset Base"),
            AnUtility.base2Icon,
            AnLocale.getString("Reset Base to <Total>"),
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (resetBaseAction.isEnabled()) {
                  flamePanel.resetBase();
                }
              }
            });
    resetBaseAction.setKeyboardShortCut(
        this,
        "FLAME_SHIFT_ENTER",
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK));

    setBaseAction =
        new AnAction(
            AnLocale.getString("Set As Base"),
            AnUtility.base1Icon,
            AnLocale.getString("Set Base to Selected Function"),
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (setBaseAction.isEnabled()) {
                  flamePanel.setAsBase();
                }
              }
            });
    setBaseAction.setKeyboardShortCut(
        this, "FLAME_ENTER", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));

    upOneAction =
        new AnAction(
            AnLocale.getString("Up One"),
            AnUtility.up_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (upOneAction.isEnabled()) {
                  flamePanel.upOne();
                }
              }
            });
    upOneAction.setKeyboardShortCut(
        this, "FLAME_UP", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false));

    //        String ks_string = "FLAME_UP"; ;;;
    //        KeyStroke ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
    //        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks_def,
    // ks_string);
    //        this.getActionMap().put(ks_string, new AbstractAction() {
    //            @Override
    //            public void actionPerformed(ActionEvent ev) {
    //                upOneAction.actionPerformed(ev);
    //            }
    //        });
    downOneAction =
        new AnAction(
            AnLocale.getString("Down One"),
            AnUtility.down_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (downOneAction.isEnabled()) {
                  flamePanel.downOne();
                }
              }
            });
    downOneAction.setKeyboardShortCut(
        this, "FLAME_DOWN", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false));

    leftOneAction =
        new AnAction(
            AnLocale.getString("Left One"),
            AnUtility.back_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (leftOneAction.isEnabled()) {
                  flamePanel.leftOne();
                }
              }
            });
    leftOneAction.setKeyboardShortCut(
        this, "FLAME_LEFT", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false));

    rightOneAction =
        new AnAction(
            AnLocale.getString("Right One"),
            AnUtility.forw_icon,
            null,
            null,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (rightOneAction.isEnabled()) {
                  flamePanel.rightOne();
                }
              }
            });
    rightOneAction.setKeyboardShortCut(
        this, "FLAME_RIGHT", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false));
  }

  protected AnAction getUndoSetBaseAction() {
    return undoSetBaseAction;
  }

  protected AnAction getRedoSetBaseAction() {
    return redoSetBaseAction;
  }

  protected AnAction getResetBaseAction() {
    return resetBaseAction;
  }

  protected AnAction getSetBaseAction() {
    return setBaseAction;
  }

  protected AnAction getUpOneAction() {
    return upOneAction;
  }

  protected AnAction getDownOneAction() {
    return downOneAction;
  }

  protected AnAction getLeftOneAction() {
    return leftOneAction;
  }

  protected AnAction getRightOneAction() {
    return rightOneAction;
  }

  protected void updateActionsEnableStatus() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            getFlameToolBar().setControilsEnabled(true);
            getUndoSetBaseAction().setEnabled(flameToolBar.canUndoSetBaseStack());
            getRedoSetBaseAction().setEnabled(flameToolBar.canRedoSetBaseStack());
            getResetBaseAction().setEnabled(flamePanel.canResetBase());
            getSetBaseAction().setEnabled(flamePanel.canSetBase());
            getUpOneAction().setEnabled(flamePanel.canUpOne());
            getDownOneAction().setEnabled(flamePanel.canDownOne());
            getLeftOneAction().setEnabled(flamePanel.canLeftOne());
            getRightOneAction().setEnabled(flamePanel.canRightOne());
          }
        });
  }

  protected void resetActionsEnableStatus() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            getFlameToolBar().setControilsEnabled(false);
            getUndoSetBaseAction().setEnabled(false);
            getRedoSetBaseAction().setEnabled(false);
            getResetBaseAction().setEnabled(false);
            getSetBaseAction().setEnabled(false);
            getUpOneAction().setEnabled(false);
            getDownOneAction().setEnabled(false);
            getLeftOneAction().setEnabled(false);
            getRightOneAction().setEnabled(false);
          }
        });
  }

  @Override
  protected void initComponents() {
    initActions();

    setLayout(new BorderLayout());

    JPanel toolbarPanel = new JPanel();
    toolbarPanel.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
    toolbarPanel.add(flameToolBar = new FlameToolBar(this));

    mainToolbarPanel = new JPanel(new BorderLayout());
    mainToolbarPanel.add(toolbarPanel, BorderLayout.NORTH);
    add(mainToolbarPanel, BorderLayout.NORTH);

    flamePanel = new FlamePanel(this);
    mainScrollPane = new JScrollPane(flamePanel);
    mainScrollPane.setBorder(null);
    mainScrollPane.getVerticalScrollBar().setUnitIncrement(32);
    mainScrollPane.getVerticalScrollBar().setBlockIncrement(32);
    mainScrollPane.getVerticalScrollBar().setValueIsAdjusting(true);
    add(mainScrollPane, BorderLayout.CENTER);

    detailsPanel = new JPanel();
    detailsPanel.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, AnEnvironment.TABBED_PANE_BORDER_COLOR));
    detailsPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints;

    detailsLabel = new JLabel();
    //        detailsLabel.setFont(detailsLabel.getFont().deriveFont(Font.BOLD));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(4, 4, 0, 0);
    detailsPanel.add(detailsLabel, gridBagConstraints);

    detailsPanel.setMinimumSize(new Dimension(10, 10));

    add(detailsPanel, BorderLayout.SOUTH);

    resetActionsEnableStatus();
  }

  protected void updateSelection() {
    if (getFlameData() != null) {
      updateSelection(getFlameData().getSelectedFlameBlock());
    } else {
      updateSelection(null);
    }
  }

  protected void updateSelection(final FlameBlock selectedFlameBlock) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            if (selectedFlameBlock != null) {
              detailsLabel.setText(selectedFlameBlock.getDetailsTextHTML());
            } else {
              detailsLabel.setText(FlameBlock.getDetailsEmptyTextHTML());
            }
            detailsLabel.setToolTipText(detailsLabel.getText());
          }
        });
  }

  protected void updateGUI(final AnMetric anMetric) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            flamePanel.setPanelSize(rowCount);
            SwingUtilities.invokeLater(
                new Runnable() {
                  @Override
                  public void run() {
                    flameToolBar.resetZoom();
                    flameToolBar.resetMetrics(anMetric);
                    scrollToRow(0);
                  }
                });
          }
        });
  }

  protected void scrollToRow(final int row) {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            int extraPixels = 10;
            //                System.out.println(height + " " + (height - (row + 1) *
            // flamePanel.getRowHeight() + 2));
            flamePanel.scrollRectToVisible(
                new Rectangle(
                    0,
                    flamePanel.getSize().height
                        - (row + 1) * flamePanel.getRowHeight()
                        - flamePanel.getOuterGap()
                        - flamePanel.getInnerGap()
                        - extraPixels / 2,
                    10,
                    flamePanel.getRowHeight() + 1 + extraPixels));
            mainScrollPane.revalidate();
          }
        });
  }

  protected void setMetric(AnMetric anMetric) {
    anMetricToBeUsed = anMetric;
    computed = false;
    if (selected) {
      computeOnAWorkerThread();
    }
  }

  private final long getFuncObj() {
    final long funcObj;
    funcObj = AnWindow.getInstance().getSelectedObject().getSelObjV2("FUNCTION"); // API V2
    return funcObj;
  }

  @Override
  public void doCompute() {
    if (!supportedExperiments) { // Some not suppoted conditions (compare)
      return;
    }
    long selectedFunction =
        getFuncObj(); // Synchronize selected function from Functions and other views.

    if (!computed) {
      resetActionsEnableStatus();
      if (mainThreadKilled) {
        if (mainThread != null) {
          try {
            mainThread.join();
          } catch (InterruptedException ie) {
            System.err.println("mainThread.join() interrupted...");
          }
          mainThread = null;
        }
        mainThreadKilled = false;
      }
      if (mainThread != null) {
        // being computed
        return;
      }

      // Do compute
      //            beginDate = new Date();
      AnMetric anMetric;
      if (anMetricToBeUsed != null) {
        anMetric = anMetricToBeUsed;
      } else {
        anMetric = AnWindow.getInstance().getSettings().getMetricsSetting().getSortColumnMetric(0);
      }
      resetView();
      rowCount = getCallTreeNumLevelsIPC();
      updateGUI(anMetric);
      if (rowCount > 0) {
        flameData = new FlameData(this, anMetric, rowCount);

        mainThread = new MainThread(anMetric.getComd(), selectedFunction);
        mainThread.start();
      } else {
        initView(); // ???
      }
    } else {
      FlameBlock selectedFlameBlock = flameData.findFunctionFlameBlock(selectedFunction);
      if (selectedFlameBlock != null) {
        flameData.setSelectedFlameBlock(selectedFlameBlock);
        updateActionsEnableStatus();
      }
    }
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  @Override
  public int find(String str, boolean forward, boolean caseSensitive) {
    FlameBlock selectedFlameBlock = getFlameData().getSelectedFlameBlock();
    FlameBlock fb = getFlameData().nextFlameBlock(selectedFlameBlock, forward);
    while (fb != null) {
      boolean match = false;
      if (caseSensitive) {
        match = fb.getFunctionDisplayName().contains(str);
      } else {
        match = fb.getFunctionDisplayName().toLowerCase().contains(str.toLowerCase());
      }
      if (match) {
        flamePanel.setSelectedFlameBlock(fb);
        return 1;
      }
      if (fb == selectedFlameBlock) {
        return -1;
      }
      fb = getFlameData().nextFlameBlock(fb, forward);
    }
    return -1;
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

  /*
   * For GUI testing
   */
  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    return getFlameData().dump(limit);
  }

  @Override
  public List<Subview> getVisibleSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    //        list.add(window.getTimelineCallStackSubview());
    //        list.add(window.getIoCallStackSubview());
    return list;
  }

  @Override
  public List<Subview> getSelectedSubviews() {
    List<Subview> list = new ArrayList();
    list.add(window.getSelectedDetailsSubview());
    return list;
  }

  private void handleSupportedExperiments() {
    if (window.getSettings().getCompareModeSetting().comparingExperiments()) {
      // Not supported
      remove(mainToolbarPanel);
      remove(mainScrollPane);
      JLabel errorLabel = new JLabel();
      errorLabel.setHorizontalAlignment(JLabel.CENTER);
      errorLabel.getAccessibleContext().setAccessibleName(AnLocale.getString("Error message"));
      errorLabel
          .getAccessibleContext()
          .setAccessibleDescription(AnLocale.getString("Error message"));
      String msg = AnLocale.getString("Not available when comparing experiments");
      errorLabel.setText(msg);
      add(errorLabel, BorderLayout.CENTER);
      supportedExperiments = false;
    } else {
      // Supported
      if (!supportedExperiments) {
        add(mainToolbarPanel, BorderLayout.NORTH);
        add(mainScrollPane, BorderLayout.CENTER);
      }
      supportedExperiments = true;
    }
  }

  /** Called when new experiment loaded */
  private void initView() {
    mainThread = null;
    flameData = null;
    flamePanel.initView();
    resetActionsEnableStatus();
  }

  /** Called before everything is recomputed (doCompute) */
  private void resetView() {
    mainThread = null;
    flameData = null;
    flamePanel.resetView();
  }

  private void interruptMainThread() {
    if (mainThread != null) {
      mainThread.kill();
      mainThreadKilled = true;
    }
  }

  class MainThread extends Thread {

    String metricName;
    long selectedFunction;

    RowThread[] rowThreads = null;
    boolean killed = false;

    private MainThread(String metricName, long selectedFunction) {
      this.metricName = metricName;
      this.selectedFunction = selectedFunction;
    }

    @Override
    public void run() {
      killed = false;

      rowThreads = new RowThread[rowCount];
      for (int row = 0; row < rowCount; row++) {
        rowThreads[row] = new RowThread(this, metricName, row);
        rowThreads[row].start();
      }

      // Wait till all rows are filled (or thred is killed)
      synchronized (this) {
        try {
          wait();
        } catch (InterruptedException ie) {
        }
      }

      if (!killed) {
        computed = true;
        FlameBlock selectedFlameBlock = flameData.findFunctionFlameBlock(selectedFunction);
        if (selectedFlameBlock != null) {
          flameData.setSelectedFlameBlock(selectedFlameBlock);
        }
        updateActionsEnableStatus();
      }
      mainThread = null;
    }

    private void rowIsComplete(FlameRow flameRow) {
      getFlameData().rowDone(flameRow); // FIXUP: NPE
      rowThreads[flameRow.getRow()] = null;
      flamePanel.rowIsComplete(flameRow.getRow());

      // Check if all rows are done
      boolean allDone = true;
      for (RowThread rowThread : rowThreads) {
        if (rowThread != null) {
          allDone = false;
          break;
        }
      }
      if (allDone) {
        interrupt();
      }
    }

    private void kill() {
      for (RowThread rowThread : rowThreads) {
        if (rowThread != null) {
          rowThread.kill();
        }
      }
      killed = true;
      interrupt();
    }
  }

  protected FlamePanel getFlamePanel() {
    return flamePanel;
  }

  class RowThread extends Thread {

    MainThread mainThread;
    private int row;
    private String metricName;
    private boolean killed = false;

    private RowThread(MainThread mainThread, String metricName, int row) {
      this.mainThread = mainThread;
      this.metricName = metricName;
      this.row = row;
    }

    @Override
    public void run() {
      killed = false;

      List<FlameRow> flameRows = getRowDataIPC(metricName, row);

      if (!killed && !isInterrupted()) {
        for (FlameRow flameRow : flameRows) {
          mainThread.rowIsComplete(flameRow);
        }
      }
    }

    private void kill() {
      killed = true;
      interrupt();
    }
  }

  protected List<JComponent> getFilterMenuList(FlameBlock flameBlock) {
    String text;
    JMenuItem menuItem;
    List<JComponent> list = new ArrayList<JComponent>();

    text = ADD_FILTER + FILTER_SELECTED_BRANCH_LONG_NAME;
    menuItem = new JMenuItem(new FilterSelectedBranch(text, flameBlock));
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(flameBlock != null);
    list.add(menuItem);

    text = ADD_FILTER + FILTER_NOT_SELECTED_BRANCH_LONG_NAME;
    menuItem = new JMenuItem(new FilterNotSelectedBranch(text, flameBlock));
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(flameBlock != null);
    list.add(menuItem);

    text = ADD_FILTER + FILTER_SELECTED_FUNCTION_LONG_NAME;
    menuItem = new JMenuItem(new FilterSelectedFunction(text, flameBlock));
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(flameBlock != null);
    list.add(menuItem);

    text = ADD_FILTER + FILTER_SELECTED_LEAF_LONG_NAME;
    menuItem = new JMenuItem(new FilterSelectedLeaf(text, flameBlock));
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(flameBlock != null);
    list.add(menuItem);

    text = STR_ACTION_CUSTOM_FILTER;
    menuItem = new JMenuItem(new CustomFilterAction());
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    list.add(menuItem);

    list.add(new JSeparator());

    text = STR_ACTION_UNDO_FILTER;
    menuItem = new JMenuItem(new UndoFilterAction());
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(window.getFilters().canUndoLastFilter());
    list.add(menuItem);

    text = STR_ACTION_REDO_FILTER;
    menuItem = new JMenuItem(new RedoFilterAction());
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(window.getFilters().canRedoLastFilter());
    list.add(menuItem);

    text = STR_ACTION_REMOVE_ALL_FILTERS;
    menuItem = new JMenuItem(new RemoveAllFilterAction());
    menuItem.getAccessibleContext().setAccessibleDescription(text);
    menuItem.setEnabled(window.getFilters().canRemoveAllFilters());
    list.add(menuItem);

    if (window.getFilters().anyFilters()) {
      list.add(window.getFilters().removeFilterMenuItem());
    }
    return list;
  }

  private class FilterSelectedBranch extends AbstractAction {

    private FlameBlock flameBlock;

    private FilterSelectedBranch(String text, FlameBlock flameBlock) {
      super(text);
      this.flameBlock = flameBlock;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      StringBuilder buf = new StringBuilder();
      buf.append("((");
      String buf2 = "";
      FlameBlock fb = flameBlock;
      while (fb != null) {
        if (!buf2.isEmpty()) {
          buf2 = "," + buf2;
        }
        buf2 = fb.getFunctionID() + buf2;
        fb = fb.getNextFlameBlock(FlameBlock.DOWN);
      }
      buf.append(buf2);
      buf.append(") ORDERED IN " + getStack() + ")");
      String clause = buf.toString();
      setFilterClause(FILTER_SELECTED_BRANCH_SHORT_NAME, FILTER_SELECTED_BRANCH_LONG_NAME, clause);
    }
  }

  private class FilterNotSelectedBranch extends AbstractAction {

    private FlameBlock flameBlock;

    private FilterNotSelectedBranch(String text, FlameBlock flameBlock) {
      super(text);
      this.flameBlock = flameBlock;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      StringBuilder buf = new StringBuilder();
      buf.append("(!((");
      String buf2 = "";
      FlameBlock fb = flameBlock;
      while (fb != null) {
        if (!buf2.isEmpty()) {
          buf2 = "," + buf2;
        }
        buf2 = fb.getFunctionID() + buf2;
        fb = fb.getNextFlameBlock(FlameBlock.DOWN);
      }
      buf.append(buf2);
      buf.append(") ORDERED IN " + getStack() + "))");
      String clause = buf.toString();
      setFilterClause(
          FILTER_NOT_SELECTED_BRANCH_SHORT_NAME, FILTER_NOT_SELECTED_BRANCH_LONG_NAME, clause);
    }
  }

  private class FilterSelectedFunction extends AbstractAction {

    private FlameBlock flameBlock;

    private FilterSelectedFunction(String text, FlameBlock flameBlock) {
      super(text);
      this.flameBlock = flameBlock;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      StringBuilder buf = new StringBuilder();
      buf.append("((");
      buf.append("" + flameBlock.getFunctionID());
      buf.append(") IN " + getStack() + ")");
      String clause = buf.toString();
      setFilterClause(
          FILTER_SELECTED_FUNCTION_SHORT_NAME, FILTER_SELECTED_FUNCTION_LONG_NAME, clause);
    }
  }

  private class FilterSelectedLeaf extends AbstractAction {

    private FlameBlock flameBlock;

    private FilterSelectedLeaf(String text, FlameBlock flameBlock) {
      super(text);
      this.flameBlock = flameBlock;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      StringBuilder buf = new StringBuilder();
      buf.append("((" + getStack() + "+0) IN (");
      buf.append("" + flameBlock.getFunctionID());
      buf.append("))");
      String clause = buf.toString();
      setFilterClause(FILTER_SELECTED_LEAF_SHORT_NAME, FILTER_SELECTED_LEAF_LONG_NAME, clause);
    }
  }

  private String getStack() {
    String stack = "USTACK";
    Settings settings = AnWindow.getInstance().getSettings();
    if (settings.getViewModeEnabledSetting().isViewModeEnabled()) {
      ViewModeSetting viewModeSetting = settings.getViewModeSetting();
      if (viewModeSetting.get() == ViewModeSetting.ViewMode.USER) {
        stack = "USTACK";
      } else if (viewModeSetting.get() == ViewModeSetting.ViewMode.EXPERT) {
        stack = "XSTACK";
      } else if (viewModeSetting.get() == ViewModeSetting.ViewMode.MACHINE) {
        stack = "MSTACK";
      }
    }
    return stack;
  }

  private void setFilterClause(String shortName, String longName, String clause) {
    window
        .getFilters()
        .addClause(
            getAnDispTab().getTName() + ": " + shortName,
            longName,
            clause,
            FilterClause.Kind.STANDARD);
  }

  // **************** Public ************************
  protected FlameData getFlameData() {
    return flameData;
  }

  protected Object getFlameDataLock() {
    return flameDataLock;
  }

  // **************** IPC ************************
  private int getCallTreeNumLevelsIPC() {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getCallTreeNumLevels");
      anWindow.IPC().send(0);
      return anWindow.IPC().recvInt();
    }
  }

  private Object[] getCallTreeFuncsIPC(int startLevel, int endLevel) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      //            Date start = new Date();//YXXX timing debug (delete-me later)

      anWindow.IPC().send("getCallTreeLevelFuncs");
      anWindow.IPC().send(0);
      anWindow.IPC().send(startLevel);
      anWindow.IPC().send(endLevel);
      Object[] res = (Object[]) anWindow.IPC().recvObject();

      //            {//YXXX timing debug (delete-me later)
      //                Date now = new Date();
      //                long duration = now.getTime() - start.getTime();
      //                int nfuncs = (res == null) ? 0 : ((long[]) res[0]).length;
      //                System.out.println("getCallTreeFuncsIPC duration=" + duration + "ms
      // funcCount=" + nfuncs);
      //                System.out.flush();
      //            }
      return res;
    }
  }

  private Object[] getCallTreeLevelsIPC(String metricName) { // get all rows
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      //            Date start = new Date();//YXXX timing debug (delete-me later)

      anWindow.IPC().send("getCallTreeLevels");
      anWindow.IPC().send(0);
      anWindow.IPC().send(metricName);
      Object[] res = (Object[]) anWindow.IPC().recvObject();

      //            {//YXXX timing debug (delete-me later)
      //                Date now = new Date();
      //                long duration = now.getTime() - start.getTime();
      //                int row_count = res.length;
      //                System.out.println("getRowDataIPC duration=" + duration + "ms total_rows=" +
      // row_count);
      //                System.out.flush();
      //            }
      return res;
    }
  }

  private static long YXXXduration = 0;
  private static long YXXXnode_total = 0;

  private Object[] getCallTreeLevelIPC(String metricName, int row) { // get one row
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      Date start = new Date(); // YXXX timing debug (delete-me later)

      anWindow.IPC().send("getCallTreeLevel");
      anWindow.IPC().send(0);
      anWindow.IPC().send(metricName);
      anWindow.IPC().send(row);
      Object[] res = (Object[]) anWindow.IPC().recvObject();

      { // YXXX timing debug (delete-me later)
        Date now = new Date();
        long duration = now.getTime() - start.getTime();
        YXXXduration += duration;
        int item_cnt = (res != null) ? ((int[]) res[0]).length : 0;
        YXXXnode_total += item_cnt;
        System.out.printf(
            "getRowDataIPC row=%d items=%d sum_items=%d duration=%dms sum_duration=%dms\n",
            row, item_cnt, YXXXnode_total, duration, YXXXduration);
        System.out.flush();
      }
      return res;
    }
  }

  private Object[] getCallTreeChildrenIPC(
      String metricName, int row, int[] parentNodeIds) { // get node children
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      Date start = new Date(); // YXXX timing debug (delete-me later)

      anWindow.IPC().send("getCallTreeChildren");
      anWindow.IPC().send(0);
      anWindow.IPC().send(metricName);
      anWindow.IPC().send(parentNodeIds);
      Object[] res = (Object[]) anWindow.IPC().recvObject();

      { // YXXX timing debug (delete-me later)
        Date now = new Date();
        long duration = now.getTime() - start.getTime();
        YXXXduration += duration;
        int parent_cnt = 0;
        int node_cnt = 0;
        if (res != null) {
          parent_cnt = res.length;
          for (int ii = 0; ii < parent_cnt; ii++) {
            Object[] tmp = (Object[]) res[ii];
            if (tmp != null) {
              int tmp2[] = (int[]) tmp[0];
              node_cnt += tmp2 != null ? tmp2.length : 0;
            }
          }
        }
        YXXXnode_total += node_cnt;
        System.out.printf(
            "getCallTreeChildrenIPC row=%d parents=%d items=%d sum_items=%d duration=%dms"
                + " sum_duration=%dms\n",
            row, parent_cnt, node_cnt, YXXXnode_total, duration, YXXXduration);
        System.out.flush();
      }
      return res;
    }
  }

  // processing of processCallTree*IPC() data for one set of blocks
  private List<FlameBlock> processCallTreeNodeIPC(Object[] blockInfo) {
    List<FlameBlock> flameBlocks = new ArrayList<FlameBlock>();
    if (blockInfo == null) {
      return flameBlocks;
    }
    int[] blockIDs = (int[]) blockInfo[0];
    int[] parentIDs = (int[]) blockInfo[1];
    long[] functionIDs = (long[]) blockInfo[2];
    Object metricObjs = (Object) blockInfo[3]; // could be double, int, long

    if (blockIDs == null || blockIDs.length == 0) {
      return flameBlocks;
    }

    final double metricValues[];
    {
      if (metricObjs instanceof int[]) {
        int objv[] = (int[]) metricObjs;
        metricValues = new double[objv.length];
        for (int kk = 0; kk < objv.length; kk++) {
          metricValues[kk] = objv[kk];
        }
      } else if (metricObjs instanceof long[]) {
        long objv[] = (long[]) metricObjs;
        metricValues = new double[objv.length];
        for (int kk = 0; kk < objv.length; kk++) {
          metricValues[kk] = objv[kk];
        }
      } else if (metricObjs instanceof double[]) {
        double objv[] = (double[]) metricObjs;
        metricValues = new double[objv.length];
        for (int kk = 0; kk < objv.length; kk++) {
          metricValues[kk] = objv[kk];
        }
      } else {
        return flameBlocks; // weird
      }
    }
    for (int kk = 0; kk < blockIDs.length; kk++) {
      int id = blockIDs[kk];
      int parentId = parentIDs[kk];
      long functionId = functionIDs[kk];
      double exclusiveValue = 0;
      double attributedValue = metricValues[kk];
      if (kk < (blockIDs.length - 1)
          && functionId == functionIDs[kk + 1]
          && parentId == parentIDs[kk + 1]) {
        // Combine exclusive and attributed into one block
        id = blockIDs[kk + 1];
        exclusiveValue = metricValues[kk];
        attributedValue = metricValues[kk + 1];
        kk++;
      }
      flameBlocks.add(new FlameBlock(id, parentId, functionId, exclusiveValue, attributedValue));
    }

    Collections.sort(flameBlocks, new FlameBlockComrator()); // Sort by value

    return flameBlocks;
  }

  class FlameBlockComrator implements Comparator<FlameBlock> {

    @Override
    public int compare(FlameBlock o1, FlameBlock o2) {
      if (o1.getParentID() == o2.getParentID()) {
        double v1 = o1.getInclusiveValue();
        double v2 = o2.getInclusiveValue();
        if (v1 == v2) {
          return 0;
        } else if (v1 < v2) {
          return 1;
        } else {
          return -1;
        }
      } else {
        return 0; // keep parent order unchanged
      }
    }
  }

  private boolean XXXgetRowByLevel = false;
  private boolean XXXallRowsAtOnce = true;

  private void updateColorMapIPC(int row) {
    int level_start = 0, level_end = -1;
    if (XXXgetRowByLevel) {
      level_start = level_end = row;
    } else if (row != 0) {
      return;
    }
    Object[] res = getCallTreeFuncsIPC(level_start, level_end);
    if (res != null) {
      AnWindow anWindow = AnWindow.getInstance();
      StateColorMap colorMap = anWindow.getColorChooser().getColorMap();
      long[] ids = (long[]) res[0];
      String[] names = (String[]) res[1];
      long[] functions = (long[]) res[2];
      List<StackState> states = new ArrayList();
      HashMap<Long, StackState> functionIdNameMap = new HashMap<Long, StackState>();
      for (int ii = 0; ii < ids.length; ii++) {
        // Colors
        if (names[ii] == null) {
          continue;
        }
        StackState state = new StackState(names[ii], functions[ii]); // name, Histable::Function*
        states.add(state);
        // Function names
        functionIdNameMap.put(ids[ii], state);
      }
      colorMap.addStates(states);
      getFlameData().setFunctionIdNameMap(functionIdNameMap); // FIXUP: NPE
    }
  }

  /** returns row data */
  private List<FlameRow> getRowDataIPC(String metricName, int row) {
    // examples of three methods.

    updateColorMapIPC(row);

    List<FlameRow> flameRows = new ArrayList();

    if (XXXgetRowByLevel) {
      Object[] tmp = getCallTreeLevelIPC(metricName, row);
      List<FlameBlock> blocks = processCallTreeNodeIPC(tmp);
      flameRows.add(new FlameRow(getFlameData(), row, blocks));
      return flameRows;
    }

    if (row != 0) {
      // only return multi-row values on thread 0
      return flameRows;
    }

    int row_count = 0;
    int totalBlockCount = 0;
    if (XXXallRowsAtOnce) {
      // get all levels with single IPC
      Object[] tmp = getCallTreeLevelsIPC(metricName);
      if (tmp != null) {
        row_count = tmp.length;
        for (int tmpRow = 0; tmpRow < row_count; tmpRow++) {
          List<FlameBlock> blocks = processCallTreeNodeIPC((Object[]) tmp[tmpRow]);
          flameRows.add(new FlameRow(getFlameData(), tmpRow, blocks));
          totalBlockCount += blocks.size(); // YXXX sanity checking node counts
        }
      }
    } else {
      // get each level by querying list of nodes
      row_count = getCallTreeNumLevelsIPC();
      int node_idxs[] = {0}; // 0 will fetch the root node
      for (int tmpRow = 0; tmpRow < row_count; tmpRow++) {
        List<FlameBlock> blocks = new ArrayList();
        // get children of nodes in list
        Object[] res = getCallTreeChildrenIPC(metricName, tmpRow, node_idxs);
        // get results
        ArrayList<Integer> children = new ArrayList<Integer>(); // nodes for next query
        if (res != null) {
          for (int jj = 0; jj < res.length; jj++) { // should match node_idxs.length
            Object[] tmp = (Object[]) res[jj];
            List<FlameBlock> nodeBlocks = processCallTreeNodeIPC(tmp);
            for (FlameBlock block : nodeBlocks) {
              children.add(block.getID());
              blocks.add(block); // addall? // children of a given node_idxs[]
            }
          }
        }
        flameRows.add(new FlameRow(getFlameData(), tmpRow, blocks));

        // build node list for next query
        int level_total = children.size();
        if (level_total > 0) {
          totalBlockCount += level_total; // just for sanity checking
          node_idxs = new int[level_total];
          for (int kk = 0; kk < level_total; kk++) {
            node_idxs[kk] = children.get(kk);
          }
        } else if (tmpRow != row_count - 1) {
          int ii = 0; // weird!;
          break;
        }
      }
    }
    //        System.out.printf("getRowDataIPC rows=%d blocks=%d\n", row_count, totalBlockCount);
    return flameRows;
  }
}
