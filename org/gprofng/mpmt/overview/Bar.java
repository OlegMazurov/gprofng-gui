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

package org.gprofng.mpmt.overview;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.metrics.CompareBarData;
import org.gprofng.mpmt.metrics.MetricValue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

public class Bar extends JPanel {

  protected static final int BAR_MAX_PERCENT = 150;
  public static final Color[] BAR_COMPARE_COLORS =
      new Color[] {AnEnvironment.BAR_COMPARE_COLOR1, AnEnvironment.BAR_COMPARE_COLOR2};
  protected static final int barWidth = 200;
  public static final int barHeight = 13;
  private int panelWidth = barWidth;
  private int panelHeight = barHeight;
  private Font smallFont;
  private boolean showBorder = true;
  private boolean showFillColor = true;
  private boolean paintNumber = true;
  private CompareBarData[] compareValues = null;
  private List<ValueColor> valueColor;
  private int value;

  public Bar(Color color, int value, CompareBarData[] compareValues) {
    List<ValueColor> vc = new ArrayList<ValueColor>();
    vc.add(new ValueColor(value, color));
    init(vc, value, compareValues);
    //        paintNumber = false;
    //        showBorder = false;
  }

  public Bar(ValueColor[] valueColor, int value, CompareBarData[] compareValues) {
    init(new ArrayList<ValueColor>(Arrays.asList(valueColor)), value, compareValues);
  }

  public Bar(List<ValueColor> valueColor, int value, CompareBarData[] compareValues) {
    init(valueColor, value, compareValues);
  }

  private void init(List<ValueColor> valueColor, int value, CompareBarData[] compareValues) {
    if (compareValues == null) {
      panelHeight = barHeight;
      panelWidth = barWidth;
    } else {
      panelHeight = barHeight + compareValues.length * barHeight - 2;
      panelWidth = calculateBarWidth(compareValues);
    }
    setPreferredSize(new Dimension(panelWidth, panelHeight));
    //        smallFont = getFont().deriveFont((float) getFont().getSize() - 2);
    smallFont =
        getFont().deriveFont((float) 10); // Always use size 10 font to paint numbers (for now!)
    this.valueColor = valueColor;
    this.value = value;
    this.compareValues = compareValues;
    setOpaque(false);
  }

  private int calculateBarWidth(CompareBarData[] compareValues) {
    int maxX = barWidth;
    if (compareValues != null) {
      for (int compareBarNo = 0; compareBarNo < compareValues.length; compareBarNo++) {
        //                ValueColor compareValueColor =
        // compareValues[compareBarNo].getValueColors().get(0); // << FIXUP
        int value = compareValues[compareBarNo].getPercentOfTotal();
        if (value == MetricValue.PERCENT_NAN || value >= BAR_MAX_PERCENT) {
          value = BAR_MAX_PERCENT;
        }
        int vbarValue = (int) (barWidth * value) / 100;
        int fillValue = vbarValue;
        if (fillValue <= 1) {
          fillValue = 1;
        }
        maxX = ((1 + fillValue) > maxX ? (1 + fillValue) : maxX);
      }
    }
    return maxX;
  }

  public int noCompareBars() {
    if (compareValues == null) {
      return 0;
    } else {
      return compareValues.length;
    }
  }

  // @Override
  @Override
  public void paint(Graphics g) {
    paint(g, 0, 0);
  }

  public void paint(Graphics g, int xOffset, int yOffset) {
    super.paint(g);
    int maxX = barWidth;
    int x = xOffset;
    int y = yOffset;
    if (showFillColor) {
      g.setColor(AnEnvironment.BAR_BACKGROUND_COLOR);
      g.fillRect(x, y, barWidth - 1, barHeight - 1);
    }
    if (showBorder) {
      g.setColor(AnEnvironment.BAR_BORDER_COLOR);
      g.drawRect(x, y, barWidth - 1, barHeight - 1);
    }
    x = x + 1;
    for (ValueColor vc : getValueColor()) {
      int vbarValue;

      // is value of main bar 0?
      if (noCompareBars() > 0 && compareValues[0].getPercentOfTotal() == MetricValue.PERCENT_NAN) {
        vbarValue = 0; // YXXX For now, show zero value for main bar (Thomas: TBD)
      } else {
        vbarValue = (int) (barWidth * vc.getValue()) / 100;
      }

      g.setColor(vc.getColor());
      int fillValue = vbarValue;
      if (fillValue <= 1) {
        fillValue = 1;
      }
      g.fillRect(x, y + 1, fillValue, barHeight - 2);
      x += vbarValue;
    }

    for (int compareBarNo = 0; compareBarNo < noCompareBars(); compareBarNo++) {
      int combinedValue = 0;
      int compareX = xOffset + 1;
      for (ValueColor compareValueColor : compareValues[compareBarNo].getValueColors()) {
        g.setColor(compareValueColor.color);
        int value;
        boolean maxReached = false;
        if (compareValues[compareBarNo].getPercentOfTotal() == MetricValue.PERCENT_NAN) {
          maxReached = true;
          value = BAR_MAX_PERCENT - combinedValue;
        } else {
          value = compareValueColor.value * compareValues[compareBarNo].getPercentOfTotal() / 100;
          combinedValue += value;
          if (value == MetricValue.PERCENT_NAN || combinedValue >= BAR_MAX_PERCENT) {
            maxReached = true;
            value = BAR_MAX_PERCENT - (combinedValue - value);
          }
        }
        int vbarValue = (int) (barWidth * value) / 100;
        int borderOffset = 1;
        if (showBorder) {
          g.setColor(AnEnvironment.BAR_BORDER_COLOR);
          g.drawRect(xOffset, y + barHeight + compareBarNo * (barHeight - 2), 0, barHeight - 3);
          borderOffset = 0;
        }

        g.setColor(compareValueColor.color);
        int fillValue = vbarValue;
        if (fillValue <= 1) {
          fillValue = 1;
        }
        g.fillRect(
            compareX,
            y + barHeight + compareBarNo * (barHeight - 1) - borderOffset,
            fillValue,
            barHeight - 2);

        if (maxReached) {
          int nearEnd = BAR_MAX_PERCENT * barWidth / 100;
          nearEnd -= 15;
          g.setColor(Color.WHITE);
          for (int i = 0; i < 5; i++) {
            compareX = xOffset + 1 + nearEnd;
            int yy = y + barHeight + compareBarNo * (barHeight - 2) - 1;
            g.drawLine(compareX + i, yy, compareX - 20 + i, yy + yy + barHeight - 2);
          }
        }
        maxX = ((compareX + fillValue) > maxX ? (compareX + fillValue) : maxX);
        compareX += vbarValue;
      }
    }

    //        panelWidth = maxX;
    //        Dimension dim = getPreferredSize();
    //        dim = new Dimension(panelWidth, dim.height);
    //        setPreferredSize(dim);

    if (paintNumber) {
      g.setFont(smallFont);
      String valueString = "" + getValue() + "%";
      int valueWidthInPixels = pixelWidth(g, valueString);

      g.setClip(0, 0, x, y + barHeight);
      g.setColor(Color.WHITE);
      g.drawString(valueString, xOffset + barWidth - valueWidthInPixels - 2, y + 10);

      g.setClip(x, 0, x + barWidth, y + barHeight);
      g.setColor(Color.BLACK);
      g.drawString(valueString, xOffset + barWidth - valueWidthInPixels - 2, y + 10);
    }
  }

  private int pixelWidth(Graphics g, String s) {
    FontMetrics fontMetrics = g.getFontMetrics();
    return fontMetrics.stringWidth(s);
  }

  /**
   * @return the value
   */
  public int getValue() {
    return value;
  }

  public int getPanelWidth() {
    return panelWidth;
  }

  /**
   * @return the valueColor
   */
  public List<ValueColor> getValueColor() {
    return valueColor;
  }

  public static class ValueColor {

    private int value;
    private Color color;

    public ValueColor(int value, Color color) {
      this.value = value;
      this.color = color;
    }

    /**
     * @return the value
     */
    public int getValue() {
      return value;
    }

    /**
     * @return the color
     */
    public Color getColor() {
      return color;
    }
  }
}
