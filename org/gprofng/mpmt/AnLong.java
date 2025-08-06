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

import static org.gprofng.mpmt.AnObject.format_group_integer;

public final class AnLong extends AnObject implements Comparable<AnLong> {

  private final long value;
  private final Long obj;

  public AnLong(long value) {
    this.value = value;
    obj = this.value;
  }

  // Analyzer Long printing format
  @Override
  public String toString() {
    if (sign) {
      return String.format("%+d", value);
    }
    return obj.toString();
  }

  // Analyzer formatted printing format
  @Override
  public String toFormString() {
    if (value == 0) {
      if (!showZero) {
        return quote_space;
      }
    }
    if (sign) {
      return format_group_sinteger.format(value);
    }
    return format_group_integer.format(value);
  }

  // Percent printing
  @Override
  public String toPercent(double total) {
    if (value == 0) {
      if (!showZero) {
        return quote_space;
      }
      return zero_percent;
    }
    return format_percent.format(value * total);
  }

  // Time printing
  @Override
  public String toTime(double clock) {
    if (value == 0) {
      return get_zero_decimal();
    }
    AnDouble d = new AnDouble(value * clock);
    d.showSign(sign);
    d.showZero(showZero);
    return d.toString();
  }

  // Time formatted printing format
  @Override
  public String toFormTime(double clock) {
    if (value == 0) {
      return get_zero_decimal();
    }
    AnDouble d = new AnDouble(value * clock);
    d.showSign(sign);
    d.showZero(showZero);
    return d.toFormString();
  }

  // To Long
  public Long toLong() {
    return obj;
  }

  // To double
  @Override
  public double doubleValue() {
    return value;
  }

  // As Long.compareTo
  @Override
  public int compareTo(AnLong o) {
    return obj.compareTo(o.toLong());
  }

  // Convert long[] to AnLong[]
  public static AnLong[] toArray(long[] list) {
    int length = list.length;
    AnLong[] new_list = new AnLong[length];
    for (int i = 0; i < length; i++) {
      new_list[i] = new AnLong(list[i]);
    }
    return new_list;
  }
}
