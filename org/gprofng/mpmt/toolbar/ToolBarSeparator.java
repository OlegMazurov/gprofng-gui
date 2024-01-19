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


package org.gprofng.mpmt.toolbar;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * @author tpreisle
 */
public class ToolBarSeparator extends JPanel {

  public ToolBarSeparator(int leftSpace, int rightSpace) {
    setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);
    setLayout(new GridBagLayout());
    JSeparator separator = new JSeparator(JSeparator.VERTICAL);
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(2, leftSpace, 2, rightSpace);
    add(separator, gridBagConstraints);
  }
}
