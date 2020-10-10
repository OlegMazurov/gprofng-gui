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

package org.gprofng.mpmt.settings;

import static org.gprofng.mpmt.settings.Settings.getMetricList;

import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.metrics.MetricNode;
import java.util.ArrayList;
import java.util.List;

public class MetricStates {

  private List<MetricState> metricStateList; // Current selection. Capable and factory settings.
  private MMetric[] metricListsByMType; // Selected and ordered
  private boolean reverseSortDirection = false;
  private MetricNode metricsRootNode;

  private List<MetricType> newMetricOrder = null;
  private MetricType newSortMetric = null;
  private Boolean newReverseSortDirection = null;

  public MetricStates() {}

  public MetricStates(boolean reverseSortDirection) {
    this.reverseSortDirection = reverseSortDirection;
  }

  public MetricState findMetricStateByName(String name) {
    for (MetricState metricState : metricStateList) {
      if (metricState.getAnMetric().getComd().equals(name)) {
        return metricState;
      }
    }
    //        System.err.println("METRICERROR: MetricStates.findMetricStateByName Can't find " +
    // name);
    return null;
  }

  public void setSelection(String name, boolean selected) {
    MetricState metricState = findMetricStateByName(name);
    if (metricState != null) {
      metricState.getSelection().setSelected(selected);
      // make sure something is selected
      if (!metricState.getSelection().getAttributes().anySelections()) {
        metricState.getSelection().setAttributes(metricState.getFactory().getAttributes().copy());
      }
    }
  }

  public void setSelection(List<MetricNameSelection> metricNameSelectionList) {
    if (metricNameSelectionList != null) {
      for (MetricNameSelection metricNameSelection : metricNameSelectionList) {
        setSelection(metricNameSelection);
      }
    }
  }

  public void setSelection(MetricNameSelection metricNameSelection) {
    MetricState metricState = findMetricStateByName(metricNameSelection.getName());
    if (metricState != null) {
      metricState.getSelection().set(metricNameSelection.getMetricSelection());
    }
  }

  public boolean isSelected(String metricName) {
    MetricState metricState = findMetricStateByName(metricName);
    if (metricState != null) {
      return metricState.getSelection().isSelected();
    }
    return false;
  }

  public void updateStates(MetricNode metricsRootNode, List<MetricState> metricStateList) {
    this.metricsRootNode = metricsRootNode;
    this.metricStateList = metricStateList;
  }

  public List<MetricState> getMetricStateList() {
    return metricStateList;
  }

  public void resetMetricListsByMType() {
    //        System.out.println("resetMetricListsByMType");
    metricListsByMType = new MMetric[MetricsSetting.MET_LAST];
    // Use "async" IPC calls to minimize the "high latency" effect
    IPCResult[] ipcrs = new IPCResult[MetricsSetting.MET_LAST + 1];
    for (int mtype = 0; mtype < MetricsSetting.MET_LAST; mtype++) {
      ipcrs[mtype] = MetricsSetting.SendRequest_getCurMetricsV2IPC(mtype); // ASYNC IPC
    }
    for (int mtype = 0; mtype < MetricsSetting.MET_LAST; mtype++) {
      Object[] ref_data = ipcrs[mtype].getObjects(); // Get IPC result
      if (ref_data != null && ref_data.length > 0) {
        metricListsByMType[mtype] = new MMetric(getMetricList(ref_data), reverseSortDirection);
      } else {
        metricListsByMType[mtype] = null;
      }
    }
    // Old code used "sync" IPC calls
    //        for (int mtype = 0; mtype < MetricsSetting.MET_LAST; mtype++) {
    //            Object[] ref_data = MetricsSetting.getCurMetricsV2IPC(mtype); // IPC
    //            if (ref_data != null && ref_data.length > 0) {
    //                metricListsByMType[mtype] = new MMetric(getMetricList(ref_data),
    // reverseSortDirection);
    //            } else {
    //                metricListsByMType[mtype] = null;
    //            }
    //        }
  }

  /**
   * @return Returns a deep copy
   */
  protected MetricStates copy() {
    MetricStates copy = new MetricStates(reverseSortDirection);
    MMetric[] copyMetricListsByMType = new MMetric[metricListsByMType.length];
    int index = 0;
    for (MMetric mMetric : metricListsByMType) {
      copyMetricListsByMType[index++] = mMetric.copy();
    }

    copy.setMetricListsByMType(copyMetricListsByMType);
    List<MetricState> copyMetricStateList = new ArrayList<MetricState>();
    for (MetricState metricState : metricStateList) {
      copyMetricStateList.add(metricState.copy());
    }
    copy.updateStates(metricsRootNode, copyMetricStateList);
    return copy;
  }

  /**
   * @return list of changed metrics. Assume old and new lists contains same metrics in same order.
   */
  public static List<MetricNameSelection> getChangedMetrics(
      MetricStates oldMetricsStates, MetricStates newMetricsStates) {
    if (oldMetricsStates == null) {
      // Just copy the 'new' list
      List<MetricNameSelection> changes = new ArrayList<MetricNameSelection>();
      for (MetricState metricState : newMetricsStates.getMetricStateList()) {
        changes.add(
            new MetricNameSelection(
                metricState.getAnMetric().getComd(),
                metricState.getSelection().copy())); // FIXUP: copy necessary?
      }
      return changes;
    }

    List<MetricState> oldMetricStateList = oldMetricsStates.getMetricStateList();
    List<MetricState> newMetricStateList = newMetricsStates.getMetricStateList();

    boolean okToCheck = true;
    if (oldMetricStateList.size() != newMetricStateList.size()) {
      okToCheck = false;
    }
    if (okToCheck) {
      for (int i = 0; i < oldMetricStateList.size(); i++) {
        AnMetric oldAnMetric = oldMetricStateList.get(i).getAnMetric();
        AnMetric newAnMetric = newMetricStateList.get(i).getAnMetric();
        if (oldAnMetric != newAnMetric) {
          okToCheck = false;
        }
      }
    }
    if (!okToCheck) {
      System.err.println(
          "METRICERROR: MetricStates.getChangedMetrics: old and new lists are different");
    }

    List<MetricNameSelection> changes = new ArrayList<MetricNameSelection>();
    for (int i = 0; i < oldMetricStateList.size(); i++) {
      MetricState oldMetricState = oldMetricStateList.get(i);
      MetricState newMetricState = newMetricStateList.get(i);
      if (!oldMetricState.getSelection().equals(newMetricState.getSelection())) {
        changes.add(
            new MetricNameSelection(
                newMetricState.getAnMetric().getComd(),
                newMetricState.getSelection().copy())); // FIXUP: copy necessary?
      }
    }
    return changes;
  }

  public List<int[]> getChangedMetricSortColumns() {
    List<int[]> list = new ArrayList<int[]>();

    // Check change in sort column index
    for (int mtype = 0; mtype < MetricsSetting.MET_LAST; mtype++) {
      MMetric mMetric = metricListsByMType[mtype];
      // Sort column changed?
      AnMetric[] anMetricList = mMetric.getmMetricList();
      int oldSortIndex = -1;
      int index = 0;
      for (AnMetric anMetric : anMetricList) {
        if (anMetric.isSorted()) {
          oldSortIndex = index;
          break;
        }
        index++;
      }
      int newSortIndex = metricListsByMType[mtype].getSortIndex();
      if (oldSortIndex != newSortIndex) {
        list.add(new int[] {mMetric.getSortIndex(), mtype});
      }
    }

    // Check change to sort direction
    for (int mtype = 0; mtype < MetricsSetting.MET_LAST; mtype++) {
      MMetric mMetric = metricListsByMType[mtype];
      boolean newReverseSortDirection = mMetric.isReverseSortDirection();
      if (reverseSortDirection != newReverseSortDirection) {
        list.add(new int[] {mMetric.getSortIndex(), mtype});
      }
    }

    return list;
  }

  public void transferSortChangesToMetricStates(List<int[]> list) {
    for (int[] l : list) {
      int index = l[0];
      int mtype = l[1];
      MMetric mMetric = metricListsByMType[mtype];
      for (int i = 0; i < mMetric.getmMetricList().length; i++) {
        mMetric.getmMetricList()[i].setSorted(false);
      }
      mMetric.getmMetricList()[index].setSorted(true);

      reverseSortDirection = mMetric.isReverseSortDirection();
    }
  }

  public void updateSortMetric(int index, int mtype) {
    if (mtype < 0 || mtype >= metricListsByMType.length) {
      return;
    }
    MMetric mMetric = metricListsByMType[mtype];
    int currentIndex = mMetric.getSortIndex();
    if (index != currentIndex) {
      mMetric.setSortIndex(index);
      mMetric.setReverseSortDirection(false);
    } else {
      mMetric.setReverseSortDirection(!mMetric.isReverseSortDirection());
    }
  }

  public void updateMetricOrder(int from, int to, int mtype) {
    if (from == to) {
      return;
    }
    if (mtype < 0 || mtype >= metricListsByMType.length) {
      return;
    }
    MMetric mMetric = metricListsByMType[mtype];
    AnMetric[] mlist = mMetric.getmMetricList();
    if (from < 0 || from >= mlist.length || to < 0 || to >= mlist.length) {
      return;
    }
    AnMetric m = mlist[from];
    mlist[from] = mlist[to];
    mlist[to] = m;

    // update sort index
    if (mMetric.getSortIndex() == from) {
      mMetric.setSortIndex(to);
    } else if (mMetric.getSortIndex() == to) {
      mMetric.setSortIndex(from);
    }
    //        updateMetricOrder(mlist[from].getComd(), mlist[to].getComd(), from > to, mtype);
  }

  public void updateMetricOrder(
      String metricName, String metricRefName, boolean before, int mtype) {
    if (mtype < 0 || mtype >= metricListsByMType.length) {
      return;
    }
    MMetric mMetric = metricListsByMType[mtype];
    AnMetric sortMetric = mMetric.getSortMetric();
    AnMetric[] mlist = mMetric.getmMetricList();
    List<AnMetric> metricsToMove = new ArrayList<AnMetric>();
    List<AnMetric> metricsNotToMove = new ArrayList<AnMetric>();
    for (int i = 0; i < mlist.length; i++) {
      if (mlist[i].getComd().equals(metricName)) {
        metricsToMove.add(mlist[i]);
      } else {
        metricsNotToMove.add(mlist[i]);
      }
    }
    int insertionIndex = -1;
    for (int i = 0; i < metricsNotToMove.size(); i++) {
      if (metricsNotToMove.get(i).getComd().equals(metricRefName)) {
        insertionIndex = i;
        if (before) {
          break;
        }
      }
    }
    if (insertionIndex != -1) {
      List<AnMetric> newList = new ArrayList<AnMetric>();
      if (before) {
        for (int i = 0; i <= insertionIndex - 1; i++) {
          newList.add(metricsNotToMove.get(i));
        }
        newList.addAll(metricsToMove);
        for (int i = insertionIndex; i < metricsNotToMove.size(); i++) {
          newList.add(metricsNotToMove.get(i));
        }
      } else {
        for (int i = 0; i <= insertionIndex; i++) {
          newList.add(metricsNotToMove.get(i));
        }
        newList.addAll(metricsToMove);
        for (int i = insertionIndex + 1; i < metricsNotToMove.size(); i++) {
          newList.add(metricsNotToMove.get(i));
        }
      }
      if (newList.size() != mlist.length) {
        System.err.println("ERROR: MetricStates.UpdateMetricOrder newList.size() != mlist.length");
      } else {
        newList.toArray(mlist);
      }
      int newSortIndex = mMetric.getIndex(sortMetric);
      mMetric.setSortIndex(newSortIndex);

      //            // update sort index
      //            if (mMetric.getSortIndex() == from) {
      //                mMetric.setSortIndex(to);
      //            } else if (mMetric.getSortIndex() == to) {
      //                mMetric.setSortIndex(from);
      //            }
    }
  }

  public List<AnMetric> getSelectedMetricsByMType(int mType) {
    List<AnMetric> list = new ArrayList<AnMetric>();
    String lastName = null;

    MMetric mList = metricListsByMType[mType];
    for (AnMetric m : mList.getmMetricList()) {
      if (lastName == null || !lastName.equals(m.getComd())) {
        list.add(m);
        lastName = m.getComd();
      }
    }

    return list;
  }

  public int getSortIndex(int mtype) {
    int sortIndex = -1;
    if (metricListsByMType != null && mtype >= 0 && mtype < metricListsByMType.length) {
      AnMetric[] mlist = metricListsByMType[mtype].getmMetricList();
      for (int index = 0; index < mlist.length; index++) {
        if (mlist[index].isSorted()) {
          sortIndex = index;
          break;
        }
      }
    }
    return sortIndex;
  }

  public void dump() {
    System.out.println("--------------------------metricStates");
    for (MetricState metricState : metricStateList) {
      metricState.dump();
    }
  }

  protected void dumpMetricStates() {
    if (metricListsByMType != null) {
      for (int mtype = 0; mtype < metricListsByMType.length; mtype++) {
        dumpMetricListsByMType(mtype);
      }
    }
    System.out.println("reverseSortDirection: " + reverseSortDirection);
  }

  protected void dumpMetricListsByMType(int mtype) {
    System.out.println("State mtype: " + mtype);
    metricListsByMType[mtype].dump();
  }

  public boolean isReverseDirectionSorting() {
    return reverseSortDirection;
  }

  public void setReverseDirectionSorting(boolean reverseDirectionSorting) {
    this.reverseSortDirection = reverseDirectionSorting;
    for (MMetric mMetric : metricListsByMType) {
      mMetric.setReverseSortDirection(reverseSortDirection);
    }
  }

  public MetricType[] getMetricSortByMType() {
    MetricType[] metricSortByMType = new MetricType[MetricsSetting.MET_LAST];
    int index = 0;
    for (MMetric mMetric : metricListsByMType) {
      AnMetric sortMetric = mMetric.getSortMetric();
      metricSortByMType[index] = new MetricType(sortMetric.getComd(), sortMetric.getSubType());
      index++;
    }
    return metricSortByMType;
  }

  public List<MetricType>[] getMetricOrderLists() {
    List<MetricType>[] lists = (ArrayList<MetricType>[]) new ArrayList[MetricsSetting.MET_LAST];

    for (int mtype = 0; mtype < MetricsSetting.MET_LAST; mtype++) {
      List<MetricType> orderList = new ArrayList<MetricType>();
      MMetric metricList = metricListsByMType[mtype];
      for (AnMetric anMetric : metricList.getmMetricList()) {
        orderList.add(new MetricType(anMetric.getComd(), anMetric.getSubType()));
      }
      lists[mtype] = orderList;
    }

    return lists;
  }

  public MMetric[] getMetricListsByMType() {
    return metricListsByMType;
  }

  public AnMetric[] getMetricListByMType(int mtype) {
    if (metricListsByMType == null) {
      return null;
    }
    if (mtype < 0 || mtype >= metricListsByMType.length) {
      return null;
    }
    return metricListsByMType[mtype].getmMetricList();
  }

  public List<AnMetric> getAnMetricsByMTypeTrimmed(int mtype) {
    AnMetric[] anMetricList = getMetricListByMType(mtype);
    List<AnMetric> list = new ArrayList<AnMetric>();
    if (anMetricList != null) {
      String last = null;
      for (AnMetric anMetric : anMetricList) {
        if (!anMetric.getComd().equals(last)) {
          list.add(anMetric);
          last = anMetric.getComd();
        }
      }
    }
    return list;
  }

  public void setMetricListsByMType(MMetric[] metricListsByMType) {
    this.metricListsByMType = metricListsByMType;
  }

  public List<MetricType> getNewMetricOrder() {
    return newMetricOrder;
  }

  public void setNewMetricOrder(List<MetricType> newMetricOrder) {
    this.newMetricOrder = newMetricOrder;
  }

  public MetricType getNewSortMetric() {
    return newSortMetric;
  }

  public void setNewSortMetric(MetricType newSortMetric) {
    this.newSortMetric = newSortMetric;
  }

  public Boolean getNewReverseSortDirection() {
    return newReverseSortDirection;
  }

  public void setNewReverseSortDirection(Boolean newReverseSortDirection) {
    this.newReverseSortDirection = newReverseSortDirection;
    setReverseDirectionSorting(newReverseSortDirection);
  }

  /**
   * @return the metricsRootNode
   */
  public MetricNode getMetricsRootNode() {
    return metricsRootNode;
  }
}
