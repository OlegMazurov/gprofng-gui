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


package org.gprofng.mpmt.mainview;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.BorderLayout;
import javax.swing.JPanel;

public class Panel extends JPanel {
  public Panel() {
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    setBorder(null);
    setLayout(new BorderLayout());
  }
}
