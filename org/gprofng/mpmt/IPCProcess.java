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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JOptionPane;

/** Process which delegates Runtime.exec process for gp-display-text -IPC */
public final class IPCProcess extends Process {
  private OutputStream stdin;
  private InputStream stdout, stderr;

  public ExitMonitor monitor;
  private final Process delegate;
  private final Analyzer parent_Analyzer;
  public ProcListener a_Proc_Listener;

  public IPCProcess(
      final Analyzer parent,
      final Process delegate,
      final OutputStream stdin,
      final InputStream stdout,
      final InputStream stderr) {
    this.delegate = delegate;
    this.stdin = stdin;
    this.stdout = stdout;
    this.stderr = stderr;
    this.parent_Analyzer = parent;
  }

  @Override
  public InputStream getErrorStream() {
    return stderr;
  }

  @Override
  public OutputStream getOutputStream() {
    return stdin;
  }

  @Override
  public InputStream getInputStream() {
    return stdout;
  }

  @Override
  public int waitFor() throws InterruptedException {
    try {
      return delegate.waitFor();
    } catch (InterruptedException x) {
      return -1;
    }
  }

  @Override
  public int exitValue() throws IllegalThreadStateException {
    return delegate.exitValue();
  }

  @Override
  public void destroy() {
    delegate.destroy();
  }

  public synchronized void setExitListener() {
    a_Proc_Listener = new ProcListener();

    if (monitor != null) {
      monitor.interrupt();
      monitor = null;
    }
    monitor = new ExitMonitor(this, a_Proc_Listener);
    monitor.start();
  }

  public synchronized void removeExitListener() {
    a_Proc_Listener = null;

    if (monitor != null) {
      monitor.interrupt();
      monitor = null;
    }
  }

  /*-------------------------------------    INNER CLASSES    ----------------------------------------------------*/
  /**
   * Class for catching process exit.
   *
   * @see ExitListener
   */
  public final class ProcListener implements ExitListener {
    public ProcListener() {}

    /**
     * Implements method processExited of ExitListener interface
     *
     * @param p - external process Method prints error message and cleanups analyzer (standalone) or
     *     close TopComponent (netbeans module).
     * @see ExitListener
     */
    @Override
    public void processExited(final Process p) {
      String msg;
      int exit_value = -1;

      try {
        exit_value = p.exitValue();
      } catch (IllegalThreadStateException ex) {
      } // process has already exited
      switch (exit_value) {
        case -1:
          msg = null; // process has not yet terminated, default exit value
          break;
        case 0:
          msg = AnLocale.getString("Communication channel will be closed");
          break;
        case 2: // SIGINT
          msg = AnLocale.getString("gp-display-text has been interrupted.");
          break;
        case 3: // SIGQUIT
          msg = AnLocale.getString("gp-display-text has quit.");
          break;
        case 4: // SIGILL
          msg = AnLocale.getString("Illegal instruction in gp-display-text.");
          break;
        case 5: // SIGTRAP
          msg = AnLocale.getString("Trace/Breakpoint trap in gp-display-text.");
          break;
        case 6: // SIGABRT
          msg = AnLocale.getString("gp-display-text has been aborted.");
          break;
        case 7: // SIGEMT
          msg = AnLocale.getString("Emulation trap in gp-display-text.");
          break;
        case 8: // SIGFPE
          msg = AnLocale.getString("Floating point exception in gp-display-text.");
          break;
        case 9: // SIGKILL
          msg = AnLocale.getString("gp-display-text has been killed.");
          break;
        case 10: // SIGBUS
          msg = AnLocale.getString("Bus Error in gp-display-text.");
          break;
        case 11: // SIGSEGV
          msg = AnLocale.getString("Segmentation Fault in gp-display-text.");
          break;
        case 15: // SIGTERM
          msg = AnLocale.getString("gp-display-text has been terminated.");
          break;
        case 16: // SIGUSR1
          msg = AnLocale.getString("Out of memory Error in gp-display-text.");
          break;
        default: // All other signals
          msg = AnLocale.getString("gp-display-text has exited unexpectedly.");
          break;
      }

      if (exit_value != -1) {
        msg += AnLocale.getString(" Exit status is ") + exit_value;
      }
      // msg += AnLocale.getString("\nAnalyzer session will be reinitialized.");

      // if (!Analyzer.inNetBeans() && Analyzer.gui_initialized)
      //  AnWindow.getRootWindow().reinit(); // analyzer cleanup

      //            if (parent_Analyzer.get_win_size() != 0) // No SIGTERM occured (netbeans only)
      //                parent_Analyzer.endIPC(msg);
      try {
        stdin.close();
        stdout.close();
        stderr.close();
      } catch (IOException ex) {
        // ex.printStackTrace(); // No need - we are going to exit
      }
      if (parent_Analyzer.isConnected() && (!parent_Analyzer.isRemote())) {
        if (!parent_Analyzer.connectingToRemoteHost) { // not connecting?
          // Show error message and save it in the log file
          AnWindow.getInstance()
              .getExperimentsView()
              .appendLog(AnLocale.getString("Error: ") + msg);
          System.err.println(AnLocale.getString("Error: ") + msg);
          JOptionPane.showMessageDialog(
              (AnWindow.getInstance()).getFrame(),
              msg,
              AnLocale.getString("Error"),
              JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    }
  }

  /** Monitor class for listening external process to catch process exit. */
  public final class ExitMonitor extends Thread {
    private final Process proc;
    private final ExitListener listener;

    /**
     * Monitor constructor
     *
     * @param proc - external process for listening
     * @param listener - listener interface
     * @see ExitListener
     */
    ExitMonitor(final Process proc, final ExitListener listener) {
      this.proc = proc;
      this.listener = listener;
    }

    @Override
    public final void run() {
      try {
        proc.waitFor();
      } catch (InterruptedException ex) { // ignore monitor interruption
      }
      // Show error dialog - user may want to reestablish connection
      listener.processExited(proc);
      monitor = null;
    }
  }

  public interface ExitListener {
    public void processExited(Process p);
  }
}
