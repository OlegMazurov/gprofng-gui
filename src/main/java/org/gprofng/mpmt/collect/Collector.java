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

package org.gprofng.mpmt.collect;

import static java.lang.Thread.sleep;
import org.gprofng.mpmt.AnFile;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Wrapper class for standalone Collect Dialog. */
public class Collector {

  public static final int COLLECTING_RUNNING = 3;
  public static final int COLLECTING_PAUSED = 4;
  public static final int COLLECTING_TERMINATED = 5;
  public static final int COLLECTING_COMPLETED = 6;
  public static final int COLLECTING_NONE = 7;
  public static final int COLLECTING_TERMINATING = 8;
  // Collection types
  public static final int PROFILE_APPLICATION = 0;
  public static final int PROFILE_RUNNING_APPLICATION = 1;
  public static final int SYSTEM_PROFILING = 2;

  private String finaldir, curr_exp_name;
  private String experimentGroup;
  protected JPanel pioPanel;
  protected JTextArea pioTextArea;
  private AnWindow window;
  private ShellCommand shellCommand;
  private Collector collector;
  protected OutputStream logStream;
  protected BufferedWriter logStreamWriter;
  protected List<String> logVector;
  protected List<String> stderr_stdout;
  private int collectorState;
  protected List<ActionListener> collectingStatusListenersList;
  private long elapsedTime = 0;
  private int exitValue = -1;
  private long processID = -1;
  private String errorMessage = null;
  private final String eol = "\n";
  private final String empty_string = "";
  private int shortDelay = 10; // 10 milliseconds

  public long getElapsedTime() {
    return elapsedTime;
  }

  public long getProcessPID() {
    return processID;
  }

  public long setProcessPID(long p) {
    return processID = p;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String m) {
    errorMessage = m;
  }

  public int getProcessExitValue() {
    return exitValue;
  }

  public void setActualExpName(String exp) {
    curr_exp_name = exp;
  }

  private int profiling_type = 0; // XXX Replace with a named const!

  /**
   * Constructor
   *
   * @param aWindow
   */
  public Collector(final AnWindow aWindow) {
    this.window = aWindow;
    collector = this;
    collectorState = COLLECTING_NONE;
    collectingStatusListenersList = new ArrayList<>();
  }

  /** Get Profiling Type */
  public int getProfilingType() {
    return profiling_type;
  }

  /** Set Profiling Type */
  public void setProfilingType(int profiling_type) {
    this.profiling_type = profiling_type;
  }

  public void addCollectingStatusListener(ActionListener al) {
    collectingStatusListenersList.add(al);
  }

  public void removeCollectingStatusListener(ActionListener al) {
    collectingStatusListenersList.remove(al);
  }

  private void fireCollectingStatusChanged(final int eventId) {
    // through all listeners
    for (Iterator<ActionListener> it = collectingStatusListenersList.iterator();
        it.hasNext(); ) {
      it.next().actionPerformed(new ActionEvent(0, eventId, empty_string));
    }
  }

  public int getCollectorState() {
    return collectorState;
  }

  private void setCollectorState(final int newSt) {
    collectorState = newSt;
    fireCollectingStatusChanged(collectorState);
  }

  public void setPioPanel(final JPanel aPioPanel) {
    this.pioPanel = aPioPanel;
  }

  public void setPioTextArea(final JTextArea aPioTextArea) {
    this.pioTextArea = aPioTextArea;
  }

  public void setLog(final OutputStream outLog) {
    logStream = outLog;
    logStreamWriter = new BufferedWriter(new OutputStreamWriter(logStream));
  }

  public void setLog(final List<String> outLog) {
    logVector = outLog;
  }

  public boolean pause(int signum) {
    if (sendSignal(signum, AnLocale.getString("Pause"))) {
      setCollectorState(COLLECTING_PAUSED);
      return true;
    } else {
      return false;
    }
  }

  public boolean resume(final int signum) {
    if (sendSignal(signum, AnLocale.getString("Resume"))) {
      setCollectorState(COLLECTING_RUNNING);
      return true;
    } else {
      return false;
    }
  }

  public boolean sample(final int signum) {
    return sendSignal(signum, AnLocale.getString("Sample"));
  }

  public boolean terminate_expt(boolean SIGINT) {
    int signal = 9; // SIGKILL
    if (SIGINT) {
      signal = 2; // SIGINT
    }
    if (sendSignal(signal, null)) {
      setCollectorState(COLLECTING_TERMINATING);
      return true;
    }
    return false;
  }

  public boolean checkWorkDir(final String work_dir_str) {
    if (work_dir_str.length() > 0) {
      AnFile curDir = window.getAnFile(work_dir_str); // IPC call
      if (curDir.exists()) {
        if (curDir.isDirectory()) {
          return true;
        }
      }
    }
    return false;
  }

  // Invoke collect command
  public void collect(
      final String target,
      final String exec_cmd,
      final String work_str,
      final String exp_name,
      final String exp_dir,
      final String env_vars,
      final String collect_output_file,
      final String experimentGroup) {
    final Thread worker;

    this.experimentGroup = experimentGroup;
    setProcessPID(-1); // Not started yet
    if (null == window.getAnalyzer().remoteConnection) { // checkWorkDir does not support remote yet
      if (!checkWorkDir(work_str)) {
        String err =
            AnLocale.getString("Collector error: cannot access working directory:\n" + work_str);
        AnUtility.showMessage(window.getFrame(), err, JOptionPane.ERROR_MESSAGE);
        return;
      }
      // Check experiment directory
      String full_exp_dir = exp_dir;
      boolean check_full_exp_dir = false;
      if (full_exp_dir == null) {
        full_exp_dir = empty_string;
      } else {
        full_exp_dir += "/";
      }
      if (exp_name != null) {
        full_exp_dir += exp_name;
      }
      if (full_exp_dir != null) {
        if (full_exp_dir.endsWith(".er")) {
          int len = full_exp_dir.length();
          for (int i = 4; i < len; i++) { // Is it an autoadjusted name?
            int c = full_exp_dir.charAt(len - i);
            if (('0' <= c) && (c <= '9')) {
              continue;
            }
            if (c == '.') {
              if (i > 4) {
                break; // autoadjusted name: "*.[0-9].er"
              }
            }
            check_full_exp_dir = true; // not of form "*.[0-9].er"
            break;
          }
        }
      }
      if (check_full_exp_dir && checkWorkDir(full_exp_dir)) {
        String err =
            AnLocale.getString(
                "Collector error: experiment directory already exists:\n" + full_exp_dir);
        AnUtility.showMessage(window.getFrame(), err, JOptionPane.ERROR_MESSAGE);
        return;
      }
    }

    curr_exp_name = exp_name;
    worker =
        new Thread("Collector Process Thread") {
          @Override
          public void run() {
            boolean collect_failed = false;
            try {
              stderr_stdout = new ArrayList<>();
              AnUtility.setLibPath();
              String remoteConnection = window.getAnalyzer().remoteConnectCommand;
              String remoteShell = window.getAnalyzer().remoteShell;
              String remoteHost = window.getAnalyzer().remoteHost;
              String cmd = exec_cmd;
              String dir = work_str;
              // System.out.println("Collector: cmd = " + cmd); // DEBUG
              ShellCommand sc = new ShellCommand();
              shellCommand = sc;
              sc.setRemoteConnection(remoteConnection, remoteShell, remoteHost);
              if (null == remoteConnection) {
                sc.run(cmd);
              } else {
                sc.runRemoteCmd(dir, cmd);
              }
              setCollectorState(COLLECTING_RUNNING);

              final String STDIN_WRITER = "Collector Stdin Writer";
              final String STDOUT_READER = "Collector Stdout Reader";
              final String STDERR_READER = "Collector Stderr Reader";

              // Create StreamThread STDIN_WRITER
              StreamThread tr0 = new StreamThread();
              tr0.setName(STDIN_WRITER);
              tr0.setTask(tr0.WRITE_STDIN, sc);
              tr0.start();

              // Create StreamThread STDOUT_READER
              StreamThread tr1 = new StreamThread();
              tr1.setName(STDOUT_READER);
              tr1.setTask(tr1.READ_STDOUT, sc);
              if (logVector == null) {
                stderr_stdout = new ArrayList<>();
              } else {
                stderr_stdout = logVector;
              }
              tr1.setOut(stderr_stdout);
              tr1.start();

              // Create StreamThread STDERR_READER
              StreamThread tr2 = new StreamThread();
              tr2.setName(STDERR_READER);
              tr2.setTask(tr2.READ_STDERR, sc);
              // NM1 Vector stderr = new Vector();
              tr2.setOut(stderr_stdout);
              tr2.start();

              // Remote connection
              if (null != remoteConnection) {
                // ConnectionChooser cc = window.connect;
                // if (null == cc) {
                //    System.err.println("Collector: ERROR: remote connection is not established
                // yet");
                // } else {
                // cc.setVisible(true, window.getWindowID());
                Analyzer parent_analyzer = Analyzer.getInstance();
                try {
                  parent_analyzer.startRemoteCollect(collector, null /*cc*/);
                } catch (Exception e) {
                  sc.interrupt();
                }
                // cc.setVisible(false, window.getWindowID());
                // }
              }

              // Read standard output
              long time = System.currentTimeMillis();
              if (logVector == null) {
                String line;
                // NM1 boolean shouldWait = false;
                while ((sc.isRunning() == true) || (stderr_stdout.size() > 0)) {
                  // copy output to the log file
                  try {
                    // NM1 line = sc.readOutput(shouldWait);
                    // NM1 if ((null != line) && (line.length() > 0)) {
                    // NM1     writeln(line);
                    // NM1 } else {
                    // copy stderr_stdout to the log file
                    while (stderr_stdout.size() > 0) { // NM1
                      line = stderr_stdout.get(0).toString();
                      stderr_stdout.remove(0);
                      writeln(line);
                    }
                    // NM1 }
                  } catch (Exception e) {
                    // reply="# Exception: " + e; // Ignore exception
                  }
                }
                // NM1 shouldWait = true;
              }
              elapsedTime = System.currentTimeMillis() - time;
              exitValue = sc.exitValue();
              setCollectorState(COLLECTING_COMPLETED);
              removeTempFile(collect_output_file);
            } catch (/*IO*/ Exception e) {
              collect_failed = true;
              // e.printStackTrace();
              // printException(e, AnLocale.getString("Collector.collect: I/O exception 2"));
              // System.err.println("Collector.collect: command failed: " + exec_cmd);
            }
            if (collect_failed) {
              // Try to get error message
              ShellCommand sc = shellCommand;
              String err = AnLocale.getString("Collector command:\n\n" + exec_cmd + "\n\n");
              writeln(err);
              boolean closed = false;
              String prev_line = null;
              while (!closed) {
                try {
                  boolean shouldWait = true; // false;
                  String line = sc.readError(shouldWait);
                  if (null == line) {
                    break;
                  }
                  if (line.length() > 0) {
                    writeln(line);
                    if (line.equals(prev_line)) {
                      continue; // Sometimes identical line appear several times
                    } else {
                      prev_line = line;
                    }
                    if (line.contains("Run collect with no arguments for a usage message")) {
                      continue; // Not useful a part of the error message
                    }
                    err += line + eol;
                  }
                } catch (Exception e) {
                  closed = true;
                  break;
                }
              }
              setErrorMessage(err);
              // AnUtility.showMessage(window.getFrame(), err, JOptionPane.ERROR_MESSAGE);
            }
            shellCommand = null;
          }
        };
    worker.start();
  }

  /**
   * Read Collect Output File
   *
   * @param collect_output_file
   */
  protected void read_collect_output_file(String collect_output_file) {
    if (null == collect_output_file) {
      return;
    }
    try {
      File f = new File(collect_output_file);
      if (f == null) {
        return;
      }
      if (f.exists()) {
        // open file, read, try to find process ID
        String line;
        BufferedReader br = new BufferedReader(new FileReader(f));
        while ((line = br.readLine()) != null) {
          String pattern1 = "Creating experiment directory ";
          int idx = line.indexOf(pattern1);
          if (idx >= 0) {
            String substr = line.substring(idx + pattern1.length());
            idx = substr.indexOf(" ");
            if (idx > 1) {
              String expname = substr.substring(0, idx); // test.*.er
              setActualExpName(expname);
            }
            String pattern2 = "(Process ID: ";
            int n = line.indexOf(pattern2);
            if (n >= 0) {
              n += pattern2.length();
              long p = 0;
              int d = 0;
              while ((d >= 0) && (d <= 9)) {
                p = p * 10 + d;
                d = line.charAt(n) - '0';
                n++;
              }
              if (p > 0) {
                setProcessPID(p);
                return;
              }
            }
          }
          String pattern3 = "Target `";
          if (line.startsWith(pattern3)) {
            String pattern31 = "' not found";
            int n = line.indexOf(pattern31);
            if (n > 0) {
              setProcessPID((long) -2);
              setErrorMessage(line);
              return;
            }
          }
          String pattern4 = "Unable to create directory ";
          if (line.startsWith(pattern4)) {
            String pattern41 = " -- File exists";
            int n = line.indexOf(pattern41);
            if (n > 0) {
              setProcessPID((long) -4);
              setErrorMessage(line);
              return;
            }
          }
          String pattern5 = "The HW counter configuration could not be loaded:";
          if (line.startsWith(pattern5)) {
            setProcessPID((long) -3);
            setErrorMessage(line);
            return;
          }
        }
        // sleep(shortDelay); // sleep 10 milliseconds
      }
    } catch (/*IO*/ Exception e) {
      // e.printStackTrace();
      printException(e, AnLocale.getString("Collector.collect: I/O exception 4"));
      System.err.println("Collector.collect: cannot read file " + collect_output_file);
    }
  }

  /**
   * Remove Temporary File
   *
   * @param temp_file_name
   */
  protected void removeTempFile(final String temp_file_name) {
    if (temp_file_name != null) {
      try {
        // Remove temporary file
        File f = new File(temp_file_name);
        if (f.exists()) {
          f.delete();
        }
      } catch (/*IO*/ Exception e) {
        // Ignore it
      }
    }
  }

  // Output
  protected void write(final String line) {
    try {
      logStreamWriter.write(line);
      logStreamWriter.flush();
      // ??            logStream.flush();
    } catch (IOException e) {
      // don't print "Read end dead" message
      printException(e, AnLocale.getString("Collector.write: I/O exception 5"));
    }
  }

  // Output with new line
  protected void writeln(final String line) {
    write(line + eol);
  }

  // Handle pause/resume signal
  private boolean sendSignal(final int signal, final String cmd) {
    if (processID < 2) {
      // Process lost
      String err =
          AnLocale.getString(
              "Error: cannot send signal. Process ID is not correct (" + processID + ").");
      AnUtility.showMessage(window.getFrame(), err, JOptionPane.ERROR_MESSAGE);
      return false;
    }
    String result = sendSignal(processID, signal);
    if (null == result) {
      return true;
    } else {
      return false;
    }
    /*
    ShellCommand sc = new ShellCommand();
    sc.setRemoteConnection(window.getAnalyzer().remoteConnection);
    String exe_cmd = "kill -" + signal + " " + processID;
    try {
    sc.run(exe_cmd);
    } catch (Exception e) {
    return false;
    }
    return true;
    */
  }

  /**
   * Gets Current Process Output
   *
   * @return String output
   */
  public String getCurrentOutput() {
    String output = empty_string;
    if (shellCommand != null) {
      output = shellCommand.output;
    }
    return output;
  }

  /**
   * Write one character to process input and flush
   *
   * @param s
   */
  public void writeInputChar(char c) throws Exception {
    shellCommand.writeInputChar(c);
  }

  public boolean isProcRunning() { // terminate button is enabled
    return (getCollectorState() == COLLECTING_RUNNING);
  }

  private static void printException(final Exception e, final String annot_msg) {

    if (annot_msg != null) {
      System.err.println(annot_msg + ": " + e.getMessage());
    } else {
      System.err.println(e.getMessage());
    }
  }

  public String getActualExpName() {
    final String slash = "/";
    if (curr_exp_name.startsWith(slash) || (null == finaldir)) {
      return curr_exp_name;
    }
    return (finaldir + slash + curr_exp_name);
  }

  public String getNextExpName() {
    return Collector.getExpName1(finaldir);
  }

  public String getExperimentGroup() {
    return experimentGroup;
  }

  /*-------------------------------------    INNER CLASSES    ----------------------------------------------------*/
  /**
   * Class ShellCommand executes an external program and supports input/output functions
   *
   * <p>Main functions: run a program write to stdin read from stdout read from stderr return exit
   * status
   */
  class ShellCommand {

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
    protected String remoteShell;
    protected String remoteHost;

    // NM    protected static ByteArrayOutputStream lastOutput;
    /** Creates a new instance of ShellCommand */
    public ShellCommand() {
      processOutput = null;
      processError = null;
      thisProcess = null;
      rt = Runtime.getRuntime();
      interrupted = false;
      remoteConnection = null;
      shArgs = null;
      errors = empty_string;
      output = empty_string;
    }

    /**
     * Set the remote connection
     *
     * @param args - connection
     */
    public void setRemoteConnection(String rc, String rs, String rh) {
      remoteConnection = rc;
      remoteShell = rs;
      remoteHost = rh;
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
            ret += eol;
          }
          return (ret);
        }
        ret = reader.readLine();
        if (ret != null) {
          ret += eol;
        }
      } catch (IllegalThreadStateException ie) {
        return (empty_string);
      }
      return (ret);
    }

    /**
     * Gets Current Process Output
     *
     * @return String output
     */
    public String getCurrentOutput() {
      return output;
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
     * Write one character to standard input stream
     *
     * @param c - a character
     */
    public void writeInputChar(char c) throws Exception {
      processInput.print(c);
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
      ss[1] = "-e";
      ss[2] = cmnd;
      interrupted = false;
      try {
        // System.err.println("DEBUG:run rt.exec(): " + ss[0] + " " + ss[1] + " " + ss[2]); // DEBUG
        // (perf test)
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
    }

    /**
     * Run a program
     *
     * @param cmnd - command line
     */
    public void runRemoteCmd(String dir, String cmnd) throws Exception {
      String ss = remoteConnection + " sh -ec \"(";
      if (dir != null) {
        ss += "cd " + dir + " && ";
      }
      ss += cmnd + ")\"";
      interrupted = false;
      try {
        thisProcess = rt.exec(ss);
        InputStream os = thisProcess.getInputStream();
        InputStream os_err = thisProcess.getErrorStream();
        OutputStream is = thisProcess.getOutputStream();
        processOutput = new BufferedReader(new InputStreamReader(os));
        processError = new BufferedReader(new InputStreamReader(os_err));
        processInput = new PrintStream(is, true);
      } catch (Exception ee) {
        String msg = "Command \"" + ss + "\" failed:\n" + ee.toString();
        throw new Exception(msg);
      }
    }
  }

  /* End of class ShellCommand */

  class StreamThread extends Thread {

    private ShellCommand sc = null;
    /* Tasks */
    public final int WRITE_STDIN = 0;
    public final int READ_STDOUT = 1;
    public final int READ_STDERR = 2;
    public final int NO_TASK = -1;
    private int task = NO_TASK;
    /* Connection */
    public final int DISCONNECTED = 0;
    public final int CONNECTED = 1;
    private int connect = DISCONNECTED;
    private long total_time = 0;
    private boolean tosleep = true;
    private boolean todie = false;
    private int shortDelay = 10; // 10 milliseconds
    private int mediumDelay = 100; // 100 milliseconds
    private int longDelay = 1000; // 1000 milliseconds (1 second)

    /** Creates a new instance of StreamThread */
    public StreamThread() {
      long t = System.currentTimeMillis();
    }

    public int setTask(final int TASK, ShellCommand sc) {
      task = TASK; // Q: check TASK ?
      this.sc = sc;
      return (task);
    }

    public int checkConnection() {
      return (connect);
    }

    public String writeInput(String request) {
      if (request == null) {
        return null; // Nothing to send
      }
      String reply = null;
      request = request + eol;
      logMessage(request);
      try {
        // log.severe("sc.writeInput("+request+");"); // FIXUP (remove)
        sc.writeInput(request);
      } catch (Exception e) {
        reply = "# Exception: " + e; // FIXUP (i18n)
        reportEvent(reply);
      }
      return (reply);
    }

    public int readOutput(boolean shouldWait) {
      String output;
      int len = 0;
      try {
        output = sc.readOutput(shouldWait);
        len = output.length();
        if (len > 0) {
          reportEvent(output);
        }
      } catch (Exception e) {
        // reply="# Exception: " + e; // Ignore exception
      }
      return len;
    }

    public int readError(boolean shouldWait) {
      String output;
      int len = 0;
      try {
        output = sc.readError(shouldWait);
        len = output.length();
        if (len > 0) {
          reportEvent(output);
        }
      } catch (Exception e) {
        // reply="# Exception: " + e; // Ignore exception
      }
      return len;
    }

    @Override
    public void run() {

      // String output;
      boolean shouldWait = false;
      /* Variables for time in milliseconds */
      // long time0, time1, time2; // DEBUG (Performance test)

      // time0 = System.currentTimeMillis();
      // time1 = time0;
      // time2 = time0;
      while (true) {
        if (sc != null) {
          if (
          /*sc.isRunning()*/ task != NO_TASK) {
            connect = 1;
            if (task == WRITE_STDIN) {
              // NM                        writeInput(parent.getCommandToSend());
            }
            int len = 0;
            if (task == READ_STDOUT) {
              len = readOutput(shouldWait);
            }
            if (task == READ_STDERR) {
              len = readError(shouldWait);
            }
            try {
              if (len <= 0) { // empty line
                if (!sc.isRunning()) {
                  break;
                }
              }
              if ((len <= 0) // empty line
                  || (task == WRITE_STDIN)) { // DEBUG (Windows)
                sleep(shortDelay); // a short delay
              }
            } catch (InterruptedException tie50) {
              // sleep 50 milliseconds
            }
            continue;
            // } else {
            //    break; // stop task
          }
          // shouldWait = true;
          if (task == READ_STDOUT) {
            /* Try to read last messages */
            readOutput(shouldWait);
          }
          if (task == READ_STDERR) {
            /* Try to read last messages */
            readError(shouldWait);
          }
        }
        break;
      }
      // NM        parent.reportProcessExited();
      // time2 = System.currentTimeMillis();
      // total_time = ((time2-time0)/1000);
    }

    private List<String> out = null; // NM1 new Vector();
    // private Boolean out_sync = true; // NM1

    public void setOut(List<String> out) {
      this.out = out;
    }

    private void reportEvent(String message) {
      // NM        parent.reportEvent(message);
      if (null != out) { // NM1
        // synchronized (out_sync) { // NM1
        out.add(message);
        // }
      }
    }

    private void logMessage(String message) {
      // NM        parent.logMessage(message);
    }
  }

  /* End of StreamThread */

  /**
   * Sends signal to the process specified by process id
   *
   * @param pid Process ID
   * @param sig Signal number
   * @return result
   */
  public static final String sendSignal(long pid, int sig) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("sendSignal");
      anWindow.IPC().send(pid);
      anWindow.IPC().send(sig);
      return anWindow.IPC().recvString();
    }
  }

  /**
   * Get experimentsDisp name specified by directory name
   *
   * @param dir_name
   * @return result
   */
  public static String getExpName1(final String dir_name) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getExpName1");
      anWindow.IPC().send(dir_name);
      return anWindow.IPC().recvString();
    }
  }

  /**
   * A generic method to get Available Collector Options.
   *
   * @param collectorOptions - array of arrays of Strings
   * @return availableCollectorOptions - array of arrays of Strings
   */
  public static Object[] getAvailableCollectorOptions(final Object[] collectorOptions) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getAvailableCollectorOptions");
      anWindow.IPC().send(0);
      anWindow.IPC().send(collectorOptions);
      return (Object[]) anWindow.IPC().recvObject();
    }
  }

  /**
   * A method to get list of running processes.
   *
   * @param format - format of output, e.g. "/bin/ps -ef"
   * @return String - list of processes
   */
  public static String getRunningProcesses(final String format) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getRunningProcesses");
      anWindow.IPC().send(format);
      return anWindow.IPC().recvString();
    }
  }

  /**
   * A method to get OS family.
   *
   * @return String - OS family, e.g. "Solaris" or "Linux"
   */
  public static String getOSFamily() {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getOSFamily");
      return anWindow.IPC().recvString();
    }
  }

  /**
   * A generic method to get Collector control value.
   *
   * @param control - String
   * @return value - String
   */
  public static String getCollectorControlValue(final String control) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getCollectorControlValue");
      anWindow.IPC().send(0);
      anWindow.IPC().send(control);
      return anWindow.IPC().recvString();
    }
  }

  /**
   * A generic method to set Collector control value.
   *
   * @param control - String
   * @param value - String
   * @return result - String
   */
  public static String setCollectorControlValue(final String control, final String value) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      anWindow.IPC().send("setCollectorControlValue");
      anWindow.IPC().send(0);
      anWindow.IPC().send(control);
      anWindow.IPC().send(value);
      String s = anWindow.IPC().recvString();
      AnUtility.checkIPCOnWrongThread(true);
      return s;
    }
  }

  /**
   * A generic method to unset Collector control value (restore the default value).
   *
   * @param control - String
   * @return result - String
   */
  public static String unsetCollectorControlValue(final String control) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      anWindow.IPC().send("unsetCollectorControlValue");
      anWindow.IPC().send(0);
      anWindow.IPC().send(control);
      String s = anWindow.IPC().recvString();
      AnUtility.checkIPCOnWrongThread(true);
      return s;
    }
  }
}
