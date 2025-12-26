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

package org.gprofng.mpmt.metrics;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnObject;
import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import org.gprofng.mpmt.util.gui.AnUtility;
import javax.swing.ImageIcon;
import static org.gprofng.mpmt.util.gui.AnUtility.SPACES_BETWEEN_COLUMNS;

public class MetricLabel {

  private ImageIcon icon;
  private ImageIcon sortIcon;
  private String[] titleLines;
  private String tip;
  private String unit;
  private double clock;
  private double total;
  private AnObject maxAnObject;
  private AnMetric anMetric;

  public MetricLabel(
      AnMetric anMetric,
      String tip,
      String unit,
      double clock,
      double total,
      AnObject maxAnObject,
      ImageIcon sortIcon) {
    this.anMetric = anMetric;
    this.tip = tip;
    this.unit = unit;
    this.clock = clock;
    this.total = total;
    this.maxAnObject = maxAnObject;
    this.sortIcon = sortIcon;
    titleLines = new String[2];
    titleLines[1] = anMetric.getUserName();
    String compareModifier = "";
    if (anMetric.getCompareMode() == CompareMode.CMP_DELTA) {
      compareModifier = " " + AnLocale.getString("Delta");
    } else if (anMetric.getCompareMode() == CompareMode.CMP_RATIO) {
      compareModifier = " " + AnLocale.getString("Ratio");
    }

    if (this.anMetric.getSubType() == AnMetric.EXCLUSIVE) {
      titleLines[0] = AnLocale.getString("Exclusive") + compareModifier;
      icon = AnUtility.excl_icon;
    } else if (this.anMetric.getSubType() == AnMetric.INCLUSIVE) {
      titleLines[0] = AnLocale.getString("Inclusive") + compareModifier;
      icon = AnUtility.incl_icon;
    } else if (this.anMetric.getSubType() == 8) { // 8 = ATTRIBUTED
      titleLines[0] = AnLocale.getString("Attributed") + compareModifier;
      icon = null;
    } else {
      titleLines[0] = "";
      icon = null;
    }
  }

  public String getText() {
    return getTip();
  }

  public final ImageIcon getIcon() {
    return icon;
  }

  public final String[] getLegendAndTitleLines() {
    if (getAnMetric() != null) {
      if (getAnMetric().legend != null && getAnMetric().legend.length() > 0) {
        String[] t = new String[getTitleLines().length + 1];
        t[0] = getAnMetric().legend;
        for (int i = 0; i < getTitleLines().length; i++) {
          t[i + 1] = getTitleLines()[i];
        }
        return t; // title
      }
    }
    return getTitleLines();
  }

  public AnMetric getAnMetric() {
    return anMetric;
  }

  public double getClock() {
    return clock;
  }

  public String getUnit() {
    return unit;
  }

  public ImageIcon getSortIcon() {
    return sortIcon;
  }

  public String[] getTitleLines() {
    return titleLines;
  }

  public String getTip() {
    return tip;
  }

  public double getTotal() {
    return total;
  }

  public AnObject getMaxAnObject() {
    return maxAnObject;
  }

  /**
   * @param maxAnObject the maxAnObject to set
   */
  public void setMaxAnObject(AnObject maxAnObject) {
    this.maxAnObject = maxAnObject;
  }

  private int columnWidth = 0;
  private int headerWidth = 0;
  private int valWidth = 0;
  private int percentWidth = 0;

  public void init_width() {
    if (columnWidth > 0) {
      return;
    }
    AnMetric m = getAnMetric();
    AnObject obj = getMaxAnObject();
    if (m.isTVisible() || m.isVVisible()) {
      valWidth = obj.toString().length();
      if (unit != null && valWidth < unit.length()) {
        valWidth = unit.length();
      }
    }
    if (m.isPVisible()) {
      if (valWidth > 0) {
        columnWidth += SPACES_BETWEEN_COLUMNS;
      }
      percentWidth = obj.toPercent(getTotal()).length();
    }
    columnWidth += valWidth + percentWidth;

    if (unit != null) {
      headerWidth = unit.length();
    }
    String[] titles = getLegendAndTitleLines();
    for (int i = 0; i < titles.length; i++) {
      if (headerWidth < titles[i].length()) {
        headerWidth = titles[i].length();
      }
    }
    if (columnWidth < headerWidth) {
      columnWidth = headerWidth;
    }
  }
  
  private int max_val(int i1, int i2, int i3) {
    int cnt = i1;
    if (cnt < i2) {
      cnt = i2;
    }
    return i3 > cnt ? i3 : cnt;
  }

  // Make the caller and callee views the same width
  public void updateWidth(MetricLabel m1, MetricLabel m2) {
    init_width();
    m1.init_width();
    m2.init_width();
    valWidth = max_val(valWidth, m1.valWidth, m2.valWidth);
    percentWidth = max_val(percentWidth, m1.percentWidth, m2.percentWidth);
    headerWidth = max_val(headerWidth, m1.headerWidth, m2.headerWidth);
    columnWidth = valWidth + percentWidth;
    if (valWidth > 0) {
      columnWidth += SPACES_BETWEEN_COLUMNS;
    }
    if (columnWidth < headerWidth) {
      columnWidth = headerWidth;
    }
    m1.valWidth = m2.valWidth = valWidth;
    m1.percentWidth = m2.percentWidth = percentWidth;
    m1.headerWidth = m2.headerWidth = headerWidth;
    m1.columnWidth = m2.columnWidth = columnWidth;
  }

  public int getValWidth() {
    init_width();
    return valWidth;
  }

  public int getPercentWidth() {
    init_width();
    return percentWidth;
  }

  public int getHeaderWidth() {
    init_width();
    return headerWidth;
  }

  public int getColumnWidth() {
    init_width();
    return columnWidth;
  }

  public void dump() {
    System.out.print("MetricLabel: ");
    for (int i = 0; i < titleLines.length; i++) {
      System.out.print(titleLines[i] + " ");
    }
    System.out.println();
  }
}
