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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public final class AnVariable {

  public static final Object mainFlowLock = new Object();
  // Runtime environment
  public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
  // Preferred color
  public static final Color LABEL_COLOR = Color.black; // regular label
  public static final Color CCOMP_COLOR = new Color(46, 146, 199); // comp-comm
  public static final Color DISRC_COLOR = new Color(117, 117, 117); // src for dis
  public static final Color QUOTE_COLOR = new Color(199, 0, 0); // quote
  public static final Color HILIT_COLOR = Color.ORANGE; // flagged
  public static final Color HILIT_INC_COLOR = new Color(255, 245, 0); // flagged
  public static final Color HOTLINE_COLOR = new Color(255, 165, 0); // flagged
  public static final Color WARMLINE_COLOR = new Color(215, 215, 225); // flagged
  public static final Color COMENTARY_COLOR =
      new Color(233, 239, 248); // background color for commentary
  public static final Color SRCDIS_COLOR =
      new Color(245, 245, 245); // background color for source in disasm view
  public static final Color KEYWORD_COLOR = new Color(0, 0, 230); // syntax highlighting
  public static final Color COMMENT_COLOR = new Color(155, 155, 155); // syntax highlighting
  public static final Color DIRECTIVE_COLOR = new Color(0, 155, 0); // syntax highlighting
  public static final Color STRING_COLOR = new Color(206, 123, 0); // syntax highlighting
  public static final Color LINENO_COLOR = new Color(155, 155, 183); // syntax highlighting
  public static final Color LINENO_COLOR_BOLD = new Color(95, 95, 123); // syntax highlighting
  public static final Color OPCODE_COLOR = new Color(0, 155, 0); // syntax highlighting
  public static final Color OPRANDREG_COLOR = new Color(46, 146, 199); // syntax highlighting
  public static final Color OPRANDVALUE_COLOR = new Color(153, 51, 204); // syntax highlighting
  public static final Color REGSEL_COLOR =
      new Color(135, 206, 250); // background color for selected reg
  public static final Color WARNINGPANEL_COLOR =
      new Color(255, 255, 199); // background color for warning panel
  public static final Color JMPOTHER_COLOR =
      new Color(0, 185, 0); // forground color for jump forward address
  public static final Color JMPBACK_COLOR =
      new Color(243, 143, 0); // forground color for jump back address
  public static final Color JMPOUTSIDE_COLOR =
      Color.RED; // forground color for jump outside function address
  // Preferred windows size
  public static final int WIN_WIDTH, WIN_HEIGHT;
  private static final int INFO_PRF_WIDTH, INFO_MIN_WIDTH;
  private static final int FVIEW_WIDTH, FVIEW_HEIGHT;
  public static final Dimension WIN_SIZE;
  public static final Dimension INFO_PRF_SIZE;
  public static final Dimension INFO_MIN_SIZE;
  public static final Dimension FVIEW_SIZE;

  static {
    WIN_WIDTH = 850;
    INFO_PRF_WIDTH = 370;

    WIN_HEIGHT = 450;
    INFO_MIN_WIDTH = 200;
    FVIEW_WIDTH = 650;
    FVIEW_HEIGHT = 400;
    WIN_SIZE = new Dimension(WIN_WIDTH, WIN_HEIGHT);
    INFO_PRF_SIZE = new Dimension(INFO_PRF_WIDTH, WIN_HEIGHT);
    INFO_MIN_SIZE = new Dimension(INFO_MIN_WIDTH, WIN_HEIGHT);
    FVIEW_SIZE = new Dimension(FVIEW_WIDTH, FVIEW_HEIGHT);
  }

  private static final int HELP_WIDTH = 600;
  private static final int HELP_HEIGHT = 700;
  public static final Dimension HELP_SIZE = new Dimension(HELP_WIDTH, HELP_HEIGHT);
  // Label with left/right margin space
  private static final int sizeLabel = 4;
  public static final int sizeIcon = 1;
  public static final Border labelBorder = new EmptyBorder(0, sizeLabel, 0, sizeLabel);
  public static final Border fieldBorder = new EmptyBorder(sizeLabel, 0, sizeLabel, 0);
  public static final Border iconBorder = new EmptyBorder(0, sizeIcon, 0, sizeIcon);
  public static final Border boxBorder =
      new EmptyBorder(sizeLabel, sizeLabel, sizeLabel, sizeLabel);
  public static final Border textBorder = UIManager.getBorder("TextField.border");
  public static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
  public static final Border listFocusBorder = UIManager.getBorder("List.focusCellHighlightBorder");
  public static final Border tableFocusBorder =
      UIManager.getBorder("Table.focusCellHighlightBorder");
  public static final Border treeFocusBorder =
      new LineBorder(UIManager.getColor("Tree.selectionBorderColor"));
  // Button margin
  public static final Insets buttonMargin = new Insets(1, 1, 1, 1);
  // All chars
  public static final String all_chars =
      "abcdefghijklmnopqrstuvwxyz"
          + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          + "1234567890-=\\`[];',./"
          + "!@#$%^&*()_+|~{}:\"<>?";

  // Help mapIDs
  public static final String HELP_WelcomeAnalyzer = "Welcome_Analyzer";
  public static final String HELP_InformationMap = "Information";
  public static final String HELP_NewFeatures = "Welcome_WhatsNew";
  public static final String HELP_QuickReference = "Navigate_QuickRef";
  public static final String HELP_KeyboardShortcuts = "Navigate_Keyboard";
  public static final String HELP_HelpShortcuts = "Navigate_KbdNavHelp";
  public static final String HELP_Troubleshooting = "Trouble_Trouble";
  public static final String HELP_Welcome = "WelcomeView"; // "Tabs_Welcome";
  public static final String HELP_Export = null; // "Tabs_Welcome";
  public static final String HELP_Overview = "Tabs_Overview";
  public static final String HELP_TabsFunctions = "Tabs_Functions";
  public static final String HELP_TabsCallersCallees = "Tabs_CallersCallees";
  public static final String HELP_CallFlameChart = "CallFlameChart";
  public static final String HELP_TabsCallTree = "Tabs_CallTree";
  public static final String HELP_TabsSource = "Tabs_Source";
  public static final String HELP_TabsLines = "Tabs_Lines";
  public static final String HELP_TabsDisassembly = "Tabs_Disassembly";
  public static final String HELP_TabsPCs = "Tabs_PCs";
  public static final String HELP_TabsDataLayout = "Tabs_DataLayout";
  public static final String HELP_TabsDataObjects = "Tabs_DataObjects";
  public static final String HELP_TabsTimeline = "Tabs_Timeline";
  public static final String HELP_TabsStatistics = "Tabs_Statistics";
  public static final String HELP_TabsExperiments = "Tabs_Experiments";
  public static final String HELP_TabsExperimentIDs = "Tabs_ExperimentIDs";
  public static final String HELP_TabsLeaklist = "Tabs_Leaklist";
  public static final String HELP_TabsHeap = "Tabs_Heap";
  public static final String HELP_TabsMemObj = "Tabs_MemObj";
  public static final String HELP_TabsIndxObj = "Tabs_IndxObj";
  public static final String HELP_TabsIFreq = "Tabs_IFreq";
  public static final String HELP_TabsSummary = "Tabs_Summary";
  public static final String HELP_TabsEvent = "Tabs_Event";
  public static final String HELP_TabsLegend = "Tabs_Legend";
  public static final String HELP_TabsLeak = "Tabs_Leak";
  public static final String HELP_TabsProcesses = "Tabs_Processes";
  public static final String HELP_TabsRaceList = "Tabs_RaceList";
  public static final String HELP_TabsRaceDetails = "Tabs_RaceDetails";
  public static final String HELP_TabsRaceDualSrc = "DualSource";
  public static final String HELP_TabsSrcDisassm = "Tabs_SrcDisassm";
  public static final String HELP_TabsThreads = "Threads";
  public static final String HELP_TabsCPUs = "CPUs";
  public static final String HELP_TabsSamples = "Samples";
  public static final String HELP_TabsSeconds = "Seconds";
  public static final String HELP_TabsThreadsChart = "Threads";
  public static final String HELP_TabsDeadlockDetails = "Tabs_DeadlockDetails";
  public static final String HELP_TabsDeadlocksList = "Tabs_DeadlocksList";
  public static final String HELP_TabsIOActivity = "Tabs_IOActivity";
  public static final String HELP_TabsMPITimeline = "MPITimeline";
  public static final String HELP_TabsMPIChart = "MPIChart";
  public static final String HELP_TabsOMPParallelRegions =
      "Tabs_OMPParallelRegions"; // new tag?????
  public static final String HELP_TabsOpenMPTasks = "Tabs_OpenMPTasks";
  public static final String HELP_Settings = "Data_SetPref";
  public static final String HELP_Filter = "CustomFilters";
  public static final String HELP_ShowHideFunctions = "Data_ShowHide";
  public static final String HELP_CollectDialog = "CollectAnalyzer";
  public static final String HELP_CollectRunningDialog = "CollectRunning";
  public static final String HELP_CollectKernelDialog = "ProfileKernel";
  public static final String HELP_MachModel = "LoadMachModel";
  public static final String HELP_MachineModelBrowser = "BrowseMachModel";
  public static final String HELP_CustomMemTab = "AddCustomMemTab";
  public static final String HELP_CustomIndxTab = "AddCustomIndxTab";
  public static final String HELP_DataSize = "Tabs_DataSize";
  public static final String HELP_Duration = "Tabs_Duration";
  public static final String HELP_Compare = "Compare";
  public static final String HELP_Aggregate = "Aggregate";
  // public final static String	HELP_RemoteAnalyzer    = "RemoteAnalyzer";
  public static final String HELP_ConnectRemoteHost = "ConnectToRemoteHost";
  public static final String HELP_ConnectAuthentication = "Managing_Auth";
  public static final String HELP_ExportSettings = "ExportSettings"; //  FIXUP
  public static final String HELP_ResolveSourceFile = "ResolveSourceFile"; //  FIXUP
}
