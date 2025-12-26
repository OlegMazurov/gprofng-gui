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

import java.util.Formatter;

public class MetricValue {
  public static final int PERCENT_NAN = Integer.MAX_VALUE;

  public enum ValueType {
    LONG,
    DOUBLE,
    LABEL_ONLY /* no value or unit */
  };

  private String label; // "I/O Bytes"
  private double dValue; // 12.345
  private long lValue; // 12345
  private final ValueType valueType; // LONG/DOUBLE
  private final String unit; // "MB"
  private boolean highlight; // Metric value is "hot"

  public MetricValue(String label, long lValue, ValueType valueType, String unit) {
    this.label = label;
    this.lValue = lValue;
    this.dValue = 0;
    this.valueType = valueType;
    this.unit = unit;
    if (valueType != ValueType.LONG) {
      System.err.println("MetricValue error: value doesn't match value type!");
    }
    highlight = false;
  }

  public MetricValue(String label, double dValue, ValueType valueType, String unit) {
    this.label = label;
    this.lValue = 0;
    this.dValue = dValue;
    this.valueType = valueType;
    this.unit = unit;
    if (valueType != ValueType.DOUBLE) {
      System.err.println("MetricValue error: value doesn't match value type!");
    }
    highlight = false;
  }

  public MetricValue(String label, ValueType valueType) {
    this.label = label;
    this.lValue = 0;
    this.dValue = 0;
    this.valueType = valueType;
    this.unit = null;
    if (valueType != ValueType.LABEL_ONLY) {
      System.err.println("MetricValue error: value doesn't match value type!");
    }
    highlight = false;
  }

  public void setHighlight(boolean highlight) {
    this.highlight = highlight;
  }

  public boolean getHighlight() {
    return highlight;
  }

  public String getLabel() {
    return label;
  }

  public double getDValue() {
    return dValue;
  }

  public void setDValue(double value) {
    this.dValue = value;
    // Reformat label, if necessary
    // Hack. See 18378636 - Overview is insufficient to pick metrics for er_kernel experiments
    if (valueType == ValueType.LABEL_ONLY) {
      String valueString = new Formatter().format("%.3f", getDValue()).toString();
      if (label != null && !label.startsWith(valueString)) {
        int spaceIndex = label.lastIndexOf(' ');
        if (spaceIndex > 0) {
          label = valueString + label.substring(spaceIndex);
        }
      }
    }
  }

  public long getLValue() {
    return lValue;
  }

  public void setLValue(long lValue) {
    this.lValue = lValue;
  }

  public ValueType getValueType() {
    return valueType;
  }

  public String getUnit() {
    return unit;
  }

  public String formatValue() {
    String value = "";
    if (null != getValueType()) {
      switch (getValueType()) {
        case LONG:
          value = "" + getLValue();
          break;
        case DOUBLE:
          value = new Formatter().format("%.3f", getDValue()).toString();
          break;
        case LABEL_ONLY:
          value = ""; // nothing to add
          break;
        default:
          value = "";
          assert false; // should not happen
          break;
      }
    }
    return value;
  }

  public String formatValueUnit() {
    String valueUnit = formatValue();
    if (getUnit() != null) {
      valueUnit += " " + getUnit();
    }
    return valueUnit;
  }

  public boolean gtZero() {
    if (null != getValueType()) {
      switch (getValueType()) {
        case DOUBLE:
          return getDValue() > 0;
        case LONG:
          return getLValue() > 0;
        case LABEL_ONLY:
          return false;
        default:
          System.err.println("MetricValue:percentOf() not handling type " + getValueType());
          assert false;
          break;
      }
    }
    return false;
  }

  public boolean ltZero() {
    if (null != getValueType()) {
      switch (getValueType()) {
        case DOUBLE:
          return getDValue() < 0;
        case LONG:
          return getLValue() < 0;
        case LABEL_ONLY:
          return false;
        default:
          System.err.println("MetricValue:percentOf() not handling type " + getValueType());
          assert false;
          break;
      }
    }
    return false;
  }

  public void subtract(MetricValue opNode) {
    if (null != getValueType()) {
      switch (getValueType()) {
        case DOUBLE:
          dValue -= opNode.getDValue();
          break;
        case LONG:
          lValue -= opNode.getLValue();
          break;
        case LABEL_ONLY:
          break;
        default:
          System.err.println("MetricValue:percentOf() not handling type " + getValueType());
          assert false;
          break;
      }
    }
  }

  public int percentOf(MetricValue totalValueNode) {
    int percent = 0;
    if (null != getValueType()) {
      switch (getValueType()) {
        case DOUBLE:
          {
            double totalValue = totalValueNode.getDValue();
            double childValue = getDValue();
            if (totalValue == 0) {
              if (childValue == 0) {
                percent = 0;
              } else {
                percent = PERCENT_NAN;
              }
            } else {
              percent =
                  (int)
                      ((childValue * 100) / totalValue
                          + 0.5f); // YXXX Thomas, comment on why add 0.5 to a double?
            }
            break;
          }
        case LONG:
          {
            long totalValue = totalValueNode.getLValue();
            long childValue = getLValue();
            if (totalValue == 0) {
              if (childValue == 0) {
                percent = 0;
              } else {
                percent = PERCENT_NAN;
              }
            } else {
              percent =
                  (int)
                      ((childValue * 100) / totalValue
                          + 0.5f); // YXXX don't you want to convert to double before division?
            }
            break;
          }
        case LABEL_ONLY:
          percent = PERCENT_NAN;
          break;
        default:
          System.err.println("MetricValue:percentOf() not handling type " + getValueType());
          assert false;
          break;
      }
    }

    return percent;
  }

  public MetricValue clone() {
    MetricValue clone = null;

    if (null != getValueType()) {
      switch (getValueType()) {
        case DOUBLE:
          clone = new MetricValue(getLabel(), getDValue(), getValueType(), getUnit());
          break;
        case LONG:
          clone = new MetricValue(getLabel(), getLValue(), getValueType(), getUnit());
          break;
        case LABEL_ONLY:
          clone = new MetricValue(getLabel(), getValueType());
          break;
        default:
          System.err.println("MetricValue:clone() error");
          assert false;
          break;
      }
    }
    return clone;
  }
}
