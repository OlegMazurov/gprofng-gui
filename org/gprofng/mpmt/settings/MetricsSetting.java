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

package org.gprofng.mpmt.settings;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnDouble;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnLong;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnObject;
import org.gprofng.mpmt.AnString;
import org.gprofng.mpmt.AnTable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.metrics.MetricAttr;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.metrics.MetricNode;
import org.gprofng.mpmt.metrics.SelectableMetricNode;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

public class MetricsSetting extends Setting {

  private AnMetric[] availableAnMetrics; // Static once experiment has been loaded

  private MetricStates metricStates; // the states

  public static final int MET_NORMAL = 0;
  public static final int MET_CALL = 1;
  public static final int MET_DATA = 2;
  public static final int MET_INDEX = 3;
  public static final int MET_CALL_AGR = 4;
  public static final int MET_COMMON = 5;
  public static final int MET_IO = 6;
  public static final int MET_SRCDIS = 7;
  public static final int MET_HEAP = 8;
  public static final int MET_LAST = 9; // Count of metrics; <last metric type> + 1

  public MetricsSetting() {}

  public void setHotMetric(String name, boolean hot) {
    for (AnMetric anMetric : availableAnMetrics) {
      if (anMetric.getComd().equals(name)) {
        anMetric.setHot(hot);
        break;
      }
    }
  }

  /** Called from loadExperimentGroupsInternal when experiment has been loaded */
  public synchronized void initMetricListsByMType() {
    metricStates = new MetricStates();
    metricStates.resetMetricListsByMType();
  }

  /** Called from loadExperimentGroupsInternal when experiment has been loaded */
  public synchronized void init(
      Object originalSource,
      List<MetricNameSelection> metricsSelectionList,
      Object[] ref_data,
      MetricNode metricsRootNode,
      boolean metricsReversedSort,
      MetricType[] metricSortByMType,
      List<MetricType>[] metricOrderLists) {
    availableAnMetrics = Settings.getMetricList(ref_data);
    List<MetricState> metricStateList = constructMetricStates(metricsRootNode);
    metricStates.updateStates(metricsRootNode, metricStateList);

    init(
        originalSource,
        metricsSelectionList,
        metricsReversedSort,
        metricSortByMType,
        metricOrderLists);
  }

  private List<MetricState> constructMetricStates(MetricNode metricsRootNode) {
    List<MetricState> list = new ArrayList<MetricState>();
    constructMetricStates(metricsRootNode, null, -1, list);
    // Add Name metric to the end
    AnMetric nameMetric = availableAnMetrics[availableAnMetrics.length - 1]; // Always last ?
    if (nameMetric.isNameMetric()) {
      MetricAttributes capableAttributes =
          new MetricAttributes(false, false, false, false, false, false);
      MetricAttributes factoryAttributes =
          new MetricAttributes(false, false, false, false, false, false);
      MetricSelection factorySelection = new MetricSelection(true, factoryAttributes);
      MetricSelection selection = factorySelection.copy();
      list.add(
          new MetricState("Name", -1, nameMetric, capableAttributes, factorySelection, selection));
    } else {
      System.err.println("METRICERROR: constructMetricStates: name metric is not last!");
    }
    return list;
  }

  private void constructMetricStates(
      MetricNode node, String listDisplayName, int listId, List<MetricState> list) {
    if (node.getMetricType() == MetricNode.MetricType.PLAIN_LIST_CATEGORY) {
      listDisplayName = node.getMetricBasic().getDisplayName();
      String listName = node.getMetricBasic().getName();
      if (listName.contains("PROFDATA_TYPE_HEAP")) { // heap
        listId = MetricNode.HEAP_LIST_ID;
      } else if (listName.contains("PROFDATA_TYPE_IOTRACE")) { // io
        listId = MetricNode.IO_LIST_ID;
      } else if (listName.contains("Dataspace")) { // old name ???
        listId = MetricNode.DATASPACE_HWC_LIST_ID;
      } else if (listName.contains("PROFDATA_TYPE_HWC_DSPACE")) { // memoryspace
        listId = MetricNode.DATASPACE_HWC_LIST_ID;
      } else {
        listId = MetricNode.ALL_LIST_ID;
      }
    }
    if (node instanceof SelectableMetricNode) {
      SelectableMetricNode selectableMetricNode = (SelectableMetricNode) node;
      String name = selectableMetricNode.getMetricBasic().getName();
      AnMetric anMetric = null;
      for (AnMetric am : availableAnMetrics) {
        if (am.getComd().equals(name)) {
          anMetric = am;
        }
      }
      if (anMetric == null) {
        System.err.println("METRICERROR: constructMetricStates: can't find metric " + name);
      }
      // Capable
      MetricAttr exclusiveCapable = selectableMetricNode.getExclusive().getCapable();
      MetricAttr inclusiveCapable = selectableMetricNode.getInclusive().getCapable();
      MetricAttributes capableAttributes =
          new MetricAttributes(
              exclusiveCapable.isTime(),
              exclusiveCapable.isValue(),
              exclusiveCapable.isPercent(),
              inclusiveCapable.isTime(),
              inclusiveCapable.isValue(),
              inclusiveCapable.isPercent());
      // Factory
      MetricAttr exclusiveDefault = selectableMetricNode.getExclusive().getDefaultAttrs();
      MetricAttr inclusiveDefault = selectableMetricNode.getInclusive().getDefaultAttrs();
      boolean defaultSelected =
          exclusiveDefault.hasVisibleAttrs() || inclusiveDefault.hasVisibleAttrs();
      MetricAttributes factoryAttributes =
          new MetricAttributes(
              exclusiveDefault.isTime(),
              exclusiveDefault.isValue(),
              exclusiveDefault.isPercent(),
              inclusiveDefault.isTime(),
              inclusiveDefault.isValue(),
              inclusiveDefault.isPercent());
      MetricSelection factorySelection = new MetricSelection(defaultSelected, factoryAttributes);
      // Selection
      MetricSelection selection = factorySelection.copy();
      list.add(
          new MetricState(
              listDisplayName, listId, anMetric, capableAttributes, factorySelection, selection));
    }
    for (MetricNode child : node.getChildren()) {
      constructMetricStates(child, listDisplayName, listId, list);
    }
  }

  /*
   * Called from Settings, Overview, ...
   */
  public synchronized void set(Object originalSource, String metricName, boolean selected) {
    MetricStates metricStatesCopy = metricStates.copy();
    metricStatesCopy.setSelection(metricName, selected);
    set(originalSource, metricStatesCopy);
  }

  /*
   * Called from Overview, ...
   */
  public synchronized void setList(
      Object originalSource, List<MetricNameSelected> metricNameSelectedList) {
    MetricStates metricStatesCopy = metricStates.copy();
    for (MetricNameSelected mns : metricNameSelectedList) {
      metricStatesCopy.setSelection(mns.getName(), mns.isSelected());
    }
    set(originalSource, metricStatesCopy);
  }

  private void set(Object originalSource, MetricStates newMetricStates) {
    setValueAndFireChangeEvent(originalSource, this, newMetricStates);
  }

  private synchronized void set(
      Object originalSource, String metricName, MetricAttributes metricAttributes) {
    MetricStates metricStatesCopy = metricStates.copy();
    MetricState metricState = metricStatesCopy.findMetricStateByName(metricName);
    if (metricState != null) {
      metricState.getSelection().setAttributes(metricAttributes.copy());
      metricState.getSelection().setSelected(metricAttributes.anySelections());
      set(originalSource, metricStatesCopy);
    }
  }

  /*
   * Not used in Dodona
   */
  public synchronized void setExclusive(
      Object originalSource,
      String metricName,
      boolean eTimeSelected,
      boolean eValueSelected,
      boolean ePercentSelected) {
    MetricState metricState = metricStates.findMetricStateByName(metricName);
    if (metricState != null) {
      MetricAttributes metricAttributesCopy = metricState.getSelection().getAttributes().copy();
      metricAttributesCopy.setETime(eTimeSelected);
      metricAttributesCopy.setEValue(eValueSelected);
      metricAttributesCopy.setEPercent(ePercentSelected);
      set(originalSource, metricName, metricAttributesCopy);
    }
  }

  /*
   * Not used in Dodona
   */
  public synchronized void setInclusive(
      Object originalSource,
      String metricName,
      boolean iTimeSelected,
      boolean iValueSelected,
      boolean iPercentSelected) {
    MetricState metricState = metricStates.findMetricStateByName(metricName);
    if (metricState != null) {
      MetricAttributes metricAttributesCopy = metricState.getSelection().getAttributes().copy();
      metricAttributesCopy.setITime(iTimeSelected);
      metricAttributesCopy.setIValue(iValueSelected);
      metricAttributesCopy.setIPercent(iPercentSelected);
      set(originalSource, metricName, metricAttributesCopy);
    }
  }

  /** Called from Settings */
  public synchronized void set(Object originalSource, List<MetricNameSelection> list) {
    boolean changed = true;
    //        for (int i = 0; i < metricStates.getSelections().length; i++) { // OK
    //            if (metricStates.getSelections()[i] != newChksBoxValue[i]) { // OK
    //                changed = true;
    //                break;
    //            }
    //        }
    if (changed) {
      MetricStates metricStatesCopy = metricStates.copy();
      //            metricStatesCopy.setSelections(newChksBoxValue);
      metricStatesCopy.setSelection(list);
      setValueAndFireChangeEvent(originalSource, this, metricStatesCopy);
    }
  }

  /** Called when changing column order (old headers) */
  private synchronized void setMetricOrder(Object originalSource, int from, int to, int mtype) {
    MetricStates metricStatesCopy = metricStates.copy();
    metricStatesCopy.updateMetricOrder(from, to, mtype);
    setValueAndFireChangeEvent(originalSource, this, metricStatesCopy);
  }

  /** Called when changing column order (new headers) */
  public synchronized void setMetricOrder(
      Object originalSource, String metricName, String metricRefName, boolean before) {
    setMetricOrder(originalSource, metricName, metricRefName, before, MET_NORMAL);
  }

  private synchronized void setMetricOrder(
      Object originalSource, String metricName, String metricRefName, boolean before, int mtype) {
    MetricStates metricStatesCopy = metricStates.copy();
    metricStatesCopy.updateMetricOrder(metricName, metricRefName, before, mtype);
    setValueAndFireChangeEvent(originalSource, this, metricStatesCopy);
  }

  private synchronized void setSortMetric(Object originalSource, int index, int mtype) {
    MetricStates metricStatesCopy = metricStates.copy();
    metricStatesCopy.updateSortMetric(index, mtype);
    setValueAndFireChangeEvent(originalSource, this, metricStatesCopy);
  }

  private void overrideMetricStates(
      MetricStates metricStates,
      List<MetricNameSelection> metricsSelectionList,
      boolean metricsReversedSort,
      MetricType[] metricSortByMType,
      List<MetricType>[] metricOrderLists) {
    // Override selection with saved metrics settings
    //        if (metricsSelectionList != null) {
    //            for (MetricNameSelection ms : metricsSelectionList) {
    //                int baseIndex = findBaseIndex(ms.getName());
    //                if (baseIndex >= 0) {
    //                    for (int i = 0; i < 6; i++) {
    //                        metricStates.getSelections()[6 * baseIndex + i] =
    // ms.getExclusiveInclusive()[i]; // OK
    //                    }
    //                }
    //            }
    //        }
    metricStates.setSelection(metricsSelectionList);

    // Override sort direction
    metricStates.setNewReverseSortDirection(metricsReversedSort);

    // Override sort metric (restore only MET_NORMAL)
    if (metricSortByMType != null) {
      metricStates.setNewSortMetric(metricSortByMType[MET_NORMAL]);
    }

    // Override metric order (restore only MET_NORMAL)
    if (metricOrderLists != null) {
      List<MetricType> newMetricOrderList = metricOrderLists[MET_NORMAL];
      //            MetricType[] newMetricOrderList = normalListOrder.toArray(new
      // MetricType[normalListOrder.size()]);
      metricStates.setNewMetricOrder(newMetricOrderList);
    }
  }

  /** Called from Import Settings */
  public synchronized void set(
      Object originalSource,
      List<MetricNameSelection> metricsSelectionList,
      boolean metricsReversedSort,
      MetricType[] metricSortByMType,
      List<MetricType>[] metricOrderLists) {
    MetricStates metricStatesCopy = metricStates.copy();
    overrideMetricStates(
        metricStatesCopy,
        metricsSelectionList,
        metricsReversedSort,
        metricSortByMType,
        metricOrderLists);
    setValueAndFireChangeEvent(originalSource, this, metricStatesCopy);
  }

  private void init(
      Object originalSource,
      List<MetricNameSelection> metricsSelectionList,
      boolean metricsReversedSort,
      MetricType[] metricSortByMType,
      List<MetricType>[] metricOrderLists) {
    overrideMetricStates(
        metricStates,
        metricsSelectionList,
        metricsReversedSort,
        metricSortByMType,
        metricOrderLists);
    updateBackend(); // IPC
    fireChangeEvent(originalSource, metricStates);
  }

  public AnMetric[] getAvailableAnMetrics() {
    return availableAnMetrics;
  }

  public AnMetric getAvailableAnMetric(String cmd) {
    for (AnMetric anMetric : availableAnMetrics) {
      if (anMetric.getComd().equals(cmd)) {
        return anMetric;
      }
    }
    return null;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.METRICS;
  }

  public synchronized MetricStates getMetricStates() {
    return metricStates;
  }

  public synchronized AnMetric[] getMetricListByMType(int mtype) {
    if (getMetricStates() == null) {
      return null;
    }
    return getMetricStates().getMetricListByMType(mtype);
  }

  public synchronized AnMetric[] getMetricListByDType(int dtype) {
    return getMetricListByMType(dtype2mtype(dtype));
  }

  // Get index of sorting metric
  public static synchronized int getSortIndex(final AnMetric[] mlist) {
    for (int i = 0; i < mlist.length; i++) {
      if (mlist[i].isSorted()) {
        return i;
      }
    }
    return 0;
  }

  public synchronized int getSortColumnByDType(int dtype) {
    int mtype = dtype2mtype(dtype);
    return getSortIndex(mtype);
  }

  // Get index of sorting metric
  private int getSortIndex(final int mtype) {
    final AnMetric[] mlist = getMetricListByMType(mtype);
    return getSortIndex(mlist);
  }

  @Override
  Object getValue() {
    return metricStates;
  }

  @Override
  synchronized void setValue(Object newValue) {
    MetricStates newMetricStates = (MetricStates) newValue;
    metricStates = newMetricStates;
    updateBackend(); // IPC
  }

  public synchronized boolean isSelected(String metricName) {
    return getMetricStates().isSelected(metricName);
  }

  public synchronized List<MetricNameSelection> getMetricOrder() {
    List<MetricNameSelection> list = new ArrayList<MetricNameSelection>();
    if (availableAnMetrics != null) {
      for (AnMetric anMetric : availableAnMetrics) {
        String name = anMetric.getComd();
        MetricState metricState = metricStates.findMetricStateByName(name);
        if (metricState != null) {
          MetricNameSelection metricSetting =
              new MetricNameSelection(name, metricState.getSelection().copy());
          list.add(metricSetting);
        }
      }
    }

    return list;
  }

  public static synchronized int getNameColumnIndex(final AnMetric[] mlist) {
    for (int i = 0; i < mlist.length; i++) {
      if (mlist[i].isNameMetric()) {
        return i;
      }
    }
    return -1;
  }

  public synchronized int getNameColumnIndexByDType(int dtype) {
    AnMetric[] list = getMetricListByDType(dtype);
    return getNameColumnIndex(list);
  }

  private int str_cmp(final String s1, final String s2) {
    if (s1 == null) {
      return s2 == null ? 0 : 1;
    }
    if (s2 == null) {
      return -1;
    }
    return s1.compareTo(s2);
  }

  private AnMetric[] reorder(List<MetricType> metricOrderTemplate, AnMetric[] mMetricList) {
    AnMetric[] newMMetricList = new AnMetric[mMetricList.length];
    // If last entry in metricOrderTemplate is Name, remove this entry so all unmatched metrics get
    // added to the end of list but also to the left of Name (23332712)
    int lastEntryIndex = metricOrderTemplate.size() - 1;
    if (lastEntryIndex >= 0) {
      if (metricOrderTemplate.get(lastEntryIndex).getCommand().equals("name")) {
        metricOrderTemplate.remove(lastEntryIndex);
      }
    }
    int newIndex = 0;
    for (MetricType metricType : metricOrderTemplate) {
      for (int index = 0; index < mMetricList.length; index++) {
        if (mMetricList[index] != null) {
          if (mMetricList[index].getComd().equals(metricType.getCommand())
              && mMetricList[index].getSubType() == metricType.getSubType()) {
            newMMetricList[newIndex++] = mMetricList[index];
            mMetricList[index] = null;
            break;
          }
        }
      }
    }
    for (AnMetric anMetric : mMetricList) {
      if (anMetric != null) {
        newMMetricList[newIndex++] = anMetric;
      }
    }
    return newMMetricList;
  }

  private synchronized void updateBackend() {
    List<int[]> sortColumnListChanged = metricStates.getChangedMetricSortColumns();
    if (sortColumnListChanged.size() > 0) {
      metricStates.transferSortChangesToMetricStates(sortColumnListChanged);
    }

    List<AnMetric> metricsWithSelections = new ArrayList<AnMetric>();
    List<MetricState> metricStatesList = getMetricStates().getMetricStateList();
    if (metricStatesList != null) {
      for (MetricState metricState : metricStatesList) {
        AnMetric bm = metricState.getAnMetric();
        MetricAttributes metricAttributes = metricState.getSelection().getAttributes();
        if (bm.isNameMetric()) {
          AnMetric m = new AnMetric(bm);
          metricsWithSelections.add(m);
          m.setSubType(AnMetric.STATIC);
          m.set_vis_bits(false, true, false);
          continue;
        }
        if (bm.isStatic() && metricState.getSelection().isSelected()) {
          AnMetric m = new AnMetric(bm);
          metricsWithSelections.add(m);
          m.setSubType(AnMetric.STATIC);
          m.set_vis_bits(false, true, false);
          continue;
        }
        if (metricState.getSelection().isSelected()
            && (metricAttributes.isETime()
                || metricAttributes.isEValue()
                || metricAttributes.isEPercent())) {
          AnMetric m = new AnMetric(bm);
          metricsWithSelections.add(m);
          m.setSubType(AnMetric.EXCLUSIVE);
          m.set_vis_bits(
              metricAttributes.isETime(),
              metricAttributes.isEValue(),
              metricAttributes.isEPercent());
        }
        if (metricState.getSelection().isSelected()
            && (metricAttributes.isITime()
                || metricAttributes.isIValue()
                || metricAttributes.isIPercent())) {
          AnMetric m = new AnMetric(bm);
          metricsWithSelections.add(m);
          m.setSubType(AnMetric.INCLUSIVE);
          m.set_vis_bits(
              metricAttributes.isITime(),
              metricAttributes.isIValue(),
              metricAttributes.isIPercent());
        }
      }
    }
    boolean set_sort = false;
    List<AnMetric> metricsWithSelectionsSorted = new ArrayList<AnMetric>();

    // populate sorted_mlist - start with items in the previous list
    AnMetric[] metrics;
    if (getMetricStates().getMetricListsByMType() != null) {
      metrics =
          getMetricStates().getMetricListsByMType()[MetricsSetting.MET_NORMAL].getmMetricList();
    } else {
      metrics = getMetricListByMType(MetricsSetting.MET_NORMAL);
    }
    for (AnMetric m : metrics) {
      String expr_spec = m.expr_spec;
      m.expr_spec = null;
      int ind = m.indexIn(metricsWithSelections);
      m.expr_spec = expr_spec;
      if (ind >= 0) {
        metricsWithSelectionsSorted.add(metricsWithSelections.get(ind));
        if (m.isSorted()) {
          metricsWithSelections.get(ind).setSorted(true);
          set_sort = true;
        }
        metricsWithSelections.remove(ind);
      }
    }
    // if the last metric in old was "name", save it for last in new
    if (!metricsWithSelectionsSorted.isEmpty()) {
      AnMetric last = metricsWithSelectionsSorted.get(metricsWithSelectionsSorted.size() - 1);
      if (last.isNameMetric()) {
        metricsWithSelections.add(last);
        metricsWithSelectionsSorted.remove(last);
      }
    }
    // add remaning items, they are newly enabled metrics
    metricsWithSelectionsSorted.addAll(metricsWithSelections);

    if (!set_sort) {
      metricsWithSelectionsSorted.get(0).setSorted(true);
    }

    AnMetric[] metricsWithSelectionsSortedArray = new AnMetric[metricsWithSelectionsSorted.size()];
    metricsWithSelectionsSorted.toArray(metricsWithSelectionsSortedArray);

    // Override MET_NORMAL metric order (from init)
    List<MetricType> metricOrderTemplate = metricStates.getNewMetricOrder();
    if (metricOrderTemplate != null) {
      metricsWithSelectionsSortedArray =
          reorder(metricOrderTemplate, metricsWithSelectionsSortedArray);
      metricStates.setNewMetricOrder(null);
    }

    // Override MET_NORMAL sort metric (from init)
    MetricType newSortmetric = metricStates.getNewSortMetric();
    if (newSortmetric != null) {
      AnMetric sortMetric = null;
      for (AnMetric anMetric : metricsWithSelectionsSortedArray) {
        if (anMetric.getComd().equals(newSortmetric.getCommand())
            && anMetric.getSubType() == newSortmetric.getSubType()) {
          sortMetric = anMetric;
          break;
        }
      }
      if (sortMetric != null) {
        for (AnMetric anMetric : metricsWithSelectionsSortedArray) {
          anMetric.setSorted(false);
        }
        sortMetric.setSorted(true);
      }
      metricStates.setNewSortMetric(null);
    }

    // Sort the metric list so excluded/included for the same metric are next to each other and
    // excluded is before included.
    //        System.out.println("-----------------------------------------------");
    //        for (AnMetric anMetric : metricsWithSelectionsSortedArray) {
    //            System.out.println(anMetric.getUserName() + " " + anMetric.getSubType());
    //        }
    // Sort so excluded/included for the same metric are next to each other
    List<AnMetric> pairedList = new ArrayList<AnMetric>();
    int index1 = 0;
    while (index1 < metricsWithSelectionsSortedArray.length) {
      AnMetric anMetric = metricsWithSelectionsSortedArray[index1];
      if (anMetric != null) {
        pairedList.add(anMetric);
        metricsWithSelectionsSortedArray[index1] = null;
        int index2 = index1;
        while (index2 < metricsWithSelectionsSortedArray.length) {
          if (metricsWithSelectionsSortedArray[index2] != null
              && metricsWithSelectionsSortedArray[index2].getComd().equals(anMetric.getComd())) {
            pairedList.add(metricsWithSelectionsSortedArray[index2]);
            metricsWithSelectionsSortedArray[index2] = null;
            break;
          }
          index2++;
        }
      }
      index1++;
    }
    pairedList.toArray(metricsWithSelectionsSortedArray);
    //        System.out.println("----------");
    //        for (AnMetric anMetric : metricsWithSelectionsSortedArray) {
    //            System.out.println(anMetric.getUserName() + " " + anMetric.getSubType());
    //        }
    // Sort so excluded is before included
    for (int index = 0; index < metricsWithSelectionsSortedArray.length; index++) {
      if (index + 1 < metricsWithSelectionsSortedArray.length) {
        AnMetric anMetric1 = metricsWithSelectionsSortedArray[index];
        AnMetric anMetric2 = metricsWithSelectionsSortedArray[index + 1];
        if (anMetric1.getComd().equals(anMetric2.getComd())) {
          if (anMetric1.getSubType() == 4 && anMetric2.getSubType() == 2) {
            metricsWithSelectionsSortedArray[index] = anMetric2;
            metricsWithSelectionsSortedArray[index + 1] = anMetric1;
          }
        }
      }
    }
    //        System.out.println("----------");
    //        for (AnMetric anMetric : metricsWithSelectionsSortedArray) {
    //            System.out.println(anMetric.getUserName() + " " + anMetric.getSubType());
    //        }

    // Set visible metrics and order
    setCurMetricsV2IPC(
        MET_NORMAL,
        metricsWithSelectionsSortedArray,
        AnWindow.getInstance().getSettings().getCompareModeSetting().get().value()); // IPC

    // Override sort direction (from init)
    Boolean newReverseSortDirection = metricStates.getNewReverseSortDirection();
    if (newReverseSortDirection != null && newReverseSortDirection) {
      for (int index = 0; index < metricsWithSelectionsSortedArray.length; index++) {
        if (metricsWithSelectionsSortedArray[index].isSorted()) {
          setSort(index, MET_NORMAL, newReverseSortDirection); // IPC
          //                    System.out.println("SORT (new): " + index + " " + MET_NORMAL + " " +
          // metricStates.isReverseDirectionSorting());
          break;
        }
      }
    }

    // Update sort column and sort direction (from clicking)
    if (sortColumnListChanged.size() > 0) {
      for (int[] sortColumnChanged : sortColumnListChanged) {
        setSort(
            sortColumnChanged[0],
            sortColumnChanged[1],
            metricStates.isReverseDirectionSorting()); // IPC
        //                System.out.println("SORT: " + sortColumnChanged[0] + " " +
        // sortColumnChanged[1] + " " + metricStates.isReverseDirectionSorting());
      }
    }

    // Reset and fill the cach
    metricStates.resetMetricListsByMType(); // IPC
  }

  public synchronized void setMetricOrderByDType(
      Object originalSource, int from, int to, int dtype) {
    int mtype = dtype2mtype(dtype);
    int functionsFrom = mapToFunctionsSortColumn(from, mtype);
    int functionsTo = mapToFunctionsSortColumn(to, mtype);
    setMetricOrder(originalSource, functionsFrom, functionsTo, MetricsSetting.MET_NORMAL);
  }

  private int mapToFunctionsSortColumn(int index, int mtype) {
    int ret = -1;

    if (mtype == MetricsSetting.MET_NORMAL) {
      return index;
    }
    AnMetric[] functionsMetricList = getMetricListByMType(MetricsSetting.MET_NORMAL);

    AnMetric[] metricList = getMetricListByMType(mtype);
    if (index < 0 || index >= metricList.length) {
      return -1;
    }

    String metricUserName = metricList[index].getUserName();
    for (int ind = 0; ind < functionsMetricList.length; ind++) {
      if (metricUserName.equals(functionsMetricList[ind].getUserName())) {
        ret = ind;
        break;
      }
    }

    if (ret == -1) {
      ret = getSortColumnByDType(AnDisplay.DSP_Functions);
    }
    return ret;
  }

  public synchronized void setSortMetricByDType(Object originalSource, int index, int dtype) {
    int mtype = dtype2mtype(dtype);
    setSortMetric(this, index, mtype);
  }

  public synchronized void dumpMetricsByDType(int dtype) {
    int mtype = dtype2mtype(dtype);
    getMetricStates().dumpMetricListsByMType(mtype);
  }

  public synchronized void dump() {
    getMetricStates().dump();
  }

  public static int dtype2mtype(final int dtype) {
    switch (dtype) {
      case AnDisplay.DSP_Callers:
        return MetricsSetting.MET_CALL;
      case AnDisplay.DSP_CallTree:
      case AnDisplay.DSP_CallFlame:
        return MetricsSetting.MET_CALL_AGR;
      case AnDisplay.DSP_SourceV2:
      case AnDisplay.DSP_DisassemblyV2:
        return MetricsSetting.MET_SRCDIS;
      case AnDisplay.DSP_Source:
      case AnDisplay.DSP_Disassembly:
        return MetricsSetting.MET_SRCDIS;
      case AnDisplay.DSP_IndexObject:
        return MetricsSetting.MET_INDEX;
      case AnDisplay.DSP_DataObjects:
      case AnDisplay.DSP_DataLayout:
      case AnDisplay.DSP_MemoryObject:
        return MetricsSetting.MET_DATA;
      case AnDisplay.DSP_IO:
      case AnDisplay.DSP_IOFileDescriptors:
      case AnDisplay.DSP_IOCallStacks:
        return MetricsSetting.MET_IO;
      case AnDisplay.DSP_Heap:
        return MetricsSetting.MET_HEAP;
      default:
        return MetricsSetting.MET_NORMAL;
    }
  }

  // Get metric label array for function list table title
  public synchronized MetricLabel[] getLabel(
      Object[][] data, Object[] total_max, int dtype, AnTable table) {
    final int mtype = dtype2mtype(dtype);
    final AnMetric[] mlist = getMetricListByMType(mtype);
    if (total_max == null) {
      total_max = AnObject.updateMaxValues(data, null);
    }
    return getLabel(data, total_max, mlist, dtype, table);
  }

  public synchronized MetricLabel[] getLabel(
      Object[][] data, Object[] total_max, AnMetric[] mlist, int dtype, AnTable table) {
    ImageIcon so_icon;
    AnObject max_obj = null;
    String name, unit;
    double pclock, ptotal;

    if ((data == null) || (total_max == null) || (mlist == null)) {
      return null;
    }

    // total and max are already sorted to show visible columns in order
    final Object[] total = (Object[]) total_max[0];
    Object[] maximum = (Object[]) total_max[1];
    final MetricLabel[] metricLabels = new MetricLabel[mlist.length];
    try {
      // This loop is not reliable - it can cause different exceptions
      for (int i = 0; i < mlist.length; i++) {
        AnMetric m = mlist[i];
        so_icon =
            (getMetricStates().isReverseDirectionSorting())
                ? AnUtility.smallArrowUpIcon
                : AnUtility.smallArrowDownIcon;
        //                so_icon =
        // (getMetricsSetting().getMetricStates().isReverseDirectionSorting()) ?
        // AnUtility.hollowArrowUpIcon : AnUtility.hollowArrowDownIcon;
        Object obj = maximum[i];
        if (obj == null) {
          max_obj = null;
          so_icon = AnUtility.blankIcon;
        } else if (obj instanceof String) {
          String s = (String) obj;
          max_obj = new AnString(s);
        } else {
          max_obj = (AnObject) obj;
        }
        name = m.getName();
        if (m.expr_spec != null) {
          name = name + "; (" + m.expr_spec + ")";
        }

        // XXX Need to rewrite this code.
        unit = null;
        pclock = -1.0;
        if (m.is_time_val()) {
          if (m.isVVisible()) {
            unit = m.getUnit();
          }
          if (m.isTVisible()) {
            int clock = m.getClock();
            pclock = (clock != 0) ? 1.e-6 / clock : 0.0;
          }
        } else if (m.isTVisible() || m.isVVisible()) {
          unit = m.getUnit();
        }
        //                if (GUITesting.getInstance().isRunningUnderGUITesting()) { // FIXUP: to be
        // removed when headers are done. Causing golden files diffs...
        //                    if (unit != null && unit.length() > 0) {
        //                        unit = "(" + unit + ")";
        //                    }
        //                }
        if (m.isPVisible()) {
          if (total[i] == null) {
            ptotal = 0.0;
          } else if (!(total[i] instanceof AnDouble) && !(total[i] instanceof AnLong)) {
            // XXXX Why total[i] is AnLong for Cycles in Source/Disasm
            // in compare mode and AnDouble in the other cases ?
            ptotal = 0.0;
          } else {
            double d = ((AnObject) total[i]).doubleValue();
            if (d == 0.0) {
              // Try to update the total value if necessary
              try {
                Object[][] tm =
                    (Object[][]) AnWindow.getInstance().getFunctionsView().getTotalMax();
                double t = ((AnObject) tm[0][i]).doubleValue();
                final int mtype = dtype2mtype(dtype);
                if (mtype == MetricsSetting.MET_SRCDIS) {
                  final AnMetric[] func_mlist =
                      getMetricListByMType(dtype2mtype(AnDisplay.DSP_Functions));
                  for (int func_mlist_num = 0;
                      func_mlist_num < func_mlist.length;
                      func_mlist_num++) {
                    int func_type = func_mlist[func_mlist_num].getType();
                    int srcdis_type = mlist[i].getType();
                    if (func_type == srcdis_type) {
                      t = ((AnObject) tm[0][func_mlist_num]).doubleValue();
                      break;
                    }
                  }
                }
                if (d < t) {
                  d = t;
                }
              } catch (Exception e) {
                // ignore it
              }
            }
            ptotal = (d != 0.0) ? 100.0 / d : 0.0;
          }
        } else {
          ptotal = -1.0;
        }
        metricLabels[i] = new MetricLabel(m, name, unit, pclock, ptotal, max_obj, so_icon);
      }
      //            for (int i = 0; i < mlist.length; i++) {
      //                if (mlist[i].isNameMetric()) {
      //                    name_col = i;
      //                    break;
      //                }
      //            }
    } catch (Exception internalErrorException) {
      // An internal error happened
      StackTraceElement[] se = internalErrorException.getStackTrace();
      StringBuilder s = new StringBuilder();
      s.append("*** internalErrorException ***\n");
      s.append("Thread: " + Thread.currentThread().getName() + "\n");
      for (int i = 0; i < se.length; i++) {
        s.append(se[i].toString() + "\n");
      }
      AnLog.log(s.toString());
      return null;
    }
    return metricLabels;
  }

  // Get index of sorting metric
  public synchronized String getSortColumnMetricDisplayName(final int mtype) {
    final AnMetric[] mlist = getMetricListByMType(mtype);
    if (mlist != null) {
      for (int i = 0; i < mlist.length; i++) {
        if (mlist[i].isSorted()) {
          return mlist[i].getName();
        }
      }
    }
    return null;
  }

  // Get index of sorting metric
  public synchronized AnMetric getSortColumnMetric(final int mtype) {
    final AnMetric[] mlist = getMetricListByMType(mtype);
    if (mlist != null) {
      for (int i = 0; i < mlist.length; i++) {
        if (mlist[i].isSorted()) {
          return mlist[i];
        }
      }
    }
    return null;
  }

  // Get index of sorting metric
  public synchronized void setSortColumnName(String name, final int mtype) {
    final AnMetric[] mlist = getMetricListByMType(mtype);
    for (int i = 0; i < mlist.length; i++) {
      if (mlist[i].getName().equals(name)) {
        setSortMetricByDType(this, i, mtype);
      }
    }
  }

  public static Object[] getRefMetricsV2() {
    synchronized (IPC.lock) {
      final IPC ipc = AnWindow.getInstance().IPC();
      ipc.send("getRefMetricsV2");
      return (Object[]) ipc.recvObject();
    }
  }

  private static void sendMetricListV2(final int mtype, final AnMetric[] mlist) {
    final IPC ipc = AnWindow.getInstance().IPC();
    final int msize = mlist.length;
    final int[] type = new int[msize];
    final int[] subtype = new int[msize];
    final boolean[] sort = new boolean[msize];
    final int[] vis = new int[msize];
    final String[] cmd = new String[msize];
    final String[] expr_spec = new String[msize];
    final String[] legend = new String[msize];
    ipc.send(mtype);
    for (int i = 0; i < msize; i++) {
      AnMetric m = mlist[i];
      type[i] = m.getType();
      subtype[i] = m.getSubType();
      sort[i] = m.isSorted();
      vis[i] = m.get_vis_bits();
      cmd[i] = m.getComd();
      expr_spec[i] = m.expr_spec;
      legend[i] = m.legend;
    }
    ipc.send(type);
    ipc.send(subtype);
    ipc.send(sort);
    ipc.send(vis);
    ipc.send(cmd);
    ipc.send(expr_spec);
    ipc.send(legend);
  }

  public static void setCurMetricsV2IPC(
      final int mtype, final AnMetric[] mlist, final int cmp_mode) {
    synchronized (IPC.lock) {
      final IPC ipc = AnWindow.getInstance().IPC();
      ipc.send("setCurMetricsV2");
      ipc.send(0);
      ipc.send(cmp_mode);
      sendMetricListV2(mtype, mlist);
      ipc.recvVoid();
    }
  }

  public static Object[] getCurMetricsV2IPC(final int mtype) {
    synchronized (IPC.lock) {
      final IPC ipc = AnWindow.getInstance().IPC();
      ipc.send("getCurMetricsV2");
      ipc.send(0);
      ipc.send(mtype);
      return (Object[]) ipc.recvObject();
    }
  }

  /**
   * Send request to get Current Metrics. Non-blocking IPC call. Caller should call
   * ipcResult.getObjects() to get the result @Parameters: mtype - metric type
   *
   * @return IPCResult
   */
  public static IPCResult SendRequest_getCurMetricsV2IPC(final int mtype) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getCurMetricsV2");
    ipcHandle.append(0);
    ipcHandle.append(mtype);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }

  private static void setSort(final int sort_ind, final int mtype, final boolean direction) {
    synchronized (IPC.lock) {
      AnWindow m_window = AnWindow.getInstance();
      m_window.IPC().send("setSort");
      m_window.IPC().send(0);
      m_window.IPC().send(sort_ind);
      m_window.IPC().send(mtype);
      m_window.IPC().send(direction);
      m_window.IPC().recvString(); // synchronize
    }
  }

  public synchronized void hotAction() {
    MetricStates metricStatesCopy = metricStates.copy();
    List<MetricState> list = metricStatesCopy.getMetricStateList();
    for (MetricState metricState : list) {
      metricState.getSelection().setSelected(metricState.getAnMetric().isHot());
      // Make sure something is selected
      if (metricState.getSelection().isSelected()) {
        if (!metricState.getSelection().getAttributes().anySelections()) {
          metricState.getSelection().setAttributes(metricState.getFactory().getAttributes().copy());
        }
      }
    }
    set(this, metricStatesCopy);
  }

  public synchronized void resetAction() {
    MetricStates metricStatesCopy = metricStates.copy();
    List<MetricState> list = metricStatesCopy.getMetricStateList();
    for (MetricState metricState : list) {
      metricState.getSelection().setSelected(metricState.getFactory().isSelected());
      metricState.getSelection().setAttributes(metricState.getFactory().getAttributes().copy());
    }
    set(this, metricStatesCopy);
  }

  public synchronized void clearAction() {
    MetricStates metricStatesCopy = metricStates.copy();
    List<MetricState> list = metricStatesCopy.getMetricStateList();
    for (MetricState metricState : list) {
      if (!metricState.getAnMetric().isNameMetric()) {
        metricState.getSelection().setSelected(false);
      }
    }
    set(this, metricStatesCopy);
  }

  public synchronized JPopupMenu addMetricsPopupMenus(
      JPopupMenu popup, final AnMetric anMetric, final AnTable anTable, JMenu sortByMenu) {
    //        System.out.println(anTable.getType());
    // Metric Options
    JLabel metricNameLabel = new JLabel();
    metricNameLabel.setText(" " + AnLocale.getString("Options for " + anMetric.getUserName()));
    metricNameLabel.setFont(metricNameLabel.getFont().deriveFont(Font.BOLD));
    metricNameLabel.setForeground(AnEnvironment.METRIC_MENU_SECTION_COLOR);
    popup.add(metricNameLabel);
    for (Component component : getMetricOptionsItems(anMetric, anTable, sortByMenu)) {
      popup.add(component);
    }
    // Other
    popup.add(new JSeparator(SwingConstants.HORIZONTAL));
    JLabel otherLabel = new JLabel(" " + AnLocale.getString("Other Metric Options"));
    otherLabel.setFont(otherLabel.getFont().deriveFont(Font.BOLD));
    otherLabel.setForeground(AnEnvironment.METRIC_MENU_SECTION_COLOR);
    popup.add(otherLabel);
    for (Component component : getOtherMetricItems(anTable)) {
      popup.add(component);
    }

    return popup;
  }

  private List<Component> getMetricOptionsItems(
      final AnMetric anMetric, final AnTable anTable, JMenu sortByMenu) {
    List<Component> componentList = new ArrayList<Component>();

    final MetricState metricState = getMetricStates().findMetricStateByName(anMetric.getComd());
    if (metricState != null) {
      if (!anMetric.isNameMetric() && !anMetric.isStatic()) {
        final MetricAttributes capableAttributes = metricState.getCapable();
        final MetricSelection metricSelection = metricState.getSelection();
        final MetricSelection factoryMetricSelection = metricState.getFactory();
        final MetricAttributes selectedAttributes = metricSelection.getAttributes();
        final JCheckBoxMenuItem exclusiveCheckBox =
            new IndentCheckBox(AnLocale.getString("Exclusive"), 1);
        final JCheckBoxMenuItem eTimeCheckBox = new IndentCheckBox(AnLocale.getString("Time"), 2);
        final JCheckBoxMenuItem eValueCheckBox = new IndentCheckBox(AnLocale.getString("Value"), 2);
        final JCheckBoxMenuItem ePercentCheckBox =
            new IndentCheckBox(AnLocale.getString("Percent"), 2);
        final JCheckBoxMenuItem inclusiveCheckBox =
            new IndentCheckBox(AnLocale.getString("Inclusive"), 1);
        final JCheckBoxMenuItem iTimeCheckBox = new IndentCheckBox(AnLocale.getString("Time"), 2);
        final JCheckBoxMenuItem iValueCheckBox = new IndentCheckBox(AnLocale.getString("Value"), 2);
        final JCheckBoxMenuItem iPercentCheckBox =
            new IndentCheckBox(AnLocale.getString("Percent"), 2);
        final JCheckBoxMenuItem timeCheckBox = new IndentCheckBox(AnLocale.getString("Time"), 1);
        final JCheckBoxMenuItem valueCheckBox = new IndentCheckBox(AnLocale.getString("Value"), 1);
        final JCheckBoxMenuItem percentCheckBox =
            new IndentCheckBox(AnLocale.getString("Percent"), 1);

        if (anTable.getAnParent().showOnlyValuesInTables()
            || anTable.getAnParent().showOnlyIncludedInTables()
            || anTable.getAnParent().showOnlyAttributedInTables()) {
          timeCheckBox.setSelected(selectedAttributes.isVTime());
          timeCheckBox.setEnabled(capableAttributes.isVTime());
          timeCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setVTime(timeCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });
          valueCheckBox.setSelected(selectedAttributes.isVValue());
          valueCheckBox.setEnabled(capableAttributes.isVValue());
          valueCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setVValue(valueCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });
          percentCheckBox.setSelected(selectedAttributes.isVPercent());
          percentCheckBox.setEnabled(capableAttributes.isVPercent());
          percentCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setVPercent(percentCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          componentList.add(timeCheckBox);
          componentList.add(valueCheckBox);
          componentList.add(percentCheckBox);
        } else {
          exclusiveCheckBox.setSelected(selectedAttributes.anyExclusiveSelections());
          exclusiveCheckBox.setEnabled(capableAttributes.anyExclusiveSelections());
          exclusiveCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  if (exclusiveCheckBox.isSelected()) {
                    // Try factory first
                    copySelectedAttributes.setETime(
                        factoryMetricSelection.getAttributes().isETime());
                    copySelectedAttributes.setEValue(
                        factoryMetricSelection.getAttributes().isEValue());
                    copySelectedAttributes.setEPercent(
                        factoryMetricSelection.getAttributes().isEPercent());
                    if (!copySelectedAttributes.anyExclusiveSelections()) {
                      // Set first capable
                      if (capableAttributes.isETime()) {
                        copySelectedAttributes.setETime(true);
                      } else if (capableAttributes.isEValue()) {
                        copySelectedAttributes.setEValue(true);
                      } else if (capableAttributes.isEPercent()) {
                        copySelectedAttributes.setEPercent(true);
                      }
                    }
                  } else {
                    // Clear all...
                    copySelectedAttributes.setETime(false);
                    copySelectedAttributes.setEValue(false);
                    copySelectedAttributes.setEPercent(false);
                  }
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          eTimeCheckBox.setSelected(selectedAttributes.isETime());
          eTimeCheckBox.setEnabled(capableAttributes.isETime());
          eTimeCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setETime(eTimeCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          eValueCheckBox.setSelected(selectedAttributes.isEValue());
          eValueCheckBox.setEnabled(capableAttributes.isEValue());
          eValueCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setEValue(eValueCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          ePercentCheckBox.setSelected(selectedAttributes.isEPercent());
          ePercentCheckBox.setEnabled(capableAttributes.isEPercent());
          ePercentCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setEPercent(ePercentCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          inclusiveCheckBox.setSelected(selectedAttributes.anyInclusiveSelections());
          inclusiveCheckBox.setEnabled(capableAttributes.anyInclusiveSelections());
          inclusiveCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  if (inclusiveCheckBox.isSelected()) {
                    // Try factory first
                    copySelectedAttributes.setITime(
                        factoryMetricSelection.getAttributes().isITime());
                    copySelectedAttributes.setIValue(
                        factoryMetricSelection.getAttributes().isIValue());
                    copySelectedAttributes.setIPercent(
                        factoryMetricSelection.getAttributes().isIPercent());
                    if (!copySelectedAttributes.anyInclusiveSelections()) {
                      // Set first capable
                      if (capableAttributes.isITime()) {
                        copySelectedAttributes.setITime(true);
                      } else if (capableAttributes.isIValue()) {
                        copySelectedAttributes.setIValue(true);
                      } else if (capableAttributes.isIPercent()) {
                        copySelectedAttributes.setIPercent(true);
                      }
                    }
                  } else {
                    // Clear all...
                    copySelectedAttributes.setITime(false);
                    copySelectedAttributes.setIValue(false);
                    copySelectedAttributes.setIPercent(false);
                  }
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          iTimeCheckBox.setSelected(selectedAttributes.isITime());
          iTimeCheckBox.setEnabled(capableAttributes.isITime());
          iTimeCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setITime(iTimeCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          iValueCheckBox.setSelected(selectedAttributes.isIValue());
          iValueCheckBox.setEnabled(capableAttributes.isIValue());
          iValueCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setIValue(iValueCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          iPercentCheckBox.setSelected(selectedAttributes.isIPercent());
          iPercentCheckBox.setEnabled(capableAttributes.isIPercent());
          iPercentCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  MetricAttributes copySelectedAttributes = selectedAttributes.copy();
                  copySelectedAttributes.setIPercent(iPercentCheckBox.isSelected());
                  set(this, anMetric.getComd(), copySelectedAttributes);
                }
              });

          componentList.add(exclusiveCheckBox);
          componentList.add(eTimeCheckBox);
          componentList.add(eValueCheckBox);
          componentList.add(ePercentCheckBox);
          componentList.add(inclusiveCheckBox);
          componentList.add(iTimeCheckBox);
          componentList.add(iValueCheckBox);
          componentList.add(iPercentCheckBox);
        }

        AnMetric auxMetric = null;
        String aux = anMetric.getAux();
        if (aux != null && anMetric.getComd().equals(aux)) {
          for (AnMetric am : availableAnMetrics) {
            // Look for metrcis with aux equal comd
            if (am.getAux() != null
                && am.getAux().equals(anMetric.getComd())
                && !am.getComd().equals(am.getAux())) {
              auxMetric = am;
              break;
            }
          }
        } else if (aux != null && !anMetric.getComd().equals(aux)) {
          for (AnMetric am : availableAnMetrics) {
            // Look for metrcis with aux equal comd
            if (am.getComd().equals(aux)) {
              auxMetric = am;
              break;
            }
          }
        }
        if (auxMetric != null) {
          MetricState auxMetricState = getMetricStates().findMetricStateByName(auxMetric.getComd());
          if (auxMetricState != null) {
            JLabel filler = new JLabel();
            Dimension dim = new Dimension(5, 5);
            filler.setPreferredSize(dim);
            componentList.add(filler);
            final JCheckBoxMenuItem auxCheckBox = new IndentCheckBox(auxMetric.getUserName(), 1);
            auxCheckBox.setSelected(auxMetricState.getSelection().isSelected());
            componentList.add(auxCheckBox);
            final AnMetric finalAuxMetric = auxMetric;
            auxCheckBox.addActionListener(
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    set(this, finalAuxMetric.getComd(), auxCheckBox.isSelected());
                  }
                });
          }
        }

        JMenuItem removeMemyItem = new JMenuItem(AnLocale.getString("Remove This Metric"));
        removeMemyItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                set(this, metricState.getAnMetric().getComd(), false);
              }
            });
        componentList.add(removeMemyItem);
      }

      if (sortByMenu != null) {
        componentList.add(sortByMenu);
      }

      List<AnMetric> list =
          getMetricStates().getSelectedMetricsByMType(dtype2mtype(anTable.getType()));
      JMenu reorderMenu = new JMenu(AnLocale.getString("Move This Metric To"));
      // Before...
      JMenu beforeMenu = new JMenu(AnLocale.getString("Before Metric"));
      for (AnMetric s : list) {
        final AnMetric refMetric = s;
        JMenuItem menuItem = new JMenuItem(refMetric.getUserName());
        if (s.getComd().equals(anMetric.getComd())) {
          menuItem.setEnabled(false);
        }
        menuItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                setMetricOrder(this, anMetric.getComd(), refMetric.getComd(), true, MET_NORMAL);
              }
            });
        beforeMenu.add(menuItem);
      }
      reorderMenu.add(beforeMenu);
      // After...
      JMenu toAfterMenu = new JMenu(AnLocale.getString("After Metric"));
      for (AnMetric s : list) {
        final AnMetric refMetric = s;
        JMenuItem menuItem = new JMenuItem(refMetric.getUserName());
        if (s.getComd().equals(anMetric.getComd())) {
          menuItem.setEnabled(false);
        }
        menuItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                setMetricOrder(this, anMetric.getComd(), refMetric.getComd(), false, MET_NORMAL);
              }
            });
        toAfterMenu.add(menuItem);
      }
      reorderMenu.add(toAfterMenu);
      componentList.add(reorderMenu);
    }

    return componentList;
  }

  public synchronized List<Component> getOtherMetricItems(AnTable anTable) {
    List<Component> list = new ArrayList<Component>();

    // Table format
    JMenu formatMenuItem = new JMenu(AnLocale.getString("Format"));
    final JCheckBoxMenuItem wrapCheckBoxMenuItem =
        new JCheckBoxMenuItem(AnLocale.getString("Wrap Long Metric Names in Table Headers"));
    wrapCheckBoxMenuItem.setSelected(
        AnWindow.getInstance().getSettings().getTableSettings().wrapMetricNames());
    wrapCheckBoxMenuItem.setEnabled(anTable.wrapMetricNames());
    wrapCheckBoxMenuItem.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance()
                .getSettings()
                .getTableSettings()
                .setWrapMetricNames(MetricsSetting.this, wrapCheckBoxMenuItem.isSelected());
          }
        });
    formatMenuItem.add(wrapCheckBoxMenuItem);
    list.add(formatMenuItem);

    // Compare mode
    CompareModeSetting.CompareMode mode =
        AnWindow.getInstance().getSettings().getCompareModeSetting().get();
    JMenu compareMenuItem = new JMenu(AnLocale.getString("Compare Mode"));
    if (mode != CompareModeSetting.CompareMode.CMP_DISABLE) {
      JRadioButtonMenuItem absoluteRadioButtonMenuItem =
          new JRadioButtonMenuItem(AnLocale.getString("Absolute"));
      JRadioButtonMenuItem deltaRadioButtonMenuItem =
          new JRadioButtonMenuItem(AnLocale.getString("Delta"));
      JRadioButtonMenuItem ratioRadioButtonMenuItem =
          new JRadioButtonMenuItem(AnLocale.getString("Ratio"));
      absoluteRadioButtonMenuItem.setSelected(mode == CompareModeSetting.CompareMode.CMP_ENABLE);
      deltaRadioButtonMenuItem.setSelected(mode == CompareModeSetting.CompareMode.CMP_DELTA);
      ratioRadioButtonMenuItem.setSelected(mode == CompareModeSetting.CompareMode.CMP_RATIO);
      absoluteRadioButtonMenuItem.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              AnWindow.getInstance()
                  .getSettings()
                  .getCompareModeSetting()
                  .set(this, CompareModeSetting.CompareMode.CMP_ENABLE);
            }
          });
      deltaRadioButtonMenuItem.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              AnWindow.getInstance()
                  .getSettings()
                  .getCompareModeSetting()
                  .set(this, CompareModeSetting.CompareMode.CMP_DELTA);
            }
          });
      ratioRadioButtonMenuItem.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              AnWindow.getInstance()
                  .getSettings()
                  .getCompareModeSetting()
                  .set(this, CompareModeSetting.CompareMode.CMP_RATIO);
            }
          });
      compareMenuItem.add(absoluteRadioButtonMenuItem);
      compareMenuItem.add(deltaRadioButtonMenuItem);
      compareMenuItem.add(ratioRadioButtonMenuItem);
      formatMenuItem.add(compareMenuItem);
    }

    // More metrics
    JMenu moreMetricsMenuItem = new JMenu(AnLocale.getString("More Metrics"));
    addMetricMenuSelector(moreMetricsMenuItem, anTable.getType(), true);
    list.add(moreMetricsMenuItem);

    // Settings
    for (JComponent component : createMetricSettingsSelector()) {
      list.add(component);
    }

    return list;
  }

  public synchronized JPopupMenu addMetricsPopupSelector(
      JPopupMenu popup, int dtype, boolean addMetricActions) {
    List<JComponent> list = createMetricsSelector(dtype, addMetricActions);
    for (JComponent component : list) {
      popup.add(component);
    }
    //        popup.add(new JSeparator(SwingConstants.HORIZONTAL));
    //        for (JComponent component : createMetricSettingsSelector()) {
    //            popup.add(component);
    //        }
    return popup;
  }

  public synchronized JMenu addMetricMenuSelector(JMenu menu, int dtype, boolean addMetricActions) {
    for (JComponent component : createMetricsSelector(dtype, addMetricActions)) {
      menu.add(component);
    }
    return menu;
  }

  public synchronized List<JComponent> createMetricSettingsSelector() {
    List<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(AnWindow.getInstance().getMetricsSettingsAction().getMenuItem());
    return componentList;
  }

  private List<JComponent> createMetricsSelector(int dtype, boolean addMetricActions) {
    List<JComponent> componentList = new ArrayList<JComponent>();
    List<MetricState> metricStateList =
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .getMetricStates()
            .getMetricStateList();
    boolean anyHot = false;
    String listName = null;
    int numberAdded = 0;
    int metricListID =
        AnWindow.getInstance().getSettings().getViewsSetting().getViewMetricListIdMapping(dtype);
    //        System.out.println("------------------------- " + dtype + " " + metricListID);
    for (MetricState metricState : metricStateList) {
      if (metricState.getAnMetric().isNameMetric()) {
        continue;
      }
      if (metricState.getAnMetric().isStatic() && dtype2mtype(dtype) != MET_NORMAL) {
        continue;
      }
      if (metricListID != MetricNode.ALL_LIST_ID && metricListID != metricState.getListId()) {
        continue;
      }
      String ln = metricState.getListName();
      if (ln != null && !ln.equals(listName)) {
        //                System.out.println(ln);
        if (listName != null) {
          JLabel filler = new JLabel(" ");
          filler.setPreferredSize(new Dimension(4, 4));
          componentList.add(filler);
          //                    JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
          //
          // separator.setForeground(AnEnvironment.METRIC_SELCTOR_MENU_SEPARATOR_COLOR);
          //                    separator.setBorder(null);
          //                    separator.setPreferredSize(new
          // Dimension(separator.getPreferredSize().width, 1));
          //                    componentList.add(separator);
        }
        JLabel label = new JLabel("     " + ln);
        label = new JLabel(" " + ln);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(AnEnvironment.METRIC_MENU_SECTION_COLOR);
        componentList.add(label);
        listName = ln;
      }
      MetricCheckBox checkBox =
          new MetricCheckBox(
              metricState.getAnMetric(),
              metricState.getSelection().isSelected(),
              metricState.getAnMetric().isHot() ? AnUtility.hot_icon : null);
      componentList.add(checkBox);
      if (metricState.getAnMetric().isHot()) {
        anyHot = true;
      }
      numberAdded++;
    }
    if (numberAdded > 0) {
      if (addMetricActions) {
        componentList.add(new JSeparator(SwingConstants.HORIZONTAL));

        JMenuItem settingsItem;
        settingsItem =
            new MetricMenuItem(AnLocale.getString("Select Hot Metrics"), AnUtility.hot_icon);
        settingsItem.setEnabled(anyHot);
        settingsItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                AnWindow.getInstance().getSettings().getMetricsSetting().hotAction();
              }
            });
        componentList.add(settingsItem);

        settingsItem = new JMenuItem(AnLocale.getString("Reset All Metrics"));
        settingsItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                AnWindow.getInstance().getSettings().getMetricsSetting().resetAction();
              }
            });
        componentList.add(settingsItem);

        settingsItem = new JMenuItem(AnLocale.getString("Remove All Metrics"));
        settingsItem.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                AnWindow.getInstance().getSettings().getMetricsSetting().clearAction();
              }
            });
        componentList.add(settingsItem);
      }
    } else {
      JLabel label = new JLabel("  " + AnLocale.getString("No metrics available for this view"));
      //            label.setIcon(AnUtility.blankIcon);
      //            label.setFont(label.getFont().deriveFont(Font.BOLD));
      //            label.setForeground(AnEnvironment.METRIC_SELCTOR_MENU_GROUP_COLOR);
      componentList.add(label);
    }

    return componentList;
  }

  public synchronized List<Component> createTableColumnMenuItems(AnTable table) {
    int dtype = table.getType();
    int mtype = dtype2mtype(dtype);
    List<AnMetric> anMetricList = metricStates.getAnMetricsByMTypeTrimmed(mtype);
    List<Component> componentList = new ArrayList<Component>();
    for (AnMetric anMetric : anMetricList) {
      JMenu metricMenu = new JMenu(anMetric.getUserName());
      JPopupMenu popupMenu = new JPopupMenu();
      addMetricsPopupMenus(popupMenu, anMetric, table, null);
      List<Component> metricOptionList = getMetricOptionsItems(anMetric, table, null);
      for (Component comp : metricOptionList) {
        metricMenu.add(comp);
      }
      componentList.add(metricMenu);
    }
    return componentList;
  }

  private static class IndentCheckBox extends JCheckBoxMenuItem {

    public IndentCheckBox(String text, int indent) {
      this.setText(text);
      setBorder(BorderFactory.createMatteBorder(1, 1 + 16 * indent, 1, 1, getBackground()));
    }
  }

  public static class MetricCheckBox extends JCheckBoxMenuItem {

    private ImageIcon icon;
    private AnMetric anMetric;

    public MetricCheckBox(final AnMetric anMetric, boolean selected, ImageIcon icon) {
      this.setText(anMetric.getUserName());
      this.anMetric = anMetric;
      this.icon = icon;
      setBorder(BorderFactory.createMatteBorder(1, 16, 1, 1, getBackground()));

      setSelected(selected);
      addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              AnWindow.getInstance()
                  .getSettings()
                  .getMetricsSetting()
                  .set(MetricCheckBox.this, anMetric.getComd(), isSelected());
            }
          });
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g); // To change body of generated methods, choose Tools | Templates.
      if (icon != null) {
        g.drawImage(icon.getImage(), 4, 3, this);
      }
    }

    /**
     * @return the anMetric
     */
    public AnMetric getAnMetric() {
      return anMetric;
    }
  }

  private static class MetricMenuItem extends JMenuItem {

    private ImageIcon icon;

    public MetricMenuItem(String text, ImageIcon icon) {
      this.setText(text);
      this.icon = icon;
      setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, getBackground()));
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g); // To change body of generated methods, choose Tools | Templates.
      if (icon != null) {
        g.drawImage(icon.getImage(), 4, 4, this);
      }
    }
  }
}
