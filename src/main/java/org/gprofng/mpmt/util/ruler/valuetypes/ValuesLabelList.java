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

package org.gprofng.mpmt.util.ruler.valuetypes;

/** Display formatting for any text-based values such (e.g. function name) */
public class ValuesLabelList implements ValuesGeneric {
  private long low;
  private long high;
  private String labels[];

  /** Instantiate a new GenericValueType */
  public ValuesLabelList(String labels[]) {
    this.labels = labels;
  }

  /** Display as the value converted to a string */
  public String getFormattedValue(long value) {
    if (value >= 0 && value < labels.length) {
      int idx = (int) value;
      return labels[idx]; // YXXX could show a range in the future
    } else {
      return "";
    }
  }

  /** No units for generic data */
  public String getUnitString() {
    return null;
  }

  /** Display as the value converted to a string */
  public String getFormattedValue(long value, long range) {
    return getFormattedValue(value);
  }

  /** Return the low value index */
  public long getLowValue() {
    return low;
  }

  /** Return the high value index */
  public long getHighValue() {
    return high;
  }

  private int limit(long value) {
    int rc;
    if (value < 0) {
      rc = 0;
    } else if (value >= labels.length) {
      rc = labels.length - 1;
    } else {
      rc = (int) value;
    }
    return rc;
  }

  /** Set the range */
  public void setRange(long v1, long v2) {
    if (v1 < v2) {
      low = limit(v1);
      high = limit(v2);
    } else {
      low = limit(v2);
      high = limit(v1);
    }
  }
}
