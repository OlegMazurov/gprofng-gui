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

package org.gprofng.mpmt;

// Analyzer Double object
import static org.gprofng.mpmt.AnObject.format_group_decimal;
import static org.gprofng.mpmt.AnObject.format_sdecimal;
import static org.gprofng.mpmt.AnObject.zero_decimal;

public final class AnDouble extends AnObject implements Comparable<AnDouble> {

  private final double value;
  private final Double obj;

  // Constructor
  public AnDouble(double value) {
    this.value = value;
    obj = value;
  }

  // Analyzer Double printing format
  @Override
  public String toString() {
    if (value > 99.999 && xtimes.compareTo("") != 0) {
      return ">99.999" + xtimes;
    }
    if (value == 0.0) {
      return get_zero_decimal() + xtimes;
    }
    if (sign) {
      return format_sdecimal.format(value) + xtimes;
    }
    return format_decimal.format(value) + xtimes;
  }

  // Analyzer formatted printing format
  @Override
  public String toFormString() {
    if (value > 99.999 && xtimes.compareTo("") != 0) {
      return ">99.999" + xtimes;
    }
    if (value == 0.0) {
      if (showZero) {
        return zero_decimal + xtimes;
      } else {
        return quote_space + xtimes;
      }
    }
    if (sign) {
      return format_group_sdecimal.format(value) + xtimes;
    }
    return format_group_decimal.format(value) + xtimes;
  }

  // Percent printing
  @Override
  public String toPercent(double total) {
    if (value == 0.0) {
      if (showZero) {
        return zero_percent;
      } else {
        return quote_space;
      }
    }
    double percent = value * total;
    if (percent > 100.0) {
      percent = 100.0;
    }
    return format_percent.format(percent);
  }

  // To Double
  public Double toDouble() {
    return obj;
  }

  // To double
  @Override
  public double doubleValue() {
    return value;
  }

  // As Double.compareTo
  @Override
  public int compareTo(AnDouble o) {
    return obj.compareTo(o.toDouble());
  }

  // Convert double[] to AnDouble[]
  public static AnDouble[] toArray(double[] list) {
    int length = list.length;
    AnDouble[] new_list = new AnDouble[length];

    for (int i = 0; i < length; i++) {
      new_list[i] = new AnDouble(list[i]);
    }

    return new_list;
  }
}
