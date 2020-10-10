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
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JCheckBox;

/**
 * @author tpreisle
 */
public class AnCheckBox extends JCheckBox {

  public AnCheckBox() {
    super();
    init();
  }

  public AnCheckBox(String text) {
    super(text);
    init();
  }

  public AnCheckBox(String text, boolean selected) {
    super(text, selected);
    init();
  }

  private void init() {
    setMargin(new Insets(0, 0, 0, 0));
    setOpaque(false);
  }

  @Override
  public void paint(Graphics g) {
    if (isEnabled()) {
      super.paint(g);
      if (hasFocus() && getText().length() == 0) {
        // Accessibility
        g.setColor(new Color(194 - 40, 214 - 40, 233 - 40));
        g.drawLine(getWidth() - 1, 2, getWidth() - 1, getHeight() - 3);
      }
    } else {
      g.setColor(new Color(230, 230, 230));
      g.drawRect(2, 2, 12, 12);
    }
  }
}
