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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.collect.CollectDialog;
import org.gprofng.mpmt.collect.CollectPanel;
import org.gprofng.mpmt.collect.Collector;
import org.gprofng.mpmt.compare.CompareAdvancedDialog;
import org.gprofng.mpmt.compare.CompareSimpleDialog;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.experiment_props.Experiments;
import org.gprofng.mpmt.export.Export;
import org.gprofng.mpmt.export.ExportDialog;
import org.gprofng.mpmt.export.ExportSupport;
import org.gprofng.mpmt.export.ExportSupport.ExportFormat;
import org.gprofng.mpmt.filter.Filters;
import org.gprofng.mpmt.flame.FlameView;
import org.gprofng.mpmt.guitesting.GUITesting;
import org.gprofng.mpmt.ipc.IPCCancelledException;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.mainview.MainViewPanel;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.metrics.IPCMetricsAPI;
import org.gprofng.mpmt.metrics.MetricNode;
import org.gprofng.mpmt.navigation.NavigationPanel;
import org.gprofng.mpmt.navigation.View;
import org.gprofng.mpmt.navigation.ViewsPanel;
import org.gprofng.mpmt.overview.OverviewView;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.picklist.PickListElement;
import org.gprofng.mpmt.progress.SystemProgressPanel;
import org.gprofng.mpmt.remote.CloseExperimentDialog;
import org.gprofng.mpmt.remote.ConnectionDialog;
import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.settings.CompareModeSetting;
import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import org.gprofng.mpmt.settings.ExportSettingsDialog;
import org.gprofng.mpmt.settings.FormatSetting;
import org.gprofng.mpmt.settings.ImportSettingsDialog;
import org.gprofng.mpmt.settings.LibraryVisibilitySetting;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.settings.PathMapSetting;
import org.gprofng.mpmt.settings.SearchPathSetting;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.settings.TimelineSetting;
import org.gprofng.mpmt.settings.ViewModeEnabledSetting;
import org.gprofng.mpmt.settings.ViewModeSetting;
import org.gprofng.mpmt.settings.ViewModeSetting.ViewMode;
import org.gprofng.mpmt.settings.ViewsSetting;
import org.gprofng.mpmt.settings.ViewsSetting.CustomObject;
import org.gprofng.mpmt.statecolors.AnColorChooser;
import org.gprofng.mpmt.statuspanel.StatusHandleFactory;
import org.gprofng.mpmt.statuspanel.StatusLabelHandle;
import org.gprofng.mpmt.statuspanel.StatusLabelValueHandle;
import org.gprofng.mpmt.statuspanel.StatusPanel;
import org.gprofng.mpmt.timeline2.TimelineView;
import org.gprofng.mpmt.toolbar.ToolBarFiller;
import org.gprofng.mpmt.toolbar.ToolBarPanel;
import org.gprofng.mpmt.toolbar.ToolBarSeparator;
import org.gprofng.mpmt.util.gui.AnDialog2;
import org.gprofng.mpmt.util.gui.AnSplitPane;
import org.gprofng.mpmt.util.gui.AnTextIcon;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.gui.ToolTipPopup;
import org.gprofng.mpmt.welcome.WelcomeView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public final class AnWindow implements AnChangeListener {

  private static AnWindow instance;

  // Actions
  private static final String ACTION_PROFILE_APP =
      AnLocale.getString("Profile Application...", "ACTION_PROFILE_APP");
  private static final String ACTION_PROFILE_APP_TT =
      AnLocale.getString(
          "Start an application and collect profiling data to create an experiment",
          "ACTION_PROFILE_APP_TT"); // GUI test failures!!!
  private static final String ACTION_PROFILE_APP_TT_GUITESTING =
      AnLocale.getString("Profile Application", "ACTION_PROFILE_APP_TT");
  private static final String ACTION_PROFILE_APP_AC = ACTION_PROFILE_APP_TT;
  private static final String ACTION_PROFILE_RUNNING_PROCESS =
      AnLocale.getString("Profile Running Process...", "ACTION_PROFILE_RUNNING_PROCESS");
  private static final String ACTION_PROFILE_RUNNING_PROCESS_TT =
      AnLocale.getString(
          "Attach to a running process and create an experiment",
          "ACTION_PROFILE_RUNNING_PROCESS_TT");
  private static final String ACTION_PROFILE_RUNNING_PROCESS_AC = ACTION_PROFILE_RUNNING_PROCESS_TT;
  private static final String ACTION_PROFILE_KERNEL =
      AnLocale.getString("Profile Kernel...", "ACTION_PROFILE_KERNEL");
  private static final String ACTION_PROFILE_KERNEL_TT =
      AnLocale.getString("Profile Kernel", "ACTION_PROFILE_KERNEL_TT");
  private static final String ACTION_PROFILE_KERNEL_AC = ACTION_PROFILE_KERNEL_TT;
  private static final String ACTION_OPEN_EXPERIMENT =
      AnLocale.getString("Open Experiment...", "ACTION_OPEN_EXPERIMENT");
  private static final String ACTION_OPEN_EXPERIMENT_TT =
      AnLocale.getString(
          "Select an experiment to view", "ACTION_OPEN_EXPERIMENT_TT"); // GUI test failures!!!
  private static final String ACTION_OPEN_EXPERIMENT_TT_GUITESTING =
      AnLocale.getString("Open Experiment", "ACTION_OPEN_EXPERIMENT_TT");
  private static final String ACTION_OPEN_EXPERIMENT_AC = ACTION_OPEN_EXPERIMENT_TT;
  private static final String ACTION_COMPARE =
      AnLocale.getString("Compare Experiments...", "ACTION_COMPARE");
  private static final String ACTION_COMPARE_TT =
      AnLocale.getString(
          "Select multiple experiments and compare their data",
          "ACTION_COMPARE_TT"); // GUI test failures!!!
  private static final String ACTION_COMPARE_TT_GUITESTING =
      AnLocale.getString("Compare Experiments", "ACTION_COMPARE_TT");
  private static final String ACTION_COMPARE_AC = ACTION_COMPARE_TT;
  private static final String ACTION_AGGREGATE =
      AnLocale.getString("Aggregate Experiments...", "ACTION_AGGREGATE");
  private static final String ACTION_AGGREGATE_TT =
      AnLocale.getString("Aggregate Experiments", "ACTION_AGGREGATE_TT");
  private static final String ACTION_AGGREGATE_AC = ACTION_AGGREGATE_TT;
  private static final String ACTION_SHOW_ERRORS_WARNINGS =
      AnLocale.getString("Show Errors & Warnings", "ACTION_SHOW_ERRORS_WARNINGS");
  private static final String ACTION_SHOW_ERRORS_WARNINGS_TT =
      AnLocale.getString("Show Errors & Warnings", "ACTION_SHOW_ERRORS_WARNINGS_TT");
  private static final String ACTION_SHOW_ERRORS_WARNINGS_AC = ACTION_SHOW_ERRORS_WARNINGS_TT;
  private static final String ACTION_CONNECT =
      AnLocale.getString("Connect to Remote Host...", "ACTION_CONNECT");
  private static final String ACTION_CONNECT_TT =
      AnLocale.getString(
          "Connect to a remote host to profile applications or view experiments",
          "ACTION_CONNECT_TT");
  private static final String ACTION_CONNECT_AC = ACTION_CONNECT_TT;
  private static final String ACTION_EXPORT = AnLocale.getString("Export...", "ACTION_EXPORT");
  private static final String ACTION_EXPORT_TT = AnLocale.getString("Export", "ACTION_EXPORT_TT");
  private static final String ACTION_EXPORT_AC = ACTION_EXPORT_TT;
  private static final String ACTION_EXIT = AnLocale.getString("Exit", "ACTION_EXIT");

  private static final String ACTION_PREVIOUS_VIEW =
      AnLocale.getString("Previous View", "ACTION_PREVIOUS_VIEW");
  private static final String ACTION_PREVIOUS_VIEW_TT =
      AnLocale.getString("Previous View  (Shift+F7)", "ACTION_PREVIOUS_VIEW_TT");
  private static final String ACTION_PREVIOUS_VIEW_AC = ACTION_PREVIOUS_VIEW_TT;
  private static final String ACTION_NEXT_VIEW =
      AnLocale.getString("Next View", "ACTION_NEXT_VIEW");
  private static final String ACTION_NEXT_VIEW_TT =
      AnLocale.getString("Next View  (F7)", "ACTION_NEXT_VIEW_TT");
  private static final String ACTION_NEXT_VIEW_AC = ACTION_PREVIOUS_VIEW_TT;
  private static final String ACTION_VIEWS_SETTINGS =
      AnLocale.getString("Views Settings...", "ACTION_VIEWS_SETTINGS");
  private static final String ACTION_VIEWS_SETTINGS_TT =
      AnLocale.getString("Views Settings", "ACTION_VIEWS_SETTINGS_TT");
  private static final String ACTION_VIEWS_SETTINGS_AC = ACTION_VIEWS_SETTINGS_TT;

  private static final String ACTION_METRICS_SETTINGS =
      AnLocale.getString("Metrics Settings...", "ACTION_METRICS_SETTINGS");
  private static final String ACTION_METRICS_SETTINGS_TT =
      AnLocale.getString("Metrics Settings", "ACTION_METRICS_SETTINGS_TT");
  private static final String ACTION_METRICS_SETTINGS_AC = ACTION_METRICS_SETTINGS_TT;

  private static final String ACTION_ADD_REMOVE_FILTERS =
      AnLocale.getString("Add or remove filters...", "ACTION_ADD_REMOVE_FILTERS");
  private static final String ACTION_ADD_REMOVE_FILTERS_TT =
      AnLocale.getString("Add or remove filters", "ACTION_ADD_REMOVE_FILTERS_TT");
  private static final String ACTION_ADD_REMOVE_FILTERS_AC = ACTION_ADD_REMOVE_FILTERS_TT;
  private static final String ACTION_LIBRARY_VISIBILITY =
      AnLocale.getString("Library Visibility...", "ACTION_LIBRARY_VISIBILITY");
  private static final String ACTION_LIBRARY_VISIBILITY_TT =
      AnLocale.getString("Library Visibility", "ACTION_LIBRARY_VISIBILITY_TT");
  private static final String ACTION_LIBRARY_VISIBILITY_AC = ACTION_LIBRARY_VISIBILITY_TT;
  private static final String ACTION_FUNCTION_COLORS =
      AnLocale.getString("Function Colors...", "ACTION_FUNCTION_COLORS");
  private static final String ACTION_FUNCTION_COLORS_TT =
      AnLocale.getString("Function Colors", "ACTION_FUNCTION_COLORS_TT");
  private static final String ACTION_FUNCTION_COLORS_AC = ACTION_FUNCTION_COLORS_TT;
  private static final String ACTION_SETTINGS =
      AnLocale.getString("Settings...", "ACTION_SETTINGS");
  private static final String ACTION_SETTINGS_TT =
      AnLocale.getString("Settings", "ACTION_SETTINGS_TT");
  private static final String ACTION_SETTINGS_AC = ACTION_SETTINGS_TT;
  private static final String ACTION_EXPORT_SETTINGS =
      AnLocale.getString("Export Settings...", "ACTION_EXPORT_SETTINGS");
  private static final String ACTION_IMPORT_SETTINGS =
      AnLocale.getString("Import Settings...", "ACTION_IMPORT_SETTINGS");
  private static final String ACTION_EXPORT_SETTINGS_AS =
      AnLocale.getString("Export Settings as .er.rc...", "ACTION_EXPORT_SETTINGS_AS");

  private static final String ACTION_HELP_TT =
      AnLocale.getString("Opens a documentation window in the Gprofng GUI");
  private static final String ACTION_HELP_AC = ACTION_HELP_TT;
  private static final String ACTION_HELP_PERFORMANCE_ANALYZER =
      AnLocale.getString("Gprofng Analyzer");
  private static final String ACTION_HELP_NEW_FEATURES = AnLocale.getString("New Features");
  private static final String ACTION_HELP_INFORMATION_MAP = AnLocale.getString("Information Map");
  private static final String ACTION_HELP_KEYBOARD_SHORTCUTS =
      AnLocale.getString("Keyboard Shortcuts");
  private static final String ACTION_HELP_SHORTCUTS = AnLocale.getString("Help Shortcuts");
  private static final String ACTION_HELP_TROUBLESHOOTING = AnLocale.getString("Troubleshooting");
  private static final String ACTION_ABOUT_ANALYZER = AnLocale.getString("About Gprofng GUI");

  // Experiment status
  private static final int EXP_SUCCESS = 0;
  private static final int EXP_BROKEN = 4;
  private static final int EXP_OBSOLETE = 8;

  // Panels/Panes/Components
  private final AnFrame frame;
  private JPanel mainPanel;
  private ToolBarPanel toolBarPanel;
  private JPanel centerPanel;
  private StatusPanel statusPanel;

  private JComponent menuBar;
  private JPanel centerPanelSplit;
  private MainViewPanel mainViewPanel;
  private AnSplitPane navigationPanelSplitPane;
  private NavigationPanel navigationPanel;
  private SystemProgressPanel systemProgressPanel;
  private StatusLabelHandle tableStatusHandle;
  private StatusLabelValueHandle localHostStatusHandle;
  private StatusLabelValueHandle remoteHostStatusHandle;
  private StatusLabelValueHandle compareStatusHandle;
  private StatusLabelValueHandle showErrorsWarningsStatusHandle;
  private StatusLabelValueHandle filterStatusHandle;
  private StatusLabelValueHandle workingDirectoryStatusHandle;
  private StatusLabelValueHandle debugStatusHandle;
  private SummaryPanel summaryPanel;

  // Dialogs
  private Settings settings = null;
  private AnColorChooser colorChooser;
  private AnChooser anFileChooser;
  private ConnectionDialog connectionChooser = null;
  private AnChooser anRemoteFileChooser;
  private JLabel viewModeLabel;
  private JComboBox viewModeComboBox;
  private ToolBarSeparator viewModeSeparator;
  private ToolBarFiller viewModeFiller;
  private CollectDialog profileApplicationDialog = null;
  private CollectDialog profileRunningProcessDialog = null;
  private CollectDialog profileKernelDialog = null;
  private LibraryVisibilityDialog libraryVisibilityDialog = null;

  // Actions
  private AnAction profileApplicationAction;
  private AnAction profileRunningProcessAction;
  private AnAction profileKernelAction;
  private AnAction openExperimentAction;
  private AnAction compareExperimentsAction;
  private AnAction aggregateExperimentsAction;
  private AnAction showErrorsWarningsAction;
  private AnAction connectAction;
  private AnAction exportAction;
  private AnAction exitAction;
  //
  private AnAction previousViewAction;
  private AnAction nextViewAction;
  private AnAction viewsSettingsAction;
  //
  private AnAction metricsSettingsAction;
  //
  private AnAction addRemoveFiltersAction;
  private AnAction libraryVisibilityAction;
  private AnAction functionColorsAction;
  private AnAction settingsAction;
  private AnAction exportSettingsAction;
  private AnAction importSettingsAction;
  private AnAction exportSettingsAsAction;
  //
  private AnAction helpAnalyzerAction;
  private AnAction helpNewFeaturesAction;
  private AnAction helpInformationMapAction;
  private AnAction helpKeyboardShortcutsAction;
  private AnAction helpShortcutsAction;
  private AnAction helpTroubleShootingAction;
  private AnAction aboutAnalyzerAction;

  // Menus
  private JMenu fileMenu;
  private JMenu recentExperimentsMenu;
  private JMenu metricsMenu;
  private JMenu viewsMenu;
  private JMenu toolsMenu;
  private JMenu filtersMenu;
  private JMenu helpMenu;
  private JSeparator showErrorsWarningsSeparator;
  private JMenuItem showErrorsWarningsMenuItem;

  // States
  private Analyzer analyzer;
  private ViewDisplayPanel viewDisplayPanel;
  private Filters filters = null; // Holding and managing filters
  private Experiments experimentProperties;
  private SelectedObject selectedObject = null;
  private SelectionManager selectionManager = null;
  private String[][] experimentGroups = null; // The groups of loaded experiments
  private boolean hasErrorsAndWarnings = false;

  // AnDispTab's
  private AnDispTab welcomeDispTab = null;
  private AnDispTab overviewDispTab = null;

  // Current View and View lists
  private List<AnDisplay> standardDisplayList; // Display list (static)
  private List<AnDisplay> memoryIndexDisplayList; // Memory/Index objects Display list (dynamic)
  // Main Views (AnDisplay)
  private WelcomeView welcomeView;
  private OverviewView overviewView;
  private FunctionsView functionsView;
  private CallerCalleesView callerCalleesView;
  private CallTreeView callTreeView;
  private FlameView flameView;
  private SourceView sourceView;
  private DisassemblyView disassemblyView;
  private LinesView linesView;
  private PCsView pcsView;
  private DataLayoutView dataLayoutView;
  private DataObjectsView dataObjectsView;
  private IOView ioView;
  private HeapView heapView;
  private StatisticsView statisticsView;
  private SourceDisassemblyView sourceDisassemblyView;
  private ExperimentsView experimentsView;
  private TimelineView timelineView;
  private DualSourceView dualSourceView;
  private InstructionFrequencyView instructionFrequencyView;

  // Others views
  private CalledByCallsSourceView calledByCallsSourceView;
  private CalledByCallsDisassemblyView calledByCallsDisassemblyView;
  private CalledByCallsFunctionsView calledByCallsFunctionsView;

  // Subviews
  private List<Subview> subviewList = new ArrayList(); // List of all subviews, may contain null's
  private Subview selectedDetailsSubview = null;
  private Subview selectedDetailsSubviewTimeLine = null;
  private Subview timelineCallStackSubview = null;
  private Subview ioCallStackSubview = null;
  private Subview heapCallStackSubview = null;
  private Subview calledByCallsSrcSubview = null;
  private Subview calledByCallsDisSubview = null;
  private Subview calledByCallsFuncSubview = null;

  // **********************************************************************************************
  // Constructor
  public AnWindow(Analyzer parent, AnFrame frame, Container contentPane, JComponent manuBar) {
    instance = this;
    this.analyzer = parent;
    this.frame = frame;
    this.menuBar = manuBar;
    manuBar.setBackground(AnEnvironment.MENUBAR_BACKGROUND_COLOR);
    initComponents();
    enableSessionActions(false, false);
    selectedObject = new SelectedObject();
    selectionManager = new SelectionManager();

    AnEventManager.getInstance().addListener(this);
  }

  public static AnWindow getInstance() {
    return instance;
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
    //        System.out.println("AnWindow stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADING_NEW:
      case EXPERIMENTS_LOADED_FAILED:
        break;
      case EXPERIMENTS_LOADED:
        if (connectionChooser != null) {
          if (connectionChooser.isVisible()) {
            connectionChooser.requestFocus();
          }
        }
        break;
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
        if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEW_MODE) {
          updateViewMode();
        } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEW_MODE_ENABLED) {
          updateViewModeVisibility();
        } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.VIEWS) {
          ViewsSetting.Setting oldSetting =
              (ViewsSetting.Setting) anSettingChangeEvent.getOldValue();
          ViewsSetting.Setting newSetting =
              (ViewsSetting.Setting) anSettingChangeEvent.getNewValue();
          updateViews(oldSetting, newSetting);
        } else if (anSettingChangeEvent.getType() == AnSettingChangeEvent.Type.COMPARE_MODE) {
          updateCompareModeStatus();
        }
        break;
      case DEBUG:
        //                debug();
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
  }

  // Initialize GUI components
  private void initComponents() {
    mainPanel = new JPanel(new BorderLayout());
    getFrame().getContentPane().add(mainPanel);

    initActions();
    toolBarPanel = initToolBarPanel();
    initMenus();
    initViewsAndOtherComponents();
    centerPanel = initCenterPanel();
    statusPanel = initStatusPanel();

    mainPanel.add(toolBarPanel, BorderLayout.NORTH);
    mainPanel.add(centerPanel, BorderLayout.CENTER);
    mainPanel.add(statusPanel, BorderLayout.SOUTH);
  }

  private void initActions() {
    profileApplicationAction =
        new AnAction(
            ACTION_PROFILE_APP,
            AnUtility.collect_icon,
            GUITesting.getInstance().isRunningUnderGUITesting()
                ? ACTION_PROFILE_APP_TT_GUITESTING
                : ACTION_PROFILE_APP_TT,
            ACTION_PROFILE_APP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                profileApplicationAction(null);
              }
            });
    profileApplicationAction.setKeyboardShortCut(
        KeyboardShortcuts.profileApplicationActionShortcut);
    profileApplicationAction.setMnemonic(AnLocale.getString('E', "ACTION_PROFILE_APP_MN"));

    profileRunningProcessAction =
        new AnAction(
            ACTION_PROFILE_RUNNING_PROCESS,
            null,
            ACTION_PROFILE_RUNNING_PROCESS_TT,
            ACTION_PROFILE_RUNNING_PROCESS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                profileRunningProcessAction();
              }
            });
    profileRunningProcessAction.setKeyboardShortCut(
        KeyboardShortcuts.profileRunningProcessActionShortcut);
    profileRunningProcessAction.setMnemonic(
        AnLocale.getString('R', "ACTION_PROFILE_RUNNING_PROCESS_MN"));

    profileKernelAction =
        new AnAction(
            ACTION_PROFILE_KERNEL,
            null,
            ACTION_PROFILE_KERNEL_TT,
            ACTION_PROFILE_KERNEL_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                profileKernelAction();
              }
            });
    profileKernelAction.setKeyboardShortCut(KeyboardShortcuts.profileKernelActionShortcut);
    profileKernelAction.setMnemonic(AnLocale.getString('K', "ACTION_PROFILE_KERNEL_MN"));

    openExperimentAction =
        new AnAction(
            ACTION_OPEN_EXPERIMENT,
            AnUtility.open_icon,
            GUITesting.getInstance().isRunningUnderGUITesting()
                ? ACTION_OPEN_EXPERIMENT_TT_GUITESTING
                : ACTION_OPEN_EXPERIMENT_TT,
            ACTION_OPEN_EXPERIMENT_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                openExperimentAction();
              }
            });
    openExperimentAction.setKeyboardShortCut(KeyboardShortcuts.openExperimentActionShortcut);
    openExperimentAction.setMnemonic(AnLocale.getString('O', "ACTION_OPEN_EXPERIMENT_MN"));

    compareExperimentsAction =
        new AnAction(
            ACTION_COMPARE,
            AnUtility.compare_icon,
            GUITesting.getInstance().isRunningUnderGUITesting()
                ? ACTION_COMPARE_TT_GUITESTING
                : ACTION_COMPARE_TT,
            ACTION_COMPARE_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                compareExperimentsAction();
              }
            });
    compareExperimentsAction.setKeyboardShortCut(
        KeyboardShortcuts.compareExperimentsActionShortcut);
    compareExperimentsAction.setMnemonic(AnLocale.getString('m', "ACTION_COMPARE_MN"));

    aggregateExperimentsAction =
        new AnAction(
            ACTION_AGGREGATE,
            AnUtility.add_icon,
            ACTION_AGGREGATE_TT,
            ACTION_AGGREGATE_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                aggregateExperimentsAction();
              }
            });
    aggregateExperimentsAction.setKeyboardShortCut(
        KeyboardShortcuts.aggregateExperimentsActionShortcut);
    aggregateExperimentsAction.setMnemonic(AnLocale.getString('A', "ACTION_AGGREGATE_MN"));

    showErrorsWarningsAction =
        new AnAction(
            ACTION_SHOW_ERRORS_WARNINGS,
            null,
            ACTION_SHOW_ERRORS_WARNINGS_TT,
            ACTION_SHOW_ERRORS_WARNINGS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                showErrorsWarningsAction();
              }
            });
    showErrorsWarningsAction.setKeyboardShortCut(
        KeyboardShortcuts.showErrorsWarningsActionShortcut);
    showErrorsWarningsAction.setMnemonic(AnLocale.getString('W', "ACTION_SHOW_ERRORS_WARNINGS_MN"));

    connectAction =
        new AnAction(
            ACTION_CONNECT,
            AnUtility.connect_icon,
            ACTION_CONNECT_TT,
            ACTION_CONNECT_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                connectAction();
              }
            });
    connectAction.setKeyboardShortCut(KeyboardShortcuts.connectActionShortcut);
    connectAction.setMnemonic(AnLocale.getString('C', "ACTION_CONNECT_MN"));

    exportAction =
        new AnAction(
            ACTION_EXPORT,
            null,
            ACTION_EXPORT_TT,
            ACTION_EXPORT_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                exportAction();
              }
            });
    exportAction.setKeyboardShortCut(KeyboardShortcuts.exportActionShortcut);
    exportAction.setMnemonic(AnLocale.getString('P', "ACTION_EXPORT_MN"));

    exitAction =
        new AnAction(
            ACTION_EXIT,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                exitAction();
              }
            });
    exitAction.setMnemonic(AnLocale.getString('x', "ACTION_EXIT_MN"));

    previousViewAction =
        new AnAction(
            ACTION_PREVIOUS_VIEW,
            AnUtility.previousViewIcon,
            ACTION_PREVIOUS_VIEW_TT,
            ACTION_PREVIOUS_VIEW_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                previousViewAction();
              }
            });
    previousViewAction.setKeyboardShortCut(KeyboardShortcuts.previousViewActionShortcut);

    nextViewAction =
        new AnAction(
            ACTION_NEXT_VIEW,
            AnUtility.nextViewIcon,
            ACTION_NEXT_VIEW_TT,
            ACTION_NEXT_VIEW_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nextViewAction();
              }
            });
    nextViewAction.setKeyboardShortCut(KeyboardShortcuts.nextViewActionShortcut);

    viewsSettingsAction =
        new AnAction(
            ACTION_VIEWS_SETTINGS,
            null,
            ACTION_VIEWS_SETTINGS_TT,
            ACTION_VIEWS_SETTINGS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                viewsSettingsAction();
              }
            });
    viewsSettingsAction.setKeyboardShortCut(KeyboardShortcuts.viewsSettingsActionShortcut);

    metricsSettingsAction =
        new AnAction(
            ACTION_METRICS_SETTINGS,
            null,
            ACTION_METRICS_SETTINGS_TT,
            ACTION_METRICS_SETTINGS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                metricsSettingsAction();
              }
            });
    metricsSettingsAction.setKeyboardShortCut(KeyboardShortcuts.metricsSettingsShortcut);

    addRemoveFiltersAction =
        new AnAction(
            ACTION_ADD_REMOVE_FILTERS,
            AnUtility.filt_icon,
            ACTION_ADD_REMOVE_FILTERS_TT,
            ACTION_ADD_REMOVE_FILTERS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                addRemoveFiltersAction(e.getSource());
              }
            });

    libraryVisibilityAction =
        new AnAction(
            ACTION_LIBRARY_VISIBILITY,
            AnUtility.epcl_icon,
            ACTION_LIBRARY_VISIBILITY_TT,
            ACTION_LIBRARY_VISIBILITY_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                libraryVisibilityAction();
              }
            });
    libraryVisibilityAction.setKeyboardShortCut(KeyboardShortcuts.libraryVisibilityActionShortcut);
    libraryVisibilityAction.setMnemonic(AnLocale.getString('L', "ACTION_LIBRARY_VISIBILITY_MN"));

    functionColorsAction =
        new AnAction(
            ACTION_FUNCTION_COLORS,
            AnUtility.colr_icon,
            ACTION_FUNCTION_COLORS_TT,
            ACTION_FUNCTION_COLORS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                functionColorsAction();
              }
            });
    functionColorsAction.setKeyboardShortCut(KeyboardShortcuts.functionColorsActionShortcut);
    functionColorsAction.setMnemonic(AnLocale.getString('C', "ACTION_FUNCTION_COLORS_MN"));

    settingsAction =
        new AnAction(
            ACTION_SETTINGS,
            AnUtility.gear_icon,
            ACTION_SETTINGS_TT,
            ACTION_SETTINGS_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                settingsAction();
              }
            });
    settingsAction.setKeyboardShortCut(KeyboardShortcuts.settingsActionShortcut);
    settingsAction.setMnemonic(AnLocale.getString('S', "ACTION_SETTINGS_MN"));

    exportSettingsAction =
        new AnAction(
            ACTION_EXPORT_SETTINGS,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                exportSettingsAction();
              }
            });
    exportSettingsAction.setKeyboardShortCut(KeyboardShortcuts.exportSettingsActionShortcut);
    exportSettingsAction.setMnemonic(AnLocale.getString('x', "ACTION_EXPORT_SETTINGS_MN"));
    ;

    importSettingsAction =
        new AnAction(
            ACTION_IMPORT_SETTINGS,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                importSettingsAction();
              }
            });
    importSettingsAction.setMnemonic(AnLocale.getString('i', "ACTION_IMPORT_SETTINGS_MN"));
    ;
    importSettingsAction.setKeyboardShortCut(KeyboardShortcuts.importSettingsActionShortcut);

    exportSettingsAsAction =
        new AnAction(
            ACTION_EXPORT_SETTINGS_AS,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                exportSettingsAsAction();
              }
            });
    exportSettingsAsAction.setMnemonic(AnLocale.getString('A', "ACTION_EXPORT_SETTINGS_AS_MN"));

    helpAnalyzerAction =
        new AnAction(
            ACTION_HELP_PERFORMANCE_ANALYZER,
            null,
            ACTION_HELP_TT,
            ACTION_HELP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                helpAnalyzerAction();
              }
            });
    helpAnalyzerAction.setMnemonic(AnLocale.getString('P', "ACTION_HELP_PERFORMANCE_ANALYZER_MN"));

    helpNewFeaturesAction =
        new AnAction(
            ACTION_HELP_NEW_FEATURES,
            null,
            ACTION_HELP_TT,
            ACTION_HELP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                helpNewFeaturesAction();
              }
            });
    helpNewFeaturesAction.setMnemonic(AnLocale.getString('N', "ACTION_HELP_NEW_FEATURES_MN"));

    helpInformationMapAction =
        new AnAction(
            ACTION_HELP_INFORMATION_MAP,
            null,
            ACTION_HELP_TT,
            ACTION_HELP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                helpInformationMapAction();
              }
            });
    helpInformationMapAction.setMnemonic(AnLocale.getString('I', "ACTION_HELP_INFORMATION_MAP_MN"));

    helpKeyboardShortcutsAction =
        new AnAction(
            ACTION_HELP_KEYBOARD_SHORTCUTS,
            null,
            ACTION_HELP_TT,
            ACTION_HELP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                helpKeyboardShortcutsAction();
              }
            });
    helpKeyboardShortcutsAction.setMnemonic(
        AnLocale.getString('K', "ACTION_HELP_KEYBOARD_SHORTCUTS_MN"));

    helpShortcutsAction =
        new AnAction(
            ACTION_HELP_SHORTCUTS,
            null,
            ACTION_HELP_TT,
            ACTION_HELP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                helpShortcutsAction();
              }
            });
    helpShortcutsAction.setMnemonic(AnLocale.getString('S', "ACTION_HELP_SHORTCUTS_MN"));

    helpTroubleShootingAction =
        new AnAction(
            ACTION_HELP_TROUBLESHOOTING,
            null,
            ACTION_HELP_TT,
            ACTION_HELP_AC,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                helpTroubleShootingAction();
              }
            });
    helpTroubleShootingAction.setMnemonic(
        AnLocale.getString('r', "ACTION_HELP_TROUBLESHOOTING_MN"));

    aboutAnalyzerAction =
        new AnAction(
            ACTION_ABOUT_ANALYZER,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                aboutAnalyzerAction();
              }
            });
    aboutAnalyzerAction.setMnemonic(AnLocale.getString('A', "ACTION_ABOUT_ANALYZER_MN"));
  }

  private ToolBarPanel initToolBarPanel() {
    return new ToolBarPanel(initToolBar());
  }

  // Initialize tool bar area
  private JToolBar initToolBar() {
    JToolBar toolBar = new JToolBar();
    AnUtility.setAccessibleContext(toolBar.getAccessibleContext(), AnLocale.getString("Toolbar"));
    toolBar.setBorder(null);
    toolBar.setOpaque(false);

    toolBar.add(profileApplicationAction.createActionButton());
    toolBar.add(openExperimentAction.createActionButton());
    toolBar.add(compareExperimentsAction.createActionButton());
    toolBar.add(aggregateExperimentsAction.createActionButton());
    toolBar.add(connectAction.createActionButton());
    toolBar.add(new ToolBarSeparator(8, 7));
    toolBar.add(addRemoveFiltersAction.createActionButton());
    toolBar.add(libraryVisibilityAction.createActionButton());
    toolBar.add(settingsAction.createActionButton());

    // View mode
    toolBar.add(viewModeSeparator = new ToolBarSeparator(7, 11));
    viewModeLabel = new JLabel(AnLocale.getString("View Mode: "));
    viewModeLabel.setDisplayedMnemonic(AnLocale.getString('d', "ViewMode"));
    toolBar.add(viewModeLabel);
    String viewModeComboBoxName = AnLocale.getString("View Mode: ");
    String viewModeComboBoxDesc = AnLocale.getString("Select View Mode");
    String viewModeComboBoxTT =
        AnLocale.getString(
            "The View Mode setting controls the processing of Java experiments and OpenMP"
                + " experiments");
    viewModeComboBox = new JComboBox();
    viewModeLabel.setLabelFor(viewModeComboBox);
    viewModeComboBox.setFont(viewModeComboBox.getFont().deriveFont(Font.PLAIN));
    viewModeComboBox.setToolTipText(viewModeComboBoxTT);
    viewModeComboBox.getAccessibleContext().setAccessibleName(viewModeComboBoxName);
    viewModeComboBox.getAccessibleContext().setAccessibleDescription(viewModeComboBoxDesc);
    viewModeComboBox.addItem(ViewModeSetting.ViewMode.USER);
    viewModeComboBox.addItem(ViewModeSetting.ViewMode.EXPERT);
    viewModeComboBox.addItem(ViewModeSetting.ViewMode.MACHINE);
    viewModeComboBox.setSelectedIndex(0);
    viewModeComboBox.setPreferredSize(new Dimension(viewModeComboBox.getPreferredSize().width, 20));
    viewModeComboBox.setMaximumSize(viewModeComboBox.getPreferredSize());
    viewModeComboBox.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            ViewMode viewMode = (ViewMode) viewModeComboBox.getSelectedItem();
            getSettings().getViewModeSetting().set(this, viewMode);
          }

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    toolBar.add(viewModeComboBox);
    toolBar.add(viewModeFiller = new ToolBarFiller(5));
    updateViewModeVisibility(false);

    return toolBar;
  }

  // Initialize menu items
  private void initMenus() {
    // File Menu
    menuBar.add(fileMenu = new JMenu(AnLocale.getString("File")));
    AnUtility.setAccessibleContext(fileMenu.getAccessibleContext(), fileMenu.getText());
    fileMenu.setMnemonic(AnLocale.getString('F', "MNEM_MENU_FILE"));
    fileMenu.add(profileApplicationAction.getMenuItem());
    fileMenu.add(profileRunningProcessAction.getMenuItem());
    fileMenu.add(profileKernelAction.getMenuItem());
    fileMenu.addSeparator();
    fileMenu.add(openExperimentAction.getMenuItem());
    fileMenu.add(initOpenRecentExperiments());
    fileMenu.add(compareExperimentsAction.getMenuItem());
    fileMenu.add(aggregateExperimentsAction.getMenuItem());
    fileMenu.add(showErrorsWarningsSeparator = new JSeparator());
    fileMenu.add(showErrorsWarningsMenuItem = showErrorsWarningsAction.getMenuItem());
    fileMenu.addSeparator();
    fileMenu.add(connectAction.getMenuItem());
    fileMenu.add(exportAction.getMenuItem());
    fileMenu.addSeparator();
    fileMenu.add(exitAction.getMenuItem());

    // Views Menu. Note: this menu is dynamic
    menuBar.add(viewsMenu = new JMenu(AnLocale.getString("Views")));
    AnUtility.setAccessibleContext(viewsMenu.getAccessibleContext(), viewsMenu.getText());
    viewsMenu.setMnemonic(AnLocale.getString('V', "MNEM_MENU_VIEWS"));
    viewsMenu.addMenuListener(new ViewsMenuListener(viewsMenu));

    // Metrics Menu. Note: this menu is dynamic
    menuBar.add(metricsMenu = new JMenu(AnLocale.getString("Metrics")));
    AnUtility.setAccessibleContext(metricsMenu.getAccessibleContext(), metricsMenu.getText());
    metricsMenu.setMnemonic(AnLocale.getString('M', "MNEM_MENU_METRICS"));
    metricsMenu.addMenuListener(new MetricsMenuListener(metricsMenu));

    // Tools Menu
    menuBar.add(toolsMenu = new JMenu(AnLocale.getString("Tools")));
    AnUtility.setAccessibleContext(toolsMenu.getAccessibleContext(), toolsMenu.getText());
    toolsMenu.setMnemonic(AnLocale.getString('T', "MNEM_MENU_TOOLS"));
    filtersMenu = new JMenu(AnLocale.getString("Filters"));
    AnUtility.setAccessibleContext(filtersMenu.getAccessibleContext(), filtersMenu.getText());
    filtersMenu.setMnemonic(AnLocale.getString('F', "MNEM_MENU_FILTER"));
    filtersMenu.setIcon(AnUtility.filt_icon);
    toolsMenu.add(filtersMenu);
    filtersMenu.addMenuListener(
        new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            filtersMenu.removeAll();
            JPopupMenu popup = getViews().getCurrentViewDisplay().getFilterPopup();
            if (popup == null) {
              popup = getViews().getCurrentViewDisplay().getDefaultFilterPopup();
            }
            Component[] components = popup.getComponents();
            for (Component component : components) {
              filtersMenu.add(component);
            }
          }

          @Override
          public void menuDeselected(MenuEvent e) {}

          @Override
          public void menuCanceled(MenuEvent e) {}
        });
    toolsMenu.add(libraryVisibilityAction.getMenuItem());
    toolsMenu.add(functionColorsAction.getMenuItem());
    toolsMenu.add(settingsAction.getMenuItem());
    toolsMenu.addSeparator();
    toolsMenu.add(exportSettingsAction.getMenuItem());
    toolsMenu.add(importSettingsAction.getMenuItem());
    toolsMenu.add(exportSettingsAsAction.getMenuItem());

    // Help menu
    menuBar.add(helpMenu = new JMenu(AnLocale.getString("Help", "MENU_HELP")));
    AnUtility.setAccessibleContext(helpMenu.getAccessibleContext(), helpMenu.getText());
    helpMenu.setMnemonic(AnLocale.getString('H', "MNEM_MENU_HELP"));
    helpMenu.add(helpAnalyzerAction.getMenuItem());
    helpMenu.add(helpNewFeaturesAction.getMenuItem());
    helpMenu.add(helpInformationMapAction.getMenuItem());
    helpMenu.add(helpKeyboardShortcutsAction.getMenuItem());
    helpMenu.add(helpShortcutsAction.getMenuItem());
    helpMenu.add(helpTroubleShootingAction.getMenuItem());
    helpAnalyzerAction.setEnabled(false);
    helpNewFeaturesAction.setEnabled(false);
    helpInformationMapAction.setEnabled(false);
    helpKeyboardShortcutsAction.setEnabled(false);
    helpShortcutsAction.setEnabled(false);
    helpTroubleShootingAction.setEnabled(false);
    helpMenu.addSeparator();
    helpMenu.add(aboutAnalyzerAction.getMenuItem());
  }

  private JMenu initOpenRecentExperiments() {
    recentExperimentsMenu = new JMenu(AnLocale.getString("Open Recent Experiment"));
    AnUtility.setAccessibleContext(
        recentExperimentsMenu.getAccessibleContext(), recentExperimentsMenu.getText());
    recentExperimentsMenu.setMnemonic(AnLocale.getString('n', "MNEM_MENU_OPEN_RECENT_EXPERIMENT"));
    recentExperimentsMenu.addMenuListener(
        new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            recentExperimentsMenu.removeAll();

            List<ExperimentPickListElement> paths =
                UserPref.getInstance()
                    .getExperimentsPicklists()
                    .getPicklist()
                    .getRecentExperiments(20);
            //                Collections.sort(paths, new Comparator<ExperimentPickListElement>() {
            //                    @Override
            //                    public int compare(ExperimentPickListElement f1,
            // ExperimentPickListElement f2) {
            //                        String f11 = AnUtility.basename(f1.getPath());
            //                        String f22 = AnUtility.basename(f2.getPath());
            //                        return f11.toString().compareTo(f22.toString());
            //                    }
            //                });

            for (ExperimentPickListElement experimentElement : paths) {
              String name = AnUtility.basename(experimentElement.getPath());
              JMenuItem menuItem = new JMenuItem(name);
              AnUtility.setAccessibleContext(menuItem.getAccessibleContext(), " ");
              if (experimentElement.getPath().endsWith("erg")) {
                menuItem.setIcon(
                    new ImageIcon(
                        getClass()
                            .getResource(
                                "/org/gprofng/mpmt/icons/expgroup.png"))); // FIXUP: make
                // static
              } else {
                menuItem.setIcon(
                    new ImageIcon(
                        getClass()
                            .getResource(
                                "/org/gprofng/mpmt/icons/experiment.png"))); // FIXUP: make
                // static
              }
              menuItem.setActionCommand(experimentElement.getPath());
              menuItem.setToolTipText(experimentElement.getToolTip());
              menuItem.addActionListener(new RecentExperimentActionListener(experimentElement));
              recentExperimentsMenu.add(menuItem);
            }

            if (!paths.isEmpty()) {
              recentExperimentsMenu.addSeparator();
              JMenuItem clearMenuItem = new JMenuItem(AnLocale.getString("Clear Recent History"));
              AnUtility.setAccessibleContext(
                  clearMenuItem.getAccessibleContext(), clearMenuItem.getText());
              clearMenuItem.setMnemonic(AnLocale.getString('C', "ClearRecentHistoryMenuMN"));
              clearMenuItem.addActionListener(new ClearRecentHistoryActionListener());
              recentExperimentsMenu.add(clearMenuItem);
            }
          }

          // @Override
          @Override
          public void menuDeselected(MenuEvent e) {}

          // @Override
          @Override
          public void menuCanceled(MenuEvent e) {}
        });

    return recentExperimentsMenu;
  }

  private class ClearRecentHistoryActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      String asWhenClosedConfigDirPath = UserPref.getAsWhenClosedConfigDirPath();
      File[] files = new File(asWhenClosedConfigDirPath).listFiles();
      for (File file : files) {
        file.delete();
      }
      UserPref.getInstance().getExperimentsPicklists().getPicklist().removeAllElements();
      saveAnalyzerSettings();
    }
  }

  private class RecentExperimentActionListener implements ActionListener {

    ExperimentPickListElement experimentPickListElement;

    public RecentExperimentActionListener(ExperimentPickListElement experimentPickListElement) {
      this.experimentPickListElement = experimentPickListElement;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      ArrayList<String> list = new ArrayList<String>();
      list.add(experimentPickListElement.getPath());
      String confPath;
      boolean always;
      if (experimentPickListElement.getConfPath() != null) {
        confPath = experimentPickListElement.getConfPath();
        always = true;
      } else {
        confPath = UserPref.getAsWhenClosedConfigPath(experimentPickListElement.getPath());
        always = false;
      }
      loadExperimentList(
          list, false, experimentPickListElement.getWorkingDirectory(), true, confPath, always);
    }
  }

  /** Called after startup */
  private void initViewsAndOtherComponents() {
    //        System.out.println("initViewsAndOtherComponents");

    // Misc
    filters = new Filters(this, frame);
    experimentProperties = new Experiments(this);
    settings = new Settings(this, frame);
    colorChooser = new AnColorChooser(this, frame);
    summaryPanel = new SummaryPanel();

    // Views
    viewDisplayPanel = new ViewDisplayPanel();
    AccessibleContext context = viewDisplayPanel.getAccessibleContext();
    String loc_string = AnLocale.getString("Analyzer Main Window");
    context.setAccessibleName(loc_string);
    context.setAccessibleDescription(loc_string);
    standardDisplayList = new ArrayList<AnDisplay>();
    // welcomeDisp pane
    welcomeView = new WelcomeView();
    standardDisplayList.add(welcomeView);
    // Overview view
    overviewView = new OverviewView();
    standardDisplayList.add(overviewView);
    // functions view
    functionsView = new FunctionsView();
    standardDisplayList.add(functionsView);
    // caller-callee view
    callerCalleesView = new CallerCalleesView();
    standardDisplayList.add(callerCalleesView);
    // Call Tree view
    callTreeView = new CallTreeView();
    standardDisplayList.add(callTreeView);
    // Flame view
    flameView = new FlameView();
    //        flameView = new FlameViewCallTreeIPC();
    standardDisplayList.add(flameView);
    // Source view
    sourceView = new SourceView();
    standardDisplayList.add(sourceView);
    // Disassembly view
    disassemblyView = new DisassemblyView();
    standardDisplayList.add(disassemblyView);
    // Lines view
    linesView = new LinesView();
    standardDisplayList.add(linesView);
    // PCs view
    pcsView = new PCsView();
    standardDisplayList.add(pcsView);
    // DataLayout view
    dataLayoutView = new DataLayoutView();
    standardDisplayList.add(dataLayoutView);
    // DataObjects view
    dataObjectsView = new DataObjectsView();
    standardDisplayList.add(dataObjectsView);
    // IO view
    ioView = new IOView();
    standardDisplayList.add(ioView);
    // Heap view
    heapView = new HeapView();
    standardDisplayList.add(heapView);
    // Statistics view
    statisticsView = new StatisticsView();
    standardDisplayList.add(statisticsView);
    // Source/Disassembly view
    sourceDisassemblyView = new SourceDisassemblyView();
    standardDisplayList.add(sourceDisassemblyView);
    // Experiments view
    experimentsView = new ExperimentsView();
    standardDisplayList.add(experimentsView);
    // Timeline view
    timelineView = new TimelineView();
    standardDisplayList.add(timelineView);
    // Dual Source view
    dualSourceView = new DualSourceView();
    standardDisplayList.add(dualSourceView);
    // Instruction Frequency view
    instructionFrequencyView = new InstructionFrequencyView();
    standardDisplayList.add(instructionFrequencyView);
    // Called By Calls Source view
    calledByCallsSourceView = new CalledByCallsSourceView();
    standardDisplayList.add(calledByCallsSourceView);
    // Called By Calls Disassembly view
    calledByCallsDisassemblyView = new CalledByCallsDisassemblyView();
    standardDisplayList.add(calledByCallsDisassemblyView);
    // Called By Calls Functions view
    calledByCallsFunctionsView = new CalledByCallsFunctionsView();
    standardDisplayList.add(calledByCallsFunctionsView);
  }

  private JPanel initCenterPanel() {
    mainViewPanel = new MainViewPanel();
    navigationPanel = new NavigationPanel(this);
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;

    centerPanelSplit = new JPanel(new BorderLayout());
    centerPanelSplit.add(mainViewPanel, BorderLayout.CENTER);
    JPanel centerPanelNoSplitOrSplit = new JPanel(new GridBagLayout());
    centerPanelNoSplitOrSplit.setBorder(
        BorderFactory.createMatteBorder(
            5, 0, 5, 4, AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR));
    centerPanelNoSplitOrSplit.add(centerPanelSplit, gridBagConstraints);

    navigationPanelSplitPane =
        new AnSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            navigationPanel,
            centerPanelNoSplitOrSplit,
            UserPref.getInstance().getNavigationPanelDividerPosition());
    centerPanel = new JPanel(new GridBagLayout());
    centerPanel.add(navigationPanelSplitPane, gridBagConstraints);
    navigationPanelSplitPane.setHidden(true);
    mainViewPanel.getMainview().add(viewDisplayPanel);

    return centerPanel;
  }

  private StatusPanel initStatusPanel() {
    statusPanel = new StatusPanel();

    // Table status
    tableStatusHandle =
        StatusHandleFactory.createStatusLabel(
            this,
            "",
            AnLocale.getString("Selected row(s)/Total rows"),
            StatusPanel.Orientation.RIGHT);

    // Progress status
    systemProgressPanel = new SystemProgressPanel();
    StatusHandleFactory.createStatusComponent(
        this, systemProgressPanel, StatusPanel.Orientation.RIGHT);

    // Local Host status
    localHostStatusHandle =
        StatusHandleFactory.createStatusLabelValue(
            this, null, AnLocale.getString("Local Host"), AnLocale.getString("Local Host"), null);
    String localHost = analyzer.getLocalHostName();
    if (localHost != null) {
      localHostStatusHandle.update(AnUtility.getShortString(analyzer.getLocalHostName(), 30));
    }
    localHostStatusHandle.setVisible(true);
    final KeyStroke statusFocusShortCut = KeyboardShortcuts.statusAreaShortCut;
    localHostStatusHandle
        .getTextLabel()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(statusFocusShortCut, statusFocusShortCut);
    localHostStatusHandle
        .getTextLabel()
        .getActionMap()
        .put(
            statusFocusShortCut,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                localHostStatusHandle.getTextLabel().requestFocus();
              }
            });

    // Remote Host status
    remoteHostStatusHandle =
        StatusHandleFactory.createStatusLabelValue(
            this,
            null,
            AnLocale.getString("Remote Host"),
            AnLocale.getString("Connect to remote host...")
                + AnUtility.keyStrokeToStringFormatted(KeyboardShortcuts.connectActionShortcut),
            new ActionListener() {
              // @Override
              @Override
              public void actionPerformed(ActionEvent e) {
                connectAction();
              }
            });
    remoteHostStatusHandle.update("localhost");
    if (!Analyzer.getInstance().IPC_started) {
      remoteHostStatusHandle.update("");
      /* "not connected" */

    }

    // Working Directory
    workingDirectoryStatusHandle =
        StatusHandleFactory.createStatusLabelValue(
            this,
            null,
            AnLocale.getString("Working Directory"),
            AnLocale.getString("Current working directory"),
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                String currentWD = analyzer.getWorkingDirectory();
                //                AnChooser chooser = new AnChooser(frame, AnChooser.DIR_CHOOSER,
                // AnWindow.this, (boolean)false, (String)currentWD);
                AnChooser chooser =
                    getAnChooser(
                        AnLocale.getString("Working Directory"), AnChooser.DIR_CHOOSER, currentWD);
                int res = chooser.showDialog(frame, null);
                if (res == AnChooser.APPROVE_OPTION) {
                  File wdFile = chooser.getSelectedAnFile();
                  String wd = wdFile.getAbsolutePath();
                  AnUtility.checkIPCOnWrongThread(false);
                  analyzer.setWorkingDirectory(wd); // IPC call
                  AnUtility.checkIPCOnWrongThread(true);
                  //                    readRCFile(".er.rc");  // IPC call
                  //                    viewMode = null; // Clean cache <=== FIXUP: need to refresh
                  // other states too....
                }
              }
            });
    updateWorkingDirectoryStatus();

    // Compare status
    compareStatusHandle =
        StatusHandleFactory.createStatusLabelValue(
            this,
            null,
            AnLocale.getString("Compare"),
            AnLocale.getString("Compare experiments...")
                + AnUtility.keyStrokeToStringFormatted(
                    KeyboardShortcuts.compareExperimentsActionShortcut),
            new ActionListener() {
              // @Override
              @Override
              public void actionPerformed(ActionEvent e) {
                compareExperimentsAction();
              }
            });
    updateCompareModeStatus();

    // Filter status
    filterStatusHandle =
        StatusHandleFactory.createStatusLabelValue(
            this,
            null,
            AnLocale.getString("Filters"),
            AnLocale.getString("Add or remove filters"),
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                getViews()
                    .getCurrentViewDisplay()
                    .showFilterPopup(filterStatusHandle.getValueLabel());
              }
            });
    filterStatusHandle.update(AnLocale.getString("off"));

    // Dump
    if (GUITesting.getInstance().guiTestingExtraEnabled()) {
      debugStatusHandle =
          StatusHandleFactory.createStatusLabelValue(
              this,
              null,
              AnLocale.getString("Debug"),
              AnLocale.getString("Debug(GUI testing)"),
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  String dump;

                  //                    dump = GUITesting.getInstance().dumpMainview();
                  //                    System.out.println(dump);
                  //                    dump = GUITesting.getInstance().dumpViewsVisibility();
                  //                    System.out.println(dump);
                  //                    dump = GUITesting.getInstance().dumpSubviews();
                  //                    System.out.println(dump);
                  //
                  //                    System.out.println("");
                  ////                    AnEventManager.getInstance().fireAnChangeEvent(new
                  // AnChangeEvent(this, AnChangeEvent.Type.DEBUG));
                  ////                    AnEventManager.getInstance().fireAnChangeEvent(new
                  // AnChangeEvent(this, AnChangeEvent.Type.TEST1));
                  //                    AnUtility.dispatchOnAWorkerThread(new
                  // IPCTester.IPCTestSender(), "CCSender");
                  //                    IPCLogger.setIpcDelay(500);
                  //                    int dtype = getViews().getCurrentViewDisplay().getType();
                  //                    getSettings().getMetricsSetting().dumpMetricsByDType(dtype);
                  //                    getSettings().getMetricsSetting().dump();
                  //                    getSettings().getMetricsSetting().dumpSelections();
                  //                    getSettings().getMetricsSetting().getMetricStates().dump();
                  //
                  //
                  // System.out.println(GUITesting.getInstance().dumpAvailableViews());
                  //
                  // AnTable.setUseAllHeaderPanel(!AnTable.isUseAllHeaderPanel());
                  //                    getSettings().getTableSettings().setWrapMetricNames(this,
                  // !getSettings().getTableSettings().wrapMetricNames());
                  //
                  //                    AnDisplay currentAnDisplay =
                  // getViews().getCurrentViewDisplay();
                  //                    int dtype = currentAnDisplay.getType();
                  //                    int mtype = MetricsSetting.dtype2mtype(dtype);
                  //                    System.out.println(String.format("%2d", dtype) + " " + mtype
                  // + " " + currentAnDisplay.getAnDispTab().getTName() + " " +
                  // currentAnDisplay.getAnDispTab().getTCmd());
                  //                    FilterClause clause = new FilterClause("1", "1", "1",
                  // FilterClause.Kind.STANDARD);
                  //                    getFilters().addClause(clause, false);
                  System.out.println(GUITesting.getInstance().dumpMainview(7));
                }
              });
    }

    // Error/Warning status
    showErrorsWarningsStatusHandle =
        StatusHandleFactory.createStatusLabelValue(
            this,
            null,
            null,
            AnLocale.getString(
                "Experiment has errors or warnings. Click for details...  (Ctrl+Shift-W)"),
            getShowErrorsWarningsAction());
    showErrorsWarningsStatusHandle.setVisible(false);

    // Focus (tab) traversal
    localHostStatusHandle
        .getTextLabel()
        .setNextFocusableComponent(localHostStatusHandle.getValueLabel());
    localHostStatusHandle
        .getValueLabel()
        .setNextFocusableComponent(remoteHostStatusHandle.getTextLabel());
    remoteHostStatusHandle
        .getTextLabel()
        .setNextFocusableComponent(remoteHostStatusHandle.getValueLabel());
    remoteHostStatusHandle
        .getValueLabel()
        .setNextFocusableComponent(workingDirectoryStatusHandle.getTextLabel());
    workingDirectoryStatusHandle
        .getTextLabel()
        .setNextFocusableComponent(workingDirectoryStatusHandle.getValueLabel());
    workingDirectoryStatusHandle
        .getValueLabel()
        .setNextFocusableComponent(compareStatusHandle.getTextLabel());
    compareStatusHandle
        .getTextLabel()
        .setNextFocusableComponent(compareStatusHandle.getValueLabel());
    compareStatusHandle
        .getValueLabel()
        .setNextFocusableComponent(filterStatusHandle.getTextLabel());
    filterStatusHandle.getTextLabel().setNextFocusableComponent(filterStatusHandle.getValueLabel());
    filterStatusHandle
        .getValueLabel()
        .setNextFocusableComponent(showErrorsWarningsStatusHandle.getTextLabel());
    showErrorsWarningsStatusHandle
        .getTextLabel()
        .setNextFocusableComponent(showErrorsWarningsStatusHandle.getValueLabel());

    return statusPanel;
  }

  public StatusLabelHandle getTableStatusHandle() {
    return tableStatusHandle;
  }

  public SystemProgressPanel getSystemProgressPanel() {
    return systemProgressPanel;
  }

  /** Called when IPC successfully started when connecting or re-connecting (remote). */
  public void initializeAfterIPCStarted() {
    if (!analyzer.IPC_started) {
      return;
    }
    initView(0, -1);
    analyzer.initWorkingDirectory();
    updateWorkingDirectoryStatus();
  }

  // **********************************************************************************************
  private void updateViews(ViewsSetting.Setting oldSetting, ViewsSetting.Setting newSetting) {
    AnUtility.checkIfOnAWTThread(true);
    if (ViewsSetting.viewsChanged(oldSetting, newSetting)) {
      addVisibleViewsInternal();
    }
    updateSelectedDisplay();
  }

  private void updateViewMode() {
    viewModeComboBox.setSelectedItem(getSettings().getViewModeSetting().get());
  }

  private void updateViewModeVisibility() {
    boolean visible = getSettings().getViewModeEnabledSetting().isViewModeEnabled();
    updateViewModeVisibility(visible);
  }

  private void updateViewModeVisibility(boolean val) {
    AnUtility.checkIfOnAWTThread(true);
    viewModeSeparator.setVisible(val);
    viewModeLabel.setVisible(val);
    viewModeComboBox.setVisible(val);
    viewModeFiller.setVisible(val);
  }

  /**
   * @return the filters
   */
  public Filters getFilters() {
    return filters;
  }

  /**
   * @return the experimentNames
   */
  public String[][] getExperimentGroups() {
    return experimentGroups;
  }

  public boolean isExperimentGroupsSimple() {
    String[][] groups = getExperimentGroups();
    if (groups == null) {
      return true;
    }
    boolean simple = true;
    for (int i = 0; i < groups.length; i++) {
      if (groups[i].length > 1) {
        simple = false;
        break;
      }
    }
    return simple;
  }

  /**
   * @return whether experiments are loaded
   */
  public boolean experimentsLoaded() {
    return getExperimentGroups() != null && getExperimentGroups().length > 0;
  }

  public boolean hasErrorsAndWarnings() {
    return hasErrorsAndWarnings;
  }

  public AnDispTab getWelcomeDispTab() {
    if (welcomeDispTab == null) {
      welcomeDispTab = new AnDispTab(AnDisplay.DSP_Welcome, "welcome", null, null);
    }
    return welcomeDispTab;
  }

  public AnDispTab getOverviewDispTab() {
    if (overviewDispTab == null) {
      overviewDispTab = new AnDispTab(AnDisplay.DSP_Overview, "overview", null, null);
    }
    return overviewDispTab;
  }

  public ViewsPanel getViewsPanel() {
    return getNavigationPanel().getViewsPanel();
  }

  public ViewDisplayPanel getViews() {
    return viewDisplayPanel;
  }

  public NavigationPanel getNavigationPanel() {
    return navigationPanel;
  }

  public StatusPanel getStatusPanel() {
    return statusPanel;
  }

  public Analyzer getAnalyzer() {
    return analyzer;
  }

  public IPC IPC() {
    return analyzer.IPC_session;
  }

  public StatusLabelValueHandle getFilterStatusHandle() {
    return filterStatusHandle;
  }

  private void updateCompareModeStatus() {
    if (compareStatusHandle != null && getSettings() != null) {
      if (getSettings().getCompareModeSetting().comparingExperiments()) {
        compareStatusHandle.update(AnLocale.getString("on"), StatusLabelValueHandle.Mode.SET);
      } else {
        compareStatusHandle.update(AnLocale.getString("off"), StatusLabelValueHandle.Mode.DEFAULT);
      }
    }
  }

  public ToolBarPanel getToolBarPanel() {
    return toolBarPanel;
  }

  public void updateWorkingDirectoryStatus() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            String wd = analyzer.getWorkingDirectory();
            int i = wd.lastIndexOf("/");
            if (i > 0) {
              wd = "..." + wd.substring(i);
            }
            workingDirectoryStatusHandle.updateToolTip(
                AnLocale.getString("Current working directory: ") + analyzer.getWorkingDirectory());
            workingDirectoryStatusHandle.update(wd);
          }
        });
  }

  public WelcomeView getWelcomeView() {
    return welcomeView;
  }

  public OverviewView getOverviewView() {
    return overviewView;
  }

  private SummaryPanel getSummaryPanel() {
    return summaryPanel;
  }

  public FunctionsView getFunctionsView() {
    return functionsView;
  }

  public CallerCalleesView getCallerCalleesView() {
    return callerCalleesView;
  }

  public CallTreeView getCallTreeView() {
    return callTreeView;
  }

  public FlameView getFlameView() {
    return flameView;
  }

  public SourceDisassemblyView getSourceDisassemblyView() {
    return sourceDisassemblyView;
  }

  public SourceView getSourceView() {
    return sourceView;
  }

  public LinesView getLinesView() {
    return linesView;
  }

  public DisassemblyView getDisassemblyView() {
    return disassemblyView;
  }

  public PCsView getPcsView() {
    return pcsView;
  }

  public TimelineView getTimelineView() {
    return timelineView;
  }

  public ExperimentsView getExperimentsView() {
    return experimentsView;
  }

  public IOView getIOView() {
    return ioView;
  }

  public HeapView getHeapView() {
    return heapView;
  }

  public StatisticsView getStatisticsView() {
    return statisticsView;
  }

  public DataLayoutView getDataLayoutView() {
    return dataLayoutView;
  }

  public DataObjectsView getDataObjectsView() {
    return dataObjectsView;
  }

  public DualSourceView getDualSourceView() {
    return dualSourceView;
  }

  public InstructionFrequencyView getInstructionFrequencyView() {
    return instructionFrequencyView;
  }

  public CalledByCallsSourceView getCalledByCallsSourceView() {
    return calledByCallsSourceView;
  }

  public CalledByCallsDisassemblyView getCalledByCallsDisassemblyView() {
    return calledByCallsDisassemblyView;
  }

  public CalledByCallsFunctionsView getCalledByCallsFunctionsView() {
    return calledByCallsFunctionsView;
  }

  public Settings getSettings() {
    return settings;
  }

  public Experiments getExperimentProperties() {
    return experimentProperties;
  }

  class ViewsMenuListener implements MenuListener {

    private JMenu viewsMenu;

    public ViewsMenuListener(JMenu viewsMenu) {
      this.viewsMenu = viewsMenu;
    }

    @Override
    public void menuCanceled(MenuEvent e) {}

    @Override
    public void menuDeselected(MenuEvent e) {}

    @Override
    public void menuSelected(MenuEvent e) {
      if (experimentsLoaded()) {
        getViewsPanel().createViewsMenu(viewsMenu);
      }
    }
  }

  class MetricsMenuListener implements MenuListener {

    private JMenu metricsMenu;

    public MetricsMenuListener(JMenu metricsMenu) {
      this.metricsMenu = metricsMenu;
    }

    @Override
    public void menuCanceled(MenuEvent e) {}

    @Override
    public void menuDeselected(MenuEvent e) {}

    @Override
    public void menuSelected(MenuEvent e) {
      metricsMenu.removeAll();
      if (experimentsLoaded()) {
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .addMetricMenuSelector(metricsMenu, getViews().getCurrentViewDisplay().getType(), true);

        metricsMenu.add(new JSeparator(SwingConstants.HORIZONTAL));
        for (JComponent component :
            getSettings().getMetricsSetting().createMetricSettingsSelector()) {
          metricsMenu.add(component);
        }
      }
    }
  }

  public void welcomeViewOnly() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            List<AnDispTab> list = new ArrayList<AnDispTab>();
            getViewsPanel().removeAllViews();

            AnDispTab welcome = getWelcomeDispTab();
            welcome.getTComp().setSelected(true);
            welcome.getTComp().setAvailable(true);
            list.add(welcome);
            getViewsPanel().createView(instance, welcome, View.Type.STATIC_VIEW);

            boolean[] selected = new boolean[list.size()];
            for (int i = 0; i < selected.length; i++) {
              selected[i] = true;
            }
            getSettings().getViewsSetting().init(this, list, selected, welcome.getTCmd());
            addVisibleViews();
            showHideNavigationAndSubviews(welcome.getTComp());
          }
        });
  }

  private void welcomeAndOverviewViewsOnly() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            List<AnDispTab> list = new ArrayList<AnDispTab>();
            getViewsPanel().removeAllViews();

            AnDispTab welcome = getWelcomeDispTab();
            welcome.getTComp().setAvailable(true);
            list.add(welcome);
            getViewsPanel().createView(instance, welcome, View.Type.STATIC_VIEW);

            AnDispTab overview = getOverviewDispTab();
            overview.getTComp().setAvailable(true);
            overview.getTComp().setSelected(true);
            list.add(overview);
            getViewsPanel().createView(instance, overview, View.Type.STATIC_VIEW);

            boolean[] selected = new boolean[list.size()];
            for (int i = 0; i < selected.length; i++) {
              selected[i] = true;
            }
            getSettings().getViewsSetting().init(this, list, selected, overview.getTCmd());
            addVisibleViews();
            showHideNavigationAndSubviews(overview.getTComp());
          }
        });
  }

  /** Called every time an experimentsDisp is loaded */
  private void resetSubviews() {
    hideAllSubviews();
    if (selectedDetailsSubview == null) {
      createSubviews();
    }
    getTimelineCallStackSubview().setReadyToShow(false);
    getIoCallStackSubview().setReadyToShow(false);
    getHeapCallStackSubview().setReadyToShow(false);
  }

  /** Called only once */
  private void createSubviews() {
    selectedDetailsSubview =
        new Subview(
            "selectedDetailsSubview",
            AnLocale.getString("Selection Details"),
            getSummaryPanel(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_1);
    selectedDetailsSubviewTimeLine =
        new Subview(
            "selectedDetailsSubviewTimeLine",
            AnLocale.getString("Selection Details "),
            getTimelineView().getDetailsInfoTabComp(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_1);
    timelineCallStackSubview =
        new Subview(
            "timelineCallStackSubview",
            getTimelineView().getStackInfoTabName(),
            getTimelineView().getStackInfoTabComp(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_3);
    ioCallStackSubview =
        new Subview(
            "ioCallStackSubview",
            getIOView().getStackInfoTabName(),
            getIOView().getStackInfoTabComp(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_3);
    heapCallStackSubview =
        new Subview(
            "heapCallStackSubview",
            getHeapView().getStackInfoTabName(),
            getHeapView().getStackInfoTabComp(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_3);
    calledByCallsSrcSubview =
        new Subview(
            "calledByCallsSrcSubview",
            AnLocale.getString("Called-by / Calls"),
            getCalledByCallsSourceView(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_2);
    calledByCallsDisSubview =
        new Subview(
            "calledByCallsDisSubview",
            AnLocale.getString("Called-by / Calls"),
            getCalledByCallsDisassemblyView(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_2);
    calledByCallsFuncSubview =
        new Subview(
            "calledByCallsFuncSubview",
            AnLocale.getString("Called-by / Calls"),
            getCalledByCallsFunctionsView(),
            MainViewPanel.SubviewArea.SUBVIEW_AREA_2);

    // Add all subviews to list
    subviewList.clear();
    subviewList.add(selectedDetailsSubview);
    subviewList.add(selectedDetailsSubviewTimeLine);
    subviewList.add(timelineCallStackSubview);
    subviewList.add(ioCallStackSubview);
    subviewList.add(heapCallStackSubview);
    subviewList.add(calledByCallsSrcSubview);
    subviewList.add(calledByCallsDisSubview);
    subviewList.add(calledByCallsFuncSubview);
  }

  private void hideAllSubviews() {
    mainViewPanel.hideAllSubviews();
  }

  public void setSubviewReadyToShow(String subviewName, boolean value) {
    Subview subview = mainViewPanel.findSubview(subviewName);
    if (subview != null && subview.isReadyToShow() != value) {
      subview.setReadyToShow(value);
      showHideNavigationAndSubviews(getViews().getCurrentViewDisplay());
    }
  }

  private void showHideNavigationAndSubviews(AnDisplay anDisplay) {
    // show/hide subviews
    List<Subview> visibleSubviews = anDisplay.getVisibleSubviews();
    if (visibleSubviews != null) {
      for (Subview subview : subviewList) {
        if (subview != null) {
          boolean visible = visibleSubviews.contains(subview);
          subview.showSubview(visible);
        }
      }
    }
    // set selected subviews
    List<Subview> selectedSubviews = anDisplay.getSelectedSubviews();
    if (selectedSubviews != null) {
      for (Subview subview : selectedSubviews) {
        subview.setSelected();
      }
    }
    // Toolbar
    JPanel toolbarPanel = anDisplay.getToolbarPanel();
    if (toolbarPanel != null) {
      getToolBarPanel().setControls(toolbarPanel);
    }

    // Show or hide navigation area?
    getNavigationPanelSplitPane().setHidden(getViewsPanel().onlyWelcomeView());

    // Show or hide sub view areas?
    mainViewPanel.showHideSubviewAreas();
    return;
  }

  public void showSubview(Subview subview, boolean show) {
    subview.showSubview(show);
    mainViewPanel.showHideSubviewAreas(); // check whether to show or hide a subview area
  }

  public Subview getSelectedDetailsSubview() {
    return selectedDetailsSubview;
  }

  public Subview getSelectedDetailsSubviewTimeLine() {
    return selectedDetailsSubviewTimeLine;
  }

  public Subview getTimelineCallStackSubview() {
    return timelineCallStackSubview;
  }

  public Subview getIoCallStackSubview() {
    return ioCallStackSubview;
  }

  public Subview getHeapCallStackSubview() {
    return heapCallStackSubview;
  }

  public Subview getCalledByCallsSrcSubview() {
    return calledByCallsSrcSubview;
  }

  public Subview getCalledByCallsDisSubview() {
    return calledByCallsDisSubview;
  }

  public Subview getCalledByCallsFuncSubview() {
    return calledByCallsFuncSubview;
  }

  /**
   * Called when clicking on a view
   *
   * @param cmd
   */
  public void setSelectedView(String cmd) {
    setSelectedViewInternal(cmd); // direct call allowing to switch views during doCompute.
  }

  /**
   * Called from other views when switching view. View may not be viewing. Can be called from any
   * thread.
   *
   * @param dtype
   */
  public void setSelectedView(int dtype) {
    ViewsSetting.View view = settings.getViewsSetting().findView(dtype);
    if (view != null) {
      final String cmd = view.getAnDispTab().getTCmd();
      if (!getSettings().getViewsSetting().isAvailableAndShowing(cmd)) {
        getSettings().getViewsSetting().toggleTab(cmd, cmd); // Will fire AnChangeEvent
      } else {
        //                getSettings().getViewsSetting().setPreferredViewName(this, cmd); // Will
        // fire AnChangeEvent
        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              @Override
              public void run() {
                setSelectedViewInternal(
                    cmd); // direct call allowing to switch views during doCompute.
              }
            });
      }
    }
  }

  private void setSelectedViewInternal(String cmd) {
    //        System.out.println("setSelectedViewInternal " + cmd + " " + viewStackDirection);
    AnUtility.checkIfOnAWTThread(true);
    if (getSettings().getViewsSetting().isAvailableAndShowing(cmd)) {
      boolean viewChanged = viewDisplayPanel.viewComponent(cmd);
      if (viewChanged) {
        changeView();
        getNavigationPanel().getViewsPanel().setSelectedComponent(cmd);
      }
    }
  }

  private void addVisibleViews() {
    AnUtility.checkIfOnAWTThread(true);
    addVisibleViewsInternal();
    updateSelectedDisplay();
  }

  private void addVisibleViewsInternal() {
    AnUtility.checkIfOnAWTThread(true);
    getNavigationPanel().getViewsPanel().resetAllViews();
    List<AnDispTab> list = getSettings().getViewsSetting().getSelectedViews();
    for (AnDispTab anDispTab : list) {
      getNavigationPanel().getViewsPanel().showView(anDispTab);
    }
    getNavigationPanel().getViewsPanel().refresh();
  }

  private void updateSelectedDisplay() {
    boolean nameChanged = false;
    String currentViewName = getViews().getCurrentViewName();
    String preferredViewName = getSettings().getViewsSetting().getPreferredViewName();
    String newPreferredViewName = preferredViewName;

    if (preferredViewName != null
        && getSettings().getViewsSetting().isAvailableAndShowing(preferredViewName)) {
      newPreferredViewName = preferredViewName;
      nameChanged = true;
    } else if (currentViewName != null
        && getSettings().getViewsSetting().isAvailableAndShowing(currentViewName)) {
      newPreferredViewName = currentViewName;
      nameChanged = true;
    } else {
      ViewsSetting.View dataView = getSettings().getViewsSetting().getFirstDataView();
      if (dataView != null) {
        newPreferredViewName = dataView.getAnDispTab().getTCmd();
        nameChanged = true;
      }
    }
    if (nameChanged) {
      setSelectedViewInternal(newPreferredViewName);
    }
  }

  /**
   * Enable/disable session actions Called at initMetricListsByMType Called when loading
   * experiment(s)
   */
  public void enableSessionActions(boolean loadingExperiments, boolean experimentsLoaded) {
    AnUtility.checkIfOnAWTThread(true);
    boolean connected = Analyzer.getInstance().isConnected();

    // Hide if no experiment and during experiment loading
    filterStatusHandle.setVisible(!loadingExperiments && experimentsLoaded);

    // Hide during experiment loading
    compareStatusHandle.setVisible(!loadingExperiments);
    workingDirectoryStatusHandle.setVisible(!loadingExperiments);
    if (debugStatusHandle != null) {
      debugStatusHandle.setVisible(!loadingExperiments);
    }

    // Disable during experiment loading
    connectAction.setEnabled(!loadingExperiments);
    remoteHostStatusHandle.setEnabled(!loadingExperiments);

    // Disable during experiment loading and enabled only if connected
    profileApplicationAction.setEnabled(!loadingExperiments && connected);
    profileKernelAction.setEnabled(
        !loadingExperiments && connected && Analyzer.getInstance().isKernelProfilingEnabled());
    profileRunningProcessAction.setEnabled(!loadingExperiments && connected);
    openExperimentAction.setEnabled(!loadingExperiments && connected);
    recentExperimentsMenu.setEnabled(!loadingExperiments && connected);
    compareExperimentsAction.setEnabled(!loadingExperiments && connected);
    aggregateExperimentsAction.setEnabled(!loadingExperiments && connected);

    // Disable during experiment loading and enabled only if experiments loaded
    metricsMenu.setEnabled(!loadingExperiments && experimentsLoaded);
    settingsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    viewsSettingsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    metricsSettingsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    addRemoveFiltersAction.setEnabled(!loadingExperiments && experimentsLoaded);
    filtersMenu.setEnabled(!loadingExperiments && experimentsLoaded);
    libraryVisibilityAction.setEnabled(!loadingExperiments && experimentsLoaded);
    functionColorsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    exportSettingsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    importSettingsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    exportSettingsAsAction.setEnabled(!loadingExperiments && experimentsLoaded);
    exportAction.setEnabled(!loadingExperiments && experimentsLoaded);
    showErrorsWarningsAction.setEnabled(
        !loadingExperiments && experimentsLoaded && hasErrorsAndWarnings());
    showErrorsWarningsSeparator.setVisible(
        !loadingExperiments && experimentsLoaded && hasErrorsAndWarnings());
    showErrorsWarningsMenuItem.setVisible(
        !loadingExperiments && experimentsLoaded && hasErrorsAndWarnings());
  }

  /** Enable/disable per-view actions Called when switching view */
  private void enablePerViewActions(AnDisplay currentDisplay) {
    getToolBarPanel().getFindTextPanel().setConrolsEnabled(currentDisplay.supportsFindText());
    exportAction.setEnabled(currentDisplay instanceof ExportSupport);
    previousViewAction.setEnabled(getNavigationPanel().getViewsPanel().hasPreviousViewName());
    nextViewAction.setEnabled(getNavigationPanel().getViewsPanel().hasNextViewName());
  }

  public JFrame getFrame() {
    return frame;
  }

  public MainViewPanel getMainViewPanel() {
    return mainViewPanel;
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }

  public AnSplitPane getNavigationPanelSplitPane() {
    return navigationPanelSplitPane;
  }

  public View getSelectedView() {
    return getViewsPanel().getSelectedViewPanel().getView();
  }

  public AnColorChooser getColorChooser() {
    return colorChooser;
  }

  public LibraryVisibilityDialog getLibraryVisibilityDialog() {
    // Create dialog if it is not created yet
    if (libraryVisibilityDialog == null) {
      libraryVisibilityDialog = new LibraryVisibilityDialog(getFrame());
    }
    return libraryVisibilityDialog;
  }

  // AnColorChooser calls this //XXXmpview bogus! // FIXUP: REARCH
  public void setCPUIdleColor(final boolean set, final Color color) {
    getTimelineView().setCPUIdleColor(set, color);
  }

  // Export main view as text
  private String exportAsText(Integer limit, ExportFormat format, Character delimiter) {
    String text = getViews().getCurrentViewDisplay().exportAsText(limit, format, delimiter);
    // Export subviews if format is TEXT
    if (format == ExportFormat.TEXT) {
      // text += ...
    }
    return text;
  }

  /**
   * GUI testing. Use GUITesting.dumpMainview
   *
   * @param maxLines
   * @return
   */
  public String dumpMainview(int maxLines) {
    String text = exportAsText(maxLines, ExportFormat.TEXT, null);
    return text;
  }

  /**
   * GUI testing. Use GUITesting.dumpSubviews
   *
   * @return
   */
  public String dumpSubviews() {
    StringBuilder buf = new StringBuilder();
    buf.append(getMainViewPanel().dumpSubviews());
    return buf.toString();
  }

  /**
   * GUI testing. Use GUITesting.dumpViewsVisibility()
   *
   * @return
   */
  public String dumpViewsVisibility() {
    StringBuilder buf = new StringBuilder();

    buf.append(getMainViewPanel().dumpMainviewAreaVisibility());
    buf.append(getMainViewPanel().dumpSubviewAreasVisibility());

    return buf.toString();
  }

  public String[][] getExperimentGroupsIPC() {
    synchronized (IPC.lock) {
      String task = "Load experiments...";
      IPCContext ipcContext = IPCContext.getCurrentContext();
      ipcContext.setTaskName(task);
      ipcContext.setCancellable(false);
      final IPC ipc = IPC();
      ipc.send("getExperimentsGroups");
      String[][] grops = (String[][]) (ipc.recvObject());
      return grops;
    }
  }

  private void setTitle() {
    String experimentsTitle = null;

    if (getExperimentGroups() == null
        || getExperimentGroups().length == 0
        || getExperimentGroups()[0] == null) {
      experimentsTitle = null;
    } else {
      StringBuilder buf = new StringBuilder();
      int got = 0;
      outer:
      for (int i = 0; i < getExperimentGroups().length; i++) {
        for (int j = 0; j < getExperimentGroups()[i].length; j++) {
          if (getExperimentGroups()[i][j] != null) {
            if (got >= 1) {
              buf.append(", ...");
              break outer;
            }
            buf.append(AnUtility.basename(getExperimentGroups()[i][j]));
            got++;
          }
        }
      }
      experimentsTitle = buf.toString();
      setTitle(experimentsTitle);
    }
  }

  private void setTitle(String experimentsTitle) {
    String title = Analyzer.getAnalyzerReleaseName();
    if (experimentsTitle != null) {
      title = experimentsTitle + "  -  " + title;
    }
    getFrame().setTitle(title);
  }

  private void saveExperimentSettings(String[][] groups) {
    //        System.out.println("AnWindow:saveExperimentSettings");
    if (groups != null && groups.length > 0) {
      String configPath = UserPref.getAsWhenClosedConfigPath(groups[0][0]);
      //            System.out.println("AnWindow:saveExperimentSettings:configPath: " + configPath);

      List<UserPref.What> what = new ArrayList<UserPref.What>();
      what.add(UserPref.What.VIEWS);
      what.add(UserPref.What.METRICS);
      what.add(UserPref.What.TIMELINE);
      what.add(UserPref.What.SOURCEDISASSEMBLY);
      what.add(UserPref.What.CALLTREE);
      what.add(UserPref.What.FORMATS);
      what.add(UserPref.What.SEARCHPATH);
      what.add(UserPref.What.PATHMAP);
      what.add(UserPref.What.FUNCTIONCOLORS);
      what.add(UserPref.What.LIBRARYVISIBILITY);
      what.add(UserPref.What.MISC);
      UserPref.getInstance().save(configPath, what); // Always local!
      UserPref.getInstance().setLastClosedExpConfPath(configPath);
    }
  }

  private void saveAnalyzerSettings() {
    List<UserPref.What> what = new ArrayList<UserPref.What>();
    what.add(UserPref.What.USER);
    UserPref.getInstance().save(UserPref.getAnalyzerInitFilePath(), what);
  }

  private void restoreExperimentSettings(String confPath) {
    if (confPath != null) {
      AnLog.log("Configuration file to be processed: " + confPath);
      boolean homeDefaultConfiguration = confPath.startsWith(UserPref.getAnalyzerDirPath());
      // home default configuration is always on local machine
      if (Analyzer.getInstance().isRemote() && !homeDefaultConfiguration) {
        confPath = AnWindow.copyFromRemote(confPath);
      }
      if (confPath != null) {
        UserPref.getInstance().restore(confPath);
      }
    }
  }

  /** Open and load experiment(s) from chooser */
  public void openExperimentAction() {
    AnChooser ac = getAnChooser(AnLocale.getString("Open Experiment"), AnChooser.EXP_CHOOSER, null);
    int res = ac.showDialog(frame, null);
    if (res == AnChooser.APPROVE_OPTION) {
      String wd = ac.getWorkingDirectory();
      String confPath = ac.getConfiguration();
      boolean alwaysUseThisConf = ac.alwaysUseThisConfiguration();
      List<String> expList = ac.getExperiments();
      if (expList != null && expList.size() > 0) {
        loadExperimentList(expList, false, wd, true, confPath, alwaysUseThisConf);
      }
    }
  }

  /**
   * @param experimentList
   * @param cmpMode
   * @param workingDirectory
   * @param restartEngine
   * @param confPath
   * @param alwaysUseThisConf
   */
  public void loadExperimentList(
      final List<String> experimentList,
      final boolean cmpMode,
      String workingDirectory,
      boolean restartEngine,
      String confPath,
      boolean alwaysUseThisConf) {
    String[][] arr;
    if (cmpMode) {
      arr = new String[experimentList.size()][];
      for (int i = 0; i < experimentList.size(); i++) {
        String[] expName = new String[1];
        expName[0] = experimentList.get(i);
        arr[i] = expName;
      }
    } else if (experimentList.size() > 0) {
      String[] expName = new String[experimentList.size()];
      for (int i = 0; i < experimentList.size(); i++) {
        expName[i] = experimentList.get(i);
      }
      arr = new String[1][];
      arr[0] = expName;
    } else {
      arr = new String[0][];
    }
    loadExperimentGroups(arr, workingDirectory, restartEngine, confPath, alwaysUseThisConf);
  }

  /**
   * @param grops
   * @param workingDirectory
   * @param restartEngine
   * @param confPath
   * @param alwaysUseThisConf
   */
  public void loadExperimentGroups(
      final String[][] grops,
      final String workingDirectory,
      final boolean restartEngine,
      final String confPath,
      final boolean alwaysUseThisConf) {
    AnUtility.checkIfOnAWTThread(true);
    AnUtility.dispatchOnAWorkerThread(
        () -> {
          synchronized (AnVariable.mainFlowLock) {
            loadExperimentGroupsInternal(
                grops, workingDirectory, restartEngine, confPath, alwaysUseThisConf);
          }
        },
        "Set Experiments Thread");
  }

  private void loadExperimentGroupsInternal(
      final String[][] groups,
      final String workingDirectory,
      final boolean restartEngine,
      final String confPath,
      final boolean alwaysUseThisConf) {
    //        AnUtility.checkIfOnAWTThread(false);
    //        System.out.println("loadExperimentGroupsInternal");
    final boolean restart = (experimentsLoaded()) ? restartEngine : false;
    final boolean anyExperiments = groups.length > 0;
    final UserPref userPref = UserPref.getInstance();

    AnUtility.dispatchOnSwingThread(
        () -> {
          showErrorsWarningsStatusHandle.setVisible(false);
          enableSessionActions(true, false);
          // Close all (non-modal) dialogs
          getSettings().showDialog(false);
          colorChooser.setVisible(false);
          saveExperimentSettings(getExperimentGroups());
          if (restartEngine) {
            AnEventManager.getInstance()
                .fireAnChangeEvent(
                    new AnChangeEvent(groups, AnChangeEvent.Type.EXPERIMENTS_LOADING_NEW));
            userPref.resetExperimentPreferences();
          } else {
            AnEventManager.getInstance()
                .fireAnChangeEvent(
                    new AnChangeEvent(
                        groups, AnChangeEvent.Type.EXPERIMENTS_LOADING_ADDED_OR_REMOVED));
          }
        });
    hasErrorsAndWarnings = false;
    IPCContext.newCurrentContext(
        AnLocale.getString("Loading experiment(s)..."),
        IPCContext.Scope.SESSION,
        false,
        AnWindow.this);
    Object progressBarHandle =
        getSystemProgressPanel().progressBarStart(AnLocale.getString("Loading Experiment(s)"));
    resetAllViews();
    if (restart) { // <=== FIXUP: check
      // re-exec gp-display-text
      Analyzer.getInstance()
          .restartEngine(); // Note: do this early so it has a chance to start! FIXUP: need a way to
      // find out when it has starte
    }
    //        getSettings().getMetricsSetting().initMetricListsByMType(); //
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            memoryIndexDisplayList = new ArrayList<AnDisplay>();
            getFilters().resetAllFiltersAction();
            setTitle(null);
            if (anyExperiments) {
              welcomeAndOverviewViewsOnly();
            } else {
              welcomeViewOnly();
            }
            getSettings().getViewModeEnabledSetting().init(false);
            updateViewModeVisibility();
            resetSubviews();
          }
        });

    try {
      final String msg;
      restoreExperimentSettings(confPath); // Writes into UserPref
      if (anyExperiments) {
        try {
          if (workingDirectory != null) {
            synchronized (IPC.lock) {
              analyzer.setWorkingDirectory(workingDirectory); // IPC
            }
          }
        } catch (Exception e) {
        }
      }
      // Set search path and pathmap efter restart but before loading
      if (groups.length > 0 && !GUITesting.getInstance().isRunningUnderGUITesting()) {
        // Accept search path and pathmaps from .gprofng.rc if first time experiment is being loaded
        String experiment = groups[0][0];
        String asClosedPath = UserPref.getAsWhenClosedConfigPath(experiment);
        if (!new File(asClosedPath).exists()) {
          String[][] pathMaps = PathMapSetting.getPathMapsIPC();
          String[] searchPath = SearchPathSetting.getSearchPathIPC();
          UserPref.getInstance().setPathmap(pathMaps);
          UserPref.getInstance().setSearchPath(Arrays.asList(searchPath));
        }
      }
      if (groups.length > 0) {
        getSettings().getSearchPathSetting().init(this, UserPref.getInstance().getSearchPath());
        getSettings().getPathMapSetting().init(this, UserPref.getInstance().getPathmap());
      }
      synchronized (IPC.lock) {
        IPC().send("setExperimentsGroups");
        IPC().send(groups);
        msg = (String) (IPC().recvString());
      }
      if (msg == null) {
        // Experiment(s) did load but may have non-fatal errors or warnings
        getSettings()
            .getFormatSetting()
            .init(
                this,
                FormatSetting.Style.fromValue(userPref.getFormatsStyle()),
                userPref.getFormatsAppendSoName());
        IPCResult compareModeIPC = CompareModeSetting.getCompareModeV2IPCRequest(); // ASYNC IPC
        IPCResult machineModelIPC = ViewsSetting.getMachineModelIPCRequest(); // ASYNC IPC
        // Capture Experiment Data Descriptors and Properties
        experimentProperties.ipcUpdateExps();
        CompareMode compareMode = CompareMode.fromValue(compareModeIPC.getInt());
        getSettings().getCompareModeSetting().init(this, compareMode);
        IPCResult viewModeEnabledIPC =
            ViewModeEnabledSetting.getViewModeEnabledIPCRequest(); // ASYNC IPC
        IPCResult tlEntpropsIPC = TimelineSetting.ipcGetEntityPropsRequest(); // ASYNC IPC
        final boolean viewModeEnabled = viewModeEnabledIPC.getBoolean();
        final Object[] tlEntprops = tlEntpropsIPC.getObjects();
        // Metrics
        getSettings().getMetricsSetting().initMetricListsByMType();
        Object[] ref_data = MetricsSetting.getRefMetricsV2();
        IPCMetricsAPI.getJavaEnableIPC();
        MetricNode metricsRootNode = IPCMetricsAPI.getAllAvailableMetricsIPC(0);
        getSettings()
            .getMetricsSetting()
            .init(
                this,
                userPref.getMetricSelectionList(),
                ref_data,
                metricsRootNode,
                userPref.getMetricReversedSort(),
                userPref.getMetricSortByMTypeList(),
                userPref.getMetricOrderLists());
        // Views. Load machine model and custom index/memmory objects before
        // asking for available views
        final String machineModel;
        final String loadedMachineModel = machineModelIPC.getString();
        String savedMachineModel = userPref.getMachineModel();
        if (savedMachineModel != null && !savedMachineModel.equals(loadedMachineModel)) {
          machineModel = savedMachineModel;
          ViewsSetting.loadMachineModelIPC(machineModel); // SYNC IPC
        } else {
          machineModel = loadedMachineModel;
        }
        final String[] availableMachineModels = ViewsSetting.listMachineModelsIPC();
        final List<CustomObject> customIndexObjects = userPref.getCustomIndexObjects();
        if (customIndexObjects != null) {
          for (CustomObject customObject : customIndexObjects) {
            ViewsSetting.defineIndxObjIPC(
                customObject.getName(),
                customObject.getFormula(),
                customObject.getShortDesc(),
                customObject.getLongDesc());
          }
        }
        final List<CustomObject> customMemoryObjects = userPref.getCustomMemoryObjects();
        if (customMemoryObjects != null) {
          for (CustomObject customObject : customMemoryObjects) {
            ViewsSetting.defineMemObjIPC(
                customObject.getName(),
                customObject.getFormula(),
                customObject.getShortDesc(),
                customObject.getLongDesc());
          }
        }
        // Use "async" IPC calls to minimize the "high latency" effect
        IPCResult standardViewsIPC = ViewsSetting.getTabListInfoIPCRequest(); // ASYNC IPC
        IPCResult standardViewsSelectedIPC =
            ViewsSetting.getTabSelectionStateIPCRequest(); // ASYNC IPC
        IPCResult indexViewsIPC = ViewsSetting.getIndxObjDescriptionsIPCRequest(); // ASYNC IPC
        IPCResult indexViewsSelectedIPC =
            ViewsSetting.getIndxTabSelectionStateIPCRequest(); // ASYNC IPC
        IPCResult memoryViewsIPC = ViewsSetting.getMemObjectsIPCRequest(); // ASYNC IPC
        IPCResult memoryViewsSelectedIPC =
            ViewsSetting.getMemTabSelectionStateIPCRequest(); // ASYNC IPC
        IPCResult libraryVisibilityIPC =
            LibraryVisibilitySetting.getLoadObjectListIPCRequest(0); // ASYNC IPC
        // Get the results from those "async" IPC calls
        final Object[] standardViews = standardViewsIPC.getObjects();
        final boolean[] standardViewsSelected = standardViewsSelectedIPC.getBooleans();
        final Object[] indexViews = indexViewsIPC.getObjects();
        final boolean[] indexViewsSelected = indexViewsSelectedIPC.getBooleans();
        final Object[] memoryViews = memoryViewsIPC.getObjects();
        final boolean[] memoryViewsSelected = memoryViewsSelectedIPC.getBooleans();

        getColorChooser().getColorMap().initRules(userPref.getActiveColorRules(), false);
        Object[] libraryVisibility = libraryVisibilityIPC.getObjects();
        getSettings()
            .getLibraryVisibilitySetting()
            .init(this, libraryVisibility, userPref.getLibraryVisibilitySettings());

        final String[] errors = getExperimentErrorsIPC();
        final String[] warnings = getExperimentWarningsIPC();
        // ------------------- IPC END

        AnUtility.dispatchOnSwingThread(
            new Runnable() {
              @Override
              public void run() {
                if (errors != null || warnings != null) {
                  hasErrorsAndWarnings = true;
                  handleErrorsAndWarnings(errors, warnings);
                }
                getExperimentsView().initErrorsAndWarnings(errors, warnings);

                // Set settings (GUI updates)
                // Note: change events are not fired until experiments finished loading
                getLibraryVisibilityDialog()
                    .initStates(
                        userPref.getLibraryVisibilityJava(),
                        userPref.getLibraryVisibilitySettings(),
                        userPref.getLibraryVisibilityIncludePickList(),
                        userPref.getLibraryVisibilityIncludeFilter(),
                        userPref.getLibraryVisibilityExcludePickList(),
                        userPref.getCurrenLibraryVisibilityExcludeFilter());
                //                        getSettings().getViewModeSetting().init(this,
                // ViewModeSetting.ViewMode.fromValue(viewModeValue));
                //                        getSettings().getFormatSetting().init(this,
                // FormatSetting.Style.fromValue(nameFormat), soName);
                getSettings().getViewModeEnabledSetting().init(this, viewModeEnabled);
                //                        getSettings().getSourceDisassemblySetting().init(this,
                // srcDisSettings);
                getSettings()
                    .getTimelineSetting()
                    .init(
                        this,
                        tlEntprops,
                        userPref.getTimelineTLDataCmd(),
                        userPref.getTimelineGroupDataByButtonName(),
                        userPref.getTimelineStackAlign(),
                        userPref.getTimelineStackDepth(),
                        userPref.getTimelineStackFramePixels(),
                        userPref.getTimelineShowEventStates(),
                        userPref.getTimelineShowEventDensity());
                getSettings()
                    .getViewsSetting()
                    .init(
                        this,
                        userPref.getViewPanelOrder(),
                        groups,
                        standardViews,
                        standardViewsSelected,
                        indexViews,
                        indexViewsSelected,
                        memoryViews,
                        memoryViewsSelected,
                        availableMachineModels,
                        machineModel,
                        customIndexObjects,
                        customMemoryObjects);
                if (restartEngine) {
                  getToolBarPanel()
                      .getFindTextPanel()
                      .initializeFindTexts(userPref.getFindPickList(), null); // Find text
                  getSettings()
                      .getCallTreeSetting()
                      .setThreshold(this, userPref.getCallStackThreshold()); // Call Tree properties
                  getSettings()
                      .getViewModeSetting()
                      .set(
                          null,
                          ViewModeSetting.ViewMode.fromValue(
                              userPref.getFormatsViewMode())); // View Mode property
                  getSettings()
                      .getFormatSetting()
                      .set(
                          this,
                          FormatSetting.Style.fromValue(userPref.getFormatsStyle()),
                          userPref.getFormatsAppendSoName());
                  getSettings()
                      .getSourceDisassemblySetting()
                      .set(null, userPref.getSourceDisassemblySettings()); // View Mode property
                }
                if (comparingExperiments(groups)) {
                  if (userPref.getFormatsCompareMode() != userPref.getFormatsCompareModeDefault()) {
                    getSettings()
                        .getCompareModeSetting()
                        .set(this, CompareMode.fromValue(userPref.getFormatsCompareMode()));
                  }
                }
                getSettings()
                    .getTableSettings()
                    .initWrapMetricNames(userPref.wrapMetricNamesInTables());

                experimentGroups = groups;
                setTitle();
                updatePickList(groups, workingDirectory, confPath, alwaysUseThisConf); //
                enableSessionActions(false, anyExperiments);
                AnEventManager.getInstance()
                    .fireAnChangeEvent(
                        new AnChangeEvent(groups, AnChangeEvent.Type.EXPERIMENTS_LOADED));
              }
            });
      } else {
        AnUtility.dispatchOnSwingThread(
            () -> {
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(groups, AnChangeEvent.Type.EXPERIMENTS_LOADED_FAILED));
              // Error(s)
              handleFatalError(msg);
              if (anyExperiments) {
                String exp = groups[0][0];
                userPref.getExperimentsPicklists().getPicklist().removeExperiment(exp);
              }
              welcomeViewOnly();
              enableSessionActions(false, false);
            });
      }
    } catch (IPCCancelledException ex) {
      System.out.println("\nloadExperiments cancelled...");
      welcomeViewOnly();
    }
    getSystemProgressPanel().progressBarStop(progressBarHandle);
    IPCContext.newCurrentContext(null, IPCContext.Scope.SESSION, false, AnWindow.this);
  }

  private boolean comparingExperiments(String[][] groups) {
    return groups != null && groups.length > 1 && groups[1] != null && groups[1].length > 0;
  }

  private void updatePickList(
      String[][] groups, String workingDirectory, String confPath, boolean alwaysUseThisConf) {
    List<PickListElement> list = new ArrayList<PickListElement>();
    for (String[] group : groups) {
      for (String experiment : group) {
        experiment = AnUtility.toFullPath(experiment);
        list.add(
            new ExperimentPickListElement(
                experiment, workingDirectory, alwaysUseThisConf ? confPath : null));
      }
    }

    UserPref.getInstance().getExperimentsPicklists().getPicklist().addElements(list);
  }

  private void handleFatalError(String msg) {
    JTextArea textArea = new JTextArea();
    textArea.setText(AnLocale.getString("Experiment(s) failed to load.\n\n"));
    textArea.append(msg);
    textArea.setCaretPosition(0);
    textArea.setEditable(false);
    textArea.setOpaque(false);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setOpaque(true);
    textArea.setBackground(AnEnvironment.DEFAULT_DIALOG_BACKGROUND);
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setBorder(BorderFactory.createLineBorder(AnEnvironment.SCROLLBAR_BORDER_COLOR));
    scrollPane.setPreferredSize(new Dimension(700, 160));
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(scrollPane, BorderLayout.CENTER);

    JOptionPane pane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE);
    JDialog dialog = pane.createDialog(frame, AnLocale.getString("Error"));
    dialog.setResizable(true);
    dialog.setVisible(true);
  }

  /**
   * Handle non-fatal errors and warnings
   *
   * @param errors
   * @param warnings
   */
  private void handleErrorsAndWarnings(String[] errors, String[] warnings) {
    showErrorsWarningsStatusHandle.setVisible(false);
    String labelText = null;
    ImageIcon labelIcon = null;
    if (errors != null && warnings != null) {
      labelIcon = AnUtility.errorWarningNewIcon;
      labelText = AnLocale.getString("Errors & Warnings");

    } else if (errors != null) {
      labelIcon = AnUtility.errorNewIcon;
      labelText = AnLocale.getString("Errors");

    } else if (warnings != null) {
      labelIcon = AnUtility.warningNewIcon;
      labelText = AnLocale.getString("Warnings");
    }
    showErrorsWarningsStatusHandle.update(labelText);
    showErrorsWarningsStatusHandle.update(labelIcon);
    showErrorsWarningsStatusHandle.setVisible(true);

    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            showErrorsWarningsTooltipPopup();
          }
        });
  }

  private void showErrorsWarningsTooltipPopup() {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new GridBagLayout());

    int gridx = 0;
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;

    JLabel label0 = new JLabel();
    label0.setText(AnLocale.getString("Experiment has errors or wanings:"));
    label0.setForeground(AnEnvironment.TOOLTIP_POPUP_FOREGROUND_COLOR);
    label0.setFont(label0.getFont().deriveFont(Font.BOLD));
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    panel.add(label0, gridBagConstraints);

    JLabel label1 = new JLabel();
    label1.setFont(label1.getFont().deriveFont(Font.BOLD));
    label1.setText(AnLocale.getString("Click flag for details..."));
    label1.setForeground(AnEnvironment.FILTER_FOREGROUND_COLOR);
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    panel.add(label1, gridBagConstraints);

    ToolTipPopup toolTipPopup =
        new ToolTipPopup(
            showErrorsWarningsStatusHandle.getTextLabel(),
            panel,
            ToolTipPopup.Location.NORTHEAST,
            true);
    toolTipPopup.show(750, 5000);
  }

  /** Drops all experimentsDisp */
  private void dropAllExperiments() {
    if (experimentsLoaded()) {
      AnUtility.checkIPCOnWrongThread(false);
      loadExperimentGroupsInternal(new String[0][0], null, false, null, false);
      AnUtility.checkIPCOnWrongThread(true);
    }
  }

  /** Reset Collect Dialogs */
  public synchronized void resetProfileDialogs() {
    profileApplicationDialog = null;
    profileRunningProcessDialog = null;
    profileKernelDialog = null;
  }

  // *********************************************************************************
  public AnAction getProfileApplicationAction() {
    return profileApplicationAction;
  }

  public AnAction getProfileRunningProcessAction() {
    return profileRunningProcessAction;
  }

  public AnAction getProfileKernelAction() {
    return profileKernelAction;
  }

  public AnAction getOpenExperimentAction() {
    return openExperimentAction;
  }

  public AnAction getCompareExperimentsAction() {
    return compareExperimentsAction;
  }

  public AnAction getAggregateExperimentsAction() {
    return aggregateExperimentsAction;
  }

  public AnAction getShowErrorsWarningsAction() {
    return showErrorsWarningsAction;
  }

  public AnAction getConnectAction() {
    return connectAction;
  }

  public AnAction getExportAction() {
    return exportAction;
  }

  public AnAction getExitAction() {
    return exitAction;
  }

  public AnAction getPreviousViewAction() {
    return previousViewAction;
  }

  public AnAction getNextViewAction() {
    return nextViewAction;
  }

  public AnAction getViewsSettingsAction() {
    return viewsSettingsAction;
  }

  public AnAction getMetricsSettingsAction() {
    return metricsSettingsAction;
  }

  public AnAction getAddRemoveFiltersAction() {
    return addRemoveFiltersAction;
  }

  public AnAction getLibraryVisibilityAction() {
    return libraryVisibilityAction;
  }

  public AnAction getFunctionColorsAction() {
    return functionColorsAction;
  }

  public AnAction getSettingsAction() {
    return settingsAction;
  }

  public AnAction getExportSettingsAction() {
    return exportSettingsAction;
  }

  public AnAction getImportSettingsAction() {
    return importSettingsAction;
  }

  public AnAction getExportSettingsAsAction() {
    return exportSettingsAsAction;
  }

  public AnAction getHelpAnalyzerAction() {
    return helpAnalyzerAction;
  }

  public AnAction getHelpNewFeaturesAction() {
    return helpNewFeaturesAction;
  }

  public AnAction getHelpInformationMapAction() {
    return helpInformationMapAction;
  }

  public AnAction getHelpKeyboardShortcutsAction() {
    return helpKeyboardShortcutsAction;
  }

  public AnAction getHelpShortcutsAction() {
    return helpShortcutsAction;
  }

  public AnAction getHelpTroubleShootingAction() {
    return helpTroubleShootingAction;
  }

  public AnAction getAboutAnalyzerAction() {
    return aboutAnalyzerAction;
  }

  // *********************************************************************************
  public void profileApplicationAction(String[] arguments) {
    if (profileApplicationAction.isEnabled()) {
      if (null == profileApplicationDialog) {
        profileApplicationDialog =
            new CollectDialog(this, getFrame(), Collector.PROFILE_APPLICATION, CollectPanel.title);
      }
      profileApplicationDialog.doDialog(arguments);
    }
  }

  public void profileRunningProcessAction() {
    if (profileRunningProcessAction.isEnabled()) {
      if (null == profileRunningProcessDialog) {
        profileRunningProcessDialog =
            new CollectDialog(
                this, getFrame(), Collector.PROFILE_RUNNING_APPLICATION, CollectPanel.title1);
      }
      profileRunningProcessDialog.doDialog();
    }
  }

  public void profileKernelAction() {
    if (profileKernelAction.isEnabled()) {
      if (null == profileKernelDialog) {
        profileKernelDialog =
            new CollectDialog(this, getFrame(), Collector.SYSTEM_PROFILING, CollectPanel.title2);
      }
      profileKernelDialog.doDialog();
    }
  }

  public void showErrorsWarningsAction() {
    if (showErrorsWarningsAction.isEnabled()) {
      String experimentsViewName = "header";
      getNavigationPanel().getViewsPanel().selectView(experimentsViewName);
    }
  }

  public void aggregateExperimentsAction() {
    if (aggregateExperimentsAction.isEnabled()) {
      String title = AnLocale.getString("Aggregate Experiments");
      CompareAdvancedDialog dlg = new CompareAdvancedDialog(this, getFrame(), false, title);
      dlg.setVisible(true, 0);
    }
  }

  public void compareExperimentsAction() {
    if (compareExperimentsAction.isEnabled()) {
      //        if (GUITesting.getInstance().isRunningUnderGUITesting()) {
      //            compareExperimentsAdvancedDialog();
      //            return;
      //        }
      if (isExperimentGroupsSimple()) {
        CompareSimpleDialog compareSimpleDialog = new CompareSimpleDialog(getFrame());
        compareSimpleDialog.setVisible(true);
      } else {
        compareExperimentsAdvancedDialog();
      }
    }
  }

  public void compareExperimentsAdvancedDialog() {
    String title = AnLocale.getString("Compare Experiments");
    CompareAdvancedDialog dlg = new CompareAdvancedDialog(this, getFrame(), true, title);
    dlg.setVisible(true, 0);
  }

  public void connectAction() {
    if (connectAction.isEnabled()) {
      // Check if there are loaded experimentsDisp
      if (experimentsLoaded()) {
        CloseExperimentDialog closeExperientDialog = new CloseExperimentDialog(getFrame());
        closeExperientDialog.setVisible(true);
        if (closeExperientDialog.getStatus() == AnDialog2.Status.OK) {
          try {
            dropAllExperiments();
          } catch (Exception e) {
            // We can get an exception if IPC is not connected
          }
        } else {
          return;
        }
      }
      Object progressBarHandle =
          getSystemProgressPanel().progressBarStart(AnLocale.getString("Connecting"));
      if (null == connectionChooser) {
        connectionChooser = new ConnectionDialog(this, frame, remoteHostStatusHandle);
      }
      getSystemProgressPanel().progressBarStop(progressBarHandle);
      connectionChooser.setVisible(true, 0);
      // Requesting connectionChooser focus here may not work because unloading experiments
      // may be running in the background and steel the focus. Also requesting focus when
      // experiments have done unloading (AnChangeEvent.EXPERIMENTS_LOADED).
      connectionChooser.requestFocus();
    }
  }

  public void exportAction() {
    if (exportAction.isEnabled()) {
      if (!(getViews().getCurrentViewDisplay() instanceof ExportSupport)) {
        return;
      }
      ExportSupport exportSupport = (ExportSupport) getViews().getCurrentViewDisplay();
      //        List<ExportFormat> formats = exportSupport.getSupportedExportFormats();

      ExportDialog exportDialog = new ExportDialog(frame, exportSupport);
      exportDialog.setVisible(true);
      if (exportDialog.getStatus() == ExportDialog.Status.OK) {
        ExportSupport.ExportFormat format = exportDialog.getExportFormat();

        if (format == ExportFormat.JPG) {
          boolean includeSubvies = exportDialog.getIncludeSubviews();
          String outputFilePath = exportDialog.getOutputFilePath();
          Component component;
          if (includeSubvies) {
            component = getMainViewPanel();
          } else {
            component = getViews().getCurrentViewDisplay();
          }
          Export.exportImage(component, outputFilePath);
        } else {
          Integer limit = exportDialog.getLimit();
          Character delimiter = exportDialog.getDelimiter();
          String text = exportAsText(limit, format, delimiter);
          String outputFilePath = exportDialog.getOutputFilePath();
          Export.exportText(text, outputFilePath);
        }
      }
    }
  }

  public void exitAction() {
    AnUtility.checkIfOnAWTThread(true);
    frame.setVisible(false);
    /* Everything below should be done on a worker thread */
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            saveExperimentSettings(getExperimentGroups());
            if (!GUITesting.getInstance().isRunningUnderGUITesting()) {
              saveAnalyzerSettings();
            }
            UserPref.getInstance().cleanupAsWhenClosedFolder();
            analyzer.IPC_session.destroyIPCProc();
            if (null != analyzer.old_IPC_session) {
              analyzer.old_IPC_session.destroyIPCProc();
            }
            System.exit(0);
          }
        },
        "Exit Thread");
  }

  public void previousViewAction() {
    if (previousViewAction.isEnabled()) {
      String previousViewName = getNavigationPanel().getViewsPanel().previousViewName();
      if (previousViewName != null) {
        //                setSelectedViewInternal(previousViewName, -1);
        getNavigationPanel().getViewsPanel().moveViewStackPointer(-1);
        getNavigationPanel().getViewsPanel().selectView(previousViewName);
      }
    }
  }

  public void nextViewAction() {
    if (nextViewAction.isEnabled()) {
      String nextViewName = getNavigationPanel().getViewsPanel().nextViewName();
      if (nextViewName != null) {
        //                setSelectedViewInternal(nextViewName, 1);
        getNavigationPanel().getViewsPanel().moveViewStackPointer(+1);
        getNavigationPanel().getViewsPanel().selectView(nextViewName);
      }
    }
  }

  public void viewsSettingsAction() {
    if (viewsSettingsAction.isEnabled()) {
      if (experimentsLoaded()) {
        getSettings().showDialog(getSettings().settingsviewsIndex);
      }
    }
  }

  public void metricsSettingsAction() {
    if (metricsSettingsAction.isEnabled()) {
      if (experimentsLoaded()) {
        getSettings().showDialog(getSettings().settingsMetricsIndex);
      }
    }
  }

  public void addRemoveFiltersAction(Object source) {
    if (addRemoveFiltersAction.isEnabled()) {
      Component parent = null;
      if (source != null && (source instanceof Component)) {
        parent = (Component) source;
      }
      getViews().getCurrentViewDisplay().showFilterPopup(parent);
    }
  }

  public void libraryVisibilityAction() {
    if (libraryVisibilityAction.isEnabled()) {
      if (libraryVisibilityDialog == null) {
        libraryVisibilityDialog = new LibraryVisibilityDialog(getFrame());
      }
      libraryVisibilityDialog.setVisible(true);
    }
  }

  public void functionColorsAction() {
    if (functionColorsAction.isEnabled()) {
      colorChooser.setVisible(true);
    }
  }

  public void settingsAction() {
    if (settingsAction.isEnabled()) {
      int index = -1;
      if (getViews().getCurrentViewDisplay().getType() == AnDisplay.DSP_Timeline) {
        index = getSettings().settingsTimelineIndex;
      } else if (getViews().getCurrentViewDisplay().getType() == AnDisplay.DSP_CallTree) {
        index = getSettings().settingsCalltreeIndex;
      } else if (getViews().getCurrentViewDisplay().getType() == AnDisplay.DSP_Source
          || getViews().getCurrentViewDisplay().getType() == AnDisplay.DSP_Disassembly
          || getViews().getCurrentViewDisplay().getType() == AnDisplay.DSP_SourceDisassembly) {
        index = getSettings().settingsSourceDisassemblyIndex;
      } else if (getViews().getCurrentViewDisplay().getType() == AnDisplay.DSP_Overview) {
        index = getSettings().settingsMetricsIndex;
      }
      getSettings().showDialog(index);
    }
  }

  public void exportSettingsAction() {
    if (exportSettingsAction.isEnabled()) {
      new ExportSettingsDialog(frame).setVisible(true);
    }
  }

  public void importSettingsAction() {
    if (importSettingsAction.isEnabled()) {
      new ImportSettingsDialog(frame).setVisible(true);
    }
  }

  public void exportSettingsAsAction() {
    if (exportSettingsAsAction.isEnabled()) {
      getSettings().saveAsDotErRc();
    }
  }

  public void helpAnalyzerAction() {
    if (helpAnalyzerAction.isEnabled()) {
      Analyzer.showHelp(AnVariable.HELP_WelcomeAnalyzer);
    }
  }

  public void helpNewFeaturesAction() {
    if (helpNewFeaturesAction.isEnabled()) {
      Analyzer.showHelp(AnVariable.HELP_NewFeatures);
    }
  }

  public void helpInformationMapAction() {
    if (helpInformationMapAction.isEnabled()) {
      Analyzer.showHelp(AnVariable.HELP_InformationMap);
    }
  }

  public void helpKeyboardShortcutsAction() {
    if (helpKeyboardShortcutsAction.isEnabled()) {
      Analyzer.showHelp(AnVariable.HELP_KeyboardShortcuts);
    }
  }

  public void helpShortcutsAction() {
    if (helpShortcutsAction.isEnabled()) {
      Analyzer.showHelp(AnVariable.HELP_HelpShortcuts);
    }
  }

  public void helpTroubleShootingAction() {
    if (helpTroubleShootingAction.isEnabled()) {
      Analyzer.showHelp(AnVariable.HELP_Troubleshooting);
    }
  }

  public void aboutAnalyzerAction() {
    if (aboutAnalyzerAction.isEnabled()) {
      AboutPanel.showDialog();
    }
  }

  // *********************************************************************************
  /**
   * Get AnFile with correct AnFileSystemView for remote case
   *
   * @param path
   * @return AnFile
   */
  public AnFile getAnFile(final String path) {
    if (path == null) {
      return null;
    }
    AnFile afile = new AnFile(path);
    if (analyzer.remoteConnection != null) {
      AnFileSystemView afsv = AnFileSystemView.getFileSystemView();
      afsv.setAnWindow(this);
      afsv.updateFileAttributes(afile);
    } else {
      File f = new File(path);
      if (!f.exists()) {
        afile.existsFlag = false;
      }
      if (!f.isDirectory()) {
        afile.isDirectoryFlag = false;
      }
    }
    return afile;
  }

  /**
   * Gets current remote AnChooser
   *
   * @return anRemoteFileChooser
   */
  public AnChooser getAnChooserRemote() {
    return anRemoteFileChooser;
  }

  /**
   * return files in remote directory
   *
   * @param remoteDirectoryPath
   * @return
   */
  public File[] getFiles(String remoteDirectoryPath) {
    AnFileSystemView afsv = AnFileSystemView.getFileSystemView();
    File[] ret = afsv.getFiles(new File(remoteDirectoryPath), false);
    return ret;
  }

  /**
   * Get AnChooser with correct AnFileSystemView for remote case
   *
   * @param title
   * @param chooser_type
   * @param st_dir
   * @return AnChooser
   */
  public AnChooser getAnChooser(final String title, final int chooser_type, String st_dir) {
    AnFileSystemView afsv = null;
    String host = null;
    if (analyzer.remoteConnection != null) {
      if (st_dir == null) {
        st_dir = getCurrentRemoteDirectory();
        if (st_dir == null) {
          st_dir = analyzer.getWorkingDirectory();
          if (st_dir == null) {
            st_dir = "/";
          }
        }
      }
      host = AnLocale.getString("Remote host: ") + analyzer.remoteHost;
      afsv = AnFileSystemView.getFileSystemView();
      afsv.setAnWindow(this);
      AnFile dir = new AnFile(st_dir); // NM incomplete initMetricListsByMType
      anRemoteFileChooser = new AnChooser(frame, chooser_type, this, title, dir, host, afsv);
      anRemoteFileChooser.setFileHidingEnabled(true);
      afsv.setAnChooser(anRemoteFileChooser);
      return anRemoteFileChooser;
    } else {
      if (st_dir == null) {
        st_dir = Analyzer.getInstance().getWorkingDirectory();
        String cd = null;
        if (null != anFileChooser) {
          if (null != anFileChooser.getCurrentDirectory()) {
            cd = anFileChooser.getCurrentDirectory().getPath();
            if (null != cd) {
              st_dir = cd;
            }
          }
        }
      }
      afsv = null;
      if (null != analyzer.localHost) {
        host = AnLocale.getString("Host: ") + analyzer.localHost;
      }
      anFileChooser = new AnChooser(frame, chooser_type, this, title, st_dir, host, afsv);
      anFileChooser.setFileHidingEnabled(true);
      return anFileChooser;
    }
  }

  /**
   * Gets Current Remote Directory
   *
   * @return
   */
  public String getCurrentRemoteDirectory() {
    String cd = null;
    if (null != anRemoteFileChooser) {
      if (null != anRemoteFileChooser.getCurrentDirectory()) {
        cd = anRemoteFileChooser.getCurrentDirectory().getPath();
      }
    }
    return cd;
  }

  private void unSelectAllViews() {
    for (AnDisplay anDisplay : standardDisplayList) {
      anDisplay.setSelected(false);
    }
    if (memoryIndexDisplayList != null) {
      for (AnDisplay anDisplay : memoryIndexDisplayList) {
        anDisplay.setSelected(false);
      }
    }
  }

  private void resetAllViews() {
    for (AnDisplay anDisplay : standardDisplayList) {
      anDisplay.setSelected(false);
      anDisplay.setAvailable(false);
    }
    if (memoryIndexDisplayList != null) {
      for (AnDisplay anDisplay : memoryIndexDisplayList) {
        anDisplay.setSelected(false);
        anDisplay.setAvailable(false);
      }
    }
  }

  private void changeView() {
    //            System.out.println("AnWindow:changeView");
    tableStatusHandle.setVisible(false);
    AnUtility.checkIfOnAWTThread(true);
    //        AnMemoryManager.getInstance().checkMemoryUsage();
    getToolBarPanel().removeControls();
    unSelectAllViews();
    final AnDisplay sel = getViews().getCurrentViewDisplay();
    if (sel == null) {
      return;
    }
    showHideNavigationAndSubviews(sel);
    sel.setSelected(true);
    sel.computeOnAWorkerThread(
        null,
        new AbstractAction() { // ?????? FIXUP Don't do Swing stuff on worker thread!!!!
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().getViews().getCurrentViewDisplay().requestFocus();
            enablePerViewActions(sel);
            //                AnMemoryManager.getInstance().checkMemoryUsage();
            //                AnMain.showImportOldSettingsDialog(); // Do this after request view
            // focus....
          }
        });

    // Set table status visible for most table views
    if (sel instanceof FuncListDisp && sel.getType() != AnDisplay.DSP_Callers) {
      tableStatusHandle.setVisible(true);
    }
  }

  public class AnDispTab {

    private final int type;
    private final int subtype;
    private String tname;
    private char tmnem;
    private final String cmd;
    private AnDisplay tcomp;
    private String shortDescr;
    private String longDescr;

    public AnDispTab(int dtype, String cmdstr, String shortDescr, String longDescr) {
      //            System.out.println("AnWindow.AnDispTab standard: " + cmdstr + " " + shortDescr +
      // " " + longDescr);
      type = dtype;
      cmd = cmdstr;
      subtype = 0; // Only relevant for memobj
      this.shortDescr = shortDescr;
      this.longDescr = longDescr;
      initDispTab();
    }

    // fixme,  Code should move to mem/index classes
    public AnDispTab(
        int dtype,
        int sub,
        String sname,
        char mnem,
        String cmdstr,
        String shortDesc,
        String longDesc) {
      //            System.out.println("AnWindow.AnDispTab index/memory: " + dtype + " " + sub + " "
      // + sname + " " + cmdstr);
      type = dtype;
      cmd = cmdstr;
      tname = sname;
      tmnem = mnem;
      subtype = sub;
      this.shortDescr = shortDesc;
      this.longDescr = longDesc;
      String HELP_token_ID = null;

      if (type == AnDisplay.DSP_MemoryObject) {
        HELP_token_ID = AnVariable.HELP_TabsMemObj;
      } else if (type == AnDisplay.DSP_IndexObject) {
        switch (cmd) {
          case "Threads":
            HELP_token_ID = AnVariable.HELP_TabsThreads;
            break;
          case "CPUs":
            HELP_token_ID = AnVariable.HELP_TabsCPUs;
            break;
          case "Samples":
            HELP_token_ID = AnVariable.HELP_TabsSamples;
            break;
          case "GCEvents":
            HELP_token_ID = null; // AnVariable.HELP_TabsIndxObj;
            break;
          case "Seconds":
            HELP_token_ID = AnVariable.HELP_TabsSeconds;
            break;
          case "Processes":
            HELP_token_ID = AnVariable.HELP_TabsProcesses;
            break;
          case "Experiment_IDs":
            HELP_token_ID = AnVariable.HELP_TabsExperimentIDs;
            break;
          case "Datasize":
            HELP_token_ID = AnVariable.HELP_DataSize;
            break;
          case "Duration":
            HELP_token_ID = AnVariable.HELP_Duration;
            break;
          case "OMP_preg":
            HELP_token_ID = AnVariable.HELP_TabsOMPParallelRegions;
            break;
          case "OMP_task":
            HELP_token_ID = AnVariable.HELP_TabsOpenMPTasks;
            break;
          default:
            HELP_token_ID = null; // AnVariable.HELP_TabsIndxObj;
            break;
        }
      }
      //            System.out.println("HELP_token_ID:" + HELP_token_ID);
      MemoryIndexObjectView memoryIndexAnDisplay =
          new MemoryIndexObjectView(dtype, subtype, HELP_token_ID);
      memoryIndexDisplayList.add(memoryIndexAnDisplay);
      tcomp = memoryIndexAnDisplay;
      tcomp.setAnDispTab(this);
      tcomp.setAvailable(true);
    }

    private void initDispTab() {
      switch (type) {
        case AnDisplay.DSP_Welcome:
          tname = AnLocale.getString("Welcome");
          tmnem = AnLocale.getString('W', "MNEM_TAB_WELCOME");
          tcomp = getWelcomeView();
          shortDescr = getWelcomeView().getShortDescr();
          longDescr = getWelcomeView().getLongDescr();
          break;
        case AnDisplay.DSP_Overview:
          tname = AnLocale.getString("Overview");
          tmnem = AnLocale.getString('i', "MNEM_TAB_OVERVIEW");
          tcomp = getOverviewView();
          shortDescr = getOverviewView().getShortDescr();
          longDescr = getOverviewView().getLongDescr();
          break;
        case AnDisplay.DSP_Functions:
          tname = AnLocale.getString("Functions");
          tmnem = AnLocale.getString('F', "MNEM_TAB_FUNCTION");
          tcomp = getFunctionsView();
          break;
        case AnDisplay.DSP_Callers:
          tname = AnLocale.getString("Callers-Callees");
          tmnem = AnLocale.getString('e', "MNEM_TAB_CALL");
          tcomp = getCallerCalleesView();
          break;
        case AnDisplay.DSP_CallTree:
          tname = AnLocale.getString("Call Tree");
          tmnem = AnLocale.getString('t', "MNEM_TAB_CALLTREE");
          tcomp = getCallTreeView();
          break;
        case AnDisplay.DSP_CallFlame:
          // tname = AnLocale.getString("Visual Call Tree");
          // tmnem = AnLocale.getString('v', "MNEM_TAB_FLAME");
          tname = AnLocale.getString("Flame Graph");
          tmnem = AnLocale.getString('m', "MNEM_TAB_FLAME");
          tcomp = getFlameView();
          break;
        case AnDisplay.DSP_Source:
        case AnDisplay.DSP_SourceV2:
          tname = AnLocale.getString("Source");
          tmnem = AnLocale.getString('r', "MNEM_TAB_SOURCE");
          tcomp = getSourceView();
          break;
        case AnDisplay.DSP_Lines:
          tname = AnLocale.getString("Lines");
          tmnem = AnLocale.getString('L', "MNEM_TAB_LINES");
          tcomp = getLinesView();
          break;
        case AnDisplay.DSP_Disassembly:
        case AnDisplay.DSP_DisassemblyV2:
          tname = AnLocale.getString("Disassembly");
          tmnem = AnLocale.getString('D', "MNEM_TAB_DISASM");
          tcomp = getDisassemblyView();
          break;
        case AnDisplay.DSP_PCs:
          tname = AnLocale.getString("PCs");
          tmnem = AnLocale.getString('P', "MNEM_TAB_PCS");
          tcomp = getPcsView();
          break;
        case AnDisplay.DSP_DataLayout:
          tname = AnLocale.getString("DataLayout");
          tmnem = AnLocale.getString('u', "MNEM_TAB_DATALAY");
          tcomp = getDataLayoutView();
          break;
        case AnDisplay.DSP_DataObjects:
          tname = AnLocale.getString("DataObjects");
          tmnem = AnLocale.getString('O', "MNEM_TAB_DATAOBJ");
          tcomp = getDataObjectsView();
          break;
        case AnDisplay.DSP_Timeline:
          tname = AnLocale.getString("Timeline");
          tmnem = AnLocale.getString('T', "MNEM_TAB_TIMELINE_V2");
          tcomp = getTimelineView(); // YXXX what is purpose of above linesDisp?
          break;
          //                case AnDisplay.DSP_LEAKLIST:
          //                    tname = AnLocale.getString("Leaks");
          //                    tmnem = AnLocale.getString('k', "MNEM_TAB_LEAKLIST");
          //                    tcomp = getLeaksView();
          //                    break;
        case AnDisplay.DSP_Heap:
          tname = AnLocale.getString("Heap");
          tmnem = AnLocale.getString('H', "MNEM_TAB_HEAPCALLSTACK");
          tcomp = getHeapView();
          break;
        case AnDisplay.DSP_IO:
          tname = AnLocale.getString("I/O");
          tmnem = AnLocale.getString('A', "MNEM_TAB_IOACTIVITY");
          tcomp = getIOView();
          break;
        case AnDisplay.DSP_Statistics:
          tname = AnLocale.getString("Statistics");
          tmnem = AnLocale.getString('i', "MNEM_TAB_STATIS");
          tcomp = getStatisticsView();
          break;
        case AnDisplay.DSP_Experiments:
          tname = AnLocale.getString("Experiments");
          tmnem = AnLocale.getString('n', "MNEM_TAB_EXPERIMENT");
          tcomp = getExperimentsView();
          break;
        case AnDisplay.DSP_InstructionFrequency:
          tname = AnLocale.getString("Instruction Frequency");
          tmnem = AnLocale.getString('q', "MNEM_TAB_IFREQ");
          tcomp = getInstructionFrequencyView();
          break;
        case AnDisplay.DSP_DualSource:
          tname = AnLocale.getString("Dual Source");
          tmnem = AnLocale.getString('R', "MNEM_TAB_DUALSOURCES");
          tcomp = getDualSourceView();
          break;
        case AnDisplay.DSP_SourceDisassembly:
          tname = AnLocale.getString("Source/Disassembly");
          tmnem = AnLocale.getString('u', "MNEM_TAB_SOURCE_DISASM");
          tcomp = getSourceDisassemblyView();
          break;
        default:
          System.err.println("Anwindow: unknown view: " + type);
          tname = "Unknown";
          //                    tmnem = AnLocale.getString('W', "MNEM_TAB_WELCOME");
          tcomp = getWelcomeView();
          break;
      }
      tcomp.setAnDispTab(this);
      tcomp.setAvailable(true);
    }

    public int getTType() {
      return type;
    }

    public String getTName() {
      return tname;
    }

    public char getTMnem() {
      return tmnem;
    }

    public AnDisplay getTComp() {
      return tcomp;
    }

    public String getTCmd() {
      return cmd;
    }

    public String getShortDescr() {
      return shortDescr;
    }

    public String getLongDesc() {
      return longDescr;
    }
  }

  public void showError(final String msg) {
    if (msg != null) {
      getExperimentsView().appendLog(AnLocale.getString("Error: ") + msg);
      AnUtility.showMessage(frame, msg, JOptionPane.ERROR_MESSAGE);
    }
  }

  public void showWarning(final String msg) {
    if (msg != null) {
      getExperimentsView().appendLog(AnLocale.getString("Warning: ") + msg);
      //            AnUtility.showMessage(frame, msg, JOptionPane.WARNING_MESSAGE);
    }
  }

  public void showProcessorWarning(final String msg) {
    if (msg != null) {
      getExperimentsView().appendLog(msg);
      AnUtility.showMessage(frame, msg, JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Copy file from local host to remote host
   *
   * @param from
   * @param to
   * @return res (number of bytes, or -1 - error)
   */
  public int cp(File from, AnFile to) {
    int res = 0;
    String contents = "";
    // Read file from local host
    try {
      final java.io.FileReader fr = new java.io.FileReader(from);
      java.io.BufferedReader br = new java.io.BufferedReader(fr);
      String s = br.readLine();
      while (null != s) {
        s += "\n"; // Line feed is not passed by readLine()
        res += s.length();
        contents += s;
        s = br.readLine();
      }
    } catch (java.io.IOException ex) {
      return -1; // Error: cannot read file
    }
    // Write file to remote host
    String dir = to.getDirectoryName();
    createDirectories(dir);

    String sto = to.getAbsolutePath();
    //        res = writeFile(sto, " "); // Hack to work-around problem with remote doesn't always
    // create a new file when written to the first time
    res = writeFile(sto, contents);
    //        System.out.println("Remote: " + dir + " " + to + " " + res);

    return res;
  }

  public static String copyFromRemote(String remotePath) {
    AnFile anFile = new AnFile(remotePath);
    String toFilePath = null;
    if (anFile.exists()) {
      File tempFile = tempFile();
      if (tempFile != null) {
        AnWindow.getInstance().cp(anFile, tempFile);
        toFilePath = tempFile.getAbsolutePath();
      }
    }
    return toFilePath;
  }

  public static File tempFile() {
    File tempFile = null;
    try {
      tempFile = File.createTempFile("configuration", ".tmp");
      tempFile.deleteOnExit();
    } catch (IOException ioe) {
      System.err.println(ioe);
    }
    return tempFile;
  }

  public static int copyToRemote(String fromPath, String toRemotePath) {
    String dirs = AnUtility.dirname(toRemotePath);
    new AnFile(dirs).mkdir();
    File fromFile = new File(fromPath);
    AnFile anFile = new AnFile(toRemotePath);
    int res = 0;
    if (fromFile.exists()) {
      res = AnWindow.getInstance().cp(fromFile, anFile);
    }
    return res;
  }

  /**
   * Copy file from remote host to local host
   *
   * @param from
   * @param to
   * @return res (number of bytes, or -1 - error)
   */
  public int cp(AnFile from, File to) {
    // Read file from remote host
    String[] in = readFile(from.getAbsolutePath());
    String contents = in[0];
    // Write file to local host
    if (contents != null) {
      try {
        final java.io.FileWriter fw = new java.io.FileWriter(to);
        java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
        bw.write(contents, 0, contents.length());
        bw.flush();
        bw.close();
        fw.close();
        return contents.length();
      } catch (java.io.IOException ex) {
        return -1; // Error: cannot write file
      }
    }
    return -1;
  }

  // ========================================================================//
  // Native methods from liber_dbe.so
  public void initView(final int win_id, final int clone_id) {
    synchronized (IPC.lock) {
      IPC().send("initView");
      IPC().send(win_id);
      IPC().send(clone_id);
      IPC().recvString(); // synchronize
    }
  }

  /**
   * A method to create directory.
   *
   * @param path - full path
   * @return String - error
   */
  public String createDirectories(final String path) {
    synchronized (IPC.lock) {
      IPC().send("dbeCreateDirectories");
      IPC().send(path);
      return IPC().recvString();
    }
  }

  /**
   * A method to delete file or directory.
   *
   * @param path - full path
   * @return String - error
   */
  public String deleteFile(final String path) {
    synchronized (IPC.lock) {
      IPC().send("dbeDeleteFile");
      IPC().send(path);
      return IPC().recvString();
    }
  }

  /**
   * Read File If the operation was successful, the contents is in the first element, and second
   * element is NULL. If the operation failed, then first element is NULL, and second element
   * contains the error message.
   *
   * @param file_name
   * @return String[] result
   */
  public final String[] readFile(final String file_name) {
    synchronized (IPC.lock) {
      IPC().send("dbeReadFile");
      IPC().send(file_name);
      return (String[]) IPC().recvObject();
    }
  }

  /**
   * Write File
   *
   * @param file_name
   * @param contents
   * @return result (written bytes)
   */
  public final int writeFile(final String file_name, final String contents) {
    synchronized (IPC.lock) {
      IPC().send("dbeWriteFile");
      IPC().send(file_name);
      IPC().send(contents);
      return IPC().recvInt();
    }
  }

  public final String getHomeDir() {
    synchronized (IPC.lock) {
      IPC().send("getHomeDirectory");
      return IPC().recvString();
    }
  }

  // Native methods from liber_dbe.so
  public String[] getExpPreview(final String exp_name) {
    synchronized (IPC.lock) {
      IPC().send("getExpPreview");
      IPC().send(exp_name);
      return (String[]) IPC().recvObject();
    }
  }

  private int[] getUserExpId(final int[] expIds) {
    synchronized (IPC.lock) {
      IPC().send("getUserExpId");
      IPC().send(expIds);
      return (int[]) IPC().recvObject();
    }
  }

  public String[] getExpName() {
    synchronized (IPC.lock) {
      IPC().send("getExpName");
      return (String[]) IPC().recvObject();
    }
  }

  private int[] getExpState() {
    synchronized (IPC.lock) {
      IPC().send("getExpState");
      return (int[]) IPC().recvObject();
    }
  }

  public boolean[] getExpEnable() {
    synchronized (IPC.lock) {
      IPC().send("getExpEnable");
      IPC().send(0);
      return (boolean[]) IPC().recvObject();
    }
  }

  public String[] getExpInfo() {
    synchronized (IPC.lock) {
      IPC().send("getExpInfo");
      IPC().send(0);
      return (String[]) IPC().recvObject();
    }
  }

  protected String composeFilterClause(
      final int type, final int subtype, final int[] selected_ids) {
    synchronized (IPC.lock) {
      IPC().send("composeFilterClause");
      IPC().send(0);
      IPC().send(type);
      IPC().send(subtype);
      IPC().send(selected_ids);
      return IPC().recvString();
    }
  }

  public String getMsg(final int type) {
    synchronized (IPC.lock) {
      IPC().send("getMsg");
      IPC().send(0);
      IPC().send(type);
      return IPC().recvString();
    }
  }

  // Get experimentsDisp list with icons
  public AnTextIcon[] getExperimentList() {
    final String[] names;
    final int[] state;
    final AnTextIcon[] exp_list;
    final int size;
    ImageIcon ex_icon;

    names = getExpName(); // IPC
    state = getExpState(); // IPC

    if (names == null) {
      return null;
    }

    int expIds[] = new int[names.length];
    for (int i = 0; i < expIds.length; i++) {
      expIds[i] = i;
    }
    int userExpIds[] = getUserExpId(expIds); // IPC
    int maxUserExpId = 0;
    for (int id : userExpIds) {
      if (id > maxUserExpId) {
        maxUserExpId = id;
      }
    }

    // Get icons for experimentsDisp
    size = names.length;
    exp_list = new AnTextIcon[size];
    for (int i = 0; i < size; i++) {
      if (state[i] == EXP_SUCCESS) {
        ex_icon = AnUtility.expt_icon;
      } else if ((state[i] & EXP_OBSOLETE) != 0) {
        ex_icon = AnUtility.eold_icon;
      } else if ((state[i] & EXP_BROKEN) != 0) {
        ex_icon = AnUtility.ebad_icon;
      } else {
        ex_icon = AnUtility.ewarn_icon;
      }

      String padding = "";
      if (maxUserExpId >= 10 && userExpIds[i] < 10) {
        padding = "  ";
      }
      exp_list[i] = new AnTextIcon(padding + userExpIds[i] + "  " + names[i], ex_icon);
    }
    return exp_list;
  }

  public SelectedObject getSelectedObject() {
    return selectedObject;
  }

  public SelectionManager getSelectionManager() {
    return selectionManager;
  }

  public String[] getStackNames(final long stack) {
    synchronized (IPC.lock) {
      IPC().send("getStackNames");
      IPC().send(0);
      IPC().send(stack);
      return (String[]) IPC().recvObject();
    }
  }

  // Histable::Function*
  public long[] getStackFunctions(final long stack) {
    synchronized (IPC.lock) {
      IPC().send("getStackFunctions");
      IPC().send(stack);
      return (long[]) IPC().recvObject();
    }
  }

  // Histable::Function*
  public Object[] getStacksFunctions(final long stacks[]) {
    synchronized (IPC.lock) {
      IPC().send("getStacksFunctions");
      IPC().send(stacks);
      return (Object[]) IPC().recvObject();
    }
  }

  // Histable::DbeInstr*
  public long[] getStackPCs(final long stack) {
    synchronized (IPC.lock) {
      IPC().send("getStackPCs");
      IPC().send(stack);
      return (long[]) IPC().recvObject();
    }
  }

  public String getFuncName(final long func) {
    synchronized (IPC.lock) {
      IPC().send("getFuncName");
      IPC().send(0);
      IPC().send(func);
      return IPC().recvString();
    }
  }

  public String[] getFuncNames(final long[] funcs) {
    synchronized (IPC.lock) {
      IPC().send("getFuncNames");
      IPC().send(0);
      IPC().send(funcs);
      return (String[]) IPC().recvObject();
    }
  }

  public long[] getFuncIds(final long[] funcs) {
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      IPC().send("getFuncIds");
      IPC().send(0);
      IPC().send(funcs);
      long[] ll = (long[]) IPC().recvObject();
      AnUtility.checkIPCOnWrongThread(true);
      return ll;
    }
  }

  public String getFilterStr() {
    synchronized (IPC.lock) {
      IPC().send("getFilterStr");
      IPC().send(0);
      return IPC().recvString();
    }
  }

  /**
   * A generic method to get data for Analyzer tabs.
   *
   * @param mlistStr metric list: "MET_NORMAL", "MET_CALL", "MET_CALL_AGR", ...
   * @param modeStr mode: "ALL", "CALLERS", "CALLEES", "SELF"
   * @param typeStr type: "FUNCTION", "INDEXOBJ"
   * @param subtypeStr string of subtype for index objects, e.q. "0", "1", ...
   * @param cstack array of function IDs (can be null if modeStr="ALL")
   * @return
   */
  public Object[] getTableDataV2(
      final String mlistStr,
      final String modeStr,
      final String typeStr,
      final String subtypeStr,
      final long[] cstack) {
    synchronized (IPC.lock) {
      IPC().send("getTableDataV2");
      IPC().send(0);
      IPC().send(mlistStr);
      IPC().send(modeStr);
      IPC().send(typeStr);
      IPC().send(subtypeStr);
      IPC().send(cstack);
      return (Object[]) IPC().recvObject();
    }
  }

  public String getObjNameV2(final long id) {
    synchronized (IPC.lock) {
      IPC().send("getObjNameV2");
      IPC().send(0);
      IPC().send(id);
      return IPC().recvString();
    }
  }

  public String[] getObjNamesV2(final long[] ids) {
    synchronized (IPC.lock) {
      IPC().send("getObjNamesV2");
      IPC().send(0);
      IPC().send(ids);
      return (String[]) IPC().recvObject();
    }
  }

  public int[] getGroupIds() {
    synchronized (IPC.lock) {
      final IPC ipc = IPC();
      ipc.send("getGroupIds");
      ipc.send(0);
      return (int[]) ipc.recvObject();
    }
  }

  private String[] getExperimentWarningsIPC() {
    String s = "";
    IPC().send("getExpsProperty"); // IPC
    IPC().send("WARNINGS"); // IPC
    String[] msgs = (String[]) IPC().recvObject();
    return msgs;
  }

  private String[] getExperimentErrorsIPC() {
    String s = "";
    IPC().send("getExpsProperty"); // IPC
    IPC().send("ERRORS"); // IPC
    String[] msgs = (String[]) IPC().recvObject();
    return msgs;
  }

  /**
   * Check Remote Connection
   *
   * @param s
   * @return s
   */
  public String checkConnection(final String s) {
    synchronized (IPC.lock) {
      IPC().send("checkConnection");
      IPC().send(s);
      return IPC().recvString();
    }
  }
}
