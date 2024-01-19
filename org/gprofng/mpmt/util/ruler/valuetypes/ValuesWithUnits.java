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

package org.gprofng.mpmt.util.ruler.valuetypes;

import java.text.DecimalFormat;

/*
 * The ValuesWithUnits stores a range for values that can be expressed
 * with modifiers like kilo or nano.
 * Values are of type long and must be stored in the highest-resolusion.
 */
public abstract class ValuesWithUnits implements ValuesGeneric {
  protected long low;
  protected long high;
  protected long absMax; // maximum absolute value

  /** Instantiate a new ValuesWithUnits object */
  public ValuesWithUnits(long v1, long v2) {
    setRange(v1, v2);
  }

  /*
   * Get the units' string.
   * Example: value units = bytes -> Bytes, KB, MB, ...
   * Example: value units = nanoseconds -> Nanoseconds, Milliseconds...
   * Assumes <value> is in highest-resolusion units.
   */
  public abstract String getUnitString();

  /*
   * Get the divisor required to convert to the units
   * supplied by getUnitString().
   */
  public abstract long getUnitDivisor();

  /*
   * Format a value using <range> to set number of sig digits
   */
  public String getFormattedValue(long value, long range) {
    String myformat = "###,###,###,###,###,##0";

    // convert origValue to desired units
    long uDiv = getUnitDivisor();
    double uValue = value;
    uValue /= uDiv;

    // calculate # of places to right of decimal point required to show
    // MSD of <range>
    range = Math.abs(range); // ensure range>=1
    range = Math.max(1, range);
    if (range < uDiv) {
      // range is a fraction of uDiv
      long tmp = range;
      int decimalPlaces = 0;
      while (tmp < uDiv) {
        tmp *= 10;
        decimalPlaces++;
      }
      String decimalfmt = ".000000000000000000000000000";
      myformat = myformat + decimalfmt.substring(0, decimalPlaces + 1);
    }

    // generate string
    DecimalFormat df = new DecimalFormat(myformat);
    // bd = bd.setScale(decimalPlaces,BigDecimal.ROUND_HALF_UP);
    String text = df.format(uValue);
    return text;
  }

  /** Format a value based on default setting */
  public String getFormattedValue(long value) {
    long range = high - low + 1;
    String ss = getFormattedValue(value, range);
    return ss;
  }

  /** Get the low value */
  public long getLowValue() {
    return low;
  }

  /** Get the high value */
  public long getHighValue() {
    return high;
  }

  /** Set the range of values */
  public void setRange(long v1, long v2) {
    if (v1 < v2) {
      low = v1;
      high = v2;
    } else {
      low = v2;
      high = v1;
    }
    absMax = 0;
    long tmp = Math.abs(v1);
    if (absMax < tmp) {
      absMax = tmp;
    }
    tmp = Math.abs(v2);
    if (absMax < tmp) {
      absMax = tmp;
    }
  }
}
