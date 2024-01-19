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

import java.awt.Dimension;
import java.awt.Graphics;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class ZoomRulerComponent extends JComponent implements Accessible {
  private JLabel accessibleHelper = new JLabel();
  private ZoomRulerOverlay overlay;

  public ZoomRulerComponent(
      int orientation,
      int slide_length,
      boolean resetable1d,
      boolean revertable,
      boolean autoRepeat) {
    super();
    overlay =
        new ZoomRulerOverlay(
            this, 0, 0, orientation, slide_length, resetable1d, false, revertable, autoRepeat);
    Dimension d;
    d = new Dimension(overlay.getWidth() + 1, overlay.getHeight() + 1);
    super.setMinimumSize(d);
    super.setMaximumSize(d);
    super.setPreferredSize(d);
  }

  public void setNumberOfLevels(int n) {
    overlay.setNumberOfLevels(n);
  }

  public void addZoomRulerListener(ZoomRulerListener listener) {
    overlay.activateMouseHandling();
    overlay.addZoomRulerListener(listener);
  }

  @Override
  public void setEnabled(boolean enable) {
    overlay.show(enable);
  }

  public void setZoomLevel(int il) {
    overlay.setZoomLevel(il);
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    overlay.paintRuler(g);
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    return accessibleHelper.getAccessibleContext();
  }
}
