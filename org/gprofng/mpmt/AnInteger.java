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

// Analyzer Integer object
public final class AnInteger extends AnObject implements Comparable {

  private final int value;
  private final Integer obj;

  // Constructor
  public AnInteger(int value) {
    this.value = value;
    obj = this.value;
  }

  // Analyzer int printing format
  public String toString() {
    return obj.toString();
  }

  // Analyzer formatted printing format
  public String toFormString() {
    if (value == 0) {
      if (!showZero) {
        return quote_space;
      }
    }
    return format_group_integer.format(value);
  }

  // Percent printing
  public String toPercent(double total) {
    if (value == 0) {
      if (!showZero) {
        return quote_space;
      }
      return zero_percent;
    }
    return format_percent.format(value * total);
  }

  // To Integer
  public Integer toInteger() {
    return obj;
  }

  // To int
  public int intValue() {
    return value;
  }

  // To double
  public double doubleValue() {
    return value;
  }

  // As int.compareTo
  public int compareTo(Object o) {
    return obj.compareTo(((AnInteger) o).toInteger());
  }

  // Convert int[] to AnInteger[]
  public static AnInteger[] toArray(int[] list) {
    int length = list.length;
    AnInteger[] new_list = new AnInteger[length];

    for (int i = 0; i < length; i++) {
      new_list[i] = new AnInteger(list[i]);
    }
    return new_list;
  }
}
