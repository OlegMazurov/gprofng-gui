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

package org.gprofng.mpmt.timeline2.data;

public class StackEvent extends GenericEvent {
  public final long stackId;
  public final int mState; // OS state at time

  public StackEvent(
      long eventId,
      int binStart,
      int binCount,
      long timeStart,
      long timeEnd,
      long stackId,
      int mstate) {
    super(eventId, binStart, binCount, timeStart, timeEnd);
    this.stackId = stackId;
    this.mState = mstate;
  }

  @Override
  public String toString() {
    final StackEvent event = this;
    StringBuilder buf = new StringBuilder();
    buf.append(super.toString());
    // buf.append(" stackId=" + event.stackId); // Not stable GUI testing
    buf.append(" mState=" + event.mState);
    return buf.toString();
  }
}
