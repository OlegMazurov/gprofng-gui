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

package org.gprofng.mpmt.statecolors;

import java.awt.Color;

public final class StateColorMap extends ColorMap {

  private final int rgb_min[] = {110, 120, 200};
  private final int rgb_max[] = {195, 195, 255};
  private final int max_brightness = 225 * 3; // max possible is 255*3;

  public StateColorMap() {}

  @Override
  protected Color getBaseColor(String name) {
    Color c;
    int hc = name.hashCode();
    double rgb[] = new double[3];
    double brightness = 0;
    for (int ii = 0; ii < 3; ii++) {
      rgb[ii] =
          ((hc >> 8 * ii) & 0xff) / 256.0 * (rgb_max[ii] - rgb_min[ii]) + (double) rgb_min[ii];
      brightness += rgb[ii];
    }
    if (brightness > max_brightness) {
      double dim_factor = max_brightness / brightness;
      for (int ii = 0; ii < 3; ii++) {
        rgb[ii] *= dim_factor;
      }
    }
    c = new Color((int) rgb[0], (int) rgb[1], (int) rgb[2]);
    return c;
  }
}
