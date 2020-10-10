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

import org.gprofng.mpmt.timeline_common.CoordCalcDataMaster;
import java.util.ArrayList;

/**
 * Coordinate calculator for timeline rows
 *
 * <p>Coordinate systems used:
 *
 * <p>"vcoord": coordinate system to represent user rows on screen where Zero corresponds to upper
 * edge of first row. "canvas": coordinate system of canvas. Scrollbar and margins are added to
 * vcoord.
 *
 * <p>Note: A lot of the complexity in this class is related to vertical zoom, a feature which is
 * currently disabled.
 */
public class TL2CoordCalcData extends CoordCalcDataMaster {
  // --- total range of rows to be displayed
  private int abs_row_end = 0; // absolute end row   (at full zoom out)
  private int abs_row_count = 0; // number of rows   (at full zoom out)

  // --- canvas size
  private int canvasPixels = 0;

  // --- zoom range (value [0.0-1.0] corresponds to [0-vcoord_total_pixels])
  private boolean use_rows_for_range = false; // rows or percent to set range?
  private double visible_start_pct = 0.0; // visible start (% of total)
  private double visible_end_pct = 1.0; // visible end   (% of total)
  private int visible_start_row = 0; // visible start (row)
  private int visible_end_row = 0; // visible start (row)

  // --- state
  private boolean is_initialized = false; // edt_revalidate() has been called at least once
  private boolean needs_compute = false;
  private int vcoord_visible_top = 0; // vcoord offset to start of visible data

  // --- row layout (vcoord)
  private ArrayList<Integer> vcoord_row_tops; // row upper edge (unscaled)
  private ArrayList<Integer> vcoord_row_bottoms; // row lower edge (unscaled)
  private int vcoord_total_pixels = 1; // total size of all rows (unscaled)

  // preferences
  private final int canvasY_header = 0; // optional vertical offset
  private final int canvasY_margin; // space between TL and ruler
  public static final double HUNDRED_PERCENT_ROUNDUP = 0.999;

  public TL2CoordCalcData(int canvas_margin) {
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
    // update vcoord_visible_top
    if (vcoord_row_tops == null) {
      vcoord_total_pixels = 1;
      vcoord_visible_top = 0;
      needs_compute = false;
      return;
    }
    double requested_pct = visible_end_pct - visible_start_pct;
    double avail_pct = (double) getAvailPixels() / vcoord_total_pixels;
    if (avail_pct > 1.0) {
      avail_pct = 1.0;
    }
    if (requested_pct < avail_pct) {
      // enough space to show more; expand range
      double center_pct = (visible_end_pct + visible_start_pct) / 2;
      visible_start_pct = center_pct - avail_pct / 2;
      visible_end_pct = center_pct + avail_pct / 2;
    }
    if (visible_start_pct < 0) {
      // shift range to start at 0
      visible_end_pct += (0 - visible_start_pct);
      visible_start_pct = 0;
    }
    if (visible_end_pct > 1.0) {
      // shift range to end below %100
      visible_start_pct -= (visible_end_pct - 1.0);
      visible_end_pct = 1.0;
    }
    if (visible_start_pct < 0) {
      visible_start_pct = 0; // weird, request was greater than %100
    }

    visible_start_row = getRowNearPercent(visible_start_pct);
    visible_end_row = getRowNearPercent(visible_end_pct);
    vcoord_visible_top = (int) (vcoord_total_pixels * visible_start_pct);

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
      needs_compute = true;
    }
  }

  public int getAbsRowEnd() {
    return abs_row_end;
  }

  public int getAbsRowCount() {
    return abs_row_count;
  }

  private void internalSetRowHeights(ArrayList<Integer> mins, int rowCount, int minPixels) {
    int absRowCount = (mins != null) ? mins.size() : rowCount;
    setAbsRowCount(absRowCount);
    int total_used = 0;
    if (absRowCount == 0) {
      vcoord_row_tops = vcoord_row_bottoms = null;
      needs_compute = true;
    } else {
      ArrayList<Integer> new_vcoord_row_tops = new ArrayList(absRowCount);
      ArrayList<Integer> new_vcoord_row_bottoms = new ArrayList(absRowCount);
      for (int ii = 0; ii < absRowCount; ii++) {
        new_vcoord_row_tops.add(total_used);
        final int rowHeight;
        if (mins != null) {
          rowHeight = mins.get(ii);
        } else {
          rowHeight = minPixels;
        }
        total_used += rowHeight;
        new_vcoord_row_bottoms.add(total_used);
      }

      for (int ii = 0; !needs_compute && ii < absRowCount; ii++) {
        if (new_vcoord_row_tops.get(ii) != vcoord_row_tops.get(ii)
            || new_vcoord_row_bottoms.get(ii) != vcoord_row_bottoms.get(ii)) {
          needs_compute = true;
        }
      }
      vcoord_row_tops = new_vcoord_row_tops;
      vcoord_row_bottoms = new_vcoord_row_bottoms;
    }

    // set unscaled row layout
    vcoord_total_pixels = total_used;
    if (vcoord_total_pixels < 1) {
      vcoord_total_pixels = 1;
    }

    edt_revalidate();
    // visible_start_row = getRowNearPercent(visible_start_pct);
    // visible_end_row   = getRowNearPercent(visible_end_pct);
  }

  public void setAbsRowMinHeights(int rowCount, int minPixels) {
    internalSetRowHeights(null, rowCount, minPixels);
  }

  public void setAbsRowMinHeights(ArrayList<Integer> mins) {
    internalSetRowHeights(mins, 0, 0);
  }

  public void setExpandToFill(boolean expands) {}

  public void setRowRange(int startrow, int endrow) {
    // snap to row not implemented at this time
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
    return vcoord_total_pixels;
  }

  public int getMinPixelsRequired(int row_lo, int row_hi) { // YXXX unused
    if (vcoord_row_tops == null) {
      return 0;
    }
    int top = vcoord_row_tops.get(row_lo);
    int bottom = vcoord_row_tops.get(row_hi);
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

  public double getCenterPercentForRow(int yrow) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int uy = (vcoord_row_tops.get(yrow) + vcoord_row_bottoms.get(yrow)) / 2;
    double percent = (double) uy / vcoord_total_pixels;
    return percent;
  }

  public double getRowPercentOfTotal(int yrow) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int uy = vcoord_row_bottoms.get(yrow) - vcoord_row_tops.get(yrow);
    double percent = (double) uy / vcoord_total_pixels;
    return percent;
  }

  public int getRowNearPercent(double percent) {
    if (vcoord_row_tops == null) {
      return 0;
    }
    int uy = (int) (vcoord_total_pixels * percent);
    if (uy <= 0) {
      return 0;
    }
    int nrows = getAbsRowCount();
    for (int ii = 0; ii < nrows - 1; ii++) {
      if (uy >= vcoord_row_tops.get(ii) && uy < vcoord_row_bottoms.get(ii)) {
        return ii;
      }
    }
    return nrows - 1;
  }
}
