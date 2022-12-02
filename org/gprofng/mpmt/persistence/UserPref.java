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

package org.gprofng.mpmt.persistence;

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.ExperimentPickLists;
import org.gprofng.mpmt.guitesting.GUITesting;
import org.gprofng.mpmt.metrics.MetricColors.MetricColor;
import org.gprofng.mpmt.picklist.StringPickList;
import org.gprofng.mpmt.remote.Authentication;
import org.gprofng.mpmt.settings.CallTreeSetting;
import org.gprofng.mpmt.settings.CompareModeSetting;
import org.gprofng.mpmt.settings.FormatSetting;
import org.gprofng.mpmt.settings.MetricNameSelection;
import org.gprofng.mpmt.settings.MetricType;
import org.gprofng.mpmt.settings.PathMapSetting;
import org.gprofng.mpmt.settings.SearchPathSetting;
import org.gprofng.mpmt.settings.SourceDisassemblySetting;
import org.gprofng.mpmt.settings.TableSettings;
import org.gprofng.mpmt.settings.TimelineSetting;
import org.gprofng.mpmt.settings.ViewModeSetting;
import org.gprofng.mpmt.settings.ViewsSetting.CustomObject;
import org.gprofng.mpmt.statecolors.ColorRule;
import org.gprofng.mpmt.toolbar.FindTextPanel;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;

public class UserPref {

  public enum What {
    USER,
    METRICS,
    SOURCEDISASSEMBLY,
    FORMATS,
    TIMELINE,
    SEARCHPATH,
    PATHMAP,
    VIEWS,
    CALLTREE,
    FUNCTIONCOLORS,
    LIBRARYVISIBILITY,
    MISC;

    public static What toWhat(String whatString) {
      if (whatString.equals("USER")) {
        return USER;
      } else if (whatString.equals("METRICS")) {
        return METRICS;
      } else if (whatString.equals("SOURCEDISASSEMBLY")) {
        return SOURCEDISASSEMBLY;
      } else if (whatString.equals("CALLTREE")) {
        return CALLTREE;
      } else if (whatString.equals("FORMATS")) {
        return FORMATS;
      } else if (whatString.equals("TIMELINE")) {
        return TIMELINE;
      } else if (whatString.equals("SEARCHPATH")) {
        return SEARCHPATH;
      } else if (whatString.equals("PATHMAP")) {
        return PATHMAP;
      } else if (whatString.equals("VIEWS")) {
        return VIEWS;
      } else if (whatString.equals("CALLTREE")) {
        return CALLTREE;
      } else if (whatString.equals("FUNCTIONCOLORS")) {
        return FUNCTIONCOLORS;
      } else if (whatString.equals("LIBRARYVISIBILITY")) {
        return LIBRARYVISIBILITY;
      } else if (whatString.equals("MISC")) {
        return MISC;
      }
      return null;
    }
  }

  private static UserPref instance = null;
  private static final String THA_INIT_FILE = "tha.xml";
  private static final String ANALYZER_INIT_FILE = "analyzer.xml";
  /* Update OldUserDirs with old user dirs it should search for settings */
  private static final String UserDir = ".gprofng/gui";
  private static final String[] OldUserDirs =
      new String[] {
        "../../.oracledevstudio/analyzer-paragon-dev",
        "../../.oracledevstudio/analyzer-12.6"
      }; // Newest first!
  public static final String configDirName = "configurations";
  public static final String configAsWhenClosedName = "asWhenClosed";
  public static final String configDefaultName = "default";
  public static final String configurationSuffix = ".config.xml";
  private static String userDirFromCommandLine = null; // from gp-display-gui script
  public static String binDirFromCommandLine = null;
  public static String dataDirFromCommandLine = null;
  
  private static long threeMonth = 90l * 24l * 3600l * 1000l; // 3 month

  private int version;

  // What's in this file
  private List<What> whatList = null;

  // System settings
  private Dimension frameSize;
  private Point frameLocation;
  private SplitPaneFixedRightSizeProp splitPane1 = new SplitPaneFixedRightSizeProp(375);
  private SplitPaneFixedRightSizeProp splitPane2 = new SplitPaneFixedRightSizeProp(160);
  private SplitPaneFixedRightSizeProp splitPane3 =
      new SplitPaneFixedRightSizeProp(getDefaultSplitPane3RightSize());
  private SplitPaneFixedRightSizeProp navigationFilterSplitPane =
      new SplitPaneFixedRightSizeProp(160);
  private Integer navigationPanelDividerPosition;
  private ExperimentPickLists experimentsPicklists;
  private Map<String, ConnectionProperties> connectionPropertiesMap =
      new LinkedHashMap<String, ConnectionProperties>();
  private StringPickList hostNamePicklist = new StringPickList();
  private String lastClosedExpConfPath = null;
  private Boolean wrapMetricNamesInTables = null;

  //
  private String lastExportImportConfPath = null;

  // Experiment settings
  private StringPickList findPickList = null;
  private List<String> libraryVisibilitySettings = null;
  private StringPickList libraryVisibilityIncludeFilterPickList = null;
  private String currentLibraryVisibilityIncludeFilter = null;
  private StringPickList libraryVisibilityExcludeFilterPickList = null;
  private String currentLibraryVisibilityExcludeFilter = null;
  private Boolean libraryVisibilityJava = null;
  private Integer callStackThreshold;
  private Integer formatsViewMode = null;
  private Integer formatsStyle = null;
  private Boolean formatsAppendSoName = null;
  private Integer compareMode = null;
  private int[] sourceDisassemblySettings = null;
  private List<ViewPanelOrder> viewPanelOrder;
  private String machineModel;
  private List<CustomObject> customIndexObjects = null;
  private List<CustomObject> customMemoryObjects = null;
  private List<ColorRule> activeColorRules = null;
  private String timelineGroupDataByButtonName = null;
  private String timelineTLDataCmd = null;
  private Integer timelineStackAlign = null;
  private Integer timelineStackDepth = null;
  private Integer timelineStackFramePixels = null;
  private Boolean timelineShowEventStates = null;
  private Boolean timelineShowEventDensity = null;
  private List<String> searchPath = null;
  private String[][] pathmap = null;
  private List<MetricColor> customMetricColors = null;
  private Boolean showCompareSourceWarning = null;

  private List<MetricNameSelection> metricSelectionList;
  private Boolean metricReversedSort = null;
  private MetricType[] metricSortByMTypeList = null;
  private List<MetricType>[] metricOrderLists = null;

  public static UserPref getInstance() {
    if (instance == null) {
      instance = new UserPref();
    }
    return instance;
  }

  public static String getAnalyzerDirPath(String userDir) {
    String ret;
    String userHome = getHomeDirectory();
    ret = userHome + "/" + userDir;
    return ret;
  }

  public static String getAnalyzerInitFilePath(String userDir) {
    return getAnalyzerDirPath(userDir) + "/" + getInitFileName();
  }

  public static String getAnalyzerDirPath() {
    String ret = userDirFromCommandLine; // from analyzer/tha script
    if (ret == null || ret.length() == 0) {
      String userHome = getHomeDirectory();
      ret = userHome + "/" + UserDir;
    }
    return ret;
  }

  public static String getAnalyzerInitFilePath() {
    return getAnalyzerDirPath() + "/" + getInitFileName();
  }

  public static String[] getOldUserDirs() {
    String[] ods = OldUserDirs.clone();
    return ods;
  }

  public static String getDefaultAnalyzerDirPath() {
    return getAnalyzerDirPath(UserDir);
  }

  public static String getDefaultAnalyzerInitFilePath() {
    return getAnalyzerInitFilePath(getDefaultAnalyzerDirPath());
  }

  public static String getHomeDirectory() {
    String userHome = System.getProperty("user.home");
    if (userHome == null
        || userHome.equals("?")) { // on some Linux the systems the call above returns "?".
      userHome = System.getenv("HOME");
    }
    return userHome;
  }

  public static String getInitFileName() {
    return ANALYZER_INIT_FILE;
  }

  public static String getConfigurationDirPath(String experimentName) {
    return experimentName + "/" + configDirName;
  }

  public static String getHomeConfigurationDirPath() {
    return getAnalyzerDirPath() + "/" + configDirName;
  }

  public static String getAsWhenClosedConfigName() {
    return configAsWhenClosedName + "." + getUserName() + configurationSuffix;
  }

  public static String getDefaultConfigurationName() {
    return configDefaultName + configurationSuffix;
  }

  public static String getAsWhenClosedConfigDirPath() {
    return getHomeConfigurationDirPath() + "/" + configAsWhenClosedName;
  }

  public static String getAsWhenClosedConfigPath(String experimentPath) {
    if (!experimentPath.startsWith("/")) {
      experimentPath = Analyzer.getInstance().getWorkingDirectory() + "/" + experimentPath;
    }
    String host = "";
    if (Analyzer.getInstance().isRemote()) {
      host = Analyzer.getInstance().getRemoteHost();
    }
    String mangledFileName = getMangledFileName(experimentPath, host, true);
    String asWhenClosedConfigPath =
        getAsWhenClosedConfigDirPath() + "/" + mangledFileName + configurationSuffix;

    File asWhenClosedConfigFile = new File(asWhenClosedConfigPath);
    if (!asWhenClosedConfigFile.exists()) {
      // If asWhenClosedConfigFile doesn't exist and old style do exists, move old to new...
      String mangledFileNameOld = getMangledFileName(experimentPath, host, false);
      String asWhenClosedConfigPathOld =
          getAsWhenClosedConfigDirPath() + "/" + mangledFileNameOld + configurationSuffix;
      File asWhenClosedConfigFileOld = new File(asWhenClosedConfigPathOld);
      if (asWhenClosedConfigFileOld.exists()) {
        asWhenClosedConfigFileOld.renameTo(asWhenClosedConfigFile);
      }
    }

    return asWhenClosedConfigPath;
  }

  /**
   * @param experimentPath assuming full path
   * @param host
   * @return
   */
  public static String getMangledFileName(
      String experimentPath, String host, boolean useTimestamp) {
    String experimentName = basename(experimentPath);
    String mangledFileName = null;
    if (useTimestamp) {
      File logFile = new File(experimentPath + "/" + "log.xml");
      if (logFile.exists()) {
        String timestamp = null;
        try {
          // use creationTime, if available
          BasicFileAttributes attr =
              Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
          timestamp = "" + attr.creationTime().toMillis();
        } catch (IOException ioe) {
        }
        if (timestamp == null) {
          // else use lastModified
          timestamp = "" + logFile.lastModified();
        }
        mangledFileName = experimentName + "_" + timestamp;
      }
    }
    if (mangledFileName == null) {
      // Old style...
      if (host == null || host.isEmpty() || host.equals("localhost")) {
        host = "";
      } else if (!host.endsWith(".")) {
        host = host + ".";
      }
      String hostPlusExperimentPath = host + experimentPath;
      int hash = hostPlusExperimentPath.hashCode();
      if (hash < 0) {
        hash = -hash;
      }
      mangledFileName = experimentName + "_" + hash;
    }
    return mangledFileName;
  }

  public static String basename(String path) {
    String name = path;
    int i = path.lastIndexOf("/");
    if (i >= 0 && i < (path.length() - 1)) {
      name = path.substring(i + 1);
    }
    return name;
  }

  public static String getUserName() {
    return System.getProperty("user.name");
  }

  /**
   * ************************************************************************************************
   */
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public List<What> getWhatList() {
    return whatList;
  }

  public void setWhatList(List<What> whatList) {
    this.whatList = whatList;
  }

  public ExperimentPickLists getExperimentsPicklists() {
    if (experimentsPicklists == null) {
      experimentsPicklists = new ExperimentPickLists();
    }
    return experimentsPicklists;
  }

  public void setExperimentsPicklists(ExperimentPickLists experimentsPicklists) {
    this.experimentsPicklists = experimentsPicklists;
  }

  public Map<String, ConnectionProperties> getConnectionPropertiesMap() {
    return connectionPropertiesMap;
  }

  public void setConnectionPropertiesMap(
      Map<String, ConnectionProperties> connectionPropertiesMap) {
    this.connectionPropertiesMap = connectionPropertiesMap;
  }

  public static class ConnectionProperties {

    private String path;
    private String userName;
    private List<Authentication> authentications;

    public ConnectionProperties(
        String path, String userName, List<Authentication> authentications) {
      this.path = path;
      this.userName = userName;
      this.authentications = authentications;
    }

    public String getPath() {
      return path;
    }

    public String getUserName() {
      return userName;
    }

    public List<Authentication> getAuthentications() {
      return authentications;
    }
  }

  public static List<Authentication> getDefaultAuthentications() {
    return Authentication.getDefaultAuthentications();
  }

  public StringPickList getHostNamePicklist() {
    return hostNamePicklist;
  }

  public void setHostNamePicklist(StringPickList hostNamePicklist) {
    this.hostNamePicklist = hostNamePicklist;
  }

  public Dimension getFrameSize() {
    if (frameSize != null) {
      return frameSize;
    } else {
      return getDefaultFrameSize();
    }
  }

  public Dimension getDefaultFrameSize() {
    int screenWidth = AnVariable.SCREEN_SIZE.width;
    int screenHeight = AnVariable.SCREEN_SIZE.height;
    int width;
    int height;

    if (GUITesting.getInstance().isRunningUnderGUITesting()) {
      // Use fixed size frame if running under GUI testing
      return new Dimension(1400, 950);
    }

    // Guess a max width so it doesn't come up too wide in a multi monitor environment
    if (screenHeight <= 480 && screenWidth > 640) {
      screenWidth = 640;
    } else if (screenHeight <= 600 && screenWidth >= 800) {
      screenWidth = 800;
    } else if (screenHeight <= 768 && screenWidth >= 1024) {
      screenWidth = 1024;
    } else if (screenHeight <= 1200 && screenWidth >= 1920) {
      screenWidth = 1920;
    } else if (screenWidth >= 1024) { // ???
      screenWidth = 1024; // ???
    }

    if (screenWidth <= 400) {
      width = screenWidth - 10;
    } else if (screenWidth <= 800) {
      width = screenWidth - 25;
    } else if (screenWidth <= 1280) {
      width = screenWidth - 50;
    } else {
      width = screenWidth - 200;
    }

    if (screenHeight <= 300) {
      height = screenHeight - 10;
    } else if (screenHeight <= 600) {
      height = screenHeight - 50;
    } else if (screenHeight <= 1024) {
      height = screenHeight - 100;
    } else {
      height = screenHeight - 200;
    }

    return new Dimension(width, height);
  }

  public void setFrameSize(Dimension frameSize) {
    this.frameSize = frameSize;
  }

  public Point getFrameLocation() {
    if (frameLocation != null) {
      return frameLocation;
    } else {
      return getDefaultFrameLocation();
    }
  }

  public Point getDefaultFrameLocation() {
    Dimension size = getDefaultFrameSize();
    int x = (AnVariable.SCREEN_SIZE.width - size.width) / 2;
    int y = (AnVariable.SCREEN_SIZE.height - size.height) / 2;
    if (x <= 0) {
      x = 0;
    }
    if (y <= 0) {
      y = 0;
    }
    return new Point(x, y);
  }

  public void setFrameLocation(Point frameLocation) {
    this.frameLocation = frameLocation;
  }

  public SplitPaneFixedRightSizeProp getSplitPane1() {
    return splitPane1;
  }

  public SplitPaneFixedRightSizeProp getSplitPane2() {
    return splitPane2;
  }

  public SplitPaneFixedRightSizeProp getSplitPane3() {
    return splitPane3;
  }

  public SplitPaneFixedRightSizeProp getNavigationFilterSplitPane() {
    return navigationFilterSplitPane;
  }

  public static class SplitPaneFixedRightSizeProp {

    private Integer defaultSize;
    private Integer size;

    public SplitPaneFixedRightSizeProp(Integer defaultSize) {
      this.defaultSize = defaultSize;
    }

    /**
     * @return the size
     */
    public Integer getSize() {
      if (size == null) {
        size = getDefaultSize();
      }
      return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Integer size) {
      this.size = size;
    }

    /**
     * @return the defaultSize
     */
    public Integer getDefaultSize() {
      return defaultSize;
    }
  }

  public Integer getNavigationPanelDividerPosition() {
    if (navigationPanelDividerPosition == null) {
      navigationPanelDividerPosition = 145; // default value
    }
    return navigationPanelDividerPosition;
  }

  public void setNavigationPanelDividerPosition(Integer navigationPanelDividerPosition) {
    this.navigationPanelDividerPosition = navigationPanelDividerPosition;
  }

  protected Integer getDefaultSplitPane1RightSize() {
    return 375;
  }

  protected Integer getDefaultSplitPane3RightSize() {
    Dimension size = getDefaultFrameSize();
    return (int) (size.height * 0.4);
  }

  public String getLastClosedExpConfPath() {
    return lastClosedExpConfPath;
  }

  public void setLastClosedExpConfPath(String lastClosedExpConfPath) {
    this.lastClosedExpConfPath = lastClosedExpConfPath;
  }

  /**
   * ************************************************************************************************
   */
  public String getLastExportImportConfPath() {
    return lastExportImportConfPath;
  }

  public void setLastExportImportConfPath(String lastExportImportConfPath) {
    this.lastExportImportConfPath = lastExportImportConfPath;
  }

  /** Find text */
  public StringPickList getFindPickList() {
    if (findPickList == null) {
      findPickList = new StringPickList(FindTextPanel.MAX_FIND_ITEMS);
    }
    return findPickList;
  }

  public void setFindPickList(StringPickList findPickList) {
    this.findPickList = findPickList;
  }

  /** Library Visibility */
  public StringPickList getLibraryVisibilityIncludePickList() {
    if (libraryVisibilityIncludeFilterPickList == null) {
      libraryVisibilityIncludeFilterPickList = new StringPickList(FindTextPanel.MAX_FIND_ITEMS);
    }
    return libraryVisibilityIncludeFilterPickList;
  }

  public void setLibraryVisibilityIncludePickList(
      StringPickList libraryVisibilityIncludeFilterPickList) {
    this.libraryVisibilityIncludeFilterPickList = libraryVisibilityIncludeFilterPickList;
  }

  public String getLibraryVisibilityIncludeFilter() {
    return currentLibraryVisibilityIncludeFilter;
  }

  public void setLibraryVisibilityIncludeFilter(String currentLibraryVisibilityIncludeFilter) {
    this.currentLibraryVisibilityIncludeFilter = currentLibraryVisibilityIncludeFilter;
  }

  public StringPickList getLibraryVisibilityExcludePickList() {
    if (libraryVisibilityExcludeFilterPickList == null) {
      libraryVisibilityExcludeFilterPickList = new StringPickList(FindTextPanel.MAX_FIND_ITEMS);
    }
    return libraryVisibilityExcludeFilterPickList;
  }

  public void setLibraryVisibilityExcludePickList(
      StringPickList libraryVisibilityExcludeFilterPickList) {
    this.libraryVisibilityExcludeFilterPickList = libraryVisibilityExcludeFilterPickList;
  }

  public String getCurrenLibraryVisibilityExcludeFilter() {
    return currentLibraryVisibilityExcludeFilter;
  }

  public void setCurrentLibraryVisibilityExcludeFilter(
      String currentLibraryVisibilityExcludeFilter) {
    this.currentLibraryVisibilityExcludeFilter = currentLibraryVisibilityExcludeFilter;
  }

  public boolean getLibraryVisibilityJava() {
    if (libraryVisibilityJava == null) {
      libraryVisibilityJava = getLibraryVisibilityJavaDefault();
    }
    return libraryVisibilityJava;
  }

  public void setLibraryVisibilityJava(boolean libraryVisibilityJava) {
    this.libraryVisibilityJava = libraryVisibilityJava;
  }

  public boolean getLibraryVisibilityJavaDefault() {
    return false;
  }

  /** Call Tree threshold */
  public int getCallStackThreshold() {
    if (callStackThreshold == null) {
      callStackThreshold = getCallStackThresholdDefault();
    }
    return callStackThreshold;
  }

  public void setCallStackThreshold(int callStackThreshold) {
    this.callStackThreshold = callStackThreshold;
  }

  public int getCallStackThresholdDefault() {
    return CallTreeSetting.getDefaultThreshold();
  }

  /** Formats View Mode */
  public int getFormatsViewMode() {
    if (formatsViewMode == null) {
      formatsViewMode = getFormatsViewModeDefault();
    }
    return formatsViewMode;
  }

  public void setFormatsViewMode(int formatsViewMode) {
    this.formatsViewMode = formatsViewMode;
  }

  public int getFormatsViewModeDefault() {
    return ViewModeSetting.getDefaultViewMode().value();
  }

  /** Formats Style */
  public int getFormatsStyle() {
    if (formatsStyle == null) {
      formatsStyle = getFormatsStyleDefault();
    }
    return formatsStyle;
  }

  public void setFormatsStyle(int formatsStyle) {
    this.formatsStyle = formatsStyle;
  }

  public int getFormatsStyleDefault() {
    return FormatSetting.getDefaultStyle().value();
  }

  /** Formats AppendSoName */
  public boolean getFormatsAppendSoName() {
    if (formatsAppendSoName == null) {
      formatsAppendSoName = getFormatsAppendSoNameDefault();
    }
    return formatsAppendSoName;
  }

  public void setFormatsAppendSoName(boolean formatsAppendSoName) {
    this.formatsAppendSoName = formatsAppendSoName;
  }

  public boolean getFormatsAppendSoNameDefault() {
    return FormatSetting.getDefaultAppendSoName();
  }

  /** Formats Compare Mode */
  public int getFormatsCompareMode() {
    if (compareMode == null) {
      compareMode = getFormatsCompareModeDefault();
    }
    return compareMode;
  }

  public void setFormatsCompareMode(int compareMode) {
    this.compareMode = compareMode;
  }

  public int getFormatsCompareModeDefault() {
    return CompareModeSetting.getDefaultCompareMode().value();
  }

  /** source Disassembly Settings */
  public int[] getSourceDisassemblySettings() {
    if (sourceDisassemblySettings == null) {
      sourceDisassemblySettings = getSourceDisassemblySettingsDefault();
    }
    return sourceDisassemblySettings;
  }

  public void setSourceDisassemblySettings(int[] sourceDisassembySettings) {
    this.sourceDisassemblySettings = sourceDisassembySettings;
  }

  public int[] getSourceDisassemblySettingsDefault() {
    return SourceDisassemblySetting.getDefaultSourceDisassemblySetting();
  }

  /** Views order */
  public List<ViewPanelOrder> getViewPanelOrder() {
    return viewPanelOrder;
  }

  public void setViewPanelOrder(List<ViewPanelOrder> viewPanelOrder) {
    this.viewPanelOrder = viewPanelOrder;
  }

  public static class ViewPanelOrder {

    private String name;
    private boolean shown;

    public ViewPanelOrder(String name, boolean shown) {
      this.name = name;
      this.shown = shown;
    }

    /**
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the shown
     */
    public boolean isShown() {
      return shown;
    }
  }

  /** Machine model */
  public String getMachineModel() {
    return machineModel;
  }

  public void setMachineModel(String machineModel) {
    this.machineModel = machineModel;
  }

  /** Index and Memory Objects */
  public List<CustomObject> getCustomIndexObjects() {
    return customIndexObjects;
  }

  public void setCustomIndexObjects(List<CustomObject> indexObjects) {
    this.customIndexObjects = indexObjects;
  }

  public List<CustomObject> getCustomMemoryObjects() {
    return customMemoryObjects;
  }

  public void setCustomMemoryObjects(List<CustomObject> memoryObjects) {
    this.customMemoryObjects = memoryObjects;
  }

  public static void setUserDir(String aUserDir) {
    userDirFromCommandLine = aUserDir;
  }

  /** Function Colors */
  public List<ColorRule> getActiveColorRules() {
    if (activeColorRules != null) {
      return activeColorRules;
    } else {
      return getActiveColorRulesDefault();
    }
  }

  public List<ColorRule> getActiveColorRulesDefault() {
    return new ArrayList<ColorRule>();
  }

  public void setActiveColorRules(List<ColorRule> activeColorRules) {
    this.activeColorRules = activeColorRules;
  }

  /** Metrics */
  public List<MetricNameSelection> getMetricSelectionList() {
    return metricSelectionList;
  }

  public void setMetricSelectionList(List<MetricNameSelection> metricOrder) {
    this.metricSelectionList = metricOrder;
  }

  public boolean getMetricReversedSort() {
    if (metricReversedSort != null) {
      return metricReversedSort;
    } else {
      return getMetricReversedSortDefault();
    }
  }

  public boolean getMetricReversedSortDefault() {
    return false;
  }

  public void setMetricReversedSort(Boolean metricReversedSort) {
    this.metricReversedSort = metricReversedSort;
  }

  public MetricType[] getMetricSortByMTypeList() {
    return metricSortByMTypeList;
  }

  public void setMetricSortByMTypeList(MetricType[] metricSortMType) {
    this.metricSortByMTypeList = metricSortMType;
  }

  public List<MetricType>[] getMetricOrderLists() {
    return metricOrderLists;
  }

  public void setMetricOrderLists(List<MetricType>[] metricOrderLists) {
    this.metricOrderLists = metricOrderLists;
  }

  /** Timeline */
  public String getTimelineGroupDataByButtonName() {
    if (timelineGroupDataByButtonName != null) {
      return timelineGroupDataByButtonName;
    } else {
      return getTimelineGroupDataByButtonNameDefault();
    }
  }

  public String getTimelineGroupDataByButtonNameDefault() {
    return TimelineSetting.getDefaultGroupDataByButtonName();
  }

  public void setTimelineGroupDataByButtonName(String timelineGroupDataByButtonName) {
    this.timelineGroupDataByButtonName = timelineGroupDataByButtonName;
  }

  public String getTimelineTLDataCmd() {
    if (timelineTLDataCmd != null) {
      return timelineTLDataCmd;
    } else {
      return getTimelineTLDataCmdDefault();
    }
  }

  public String getTimelineTLDataCmdDefault() {
    return TimelineSetting.getDefaultTLDataCmd();
  }

  public void setTimelineTLDataCmd(String timelineTLDataCmd) {
    this.timelineTLDataCmd = timelineTLDataCmd;
  }

  public int getTimelineStackAlign() {
    if (timelineStackAlign != null) {
      return timelineStackAlign;
    } else {
      return getDefaultTimelineStackAlign();
    }
  }

  public int getDefaultTimelineStackAlign() {
    return TimelineSetting.getDefaultStackAlign();
  }

  public void setTimelineStackAlign(Integer timelineStackAlign) {
    this.timelineStackAlign = timelineStackAlign;
  }

  public int getTimelineStackDepth() {
    if (timelineStackDepth != null) {
      return timelineStackDepth;
    } else {
      return getTimelineStackDepthDefault();
    }
  }

  public int getTimelineStackDepthDefault() {
    return TimelineSetting.getDefaultStackDepth();
  }

  public void setTimelineStackDepth(Integer timelineStackDepth) {
    this.timelineStackDepth = timelineStackDepth;
  }

  public int getTimelineStackFramePixels() {
    if (timelineStackFramePixels != null) {
      return timelineStackFramePixels;
    } else {
      return getTimelineStackFramePixelsDefault();
    }
  }

  public int getTimelineStackFramePixelsDefault() {
    return TimelineSetting.getDefaultStackFramePixels();
  }

  public void setTimelineStackFramePixels(Integer timelineStackFramePixels) {
    this.timelineStackFramePixels = timelineStackFramePixels;
  }

  public boolean getTimelineShowEventStates() {
    if (timelineShowEventStates != null) {
      return timelineShowEventStates;
    } else {
      return getTimelineShowEventStatesDefault();
    }
  }

  public boolean getTimelineShowEventStatesDefault() {
    return TimelineSetting.getTimelineShowEventStatesDefault();
  }

  public void setTimelineShowEventStates(Boolean timelineShowEventStates) {
    this.timelineShowEventStates = timelineShowEventStates;
  }

  public boolean getTimelineShowEventDensity() {
    if (timelineShowEventDensity != null) {
      return timelineShowEventDensity;
    } else {
      return getTimelineShowEventDensityDefault();
    }
  }

  public boolean getTimelineShowEventDensityDefault() {
    return TimelineSetting.getTimelineShowEventDensityDefault();
  }

  public void setTimelineShowEventDensity(Boolean timelineShowEventDensity) {
    this.timelineShowEventDensity = timelineShowEventDensity;
  }

  public List<String> getLibraryVisibilitySettings() {
    return libraryVisibilitySettings;
  }

  public void setLibraryVisibilitySettings(List<String> loadObjectSettings) {
    this.libraryVisibilitySettings = loadObjectSettings;
  }

  public List<String> getSearchPath() {
    if (searchPath == null) {
      return getSearchPathDefault();
    } else {
      return searchPath;
    }
  }

  public List<String> getSearchPathDefault() {
    return SearchPathSetting.getDefaultSearchPath();
  }

  public void setSearchPath(List<String> searchPath) {
    this.searchPath = searchPath;
  }

  public String[][] getPathmap() {
    if (pathmap == null) {
      return getPathmapDefault();
    } else {
      return pathmap;
    }
  }

  public String[][] getPathmapDefault() {
    return PathMapSetting.getDefaultPathmap();
  }

  public void setPathmap(String[][] pathmap) {
    this.pathmap = pathmap;
  }

  public List<MetricColor> getCustomMetricColors() {
    return customMetricColors;
  }

  public void setCustomMetricColors(List<MetricColor> customMetricColors) {
    this.customMetricColors = customMetricColors;
  }

  public boolean showCompareSourceWarning() {
    if (showCompareSourceWarning == null) {
      showCompareSourceWarning = showCompareSourceWarningDefault();
    }
    return showCompareSourceWarning;
  }

  public boolean showCompareSourceWarningDefault() {
    return true;
  }

  public void setShowCompareSourceWarning(boolean showCompareSourceWarning) {
    this.showCompareSourceWarning = showCompareSourceWarning;
  }

  public Boolean wrapMetricNamesInTables() {
    if (wrapMetricNamesInTables == null) {
      wrapMetricNamesInTables = wrapMetricNamesInTablesDefault();
    }
    return wrapMetricNamesInTables;
  }

  public void setWrapMetricNamesInTables(Boolean wrapMetricNamesInTables) {
    this.wrapMetricNamesInTables = wrapMetricNamesInTables;
  }

  public Boolean wrapMetricNamesInTablesDefault() {
    return TableSettings.wrapMetricNamesDefault();
  }

  /**
   * ************************************************************************************************
   */
  public void save(String configFilePath, List<UserPref.What> what) {
    AnWindow anWindow = AnWindow.getInstance();
    JFrame anFrame = anWindow.getFrame();

    // Copy states back in to UserPref
    if (what.contains(UserPref.What.USER)) {
      setFrameSize(anFrame.getSize());
      setFrameLocation(anFrame.getLocation());
      setNavigationPanelDividerPosition(
          anWindow.getNavigationPanelSplitPane().getDividerLocationWhenShowing());
      getSplitPane1().setSize(anWindow.getMainViewPanel().getSplitPane1().getSizeInPixels());
      getSplitPane2().setSize(anWindow.getMainViewPanel().getSplitPane2().getSizeInPixels());
      getSplitPane3().setSize(anWindow.getMainViewPanel().getSplitPane3().getSizeInPixels());
      getNavigationFilterSplitPane()
          .setSize(anWindow.getNavigationPanel().getNavigationSplitPaneSizeInPixels());
    }
    if (!what.contains(UserPref.What.USER) && what.size() > 0
        || what.contains(UserPref.What.USER) && what.size() > 1) {
      if (!anWindow.getViewsPanel().onlyStaticViews()) {
        setViewPanelOrder(anWindow.getViewsPanel().getViewPanelOrderPref());
      }
      setCallStackThreshold(anWindow.getSettings().getCallTreeSetting().getThreshold());
      setFormatsViewMode(anWindow.getSettings().getViewModeSetting().get().value());
      setFormatsStyle(anWindow.getSettings().getFormatSetting().getStyle().value());
      setFormatsAppendSoName(anWindow.getSettings().getFormatSetting().getAppendSoName());
      if (anWindow.getSettings().getCompareModeSetting().comparingExperiments()) {
        setFormatsCompareMode(anWindow.getSettings().getCompareModeSetting().get().value());
      }
      setSourceDisassemblySettings(anWindow.getSettings().getSourceDisassemblySetting().get());
      setMachineModel(anWindow.getSettings().getViewsSetting().getMachineModel());
      setCustomIndexObjects(anWindow.getSettings().getViewsSetting().getCustomIndexObjects());
      setCustomMemoryObjects(anWindow.getSettings().getViewsSetting().getCustomMemoryObjects());
      setActiveColorRules(anWindow.getColorChooser().getColorMap().getActiveRules());
      setMetricSelectionList(anWindow.getSettings().getMetricsSetting().getMetricOrder());
      setMetricReversedSort(
          anWindow.getSettings().getMetricsSetting().getMetricStates().isReverseDirectionSorting());
      setMetricSortByMTypeList(
          anWindow.getSettings().getMetricsSetting().getMetricStates().getMetricSortByMType());
      setMetricOrderLists(
          anWindow.getSettings().getMetricsSetting().getMetricStates().getMetricOrderLists());
      setTimelineStackDepth(anWindow.getSettings().getTimelineSetting().getTLStackDepth());
      setTimelineStackFramePixels(
          anWindow.getSettings().getTimelineSetting().getTLStackFramePixels());
      setTimelineGroupDataByButtonName(
          anWindow.getSettings().getTimelineSetting().getGroupDataByButtonName());
      setTimelineTLDataCmd(anWindow.getSettings().getTimelineSetting().getTLDataCmd());
      setTimelineStackAlign(anWindow.getSettings().getTimelineSetting().getStackAlign());
      setTimelineShowEventStates(anWindow.getSettings().getTimelineSetting().getShowEventStates());
      setTimelineShowEventDensity(
          anWindow.getSettings().getTimelineSetting().getShowEventDensity());

      setLibraryVisibilitySettings(
          AnWindow.getInstance().getSettings().getLibraryVisibilitySetting().getStates());
      setLibraryVisibilityJava(anWindow.getLibraryVisibilityDialog().isJava());
      setSearchPath(anWindow.getSettings().getSearchPathSetting().get());
      setPathmap(anWindow.getSettings().getPathMapSetting().get());
      setWrapMetricNamesInTables(anWindow.getSettings().getTableSettings().wrapMetricNames());
    }

    // Save states in config file
    new UserPrefPersistence().saveSettings(configFilePath, this, what);
  }

  public void restore(String configFilePath) {
    new UserPrefPersistence().restoreSettings(configFilePath, this);
  }

  public void resetExperimentPreferences() {
    findPickList.removeAllElements();
    callStackThreshold = null;
    formatsViewMode = null;
    formatsAppendSoName = null;
    formatsStyle = null;
    sourceDisassemblySettings = null;
    machineModel = null;
    compareMode = null;
    customIndexObjects = null;
    customMemoryObjects = null;
    activeColorRules = null;
    viewPanelOrder = null;
    metricSelectionList = null;
    metricReversedSort = null;
    metricSortByMTypeList = null;
    metricOrderLists = null;
    timelineStackDepth = null;
    timelineStackFramePixels = null;
    timelineGroupDataByButtonName = null;
    timelineTLDataCmd = null;
    timelineStackAlign = null;
    timelineShowEventStates = null;
    timelineShowEventDensity = null;
    libraryVisibilitySettings = null;
    searchPath = null;
    pathmap = null;
    libraryVisibilityIncludeFilterPickList = null;
    currentLibraryVisibilityIncludeFilter = null;
    libraryVisibilityExcludeFilterPickList = null;
    currentLibraryVisibilityExcludeFilter = null;
    libraryVisibilityJava = null;
    wrapMetricNamesInTables = null;
  }

  /** Remove config files that are older than threeMonth (3 month)... */
  public void cleanupAsWhenClosedFolder() {
    File asWhenClosedFile = new File(getAsWhenClosedConfigDirPath());
    if (asWhenClosedFile.exists()) {
      File[] files = asWhenClosedFile.listFiles();
      long oldTimeStamp = new Date().getTime() - threeMonth;
      for (File file : files) {
        if (!file.isDirectory() && file.getName().endsWith(configurationSuffix)) {
          long lastModified = file.lastModified();
          if (lastModified < oldTimeStamp) {
            file.delete();
            AnLog.log("Old config file removed: " + file.getAbsolutePath());
          }
        }
      }
    }
  }

  /**
   * FIXUP: Should move this code to AnUtility but AnUtility doesn't run this early due to class
   * loader issue!
   *
   * @param fromDirectoryFile
   * @param toDirectoryFile
   * @throws IOException
   */
  public static void copyDirectory(File fromDirectoryFile, File toDirectoryFile)
      throws IOException {
    if (fromDirectoryFile.exists() && fromDirectoryFile.isDirectory()) {
      if (!toDirectoryFile.exists()) {
        toDirectoryFile.mkdirs();
      }

      String[] children = fromDirectoryFile.list();
      for (int i = 0; i < children.length; i++) {
        File fromFile = new File(fromDirectoryFile, children[i]);
        if (fromFile.getName().equals(UserPref.configAsWhenClosedName)) {
          // Don't copy these files just yet....
          continue;
        }
        File toFile = new File(toDirectoryFile, children[i]);
        if (fromFile.isDirectory()) {
          copyDirectory(fromFile, toFile);
        } else {
          copyFile(fromFile, toFile);
        }
      }
    } else {
      // FIXUP: do something reasonable
    }
  }

  public static void copyFile(File fromFile, File toFile) throws IOException {
    if (fromFile.exists() && !fromFile.isDirectory()) {
      InputStream inputStream = new FileInputStream(fromFile);
      OutputStream outputStream = new FileOutputStream(toFile);

      byte[] buffer = new byte[1024];
      int nbytes;
      while ((nbytes = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, nbytes);
      }
      inputStream.close();
      outputStream.close();
      toFile.setLastModified(fromFile.lastModified());
    } else {
      // FIXUP: do something reasonable
    }
  }
}
