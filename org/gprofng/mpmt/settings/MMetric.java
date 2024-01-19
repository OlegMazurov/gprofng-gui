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


package org.gprofng.mpmt.settings;

import org.gprofng.mpmt.AnMetric;

public class MMetric {
  private AnMetric[] mMetricList;
  private int sortIndex;
  private boolean reverseSortDirection;

  public MMetric(AnMetric[] mMetricList, boolean reverseSortDirection) {
    this.mMetricList = mMetricList;
    this.reverseSortDirection = reverseSortDirection;
    refreshStates();
  }

  private void refreshStates() {
    int index = 0;
    for (AnMetric anMetric : mMetricList) {
      if (anMetric.isSorted()) {
        sortIndex = index;
        break;
      }
      index++;

      if (index >= mMetricList.length) {
        mMetricList[0].setSorted(true);
        sortIndex = 0;
      }
    }
  }

  public MMetric copy() {
    AnMetric[] copyMMetricList = new AnMetric[getmMetricList().length];
    int i = 0;
    for (AnMetric anMetric : getmMetricList()) {
      copyMMetricList[i++] = anMetric;
    }

    MMetric copyMMetric = new MMetric(copyMMetricList, isReverseSortDirection());
    return copyMMetric;
  }

  public AnMetric[] getmMetricList() {
    return mMetricList;
  }

  public void setmMetricList(AnMetric[] mMetricList) {
    this.mMetricList = mMetricList;
  }

  public int getIndex(AnMetric am) {
    int index = 0;
    for (AnMetric anMetric : mMetricList) {
      if (anMetric == am) {
        return index;
      }
      index++;
    }
    return -1;
  }

  public int getSortIndex() {
    return sortIndex;
  }

  public AnMetric getSortMetric() {
    return mMetricList[sortIndex];
  }

  public boolean isReverseSortDirection() {
    return reverseSortDirection;
  }

  public void setSortIndex(int sortIndex) {
    if (sortIndex >= 0 && sortIndex < mMetricList.length) {
      this.sortIndex = sortIndex;
    }
  }

  /**
   * @param reverseSortDirection the reverseSortDirection to set
   */
  public void setReverseSortDirection(boolean reverseSortDirection) {
    this.reverseSortDirection = reverseSortDirection;
  }

  protected void dump() {
    if (mMetricList != null) {
      int n = 0;
      for (AnMetric anMetric : mMetricList) {
        System.out.print("  " + n++ + ": " + anMetric.getName());
        if (anMetric.isSorted()) {
          System.out.print(" <== " + "SORT");
        }
        System.out.println();
      }
    }
    System.out.println("  sortIndex: " + sortIndex);
    System.out.println("  reverseSortDirection: " + reverseSortDirection);
  }
}
