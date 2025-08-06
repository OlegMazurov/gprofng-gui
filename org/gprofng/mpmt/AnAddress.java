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

// Analyzer Address object
public final class AnAddress extends AnObject implements Comparable<AnAddress> {

  static final long HI_BIT = 0x8000000000000000L;
  private static final String hexChars = "0123456789ABCDEF";

  private final long value;
  private final Long obj;

  // Constructor
  public AnAddress(long value) {
    this.value = value & (~HI_BIT);
    obj = this.value;
  }

  // Analyzer Address printing format
  @Override
  public String toString() {
    if (value == 0) {
      if (!showZero) {
        return quote_space;
      }
    }
    return Integer.toString((int) (value >> 32)) + ":" + toHexString((int) (value & 0xFFFFFFFFL));
  }

  // To Long
  private Long toLong() {
    return obj;
  }

  // To double
  @Override
  public double doubleValue() {
    return value;
  }

  // As Long.compareTo
  @Override
  public int compareTo(AnAddress o) {
    return obj.compareTo(o.toLong());
  }

  // String representation as an unsigned integer in base 16
  public static String toHexString(int i) {
    return toUnsignedString((long) i, 4, 8);
  }

  // String representation as an unsigned integer in base 16
  public static String toHexString(long i) {
    return toUnsignedString(i, 4, 16);
  }

  // String representation as an unsigned integer in base 2^shift
  private static String toUnsignedString(long i, int shift, int min_len) {
    char[] buf = new char[32];
    int charPos = 32;
    int end = 32 - min_len;
    int radix = 1 << shift;
    int mask = radix - 1;

    // Get the digits
    do {
      buf[--charPos] = hexChars.charAt((int) (i & mask));
      i >>>= shift;
    } while (i != 0);

    // Fill zeros
    while (charPos > end) {
      buf[--charPos] = '0';
    }
    return "0x" + new String(buf, charPos, (32 - charPos));
  }

  // Check if an address element
  public static boolean isAddress(long value) {
    return (value & HI_BIT) != 0;
  }

  // Convert long[] to AnAddress[]
  public static AnAddress[] toArray(long[] list) {
    int length = list.length;
    AnAddress[] new_list = new AnAddress[length];
    for (int i = 0; i < length; i++) {
      new_list[i] = new AnAddress(list[i]);
    }
    return new_list;
  }
}
