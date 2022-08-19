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

package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.timeline2.data.*;

// RowData contains per-row event data that must be synchronized between
// a worker fetch thread and the AWT thread.
//
// In order to minimize MT risks, AWT-only data should not go into this class.

public class RowData {
  // final
  public final RowDefinition rowDefinition; // immutable
  public final int idx; // location within RowData list (stable for life of snapshot)

  // dynamic data from gp-display-text
  private RowDataRequestParams params;
  private GenericEvent[] events; // primary row events
  private Object[] perPropChartData; // chart data
  private boolean dataValid = false;

  public RowData(RowDefinition rowDefinition, int idx) {
    this.rowDefinition = rowDefinition;
    this.idx = idx;
  }

  // --- the following should only be called from data fetcher

  public synchronized RowDataRequestParams getParams() {
    return params;
  }

  public synchronized void setEventData(
      RowDataRequestParams params, GenericEvent[] events, Object[] perPropData) {
    this.params = params;
    this.dataValid = true;
    this.events = events;
    this.perPropChartData = perPropData;
  }

  public synchronized RowData grabEventData() {
    RowData rowData = new RowData(rowDefinition, idx);
    rowData.params = this.params;
    rowData.dataValid = this.dataValid;
    rowData.events = this.events;
    rowData.perPropChartData = this.perPropChartData;
    return rowData;
  }

  public synchronized void invalidateEventData() {
    this.params = null;
    this.dataValid = false;
    this.events = null;
    this.perPropChartData = null;
  }

  public synchronized boolean eventDataIsValid() {
    // set to FALSE when data is invalid, set to TRUE after data is valid
    return dataValid;
  }

  // --- the following can be called by any thread (e.g. paint thread)

  public RowDefinition getRowDef() { // does not contain changing data; not synchronized
    return rowDefinition;
  }

  public synchronized GenericEvent[] getEvents() {
    return events;
  }

  public synchronized Object getChartData(int idx) {
    if (perPropChartData == null) {
      return null;
    }
    if (idx < 0 || idx >= perPropChartData.length) {
      return null; // weird
    }
    return perPropChartData[idx];
  }
}
