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

import org.gprofng.mpmt.IPC;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IPCErrorReader implements Runnable {

  private IPC ipc;
  private Map<Integer, IPCResult> requestPool = new HashMap<Integer, IPCResult>();
  private final int READY_TO_RUN = 1;
  private final int RUNNING = 2;
  private final int REQUEST_TO_STOP = 3;
  private final int CONNECTING =
      4; // NM Temporary solution for remote analyzer: special state "CONNECTING"
  private int state = READY_TO_RUN;
  private String unknown_input = "";

  public IPCErrorReader(IPC ipc) {
    this.ipc = ipc;
    state = RUNNING;
    new Thread(this, "IPCErrorReader").start();
  }

  // NM Temporary solution for remote analyzer: use flag "run" to decide how to read data
  public IPCErrorReader(IPC ipc, boolean run) {
    this.ipc = ipc;
    if (run) {
      state = RUNNING;
    } else {
      state = CONNECTING;
    }
    new Thread(this, "IPCErrorReader").start();
  }

  /** Sets request to stop IPCErrorReader thread */
  public void stopThread() {
    state = REQUEST_TO_STOP;
    try {
      ipc.getErrorStream().close();
    } catch (IOException ex) {
    }
  }

  /** //NM Temporary solution for remote analyzer: Sets request to run IPCReader thread */
  public void runThread() {
    state = RUNNING;
  }

  /** Sets request to run IPCErrorReader thread */
  public String getUnknownInput() {
    return unknown_input;
  }

  /**
   * Reads bytes from standard error stream.
   *
   * @param buffer
   * @return bytes read (buffer.length())
   */
  private int readFromErrorStream(byte[] buffer) {
    try {
      int nBytesToRead = buffer.length;
      int nBytesRead = 0;
      while (nBytesRead < nBytesToRead) {
        int got = ipc.getErrorStream().read(buffer, nBytesRead, nBytesToRead - nBytesRead);
        if (got < 0) {
          if (state == REQUEST_TO_STOP) {
            return 0;
          }
          if (state == CONNECTING) {
            return got;
          }
          throw new IPC.AnIPCException();
        }
        nBytesRead += got;
      }
      return nBytesToRead;
    } catch (IOException ioe) {
      if (state == REQUEST_TO_STOP) {
        return 0;
      }
      throw new IPC.AnIPCException(ioe);
    }
  }

  // @Override
  public void run() {
    while (true) {
      try {
        byte[] buffer_1 = new byte[1];
        int nBytes = readFromErrorStream(buffer_1);
        if (state == REQUEST_TO_STOP) { // Connection closed
          break;
        }
        if (nBytes < 0) { // Connection error
          break;
        }
        for (int i = 0; i < nBytes; i++) {
          char c = (char) buffer_1[i];
          if (c == '\n') {
            System.err.println(unknown_input);
            unknown_input = "";
          } else {
            unknown_input += c;
          }
        }
        continue;
      } catch (IPC.AnIPCException ae) {
        break; // gp-display-text closed stderr
      }
    }
  }
}
