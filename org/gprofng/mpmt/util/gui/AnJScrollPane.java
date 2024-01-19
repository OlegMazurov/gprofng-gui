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

import org.gprofng.analyzer.AnEnvironment;
import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

public class AnJScrollPane extends JScrollPane {

  public AnJScrollPane() {
    super();
    init();
  }

  public AnJScrollPane(Component view) {
    super(view);
    init();
  }

  public AnJScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
    super(view, vsbPolicy, hsbPolicy);
    init();
  }

  private void init() {
    getViewport().setBackground(AnEnvironment.SCROLLPANE_BACKGROUND);
    setBorder(new LineBorder(AnEnvironment.SPLIT_PANE_BORDER_COLOR, 0));
  }
}
