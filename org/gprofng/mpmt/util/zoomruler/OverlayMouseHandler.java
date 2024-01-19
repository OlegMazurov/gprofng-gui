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

import java.awt.Cursor;

/**
 * The OverlayMouseHander object allows for components with an overlayed image to defer mouse
 * handling in the overlay to the overlay. The overlayed image will be the zoomruler but this could
 * be used with other overlayed images that want mouse control.
 */
public interface OverlayMouseHandler {

  /** Test wether a mouse action was in the overlay. */
  public boolean inOverlay(int x, int y);

  /**
   * Deactive mouse handling and let the overlay handle mouse event, presumably called when the
   * mouse gestures are within the overlay.
   */
  public void deactivate();

  /**
   * Active mouse handling and handling and handle mouse events, presumably called when the mouse
   * has left the overlay.
   */
  public void activate();

  /** Get the cursor cursor. */
  public Cursor getCursor();

  /** The default cursor to show when the mouse is in an overlay. */
  public static final Cursor overlaycursor = new Cursor(Cursor.HAND_CURSOR);
}
