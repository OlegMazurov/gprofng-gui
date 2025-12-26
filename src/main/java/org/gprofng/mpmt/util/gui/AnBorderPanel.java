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

package org.gprofng.mpmt.util.gui;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 * @author tpreisle
 */
public class AnBorderPanel extends JPanel {
  private Color topBorderColor = null;
  private Color rightBorderColor = null;
  private Color leftBorderColor = null;
  private Color bottomBorderColor = null;

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (topBorderColor != null) {
      g.setColor(topBorderColor);
      g.drawLine(0, 0, getWidth() - 1, 0);
    }
    if (rightBorderColor != null) {
      g.setColor(rightBorderColor);
      g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
    }
    if (getLeftBorderColor() != null) {
      g.setColor(getLeftBorderColor());
      g.drawLine(0, 0, 0, getHeight() - 1);
    }
    if (bottomBorderColor != null) {
      g.setColor(bottomBorderColor);
      g.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
    }
  }

  /**
   * @return the topBorder
   */
  public Color getTopBorderColor() {
    return topBorderColor;
  }

  /**
   * @param topBorder the topBorder to set
   */
  public void setTopBorderColor(Color topBorderColor) {
    this.topBorderColor = topBorderColor;
  }

  /**
   * @return the rightBorderColor
   */
  public Color getRightBorderColor() {
    return rightBorderColor;
  }

  /**
   * @param rightBorderColor the rightBorderColor to set
   */
  public void setRightBorderColor(Color rightBorderColor) {
    this.rightBorderColor = rightBorderColor;
  }

  /**
   * @return the bottomBorderColor
   */
  public Color getBottomBorderColor() {
    return bottomBorderColor;
  }

  /**
   * @param bottomBorderColor the bottomBorderColor to set
   */
  public void setBottomBorderColor(Color bottomBorderColor) {
    this.bottomBorderColor = bottomBorderColor;
  }

  /**
   * @return the leftBorderColor
   */
  public Color getLeftBorderColor() {
    return leftBorderColor;
  }

  /**
   * @param leftBorderColor the leftBorderColor to set
   */
  public void setLeftBorderColor(Color leftBorderColor) {
    this.leftBorderColor = leftBorderColor;
  }
}
