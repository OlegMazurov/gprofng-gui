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

package org.gprofng.mpmt.navigation;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.compare.CompareNavigationPanel;
import org.gprofng.mpmt.filter.FilterNavigationPanel;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnSplitPane;
import org.gprofng.mpmt.util.gui.AnSplitPaneFixedRightSize;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class NavigationPanel extends JPanel {
  private static boolean compareStatusPanelShowing = false;
  private static int heightOfCompareStatusPanel = 0;
  private AnWindow anWindow;
  private AnJScrollPane scrollPane;
  private ViewsPanel viewsPanel;
  private TopNavigationPanel topNavigationPanel;
  private AnSplitPaneFixedRightSize navigationSplitPane;
  private FilterNavigationPanel filterNavigationPanel;
  private CompareNavigationPanel compareNavigationPanel;

  public NavigationPanel(AnWindow anWindow) {
    this.anWindow = anWindow;

    setLayout(new GridBagLayout());
    setBackground(AnEnvironment.DEFAULT_BACKGROUND_COLOR);

    JPanel viewsNavigationPanel = new JPanel();
    viewsNavigationPanel.setBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR));
    viewsNavigationPanel.setLayout(new BorderLayout());

    // Top Navigation area
    topNavigationPanel = new TopNavigationPanel(anWindow);
    viewsNavigationPanel.add(topNavigationPanel, BorderLayout.NORTH);

    // View Panels
    scrollPane = new AnJScrollPane();
    viewsPanel = new ViewsPanel(scrollPane);
    //        viewPanels.setViewPanelOrderList(UserPref.getInstance().getViewPanelOrder());
    scrollPane.setBorder(null);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setViewportView(viewsPanel);
    scrollPane.setMinimumSize(new Dimension(0, 50));
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    viewsNavigationPanel.add(scrollPane, BorderLayout.CENTER);

    filterNavigationPanel = new FilterNavigationPanel(anWindow);
    compareNavigationPanel = new CompareNavigationPanel();

    JPanel filterCompareNavigationPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    filterCompareNavigationPanel.add(filterNavigationPanel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    filterCompareNavigationPanel.add(compareNavigationPanel, gridBagConstraints);

    navigationSplitPane =
        new AnSplitPaneFixedRightSize(
            AnSplitPane.VERTICAL_SPLIT,
            viewsNavigationPanel,
            filterCompareNavigationPanel,
            UserPref.getInstance().getNavigationFilterSplitPane().getSize(),
            UserPref.getInstance().getNavigationFilterSplitPane().getDefaultSize());
    navigationSplitPane.setOneTouchExpandable(false);
    navigationSplitPane.setDividerSize(2);
    navigationSplitPane.setBackground(AnEnvironment.NAVIGATION_SPLIT_PANE_DIVIDER_COLOR);
    //        filterPanel.setBackground(Color.red);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(navigationSplitPane, gridBagConstraints);

    setMinimumSize(new Dimension(5, 5)); // so the divider can be moved all the way to the left
  }

  public void adjustCompareNavigationPanelHeightChanged(int diff) {
    int now = navigationSplitPane.getDividerLocation();
    navigationSplitPane.setDividerLocationInternal(now - diff);
  }

  /**
   * @return the panel holding all the views
   */
  public ViewsPanel getViewsPanel() {
    return viewsPanel;
  }

  public int getNavigationSplitPaneSizeInPixels() {
    int sizeInPixels = navigationSplitPane.getSizeInPixels();
    sizeInPixels -= compareNavigationPanel.getDiffFromInitialHeight();
    return sizeInPixels;
  }
}
