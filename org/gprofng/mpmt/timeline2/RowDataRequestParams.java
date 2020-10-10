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

public class RowDataRequestParams {
  // final
  public final TL2DataSnapshot snapshot;
  public final boolean forceLoad;
  public final long binTime;
  public final long alignedTimeStart;
  public final long timeEnd;
  public final int nbins;
  public final int rowStart;
  public final int rowEnd;
  public final long required_timeStart;
  public final long required_timeEnd;
  public final int required_rowStart;
  public final int required_rowEnd;

  // dynamic
  private boolean cancel;

  public RowDataRequestParams(
      final TL2DataSnapshot snapshot,
      final boolean forceLoad,
      final long bin_time,
      final long alignedStart,
      final int nbins,
      final int rowStart,
      final int rowEnd,
      final long required_timeStart,
      final long required_timeEnd,
      final int required_rowStart,
      final int required_rowEnd) {
    this.snapshot = snapshot;
    this.forceLoad = forceLoad;
    this.binTime = bin_time;
    this.alignedTimeStart = alignedStart;
    this.timeEnd = alignedStart + (nbins * bin_time - 1);
    this.nbins = nbins;
    this.rowStart = rowStart;
    this.rowEnd = rowEnd;
    this.cancel = false;
    this.required_timeStart = required_timeStart;
    this.required_timeEnd = required_timeEnd;
    this.required_rowStart = required_rowStart;
    this.required_rowEnd = required_rowEnd;
  }

  public synchronized void cancel() {
    cancel = true;
  }

  public synchronized boolean isCancelled() {
    return cancel;
  }
}
