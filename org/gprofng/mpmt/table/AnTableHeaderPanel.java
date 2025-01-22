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
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnTable;
import org.gprofng.mpmt.metrics.MetricLabel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AnTableHeaderPanel extends JPanel {
  protected static final Font smallFont =
      new JLabel().getFont().deriveFont((float) new JLabel().getFont().getSize() - 1);
  protected static final Font verySmallFont =
      new JLabel().getFont().deriveFont((float) new JLabel().getFont().getSize() - 2);
  protected static final Font boldFont = new JLabel().getFont().deriveFont(Font.BOLD);
  protected static final Font smallBoldFont = boldFont.deriveFont((float) boldFont.getSize() - 1);
  protected static final Font verySmallBoldFont = verySmallFont.deriveFont(Font.BOLD);
  protected static final JLabel testLabel = new JLabel();

  static {
    testLabel.setFont(boldFont);
  }

  private List<MetricPanel> metricPanels = new ArrayList<MetricPanel>();
  private List<Integer> startColumns = new ArrayList<Integer>();
  private List<Integer> columnCounts = new ArrayList<Integer>();
  private List<Integer> panelWidths = new ArrayList<Integer>();

  private int dropPosition = -1;

  public AnTableHeaderPanel(AnTable anTable, MetricLabel[] metricLabels, boolean wrapMetricName) {
    metricPanels = new ArrayList<MetricPanel>();
    startColumns = new ArrayList<Integer>();
    columnCounts = new ArrayList<Integer>();
    panelWidths = new ArrayList<Integer>();

    int[][] columnWidth = anTable.getColumnWidth();

    setBackground(AnEnvironment.TABLE_HEADER_BACKGROUND_COLOR_1);
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;

    int panelCount = 0;
    int gridX = 0;
    int startColumn = 0;
    while (startColumn < metricLabels.length) {
      int count = 1;
      while (startColumn + count < metricLabels.length
          && metricLabels[startColumn + count - 1]
              .getAnMetric()
              .getComd()
              .equals(metricLabels[startColumn + count].getAnMetric().getComd())) {
        count++;
      }
      int panelWidth = 0;
      for (int i = startColumn; i < startColumn + count; i++) {
        panelWidth += columnWidth[i][0];
      }
      Color backgroundcolor;
      if (panelCount % 2 == 0) {
        backgroundcolor = AnEnvironment.TABLE_HEADER_BACKGROUND_COLOR_1;
      } else {
        backgroundcolor = AnEnvironment.TABLE_HEADER_BACKGROUND_COLOR_2;
      }

      if (metricLabels[startColumn].getAnMetric().isNameMetric()) {
        MetricPanel metricPanel =
            new MetricPanel(
                this,
                metricLabels[startColumn].getAnMetric(),
                anTable,
                metricLabels,
                wrapMetricName,
                startColumn,
                count,
                backgroundcolor);
        //                metricPanel.setPreferredSize(new Dimension(panelWidth,
        // metricPanel.getPreferredSize().height));
        gridBagConstraints.gridx = startColumn;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(metricPanel, gridBagConstraints);
        metricPanels.add(metricPanel);
        startColumns.add(startColumn);
        columnCounts.add(count);
        panelWidths.add(panelWidth);
      } else {
        MetricPanel metricPanel =
            new MetricPanel(
                this,
                metricLabels[startColumn].getAnMetric(),
                anTable,
                metricLabels,
                wrapMetricName,
                startColumn,
                count,
                backgroundcolor);
        //                metricPanel.setPreferredSize(new Dimension(panelWidth,
        // metricPanel.getPreferredSize().height));
        gridBagConstraints.gridx = startColumn;
        add(metricPanel, gridBagConstraints);
        add(metricPanel, gridBagConstraints);
        metricPanels.add(metricPanel);
        startColumns.add(startColumn);
        columnCounts.add(count);
        panelWidths.add(panelWidth);
      }

      startColumn += count;
      panelCount++;
    }

    // Deal with long metric names
    if (wrapMetricName) {
      int noMetricNameRows = 0;
      for (int i = 0; i < metricPanels.size(); i++) {
        MetricPanel metricPanel = metricPanels.get(i);
        JPanel iconNamePanel = metricPanel.getIconNamePanel();
        int column = startColumns.get(i);
        int columnCount = columnCounts.get(i);
        int actualColumnWidth = 0;
        for (int n = 0; n < columnCount; n++) {
          actualColumnWidth += columnWidth[column + n][0];
        }
        boolean nameTooLong = iconNamePanel.getPreferredSize().width >= actualColumnWidth;
        //                System.out.println(column + ":" + columnCount + ": " +
        // iconNamePanel.getPreferredSize().width + " " + actualColumnWidth + " " + (nameTooLong ?
        // "*" : ""));
        if (nameTooLong) {
          int rows = metricPanel.splitMetricNameIntoMultipleRows(actualColumnWidth);
          if (rows > noMetricNameRows) {
            noMetricNameRows = rows;
          }
        }
      }
      if (noMetricNameRows > 0) {
        for (int i = 0; i < metricPanels.size(); i++) {
          MetricPanel metricPanel = metricPanels.get(i);
          metricPanel.adjustMetricNameRows(noMetricNameRows);
        }
      }
    }

    // Set width...
    for (int i = 0; i < metricPanels.size(); i++) {
      MetricPanel metricPanel = metricPanels.get(i);
      metricPanel.setPreferredSize(
          new Dimension(panelWidths.get(i), metricPanel.getPreferredSize().height));
    }
  }

  public static int getMinimumColumnWidth() {
    JLabel label = new JLabel(AnLocale.getString("EXCLUSIVE"));
    AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
    label.setFont(AnTableHeaderPanel.verySmallFont);
    return label.getPreferredSize().width + 26;
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    if (dropPosition >= 0) {
      paintDropPosition(g, dropPosition);
    }
  }

  private void paintDropPosition(Graphics g, int dropPosition) {
    int lineCenter = 0;

    if (dropPosition == 0) {
      lineCenter = 0;
    } else {
      for (int i = 0; i < dropPosition; i++) {
        lineCenter += panelWidths.get(i);
      }
      lineCenter -= 1;
    }

    int h = getHeight();

    g.setColor(AnEnvironment.DROP_LINE_COLOR);
    g.drawLine(lineCenter - 1, 0, lineCenter - 1, h);
    g.drawLine(lineCenter, 0, lineCenter, h);
    g.drawLine(lineCenter + 1, 0, lineCenter + 1, h);

    g.drawLine(lineCenter - 3, 0, lineCenter + 3, 0);
    g.drawLine(lineCenter - 2, 1, lineCenter + 2, 1);
    g.drawLine(lineCenter - 2, h - 2, lineCenter + 2, h - 2);
    g.drawLine(lineCenter - 3, h - 1, lineCenter + 3, h - 1);
  }

  public void setDropPosition(int dropPosition) {
    if (dropPosition != this.dropPosition) {
      this.dropPosition = dropPosition;
      repaint();
    }
  }

  public void setDropLocation(int xOnScreen) {
    int x = xOnScreen - getLocationOnScreen().x; // Relative to anTableHeaderPanel
    int dropPosition = 0;
    if (x < panelWidths.get(0) / 2) {
      dropPosition = 0;
    } else {
      int startX = panelWidths.get(0) / 2;
      int totalPanelWidth = panelWidths.get(0);
      dropPosition = 1;
      for (int panelNo = 1; panelNo < panelWidths.size(); panelNo++) {
        if (x >= startX && x < totalPanelWidth + panelWidths.get(panelNo) / 2) {
          break;
        }
        startX = totalPanelWidth + panelWidths.get(panelNo) / 2;
        totalPanelWidth += panelWidths.get(panelNo);
        dropPosition++;
      }
    }
    setDropPosition(dropPosition);

    Rectangle visibleRectangle = getVisibleRect();
    if (x < visibleRectangle.x + 50) {
      scrollRectToVisible(new Rectangle(visibleRectangle.x - 50, 1, x, 1));
    } else if (x > visibleRectangle.x + visibleRectangle.width - 50) {
      scrollRectToVisible(new Rectangle(visibleRectangle.x + 50, 1, x, 1));
    }
  }
}
