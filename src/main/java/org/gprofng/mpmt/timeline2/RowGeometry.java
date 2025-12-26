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

package org.gprofng.mpmt.timeline2;

import java.util.List;

public class RowGeometry {
  public final int rowNum; // index into parent List
  public final List<RowGeometry> parent; // parent list
  public final int nSubrows; // total number of rows within data type
  public final int subrowNum; // row number within data type
  public final RowData rowData;
  public final int chartPropIdx; // -1 for event data, >=0 for data chart
  public final String propName; // internal name of PROP_* property
  public final String propUName; // user-visible name of PROP_* property
  public final int startY;
  public final int endY;
  public final int dataStartY;
  public final int dataEndY;
  public final boolean isEntityStart;
  public final boolean isEntityEnd;
  public final boolean isExperimentEnd;
  public final boolean isCompareGroupEnd;
  public final int rowDividerHeight; // height of divider below this row

  private RowGeometry masterRow; // points to row used for selection

  public RowGeometry(
      int rowNum,
      List<RowGeometry> parent,
      int nSubrows,
      int subrowNum,
      RowData rowData,
      int chartPropIdx, // -1 for event data, >=0 for data chart
      String propName,
      String propUName,
      int startY,
      int endY,
      int dataStartY,
      int dataEndY,
      boolean isEntityStart,
      boolean isEntityEnd,
      boolean isExperimentEnd,
      boolean isCompareGroupEnd,
      int rowDividerHeight) {
    this.rowNum = rowNum;
    this.parent = parent;
    this.nSubrows = nSubrows;
    this.subrowNum = subrowNum;
    this.rowData = rowData; // note: rowData will be updated asynchronously
    this.masterRow = this;
    this.chartPropIdx = chartPropIdx;
    this.propName = propName;
    this.propUName = propUName;
    this.startY = startY;
    this.endY = endY;
    this.dataStartY = dataStartY;
    this.dataEndY = dataEndY;
    this.isEntityStart = isEntityStart;
    this.isEntityEnd = isEntityEnd;
    this.isExperimentEnd = isExperimentEnd;
    this.isCompareGroupEnd = isCompareGroupEnd;
    this.rowDividerHeight = rowDividerHeight;
  }

  public void setMasterRow(RowGeometry row) {
    masterRow = row;
  }

  public RowGeometry getMasterRow() {
    return masterRow;
  }

  public boolean isChartData() {
    return (chartPropIdx != -1);
  }
}
