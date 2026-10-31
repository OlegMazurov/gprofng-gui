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


package org.gprofng.mpmt.timeline2;

/**
 * How an application metrics chart is drawn: its vertical range (fixed, or following the visible
 * data) and, when it holds several metrics, whether they are stacked or drawn over each other.
 */
public final class ChartStyle {
  /** Range of the bins on screen, a single metric (or metrics drawn over each other). */
  public static final ChartStyle VISIBLE = new ChartStyle(false, 0, 0, false);

  public final boolean fixed;
  public final double min;
  public final double max;
  public final boolean stacked;

  private ChartStyle(
      final boolean fixed, final double min, final double max, final boolean stacked) {
    this.fixed = fixed;
    this.min = min;
    this.max = max;
    this.stacked = stacked;
  }

  public static ChartStyle fixed(final double min, final double max) {
    return new ChartStyle(true, min, max, false);
  }

  public ChartStyle withStacked(final boolean stacked) {
    return new ChartStyle(fixed, min, max, stacked);
  }
}
