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
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.KeyboardShortcuts;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public class ToolBarPanel extends JPanel {
  private JPanel controlPanel;
  private JComponent separator;
  private JComponent currentControls = null;
  private FindTextPanel findTextPanel;

  public ToolBarPanel(JToolBar toolBar) {
    initToolBarPanel(toolBar);
  }

  // Initialize aux area
  private void initToolBarPanel(JToolBar toolBar) {
    GridBagConstraints gridBagConstraints;
    int gridx = 0;

    JPanel tbPanel = this;
    tbPanel.setBorder(
        BorderFactory.createMatteBorder(1, 0, 1, 0, AnEnvironment.TOOLBAR_BORDER_COLOR));
    tbPanel.setLayout(new GridBagLayout());
    tbPanel.setBackground(AnEnvironment.TOOLBAR_BACKGROUND_COLOR);

    // Toolbar
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = gridx++;
    //        gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(1, 0, 1, 0);
    tbPanel.add(toolBar, gridBagConstraints);

    separator = new ToolBarSeparator(3, 6);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new Insets(1, 0, 2, 0);
    tbPanel.add(separator, gridBagConstraints);

    // Control panel
    controlPanel = new JPanel();
    controlPanel.setLayout(new BorderLayout());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(1, 0, 1, 0);
    tbPanel.add(controlPanel, gridBagConstraints);

    // Find panel
    findTextPanel = new FindTextPanel();
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.weightx = 1.0;
    tbPanel.add(findTextPanel, gridBagConstraints);

    separator.setVisible(false);
    controlPanel.setVisible(false);

    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut);
    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut);
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyboardShortcuts.helpActionShortcut, KeyboardShortcuts.helpActionShortcut);
    getActionMap()
        .put(
            KeyboardShortcuts.helpActionShortcut,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                //                Analyzer.getInstance().showHelp(null);
                AnWindow.getInstance().getViews().getCurrentViewDisplay().showHelp();
              }
            });
  }

  public void setControls(JComponent component) {
    removeControls();
    controlPanel.add(component);
    currentControls = component;
    separator.setVisible(true);
    controlPanel.setVisible(true);
  }

  public FindTextPanel getFindTextPanel() {
    return findTextPanel;
  }

  public JComponent getControls() {
    return currentControls;
  }

  public void removeControls() {
    controlPanel.removeAll();
    currentControls = null;
    separator.setVisible(false);
    controlPanel.setVisible(false);
  }
}
