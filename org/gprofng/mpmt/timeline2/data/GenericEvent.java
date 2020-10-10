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

package org.gprofng.mpmt.timeline2.data;

public class GenericEvent {
  public final long eventIdx;
  public final int binStart;
  public final int binCount;
  public final long timeStart, timeEnd;
  private final double NS_PER_SEC = 1000000000.0;

  public GenericEvent(long eventIdx, int binStart, int binCount, long timeStart, long timeEnd) {
    this.eventIdx = eventIdx;
    this.binStart = binStart;
    this.binCount = binCount;
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
  }

  @Override
  public String toString() {
    final GenericEvent event = this;
    StringBuilder buf = new StringBuilder();
    // buf.append("eventIdx=" + event.eventIdx); // Not stable GUI testing?
    buf.append(" timeStart=" + event.timeStart / NS_PER_SEC);
    buf.append(" timeEnd=" + event.timeEnd / NS_PER_SEC);
    buf.append(" binStart=" + event.binStart);
    buf.append(" binCount=" + event.binCount);
    return buf.toString();
  }
}
