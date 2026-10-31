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
 * The application metrics of one experiment, as served by the engine's getAppMetricTree: parallel
 * arrays, one entry per metric. The hierarchy is in the names, split at ':'.
 */
public final class AppMetricTree {
  /** Engine's stand-in for "no value": IPC cannot carry NaN. */
  public static final double MISSING_VALUE = -1.0e18;

  /** Joins the metric names of a stacked chart in its chart property name (as in the engine). */
  public static final char STACK_SEPARATOR = '\u001f';

  public final String[] names;
  public final String[] descriptions;
  public final String[] groups;
  public final int[] types; // 0 = int, 1 = double, 2 = boolean
  public final double[] mins;
  public final double[] maxs;

  private AppMetricTree(
      String[] names,
      String[] descriptions,
      String[] groups,
      int[] types,
      double[] mins,
      double[] maxs) {
    this.names = names;
    this.descriptions = descriptions;
    this.groups = groups;
    this.types = types;
    this.mins = mins;
    this.maxs = maxs;
  }

  /** @param raw reply of getAppMetricTree, or null if the experiment has no metrics */
  static AppMetricTree fromIpc(final Object[] raw) {
    if (raw == null || raw.length < 6 || !(raw[0] instanceof String[])) {
      return null;
    }
    return new AppMetricTree(
        (String[]) raw[0],
        (String[]) raw[1],
        (String[]) raw[2],
        (int[]) raw[3],
        (double[]) raw[4],
        (double[]) raw[5]);
  }

  public int size() {
    return names.length;
  }

  /** Index of a metric by name, or -1. */
  public int indexOf(final String name) {
    for (int i = 0; i < names.length; i++) {
      if (names[i].equals(name)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Text for a metric value: integers as such, others in plain notation with up to 3 decimals.
   * Exponent notation is only used for values too small or too large for that.
   */
  public static String format(final double v) {
    if (v == Math.rint(v) && Math.abs(v) < 1e15) {
      return Long.toString((long) v);
    }
    final double a = Math.abs(v);
    if (a < 1e-3 || a >= 1e15) {
      return String.format("%.3g", v);
    }
    String s = String.format("%.3f", v);
    if (s.indexOf('.') >= 0 && !s.contains("e")) { // drop trailing zeros: 1.500 -> 1.5
      s = s.replaceAll("0+$", "").replaceAll("[.,]$", "");
    }
    return s;
  }
}
