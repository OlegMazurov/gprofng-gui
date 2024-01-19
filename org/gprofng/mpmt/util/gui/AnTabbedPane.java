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
import org.gprofng.mpmt.AnLocale;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;

public class AnTabbedPane extends JTabbedPane {

  public AnTabbedPane() {
    AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Tabbed Pane"));
    setFont(getFont().deriveFont(Font.BOLD));
    setBorder(BorderFactory.createLineBorder(AnEnvironment.TABBED_PANE_BORDER_COLOR, 1));
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    setMinimumSize(new Dimension(0, 0));
    setUI(new AnTabbedPaneUI());
  }
}
