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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnTable.UpdateAction;
import org.gprofng.mpmt.AnWindow.AnDispTab;
import org.gprofng.mpmt.Analyzer.HelpAction;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.filter.CustomFilterAction;
import org.gprofng.mpmt.filter.RedoFilterAction;
import org.gprofng.mpmt.filter.RemoveAllFilterAction;
import org.gprofng.mpmt.filter.UndoFilterAction;
import org.gprofng.mpmt.ipc.IPCCancelledException;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

public abstract class AnDisplay extends JPanel {

  public final AnWindow window;
  private static JFrame jframe;

  protected final int type;
  protected final int subtype;
  protected int parent_type = -1;

  protected boolean available; // true if view is available
  protected boolean selected; // true if view is selected and showing
  protected boolean computed; // true if view's data has been computed and is up-to-date
  protected UpdateAction tailAction;
  protected boolean inCompute;
  protected boolean forceCompute;
  protected final boolean can_sort;

  public AnHotGap gap;
  private String help_id;
  private AnDispTab anDispTab;

  // Constructor
  protected AnDisplay(final AnWindow window, final int type, final String help_id) {
    this(window, type, 0, help_id);
  }

  protected AnDisplay(
      final AnWindow window, final int type, final int subtype, final String help_id) {
    this.window = window;
    jframe = window.getFrame();
    this.type = type;
    this.subtype = subtype;
    this.help_id = help_id;
    selected = false;
    computed = false;
    inCompute = false;
    forceCompute = false;
    can_sort =
        ((type != DSP_Source)
            && (type != DSP_Disassembly)
            && (type != DSP_SourceV2)
            && (type != DSP_DisassemblyV2)); // FIXUP: REARCH

    tailAction = null;

    // Initialize GUI components & set preferred size
    initComponents();

    setHelpAction(help_id);

    setBorder(new LineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR, 0));
  }

  // Initialize GUI components
  protected abstract void initComponents();

  // Should be overridden if ...
  public JPopupMenu getFilterPopup() {
    return null;
  }

  //    abstract JPopupMenu getFilterPopup();
  public void showFilterPopup(Component parent) {
    JPopupMenu popup;
    popup = getFilterPopup();
    if (popup == null) {
      popup = getDefaultFilterPopup();
    }
    if (popup != null) {
      if (parent == null || !parent.isShowing()) {
        parent = this;
      }
      popup.show(parent, 15, 15);
    }
  }

  private void setHelpAction(final String help_id) {
    //        if (help_id != null) {
    HelpAction helpAction = new HelpAction(help_id);
    registerKeyboardAction(
        helpAction,
        "help",
        KeyboardShortcuts.helpActionShortcut,
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    registerKeyboardAction(
        helpAction, "help", KeyboardShortcuts.helpActionShortcut, JComponent.WHEN_FOCUSED);
    registerKeyboardAction(
        helpAction,
        "help",
        KeyboardShortcuts.helpActionShortcut,
        JComponent.WHEN_IN_FOCUSED_WINDOW);
    //        }
  }

  public void showHelp() {
    Analyzer.showHelp(help_id);
  }

  public String getHelpID() {
    return help_id;
  }

  // Display type
  public final int getType() {
    return type;
  }

  // Is current selected tab?
  public void setSelected(final boolean set) {
    selected = set;
  }

  public final boolean isSelected() {
    return selected;
  }

  // Need re-compute?
  public void setComputed(final boolean set) {
    computed = set;
  }

  public final boolean isComputed() {
    return computed;
  }

  // Clear display
  protected void clear() { // FIXUP: RE_ARCH
  }

  // Clear navigation history
  protected void clearHistory() { // FIXUP: RE_ARCH
  }

  public void setNavigationAction(final UpdateAction action) {
    tailAction = action;
  }

  // Update view specific tool bar
  public void updateToolBar() {}

  // sync src&dis view
  public void syncSrcDisWin() {}

  // Reset display
  protected void reset() {
    clear();
  }

  public final void computeOnAWorkerThread() {
    computeOnAWorkerThread(null, null);
  }

  /**
   * Do long-running task in a separate thread
   *
   * @param context
   */
  public final void computeOnAWorkerThread(
      final IPCContext context, final AbstractAction postComputeAction) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            compute(context, postComputeAction);
          }
        },
        "Display_thread");
    ;
  }

  public void compute(final IPCContext context, final AbstractAction postComputeAction) {
    synchronized (AnVariable.mainFlowLock) {
      IPCContext currentContext = context;
      if (currentContext == null) {
        currentContext =
            IPCContext.newCurrentContext(null, IPCContext.Scope.SESSION, false, window);
      }
      //          System.out.println("\n============= AnDisplay compute Start" +
      // currentContext.getTaskName() + "  Channel: " + currentContext.getChannel());
      Object progressBarHandle =
          window.getSystemProgressPanel().progressBarStart(AnLocale.getString("Computing"));
      try { // in case there will be an exception inside doCompute
        doCompute(); // on worker thread and synchronized (AnVariable.mainFlowLock)
      } catch (IPCCancelledException ex) {
        System.out.println("\ncompute cancelled...");
        inCompute = false;
        computed = false;
      } finally {
        currentContext =
            IPCContext.newCurrentContext(null, IPCContext.Scope.SESSION, false, window);
        //              System.out.println("\n============= AnDisplay compute End" +
        // currentContext.getTaskName() + "  Channel: " + currentContext.getChannel());
      }
      if (postComputeAction != null) {
        postComputeAction.actionPerformed(null);
      }
      window.getSystemProgressPanel().progressBarStop(progressBarHandle);
    }
  }

  /*
   * Change selection
   */
  public void changeSelection(AnTable table, int index) {
    return;
  }

  /**
   * Override and return true if text find is supported
   *
   * @return
   */
  protected boolean supportsFindText() {
    return false;
  }

  // Find
  public int find(final String str, final boolean next, boolean caseSensitive) {
    return -1;
  }

  // Check if "Back" action is available
  public boolean isBackActionAvailable() {
    return false;
  }

  // Check if "Forward" action is available
  public boolean isForwardActionAvailable() {
    return false;
  }

  // Compute & update display
  public abstract void doCompute();

  public String exportAsText(
      Integer limit, ExportSupport.ExportFormat format, Character delimiter) {
    String text = "not implemented";
    return text;
  }

  // Display error string on the status area
  protected final void showError(final String msg) {
    window.showError(msg);
  }

  // Display warning string on the status area
  protected final void showWarning(final String msg) {
    window.showWarning(msg);
  }

  /**
   * @return the anDispTab
   */
  public AnDispTab getAnDispTab() {
    return anDispTab;
  }

  /**
   * @param anDispTab the anDispTab to set
   */
  public void setAnDispTab(AnDispTab anDispTab) {
    this.anDispTab = anDispTab;
  }

  public JPopupMenu getDefaultFilterPopup() {
    List<JComponent> menuItems = getDefaultFilterMenuItems();
    JPopupMenu popup = new JPopupMenu();
    for (JComponent menuItem : menuItems) {
      popup.add(menuItem);
    }
    return popup;
  }

  public List<JComponent> getDefaultFilterMenuItems() {
    List<JComponent> menuItems = new ArrayList<>();
    JMenuItem menuItem;

    CustomFilterAction customFilter = new CustomFilterAction();
    menuItem = new JMenuItem(customFilter);
    customFilter.jmi = menuItem;
    menuItem.setEnabled(true);
    menuItems.add(menuItem);

    menuItems.add(new JSeparator(SwingConstants.HORIZONTAL));

    UndoFilterAction undoFilter = new UndoFilterAction();
    menuItem = new JMenuItem(undoFilter);
    undoFilter.jmi = menuItem;
    menuItem.setEnabled(window.getFilters().canUndoLastFilter());
    menuItems.add(menuItem);

    RedoFilterAction redoFilter = new RedoFilterAction();
    menuItem = new JMenuItem(redoFilter);
    redoFilter.jmi = menuItem;
    menuItem.setEnabled(window.getFilters().canRedoLastFilter());
    menuItems.add(menuItem);

    RemoveAllFilterAction removeAllFilter = new RemoveAllFilterAction();
    menuItem = new JMenuItem(removeAllFilter);
    removeAllFilter.jmi = menuItem;
    menuItem.setEnabled(window.getFilters().canRemoveAllFilters());
    menuItems.add(menuItem);

    if (window.getFilters().anyFilters()) {
      menuItems.add(window.getFilters().removeFilterMenuItem());
    }

    return menuItems;
  }

  public List<Subview> getVisibleSubviews() {
    return new ArrayList<>();
  }

  public List<Subview> getSelectedSubviews() {
    return null;
  }

  public JPanel getToolbarPanel() {
    return null;
  }

  protected void setAccessibility(String name) {
    AccessibleContext context = getAccessibleContext();
    context.setAccessibleName(name);
    context.setAccessibleDescription(name);
  }

  /**
   * @return the available
   */
  protected boolean isAvailable() {
    return available;
  }

  /**
   * @param available the available to set
   */
  protected void setAvailable(boolean available) {
    this.available = available;
  }

  /**
   * Some views show only 'Values' and not Exclusive and Inclusive in tables.
   * Fixup: they come in as 'Exclusive' but are not really 'Exclusive'.
   * Should be fixed in gp-display-text
   *
   * @return
   */
  public boolean showOnlyValuesInTables() {
    return type == DSP_MemoryObject
        || type == DSP_DataObjects
        || type == DSP_DataLayout
        || type == DSP_IndexObject
        || type == DSP_IO
        || type == DSP_IOFileDescriptors
        || type == DSP_IOCallStacks
        || type == DSP_Heap;
  }

  /**
   * Some views show only 'Included' and not Exclusive and Inclusive in tables.
   * Fixup: Should be in gp-display-text.
   *
   * @return
   */
  public boolean showOnlyIncludedInTables() {
    return type == DSP_Source || type == DSP_Disassembly || type == DSP_SourceDisassembly;
  }

  /**
   * Some views show only 'Attributed' and not Exclusive and Inclusive in tables.
   * Fixup: they come in as 'Exclusive' but are not really 'Exclusive'.
   * Should be fixed in gp-display-text
   *
   * @return
   */
  public boolean showOnlyAttributedInTables() {
    return type == DSP_Callers;
  }

  protected void debug() {
    if (getAnDispTab() != null) {
      System.out.println("AnDisplay: " + getAnDispTab().getTCmd());
    } else {
      System.out.println("AnDisplay: " + getClass().getName());
    }
    System.out.println("  available: " + available);
    System.out.println("  selected: " + selected);
    System.out.println("  computed: " + computed);
  }

  /*------ Constants shared with enums.h (KEEP IN SYNC!)  ---------------*/
  public static final int DSP_Welcome = -3; // not used by DBE, not in enums.h
  public static final int DSP_Null = 0; // ???
  public static final int DSP_Functions = 1;
  public static final int DSP_Lines = 2;
  public static final int DSP_PCs = 3;
  public static final int DSP_Source = 4;
  public static final int DSP_Disassembly = 5;
  public static final int DSP_CallerCalleeSelf = 6;
  public static final int DSP_Callers = 7;
  public static final int DSP_Callees = 8;
  public static final int DSP_CallTree = 9;
  public static final int DSP_Timeline = 10;
  public static final int DSP_Statistics = 11;
  public static final int DSP_Experiments = 12;
  public static final int DSP_MemoryObject = 14;
  public static final int DSP_DataObjects = 15;
  public static final int DSP_DataLayout = 16;
  public static final int DSP_SourceSelectedObject = 17;
  public static final int DSP_InstructionFrequency = 18;
  public static final int DSP_Races = 19;
  public static final int DSP_IndexObject = 20;
  public static final int DSP_DualSource = 21;
  public static final int DSP_SourceDisassembly = 22;
  public static final int DSP_Deadlocks = 23;
  public static final int DSP_MPITimeline = 24;
  public static final int DSP_MPIChart = 25;
  public static final int DSP_SourceV2 = 27;
  public static final int DSP_DisassemblyV2 = 28;
  public static final int DSP_IO = 31;
  public static final int DSP_Overview = 32;
  public static final int DSP_IOFileDescriptors = 33;
  public static final int DSP_IOCallStacks = 34;
  public static final int DSP_MiniFunctions = 35;
  public static final int DSP_MiniCallerCalleeSelf = 36;
  public static final int DSP_MiniCaller = 37;
  public static final int DSP_MiniCallee = 38;
  public static final int DSP_Heap = 39;
  public static final int DSP_CallFlame = 40;

  public static final int DSP_MAX = DSP_CallFlame; // Always the highest number
  /*

   By Type:

          AnDisplay
      Type Family Variable          User Name                Internal Name
      ---- ------ ----------------- ------------------------ -----------------
       -3     0   DSP_WELCOME       Welcome                  welcome
        1     0   DSP_FUNCTION      Functions                functions
        2     0   DSP_LINE          Lines                    lines
        3     0   DSP_PC            PCs                      pcs
        4     7   DSP_SOURCE        Source                   source
        5     7   DSP_DISASM        Disassembly              disasm
        6     0   DSP_SELF          <Slave View>
        7     1   DSP_CALLER        Callers-Callees          callers-callees
        8     0   DSP_CALLEE        <Slave View>
        9     4   DSP_CALLTREE      Call Tree                calltree
       10     0   DSP_TIMELINE      Timeline                 timeline
       11     0   DSP_STATIS        Statistics               statistics
       12     0   DSP_EXP           Experiments              header
       13
       14     2   DSP_MEMOBJ        Generic Memory Object    Views
       15     2   DSP_DATAOBJ       Data Objects             data_objects
       16     2   DSP_DLAYOUT       Data Layout              data_layout
       17     0   DSP_SRC_FILE      ***Not used anymore ???***
       18     0   DSP_IFREQ         Instruction Frequency    ifreq
       19     0   DSP_RACES         Races                    races
       20     3   DSP_INDXOBJ       Generic Index Object Views
       21     0   DSP_RACESOURCE    Dual Source              dsrc
       22     0   DSP_SOURCE_DISASM Source/Disassembly       srcdis
       23     0   DSP_DEADLOCKS     Deadlocks                deadlocks
       24     0   DSP_MPVIEW_TL     MPI Timeline             mpi_timeline
       25     0   DSP_MPVIEW_CHART  MPI Chart                mpi_chart
       26
       27     7   DSP_SOURCE_V2     ***Not used anymore ???***
       28     7   DSP_DISASM_V2     ***Not used anymore ???***
       29
       30
       31     6   DSP_IOACTIVITY    I/O                      ioactivity
       32     0   DSP_OVERVIEW      Overview                 overview
       33     6   DSP_IOVFD         ***Not used anymore ???***
       34     6   DSP_IOCALLSTACK   ***Not used anymore ???***
       35     0   DSP_MINIFUNCTION  <Slave View>
       36     0   DSP_MINISELF      <Slave View>
       37     0   DSP_MINICALLER    <Slave View>
       38     0   DSP_MINICALLEE    <Slave View>
       39     8   DSP_HEAP          Heap                     heap


   By Family:

          AnDisplay
      Type Family Variable          User Name                Internal Name
      ---- ------ ----------------- ------------------------ -----------------
       -3     0   DSP_WELCOME       Welcome                  welcome
        1     0   DSP_FUNCTION      Functions                functions
        2     0   DSP_LINE          Lines                    lines
        3     0   DSP_PC            PCs                      pcs
        6     0   DSP_SELF          <Slave View>
        8     0   DSP_CALLEE        <Slave View>
       10     0   DSP_TIMELINE      Timeline                 timeline
       11     0   DSP_STATIS        Statistics               statistics
       12     0   DSP_EXP           Experiments              header
       17     0   DSP_SRC_FILE      ***Not used anymore ???***
       18     0   DSP_IFREQ         Instruction Frequency    ifreq
       19     0   DSP_RACES         Races                    races
       21     0   DSP_RACESOURCE    Dual Source              dsrc
       22     0   DSP_SOURCE_DISASM Source/Disassembly       srcdis
       23     0   DSP_DEADLOCKS     Deadlocks                deadlocks
       24     0   DSP_MPVIEW_TL     MPI Timeline             mpi_timeline
       25     0   DSP_MPVIEW_CHART  MPI Chart                mpi_chart
       32     0   DSP_OVERVIEW      Overview                 overview
       35     0   DSP_MINIFUNCTION  <Slave View>
       36     0   DSP_MINISELF      <Slave View>
       37     0   DSP_MINICALLER    <Slave View>
       38     0   DSP_MINICALLEE    <Slave View>
        7     1   DSP_CALLER        Callers-Callees          callers-callees
       14     2   DSP_MEMOBJ        Generic Memory Object    Views
       15     2   DSP_DATAOBJ       Data Objects             data_objects
       16     2   DSP_DLAYOUT       Data Layout              data_layout
       20     3   DSP_INDXOBJ       Generic Index Object Views
        9     4   DSP_CALLTREE      Call Tree                calltree
       31     6   DSP_IOACTIVITY    I/O                      ioactivity
       33     6   DSP_IOVFD         ***Not used anymore ???***
       34     6   DSP_IOCALLSTACK   ***Not used anymore ???***
        4     7   DSP_SOURCE        Source                   source
        5     7   DSP_DISASM        Disassembly              disasm
       27     7   DSP_SOURCE_V2     ***Not used anymore ???***
       28     7   DSP_DISASM_V2     ***Not used anymore ???***
       39     8   DSP_HEAP          Heap                     heap
  */
}
