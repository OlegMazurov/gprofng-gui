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

package org.gprofng.mpmt.persistence;

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.ExperimentPickList;
import org.gprofng.mpmt.ExperimentPickListElement;
import org.gprofng.mpmt.metrics.MetricColors.MetricColor;
import org.gprofng.mpmt.persistence.UserPref.ConnectionProperties;
import org.gprofng.mpmt.persistence.UserPref.ViewPanelOrder;
import org.gprofng.mpmt.picklist.StringPickList;
import org.gprofng.mpmt.picklist.StringPickListElement;
import org.gprofng.mpmt.remote.Authentication;
import org.gprofng.mpmt.settings.MetricNameSelection;
import org.gprofng.mpmt.settings.MetricType;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.settings.PathMapSetting;
import org.gprofng.mpmt.settings.SearchPathSetting;
import org.gprofng.mpmt.settings.SourceDisassemblySetting;
import org.gprofng.mpmt.settings.ViewsSetting.CustomObject;
import org.gprofng.mpmt.statecolors.ColorRule;
import org.gprofng.mpmt.toolbar.FindTextPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.swing.JOptionPane;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class UserPrefPersistence {

  /**
   * V16: HOSTNAME_CONNECT_COMMAND_ATTR
   * V15: HOSTNAME_AUTH_ATTR V14: LIBRARY_VISIBILITY_JAVA V13: FULL_METRIC_NAME_IN_TABLES_ELEM V12:
   * Metric sorting, direction, and order V11: changed order and added sections V10: call stack
   * threshold V9 : working directory V8 : find history V7 : splitpane(123) size V6 : navigation
   * panel splitpane position V5 : recent experiments per host V4 : added userName attr to host V3 :
   * added hostNamePath maps V2 : added most recent experiments V1 : initial version
   */
  private static final int version = 16;

  private static final String spaces = "                                  ";
  // Elements
  private static final String VERSION_ELEM = "version";
  private static final String ANALYZER_ELEM = "analyzer";
  private static final String USER_ELEM = "user";
  private static final String FRAME_ELEM = "frame";
  private static final String FRAME_WIDTH_ATTR = "width";
  private static final String FRAME_HEIGHT_ATTR = "height";
  private static final String FRAME_X_ATTR = "x";
  private static final String FRAME_Y_ATTR = "y";
  private static final String SPLITPANE1_ELEM = "splitpane1";
  private static final String SPLITPANE2_ELEM = "splitpane2";
  private static final String SPLITPANE3_ELEM = "splitpane3";
  private static final String NAV_SPLITPANE_ELEM = "navsplitpane";
  private static final String NAV_FILTER_SPLITPANE_ELEM = "navfiltersplitpane";
  private static final String SPLITPANE_POS_ATTR = "pos";
  private static final String SPLITPANE_LAST_POS_ATTR = "lastpos";
  private static final String SPLITPANE_RIGHT_SIZE_ATTR = "rightsize";
  private static final String VIEWS_ELEM = "views";
  private static final String VIEW_ORDER_LIST_ELEM = "vieworderlist";
  private static final String VIEW_ORDER_ELEM = "viewelem";
  private static final String VIEW_ORDER_NAME_ATTR = "name";
  private static final String VIEW_ORDER_SHOWN_ATTR = "shown";
  private static final String EXPERIMENT_LIST_ELEM = "recentExperimentsList";
  private static final String EXPERIMENT_PATH_ELEM = "experimentPath";
  private static final String EXPERIMENT_ELEM = "experiment";
  private static final String HOSTNAME_LIST_ELEM = "hostNameList";
  private static final String HOSTNAME_ELEM = "host";
  private static final String HOSTNAME_NAME_ATTR = "name";
  private static final String HOSTNAME_PATH_ATTR = "path";
  private static final String HOSTNAME_CONNECT_COMMAND_ATTR = "connectCommand";
  private static final String HOSTNAME_AUTH_ATTR = "auth";
  private static final String HOSTNAME_USERNAME_ATTR = "username";
  private static final String HOST_NAME_ATTR = "host";
  private static final String PATH_ATTR = "path";
  private static final String WD_ATTR = "wd";
  private static final String LAST_CLOSED_EXP_CONF_PATH_ELEM = "lceconfpath";
  private static final String LAST_EXPORT_IMPORT_EXP_CONF_PATH_ELEM = "leiconfpath";
  private static final String SHOW_COMPARE_WARNING_ELEM = "comparewarning";
  private static final String CONF_ATTR = "conf";
  private static final String MISC_ELEM = "misc";
  private static final String FIND_LIST_ELEM = "findlist";
  private static final String FIND_ELEM = "text";
  private static final String WHAT_LIST_ELEM = "whatlist";
  private static final String WHAT_ELEM = "what";
  private static final String LIBRARY_VISIBILITY_INCLUDE_FILTER_LIST_ELEM =
      "hidefuncsincludefilterlist";
  private static final String LIBRARY_VISIBILITY_INCLUDE_FILTER_ELEM = "hidefuncsincludefiltertext";
  private static final String LIBRARY_VISIBILITY_EXCLUDE_FILTER_LIST_ELEM =
      "hidefuncsexcludefilterlist";
  private static final String LIBRARY_VISIBILITY_EXCLUDE_FILTER_ELEM = "hidefuncsexcludefiltertext";
  private static final String LIBRARY_VISIBILITY_CURRENT_FILTER_ATTR = "current";
  private static final String LIBRARY_VISIBILITY_JAVA = "libraryvisibilityjava";
  private static final String CALL_TREE_PROP = "calltree";
  private static final String CALL_TREE_THRESHOLD = "calltreethreshold";
  private static final String FORMATS_ELEM = "formats";
  private static final String FORMATS_VIEW_MODE = "viewmode";
  private static final String FORMATS_STYLE = "style";
  private static final String FORMATS_APPEND_SO_NAME = "appendsoname";
  private static final String FORMATS_COMPARE_MODE = "comparemode";
  private static final String FORMATS_WRAP_METRIC_NAMES_IN_TABLES_ELEM = "wrapmetricnamesintables";
  private static final String SOURCE_DISASSEMBLY_ELEM = "sourceanddisassembly";
  private static final String SOURCE_DISASSEMBLY = "sourcedisassembly";
  private static final String VALUE_ELEM = "value";
  private static final String MACHINE_MODEL_ELEM = "machinemodel";
  private static final String INDEX_OBJECT_LIST_ELEM = "indexobjectlist";
  private static final String MEMORY_OBJECT_LIST_ELEM = "memoryobjectlist";
  private static final String CUSTOM_OBJECT_LIST_ELEM = "customobject";
  private static final String CUSTOM_OBJECT_NAME_ATTR = "name";
  private static final String CUSTOM_OBJECT_FORMULA_ATTR = "formula";
  private static final String CALL_STACK_FUNCTION_COLORS_ELEM = "callstackfunctioncolors";
  private static final String FUNCTION_COLORS_LIST_ELEM = "functioncolors";
  private static final String FUNCTION_COLORS_COLOR_RULE_ELEM = "colorrule";
  private static final String FUNCTION_COLORS_COLOR_RULE_TYPE_ATTR = "type";
  private static final String FUNCTION_COLORS_COLOR_RULE_TEXT_ATTR = "text";
  private static final String FUNCTION_COLORS_COLOR_RULE_COLOR_ATTR = "color";

  private static final String METRICS_ELEM = "metrics";
  private static final String METRIC_SELECTION_LIST_ELEM_V12 = "metricorderlist";
  private static final String METRIC_SELECTION_LIST_ELEM = "metricselectionlist";
  private static final String METRIC_SELECTION_ELEM = "metricelem";
  private static final String METRIC_NAME_ATTR = "name";
  private static final String METRIC_ORDER_SELECTED_ATTR = "selected";
  private static final String METRIC_ORDER_EXCLINCL_ATTR = "x";
  private static final String METRIC_REVERSED_SORT = "metricreversedsort";
  private static final String METRIC_SORT_LIST_ELEM = "metricsortlist";
  private static final String METRIC_SORT_ELEM = "metricsortbymtype";
  private static final String METRIC_MTYPE_ATTR = "mtype";
  private static final String METRIC_SUBTYPE_ATTR = "subtype";
  private static final String METRIC_ORDER_LIST_ELEM = "metricorderlists";
  private static final String METRIC_ORDER_MTYPE_LIST_ELEM = "metricordermtypelist";
  private static final String METRIC_ORDER_ELEM = "metricorderelem";

  private static final String TIMELINE_ELEM = "timeline";
  private static final String TIMELINE_STACK_DEPTH_ELEM = "verticalzoom";
  private static final String TIMELINE_STACK_FRAME_PIXELS_ELEM = "stackframepixels";
  private static final String TIMELINE_GROUP_DATA_BY_BUTTON_NAME_ELEM = "groupdatabybuttonname";
  private static final String TIMELINE_TLDATA_CMD_ELEM = "tldatacmd";
  private static final String TIMELINE_STACK_ALIGN_ELEM = "stackalign";
  private static final String TIMELINE_SHOW_STATES_ELEM = "showstates";
  private static final String TIMELINE_SHOW_DENSITY_ELEM = "showdensity";
  private static final String LIBRARY_VISIBILITY_ELEM = "libraryvisibility";
  private static final String LOADOBJECT_LIST_ELEM = "loadobjects";
  private static final String LOADOBJECT_ITEM_ELEM = "loitem";
  private static final String SEARCHPATH_ELEM = "searchpath";
  private static final String SEARCHPATH_LIST_ELEM = "searchpathlist";
  private static final String SEARCHPATH_ITEM_ELEM = "searchpathitem";
  private static final String PATHMAP_ELEM = "pathmap";
  private static final String PATHMAP_LIST_ELEM = "pathmaplist";
  private static final String PATHMAP_ITEM_ELEM = "pathmapitem";
  private static final String PATHMAP_FROM_ATTR = "from";
  private static final String PATHMAP_TO_ATTR = "to";
  private static final String METRIC_COLOR_LIST_ELEM = "customMetricColors";
  private static final String METRIC_COLOR_ITEM_ELEM = "metric";
  private static final String METRIC_COLOR_NAME_ATTR = "name";
  private static final String METRIC_COLOR_COLOR_ATTR = "color";

  private UserPref userPref;
  private static HashSet<String> experimentErrorHashSet = new HashSet<>();

  public UserPrefPersistence() {}

  public void saveSettings(String path, UserPref userPref, List<UserPref.What> what) {
    PrintWriter out = null;

    String dirPath = AnUtility.dirname(path);
    try {
      if (!new File(dirPath).exists()) {
        new File(dirPath).mkdirs();
      }
      out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception e) {
      saveErrorDialog(path, e);
      return;
    }

    writeInitFile(out, userPref, what);

    out.flush();
    out.close();
  }

  private void saveErrorDialog(String path, Exception e) {
    if (experimentErrorHashSet.contains(path)) {
      return;
    }
    experimentErrorHashSet.add(path);
    StringBuilder sb = new StringBuilder();
    sb.append(AnLocale.getString("Cannot access persistence file:"));
    sb.append("\n");
    sb.append(path);
    sb.append("\n");
    sb.append(AnLocale.getString("Because:"));
    sb.append("\n");
    sb.append(e);
    String errmsg = sb.toString();
    AnUtility.showMessage(AnWindow.getInstance().getFrame(), errmsg, JOptionPane.ERROR_MESSAGE);
  }

  public void restoreSettings(String path, UserPref userPref) {
    this.userPref = userPref;
    if (new File(path).exists()) {
      //            System.out.println("Configuration file processed: " + path);
      AnLog.log("Configuration file processed: " + path);
      parseInitFile(path);
    }
  }

  private void writeInitFile(PrintWriter out, UserPref userPref, List<UserPref.What> what) {
    out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    writeEntry(out, 0, ANALYZER_ELEM, false);
    writeUserPref(out, 2, userPref, what);
    writeEntry(out, 0, ANALYZER_ELEM, true);
  }

  private void writeUserPref(
      PrintWriter out, int indent, UserPref userPref, List<UserPref.What> whatList) {
    writeEntry(out, indent, VERSION_ELEM, "" + version);

    writeWhatList(out, indent, whatList);

    if (whatList.contains(UserPref.What.USER)) {
      writeEntry(out, indent, USER_ELEM, false);
      if (userPref.getFrameSize() != null && userPref.getFrameLocation() != null) {
        AttrValuePair[] attrValues = new AttrValuePair[4];
        attrValues[0] =
            new AttrValuePair(
                FRAME_WIDTH_ATTR,
                ""
                    + (userPref.getFrameSize()
                        .width)); // FIXUP: loosing 10 pixels when ssh from a Mac
        attrValues[1] =
            new AttrValuePair(
                FRAME_HEIGHT_ATTR,
                ""
                    + (userPref.getFrameSize()
                        .height)); // FIXUP: loosing 8 pixels when ssh from a Mac
        attrValues[2] = new AttrValuePair(FRAME_X_ATTR, "" + userPref.getFrameLocation().x);
        attrValues[3] = new AttrValuePair(FRAME_Y_ATTR, "" + userPref.getFrameLocation().y);
        writeEntry(out, indent + 2, FRAME_ELEM, attrValues, true);
      }
      if (userPref.getSplitPane1().getSize() >= 0) {
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(SPLITPANE_RIGHT_SIZE_ATTR, "" + userPref.getSplitPane1().getSize());
        writeEntry(out, indent + 2, SPLITPANE1_ELEM, attrValues, true);
      }
      if (userPref.getSplitPane2().getSize() >= 0) {
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(SPLITPANE_RIGHT_SIZE_ATTR, "" + userPref.getSplitPane2().getSize());
        writeEntry(out, indent + 2, SPLITPANE2_ELEM, attrValues, true);
      }
      if (userPref.getSplitPane3().getSize() >= 0) {
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(SPLITPANE_RIGHT_SIZE_ATTR, "" + userPref.getSplitPane3().getSize());
        writeEntry(out, indent + 2, SPLITPANE3_ELEM, attrValues, true);
      }
      if (userPref.getNavigationPanelDividerPosition() >= 0) {
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(
                SPLITPANE_POS_ATTR, "" + userPref.getNavigationPanelDividerPosition());
        writeEntry(out, indent + 2, NAV_SPLITPANE_ELEM, attrValues, true);
      }
      if (userPref.getNavigationFilterSplitPane().getSize() >= 0
          && userPref.getNavigationFilterSplitPane().getSize()
              < 300) { // FIXUP: sometimes Filter Status panel is too big (18307885)
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(
                SPLITPANE_RIGHT_SIZE_ATTR, "" + userPref.getNavigationFilterSplitPane().getSize());
        writeEntry(out, indent + 2, NAV_FILTER_SPLITPANE_ELEM, attrValues, true);
      }
      writeRecentExperiments(out, indent + 2, userPref);
      writeHostNames(out, indent + 2, userPref);
      writeLastClosedConfPath(out, indent + 2, userPref);
      writeLastExportImportConfPath(out, indent + 2, userPref);
      writeCompareWarning(out, indent + 2, userPref);
      writeCustomMetricColors(out, indent + 2, userPref);
      writeEntry(out, indent, USER_ELEM, true);
    }

    if (whatList.contains(UserPref.What.VIEWS)) {
      writeViews(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.METRICS)) {
      writeMetrics(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.TIMELINE)) {
      writeTimeline(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.SOURCEDISASSEMBLY)) {
      writeSourceDisassembly(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.CALLTREE)) {
      writeCallStack(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.FORMATS)) {
      writeFormats(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.SEARCHPATH)) {
      writeSearchPath(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.PATHMAP)) {
      writePathmap(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.FUNCTIONCOLORS)) {
      writeFunctionColors(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.LIBRARYVISIBILITY)) {
      writeLibraryVisibility(out, indent, userPref);
    }
    if (whatList.contains(UserPref.What.MISC)) {
      writeMisc(out, indent, userPref);
    }
  }

  private void writeViews(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, VIEWS_ELEM, false);
    writeSideBar(out, indent + 2, userPref);
    writeMachineModel(out, indent + 2, userPref);
    writeCustomIndexObjects(out, indent + 2, userPref);
    writeCustomMemoryObjects(out, indent + 2, userPref);
    writeEntry(out, indent, VIEWS_ELEM, true);
  }

  private void writeSideBar(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getViewPanelOrder() != null && !userPref.getViewPanelOrder().isEmpty()) {
      writeEntry(out, indent + 2, VIEW_ORDER_LIST_ELEM, false);
      for (UserPref.ViewPanelOrder elem : userPref.getViewPanelOrder()) {
        AttrValuePair[] attrValues = new AttrValuePair[2];
        attrValues[0] = new AttrValuePair(VIEW_ORDER_NAME_ATTR, "" + elem.getName());
        attrValues[1] = new AttrValuePair(VIEW_ORDER_SHOWN_ATTR, "" + elem.isShown());
        writeEntry(out, indent + 4, VIEW_ORDER_ELEM, attrValues, true);
      }
      writeEntry(out, indent + 2, VIEW_ORDER_LIST_ELEM, true);
    }
  }

  private void writeMetrics(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, METRICS_ELEM, false);
    List<MetricNameSelection> list = userPref.getMetricSelectionList();
    if (list != null && !list.isEmpty()) {
      writeEntry(out, indent + 2, METRIC_SELECTION_LIST_ELEM, false);
      for (MetricNameSelection ms : list) {
        AttrValuePair[] attrValues = new AttrValuePair[8];
        attrValues[0] =
            new AttrValuePair(METRIC_NAME_ATTR, "" + escapeSpecialCharacters(ms.getName()));
        attrValues[1] = new AttrValuePair(METRIC_ORDER_SELECTED_ATTR, "" + ms.isSelected());
        attrValues[2] =
            new AttrValuePair(
                METRIC_ORDER_EXCLINCL_ATTR + 0,
                "" + ms.getMetricSelection().getAttributes().isETime());
        attrValues[3] =
            new AttrValuePair(
                METRIC_ORDER_EXCLINCL_ATTR + 1,
                "" + ms.getMetricSelection().getAttributes().isEValue());
        attrValues[4] =
            new AttrValuePair(
                METRIC_ORDER_EXCLINCL_ATTR + 2,
                "" + ms.getMetricSelection().getAttributes().isEPercent());
        attrValues[5] =
            new AttrValuePair(
                METRIC_ORDER_EXCLINCL_ATTR + 3,
                "" + ms.getMetricSelection().getAttributes().isITime());
        attrValues[6] =
            new AttrValuePair(
                METRIC_ORDER_EXCLINCL_ATTR + 4,
                "" + ms.getMetricSelection().getAttributes().isIValue());
        attrValues[7] =
            new AttrValuePair(
                METRIC_ORDER_EXCLINCL_ATTR + 5,
                "" + ms.getMetricSelection().getAttributes().isIPercent());
        writeEntry(out, indent + 4, METRIC_SELECTION_ELEM, attrValues, true);
      }
      writeEntry(out, indent + 2, METRIC_SELECTION_LIST_ELEM, true);
    }

    if (userPref.getMetricReversedSort() != userPref.getMetricReversedSortDefault()) {
      writeEntry(out, indent + 2, METRIC_REVERSED_SORT, "" + userPref.getMetricReversedSort());
    }

    MetricType[] metricSortByMTypeList = userPref.getMetricSortByMTypeList();
    if (metricSortByMTypeList != null) {
      writeEntry(out, indent + 2, METRIC_SORT_LIST_ELEM, false);
      for (int mType = 0; mType < metricSortByMTypeList.length; mType++) {
        if (mType > MetricsSetting.MET_NORMAL) {
          break; // Just save MET_NORMAL for now
        }
        AttrValuePair[] attrValues = new AttrValuePair[3];
        attrValues[0] = new AttrValuePair(METRIC_MTYPE_ATTR, "" + mType);
        attrValues[1] =
            new AttrValuePair(METRIC_NAME_ATTR, "" + metricSortByMTypeList[mType].getCommand());
        attrValues[2] =
            new AttrValuePair(METRIC_SUBTYPE_ATTR, "" + metricSortByMTypeList[mType].getSubType());
        writeEntry(out, indent + 4, METRIC_SORT_ELEM, attrValues, true);
      }
      writeEntry(out, indent + 2, METRIC_SORT_LIST_ELEM, true);
    }

    List<MetricType>[] metricOrderLists = userPref.getMetricOrderLists();
    if (metricOrderLists != null && metricOrderLists.length > 0) {
      writeEntry(out, indent + 2, METRIC_ORDER_LIST_ELEM, false);
      for (int mType = 0; mType < metricOrderLists.length; mType++) {
        if (mType > MetricsSetting.MET_NORMAL) {
          break; // Just save MET_NORMAL for now
        }
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] = new AttrValuePair(METRIC_MTYPE_ATTR, "" + mType);
        writeEntry(out, indent + 4, METRIC_ORDER_MTYPE_LIST_ELEM, attrValues, false);
        List<MetricType> orderList = metricOrderLists[mType];
        for (MetricType metricType : orderList) {
          attrValues = new AttrValuePair[2];
          attrValues[0] = new AttrValuePair(METRIC_NAME_ATTR, "" + metricType.getCommand());
          attrValues[1] = new AttrValuePair(METRIC_SUBTYPE_ATTR, "" + metricType.getSubType());
          writeEntry(out, indent + 6, METRIC_ORDER_ELEM, attrValues, true);
        }
        writeEntry(out, indent + 4, METRIC_ORDER_MTYPE_LIST_ELEM, true);
      }
      writeEntry(out, indent + 2, METRIC_ORDER_LIST_ELEM, true);
    }

    writeEntry(out, indent, METRICS_ELEM, true);
  }

  private void writeMachineModel(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getMachineModel() != null /*&& userPref.getLoadedMachineModel().length() > 0*/) {
      writeEntry(out, indent, MACHINE_MODEL_ELEM, "" + userPref.getMachineModel());
    }
  }

  private void writeCustomMemoryObjects(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getCustomMemoryObjects() != null && !userPref.getCustomMemoryObjects().isEmpty()) {
      writeEntry(out, indent, MEMORY_OBJECT_LIST_ELEM, false);
      writeCustomObjectList(out, indent + 2, userPref.getCustomMemoryObjects());
      writeEntry(out, indent, MEMORY_OBJECT_LIST_ELEM, true);
    }
  }

  private void writeCustomIndexObjects(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getCustomIndexObjects() != null && !userPref.getCustomIndexObjects().isEmpty()) {
      writeEntry(out, indent, INDEX_OBJECT_LIST_ELEM, false);
      writeCustomObjectList(out, indent + 2, userPref.getCustomIndexObjects());
      writeEntry(out, indent, INDEX_OBJECT_LIST_ELEM, true);
    }
  }

  private void writeCustomObjectList(PrintWriter out, int indent, List<CustomObject> list) {
    for (CustomObject customObject : list) {
      AttrValuePair[] attrValues = new AttrValuePair[2];
      attrValues[0] = new AttrValuePair(CUSTOM_OBJECT_NAME_ATTR, customObject.getName());
      attrValues[1] =
          new AttrValuePair(
              CUSTOM_OBJECT_FORMULA_ATTR, escapeSpecialCharacters(customObject.getFormula()));
      writeEntry(out, indent + 2, CUSTOM_OBJECT_LIST_ELEM, attrValues, true);
    }
  }

  private void writeRecentExperiments(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getExperimentsPicklists().getPicklistMap() != null
        && userPref.getExperimentsPicklists().getPicklistMap().size() > 0) {
      Map<String, ExperimentPickList> map = userPref.getExperimentsPicklists().getPicklistMap();
      Set<String> keySet = map.keySet();
      for (String host : keySet) {
        ExperimentPickList experimentPicklist = map.get(host);
        AttrValuePair[] attrValues;
        attrValues = new AttrValuePair[1];
        attrValues[0] = new AttrValuePair(HOST_NAME_ATTR, host);
        writeEntry(out, indent, EXPERIMENT_LIST_ELEM, attrValues, false);
        for (ExperimentPickListElement experiment : experimentPicklist.getRecentExperiments()) {
          int n = 1;
          if (experiment.getWorkingDirectory() != null) {
            n++;
          }
          if (experiment.getConfPath() != null) {
            n++;
          }
          attrValues = new AttrValuePair[n];
          n = 0;
          attrValues[n++] = new AttrValuePair(PATH_ATTR, experiment.getPath());
          if (experiment.getWorkingDirectory() != null) {
            attrValues[n++] = new AttrValuePair(WD_ATTR, experiment.getWorkingDirectory());
          }
          if (experiment.getConfPath() != null) {
            attrValues[n++] = new AttrValuePair(CONF_ATTR, experiment.getConfPath());
          }
          writeEntry(out, indent + 2, EXPERIMENT_ELEM, attrValues, true);
        }
        writeEntry(out, indent, EXPERIMENT_LIST_ELEM, true);
      }
    }
  }

  private void writeHostNames(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getHostNamePicklist() != null
        && userPref.getHostNamePicklist().getSize() > 0
        && userPref.getConnectionPropertiesMap() != null
        && !userPref.getConnectionPropertiesMap().isEmpty()) {
      writeEntry(out, indent, HOSTNAME_LIST_ELEM, false);
      List<StringPickListElement> elements = userPref.getHostNamePicklist().getStringElements();
      for (int i = elements.size() - 1; i >= 0; i--) {
        StringPickListElement hostElement = elements.get(i);
        String hostName = hostElement.getString();
        String path = userPref.getConnectionPropertiesMap().get(hostName).getPath();
	String connectCommand = userPref.getConnectionPropertiesMap().get(hostName).getConnectCommand();
	if (connectCommand == null) {
	   connectCommand = "";
	}
	String userName = userPref.getConnectionPropertiesMap().get(hostName).getUserName();
        List<Authentication> authentications =
            userPref.getConnectionPropertiesMap().get(hostName).getAuthentications();
        if (userName == null) {
          userName = "";
        }
        if (path != null) {
          List<AttrValuePair> attrValues = new ArrayList<>();
          attrValues.add(new AttrValuePair(HOSTNAME_NAME_ATTR, hostName));
          attrValues.add(new AttrValuePair(HOSTNAME_PATH_ATTR, path));
	  attrValues.add(new AttrValuePair(HOSTNAME_CONNECT_COMMAND_ATTR, connectCommand));
          attrValues.add(new AttrValuePair(HOSTNAME_USERNAME_ATTR, userName));
          if (authentications != null) {
            String auth = Authentication.toXMLString(authentications);
            if (auth != null && UserPref.getDefaultAuthentications() != null
                && !Authentication.toXMLString(UserPref.getDefaultAuthentications())
                    .equals(auth)) {
              attrValues.add(new AttrValuePair(HOSTNAME_AUTH_ATTR, auth));
            }
          }
          writeEntry(out, indent + 4, HOSTNAME_ELEM,
              attrValues.toArray(new AttrValuePair[attrValues.size()]), true);
        }
      }
      writeEntry(out, indent, HOSTNAME_LIST_ELEM, true);
    }
  }

  private void writeLastClosedConfPath(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getLastClosedExpConfPath() != null) {
      writeEntry(out, indent, LAST_CLOSED_EXP_CONF_PATH_ELEM, userPref.getLastClosedExpConfPath());
    }
  }

  private void writeLastExportImportConfPath(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getLastExportImportConfPath() != null) {
      writeEntry(
          out,
          indent,
          LAST_EXPORT_IMPORT_EXP_CONF_PATH_ELEM,
          userPref.getLastExportImportConfPath());
    }
  }

  private void writeCompareWarning(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.showCompareSourceWarning() != userPref.showCompareSourceWarningDefault()) {
      writeEntry(out, indent, SHOW_COMPARE_WARNING_ELEM, "" + userPref.showCompareSourceWarning());
    }
  }

  private void writeCustomMetricColors(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getCustomMetricColors() != null) {
      writeEntry(out, indent, METRIC_COLOR_LIST_ELEM, false);
      List<MetricColor> metricColors = userPref.getCustomMetricColors();
      for (MetricColor metricColor : metricColors) {
        String colorHexRed = String.format("%02x", metricColor.getColor().getRed());
        String colorHexGreen = String.format("%02x", metricColor.getColor().getGreen());
        String colorHexBlue = String.format("%02x", metricColor.getColor().getBlue());
        String colorHex = "0x" + colorHexRed + colorHexGreen + colorHexBlue;
        AttrValuePair[] attrValues = new AttrValuePair[2];
        attrValues[0] = new AttrValuePair(METRIC_COLOR_NAME_ATTR, metricColor.getMetricName());
        attrValues[1] = new AttrValuePair(METRIC_COLOR_COLOR_ATTR, colorHex);
        writeEntry(out, indent + 2, METRIC_COLOR_ITEM_ELEM, attrValues, true);
      }
      writeEntry(out, indent, METRIC_COLOR_LIST_ELEM, true);
    }
  }

  private void writeFindPickList(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getFindPickList() != null && userPref.getFindPickList().getSize() > 0) {
      writeEntry(out, indent, FIND_LIST_ELEM, false);
      List<StringPickListElement> elements = userPref.getFindPickList().getStringElements();
      for (int i = elements.size() - 1; i >= 0; i--) {
        String elem = escapeSpecialCharacters(elements.get(i).getString());
        writeEntry(out, indent + 2, FIND_ELEM, elem);
      }
      writeEntry(out, indent, FIND_LIST_ELEM, true);
    }
  }

  private void writeWhatList(PrintWriter out, int indent, List<UserPref.What> whatList) {
    writeEntry(out, indent, WHAT_LIST_ELEM, false);
    if (whatList != null && !whatList.isEmpty()) {
      for (UserPref.What what : whatList) {
        writeEntry(out, indent + 2, WHAT_ELEM, what.toString());
      }
    }
    writeEntry(out, indent, WHAT_LIST_ELEM, true);
  }

  private void writeLibraryVisibilityPickList(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getLibraryVisibilityIncludePickList() != null
        && userPref.getLibraryVisibilityIncludePickList().getSize() > 0) {
      String currentLibraryVisibilityFilter = userPref.getLibraryVisibilityIncludeFilter();
      if (currentLibraryVisibilityFilter != null && currentLibraryVisibilityFilter.length() > 0) {
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(
                LIBRARY_VISIBILITY_CURRENT_FILTER_ATTR, currentLibraryVisibilityFilter);
        writeEntry(out, indent, LIBRARY_VISIBILITY_INCLUDE_FILTER_LIST_ELEM, attrValues, false);
      } else {
        writeEntry(out, indent, LIBRARY_VISIBILITY_INCLUDE_FILTER_LIST_ELEM, false);
      }
      List<StringPickListElement> elements =
          userPref.getLibraryVisibilityIncludePickList().getStringElements();
      for (int i = elements.size() - 1; i >= 0; i--) {
        String elem = escapeSpecialCharacters(elements.get(i).getString());
        writeEntry(out, indent + 2, LIBRARY_VISIBILITY_INCLUDE_FILTER_ELEM, elem);
      }
      writeEntry(out, indent, LIBRARY_VISIBILITY_INCLUDE_FILTER_LIST_ELEM, true);
    }

    if (userPref.getLibraryVisibilityExcludePickList() != null
        && userPref.getLibraryVisibilityExcludePickList().getSize() > 0) {
      String currentLibraryVisibilityFilter = userPref.getCurrenLibraryVisibilityExcludeFilter();
      if (currentLibraryVisibilityFilter != null && currentLibraryVisibilityFilter.length() > 0) {
        AttrValuePair[] attrValues = new AttrValuePair[1];
        attrValues[0] =
            new AttrValuePair(
                LIBRARY_VISIBILITY_CURRENT_FILTER_ATTR, currentLibraryVisibilityFilter);
        writeEntry(out, indent, LIBRARY_VISIBILITY_EXCLUDE_FILTER_LIST_ELEM, attrValues, false);
      } else {
        writeEntry(out, indent, LIBRARY_VISIBILITY_EXCLUDE_FILTER_LIST_ELEM, false);
      }
      List<StringPickListElement> elements =
          userPref.getLibraryVisibilityExcludePickList().getStringElements();
      for (int i = elements.size() - 1; i >= 0; i--) {
        String elem = escapeSpecialCharacters(elements.get(i).getString());
        writeEntry(out, indent + 2, LIBRARY_VISIBILITY_EXCLUDE_FILTER_ELEM, elem);
      }
      writeEntry(out, indent, LIBRARY_VISIBILITY_EXCLUDE_FILTER_LIST_ELEM, true);
    }
  }

  private void writeCallStack(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, CALL_TREE_PROP, false);
    if (userPref.getCallStackThreshold() != userPref.getCallStackThresholdDefault()) {
      writeEntry(out, indent + 2, CALL_TREE_THRESHOLD, "" + userPref.getCallStackThreshold());
    }
    writeEntry(out, indent, CALL_TREE_PROP, true);
  }

  private void writeFormats(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, FORMATS_ELEM, false);
    if (userPref.getFormatsViewMode() != userPref.getFormatsViewModeDefault()) {
      writeEntry(out, indent + 2, FORMATS_VIEW_MODE, "" + userPref.getFormatsViewMode());
    }
    if (userPref.getFormatsStyle() != userPref.getFormatsStyleDefault()) {
      writeEntry(out, indent + 2, FORMATS_STYLE, "" + userPref.getFormatsStyle());
    }
    if (userPref.getFormatsAppendSoName() != userPref.getFormatsAppendSoNameDefault()) {
      writeEntry(out, indent + 2, FORMATS_APPEND_SO_NAME, "" + userPref.getFormatsAppendSoName());
    }
    if (userPref.getFormatsCompareMode() != userPref.getFormatsCompareModeDefault()) {
      writeEntry(out, indent + 2, FORMATS_COMPARE_MODE, "" + userPref.getFormatsCompareMode());
    }
    if (userPref.wrapMetricNamesInTables() != userPref.wrapMetricNamesInTablesDefault()) {
      writeEntry(
          out,
          indent + 2,
          FORMATS_WRAP_METRIC_NAMES_IN_TABLES_ELEM,
          "" + userPref.wrapMetricNamesInTables());
    }
    writeEntry(out, indent, FORMATS_ELEM, true);
  }

  private void writeSourceDisassembly(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, SOURCE_DISASSEMBLY_ELEM, false);
    if (SourceDisassemblySetting.changed(
        userPref.getSourceDisassemblySettingsDefault(), userPref.getSourceDisassemblySettings())) {
      int[] values = userPref.getSourceDisassemblySettings();
      writeEntry(out, indent + 2, SOURCE_DISASSEMBLY, false);
      for (int value : values) {
        writeEntry(out, indent + 4, VALUE_ELEM, "" + value);
      }
      writeEntry(out, indent + 2, SOURCE_DISASSEMBLY, true);
    }
    writeEntry(out, indent, SOURCE_DISASSEMBLY_ELEM, true);
  }

  private void writeLibraryVisibility(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, LIBRARY_VISIBILITY_ELEM, false);
    writeLibraryVisibilityLoadObjects(out, indent + 2, userPref);
    writeLibraryVisibilityNativeOrJava(out, indent + 2, userPref);
    writeLibraryVisibilityPickList(out, indent + 2, userPref);
    writeEntry(out, indent, LIBRARY_VISIBILITY_ELEM, true);
  }

  private void writeMisc(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, MISC_ELEM, false);
    writeFindPickList(out, indent + 2, userPref);
    writeEntry(out, indent, MISC_ELEM, true);
  }

  private void writeFunctionColors(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, CALL_STACK_FUNCTION_COLORS_ELEM, false);
    List<ColorRule> activeColorRules = userPref.getActiveColorRules();
    if (activeColorRules != null && !activeColorRules.isEmpty()) {
      writeEntry(out, indent + 2, FUNCTION_COLORS_LIST_ELEM, false);
      writeColorRules(out, indent + 4, activeColorRules);
      writeEntry(out, indent + 2, FUNCTION_COLORS_LIST_ELEM, true);
    }
    writeEntry(out, indent, CALL_STACK_FUNCTION_COLORS_ELEM, true);
  }

  private void writeTimeline(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, TIMELINE_ELEM, false);
    if (!userPref
        .getTimelineGroupDataByButtonName()
        .equalsIgnoreCase(userPref.getTimelineGroupDataByButtonNameDefault())) {
      writeEntry(
          out,
          indent + 2,
          TIMELINE_GROUP_DATA_BY_BUTTON_NAME_ELEM,
          "" + escapeSpecialCharacters(userPref.getTimelineGroupDataByButtonName()));
    }
    if (!userPref.getTimelineTLDataCmd().equalsIgnoreCase(userPref.getTimelineTLDataCmdDefault())) {
      writeEntry(out, indent + 2, TIMELINE_TLDATA_CMD_ELEM, "" + userPref.getTimelineTLDataCmd());
    }
    if (userPref.getTimelineStackAlign() != userPref.getDefaultTimelineStackAlign()) {
      writeEntry(out, indent + 2, TIMELINE_STACK_ALIGN_ELEM, "" + userPref.getTimelineStackAlign());
    }
    if (userPref.getTimelineStackDepth() != userPref.getTimelineStackDepthDefault()) {
      writeEntry(out, indent + 2, TIMELINE_STACK_DEPTH_ELEM, "" + userPref.getTimelineStackDepth());
    }
    if (userPref.getTimelineStackFramePixels() != userPref.getTimelineStackFramePixelsDefault()) {
      writeEntry(
          out,
          indent + 2,
          TIMELINE_STACK_FRAME_PIXELS_ELEM,
          "" + userPref.getTimelineStackFramePixels());
    }
    if (userPref.getTimelineShowEventStates() != userPref.getTimelineShowEventStatesDefault()) {
      writeEntry(
          out, indent + 2, TIMELINE_SHOW_STATES_ELEM, "" + userPref.getTimelineShowEventStates());
    }
    if (userPref.getTimelineShowEventDensity() != userPref.getTimelineShowEventDensityDefault()) {
      writeEntry(
          out, indent + 2, TIMELINE_SHOW_DENSITY_ELEM, "" + userPref.getTimelineShowEventDensity());
    }
    writeEntry(out, indent, TIMELINE_ELEM, true);
  }

  private void writeLibraryVisibilityNativeOrJava(PrintWriter out, int indent, UserPref userPref) {
    if (userPref.getLibraryVisibilityJava() != userPref.getLibraryVisibilityJavaDefault()) {
      writeEntry(out, indent, LIBRARY_VISIBILITY_JAVA, "" + userPref.getLibraryVisibilityJava());
    }
  }

  private void writeLibraryVisibilityLoadObjects(PrintWriter out, int indent, UserPref userPref) {
    List<String> list = userPref.getLibraryVisibilitySettings();
    if (list != null && !list.isEmpty()) {
      writeEntry(out, indent, LOADOBJECT_LIST_ELEM, false);
      for (String s : list) {
        writeEntry(out, indent + 2, LOADOBJECT_ITEM_ELEM, escapeSpecialCharacters(s));
      }
      writeEntry(out, indent, LOADOBJECT_LIST_ELEM, true);
    }
  }

  private void writeSearchPath(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, SEARCHPATH_ELEM, false);
    List<String> list = userPref.getSearchPath();
    if (list != null && !list.isEmpty()) {
      if (!SearchPathSetting.isDefault(list)) {
        writeEntry(out, indent + 2, SEARCHPATH_LIST_ELEM, false);
        for (String s : list) {
          writeEntry(out, indent + 4, SEARCHPATH_ITEM_ELEM, escapeSpecialCharacters(s));
        }
        writeEntry(out, indent + 2, SEARCHPATH_LIST_ELEM, true);
      }
    }
    writeEntry(out, indent, SEARCHPATH_ELEM, true);
  }

  private void writePathmap(PrintWriter out, int indent, UserPref userPref) {
    writeEntry(out, indent, PATHMAP_ELEM, false);
    String[][] pathmap = userPref.getPathmap();
    if (pathmap != null && pathmap.length > 0) {
      if (!PathMapSetting.isDefault(pathmap)) {
        writeEntry(out, indent + 2, PATHMAP_LIST_ELEM, false);
        for (int i = 0; i < pathmap[0].length; i++) {
          String from = pathmap[0][i];
          String to = pathmap[1][i];
          AttrValuePair[] attrValues = new AttrValuePair[2];
          attrValues[0] = new AttrValuePair(PATHMAP_FROM_ATTR, from);
          attrValues[1] = new AttrValuePair(PATHMAP_TO_ATTR, to);
          writeEntry(out, indent + 4, PATHMAP_ITEM_ELEM, attrValues, true);
        }
        writeEntry(out, indent + 2, PATHMAP_LIST_ELEM, true);
      }
    }
    writeEntry(out, indent, PATHMAP_ELEM, true);
  }

  private void writeColorRules(PrintWriter out, int indent, List<ColorRule> colorRules) {
    if (colorRules != null) {
      for (ColorRule cr : colorRules) {
        AttrValuePair[] attrValues = new AttrValuePair[3];
        attrValues[0] = new AttrValuePair(FUNCTION_COLORS_COLOR_RULE_TYPE_ATTR, "" + cr.getType());
        String text = cr.getText() != null ? cr.getText() : "";
        attrValues[1] =
            new AttrValuePair(FUNCTION_COLORS_COLOR_RULE_TEXT_ATTR, escapeSpecialCharacters(text));
        attrValues[2] =
            new AttrValuePair(FUNCTION_COLORS_COLOR_RULE_COLOR_ATTR, "" + cr.getColor().getRGB());
        writeEntry(out, indent, FUNCTION_COLORS_COLOR_RULE_ELEM, attrValues, true);
      }
    }
  }

  private String escapeSpecialCharacters(String s) {
    s = s.replace("&", "&amp;");
    s = s.replace("<", "&lt;");
    s = s.replace(">", "&gt;");
    s = s.replace("'", "&apos;");
    s = s.replace("\"", "&quot;");
    return s;
  }

  /** Write entry of this type: <tasks numberoftasks="2"> or <tasks numberoftasks="2"/> */
  private void writeEntry(
      PrintWriter out, int indent, String elemname, AttrValuePair[] attrValues, boolean b) {
    if (indent > 0) {
      out.print(spaces.substring(0, indent));
    }
    out.print("<");
    out.print(elemname);
    if (attrValues != null) {
      for (int i = 0; i < attrValues.length; i++) {
        out.print(" " + attrValues[i].getAttr() + "=\"" + attrValues[i].getValue() + "\"");
      }
    }
    if (b) {
      out.print("/");
    }
    out.println(">");
  }

  /** Write entry of this type: <tasks> or </tasks> */
  private void writeEntry(PrintWriter out, int indent, String elemname, boolean b) {
    if (indent > 0) {
      out.print(spaces.substring(0, indent));
    }
    out.print("<");
    if (b) {
      out.print("/");
    }
    out.print(elemname);
    out.println(">");
  }

  /** Write entry of this type: <tasks>v</tasks> */
  private void writeEntry(PrintWriter out, int indent, String elemname, String v) {
    if (indent > 0) {
      out.print(spaces.substring(0, indent));
    }
    out.print("<");
    out.print(elemname);
    out.print(">");
    out.print(v);
    out.print("</");
    out.print(elemname);
    out.println(">");
  }

  private class AttrValuePair {

    private String attr;
    private String value;

    public AttrValuePair(String attr, String value) {
      this.attr = attr;
      this.value = value;
    }

    public String getAttr() {
      return attr;
    }

    public String getValue() {
      return value;
    }
  }

  class MySaxParser extends DefaultHandler {

    private Stack<String> stack = new Stack<String>();
    private String currentElement = null;
    private String currentText = "";
    private List<ViewPanelOrder> viewPanelOrderList;
    private List<ExperimentPickListElement> recentExperiments;
    private String recentExperimentsHost;
    private Map<String, ConnectionProperties> connectionPropertiesMap;
    private StringPickList hostNamePickList;
    private StringPickList findPickList;
    private List<UserPref.What> whatList;
    private StringPickList libraryVisibilityIncludeFilterPickList;
    private StringPickList libraryVisibilityExcludeFilterPickList;
    private List<Integer> sourceDisassemblyValues;
    private List<CustomObject> customIndexObjects;
    private List<CustomObject> customMemoryObjects;
    private List<ColorRule> functionColors;
    private List<MetricNameSelection> metricsOrderList;
    private MetricType[] metricSortByMType;
    private List<MetricType>[] metricOrderLists;
    private int orderMtype;
    private List<MetricType> metricOrderMTypeList;
    private List<String> loadobjectList;
    private List<String> searchPathList;
    private List<String[]> pathmap;
    private List<MetricColor> customMetricColors;

    public void startDocument() throws SAXException {}

    public void startElement(String namespaceURI, String localName, String element, Attributes atts)
        throws SAXException {
      if (element.equals(FRAME_ELEM)) {
        String w = atts.getValue(FRAME_WIDTH_ATTR);
        String h = atts.getValue(FRAME_HEIGHT_ATTR);
        String x = atts.getValue(FRAME_X_ATTR);
        String y = atts.getValue(FRAME_Y_ATTR);
        try {
          Dimension dim = new Dimension(Integer.valueOf(w), Integer.valueOf(h));
          Point point = new Point(Integer.valueOf(x), Integer.valueOf(y));
          userPref.setFrameSize(dim);
          userPref.setFrameLocation(point);
        } catch (NumberFormatException nfe) {
          errorMessage("INIT_FILE_ERROR", element, nfe);
        }
      } else if (element.equals(VIEW_ORDER_LIST_ELEM)) {
        viewPanelOrderList = new ArrayList<>();
      } else if (element.equals(METRIC_SELECTION_LIST_ELEM)
          || element.equals(METRIC_SELECTION_LIST_ELEM_V12)) {
        metricsOrderList = new ArrayList<>();
      } else if (element.equals(METRIC_SORT_LIST_ELEM)) {
        metricSortByMType = new MetricType[MetricsSetting.MET_LAST];
      } else if (element.equals(METRIC_ORDER_LIST_ELEM)) {
        metricOrderLists = (ArrayList<MetricType>[]) new ArrayList[MetricsSetting.MET_LAST];
      } else if (element.equals(METRIC_ORDER_MTYPE_LIST_ELEM)) {
        String orderMtypeString = atts.getValue(METRIC_MTYPE_ATTR);
        orderMtype = Integer.valueOf(orderMtypeString);
        metricOrderMTypeList = new ArrayList<>();
      } else if (element.equals(LOADOBJECT_LIST_ELEM)) {
        loadobjectList = new ArrayList<>();
      } else if (element.equals(SEARCHPATH_LIST_ELEM)) {
        searchPathList = new ArrayList<>();
      } else if (element.equals(PATHMAP_LIST_ELEM)) {
        pathmap = new ArrayList<>();
      } else if (element.equals(PATHMAP_ITEM_ELEM)) {
        String from = atts.getValue(PATHMAP_FROM_ATTR);
        String to = atts.getValue(PATHMAP_TO_ATTR);
        String[] map = new String[2];
        map[0] = from;
        map[1] = to;
        pathmap.add(map);
      } else if (element.equals(INDEX_OBJECT_LIST_ELEM)) {
        customIndexObjects = new ArrayList<>();
      } else if (element.equals(FUNCTION_COLORS_LIST_ELEM)) {
        functionColors = new ArrayList<>();
      } else if (element.equals(MEMORY_OBJECT_LIST_ELEM)) {
        customMemoryObjects = new ArrayList<>();
      } else if (element.equals(EXPERIMENT_LIST_ELEM)) {
        String host = "localhost"; // pre version 5
        if (userPref.getVersion() >= 5) {
          host = atts.getValue(HOST_NAME_ATTR);
        }
        recentExperiments = new ArrayList<>();
        recentExperimentsHost = host;
      } else if (element.equals(EXPERIMENT_ELEM)) {
        String path = atts.getValue(PATH_ATTR);
        String wd = atts.getValue(WD_ATTR);
        String confPath = atts.getValue(CONF_ATTR);
        recentExperiments.add(new ExperimentPickListElement(path, wd, confPath));
      } else if (element.equals(HOSTNAME_LIST_ELEM)) {
        connectionPropertiesMap = new HashMap<>();
        hostNamePickList = new StringPickList();
      } else if (element.equals(METRIC_COLOR_LIST_ELEM)) {
        customMetricColors = new ArrayList<>();
      } else if (element.equals(HOSTNAME_ELEM)) {
        String name = atts.getValue(HOSTNAME_NAME_ATTR);
        String path = atts.getValue(HOSTNAME_PATH_ATTR);
	String connectCommand = null;
	if (userPref.getVersion() >= 16) {
	  connectCommand = atts.getValue(HOSTNAME_CONNECT_COMMAND_ATTR);
	}
	String userName = null;
        if (userPref.getVersion() >= 4) {
          userName = atts.getValue(HOSTNAME_USERNAME_ATTR);
        }
        List<Authentication> authentications = null;
        String auth = atts.getValue(HOSTNAME_AUTH_ATTR);
        if (auth != null) {
          authentications = Authentication.fromXMLString(auth);
        }
        if (authentications == null) {
          authentications = UserPref.getDefaultAuthentications();
        }
        connectionPropertiesMap.put(
            name, new ConnectionProperties(path, connectCommand, userName, authentications));
        hostNamePickList.addElement(name);
      } else if (element.equals(METRIC_COLOR_ITEM_ELEM)) {
        String name = atts.getValue(METRIC_COLOR_NAME_ATTR);
        String colorString = atts.getValue(METRIC_COLOR_COLOR_ATTR);
        int colorVal;
        if (colorString.startsWith("0x")) {
          colorVal = Integer.parseInt(colorString.substring(2), 16);
        } else {
          colorVal = Integer.parseInt(colorString, 10);
        }
        Color color = new Color(colorVal);
        customMetricColors.add(new MetricColor(name, color));
        UserPref.getInstance().setCustomMetricColors(customMetricColors);
      } else if (element.equals(FIND_LIST_ELEM)) {
        findPickList = new StringPickList(FindTextPanel.MAX_FIND_ITEMS);
      } else if (element.equals(WHAT_LIST_ELEM)) {
        whatList = new ArrayList<UserPref.What>();
      } else if (element.equals(LIBRARY_VISIBILITY_INCLUDE_FILTER_LIST_ELEM)) {
        String currentLibrryVisibilityFilter =
            atts.getValue(LIBRARY_VISIBILITY_CURRENT_FILTER_ATTR);
        UserPref.getInstance().setLibraryVisibilityIncludeFilter(currentLibrryVisibilityFilter);
        libraryVisibilityIncludeFilterPickList = new StringPickList(FindTextPanel.MAX_FIND_ITEMS);
      } else if (element.equals(LIBRARY_VISIBILITY_EXCLUDE_FILTER_LIST_ELEM)) {
        String currentLibraryVisibilityFilter =
            atts.getValue(LIBRARY_VISIBILITY_CURRENT_FILTER_ATTR);
        UserPref.getInstance()
            .setCurrentLibraryVisibilityExcludeFilter(currentLibraryVisibilityFilter);
        libraryVisibilityExcludeFilterPickList = new StringPickList(FindTextPanel.MAX_FIND_ITEMS);
      } else if (element.equals(VIEW_ORDER_ELEM)) {
        String name = atts.getValue(VIEW_ORDER_NAME_ATTR);
        String shownString = atts.getValue(VIEW_ORDER_SHOWN_ATTR);
        viewPanelOrderList.add(new ViewPanelOrder(name, Boolean.valueOf(shownString)));
      } else if (element.equals(METRIC_SELECTION_ELEM)) {
        if (metricsOrderList != null) {
          String name = atts.getValue(METRIC_NAME_ATTR);
          boolean selected = Boolean.valueOf(atts.getValue(METRIC_ORDER_SELECTED_ATTR));
          boolean[] exclIncl = new boolean[6];
          for (int i = 0; i < 6; i++) {
            boolean val = Boolean.valueOf(atts.getValue(METRIC_ORDER_EXCLINCL_ATTR + i));
            exclIncl[i] = val;
          }
          MetricNameSelection metricSetting = new MetricNameSelection(name, selected, exclIncl);
          metricsOrderList.add(metricSetting);
        }
      } else if (element.equals(METRIC_SORT_ELEM)) {
        if (metricSortByMType != null) {
          String mTypeString = atts.getValue(METRIC_MTYPE_ATTR);
          String name = atts.getValue(METRIC_NAME_ATTR);
          String subTypeString = atts.getValue(METRIC_SUBTYPE_ATTR);
          int mType = Integer.valueOf(mTypeString);
          int subType = Integer.valueOf(subTypeString);
          if (mType >= 0 && mType <= MetricsSetting.MET_LAST) {
            metricSortByMType[mType] = new MetricType(name, subType);
          }
        }
      } else if (element.equals(METRIC_ORDER_ELEM)) {
        if (metricOrderMTypeList != null) {
          String name = atts.getValue(METRIC_NAME_ATTR);
          String subTypeString = atts.getValue(METRIC_SUBTYPE_ATTR);
          int subType = Integer.valueOf(subTypeString);
          metricOrderMTypeList.add(new MetricType(name, subType));
        }
      } else if (element.equals(CUSTOM_OBJECT_LIST_ELEM)) {
        String name = atts.getValue(CUSTOM_OBJECT_NAME_ATTR);
        String cormula = atts.getValue(CUSTOM_OBJECT_FORMULA_ATTR);
        CustomObject customObject =
            new CustomObject(name, cormula, null, null); // CXXX add descriptions
        if (customIndexObjects != null) {
          customIndexObjects.add(customObject);
        } else if (customMemoryObjects != null) {
          customMemoryObjects.add(customObject);
        }
      } else if (element.equals(FUNCTION_COLORS_COLOR_RULE_ELEM)) {
        int type = Integer.valueOf(atts.getValue(FUNCTION_COLORS_COLOR_RULE_TYPE_ATTR));
        String text = atts.getValue(FUNCTION_COLORS_COLOR_RULE_TEXT_ATTR);
        if (text.length() == 0) {
          text = null;
        }
        int colorValue =
            Integer.valueOf(atts.getValue(FUNCTION_COLORS_COLOR_RULE_COLOR_ATTR));
        Color color = new Color(colorValue);
        if (functionColors != null) {
          functionColors.add(new ColorRule(color, type, text, false));
        }
      } else if (element.equals(NAV_SPLITPANE_ELEM)) {
        String pos = atts.getValue(SPLITPANE_POS_ATTR);
        try {
          int posValue = Integer.valueOf(pos);
          userPref.setNavigationPanelDividerPosition(posValue);
        } catch (NumberFormatException nfe) {
          errorMessage("INIT_FILE_ERROR", element, nfe);
        }
      } else if (element.equals(NAV_FILTER_SPLITPANE_ELEM)) {
        try {
          splitPaneProp(atts, userPref.getNavigationFilterSplitPane());
        } catch (NumberFormatException nfe) {
          errorMessage("INIT_FILE_ERROR", element, nfe);
        }
      } else if (element.equals(SPLITPANE1_ELEM)) {
        try {
          splitPaneProp(atts, userPref.getSplitPane1());
        } catch (NumberFormatException nfe) {
          errorMessage("INIT_FILE_ERROR", element, nfe);
        }
      } else if (element.equals(SPLITPANE2_ELEM)) {
        try {
          splitPaneProp(atts, userPref.getSplitPane2());
        } catch (NumberFormatException nfe) {
          errorMessage("INIT_FILE_ERROR", element, nfe);
        }
      } else if (element.equals(SPLITPANE3_ELEM)) {
        try {
          splitPaneProp(atts, userPref.getSplitPane3());
        } catch (NumberFormatException nfe) {
          errorMessage("INIT_FILE_ERROR", element, nfe);
        }
      } else if (element.equals(SOURCE_DISASSEMBLY)) {
        sourceDisassemblyValues = new ArrayList<Integer>();
      }

      currentElement = element;
      stack.push(currentElement);
    }

    private void splitPaneProp(
        Attributes atts, UserPref.SplitPaneFixedRightSizeProp splitPaneFixedRightSizeProp)
        throws NumberFormatException {
      String sizeString = atts.getValue(SPLITPANE_RIGHT_SIZE_ATTR);
      if (sizeString != null) {
        int size = Integer.valueOf(sizeString);
        splitPaneFixedRightSizeProp.setSize(size);
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      String newcurrentText = new String(ch, start, length);
      if (newcurrentText.length() > 0 && newcurrentText.charAt(0) == '\n') {
        currentText = "";
      } else {
        currentText = currentText + newcurrentText;
      }
    }

    @Override
    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
        throws SAXException {
      try {
        if (qName.equals(VERSION_ELEM)) {
          userPref.setVersion(Integer.valueOf(currentText));
        } else if (qName.equals(LAST_CLOSED_EXP_CONF_PATH_ELEM)) {
          userPref.setLastClosedExpConfPath(currentText);
        } else if (qName.equals(LAST_EXPORT_IMPORT_EXP_CONF_PATH_ELEM)) {
          userPref.setLastExportImportConfPath(currentText);
        } else if (qName.equals(SHOW_COMPARE_WARNING_ELEM)) {
          userPref.setShowCompareSourceWarning(Boolean.valueOf(currentText));
        } else if (qName.equals(LIBRARY_VISIBILITY_JAVA)) {
          userPref.setLibraryVisibilityJava(Boolean.valueOf(currentText));
        } else if (qName.equals(FORMATS_WRAP_METRIC_NAMES_IN_TABLES_ELEM)) {
          userPref.setWrapMetricNamesInTables(Boolean.valueOf(currentText));
        } else if (qName.equals(METRIC_REVERSED_SORT)) {
          userPref.setMetricReversedSort(Boolean.valueOf(currentText));
        } else if (qName.equals(VIEW_ORDER_LIST_ELEM)) {
          userPref.setViewPanelOrder(viewPanelOrderList);
          viewPanelOrderList = null;
        } else if (qName.equals(METRIC_SELECTION_LIST_ELEM)
            || qName.equals(METRIC_SELECTION_LIST_ELEM_V12)) {
          userPref.setMetricSelectionList(metricsOrderList);
          metricsOrderList = null;
        } else if (qName.equals(METRIC_SORT_LIST_ELEM)) {
          userPref.setMetricSortByMTypeList(metricSortByMType);
          metricSortByMType = null;
        } else if (qName.equals(METRIC_ORDER_LIST_ELEM)) {
          userPref.setMetricOrderLists(metricOrderLists);
          metricOrderLists = null;
        } else if (qName.equals(METRIC_ORDER_MTYPE_LIST_ELEM)) {
          metricOrderLists[orderMtype] = metricOrderMTypeList;
          metricOrderMTypeList = null;
        } else if (qName.equals(LOADOBJECT_ITEM_ELEM)) {
          loadobjectList.add(currentText);
        } else if (qName.equals(SEARCHPATH_ITEM_ELEM)) {
          searchPathList.add(currentText);
        } else if (qName.equals(LOADOBJECT_LIST_ELEM)) {
          userPref.setLibraryVisibilitySettings(loadobjectList);
          loadobjectList = null;
        } else if (qName.equals(SEARCHPATH_LIST_ELEM)) {
          userPref.setSearchPath(searchPathList);
          searchPathList = null;
        } else if (qName.equals(PATHMAP_LIST_ELEM)) {
          String[][] pathmap2 = new String[2][pathmap.size()];
          for (int i = 0; i < pathmap.size(); i++) {
            String[] pathmapItem = pathmap.get(i);
            pathmap2[0][i] = pathmapItem[0];
            pathmap2[1][i] = pathmapItem[1];
          }
          userPref.setPathmap(pathmap2);
          pathmap = null;
        } else if (qName.equals(INDEX_OBJECT_LIST_ELEM)) {
          userPref.setCustomIndexObjects(customIndexObjects);
          customIndexObjects = null;
        } else if (qName.equals(FUNCTION_COLORS_LIST_ELEM)) {
          userPref.setActiveColorRules(functionColors);
          functionColors = null;
        } else if (qName.equals(MEMORY_OBJECT_LIST_ELEM)) {
          userPref.setCustomMemoryObjects(customMemoryObjects);
          customMemoryObjects = null;
        } else if (qName.equals(EXPERIMENT_PATH_ELEM)) {
          recentExperiments.add(new ExperimentPickListElement(currentText, null, null));
        } else if (qName.equals(EXPERIMENT_LIST_ELEM)) {
          userPref.getExperimentsPicklists().add(recentExperimentsHost, recentExperiments);
          recentExperiments = null;
          recentExperimentsHost = null;
        } else if (qName.equals(HOSTNAME_LIST_ELEM)) {
          userPref.setConnectionPropertiesMap(connectionPropertiesMap);
          connectionPropertiesMap = null;
          userPref.setHostNamePicklist(hostNamePickList);
          hostNamePickList = null;
        } else if (qName.equals(METRIC_COLOR_LIST_ELEM)) {
          userPref.setCustomMetricColors(customMetricColors);
          customMetricColors = null;
        } else if (qName.equals(FIND_ELEM)) {
          findPickList.addElement(currentText);
        } else if (qName.equals(WHAT_ELEM)) {
          UserPref.What what = UserPref.What.toWhat(currentText);
          if (what != null) {
            whatList.add(what);
          }
        } else if (qName.equals(LIBRARY_VISIBILITY_INCLUDE_FILTER_ELEM)) {
          libraryVisibilityIncludeFilterPickList.addElement(currentText);
        } else if (qName.equals(LIBRARY_VISIBILITY_EXCLUDE_FILTER_ELEM)) {
          libraryVisibilityExcludeFilterPickList.addElement(currentText);
        } else if (qName.equals(FIND_LIST_ELEM)) {
          userPref.setFindPickList(findPickList);
          findPickList = null;
        } else if (qName.equals(WHAT_LIST_ELEM)) {
          userPref.setWhatList(whatList);
          whatList = null;
        } else if (qName.equals(LIBRARY_VISIBILITY_INCLUDE_FILTER_LIST_ELEM)) {
          userPref.setLibraryVisibilityIncludePickList(libraryVisibilityIncludeFilterPickList);
          libraryVisibilityIncludeFilterPickList = null;
        } else if (qName.equals(LIBRARY_VISIBILITY_EXCLUDE_FILTER_LIST_ELEM)) {
          userPref.setLibraryVisibilityExcludePickList(libraryVisibilityExcludeFilterPickList);
          libraryVisibilityExcludeFilterPickList = null;
        } else if (qName.equals(CALL_TREE_THRESHOLD)) {
          userPref.setCallStackThreshold(Integer.valueOf(currentText));
        } else if (qName.equals(TIMELINE_GROUP_DATA_BY_BUTTON_NAME_ELEM)) {
          userPref.setTimelineGroupDataByButtonName(currentText);
        } else if (qName.equals(TIMELINE_TLDATA_CMD_ELEM)) {
          userPref.setTimelineTLDataCmd(currentText);
        } else if (qName.equals(TIMELINE_STACK_ALIGN_ELEM)) {
          userPref.setTimelineStackAlign(Integer.valueOf(currentText));
        } else if (qName.equals(TIMELINE_STACK_DEPTH_ELEM)) {
          userPref.setTimelineStackDepth(Integer.valueOf(currentText));
        } else if (qName.equals(TIMELINE_STACK_FRAME_PIXELS_ELEM)) {
          userPref.setTimelineStackFramePixels(Integer.valueOf(currentText));
        } else if (qName.equals(TIMELINE_SHOW_STATES_ELEM)) {
          userPref.setTimelineShowEventStates(Boolean.valueOf(currentText));
        } else if (qName.equals(TIMELINE_SHOW_DENSITY_ELEM)) {
          userPref.setTimelineShowEventDensity(Boolean.valueOf(currentText));
        } else if (qName.equals(MACHINE_MODEL_ELEM)) {
          userPref.setMachineModel(currentText);
        } else if (qName.equals(FORMATS_VIEW_MODE)) {
          userPref.setFormatsViewMode(Integer.valueOf(currentText));
        } else if (qName.equals(FORMATS_STYLE)) {
          userPref.setFormatsStyle(Integer.valueOf(currentText));
        } else if (qName.equals(FORMATS_APPEND_SO_NAME)) {
          userPref.setFormatsAppendSoName(Boolean.valueOf(currentText));
        } else if (qName.equals(FORMATS_COMPARE_MODE)) {
          userPref.setFormatsCompareMode(Integer.valueOf(currentText));
        } else if (qName.equals(VALUE_ELEM)) {
          sourceDisassemblyValues.add(Integer.valueOf(currentText));
        } else if (qName.equals(SOURCE_DISASSEMBLY)) {
          // If reading older versions, copy default dettings into missing values
          int[] defaultSettings = SourceDisassemblySetting.getDefaultSourceDisassemblySetting();
          int[] values = new int[defaultSettings.length];
          int i = 0;
          for (i = 0; i < sourceDisassemblyValues.size(); i++) {
            if (i < values.length) {
              values[i] = sourceDisassemblyValues.get(i);
            }
          }
          // If reading an older version, fill in with default values for the missing settings
          for (; i < defaultSettings.length; i++) {
            values[i] = defaultSettings[i];
          }
          userPref.setSourceDisassemblySettings(values);
        }
      } catch (NumberFormatException ne) {
      }
      stack.pop();

      if (stack.empty()) {
        currentElement = null;
      } else {
        currentElement = stack.peek();
      }
    }

    public void endDocument() throws SAXException {}
  }

  class MyErrorHandler implements ErrorHandler {

    private PrintStream out;

    MyErrorHandler(PrintStream out) {
      this.out = out;
    }

    private String getParseExceptionInfo(SAXParseException spe) {
      String systemId = spe.getSystemId();
      if (systemId == null) {
        systemId = "null";
      }
      String info = "URI=" + systemId + " Line=" + spe.getLineNumber() + ": " + spe.getMessage();
      return info;
    }

    public void warning(SAXParseException spe) throws SAXException {
      warningMessage("INIT_FILE_READ_WARNING", getParseExceptionInfo(spe));
    }

    public void error(SAXParseException spe) throws SAXException {
      throw new SAXException("INIT_FILE_ERROR" + getParseExceptionInfo(spe));
    }

    public void fatalError(SAXParseException spe) throws SAXException {
      throw new SAXException("INIT_FILE_ERROR" + getParseExceptionInfo(spe));
    }
  }

  private String convertToFileURL(String filename) {
    String path = new File(filename).getAbsolutePath();
    if (File.separatorChar != '/') {
      path = path.replace(File.separatorChar, '/');
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return "file:" + path;
  }

  public void parseInitFile(String filename) {
    boolean validation = false;

    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setValidating(validation);

    XMLReader xmlReader = null;
    try {
      SAXParser saxParser = spf.newSAXParser();
      xmlReader = saxParser.getXMLReader();
    } catch (Exception ex) {
      System.err.println(ex);
      return;
    }

    xmlReader.setContentHandler(new MySaxParser());
    xmlReader.setErrorHandler(new MyErrorHandler(System.err));

    try {
      xmlReader.parse(convertToFileURL(filename));
    } catch (SAXException se) {
      errorMessage("INIT_FILE_READ_ERROR", se.getMessage());
    } catch (IOException ioe) {
      errorMessage("INIT_FILE_READ_ERROR", ioe.getMessage());
    }
  }

  private void errorMessage(String message, String error) {
    System.err.println("Error: " + message + " " + error);
  }

  private void errorMessage(String message, String error, Exception e) {
    System.err.println("Error: " + message + " " + error + " " + e);
  }

  private void warningMessage(String message, String warning) {
    System.err.println("Warning: " + message + " " + warning);
  }
}
