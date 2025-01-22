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

import java.io.PrintStream;
import java.util.Date;
import java.util.Formatter;

/*
 * Log Levels checks the following bits in the value of SP_ANALYZER_LOG_IPC
 * 1: log packages
 * 2: include time stamps
 * 4: log full package. Default is to trunckate packages at ~80 chars
 * 8: tracing
 *
 * Example:
 *   SP_ANALYZER_LOG_IPC=0 no logging
 *   SP_ANALYZER_LOG_IPC=1 log packages
 *   SP_ANALYZER_LOG_IPC=3 log packages with timestamps (milliseconds since start)
 *   SP_ANALYZER_LOG_IPC=8 tracing
 */
public class IPCLogger {

  private static final String ENVVAR = "SP_ANALYZER_LOG_IPC";
  private static final String DELAYENVVAR = "SP_ANALYZER_IPC_DELAY";
  private static PrintStream printStream = System.out;
  private static long now = new Date().getTime();
  private static int maxNoCharsLogged = 140;
  private static int noCharsLogged = 0;
  private static boolean logPackage = false;
  private static boolean logTimeStamp = false;
  private static boolean logFullLine = false;
  private static boolean logTrace = false;
  private static Object lock = new Object();
  private static int ipcDelay = 0;

  static {
    // Log level
    int logLevel = 0;
    String logIpc = System.getenv(ENVVAR);
    if (logIpc == null) {
      logIpc = System.getProperty(ENVVAR);
    }

    // Delay (to similuate slow connection (remote)
    ipcDelay = 0;
    String ipcDelayString = System.getenv(DELAYENVVAR);
    if (ipcDelayString == null) {
      ipcDelayString = System.getProperty(DELAYENVVAR);
    }
    if (ipcDelayString != null) {
      try {
        ipcDelay = new Integer(ipcDelayString).intValue();
      } catch (NumberFormatException nfe) {
      }
    }
    if (logIpc != null) {
      try {
        if (logIpc.startsWith("0x") || logIpc.startsWith("0X")) {
          logLevel = Integer.parseInt(logIpc.substring(2), 16);
        } else {
          logLevel = Integer.parseInt(logIpc);
        }
        logPackage = (logLevel & 1) > 0;
        logTimeStamp = (logLevel & 2) > 0;
        logFullLine = (logLevel & 4) > 0;
        logTrace = (logLevel & 8) > 0;
      } catch (NumberFormatException nfe) {
      }
      if (logLevel > 0) {
        printStream.println(
            "IPC logging:  trace="
                + logTrace
                + " fullLine="
                + logFullLine
                + " timestamp="
                + logTimeStamp
                + " package="
                + logPackage
                + " now="
                + now);
      }
    }

    if (ipcDelay > 0) {
      String txt =
          "Adding a "
              + ipcDelay
              + "ms delay to all IPC packages to simulate a slow (remote) connection.";
      System.err.println(txt);
    }
  }

  public static void logRequest(String headerBody, String requestBody) {
    if (!logPackage) {
      return;
    }
    synchronized (lock) {
      printStream.println();
      printTimeStamp();
      printStream.print("REQUEST: ");
      noCharsLogged = 0;
      logRequest(headerBody);
      logRequest(" ");
      logRequest(requestBody);
    }
  }

  private static void logRequest(String s) {
    if (s == null) {
      return;
    }
    if (!logPackage) {
      return;
    }
    if (!logFullLine && noCharsLogged >= maxNoCharsLogged) {
      return;
    }
    if (logFullLine) {
      printStream.print(s);
      return;
    }
    if (noCharsLogged + s.length() >= maxNoCharsLogged) {
      s = s.substring(0, maxNoCharsLogged - noCharsLogged);
    }
    printStream.print(s);
    noCharsLogged += s.length();
    if (noCharsLogged >= maxNoCharsLogged) {
      printStream.print("...");
      noCharsLogged += 3;
    }
  }

  public static void logResponse(byte[] responseHeaderBytes, byte[] responseBody) {
    if (!logPackage) {
      return;
    }
    synchronized (lock) {
      printStream.println();
      printTimeStamp();
      printStream.print(".......: ");
      noCharsLogged = 0;
      logResponse(responseHeaderBytes);
      if (responseBody != null) {
        logResponse(' ');
        logResponse(responseBody);
      }
    }
  }

  private static void logResponse(char c) {
    if (!logPackage) {
      return;
    }
    if (!logFullLine && noCharsLogged >= maxNoCharsLogged) {
      return;
    }
    printStream.print(c);
    noCharsLogged++;
    if (logFullLine) {
      return;
    }
    if (noCharsLogged >= maxNoCharsLogged) {
      printStream.print("...");
      noCharsLogged += 3;
    }
  }

  private static void logResponse(byte[] bytes) {
    if (!logPackage) {
      return;
    }
    for (byte b : bytes) {
      logResponse((char) b);
    }
  }

  public static void logTrace(String trace) {
    if (!logTrace) {
      return;
    }
    printTimeStamp();
    printStream.println(trace);
  }

  private static void printTimeStamp() {
    if (logTimeStamp) {
      printStream.print(timeStamp());
    }
  }

  private static String timeStamp() {
    long ts = new Date().getTime() - now;
    return new Formatter().format("%7d: ", ts).toString();
  }

  public static long getTimeStamp() {
    long ts = new Date().getTime() - now;
    return ts;
  }

  public static int getIpcDelay() {
    return ipcDelay;
  }

  public static void setIpcDelay(int id) {
    ipcDelay = id;
  }
}
