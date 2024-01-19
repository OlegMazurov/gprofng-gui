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

package org.gprofng.mpmt.table;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnTable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

public class ExclusiveAndInclusivePanel extends JPanel {

  private List<ExclusiveOrInclusivePanel> exclusiveOrInclusivePanelList;
  private AttributePanel attributePanel;

  public ExclusiveAndInclusivePanel(
      MetricPanel metricPanel,
      AnTable anTable,
      final MetricLabel[] metricLabels,
      boolean wrapMetricName,
      int column,
      int count,
      HeaderMouseHandler headerMouseHandler) {
    //            setBackground(AnEnvironment.TABLE_HEADER_BACKGROUND_COLOR);
    setOpaque(false);
    setLayout(new GridBagLayout());

    // Add Exclusive/Inclusive panels
    int[][] columnWidth = anTable.getColumnWidth();

    int exclusiveCount = 0;
    Integer exclusiveIndex = null;
    int exclusiveWidth = 0;
    ImageIcon exclusiveIcon = null;

    int inclusiveCount = 0;
    Integer inclusiveIndex = null;
    int inclusiveWidth = 0;
    ImageIcon inclusiveIcon = null;

    int attributedCount = 0;
    Integer attributedIndex = null;
    int attributedWidth = 0;
    ImageIcon attributedIcon = null;

    int staticCount = 0;
    Integer staticIndex = null;
    int staticWidth = 0;
    ImageIcon staticIcon = null;

    int nameCount = 0;
    exclusiveOrInclusivePanelList = new ArrayList<ExclusiveOrInclusivePanel>();

    for (int i = column; i < column + count; i++) {
      AnMetric anMetric = metricLabels[i].getAnMetric();
      if (anMetric.getSubType() == 2
          || // Exclusive
          anMetric.getSubType() == 16) { // Machine model metrics?
        exclusiveCount++;
        if (exclusiveIndex == null) {
          exclusiveIndex = i;
        }
        exclusiveWidth += columnWidth[i][0];
        exclusiveIcon = metricLabels[i].getIcon();
      } else if (anMetric.getSubType() == 4) { // Inclusive
        inclusiveCount++;
        if (inclusiveIndex == null) {
          inclusiveIndex = i;
        }
        inclusiveWidth += columnWidth[i][0];
        inclusiveIcon = metricLabels[i].getIcon();
      } else if (anMetric.getSubType() == 8) { // Attributed
        // System? (Callers-callees)
        attributedCount++;
        if (attributedIndex == null) {
          attributedIndex = i;
        }
        attributedWidth += columnWidth[i][0];
        attributedIcon = metricLabels[i].getIcon();
      } else if (anMetric.isNameMetric()) { // Name
        nameCount++;
      } else if (anMetric.isStatic()) { // Static (Size, ...)
        staticCount++;
        if (staticIndex == null) {
          staticIndex = i;
        }
        staticWidth += columnWidth[i][0];
        staticIcon = metricLabels[i].getIcon();
      } else {
        System.out.println("ERROR " + anMetric.getSubType());
      }
    }
    //            System.out.println(metricLabels[index].getAnMetric().getUserName() + ": " + count
    // + " " + exclusiveCount + " " + inclusiveCount + " " + attributedCount);

    int gridX = 0;
    if (exclusiveCount > 0) {
      String name;
      ImageIcon icon;
      if (anTable.getAnParent().showOnlyValuesInTables()) {
        name = AnLocale.getString("VALUES");
        icon = null;
      } else {
        name = AnLocale.getString("EXCLUSIVE");
        icon = exclusiveIcon;
      }
      String toolTip = name + ": " + metricLabels[column].getAnMetric().getUserName();
      ExclusiveOrInclusivePanel panel =
          new ExclusiveOrInclusivePanel(
              metricPanel,
              anTable,
              metricLabels,
              name,
              icon,
              toolTip,
              exclusiveIndex,
              exclusiveCount,
              headerMouseHandler);
      if (inclusiveCount > 0) {
        if (AnWindow.getInstance().getExperimentGroups().length == 1) {
          panel.setBorder(
              BorderFactory.createMatteBorder(0, 0, 0, 1, AnEnvironment.TABLE_VERTICAL_GRID_COLOR));
        } else {
          panel.setBorder(
              BorderFactory.createMatteBorder(
                  0, 0, 0, 1, AnEnvironment.TABLE_VERTICAL_GRID_ATTRIBUTE_COMP_COLOR));
        }
      }
      panel.setPreferredSize(new Dimension(exclusiveWidth, panel.getPreferredSize().height));
      panel.setMinimumSize(new Dimension(exclusiveWidth, panel.getPreferredSize().height));
      panel.setMaximumSize(new Dimension(exclusiveWidth, panel.getPreferredSize().height));
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX++;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 1.0;
      add(panel, gridBagConstraints);
      exclusiveOrInclusivePanelList.add(panel);
    }
    if (inclusiveCount > 0) {
      String name = AnLocale.getString("INCLUSIVE");
      String toolTip = name + ": " + metricLabels[column].getAnMetric().getUserName();
      ExclusiveOrInclusivePanel panel =
          new ExclusiveOrInclusivePanel(
              metricPanel,
              anTable,
              metricLabels,
              name,
              inclusiveIcon,
              toolTip,
              inclusiveIndex,
              inclusiveCount,
              headerMouseHandler);
      panel.setPreferredSize(new Dimension(inclusiveWidth, panel.getPreferredSize().height));
      panel.setMinimumSize(new Dimension(inclusiveWidth, panel.getPreferredSize().height));
      panel.setMaximumSize(new Dimension(inclusiveWidth, panel.getPreferredSize().height));
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX++;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 1.0;
      add(panel, gridBagConstraints);
      exclusiveOrInclusivePanelList.add(panel);
    }
    if (attributedCount > 0) {
      String name = AnLocale.getString("ATTRIBUTED");
      String toolTip = name + ": " + metricLabels[column].getAnMetric().getUserName();
      ExclusiveOrInclusivePanel panel =
          new ExclusiveOrInclusivePanel(
              metricPanel,
              anTable,
              metricLabels,
              name,
              null,
              toolTip,
              attributedIndex,
              attributedCount,
              headerMouseHandler);
      panel.setPreferredSize(new Dimension(attributedWidth, panel.getPreferredSize().height));
      panel.setMinimumSize(new Dimension(attributedWidth, panel.getPreferredSize().height));
      panel.setMaximumSize(new Dimension(attributedWidth, panel.getPreferredSize().height));
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX++;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 1.0;
      add(panel, gridBagConstraints);
      exclusiveOrInclusivePanelList.add(panel);
    }
    if (staticCount > 0) {
      String toolTip = metricLabels[column].getAnMetric().getUserName();
      ExclusiveOrInclusivePanel panel =
          new ExclusiveOrInclusivePanel(
              metricPanel,
              anTable,
              metricLabels,
              "VALUE",
              null,
              toolTip,
              staticIndex,
              staticCount,
              headerMouseHandler);
      panel.setPreferredSize(new Dimension(staticWidth, panel.getPreferredSize().height));
      panel.setMinimumSize(new Dimension(staticWidth, panel.getPreferredSize().height));
      panel.setMaximumSize(new Dimension(staticWidth, panel.getPreferredSize().height));
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX++;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 1.0;
      add(panel, gridBagConstraints);
      exclusiveOrInclusivePanelList.add(panel);
    }
    if (nameCount > 0) {
      if (anTable.getType() != AnDisplay.DSP_Source
          && anTable.getType() != AnDisplay.DSP_SourceV2
          && anTable.getType() != AnDisplay.DSP_Disassembly
          && anTable.getType() != AnDisplay.DSP_DisassemblyV2
          && anTable.getType() != AnDisplay.DSP_SourceDisassembly) {
        if (anTable.getNames()[1] != null) {
          JLabel label = new JLabel(anTable.getNames()[1]);
          AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
          GridBagConstraints gridBagConstraints = new GridBagConstraints();
          gridBagConstraints.anchor = GridBagConstraints.WEST;
          gridBagConstraints.weightx = 1.0;
          gridBagConstraints.insets = new Insets(0, 4, 0, 0);
          add(label, gridBagConstraints);
        }
      }
      attributePanel =
          new AttributePanel(
              metricPanel, anTable, metricLabels, null, column, headerMouseHandler, null);
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX++;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.BOTH;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.weighty = 1.0;
      add(attributePanel, gridBagConstraints);
    }
  }

  public List<JMenuItem> getSortByMenuItems() {
    List<JMenuItem> list = new ArrayList<JMenuItem>();
    for (ExclusiveOrInclusivePanel exclusiveOrInclusivePanel : exclusiveOrInclusivePanelList) {
      list.addAll(exclusiveOrInclusivePanel.getSortByMenuItems());
    }
    return list;
  }

  protected ExclusiveOrInclusivePanel findExclusiveOrInclusivePanel(int x) {
    int start = 0;
    for (int i = 0; i < exclusiveOrInclusivePanelList.size(); i++) {
      if (x >= start && x <= start + exclusiveOrInclusivePanelList.get(i).getSize().width) {
        return exclusiveOrInclusivePanelList.get(i);
      }
      start += exclusiveOrInclusivePanelList.get(i).getSize().width;
    }
    return null;
  }

  protected AttributePanel findAttributePanel() {
    return attributePanel; // Only for Name metric
  }

  protected int findExclusiveOrInclusivePanelOffset(int x) {
    int start = 0;
    for (int i = 0; i < exclusiveOrInclusivePanelList.size(); i++) {
      if (x >= start && x <= start + exclusiveOrInclusivePanelList.get(i).getSize().width) {
        return x - start;
      }
      start += exclusiveOrInclusivePanelList.get(i).getSize().width;
    }
    return 0;
  }
}
