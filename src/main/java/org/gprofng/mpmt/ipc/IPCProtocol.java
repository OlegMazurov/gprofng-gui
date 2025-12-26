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

package org.gprofng.mpmt.ipc;

public class IPCProtocol {
  public static final int version = 38; // IPC PROTOCOL VERSION - DO NOT CHANGE THIS COMMENT
  public static final String IPC_PROTOCOL_STR = "IPC_PROTOCOL_" + version; // Dodona build 29

  protected static final int L_PROGRESS = 0;
  protected static final int L_INTEGER = 1;
  protected static final int L_BOOLEAN = 2;
  protected static final int L_LONG = 3;
  protected static final int L_STRING = 4;
  protected static final int L_DOUBLE = 5;
  protected static final int L_ARRAY = 6;
  protected static final int L_OBJECT = 7;
  protected static final int L_CHAR = 8;
  protected static final int L_HEADER = 255;

  protected static final char[] hex = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  protected static String encodeByte(final int b) {
    StringBuilder buf = new StringBuilder();
    buf.append(hex[(b >> 4) & 0xf]);
    buf.append(hex[b & 0xf]);
    return buf.toString();
  }

  protected static String encodeVal(final int i) {
    StringBuilder buf = new StringBuilder();
    for (int j = 28; j >= 0; j = j - 4) {
      buf.append(hex[(i >> j) & 0xf]);
    }
    return buf.toString();
  }

  protected static String encodeVal(final long l) {
    StringBuilder buf = new StringBuilder();
    for (int j = 60; j >= 0; j = j - 4) {
      buf.append(hex[(int) ((l >> j) & 0xf)]);
    }
    return buf.toString();
  }

  protected static class CurChar {
    int pos = 0;
  }

  protected static int decodeByte(byte[] bytes, CurChar curChar) {
    int val = 0;
    for (int i = 0; i < 2; i++) {
      final int c = bytes[curChar.pos];
      curChar.pos++;
      switch (c) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          val = val * 16 + c - '0';
          break;
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
          val = val * 16 + c - 'a' + 10;
          break;
      }
    }

    return val;
  }

  protected static int decodeIVal(byte[] bytes, CurChar curChar) {
    int val = decodeByte(bytes, curChar);
    for (int i = 0; i < 3; i++) {
      val = val * 256 + decodeByte(bytes, curChar);
    }
    return val;
  }

  protected static String decodeSVal(byte[] bytes, CurChar curChar) {
    int len = decodeIVal(bytes, curChar);
    if (len == -1) {
      return null;
    }
    String s = new String(bytes, curChar.pos, len);
    curChar.pos += len;
    return s;
  }
}
