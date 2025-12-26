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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.progress.CancelContext;
import org.gprofng.mpmt.progress.Cancellable;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

/** */
public class IPCResult implements Cancellable {
  public enum CC {
    DEFAULT(0),
    SUCCESS(1),
    FAILURE(2),
    CANCELED(3);
    private final int value;

    private CC(int value) {
      this.value = value;
    }

    private int value() {
      return value;
    }

    protected static CC fromValue(int val) {
      for (CC rt : CC.values()) {
        if (rt.value() == val) {
          return rt;
        }
      }
      return null;
    }
  };

  private IPCHandle ipcHandle;
  private int requestID;
  private IPCContext ipcContext;
  private IPCResponse ipcResponse;
  private boolean resultIsReady = false;
  private int curChar;
  private int iVal;
  private boolean bVal;
  private long lVal;
  private String sVal;
  private Object aVal;
  private DecimalFormat format;

  protected IPCResult(IPCHandle ipcHandle, int requestID, IPCContext ipcContext) {
    this.ipcHandle = ipcHandle;
    this.requestID = requestID;
    this.ipcContext = ipcContext;

    curChar = 0;
    format = new DecimalFormat();
    format.setDecimalFormatSymbols(
        new DecimalFormatSymbols(Locale.US)); // Use non-localized double values
  }

  protected IPCHandle getIPCHandle() {
    return ipcHandle;
  }

  protected int getRequestID() {
    return requestID;
  }

  // Public begin
  public CC getCC() {
    waitForResult();
    return ipcResponse.getHeader().getResponseStatus();
  }

  public int getInt() {
    waitForResult();
    readResult();
    return iVal;
  }

  public String getString() {
    waitForResult();
    readResult();
    return sVal;
  }

  public long getLong() {
    waitForResult();
    readResult();
    return lVal;
  }

  public boolean getBoolean() {
    waitForResult();
    readResult();
    return bVal;
  }

  public Object getObject() {
    waitForResult();
    readResult();
    return aVal;
  }

  public Object[] getObjects() {
    return (Object[]) getObject();
  }

  public String[] getStrings() {
    return (String[]) getObject();
  }

  public int[] getInts() {
    return (int[]) getObject();
  }

  public long[] getLongs() {
    return (long[]) getObject();
  }

  public boolean[] getBooleans() {
    return (boolean[]) getObject();
  }

  public void getVoid() {
    waitForResult();
  }

  public int getVersion() {
    waitForResult();
    if (ipcResponse.getHeader().getResponseType() == IPCResponseHeader.ResponseType.HANDHAKE) {
      return ipcResponse.getHeader().getnBytes();
    }
    return -1;
  }

  /** Send a cancel request for this particular request */
  public boolean cancel() {
    IPCHandle cancelIPCHandle = IPCHandle.newCancelIPCHandle(ipcContext, requestID);
    IPCResult ipcResult = cancelIPCHandle.sendRequest(); // no body
    if (ipcResult.getCC() == IPCResult.CC.SUCCESS) {
      IPCLogger.logTrace(
          ipcResult.getRequestID()
              + " IPCResult Cancel SUCCESS channel "
              + ipcContext.getChannel());
      return true; // it is being cancelled
    } else {
      IPCLogger.logTrace(
          ipcResult.getRequestID()
              + " IPCResult Cancel FAILURE channel "
              + ipcContext.getChannel());
      System.err.println(
          "Cancel FAILURE channel " + ipcContext.getChannel() + " (may not work yet)");
      return false; // not being cancelled (not cancellable?)
    }
  }

  /** rewinds input buffer so it can be read again with getXxxx() */
  public void rewindInputBuffer() {
    curChar = 0;
  }
  // Public end

  protected void started(int requestId) {
    IPCLogger.logTrace(requestId + " IPCResult started ");
    if (ipcContext.isSystemProgressBar() && ipcContext.isCancellable()) {
      CancelContext cancelContext = new CancelContext(ipcContext.getTaskName(), this);
      AnWindow.getInstance().getSystemProgressPanel().setProgressCancelContext(cancelContext);
    }
  }

  protected void progress(IPCResponse ipcResponse) {
    IPCLogger.logTrace(ipcResponse.getHeader().getRequestID() + " IPCResult progress ");
    if (ipcContext.isSystemProgressBar()) {
      IPCProtocol.CurChar curChar = new IPCProtocol.CurChar();
      int progress = IPCProtocol.decodeByte(ipcResponse.getBody(), curChar);
      int percentage = IPCProtocol.decodeIVal(ipcResponse.getBody(), curChar);
      String what = IPCProtocol.decodeSVal(ipcResponse.getBody(), curChar);
      AnWindow.getInstance().getSystemProgressPanel().setProgress(percentage, what);
    }
  }

  protected void finished(int requestId, IPCResponse ipcResponse) {
    IPCLogger.logTrace(requestId + " IPCResult finished ");
    this.ipcResponse = ipcResponse;
    resultIsReady();
    Analyzer.getInstance().IPC_session.getIPCReader().removeFromRequestPool(requestId);
  }

  protected void fireResponse(IPCResponse ipcResponse) {
    if (ipcResponse.getHeader().getResponseType() == IPCResponseHeader.ResponseType.ACK) {
      started(ipcResponse.getHeader().getRequestID());
    } else if (ipcResponse.getHeader().getResponseType()
        == IPCResponseHeader.ResponseType.COMPLETE) {
      finished(ipcResponse.getHeader().getRequestID(), ipcResponse);
    } else if (ipcResponse.getHeader().getResponseType()
        == IPCResponseHeader.ResponseType.HANDHAKE) {
      finished(ipcResponse.getHeader().getRequestID(), ipcResponse);
    } else if (ipcResponse.getHeader().getResponseType()
        == IPCResponseHeader.ResponseType.PROGRESS) {
      progress(ipcResponse);
    } else {
      assert false;
    }
  }

  protected void waitForResult() {
    AnUtility.checkIfOnAWTThread(false);
    waitOrNotify(true);
    if (ipcResponse == null) {
      System.out.println("" + requestID + " ipcResponse1 " + ipcResponse);
    }
  }

  protected void resultIsReady() {
    waitOrNotify(false);
  }

  protected synchronized void waitOrNotify(boolean readData) {
    if (readData) {
      IPCLogger.logTrace(requestID + " IPCResult waitOrNotify 1 " + resultIsReady);
      if (!resultIsReady) {
        while (!resultIsReady) {
          try {
            IPCLogger.logTrace(requestID + " IPCResult waitOrNotify 2 " + resultIsReady);
            this.wait();
            IPCLogger.logTrace(requestID + " IPCResult waitOrNotify 3 " + resultIsReady);
          } catch (InterruptedException ie) {
            IPCLogger.logTrace(requestID + " IPCResult waitOrNotify 4 " + resultIsReady);
          }
        }
      } else {
        IPCLogger.logTrace(requestID + " IPCResult waitOrNotify 5 " + resultIsReady);
      }
    } else {
      resultIsReady = true;
      IPCLogger.logTrace(requestID + " IPCResult waitOrNotify 6 " + resultIsReady);
      this.notify();
    }
  }

  private int nextChar() {
    if (curChar < ipcResponse.getBody().length) {
      return ipcResponse.getBody()[curChar++];
    } else {
      System.err.println("IPCResult " + curChar + " " + ipcResponse.getBody().length);
      assert false;
      return 'x';
    }
  }

  private int readByte() {
    int val = 0;
    for (int i = 0; i < 2; i++) {
      final int c = nextChar();
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

  private int readIVal() {
    int val = readByte();
    for (int i = 0; i < 3; i++) {
      val = val * 256 + readByte();
    }
    return val;
  }

  private long readLVal() {
    long val = readByte();
    for (int i = 0; i < 7; i++) {
      val = val * 256 + readByte();
    }
    return val;
  }

  private boolean readBVal() {
    final int val = readByte();
    return val != 0;
  }

  private char readCVal() {
    final int val = readByte();
    return (char) val;
  }

  /**
   * Reads double value This method is synchronized because "format" is not thread safe
   *
   * @return double value
   */
  private synchronized double readDVal() {
    final String s = readSVal();
    final double DValue;
    try {
      DValue = format.parse(s).doubleValue(); // format is not thread safe
    } catch (ParseException ex) {
      ex.printStackTrace();
      return 0.0;
    }
    return DValue;
  }

  private String readSVal() {
    final int len = readIVal();
    if (len == -1) {
      return null;
    }
    final String s = new String(ipcResponse.getBody(), curChar, len);
    curChar = curChar + len;
    return s;
  }

  private Object readAVal() {
    boolean twoD = false;
    int type = readByte();
    if (type == IPCProtocol.L_ARRAY) {
      twoD = true;
      type = readByte();
    }
    final int len = readIVal();
    if (len == -1) {
      return null;
    }
    switch (type) {
      case IPCProtocol.L_INTEGER:
        if (twoD) {
          final int[][] array = new int[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (int[]) readAVal();
          }
          return array;
        } else {
          final int[] array = new int[len];
          for (int i = 0; i < len; i++) {
            array[i] = readIVal();
          }
          return array;
        }
        // break;
      case IPCProtocol.L_LONG:
        if (twoD) {
          final long[][] array = new long[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (long[]) readAVal();
          }
          return array;
        } else {
          final long[] array = new long[len];
          for (int i = 0; i < len; i++) {
            array[i] = readLVal();
          }
          return array;
        }
        // break;
      case IPCProtocol.L_DOUBLE:
        if (twoD) {
          final double[][] array = new double[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (double[]) readAVal();
          }
          return array;
        } else {
          final double[] array = new double[len];
          for (int i = 0; i < len; i++) {
            array[i] = readDVal();
          }
          return array;
        }
        // break;
      case IPCProtocol.L_BOOLEAN:
        if (twoD) {
          final boolean[][] array = new boolean[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (boolean[]) readAVal();
          }
          return array;
        } else {
          final boolean[] array = new boolean[len];
          for (int i = 0; i < len; i++) {
            array[i] = readBVal();
          }
          return array;
        }
        // break;
      case IPCProtocol.L_CHAR:
        if (twoD) {
          final char[][] array = new char[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (char[]) readAVal();
          }
          return array;
        } else {
          final char[] array = new char[len];
          for (int i = 0; i < len; i++) {
            array[i] = readCVal();
          }
          return array;
        }
        // break;
      case IPCProtocol.L_STRING:
        if (twoD) {
          final String[][] array = new String[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (String[]) readAVal();
          }
          return array;
        } else {
          final String[] array = new String[len];
          for (int i = 0; i < len; i++) {
            array[i] = readSVal();
          }
          return array;
        }
        // break;
      case IPCProtocol.L_OBJECT:
        if (twoD) {
          final Object[][] array = new Object[len][];
          for (int i = 0; i < len; i++) {
            array[i] = (Object[]) readAVal();
          }
          return array;
        } else {
          final Object[] array = new Object[len];
          for (int i = 0; i < len; i++) {
            array[i] = readAVal();
          }
          return array;
        }
        // break;
    }
    return null;
  }

  private void readResult() {
    if (AnUtility.checkIPCOnWrongThread()) {
      AnUtility.checkIfOnAWTThread(false);
    }
    for (; ; ) {
      //            AnMemoryManager.getInstance().checkMemoryUsage();
      final int tVal = readByte();
      switch (tVal) {
        case IPCProtocol.L_INTEGER:
          iVal = readIVal();
          break;
        case IPCProtocol.L_LONG:
          lVal = readLVal();
          break;
        case IPCProtocol.L_BOOLEAN:
          bVal = readBVal();
          break;
        case IPCProtocol.L_STRING:
          sVal = readSVal();
          break;
        case IPCProtocol.L_ARRAY:
          aVal = readAVal();
          break;
        default:
          System.err.println("Unknown code: " + tVal);
          break;
      }
      return;
    }
  }
}
