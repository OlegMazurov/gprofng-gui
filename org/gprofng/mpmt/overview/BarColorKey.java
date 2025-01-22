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

package org.gprofng.mpmt.overview;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class BarColorKey extends JPanel {

  protected static final int barWidth = 9;
  protected static final int barHeight = 14;
  private List<Color> keyColorList;

  public BarColorKey(Color keyColor) {
    List<Color> fillColorList = new ArrayList<Color>();
    fillColorList.add(keyColor);
    this.keyColorList = fillColorList;
    init();
  }

  public BarColorKey(List<Color> keyColorList) {
    this.keyColorList = keyColorList;
    init();
  }

  private void init() {
    Dimension dim = new Dimension(barWidth, barHeight);
    setMinimumSize(dim);
    setMaximumSize(dim);
    setPreferredSize(dim);
    setBackground(Color.WHITE);
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (keyColorList != null) {
      int x = 0;
      int y = 1;
      // Fill
      int yStep = (barHeight - 2) / keyColorList.size();
      for (Color color : keyColorList) {
        g.setColor(color);
        g.fillRect(x, y, barWidth - 1, barHeight - 1);
        y += yStep;
      }
      // Border
      g.setColor(AnEnvironment.BAR_BORDER_COLOR);
      g.drawRect(0, 0, barWidth - 1, barHeight - 1);
    }
  }
}
