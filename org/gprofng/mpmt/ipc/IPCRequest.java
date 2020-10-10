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

package org.gprofng.mpmt.ipc;

class IPCRequest {
  private StringBuilder body;

  protected IPCRequest() {
    body = new StringBuilder();
  }

  protected void append(final int i) {
    body.append(sendByte(IPCProtocol.L_INTEGER));
    body.append(sendVal(i));
  }

  protected void append(final long l) {
    body.append(sendByte(IPCProtocol.L_LONG));
    body.append(sendVal(l));
  }

  protected void append(final boolean b) {
    body.append(sendByte(IPCProtocol.L_BOOLEAN));
    body.append(sendVal(b));
  }

  protected void append(final String s) {
    body.append(sendByte(IPCProtocol.L_STRING));
    String ss = sendVal(s);
    body.append(ss);
  }

  protected void append(final Object object) {
    body.append(sendByte(IPCProtocol.L_ARRAY));
    body.append(sendVal(object));
  }

  protected String getBody() {
    return body.toString();
  }

  private String sendByte(final int b) {
    return IPCProtocol.encodeByte(b);
  }

  private String sendVal(final long l) {
    return IPCProtocol.encodeVal(l);
  }

  private String sendVal(final int i) {
    return IPCProtocol.encodeVal(i);
  }

  private String sendVal(final String s) {
    if (s == null) {
      return sendVal(-1);
    }
    byte[] sb = s.getBytes();
    StringBuilder buf = new StringBuilder();
    buf.append(sendVal(sb.length));

    for (byte b : sb) {
      buf.append((char) b);
    }
    return buf.toString();
  }

  private String sendVal(final boolean b) {
    return sendByte(b ? 1 : 0);
  }

  private String sendVal(final char c) {
    return sendByte((int) c);
  }

  private String sendVal(final double d) {
    return sendVal(Double.toString(d));
  }

  private String sendVal(final Object object) {
    StringBuilder buf = new StringBuilder();
    if (object == null) {
      buf.append(sendByte(IPCProtocol.L_INTEGER));
      buf.append(sendVal(-1));
      return buf.toString();
    }

    if (object instanceof double[]) {
      buf.append(sendByte(IPCProtocol.L_DOUBLE));
      final double[] array = (double[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    } else if (object instanceof int[]) {
      buf.append(sendByte(IPCProtocol.L_INTEGER));
      final int[] array = (int[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    } else if (object instanceof long[]) {
      buf.append(sendByte(IPCProtocol.L_LONG));
      final long[] array = (long[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    } else if (object instanceof char[]) {
      buf.append(sendByte(IPCProtocol.L_CHAR));
      final char[] array = (char[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    } else if (object instanceof boolean[]) {
      buf.append(sendByte(IPCProtocol.L_BOOLEAN));
      final boolean[] array = (boolean[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    } else if (object instanceof String[]) {
      buf.append(sendByte(IPCProtocol.L_STRING));
      final String[] array = (String[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    } else if (object instanceof Object[]) {
      buf.append(sendByte(IPCProtocol.L_OBJECT));
      final Object[] array = (Object[]) object;
      buf.append(sendVal(array.length));
      for (int i = 0; i < array.length; i++) {
        buf.append(sendVal(array[i]));
      }
    }
    return buf.toString();
  }
}
