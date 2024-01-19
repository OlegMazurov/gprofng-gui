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

package org.gprofng.mpmt.timeline2.cursorevent;

import org.gprofng.mpmt.timeline2.RowGeometry;
import org.gprofng.mpmt.timeline2.TimelineSelectionEvent;

public class CursorSetEvent extends CursorEvent {

  public final RowGeometry rowGeometry;
  public final long clickTime;
  public final boolean ctrlKey;
  public final boolean shiftKey;
  public final long eventIdx; // may be TimelineSelectionEvent.EVT_IDX_INVALID
  public final long eventTimeStart; // only set if eventIdx is valid
  public final long eventTimeEnd; // only set if eventIdx is valid
  public final boolean directHit; // click was on event
  public final boolean requestRefreshCaliper; // update of calipers requested
  public final boolean updateRowSelections; // update row selections

  public CursorSetEvent(
      RowGeometry row,
      long clickTime,
      boolean ctrlKey,
      boolean shiftKey,
      long eventIdx,
      long eventTimeStart,
      long eventTimeEnd,
      boolean directHit,
      boolean requestRefreshCaliper,
      boolean updateRowSelections) {
    this.rowGeometry = row;
    this.clickTime = clickTime;
    this.ctrlKey = ctrlKey;
    this.shiftKey = shiftKey;
    this.eventIdx = eventIdx;
    this.eventTimeStart = eventTimeStart;
    this.eventTimeEnd = eventTimeEnd;
    this.directHit = directHit;
    this.requestRefreshCaliper = requestRefreshCaliper;
    this.updateRowSelections = updateRowSelections;
  }

  public CursorSetEvent(
      RowGeometry row,
      long clickTime,
      boolean ctrlKey,
      boolean shiftKey,
      boolean requestRefreshCaliper,
      boolean updateRowSelections) {
    this.rowGeometry = row;
    this.clickTime = clickTime;
    this.ctrlKey = ctrlKey;
    this.shiftKey = shiftKey;
    this.eventIdx = TimelineSelectionEvent.EVT_IDX_INVALID;
    this.eventTimeStart = -1;
    this.eventTimeEnd = -1;
    this.directHit = false;
    this.requestRefreshCaliper = requestRefreshCaliper;
    this.updateRowSelections = updateRowSelections;
  }
}
