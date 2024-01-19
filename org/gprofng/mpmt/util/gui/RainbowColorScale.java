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


package org.gprofng.mpmt.util.gui;

import java.awt.Color;

/**
 * The RainbowColorScale maps a range of values to colors to use in a chart. The colors range from a
 * red for high values to green for moderate values to blue for low values.
 *
 * @author blewis
 */
public class RainbowColorScale {
  private static Color[] colors;
  private static final int MAX_RAINBOW_COLORS = 256;

  /** Instantiate a new RainbowColorScale object */
  public RainbowColorScale() {
    initColors();
  }

  /** Compute the colors */
  void initColors() {
    if (colors != null) {
      return;
    }
    int height = MAX_RAINBOW_COLORS;
    if (height <= 0) {
      System.err.println("ERROR: RainbowColorScale.initColors(): height<=0, height=" + height);
      return;
    }

    Color colVector[] = new Color[height];

    final double minr = 70.0, ming = 70.0, minb = 70.0;
    final double maxr = 255.0, maxg = 245.0, maxb = 250.0;
    final double medr = 220;
    double fr = maxr, fg = ming, fb = minb;
    int phase;
    double dstep;

    phase = 1;
    dstep = (maxg - ming) / (0.45 * height); // next: add green for 0.45 height

    for (int i = 0; i < height; i++) {
      Color c =
          new Color(
              new Double(fr).intValue(), new Double(fg).intValue(), new Double(fb).intValue());
      int idx = height - i - 1;
      colVector[idx] = c;
      if (phase == 1) { // add green till we get to yellow
        fg += dstep;
        if (fg > maxg) {
          fg = maxg;
          phase++;
          dstep = (maxr - medr) / (0.1 * height); // next: subtract a little red for 0.1
        }
      } else if (phase == 2) { // subtract red till yellow-green
        fr -= dstep;
        if (fr < medr) {
          fr = medr;
          phase++;
          dstep = (medr - minr) / (0.1 * height); // next: subtract remainder of red for 0.1
        }
      } else if (phase == 3) { // subtract red till light green
        fr -= dstep;
        if (fr < minr) {
          fr = minr;
          phase++;
          dstep = (maxb - minb) / (0.25 * height); // next: add blue for 0.25
        }
      } else if (phase == 4) { // add blue until full blue
        fb += dstep;

        fg -= dstep * 0.8; // start removing green
        if (fg < ming) {
          fg = ming;
        }

        fr -= dstep * 1.0; // continue removing red
        if (fr < 0.0) {
          fr = 0.0;
        }

        if (fb > maxb) {
          fb = maxb;
          phase++;
          // dstep // keep it the same, seems ok
        }
      } else if (phase == 5) { // intensify/darken the blue
        fr += dstep * 0.2; // add a bit of red
        fb -= dstep * 0.5;
        fg -= dstep * 0.8; // continue removing green
        if (fg < 0.0) {
          fg = 0.0;
        }
      }
    }

    colors = colVector;
  }

  /** Compute the color for a single value within a range */
  public Color getColorForValue(long v, long start, long end) {
    int height = MAX_RAINBOW_COLORS;
    long length = end - start;
    if (start > end) {
      return Color.BLACK;
    }
    if (v < start || v > end) {
      return Color.BLACK;
    }
    if (length < 0) {
      return Color.BLACK;
    }
    if (length == 0) {
      return colors[0];
    }
    double percent = (double) (v - start) / length;
    int i = (int) (percent * height);
    if (i < 0) {
      i = 0;
    } else if (i >= height) {
      i = height - 1;
    }
    Color c = colors[i];
    return c;
  }
}
