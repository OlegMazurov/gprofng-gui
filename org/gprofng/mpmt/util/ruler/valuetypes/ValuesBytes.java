/* Copyright (C) 2022 Free Software Foundation

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

import org.gprofng.mpmt.AnLocale;

/** This class provides unit strings for a number of bytes that fall withing a specified range. */
public class ValuesBytes extends ValuesWithUnits {

  /** Instantiate a new ValuesBytes object with a specified range */
  public ValuesBytes(long low, long high) {
    super(low, high);
  }

  public String getUnitString() {
    String units = getUnitString(absMax);
    return units;
  }

  public long getUnitDivisor() {
    long uDiv = getUnitDivisor(absMax);
    return uDiv;
  }

  /** Get the Unit String to use */
  public String getUnitString(long value) {
    String units;
    if (value < 1000L) {
      units = AnLocale.getString("Bytes"); // NOI18N
    } else if (value < 1000000L) {
      units = AnLocale.getString("KB"); // NOI18N
    } else if (value < 1000000000L) {
      units = AnLocale.getString("MB"); // NOI18N
    } else {
      units = AnLocale.getString("GB"); // NOI18N
    }
    return units;
  }

  private long getUnitDivisor(long value) {
    long divisor;
    if (value < 1000L) {
      divisor = 1L;
    } else if (value < 1000000L) {
      divisor = 1000L;
    } else if (value < 1000000000L) {
      divisor = 1000000L;
    } else {
      divisor = 1000000000L;
    }
    return divisor;
  }
}
