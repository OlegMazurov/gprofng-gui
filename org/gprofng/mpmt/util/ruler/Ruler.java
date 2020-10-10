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

package org.gprofng.mpmt.util.ruler;


/**
 * A Component that is suitable to representa range of values or a discrete set of labels in a chart
 * or timeline. The ruler has three levels, one for ticks, one for labels, and one for the title.
 *
 * @author blewis
 */
public interface Ruler {
  // orientation
  public static final int VERTICAL_ORIENTATION = 1;
  public static final int HORIZONTAL_ORIENTATION = 2;

  // ticks placement
  public static final int TICKS_ON_THE_BOTTOM = 1;
  public static final int TICKS_ON_THE_TOP = 2;

  // title location
  public static final int PLACEMENT_NONE = 0; // title not displayed
  public static final int PLACEMENT_WITH_LABELS = 1; // title inline w/tick labels
  public static final int PLACEMENT_TITLE_LEVEL = 2; // separate row for title

  /**
   * Set a spacing to align component padLo is at the 0 end of the graphics' coordinate system padHi
   * is at the opposite end
   */
  public void setPadding(int padLo, int padHi);

  /** set direction of ruler w.r.t. coordinate system */
  public void setOrderReverse(boolean reverse);
}
