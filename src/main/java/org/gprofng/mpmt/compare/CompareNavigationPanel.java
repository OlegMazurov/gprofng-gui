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

package org.gprofng.mpmt.compare;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class CompareNavigationPanel extends JPanel {

  private CompareStatusPanel compareStatusPanel;

  public CompareNavigationPanel() {
    setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 1, AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR));
    setBackground(AnEnvironment.NAVIGATION_PANEL_SECTION_BACKGROUND_COLOR);

    compareStatusPanel = new CompareStatusPanel();

    setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(2, 4, 6, 4);
    add(compareStatusPanel, gridBagConstraints);
    compareStatusPanel.repaint();
  }

  public int getDiffFromInitialHeight() {
    return compareStatusPanel.getDiffFromInitialHeight();
  }
}
