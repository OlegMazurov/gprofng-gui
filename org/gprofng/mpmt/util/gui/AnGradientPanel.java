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

package org.gprofng.mpmt.util.gui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * @author tpreisle
 */
public class AnGradientPanel extends javax.swing.JPanel {
  private Color topColor;
  private Color bottomColor;

  public AnGradientPanel(Color topColor, Color bottomColor) {
    this.topColor = topColor;
    this.bottomColor = bottomColor;
  }

  @Override
  public void paintComponent(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    Graphics2D g2d = (Graphics2D) g;
    GradientPaint gp;
    gp = new GradientPaint(0, 0, topColor, 0, height, bottomColor);
    g2d.setPaint(gp);
    g2d.fillRect(0, 0, width - 1, height - 1);
    setOpaque(false);
  }
}
