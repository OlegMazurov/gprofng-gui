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
package org.gprofng.mpmt.timeline_common;

public class TimelineCaliper {

  private final long time[]; // length[2], one for each caliper. [0] is anchor.
  private int activeIdx; // which of two calipers is dynamically changing, or -1 for none

  public TimelineCaliper(long time0, long time1) {
    this.time = new long[2];
    this.time[0] = time0;
    this.time[1] = time1;
    this.activeIdx = -1;
  }

  public TimelineCaliper clone() {
    TimelineCaliper clone = new TimelineCaliper(time[0], time[1]);
    clone.activeIdx = this.activeIdx;
    return clone;
  }

  public int getSize() {
    return time.length;
  }

  public long getTime(int idx) {
    return time[idx]; // throws if out of bounds
  }

  public void setTime(int idx, long newTime) {
    time[idx] = newTime; // throws if out of bounds
  }

  public boolean isSingleEvent() {
    return time[0] == time[1];
  }

  public long getLowTime() {
    return (time[0] < time[1]) ? time[0] : time[1];
  }

  public long getHighTime() {
    return (time[0] < time[1]) ? time[1] : time[0];
  }

  public int getActiveIdx() {
    return activeIdx;
  }

  public long getActiveCaliperTime() {
    if (activeIdx == -1) {
      return -1;
    }
    return time[activeIdx];
  }

  public void setActiveIdx(int idx) {
    if (idx >= 0 && idx < time.length) {
      activeIdx = idx;
    } else {
      activeIdx = -1;
    }
  }

  public boolean setActiveCaliperTime(long newTime) {
    if (activeIdx == -1) {
      return false;
    }
    time[activeIdx] = newTime;
    return true;
  }

  public void swapAnchor() {
    long tmp = time[0];
    time[0] = time[1];
    time[1] = tmp;
  }
}
