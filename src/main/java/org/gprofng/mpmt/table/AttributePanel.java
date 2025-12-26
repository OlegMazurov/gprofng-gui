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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnCompDisp;
import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnTable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

public class AttributePanel extends JPanel {

  private MetricPanel metricPanel;
  private AnTable anTable;
  private ExclusiveOrInclusivePanel exclusiveOrInclusivePanel;
  private String compareLabelText = null;
  private JLabel sortIconLabel;
  private int column;
  private ImageIcon sortIcon;

  public AttributePanel(
      MetricPanel metricPanel,
      AnTable anTable,
      final MetricLabel[] metricLabels,
      ExclusiveOrInclusivePanel exclusiveOrInclusivePanel,
      int column,
      HeaderMouseHandler headerMouseHandler,
      String toolTip) {
    this.metricPanel = metricPanel;
    this.anTable = anTable;
    this.exclusiveOrInclusivePanel = exclusiveOrInclusivePanel;
    this.column = column;
    setLayout(new GridBagLayout());
    setOpaque(false);
    JLabel label;
    GridBagConstraints gridBagConstraints;
    int gridX2 = 0;
    addMouseListener(headerMouseHandler);
    addMouseMotionListener(headerMouseHandler);
    setToolTipText(toolTip);

    if (anTable.canSort()) {
      boolean sortedByThisColumn = false;
      if (anTable.getSortColumn() == column) {
        sortedByThisColumn = true;
      }

      if (sortedByThisColumn) {
        setOpaque(true);
        setBackground(AnEnvironment.TABLE_HEADER_SELECTED_COLOR);
      }
    }
    JPanel attributePanel = new JPanel(new GridBagLayout());
    attributePanel.setOpaque(false);

    MetricLabel metricLabel = metricLabels[column];
    if (metricLabel.getClock() != -1.0) {
      label = new JLabel(AnLocale.getString("sec."));
      AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
      label.setFont(AnTableHeaderPanel.smallFont);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX2++;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      int width = anTable.getColumnWidth()[column][1];
      label.setHorizontalAlignment(JLabel.CENTER);
      label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
      label.setMinimumSize(new Dimension(width, label.getPreferredSize().height));
      label.setMaximumSize(new Dimension(width, label.getPreferredSize().height));
      attributePanel.add(label, gridBagConstraints);
    }
    if (metricLabel.getUnit() != null) {
      String unitText = metricLabel.getUnit();
      if (!metricLabel.getAnMetric().isNameMetric() && unitText.length() == 0) {
        if (metricLabel.getAnMetric().getAux() != null
            && !metricLabel.getAnMetric().getAux().equals(metricLabel.getAnMetric().getComd())) {
          unitText = AnLocale.getString("sec.");
        } else {
          unitText = "#";
        }
      }
      label = new JLabel(unitText);
      AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
      label.setFont(AnTableHeaderPanel.smallFont);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX2++;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      int width = anTable.getColumnWidth()[column][2];
      label.setHorizontalAlignment(JLabel.CENTER);
      label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
      label.setMinimumSize(new Dimension(width, label.getPreferredSize().height));
      label.setMaximumSize(new Dimension(width, label.getPreferredSize().height));
      attributePanel.add(label, gridBagConstraints);
    }
    if (metricLabel.getTotal() != -1.0) {
      label = new JLabel("%");
      AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
      label.setFont(AnTableHeaderPanel.smallFont);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridX2++;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      int width = anTable.getColumnWidth()[column][3];
      label.setHorizontalAlignment(JLabel.CENTER);
      label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
      label.setMinimumSize(new Dimension(width, label.getPreferredSize().height));
      label.setMaximumSize(new Dimension(width, label.getPreferredSize().height));
      attributePanel.add(label, gridBagConstraints);
    }
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    add(attributePanel, gridBagConstraints);

    // Add compare group and border if in compare mode
    if (!metricLabels[column].getAnMetric().isNameMetric()) {
      String[][] groups = AnWindow.getInstance().getExperimentGroups();
      int numberOfGroups = groups.length;
      if (numberOfGroups > 1) {
        int nameColumn = anTable.getNameCol();
        int adjColumn = column > nameColumn ? column - 1 : column;
        int counter = adjColumn % numberOfGroups;
        if (anTable.getType() == AnDisplay.DSP_SourceV2
            || anTable.getType() == AnDisplay.DSP_DisassemblyV2) {
          // Treat Source view special. Source view has one seperate table per compare group and not
          // alternating columns.
          AnDisplay currentAnDisplay = AnWindow.getInstance().getViews().getCurrentViewDisplay();
          if (currentAnDisplay instanceof AnCompDisp) {
            AnCompDisp currentAnCompDisp = (AnCompDisp) currentAnDisplay;
            counter = currentAnCompDisp.getTableIndex(anTable);
          }
        }
        String compareModeText = "";
        if (AnWindow.getInstance().getSettings().getCompareModeSetting().get()
                == CompareMode.CMP_DELTA
            || AnWindow.getInstance().getSettings().getCompareModeSetting().get()
                == CompareMode.CMP_RATIO) {
          compareModeText =
              " ("
                  + AnWindow.getInstance().getSettings().getCompareModeSetting().get().toString()
                  + ")";
        }
        if (counter == 0) {
          compareLabelText = AnLocale.getString("Baseline");
          setToolTipText(AnLocale.getString("Comparison Baseline") + ": " + getToolTipText());
        } else if (counter > 0) {
          if (AnWindow.getInstance().isExperimentGroupsSimple()) {
            compareLabelText = AnLocale.getString("Exp. ") + counter + compareModeText;
            setToolTipText(
                AnLocale.getString("Comparison Experiment")
                    + " "
                    + counter
                    + ": "
                    + getToolTipText());
          } else {
            compareLabelText = AnLocale.getString("Group " + counter + compareModeText);
            setToolTipText(
                AnLocale.getString("Comparison Group") + " " + counter + ": " + getToolTipText());
          }
          setBorder(
              BorderFactory.createMatteBorder(0, 1, 0, 0, AnEnvironment.TABLE_VERTICAL_GRID_COLOR));
        } else {
          compareLabelText = "";
        }
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JLabel compareLabel = new JLabel(compareLabelText);
        AnUtility.setAccessibleContext(compareLabel.getAccessibleContext(), compareLabel.getText());
        compareLabel.setFont(AnTableHeaderPanel.verySmallFont);
        add(compareLabel, gridBagConstraints);
      }
    }

    // Sort icon
    if (anTable.canSort()) {
      if (column == anTable.getSortColumn()) {
        sortIcon = metricLabel.getSortIcon();
      } else {
        sortIcon = AnUtility.smallArrowBlankIcon;
      }
    } else {
      sortIcon = AnUtility.smallArrowBlankIcon;
    }
    sortIconLabel = new JLabel(sortIcon);
    AnUtility.setAccessibleContext(
        sortIconLabel.getAccessibleContext(), AnLocale.getString("Sort Icon"));
    if (!metricLabels[column].getAnMetric().isNameMetric()) {
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = GridBagConstraints.SOUTHEAST;
      gridBagConstraints.insets = new Insets(0, 0, 3, 3);
      add(sortIconLabel, gridBagConstraints);
    } else {
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
      gridBagConstraints.weighty = 1.0;
      gridBagConstraints.insets = new Insets(0, 3, 3, 0);
      add(sortIconLabel, gridBagConstraints);
    }

    // DnD
    addMouseMotionListener(new MouseDraggedListener());
  }

  class MouseDraggedListener extends MouseMotionAdapter {

    @Override
    public void mouseDragged(MouseEvent e) {
      JComponent c = (JComponent) e.getSource();
      AttributePanel attributePanel = (AttributePanel) c;
      MetricPanel metricPanel = attributePanel.getMetricPanel();
      TransferHandler handler = metricPanel.getTransferHandler();
      handler.exportAsDrag(metricPanel, e, TransferHandler.COPY);
    }
  }

  public MetricPanel getMetricPanel() {
    return metricPanel;
  }

  private void changeSortIcon(ImageIcon newIcon) {
    if (true || column != anTable.getSortColumn()) {
      if (sortIcon != newIcon) {
        sortIcon = newIcon;
        sortIconLabel.setIcon(sortIcon);
      }
    }
  }

  public void mouseEntered(MouseEvent e) {
    //            System.out.println("AttributePanel.mouseEntered " + column);
    if (anTable.canSort()) {
      if (column == anTable.getSortColumn()) {
        if (sortIcon == AnUtility.smallArrowDownIcon) {
          sortIconLabel.setIcon(AnUtility.smallArrowUpFocusedIcon);
        } else {
          sortIconLabel.setIcon(AnUtility.smallArrowDownFocusedIcon);
        }

      } else {
        sortIconLabel.setIcon(AnUtility.smallArrowDownFocusedIcon);
      }
    }
  }

  public void mouseExited(MouseEvent e) {
    //            System.out.println("AttributePanel.mouseExited " + column);
    if (anTable.canSort()) {
      if (column == anTable.getSortColumn()) {
        sortIconLabel.setIcon(sortIcon);
      } else {
        sortIconLabel.setIcon(AnUtility.smallArrowBlankIcon);
      }
    }
  }

  public void mouseClicked(MouseEvent e) {
    //            System.out.println("AttributePanel.mouseClicked " + column);
    anTable.sortTable(column);
  }

  public JMenuItem getSortByMenuItem() {
    String text = "";
    text = exclusiveOrInclusivePanel.getLabelText();
    if (compareLabelText != null && compareLabelText.length() > 0) {
      text += "  " + compareLabelText;
    }
    JMenuItem menuItem = new JMenuItem(text);
    menuItem.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance()
                .getSettings()
                .getMetricsSetting()
                .setSortMetricByDType(this, column, anTable.getType());
          }
        });
    return menuItem;
  }
}
