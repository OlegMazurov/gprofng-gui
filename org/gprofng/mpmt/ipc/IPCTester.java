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

import org.gprofng.mpmt.util.gui.AnUtility;

public class IPCTester {
  public static int ipcTestCounter = 0;

  public static class IPCTestSender implements Runnable {
    @Override
    public void run() {
      System.out.println("\nStart");
      for (int i = 0; i < 25; i++) {
        if (i == 10) {
          IPCLogger.setIpcDelay(500);
        }
        IPCHandle ipcHandle = IPCHandle.newHandshakeIPCHandle();
        IPCResult ipcResult = ipcHandle.sendRequest(); // no body
        IPCTestReader ccReader = new IPCTestReader(ipcResult, ipcTestCounter++);
        System.out.println("Request " + ipcTestCounter + " sent");
        AnUtility.dispatchOnAWorkerThread(ccReader, "CCReader" + " " + ipcTestCounter);
      }
      System.out.println("End");
    }
  }

  private static class IPCTestReader implements Runnable {
    IPCResult ipcResult = null;
    int counter;

    public IPCTestReader(IPCResult ipcResult, int counter) {
      this.ipcResult = ipcResult;
      this.counter = counter;
    }

    @Override
    public void run() {
      IPCResult.CC cc = ipcResult.getCC(); // blocking
      System.out.println("Result " + counter + " received: " + cc);
    }
  }
}
