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

import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline.events.DetailsIPC;
import org.gprofng.mpmt.timeline.events.EventDetail;
import org.gprofng.mpmt.timeline_common.TimelineSelectionGenericEvent;
import java.util.Vector;

/**
 * Holds information about the selected timeline event.
 *
 * <p>Code is based on old timeline events and MPITL event selection.
 */
public class TimelineSelectionEvent extends TimelineSelectionGenericEvent {
  public static final long EVT_IDX_INVALID = -1; // note: value used in IPC

  public enum CursorAxis {
    UP_DOWN,
    LEFT_RIGHT,
    NA
  };

  public final EventDetail eventDetail; // null for unknown
  public final int rowNum; // if -1, equivelant to "unselect"
  private final RowGeometry rowGeometry; // identifies row information, may be null
  private final RowDefinition rowDefinition; // identifies row information, may be null
  public final long stackId; // DetailsIPC.INVALID_STACK_ID for unknown
  private final Vector<StackState> stackFrames; // null for unknown

  // cursor movements that can be applied w.r.t. this selection
  public final boolean left;
  public final boolean right;
  public final boolean up;
  public final boolean down;
  public final boolean find;

  public final long eventIdx; // EVT_IDX_INVALID if no event under cursor; not valid after filter

  public final long clickTime; // time-location of mouse click, must be valid
  public final long eventTimeStart;
  public final long eventTimeEnd;
  public final boolean ctrlKey;
  public final boolean shiftKey;
  public final CursorAxis moveAxis;
  public final boolean requestCaliperUpdate;
  public final boolean updateRowSelections;

  public TimelineSelectionEvent() { // the "unselect" event
    this.rowNum = -1; // flags this as an unselect event
    this.rowGeometry = null;
    this.rowDefinition = null;

    this.eventDetail = null;
    this.stackId = DetailsIPC.INVALID_STACK_ID;
    this.stackFrames = null;

    left = right = up = down = find = false;
    this.eventIdx = EVT_IDX_INVALID;

    this.clickTime = 0;
    this.eventTimeStart = clickTime;
    this.eventTimeEnd = clickTime;
    this.ctrlKey = false;
    this.shiftKey = false;
    this.moveAxis = CursorAxis.NA;

    this.requestCaliperUpdate = false;
    this.updateRowSelections = false;
  }

  public TimelineSelectionEvent(
      RowGeometry rowGeometry,
      long clickTime,
      boolean ctrlKey,
      boolean shiftKey,
      CursorAxis axis,
      EventDetail evt,
      Vector<StackState> stackFrames,
      boolean left,
      boolean right,
      boolean up,
      boolean down,
      long eventIdx,
      long eventStartTime,
      long eventEndTime,
      boolean requestCaliperUpdate,
      boolean updateRowSelections) {
    this.rowGeometry = rowGeometry;
    if (rowGeometry == null) {
      this.rowNum = -1;
      this.rowDefinition = null;
    } else {
      this.rowNum = rowGeometry.rowNum;
      this.rowDefinition = rowGeometry.rowData.rowDefinition;
    }

    this.eventDetail = evt;
    if (evt == null) {
      this.stackId = DetailsIPC.INVALID_STACK_ID;
    } else {
      this.stackId = evt.getStack();
    }
    this.stackFrames = stackFrames;

    this.left = left;
    this.right = right;
    this.up = up;
    this.down = down;
    this.eventIdx = eventIdx;
    this.find = (eventIdx != EVT_IDX_INVALID);

    this.clickTime = clickTime;
    this.eventTimeStart = eventStartTime;
    this.eventTimeEnd = eventEndTime;
    this.ctrlKey = ctrlKey;
    this.shiftKey = shiftKey;
    this.moveAxis = axis;

    this.requestCaliperUpdate = requestCaliperUpdate;
    this.updateRowSelections = updateRowSelections;
  }

  public RowGeometry getRowGeometry() {
    return rowGeometry; // may be null
  }

  public RowDefinition getRowDefinition() {
    return rowDefinition; // may be null
  }

  public Vector<StackState> getStackFrames() {
    Vector<StackState> vec = (Vector<StackState>) stackFrames.clone();
    return vec;
  }
}
