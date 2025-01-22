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

package org.gprofng.mpmt.table;

import org.gprofng.mpmt.AnTable;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

public class ExclusiveOrInclusivePanel extends JPanel {

  private MetricPanel metricPanel;
  private String labelText;
  private AttributePanel[] attributepanels;

  public ExclusiveOrInclusivePanel(
      MetricPanel metricPanel,
      AnTable anTable,
      final MetricLabel[] metricLabels,
      String labelText,
      ImageIcon imageIcon,
      String tooltip,
      int column,
      int count,
      HeaderMouseHandler headerMouseHandler) {
    //            System.out.println(txt + " " + index + " " + count);
    this.metricPanel = metricPanel;
    this.labelText = labelText;
    setLayout(new GridBagLayout());
    setOpaque(false);
    setToolTipText(tooltip);

    addMouseListener(headerMouseHandler);
    addMouseMotionListener(headerMouseHandler);

    // What label
    JLabel whatLabel = new JLabel(labelText);
    AnUtility.setAccessibleContext(whatLabel.getAccessibleContext(), whatLabel.getText());
    whatLabel.setFont(AnTableHeaderPanel.verySmallFont);
    if (imageIcon != null) {
      whatLabel.setIcon(imageIcon);
    }
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(0, 2, 0, 2);
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    add(whatLabel, gridBagConstraints);

    // Attribute panel
    JPanel attributePanelOuter = new JPanel(new GridBagLayout());
    attributePanelOuter.setOpaque(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(attributePanelOuter, gridBagConstraints);

    attributepanels = new AttributePanel[count];
    int gridX = 0;
    for (int i = 0; i < count; i++) {
      AttributePanel attributePanel =
          new AttributePanel(
              metricPanel, anTable, metricLabels, this, column + i, headerMouseHandler, tooltip);
      attributepanels[i] = attributePanel;

      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX++;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.insets = new Insets(1, 0, 0, 0);
      attributePanelOuter.add(attributePanel, gridBagConstraints);
    }

    // DnD
    addMouseMotionListener(new MouseDraggedListener());
  }

  class MouseDraggedListener extends MouseMotionAdapter {

    @Override
    public void mouseDragged(MouseEvent e) {
      JComponent c = (JComponent) e.getSource();
      ExclusiveOrInclusivePanel exclusiveOrInclusivePanel = (ExclusiveOrInclusivePanel) c;
      MetricPanel metricPanel = exclusiveOrInclusivePanel.getMetricPanel();
      TransferHandler handler = metricPanel.getTransferHandler();
      handler.exportAsDrag(metricPanel, e, TransferHandler.COPY);
    }
  }

  public MetricPanel getMetricPanel() {
    return metricPanel;
  }

  public String getLabelText() {
    return labelText;
  }

  protected AttributePanel findAttributePanel(int x) {
    int start = 0;
    for (int i = 0; i < attributepanels.length; i++) {
      if (x >= start && x <= start + attributepanels[i].getSize().width) {
        return attributepanels[i];
      }
      start += attributepanels[i].getSize().width;
    }
    return null;
  }

  List<JMenuItem> getSortByMenuItems() {
    List<JMenuItem> list = new ArrayList<JMenuItem>();
    for (AttributePanel attributepanel : attributepanels) {
      list.add(attributepanel.getSortByMenuItem());
    }
    return list;
  }
}
