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

import static org.gprofng.mpmt.event.AnChangeEvent.Type.DEBUG;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADED_FAILED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADING_ADDED_OR_REMOVED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.EXPERIMENTS_LOADING_NEW;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.FILTER_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.FILTER_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.MOST_RECENT_EXPERIMENT_LIST_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.REMOTE_CONNECTION_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.REMOTE_CONNECTION_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTED_OBJECT_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTION_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTION_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SELECTION_UPDATE;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SETTING_CHANGING;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SOURCE_FINDING_CHANGED;
import static org.gprofng.mpmt.event.AnChangeEvent.Type.SOURCE_FINDING_CHANGING;
import static org.gprofng.mpmt.util.gui.AnUtility.SPACES_BETWEEN_COLUMNS;
import static org.gprofng.mpmt.util.gui.AnUtility.EOL;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.DisasmDisp.DisRenderer;
import org.gprofng.mpmt.SourceDisp.SrcRenderer;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.filter.FilterClause;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.table.AnJTable;
import org.gprofng.mpmt.table.AnTableHeaderPanel;
import org.gprofng.mpmt.table.AnTableScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public final class AnTable extends AnTableScrollPane implements AnChangeListener {

  private static final int AT_LIST = 0;
  public static final int AT_SRC = 1;
  protected static final int AT_SRC_ONLY = 2;
  public static final int AT_DIS = 3;
  private static final int AT_COM = 4;
  public static final int AT_QUOTE = 5;
  protected static final int AT_FUNC = 6;
  private static final int AT_EMPTY = 7;
  protected static final int AT_DIS_ONLY = 8;

  private static final int NUMBER_SPACE = 3;
  private static Font TableFont;
  private static Font QuoteFont;

  static {
    JTable jtable = new JTable();
    Font org_font = jtable.getFont();
    TableFont = new Font("Monospaced", org_font.getStyle(), org_font.getSize());
    QuoteFont = TableFont.deriveFont(Font.ITALIC);
  }

  // Action names
  private final String STR_ACTION_COPY_ALL = AnLocale.getString("Copy All");
  private final String STR_ACTION_COPY_SEL = AnLocale.getString("Copy Selected");
  private final String STR_ACTION_BACK = AnLocale.getString("Back");
  private final String STR_ACTION_FORWARD = AnLocale.getString("Forward");
  private final String STR_ACTION_SETHEAD = AnLocale.getString("Set Head");
  private final String STR_ACTION_SETTAIL = AnLocale.getString("Set Tail");
  private final String STR_ACTION_REMOVE = AnLocale.getString("Remove");
  private final String STR_ACTION_PREPEND = AnLocale.getString("Add Caller");
  private final String STR_ACTION_APPEND = AnLocale.getString("Add Callee");
  private final String STR_ACTION_RECENTER = AnLocale.getString("Set Center");
  private final String STR_ACTION_RESET = AnLocale.getString("Reset");
  private final String STR_ACTION_SHOW_DISASM = AnLocale.getString("Show Disassembly");
  private final String STR_ACTION_SHOW_SOURCE = AnLocale.getString("Show Source");
  private final String STR_ACTION_SHOW_SELECTED_FUNCTION =
      AnLocale.getString("Show the Selected Function");
  private final String STR_ACTION_SHOW_CALLER_DISASM =
      AnLocale.getString("Show Caller Disassembly");
  protected static final String STR_ACTION_SHOW_CALLEE_DISASM =
      AnLocale.getString("Show Callee Disassembly");
  private final String STR_ACTION_SHOW_CALLER_SOURCE = AnLocale.getString("Show Caller Source");
  protected static final String STR_ACTION_SHOW_CALLEE_SOURCE =
      AnLocale.getString("Show Callee Source");

  private static final String ADD_FILTER = AnLocale.getString("Add Filter");

  private static final String FILTER_SIMILARLY_NAMED_SHORT_NAME =
      AnLocale.getString("Similarly Named");
  private static final String FILTER_SIMILARLY_NAMED_LONG_NAME =
      AnLocale.getString("Include only stacks containing similarly-named functions");
  private static final String FILTER_SIMILARLY_NAMED_ACTION_NAME = "FilterSimilarlyNamed";

  private static final String FILTER_CALLSTACK_FRAGMENT_SHORT_NAME =
      AnLocale.getString("Callstack Fragment");
  private static final String FILTER_CALLSTACK_FRAGMENT_LONG_NAME =
      AnLocale.getString("Include only stacks containing this callstack fragment");
  private static final String FILTER_CALLSTACK_FRAGMENT_ACTION_NAME = "FilterCallstackFragment";

  private static final String FILTER_NOT_CALLSTACK_FRAGMENT_SHORT_NAME =
      AnLocale.getString("Not Callstack Fragment");
  private static final String FILTER_NOT_CALLSTACK_FRAGMENT_LONG_NAME =
      AnLocale.getString("Include only stacks not containing this callstack fragment");
  private static final String FILTER_NOT_CALLSTACK_FRAGMENT_ACTION_NAME =
      "FilterNotCallstackFragment";

  private static final String FILTER_SELECTED_FUNCTION_SHORT_NAME =
      AnLocale.getString("Selected Function");
  private static final String FILTER_SELECTED_FUNCTION_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected function");
  private static final String FILTER_SELECTED_FUNCTION_ACTION_NAME = "FilterSelectedFunction";

  private static final String FILTER_SELECTED_FUNCTIONS_SHORT_NAME =
      AnLocale.getString("Selected Functions");
  private static final String FILTER_SELECTED_FUNCTIONS_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected functions");
  private static final String FILTER_SELECTED_FUNCTIONS_ACTION_NAME = "FilterSelectedFunctions";

  private static final String FILTER_NOT_SELECTED_FUNCTION_SHORT_NAME =
      AnLocale.getString("Not Selected Function");
  private static final String FILTER_NOT_SELECTED_FUNCTION_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected function");
  private static final String FILTER_NOT_SELECTED_FUNCTION_ACTION_NAME =
      "FilterNotSelectedFunction";

  private static final String FILTER_NOT_SELECTED_FUNCTIONS_SHORT_NAME =
      AnLocale.getString("Not Selected Functions");
  private static final String FILTER_NOT_SELECTED_FUNCTIONS_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected functions");
  private static final String FILTER_NOT_SELECTED_FUNCTIONS_ACTION_NAME =
      "FilterNotSelectedFunctions";

  private static final String FILTER_LEAF_FUNCTION_SHORT_NAME =
      AnLocale.getString("Selected Leaf Function");
  private static final String FILTER_LEAF_FUNCTION_LONG_NAME =
      AnLocale.getString("Include only stacks with the selected function as leaf");
  private static final String FILTER_LEAF_FUNCTION_ACTION_NAME = "FilterSelectedLeafFunction";

  private static final String FILTER_LEAF_FUNCTIONS_SHORT_NAME =
      AnLocale.getString("Selected Leaf Functions");
  private static final String FILTER_LEAF_FUNCTIONS_LONG_NAME =
      AnLocale.getString("Include only stacks with the selected functions as leaf");
  private static final String FILTER_LEAF_FUNCTIONS_ACTION_NAME = "FilterSelectedLeafFunctions";

  private final String FILTER_SELECTED_LINE_SHORT_NAME = AnLocale.getString("Selected Line");
  private final String FILTER_SELECTED_LINE_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected line");
  private final String FILTER_SELECTED_LINE_ACTION_NAME = "FilterSelectedLine";

  private final String FILTER_SELECTED_LINES_SHORT_NAME = AnLocale.getString("Selected Lines");
  private final String FILTER_SELECTED_LINES_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected lines");
  private final String FILTER_SELECTED_LINES_ACTION_NAME =
      AnLocale.getString("FilterSelectedLines");

  private final String FILTER_NOT_SELECTED_LINE_SHORT_NAME =
      AnLocale.getString("Not Selected Line");
  private final String FILTER_NOT_SELECTED_LINE_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected line");
  private final String FILTER_NOT_SELECTED_LINE_ACTION_NAME = "FilterNotSelectedLine";

  private final String FILTER_NOT_SELECTED_LINES_SHORT_NAME =
      AnLocale.getString("Not Selected Lines");
  private final String FILTER_NOT_SELECTED_LINES_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected lines");
  private final String FILTER_NOT_SELECTED_LINES_ACTION_NAME = "FilterNotSelectedLines";

  private final String FILTER_SELECTED_PC_SHORT_NAME = AnLocale.getString("Selected PC");
  private final String FILTER_SELECTED_PC_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected PC");
  private final String FILTER_SELECTED_PC_ACTION_NAME = "FilterSelected PC";

  private final String FILTER_SELECTED_PCS_SHORT_NAME = AnLocale.getString("Selected PCs");
  private final String FILTER_SELECTED_PCS_LONG_NAME =
      AnLocale.getString("Include only stacks containing the selected PCs");
  private final String FILTER_SELECTED_PCS_ACTION_NAME = "AddFilterSelectedPCs";

  private final String FILTER_NOT_SELECTED_PC_SHORT_NAME = AnLocale.getString("Not Selected PC");
  private final String FILTER_NOT_SELECTED_PC_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected PC");
  private final String FILTER_NOT_SELECTED_PC_ACTION_NAME =
      AnLocale.getString("FilterNotSelectedPC");

  private final String FILTER_NOT_SELECTED_PCS_SHORT_NAME = AnLocale.getString("Not Selected PCs");
  private final String FILTER_NOT_SELECTED_PCS_LONG_NAME =
      AnLocale.getString("Include only stacks not containing the selected PCs");
  private final String FILTER_NOT_SELECTED_PCS_ACTION_NAME =
      AnLocale.getString("FilterNotSelected PCs");

  private static final String FILTER_WITHOUT_SELECTED_ITEMS_SHORT_NAME =
      AnLocale.getString("Without Selected Items");
  private static final String FILTER_WITHOUT_SELECTED_ITEMS_LONG_NAME =
      AnLocale.getString("Include only events without selected items");
  private static final String FILTER_WITHOUT_SELECTED_ITEMS_ACTION_NAME =
      "FilterWithoutSelectedItems";

  private static final String FILTER_WITH_SELECTED_ITEMS_SHORT_NAME =
      AnLocale.getString("With Selected Items");
  private static final String FILTER_WITH_SELECTED_ITEMS_LONG_NAME =
      AnLocale.getString("Include only events with selected items");
  private static final String FILTER_WITH_SELECTED_ITEMS_ACTION_NAME = "FilterWithSelectedItems";

  private static final String FILTER_WITH_SELECTED_FILE_SHORT_NAME =
      AnLocale.getString("Selected Files");
  private static final String FILTER_WITH_SELECTED_FILE_LONG_NAME =
      AnLocale.getString("Include only events containing the selected files");
  private static final String FILTER_WITH_SELECTED_FILE_ACTION_NAME = "FilterWithSelectedFiles";

  private static final String FILTER_NOT_SELECTED_FILE_SHORT_NAME =
      AnLocale.getString("Unselected Files");
  private static final String FILTER_NOT_SELECTED_FILE_LONG_NAME =
      AnLocale.getString("Include only events not containing the selected files");
  private static final String FILTER_NOT_SELECTED_FILE_ACTION_NAME = "FilterWithNotSelectedFiles";

  private static final String FILTER_WITH_SELECTED_IOVFD_SHORT_NAME =
      AnLocale.getString("Selected File Descriptors");
  private static final String FILTER_WITH_SELECTED_IOVFD_LONG_NAME =
      AnLocale.getString("Include only events containing the selected file descriptors");
  private static final String FILTER_WITH_SELECTED_IOVFD_ACTION_NAME =
      "FilterWithSelectedFileDescriptors";

  private static final String FILTER_NOT_SELECTED_IOVFD_SHORT_NAME =
      AnLocale.getString("Unselected File Descriptors");
  private static final String FILTER_NOT_SELECTED_IOVFD_LONG_NAME =
      AnLocale.getString("Include only events not containing the selected file descriptors");
  private static final String FILTER_NOT_SELECTED_IOVFD_ACTION_NAME =
      "FilterWithNotSelectedFileDescriptors";

  private static final String FILTER_WITH_SELECTED_IOSTACK_SHORT_NAME =
      AnLocale.getString("Selected Stacks");
  private static final String FILTER_WITH_SELECTED_IOSTACK_LONG_NAME =
      AnLocale.getString("Include only events containing the selected stacks");
  private static final String FILTER_WITH_SELECTED_IOSTACK_ACTION_NAME =
      "FilterWithSelectedIOStacks";

  private static final String FILTER_NOT_SELECTED_IOSTACK_SHORT_NAME =
      AnLocale.getString("Unselected Stacks");
  private static final String FILTER_NOT_SELECTED_IOSTACK_LONG_NAME =
      AnLocale.getString("Include only events not containing the selected stacks");
  private static final String FILTER_NOT_SELECTED_IOSTACK_ACTION_NAME =
      "FilterWithNotSelectedIOStacks";

  private static final String FILTER_WITH_SELECTED_HEAPSTACK_SHORT_NAME =
      AnLocale.getString("Selected Stacks");
  private static final String FILTER_WITH_SELECTED_HEAPSTACK_LONG_NAME =
      AnLocale.getString("Include only events containing the selected stacks");
  private static final String FILTER_WITH_SELECTED_HEAPSTACK_ACTION_NAME =
      "FilterWithSelectedHeapStacks";

  private static final String FILTER_NOT_SELECTED_HEAPSTACK_SHORT_NAME =
      AnLocale.getString("Unselected Stacks");
  private static final String FILTER_NOT_SELECTED_HEAPSTACK_LONG_NAME =
      AnLocale.getString("Include only events not containing the selected stacks");
  private static final String FILTER_NOT_SELECTED_HEAPSTACK_ACTION_NAME =
      "FilterWithNotSelectedHeapStacks";

  private static final String FILTER_WITH_SELECTED_HEAPACTIVESTACK_SHORT_NAME =
      AnLocale.getString("Peak Active Allocations");
  private static final String FILTER_WITH_SELECTED_HEAPACTIVESTACK_LONG_NAME =
      AnLocale.getString("Include only active allocations for the peak");
  private static final String FILTER_WITH_SELECTED_HEAPACTIVESTACK_ACTION_NAME =
      "FilterActiveHeapAllocations";

  private static final String FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_SHORT_NAME =
      AnLocale.getString("Only Leaks");
  private static final String FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_LONG_NAME =
      AnLocale.getString("Include only allocations that leaked");
  private static final String FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_ACTION_NAME = "FilterHeapLeaked";

  private static final String FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_SHORT_NAME =
      AnLocale.getString("Exclude Leaks");
  private static final String FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_LONG_NAME =
      AnLocale.getString("Exclude allocations that leaked");
  private static final String FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_ACTION_NAME =
      "FilterHeapExcludeLeaked";

  private static final String FILTER_WITH_SELECTED_JGROUP_SHORT_NAME =
      AnLocale.getString("Selected JGroup");
  private static final String FILTER_WITH_SELECTED_JGROUP_LONG_NAME =
      AnLocale.getString("Include only JThreads in selected group");
  private static final String FILTER_WITH_SELECTED_JGROUP_ACTION_NAME = "FilterWithSelectedJGroup";

  private static final String FILTER_WITH_SELECTED_JPARENT_SHORT_NAME =
      AnLocale.getString("Selected JParent");
  private static final String FILTER_WITH_SELECTED_JPARENT_LONG_NAME =
      AnLocale.getString("Include only JThreads in selected parent");
  private static final String FILTER_WITH_SELECTED_JPARENT_ACTION_NAME =
      "FilterWithSelectedJParent";

  private final String STR_ACTION_UNDOFILTER = AnLocale.getString("Undo Filter Action");
  private final String STR_ACTION_REDOFILTER = AnLocale.getString("Redo Filter Action");
  private final String STR_ACTION_REMOVE_ALL_FILTERS = AnLocale.getString("Remove All Filters");
  private final String STR_ACTION_CUSTOM_FILTER =
      ADD_FILTER + ": " + AnLocale.getString("Advanced Custom Filter...");

  private AnMenuListener menuListener;
  private AnTable anTable;
  private JTable table;
  private int type;
  private JTableHeader tableHeader;
  private FListTableModel tableModel;
  private FListColumnModel columnModel;
  private FontMetrics fontMetrics;
  private int[][] columnWidth;
  private int rowHeight;
  private String[] names;
  private int orgRow; // What is this?
  private int selectedRow;
  private int nameColumn;
  private int sortColumn;
  private int nameWidth;
  private boolean hasHeader;
  private boolean canSort;
  private boolean isSingle;
  private boolean colScroll;
  private boolean hasSelect;
  private boolean wrapMetricNames;
  public boolean columnsSaved = false;
  public int[] selected_indices;
  private String acName;
  private String acDesc;
  private JLabel acLabel;
  private AnDisplay parent = null;
  public AnHotGap gap;

  protected SrcRenderer srcRenderer = null;
  protected DisRenderer disRenderer = null;
  private NavigationHistoryPool navHistoryPool = null;

  private int groupId = 0;
  private JComponent allHeaderPanel = null;
  private static Integer MIN_CELL_WIDTH = null;

  private HotGapPanel hotGapPanel = null;

  /**
   * Returns a new instance of AnTable, which extends JScrollPane and contains a JTable inside.
   *
   * <p>This method always creates a new JTable, according to the specified requirements. It sets
   * Accessible Name, Accessible Description and Label for the JTable.
   *
   * @param type Type of AnDisplay, that creates AnTable (parent type)
   * @param hasHeader Boolean flag: if true, AnTable has header
   * @param hasColumnButton
   * @param can_sort Boolean flag: if true, information can be sorted
   * @param isSingle Boolean flag: if true, the table does not allow multiselection
   * @param colScroll Boolean flag: if true, set HorizontalScrollBar as needed (false - never)
   * @param hasSelect Boolean flag: if true, show selection
   * @param wrapMetricNames
   * @param accessibleName Accessible Name for JTable
   * @param accessibleDescr Accessible Description for JTable
   * @param LabelFor Label for JTable
   * @see AnTable
   */
  public AnTable(
      int type,
      boolean hasHeader,
      boolean hasColumnButton,
      boolean can_sort,
      boolean isSingle,
      boolean colScroll,
      boolean hasSelect,
      boolean wrapMetricNames,
      String accessibleName,
      String accessibleDescr,
      JLabel LabelFor) {
    super(type, hasColumnButton);
    this.type = type;
    this.hasHeader = hasHeader;
    this.canSort = can_sort;
    this.isSingle = isSingle;
    this.colScroll = colScroll;
    this.hasSelect = hasSelect;
    this.wrapMetricNames = wrapMetricNames;
    this.acName = accessibleName;
    this.acDesc = accessibleDescr;
    this.acLabel = LabelFor;
    anTable = this;
    fontMetrics = null;
    rowHeight = 20;
    orgRow = -1;
    selectedRow = 0;
    nameColumn = 0;
    sortColumn = -1;
    nameWidth = 0;
    columnWidth = new int[1][4];
    columnWidth[0][0] = 30;
    columnWidth[0][1] = columnWidth[0][2] = columnWidth[0][3] = 10;

    initComponents();

    AnEventManager.getInstance().addListener(this);
  }

  public void setHotGapPanel(HotGapPanel hotGapPanel) {
    this.hotGapPanel = hotGapPanel;
  }

  private static int getMinimumColumnWidth() {
    if (MIN_CELL_WIDTH == null) {
      MIN_CELL_WIDTH = AnTableHeaderPanel.getMinimumColumnWidth();
    }
    return MIN_CELL_WIDTH;
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
      case EXPERIMENTS_LOADED_FAILED:
      case EXPERIMENTS_LOADED:
      case FILTER_CHANGED:
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
        if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.TABLE_FORMATS) {
          tableModel.updateTableHeaders();
        }
        break;
      case DEBUG:
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
  }

  /*
   * Hack: Set parent
   * used to clear selections in all tables in Callers-Callees tab
   */
  public void setParent(AnDisplay parent) {
    this.parent = parent;
  }

  public AnDisplay getAnParent() {
    return parent;
  }

  protected NavigationHistoryPool getNavigationHistoryPool() {
    return navHistoryPool;
  }

  public int getNameCol() {
    return nameColumn;
  }

  public int getType() {
    return anTable.type;
  }

  public boolean wrapMetricNames() {
    return wrapMetricNames;
  }

  public void setWrapMetricNames(boolean val) {
    wrapMetricNames = val;
  }

  protected AnMenuListener getMenuListener() {
    return menuListener;
  }

  // Initialize GUI components
  private void initComponents() {
    final Dimension psize;
    final ListSelectionModel sel_model;
    final CallHandler callHandler;
    final JLabel corner;

    switch (type) {
      case AnDisplay.DSP_Functions:
        navHistoryPool = new NavigationHistoryPool();
        break;
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_DisassemblyV2:
        disRenderer = new DisRenderer(anTable);
        navHistoryPool = new NavigationHistoryPool();
        break;
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_SourceV2:
        srcRenderer = new SrcRenderer(anTable);
        navHistoryPool = new NavigationHistoryPool();
        break;
    }

    gap = null;
    tableModel = new FListTableModel();
    columnModel = new FListColumnModel();
    table = new AnJTable(tableModel, columnModel);
    AnUtility.setAccessibleContext(
        table.getAccessibleContext(),
        AnLocale.getString("Table")); // FIXUP: export table is using AccessibleContext in output!
    table.setBorder(null);

    // A11Y for tables
    AccessibleContext ac = table.getAccessibleContext();
    if (acName != null) {
      ac.setAccessibleName(acName);
    }
    if (acDesc != null) {
      ac.setAccessibleDescription(acDesc);
    }
    if (acLabel != null) {
      acLabel.setLabelFor(table);
    }

    callHandler = new CallHandler();
    table.registerKeyboardAction(callHandler, "ENTER",
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    table.registerKeyboardAction(callHandler, "CNTRL_C",
        KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    // Popup menu
    menuListener = new AnMenuListener(this);
    table.addMouseListener(menuListener);
    this.addMouseListener(menuListener);

    KeyStroke ks = KeyboardShortcuts.contextMenuActionShortcut;
    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, ks);
    table
        .getActionMap()
        .put(
            ks,
            new AbstractAction() {

              @Override
              public void actionPerformed(ActionEvent ev) {
                JPopupMenu popup = initPopup(false);
                if (popup != null) {
                  JTable src = (JTable) ev.getSource();
                  Rectangle cellRect, visRect;

                  visRect = src.getVisibleRect();
                  cellRect = visRect;
                  int selrow = src.getSelectedRow();
                  if (selrow >= 0) {
                    cellRect = src.getCellRect(selrow, 0, false);
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

    switch (type) {
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_SourceV2:
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_DisassemblyV2:
        // Previous Hot Line
        KeyStroke keyStrokeUp = KeyboardShortcuts.sourcePreviousHotLineActionShortcut;
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeUp, keyStrokeUp);
        table
            .getActionMap()
            .put(
                keyStrokeUp,
                new AbstractAction() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    gap.HG_Prev_Hot_Line();
                  }
                });
        // Next Hot Line
        KeyStroke keyStrokeDown = KeyboardShortcuts.sourceNextHotLineActionShortcut;
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeDown, keyStrokeDown);
        table
            .getActionMap()
            .put(
                keyStrokeDown,
                new AbstractAction() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    gap.HG_Next_Hot_Line();
                  }
                });
        // Next NZ Line
        KeyStroke keyStrokeNextNZ = KeyboardShortcuts.sourceNextNonZeroLineActionShortcut;
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStrokeNextNZ, keyStrokeNextNZ);
        table
            .getActionMap()
            .put(
                keyStrokeNextNZ,
                new AbstractAction() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    gap.HG_Next_NZ_Line();
                  }
                });
        // Previous NZ Line
        KeyStroke keyStrokePreviousNZ = KeyboardShortcuts.sourcePreviousNonZeroLineActionShortcut;
        table
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(keyStrokePreviousNZ, keyStrokePreviousNZ);
        table
            .getActionMap()
            .put(
                keyStrokePreviousNZ,
                new AbstractAction() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    gap.HG_Prev_NZ_Line();
                  }
                });
    }

    table.setAutoCreateColumnsFromModel(true);
    table.setShowGrid(false);
    table.setIntercellSpacing(new Dimension(0, 0));
    //            table.setShowVerticalLines(true);
    //            table.setGridColor(AnEnvironment.TABLE_VERTICAL_GRID_COLOR);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.setAutoscrolls(false);
    if (isSingle) {
      psize = table.getPreferredScrollableViewportSize();
      psize.height = TableFont.getSize() + table.getRowMargin() + 5;
      table.setPreferredScrollableViewportSize(psize);
    }

    // Set selecting mode & add events listener for row selecting
    sel_model = table.getSelectionModel();
    // sel_model.setSelectionMode(canSort
    //			? ListSelectionModel.SINGLE_SELECTION
    //			: ListSelectionModel.SINGLE_INTERVAL_SELECTION);

    if (!isSingle) {
      sel_model.addListSelectionListener(new CellHandler());
    }

    if (!hasSelect) {
      table.addMouseListener(callHandler);
      table.registerKeyboardAction(callHandler, "SPACE",
          KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
          JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    } else {
      table.addMouseListener(new TableAdapter());
    }

    // Set table font as proportionally-spaced
    table.setFont(TableFont);

    // Set default table cell renderer
    table.setDefaultRenderer(Object.class, new CellRenderer());
    table.setDefaultEditor(Object.class, new CellEditor());

    // Set table as the View component
    setViewportView(table);

    corner = new JLabel();
    corner.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    setCorner(UPPER_RIGHT_CORNER, corner);

    setHorizontalScrollBarPolicy(
        colScroll ? HORIZONTAL_SCROLLBAR_AS_NEEDED : HORIZONTAL_SCROLLBAR_NEVER);
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
    if (isSingle) {
      getVerticalScrollBar().setEnabled(false);
    }

    // Add events listener for sorting (clicks on header), and
    // Set default header renderer
    tableHeader = table.getTableHeader();
    if (hasHeader) {
    } else {
      table.setTableHeader(null);
      if (colScroll) {
        getHorizontalScrollBar().addAdjustmentListener(new ScrollHandler());
      }
    }

    // Add resize listener
    addComponentListener(new TableListener());
  }

  public JTable getTable() {
    return table;
  }

  /**
   * @param view Hack to prevent header panel to be overriden
   */
  @Override
  public void setColumnHeaderView(Component view) {
    if (allHeaderPanel != null) {
      super.setColumnHeaderView(allHeaderPanel);
    } else {
      super.setColumnHeaderView(view);
    }
  }

  // Set header for table without real header
  public void setHeader(final AnTable other_table) {
    tableHeader = other_table.tableHeader;
  }

  // Set table as the View component
  public void setViewport() {
    setViewportView(table);
  }

  // Returns sort column
  public int getSortColumn() {
    return sortColumn;
  }

  public boolean canSort() {
    return canSort;
  }

  public String[] getNames() {
    return names;
  }

  public int getSrcType(int row) {
    if (tableModel != null) {
      return tableModel.getSrcType(row);
    }
    return AT_LIST;
  }

  public void goToLine(int row) {
    if (row < 0) {
      return;
    }
    switch (type) {
      case AnTable.AT_SRC:
      case AnTable.AT_DIS:
      case AnTable.AT_QUOTE:
        break;
      default:
        return;
    }
    Object[][] d = getTableData();
    if (d.length <= row) {
      return;
    }
    setSelectedRow(row);
  }

  // Returns row count
  public int getRowCount() {
    if (null == table) {
      return -1;
    }
    return table.getRowCount();
  }

  /** Performs default action for double click */
  public void performDefaultAction() {
    //        System.out.println("performDefaultAction");
    // Switch Display
    switch (type) {
      case AnDisplay.DSP_Functions:
      case AnDisplay.DSP_Lines:
        { // Default action: Show Source
          parent.window.setSelectedView(AnDisplay.DSP_Source);
          break;
        }
      case AnDisplay.DSP_PCs:
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_SourceV2:
        { // Default action: Show Disassembly
          parent.window.setSelectedView(AnDisplay.DSP_Disassembly);
          break;
        }
      case AnDisplay.DSP_MiniFunctions:
        {
          parent.window.setSelectedView(AnDisplay.DSP_Functions);
          break;
        }
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_DisassemblyV2:
        {
          parent.window.getSettings().toggleDisFuncScope();
          break;
        }
      case AnDisplay.DSP_MiniCaller:
      case AnDisplay.DSP_MiniCallee:
        {
          updateSelectedRow(table.getSelectedRow());
          break;
        }
    }
  }

  /** Create text presentation of the table header. */
  protected String printTableHeader(String sortedby, Object[][] MaximumValues) {
    JTable t = table;
    String empty = "";
    String tname = empty;
    // Get table name (preferably localized)
    if (null != t.getAccessibleContext()) {
      tname = t.getAccessibleContext().getAccessibleName();
    } else {
      // There should be some other way to get table name
      tname = t.getName();
    }
    if (null == tname) {
      tname = empty;
    }
    return printTableHeader(tname, sortedby, MaximumValues);
  }

  private static void append_blanks(StringBuilder sb, int n) {
    for (int i = 0; i < n; i++) {
      sb.append(' ');
    }
  }

  private static void stripTrailing(StringBuilder sb) {
    int length = sb.length();
    while (length > 0 && Character.isWhitespace(sb.charAt(length - 1))) {
        length--;
    }
    sb.setLength(length);
  }

  /** Create text presentation of the table header. */
  protected synchronized String printTableHeader(
      String tname, String sortedby, Object[][] MaximumValues) {
    // Prepare text presentation of the table
    StringBuilder sb = new StringBuilder();
    if (canSort) {
      // text = "Functions sorted by metric: Exclusive Total CPU Time\n"
      int sc = getSortColumn();
      if (sc >= 0) {
        MetricLabel m = getTableModel().getLabel(sc);
        String ltext = m.getAnMetric().getName();
        if (tname != null && tname.length() > 0) {
          sb.append(tname);
          sb.append(" ");
        }
        sb.append(sortedby);
        sb.append(" ");
        sb.append(ltext);
        sb.append(EOL);
      }
    } else if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2) {
      // Print Source File name
      if (names != null && names.length > 0) {
        sb.append(names[0]); // Source File: ...
      }
    } else if (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2) {
      // Print Load Object name
      if (names != null && names.length > 2) {
        sb.append(names[2]); // Load Object: ...
      }
    }
    if (sb.length() > 0) {
      sb.append(EOL);
    } else if (tname != null && tname.length() > 0) {
      sb.append(tname);
      sb.append(EOL);
    }

    StringBuilder line = new StringBuilder();
    StringBuilder column = new StringBuilder();
    MetricLabel[] labels = tableModel.metricLabels;
    for (int lineN = 0;; lineN++) {
      line.setLength(0);
      for (int i = 0; i < labels.length; i++) {
        MetricLabel ml = labels[i];
        String[] titles = ml.getLegendAndTitleLines();
        String s = "";
        if (lineN < titles.length && titles[lineN] != null) {
          s = titles[lineN];
        }
        line.append(s);
        if (i + 1 < labels.length) {
          append_blanks(line, ml.getColumnWidth() + SPACES_BETWEEN_COLUMNS
              - s.length());
        }
      }
      stripTrailing(line);
      if (line.length() == 0) {
        break;
      }
      sb.append(line);
      sb.append(EOL);
    }

    line.setLength(0);
    for (int i = 0; i < labels.length; i++) {
      MetricLabel ml = labels[i];
      AnMetric m = ml.getAnMetric();
      column.setLength(0);
      String s = ml.getUnit();
      if (s == null || s.length() == 0) {
        if (!m.isNameMetric() && (m.isTVisible() || m.isVVisible())) {
          column.append("#");
        }
      } else {
        column.append(s);
      }
      if (m.isPVisible()) {
        append_blanks(column, ml.getValWidth() - column.length());
        if (ml.getValWidth() > 0) {
          append_blanks(column, SPACES_BETWEEN_COLUMNS);
        }
        append_blanks(column, ml.getPercentWidth() - 3);
        column.append("%");
      }
      if (i + 1 < labels.length) {
        append_blanks(column, ml.getColumnWidth() + SPACES_BETWEEN_COLUMNS
            - column.length());
      }
      line.append(column);
    }
    stripTrailing(line);
    if (line.length() != 0) {
      sb.append(line);
      sb.append(EOL);
    }
    return sb.toString();
  }

  /** Create text presentation of the table. */
  protected String printTableContents(Object[][] MaximumValues, int printLimit) {
    boolean last_only = false;
    boolean selected_only = false;
    return printTableContents(printLimit, last_only, selected_only);
  }

  /** Create text presentation of the selected rows of the table. */
  protected String printSelectedTableContents(Object[][] MaximumValues, int printLimit) {
    boolean last_only = false;
    boolean selected_only = true;
    return printTableContents(printLimit, last_only, selected_only);
  }

  /** Create text presentation of the table.
   * @param printLimit
   * @param last_only
   * @param selected_only
   * @return  */
  public synchronized String printTableContents(int printLimit,
      boolean last_only, boolean selected_only) {
    if (printLimit < 0) {
      return "";
    }
    TableModel tModel = table.getModel();
    int rows = tModel.getRowCount();
    if (columnWidth.length <= 1) {
      return EOL + AnLocale.getString("No metrics selected for this view") + EOL;
    }
    if (printLimit > 0 && rows < printLimit) {
      rows = printLimit;  // print max printLimit
    }

    MetricLabel[] labels = tableModel.metricLabels;
    int sz = rows;
    int[] selected_ind = null;
    if (selected_only) {
      selected_ind = table.getSelectedRows();
      if (null == selected_ind) {
        return "";
      }
      sz = selected_ind.length;
    }
    StringBuilder sb = new StringBuilder();
    StringBuilder line = new StringBuilder();
    StringBuilder column = new StringBuilder();

    for (int index = 0; index < sz; index++) {
      int i = index;
      if (selected_only) {
        i = selected_ind[index];
      }

      // Check if all values are empty
      boolean emptyValues = false;
      int src_type = tableModel.getSrcType(i);
      if (src_type < 0) {
        src_type = -src_type;
      }
      if ((src_type == AT_SRC_ONLY)
          || (src_type == AT_COM)
          || (src_type == AT_FUNC)
          || (src_type == AT_EMPTY)
          || (src_type == AT_DIS_ONLY)) {
        emptyValues = true;
      }

      line.setLength(0);
      for (int j = 0; j < labels.length; j++) {
        MetricLabel ml = labels[j];
        AnMetric m = ml.getAnMetric();
        Object cellValue = tModel.getValueAt(i, j);
        column.setLength(0);
        if (m.isNameMetric()) {
          column.append(cellValue.toString());
        } else if (!emptyValues && !(last_only && (i + 1 != rows))) {
          if (m.isTVisible() || m.isVVisible()) {
            String s = cellValue.toString();
            append_blanks(column, ml.getValWidth() - s.length());
            column.append(s);
          } else if (ml.getValWidth() > 0) {
            append_blanks(column, ml.getValWidth());
          }
          if (m.isPVisible()) {
            if (ml.getValWidth() > 0) {
              append_blanks(column, SPACES_BETWEEN_COLUMNS);
            }
            String s = ((AnObject) cellValue).toPercent(ml.getTotal());
            append_blanks(column, ml.getPercentWidth() - s.length());
            column.append(s);
          }
        }
        if (j + 1 < labels.length) {
          append_blanks(column, ml.getColumnWidth() + SPACES_BETWEEN_COLUMNS
              - column.length());
        }
        line.append(column);
      }
      stripTrailing(line);
      sb.append(line);
      sb.append(EOL);
    }
    sb.append(EOL);
    return sb.toString();
  }

  // Listener for resize
  private final class TableListener implements ComponentListener {

    @Override
    public final void componentResized(final ComponentEvent e) {
      tableModel.fitColumnResize(-1);
      if (nameColumn != (columnWidth.length - 1)) {
        // See 22289649 - table headers not aligned with numbers when window is resized
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn nameColumn = columnModel.getColumn(AnTable.this.nameColumn);
        if (AnTable.this.nameColumn >= 0
            && AnTable.this.nameColumn < columnWidth.length - 1
            && columnWidth[AnTable.this.nameColumn].length > 0) {
          if (columnWidth[AnTable.this.nameColumn][0] != nameColumn.getWidth()) {
            columnWidth[AnTable.this.nameColumn][0] = nameColumn.getWidth();
            tableModel.updateTableHeaders(); // FIXUP: called too often in some cases....
          }
        }
      }
    }

    @Override
    public void componentHidden(final ComponentEvent e) {}

    @Override
    public void componentMoved(final ComponentEvent e) {}

    @Override
    public void componentShown(final ComponentEvent e) {}
  }

  // Returns table model
  public FListTableModel getTableModel() { // public for mem/idx objs
    return tableModel;
  }

  // A JLabel for table cell
  private static final class FListLabel extends JLabel {

    final boolean is_number;

    public FListLabel(final String text, final JComponent cmp, final int horizontalAlignment) {
      super(text);
      getAccessibleContext().setAccessibleDescription(text);
      getAccessibleContext().setAccessibleName(text);
      setHorizontalAlignment(horizontalAlignment);
      setVerticalAlignment(TOP);
      setFont(cmp.getFont());
      setBorder(AnVariable.labelBorder);

      setForeground(cmp.getForeground());
      setBackground(cmp.getBackground());

      is_number = (cmp instanceof JTable) && (horizontalAlignment == RIGHT);
    }

    protected final void paintComponent(final Graphics g) {
      final FontMetrics fm;
      final int ascent;
      int pos;
      final String str;
      String cstr;
      char ch;

      if (g == null) {
        System.err.println("NULL GRAPHICS");
      }

      if (!is_number) {
        super.paintComponent(g);
        return;
      }

      fm = g.getFontMetrics();
      ascent = fm.getAscent();
      pos = getWidth() - getInsets().right;
      str = getText();

      for (int i = str.length() - 1; i >= 0; i--) {
        ch = str.charAt(i);

        if (ch == '@') {
          pos -= NUMBER_SPACE;
        } else {
          cstr = String.valueOf(ch);
          pos -= fm.stringWidth(cstr);
          g.drawString(cstr, pos, ascent);
        }
      }
    }

    // Overridden for performance reasons.
    public final void validate() {}

    public final void revalidate() {}

    public final void repaint(
        final long tm, final int x, final int y, final int width, final int height) {}

    public final void repaint(final Rectangle r) {}

    protected final void firePropertyChange(
        final String propertyName, final Object oldValue, final Object newValue) {
      if (propertyName.equals("text")) {
        super.firePropertyChange(propertyName, oldValue, newValue);
      }
    }

    public final void firePropertyChange(
        final String propertyName, final boolean oldValue, final boolean newValue) {}
  }

  // A JPanel which may contain one or more values
  private final class CellPanel extends JPanel {

    public CellPanel(JTable table, boolean isSelected, int align) {
      setLayout(new FlowLayout(align, 0, 0));

      if (isSelected || isSingle) {
        setForeground(table.getSelectionForeground());
        setBackground(table.getSelectionBackground());
      } else {
        setForeground(table.getForeground());
        setBackground(table.getBackground());
      }
    }
  }

  private final class CellEditor extends AbstractCellEditor
      implements TableCellEditor, Serializable {

    private CellRenderer cr = new CellRenderer();

    public Component getTableCellEditorComponent(
        final JTable table,
        final Object value,
        final boolean isSelected,
        final int row,
        final int column) {
      return (cr.getTableCellRendererComponent(table, value, isSelected, true, row, column));
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }
  }

  // Table cell renderer
  private final class CellRenderer implements TableCellRenderer, Serializable {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

      final CellPanel panel;
      final int mcolumn;
      int src_type;
      final int[] col_w;
      FListLabel fl;
      final MetricLabel label;
      Dimension psize;

      if (value == null) {
        JPanel panel2 = new JPanel();
        panel2.setBackground(Color.red);
        return panel2;
      }

      panel =
          new CellPanel(
              table, isSelected, value instanceof String ? FlowLayout.LEFT : FlowLayout.CENTER);
      if (hasFocus) {
        panel.setBorder(AnVariable.tableFocusBorder);
      }
      // color background every other line
      if (!isSelected) {
        if (canSort() && column == sortColumn) {
          if (row % 2 == 1) {
            panel.setBackground(AnEnvironment.TABLE_LINE_BACKGROUND_SELECTED_COLUMN_COLOR_2);
          } else {
            panel.setBackground(AnEnvironment.TABLE_LINE_BACKGROUND_SELECTED_COLUMN_COLOR_1);
          }
        } else if (row % 2 == 1) {
          panel.setBackground(AnEnvironment.TABLE_LINE_BACKGROUND_COLOR_2);
        }
      }
      mcolumn = table.convertColumnIndexToModel(column);
      col_w = columnWidth[mcolumn];

      panel.setOpaque(true);
      // Check if flagged as important
      src_type = tableModel.getSrcType(row);
      if (src_type < 0) {
        src_type = -src_type;
      }

      if (mcolumn != nameColumn) {
        HashMap<Integer, Boolean> mark = tableModel.hotMarks.get(row);
        if (mark != null) {
          Boolean bvalue = mark.get(mcolumn);
          if (bvalue != null && bvalue) {
            panel.setBackground(AnVariable.HILIT_COLOR);
          }
        }
        mark = tableModel.hotMarks_inc.get(row);
        if (mark != null) {
          Boolean bvalue = mark.get(mcolumn);
          if (bvalue != null && bvalue) {
            panel.setBackground(AnVariable.HILIT_INC_COLOR);
          }
        }
      }

      // Just single string
      if (value instanceof String) {
        if (type != AnDisplay.DSP_DataLayout) {
          if ((src_type == AT_SRC) || (src_type == AT_SRC_ONLY)) {
            // get rendered src panel from SourceDisp
            SrcRenderer.SrcTextPane st = null;
            if (srcRenderer != null) {
              st = srcRenderer.getSrcTextPane((String) value, table, row);
            } else if (disRenderer != null) {
              st = disRenderer.getSrcTextPane((String) value, table, row);
            }
            if (st != null) {
              psize = new Dimension(col_w[1], rowHeight);
              st.setPreferredSize(new Dimension(psize));
              st.setMinimumSize(new Dimension(psize));
              st.setMaximumSize(new Dimension(psize));
              if (disRenderer != null) {
                panel.setBackground(AnVariable.SRCDIS_COLOR);
              }
              panel.add(st);
              return panel;
            }
          } else if ((src_type == AT_DIS) || (src_type == AT_DIS_ONLY)) {
            if (disRenderer != null) {
              DisRenderer.DisTextPane dt;
              dt = disRenderer.getDisTextPane((String) value, table, row);
              psize = new Dimension(col_w[1], rowHeight);
              dt.setPreferredSize(new Dimension(psize));
              dt.setMinimumSize(new Dimension(psize));
              dt.setMaximumSize(new Dimension(psize));
              panel.add(dt);
              return panel;
            }
          }
        }
        panel.add(fl = new FListLabel((String) value, table, JLabel.LEFT));
        if (((String) value).startsWith("<")) {
          fl.setFont(QuoteFont);
        }
        if ((((String) value).trim()).startsWith("(")) {
          fl.setFont(QuoteFont);
        }

        psize = new Dimension(col_w[1], rowHeight);
        fl.setPreferredSize(new Dimension(psize));
        fl.setMinimumSize(new Dimension(psize));
        fl.setMaximumSize(new Dimension(psize));
        if (type == AnDisplay.DSP_MiniCallee
            || type == AnDisplay.DSP_MiniCaller
            || type == AnDisplay.DSP_MiniCallerCalleeSelf) {
          panel.setToolTipText((String) value);
        }

        // Set text color
        if (src_type == AT_COM) {
          fl.setForeground(AnVariable.CCOMP_COLOR);
          fl.setBackground(AnVariable.COMENTARY_COLOR);
          panel.setBackground(AnVariable.COMENTARY_COLOR);
        } else if ((type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2)
            && ((src_type == AT_SRC) || (src_type == AT_SRC_ONLY))) {
          fl.setForeground(AnVariable.DISRC_COLOR);
        } else if ((type == AnDisplay.DSP_DataLayout)
            && ((src_type == AT_SRC) || (src_type == AT_SRC_ONLY))) {
          fl.setFont(QuoteFont);
        } else if ((src_type == AT_QUOTE) || (src_type == AT_FUNC)) {
          fl.setForeground(AnVariable.QUOTE_COLOR);
          fl.setFont(QuoteFont);
          panel.setBackground(AnVariable.COMENTARY_COLOR);
          if (src_type == AT_FUNC) {
            if (srcRenderer != null) {
              if (srcRenderer.functionCallerCalleeAdded.get(row) == null) {
                srcRenderer.addFunctionByRow(row);
              }
            } else if (disRenderer != null) {
              if (disRenderer.functionCallerCalleeAdded.get(row) == null) {
                disRenderer.addFunctionByRow(row);
              }
            }
          }
        }
        if (column < tableModel.columnCount - 1) {
          Color borderColor;
          if (AnWindow.getInstance().getExperimentGroups().length == 1) {
            borderColor = AnEnvironment.TABLE_VERTICAL_GRID_METRIC_COLOR;
          } else {
            borderColor = AnEnvironment.TABLE_VERTICAL_GRID_METRIC_COMP_COLOR;
          }
          panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
        }
        return panel;
      }

      // Check the source line type, return for empty metrics
      if ((src_type == AT_SRC_ONLY)
          || (src_type == AT_COM)
          || (src_type == AT_FUNC)
          || (src_type == AT_EMPTY)
          || (src_type == AT_DIS_ONLY)) {
        return panel;
      }

      // May contain multiple columns; value, time, and percent
      label = tableModel.getLabel(mcolumn);
      if (label == null) {
        return panel;
      }

      if (label.getClock() != -1.0) {
        panel.add(
            fl =
                new FListLabel(
                    ((AnObject) value).toFormTime(label.getClock()), table, JLabel.RIGHT));
        psize = new Dimension(col_w[1], rowHeight);
        fl.setPreferredSize(new Dimension(psize));
        fl.setMinimumSize(new Dimension(psize));
        fl.setMaximumSize(new Dimension(psize));
      }

      if (label.getUnit() != null) {
        panel.add(fl = new FListLabel(((AnObject) value).toFormString(), table, JLabel.RIGHT));
        psize = new Dimension(col_w[2], rowHeight);
        fl.setPreferredSize(new Dimension(psize));
        fl.setMinimumSize(new Dimension(psize));
        fl.setMaximumSize(new Dimension(psize));
      }

      if (label.getTotal() != -1.0) {
        panel.add(
            fl =
                new FListLabel(
                    ((AnObject) value).toPercent(label.getTotal()), table, JLabel.RIGHT));
        psize = new Dimension(col_w[3], rowHeight);
        fl.setPreferredSize(new Dimension(psize));
        fl.setMinimumSize(new Dimension(psize));
        fl.setMaximumSize(new Dimension(psize));
      }

      if (column < tableModel.columnCount - 1) {
        Color borderColor;
        if (AnWindow.getInstance().getExperimentGroups().length == 1) {
          if (tableModel
              .metricLabels[column]
              .getAnMetric()
              .getComd()
              .equals(tableModel.metricLabels[column + 1].getAnMetric().getComd())) {
            borderColor = AnEnvironment.TABLE_VERTICAL_GRID_COLOR;
          } else {
            borderColor = AnEnvironment.TABLE_VERTICAL_GRID_METRIC_COLOR;
          }
        } else {
          // Compare mode
          if (tableModel
              .metricLabels[column]
              .getAnMetric()
              .getComd()
              .equals(tableModel.metricLabels[column + 1].getAnMetric().getComd())) {
            if (tableModel.metricLabels[column].getAnMetric().getSubType()
                == tableModel.metricLabels[column + 1].getAnMetric().getSubType()) {
              borderColor = AnEnvironment.TABLE_VERTICAL_GRID_COLOR;
            } else {
              borderColor = AnEnvironment.TABLE_VERTICAL_GRID_ATTRIBUTE_COMP_COLOR;
            }
          } else {
            borderColor = AnEnvironment.TABLE_VERTICAL_GRID_METRIC_COMP_COLOR;
          }
        }
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
      }
      if (hasFocus) {
        // Override other border settings
        panel.setBorder(AnVariable.tableFocusBorder);
      }
      return panel;
    }
  }

  public void clearHistory() {
    if (navHistoryPool != null) {
      navHistoryPool.clearAll();
    }
  }

  // Remove all rows & columns
  @Override
  public void removeAll() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            tableModel.removeAll();
          }
        });
  }

  // Remove all rows
  public void removeAllRows() {
    tableModel.removeAllRows();
  }

  // Show message only
  public void showMessage(String msg) {
    showMessage(getErrorMessagePanel(msg));
  }

  public void showMessage(Component component) {
    allHeaderPanel = null;
    setViewportView(component);
  }

  private JPanel getErrorMessagePanel(String text) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    JLabel label = new JLabel(text);
    label.setHorizontalAlignment(JLabel.CENTER);
    label.getAccessibleContext().setAccessibleName(AnLocale.getString("Error message"));
    label.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Error message"));
    panel.add(label);
    return panel;
  }

  // Set table data
  public void setData(
      final MetricLabel[] label,
      final Object[][] data,
      final String[] names,
      final int[] src_type,
      final int org_row,
      final int name_col,
      final int sort_col) {
    this.names = names;
    this.orgRow = org_row;
    this.nameColumn = name_col;
    this.sortColumn = sort_col;

    switch (type) {
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_DisassemblyV2:
        if (disRenderer != null) {
          long sel_src =
              parent.window.getSelectedObject().getSelObj(AnDisplay.DSP_SourceSelectedObject, 0);
          long sel_instr =
              parent.window.getSelectedObject().getSelObj(AnDisplay.DSP_Disassembly, 0);
          int view_mode = parent.window.getSettings().getViewModeSetting().get().value();
          disRenderer.maxSrcCachedNumber =
              1; // XXXX don't cache disasm view data since there is no consistant row to content
          // map
          disRenderer.renderSrc(names, data, src_type, sel_src, view_mode);
          disRenderer.renderDis(data, src_type, sel_instr); // must be called after renderSrc
        }
        break;
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_SourceV2:
        if (srcRenderer != null) {
          long sel_src =
              parent.window.getSelectedObject().getSelObj(AnDisplay.DSP_SourceSelectedObject, 0);
          int view_mode = parent.window.getSettings().getViewModeSetting().get().value();
          srcRenderer.renderSrc(names, data, src_type, sel_src, view_mode);
        }
        break;
      default:
    }
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            tableModel.setData(label, data, src_type, null, null);
          }
        });
  }

  // Set table data
  public void setData(
      final MetricLabel[] metricLabels,
      final Object[][] data,
      final String[] names,
      final int[] src_type,
      final int org_row,
      final int name_col,
      final int sort_col,
      final int[][] marks,
      final int[][] marks_inc) {
    this.names = names;
    this.orgRow = org_row;
    this.nameColumn = name_col;
    this.sortColumn = sort_col;

    switch (type) {
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_DisassemblyV2:
        if (disRenderer != null) {
          long sel_src =
              parent.window.getSelectedObject().getSelObj(AnDisplay.DSP_SourceSelectedObject, 0);
          long sel_instr =
              parent.window.getSelectedObject().getSelObj(AnDisplay.DSP_Disassembly, 0);
          int view_mode = parent.window.getSettings().getViewModeSetting().get().value();
          disRenderer.maxSrcCachedNumber =
              1; // XXXX don't cache disasm view data since there is no consistant row to content
          // map
          disRenderer.renderSrc(names, data, src_type, sel_src, view_mode);
          disRenderer.renderDis(data, src_type, sel_instr); // must be called after renderSrc
        }
        break;
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_SourceV2:
        if (srcRenderer != null) {
          AnUtility.checkIPCOnWrongThread(false);
          long sel_src =
              parent.window.getSelectedObject().getSelObj(AnDisplay.DSP_SourceSelectedObject, 0);
          AnUtility.checkIPCOnWrongThread(true);
          int view_mode = parent.window.getSettings().getViewModeSetting().get().value();
          srcRenderer.renderSrc(names, data, src_type, sel_src, view_mode);
        }
        break;
      default:
    }
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            // Set row selection
            if (org_row > 0 && org_row < table.getRowCount()) {
              table.changeSelection(org_row, 0, false, false);
            }
            tableModel.setData(metricLabels, data, src_type, marks, marks_inc);
          }
        });
  }

  private Object[][] getTableData() {
    return tableModel.getTableData();
  }

  // Set table column size
  public void setColumn(final int row_height) {
    this.rowHeight = row_height;

    tableModel.setColumn();
  }

  // Set row visible; Place the actual update on the system event queue
  public void showSelectedRow() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            showSelectedRowNow();
          }
        });
  }

  /** Show selected row Note: use EventDispatchThread to call this method */
  public void showSelectedRowNow() {
    final int vis_row;
    final Rectangle cellRect, visRect;

    vis_row = canSort ? tableModel.getSortRow(orgRow) : orgRow;
    cellRect = table.getCellRect(vis_row, 0, false);
    visRect = table.getVisibleRect();
    cellRect.x = visRect.x;

    // if current view doesn't include selected row, scroll
    if (!visRect.contains(cellRect)) {
      // calculate middle based on selected row
      // being below or above current visible rows
      if (visRect.y < cellRect.y) {
        cellRect.y += visRect.height / 2;
      } else {
        cellRect.y -= visRect.height / 2;
      }
      table.scrollRectToVisible(cellRect);
    }

    // clear multiple selection
    if (table.getSelectedRowCount() > 1) {
      table.clearSelection();
    }

    // Set row selection
    if (table.getSelectedRow() != vis_row) {
      table.changeSelection(vis_row, 0, false, false);
    }
  }

  // Get keyboard focus
  @Override
  public void requestFocus() {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            table.requestFocus();
          }
        });
  }

  public void clearSelection() {
    table.clearSelection();
  }

  // Clear selected row
  public void clearSelectedRow() {
    table.clearSelection();

    // Should reset the rows after clearSelection, otherwise repeated
    // recompute request event will be sent.
    orgRow = -1;
  }

  // Get scrolling value
  public int getScroll() {
    return getHorizontalScrollBar().getValue();
  }

  // Set scrolling value; Place the actual update on the system event queue
  public void setScroll(final int scroll_pos) {
    SwingUtilities.invokeLater(
        new Runnable() {

          public void run() {
            getHorizontalScrollBar().setValue(scroll_pos);
          }
        });
  }

  // Move columns
  public void moveColumn(final int from, final int to) {
    table.moveColumn(from, to);
  }

  // Set column width
  public void setColumnWidth(final int column, final int width) {
    final TableColumn col = table.getColumnModel().getColumn(column);

    if (col.getPreferredWidth() != width) {
      col.setPreferredWidth(columnWidth[column][0] = width);
    }
  }

  // Return column width info
  public int[][] getColumnWidth() {
    return columnWidth;
  }

  // Return raw height
  public int getRowHeight() {
    return rowHeight;
  }

  // Get string width
  public int stringWidth(final String str) {
    if (fontMetrics == null) {
      if (table.getGraphics() == null) {
        return 0;
      }
      fontMetrics = table.getGraphics().getFontMetrics();
    }
    return fontMetrics.stringWidth(str);
  }

  public boolean canGoBack() {
    if (navHistoryPool == null) {
      return false;
    }
    SelObjInfo so = navHistoryPool.getHistory().getBack();
    boolean can_go_back = navHistoryPool.getHistory().canGoBack();
    boolean inError = anTable.getViewport().getView() != table;
    if (inError) {
      so = navHistoryPool.getHistory().getCurrent();
      can_go_back = (so != null);
    }
    return can_go_back;
  }

  public boolean canGoForward() {
    if (navHistoryPool == null) {
      return false;
    }
    return navHistoryPool.getHistory().canGoForward();
  }

  public void goBack() {
    String txt = STR_ACTION_BACK;
    SelObjInfo so = navHistoryPool.getHistory().getBack();
    boolean can_go_back = navHistoryPool.getHistory().canGoBack();
    boolean inError = anTable.getViewport().getView() != table;
    if (inError) {
      so = navHistoryPool.getHistory().getCurrent();
      can_go_back = (so != null);
    }
    if (so != null && can_go_back) {
      NavigationHistoryUpdateAction nhua =
          new NavigationHistoryUpdateAction(txt, so.id, so.lineno, so.name);
      nhua.actionPerformed(null);
    }
  }

  public void goForward() {
    String txt = STR_ACTION_FORWARD;
    SelObjInfo so = navHistoryPool.getHistory().getForward();
    if (so != null && navHistoryPool.getHistory().canGoForward()) {
      NavigationHistoryUpdateAction nhua =
          new NavigationHistoryUpdateAction(txt, so.id, so.lineno, so.name);
      nhua.actionPerformed(null);
    }
  }

  // Set current selected row and make it visible
  public void setSelectedRow(final int org_row) {
    this.orgRow = org_row;

    showSelectedRow();
    // record history
    if (navHistoryPool != null && navHistoryPool.getHistory().enabled) {
      navHistoryPool.getHistory().goToSelected(org_row, false);
    }
  }

  // Experimental code
  // Set current selected row and make it visible
  public void setSelectedRowNow(final int org_row) {
    this.orgRow = org_row;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          public void run() {
            showSelectedRowNow();
          }
        });
  }

  // Return current selected row
  public int getSelectedRow() {
    return orgRow;
  }

  // Return current selected rows
  public int[] getSelectedRows() {
    return selected_indices;
  }

  // Find the part after the current row
  public int findAfter(final String str, final boolean next, boolean caseSensitive) {
    final int row, nr, last_line;
    row = tableModel.getSortRow(orgRow);
    nr = tableModel.getRowCount();
    last_line = nr - 1;

    if (next) {
      if (row < last_line) {
        return tableModel.find(str, 1, row + 1, nr, caseSensitive);
      }
    } else {
      if (row > 0) {
        return tableModel.find(str, -1, row - 1, -1, caseSensitive);
      }
    }

    return -1;
  }

  // Find the part before the current row
  public int findBefore(final String str, final boolean next, boolean caseSensitive) {
    final int row, nr, last_line;

    row = tableModel.getSortRow(orgRow);

    if (row < 0) {
      return -1;
    }

    nr = tableModel.getRowCount();
    last_line = nr - 1;

    if (next) {
      return tableModel.find(str, 1, 0, row + 1, caseSensitive);
    } else if (row <= last_line) {
      return tableModel.find(str, -1, last_line, row - 1, caseSensitive);
    }

    return -1;
  }

  // Sort table data
  public void sort(final int col) {
    if (canSort && (col >= 0)) {
      tableModel.sort(table.convertColumnIndexToModel(col));
    }
  }

  // Listener to the event changed
  private final EventListenerList listenerList = new EventListenerList();

  // Add a listener to the list
  public void addAnListener(final AnListener listener) {
    listenerList.add(AnListener.class, listener);
  }

  // Never used
  //    // Remove a listener from the list
  //    public void removeAnListener(final AnListener listener) {
  //	listenerList.remove(AnListener.class, listener);
  //    }
  //
  // Fire AnEvent to the listener
  public void fireAnEvent(final AnEvent event) {
    //        System.out.println("AnTable fireAnEvent: " + event);
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            final Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2) {
              if (listeners[i] == AnListener.class) {
                ((AnListener) listeners[i + 1]).valueChanged(event);
              }
            }
          }
        });
  }

  // Events listener for row selecting, clicks on cell/row
  private final class CellHandler implements ListSelectionListener {

    public final void valueChanged(final ListSelectionEvent event) {
      final int first, last, new_row;
      final Rectangle cellRect, visRect;

      // for mini-c-c pane, only handle mouseclicked event
      if (type == AnDisplay.DSP_MiniCallee || type == AnDisplay.DSP_MiniCaller) {
        return;
      }

      if (event.getValueIsAdjusting()) {
        return;
      }

      // Which row is selected
      first = event.getFirstIndex();
      if (first == -1) {
        return;
      }

      if (table.getSelectedRowCount() == 1) { // Single selection
        selectedRow = table.getSelectedRow();
      } else {
        if (selectedRow == first) {
          last = event.getLastIndex(); // index of last row that may have changed

          // No change, happens when clicked on header (but now skipped for multi-sel)
          if ((selectedRow == last)
              && (type == AnDisplay.DSP_MiniCallee || type == AnDisplay.DSP_MiniCaller)) {
            return;
          }

          selectedRow = last;
        } else {
          selectedRow = first;
        }
      }

      // Update status with selected row(s) and total number of rows
      String selectedRows;
      if (table.getSelectedRowCount() == 1) { // Single selection
        selectedRows = "" + (table.getSelectedRow() + 1);
      } else {
        DefaultListSelectionModel dlsm = (DefaultListSelectionModel) event.getSource();
        selectedRows = (dlsm.getMinSelectionIndex() + 1) + "-" + (dlsm.getMaxSelectionIndex() + 1);
      }
      AnWindow.getInstance()
          .getTableStatusHandle()
          .setText(selectedRows + "/" + table.getRowCount());

      // Make it visible (setAutoscrolls has been turned off)
      cellRect = table.getCellRect(selectedRow, 0, false);
      visRect = table.getVisibleRect();

      cellRect.x = visRect.x;
      if (!visRect.contains(cellRect)) {
        table.scrollRectToVisible(cellRect);
      }

      // Need to update summary display
      new_row = tableModel.getRow(selectedRow);

      // Get all selected indices
      ListSelectionModel listmodel = table.getSelectionModel();
      if (listmodel.isSelectionEmpty() == false) {
        selected_indices = table.getSelectedRows();
      }

      // Fire valueChanged event
      if (orgRow != new_row) {
        orgRow = new_row;
        // record history
        if (!(selectedRow != 0 && orgRow == 0)) {
          fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, orgRow, null));
          if (navHistoryPool != null && navHistoryPool.getHistory().enabled) {
            navHistoryPool.getHistory().goToSelected(orgRow, true);
          }
        }
      } else {
        if ((type == AnDisplay.DSP_Lines) || (type == AnDisplay.DSP_PCs)) {
          fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, orgRow, null));
        }
      }
    }
  }

  // -------------- Properties ---------------- //
  /** Implements menu item: Properties */
  class SettingsAction extends AbstractAction {

    int index = 0;

    public SettingsAction(String what, int id) {
      super(what, AnUtility.gear_icon);
      index = id;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      parent.window.getSettings().showDialog(index);
    }
  }

  // Events listener for selecting on already selected row for CallerCalleesDisp
  private final class CallHandler extends MouseAdapter implements ActionListener {

    @Override
    public void mouseClicked(final MouseEvent event) {
      if (event.getClickCount() == 2) {
        updateSelectedRow(table.rowAtPoint(event.getPoint()));
      } else {
        if (event.getButton() == MouseEvent.BUTTON3
            && (type == AnDisplay.DSP_MiniCaller || type == AnDisplay.DSP_MiniCallee)) {
          return;
        }
        setSelectedRow(table.rowAtPoint(event.getPoint()));
      }
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      final String cmd = event.getActionCommand();

      if (cmd.equals("SPACE")) {
        setSelectedRow(table.getSelectedRow());
      } else if (cmd.equals("ENTER")) {
        int table_row = table.getSelectedRow();
        if (table_row == -1) {
          return;
        }
        orgRow = tableModel.getRow(table_row);
        if (orgRow == -1) {
          return;
        }
        performDefaultAction();
      } else if (cmd.equals("CNTRL_C")) {
        int printLimit = 0;
        String text = anTable.printSelectedTableContents(null, printLimit);
        AnUtility.copyToClipboard(text);
      }
    }

    public void setSelectedRow(final int table_row) {
      // CellHandler will handle this kind of event
      if (table_row == -1) {
        return;
      }

      orgRow = tableModel.getRow(table_row);
      if (orgRow < 0) {
        System.out.println("AnTable.CallHandler.setSelectedRow() org_row=" + orgRow);
        return;
      }
      fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, orgRow, null));
    }
  }

  private void updateSelectedRow(final int table_row) {
    // CellHandler will handle this kind of event
    if (table_row == -1) {
      return;
    }

    orgRow = tableModel.getRow(table_row);
    if (orgRow < 0) {
      System.out.println("AnTable.CallHandler.updateSelectedRow() org_row=" + orgRow);
      return;
    }
    fireAnEvent(new AnEvent(anTable, AnEvent.EVT_COMPUTE, orgRow, null));
  }

  private final class TableAdapter extends MouseAdapter {

    @Override
    public void mouseClicked(final MouseEvent event) {
      final int table_row;
      // int src_type = model.getSrcType(table.getSelectedRow()); // never used

      if (event.getClickCount() == 2) {
        table_row = table.getSelectedRow();
        if ((orgRow == -1) || (table_row == -1)) {
          return;
        }
        orgRow = tableModel.getRow(table_row);

        // Perform default action
        performDefaultAction();

        //                fireAnEvent(new AnEvent(anTable, AnEvent.EVT_NEWSRC,
        //                        orgRow, null));
      } else { // Single click is already processed in CellHandler.valueChanged() when selection
        // changed
        //    fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT,
        //            orgRow, null));
        switch (type) {
          case AnDisplay.DSP_Disassembly:
          case AnDisplay.DSP_DisassemblyV2:
            if (event.getClickCount() == 1
                && event.getButton() == event.BUTTON1
                && disRenderer != null) {
              int row =
                  table.getSelectedRow(); // XXXX since source/disasm views are unsortable, we just
              // assume no need for model.getRow()
              disRenderer.updateSelectedRegs(row);
              boolean isJump = disRenderer.goToAddr(row);
              if (!isJump) {
                disRenderer.goToCallee(row, event);
              }
              disRenderer.goToSrcLine(row);
              disRenderer.caretLinePosition = -1; // reset caret position
            }
          case AnDisplay.DSP_Source:
          case AnDisplay.DSP_SourceV2:
            if (event.getClickCount() == 1
                && event.getButton() == event.BUTTON1
                && srcRenderer != null) {
              int row =
                  table.getSelectedRow(); // XXXX since source/disasm views are unsortable, we just
              // assume no need for model.getRow()
              srcRenderer.goToCallee(row, event);
              srcRenderer.caretLinePosition = -1; // reset caret position
            }
        }
      }
    }
  }

  // Events listener for scrolling
  private final class ScrollHandler implements AdjustmentListener {

    @Override
    public void adjustmentValueChanged(final AdjustmentEvent event) {
      fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SCROLL, event.getValue(), null));
    }
  }

  // Events listener for column switching
  private final class OldColumnHandler implements TableColumnModelListener {

    @Override
    public void columnMoved(final TableColumnModelEvent event) {
      final int from, to;

      from = event.getFromIndex();
      to = event.getToIndex();

      if (from == to) {
        return;
      }

      fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SWITCH, from, new Integer(to)));
    }

    @Override
    public void columnMarginChanged(final ChangeEvent event) {
      final TableColumn resizingColumn;
      final TableColumnModel cmodel;
      final int nc, width;
      int columnIndex;

      if (tableHeader == null) {
        return;
      }

      if ((resizingColumn = tableHeader.getResizingColumn()) == null) {
        return;
      }

      cmodel = table.getColumnModel();
      nc = cmodel.getColumnCount();
      for (columnIndex = 0; columnIndex < nc; columnIndex++) {
        if (cmodel.getColumn(columnIndex) == resizingColumn) {
          tableModel.fitColumnResize(columnIndex);

          if (type == AnDisplay.DSP_Callers) {
            width = resizingColumn.getWidth();
            fireAnEvent(new AnEvent(anTable, AnEvent.EVT_RESIZE, columnIndex, new Integer(width)));
          }

          break;
        }
      }
    }

    @Override
    public void columnAdded(final TableColumnModelEvent event) {}

    @Override
    public void columnRemoved(final TableColumnModelEvent event) {}

    @Override
    public void columnSelectionChanged(final ListSelectionEvent event) {}
  }

  // Table Model for Function List
  public final class FListTableModel extends AbstractTableModel { // public for memobj/indexobj

    public MetricLabel[] metricLabels;
    private Object[][] data;
    private int[] src_type;
    private Row[] rows;
    private int rowCount;
    private int columnCount;
    public HashMap<Integer, HashMap<Integer, Boolean>> hotMarks;
    public HashMap<Integer, HashMap<Integer, Boolean>> hotMarks_inc;

    // Constructor
    public FListTableModel() {
      initData();
    }

    public void updateTableHeaders() {
      boolean wrapMetricNames = AnTable.this.wrapMetricNames;
      if (AnTable.this.wrapMetricNames) {
        wrapMetricNames = AnWindow.getInstance().getSettings().getTableSettings().wrapMetricNames();
      }
      allHeaderPanel = new AnTableHeaderPanel(AnTable.this, metricLabels, wrapMetricNames);
      setColumnHeaderView(allHeaderPanel);
      if (allHeaderPanel.getPreferredSize().height > 1 && hotGapPanel != null) {
        hotGapPanel.headerheightChanged(allHeaderPanel.getPreferredSize().height);
      }
    }

    public boolean isCellEditable(int row, int col) {
      int st = tableModel.getSrcType(row);
      if (st < 0) {
        st = -st;
      }

      // Just single string
      if (this.data[col][row] instanceof String) {
        if ((type != AnDisplay.DSP_DataLayout)
            && ((st == AT_SRC) || (st == AT_SRC_ONLY) || (st == AT_DIS) || (st == AT_DIS_ONLY))) {
          return (col == nameColumn);
        }
      }
      return false;
    }

    // Initialize data
    private void initData() {
      metricLabels = new MetricLabel[0];
      data = new Object[1][0];
      src_type = new int[0];
      rows = new Row[0];

      columnCount = 0;
      rowCount = 0;

      hotMarks = new HashMap<Integer, HashMap<Integer, Boolean>>();
      hotMarks_inc = new HashMap<Integer, HashMap<Integer, Boolean>>();
    }

    // Remove all rows & columns
    public final void removeAll() {
      initData();
      fireTableChanged(null);
    }

    // Remove all rows
    public final void removeAllRows() {
      rowCount = 0;
      fireTableChanged(null);
    }

    // Set table data
    public final void setData(
        MetricLabel[] metricLabels,
        Object[][] data,
        int[] src_type,
        int[][] marks,
        int[][] marks_inc) {
      this.data = data;
      this.src_type = src_type;

      // Get numbers of rows/columns
      columnCount = metricLabels.length;
      // FIXUP: sometimes label == null if you quickly select/deselect metrics in
      // overview
      rowCount = data[0].length;

      // Initialize the rows elements
      rows = new Row[rowCount];
      for (int i = 0; i < rowCount; i++) {
        rows[i] = new Row(i);
      }

      setLabel(metricLabels);
      if (marks != null && marks_inc != null) {
        for (int i = 0; i < marks[0].length; i++) {
          HashMap<Integer, Boolean> colMarks = hotMarks.get(marks[0][i]);
          if (colMarks == null) {
            colMarks = new HashMap<Integer, Boolean>();
            hotMarks.put(marks[0][i], colMarks);
          }
          colMarks.put(marks[1][i], true);
        }
        for (int i = 0; i < marks_inc[0].length; i++) {
          HashMap<Integer, Boolean> colMarks = hotMarks_inc.get(marks_inc[0][i]);
          if (colMarks == null) {
            colMarks = new HashMap<Integer, Boolean>();
            hotMarks_inc.put(marks_inc[0][i], colMarks);
          }
          colMarks.put(marks_inc[1][i], true);
        }
      }
      // New headers
      if (hasHeader) {
        updateTableHeaders();
      }
    }

    private Object[][] getTableData() { // public for mem/idx objs
      return data;
    }

    // Returns line contents for selected row
    public String getSrcLine(final int row) {
      return (row != -1) ? (String) getValueAt(row, nameColumn) : null;
    }

    // Set table label only
    private void setLabel(final MetricLabel[] label) {
      this.metricLabels = label;
      // Use "AWT-EventQueue-0" thread to avoid race conditions
      AnUtility.dispatchOnSwingThread(
          new Runnable() {
            public void run() {
              if (!columnsSaved) {
                columnWidth = new int[columnCount][4];
              }

              if (columnCount != 0) {
                initWidth();
              }

              // Update later for caller-callee display; need the maximum width
              if (hasSelect) {
                setColumn();
              }
            }
          });
    }

    // Set table columns for caller-callee display
    public final void setColumn() {
      if (columnCount != 0) {
        fitColumn();
        table.setRowHeight(rowHeight);
      }

      fireTableChanged(null);
    }

    // Sizes the table columns to fit the available space.
    private void fitColumn() {
      int total_width, i;

      if (isSingle || (nameColumn < 0)) {
        return;
      }

      total_width = getViewportBorderBounds().width;

      for (i = 0; i < columnCount; i++) {
        if (i != nameColumn) {
          total_width -= columnWidth[i][0];
        }
      }

      nameWidth = columnWidth[nameColumn][0];
      if (columnWidth[nameColumn][0] < total_width) {
        columnWidth[nameColumn][0] = total_width;
      }
    }

    // Sizes the table columns to fit the available space.
    public final void fitColumnResize(final int index) {
      int ext_width;
      final TableColumnModel cm;
      final TableColumn column;

      if (isSingle || (index == nameColumn) || (nameColumn < 0)) {
        return;
      }

      ext_width = getViewportBorderBounds().width;
      cm = table.getColumnModel();

      // Compute the extendable width of the name column
      for (int i = 0; i < columnCount; i++) {
        if (i != nameColumn) {
          ext_width -= cm.getColumn(i).getWidth();
        }
      }

      // Extend the name column
      if (nameWidth < ext_width) {
        column = cm.getColumn(nameColumn);
        column.setPreferredWidth(ext_width);
        column.setWidth(ext_width);

        if (type == AnDisplay.DSP_Callers) {
          fireAnEvent(new AnEvent(anTable, AnEvent.EVT_RESIZE, nameColumn, new Integer(ext_width)));
        }
      }

      // if the user has manually adjusted the columns, note it & save the size
      if (index >= 0) {
        columnsSaved = true;
        final TableColumn col = cm.getColumn(index);
        columnWidth[index][0] = col.getWidth();
      }
    }

    // Find the maximum column width for each field
    private void initWidth() {
      CellPanel cpanel = new CellPanel(table, true, FlowLayout.CENTER);
      cpanel.add(new FListLabel(AnVariable.all_chars, table, JLabel.LEFT));
      rowHeight = cpanel.getPreferredSize().height;

      for (int i = 0; i < columnCount; i++) {
        int totalColumnWidth = 0;
        CellPanel cellPanel = new CellPanel(table, true, FlowLayout.CENTER);
        MetricLabel lb = metricLabels[i];
        if (data[i] instanceof String[]) {
          FListLabel fListLabel =
              new FListLabel(lb.getMaxAnObject().toString(), table, JLabel.LEFT);
          int cellwidth = fListLabel.getPreferredSize().width + 15;
          totalColumnWidth += cellwidth;
          cellPanel.add(fListLabel);
          columnWidth[i][1] = cellwidth;
        } else {
          if (lb.getClock() != -1.0) {
            FListLabel fListLabel =
                new FListLabel(lb.getMaxAnObject().toFormTime(lb.getClock()), table, JLabel.RIGHT);
            int cellwidth = fListLabel.getPreferredSize().width + 15;
            totalColumnWidth += cellwidth;
            columnWidth[i][1] = cellwidth;
          }
          if (lb.getUnit() != null) {
            FListLabel fListLabel =
                new FListLabel(lb.getMaxAnObject().toFormString(), table, JLabel.RIGHT);
            int cellwidth = fListLabel.getPreferredSize().width + 15;
            totalColumnWidth += cellwidth;
            columnWidth[i][2] = cellwidth;
          }
          if (lb.getTotal() != -1.0) {
            FListLabel fListLabel =
                new FListLabel(lb.getMaxAnObject().toPercent(lb.getTotal()), table, JLabel.RIGHT);
            int cellwidth = fListLabel.getPreferredSize().width + 15;
            totalColumnWidth += cellwidth;
            columnWidth[i][3] = cellwidth;
          }
        }

        columnWidth[i][0] = totalColumnWidth + 30;
        // Saves white space
        int minCellWidth;
        if (AnWindow.getInstance().getExperimentGroups().length == 1) {
          minCellWidth = getMinimumColumnWidth();
        } else {
          minCellWidth = getMinimumColumnWidth() / 2;
        }
        if (columnWidth[i][0] < minCellWidth) {
          columnWidth[i][0] = minCellWidth;
        }
      }
    }

    @Override
    public final int getRowCount() {
      return rowCount;
    }

    @Override
    public final int getColumnCount() {
      return columnCount;
    }

    public final int getSrcType(final int r) {
      int res = AT_LIST;
      try {
        if ((src_type != null) && (r < rowCount)) {
          res = src_type[r];
        }
      } catch (java.lang.ArrayIndexOutOfBoundsException e) {
        // CR 6999912
      }
      return (res);
    }

    public final MetricLabel getLabel(final int c) {
      if (metricLabels != null) {
        if (metricLabels.length > c) {
          if (c >= 0) {
            return metricLabels[c];
          }
        }
      }
      return null;
    }

    public final String getColumnName(final int c) {
      return ((c >= 0) && (c < columnCount))
          ? metricLabels[c].getTitleLines()[0]
              + metricLabels[c].getTitleLines()[1]
              + metricLabels[c].getIcon()
          : "";
    }

    // Needed for memobj/indexobj display
    public int getNameCol() {
      return nameColumn;
    }

    public final Object getValueAt(final int r, final int c) {
      int idx = 0;
      try {
        if (r < rowCount) {
          if (rows[r] == null) {
            return null;
          }
          idx = rows[r].index;
        }
        if ((c >= 0) && (c < columnCount) && (data[c] != null) && (idx < data[c].length)) {
          return data[c][idx];
        }
      } catch (java.lang.ArrayIndexOutOfBoundsException e) {
        // CR 6999912 (desynchronization)
        return null;
      }
      return null;
    }

    // Find
    private final int find(
        String str, final int incr, int start, final int end, boolean caseSensitive) {
      String text;

      if (str == null) { // Search for High-Metric item
        while (start != end) {
          if (getSrcType(start) < 0) {
            return start;
          }

          start += incr;
        }
      } else if (nameColumn >= 0) { // Search string
        if (!caseSensitive) {
          str = str.toLowerCase();
        }
        while ((start != -1) && (start != end)) {
          text = (String) getValueAt(start, nameColumn);
          if (text == null) { // see 23193063
            return -1;
          }
          if (!caseSensitive) {
            text = text.toLowerCase();
          }
          if ((text != null) && (text.indexOf(str) != -1)) {
            return getRow(start);
          }

          start += incr;
        }
      }

      return -1;
    }

    // Get the un-sorted row index
    public final int getRow(final int row) {
      if (row >= rows.length) {
        return 0;
      }
      return (rowCount == 0) ? row : rows[row].index;
    }

    // Get the sorted row index
    public final int getSortRow(final int row) {
      try {
        // For most case, non-sorted, it's just the original row index
        if ((rowCount == 0) || (row == -1) || (rows[row].index == row)) {
          return row;
        }

        for (int i = 0; i < rowCount; i++) {
          if (rows[i].index == row) {
            return i;
          }
        }
      } catch (java.lang.ArrayIndexOutOfBoundsException e) {
        // CR 6999912 (desynchronization)
        return row;
      }

      return row;
    }

    // Sort the table data
    public void sort(final int col) {
      // Sort by column sortColumn
      sortColumn = col;

      // if (type != AnDisplay.DSP_DLAYOUT)
      //  Arrays.sort(rows);
      fireTableDataChanged();

      // Repaint header, header's sort indicator changed
      if (hasHeader) {
        tableHeader.repaint();
      }

      // Restore the selected row, should be after updating table data
      if (hasSelect) {
        showSelectedRow();
      }
    }

    // Object which stores the un-sorted row index
    private final class Row implements Comparable {

      public final int index;

      public Row(final int index) {
        this.index = index;
      }

      public final int compareTo(final Object other) {
        final int col = (sortColumn >= 0) ? sortColumn : -sortColumn - 1;
        final int index_o = ((Row) other).index;

        final Object a = data[col][index];
        final Object b = data[col][index_o];
        return index - index_o;
      }
    }
  }

  // Table Column Model for Function List
  private final class FListColumnModel extends DefaultTableColumnModel {
    // Set column width here

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
      if (columnIndex == newIndex) {
        return;
      }
      if (columnIndex
          < 0) { // HACK: why is columnIndex < 0? Sometimes it is if you quickly move a column.
        return;
      }
      super.moveColumn(
          columnIndex, newIndex); // To change body of generated methods, choose Tools | Templates.
    }

    public void addColumn(final TableColumn tc) {
      final int width;

      width = columnWidth[tc.getModelIndex()][0];
      tc.setPreferredWidth(width);
      tc.setWidth(width);
      super.addColumn(tc);
    }

    // Workaround for (columnIndex>=tableColumns.size()), Swing sync problem
    public TableColumn getColumn(int columnIndex) {
      final int size = tableColumns.size();
      TableColumn tc;

      if ((columnIndex < 0) || (columnIndex >= size)) {
        if (size > 0) {
          columnIndex = 0;
        } else {
          return new TableColumn();
        }
      }
      if (size <= 0) {
        return new TableColumn();
      }
      try {
        tc = (TableColumn) tableColumns.elementAt(columnIndex);
      } catch (java.lang.ArrayIndexOutOfBoundsException e) {
        tc = new TableColumn();
      }
      return tc;
    }
  }

  public void sortTable(int colNum) {
    if (colNum >= tableModel.getColumnCount()) {
      return;
    }
    final int model_col = table.convertColumnIndexToModel(colNum);
    tableModel.sort(model_col);
    AnEvent ev = new AnEvent(anTable, AnEvent.EVT_SORT, model_col, type, new Integer(colNum));
    fireAnEvent(ev);
    table.requestFocus();
  }

  /*
   * Update tables in Callers-Callees tab
   * Possible actions:
   * Prepend - insert selected caller at the beginning of the stack fragment
   * Append - add selected callee to the end of the stack fragment
   * Recenter - replace the stack fragment with the selected function
   * Remove - remove selected function from the stack fragment
   * Reset - replace the stack fragment with the Selected Object
   * Set Head - remove all functions above the selected function from the stack fragment
   * Set Tail - remove all functions below the selected function from the stack fragment
   * Set Filter: Name in Stack - set the selected function name as filter
   * Set Filter: Call Path in Stack - set the call path to the selected function as filter
   * Set Filter: Function in Stack - set the selected function as filter
   * Set Filter: Function is Leaf - set the selected function as filter
   */
  protected void updateAnTable(String actionName) {
    updateTable(null, null, actionName);
    if (!(parent instanceof IOView) && !(parent instanceof HeapView)) {
      parent.updateToolBar();
      parent.syncSrcDisWin();
    }
  }

  private void updateTable(String shortName, String longName, String actionName) {
    if ((parent != null)
        && ((parent instanceof CallerCalleesView) || (parent instanceof FuncListDisp))) {
      if (actionName.equals(STR_ACTION_REMOVE_ALL_FILTERS)) {
        parent.window.getFilters().addClause(FilterClause.getNoFiltersClause(), false);
        return;
      }
      if (actionName.equals(STR_ACTION_UNDOFILTER)) {
        parent.window.getFilters().undoLastFilteraction();
        return;
      }
      if (actionName.equals(STR_ACTION_REDOFILTER)) {
        parent.window.getFilters().redoLastFilteraction();
        return;
      }
      if (actionName.equals(STR_ACTION_SHOW_SELECTED_FUNCTION)) {
        if (orgRow >= 0) {
          fireAnEvent(
              new AnEvent(
                  anTable,
                  AnEvent.EVT_COMPUTE, // AnEvent.EVT_UPDATE,
                  orgRow,
                  null));
        }
        return;
      }
      if (actionName.equals(STR_ACTION_SHOW_SOURCE)) {
        parent.window.setSelectedView(AnDisplay.DSP_Source);
        return;
      }
      if (actionName.equals(STR_ACTION_SHOW_CALLEE_SOURCE)
          || actionName.equals(STR_ACTION_SHOW_CALLER_SOURCE)) {
        if (parent.parent_type == AnDisplay.DSP_SourceDisassembly
            || parent.parent_type == AnDisplay.DSP_DualSource) {
          parent.window.setSelectedView(parent.parent_type);
        } else {
          parent.window.setSelectedView(AnDisplay.DSP_Source);
        }
        return;
      }
      if (actionName.equals(STR_ACTION_SHOW_CALLEE_DISASM)
          || actionName.equals(STR_ACTION_SHOW_CALLER_DISASM)) {
        if (parent.parent_type == AnDisplay.DSP_SourceDisassembly) {
          parent.window.setSelectedView(parent.parent_type);
        } else {
          parent.window.setSelectedView(AnDisplay.DSP_Disassembly);
        }
        return;
      }
      if (actionName.equals(STR_ACTION_SHOW_DISASM)) {
        parent.window.setSelectedView(AnDisplay.DSP_Disassembly);
        return;
      }
      if (actionName.equals(STR_ACTION_CUSTOM_FILTER)) {
        parent.window.getFilters().showCustomFilterDialog();
        return;
      }

      boolean setFilter = false;
      long ID = parent.window.getSelectedObject().getSelObjV2("FUNCTION");
      long[] cstack = null;
      String clause = "(" + ID + ")";
      String stack = parent.window.getSettings().getViewModeSetting().getStackName();
      if (parent instanceof CallerCalleesView) {
        cstack = ((CallerCalleesView) parent).getStack();
      }
      if (actionName.equals(FILTER_SIMILARLY_NAMED_ACTION_NAME)) {
        setFilter = true;
        clause = parent.window.getObjNameV2(ID);
        clause = getFunctionName(clause);
        clause = "(FNAME(\".*" + clause + ".*\") SOME IN " + stack + ")";
      } else if (actionName.equals(FILTER_WITH_SELECTED_JGROUP_ACTION_NAME)) {
        setFilter = true;
        int namecol = getTableModel().getNameCol();
        for (int selrow : anTable.getSelectedRows()) {
          String name = (String) getTableModel().data[namecol][selrow];
          if (name.contains("JThread")) {
            int indx0 = name.indexOf("Group");
            int indx1 = name.indexOf("'", indx0);
            int indx2 = name.indexOf(",", indx1 + 1);
            if (indx2 == -1) {
              indx2 = name.lastIndexOf("'");
            } else {
              indx2--;
            }
            clause = name.substring(indx1 + 1, indx2);
            break;
          }
        }
        clause = "(JGROUP(\"" + clause + "\") SOME IN JTHREAD)";
      } else if (actionName.equals(FILTER_WITH_SELECTED_JPARENT_ACTION_NAME)) {
        setFilter = true;
        int namecol = getTableModel().getNameCol();
        for (int selrow : anTable.getSelectedRows()) {
          String name = (String) getTableModel().data[namecol][selrow];
          if (name.contains("JThread")) {
            int indx0 = name.indexOf("Parent");
            int indx1 = name.indexOf("'", indx0);
            int indx2 = name.indexOf(",", indx1 + 1);
            if (indx2 == -1) {
              indx2 = name.lastIndexOf("'");
            } else {
              indx2--;
            }
            clause = name.substring(indx1 + 1, indx2);
            break;
          }
        }
        clause = "(JPARENT(\"" + clause + "\") SOME IN JTHREAD)";
      } else if (actionName.equals(FILTER_SELECTED_FUNCTION_ACTION_NAME)
          || actionName.equals(FILTER_SELECTED_FUNCTIONS_ACTION_NAME)) {
        setFilter = true;
        if ((type == AnDisplay.DSP_Callers)
            || (type == AnDisplay.DSP_CallerCalleeSelf)
            || (type == AnDisplay.DSP_Callees)) {
          // Special case for Callers-Callees view (CR 25631707)
          if (selected_indices.length > 1) {
            clause = "(" + clause + " SOME IN " + stack + ")";
          } else {
            clause = "(" + clause + " IN " + stack + ")";
          }
        } else {
          clause = getFilterForSelectedObjects(stack);
        }
      } else if (actionName.equals(FILTER_SELECTED_LINE_ACTION_NAME)
          || actionName.equals(FILTER_SELECTED_LINES_ACTION_NAME)) {
        setFilter = true;
        clause = getFilterForSelectedObjects(stack + "L");
      } else if (actionName.equals(FILTER_SELECTED_PC_ACTION_NAME)
          || actionName.equals(FILTER_SELECTED_PCS_ACTION_NAME)) {
        setFilter = true;
        clause = getFilterForSelectedObjects(stack + "I");
      } else if (actionName.equals(FILTER_NOT_SELECTED_FUNCTION_ACTION_NAME)
          || actionName.equals(FILTER_NOT_SELECTED_FUNCTIONS_ACTION_NAME)) {
        setFilter = true;
        clause = String.format("(!%s)", getFilterForSelectedObjects(stack));
      } else if (actionName.equals(FILTER_NOT_SELECTED_LINE_ACTION_NAME)
          || actionName.equals(FILTER_NOT_SELECTED_LINES_ACTION_NAME)) {
        setFilter = true;
        clause = String.format("(!%s)", getFilterForSelectedObjects(stack + "L"));
      } else if (actionName.equals(FILTER_NOT_SELECTED_PC_ACTION_NAME)
          || actionName.equals(FILTER_NOT_SELECTED_PCS_ACTION_NAME)) {
        setFilter = true;
        clause = String.format("(!%s)", getFilterForSelectedObjects(stack + "I"));
      } else if (actionName.equals(FILTER_LEAF_FUNCTION_ACTION_NAME)
          || actionName.equals(FILTER_LEAF_FUNCTIONS_ACTION_NAME)) {
        setFilter = true;
        if ((type == AnDisplay.DSP_Callers)
            || (type == AnDisplay.DSP_CallerCalleeSelf)
            || (type == AnDisplay.DSP_Callees)) {
          // Special case for Callers-Callees view (CR 25644886)
          clause = "((" + stack + "+0) IN " + clause + ")";
        } else {
          clause = String.format("((%s+0) IN %s)", stack, get_list_of_selected_objects());
        }
      } else if (actionName.equals(FILTER_CALLSTACK_FRAGMENT_ACTION_NAME)) {
        setFilter = true;
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
      } else if (actionName.equals(FILTER_NOT_CALLSTACK_FRAGMENT_ACTION_NAME)) {
        setFilter = true;
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
      } else if (actionName.equals(FILTER_WITH_SELECTED_FILE_ACTION_NAME)) {
        long ids[] =
            AnWindow.getInstance().getIOView().getSelectedIds(selected_indices, AnDisplay.DSP_IO);
        if (ids != null && ids.length > 0) {
          long[] vfds = parent.window.getSelectedObject().getSelObjsIO(ids, AnDisplay.DSP_IO);
          if (vfds != null && vfds.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("(IOVFD IN (");
            for (int i = 0; i < vfds.length; i++) {
              strBuf.append(vfds[i]);
              if (i + 1 < vfds.length) {
                strBuf.append(",");
              }
            }
            strBuf.append("))");
            clause = strBuf.toString();
            setFilter = true;
          }
        }
      } else if (actionName.equals(FILTER_NOT_SELECTED_FILE_ACTION_NAME)) {
        long ids[] =
            AnWindow.getInstance().getIOView().getSelectedIds(selected_indices, AnDisplay.DSP_IO);
        if (ids != null && ids.length > 0) {
          long[] vfds = parent.window.getSelectedObject().getSelObjsIO(ids, AnDisplay.DSP_IO);
          if (vfds != null && vfds.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("!(IOVFD IN (");
            for (int i = 0; i < vfds.length; i++) {
              strBuf.append(vfds[i]);
              if (i + 1 < vfds.length) {
                strBuf.append(",");
              }
            }
            strBuf.append("))");
            clause = strBuf.toString();
            setFilter = true;
          }
        }
      } else if (actionName.equals(FILTER_WITH_SELECTED_IOVFD_ACTION_NAME)) {
        long ids[] =
            AnWindow.getInstance()
                .getIOView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_IOFileDescriptors);
        if (ids != null && ids.length > 0) {
          long[] vfds =
              parent.window.getSelectedObject().getSelObjsIO(ids, AnDisplay.DSP_IOFileDescriptors);
          if (vfds != null && vfds.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("(IOVFD IN (");
            for (int i = 0; i < vfds.length; i++) {
              strBuf.append(vfds[i]);
              if (i + 1 < vfds.length) {
                strBuf.append(",");
              }
            }
            strBuf.append("))");
            clause = strBuf.toString();
            setFilter = true;
          }
        }
      } else if (actionName.equals(FILTER_NOT_SELECTED_IOVFD_ACTION_NAME)) {
        long ids[] =
            AnWindow.getInstance()
                .getIOView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_IOFileDescriptors);
        if (ids != null && ids.length > 0) {
          long[] vfds =
              parent.window.getSelectedObject().getSelObjsIO(ids, AnDisplay.DSP_IOFileDescriptors);
          if (vfds != null && vfds.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("!(IOVFD IN (");
            for (int i = 0; i < vfds.length; i++) {
              strBuf.append(vfds[i]);
              if (i + 1 < vfds.length) {
                strBuf.append(",");
              }
            }
            strBuf.append("))");
            clause = strBuf.toString();
            setFilter = true;
          }
        }
      } else if (actionName.equals(FILTER_WITH_SELECTED_IOSTACK_ACTION_NAME)) {
        long stackIds[] =
            AnWindow.getInstance()
                .getIOView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_IOCallStacks);
        if (stackIds != null && stackIds.length > 0) {
          StringBuffer strBuf1 = new StringBuffer();
          strBuf1.append("(");
          for (int j = 0; j < stackIds.length; j++) {
            StringBuffer strBuf2 = new StringBuffer();
            strBuf2.append("((");
            strBuf2.append(stackIds[j]);
            strBuf2.append(")");
            strBuf2.append(" == STACKID )");
            setFilter = true;

            if (strBuf2.length() > 0) {
              strBuf1.append(strBuf2.toString());
            }

            if ((j + 1 < stackIds.length) && (strBuf2.length() > 0)) {
              strBuf1.append(" || ");
            }
          }
          strBuf1.append(")");
          clause = strBuf1.toString();
        }
      } else if (actionName.equals(FILTER_NOT_SELECTED_IOSTACK_ACTION_NAME)) {
        long stackIds[] =
            AnWindow.getInstance()
                .getIOView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_IOCallStacks);
        if (stackIds != null && stackIds.length > 0) {
          StringBuffer strBuf1 = new StringBuffer();
          strBuf1.append("(!(");
          for (int j = 0; j < stackIds.length; j++) {
            StringBuffer strBuf2 = new StringBuffer();
            strBuf2.append("((");
            strBuf2.append(stackIds[j]);
            strBuf2.append(")");
            strBuf2.append(" == STACKID )");
            setFilter = true;

            if (strBuf2.length() > 0) {
              strBuf1.append(strBuf2.toString());
            }

            if ((j + 1 < stackIds.length) && (strBuf2.length() > 0)) {
              strBuf1.append(" || ");
            }
          }
          strBuf1.append("))");
          clause = strBuf1.toString();
        }
      } else if (actionName.equals(FILTER_WITH_SELECTED_HEAPSTACK_ACTION_NAME)) {
        long stackIds[] =
            AnWindow.getInstance()
                .getHeapView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_Heap);
        if (stackIds != null && stackIds.length > 0) {
          StringBuffer strBuf1 = new StringBuffer();
          strBuf1.append("(");
          for (int j = 0; j < stackIds.length; j++) {
            StringBuffer strBuf2 = new StringBuffer();
            strBuf2.append("((");
            strBuf2.append(stackIds[j]);
            strBuf2.append(")");
            strBuf2.append(" == STACKID )");
            setFilter = true;

            if (strBuf2.length() > 0) {
              strBuf1.append(strBuf2.toString());
            }

            if ((j + 1 < stackIds.length) && (strBuf2.length() > 0)) {
              strBuf1.append(" || ");
            }
          }
          strBuf1.append(")");
          clause = strBuf1.toString();
        }
      } else if (actionName.equals(FILTER_NOT_SELECTED_HEAPSTACK_ACTION_NAME)) {
        long stackIds[] =
            AnWindow.getInstance()
                .getHeapView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_Heap);
        if (stackIds != null && stackIds.length > 0) {
          StringBuffer strBuf1 = new StringBuffer();
          strBuf1.append("(!(");
          for (int j = 0; j < stackIds.length; j++) {
            StringBuffer strBuf2 = new StringBuffer();
            strBuf2.append("((");
            strBuf2.append(stackIds[j]);
            strBuf2.append(")");
            strBuf2.append(" == STACKID )");
            setFilter = true;

            if (strBuf2.length() > 0) {
              strBuf1.append(strBuf2.toString());
            }

            if ((j + 1 < stackIds.length) && (strBuf2.length() > 0)) {
              strBuf1.append(" || ");
            }
          }
          strBuf1.append("))");
          clause = strBuf1.toString();
        }
      } else if (actionName.equals(FILTER_WITH_SELECTED_HEAPACTIVESTACK_ACTION_NAME)) {
        long stackId = 0;
        long stackIds[] =
            AnWindow.getInstance()
                .getHeapView()
                .getSelectedIds(selected_indices, AnDisplay.DSP_Heap);

        if (stackIds != null && stackIds.length > 0) {
          // Only one stack id can be selected.
          stackId = stackIds[0];
        }

        long peakTimestamp = parent.window.getSelectedObject().getSelObjHeapTimestamp(stackId);

        // Get the user experiment id. Set the stackId to zero for Total Hist obj
        int userExpId = parent.window.getSelectedObject().getSelObjHeapUserExpId(0);

        StringBuffer strBuf1 = new StringBuffer();
        if (peakTimestamp > 0) {
          strBuf1.append(
              "!((TSTAMP_HI < "
                  + peakTimestamp
                  + ") || (TSTAMP_LO >  "
                  + peakTimestamp
                  + ")) && (( EXPID=="
                  + userExpId
                  + " ))");
          setFilter = true;
          clause = strBuf1.toString();
        }
      } else if (actionName.equals(FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_ACTION_NAME)) {
        StringBuffer strBuf1 = new StringBuffer();
        strBuf1.append("( HLEAKED > 0 )");
        setFilter = true;
        clause = strBuf1.toString();
      } else if (actionName.equals(FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_ACTION_NAME)) {
        StringBuffer strBuf1 = new StringBuffer();
        strBuf1.append("(( HSIZE > 0 ) && ( HLEAKED == 0))");
        setFilter = true;
        clause = strBuf1.toString();
      }
      if (setFilter) {
        //                parent.window.filter.showDialog();
        //                parent.window.filter.setSelectedTab(1);  // Select advanced filter tab
        parent
            .window
            .getFilters()
            .addClause(
                shortName,
                longName,
                clause,
                FilterClause.Kind.STANDARD); // Put the string in text field
        return;
      }
    }
    if (gap != null) {
      if (actionName.equals(gap.STR_ACTION_NEXT_HOT_LINE)) {
        gap.HG_Next_Hot_Line();
        return;
      }
      if (actionName.equals(gap.STR_ACTION_NEXT_NZ_LINE)) {
        gap.HG_Next_NZ_Line();
        return;
      }
      if (actionName.equals(gap.STR_ACTION_PREV_HOT_LINE)) {
        gap.HG_Prev_Hot_Line();
        return;
      }
      if (actionName.equals(gap.STR_ACTION_PREV_NZ_LINE)) {
        gap.HG_Prev_NZ_Line();
        return;
      }
    }
    if ((parent != null)
        && ((parent.type == AnDisplay.DSP_IndexObject
            || parent.type == AnDisplay.DSP_MemoryObject))) {
      // Threads, CPUs, Samples, Seconds tabs
      AnUtility.checkIPCOnWrongThread(false);
      setStandardFilter(shortName, longName, actionName); // IPC
      AnUtility.checkIPCOnWrongThread(true);
    }

    // XXX: AWT - Everything below can be done on AWT thread,
    // so this code should be moved up to the beginning of this method
    int disp_type = type;
    if (parent.parent_type == AnDisplay.DSP_SourceDisassembly
        || parent.parent_type == AnDisplay.DSP_DualSource) {
      disp_type = parent.parent_type;
    }

    if ((disp_type == AnDisplay.DSP_Source)
        || (disp_type == AnDisplay.DSP_SourceV2)
        || (disp_type == AnDisplay.DSP_Disassembly)
        || (disp_type == AnDisplay.DSP_DisassemblyV2)
        || (disp_type == AnDisplay.DSP_SourceDisassembly)
        || (disp_type == AnDisplay.DSP_DualSource)) {
      if (actionName.equals(STR_ACTION_BACK) || actionName.equals(STR_ACTION_FORWARD)) {
        parent.window.setSelectedView(disp_type);
        return;
      }
    }

    // Fire events
    AnEvent ev;
    if (actionName.equals(STR_ACTION_BACK)) {
      ev = new AnEvent(anTable, AnEvent.EVT_BACK, orgRow, null);
    } else if (actionName.equals(STR_ACTION_FORWARD)) {
      ev = new AnEvent(anTable, AnEvent.EVT_FORWARD, orgRow, null);
    } else if (actionName.equals(STR_ACTION_SETHEAD)) {
      ev = new AnEvent(anTable, AnEvent.EVT_SETHEAD, orgRow, null);
    } else if (actionName.equals(STR_ACTION_SETTAIL)) {
      ev = new AnEvent(anTable, AnEvent.EVT_SETTAIL, orgRow, null);
    } else if (actionName.equals(STR_ACTION_RECENTER)) {
      ev = new AnEvent(anTable, AnEvent.EVT_SET, orgRow, null);
    } else if (actionName.equals(STR_ACTION_RESET)) {
      ev = new AnEvent(anTable, AnEvent.EVT_RESET, orgRow, null);
    } else if (actionName.equals(STR_ACTION_COPY_ALL)) {
      ev = new AnEvent(anTable, AnEvent.EVT_COPY_ALL, orgRow, null);
    } else if (actionName.equals(STR_ACTION_COPY_SEL)) {
      ev = new AnEvent(anTable, AnEvent.EVT_COPY_SEL, orgRow, null);
    } else {
      ev = new AnEvent(anTable, AnEvent.EVT_UPDATE, orgRow, null);
    }
    fireAnEvent(ev);
    table.requestFocus();
  }

  private String get_list_of_selected_objects() {
    AnUtility.checkIPCOnWrongThread(false);
    String sObjs =
        ((FuncListDisp) parent).composeFilterClause(parent.type, parent.subtype, selected_indices);
    AnUtility.checkIPCOnWrongThread(true);
    if (sObjs == null) { // Should never happen
      sObjs = "0";
    }
    String pattern1 = "(LEAF IN (";
    if (sObjs.startsWith(pattern1)) {
      String pattern2 = "))";
      if (sObjs.endsWith(pattern2)) {
        int c1 = pattern1.length() - 1;
        int c2 = sObjs.length() - 1;
        sObjs = sObjs.substring(c1, c2);
      }
    }
    return sObjs;
  }

  private String getFilterForSelectedObjects(final String stackName) {
    String sObjs = get_list_of_selected_objects();
    AnUtility.checkIPCOnWrongThread(false);
    String fltr = String.format("(%s SOME IN %s)", sObjs, stackName);
    return fltr;
  }

  /**
   * Get Function Name
   *
   * @param s - a function name with arguments
   * @return fs - function name
   */
  private String getFunctionName(String s) {
    String fs = s;
    if (null != s) {
      int i = s.indexOf("(");
      if (i >= 0) {
        // Everything that is inside (...) is not a part of function name
        fs = s.substring(0, i);
      }
    }
    return fs;
  }

  /**
   * Sets Standard Filter for Index Object tabs: Threads, CPUs, Samples, Seconds
   *
   * @param actionName
   */
  private void setStandardFilter(String shortName, String longName, String actionName) {
    boolean setFilter = false;
    String clause = null;
    if (selected_indices.length > 0) {
      clause = parent.window.composeFilterClause(parent.type, parent.subtype, selected_indices);
      if (actionName.equals(FILTER_WITH_SELECTED_ITEMS_ACTION_NAME)) {
        setFilter = true;
      }
      if (actionName.equals(FILTER_WITHOUT_SELECTED_ITEMS_ACTION_NAME)) {
        clause = "(!" + clause + ")";
        setFilter = true;
      }
    }
    if (actionName.equals(STR_ACTION_REMOVE_ALL_FILTERS)) {
      clause = "1";
      setFilter = true;
    }
    if (setFilter) {
      //                parent.window.filter.showDialog();
      //                parent.window.filter.setSelectedTab(1);  // Select advanced filter tab
      parent
          .window
          .getFilters()
          .addClause(
              shortName,
              longName,
              clause,
              FilterClause.Kind.STANDARD); // Put the string in text field
      return;
    }
  }

  private String getSelectedViewDisplayName() {
    String ret = "Table";
    if (parent != null && parent.window != null) {
      ret = parent.window.getSelectedView().getDisplayName();
    }
    return ret;
  }

  public JPopupMenu initPopup(boolean filterOnly) {
    JPopupMenu popup = new JPopupMenu();

    boolean row_selected = false;
    int row = table.getSelectedRow();
    if (row >= 0) {
      row_selected = true;
    }
    String viewName = getSelectedViewDisplayName();

    if (filterOnly) {
      addFilterPopup(filterOnly, popup, type, viewName, row_selected);
    } else {
      addAllPopup(filterOnly, popup, type, viewName, row, row_selected);
    }
    return popup;
  }

  private void addAllPopup(
      boolean filterOnly,
      JPopupMenu popup,
      int type,
      String viewName,
      int row,
      boolean row_selected) {
    AccessibleContext ac;
    UpdateAction ua;
    JMenuItem mi;
    JMenu me;
    String txt;
    // Add default action
    if ((type == AnDisplay.DSP_Functions) || (type == AnDisplay.DSP_Lines)) {
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      // Add "Show Source" action
      txt = STR_ACTION_SHOW_SOURCE;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);
      //                popup.addSeparator();
      // Add "Show Disassembly" action
      txt = STR_ACTION_SHOW_DISASM;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);
    } else if (type == AnDisplay.DSP_PCs) {
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      // Add "Show Disassembly" action
      txt = STR_ACTION_SHOW_DISASM;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);
    } else if ((type == AnDisplay.DSP_Source)
        || (type == AnDisplay.DSP_SourceV2)
        || (type == AnDisplay.DSP_Disassembly)
        || (type == AnDisplay.DSP_DisassemblyV2)) {

      SrcRenderer renderer = null;
      if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2) {
        renderer = srcRenderer;
      } else {
        renderer = disRenderer;
      }
      // show a list if lines in multiple functions that are chosen
      int num = table.getSelectedRows().length;
      HashMap<Long, Integer> funcMap = new HashMap<Long, Integer>();
      int funcNum = 0;
      if (num > 0) {
        for (int i = 0; i < num; i++) {
          int r = table.getSelectedRows()[i];
          if (renderer.functionId != null && r > 0 && r <= renderer.functionId.size()) {
            Long funcId = renderer.functionId.get(r);
            if (funcId != 0 && funcMap.get(funcId) == null) {
              funcMap.put(funcId, r);
            }
          }
        }
        for (Long idx : funcMap.keySet()) {
          int r = funcMap.get(idx).intValue();
          String funcName = renderer.getFunction(r);
          if (funcName != null) {
            funcNum++;
          }
        }
      }

      if ((type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2)
          || (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2)) {

        txt = STR_ACTION_BACK;
        SelObjInfo so = navHistoryPool.getHistory().getBack();
        boolean can_go_back = navHistoryPool.getHistory().canGoBack();
        boolean inError = anTable.getViewport().getView() != table;
        if (inError) {
          so = navHistoryPool.getHistory().getCurrent();
          can_go_back = (so != null);
        }
        if (so != null) {
          NavigationHistoryUpdateAction nhua =
              new NavigationHistoryUpdateAction(txt, so.id, so.lineno, so.name);
          mi = new JMenuItem(nhua);
        } else {
          mi = new JMenuItem(txt);
        }
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(txt);
        mi.setEnabled(can_go_back);
        mi.setAccelerator(KeyboardShortcuts.backwardActionShortcut);
        popup.add(mi);

        txt = STR_ACTION_FORWARD;
        so = navHistoryPool.getHistory().getForward();
        if (so != null) {
          NavigationHistoryUpdateAction nhua =
              new NavigationHistoryUpdateAction(txt, so.id, so.lineno, so.name);
          mi = new JMenuItem(nhua);
        } else {
          mi = new JMenuItem(txt);
        }
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(txt);
        mi.setEnabled(navHistoryPool.getHistory().canGoForward());
        mi.setAccelerator(KeyboardShortcuts.forwardActionShortcut);
        popup.add(mi);

        if (popup.getComponentCount() > 0) {
          popup.addSeparator();
        }

        // add "show callee source/disasm"
        int[] selectedRows = table.getSelectedRows();
        ArrayList<Integer> callsiteRows = new ArrayList<Integer>();
        for (int i = 0; i < selectedRows.length; i++) {
          int sr = selectedRows[i];
          ArrayList<SelObjInfo> calleeFuncs = renderer.calleeInfo.get(sr);
          if (calleeFuncs != null) {
            callsiteRows.add(sr);
          }
        }
        JMenuItem mi_dis = null;
        JMenu me_dis = null;
        if (callsiteRows.size() > 0) {
          txt = STR_ACTION_SHOW_CALLEE_SOURCE; // XXXX if the position is changed, please update the
          // offset in srcRenderer.goToCallee too
          String txt_dis = STR_ACTION_SHOW_CALLEE_DISASM;
          me = new JMenu(new UpdateAction(txt));
          me_dis = new JMenu(new UpdateAction(txt_dis));
          me.setEnabled(
              row_selected
                  && (type == AnDisplay.DSP_Source
                      || type == AnDisplay.DSP_SourceV2
                      || parent.parent_type != AnDisplay.DSP_SourceDisassembly));
          JMenu cur_me = me;
          me_dis.setEnabled(
              row_selected
                  && (type == AnDisplay.DSP_Disassembly
                      || type == AnDisplay.DSP_DisassemblyV2
                      || parent.parent_type != AnDisplay.DSP_SourceDisassembly));
          JMenu cur_me_dis = me_dis;
          int num_items = 0;
          HashMap<Long, Boolean> calleeSeen = new HashMap<Long, Boolean>();
          for (int i = 0; i < callsiteRows.size(); i++) {
            int sr = callsiteRows.get(i);
            ArrayList<SelObjInfo> calleeFuncs = renderer.calleeInfo.get(sr);

            if (calleeFuncs == null) {
              continue;
            }
            Collections.sort(calleeFuncs);
            boolean seperaterAdded = false;
            for (int j = 0; j < calleeFuncs.size(); j++) {
              SelObjInfo fi = calleeFuncs.get(j);
              if (calleeSeen.get(fi.id) != null && calleeSeen.get(fi.id) == true) {
                continue;
              }
              if (i > 0 && !seperaterAdded) {
                cur_me.addSeparator();
                cur_me_dis.addSeparator();
                seperaterAdded = true;
              }
              calleeSeen.put(fi.id, true);
              NavigationUpdateAction nua = new NavigationUpdateAction(txt, fi.id, fi.lineno);
              NavigationUpdateAction nua_dis =
                  new NavigationUpdateAction(txt_dis, fi.id, fi.lineno);
              mi = new JMenuItem();
              mi.setAction(nua);
              mi.setText(fi.name);
              cur_me.add(mi);
              mi_dis = new JMenuItem();
              mi_dis.setAction(nua_dis);
              mi_dis.setText(fi.name);
              cur_me_dis.add(mi_dis);
              num_items++;
              if (num_items > 0
                  && num_items % 20 == 0
                  && !(j == calleeFuncs.size() - 1 && i == callsiteRows.size() - 1)) {
                JMenu more_me = new JMenu(AnLocale.getString("more..."));
                cur_me.add(more_me);
                cur_me = more_me;
                JMenu more_me_dis = new JMenu(AnLocale.getString("more..."));
                cur_me_dis.add(more_me_dis);
                cur_me_dis = more_me_dis;
              }
            }
          }
          if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2) {
            popup.add(me);
          } else if (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2) {
            popup.add(me_dis);
          }
        } else {
          if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2) {
            mi = new JMenuItem(STR_ACTION_SHOW_CALLEE_SOURCE);
            mi.setEnabled(false);
            popup.add(mi);
          }
          if (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2) {
            mi = new JMenuItem(STR_ACTION_SHOW_CALLEE_DISASM);
            mi.setEnabled(false);
            popup.add(mi);
          }
        }

        if (popup.getComponentCount() > 0) {
          popup.addSeparator();
        }

        // add "show caller source/disasm"
        txt = STR_ACTION_SHOW_CALLER_SOURCE;
        String txt_dis = STR_ACTION_SHOW_CALLER_DISASM;

        HashMap<String, Boolean> callerSeen = new HashMap<String, Boolean>();
        me = new JMenu(new UpdateAction(txt));
        me.setEnabled(
            row_selected
                && (type == AnDisplay.DSP_Source
                    || type == AnDisplay.DSP_SourceV2
                    || parent.parent_type != AnDisplay.DSP_SourceDisassembly));
        JMenu cur_me = me;
        me_dis = new JMenu(new UpdateAction(txt_dis));
        me_dis.setEnabled(
            row_selected
                && (type == AnDisplay.DSP_Disassembly
                    || type == AnDisplay.DSP_DisassemblyV2
                    || parent.parent_type != AnDisplay.DSP_SourceDisassembly));
        JMenu cur_me_dis = me_dis;
        int num_items = 0;
        int num_funcs = 0;
        for (Long idx : funcMap.keySet()) {
          ArrayList<SelObjInfo> callerFuncs = renderer.callerInfo.get(idx.longValue());
          if (callerFuncs == null) {
            continue;
          }
          Collections.sort(callerFuncs);
          boolean seperaterAdded = false;
          for (int j = 0; j < callerFuncs.size(); j++) {
            SelObjInfo fi = callerFuncs.get(j);
            if (callerSeen.get(fi.name) != null && callerSeen.get(fi.name) == true) {
              continue;
            }
            if (num_funcs > 0 && !seperaterAdded) {
              cur_me.addSeparator();
              cur_me_dis.addSeparator();
              seperaterAdded = true;
            }
            callerSeen.put(fi.name, true);
            NavigationUpdateAction nua = new NavigationUpdateAction(txt, fi.id, fi.lineno);
            mi = new JMenuItem();
            mi.setAction(nua);
            mi.setText(fi.name);
            cur_me.add(mi);
            NavigationUpdateAction nua_dis = new NavigationUpdateAction(txt_dis, fi.id, fi.lineno);
            mi_dis = new JMenuItem();
            mi_dis.setAction(nua_dis);
            mi_dis.setText(fi.name);
            cur_me_dis.add(mi_dis);
            num_items++;
            if (num_items > 0
                && num_items % 20 == 0
                && !(j == callerFuncs.size() - 1 && num_funcs == funcMap.size() - 1)) {
              JMenu more_me = new JMenu(AnLocale.getString("more..."));
              cur_me.add(more_me);
              cur_me = more_me;
              JMenu more_me_dis = new JMenu(AnLocale.getString("more..."));
              cur_me_dis.add(more_me_dis);
              cur_me_dis = more_me_dis;
            }
          }
          num_funcs++;
        }
        if (num_items > 0) {
          if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2) {
            popup.add(me);
          } else if (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2) {
            popup.add(me_dis);
          }
        } else {
          if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_SourceV2) {
            mi = new JMenuItem(STR_ACTION_SHOW_CALLER_SOURCE);
            mi.setEnabled(false);
            popup.add(mi);
          }
          if (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_DisassemblyV2) {
            mi = new JMenuItem(STR_ACTION_SHOW_CALLER_DISASM);
            mi.setEnabled(false);
            popup.add(mi);
          }
        }
      }

      if ((type == AnDisplay.DSP_Source) || (type == AnDisplay.DSP_SourceV2)) {
        if (popup.getComponentCount() > 0) {
          popup.addSeparator();
        }

        // Add "Show Disassembly" action
        txt = STR_ACTION_SHOW_DISASM;

        if (funcNum > 1 && funcMap != null) {
          me = new JMenu(new UpdateAction(txt));
          me.setEnabled(row_selected);
          for (Long idx : funcMap.keySet()) {
            int r = funcMap.get(idx).intValue();
            String funcName = renderer.getFunction(r);
            if (funcName != null) {
              SrcLineUpdateAction sua = new SrcLineUpdateAction(txt, r);
              mi = new JMenuItem();
              mi.setAction(sua);
              mi.setText(funcName);
              me.add(mi);
            }
          }
          popup.add(me);
        } else {
          mi = new JMenuItem(new UpdateAction(txt));
          ac = mi.getAccessibleContext();
          ac.setAccessibleDescription(txt);
          mi.setEnabled(row_selected);
          popup.add(mi);
        }
      }
    }
    if ((type == AnDisplay.DSP_Callers)) {
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      // Add "Prepend" action
      txt = STR_ACTION_PREPEND;
      UpdateAction ppa = new UpdateAction(txt);
      mi = new JMenuItem(ppa);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);
    }
    if ((type == AnDisplay.DSP_Callees)) {
      // Add "Append" action
      txt = STR_ACTION_APPEND;
      UpdateAction apa = new UpdateAction(txt);
      mi = new JMenuItem(apa);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);
    }
    if ((type == AnDisplay.DSP_CallerCalleeSelf)) {
      // Add "Remove" action
      txt = STR_ACTION_REMOVE;
      UpdateAction rma = new UpdateAction(txt);
      mi = new JMenuItem(rma);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(false);
      if (row == 0) {
        mi.setEnabled(true);
      }
      if (table.getRowCount() - 1 == row) {
        mi.setEnabled(row_selected);
      }
      if (table.getRowCount() <= 1) {
        mi.setEnabled(false);
      }
      popup.add(mi);
      // Add "Set Head" action
      txt = STR_ACTION_SETHEAD;
      ua = new UpdateAction(txt);
      mi = new JMenuItem(ua);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (row <= 0) {
        mi.setEnabled(false);
      } else {
        mi.setEnabled(true);
      }
      popup.add(mi);
    }
    if ((type == AnDisplay.DSP_Callers)
        || (type == AnDisplay.DSP_CallerCalleeSelf)
        || (type == AnDisplay.DSP_Callees)) {
      // Add "Set Center" action
      txt = STR_ACTION_RECENTER;
      UpdateAction rca = new UpdateAction(txt);
      mi = new JMenuItem(rca);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      if ((type == AnDisplay.DSP_CallerCalleeSelf)) {
        if (table.getRowCount() <= 1) {
          mi.setEnabled(false);
        }
      }
      popup.add(mi);
    }
    if ((type == AnDisplay.DSP_CallerCalleeSelf)) {
      // Add "Set Tail" action
      txt = STR_ACTION_SETTAIL;
      ua = new UpdateAction(txt);
      mi = new JMenuItem(ua);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(false);
      if (row >= 0) {
        if (row < table.getRowCount() - 1) {
          mi.setEnabled(true);
        }
      }
      popup.add(mi);
    }
    if ((type == AnDisplay.DSP_Callers)
        || (type == AnDisplay.DSP_CallerCalleeSelf)
        || (type == AnDisplay.DSP_Callees)) {

      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }

      // NM // Add "Reset" action
      // NM txt = STR_ACTION_RESET;
      // NM UpdateAction rsa = new UpdateAction(txt);
      // NM mi = new JMenuItem(rsa);
      // NM ac = mi.getAccessibleContext();
      // NM ac.setAccessibleDescription(txt);
      // NM mi.setEnabled(true);
      // NM popup.add(mi);
      // NM
      // NM // Add separator
      // NM popup.addSeparator();
      // Add "Back" action
      txt = STR_ACTION_BACK;
      UpdateAction bka = new UpdateAction(txt);
      mi = new JMenuItem(bka);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (parent != null) {
        mi.setEnabled(parent.isBackActionAvailable());
        popup.add(mi);
      }
      // Add "Forward" action
      txt = STR_ACTION_FORWARD;
      UpdateAction fwa = new UpdateAction(txt);
      mi = new JMenuItem(fwa);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      if (parent != null) {
        mi.setEnabled(parent.isForwardActionAvailable());
        popup.add(mi);
      }

      // Add separator
      popup.addSeparator();
    }
    if ((type == AnDisplay.DSP_MiniCaller || type == AnDisplay.DSP_MiniCallee)) {
      txt = STR_ACTION_SHOW_SELECTED_FUNCTION;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(row_selected);
      popup.add(mi);
    }

    addFilterPopup(filterOnly, popup, type, viewName, row_selected);

    popup.addSeparator();
    JMenu tableColumnsSubMenu;
    tableColumnsSubMenu = new JMenu(AnLocale.getString("Table Columns"));
    for (Component component :
        AnWindow.getInstance().getSettings().getMetricsSetting().createTableColumnMenuItems(this)) {
      tableColumnsSubMenu.add(component);
    }
    tableColumnsSubMenu.addSeparator();
    for (Component component :
        AnWindow.getInstance().getSettings().getMetricsSetting().getOtherMetricItems(this)) {
      tableColumnsSubMenu.add(component);
    }
    popup.add(tableColumnsSubMenu);

    if (canSort
        && !(type == AnDisplay.DSP_MiniCallee
            || type == AnDisplay.DSP_MiniCaller
            || type == AnDisplay.DSP_MiniCallerCalleeSelf)) {
      // Add Select Next submenu
      JMenu sortSubMenu;
      sortSubMenu = new JMenu(AnLocale.getString("Sort Table By "));
      // Add sorting menu items
      for (int i = 0; i < tableModel.getColumnCount(); i++) {
        txt = tableModel.getLabel(i).getText(); // FIXUP
        SortMenu sm = new SortMenu(i, txt);
        mi = new JMenuItem(sm);
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(txt);
        mi.setEnabled(canSort);
        sortSubMenu.add(mi);
      }
      popup.add(sortSubMenu);
    }

    switch (type) {
      case AnDisplay.DSP_Functions:
      case AnDisplay.DSP_Lines:
      case AnDisplay.DSP_SourceDisassembly:
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_SourceV2:
      case AnDisplay.DSP_DisassemblyV2:
        //            case AnDisplay.DSP_LEAKLIST:
      case AnDisplay.DSP_InstructionFrequency:
        {
          if (gap != null) {
            if (popup.getComponentCount() > 0) {
              popup.addSeparator();
            }

            // Add "Previous Hot Line" action
            txt = gap.STR_ACTION_PREV_HOT_LINE;
            mi = new JMenuItem(new UpdateAction(txt));
            mi.setAccelerator(KeyboardShortcuts.sourcePreviousHotLineActionShortcut);
            ac = mi.getAccessibleContext();
            ac.setAccessibleDescription(txt);
            mi.setEnabled(gap.HG_Is_Prev_Hot_Line());
            popup.add(mi);

            // Add "Next Hot Line" action
            txt = gap.STR_ACTION_NEXT_HOT_LINE;
            mi = new JMenuItem(new UpdateAction(txt));
            mi.setAccelerator(KeyboardShortcuts.sourceNextHotLineActionShortcut);
            ac = mi.getAccessibleContext();
            ac.setAccessibleDescription(txt);
            mi.setEnabled(gap.HG_Is_Next_Hot_Line());
            popup.add(mi);

            // Add separator
            popup.addSeparator();

            // Add "Previous Non-zero Metric Line" action
            txt = gap.STR_ACTION_PREV_NZ_LINE;
            mi = new JMenuItem(new UpdateAction(txt));
            mi.setAccelerator(KeyboardShortcuts.sourcePreviousNonZeroLineActionShortcut);
            ac = mi.getAccessibleContext();
            ac.setAccessibleDescription(txt);
            mi.setEnabled(gap.HG_Is_Prev_NZ_Line());
            popup.add(mi);

            // Add "Next Non-zero Metric Line" action
            txt = gap.STR_ACTION_NEXT_NZ_LINE;
            mi = new JMenuItem(new UpdateAction(txt));
            mi.setAccelerator(KeyboardShortcuts.sourceNextNonZeroLineActionShortcut);
            ac = mi.getAccessibleContext();
            ac.setAccessibleDescription(txt);
            mi.setEnabled(gap.HG_Is_Next_NZ_Line());
            popup.add(mi);
          }
          break;
        }
    }

    //        popup.addSeparator();
    //        // Format
    //        JMenu formatByMenuItem = new JMenu(AnLocale.getString("Format"));
    //        JRadioButtonMenuItem jRadioButtonMenuItem;
    //        formatByMenuItem.add(jRadioButtonMenuItem = new
    // JRadioButtonMenuItem(AnLocale.getString("Text")));
    //        jRadioButtonMenuItem.setSelected(true);
    //        formatByMenuItem.add(jRadioButtonMenuItem = new
    // JRadioButtonMenuItem(AnLocale.getString("Bar")));
    //        jRadioButtonMenuItem.setEnabled(false);
    //        formatByMenuItem.add(jRadioButtonMenuItem = new
    // JRadioButtonMenuItem(AnLocale.getString("Pie")));
    //        jRadioButtonMenuItem.setEnabled(false);
    //        popup.add(formatByMenuItem);
    // Table format
    //        JMenu formatMenuItem = new JMenu(AnLocale.getString("Format"));
    //        final JCheckBoxMenuItem wrapCheckBoxMenuItem = new
    // JCheckBoxMenuItem(AnLocale.getString("Wrap Long Metric Names in Table Headers"));
    //
    // wrapCheckBoxMenuItem.setSelected(AnWindow.getInstance().getSettings().getTableSettings().wrapMetricNames());
    //        wrapCheckBoxMenuItem.addActionListener(new ActionListener() {
    //            @Override
    //            public void actionPerformed(ActionEvent e) {
    //
    // AnWindow.getInstance().getSettings().getTableSettings().setWrapMetricNames(AnTable.this,
    // wrapCheckBoxMenuItem.isSelected());
    //            }
    //
    //        });
    //        formatMenuItem.add(wrapCheckBoxMenuItem);
    //        popup.add(formatMenuItem);
    // Compare
    //        CompareModeSetting.CompareMode mode =
    // AnWindow.getInstance().getSettings().getCompareModeSetting().get();
    //        JMenu compareMenuItem = new JMenu(AnLocale.getString("Compare Mode"));
    //        if (mode != CompareModeSetting.CompareMode.CMP_DISABLE) {
    //            JRadioButtonMenuItem absoluteRadioButtonMenuItem = new
    // JRadioButtonMenuItem(AnLocale.getString("Absolute"));
    //            JRadioButtonMenuItem deltaRadioButtonMenuItem = new
    // JRadioButtonMenuItem(AnLocale.getString("Delta"));
    //            JRadioButtonMenuItem ratioRadioButtonMenuItem = new
    // JRadioButtonMenuItem(AnLocale.getString("Ratio"));
    //            absoluteRadioButtonMenuItem.setSelected(mode ==
    // CompareModeSetting.CompareMode.CMP_ENABLE);
    //            deltaRadioButtonMenuItem.setSelected(mode ==
    // CompareModeSetting.CompareMode.CMP_DELTA);
    //            ratioRadioButtonMenuItem.setSelected(mode ==
    // CompareModeSetting.CompareMode.CMP_RATIO);
    //            absoluteRadioButtonMenuItem.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    AnWindow.getInstance().getSettings().getCompareModeSetting().set(this,
    // CompareModeSetting.CompareMode.CMP_ENABLE);
    //                }
    //            });
    //            deltaRadioButtonMenuItem.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    AnWindow.getInstance().getSettings().getCompareModeSetting().set(this,
    // CompareModeSetting.CompareMode.CMP_DELTA);
    //                }
    //            });
    //            ratioRadioButtonMenuItem.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    AnWindow.getInstance().getSettings().getCompareModeSetting().set(this,
    // CompareModeSetting.CompareMode.CMP_RATIO);
    //                }
    //            });
    //            compareMenuItem.add(absoluteRadioButtonMenuItem);
    //            compareMenuItem.add(deltaRadioButtonMenuItem);
    //            compareMenuItem.add(ratioRadioButtonMenuItem);
    //            formatMenuItem.add(compareMenuItem);
    //        }
    if ((type == AnDisplay.DSP_Callers)
        || (type == AnDisplay.DSP_CallerCalleeSelf)
        || (type == AnDisplay.DSP_Callees)) {
      // Add menu item "Copy All"
      txt = STR_ACTION_COPY_ALL;
      UpdateAction ca = new UpdateAction(txt);
      mi = new JMenuItem(ca);
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      popup.addSeparator();
      popup.add(mi);
    }

    switch (type) {
      case AnDisplay.DSP_Callees:
      case AnDisplay.DSP_Callers:
      case AnDisplay.DSP_CallerCalleeSelf:
      case AnDisplay.DSP_MPIChart:
      case AnDisplay.DSP_MPITimeline:
      case AnDisplay.DSP_Null:
      case AnDisplay.DSP_Overview:
      case AnDisplay.DSP_Statistics:
      case AnDisplay.DSP_Timeline:
      case AnDisplay.DSP_Welcome:
        break;
      default:
        popup.addSeparator();
        // Add menu item "Copy Selected"
        txt = STR_ACTION_COPY_SEL;
        UpdateAction cs = new UpdateAction(txt);
        mi = new JMenuItem(cs);
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(txt);
        popup.add(mi);
        // Add menu item "Copy All"
        txt = STR_ACTION_COPY_ALL;
        UpdateAction ca = new UpdateAction(txt);
        mi = new JMenuItem(ca);
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(txt);
        popup.add(mi);
        break;
    }

    switch (type) {
      case AnDisplay.DSP_Functions:
      case AnDisplay.DSP_MiniFunctions:
      case AnDisplay.DSP_IO:
      case AnDisplay.DSP_Heap:
      case AnDisplay.DSP_IOFileDescriptors:
      case AnDisplay.DSP_Lines:
      case AnDisplay.DSP_PCs:
      case AnDisplay.DSP_IndexObject:
      case AnDisplay.DSP_Callers:
      case AnDisplay.DSP_CallerCalleeSelf:
      case AnDisplay.DSP_Callees:
        popup.addSeparator();
        // Add menu item Properties
        mi =
            new JMenuItem(
                new SettingsAction(
                    AnLocale.getString("Metric Settings"),
                    parent.window.getSettings().settingsMetricsIndex));
        mi.setAccelerator(KeyboardShortcuts.metricsSettingsShortcut);
        popup.add(mi);
        break;
      case AnDisplay.DSP_SourceDisassembly:
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_Disassembly:
      case AnDisplay.DSP_SourceV2:
      case AnDisplay.DSP_DisassemblyV2:
        popup.addSeparator();
        // Add menu item Properties
        mi =
            new JMenuItem(
                new SettingsAction(
                    AnLocale.getString("Source/Disassembly Settings"),
                    parent.window.getSettings().settingsSourceDisassemblyIndex));
        mi.setAccelerator(KeyboardShortcuts.settingsActionShortcut);
        popup.add(mi);
        break;
    }
  }

  private void addFilterPopup(
      boolean filterOnly, JPopupMenu popup, int type, String viewName, boolean row_selected) {
    AccessibleContext ac;
    JMenuItem mi;
    String txt;

    if ((type == AnDisplay.DSP_Callers)
        || (type == AnDisplay.DSP_CallerCalleeSelf)
        || (type == AnDisplay.DSP_Callees)) {
      // Add "Set Filter: Stack Fragment in Stack ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_CALLSTACK_FRAGMENT_SHORT_NAME,
                  FILTER_CALLSTACK_FRAGMENT_LONG_NAME,
                  FILTER_CALLSTACK_FRAGMENT_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_CALLSTACK_FRAGMENT_LONG_NAME);
      mi.setEnabled(true);
      popup.add(mi);
      // Add "Set Filter: Not Stack Fragment in Stack ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_NOT_CALLSTACK_FRAGMENT_SHORT_NAME,
                  FILTER_NOT_CALLSTACK_FRAGMENT_LONG_NAME,
                  FILTER_NOT_CALLSTACK_FRAGMENT_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_NOT_CALLSTACK_FRAGMENT_LONG_NAME);
      mi.setEnabled(true);
      popup.add(mi);
      // Add "Set Filter: Function in Stack ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_SELECTED_FUNCTION_SHORT_NAME,
                  FILTER_SELECTED_FUNCTION_LONG_NAME,
                  FILTER_SELECTED_FUNCTION_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_SELECTED_FUNCTION_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
      // Add "Set Filter: Function is Leaf ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_LEAF_FUNCTION_SHORT_NAME,
                  FILTER_LEAF_FUNCTION_LONG_NAME,
                  FILTER_LEAF_FUNCTION_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_LEAF_FUNCTION_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
      if (filterOnly) {
        popup.addSeparator();
      }
      // Add Manage Filters
      txt = STR_ACTION_CUSTOM_FILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);
      // Add "Undo Last Filter ..." action
      txt = STR_ACTION_UNDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canUndoLastFilter());
      popup.add(mi);
      // Add "Redo Last Filter ..." action
      txt = STR_ACTION_REDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRedoLastFilter());
      popup.add(mi);
      // Add "Reset Default Filter ..." action
      txt = STR_ACTION_REMOVE_ALL_FILTERS;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRemoveAllFilters());
      popup.add(mi);
      // Add "Remove Filter" action
      if (parent.window.getFilters().anyFilters()) {
        popup.add(parent.window.getFilters().removeFilterMenuItem());
      }
      //                if (filterOnly) {
      //                    popup.addSeparator();
      //                }
    }
    if (type == AnDisplay.DSP_Source || type == AnDisplay.DSP_Lines) {
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_SELECTED_LINES_SHORT_NAME,
                  FILTER_SELECTED_LINES_LONG_NAME,
                  FILTER_SELECTED_LINES_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_SELECTED_LINES_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);

      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_NOT_SELECTED_LINES_SHORT_NAME,
                  FILTER_NOT_SELECTED_LINES_LONG_NAME,
                  FILTER_NOT_SELECTED_LINES_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_NOT_SELECTED_LINES_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
    }
    if (type == AnDisplay.DSP_Disassembly || type == AnDisplay.DSP_PCs) {
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_SELECTED_PCS_SHORT_NAME,
                  FILTER_SELECTED_PCS_LONG_NAME,
                  FILTER_SELECTED_PCS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_SELECTED_PCS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);

      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_NOT_SELECTED_PCS_SHORT_NAME,
                  FILTER_NOT_SELECTED_PCS_LONG_NAME,
                  FILTER_NOT_SELECTED_PCS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_NOT_SELECTED_PCS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
    }
    if (type == AnDisplay.DSP_Functions) {
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      // Add "Set Filter: Function in Stack ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_SELECTED_FUNCTIONS_SHORT_NAME,
                  FILTER_SELECTED_FUNCTIONS_LONG_NAME,
                  FILTER_SELECTED_FUNCTIONS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_SELECTED_FUNCTIONS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
      // Add "Set Filter: Function in Stack ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_NOT_SELECTED_FUNCTIONS_SHORT_NAME,
                  FILTER_NOT_SELECTED_FUNCTIONS_LONG_NAME,
                  FILTER_NOT_SELECTED_FUNCTIONS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_NOT_SELECTED_FUNCTIONS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
      // Add "Set Filter: Function is Leaf ..."
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_LEAF_FUNCTIONS_SHORT_NAME,
                  FILTER_LEAF_FUNCTIONS_LONG_NAME,
                  FILTER_LEAF_FUNCTIONS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_LEAF_FUNCTIONS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
      // Add "Set Filter: Name in Stack ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_SIMILARLY_NAMED_SHORT_NAME,
                  FILTER_SIMILARLY_NAMED_LONG_NAME,
                  FILTER_SIMILARLY_NAMED_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_SIMILARLY_NAMED_LONG_NAME);
      mi.setEnabled(row_selected);
      if (table.getSelectedRowCount() > 1) {
        mi.setEnabled(false);
      }
      popup.add(mi);
    }
    if (type == AnDisplay.DSP_Source
        || type == AnDisplay.DSP_Lines
        || type == AnDisplay.DSP_Disassembly
        || type == AnDisplay.DSP_PCs
        || type == AnDisplay.DSP_Functions
        || type == AnDisplay.DSP_MiniFunctions) {
      // Add "Manage Filters..." action
      txt = STR_ACTION_CUSTOM_FILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);
      if (filterOnly) {
        popup.addSeparator();
      }
      // Add "Undo Last Filter ..." action
      txt = STR_ACTION_UNDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canUndoLastFilter());
      popup.add(mi);
      // Add "Redo Last Filter ..." action
      txt = STR_ACTION_REDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRedoLastFilter());
      popup.add(mi);
      // Add "Set Filter: Reset Default Filter ..." action
      txt = STR_ACTION_REMOVE_ALL_FILTERS;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRemoveAllFilters());
      popup.add(mi);
      // Add "Remove Filter" action
      if (parent.window.getFilters().anyFilters()) {
        popup.add(parent.window.getFilters().removeFilterMenuItem());
      }
      //                if (filterOnly) {
      //                    popup.addSeparator();
      //                }
    }

    if ((type == AnDisplay.DSP_IO)
        || (type == AnDisplay.DSP_IOFileDescriptors)
        || (type == AnDisplay.DSP_IOCallStacks)) {
      if (AnWindow.getInstance().getIOView().getDisplayMode() == AnDisplay.DSP_IO) {
        // Add "Set Filter: Selected File Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_WITH_SELECTED_FILE_SHORT_NAME,
                    FILTER_WITH_SELECTED_FILE_LONG_NAME,
                    FILTER_WITH_SELECTED_FILE_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_WITH_SELECTED_FILE_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Exclude Selected File Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_NOT_SELECTED_FILE_SHORT_NAME,
                    FILTER_NOT_SELECTED_FILE_LONG_NAME,
                    FILTER_NOT_SELECTED_FILE_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_NOT_SELECTED_FILE_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
      } else if (AnWindow.getInstance().getIOView().getDisplayMode()
          == AnDisplay.DSP_IOFileDescriptors) {
        // Add "Set Filter: Selected IOVFD Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_WITH_SELECTED_IOVFD_SHORT_NAME,
                    FILTER_WITH_SELECTED_IOVFD_LONG_NAME,
                    FILTER_WITH_SELECTED_IOVFD_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_WITH_SELECTED_IOVFD_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Exclude Selected IOVFD Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_NOT_SELECTED_IOVFD_SHORT_NAME,
                    FILTER_NOT_SELECTED_IOVFD_LONG_NAME,
                    FILTER_NOT_SELECTED_IOVFD_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_NOT_SELECTED_IOVFD_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
      } else if (AnWindow.getInstance().getIOView().getDisplayMode()
          == AnDisplay.DSP_IOCallStacks) {
        // Add "Set Filter: Selected Stack Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_WITH_SELECTED_IOSTACK_SHORT_NAME,
                    FILTER_WITH_SELECTED_IOSTACK_LONG_NAME,
                    FILTER_WITH_SELECTED_IOSTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_WITH_SELECTED_IOSTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Exclude Selected Stack Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_NOT_SELECTED_IOSTACK_SHORT_NAME,
                    FILTER_NOT_SELECTED_IOSTACK_LONG_NAME,
                    FILTER_NOT_SELECTED_IOSTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_NOT_SELECTED_IOSTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
      }
      // Add "Manage Filters..." action
      txt = STR_ACTION_CUSTOM_FILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);
      if (filterOnly) {
        popup.addSeparator();
      }
      // Add "Undo Last Filter ..." action
      txt = STR_ACTION_UNDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canUndoLastFilter());
      popup.add(mi);
      // Add "Redo Last Filter ..." action
      txt = STR_ACTION_REDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRedoLastFilter());
      popup.add(mi);
      // Add "Set Filter: Reset Default Filter ..." action
      txt = STR_ACTION_REMOVE_ALL_FILTERS;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRemoveAllFilters());
      popup.add(mi);
      // Add "Remove Filter" action
      if (parent.window.getFilters().anyFilters()) {
        popup.add(parent.window.getFilters().removeFilterMenuItem());
      }
    }

    if (type == AnDisplay.DSP_Heap) {
      if (AnWindow.getInstance().getHeapView().getDisplayMode() == AnDisplay.DSP_Heap) {
        // Add "Set Filter: Active Allocation Stacks Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_WITH_SELECTED_HEAPACTIVESTACK_SHORT_NAME,
                    FILTER_WITH_SELECTED_HEAPACTIVESTACK_LONG_NAME,
                    FILTER_WITH_SELECTED_HEAPACTIVESTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_WITH_SELECTED_HEAPACTIVESTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Allocations That Leaked ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_SHORT_NAME,
                    FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_LONG_NAME,
                    FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_WITH_SELECTED_HEAPLEAKEDSTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Allocations That Not Leaked ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_SHORT_NAME,
                    FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_LONG_NAME,
                    FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_NOT_SELECTED_HEAPLEAKEDSTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Selected Stack Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_WITH_SELECTED_HEAPSTACK_SHORT_NAME,
                    FILTER_WITH_SELECTED_HEAPSTACK_LONG_NAME,
                    FILTER_WITH_SELECTED_HEAPSTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_WITH_SELECTED_HEAPSTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
        // Add "Set Filter: Exclude Selected Stack Only ..." action
        mi =
            new JMenuItem(
                new FilterUpdateAction(
                    viewName,
                    FILTER_NOT_SELECTED_HEAPSTACK_SHORT_NAME,
                    FILTER_NOT_SELECTED_HEAPSTACK_LONG_NAME,
                    FILTER_NOT_SELECTED_HEAPSTACK_ACTION_NAME));
        ac = mi.getAccessibleContext();
        ac.setAccessibleDescription(FILTER_NOT_SELECTED_HEAPSTACK_LONG_NAME);
        mi.setEnabled(row_selected);
        popup.add(mi);
      }
      // Add "Manage Filters..." action
      txt = STR_ACTION_CUSTOM_FILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);
      if (filterOnly) {
        popup.addSeparator();
      }
      // Add "Undo Last Filter ..." action
      txt = STR_ACTION_UNDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canUndoLastFilter());
      popup.add(mi);
      // Add "Redo Last Filter ..." action
      txt = STR_ACTION_REDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRedoLastFilter());
      popup.add(mi);
      // Add "Set Filter: Reset Default Filter ..." action
      txt = STR_ACTION_REMOVE_ALL_FILTERS;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRemoveAllFilters());
      popup.add(mi);
      // Add "Remove Filter" action
      if (parent.window.getFilters().anyFilters()) {
        popup.add(parent.window.getFilters().removeFilterMenuItem());
      }
    }

    if ((type == AnDisplay.DSP_IndexObject) || (type == AnDisplay.DSP_MemoryObject)) {
      // Threads, CPUs, Samples, Seconds tabs
      if (popup.getComponentCount() > 0) {
        popup.addSeparator();
      }
      // Add "Set Filter: Selected Objects Only ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_WITH_SELECTED_ITEMS_SHORT_NAME,
                  FILTER_WITH_SELECTED_ITEMS_LONG_NAME,
                  FILTER_WITH_SELECTED_ITEMS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_WITH_SELECTED_ITEMS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);
      // Add "Set Filter: Exclude Selected Objects ..." action
      mi =
          new JMenuItem(
              new FilterUpdateAction(
                  viewName,
                  FILTER_WITHOUT_SELECTED_ITEMS_SHORT_NAME,
                  FILTER_WITHOUT_SELECTED_ITEMS_LONG_NAME,
                  FILTER_WITHOUT_SELECTED_ITEMS_ACTION_NAME));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(FILTER_WITHOUT_SELECTED_ITEMS_LONG_NAME);
      mi.setEnabled(row_selected);
      popup.add(mi);

      if (viewName.equals("Threads")) {
        boolean jthread_selected = false;
        int namecol = getTableModel().getNameCol();
        for (int selrow : anTable.getSelectedRows()) {
          String name = (String) getTableModel().data[namecol][selrow];
          if (name.contains("JThread")) {
            jthread_selected = true;
            break;
          }
        }

        if (jthread_selected) {
          // Add "Set Filter: Include only JThreads in selected group ..." action
          mi =
              new JMenuItem(
                  new FilterUpdateAction(
                      viewName,
                      FILTER_WITH_SELECTED_JGROUP_SHORT_NAME,
                      FILTER_WITH_SELECTED_JGROUP_LONG_NAME,
                      FILTER_WITH_SELECTED_JGROUP_ACTION_NAME));
          ac = mi.getAccessibleContext();
          ac.setAccessibleDescription(FILTER_WITH_SELECTED_JGROUP_LONG_NAME);
          mi.setEnabled(row_selected);
          popup.add(mi);

          // Add "Set Filter: Include only JThreads in selected parent ..." action
          mi =
              new JMenuItem(
                  new FilterUpdateAction(
                      viewName,
                      FILTER_WITH_SELECTED_JPARENT_SHORT_NAME,
                      FILTER_WITH_SELECTED_JPARENT_LONG_NAME,
                      FILTER_WITH_SELECTED_JPARENT_ACTION_NAME));
          ac = mi.getAccessibleContext();
          ac.setAccessibleDescription(FILTER_WITH_SELECTED_JPARENT_LONG_NAME);
          mi.setEnabled(row_selected);
          popup.add(mi);
        }
      }

      // Add "Manage Filters..." action
      txt = STR_ACTION_CUSTOM_FILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(true);
      popup.add(mi);
      if (filterOnly) {
        popup.addSeparator();
      }
      // Add "Undo Last Filter ..." action
      txt = STR_ACTION_UNDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canUndoLastFilter());
      popup.add(mi);
      // Add "Redo Last Filter ..." action
      txt = STR_ACTION_REDOFILTER;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRedoLastFilter());
      popup.add(mi);
      // Add "Set Filter: Reset Default Filter ..." action
      txt = STR_ACTION_REMOVE_ALL_FILTERS;
      mi = new JMenuItem(new UpdateAction(txt));
      ac = mi.getAccessibleContext();
      ac.setAccessibleDescription(txt);
      mi.setEnabled(parent.window.getFilters().canRemoveAllFilters());
      popup.add(mi);
      // Add "Remove Filter" action
      if (parent.window.getFilters().anyFilters()) {
        popup.add(parent.window.getFilters().removeFilterMenuItem());
      }
      //                if (filterOnly) {
      //                    popup.addSeparator();
      //                }
    }
  }

  // ------- Private classes to implement popup menu items ------- //
  protected class AnMenuListener extends MouseAdapter {

    private boolean debug;
    private AnTable anTable;

    AnMenuListener(AnTable _anTable) {
      anTable = _anTable;
      debug = false;
    }

    // Experimental code, mostly "quick and dirty hack"
    public JPopupMenu initPopup(MouseEvent event) {
      // Experimental code
      if (parent != null) {
        if (parent instanceof CallerCalleesView) {
          try {
            parent.changeSelection(anTable, table.rowAtPoint(event.getPoint()));
          } catch (Exception exc) {
            System.out.println("AnTable.init_popup() exception: " + exc);
            exc.printStackTrace();
          }
          setSelectedRowNow(table.rowAtPoint(event.getPoint()));
        }
      }
      return anTable.initPopup(false);
    }
    /*
    private JMenuItem menuItem(String txt, boolean enbl) {
    UpdateAction act = new UpdateAction(txt);
    JMenuItem mi = new JMenuItem(act);
    AccessibleContext ac = mi.getAccessibleContext();
    ac.setAccessibleDescription(txt);
    mi.setEnabled(enbl);
    return mi;
    }
    */

    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        showPopup(e);
      }
    }

    public void showPopup(MouseEvent e) {
      if (!Analyzer.getInstance().normalSelection) {
        int row = table.rowAtPoint(e.getPoint());
        if (row != -1 && !table.isRowSelected(row)) {
          setSelectedRow(row);
          fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, row, null));
        }
      }
      JPopupMenu popup = initPopup(e);
      if (popup != null) {
        popup.show(e.getComponent(), e.getX(), e.getY());
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

  class SortMenu extends AbstractAction {

    private int colNum;

    public SortMenu(int colNum, String txt) {
      super(txt);
      this.colNum = colNum;
    }

    public SortMenu(int colNum) {
      this.colNum = colNum;
    }

    public void actionPerformed(ActionEvent ev) {
      if (colNum < tableModel.getColumnCount()) {
        sortTable(colNum);
      }
    }
  }

  /*
   * Generic action for context menu items.
   * Action name is passed as String.
   */
  class UpdateAction extends AbstractAction {

    String actionName = null;

    public UpdateAction(String txt) {
      super(txt);
      actionName = txt;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      Object progressBarHandle =
          parent
              .window
              .getSystemProgressPanel()
              .progressBarStart(AnLocale.getString("Table Update"));
      updateAnTable(actionName);
      parent.window.getSystemProgressPanel().progressBarStop(progressBarHandle);
    }

    public void tailFunction() {}
  }

  class FilterUpdateAction extends AbstractAction {

    String viewName;
    String shortName;
    String longName;
    String actionName;

    public FilterUpdateAction(
        String viewName, String shortName, String longName, String actionName) {
      super(ADD_FILTER + ": " + longName);
      this.viewName = viewName;
      this.shortName = shortName;
      this.longName = longName;
      this.actionName = actionName;
    }

    public void actionPerformed(ActionEvent ev) {
      //            parent.window.setBusyCursor(true);
      AnUtility.checkIPCOnWrongThread(false);
      updateTable(viewName + ": " + shortName, longName, actionName);
      AnUtility.checkIPCOnWrongThread(true);
      //            parent.window.setBusyCursor(false);
    }
  }

  /*
   * Action for context menu items of source line navigation.
   * Action name is passed as String.
   */
  class SrcLineUpdateAction extends UpdateAction {

    String actionName = null;
    int row = -1;

    public SrcLineUpdateAction(String txt, int r) {
      super(txt);
      actionName = txt;
      row = r;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      Object progressBarHandle =
          parent
              .window
              .getSystemProgressPanel()
              .progressBarStart(AnLocale.getString("Source Line"));
      // change selected object
      parent.window.getSelectedObject().setSelObj(row, parent.type, parent.subtype);
      updateAnTable(actionName);
      parent.window.getSystemProgressPanel().progressBarStop(progressBarHandle);
    }
  }

  /*
   * Action for context menu items of function navigation.
   * Action name is passed as String.
   */
  class NavigationUpdateAction extends UpdateAction {

    protected String actionName = null;
    protected long functionId = -1;
    protected int lineNo = -1;

    public NavigationUpdateAction(String txt, long id, int no) {
      super(txt);
      actionName = txt;
      functionId = id;
      lineNo = no;
    }

    public void actionPerformed(ActionEvent ev) {
      navHistoryPool.getHistory().enabled = true;

      // change selected object
      parent.window.getSelectedObject().setSelObjV2(functionId);
      parent.setNavigationAction(this);
      parent.setComputed(false);
      parent.computeOnAWorkerThread();
    }

    public void tailFunction() {
      if ((type == AnDisplay.DSP_Source) || (type == AnDisplay.DSP_SourceV2)) {
        if (lineNo != -1) { // -1 is for callee, but here is for caller
          int base_row = table.getSelectedRow();
          int row = srcRenderer.getRowByLineNo(lineNo);
          if (row != -1) {
            navHistoryPool.getHistory().enabled = false;
            anTable.setSelectedRow(row);
            navHistoryPool.getHistory().enabled = true;
            anTable.fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, row, null));
            // adjust history
            SelObjInfo back_obj = navHistoryPool.getHistory().goBack();
            if (back_obj != null && back_obj.id == functionId && back_obj.lineno == lineNo) {
              navHistoryPool.getHistory().goBack();
              SelObjInfo new_obj = new SelObjInfo(back_obj.id, back_obj.lineno, back_obj.name);
              navHistoryPool.getHistory().goToNew(new_obj);
            } else {
              navHistoryPool.getHistory().goForward();
              SrcRenderer.SrcTextMarker sm =
                  srcRenderer.srcRendered.get(srcRenderer.mapRow(base_row));
              int base_lineNo = sm == null ? -1 : sm.lineNo;
              SelObjInfo cur_obj = navHistoryPool.getHistory().getCurrent();
              if (navHistoryPool.getHistory().newAdded == false
                  && cur_obj != null
                  && cur_obj.id == functionId
                  && cur_obj.lineno == base_lineNo) {
                SelObjInfo new_obj = new SelObjInfo(cur_obj.id, lineNo, cur_obj.name);
                navHistoryPool.getHistory().goToNew(new_obj);
              } else {
                cur_obj.lineno = lineNo;
              }
            }
          }
        }
      } else if ((type == AnDisplay.DSP_Disassembly) || (type == AnDisplay.DSP_DisassemblyV2)) {
        if (lineNo != -1) { // -1 is for callee, but here is for caller
          int base_row = table.getSelectedRow();
          String baseAddrStr = disRenderer.getRowToAddr(base_row, base_row);
          if (baseAddrStr != null) {
            Long callsiteAddr = Long.parseLong(baseAddrStr, 16);
            callsiteAddr += lineNo; // lineNo is actually offset inside function for disasm view
            Integer callsite = disRenderer.getAddrToRow(Long.toHexString(callsiteAddr), base_row);
            if (callsite != null) {
              navHistoryPool.getHistory().enabled = false;
              anTable.setSelectedRow(callsite);
              navHistoryPool.getHistory().enabled = true;
              anTable.fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, callsite, null));
              // adjust history
              SelObjInfo back_obj = navHistoryPool.getHistory().goBack();
              if (back_obj != null && back_obj.id == functionId && back_obj.lineno == callsite) {
                navHistoryPool.getHistory().goBack();
                SelObjInfo new_obj = new SelObjInfo(back_obj.id, back_obj.lineno, back_obj.name);
                navHistoryPool.getHistory().goToNew(new_obj);
              } else {
                navHistoryPool.getHistory().goForward();
                SelObjInfo cur_obj = navHistoryPool.getHistory().getCurrent();
                if (navHistoryPool.getHistory().newAdded == false
                    && cur_obj != null
                    && cur_obj.id == functionId
                    && cur_obj.lineno == base_row) {
                  SelObjInfo new_obj =
                      new SelObjInfo(cur_obj.id, callsite, Long.toHexString(callsiteAddr));
                  navHistoryPool.getHistory().goToNew(new_obj);
                } else {
                  cur_obj.lineno = callsite;
                  cur_obj.name = Long.toHexString(callsiteAddr);
                }
              }
            }
          }
        }
      }
      updateAnTable(actionName);
    }
  }

  class NavigationHistoryUpdateAction extends NavigationUpdateAction {

    private String addr;

    public NavigationHistoryUpdateAction(String txt, long id, int no, String a) {
      super(txt, id, no);
      addr = a;
    }

    public void actionPerformed(ActionEvent ev) {
      boolean inError = anTable.getViewport().getView() != table;

      if (actionName.equalsIgnoreCase(STR_ACTION_BACK)) {
        if (navHistoryPool.getHistory().canGoBack() && !inError) {
          navHistoryPool.getHistory().goBack();
        }
      }
      if (actionName.equalsIgnoreCase(STR_ACTION_FORWARD)) {
        if (navHistoryPool.getHistory().canGoForward()) {
          navHistoryPool.getHistory().goForward();
        }
      }

      boolean done = false;
      navHistoryPool.getHistory().enabled = false;
      if (!inError
          && ((type == AnDisplay.DSP_Disassembly) || (type == AnDisplay.DSP_DisassemblyV2))) {
        int sel_row = anTable.getSelectedRow();
        long fun_id = -1;
        if (sel_row >= 0 && sel_row < disRenderer.functionId.size()) {
          fun_id = disRenderer.functionId.get(sel_row);
        }
        if (fun_id == functionId) {
          Integer targetRowObj = disRenderer.getAddrToRow(addr, sel_row);
          if (targetRowObj != null) {
            int targetRow = targetRowObj.intValue();
            setSelectedRow(targetRow);
            fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, targetRow, null));
            done = true;
          }
        }
      }
      if (!inError && !done) {
        if ((type == AnDisplay.DSP_Disassembly) || (type == AnDisplay.DSP_DisassemblyV2)) {
          Integer targetRowObj = disRenderer.getAddrToRow(addr, selectedRow);
          if (targetRowObj != null) {
            int targetRow = targetRowObj.intValue();
            long fun_id = -1;
            if (targetRow >= 0 && targetRow < disRenderer.functionId.size()) {
              fun_id = disRenderer.functionId.get(targetRow);
            }
            if (fun_id == functionId) {
              setSelectedRow(targetRow);
              fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, targetRow, null));
              done = true;
            }
          }
        }
      }
      if (done) {
        parent.updateToolBar(); // only for dis view
        navHistoryPool.getHistory().enabled = true;
      }
      if (!done) {
        // change selected object
        if (type == AnDisplay.DSP_Functions) {
          parent.window.getSelectedObject().setSelObj(lineNo, parent.type, parent.subtype);
        } else {
          parent.window.getSelectedObject().setSelObjV2(functionId);
        }
        parent.setNavigationAction(this);
        parent.setComputed(false);
        parent.computeOnAWorkerThread();
      }
    }

    @Override
    public void tailFunction() {
      if ((type == AnDisplay.DSP_Source) || (type == AnDisplay.DSP_SourceV2)) {
        if (lineNo != -1) {
          int row = srcRenderer.getRowByLineNo(lineNo);
          if (row != -1) {
            anTable.setSelectedRow(row);
            anTable.fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, row, null));
          }
        }
      } else if ((type == AnDisplay.DSP_Disassembly) || (type == AnDisplay.DSP_DisassemblyV2)) {
        int row = table.getSelectedRow();
        for (int i = row; i < anTable.getRowCount(); i++) {
          String baseAddrStr = disRenderer.getRowToAddr(i, i);
          if ((baseAddrStr != null && baseAddrStr.equalsIgnoreCase(addr)) || lineNo == i) {
            anTable.setSelectedRow(i);
            anTable.fireAnEvent(new AnEvent(anTable, AnEvent.EVT_SELECT, i, null));
            break;
          }
        }
      }
      updateAnTable(actionName);
      navHistoryPool.getHistory().enabled = true;
    }
  }

  static class SelObjInfo implements Comparable<SelObjInfo> {

    long id; // function id
    int lineno; // for callsite dis, lineno is instr offset in one function
    // for history dis, lineno is row number
    String name; // for history dis data, name is address if lineno is -1

    public SelObjInfo(long idp, int no, String namep) {
      id = idp;
      lineno = no;
      name = namep;
    }

    public int compareTo(SelObjInfo o) {
      return this.name.compareTo(o.name);
    }
  }

  class NavigationHistoryPool {

    private ArrayList<HashMap<String, NavigationHistory>> historyPool = null;

    public NavigationHistoryPool() {
      historyPool = new ArrayList<HashMap<String, NavigationHistory>>();
      for (int i = 0; i < 3; i++) {
        historyPool.add(new HashMap<String, NavigationHistory>());
      }

      for (int i = 0; i < 3; i++) {
        historyPool.get(i).put("", new NavigationHistory());
      }
    }

    public NavigationHistory getHistory() {
      int viewMode = parent.window.getSettings().getViewModeSetting().get().value();
      if (viewMode < 0 || viewMode > 2) {
        System.err.println("Error: wrong view mode!");
        return null;
      }

      String filterExp = parent.window.getFilters().getFilterStr(0);
      if (filterExp == null) {
        filterExp = "";
      }
      NavigationHistory ret = historyPool.get(viewMode).get(filterExp);
      if (ret == null) {
        historyPool.get(viewMode).put(filterExp, new NavigationHistory());
        ret = historyPool.get(viewMode).get(filterExp);
      }

      return ret;
    }

    public void clearAll() {
      for (int i = 0; i < 3; i++) {
        if (historyPool.get(i) != null) {
          historyPool.get(i).clear();
        }
      }
      for (int i = 0; i < 3; i++) {
        if (historyPool.get(i) != null) {
          historyPool.get(i).put("", new NavigationHistory());
        }
      }
    }
  }

  class NavigationHistory {

    private ArrayList<SelObjInfo> history;
    private int curPos;
    public boolean enabled;
    public boolean newAdded;

    public NavigationHistory() {
      curPos = -1;
      history = new ArrayList<SelObjInfo>();
      enabled = true;
      newAdded = false;
    }

    public void goToNew(SelObjInfo obj) {
      newAdded = true;
      curPos++;
      history.add(curPos, obj);
      int size = history.size();
      for (int i = size - 1; i > curPos; i--) {
        history.remove(i);
      }
      shrinkDuplicated();
    }

    public SelObjInfo goBack() {
      curPos--;
      if (curPos >= 0) {
        return history.get(curPos);
      }
      curPos = -1;
      return null;
    }

    public SelObjInfo goForward() {
      curPos++;
      if (curPos < history.size()) {
        return history.get(curPos);
      }
      curPos = history.size();
      return null;
    }

    public SelObjInfo getBack() {
      if (curPos > 0) {
        return history.get(curPos - 1);
      }
      return null;
    }

    public SelObjInfo getForward() {
      if (curPos < history.size() - 1) {
        return history.get(curPos + 1);
      }
      return null;
    }

    public boolean canGoBack() {
      return (curPos > 0);
    }

    public boolean canGoForward() {
      return (curPos < history.size() - 1);
    }

    public SelObjInfo getCurrent() {
      if (curPos >= 0 && curPos < history.size()) {
        return history.get(curPos);
      }
      return null;
    }

    public void goToFuncNew(int row) {
      SelObjInfo cur_so = navHistoryPool.getHistory().getCurrent();
      if (cur_so == null || cur_so.lineno != row) {
        SelObjInfo new_so = new SelObjInfo(0, row, "");
        navHistoryPool.getHistory().goToNew(new_so);
      }
    }

    public void goToSrcNew(int row) {
      if (srcRenderer == null) {
        return;
      }
      long fun_id = -1;
      if (row >= 0 && row < srcRenderer.functionId.size()) {
        fun_id = srcRenderer.functionId.get(row);
      }
      if (fun_id == 0) {
        fun_id = parent.window.getSelectedObject().getSelObjV2("FUNCTION");
      }
      SrcRenderer.SrcTextMarker marker = srcRenderer.srcRendered.get(srcRenderer.mapRow(row));
      int lineNo = marker == null ? -1 : marker.lineNo;
      SelObjInfo cur_so = navHistoryPool.getHistory().getCurrent();
      if (cur_so == null || cur_so.id != fun_id || cur_so.lineno != lineNo) {
        SelObjInfo new_so = new SelObjInfo(fun_id, lineNo, "");
        navHistoryPool.getHistory().goToNew(new_so);
      }
    }

    public void goToDisNew(int row) {
      if (disRenderer == null) {
        return;
      }
      long fun_id = -1;
      if (row >= 0 && row < disRenderer.functionId.size()) {
        fun_id = disRenderer.functionId.get(row);
      }
      if (fun_id == 0) {
        fun_id = parent.window.getSelectedObject().getSelObjV2("FUNCTION");
      }
      SelObjInfo cur_so = navHistoryPool.getHistory().getCurrent();
      if (cur_so == null || cur_so.id != fun_id || cur_so.lineno != row) {
        String addr = disRenderer.getRowToAddr(row, row);
        addr = (addr == null) ? "" : addr;
        SelObjInfo new_so = new SelObjInfo(fun_id, row, addr);
        navHistoryPool.getHistory().goToNew(new_so);
      }
    }

    public void shrinkDuplicated() {
      SelObjInfo cur_so = getCurrent();
      if (canGoBack()) {
        SelObjInfo back_so = getBack();
        if (back_so.id == cur_so.id && back_so.lineno == cur_so.lineno) {
          int i = curPos;
          while (i < history.size() - 1) {
            history.set(i, history.get(i + 1));
            i++;
          }
          history.remove(i);
          curPos--;
        }
      }

      if (canGoForward()) {
        SelObjInfo next_so = getForward();
        if (next_so.id == cur_so.id && next_so.lineno == cur_so.lineno) {
          int i = curPos;
          while (i < history.size() - 1) {
            history.set(i, history.get(i + 1));
            i++;
          }
          history.remove(i);
        }
      }
    }

    public void goToSelected(int org_row, boolean isNew) {
      if ((type == AnDisplay.DSP_Source) || (type == AnDisplay.DSP_SourceV2)) {
        if (isNew) {
          goToSrcNew(org_row);
          parent.updateToolBar();
        } else {
          SelObjInfo sel_obj = navHistoryPool.getHistory().getCurrent();
          Long funcId = null;
          if ((org_row >= 0)
              && (srcRenderer.functionId != null)
              && (org_row < srcRenderer.functionId.size())) {
            funcId = srcRenderer.functionId.get(org_row);
          }
          if (funcId == null) {
            funcId = parent.window.getSelectedObject().getSelObjV2("FUNCTION");
          }
          SrcRenderer.SrcTextMarker sm = srcRenderer.srcRendered.get(srcRenderer.mapRow(org_row));
          int lineNo = -1;
          if (sel_obj != null
              && funcId != null
              && funcId.longValue() != 0
              && sm != null) { // XXXX ugly assumption that 0 function id is invalid
            lineNo = sm.lineNo;
            sel_obj.id = funcId;
            sel_obj.lineno = lineNo;
            sel_obj.name = "";
            shrinkDuplicated();
          }
        }
      } else if ((type == AnDisplay.DSP_Disassembly) || (type == AnDisplay.DSP_DisassemblyV2)) {
        if (isNew) {
          goToDisNew(org_row);
          parent.updateToolBar();
        } else {
          SelObjInfo sel_obj = navHistoryPool.getHistory().getCurrent();
          if (sel_obj != null) {
            Long funcId = null;
            if (org_row >= 0 && org_row < disRenderer.functionId.size()) {
              funcId = disRenderer.functionId.get(org_row);
            }
            if (funcId == null) {
              funcId = parent.window.getSelectedObject().getSelObjV2("FUNCTION");
            }
            String addr = disRenderer.getRowToAddr(org_row, org_row);
            if (funcId != null
                && funcId.longValue() != 0) { // XXXX ugly assumption that 0 function id is invalid
              sel_obj.id = funcId;
              sel_obj.lineno = org_row;
              sel_obj.name = addr;
              shrinkDuplicated();
            }
          }
        }
      } else if (type == AnDisplay.DSP_Functions) {
        if (isNew) {
          goToFuncNew(org_row);
          parent.updateToolBar();
        } else {
          SelObjInfo sel_obj = navHistoryPool.getHistory().getCurrent();
          if (sel_obj != null) {
            sel_obj.id = 0;
            sel_obj.lineno = org_row;
            sel_obj.name = "";
            shrinkDuplicated();
          }
        }
      }
    }

    public void clear() {
      history.clear();
      curPos = -1;
    }
  }

  public void setGroupId(int id) {
    groupId = id;
  }

  public int getGroupId() {
    return groupId;
  }
}
