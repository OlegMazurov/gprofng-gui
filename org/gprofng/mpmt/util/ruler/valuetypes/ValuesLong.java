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

/** Display formatting for generic values such as a count. */
public class ValuesLong implements ValuesGeneric {
  private long low;
  private long high;

  /** Instantiate a new ValuesLong */
  public ValuesLong(long low, long high) {
    setRange(low, high);
  }

  /** Display as the value converted to a string */
  public String getFormattedValue(long value) {
    return Long.toString(value);
  }

  /** No units for generic data */
  public String getUnitString() {
    return null;
  }

  /** Display as the value converted to a string */
  public String getFormattedValue(long value, long range) {
    return getFormattedValue(value);
  }

  /** Return the low value */
  public long getLowValue() {
    return low;
  }

  /** Return the high value */
  public long getHighValue() {
    return high;
  }

  /** Set the range */
  public void setRange(long v1, long v2) {
    if (v1 < v2) {
      low = v1;
      high = v2;
    } else {
      low = v2;
      high = v1;
    }
  }
}
