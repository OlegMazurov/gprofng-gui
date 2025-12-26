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

package org.gprofng.mpmt.statecolors;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

// State represents each element in the callstack

// This class is immutable
public class StackState {

  private static final Font f;

  static {
    f = javax.swing.UIManager.getFont("List.font");
  }

  private static final int ICON_WIDTH = f.getSize() < 17 ? 19 : f.getSize() + 2;
  private static final int ICON_HEIGHT = f.getSize() < 17 ? 19 : f.getSize() + 2;
  private final String fname; // func name
  private final long fnumber; // Histable::Function* (NOT Function->id)

  // Constructor
  public StackState(String fname, long fnumber) {
    this.fname = fname;
    this.fnumber = fnumber;
  }

  public String getName() {
    return fname;
  }

  public long getNumber() {
    return fnumber;
  }

  @Override
  public Object clone() {
    StackState s = new StackState(fname, fnumber);
    return s;
  }

  public static Image createContigIcon(Color color) {
    return createIcon(color, ICON_WIDTH, ICON_HEIGHT, false);
  }

  public static Image createIcon(Color color) {
    return createIcon(color, ICON_WIDTH - 2, ICON_HEIGHT - 2, true);
  }

  public static Image createIcon(Color color, int width, int height, boolean draw_outline) {
    BufferedImage image;
    WritableRaster raster;
    ColorModel model;
    int argb, x_last, y_last, i, j;
    Object data;

    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    raster = image.getRaster();
    model = image.getColorModel();
    argb = color.getRGB();
    data = model.getDataElements(argb, null);
    x_last = width - 1;
    y_last = height - 1;

    for (i = 1; i < x_last; i++) {
      for (j = 1; j < y_last; j++) {
        raster.setDataElements(i, j, data);
      }
    }

    if (draw_outline) {
      color = Color.black;
      argb = color.getRGB();
      data = model.getDataElements(argb, null);

      for (i = 0; i < width; i++) {
        raster.setDataElements(i, 0, data);
        raster.setDataElements(i, y_last, data);
      }

      for (j = 0; j < height; j++) {
        raster.setDataElements(0, j, data);
        raster.setDataElements(x_last, j, data);
      }
    }

    return image;
  }

  @Override
  public String toString() {
    return fname;
  }
}
