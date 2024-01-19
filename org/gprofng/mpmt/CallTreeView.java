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

import static org.gprofng.mpmt.util.gui.AnUtility.getImageIconBar;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.filter.FilterClause;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.settings.ViewModeSetting;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public final class CallTreeView extends AnDisplay implements ExportSupport, AnChangeListener {
  private boolean updated = false;
  protected boolean convertHWCtoTime = false; // Temporary fix for patch (CR 6985920)
  protected double clock_frequency = 1.0; // Temporary fix for patch (CR 6985920)
  protected long sel_func, sel_src;
  protected int name_col, sort_col, metric_col;
  private AnTable caller;
  private Object[][] caller_data, func_data, callee_data;
  private JTree calltree; // Call tree
  private final Object CallTreeLock =
      new Object(); // global lock to synchronize call tree modifications
  private JScrollPane tree_jsp; // Call tree scrollable pane
  private JScrollPane errorPane = null;
  private JLabel tree_label; // Call tree label
  private CT_TreeNode tnTotal = null; // Call tree node <Total>
  private boolean show_inclusive = true;
  private boolean show_exclusive = false;
  private boolean show_percentage = true;
  private boolean show_colorbars = true;
  private boolean show_tooltips = true;
  private boolean stop_expanding = false;
  private boolean stop_expanding_enabled = false;
  private final AtomicBoolean skip_build_subtree = new AtomicBoolean(false); // Optimization flag
  private boolean skip_update_summary = false; // Optimization flag
  private boolean sortByName = false;
  private int previous_metric_col = -1; // used in "Sort by Name" case
  private DefaultMutableTreeNode rootnode; // Call tree root node
  private long[] ids_callers = null;
  private long[] ids_callees = null;
  private CT_CallStack ct_callstack;
  private MetricLabel[] caller_label;
  private AnMetric[] available_metrics = new AnMetric[0];
  private String metricName; // To show metric name in the Title
  private String metricCommandName; // To get colors for icons
  final String mlistStr = "MET_CALL_AGR";
  final String typeStrFunction = "FUNCTION";
  final String subtypeStr = "0";
  final String stab_callers = "CALLERS";
  final String stab_callees = "CALLEES";
  final String stab_self = "SELF";
  private AnMenuListener menuListener;
  // Menu actions
  final String STR_ACTION_SHOWSOURCE = AnLocale.getString("Show Source");
  final String STR_ACTION_SHOWDISASM = AnLocale.getString("Show Disassembly");
  final String STR_ACTION_EXPANDNODE = AnLocale.getString("Expand Node");
  final String STR_ACTION_EXPANDBRANCH = AnLocale.getString("Expand Branch");
  final String STR_ACTION_EXPANDHOTTEST = AnLocale.getString("Expand Hottest Branch");
  final String STR_ACTION_EXPANDHOT = AnLocale.getString("Expand Hot Branches");
  final String STR_ACTION_EXPANDTREE = AnLocale.getString("Expand All Branches");
  final String STR_ACTION_STOPEXPANDING = AnLocale.getString("Stop Expanding");
  final String STR_ACTION_COLLAPSEBRANCH = AnLocale.getString("Collapse Branch");
  final String STR_ACTION_COLLAPSENODE = AnLocale.getString("Collapse Node");
  final String STR_ACTION_COLLAPSETREE = AnLocale.getString("Collapse All Branches");
  final String STR_ACTION_SETROOT = AnLocale.getString("Set Root");
  final String STR_ACTION_RESET = AnLocale.getString("Reset Root");
  final String STR_ACTION_SETMETRIC = AnLocale.getString("Set Metric");
  final String STR_ACTION_REFRESH = AnLocale.getString("Refresh Tree");
  final String STR_ACTION_COPY_ALL = AnLocale.getString("Copy All");
  final String STR_ACTION_SETTINGS = AnLocale.getString("Call Tree Settings");
  private static final String STR_SORT_BY_METRIC = AnLocale.getString("Sort By Metric");
  private static final String STR_SORT_BY_NAME = AnLocale.getString("Sort By Name");
  private static final String ADD_FILTER = AnLocale.getString("Add Filter: ");
  static final String FILTER_NAME_ACTION_NAME = "filterNameinStack";
  static final String FILTER_NAME_LONG_NAME =
      AnLocale.getString("Include only stacks containing name");
  static final String FILTER_NAME_SHORT_NAME = AnLocale.getString("Name in Stack");
  static final String FILTER_SELECTED_BRANCH_ACTION_NAME = "filterSelectedBranch";
  static final String FILTER_SELECTED_BRANCH_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected branch");
  static final String FILTER_SELECTED_BRANCH_SHORT_NAME = AnLocale.getString("Selected Branch");
  static final String FILTER_NOT_SELECTED_BRANCH_ACTION_NAME = "filterNotSelectedBranch";
  static final String FILTER_NOT_SELECTED_BRANCH_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected branch");
  static final String FILTER_NOT_SELECTED_BRANCH_SHORT_NAME =
      AnLocale.getString("Not Selected Branch");
  static final String FILTER_SELECTED_FUNCTION_ACTION_NAME = "filterSelectedfunction";
  static final String FILTER_SELECTED_FUNCTION_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected function");
  static final String FILTER_SELECTED_FUNCTION_SHORT_NAME = AnLocale.getString("Selected Function");
  static final String FILTER_SELECTED_LEAF_ACTION_NAME = "filterSelectedLeaf";
  static final String FILTER_SELECTED_LEAF_LONG_NAME =
      AnLocale.getString("Include only stacks with the selected function as leaf");
  static final String FILTER_SELECTED_LEAF_SHORT_NAME = AnLocale.getString("Selected Leaf");
  final String STR_ACTION_UNDO_FILTER = AnLocale.getString("Undo Filter Action");
  final String STR_ACTION_REDO_FILTER = AnLocale.getString("Redo Filter Action");
  final String STR_ACTION_REMOVE_ALL_FILTERS = AnLocale.getString("Remove All Filters");
  final String STR_ACTION_CUSTOM_FILTER =
      ADD_FILTER + AnLocale.getString("Advanced Custom Filter...");
  final String STR_ACTION_SHOWNEXT = AnLocale.getString("Show Next Reference To Function");
  final String STR_ACTION_SHOWALL = AnLocale.getString("Show All References To Function");
  final String STR_ACTION_HIDE_INCLUSIVE = AnLocale.getString("Hide Inclusive Metric");
  final String STR_ACTION_SHOW_INCLUSIVE = AnLocale.getString("Show Inclusive Metric");
  final String STR_ACTION_SHOW_EXCLUSIVE = AnLocale.getString("Show Exclusive Metric");
  final String STR_ACTION_HIDE_PERCENTAGE = AnLocale.getString("Hide Percentage");
  final String STR_ACTION_SHOW_PERCENTAGE = AnLocale.getString("Show Percentage");
  final String STR_ACTION_HIDE_COLORBARS = AnLocale.getString("Hide Color Bars");
  final String STR_ACTION_SHOW_COLORBARS = AnLocale.getString("Show Color Bars");
  final String STR_ACTION_HIDE_TOOLTIPS = AnLocale.getString("Hide Tool tips");
  final String STR_ACTION_SHOW_TOOLTIPS = AnLocale.getString("Show Tool tips");
  private String STR_TREE_TITLE_FUNCTIONS;
  private String STR_TREE_TITLE_COMPLETE_VIEW;
  private String STR_TREE_TITLE_PARTIAL;
  private String STR_TREE_TITLE_FILTER;
  private String acName;
  private String acDesc;
  private String lastFilter = null;

  // Constructor
  public CallTreeView() {
    super(AnWindow.getInstance(), AnDisplay.DSP_CallTree, AnVariable.HELP_TabsCallTree);

    // System.err.println("XXX In CallTreeDisp() type: " + type + " subtype: " + subtype);
    STR_TREE_TITLE_FUNCTIONS = AnLocale.getString("Call Tree: FUNCTIONS.");
    STR_TREE_TITLE_COMPLETE_VIEW = AnLocale.getString("Complete view.");
    STR_TREE_TITLE_PARTIAL = AnLocale.getString("Partial view.");
    STR_TREE_TITLE_FILTER = AnLocale.getString("Filtered view.");
    // Accessibility
    acName = AnLocale.getString("Call Tree");
    acDesc = AnLocale.getString("Functions Call Tree");
    if (null != calltree) {
      calltree.getAccessibleContext().setAccessibleName(acName);
      calltree.getAccessibleContext().setAccessibleDescription(acDesc);
    }
    if (null != tree_jsp) {
      tree_jsp.getAccessibleContext().setAccessibleName(acName);
      tree_jsp.getAccessibleContext().setAccessibleDescription(acDesc);
    }
    if (null != tree_label) {
      String text = STR_TREE_TITLE_FUNCTIONS;
      tree_label.getAccessibleContext().setAccessibleName(text);
      tree_label.getAccessibleContext().setAccessibleDescription(text);
    }
    name_col = -1;
    sort_col = -1;

    setAccessibility(AnLocale.getString("Call Tree"));
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
    //        System.out.println("CallTreeView stateChanged:  " + e.getType());
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
            || anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.CALL_TREE
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
  public void requestFocus() {
    if (calltree != null) {
      calltree.requestFocus();
    }
  }

  @Override
  public JPopupMenu getFilterPopup() {
    return initPopup(true);
  }

  /** Initialize GUI components */
  @Override
  protected void initComponents() {
    // System.err.println("XXX In CallTreeDisp.initComponents()");
    setLayout(new BorderLayout());
    // This is just a minimal initialization
    // The remaining part is in reset().
  }

  /** Reset GUI components */
  public @Override void reset() {
    // System.err.println("XXX In CallTreeDisp.reset()");
    // Remove Call Tree
    if (null != tree_jsp) {
      // Remove selection listener
      CallTreeSelectionListener[] cts_listeners =
          calltree.getListeners(CallTreeSelectionListener.class);
      for (CallTreeSelectionListener listener : cts_listeners) {
        calltree.removeTreeSelectionListener(listener);
      }
      // Remove expansion listener
      CallTreeExpansionListener[] cte_listeners =
          calltree.getListeners(CallTreeExpansionListener.class);
      for (CallTreeExpansionListener listener : cte_listeners) {
        calltree.removeTreeExpansionListener(listener);
      }
      // CT_CollapseTree();
      tnTotal = null;
      remove(tree_jsp);
    }
    // Init Call Tree
    initCallTree();

    // Add Call Tree
    tree_jsp = new AnJScrollPane(calltree);
    tree_jsp.add(tree_label);
    add(tree_jsp, BorderLayout.CENTER);

    // Popup menu
    menuListener = new AnMenuListener(this, calltree);
    calltree.addMouseListener(menuListener);
    // this.addMouseListener(menuListener); // Do we need it?

    KeyStroke ks = KeyboardShortcuts.contextMenuActionShortcut;
    calltree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, ks);
    calltree
        .getActionMap()
        .put(
            ks,
            new AbstractAction() {
              public void actionPerformed(ActionEvent ev) {
                JPopupMenu popup = initPopup(false);
                if (popup != null) {
                  JTree src = (JTree) ev.getSource();
                  Rectangle cellRect, visRect;

                  visRect = src.getVisibleRect();
                  cellRect = visRect;
                  int selrow = src.getMaxSelectionRow();
                  if (selrow >= 0) {
                    cellRect = src.getRowBounds(selrow);
                  }

                  // if current view doesn't include selected row, scroll
                  if (!visRect.contains(cellRect)) {
                    // calculate middle based on selected row
                    // being below or above current visible rows
                    if (visRect.y < cellRect.y) {
                      cellRect.y += visRect.height / 2;
                    } else {
                      cellRect.y -= visRect.height / 2;
                    }
                    src.scrollRectToVisible(cellRect);
                  }
                  popup.show(src, cellRect.x, cellRect.y + cellRect.height);
                }
              }
            });
  }

  // Init Call Tree
  private void initCallTree() {
    String text = STR_TREE_TITLE_FUNCTIONS;
    rootnode = new DefaultMutableTreeNode(STR_TREE_TITLE_FUNCTIONS, true);
    calltree = new CT_Tree(rootnode);

    tree_label = new JLabel(text);
    tree_label.setDisplayedMnemonic(AnLocale.getString('A', "MNEM_CALLTREEFUNCTIONS"));
    tree_label.setLabelFor(calltree);
    tree_label.setVisible(false);
    // Add a selection listener, to listen for selected nodes
    calltree.addTreeSelectionListener(new CallTreeSelectionListener());
    // Add an expansion listener, to listen for expanded nodes
    calltree.addTreeExpansionListener(new CallTreeExpansionListener());
    // Allow only a single node to be selected (default)
    calltree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    if (show_tooltips) {
      // Show tooltips
      javax.swing.ToolTipManager.sharedInstance().registerComponent(calltree);
    }
  }

  // Clear display
  public @Override void clear() {
    // System.err.println("XXX In CallTreeDisp.clear()");
  }

  /** Copy all lines to the system clipboard */
  protected void copyAll() {
    String text = exportAsText(null, ExportFormat.TEXT, null);
    copyToClipboard(text);
  }

  @Override
  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String text = CT_PrintTree();
    return text;
  }

  // Get new selected func index & compare to the old one
  // Note: must be called from doCompute
  private final long getFuncObj() {
    final long func_obj;

    if (sel_func <= 0) { // not initialized yet
      // need Total/Maximum from func-list data
      window
          .getFunctionsView()
          .computeIfNeeded(); // on worker thread and synchronized (AnVariable.mainFlowLock)
    }
    func_obj = window.getSelectedObject().getSelObjV2(typeStrFunction); // API V2

    if (sel_func != func_obj) {
      updated = false;
    }
    return func_obj;
  }

  private static int ThreadID = 0;
  // Semaphore
  private final Object shortLock = new Object(); // For short critical sections

  /** Compute & update call tree */
  @Override
  public void doCompute() {
    synchronized (shortLock) {
      if (inCompute) {
        return;
      }
      inCompute = true;
    }
    // Only one thread at a time
    doCompute_internal();
    synchronized (shortLock) {
      inCompute = false;
    }
  }

  // Compute & update call tree
  private void doCompute_internal() {
    long ID_to_expand;

    AnUtility.checkIfOnAWTThread(false);
    if (!selected) {
      return; // Not selected - nothing to do.
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
      if (tree_jsp != null) {
        tree_jsp.setVisible(false);
      }
      return;
    }
    if (errorPane != null) {
      errorPane.setVisible(false);
    }
    if (tree_jsp != null) {
      tree_jsp.setVisible(true);
    }

    long starttime = System.currentTimeMillis();
    long t = starttime;
    sel_func = getFuncObj(); // inside doCompute, on worker thread and synchronized
    // (AnVariable.mainFlowLock) // TEMPORARY //
    long my_sel_func = sel_func;
    // System.out.println("CallTree step 1 time: " + (System.currentTimeMillis() - t) + " ms"); //
    // DEBUG
    t = System.currentTimeMillis();

    int progress = 1;
    int version = 1;

    if (!computed) { // need re-compute
      reset();
      //            window.setBusyCursor(true); // Repeat after reset()

      DefaultMutableTreeNode dn;
      if (ct_callstack == null) {
        ct_callstack = new CT_CallStack();
      } else {
        ct_callstack.reset();
        sel_func = my_sel_func; // reset() sets sel_func to <Total>
      }
      ID_to_expand = sel_func;
      sort_col = window.getSettings().getMetricsSetting().getSortColumnByDType(DSP_CallTree);
      int buildlevel = 2; // min tree depth
      int maxlevel = 5; // tree depth
      int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
      window
          .getSystemProgressPanel()
          .setProgress(progress, AnLocale.getString("Initializing call tree"));
      dn = CT_BuildTree(calltree, buildlevel, threshold, ID_to_expand);
      // System.out.println("CallTree step 1a time: " + (System.currentTimeMillis() - t) + " ms
      // buildlevel = " + buildlevel); // DEBUG
      t = System.currentTimeMillis();
      long curtime = System.currentTimeMillis() - starttime;
      /*
      if (0 == version) { // old version
      if (curtime < 2000) {
      // Minimal building took less than 2 seconds - try to build more
      if (curtime > 0) {
      int dynmaxlevel = (int) (2000 / curtime);
      if (dynmaxlevel < maxlevel) {
      maxlevel = dynmaxlevel;
      }
      }
      if (maxlevel > 1) {
      buildlevel = maxlevel;
      progress = 20;
      threshold = current_threshold; // %
      window.setProgress(progress, "Initializing call tree");
      dn = CT_BuildTree(calltree, buildlevel, threshold, ID_to_expand);
      System.out.println("CallTree step 2a time: " + (System.currentTimeMillis() - t) + " ms    buildlevel = " + buildlevel); // DEBUG
      t = System.currentTimeMillis();
      }
      }
      } // end of old version
      */
      progress = 80;
      window
          .getSystemProgressPanel()
          .setProgress(progress, AnLocale.getString("Expanding call tree"));
      if (0 == version) { // old version
        // Expand Call Tree
        int expandlevel = 1; // min tree depth
        CT_TreeNode n = CT_ExpandTree(calltree, tnTotal, expandlevel, threshold, ID_to_expand);
        curtime = System.currentTimeMillis() - starttime;
        if (curtime < 5000) {
          // Total time is less than 5 seconds - try to expand more
          long expt = System.currentTimeMillis() - t;
          expandlevel = 30; // 20; // default tree depth
          if (expt > 0) {
            int dynmaxlevel = (int) (2000 / expt);
            if (dynmaxlevel < expandlevel) {
              expandlevel = dynmaxlevel;
            }
          }
          if (expandlevel > 1) {
            n = CT_ExpandTree(calltree, tnTotal, expandlevel, threshold, ID_to_expand);
          }
        }
        System.out.println(
            "CallTree step 3a time: "
                + (System.currentTimeMillis() - t)
                + " ms    expandlevel = "
                + expandlevel); // DEBUG
        t = System.currentTimeMillis();
        if (null == n) {
          if (null != dn) {
            progress = 90;
            window
                .getSystemProgressPanel()
                .setProgress(progress, AnLocale.getString("Expanding call tree"));
            expandlevel = dn.getDepth(); // adjusted tree depth
            CT_ExpandTree(calltree, dn, expandlevel, threshold, ID_to_expand);
            System.out.println(
                "CallTree step 4a time: " + (System.currentTimeMillis() - t) + " ms"); // DEBUG
            t = System.currentTimeMillis();
            final int row = findFirst(ID_to_expand);
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  public void run() {
                    long t = System.currentTimeMillis();
                    calltree.setSelectionRow(row);
                    showSelectedRow(row);
                    System.out.println(
                        "CallTree step 5a time: "
                            + (System.currentTimeMillis() - t)
                            + " ms"); // DEBUG
                    t = System.currentTimeMillis();
                  }
                });
          }
        }
      } // end of old version
      CT_TreeNode n = null;
      if (ID_to_expand > 0) { // Selected object
        final long[] cstack = CT_FindCallStackForID(ID_to_expand); // IPC
        if ((cstack != null) && (cstack.length > 0)) {
          long id = cstack[cstack.length - 1];
          n =
              CT_ExpandBranch(
                  calltree, tnTotal, cstack.length + 1 /*level*/, 0 /*no threshold*/, id, cstack);
        }
      }

      if (null == n) { // fall back to the old code
        int level = 100; // some big value
        CT_DynamicExpandCallTree(
            /*JTree tree,*/ dn, level, threshold, ID_to_expand, starttime, progress);
      }
      CT_UpdateTitle(); // TEMPORARY
      progress = 100;
      window.getSystemProgressPanel().setProgress(100, AnLocale.getString("Expanding call tree"));
    } else if (!updated) { // need update
      int minlevel = 1; // min tree depth
      int maxlevel = 5; // tree depth
      int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
      ID_to_expand = sel_func;
      sort_col = window.getSettings().getMetricsSetting().getSortColumnByDType(DSP_CallTree);
      DefaultMutableTreeNode dn = CT_RebuildTree(minlevel, threshold, ID_to_expand);
      // System.out.println("CallTree step 1b time: " + (System.currentTimeMillis() - t) + " ms
      // minlevel = " + minlevel); // DEBUG
      t = System.currentTimeMillis();
      long curtime = System.currentTimeMillis() - starttime;
      if (0 == version) { // old version
        if (t < 2000) {
          // Minimal building took less than 2 seconds - try to build more
          if (curtime > 0) {
            int dynmaxlevel = (int) (2000 / curtime);
            if (dynmaxlevel < maxlevel) {
              maxlevel = dynmaxlevel;
            }
          }
          if (maxlevel > 0) {
            // progress = 20;
            threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
            // window.setProgress(progress, "Initializing call tree");
            dn = CT_RebuildTree(maxlevel, threshold, ID_to_expand);
            System.out.println(
                "CallTree step 2b time: "
                    + (System.currentTimeMillis() - t)
                    + " ms    maxlevel = "
                    + maxlevel); // DEBUG
            t = System.currentTimeMillis();
          }
        }
        CT_TreeNode n = CT_ExpandTree(calltree, dn, maxlevel, threshold, ID_to_expand);
        System.out.println(
            "CallTree step 3b time: " + (System.currentTimeMillis() - t) + " ms"); // DEBUG
        t = System.currentTimeMillis();
        final int row = findFirst(ID_to_expand);
        System.out.println(
            "CallTree step 4b time: " + (System.currentTimeMillis() - t) + " ms"); // DEBUG
        t = System.currentTimeMillis();
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              public void run() {
                long t = System.currentTimeMillis();
                calltree.setSelectionRow(row);
                showSelectedRow(row);
                System.out.println(
                    "CallTree step 5b time: " + (System.currentTimeMillis() - t) + " ms"); // DEBUG
                t = System.currentTimeMillis();
              }
            });
      } // end of old version
      CT_TreeNode n = null;
      if (ID_to_expand > 0) { // Selected object
        final long[] cstack = CT_FindCallStackForID(ID_to_expand);
        if ((cstack != null) && (cstack.length > 0)) {
          long id = cstack[cstack.length - 1];
          n =
              CT_ExpandBranch(
                  calltree, tnTotal, cstack.length + 1 /*level*/, 0 /*no threshold*/, id, cstack);
        }
      }

      if (null == n) { // fall back to the old code
        int level = 100; // some big value
        CT_DynamicExpandCallTree(
            /*JTree tree,*/ dn, level, threshold, ID_to_expand, starttime, progress);
      }
      CT_UpdateTitle(); // TEMPORARY
    }

    available_metrics =
        window.getSettings().getMetricsSetting().getMetricListByMType(MetricsSetting.MET_CALL_AGR);

    window.getSystemProgressPanel().setProgress(0, "");
    // Update selection?
    // ...
    computed = true;
    updated = true;
    // System.out.println("CallTree doCompute time: " + (System.currentTimeMillis() - starttime) + "
    // ms"); // DEBUG
  }

  /*
   * Experimental code to localize performance and responsiveness issues
   */
  private int CT_DynamicExpandCallTree(
      /*JTree tree,*/ DefaultMutableTreeNode dn,
      int level,
      int threshold,
      long ID_to_expand,
      long starttime,
      int progress) {
    // Expand Call Tree
    int expandlevel = 1; // min tree depth
    CT_TreeNode n = CT_ExpandTree(calltree, tnTotal, expandlevel, threshold, ID_to_expand);
    long t = System.currentTimeMillis();
    long curtime = System.currentTimeMillis() - starttime;
    if (curtime < 5000) {
      // Total time is less than 5 seconds - try to expand more
      long expt = System.currentTimeMillis() - t;
      expandlevel = level; // 20; // default tree depth
      if (expt > 0) {
        int dynmaxlevel = (int) (2000 / expt);
        if (dynmaxlevel < expandlevel) {
          expandlevel = dynmaxlevel;
        }
      }
      if (expandlevel > 1) {
        n = CT_ExpandTree(calltree, tnTotal, expandlevel, threshold, ID_to_expand);
      }
    }
    // System.out.println("CallTree step 3a time: " + (System.currentTimeMillis() - t) + " ms
    // expandlevel = " + expandlevel); // DEBUG
    t = System.currentTimeMillis();
    if (null == n) {
      if (null != dn) {
        progress = 90;
        window
            .getSystemProgressPanel()
            .setProgress(progress, AnLocale.getString("Expanding call tree"));
        expandlevel = dn.getDepth(); // adjusted tree depth
        CT_ExpandTree(calltree, dn, expandlevel, threshold, ID_to_expand);
        // System.out.println("CallTree step 4a time: " + (System.currentTimeMillis() - t) + " ms");
        // // DEBUG
        t = System.currentTimeMillis();
        final int row = findFirst(ID_to_expand);
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              public void run() {
                long t = System.currentTimeMillis();
                calltree.setSelectionRow(row);
                showSelectedRow(row);
                // System.out.println("CallTree step 5a time: " + (System.currentTimeMillis() - t) +
                // " ms"); // DEBUG
                t = System.currentTimeMillis();
              }
            });
      }
    }
    return progress;
  }

  @Override
  protected boolean supportsFindText() {
    return true;
  }

  /**
   * Find a tree node that contains the specified string
   *
   * @param str
   * @param forward Direction: true - forward, false - backward
   * @return row
   */
  @Override
  public int find(final String str, final boolean forward, boolean caseSensitive) {
    final boolean do_selection = true;
    int sel_row = -1;
    long id = -1;
    boolean wrap_around = true;
    return (find(str, forward, id, sel_row, wrap_around, do_selection, caseSensitive));
  }

  /**
   * Find a tree node that contains the specified ID
   *
   * @param id
   * @param sel_row
   * @param forward
   * @param wrap_around
   * @return row
   */
  private int find_not_used(
      long id,
      int sel_row,
      final boolean forward,
      final boolean wrap_around,
      boolean caseSensitive) {
    final boolean do_selection = true;
    final String str = null;
    return (find(str, forward, id, sel_row, wrap_around, do_selection, caseSensitive));
  }

  /**
   * Find a tree node that contains the specified ID
   *
   * @param id
   * @param sel_row
   * @return row
   */
  private int find(long id, int sel_row, final boolean do_selection, boolean caseSensitive) {
    final boolean forward = true;
    final boolean wrap_around = true;
    final String str = null;

    // Expand all branches that contain this id
    CT_ExpandAllCallStacksForID(id);
    // Update selected row
    sel_row = calltree.getMaxSelectionRow();

    return (find(str, forward, id, sel_row, wrap_around, do_selection, caseSensitive));
  }

  /**
   * Find a tree node that contains the specified string or specified ID
   *
   * @param str
   * @param forward
   * @param sel_row
   * @param id
   * @param wrap_around
   * @return row
   */
  private int find(
      String str,
      final boolean forward,
      long id,
      int sel_row,
      final boolean wrap_around,
      final boolean do_selection,
      boolean caseSensitive) {
    final JTree t = calltree;
    int found_row = -1;
    int last_found_row = -1;
    int row = 0;
    int selected_row = t.getMaxSelectionRow();
    if (sel_row >= 0) {
      selected_row = sel_row;
    }
    CT_TreeNode total = tnTotal;
    if (total == null) {
      return found_row;
    }
    if (!caseSensitive && null != str) {
      str = str.toLowerCase();
    }
    CT_TreeNode ctn = total;
    CT_TreeNode found_node = ctn;
    Enumeration num = ctn.depthFirstEnumeration(); // ctn.breadthFirstEnumeration();
    while (num.hasMoreElements()) {
      ctn = (CT_TreeNode) num.nextElement();
      TreePath path = new TreePath(ctn.getPath());
      row = t.getRowForPath(path);
      if ((row == selected_row) || (row < 0) /* not expanded */) {
        continue;
      }
      if (!wrap_around) { // Skip these checks for "Wrap around"
        if (row < selected_row) {
          if (forward) {
            continue;
          }
        } else {
          if (!forward) {
            continue;
          }
        }
      }
      boolean found = false;
      if (null != str) { // Compare strings
        String text = ctn.getUserObject().toString();
        if (!caseSensitive) {
          text = text.toLowerCase();
        }
        if (text.contains(str)) {
          found = true;
        }
      } else { // Compare IDs
        if (ctn.ID == id) {
          found_node = ctn;
          found = true;
        }
      }
      if (found) {
        if (wrap_around) {
          if (found_row < 0) { // nothing found yet
            found_row = row;
          }
        }
        if (!forward) {
          if ((row > last_found_row) || (last_found_row == -1)) {
            if (row < selected_row) { // for "Wrap around"
              last_found_row = row;
              // System.out.println("Found prev: "+ctn.getUserObject().toString()+" ID="+ctn.ID);
            }
          }
          if (wrap_around) {
            if (found_row < row) {
              found_row = row;
            }
          }
        } else { // forward
          if ((row < last_found_row) || (last_found_row == -1)) {
            if (row > selected_row) { // for "Wrap around"
              last_found_row = row;
            }
          }
          if (wrap_around) {
            if (found_row > row) {
              found_row = row;
            }
          }
        }
      }
    }
    if (last_found_row >= 0) {
      found_row = last_found_row;
    }
    if (do_selection) { // select the row
      if (found_row >= 0) {
        final int select_row = found_row;
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              // @Override
              public void run() {
                t.setSelectionRow(select_row);
                showSelectedRow(select_row);
              }
            });
        // Set Selected Object and update Summary
        final long ID = found_node.ID;
        AnUtility.dispatchOnAWorkerThread(
            new Runnable() {
              @Override
              public void run() {
                ct_callstack.setSelectedObject(ID);
                window.getSelectionManager().updateSelection();
              }
            },
            "call tree selection");
      }
    }
    return found_row;
  }

  /**
   * Find the first tree node that contains the specified ID
   *
   * @param id
   * @param do_selection
   * @return row
   */
  private int findFirst(long id) {
    final JTree t = calltree;
    int found_row = -1;
    int row = 0;
    CT_TreeNode total = tnTotal;
    if (total == null) {
      return found_row;
    }
    CT_TreeNode ctn = total;
    Enumeration num = ctn.depthFirstEnumeration();
    while (num.hasMoreElements()) {
      ctn = (CT_TreeNode) num.nextElement();
      TreePath path = new TreePath(ctn.getPath());
      row = t.getRowForPath(path);
      if ((row < 0) /* not expanded */) {
        continue;
      }
      if (ctn.ID == id) {
        found_row = row;
        break;
      }
    }
    return found_row;
  }

  /** Show selected row via scrolling up or down Note: use AWT Thread to call this method */
  public void showSelectedRow(int row) {
    final Rectangle cellRect, visRect;
    if (row < 0) {
      return;
    }

    cellRect = calltree.getRowBounds(row);
    visRect = calltree.getVisibleRect();
    //        if (cellRect == null) {
    //            return;
    //        }
    //        if (visRect == null) {
    //            return;
    //        }
    if (visRect.x + visRect.width > cellRect.x) {
      // Visible area is wide enough to show the object
      cellRect.x = visRect.x;
    }

    // if current view doesn't include selected row, scroll
    if (!visRect.contains(cellRect)) {
      // calculate middle based on selected row
      // being below or above current visible rows
      if (visRect.y < cellRect.y) {
        cellRect.y += visRect.height / 2;
      } else {
        cellRect.y -= visRect.height / 2;
      }
      calltree.scrollRectToVisible(cellRect);
    }

    // Set row selection
    if (calltree.getMaxSelectionRow() != row) {
      calltree.setSelectionRow(row);
    }
  }

  /**
   * Sorting is not allowed in the Call Tree
   *
   * @param index
   */
  public void sort(final int index) {
    // No sorting
  }

  Object[][] MaximumValues = null;

  // Process the raw data from mixed Primary/Object to pure Object array.
  // Creates and updates MaximumValues.
  protected Object[][] localProcessData(final Object[] raw_data) {
    int i;
    AnAddress aa = new AnAddress(0);
    AnDouble ad = new AnDouble(0.0);
    AnInteger ai = new AnInteger(0);
    AnLong al = new AnLong(0);
    AnString as = new AnString("");
    final int nc = raw_data.length - 1;
    final Object[][] data = new Object[nc][];

    MaximumValues = new Object[2][nc];
    for (i = 0; i < nc; i++) {
      if (raw_data[i] instanceof double[]) {
        data[i] = AnDouble.toArray((double[]) raw_data[i]);
        MaximumValues[1][i] = ad;
      } else if (raw_data[i] instanceof int[]) {
        data[i] = AnInteger.toArray((int[]) raw_data[i]);
        MaximumValues[1][i] = ai;
      } else if (raw_data[i] instanceof long[]) {
        if ((((long[]) raw_data[i]).length == 0)
            || !AnAddress.isAddress(((long[]) raw_data[i])[0])) {
          data[i] = AnLong.toArray((long[]) raw_data[i]);
          MaximumValues[1][i] = al;
        } else {
          data[i] = AnAddress.toArray((long[]) raw_data[i]);
          MaximumValues[1][i] = aa;
        }
      } else {
        data[i] = (Object[]) raw_data[i];
        MaximumValues[1][i] = as;
      }
      updateTotalMax(data[i], i);
    }
    for (i = 0; i < nc; i++) {
      // make sure there are no null elements in MaximumValues[1]
      if (MaximumValues[1][i] == null) {
        MaximumValues[1][i] = ad;
      }
    }
    return data;
  }

  /*
   * Updates MaximumValues (Total and Max values)
   * Note: Total is not used, so only Maximum is updated.
   */
  void updateTotalMax(Object[] table_column, int index) {
    for (int i = 0; i < table_column.length; i++) {
      if (MaximumValues[1][index] == null) {
        MaximumValues[1][index] = table_column[i];
        continue;
      }
      int j = MaximumValues[1][index].toString().length();
      int k = table_column[i].toString().length();
      if (j < k) {
        MaximumValues[1][index] = table_column[i];
      }
    }
  }

  /*
   * Returns MaximumValues (Total and Max values), calculated by processData()
   */
  Object[] getTotalMax() {
    return MaximumValues;
  }

  // Create the filter clause based on the selected objects
  public String createFilterClause(String action) {
    long ID = -1;
    long[] cstack = null;
    String clause = null;
    // int selected = calltree.getLeadSelectionRow();

    TreePath tp = calltree.getSelectionPath();
    DefaultMutableTreeNode dn;
    if ((tp != null) && (tp.getPathCount() > 1)) {
      dn = (DefaultMutableTreeNode) tp.getLastPathComponent();
      if (dn instanceof CT_TreeNode) {
        ID = ((CT_TreeNode) dn).ID;
        cstack = ((CT_TreeNode) dn).stack;
      }
    }
    if (ID >= 0) {
      clause = "(" + ID + ")";
      String stack = "USTACK";
      if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.MACHINE) {
        stack = "MSTACK";
      }
      if (window.getSettings().getViewModeSetting().get() == ViewModeSetting.ViewMode.EXPERT) {
        stack = "XSTACK";
      }
      if (action.equals(STR_ACTION_REMOVE_ALL_FILTERS)) {
        clause = "1";
      }
      if (action.equals(FILTER_NAME_ACTION_NAME)) {
        clause = window.getObjNameV2(ID);
        clause = "(FNAME(\".*" + clause + ".*\") SOME IN " + stack + ")";
      }
      if (action.equals(FILTER_SELECTED_FUNCTION_ACTION_NAME)) {
        clause = "(" + clause + " IN " + stack + ")";
      }
      if (action.equals(FILTER_SELECTED_LEAF_ACTION_NAME)) {
        // clause = "(LEAF IN " + clause + ")";
        clause = "((" + stack + "+0) IN " + clause + ")";
      }
      if (action.equals(FILTER_SELECTED_BRANCH_ACTION_NAME)) {
        if ((cstack != null) && (cstack.length > 0)) {
          clause = "(";
          for (int i = 0; ; ) {
            clause = clause + cstack[i];
            if (++i < cstack.length) {
              clause = clause + ",";
            } else {
              clause = clause + ")";
              break;
            }
          }
          clause = "(" + clause + " ORDERED IN " + stack + ")";
        }
      }
      if (action.equals(FILTER_NOT_SELECTED_BRANCH_ACTION_NAME)) {
        if ((cstack != null) && (cstack.length > 0)) {
          clause = "(";
          for (int i = 0; ; ) {
            clause = clause + cstack[i];
            if (++i < cstack.length) {
              clause = clause + ",";
            } else {
              clause = clause + ")";
              break;
            }
          }
          clause = "(!(" + clause + " ORDERED IN " + stack + "))";
        }
      }
    }
    return clause;
  }

  // CallTree tree
  private class CT_Tree extends JTree {

    private CT_TreeRenderer renderer;

    public CT_Tree() {
      super();
      init();
    }

    public CT_Tree(TreeNode root) {
      super(root);
      init();
    }

    private void init() {
      AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Call Tree"));
      if (AnEnvironment.isLFNimbus()) {
        setBackground(AnEnvironment.NIMBUS_TREE_BACKGROUND);
      }
      ((DefaultTreeCellRenderer) getCellRenderer()).setLeafIcon(null);
      ((DefaultTreeCellRenderer) getCellRenderer()).setClosedIcon(null);
      ((DefaultTreeCellRenderer) getCellRenderer()).setOpenIcon(null);
      renderer = new CT_TreeRenderer();
      setCellRenderer(renderer);
    }

    // Call Tree cell renderer
    private final class CT_TreeRenderer extends DefaultTreeCellRenderer {

      public Component getTreeCellRendererComponent(
          final JTree tree,
          Object value,
          final boolean sel,
          final boolean expanded,
          final boolean leaf,
          final int row,
          final boolean hasFocus) {
        final Object object;
        Icon icon = null;

        setLeafIcon(null);
        setClosedIcon(null);
        setOpenIcon(null);

        if (value instanceof DefaultMutableTreeNode) {
          object = value; // NM ((DefaultMutableTreeNode) value).getUserObject();
          if (object instanceof CT_TreeNode) {
            final CT_TreeNode item = (CT_TreeNode) object;
            value = item.getText();
            if (show_colorbars) {
              icon = item.getIcon();
            }
            if (show_tooltips) {
              setToolTipText(metricName + value);
            }
          } else if (object instanceof JComponent) {
            // FINDBUGS: reports this is always false.  Is there a JComponent that is a
            // DefaultMutableTreeNode?
            return (Component) object;
          }
        }

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (icon != null) {
          setIcon(icon);
        }
        return this;
      }
    }
  }

  // CallTree icons
  private static ImageIcon p_icon[] = new ImageIcon[101];

  private static void cleanMetricIcons() {
    for (int i = 0; i < p_icon.length; i++) {
      p_icon[i] = null;
    }
  }

  // CallTree node
  private class CT_TreeNode extends DefaultMutableTreeNode {

    private String sname;
    private long ID;
    private long[] stack;
    private double inclusive = 0.0;
    private AnObject incValue;
    private double exclusive;
    private int callees;
    private int inc_percent = 0;
    private String text = null;
    private javax.swing.Icon icon = null;

    public CT_TreeNode(String name, boolean b) {
      super(name, b);
      this.sname = name;
      callees = -1;
    }

    // To String
    public String toString() {
      return getText();
    }

    // Get text
    public String getText() {
      if (null == text) {
        text = generateTextAndIcon();
      }
      return text;
    }

    // Get icon
    public Icon getIcon() {
      if (null == icon) {
        int n = inc_percent;
        if (n < 1) {
          return null;
        }
        if (n > 100) {
          n = 100;
        }
        if (null == p_icon[n]) {
          String metric = metricCommandName;
          int height = 10; // 10 should be replaced with Font height
          p_icon[n] = getImageIconBar(metric, n, height);
        }
        icon = p_icon[n];
      }
      return icon;
    }

    /**
     * Set Inclusive Value
     *
     * @param value
     */
    public void setInclusiveValue(AnObject value) {
      this.incValue = value;
      if (null != incValue) {
        inclusive = incValue.doubleValue();
      }
    }

    // Set Exclusive Value
    public void setExclusiveValue(double value) {
      exclusive = value;
    }

    // Set Name
    public void setName(String name) {
      sname = name;
    }

    public String generateTextAndIcon() {
      double adjustment = 0.0005;
      double inclusive_value = inclusive;
      if (inclusive_value > 0.0) {
        inclusive_value = inclusive_value + adjustment;
      }
      // Code to generate visible node name
      String m = "";
      if (convertHWCtoTime) {
        inclusive_value = inclusive_value * clock_frequency; // "clock_frequency" is 1/frequency
      }
      if (show_inclusive) {
        m = " " + incValue.toString();
      }
      if (show_percentage) {
        int percentage = 0;
        if (tnTotal != null) {
          double total_incValue = tnTotal.incValue.doubleValue();
          if (total_incValue > 0.) {
            if (total_incValue != inclusive) {
              double d = inclusive * 100 / total_incValue;
              percentage = (int) (d + 0.49);
            } else {
              percentage = 100;
            }
            m += "   (" + percentage + "%)  ";
          }
        }
        inc_percent = percentage; // To pick appropriate icon
      }
      String s = " " + m + "  " + sname; //  + "  (ID=" + ID + ")";
      return s;
    }
  }

  /*
   * Add a node to the Call Tree
   */
  private void CT_AddTreeNode(final DefaultMutableTreeNode parent, final CT_TreeNode tn) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            parent.add(tn); // PERFORMANCE: 30% real time, call from CT_BuildSubTree
          }
        });
  }

  /*
   * Add nodes to the Call Tree
   */
  private void CT_AddTreeNodes(final CT_TreeNode parent, final CT_TreeNode[] tns) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            if (parent.callees < 0) {
              parent.callees = 0;
            }
            parent.callees += tns.length;
            for (int i = 0; i < tns.length; i++) {
              parent.add(tns[i]); // PERFORMANCE: ?% real time, call from CT_BuildSubTree
            }
          }
        });
  }

  /*
   * Remove a node from the Call Tree
   */
  private void CT_RemoveTreeNode_notused(final DefaultMutableTreeNode parent, final int nodeIndex) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            parent.remove(nodeIndex);
          }
        });
  }

  /** Collapse all tree nodes */
  public void CT_CollapseTree() {
    final JTree t = calltree;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            int row = t.getMaxSelectionRow();
            if (row < 1) {
              row = 1; // Do not collapse root node
            }
            for (; row >= 1; row--) {
              t.collapseRow(row);
            }
          }
        });
  }

  /** Collapse selected node */
  public void CT_CollapseNode() {
    final JTree t = calltree;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            int row = t.getMaxSelectionRow();
            if (row < 1) {
              row = 1; // Do not collapse root node
            }
            t.collapseRow(row);
          }
        });
  }

  /** Build the Call Tree */
  public DefaultMutableTreeNode CT_BuildTree(
      JTree tree, int maxlevel, int threshold, long ID_to_expand) {
    Object[] raw_data;
    CT_TreeNode tn;
    DefaultMutableTreeNode anode;
    DefaultMutableTreeNode node_to_expand = null;
    Vector nextNodes;
    Vector prevNodes;
    String nodeName;

    double minValue = 0.0;

    AnObject[] incMetrics = null;

    // long t = System.currentTimeMillis(); // DEBUG
    anode = rootnode;
    if (anode.getChildCount() > 0) {
      // Collapse all tree nodes
      CT_CollapseTree(/*calltree*/ );

      // Reset selection
      calltree.clearSelection();
    }

    // Step 1: add <Total>
    // get call stack with one element: <Total>
    ct_callstack = new CT_CallStack();
    raw_data = getCalleeList(null);
    ct_callstack.append(0);
    // get callers of <Total> - this is necessary to initialize data
    long[] cstack = ct_callstack.getIDs();
    raw_data = getCallerList(cstack); // to initialize name_col, metric_col and sort_col

    CT_UpdateTitle();

    cstack = ct_callstack.getIDs();
    String[] names = ct_callstack.getNames();

    nodeName = names[0];

    raw_data = getFuncItemData(cstack); // getFuncItemData() creates func_data
    if (func_data[metric_col] instanceof AnObject[]) {
      incMetrics = (AnObject[]) func_data[metric_col];
      show_inclusive = true;
      show_percentage = true;
      if (incMetrics instanceof AnAddress[]) {
        show_percentage = false;
      }
      // If sort metric is IPC or CPI - don't show percentage
      if (null != metricName) {
        String mn1 = "Instructions Per Cycle";
        String mn2 = AnLocale.getString("Attributed ") + mn1;
        if (metricName.equals(mn1) || metricName.equals(mn2)) {
          show_percentage = false;
        }
        if (false != show_percentage) {
          mn1 = "Cycles Per Instruction";
          mn2 = AnLocale.getString("Attributed ") + mn1;
          if (metricName.equals(mn1) || metricName.equals(mn2)) {
            show_percentage = false;
          }
        }
      }
    } else {
      // Names only. Don't show metric value and percentage
      show_inclusive = false;
      show_percentage = false;
    }

    // build the node as CT_TreeNode. We need a synchronization here
    if (tnTotal == null) {
      tnTotal = new CT_TreeNode(nodeName, true);
    }
    tnTotal.ID = cstack[0];
    tnTotal.stack = cstack;
    if ((incMetrics != null) && (incMetrics.length > 0) && (incMetrics[0] != null)) {
      tnTotal.setInclusiveValue(incMetrics[0]);
    }
    // System.out.println("DEBUG: CallTreeDisp.CT_BuildTree: tnTotal.ID=" + tnTotal.ID);
    // Remove all children
    // tnTotal.removeAllChildren();
    if (tnTotal.getParent() == null) {
      CT_AddTreeNode(rootnode, tnTotal); // Should be here only once!
    }
    if (node_to_expand == null) {
      if (tnTotal.ID == ID_to_expand) {
        node_to_expand = tnTotal;
      }
    }
    prevNodes = new Vector(1);
    prevNodes.add(tnTotal);
    int nrows = prevNodes.size();

    int progress = 1;
    window
        .getSystemProgressPanel()
        .setProgress(progress, AnLocale.getString("Initializing call tree"));

    // Step 2: add callees to tnTotal node
    nextNodes = new Vector(0); // Next level nodes
    int level = 1;
    for (int row = 0; /* row < nrows */ ; ) { // We don't know yet how many rows this tree will have
      CT_TreeNode dn = (CT_TreeNode) prevNodes.elementAt(row);
      CT_TreeNode[] tns = getChildrenOfTreeNode(dn, false);

      if (++progress > 98) {
        progress = 98;
      } else {
        window
            .getSystemProgressPanel()
            .setProgress(progress, AnLocale.getString("Initializing call tree"));
      }

      // add children
      for (int i = 0; i < tns.length; i++) {
        tn = tns[i];
        // Add this node to the Call Tree
        // NM ((DefaultMutableTreeNode) prevNodes.elementAt(row)).add(tn);
        // NM Use AWT-thread to add nodes
        final DefaultMutableTreeNode parent;
        final CT_TreeNode child = tn;
        parent = ((DefaultMutableTreeNode) prevNodes.elementAt(row));
        CT_AddTreeNode(parent, child);
        if ((minValue > 0.0) && (tn.inclusive < minValue)) { // TEMPORARY
          // Do not expand this node -
          // its value is less than threshold
          // try to add its children
          addChildren(tn);
        } else {
          // Save for the next level
          nextNodes.add(tn);
        }
        if (ID_to_expand > 0) {
          if (node_to_expand == null) { // Not found yet
            if (tn.ID == ID_to_expand) {
              node_to_expand = tn;
            }
          }
        }
      }
      if (++row == nrows) {
        // this level is done, go one level deeper
        // System.out.println("DEBUG: CallTreeDisp.CT_BuildTree: level=" + level + " is done.");
        if (++level > maxlevel) {
          // We are done, the tree is built deep enough
          // NM Add fake child to each "leaf" node for future
          for (int i = 0; i < nextNodes.size(); i++) {
            tn = (CT_TreeNode) nextNodes.elementAt(i);
            // try to add its children
            addChildren(tn);
          }
          break;
        }
        nrows = nextNodes.size();
        if (nrows == 0) {
          // We are done, the tree is built
          break;
        }
        prevNodes = nextNodes;
        nextNodes = new Vector(0); // Next level nodes
        row = 0;
      }
      cstack = (long[]) ((CT_TreeNode) prevNodes.elementAt(row)).stack;
      ct_callstack.setIDs(cstack);
    }
    // t = System.currentTimeMillis() - t;
    // System.out.println("DEBUG: CallTreeDisp.CT_BuildTree(maxlevel=" + maxlevel + "): time=" + t +
    // " mls.");

    // Finally add all nodes to the tree
    // Probably we have to use EventDispatchThread there
    // ((DefaultMutableTreeNode)tree.getModel().getRoot()).add(tnTotal);
    window.getSystemProgressPanel().setProgress(99, AnLocale.getString("Initializing call tree"));
    return node_to_expand;
  }

  private void CT_ExpandAllCallStacksForID(long id) {
    // Expand all branches that contain this id
    final Vector<long[]> cstacks = CT_FindAllCallStacksForID(id); // IPC
    if (cstacks != null) {
      int len = cstacks.size();
      for (int i = 0; i < len; i++) {
        long[] cstack = cstacks.elementAt(i);
        if ((cstack != null) && (cstack.length > 0)) {
          CT_ExpandBranch(
              calltree,
              tnTotal,
              cstack.length + 1 /*level*/,
              0 /*no threshold*/,
              -1 /*no select*/,
              cstack);
        }
      }
    }
  }

  /**
   * Find Call Stack For ID
   *
   * @param id
   * @return stack
   */
  private long[] CT_FindCallStackForID(long id) {
    if (id <= 0) {
      return null;
    }
    long[] stack = new long[1];
    stack[0] = id;
    long[] cstack = CT_FindCallStackForStackFragment(stack);
    return cstack;
  }

  /**
   * Find Call Stack For Stack Fragment
   *
   * @return stack
   */
  private long[] CT_FindCallStackForStackFragment(long[] stack) {
    if (null == stack || stack.length <= 0) {
      return null;
    }
    CT_TreeNode total = tnTotal;
    if (null == total) {
      return null;
    }
    long totalID = total.ID;
    // build call stack up to <Total>
    CT_CallStack callstack = new CT_CallStack();
    long[] cstack = stack; // new long[1];
    callstack.setIDs(cstack);
    Object[] raw_data = getCallerList(cstack); // IPC, updates ids_callers
    while ((ids_callers != null) && (ids_callers.length > 0)) {
      callstack.prepend(0);
      cstack = callstack.getIDs();
      if (cstack[0] == totalID) {
        break;
      }
      raw_data = getCallerList(cstack); // IPC, updates ids_callers
    }
    return cstack;
  }

  /**
   * Find All Call Stacks For ID
   *
   * @param id
   * @return Vector
   */
  private Vector<long[]> CT_FindAllCallStacksForID(long id) {
    // Get all callers
    long[] stack = new long[1];
    stack[0] = id;
    CT_TreeNode total = tnTotal;
    if (null == total) {
      return null;
    }
    long totalID = total.ID;
    Vector v = new Vector();
    CT_FindAllCallStacksForStackFragment(stack, totalID, v);
    return (v);
  }

  /**
   * Find All Call Stacks For StackFragment. Recursive. Updates Vector.
   *
   * @param stack
   * @param total
   * @param Vector
   */
  private void CT_FindAllCallStacksForStackFragment(long[] stack, long total, Vector<long[]> v) {
    CT_CallStack callstack = new CT_CallStack();
    callstack.setIDs(stack);
    getCallerList(stack); // IPC. Updates ids_callers
    if ((ids_callers == null) || (0 >= ids_callers.length)) {
      return;
    }
    long[] callers = new long[ids_callers.length]; // need a copy
    for (int i = 0; i < ids_callers.length; i++) {
      callers[i] = ids_callers[i];
    }
    // Get all call stacks
    for (int i = 0; i < callers.length; i++) {
      long[] cstack = new long[stack.length + 1]; // a copy + caller
      cstack[0] = callers[i];
      for (int j = 0; j < stack.length; j++) {
        cstack[j + 1] = stack[j];
      }
      if (total == callers[i]) {
        v.add(cstack); // Add call stack
        continue;
      }
      CT_FindAllCallStacksForStackFragment(cstack, total, v);
    }
    return;
  }

  /*
   * Rebuild the Call Tree
   */
  public DefaultMutableTreeNode CT_RebuildTree(int maxlevel, int threshold, long ID_to_expand) {
    Object[] raw_data;
    CT_TreeNode tn;
    DefaultMutableTreeNode anode;
    DefaultMutableTreeNode node_to_expand = null;
    Vector nextNodes;
    Vector prevNodes;

    // double minValue = 0.0;
    AnObject[] incMetrics = null;

    // long t = System.currentTimeMillis(); // DEBUG
    // Initialize name_col, metric_col and sort_col
    if (ct_callstack == null) {
      ct_callstack = new CT_CallStack();
    } else {
      ct_callstack.reset();
    }
    long[] cstack = ct_callstack.getIDs();
    raw_data = getCallerList(cstack);
    if ((rootnode == null) || (tnTotal == null)) {
      return node_to_expand; // The tree is not initialized yet
    }

    CT_UpdateTitle();

    anode = rootnode;
    if (anode.getChildCount() > 0) {
      // Collapse all tree nodes
      // CT_CollapseTree();
      // Reset selection
      // calltree.clearSelection();
    }

    // Step 1: update <Total>
    cstack = new long[1];
    cstack[0] = tnTotal.ID;
    /* raw_data = */ getFuncItemData(cstack); // getFuncItemData() creates func_data
    if (func_data[metric_col] instanceof AnObject[]) {
      incMetrics = (AnObject[]) func_data[metric_col];
      show_inclusive = true;
      show_percentage = true;
      if (incMetrics instanceof AnAddress[]) {
        show_percentage = false;
      }
      // If sort metric is IPC or CPI - don't show percentage
      if (null != metricName) {
        String mn1 = "Instructions Per Cycle";
        String mn2 = AnLocale.getString("Attributed ") + mn1;
        if (metricName.equals(mn1) || metricName.equals(mn2)) {
          show_percentage = false;
        }
        if (false != show_percentage) {
          mn1 = "Cycles Per Instruction";
          mn2 = AnLocale.getString("Attributed ") + mn1;
          if (metricName.equals(mn1) || metricName.equals(mn2)) {
            show_percentage = false;
          }
        }
      }
    } else {
      // Names only. Don't show metric value and percentage
      show_inclusive = false;
      show_percentage = false;
    }
    // tnTotal.ID = cstack[0];
    // tnTotal.stack = cstack;
    if ((incMetrics != null) && (incMetrics.length > 0) && (incMetrics[0] != null)) {
      tnTotal.setInclusiveValue(incMetrics[0]);
    }

    // Step 2: update tnTotal children
    prevNodes = new Vector(1);
    prevNodes.add(tnTotal);
    int nrows = prevNodes.size();
    nextNodes = new Vector(0); // Next level nodes
    int level = 1;
    for (int row = 0; /* row < nrows */ ; ) {
      { // new version
        CT_TreeNode pn = (CT_TreeNode) prevNodes.elementAt(row);
        CT_TreeNode[] tns = getChildrenOfTreeNode(pn, false);
        // update children
        CT_TreeNode child = null;
        int k = pn.getChildCount();
        for (int i = 0; i < k; i++) {
          boolean found = false;
          child = (CT_TreeNode) pn.getChildAt(i);
          for (int j = 0; j < tns.length; j++) {
            tn = tns[j];
            if (child.ID == tn.ID) {
              child.setInclusiveValue(tn.incValue);
              child.inclusive = tn.inclusive;
              child.exclusive = tn.exclusive;
              child.sname = tn.sname;
              child.stack = tn.stack;
              found = true;
              break;
            }
          }
          nextNodes.add(child);
          if (!found) {
            // Child is filtered out. Ideally we shall remove it.
            child.inclusive = 0.0; // TEMPORARY
            child.exclusive = 0.0; // TEMPORARY
            continue;
          }
          if (ID_to_expand > 0) {
            if (node_to_expand == null) {
              if (child.ID == ID_to_expand) {
                node_to_expand = child;
              }
            }
          }
        }
      } // end of new version
      if (++row == nrows) {
        // this level is done, go one level deeper
        // System.out.println("DEBUG: CallTreeDisp.CT_RebuildTree: level=" + level + " is done.");
        if (++level > maxlevel) {
          break;
        }
        nrows = nextNodes.size();
        if (nrows == 0) {
          // We are done, the tree is rebuilt
          break;
        }
        prevNodes = nextNodes;
        nextNodes = new Vector(0); // Next level nodes
        row = 0;
      }
      cstack = (long[]) ((CT_TreeNode) prevNodes.elementAt(row)).stack;
      ct_callstack.setIDs(cstack);
    }
    // t = System.currentTimeMillis() - t;
    // System.out.println("DEBUG: CallTreeDisp.CT_BuildTree(maxlevel=" + maxlevel + "): time=" + t +
    // " mls.");

    return node_to_expand;
  }

  /*
   * Build a subtree of the Call Tree
   */
  public void CT_BuildSubTree(DefaultMutableTreeNode start_node, int maxlevel) {
    long[] cstack;
    CT_TreeNode tn;
    CT_TreeNode[] tns;
    Vector<CT_TreeNode> nextNodes;
    Vector<CT_TreeNode> prevNodes;

    if (!(start_node instanceof CT_TreeNode)) {
      // root node
      return;
    }
    CT_TreeNode sn = (CT_TreeNode) start_node;

    // long t = System.currentTimeMillis(); // DEBUG
    // Step 1: add start_node
    prevNodes = new Vector<CT_TreeNode>();
    prevNodes.add(sn);
    int nrows = prevNodes.size();
    cstack = (long[]) sn.stack;
    ct_callstack.setIDs(cstack);

    // Step 2: add callees to start_node node
    nextNodes = new Vector<CT_TreeNode>(); // Next level nodes
    int level = 1;
    boolean already_has_children;
    for (int row = 0; /* row < nrows */ ; ) {
      CT_TreeNode dn = prevNodes.elementAt(row);
      // addChildren(dn); // To make sure this node is initialized
      int len = dn.getChildCount();
      already_has_children = false;
      if (len == 0) {
        // try to get children
        tns = getChildrenOfTreeNode(dn, false);
        len = tns.length;
      } else {
        already_has_children = true;
        tns = new CT_TreeNode[len];
        for (int i = 0; i < len; i++) {
          tns[i] = (CT_TreeNode) dn.getChildAt(i);
        }
      }
      // add children
      if ((!already_has_children) && (len > 0)) {
        CT_AddTreeNodes(dn, tns); // PERFORMANCE: ?% Total Thread Time
      }
      for (int i = 0; i < len; i++) {
        tn = tns[i];
        // Add this node to the Call Tree
        // ((DefaultMutableTreeNode)prevNodes.elementAt(row)).add(tn);
        //                if (!already_has_children) {
        //                    //NM dn.add(tn);
        //                    //NM Use AWT-thread to add nodes
        //                    final DefaultMutableTreeNode parent = dn;
        //                    final CT_TreeNode child = tn;
        //                    CT_AddTreeNode(parent, child); // PERFORMANCE: 60% Total Thread Time
        //                }
        // Save for the next level
        nextNodes.add(tn);
      }
      if (++row == nrows) {
        // this level is done, go one level deeper
        // System.out.println("DEBUG: CallTreeDisp.CT_BuildTree: level=" + level + " is done.");
        if (++level > maxlevel) {
          // We are done, the tree is built deep enough
          break;
        }
        nrows = nextNodes.size();
        if (nrows == 0) {
          // We are done, the tree is built
          break;
        }
        prevNodes = nextNodes;
        nextNodes = new Vector<CT_TreeNode>(); // Next level nodes
        row = 0;
      }
      cstack = (long[]) ((CT_TreeNode) prevNodes.elementAt(row)).stack;
      ct_callstack.setIDs(cstack);
    }
    // t = System.currentTimeMillis() - t;
    // System.out.println("DEBUG: CallTreeDisp.CT_BuildSubTree(maxlevel=" + maxlevel + "): time=" +
    // t + " mls.");

  }

  /*
   * Get Children
   */
  private CT_TreeNode[] getChildrenOfTreeNode(CT_TreeNode dn, boolean addFakeChild) {
    CT_TreeNode[] children = new CT_TreeNode[0];
    long[] cstack = dn.stack;
    long ID = -1;
    AnObject[] incMetrics = null;

    Object[] raw_data = getCalleeList(cstack); // IPC
    if ((callee_data != null) && (callee_data.length > 0)) {
      if (callee_data[metric_col] instanceof AnObject[]) {
        incMetrics = (AnObject[]) callee_data[metric_col];
      }
      int len = callee_data[0].length;
      children = new CT_TreeNode[len];
      for (int i = 0; i < len; i++) {
        // Get my ID
        if (raw_data[raw_data.length - 1] instanceof long[]) {
          ID = ((long[]) raw_data[raw_data.length - 1])[i];
        }
        // Create my stack
        long[] my_stack = new long[cstack.length + 1];
        my_stack[cstack.length] = ID;
        for (int k = 0; k < cstack.length; k++) {
          my_stack[k] = cstack[k];
        }
        // build the node as CT_TreeNode
        String nodeName = (String) callee_data[name_col][i];
        CT_TreeNode tn = new CT_TreeNode(nodeName, true);
        tn.ID = ID;
        tn.stack = my_stack;
        if (incMetrics != null && incMetrics.length > i) {
          tn.setInclusiveValue(incMetrics[i]);
        }
        tn.exclusive = 0.0; // TEMPORARY
        // Add this node to the Call Tree
        children[i] = tn;
      }
      dn.callees = callee_data.length;
    } else {
      dn.callees = 0;
    }
    return children;
  }

  /*
   * Expand the Call Tree
   */
  public CT_TreeNode CT_ExpandTree(
      JTree tree, DefaultMutableTreeNode dn, int level, int threshold, long id) {
    if (dn == null) {
      dn = tnTotal;
    }
    long[] cstack = null;
    return (CT_ExpandBranch(
        tree, dn, level, threshold, id,
        cstack)); // PERFORMANCE: 60% real time, call from CT_ExpandAllBranches
  }

  /**
   * Expand the selected branch of the Call Tree
   *
   * @param tree
   * @param anode - node to expand
   * @param level
   * @param threshold
   * @param id
   * @return node with specified id
   */
  public CT_TreeNode CT_ExpandBranch(
      JTree tree,
      DefaultMutableTreeNode anode,
      int level,
      int threshold,
      final long id,
      final long[] cstack) {
    CT_TreeNode select_node = null;
    if (anode == null) {
      return select_node; // Branch is not specified
    }
    if (anode instanceof CT_TreeNode) {
      if (id >= 0) {
        if (((CT_TreeNode) anode).ID == id) {
          select_node = (CT_TreeNode) anode;
        }
      }
    }
    int csindex = 0;
    int K = 1000; // will be increased in the loop if necessary

    CT_TreeNode total = tnTotal;
    if (total == null) {
      return null; // nothing to expand
    }
    double minValue = (total.inclusive / 100.0) * threshold; // TEMPORARY

    // int progress = 0;
    // window.setProgress(progress, "Expanding branch");
    for (int i = 1; ; ) {
      CT_BuildSubTree(anode, 2); // NM Need 2 levels deep
      if (anode.getChildCount() == 0) {
        // try to add its children
        addChildren(anode); // NM Not needed anymore?
      }
      // if (++progress > 98) {
      //    progress = 98;
      // }
      // window.setProgress(progress, "Expanding branch");
      if (anode.getChildCount() > 0) {
        if (cstack != null) {
          // Find next node
          csindex++;
          if (csindex >= cstack.length) {
            break; // we are done
          }
          try {
            CT_TreeNode tn = (CT_TreeNode) anode.getChildAt(0);
            int len = anode.getChildCount();
            for (int k = 1; k < len; k++) {
              if (tn.ID == cstack[csindex]) {
                break; // found
              }
              tn = (CT_TreeNode) anode.getChildAt(k);
            }
            if (tn.ID == cstack[csindex]) {
              anode = tn; // Found next node
            } else {
              return null; // Should never be here
            }
          } catch (Exception e) {
            // Not a CT_TreeNode in the tree
            return null; // Should never be here
          }
        } else { // Follow top node
          anode = (DefaultMutableTreeNode) anode.getChildAt(0);
        }
        if (level > 0) {
          if (i >= level) {
            CT_BuildSubTree(anode, 2);
            if (anode.getChildCount() == 0) {
              // try to add its children
              addChildren(anode);
            }
            break;
          }
        }
        if (++i >= K) {
          // System.out.println("DEBUG: CallTreeDisp.expandBranch(level=" + K + ")");
          K += K;
        }
        if (anode instanceof CT_TreeNode) {
          if (id >= 0) {
            if (((CT_TreeNode) anode).ID == id) {
              if (null == select_node) {
                select_node = (CT_TreeNode) anode;
              }
            }
          }
          double d = ((CT_TreeNode) anode).inclusive;
          if (d < minValue) {
            CT_BuildSubTree(anode, 2);
            if (anode.getChildCount() == 0) {
              // try to add its children
              addChildren(anode);
            }
            break;
          }
        }
        if (!selected) { // TEMPORARY code to interrupt
          break;
        }
        if (stop_expanding) { // Stop expanding
          break;
        }
        continue;
      }
      break;
    }

    TreeNode[] pathnodes = anode.getPath();
    if (null != select_node) {
      pathnodes = select_node.getPath();
    }
    TreePath anodepath = new TreePath(pathnodes);
    if (tree == null) { // old code
      calltree.expandPath(anodepath);
      calltree.setSelectionPath(anodepath);
    } else { // use AWT
      final JTree t = calltree;
      final TreePath ap = anodepath;
      final boolean preserve_select;
      if (null == select_node) {
        preserve_select = true;
      } else {
        preserve_select = false;
      }
      // Use SwingUtilities to update tree
      AnUtility.dispatchOnSwingThread(
          new Runnable() {
            // @Override
            public void
                run() { // PERFORMANCE: 60% real time, call from CT_ExpandTree (Expand All Branches)
              try {
                skip_build_subtree.set(true); // NM TEMPORARY Protection against infinite loop
                try {
                  TreePath sp = t.getSelectionPath();
                  t.expandPath(ap);
                  t.setSelectionPath(ap);
                  // Scroll if necessary
                  if (null != cstack) { // Not a good check, need a special flag for this
                    int select_row = t.getRowForPath(ap);
                    showSelectedRow(select_row);
                  }
                  // Restore selection if necessary
                  if (preserve_select && null != sp) {
                    t.setSelectionPath(sp);
                  }
                } finally {
                  skip_build_subtree.set(false); // NM TEMPORARY Remove protection
                }
              } catch (Exception exc) {
                System.err.println("CallTreeDisp.CT_ExpandBranch() exception: " + exc);
                exc.printStackTrace();
              }
            }
          });
    }
    // window.setProgress(100, "Expanding branch");
    return select_node;
  }

  /**
   * Expands All Branches
   *
   * @param tree
   * @param anode
   * @param level
   * @param threshold
   * @param id
   * @return node to select
   */
  public CT_TreeNode CT_ExpandAllBranches(
      JTree tree, DefaultMutableTreeNode anode, int level, int threshold, long id) {
    CT_TreeNode select_node = null;
    CT_TreeNode found_node = null;
    CT_TreeNode top_level_node;
    TreePath select_path = null;
    if (anode == null) {
      anode = tnTotal;
      if (anode == null) {
        return null;
      }
    }
    // int progress = 0;
    // window.setProgress(progress, "Expanding all branches");
    if (id >= 0) {
      top_level_node = (CT_TreeNode) anode;
      if (top_level_node.ID == id) {
        select_node = top_level_node;
      }
    } else {
      // Remember selected path
      select_path = calltree.getLeadSelectionPath();
    }
    CT_TreeNode total = tnTotal;
    if (total == null) {
      return null; // nothing to expand
    }
    double minValue = (total.inclusive / 100.0) * threshold; // TEMPORARY
    int max = calltree.getRowCount();
    skip_update_summary = true; // Optimization

    // Experiment: first, expand hottest branch
    long[] cstack = null;
    CT_ExpandBranch(calltree, tnTotal, level, threshold, id, cstack);

    for (int i = 1; i < max; i++) {
      if (stop_expanding) { // Stop expanding
        break;
      }
      TreePath tp = calltree.getPathForRow(i);
      if (!(tp.getLastPathComponent() instanceof CT_TreeNode)) {
        continue;
      }
      CT_TreeNode node = (CT_TreeNode) tp.getLastPathComponent();
      double d = node.inclusive;
      if ((id >= 0) && (select_node == null)) {
        if (node.ID == id) {
          select_node = node;
        }
      }
      if (d < minValue) {
        continue;
      }
      if (node.callees < 0) {
        // try to add its children
        addChildren(node);
      }
      if (node.getChildCount() == 0) {
        max = calltree.getRowCount();
        continue;
      }
      int exp_threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
      int exp_level = 1; // 1 level
      long exp_id = -1;
      if (null == found_node) {
        exp_id = id;
      }
      found_node =
          CT_ExpandTree(calltree, node, exp_level, exp_threshold, exp_id); // need CT_ExpandNode()

      max = calltree.getRowCount();
    } // End of loop - the tree is expanded
    skip_update_summary = false;
    // Restore selected object
    if (null == select_node) {
      if (null != select_path) {
        calltree.setLeadSelectionPath(select_path);
        int row = calltree.getLeadSelectionRow();
        if ((tree.getLastSelectedPathComponent() instanceof CT_TreeNode)) {
          // Set Selected Object and update Summary
          CT_TreeNode ctn = (CT_TreeNode) tree.getLastSelectedPathComponent();
          final long ID = ctn.ID;
          AnUtility.dispatchOnAWorkerThread(
              new Runnable() {
                @Override
                public void run() {
                  ct_callstack.setSelectedObject(ID);
                  window.getSelectionManager().updateSelection();
                }
              },
              "call tree selection");
        }
        // calltree.setSelectionRow(row);
        showSelectedRow(row);
      }
    }
    // window.setProgress(100, "Expanding all branches");
    return select_node;
  }

  /*
   * Switch to the Source view
   */
  private void CT_ShowSource() {
    window.setSelectedView(AnDisplay.DSP_Source);
  }

  /*
   * Switch to the Disassembly view
   */
  private void CT_ShowDisasm() {
    window.setSelectedView(AnDisplay.DSP_Disassembly);
  }

  /** Performs default action for double click */
  private void performDefaultAction() {
    CT_ShowSource();
  }

  // ==================================================== //
  //         Listeneres and event handlers                //
  // ==================================================== //

  /**
   * Inner class which listens for expansion events on the tree. When one is received, it will get
   * the path of the selected node and add sub directory nodes, if any, to it.
   */
  class CallTreeExpansionListener implements TreeExpansionListener {

    /** Called whenever an item in the tree has been expanded. */
    public void treeExpanded(TreeExpansionEvent event) {
      valueChanged(event);
    }

    /** Called whenever an item in the tree has been collapsed. */
    public void treeCollapsed(TreeExpansionEvent event) {
      // valueChanged(event);
    }

    public void valueChanged(final TreeExpansionEvent tee) {
      // NM TEMPORARY Protection against infinite loop:
      // check that we are expanding a freshly built tree
      if (skip_build_subtree.get()) {
        return;
      }

      // NM TEMPORARY Cannot use a worker thread - desynchronization with AWT thread:
      //            final boolean use_thread = true;
      //            AnUtility.dispatchOnAWorkerThread(new Runnable() {
      //                @Override
      //                public void run() {
      //                    valueChanged(tee, use_thread);
      //                }
      //            }, "CallTree_thread");
      //        }
      //
      //        private void valueChanged(TreeExpansionEvent tee, boolean use_thread) {
      // get the selected tree path
      TreePath path = tee.getPath();

      // check that a path is in fact selected
      if (path == null) {
        return;
      }

      // get the last node
      DefaultMutableTreeNode curNode = (DefaultMutableTreeNode) path.getLastPathComponent();

      // add children to the selected node
      if (curNode.getChildCount() == 0) {
        addChildren(curNode);
      }
      for (int i = 0; i < curNode.getChildCount(); i++) {
        addChildren((DefaultMutableTreeNode) curNode.getChildAt(i));
      }
    }
  }

  /**
   * Inner class which listens for selection events on the tree. When one is received, it will get
   * the path of the selected node and add sub directory nodes, if any, to it.
   */
  class CallTreeSelectionListener implements TreeSelectionListener {

    @Override
    public void valueChanged(final TreeSelectionEvent tse) {
      // NM TEMPORARY Protection against infinite loop:
      // check that we are expanding a freshly built tree
      if (skip_build_subtree.get()) {
        return;
      }

      // NM TEMPORARY Cannot use a worker thread - desynchronization with AWT thread:
      //            final boolean use_thread = true;
      //            AnUtility.dispatchOnAWorkerThread(new Runnable() {
      //                @Override
      //                public void run() {
      //                    valueChanged(tse, use_thread);
      //                }
      //            }, "CallTree_thread");
      //        }
      //
      //        private void valueChanged(TreeSelectionEvent tse, boolean use_thread) {
      // get the selected tree path
      TreePath path = tse.getNewLeadSelectionPath();

      // check that a path is in fact selected
      if (path == null) {
        return;
      }

      // get the selected node
      DefaultMutableTreeNode curNode = (DefaultMutableTreeNode) path.getLastPathComponent();

      // get all the nodes in the selected tree path
      // Object nodes[] = path.getPath();
      if (!(curNode instanceof CT_TreeNode)) {
        // root node
        return;
      }

      // add children to the selected node
      if (curNode.getChildCount() == 0) {
        addChildren(curNode);
      }
      if (!skip_update_summary) {
        // Update Summary
        final long ID = ((CT_TreeNode) curNode).ID;
        AnUtility.dispatchOnAWorkerThread(
            new Runnable() {
              @Override
              public void run() {
                ct_callstack.setSelectedObject(ID);
                window.getSelectionManager().updateSelection();
              }
            },
            "call tree selection");
      }
    }
  }

  /**
   * Adds the subnodes, if any, of the given path to the given node.
   *
   * @param node tree node to add sub directory nodes to
   */
  protected void addChildren(DefaultMutableTreeNode node) {
    synchronized (CallTreeLock) {
      if (!(node instanceof CT_TreeNode)) {
        return; // Not our node
      }
      final CT_TreeNode dn = (CT_TreeNode) node;
      // add sub nodes to given node
      if (dn != null) {
        if (dn.callees < 0) { // Not initialized yet
          // try to get children
          final CT_TreeNode[] tns = getChildrenOfTreeNode(dn, true); // IPC
          dn.callees = tns.length; // update internal children counter
          if (tns.length > 0) {
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  public void run() {
                    for (int i = 0; i < tns.length; i++) {
                      dn.add(tns[i]);
                    }
                  }
                });
          }
        }
      }
    }
  }

  // ==================================================== //
  //     Wrappers for native methods from liber_dbe.so    //
  // ==================================================== //
  // Get row Table Data
  private Object[] getTableData(
      final String mlistStr,
      final String modeStr,
      final String typeStr,
      final String subtypeStr,
      final long[] cstack) {
    // long t = System.currentTimeMillis();
    AnUtility.checkIPCOnWrongThread(false);
    Object[] data = window.getTableDataV2(mlistStr, modeStr, typeStr, subtypeStr, cstack);
    AnUtility.checkIPCOnWrongThread(true);
    // t = System.currentTimeMillis() - t;
    // if (t > 100) {
    // System.out.println("DEBUG: CallTreeDisp.getTableData(" + modeStr + "): time=" + t + " mls.");
    // }
    return data;
  }

  /**
   * Get Callee List. Uses IPC call.
   *
   * @param cstack
   * @return list
   */
  private Object[] getCalleeList(final long[] cstack) {
    Object[] raw_data_with_ids =
        getTableData(mlistStr, stab_callees, typeStrFunction, subtypeStr, cstack);

    ids_callees = (long[]) raw_data_with_ids[raw_data_with_ids.length - 1];

    Object[] raw_data = new Object[raw_data_with_ids.length];
    for (int i = 0; i < raw_data.length; i++) {
      raw_data[i] = raw_data_with_ids[i];
    }
    raw_data[raw_data.length - 1] = null;

    callee_data = localProcessData(raw_data);

    name_col = window.getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
    if (name_col < 0) {
      name_col = 0;
    }
    if ((callee_data.length > name_col) && (callee_data[name_col] instanceof String[])) {
      // Ok.
    } else {
      // Hack: find name column
      for (int i = 0; i < callee_data.length; i++) {
        if (callee_data[i] instanceof String[]) {
          name_col = i;
        }
      }
    }
    return raw_data_with_ids;
  }

  // Get function item data
  private Object[] getFuncItemData(final long[] cstack) {
    Object[] raw_data_with_ids =
        getTableData(mlistStr, stab_self, typeStrFunction, subtypeStr, cstack);

    Object[] raw_data = new Object[raw_data_with_ids.length];
    for (int i = 0; i < raw_data.length; i++) {
      raw_data[i] = raw_data_with_ids[i];
    }
    raw_data[raw_data.length - 1] = null;

    func_data = localProcessData(raw_data);
    return raw_data_with_ids;
  }

  // Get callers data list
  private Object[] getCallerList(final long[] cstack) {
    Object[] raw_data_with_ids =
        getTableData(mlistStr, stab_callers, typeStrFunction, subtypeStr, cstack);

    ids_callers = (long[]) raw_data_with_ids[raw_data_with_ids.length - 1];

    Object[] raw_data = new Object[raw_data_with_ids.length];
    for (int i = 0; i < raw_data.length; i++) {
      raw_data[i] = raw_data_with_ids[i];
    }
    raw_data[raw_data.length - 1] = null;

    caller_data = localProcessData(raw_data);
    caller_label =
        window
            .getSettings()
            .getMetricsSetting()
            .getLabel(caller_data, getTotalMax(), DSP_CallTree, caller);

    name_col = window.getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
    if (name_col < 0) {
      name_col = 0;
    }
    if ((caller_data.length > name_col) && (caller_data[name_col] instanceof String[])) {
      // Ok.
    } else {
      for (int i = 0; i < caller_data.length; i++) {
        if (caller_label[i].getAnMetric().isNameMetric()) {
          name_col = i;
        }
      }
    }

    if ((sort_col < 0) || (caller_data.length <= sort_col)) {
      sort_col = window.getSettings().getMetricsSetting().getSortColumnByDType(DSP_CallTree);
      if ((sort_col < 0) || (caller_data.length <= sort_col)) {
        sort_col = 0; // NM: Can we ever get there? Why?
      }
    }
    metric_col = sort_col;
    if (name_col == metric_col) {
      if (previous_metric_col >= 0 && (caller_data.length > previous_metric_col)) {
        metric_col = previous_metric_col; // Set previous metric
      }
    } else {
      previous_metric_col = metric_col; // Save metric column
    }

    // Check if cycles should be converted to time
    convertHWCtoTime = false;
    if (caller_label.length > metric_col) {
      if (caller_label[metric_col].getClock() != -1.0) {
        clock_frequency = caller_label[metric_col].getClock();
        convertHWCtoTime = true;
      }
    }

    metricName = caller_label[metric_col].getAnMetric().getName();
    if ((null == metricCommandName)
        || !metricCommandName.equals(caller_label[metric_col].getAnMetric().getComd())) {
      metricCommandName = caller_label[metric_col].getAnMetric().getComd();
      cleanMetricIcons();
    }
    return raw_data_with_ids;
  }

  // ==================================================== //
  //        CallStack actions                             //
  // ==================================================== //
  private final class CT_CallStack {

    long[] ids = new long[0];
    String[] names = new String[0];
    long selectionTime = 0;
    // NM String lastFilter = null;
    long last_ID = -1;
    AnTable last_selected_table = null;
    int last_selected_row = -1;

    public void append(int index) {
      if ((ids_callees == null) || (index >= ids_callees.length)) {
        // System.err.println("ERROR: CT_CallStack.append(" + index + ")");
        return;
      }
      long ID = ids_callees[index];
      long[] new_cstack = new long[ids.length + 1];
      for (int i = 0; i < ids.length; i++) {
        new_cstack[i] = ids[i];
      }
      new_cstack[new_cstack.length - 1] = ID;
      ids = new_cstack;

      String[] new_ids_names = new String[names.length + 1];
      for (int i = 0; i < names.length; i++) {
        new_ids_names[i] = names[i];
      }
      new_ids_names[names.length] = (String) callee_data[name_col][index];
      names = new_ids_names;

      // Save selection
      // saveSelection(ID, func_item, ids.length - 1);
    }

    public void prepend(int index) {
      if ((ids_callers == null) || (index >= ids_callers.length)) {
        // System.err.println("ERROR: CT_CallStack.prepend(" + index + ")");
        return;
      }
      long ID = ids_callers[index];
      // last_ID = ID;
      long[] new_cstack = new long[ids.length + 1];
      new_cstack[0] = ID;
      for (int i = 1; i < new_cstack.length; i++) {
        new_cstack[i] = ids[i - 1];
      }
      ids = new_cstack;

      String[] new_ids_names = new String[names.length + 1];
      for (int i = 0; i < names.length; i++) {
        new_ids_names[i + 1] = names[i];
      }
      new_ids_names[0] = (String) caller_data[name_col][index];
      names = new_ids_names;

      // Save selection
      // saveSelection(ID, func_item, 0);
    }

    public void remove(int index) {
      if ((index == 0) && (ids.length > 1)) {
        // Remove top function
        long[] new_cstack = new long[ids.length - 1];
        for (int i = 1; i < ids.length; i++) {
          new_cstack[i - 1] = ids[i];
        }
        // NM long ID = ids[0];
        ids = new_cstack;

        String[] new_names = new String[names.length - 1];
        for (int i = 1; i < names.length; i++) {
          new_names[i - 1] = names[i];
        }
        names = new_names;

        // Save selection
        // NM saveSelection(ID, caller, -1);
      }
      if ((index > 0) && (ids.length - 1 == index)) {
        // Remove bottom function
        long[] new_cstack = new long[ids.length - 1];
        for (int i = 0; i < new_cstack.length; i++) {
          new_cstack[i] = ids[i];
        }
        // NM long ID = ids[ids.length - 1];
        ids = new_cstack;

        String[] new_names = new String[names.length - 1];
        for (int i = 0; i < new_names.length; i++) {
          new_names[i] = names[i];
        }
        names = new_names;

        // Save selection
        // NM saveSelection(ID, callee, -1);
      }
    }

    public void replace(long[] stack, int index) {
      long ID = stack[index];
      long[] new_cstack = new long[1];
      new_cstack[0] = ID;
      ids = new_cstack;

      // Get name
      String fname = window.getObjNameV2(ID);
      String[] new_names = new String[1];
      new_names[0] = fname;
      names = new_names;

      // Save selection
      // NM saveSelection(ID, func_item, 0);
    }

    public void reset() {
      ids = new long[0];
      names = new String[0];
    }

    public long[] getIDs() {
      if ((ids == null) || (ids.length == 0)) {
        // Get ID
        long ID = window.getSelectedObject().getSelObjV2(typeStrFunction);
        long[] new_cstack = new long[1];
        new_cstack[0] = ID;
        ids = new_cstack;

        // Get name
        String fname = window.getObjNameV2(ID);
        String[] new_names = new String[1];
        new_names[0] = fname;
        names = new_names;

        // Get Filter
        // NM lastFilter = window.getFilterStr();
        // Save selection
        // NM saveSelection(ID, func_item, 0);
      }
      return ids;
    }

    public void setIDs(long[] stack) {
      ids = stack;
    }

    public String[] getNames() {
      if ((names == null) || (names.length == 0)) {
        getIDs();
      }
      return names;
    }

    public void setNames(String[] names) {
      this.names = names;
    }

    public void saveSelection(long ID, AnTable table, int index) {
      last_ID = ID;
      last_selected_table = table;
      last_selected_row = index;
    }

    /*
     * Set selected object and save selection time
     */
    private void setSelectedObject(long ID) {
      window.getSelectedObject().setSelObjV2(ID);
      selectionTime = window.getSelectedObject().getSelObjectSelectionTime();
      sel_func = ID;
      // Save selection
      // NM saveSelection(ID, func_item, 0);
      // last_ID = ID;
      // last_selected_table = null;
      // last_selected_row = -1;
    }

    /*
     * Quick and durty hack to check if reset is needed
     */
    //        public boolean needReset() {
    //            String fs = window.getFilterStr();
    //            if (fs != null) {
    //                if (!fs.equals(lastFilter)) {
    //                    // Filter changed
    //                    boolean res = true; //return true;
    //                }
    //            }
    //            if (selectionTime != window.getSelectedObject().getSelObjectSelectionTime()) {
    //                // Selected Object was changed in another tab
    //                if ((ids == null) || (ids.length <= 1)) {
    //                    // Stack is only one function
    //                    return true;
    //                }
    //            }
    //            return false;
    //        }
  } // CT_CallStack

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

  /**
   * Update tree in CalTree tab Possible actions: Set Root - set the selected function as root (not
   * implemented yet). Reset Root - set <Total> as root (not implemented yet). Set Filter: Name in
   * Stack - set the selected function name as filter. Set Filter: Call Path in Stack - set the call
   * path to the selected function as filter. Set Filter: Function in Stack - set the selected
   * function as filter. Set Filter: Function is Leaf - set the selected function as filter. Show
   * Next Reference - show next node related to the selected function (same ID). Show All References
   * - show all nodes related to the selected function (not implemented yet). Expand Branch - expand
   * selected branch (with respect to current threshold). Expand Hottest Branch - expand the hottest
   * branch (with respect to current threshold). Expand Hot Branches - expand all hot branches (top
   * node >= 10%) (not implemented yet). Expand All Branches - expand all branches (with respect to
   * current threshold). Show Inclusive Metric - show inclusive metric (default). Show Exclusive
   * Metric - show exclusive metric (not implemented yet). Show Percentage - show percentage (with
   * respect to Total inclusive) (default).
   *
   * <p>Note: this method is invoked on a "worker" thread
   */
  private void CT_UpdateTree(String actionName, ActionEvent ev) {
    DefaultMutableTreeNode dn;

    Object progressBarHandle =
        window.getSystemProgressPanel().progressBarStart(AnLocale.getString("Call Tree Update"));
    Choices:
    {
      if (actionName.equals(FILTER_SELECTED_BRANCH_ACTION_NAME)) {
        String clause = createFilterClause(actionName);
        setClause(FILTER_SELECTED_BRANCH_SHORT_NAME, FILTER_SELECTED_BRANCH_LONG_NAME, clause);
        break Choices;
      } else if (actionName.equals(FILTER_NOT_SELECTED_BRANCH_ACTION_NAME)) {
        String clause = createFilterClause(actionName);
        setClause(
            FILTER_NOT_SELECTED_BRANCH_SHORT_NAME, FILTER_NOT_SELECTED_BRANCH_LONG_NAME, clause);
        break Choices;
      } else if (actionName.equals(FILTER_SELECTED_FUNCTION_ACTION_NAME)) {
        String clause = createFilterClause(actionName);
        setClause(FILTER_SELECTED_FUNCTION_SHORT_NAME, FILTER_SELECTED_FUNCTION_LONG_NAME, clause);
        break Choices;
      } else if (actionName.equals(FILTER_SELECTED_LEAF_ACTION_NAME)) {
        String clause = createFilterClause(actionName);
        setClause(FILTER_SELECTED_LEAF_SHORT_NAME, FILTER_SELECTED_LEAF_LONG_NAME, clause);
        break Choices;
      }
      //        if (actionName.equals(FILTER_NAME_ACTION_NAME)) {
      //            String[] clause = new String[4];
      //            clause[0] = createFilterClause(FILTER_NAME_ACTION_NAME);
      //            clause[1] = createFilterClause(FILTER_SELECTED_BRANCH_ACTION_NAME);
      //            clause[2] = createFilterClause(FILTER_SELECTED_FUNCTION_ACTION_NAME);
      //            clause[3] = createFilterClause(FILTER_SELECTED_LEAF_ACTION_NAME);
      ////            window.filter.showDialog();
      ////            window.filter.setSelectedTab(1);  // Select advanced filter tab
      //            //window.filter.setClause(clause);  // Put strings in combo box
      //            window.filter.setClause(CALL_TREE + FILTER_NAME_SHORT_NAME, CALL_TREE +
      // FILTER_NAME_LONG_NAME, clause[0], true);  // Put the string in text field
      //        }
      // This must be done on AWT thread (see actionPerformed)
      if (actionName.equals(STR_ACTION_CUSTOM_FILTER)) {
        window.getFilters().showCustomFilterDialog();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_REMOVE_ALL_FILTERS)) {
        window.getFilters().addClause(FilterClause.getNoFiltersClause(), false);
        break Choices;
      }
      if (actionName.equals(STR_ACTION_UNDO_FILTER)) {
        window.getFilters().undoLastFilteraction();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_REDO_FILTER)) {
        window.getFilters().redoLastFilteraction();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_RESET)) {
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOWSOURCE)) {
        CT_ShowSource();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOWDISASM)) {
        CT_ShowDisasm();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_EXPANDBRANCH)) {
        stop_expanding_enabled = true;
        stop_expanding = false;
        final DefaultMutableTreeNode fdn;
        fdn = (DefaultMutableTreeNode) calltree.getLastSelectedPathComponent();
        int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
        int level = 0; // unlimited
        long id = -1;
        long[] cstack = null;
        CT_ExpandBranch(calltree, fdn, level, threshold, id, cstack);
        stop_expanding = false;
        stop_expanding_enabled = false;
        break Choices;
      }
      if (actionName.equals(STR_ACTION_EXPANDHOTTEST)) {
        stop_expanding_enabled = true;
        stop_expanding = false;
        int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
        int level = 0; // unlimited
        long id = -1;
        long[] cstack = null;
        CT_ExpandBranch(calltree, tnTotal, level, threshold, id, cstack);
        stop_expanding = false;
        stop_expanding_enabled = false;
        break Choices;
      }
      if (actionName.equals(STR_ACTION_EXPANDHOT)) {
        stop_expanding_enabled = true;
        stop_expanding = false;
        int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
        int level = 0; // unlimited
        long id = -1;
        CT_ExpandTree(calltree, tnTotal, level, threshold, id);
        stop_expanding = false;
        stop_expanding_enabled = false;
        break Choices;
      }
      if (actionName.equals(STR_ACTION_EXPANDTREE)) {
        stop_expanding_enabled = true;
        stop_expanding = false;
        int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
        int level = 0; // unlimited
        long id = -1;
        // long time = System.currentTimeMillis();
        CT_ExpandAllBranches(calltree, tnTotal, level, threshold, id);
        // time = System.currentTimeMillis() - time;
        // System.out.println("CT_ExpandAllBranches: time = "+time);
        stop_expanding = false;
        stop_expanding_enabled = false;
        break Choices;
      }
      if (actionName.equals(STR_ACTION_COPY_ALL)) { // Copy all lines
        copyAll();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SETTINGS)) {
        window.getSettings().showDialog(window.getSettings().settingsCalltreeIndex);
        break Choices;
      }
      if (actionName.equals(STR_SORT_BY_METRIC)) {
        if (null != metricName) {
          for (int i = 0; i < available_metrics.length; i++) {
            String mn1 = available_metrics[i].getUserName();
            String mn2 = AnLocale.getString("Attributed ") + mn1;
            if (metricName.equals(mn1) || metricName.equals(mn2)) {
              window.getSettings().getMetricsSetting().setSortMetricByDType(this, i, type);
              computed = false;
              computeOnAWorkerThread();
              sortByName = false;
              break;
            }
          }
        }
        break Choices;
      }
      if (actionName.equals(STR_SORT_BY_NAME)) {
        sortByName = true;
        window.getSettings().getMetricsSetting().setSortMetricByDType(this, name_col, type);
        computed = false;
        if (selected) {
          computeOnAWorkerThread();
        }
        break Choices;
      }
      if (actionName.equals(STR_ACTION_EXPANDNODE)) {
        dn = (DefaultMutableTreeNode) calltree.getLastSelectedPathComponent();
        int threshold = window.getSettings().getCallTreeSetting().getThreshold(); // %
        int level = 1; // 1 level
        long id = -1;
        CT_ExpandTree(calltree, dn, level, threshold, id);
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOWNEXT)) {
        dn = (DefaultMutableTreeNode) calltree.getLastSelectedPathComponent();
        if (null != dn) {
          if (dn instanceof CT_TreeNode) {
            CT_TreeNode n = (CT_TreeNode) dn;
            long id = n.ID;
            int row = calltree.getMaxSelectionRow();
            boolean do_selection = true;
            find(id, row, do_selection, true);
          }
        }
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOWALL)) {
        dn = (DefaultMutableTreeNode) calltree.getLastSelectedPathComponent();
        if (null != dn) {
          if (dn instanceof CT_TreeNode) {
            CT_TreeNode n = (CT_TreeNode) dn;
            long id = n.ID;
            CT_ExpandAllCallStacksForID(id);
          }
        }
        break Choices;
      }
      if (actionName.equals(STR_ACTION_HIDE_COLORBARS)) {
        show_colorbars = false;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOW_COLORBARS)) {
        show_colorbars = true;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_HIDE_TOOLTIPS)) {
        show_tooltips = false;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOW_TOOLTIPS)) {
        show_tooltips = true;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_REFRESH)) {
        updated = false;
        computeOnAWorkerThread();
        break Choices;
      }

      if (actionName.equals(STR_ACTION_SHOW_INCLUSIVE)) {
        show_inclusive = true;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_HIDE_INCLUSIVE)) {
        show_inclusive = false;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOW_EXCLUSIVE)) {
        show_exclusive = true;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_SHOW_PERCENTAGE)) {
        show_percentage = true;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_HIDE_PERCENTAGE)) {
        show_percentage = false;
        computed = false;
        computeOnAWorkerThread();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_STOPEXPANDING)) {
        stop_expanding = true;
        break Choices;
      }
      if (actionName.equals(STR_ACTION_COLLAPSENODE)) {
        CT_CollapseNode();
        break Choices;
      }
      if (actionName.equals(STR_ACTION_COLLAPSETREE)) {
        CT_CollapseTree();
        break Choices;
      }
      // if (actionName.equals(STR_ACTION_COLLAPSEBRANCH)) {
      //    dn = (DefaultMutableTreeNode) calltree.getLastSelectedPathComponent();
      //    CT_CollapseBranch(calltree, dn);
      // }

      // Set metric, perhaps?
      if (available_metrics.length > 1) {
        for (int i = 0; i < available_metrics.length; i++) {
          if (actionName.equals(available_metrics[i].getUserName())) {
            sortByName = false;
            window
                .getSettings()
                .getMetricsSetting()
                .setSortMetricByDType(this, i, DSP_CallTree); // Will fire change event....
            break;
          }
        }
        break Choices;
      }
    } // End of Choices
    window.getSystemProgressPanel().progressBarStop(progressBarHandle);
  }

  /** Update title */
  private synchronized void CT_UpdateTitle() {
    String titletext = STR_TREE_TITLE_FUNCTIONS;
    final String threeSpaces = "   ";
    final String STR_PERCENT = "%";
    final String STR_NO_FILTER = "1";
    final String STR_METRIC = AnLocale.getString("Metric: ");
    final String STR_THRESHOLD = AnLocale.getString("Threshold: ");
    final String STR_SORT_BY_NAME = AnLocale.getString("Sort by: name.");
    final String STR_SORT_BY_METRIC = AnLocale.getString("Sort by: metric.");
    lastFilter = window.getFilterStr(); // IPC CALL! Perhaps there is a better place?
    if ((lastFilter != null) && (!lastFilter.equals(STR_NO_FILTER))) {
      titletext += threeSpaces + STR_TREE_TITLE_FILTER;
    } else {
      titletext += threeSpaces + STR_TREE_TITLE_COMPLETE_VIEW;
    }
    titletext +=
        threeSpaces
            + STR_THRESHOLD
            + window.getSettings().getCallTreeSetting().getThreshold()
            + STR_PERCENT;
    int nc =
        AnWindow.getInstance().getSettings().getMetricsSetting().getNameColumnIndexByDType(type);
    int si = AnWindow.getInstance().getSettings().getMetricsSetting().getSortColumnByDType(type);
    if (nc == si) {
      sortByName = true;
    }
    if (sortByName) {
      titletext += threeSpaces + STR_SORT_BY_NAME;
    } else {
      titletext += threeSpaces + STR_SORT_BY_METRIC;
    }

    if (null != metricName) {
      String metrictext = threeSpaces + STR_METRIC + metricName;
      titletext += metrictext;
    }
    final String text = titletext;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            rootnode.setUserObject(text);
            calltree.repaint();
          }
        });
  }
  /** Variable to check if the node is expanded */
  static int print_row;

  /** Create text presentation of the tree and print it. Calls CT_PrintChildren(). */
  private String CT_PrintTree() {
    // Prepare text presentation of the tree
    String text;
    text = rootnode.getUserObject().toString();
    String prefix = "\n";
    print_row = 1;
    text += CT_PrintChildren(tnTotal, prefix);
    text += prefix;
    // Print it
    // System.out.println(text);
    return text;
  }

  /*
   * Recursively print all children
   */
  private String CT_PrintChildren(CT_TreeNode node, String prefix) {
    String buf = "";
    String P0 = "+-";
    String P2 = "  |";
    String P1 = "  ";
    // Print prefix
    buf = prefix + P0;
    if (prefix.endsWith(P2)) {
      buf = prefix.substring(0, prefix.length() - 1) + P0;
    }
    // Print current node info
    buf += node.toString();
    // TEMPORARY: check if print_row is correct
    TreePath tp = calltree.getPathForRow(print_row);
    if (null == tp) {
      // System.err.println("WARNING: empty tree");
      return buf;
    }
    CT_TreeNode n = (CT_TreeNode) tp.getLastPathComponent();
    String s1 = n.toString();
    String s2 = node.toString();
    if (!s1.equals(s2)) {
      System.err.println("ERROR: node does not match the row");
      return buf;
    }
    // If node is not expanded - return
    if (!calltree.isExpanded(print_row)) {
      return buf;
    }
    // Get children
    int nc = node.getChildCount();
    if (nc <= 0) {
      return buf;
    }
    // Print children
    String ch_prefix = prefix + P2;
    for (int i = 0; i < nc - 1; i++) {
      print_row++;
      buf += CT_PrintChildren((CT_TreeNode) node.getChildAt(i), ch_prefix);
    }
    ch_prefix = prefix + P1;
    print_row++;
    buf += CT_PrintChildren((CT_TreeNode) node.getChildAt(nc - 1), ch_prefix);
    return buf;
  }

  public JPopupMenu initPopup(boolean filterOnly) {
    AccessibleContext ac;
    JMenuItem mi;
    JPopupMenu popup = new JPopupMenu();
    String txt;

    boolean row_selected = false;
    int row = calltree.getLeadSelectionRow();
    if (row >= 0) {
      row_selected = true;
    }

    if (!filterOnly) {
      // Add "Show Source" action
      txt = STR_ACTION_SHOWSOURCE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);

      // Add "Show Disassembly" action
      txt = STR_ACTION_SHOWDISASM;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);

      // Add separator
      popup.addSeparator();

      // Add "Expand Node" action
      txt = STR_ACTION_EXPANDNODE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      if (calltree.isExpanded(row)) {
        mi.setEnabled(false);
      }
      // popup.add(mi); // TEMPORARY

      // Add "Expand Branch" action
      txt = STR_ACTION_EXPANDBRANCH;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);

      // Add "Expand Hottest Branch" action
      txt = STR_ACTION_EXPANDHOTTEST;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);

      // Add "Expand All Branches" action
      txt = STR_ACTION_EXPANDTREE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      // mi.setEnabled(true);
      mi.setEnabled(true);
      popup.add(mi);

      // Add separator
      popup.addSeparator();

      // Add "Stop Expanding" action
      txt = STR_ACTION_STOPEXPANDING;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(stop_expanding_enabled);
      popup.add(mi);

      // Add "Collapse Node" action
      txt = STR_ACTION_COLLAPSENODE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      // popup.add(mi); // TEMPORARY

      // Add "Collapse All Branches" action
      txt = STR_ACTION_COLLAPSETREE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);

      // Add separator
      // popup.addSeparator();
      // Add "Set Root" action
      txt = STR_ACTION_SETROOT;
      UpdateAction ua = new UpdateAction(txt);
      mi = new JMenuItem(ua);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (row <= 1) {
        mi.setEnabled(false);
      } else {
        mi.setEnabled(true);
      }
      mi.setEnabled(false); // TEMPORARY
      // popup.add(mi); // TEMPORARY

      // Add "Reset Root" action
      txt = STR_ACTION_RESET;
      UpdateAction rsa = new UpdateAction(txt);
      mi = new JMenuItem(rsa);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      // if root node is <Total>
      //     mi.setEnabled(false);
      mi.setEnabled(false); // TEMPORARY
      // popup.add(mi); // TEMPORARY

      //            // Add separator
      //            popup.addSeparator();
      //            // Add "Set Threshold 0%" action
      //            txt = STR_SET_THRESHOLD_0;
      //            UpdateAction st0 = new UpdateAction(txt);
      //            mi = new JMenuItem(st0);
      //            ac = mi.getAccessibleContext();
      //            ac.setAccessibleDescription(txt);
      //            if (0 == window.getSettings().getCallTreeSetting().getThreshold()) {
      //                mi.setEnabled(false);
      //            } else {
      //                mi.setEnabled(true);
      //            }
      //            popup.add(mi);
      //            // Add "Set Threshold 1%" action
      //            txt = STR_SET_THRESHOLD_1;
      //            UpdateAction st1 = new UpdateAction(txt);
      //            mi = new JMenuItem(st1);
      //            ac = mi.getAccessibleContext();
      //            ac.setAccessibleDescription(txt);
      //            if (0 == window.getSettings().getCallTreeSetting().getThreshold()) {
      //                mi.setEnabled(true);
      //            } else {
      //                mi.setEnabled(false);
      //            }
      //            popup.add(mi);
      // Add separator
      popup.addSeparator();
    }

    // Add "Set Filter: Call Path in Stack ..." action
    txt = ADD_FILTER + FILTER_SELECTED_BRANCH_LONG_NAME;
    mi = new JMenuItem(new UpdateAction(txt, FILTER_SELECTED_BRANCH_ACTION_NAME));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(row_selected);
    popup.add(mi);
    // Add "Set Filter: Call Path not in Stack ..." action
    txt = ADD_FILTER + FILTER_NOT_SELECTED_BRANCH_LONG_NAME;
    mi = new JMenuItem(new UpdateAction(txt, FILTER_NOT_SELECTED_BRANCH_ACTION_NAME));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(row_selected);
    popup.add(mi);
    // Add "Set Filter: Function in Stack ..." action
    txt = ADD_FILTER + FILTER_SELECTED_FUNCTION_LONG_NAME;
    mi = new JMenuItem(new UpdateAction(txt, FILTER_SELECTED_FUNCTION_ACTION_NAME));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(row_selected);
    popup.add(mi);
    // Add "Set Filter: Function is Leaf ..." action
    txt = ADD_FILTER + FILTER_SELECTED_LEAF_LONG_NAME;
    mi = new JMenuItem(new UpdateAction(txt, FILTER_SELECTED_LEAF_ACTION_NAME));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(row_selected);
    popup.add(mi);
    //            // Add "Set Filter: Name in Stack ..." action
    //            txt = ADD_FILTER + FILTER_NAME_LONG_NAME;
    //            mi = new JMenuItem(new UpdateAction(txt, FILTER_NAME_ACTION_NAME));
    //            ac = mi.getAccessibleContext();
    //            ac.setAccessibleDescription(txt);
    //            mi.setEnabled(row_selected);
    //            popup.add(mi);
    // Add "Undo Last Filter ..." action
    // Add "Manage Filters..." action
    txt = STR_ACTION_CUSTOM_FILTER;
    mi = new JMenuItem(new UpdateAction(txt));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(row_selected);
    popup.add(mi);
    if (filterOnly) {
      popup.addSeparator();
    }
    txt = STR_ACTION_UNDO_FILTER;
    mi = new JMenuItem(new UpdateAction(txt));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(window.getFilters().canUndoLastFilter());
    popup.add(mi);
    // Add "Redo Last Filter ..." action
    txt = STR_ACTION_REDO_FILTER;
    mi = new JMenuItem(new UpdateAction(txt));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(window.getFilters().canRedoLastFilter());
    popup.add(mi);
    // Add "Restore Default Filter ..." action
    txt = STR_ACTION_REMOVE_ALL_FILTERS;
    mi = new JMenuItem(new UpdateAction(txt));
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(window.getFilters().canRemoveAllFilters());
    popup.add(mi);
    // Add "Remove Filter" action
    if (window.getFilters().anyFilters()) {
      popup.add(window.getFilters().removeFilterMenuItem());
    }
    //        if (filterOnly) {
    //            popup.addSeparator();
    //        }

    if (!filterOnly) {
      // Add separator
      popup.addSeparator();

      // Add "Show Next Reference To Function" action
      txt = STR_ACTION_SHOWNEXT;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      String hotkey = "CTRL_N";
      KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, false);
      calltree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, hotkey);
      UpdateAction act = new UpdateAction(txt);
      calltree.getActionMap().put(hotkey, act);
      mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
      mi.setEnabled(true);
      popup.add(mi);

      // Add "Show All References To Function" action
      txt = STR_ACTION_SHOWALL;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      mi.setEnabled(true);
      popup.add(mi);

      // Add separator
      popup.addSeparator();
      // Add "Hide/Show Color Bars" action
      if (show_colorbars) {
        txt = STR_ACTION_HIDE_COLORBARS;
      } else {
        txt = STR_ACTION_SHOW_COLORBARS;
      }
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      if (true == show_percentage) {
        mi.setEnabled(true);
      } else {
        mi.setEnabled(false);
      }
      popup.add(mi);

      // Add "Hide/Show Tool Tips" action
      if (show_tooltips) {
        txt = STR_ACTION_HIDE_TOOLTIPS;
      } else {
        txt = STR_ACTION_SHOW_TOOLTIPS;
      }
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      mi.setEnabled(true);
      popup.add(mi);

      // Add "Hide/Show Inclusive Metric" action
      if (show_inclusive) {
        txt = STR_ACTION_HIDE_INCLUSIVE;
      } else {
        txt = STR_ACTION_SHOW_INCLUSIVE;
      }
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      // popup.add(mi); // not ready yet

      // Add "Show Exclusive Metric" action
      txt = STR_ACTION_SHOW_EXCLUSIVE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(false); // TEMPORARY
      // popup.add(mi); // not ready yet

      // Add "Hide/Show Percentage" action
      txt = STR_ACTION_SHOW_PERCENTAGE;
      if (show_percentage) {
        txt = STR_ACTION_HIDE_PERCENTAGE;
      } else {
        txt = STR_ACTION_SHOW_PERCENTAGE;
      }
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      // popup.add(mi); // not ready yet

      // Add separator
      popup.addSeparator();
      // Add "Sort By Metric" action
      txt = STR_SORT_BY_METRIC;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (sortByName) {
        mi.setEnabled(true);
      } else {
        mi.setEnabled(false);
      }
      popup.add(mi);
      // Add "Sort By Name" action
      txt = STR_SORT_BY_NAME;
      UpdateAction st1 = new UpdateAction(txt);
      mi = new JMenuItem(st1);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (sortByName) {
        mi.setEnabled(false);
      } else {
        mi.setEnabled(true);
      }
      popup.add(mi);

      // Add separator
      popup.addSeparator();

      // Add "Set Metric" action
      txt = STR_ACTION_SETMETRIC;
      UpdateAction sm = new UpdateAction(txt);
      mi = new JMenuItem(sm);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (available_metrics.length <= 1) {
        mi.setEnabled(false);
        popup.add(mi);
      } else {
        JMenu submenu1;
        submenu1 = new JMenu(txt);
        for (int i = 0; i < available_metrics.length; i++) {
          txt = available_metrics[i].getUserName();
          sm = new UpdateAction(txt);
          mi = new JMenuItem(sm);
          ac = mi.getAccessibleContext();
          ac.setAccessibleDescription(STR_ACTION_SETMETRIC + ":" + txt);
          if (available_metrics[i].isNameMetric()) {
            mi.setEnabled(false);
          } else {
            mi.setEnabled(true);
          }
          submenu1.add(mi);
        }
        popup.add(submenu1);
      }

      // Add "Refresh Tree" action
      txt = STR_ACTION_REFRESH;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      // popup.add(mi);
    }
    // Add separator
    popup.addSeparator();
    // Add menu item "Copy All"
    txt = STR_ACTION_COPY_ALL;
    UpdateAction cs = new UpdateAction(txt);
    mi = new JMenuItem(cs);
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    popup.add(mi);
    // Add separator
    popup.addSeparator();
    // Add "Settings" action
    txt = STR_ACTION_SETTINGS;
    UpdateAction st0 = new UpdateAction(txt);
    mi = new JMenuItem(st0);
    mi.setAccelerator(KeyboardShortcuts.settingsActionShortcut);
    ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    popup.add(mi);

    return popup;
  }

  @Override
  public List<ExportFormat> getSupportedExportFormats() {
    List<ExportFormat> formats = new ArrayList<ExportFormat>();
    formats.add(ExportFormat.TEXT);
    //        formats.add(ExportFormat.HTML);
    //        formats.add(ExportFormat.CSV);
    formats.add(ExportFormat.JPG);
    return formats;
  }

  @Override
  public boolean exportLimitSupported() {
    return false;
  }

  // ------- Private classes to implement popup menu items ------- //
  private class AnMenuListener extends MouseAdapter {

    private boolean debug;
    // NM private AnTable anTable;
    CallTreeView callTreeDisp;
    JTree tree;

    AnMenuListener(CallTreeView callTreeDisp, JTree tree) {
      this.callTreeDisp = callTreeDisp;
      this.tree = tree;
      debug = false;
    }

    // Experimental code, mostly "quick and dirty hack"
    public JPopupMenu initPopup(MouseEvent event) {
      // Experimental code
      // NM if (parent != null) {
      // NM     try {
      // NM         parent.changeSelection(anTable, table.rowAtPoint(event.getPoint()));
      // NM     } catch (Exception exc) {
      // NM         System.out.println("AnTable.init_popup() exception: " + exc);
      // NM         exc.printStackTrace();
      // NM     }
      // NM }
      // NM setSelectedRowNow(table.rowAtPoint(event.getPoint()));
      return callTreeDisp.initPopup(false);
    }

    /** Check for double click to performs default action */
    @Override
    public void mouseClicked(final MouseEvent e) {
      if (e.getClickCount() == 2) {
        performDefaultAction();
      }
    }

    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        if (!Analyzer.getInstance().normalSelection) {
          int row = tree.getClosestRowForLocation(e.getX(), e.getY());
          if (row != -1 && !tree.isRowSelected(row)) {
            // if (tree.getRowCount() <= (row-1)) {
            //    System.out.println("row="+row+" max="+tree.getRowCount());
            // }
            tree.setSelectionRow(row);
            // fireAnEvent(new AnEvent(fl_table, AnEvent.EVT_SELECT, row, null));
          }
        }
        JPopupMenu popup = initPopup(e);
        if (popup != null) {
          popup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuWillBecomeInvisible(" + e + ")");
      }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuWillBecomeVisible(" + e + ")");
      }
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
      if (debug) {
        System.out.println("AnMenuListener:popupMenuCanceled(" + e + ")");
      }
    }
  }

  /*
   * Generic action for context menu items.
   * Action name is passed as String.
   */
  class UpdateAction extends AbstractAction {

    String actionName = null;

    public UpdateAction(String name) {
      super(name);
      this.actionName = name;
    }

    public UpdateAction(String name, String actionName) {
      super(name);
      this.actionName = actionName;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      // Use AWT thread to perform some actions
      //            if (actionName.equals(STR_ACTION_MANAGEFILTERS)) {
      //                window.filter.setSelectedTab(0);
      //                window.filter.showDialog();
      //                return;
      //            }
      if (actionName.equals(STR_ACTION_SHOW_EXCLUSIVE)) {
        show_exclusive = true;
        return;
      }

      if (actionName.equals(FILTER_SELECTED_BRANCH_ACTION_NAME)
          || actionName.equals(FILTER_NOT_SELECTED_BRANCH_ACTION_NAME)
          || actionName.equals(FILTER_SELECTED_FUNCTION_ACTION_NAME)
          || actionName.equals(FILTER_SELECTED_LEAF_ACTION_NAME)
          || actionName.equals(STR_ACTION_CUSTOM_FILTER)
          || actionName.equals(STR_ACTION_REMOVE_ALL_FILTERS)
          || actionName.equals(STR_ACTION_UNDO_FILTER)
          || actionName.equals(STR_ACTION_REDO_FILTER)) {
        // Do these actions on AWT
        CT_UpdateTree(actionName, ev);
        return;
      }

      // Use a "worker" thread to perform other actions
      final String an = actionName;
      final ActionEvent ae = ev;
      AnUtility.dispatchOnAWorkerThread(
          new Runnable() {
            @Override
            public void run() {
              CT_UpdateTree(an, ae);
            }
          },
          "CallTree_thread");
    }
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
