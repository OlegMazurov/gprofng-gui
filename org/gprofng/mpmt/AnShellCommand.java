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

package org.gprofng.mpmt;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Class ShellCommand executes an external program and supports input/output functions
 *
 * <p>Main functions: run a program write to stdin read from stdout read from stderr return exit
 * status
 */
class AnShellCommand {

  private BufferedReader processOutput;
  private BufferedReader processError;
  private PrintStream processInput;
  private Runtime rt;
  protected boolean interrupted;
  protected Process thisProcess;
  protected String shArgs;
  protected String errors;
  protected String output;
  protected String remoteConnection;

  // NM    protected static ByteArrayOutputStream lastOutput;

  /** Creates a new instance of ShellCommand */
  public AnShellCommand() {
    processOutput = null;
    processError = null;
    thisProcess = null;
    rt = Runtime.getRuntime();
    interrupted = false;
    shArgs = null;
    errors = "";
    output = "";
  }

  /**
   * Set the remote connection
   *
   * @param args - connection
   */
  public void setRemoteConnection(String args) {
    if (null == args) {
      remoteConnection = null;
      return;
    }
    remoteConnection = new String(args);
  }

  /**
   * Set the arguments
   *
   * @param args - arguments
   */
  public void setShellArgs(String args) {
    shArgs = new String(args);
  }

  /** Interrupt and destroy the process */
  public void interrupt() {
    interrupted = true;
    thisProcess.destroy();
    thisProcess = null;
  }

  /** Return exit status */
  public int exitValue() {
    if (interrupted) {
      return (-1);
    }
    try {
      thisProcess.waitFor();
    } catch (Exception e) {
      return (-2);
    }
    return (thisProcess.exitValue());
  }

  /** Check if the process is running */
  public boolean isRunning() {
    try {
      int ev = thisProcess.exitValue();
    } catch (IllegalThreadStateException ie) {
      return (true);
    } catch (Exception ie) {
      return (false);
    }
    return (false);
  }

  /**
   * Read from a stream
   *
   * @param reader - a stream
   */
  private String readStream(BufferedReader reader) throws Exception {
    String ret = null;
    try {
      if (!reader.ready()) {
        int ev = thisProcess.exitValue();
        ret = reader.readLine();
        if (ret != null) {
          ret += "\n";
        }
        return (ret);
      }
      ret = reader.readLine();
      if (ret != null) {
        ret += "\n";
      }
    } catch (IllegalThreadStateException ie) {
      return ("");
    }
    return (ret);
  }

  /**
   * Read from standard output stream
   *
   * @param shouldWait - a flag (if true, wait while read is done)
   */
  public String readOutput(boolean shouldWait) throws Exception {
    String ret;
    if (shouldWait) {
      ret = processOutput.readLine();
    } else {
      ret = readStream(processOutput);
    }
    if (ret != null) {
      output += ret;
    }
    return (ret);
  }

  /**
   * Read from standard error stream
   *
   * @param shouldWait - a flag (if true, wait while read is done)
   */
  public String readError(boolean shouldWait) throws Exception {
    String ret;
    if (shouldWait) {
      ret = processError.readLine();
    } else {
      ret = readStream(processError);
    }
    return (ret);
  }

  /**
   * Write to standard input stream
   *
   * @param str - a message
   */
  public void writeInput(String str) throws Exception {
    processInput.print(str);
    processInput.flush();
  }

  /**
   * Run a program
   *
   * @param dirname - working directory
   * @param cmnd - command line
   */
  public void run(String dirname, String cmnd) throws Exception {
    if (null != dirname) {
      cmnd = "cd " + dirname + " && " + cmnd;
    }
    run(cmnd);
  }

  /**
   * Run a program
   *
   * @param cmnd - command line
   */
  public void run(String cmnd) throws Exception {
    String[] ss = new String[3];
    ss[0] = "sh"; // NM ss[0] = "/bin/sh";
    if (shArgs == null) {
      ss[1] = new String("-ec");
    } else {
      ss[1] = new String(shArgs);
    }
    // ss[2] = new String(cmnd + " 2>&1");
    if (null != remoteConnection) {
      cmnd = remoteConnection + " ' " + cmnd + " '";
    }
    ss[2] = new String(cmnd);

    interrupted = false;

    try {
      // System.err.println("DEBUG: rt.exec(): sh -ce "+cmnd); // DEBUG (perf test)
      thisProcess = rt.exec(ss);
      InputStream os = thisProcess.getInputStream();
      InputStream os_err = thisProcess.getErrorStream();
      OutputStream is = thisProcess.getOutputStream();
      processOutput = new BufferedReader(new InputStreamReader(os));
      processError = new BufferedReader(new InputStreamReader(os_err));
      processInput = new PrintStream(is, true);
    } catch (Exception ee) {
      String msg = "Command \"" + cmnd + "\" failed:\n" + ee.toString();
      throw new Exception(msg);
    }
    return;
  }
} /* End of class AnShellCommand */
