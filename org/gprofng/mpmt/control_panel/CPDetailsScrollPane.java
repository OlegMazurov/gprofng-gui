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

package org.gprofng.mpmt.control_panel;

import org.gprofng.mpmt.util.gui.AnJScrollPane;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

// the following is an attempt to create a scrollpane that respects
// the existing width of the RHT.  It's not completely smooth.
public class CPDetailsScrollPane extends AnJScrollPane {
  private JComponent child;

  public CPDetailsScrollPane(JComponent cc) {
    super(cc, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    child = cc;
  }

  public Dimension getPreferredSize() {
    Dimension dd = child.getPreferredSize();
    JScrollBar hscroll = this.getHorizontalScrollBar();
    if (hscroll != null) {
      // if(true || hscroll.isVisible()){ // isVisible not reliable
      int s_height = hscroll.getPreferredSize().height;
      dd.height += s_height;
    }
    dd.width = 0;
    return dd;
  }
}
