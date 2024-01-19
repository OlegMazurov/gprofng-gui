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

package org.gprofng.mpmt.timeline_common;

/** Coordinate Calculator for Timeline Time axis */
public class CoordCalcTimeImpl extends CoordCalcTimeMaster {
  // adjustable settings
  private int canvasPixels = 0;
  private long start_time = 0;
  private long end_time = 0;

  // computed
  private long start_time_align = 0; // used to align start @ data fetch
  private long time_per_bin = 1;
  private boolean is_initialized = false; // edt_revalidate() has been called at least once
  private boolean needs_compute = true;

  // preferences
  private final int min_pixels_per_bin = 1;
  private final int x_margins; // space between TL and ruler

  public CoordCalcTimeImpl(int canvas_margin) {
    x_margins = canvas_margin;
  }

  // --- canvas geometry
  /** Sets current canvas width */
  public void setCanvasPixels(int visiblePixels) {
    if (canvasPixels == visiblePixels) {
      return;
    }
    canvasPixels = visiblePixels;
    needs_compute = true;
  }

  /** Gets current canvas width */
  public int getCanvasPixels() {
    return canvasPixels;
  }

  // --- time geometry settings
  public void edt_revalidate() {
    setTimeBinAlignment();
    is_initialized = true;
  }

  private void setTimeBinAlignment() {
    // for a given zoom, determine time alignment of bins so that it is
    // consistent regardless of panning.
    long trange = getTimeDuration(); // nanoseconds
    int maxBinCount = getAvailPixels() / min_pixels_per_bin;
    long estBinSize;
    if (maxBinCount <= 0) {
      estBinSize = 0;
    } else {
      estBinSize = (trange + (maxBinCount - 1)) / maxBinCount;
    }
    time_per_bin = estBinSize;
    if (time_per_bin < 1) {
      time_per_bin = 1;
    }

    // compute alignment
    long absStartTime = 0; // Should it be: getAbsoluteTimeStart() ?
    long startdelta = start_time - absStartTime;
    long mod = startdelta % time_per_bin;
    start_time_align = mod;
    needs_compute = false;
  }

  /** Get the minimum number of pixels allowed for a bin */
  private int getMinPixelsPerBin() {
    return min_pixels_per_bin;
  }

  /** margins allow states along edges to be visible and accessible */
  public int getMargin() {
    return x_margins;
  }

  public boolean isInitialized() {
    return is_initialized;
  }

  public boolean isValid() {
    return !needs_compute;
  }

  // --- time range
  public void setTimeRange(long start, long end) {
    if (start_time == start && end_time == end) {
      return;
    }
    start_time = start;
    end_time = end;
    needs_compute = true;
  }

  public long getTimeStart() {
    return start_time;
  }

  public long getTimeEnd() {
    return end_time;
  }

  public long getTimeDuration() {
    long duration = end_time - start_time + 1;
    return duration;
  }

  // ---------- computed values ----------
  public int getAvailPixels() { // in pixels
    int availWidth = getCanvasPixels() - getMargin() * 2;
    return availWidth;
  }

  /** get avail # of bins */
  public int getNumBins() {
    long tmp = getTimeDuration() + time_per_bin - 1;
    tmp /= time_per_bin;
    return (int) tmp;
  }

  /** get avail # of bins */
  public long getTimePerBin() {
    return time_per_bin;
  }

  public long getTimeStartAligned() {
    long tstart = getTimeStart() - start_time_align;
    return tstart;
  }

  public long getTimeStartAligned(long time) {
    long absStartTime = 0; // Should it be: getAbsoluteTimeStart() ?
    // compute alignment
    if (time < absStartTime) { // punt on handling negative time
      return absStartTime;
    }
    long startdelta = time - absStartTime;
    long mod = startdelta % time_per_bin;
    long returnTime = time - mod;
    return returnTime;
  }

  public long getTimeEndAligned() {
    long tstart = getTimeStartAligned();
    int nbins = getNumBins();
    long tduration = nbins * time_per_bin; // fills the last bin
    long tend = tstart + tduration - 1; // != options.getFilterTimeEnd()
    return tend;
  }

  // return may be MPVIEW_CHART_DATA_NAN if click was not on a state
  public long getTimeAtCoord(int xx) {
    int x = xx - getMargin();
    long totalTime = getTimeDuration();
    int availWidth = getAvailPixels();
    if (availWidth <= 0) {
      availWidth = 1;
    }
    long time = x * totalTime / availWidth;
    if (time < 0) {
      return 0;
    }
    if (time >= getTimeDuration()) {
      return getTimeDuration();
    }
    time += start_time;
    return time;
  }

  // always returns a valid value
  public long getTimeNearCoord(int xx) {
    int x = xx - getMargin();
    long totalTime = getTimeDuration();
    int availWidth = getAvailPixels();
    if (availWidth <= 0) {
      return getTimeStart();
    }
    long time = x * totalTime / availWidth;
    time += start_time;
    if (time < getTimeStart()) {
      time = getTimeStart();
    }
    if (time > getTimeEnd()) {
      time = getTimeEnd();
    }
    return time;
  }

  public int getLowCoordForBin(int bin) {
    int totalBins = getNumBins();
    if (totalBins <= 0) {
      return 0;
    }
    if (bin < 0) {
      int ii = 0; // breakpoint
    }
    int availWidth = getAvailPixels();
    int x = bin * availWidth / totalBins;
    int xx = x + getMargin();
    return xx;
  }

  public int getCoordAtTime(long time) {
    long offsetTime = time - start_time;
    long duration = getTimeDuration();
    if (duration <= 0) {
      return 0;
    }
    int availWidth = getAvailPixels();
    int x = (int) (offsetTime * availWidth / duration);
    int xx = x + getMargin();
    return xx;
  }

  public int getBinAtTime(long time) {
    long offsetTime = time - start_time;
    long duration = getTimeDuration();
    if (duration <= 0) {
      return 0;
    }
    int totalBins = getNumBins();
    int bin = (int) (offsetTime * totalBins / duration);
    return bin;
  }
}
