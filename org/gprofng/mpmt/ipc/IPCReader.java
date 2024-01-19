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

package org.gprofng.mpmt.ipc;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IPCReader implements Runnable {

  private IPC ipc;
  private Map<Integer, IPCResult> requestPool = new HashMap<Integer, IPCResult>();
  private final int READY_TO_RUN = 1;
  private final int RUNNING = 2;
  private final int REQUEST_TO_STOP = 3;
  private final int CONNECTING =
      4; // Temporary solution for remote analyzer: special state "CONNECTING"
  private final int CONNECTION_CLOSDED = 5; // Connection closed by gp-display-text
  private final int RESTARTING = 6; // Restarting gp-display-text
  private int state = READY_TO_RUN;
  private String unknown_input = "";
  // Internal statistics
  private long totalReceivedMessages = 0;
  private long totalReceivedBytes = 0;

  public IPCReader(IPC ipc) {
    this.ipc = ipc;
    state = RUNNING;
    new Thread(this, "IPCReader").start();
  }

  // NM Temporary solution for remote analyzer: use flag "run" to decide how to read data
  public IPCReader(IPC ipc, boolean run) {
    this.ipc = ipc;
    if (run) {
      state = RUNNING;
    } else {
      state = CONNECTING;
    }
    new Thread(this, "IPCReader").start();
  }

  /** Checks if thread is ready to run */
  public boolean isThreadReady() {
    if (RUNNING == state) return true;
    if (READY_TO_RUN == state) return true;
    return (false);
  }

  /** Sets request to stop IPCReader thread */
  public void stopThread() {
    state = REQUEST_TO_STOP;
    try {
      ipc.getInputStream().close();
      ipc.getOutputStream().close();
    } catch (IOException ex) {
    }
  }

  /** //NM Temporary solution for remote analyzer: Sets request to run IPCReader thread */
  public void runThread() {
    state = RUNNING;
  }

  /** Sets request to suspend IPCReader thread */
  public void suspendThread() {
    state = RESTARTING; // CONNECTING
  }

  /** Sets request to resume IPCReader thread */
  public void resumeThread() {
    state = RUNNING;
  }

  /** Gets input as a String */
  public String getUnknownInput() {
    return unknown_input;
  }

  /** Gets Total Received Bytes */
  public long getTotalReceivedBytes() {
    return totalReceivedBytes;
  }

  /** Gets Total Received Messages */
  public long getTotalReceivedMessages() {
    return totalReceivedMessages;
  }

  /**
   * Reads exactly buffer.length() bytes from stream.
   *
   * @param buffer
   * @return bytes read (buffer.length())
   */
  private int readFromInputStream(byte[] buffer) {
    try {
      int nBytesToRead = buffer.length;
      int nBytesRead = 0;
      while (nBytesRead < nBytesToRead) {
        int got = ipc.getInputStream().read(buffer, nBytesRead, nBytesToRead - nBytesRead);
        if (got < 0) {
          AnWindow.getInstance().getSystemProgressPanel().reset();
          if (state == REQUEST_TO_STOP) {
            return 0;
          }
          if ((state == RESTARTING) || (state == CONNECTING)) {
            return got;
          }
          state = REQUEST_TO_STOP; // CONNECTION_CLOSDED;
          break;
          // throw new IPC.AnIPCException();
        }
        totalReceivedBytes += got;
        nBytesRead += got;
      }
      return nBytesToRead;
    } catch (IOException ioe) {
      if (state == REQUEST_TO_STOP) {
        return 0;
      }
      state = REQUEST_TO_STOP; // CONNECTION_CLOSDED;
      return -1;
      // throw new IPC.AnIPCException(ioe);
    }
  }

  private byte[] readFromStream(int nBytes) {
    byte[] buffer = new byte[nBytes];
    int got = readFromInputStream(buffer);
    return buffer;
  }

  // @Override
  @Override
  public void run() {
    while (true) {
      IPCResponseHeader ipcResponseHeader = null;
      byte[] responseBody = null;
      byte[] responseHeaderBytes = null;
      if (state != RUNNING) { // Read raw input
        byte[] responseHeaderBytes_1 = new byte[1];
        int nBytes = readFromInputStream(responseHeaderBytes_1);
        if (state == REQUEST_TO_STOP) { // Connection closed
          break;
        }
        if (state == CONNECTING) {
          if (nBytes < 0) { // Connection error
            break;
          }
          for (int i = 0; i < nBytes; i++) {
            unknown_input += (char) responseHeaderBytes_1[i];
          }
          continue;
        }
        if (state == RESTARTING) {
          if (nBytes < 0) { // Connection error
            // System.err.println("***** NOTE: RESTARTING: nBytes < 0");
            break;
          }
          for (int i = 0; i < nBytes; i++) {
            unknown_input += (char) responseHeaderBytes_1[i];
          }
          String restartEngine = "Restart engine";
          if (unknown_input.contains(restartEngine)) {
            // System.err.println("***** NOTE: gp-display-text restarted");
            resumeThread();
            // System.err.println("***** NOTE: unknown_input:" + unknown_input);
            // clean unknown input
            unknown_input = "";
          }
          continue;
        }
        if (nBytes < 0) { // Read error
          state = REQUEST_TO_STOP; // CONNECTION_CLOSDED;
          break;
          // throw new IPC.AnIPCException();
        }
        // Read the rest of the header and combine it with the first byte
        byte[] responseHeaderBytes_2 = readFromStream(IPCResponseHeader.headerSizeInBytes * 2 - 1);
        if (state == REQUEST_TO_STOP) break;
        responseHeaderBytes = new byte[responseHeaderBytes_2.length + 1];
        responseHeaderBytes[0] = responseHeaderBytes_1[0];
        for (int i = 1; i < responseHeaderBytes.length; i++) {
          responseHeaderBytes[i] = responseHeaderBytes_2[i - 1];
        }
        totalReceivedMessages++;
      } else { // state == RUNNING
        // clean unknown input
        unknown_input = "";
        // Read a header and a nBytes (specified in header) body
        responseHeaderBytes = readFromStream(IPCResponseHeader.headerSizeInBytes * 2);
        totalReceivedMessages++;
      }
      if (state == REQUEST_TO_STOP) {
        break;
      }
      ipcResponseHeader = decodeIPCResponseHeader(responseHeaderBytes);
      if (ipcResponseHeader.getResponseType() != IPCResponseHeader.ResponseType.HANDHAKE) {
        int nBytes = ipcResponseHeader.getnBytes();
        if (nBytes > 0) {
          responseBody = readFromStream(nBytes);
        }
      }
      IPCLogger.logResponse(responseHeaderBytes, responseBody);
      // Send result to IPCResult
      IPCResult ipcResult = findInRequestPool(ipcResponseHeader.getRequestID());
      final int additionalLatency = IPCLogger.getIpcDelay(); // milliseconds
      if (ipcResult != null) {
        if (additionalLatency <= 0) {
          IPCResponse ipcResponse = new IPCResponse(ipcResponseHeader, responseBody);
          ipcResult.fireResponse(ipcResponse);
          ipcResult.getIPCHandle().fireResponse(ipcResponse, ipcResult); // Fixup: new thread?
        } else {
          // use a "latency" thread
          final IPCResult my_ipcResult = ipcResult;
          final IPCResponseHeader my_ipcResponseHeader = ipcResponseHeader;
          final byte[] my_responseBody = responseBody;
          AnUtility.dispatchOnAWorkerThread(
              new Runnable() {
                @Override
                public void run() {
                  if (additionalLatency > 0) {
                    try {
                      Thread.sleep(additionalLatency); // milliseconds
                    } catch (Exception e) {
                      // check that we did sleep enough?
                    }
                  }
                  IPCResponse ipcResponse = new IPCResponse(my_ipcResponseHeader, my_responseBody);
                  my_ipcResult.fireResponse(ipcResponse);
                  my_ipcResult.getIPCHandle().fireResponse(ipcResponse, my_ipcResult);
                }
              },
              "AdditionalUploadLatency_thread");
        }
      } else {
        if (IPCResponseHeader.ResponseType.PROGRESS == ipcResponseHeader.getResponseType()) {
          // System.err.println("***** NOTE: IPCReader: PROGRESS: ipcResult==null");
          // // Check responseBody
          // String restartEngine = "Restart engine";
          // IPCProtocol.CurChar curChar = new IPCProtocol.CurChar();
          // int progress = IPCProtocol.decodeByte(responseBody, curChar);
          // int percentage = IPCProtocol.decodeIVal(responseBody, curChar);
          // String what = IPCProtocol.decodeSVal(responseBody, curChar);
          // if (100 == percentage) {
          //   if (restartEngine.equals(what)) {
          //     if ((state == RESTARTING) || (state == CONNECTING)) {
          //       resumeThread();
          //       System.err.println("***** NOTE: gp-display-text restarted");
          //     }
          //   }
          // }
          continue;
        }
        // System.err.println("********************ERROR: IPCReader: state="+state+"
        // ipcResponseHeader="+ipcResponseHeader.toString()+"
        // responseBody="+responseBody.toString());
        if (state != RUNNING) {
          continue; // ignore all messages
        }
        // FIXUP should not happen
        System.err.println("********************ERROR: IPCReader: ipcHandle==null");
        assert false;
      }
    }
  }

  private IPCResponseHeader decodeIPCResponseHeader(byte[] bytes) {
    IPCProtocol.CurChar curChar = new IPCProtocol.CurChar();
    int headerMarker = IPCProtocol.decodeByte(bytes, curChar);
    if (headerMarker != IPCProtocol.L_HEADER) {
      // FIXUP
      assert false;
    }
    int requestID = IPCProtocol.decodeIVal(bytes, curChar);
    int responseType = IPCProtocol.decodeByte(bytes, curChar);
    int responseStatus = IPCProtocol.decodeByte(bytes, curChar);
    int nBytes = IPCProtocol.decodeIVal(bytes, curChar);
    IPCResponseHeader ipcResponseHeader =
        new IPCResponseHeader(
            requestID,
            IPCResponseHeader.ResponseType.fromValue(responseType),
            IPCResult.CC.fromValue(responseStatus),
            nBytes);
    return ipcResponseHeader;
  }

  protected void addToRequestPool(IPCResult ipcResult) {
    IPCLogger.logTrace("\n" + ipcResult.getRequestID() + " IPCReader addToRequestPool");
    synchronized (requestPool) {
      requestPool.put(ipcResult.getRequestID(), ipcResult);
    }
  }

  protected IPCResult findInRequestPool(int ipcRequestId) {
    synchronized (requestPool) {
      return requestPool.get(ipcRequestId);
    }
  }

  protected void removeFromRequestPool(int ipcRequestId) {
    IPCLogger.logTrace(ipcRequestId + " IPCReader removeFromRequestPool");
    synchronized (requestPool) {
      requestPool.remove(ipcRequestId);
    }
  }
}
