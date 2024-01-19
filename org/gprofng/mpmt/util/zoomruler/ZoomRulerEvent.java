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

package org.gprofng.mpmt.util.zoomruler;

/**
 * A an event the was triggered from the ZoomRuler. The types of possible events include - set level
 * - The zoom level changes to a specific level revert - The zoom level changes back to the prevous
 * level pan - A pan up, down, left, or right is requested reset - Zoom all the way out to the
 * initial level.
 */
public class ZoomRulerEvent {
  private int arg;
  private int type;

  // Identifiers for click locations
  // NOTE: negative numbers reserved for indicating the level selected:
  public static final int SET_LEVEL = 11;
  public static final int UNDO = 12;
  public static final int PAN = 13;
  public static final int LEVEL_PLUS = 14;
  public static final int LEVEL_MINUS = 15;
  public static final int REDO = 16;

  public static final int PAN_UP = 0;
  public static final int PAN_RIGHT = 1;
  public static final int PAN_DOWN = 2;
  public static final int PAN_LEFT = 3;
  public static final int RESET = 4;
  public static final int UNKNOWN = Integer.MAX_VALUE;

  /** Create a new event with the specified type and one argument. */
  public ZoomRulerEvent(int type, int arg) {
    this.type = type;
    this.arg = arg;
  }

  /** Get the specifies the zoom level for SET_LEVEL events */
  // XXXmpview need error handling
  public int getZoomLevel() {
    return arg;
  }

  /** Get direction for pan events */
  // XXXmpview need error handling
  public int getDirection() {
    return arg;
  }

  /** Return true if this is a revert event. */
  public boolean isUndo() {
    return (type == UNDO);
  }

  /** Return true if this is a revert event. */
  public boolean isRedo() {
    return (type == REDO);
  }

  /** Return true if this is a Zoom Level Change event. */
  public boolean isLevelChange() {
    return (type == SET_LEVEL);
  }

  /** Return true if this is a Zoom Plus click */
  public boolean isLevelPlus() {
    return (type == LEVEL_PLUS);
  }

  /** Return true if this is a Zoom Minus click */
  public boolean isLevelMinus() {
    return (type == LEVEL_MINUS);
  }

  /** Return true if this is a pan event. */
  public boolean isPan() {
    return (type == PAN);
  }

  /** Return true if this is a reset event. */
  public boolean isReset() {
    return (type == RESET);
  }
}
