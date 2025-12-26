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

package org.gprofng.mpmt.control_panel;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The MPIConrolPanel class provides the features common to both the MPIChartConrolPanel and
 * TimelineControlPanel. These include providing the visual features such as the toolbar buttons and
 * filtering buttons. Some of the processing of the control input is handled but much is done in the
 * extending classes implementation of the abstract methods.
 */
public class CPLayoutPanel extends JPanel {

  public CPLayoutPanel(
      JComponent tb, JComponent filter_panel, JComponent custom, JComponent details) {
    super();

    this.setLayout(new BorderLayout()); // north = toolbar, center = belowToolbar

    // --- this.center = belowToolbarPanel
    JPanel belowToolbarPanel = new JPanel();
    belowToolbarPanel.setLayout(new BorderLayout()); // north = aboveDetails, center = Details
    belowToolbarPanel.setBorder(BorderFactory.createEmptyBorder());
    JScrollPane scrollPane = new JScrollPane(belowToolbarPanel);
    scrollPane.setBorder(null);
    this.add(scrollPane, BorderLayout.CENTER);

    // ------- belowToolbarPanel.north = aboveDetailsBox
    JPanel aboveDetailsBox = new JPanel();
    aboveDetailsBox.setLayout(new BoxLayout(aboveDetailsBox, BoxLayout.Y_AXIS));
    aboveDetailsBox.setBorder(BorderFactory.createEmptyBorder());
    belowToolbarPanel.add(aboveDetailsBox, BorderLayout.NORTH);

    if (tb != null) {
      this.add(tb, BorderLayout.NORTH);
    }

    // ----------- aboveDetails top box: filtering
    if (filter_panel != null) {
      aboveDetailsBox.add(filter_panel);
    }

    // ------- more boxes between Details and Toolbar
    if (custom != null) {
      aboveDetailsBox.add(custom);
    }

    // ------- belowToolbarPanel.center = detailsPanel
    if (details != null) {
      belowToolbarPanel.add(details, BorderLayout.CENTER);
    }

    belowToolbarPanel.revalidate();
    aboveDetailsBox.revalidate();
    this.revalidate();
  }
}
