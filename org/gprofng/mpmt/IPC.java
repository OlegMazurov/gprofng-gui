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
package org.gprofng.mpmt;

import org.gprofng.mpmt.ipc.IPCCancelledException;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.ipc.IPCErrorReader;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCReader;
import org.gprofng.mpmt.ipc.IPCResult;
import java.io.InputStream;
import java.io.OutputStream;

public final class IPC {

  public static final Object lock;
  public static String host;
  public static int port;
  protected InputStream processOutput, processError;
  protected OutputStream processInput;
  private IPCProcess thisProcess;
  private Process delegate;
  private Analyzer parent_Analyzer;
  private String cmd; // gp-display-text path + option
  private IPCHandle ipcHandle = null;
  private IPCResult ipcResult = null;
  private IPCReader ipcReader;
  private IPCErrorReader ipcErrorReader;

  static {
    lock = new Object();
  }

  IPC(Analyzer parent) {
    parent_Analyzer = parent;
  }

  public InputStream getInputStream() {
    return processOutput;
  }

  public OutputStream getOutputStream() {
    return processInput;
  }

  public InputStream getErrorStream() {
    return processError;
  }

  public IPCReader getIPCReader() {
    return ipcReader;
  }

  public IPCErrorReader getIPCErrorReader() {
    return ipcErrorReader;
  }

  /**
   * @return trimmed gp-display-text command path plus IPC argument
   */
  public String getCmd() {
    return cmd.trim();
  }

  public void init(final String cmd, boolean run) throws Exception {
    this.cmd = cmd;
    final Runtime rt = Runtime.getRuntime();
    delegate = rt.exec(cmd);

    processOutput = delegate.getInputStream();
    processInput = delegate.getOutputStream();
    processError = delegate.getErrorStream();
    thisProcess =
        new IPCProcess(parent_Analyzer, delegate, processInput, processOutput, processError);

    if (thisProcess.monitor != null) {
      thisProcess.monitor.interrupt();
      thisProcess.monitor = null;
    }

    thisProcess.setExitListener();

    // NM Temporary solution for remote analyzer: pass flag "run" to IPCReader
    ipcReader = new IPCReader(this, run);
    ipcErrorReader = new IPCErrorReader(this, true);
  }

  public void destroyIPCProc() {
    if (thisProcess != null) {
      thisProcess.removeExitListener();
      parent_Analyzer.IPC_started = false;
      thisProcess.destroy();
      if (ipcReader != null) {
        ipcReader.stopThread();
      }
      if (ipcErrorReader != null) {
        ipcErrorReader.stopThread();
      }
    }
  }

  public int recvInt() {
    sendIPCRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      System.out.append("\nrecvInt cancelled...");
      throw new IPCCancelledException();
    }
    return ipcResult.getInt();
  }

  public String recvString() {
    sendIPCRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      System.out.append("\nrecvString cancelled...");
      throw new IPCCancelledException();
    }
    return ipcResult.getString();
  }

  public long recvLong() {
    sendIPCRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      System.out.append("\nrecvLong cancelled...");
      throw new IPCCancelledException();
    }
    return ipcResult.getLong();
  }

  public boolean recvBoolean() {
    sendIPCRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      System.out.append("\nrecvBoolean cancelled...");
      throw new IPCCancelledException();
    }
    return ipcResult.getBoolean();
  }

  public Object recvObject() {
    sendIPCRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      System.out.append("\nrecvObject cancelled...");
      throw new IPCCancelledException();
    }
    return ipcResult.getObject();
  }

  public void recvVoid() {
    sendIPCRequest();
    if (ipcResult.getCC() != IPCResult.CC.SUCCESS) {
      System.out.append("\nrecvVoid cancelled...");
      throw new IPCCancelledException();
    }
    ipcResult.getVoid(); // <=== FIXUP: is this needed????
  }

  private IPCHandle getIPCHandle() {
    if (ipcHandle == null) {
      ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, IPCContext.getCurrentContext());
    }
    return ipcHandle;
  }

  private void sendIPCRequest() {
    if (ipcHandle != null) {
      ipcResult = ipcHandle.sendRequest();
    } else {
      System.err.println("********************ERROR: IPC: ipcRequest==null");
      assert false;
    }
    ipcHandle = null;
  }

  public void send(final int i) {
    getIPCHandle().append(i);
  }

  public void send(final long l) {
    getIPCHandle().append(l);
  }

  public void send(final boolean b) {
    getIPCHandle().append(b);
  }

  public void send(final String s) {
    getIPCHandle().append(s);
  }

  public void send(final Object object) {
    getIPCHandle().append(object);
  }

  public static final class AnIPCException extends RuntimeException {

    public AnIPCException() {
      super();
    }

    public AnIPCException(final Throwable thr) {
      super(thr);
    }
  }
}
