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

package org.gprofng.mpmt.timeline_common;

import java.util.ArrayList;

/**
 * Coordinate calculator for timeline rows
 *
 * <p>Coordinate systems used:
 *
 * <p>"ucoord": coordinate system to represent unscaled user rows. Zero corresponds to upper edge of
 * first row. "vcoord": coordinate system to represent user rows scaled to fit screen. Zero
 * corresponds to upper edge of first row. "canvas": coordinate system of canvas. Scrollbar and
 * margins are added to vcoord;
 */
public class CoordCalcDataImpl extends CoordCalcDataMaster {
  // ------- user settings ------
  // --- total range of rows to be displayed
  private int abs_row_end = 0; // absolute end row   (at full zoom out)
  private int abs_row_count = 0; // number of rows   (at full zoom out)

  // --- canvas size
  private int canvasPixels = 0;

  // --- unscaled row layout (ucoord)
  private ArrayList<Integer> ucoord_row_tops; // row upper edge (unscaled)
  private ArrayList<Integer> ucoord_row_bottoms; // row lower edge (unscaled)

  // --- zoom range (value [0.0-1.0] corresponds to [0-ucoord_total_pixels])
  private double visible_start_pct = 0.0; // visible start (% of total)
  private double visible_end_pct = 0.0; // visible end   (% of total)
  private int visible_start_row; // visible start (row)
  private int visible_end_row; // visible start (row)

  // ------- computed values ------
  // --- state
  private boolean is_initialized = false; // edt_revalidate() has been called at least once
  private boolean needs_compute = false;
  private boolean needs_vcoord_table;
  private int vcoord_visible_top; // vcoord offset to start of visible data
  private double vcoord2ucoordRatio = 1.0; // YXXX look for div/zero
  private boolean use_rows_for_range; // rows or percent to set range?

  // --- unscaled row layout (ucoord)
  private int ucoord_total_pixels = 1; // total size of all rows (unscaled)

  // --- scaled row layout (vcoord)
  private ArrayList<Integer> vcoord_row_tops; // row upper edge (scaled)
  private ArrayList<Integer> vcoord_row_bottoms; // row lower edge (scaled)
  private int vcoord_total_pixels = 1; // total size of all rows (scaled)

  // preferences
  private boolean expandRowsToFill = true; // expand (only at full zoom out)
  private final int canvasY_header = 0; // optional vertical offset
  private final int canvasY_margin; // space between TL and ruler
  public static final double HUNDRED_PERCENT_ROUNDUP = 0.999;

  public CoordCalcDataImpl(int canvas_margin) {
    canvasY_margin = canvas_margin;
  }

  // --- canvas geometry
  /** Sets current canvas height */
  @Override
  public void setCanvasPixels(int visiblePixels) {
    if (canvasPixels != visiblePixels) {
      canvasPixels = visiblePixels;
      needs_compute = true;
    }
  }

  /** Gets current canvas height */
  public int getCanvasPixels() {
    return canvasPixels;
  }

  // --- row geometry settings
  @Override
  public void edt_revalidate() {
    // setAbsRowMinHeight*() must be called first
    if (needs_compute) {
      calculateRowSizes();
      needs_compute = false;
    }
    is_initialized = true;
  }

  /** sets variables used to calculate row height */
  private void calculateRowSizes() {
    if (ucoord_row_tops == null) {
      setVcoordHeights(1.0);
      vcoord_visible_top = 0;
      needs_compute = false;
      return;
    }
    // calculate new scale
    double vdelta = getAvailPixels();
    double udelta;
    if (use_rows_for_range) {
      int uy_top = ucoord_row_tops.get(visible_start_row);
      int uy_bottom = ucoord_row_bottoms.get(visible_end_row);
      udelta = uy_bottom - uy_top;
    } else {
      udelta = ucoord_total_pixels * getVisiblePercent();
    }
    double ideal_scale = vdelta / udelta;

    // adjust scale if required
    double actual_scale;
    if (!expandRowsToFill && ideal_scale > 1.0) {
      actual_scale = 1.0;
    } else {
      if (ideal_scale < HUNDRED_PERCENT_ROUNDUP) {
        boolean toBigToFit = true;
      }
      actual_scale = ideal_scale;
    }

    // update the vcoord tables
    setVcoordHeights(actual_scale);

    // update vcoord_visible_top
    {
      int vy_top;
      if (use_rows_for_range) {
        vy_top = vcoord_row_tops.get(visible_start_row);
      } else {
        vy_top = (int) (vcoord_total_pixels * visible_start_pct);
      }
      vcoord_visible_top = vy_top;
    }

    needs_compute = false;
  }

  /** margins allow states along edges to be visible and accessible */
  public int getMargin() {
    return canvasY_margin;
  }

  public boolean isInitialized() {
    return is_initialized;
  }

  public boolean isValid() {
    return !needs_compute;
  }

  // --- row range
  private void setAbsRowCount(int rowCount) {
    int end = rowCount > 0 ? rowCount - 1 : 0;
    if (abs_row_count != rowCount || abs_row_end != end) {
      abs_row_count = rowCount;
      abs_row_end = end;
      needs_vcoord_table = true;
      needs_compute = true;
    }
  }

  public int getAbsRowEnd() {
    return abs_row_end;
  }

  public int getAbsRowCount() {
    return abs_row_count;
  }

  public void setAbsRowMinHeights(ArrayList<Integer> mins) { // YXXX just set mins
    setAbsRowCount(mins.size());
    int absRowCount = getAbsRowCount();
    int total_used = 0;
    if (absRowCount == 0) {
      ucoord_row_tops = ucoord_row_bottoms = null;
    } else {
      ucoord_row_tops = new ArrayList(absRowCount);
      ucoord_row_bottoms = new ArrayList(absRowCount);
      for (int ii = 0; ii < absRowCount; ii++) {
        ucoord_row_tops.add(total_used);
        int rowHeight = mins.get(ii);
        total_used += rowHeight;
        ucoord_row_bottoms.add(total_used);
      }
    }
    setUcoordHeights(total_used);
  }

  public void setAbsRowMinHeights(int rowCount, int minPixels) {
    setAbsRowCount(rowCount);
    int absRowCount = getAbsRowCount();
    int total_used = 0;
    if (absRowCount == 0) {
      ucoord_row_tops = ucoord_row_bottoms = null;
    } else {
      ucoord_row_tops = new ArrayList(absRowCount);
      ucoord_row_bottoms = new ArrayList(absRowCount);
      for (int ii = 0; ii < absRowCount; ii++) {
        ucoord_row_tops.add(total_used);
        total_used += minPixels;
        ucoord_row_bottoms.add(total_used);
      }
    }
    setUcoordHeights(total_used);
  }

  public void setExpandToFill(boolean expands) {
    expandRowsToFill = expands;
  }

  public void setRowRange(int startrow, int endrow) {
    if (use_rows_for_range != true || visible_start_row != startrow || visible_end_row != endrow) {
      use_rows_for_range = true;
      visible_start_row = startrow;
      visible_end_row = endrow;

      if (ucoord_row_tops == null) {
        visible_start_pct = 0;
        visible_end_pct = 0;
      } else {
        visible_start_pct = (double) ucoord_row_tops.get(startrow) / ucoord_total_pixels;
        visible_end_pct = (double) ucoord_row_bottoms.get(endrow) / ucoord_total_pixels;
      }
      needs_compute = true;
    }
  }

  public int getRowStart() {
    return visible_start_row;
  }

  public int getRowEnd() {
    return visible_end_row;
  }

  public int getRowCount() {
    return visible_end_row - visible_start_row + 1;
  }

  public void setVisibleRange(double start, double end) {
    if (use_rows_for_range != false || visible_start_pct != start || visible_end_pct != end) {
      use_rows_for_range = false;
      visible_start_pct = start;
      visible_end_pct = end;

      visible_start_row = getRowNearPercent(visible_start_pct);
      visible_end_row = getRowNearPercent(visible_end_pct);
      needs_compute = true;
    }
  }

  public double getVisibleStart() {
    return visible_start_pct;
  }

  public double getVisibleEnd() {
    return visible_end_pct;
  }

  public double getVisiblePercent() {
    return visible_end_pct - visible_start_pct;
  }

  /* --- initialize coordinate tables --- */
  private void setUcoordHeights(int total_used) {
    // set unscaled row layout
    ucoord_total_pixels = total_used;
    if (ucoord_total_pixels < 1) {
      ucoord_total_pixels = 1;
    }
    needs_vcoord_table = true;

    visible_start_row = getRowNearPercent(visible_start_pct);
    visible_end_row = getRowNearPercent(visible_end_pct);
    needs_compute = true;
  }

  private void setVcoordHeights(double new_vcoord2ucoordRatio) {
    if (!needs_vcoord_table && vcoord2ucoordRatio == new_vcoord2ucoordRatio) {
      return;
    }
    vcoord2ucoordRatio = new_vcoord2ucoordRatio;

    if (ucoord_row_tops == null) {
      vcoord_row_tops = vcoord_row_bottoms = null;
      needs_vcoord_table = false;
      vcoord_total_pixels = 1;
      return;
    }

    // populate vcoord_row_* ArrayLists
    int absRowCount = getAbsRowCount();
    vcoord_row_tops = new ArrayList(absRowCount);
    vcoord_row_bottoms = new ArrayList(absRowCount);
    int ii;
    int vcoord;
    // loop (below) ensures that top of each row is the same value
    // as bottom of previous row.  This prevents gaps caused by rounding.
    int ucoord = 0;
    vcoord_row_tops.add(0);
    for (ii = 1; ii < absRowCount; ii++) {
      ucoord = ucoord_row_tops.get(ii);
      vcoord = ucoord2vcoord(ucoord, new_vcoord2ucoordRatio);
      vcoord_row_bottoms.add(vcoord);
      vcoord_row_tops.add(vcoord);
    }
    ucoord = ucoord_row_bottoms.get(absRowCount - 1);
    vcoord = ucoord2vcoord(ucoord, new_vcoord2ucoordRatio);
    vcoord_row_bottoms.add(vcoord);
    vcoord_total_pixels = vcoord;
    if (vcoord_total_pixels < 1) {
      vcoord_total_pixels = 1;
    }
    needs_vcoord_table = false;
  }

  private int ucoord2vcoord(int ucoord, double ratio) {
    double vcoord = ucoord * ratio;
    if (vcoord > Integer.MAX_VALUE) {
      vcoord = Integer.MAX_VALUE;
      /* overflow! */
    }
    return (int) vcoord;
  }

  // ---------- computed values ----------
  private int vcoord2canvas(int vy) {
    int canvasY = vy - vcoord_visible_top + (canvasY_margin + canvasY_header);
    return canvasY;
  }

  private int canvas2vcoord(int canvasY) {
    int vy = canvasY + vcoord_visible_top - (canvasY_margin + canvasY_header);
    return vy;
  }

  public int getAvailPixels() {
    int availHeight = canvasPixels - canvasY_margin * 2;
    if (availHeight < 1) {
      availHeight = 1;
    }
    return availHeight;
  }

  public int getTotalPixels() { // YXXX unused
    return vcoord_total_pixels;
  }

  public int getMinPixelsForAllRows() {
    return ucoord_total_pixels;
  }

  public int getMinPixelsRequired(int row_lo, int row_hi) { // YXXX unused
    if (vcoord_row_tops == null) {
      return 0;
    }
    int top = ucoord_row_tops.get(row_lo);
    int bottom = ucoord_row_tops.get(row_hi);
    return bottom - top;
  }

  /** Get top coordinate for the ith row */
  public int getLowCoordForRow(int yrow) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int vy = vcoord_row_tops.get(yrow);
    int y = vcoord2canvas(vy);
    return y;
  }

  /** Get center coordinate for the ith row */
  public int getCenterCoordForRow(int yrow) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int vyt = vcoord_row_tops.get(yrow);
    int vyb = vcoord_row_bottoms.get(yrow);
    int vy = (vyt + vyb) / 2;
    int y = vcoord2canvas(vy);
    return y;
  }

  /** Get bottom coordinate for the ith row */
  public int getHighCoordForRow(int yrow) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int vy = vcoord_row_bottoms.get(yrow);
    int y = vcoord2canvas(vy);
    return y;
  }

  // will return -1 if yy is between states
  public int getRowAtCoord(int canvasY) { // YXXX this might no longer be needed
    int idx = internalCoord2Row(canvasY);
    if (idx >= getAbsRowCount()) {
      return -1;
    }
    return idx;
  }

  // always returns a valid row num
  public int getRowNearCoord(int canvasY) {
    int idx = internalCoord2Row(canvasY);
    int nrows = getAbsRowCount();
    if (idx < 0) {
      return 0;
    } else if (idx >= nrows) {
      return nrows - 1;
    }
    return idx;
  }

  // always returns a valid row num
  private int internalCoord2Row(int canvasY) {
    if (vcoord_row_tops == null) {
      return -1;
    }
    int vy = canvas2vcoord(canvasY);
    if (vy < 0) {
      return -1;
    }
    int nrows = getAbsRowCount();
    for (int ii = 0; ii < nrows; ii++) {
      if (vy >= vcoord_row_tops.get(ii) && vy < vcoord_row_bottoms.get(ii)) {
        return ii;
      }
    }
    return nrows;
  }

  public double getPercentNearCoord(int canvasY) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int vy = canvas2vcoord(canvasY);
    double percent = (double) vy / vcoord_total_pixels;
    return percent;
  }

  /* --- important: the following use only ucoord values --- */
  public double getCenterPercentForRow(int yrow) {
    if (ucoord_row_tops == null) {
      return 0;
    }
    int uy = (ucoord_row_tops.get(yrow) + ucoord_row_bottoms.get(yrow)) / 2;
    double percent = (double) uy / ucoord_total_pixels;
    return percent;
  }

  public double getRowPercentOfTotal(int yrow) {
    if (ucoord_row_tops == null) {
      return 0;
    }
    int uy = ucoord_row_bottoms.get(yrow) - ucoord_row_tops.get(yrow);
    double percent = (double) uy / ucoord_total_pixels;
    return percent;
  }

  public int getRowNearPercent(double percent) {
    if (ucoord_row_tops == null) {
      return 0;
    }
    int uy = (int) (ucoord_total_pixels * percent);
    if (uy <= 0) {
      return 0;
    }
    int nrows = getAbsRowCount();
    for (int ii = 0; ii < nrows - 1; ii++) {
      if (uy >= ucoord_row_tops.get(ii) && uy < ucoord_row_bottoms.get(ii)) {
        return ii;
      }
    }
    return nrows - 1;
  }
}
