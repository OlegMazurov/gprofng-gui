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

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCProtocol.CurChar;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Examples:
 *
 * <p>// Simplest code IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
 * ipcHandle.append("getFilterStr"); ipcHandle.append(window.getID()); IPCResult ipcResult =
 * ipcHandle.sendRequest(); String result = ipcResult.getString(); // blocking
 *
 * <p>// Checking completion code IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
 * ipcHandle.append("getFilterStr"); ipcHandle.append(window.getID()); IPCResult ipcResult =
 * ipcHandle.sendRequest(); if (ipcResult.getCC() == IPCResult.CC.SUCCESS) { // blocking // Things
 * are fine String result = ipcResult.getString(); return result; } else { // error handling }
 *
 * <p>// Handling progress, not cancellable IPCContext ipcContext = new
 * IPCContext(IPCContext.Scope.WINDOW, this); IPCHandle ipcHandle = new
 * IPCHandle(IPCHandle.RequestKind.DBE, ipcContext); ipcHandle.append("getFilterStr");
 * ipcHandle.append(window.getID()); ipcHandle.addIPCListener(new IPCListener() { public void
 * started(int requestId) { System.out.println("*******: MyOwnProgressListener: " + requestId + "
 * Start..."); } public void progress(int requestId, String what, int percentage) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + what + " " +
 * percentage); } public void finished(int requestId, IPCResult ipcResult) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + " Stop..."); } });
 * IPCResult ipcResult = ipcHandle.sendRequest(); String result = ipcResult.getString(); // blocking
 *
 * <p>// Handling progress, cancellable IPCContext ipcContext = new IPCContext("getFilterStr",
 * IPCContext.Scope.WINDOW, true, false, IPCContext.Request.SINGLE, 1, this); IPCHandle ipcHandle =
 * new IPCHandle(IPCHandle.RequestKind.DBE, ipcContext); ipcHandle.append("getFilterStr");
 * ipcHandle.append(window.getID()); ipcHandle.addIPCListener(new IPCListener() { public void
 * started(int requestId) { System.out.println("*******: MyOwnProgressListener: " + requestId + "
 * Start..."); } public void progress(int requestId, String what, int percentage) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + what + " " +
 * percentage); } public void finished(int requestId, IPCResult ipcResult) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + " Stop..."); } });
 * IPCResult ipcResult = ipcHandle.sendRequest(); if (ipcResult.getCC() == IPCResult.CC.SUCCESS) {
 * // blocking // Things are fine String result = ipcResult.getString(); return result; } else
 * (ipcResult.getCC() == IPCResult.CC.CANCELED) // cancelled } else { // error handling }
 *
 * <p>// Handling progress and reading result in finished() callback IPCContext ipcContext = new
 * IPCContext("getFilterStr", IPCContext.Scope.WINDOW, true, false, IPCContext.Request.SINGLE, 1,
 * this); IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, ipcContext);
 * ipcHandle.append("getFilterStr"); ipcHandle.append(window.getID()); ipcHandle.addIPCListener(new
 * IPCListener() { public void started(int requestId) { System.out.println("*******:
 * MyOwnProgressListener: " + requestId + " Start..."); } public void progress(int requestId, String
 * what, int percentage) { System.out.println("*******: MyOwnProgressListener: " + requestId + " " +
 * what + " " + percentage); } public void finished(int requestId, IPCResult ipcResult) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + " Stop..."); String
 * result = ipcResult.getString(); // blocking but result is ready System.out.println("*******:
 * MyOwnProgressListener: " + requestId + " Result=" + result); } }); IPCResult ipcResult =
 * ipcHandle.sendRequest();
 *
 * <p>// Handling progress and reading result in main flow and in finishd() callback IPCContext
 * ipcContext = new IPCContext("getFilterStr", IPCContext.Scope.WINDOW, true, false,
 * IPCContext.Request.SINGLE, 1, this); IPCHandle ipcHandle = new
 * IPCHandle(IPCHandle.RequestKind.DBE, ipcContext); ipcHandle.append("getFilterStr");
 * ipcHandle.append(window.getID()); ipcHandle.addIPCListener(new IPCListener() { public void
 * started(int requestId) { System.out.println("*******: MyOwnProgressListener: " + requestId + "
 * Start..."); } public void progress(int requestId, String what, int percentage) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + what + " " +
 * percentage); } public void finished(int requestId, IPCResult ipcResult) {
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " " + " Stop..."); // Result
 * can be read here and/or in main flow // Need to rewind input buffer before result is read again
 * ipcResult.rewindInputBuffer(); String result = ipcResult.getString(); // blocking
 * System.out.println("*******: MyOwnProgressListener: " + requestId + " Result=" + result); } });
 * IPCResult ipcResult = ipcHandle.sendRequest(); String result = ipcResult.getString(); // blocking
 */
public class IPCHandle {
  private static int globalRequestID = 0;

  public enum RequestKind {
    NON_DBE,
    DBE
  };

  public enum RequestType {
    DEFAULT(0),
    CANCEL(1),
    HANDSHAKE(2);
    private final int index;

    RequestType(int index) {
      this.index = index;
    }

    public int index() {
      return index;
    }
  };

  private List<IPCListener> ipcListeners = new ArrayList<IPCListener>();
  private RequestType requestType;
  private IPCContext ipcContext;
  private IPCRequest ipcRequest;

  public IPCHandle(RequestKind requestKind) {
    this(requestKind, null);
  }

  public IPCHandle(RequestKind requestKind, IPCContext ipcContext) {
    requestType = RequestType.DEFAULT;
    if (ipcContext == null) {
      if (requestKind == RequestKind.DBE) {
        ipcContext = IPCContext.getDefaultDBEIPCContext();
      } else {
        ipcContext = IPCContext.getDefaultNONDBEIPCContext();
      }
    }
    this.ipcContext = ipcContext;
    this.ipcRequest = new IPCRequest();
  }

  public static IPCHandle newHandshakeIPCHandle() {
    return new IPCHandle(RequestType.HANDSHAKE, IPCContext.getDefaultNONDBEIPCContext());
  }

  public static IPCHandle newCancelIPCHandle(IPCContext originalIPCContext, int originalRequestID) {
    IPCContext ipcContext =
        IPCContext.newCancelContext(
            originalIPCContext.getChannel(),
            originalIPCContext.getRequest() == IPCContext.Request.SINGLE ? originalRequestID : -1);
    return new IPCHandle(RequestType.CANCEL, ipcContext);
  }

  protected IPCHandle(RequestType requestType, IPCContext ipcContext) {
    this.requestType = requestType;
    this.ipcContext = ipcContext;
  }

  public void append(final int i) {
    ipcRequest.append(i);
  }

  public void append(final long l) {
    ipcRequest.append(l);
  }

  public void append(final boolean b) {
    ipcRequest.append(b);
  }

  public void append(final String s) {
    ipcRequest.append(s);
  }

  public void append(final Object object) {
    ipcRequest.append(object);
  }

  private static synchronized int getNextGlobalRequestID() {
    globalRequestID++;
    return (globalRequestID);
  }

  public synchronized IPCResult sendRequest() {
    int requestID = getNextGlobalRequestID(); // globalRequestID++;
    IPCResult ipcResult = new IPCResult(this, requestID, ipcContext);
    Analyzer.getInstance().IPC_session.getIPCReader().addToRequestPool(ipcResult);
    OutputStream processInput = Analyzer.getInstance().IPC_session.getOutputStream();
    try {
      String requestBody = null;
      byte[] requestBodyBytes = new byte[0]; // default empty body
      if (ipcRequest != null) {
        requestBody = ipcRequest.getBody();
        requestBodyBytes = requestBody.getBytes();
      }
      IPCRequestHeader ipcRequestHeader = null;
      if (requestType == RequestType.DEFAULT) {
        ipcRequestHeader =
            new IPCRequestHeader(
                requestID, requestType, ipcContext.getChannel(), requestBodyBytes.length);
      } else if (requestType == RequestType.HANDSHAKE) {
        ipcRequestHeader =
            new IPCRequestHeader(
                requestID, requestType, ipcContext.getChannel(), IPCProtocol.version);
      } else if (requestType == RequestType.CANCEL) {
        ipcRequestHeader =
            new IPCRequestHeader(
                requestID,
                requestType,
                ipcContext.getCancelChannel(),
                ipcContext.getCancelRequest());
      } else {
        assert false;
      }

      String headerBody = ipcRequestHeader.getBody();
      byte[] headerBodyBytes = headerBody.getBytes();
      IPCLogger.logRequest(headerBody, requestBody);

      String request = "";
      if (requestBodyBytes != null && requestBodyBytes.length > 0) {
        CurChar curChar = new CurChar();
        IPCProtocol.decodeByte(requestBodyBytes, curChar);
        request = "\"" + IPCProtocol.decodeSVal(requestBodyBytes, curChar) + "\"";
      }
      IPCLogger.logTrace(
          ipcRequestHeader.getRequestID()
              + " IPCHandle sendRequest "
              + ipcContext.getChannel()
              + " "
              + request);
      // send header and body using one write
      String request_str = headerBody;
      if (null != requestBody) {
        request_str += requestBody;
      }
      byte[] request_arr = request_str.getBytes();
      processInput.write(request_arr);
      processInput.flush();
    } catch (IOException ioe) {
      AnLog.log("sendRequest: IOException" + ioe); // ioe.getStackTrace());
      // ioe.printStackTrace();
      if (Analyzer.getInstance().known_problem_10.equals(ioe.getMessage())) {
        // Show error dialog and exit
        if (null != Analyzer.getInstance().remoteConnection) {
          String msg = AnLocale.getString("Remote connection closed");
          JOptionPane.showMessageDialog(
              (AnWindow.getInstance()).getFrame(),
              msg,
              AnLocale.getString("Error"),
              JOptionPane.ERROR_MESSAGE);
          System.err.println(msg);
        } else {
          if (Analyzer.getInstance().IPC_started != false) {
            System.err.println("Connection closed (er_print died unexpectedly)");
          } else {
            // Normal exit
            System.exit(0); // Should we simply return null here?
          }
        }
        System.exit(1); // AnWindow.getInstance().exitAction();
      }
      throw new IPC.AnIPCException(ioe);
    } catch (Exception e) {
      if (null != Analyzer.getInstance().remoteConnection) {
        String msg = AnLocale.getString("Remote connection closed");
        JOptionPane.showMessageDialog(
            (AnWindow.getInstance()).getFrame(),
            msg,
            AnLocale.getString("Error"),
            JOptionPane.ERROR_MESSAGE);
        System.err.println(msg);
        System.exit(1); // AnWindow.getInstance().exitAction();
      }
      AnLog.log("sendRequest: Exception" + e);
      // e.printStackTrace();
      throw new IPC.AnIPCException(e);
    }
    return ipcResult;
  }

  public void addIPCListener(IPCListener listener) {
    synchronized (ipcListeners) {
      ipcListeners.add(listener);
    }
  }

  public void removeIPCListener(IPCListener listener) {
    synchronized (ipcListeners) {
      ipcListeners.remove(listener);
    }
  }

  protected void fireResponse(IPCResponse ipcResponse, IPCResult ipcResult) {
    if (ipcListeners.isEmpty()) {
      return;
    }
    if (ipcResponse.getHeader().getResponseType() == IPCResponseHeader.ResponseType.ACK) {
      synchronized (ipcListeners) {
        for (IPCListener listener : ipcListeners) {
          listener.started(ipcResponse.getHeader().getRequestID());
        }
      }
    } else if (ipcResponse.getHeader().getResponseType()
        == IPCResponseHeader.ResponseType.COMPLETE) {
      synchronized (ipcListeners) {
        for (IPCListener listener : ipcListeners) {
          listener.finished(ipcResponse.getHeader().getRequestID(), ipcResult);
        }
      }
    } else if (ipcResponse.getHeader().getResponseType()
        == IPCResponseHeader.ResponseType.HANDHAKE) {
    } else if (ipcResponse.getHeader().getResponseType()
        == IPCResponseHeader.ResponseType.PROGRESS) {
      IPCProtocol.CurChar curChar = new IPCProtocol.CurChar();
      int progress = IPCProtocol.decodeByte(ipcResponse.getBody(), curChar);
      int percentage = IPCProtocol.decodeIVal(ipcResponse.getBody(), curChar);
      String what = IPCProtocol.decodeSVal(ipcResponse.getBody(), curChar);
      synchronized (ipcListeners) {
        for (IPCListener listener : ipcListeners) {
          listener.progress(ipcResponse.getHeader().getRequestID(), what, percentage);
        }
      }
    } else {
      assert false;
    }
  }
}
