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

import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.collect.Collector;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCLogger;
import org.gprofng.mpmt.ipc.IPCProtocol;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.metrics.MetricColors;
import org.gprofng.mpmt.metrics.MetricColors.MetricColor;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.remote.ConnectionDialog;
import org.gprofng.mpmt.remote.ConnectionManager;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;

public final class Analyzer {

  // Runtime environment
  public static final String PERFORMANCE_ANALYZER_RELEASE_NAME = "gprofng GUI";
  public static final String jvm_ver = System.getProperty("java.version", "unknown");
  public static final String jvm_home = System.getProperty("java.home", "unknown");
  public static final String home_dir =
      UserPref.getHomeDirectory(); // System.getProperty("user.home", ".");
  private String workingDirectory = System.getProperty("user.dir", ".");
  public String os_name = null;
  public String er_print = null;
  public String localHost = null;
  public String remoteConnectCommand = null; // For example, "ssh USER@HOST"
  public String remoteGprofngPath = null;
  public String remoteConnection = null;
  public String remoteHost = null;
  public String remoteShell = "/usr/bin/ssh";
  public static ClassLoader cls_loader = null;
  public boolean normalSelection = false; // Right click changes selection
  public boolean IPC_started = false;
  public boolean connectingToRemoteHost = false; // true during the connecting process
  public boolean connectingToRemoteHostEnabled = true;
  public boolean profileRunningProcessEnabled = false;
  public boolean kernelProfilingEnabled = false;
  public boolean old_IPC_status = false;
  public int cur_id = 0;
  public static boolean compareMode = false;

  public static String fdversion, fdhome, licpath, licsts;
  public IPC IPC_session = null;
  public IPC old_IPC_session = null;
  // the variables to control the availability of certain menus and toolbars
  // these are default values for all IPC sessions
  public static boolean m_is_open_exp_avail_default = true;
  public static boolean m_is_aggregate_exp_avail_default = true;
  public static boolean m_is_compare_exp_avail_default = true;
  public static boolean m_is_print_dsp_avail_default = true;
  public static boolean m_is_new_win_avail_default = true;
  public static boolean m_is_close_act_avail_default = true;
  public static boolean m_is_exit_act_avail_default = true;
  public static boolean m_is_sel_present_avail_default = true;
  public static boolean m_is_sel_filter_avail_default = true;
  public static boolean m_is_show_hide_obj_avail_default = true;
  public static boolean m_is_tbar_color_avail_default = true;
  public static boolean m_is_find_avail_default = true;
  public static boolean m_is_find_up_avail_default = true;
  public static boolean m_is_find_down_avail_default = true;
  public static boolean m_is_edit_filter_avail_default = true;
  public static boolean m_is_collect_exp_avail_default = true;
  public static boolean m_exit_as_last_win_closed = true;
  public static boolean m_dispose_frame_as_win_closed = true;
  // these are values for the IPC session
  public boolean m_is_open_exp_avail_session = m_is_open_exp_avail_default;
  public boolean m_is_aggregate_exp_avail_session = m_is_aggregate_exp_avail_default;
  public boolean m_is_compare_exp_avail_session = m_is_compare_exp_avail_default;
  public boolean m_is_print_dsp_avail_session = m_is_print_dsp_avail_default;
  public boolean m_is_new_win_avail_session = m_is_new_win_avail_default;
  public boolean m_is_close_act_avail_session = m_is_close_act_avail_default;
  public boolean m_is_exit_act_avail_session = m_is_exit_act_avail_default;
  public boolean m_is_sel_present_avail_session = m_is_sel_present_avail_default;
  public boolean m_is_sel_filter_avail_session = m_is_sel_filter_avail_default;
  public boolean m_is_show_hide_obj_avail_session = m_is_show_hide_obj_avail_default;
  public boolean m_is_tbar_color_avail_session = m_is_tbar_color_avail_default;
  public boolean m_is_find_avail_session = m_is_find_avail_default;
  public boolean m_is_find_up_avail_session = m_is_find_up_avail_default;
  public boolean m_is_find_down_avail_session = m_is_find_down_avail_default;
  public boolean m_is_edit_filter_avail_session = m_is_edit_filter_avail_default;
  public boolean m_is_collect_exp_avail_session = m_is_collect_exp_avail_default;
  private static Analyzer instance;
  private static String[] arguments = null;
  private AnFrame anFrame = null;
  private ConnectionManager connectionManager = null;
  private String emptyString = "";
  // Known connection problems
  private String known_problem_0 = AnLocale.getString("Empty host name is not valid.");
  private String known_problem_1 =
      AnLocale.getString("Authentication failed. User name or password did not match");
  private String known_problem_2 =
      AnLocale.getString("Authentication failed. Password is required.");
  private String known_problem_3 = AnLocale.getString("Connection failed. Timeout.");
  private String known_problem_4 =
      AnLocale.getString("Authentication failed. Password was not requested.");
  private String known_problem_5 = AnLocale.getString("Connection failed. Host is unreachable.");
  private String known_problem_6 =
      AnLocale.getString("Connection failed. Installation path is not correct.");
  private String known_problem_7 =
      AnLocale.getString("Connection failed. Unrecognized reply from remote host.");
  private String known_problem_8 = AnLocale.getString("Connection failed. Canceled by user.");
  private String known_problem_9 = AnLocale.getString("Connection failed. Incompatible versions.");
  public String known_problem_10 = AnLocale.getString("Stream Closed");
  // Explanations for known connection problems
  private String explanation_0 =
      AnLocale.getString("Empty host name is not valid. Please specified remote host.");
  private String explanation_3_1 =
      AnLocale.getString(
          "gprofng GUI did not get a reply from the remote host. Please check that the"
              + " specified remote host exists, and the specified remote gprofng path exists on the"
              + " remote host.");
  private String explanation_3_2 =
      AnLocale.getString(
          "gprofng GUI did not get a reply from the remote host. Please check that the"
              + " specified remote gprofng path exists on the remote host.");
  private String explanation_9 =
      AnLocale.getString(
          "gprofng GUI version does not match version on the remote host as specified by"
              + " the remote gprofng path.");
  // Connection protocol version
  private static String IPC_PROTOCOL = IPCProtocol.IPC_PROTOCOL_STR;
  private final String ipc_protocol = IPC_PROTOCOL;

  public static final String DisplayAppName = "gp-display-text";
  private static final String CollectAppName = "gp-collect-app";
  private static final String KernelAppName = "gp-collect-kernel";

  public Analyzer() {
    instance = this;
    String s = AnUtility.getenv("GPROFNG_DEBUG");
    if (s != null) {
      try {
        AnUtility.debugFlags = Integer.parseInt(s);
      } catch (NumberFormatException e) {
      }
    }
    IPC_session = new IPC(this);
  }

  public static Analyzer getInstance() {
    return instance;
  }

  public void initMetricColors() {
    UserPref userPref = UserPref.getInstance();
    if (userPref.getCustomMetricColors() != null) {
      for (MetricColor metricColor : userPref.getCustomMetricColors()) {
        MetricColors.setCustomMetricColor(metricColor.getMetricName(), metricColor.getColor());
      }
    }
  }

  public static boolean initPath(final boolean in_netbeans) {
    // Analyzer.in_netbeans = in_netbeans;

    // Done initialize
    if (cls_loader != null) {
      return in_netbeans; // Analyzer.in_netbeans;
    }
    // Find installed directory fdhome & nbhome
    cls_loader = Analyzer.class.getClassLoader();
    fdhome = AnUtility.findResourceHome(cls_loader, "org/gprofng/mpmt/Analyzer.class");

    // Method AnUtility.findResourceHome()returns wrong path on Windows
    // System.err.println("AnUtility.findResourceHome returned fdhome = " + fdhome);
    if (fdhome.charAt(2) == ':') {
      if (fdhome.charAt(0) == '/') {
        String os_name = System.getProperty("os.name");
        if (os_name.contains("Windows")) {
          fdhome = fdhome.substring(1);
          // System.err.println("Corrected for Windows fdhome = " + fdhome);
        }
      }
    }

    licpath = System.getProperty("analyzer.licpath", fdhome + "/lib/serial.dat");

    return in_netbeans; // Analyzer.in_netbeans;
  }

  /**
   * @return the workingDirectory
   */
  public String getWorkingDirectory() {
    //        System.out.println("getWorkingDirectory: " + workingDirectory);
    return workingDirectory;
  }

  /**
   * @param workingDirectory the workingDirectory to set
   */
  public void setWorkingDirectory(String workingDirectory) {
    //        System.out.println("setWorkingDirectory: " + workingDirectory);
    this.workingDirectory = workingDirectory;
    setCurrentDirectory(workingDirectory); // IPC call
    AnWindow.getInstance().updateWorkingDirectoryStatus();
  }

  /**
   * @param workingDirectory the workingDirectory to set
   */
  public void initWorkingDirectory() {
    if (IPC_session == null) {
      workingDirectory = System.getProperty("user.dir", ".");
    } else {
      String wd = getCurrentDirectory(); // IPC call!
      if (wd == null) {
        wd = System.getProperty("user.dir", ".");
      }
      workingDirectory = wd;
    }
  }

  /**
   * @return returns name of remote host in case of a remote connection otherwise it returns
   *     "localhost"
   */
  public String getHost() {
    if (remoteHost != null) {
      return remoteHost;
    }
    return "localhost";
  }

  /**
   * @return returns name of local host
   */
  public String getLocalHost() {
    return localHost;
  }

  /**
   * @return returns name of remote host in case of a remote connection otherwise it returns null
   */
  public String getRemoteHost() {
    String remoteHostName = null;
    if (isRemote()) {
      remoteHostName = remoteHost;
    }
    return remoteHostName;
  }

  /**
   * @return returns whether it is connected remotely
   */
  public boolean isRemote() {
    return remoteConnection != null;
  }

  /**
   * @return returns whether it is connected locally or remotely
   */
  public boolean isConnected() {
    return (IPC_started);
  }

  /**
   * @return returns whether kernel profiling is enabled
   */
  public boolean isKernelProfilingEnabled() {
    return kernelProfilingEnabled;
  }

  /** Start Connection Manager */
  public void startConnectionManager() {
    if (null == connectionManager) {
      connectionManager = new ConnectionManager();
      if (connectionManager != null) {
        connectionManager.start();
      }
    }
    if (connectionManager != null) {
      connectionManager.startChecking();
    }
  }

  /** Stop Connection Manager */
  public void stopConnectionManager() {
    if (connectionManager != null) {
      connectionManager.stopChecking();
    }
  }

  private String get_exe_name(String nm) {
    if (nm != null && !nm.equals(emptyString) && Files.exists(Paths.get(nm))) {
      return nm;
    }
    return null;
  }
  
  public void startIPC() throws Exception {
    if (IPC_started) {
      return;
    }
    fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CHANGING);
    // $SP_ANALYZER_CONFIG_MODE may be set to "R" to tell the spawned
    //	 gp-display-text to configure for race detection
    //	 It will spawn gp-display-text -RIPC, rather than -IPC in that case
    //
    // $SP_COLLECTOR_IPC_DEBUG may be set to a string to use to prepend
    //	to the gp-display-text -[R]IPC command -- examples:
    //    "LD_PRELOAD mem.so; "
    //	  "collect -Z -O /dev/null" -- to do the same thing
    //	  "collect -p on -H on -O /dev/null" to collect an experiments
    //		The -O command is necessary, since any IO from
    //		collect to stdout before the gp-display-text target is
    //		spawned will confuse the GUI and gp-display-text
    //		communication channel.
    //    "dbx ..." can not be prepended, because there's no way to convince
    //		dbx to not write anything to stdout, and anything written
    //		will confuse the gp-display-text process

    String str = null; // AnUtility.getenv("SP_COLLECTOR_IPC_DEBUG");
    if (str != null && !str.equals(emptyString)) {
      IPCLogger.logTrace("\n" + "analyzer: SP_COLLECTOR_IPC_DEBUG `" + str + "'");
    } else {
      str = emptyString;
    }

    String cmode = AnUtility.getenv("SP_ANALYZER_CONFIG_MODE");
    if (cmode == null) {
      cmode = emptyString;
    } else if (cmode.equals(emptyString)) {
      // OK, but why was this set?
    } else if (!cmode.equals("R")) {
      System.err.println("analyzer: SP_ANALYZER_CONFIG_MODE `" + cmode + "' unrecognized");
      System.exit(1);
    }

    //  SP_ANALYZER_ER_PRINT may be set to use the other gp-display-text.
    //  Also SP_ANALYZER_ER_PRINT can be used instead SP_COLLECTOR_IPC_DEBUG
    //  For example::
    //    "ssh host /bin/gp-display-text" - to use
    //          the other gp-display-text on the other machine.
    //    "LD_PRELOAD mem.so /opt/SUNWSpro/bin/gp-display-text"
    //	  "collect -H on -O /dev/null /bin/gp-display-text" -
    //          to collect an experiments.
    //		The -O command is necessary, since any IO from
    //		collect to stdout before the gp-display-text target is
    //		spawned will confuse the GUI and gp-display-text
    //		communication channel.
    if (er_print == null || er_print.equals(emptyString)) {
      er_print = AnUtility.getenv("SP_ANALYZER_ER_PRINT");
    }
    if (er_print == null || er_print.equals(emptyString)) {
      er_print = getPathToApp(DisplayAppName);
    } else {
      IPCLogger.logTrace("\n" + "analyzer: SP_ANALYZER_ER_PRINT='" + er_print + "'");
      if (remoteConnection != null) {
        if (str.equals(emptyString)) {
          str = remoteConnection;
        } else {
          str = remoteConnection + " " + str;
        }
      }
    }
    AnLog.log("analyzer: gp-display-text=" + er_print + "\n");
    // Check the OS - only Solaris and Linux are supported
    if (remoteHost == null) {
      if (null == os_name) {
        throw new Exception("OS is not supported yet.");
      }
      if ((!os_name.equals("SunOS")) && (!os_name.equals("Linux"))) {
        throw new Exception("OS " + os_name + " is not supported yet.");
      }
    }
    String er_printCmd;
    if (str.equals(emptyString)) {
      er_printCmd = er_print + " -" + cmode + "IPC";
    } else {
      er_printCmd = str + " " + er_print + " -" + cmode + "IPC";
    }
    // IPC_session.init(er_printCmd + " -" + cmode + "IPC");
    String tracelevel = null; // AnUtility.getenv("SP_ER_PRINT_TRACE_LEVEL");
    if (tracelevel != null) {
      String er_printCmd_extn = " -E SP_ER_PRINT_TRACE_LEVEL=" + tracelevel;
      er_printCmd = er_printCmd + er_printCmd_extn;
    }
    stopConnectionManager();
    IPC_session.init(er_printCmd, true);
    setIPCStarted(true);
    startConnectionManager();
    fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CHANGED);
  }

  /**
   * Creates new IPC connection Connection_thread
   *
   * @param cc
   * @param host
   * @param name
   * @param p
   * @param connectCommand
   * @param path
   */
  public String createNewIPC(
      ConnectionDialog cc,
      String host,
      String name,
      char[] p,
      String connectCommand,
      String path) {
    kernelProfilingEnabled = false;
    AnLog.log("host: " + host);
    AnLog.log("name: " + name);
    AnLog.log("p: " + (p == null ? "NULL" : String.valueOf(p)));
    AnLog.log("connectCommand: " + connectCommand);
    AnLog.log("path: " + path);

    fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CHANGING);
    stopConnectionManager();
    // Destroy old IPC session
    if (null != old_IPC_session) {
      IPC new_IPC_session = IPC_session;
      IPC_session = old_IPC_session;
      setIPCStarted(old_IPC_status);
      old_IPC_session.destroyIPCProc();
      IPC_session = new_IPC_session;
    }
    // Save current IPC session
    old_IPC_session = IPC_session;
    old_IPC_status = IPC_started;
    IPC newIPC = new IPC(this);
    String path_to_er_print = DisplayAppName;
    String l_fdhome = emptyString;
    if (null != path) {
      l_fdhome = path.trim();
      if (l_fdhome.length() > 0) {
        if (!l_fdhome.equals(path_to_er_print)) {
          if (l_fdhome.endsWith("/" + DisplayAppName)) {
            path_to_er_print = l_fdhome;
          } else if (l_fdhome.endsWith("/bin/")) {
            path_to_er_print = l_fdhome + DisplayAppName;
          } else if (l_fdhome.endsWith("/bin")) {
            path_to_er_print = l_fdhome + "/" + DisplayAppName;
          } else if (l_fdhome.endsWith("/")) {
            path_to_er_print = l_fdhome + "bin/" + DisplayAppName;
          } else {
            path_to_er_print = l_fdhome + "/bin/" + DisplayAppName;
          }
        }
      }
      AnLog.log("path: " + path);
    }
    AnLog.log("l_fdhome: " + l_fdhome);
    AnLog.log("str_gp_display_text: " + DisplayAppName);
    int i = path_to_er_print.lastIndexOf(DisplayAppName);
    String path_to_collect = path_to_er_print.substring(0, i) + CollectAppName;
    String er_printCmd = path_to_er_print;
    String rc = null;
    String emsg = null;
    // Authentication
    String rh = host;
    if (rh != null) {
      rh = rh.trim(); // remove spaces
    }
    // Empty host name is not accepted
    if (rh == null || rh.length() == 0) {
      return AnLocale.getString("Empty host name is not valid.");
    }
    if (rh.length() > 0 && !rh.equals("localhost")) {
      // Add user name if it is not empty
      if (name != null) {
        name = name.trim(); // remove spaces
        if (name.length() > 0) {
          rh = name + "@" + rh;
        }
      }
      String rs = AnUtility.getenv("SP_ANALYZER_REMOTE_SHELL");
      if (rs != null) { // Special way to login
        remoteConnectCommand = rs;
      } else if (null != connectCommand) {
        remoteConnectCommand = connectCommand;
      } else {
        remoteConnectCommand = remoteShell;
      }
      remoteConnectCommand += " " + rh;
      er_printCmd = remoteConnectCommand + " " + er_printCmd;
    }
    rc = er_printCmd;
    er_printCmd = er_printCmd + " -IPC";
    String tracelevel = null; // AnUtility.getenv("SP_ER_PRINT_TRACE_LEVEL");
    if (tracelevel != null) {
      er_printCmd = er_printCmd + " -E SP_ER_PRINT_TRACE_LEVEL=" + tracelevel;
    }
    if (ipc_protocol != null) {
      er_printCmd = er_printCmd + " -E SP_IPC_PROTOCOL=" + ipc_protocol;
    }
    // Initialize new IPC connection - start gp-display-text
    try {
      AnLog.log("Start connection:\n" + er_printCmd);
      newIPC.init(er_printCmd, false);
      sendP(newIPC, p, cc);
      er_print = path_to_er_print;
    } catch (Exception e) {
      newIPC.destroyIPCProc();
      fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
      return (e.getMessage());
    }
    newIPC.getIPCReader().runThread(); // TEMPORARY FOR DEBUG

    if (IPC_started) { // see newinit()
      // AnFrame if fully initialized
      try {
        IPC_session = newIPC;
        // Version Handshake
        int res = versionHandshake();
        if (res != 0) {
          fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
          return AnLocale.getString("Connection failed: versions do not match");
        }
        IPC_initView(0, -1);
        if (cc.cancelRequest) {
          emsg = AnLocale.getString("Connection canceled");
        }
      } catch (Exception e) {
        emsg = AnLocale.getString("Cannot establish connection with remote host ") + rh;
      }
      if (emsg != null) {
        // Could not establish new connection
        IPC_session = old_IPC_session;
        setIPCStarted(old_IPC_status);
        newIPC.destroyIPCProc();
        fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
        return (emsg);
      }
      // Successfully connected!
      remoteHost = rh;
      remoteConnection = rc;

      // Clean everything that belongs to the old IPC session
      IPC_session = old_IPC_session;
      setIPCStarted(old_IPC_status);

      // Use new IPC session
      IPC_session = newIPC;

      // Version Handshake
      int res = versionHandshake();
      if (res != 0) {
        fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
        return AnLocale.getString("Connection failed: versions do not match");
      }

      syncTime();

      // Call initApplication
      String l_licpath = System.getProperty("analyzer.licpath", l_fdhome + "/lib/serial.dat");
      String[] args = new String[1];
      args[0] = er_print;
      String[] license_info = new String[2];
      license_info[0] = "ERROR";
      license_info[1] = "Unknown";
      try {
        license_info = initApplication(false, l_fdhome, l_licpath, args);
      } catch (Exception e) {
        if (null == license_info[0]) {
          license_info[0] = "ERROR";
        }
      }
      String l_licsts = license_info[0];
      String l_fdversion = license_info[1];

      if (l_licsts != null && l_licsts.equals("ERROR")) { // Problem getting license
        emsg =
            AnLocale.getString("License Path: ")
                + l_licpath
                + "\n"
                + AnLocale.getString("Error: ")
                + l_fdversion;
        // Restore old IPC_session
        IPC_session = old_IPC_session;
        setIPCStarted(old_IPC_status);
        newIPC.destroyIPCProc();
        fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
        return (emsg);
      }

    } else { // need AnFrame initialization
      if (old_IPC_status) { // can it be true?
        // Clean everything that belongs to the old IPC session
        IPC_session = old_IPC_session;
        setIPCStarted(old_IPC_status);
      }
      // Use new IPC session
      IPC_session = newIPC;
      IPC_started = true;

      // Version Handshake
      int res = versionHandshake();
      if (res != 0) {
        return AnLocale.getString("Connection failed: versions do not match");
      }

      // Call initApplication
      String l_licpath = System.getProperty("analyzer.licpath", l_fdhome + "/lib/serial.dat");
      String[] args = new String[1];
      args[0] = er_print;
      String[] license_info = new String[2];
      license_info[0] = "ERROR";
      license_info[1] = "Unknown";
      try {
        license_info = initApplication(false, l_fdhome, l_licpath, args);
      } catch (Exception e) {
        if (null == license_info[0]) {
          license_info[0] = "ERROR";
        }
      }
      String l_licsts = license_info[0];
      String l_fdversion = license_info[1];

      if (l_licsts != null && l_licsts.equals("ERROR")) {
        emsg =
            AnLocale.getString("License Path: ")
                + l_licpath
                + "\n"
                + AnLocale.getString("Error: ")
                + l_fdversion;
        // Restore old IPC_session
        IPC_session = old_IPC_session;
        setIPCStarted(old_IPC_status);
        newIPC.destroyIPCProc();
        fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
        return (emsg);
      }
      try {
        // Successfully connected!
        remoteHost = rh;
        remoteConnection = rc;
        // Use new IPC session
        IPC_session = newIPC;
        setIPCStarted(true);
        try {
          initAnalyzerIPC();
        } catch (Exception e) {
          fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
          return AnLocale.getString("Initialization failed");
        }
        if (cc.cancelRequest) {
          emsg = AnLocale.getString("Connection canceled");
        }
      } catch (Exception e) {
        emsg = AnLocale.getString("Cannot establish connection with remote host ") + rh;
      }
      if (emsg != null) {
        // Could not establish new connection
        IPC_session = old_IPC_session;
        setIPCStarted(old_IPC_status);
        newIPC.destroyIPCProc();
        fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
        return (emsg);
      }
      // Successfully connected!
      remoteHost = rh;
      remoteConnection = rc;
    }

    // Use new IPC session
    IPC_session = newIPC;
    setIPCStarted(true);
    startConnectionManager();
    fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CHANGED);

    // Measure connection performance (DEBUG)
    measureConnectionPerformance(cc);

    //        // Check if "er_kernel" is enabled
    //        checkKernelProfilingEnabled();
    // Update connection information
    updateConnectionInfo(host, name, p, path_to_collect);

    // Synchronize Locale with remote engine
    if (isRemote()) {
      synchronizeLocale();
    }

    logInit();

    // Create Collector GUI dialog using a background thread (optimization)
    Thread createCollectorDialog =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                AnWindow.getInstance().resetProfileDialogs();
                //                AnWindow.getInstance().getRemoteCollectDialog();
              }
            });
    createCollectorDialog.run();
    return null;
  }

  /**
   * Explain Connection Problem
   *
   * @param msg
   * @return explanation
   */
  public String explainConnectionProblem(String msg, String un) {
    String explanation = msg;
    if (msg == null) {
      return explanation;
    }
    if (msg.equals(known_problem_0)) {
      explanation = explanation_0;
    }
    if (msg.equals(known_problem_3) || msg.equals(known_problem_10)) {
      if ((un == null) || (un.length() < 1)) {
        explanation = explanation_3_1;
      } else {
        explanation = explanation_3_2;
      }
    }
    if (msg.equals(known_problem_9)) {
      explanation = explanation_9;
    }
    return explanation;
  }

  private static int connectionID = 0;

  /** Send pass */
  private void sendP(IPC ipc, char[] pass, ConnectionDialog cc) throws Exception {
    // This is a temporary code, just to prove the concept
    connectionID++;
    long ts = System.currentTimeMillis();
    boolean skip_pass = false;
    boolean auth_failed = true;
    String pattern1 = "Password";
    String pattern2 = " " + pattern1 + ":";
    String pattern3 = pattern1 + " for ";
    String pattern4 = "ER_IPC: IPC_PROTOCOL_";
    String pattern5 = "Fatal error: host not found: ";
    String pattern6 = ": not found";
    String pattern7 = "Killed";
    String pattern8 = "ER_IPC: " + ipc_protocol + "\n";
    String pattern9 = "ER_IPC: IPC_PROTOCOL_UNKNOWN\n";
    String pattern10 = ": No such file";
    String status1 = AnLocale.getString("Looking for remote host...");
    String status2 = AnLocale.getString("Verifying authentication...");
    String status3 = AnLocale.getString("Connection established. Compatibility checking...");
    String status4 = AnLocale.getString("Password is not requested (!?)");
    String ask = emptyString;
    AnLog.log(
        "analyzer: Connect to Remote Host(Connection ID="
            + connectionID
            + ") started. Timestamp="
            + ts); // DEBUG
    cc.updateConnectionStatus(status1);
    if ((null != pass) && (pass.length > 0)) {
      int c = 190; // timeout 19 seconds
      // Don't read - another thread (IPCReader) is already reading it.
      while (c-- > 0) {
        Thread.sleep(100);
        ask = ipc.getIPCReader().getUnknownInput();
    AnLog.log(String.format("connectionID=%d Timestamp=%ld ask=%s;\n", ts));
        if (ask.contains(pattern2)) {
          break;
        }
        if (ask.contains(pattern3)) {
          break;
        }
        if (ask.contains(pattern8)) {
          // Connection established
          skip_pass = true;
          cc.updateConnectionStatus(status3);
          auth_failed = false;
          break;
        }
        if (ask.contains(pattern4) || ask.contains(pattern9)) {
          auth_failed = true; // Incompatible IPC protocol
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_9
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_9);
          throw new Exception(known_problem_9);
        }
        if (ask.contains(pattern5)) {
          auth_failed = true; // Unreachable host
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_5
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_5);
          throw new Exception(known_problem_5);
        }
        if (ask.contains(pattern6) || ask.contains(pattern10)) {
          auth_failed = true; // Installation path is not correct
          cc.updateConnectionStatus(known_problem_6);
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          throw new Exception(known_problem_6);
        }
        if (ask.contains(pattern7)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
      }
      if (!skip_pass) { // got request for the password
        ts = System.currentTimeMillis();
        AnLog.log(
            "analyzer: Connect to Remote Host(Connection ID="
                + connectionID
                + ") Verifying authentication... Timestamp="
                + ts); // DEBUG
        cc.updateConnectionStatus(status2);
        for (int i = 0; i < pass.length; i++) {
          ipc.processInput.write(pass[i]);
        }
        ipc.processInput.write('\n');
        // ipc.processInput.write('\0');
        ipc.processInput.flush();
      } else {
        if (auth_failed) {
          // Probably the delay is not enough to get the request for the password
          cc.updateConnectionStatus(status4);
          System.err.println("Error 1: unrecognized request: " + ask);
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") Error 1: unrecognized request: "
                  + ask
                  + " Timestamp="
                  + ts); // DEBUG
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_4
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_4);
          throw new Exception(known_problem_4);
        }
      }
      c = 190; // timeout 19 seconds
      while (c-- > 0) {
        Thread.sleep(100);
        ask = ipc.getIPCReader().getUnknownInput();
        // Check if request for password appeared twice
        if (ask.contains(pattern1)) {
          if (skip_pass) {
            System.err.println("Error 2: unrecognized request: " + ask);
            auth_failed = true; // Delay is not enough to get request for password
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") Error 2: unrecognized request: "
                    + ask
                    + " Timestamp="
                    + ts); // DEBUG
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_1
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_1);
            throw new Exception(known_problem_1);
          }
          int p = ask.indexOf(pattern1);
          String s = ask.substring(p + 1);
          if (s.contains(pattern1)) {
            auth_failed = true; // Password does not match
            // System.err.println("Error 3: unrecognized request: " + ask);  DEBUG
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") Error 3: unrecognized request: "
                    + ask
                    + " Timestamp="
                    + ts); // DEBUG
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_1
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_1);
            throw new Exception(known_problem_1);
          }
        }
        if (ask.contains(pattern8)) {
          // Connection established
          cc.updateConnectionStatus(status3);
          auth_failed = false;
          break;
        }
        if (ask.contains(pattern4) || ask.contains(pattern9)) {
          auth_failed = true; // Incompatible IPC protocol
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_9
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_9);
          throw new Exception(known_problem_9);
        }
        if (ask.contains(pattern5)) {
          auth_failed = true; // Unreachable host
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_5
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_5);
          throw new Exception(known_problem_5);
        }
        if (ask.contains(pattern6) || ask.contains(pattern10)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
        if (ask.contains(pattern7)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
        if (ask.length() > 100000) { // Why so many? Something wrong.
          auth_failed = true;
          System.err.println("Error 4: unrecognized request: " + ask);
          ts = System.currentTimeMillis();
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") Error 4: unrecognized request: "
                  + ask
                  + " Timestamp="
                  + ts); // DEBUG
          AnLog.log(
              "analyzer: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_7
                  + " Timestamp="
                  + ts); // DEBUG
          cc.updateConnectionStatus(known_problem_7);
          throw new Exception(known_problem_7);
        }
      }
      // Check if connection failed
      if (ipc_protocol == null) {
        auth_failed = false; // we don't know for sure
      }
      if (auth_failed) {
        System.err.println("Error 5: unrecognized request: " + ask);
        ts = System.currentTimeMillis();
        AnLog.log(
            "analyzer: Connect to Remote Host(Connection ID="
                + connectionID
                + ") Error 5: unrecognized request: "
                + ask
                + " Timestamp="
                + ts); // DEBUG
        AnLog.log(
            "analyzer: Connect to Remote Host(Connection ID="
                + connectionID
                + ") failed: "
                + known_problem_3
                + " Timestamp="
                + ts); // DEBUG
        cc.updateConnectionStatus(known_problem_3);
        throw new Exception(known_problem_3);
      }
      // Looks like everything is ok. Try to continue
    } else { // connection without password
      if (ipc_protocol != null) {
        ts = System.currentTimeMillis();
        AnLog.log(
            "analyzer: Connect to Remote Host(Connection ID="
                + connectionID
                + ") Connecting ... Timestamp="
                + ts); // DEBUG
        // this hangs if er_print do not send confirmation
        int c = 190; // timeout 19 seconds
        // Don't read - another thread (IPCReader) is already reading it.
        ask = emptyString;
        while (c-- > 0) {
          Thread.sleep(100);
          ask = ipc.getIPCReader().getUnknownInput();
          if (ask.contains(pattern8)) {
            // Connection established
            cc.updateConnectionStatus(status3);
            auth_failed = false;
            break;
          }
          if (ask.contains(pattern1)) {
            auth_failed = true;
            // System.err.println("Error 6: unrecognized request: " + ask);  DEBUG
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") Error 6: unrecognized request: "
                    + ask
                    + " Timestamp="
                    + ts); // DEBUG
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_2
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_2);
            throw new Exception(known_problem_2);
          }
          if (ask.contains(pattern4) || ask.contains(pattern9)) {
            auth_failed = true; // Incompatible IPC protocol
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_9
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_9);
            throw new Exception(known_problem_9);
          }
          if (ask.contains(pattern5)) {
            auth_failed = true; // Unreachable host
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_5
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_5);
            throw new Exception(known_problem_5);
          }
          if (ask.contains(pattern6) || ask.contains(pattern10)) {
            auth_failed = true; // Installation path is not correct
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_6
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_6);
            throw new Exception(known_problem_6);
          }
          if (ask.contains(pattern7)) {
            auth_failed = true; // Installation path is not correct
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_6
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_6);
            throw new Exception(known_problem_6);
          }
          if (ask.length() > 100000) { // Why so many? Something wrong.
            auth_failed = true;
            System.err.println("Error 7: unrecognized request: " + ask);
            ts = System.currentTimeMillis();
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") Error 7: unrecognized request: "
                    + ask
                    + " Timestamp="
                    + ts); // DEBUG
            AnLog.log(
                "analyzer: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_3
                    + " Timestamp="
                    + ts); // DEBUG
            cc.updateConnectionStatus(known_problem_3);
            throw new Exception(known_problem_3);
          }
        }
      }
    }
    if (auth_failed) {
      System.err.println("Error 8: unrecognized request: " + ask);
      ts = System.currentTimeMillis();
      AnLog.log(
          "analyzer: Connect to Remote Host(Connection ID="
              + connectionID
              + ") Error 8: unrecognized request: "
              + ask
              + " Timestamp="
              + ts); // DEBUG
      AnLog.log(
          "analyzer: Connect to Remote Host(Connection ID="
              + connectionID
              + ") failed: "
              + known_problem_3
              + " Timestamp="
              + ts); // DEBUG
      cc.updateConnectionStatus(known_problem_3);
      throw new Exception(known_problem_3);
    }
    // Sleep a little to make sure IPCReader got all bytes from the connection step
    Thread.sleep(100);
    ts = System.currentTimeMillis();
    AnLog.log(
        "analyzer: Connect to Remote Host(Connection ID="
            + connectionID
            + ") succeeded. Timestamp="
            + ts); // DEBUG
  }

  private void measureConnectionPerformance(ConnectionDialog cc) {
    String runTest = null; // AnUtility.getenv("SP_ANALYZER_SPEED_TEST");
    runTest = AnUtility.getenv("SP_ANALYZER_SPEED_TEST"); // DEBUG XXX SWITCH OFF WHEN RELEASE
    if (runTest == null || runTest.equals("0")) {
      return;
    }
    // Measure connection performance - loop of SYNC calls
    if (cc != null) {
      cc.updateConnectionStatus(
          AnLocale.getString("Connection established. Checking connection performance..."));
    }
    long totalBytes = IPC_session.getIPCReader().getTotalReceivedBytes();
    long totalMsg = IPC_session.getIPCReader().getTotalReceivedMessages();
    long t1 = System.currentTimeMillis();
    int max = 100;
    Integer k = new Integer(runTest);
    if (k > 0) {
      max = max * k.intValue();
    }
    for (int i = 0; i < max; i++) {
      versionHandshake(); // syncTime();
    }
    long t2 = System.currentTimeMillis();
    totalBytes = IPC_session.getIPCReader().getTotalReceivedBytes() - totalBytes;
    totalMsg = IPC_session.getIPCReader().getTotalReceivedMessages() - totalMsg;
    // System.err.println("Connection performance (SYNC): Total bytes 2=" + totalBytes + " bytes.
    // Total messages 2=" + totalMsg + " msg.");  // DEBUG
    long delta = t2 - t1;
    if (delta <= 0) {
      delta = 1;
    }
    double upload = (((double) totalBytes * 1000) / delta);
    double temp = (((double) totalMsg * 1000) / delta);
    System.err.println("Connection performance (SYNC): Loop count=" + max); // DEBUG
    System.err.printf(
        "Connection performance (SYNC): Upload=%.2f bytes/sec. Temp=%.2f messages/sec.\n",
        upload, temp); // DEBUG
    AnLog.log(
        "Connection performance (SYNC): Upload="
            + upload
            + " bytes/sec. Temp="
            + temp
            + " messages/sec."); // DEBUG
    System.err.println(
        "Connection performance (SYNC): Received bytes="
            + totalBytes
            + " bytes. Received messages="
            + totalMsg
            + " msg. Time="
            + delta
            + " ms."); // DEBUG
    AnLog.log(
        "Connection performance (SYNC): Received bytes="
            + totalBytes
            + " bytes. Received messages="
            + totalMsg
            + " msg. Time="
            + delta
            + " ms."); // DEBUG
    if (cc != null) {
      cc.updateConnectionStatus(
          "Connection performance (SYNC): Upload " + upload + " bytes/msec. Handshaking...");
    }
    // Measure connection performance - loop of ASYNC calls
    totalBytes = IPC_session.getIPCReader().getTotalReceivedBytes();
    totalMsg = IPC_session.getIPCReader().getTotalReceivedMessages();
    // System.err.println("Connection performance (ASYNC): Total bytes 3=" + totalBytes + " bytes.
    // Total messages 3=" + totalMsg + " msg.");  // DEBUG
    t1 = System.currentTimeMillis();
    IPCResult ipcResults[] = new IPCResult[max];
    for (int i = 0; i < max; i++) {
      IPCHandle ipcHandle = IPCHandle.newHandshakeIPCHandle();
      ipcResults[i] = ipcHandle.sendRequest(); // no body
    }
    for (int i = 0; i < max; i++) {
      IPCResult.CC c = ipcResults[i].getCC(); // blocking
    }
    t2 = System.currentTimeMillis();
    totalBytes = IPC_session.getIPCReader().getTotalReceivedBytes() - totalBytes;
    totalMsg = IPC_session.getIPCReader().getTotalReceivedMessages() - totalMsg;
    // System.err.println("Connection performance (ASYNC): Total bytes 4=" + totalBytes + " bytes.
    // Total messages 4=" + totalMsg + " msg.");  // DEBUG
    delta = t2 - t1;
    if (delta <= 0) {
      delta = 1;
    }
    upload = ((totalBytes * 1000) / delta);
    temp = ((totalMsg * 1000) / delta);
    System.err.println("Connection performance (ASYNC): Loop count=" + max); // DEBUG
    System.err.printf(
        "Connection performance (ASYNC): Upload=%.2f bytes/sec. Temp=%.2f messages/sec.\n",
        upload, temp); // DEBUG
    AnLog.log(
        "Connection performance (ASYNC): Upload="
            + upload
            + " bytes/sec. Temp="
            + temp
            + " messages/sec."); // DEBUG
    System.err.println(
        "Connection performance (ASYNC): Received bytes="
            + totalBytes
            + " bytes. Received messages="
            + totalMsg
            + " msg. Time="
            + delta
            + " ms."); // DEBUG
    AnLog.log(
        "Connection performance (ASYNC): Received bytes="
            + totalBytes
            + " bytes. Received messages="
            + totalMsg
            + " msg. Time="
            + delta
            + " ms."); // DEBUG
    if (cc != null) {
      cc.updateConnectionStatus(
          "Connection performance (ASYNC): Upload " + upload + " bytes/msec. Handshaking...");
    }
  }

  private String last_CC_host = null;
  private String last_CC_un = null;
  private char[] last_CC_p = null;
  private String last_CC_collect = null;

  /**
   * Update connection information to run collect
   *
   * @param host
   * @param name
   * @param p
   */
  private void updateConnectionInfo(String host, String name, char[] p, String path_to_collect) {
    last_CC_host = host;
    last_CC_un = name;
    last_CC_collect = path_to_collect;
    if (null != last_CC_p) {
      for (int i = 0; i < last_CC_p.length; i++) {
        last_CC_p[i] = 0;
      }
    }
    if (null != p) {
      last_CC_p = p.clone();
    } else {
      last_CC_p = null;
    }
  }

  /** Start Remote Collect */
  public void startRemoteCollect(Collector collector, ConnectionDialog cc) throws Exception {
    char[] p = last_CC_p;
    sendPCollect(collector, p, cc);
  }

  private String getPathToApp(String appName) {
    String path;
    if (UserPref.gprofngdir != null) {
      path = UserPref.gprofngdir + "/" + appName;
      if (Files.exists(Paths.get(path))) {
        return path;
      }
    }
    if (UserPref.binDirFromCommandLine != null) {
      path = UserPref.binDirFromCommandLine + "/" + appName;
      if (Files.exists(Paths.get(path))) {
        return path;
      }
    }
    if (fdhome != null) {
      path = fdhome + "/bin/" + appName;
      if (Files.exists(Paths.get(path))) {
        return path;
      }
    }
    return appName;
  }

  /** Get path to local or remote collect */
  public String getPathToCollect() {
    if (!isRemote()) {
      String collectPath = getPathToApp(CollectAppName);
      AnLog.log("analyzer: COLLECT=" + collectPath + "\n");
      return collectPath;
    }
    return last_CC_collect;
  }

  /**
   * Get path to er_kernel
   *
   * @return
   */
  public String getPathToCollectKernel() {
    return getPathToApp(KernelAppName);
  }

  /**
   * Connect to remote host
   *
   * @param rp
   * @param pass
   * @param cc
   * @throws Exception
   */
  private void sendPCollect(Collector collector, char[] pass, ConnectionDialog cc)
      throws Exception {
    // This is a temporary code, just to prove the concept
    connectionID++;
    long ts = System.currentTimeMillis();
    String remote_protocol = null;
    boolean skip_pass = false;
    boolean auth_failed = true;
    String pattern1 = "Password";
    String pattern2 = " " + pattern1 + ":";
    String pattern3 = pattern1 + " for ";
    String pattern4 = "Process ID:";
    String pattern5 = "Fatal error: host not found: ";
    String pattern6 = ": not found";
    String pattern7 = "Killed";
    String pattern10 = ": No such file";
    String ask = emptyString;
    AnLog.log(
        "collector: Connect to Remote Host(Connection ID="
            + connectionID
            + ") started. Timestamp="
            + ts); // DEBUG
    // cc.updateConnectionStatus("Looking for remote host...");
    if ((null != pass) && (pass.length > 0)) {
      // this hangs if remote process does not send anything, so we use a timeout
      int c = 190; // timeout 19 seconds
      while (c-- > 0) {
        Thread.sleep(100);
        ask = collector.getCurrentOutput();
        if (ask.contains(pattern2)) {
          break;
        }
        if (ask.contains(pattern3)) {
          break;
        }
        if (ask.contains(pattern4)) {
          // Connection established
          skip_pass = true;
          // cc.updateConnectionStatus("Connection established. Compatibility checking...");
          auth_failed = false;
          break;
        }
        if (ask.contains(pattern5)) {
          auth_failed = true; // Unreachable host
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_5
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_5);
          throw new Exception(known_problem_5);
        }
        if (ask.contains(pattern6) || ask.contains(pattern10)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
        if (ask.contains(pattern7)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
        // if (ask.length() > 100000) {
        //    break; // Why so many? Something wrong.
        // }
      }
      if (!skip_pass) { // got request for the password
        ts = System.currentTimeMillis();
        AnLog.log(
            "collector: Connect to Remote Host(Connection ID="
                + connectionID
                + ") Verifying authentication... Timestamp="
                + ts); // DEBUG
        // cc.updateConnectionStatus("Verifying authentication...");
        for (int i = 0; i < pass.length; i++) {
          collector.writeInputChar(pass[i]);
        }
        collector.writeInputChar('\n');
      } else {
        if (auth_failed) {
          // Probably the delay is not enough to get the request for the password
          // cc.updateConnectionStatus("Password is not requested (!?)");
          System.err.println("Error 11: unrecognized request: " + ask); // DEBUG
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") Error 11: unrecognized request: "
                  + ask
                  + " Timestamp="
                  + ts); // DEBUG
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_4
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_4);
          throw new Exception(known_problem_4);
        }
      }
      c = 190; // timeout 19 seconds
      while (c-- > 0) {
        Thread.sleep(100);
        ask = collector.getCurrentOutput(); // rp.getUnknownInput();
        // Check if request for password appeared twice
        if (ask.contains(pattern1)) {
          if (skip_pass) {
            System.err.println("Error 12: unrecognized request: " + ask); // DEBUG
            auth_failed = true; // Delay is not enough to get request for password
            ts = System.currentTimeMillis();
            AnLog.log(
                "collector: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") Error 12: unrecognized request: "
                    + ask
                    + " Timestamp="
                    + ts); // DEBUG
            AnLog.log(
                "collector: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_1
                    + " Timestamp="
                    + ts); // DEBUG
            // cc.updateConnectionStatus(known_problem_1);
            throw new Exception(known_problem_1);
          }
          int p = ask.indexOf(pattern1);
          String s = ask.substring(p + 1);
          if (s.contains(pattern1)) {
            auth_failed = true; // Password does not match
            System.err.println("Error 13: unrecognized request: " + ask); // DEBUG
            ts = System.currentTimeMillis();
            AnLog.log(
                "collector: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") Error 13: unrecognized request: "
                    + ask
                    + " Timestamp="
                    + ts); // DEBUG
            AnLog.log(
                "collector: Connect to Remote Host(Connection ID="
                    + connectionID
                    + ") failed: "
                    + known_problem_1
                    + " Timestamp="
                    + ts); // DEBUG
            // cc.updateConnectionStatus(known_problem_1);
            throw new Exception(known_problem_1);
          }
        }
        if (ask.contains(pattern4)) {
          // Connection established
          // cc.updateConnectionStatus("Connection established. Compatibility checking...");
          auth_failed = false;
          break;
        }
        if (ask.contains(pattern5)) {
          auth_failed = true; // Unreachable host
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_5
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_5);
          throw new Exception(known_problem_5);
        }
        if (ask.contains(pattern6) || ask.contains(pattern10)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
        if (ask.contains(pattern7)) {
          auth_failed = true; // Installation path is not correct
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_6
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_6);
          throw new Exception(known_problem_6);
        }
        if (ask.length() > 100000) { // Why so many? Something wrong.
          auth_failed = true;
          System.err.println("Error 14: unrecognized request: " + ask); // DEBUG
          ts = System.currentTimeMillis();
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") Error 14: unrecognized request: "
                  + ask
                  + " Timestamp="
                  + ts); // DEBUG
          AnLog.log(
              "collector: Connect to Remote Host(Connection ID="
                  + connectionID
                  + ") failed: "
                  + known_problem_7
                  + " Timestamp="
                  + ts); // DEBUG
          // cc.updateConnectionStatus(known_problem_7);
          throw new Exception(known_problem_7);
        }
      }
      // Check if connection failed
      if (remote_protocol == null) {
        auth_failed = false; // we don't know for sure
      }
      if (auth_failed) {
        System.err.println("Error 15: unrecognized request: " + ask); // DEBUG
        ts = System.currentTimeMillis();
        AnLog.log(
            "collector: Connect to Remote Host(Connection ID="
                + connectionID
                + ") Error 15: unrecognized request: "
                + ask
                + " Timestamp="
                + ts); // DEBUG
        AnLog.log(
            "collector: Connect to Remote Host(Connection ID="
                + connectionID
                + ") failed: "
                + known_problem_3
                + " Timestamp="
                + ts); // DEBUG
        // cc.updateConnectionStatus(known_problem_3);
        throw new Exception(known_problem_3);
      }
      // Looks like everything is ok. Try to continue
      // cc.updateConnectionStatus("Finished authentication. Handshaking...");
      // } else { // connection without password - nothing to do
      //        return;
    }
    ts = System.currentTimeMillis();
    AnLog.log(
        "collector: Connect to Remote Host(Connection ID="
            + connectionID
            + ") succeeded. Timestamp="
            + ts); // DEBUG
  }

  private void logInit() {
    AnLog.log(new Date().toString());
    AnLog.log(Analyzer.fdversion);
    AnLog.log(Analyzer.fdhome);
    //        AnLog.log(Analyzer.getInstance().getWorkingDirectory());
    AnLog.log(Analyzer.getInstance().er_print);
    AnLog.log(Analyzer.getInstance().remoteHost);
    AnLog.log(Analyzer.jvm_home);
    AnLog.log("-----------------------------------------------------");
  }

  private static void helpInit(final Window win) {}

  public static void showHelp(final String help_id) {
    //        System.out.println("Analyzer.showHelp: " + help_id);
    // Mez: Not supported yet because jh-2.0_05.jar cannot be used.
  }

  public static final class HelpAction implements ActionListener {

    final String id;

    public HelpAction(final String id) {
      this.id = id;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      //            System.out.println("Analyzer.HelpAction.actionPerformed: " + id);
      showHelp(id);
    }
  }

  /** Doesn't require running IPC. Called once. AWT thread */
  private void initAnalyzer() {
    String strNormalSelection = AnUtility.getenv("SP_ANALYZER_NORMAL_SELECTION");
    if (strNormalSelection != null && !strNormalSelection.equals("NO")) {
      normalSelection = true;
      AnLog.log("analyzer: SP_ANALYZER_NORMAL_SELECTION is set");
    }
    // if (remoteHost == null) {
    //     remoteHost = AnUtility.getenv("SP_ANALYZER_REMOTE_HOST");
    // }
    String specialRemoteShell = AnUtility.getenv("SP_ANALYZER_REMOTE_SHELL");
    if (specialRemoteShell != null && specialRemoteShell.length() > 0) {
      remoteShell = specialRemoteShell;
    }
    if (remoteShell == null) {
      if (fdhome.charAt(2) == ':') {
        if (fdhome.charAt(0) == '/') {
          String os_name = System.getProperty("os.name");
          if (os_name.contains("Windows")) {
            fdhome = fdhome.substring(1);
            AnLog.log("analyzer: Corrected path for Windows: fdhome = " + fdhome + "\n");
          }
        }
      }
    }
    if (remoteHost != null && !remoteHost.equals(emptyString)) {
      // System.err.println("analyzer: SP_ANALYZER_REMOTE_HOST `" + remoteHost + "'");
      AnLog.log("analyzer: SP_ANALYZER_REMOTE_HOST=" + remoteHost + "\n");
      remoteConnection = remoteShell + " " + remoteHost;
    } else {
      remoteHost = null;
      remoteConnection = null;
    }
    localHost = getLocalHostName();

    os_name = System.getProperty("os.name");
    logInit();
  }

  public String getLocalHostName() {
    String localHostName = null;
    try {
      java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
      localHostName = addr.getHostName();
    } catch (Exception e) {
    }
    return localHostName;
  }

  /** Synchronize Locale with remote engine */
  private void synchronizeLocale() {
    // Set client's locale on remote server
    // Only 3 locales are supported: ja, zh, C
    String lc = Locale.getDefault().getLanguage();
    String rlc = getLocale();
    boolean change = false;
    if (lc == null) {
      lc = "C";
    } else {
      if (lc.contains("ja")) {
        lc = "ja";
      } else {
        if (lc.contains("zh")) {
          lc = "zh";
        } else {
          lc = "C";
        }
      }
    }
    if ((rlc == null) || (rlc.length() < 1) || (!lc.equals(rlc))) { // Set locale
      change = true;
    }
    if (change) {
      rlc = setLocale(lc);
      // System.out.println(rlc);
    }
  }

  /**
   * AWT thread
   *
   * @return @throws Exception
   */
  private String initIPC() throws Exception {
    AnUtility.checkIPCOnWrongThread(false);
    String temporaryRemoteVersion = getAnalyzerReleaseName();
    fdversion = "";
    String emsg = null;
    startIPC();
    // Version Handshake
    if (IPC_started) {
      versionHandshake();
      // Measure connection performance (DEBUG)
      measureConnectionPerformance(null);
    }
    // Initialize gp-display-text
    final String[] license_info;
    if (IPC_started) {
      license_info = initApplication(false, fdhome, licpath, arguments);
    } else {
      license_info = new String[2];
      license_info[0] = "UNKNOWN";
      license_info[1] = temporaryRemoteVersion;
    }
    licsts = license_info[0];
    fdversion = license_info[1];

    if (licsts.equals("ERROR") || licsts.equals("FATAL")) {
      emsg = String.format(AnLocale.getString("License Path: %s\nError: %s"),
          licpath, fdversion);
      System.err.println(emsg);
      System.exit(1);
    } else if (licsts.equals("WARN")) {
      emsg = String.format(AnLocale.getString("License Path: %s\nWarning: %s"),
          licpath, fdversion);
      System.err.println(emsg);
    }
    if (IPC_started) {
      anFrame.getWindow().initializeAfterIPCStarted();
    }
    AnUtility.checkIPCOnWrongThread(true);
    return emsg;
  }

  /**
   * AWT Thread
   *
   * @throws Exception
   */
  private void initAnalyzerIPC() throws Exception {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            try {
              String errmsg = initIPC(); // Possible exception
              // We only get here if IPC is successfully running
              List<String> expList = AnUtility.getExpList(arguments);
              // Here we will try to guess what user wants: open an experiment or profile a target?
              boolean profile = true;
              if ((arguments != null) && (arguments.length > 0)) {
                // We will check if the first argument has suffix .er or .erg
                String target = arguments[0];
                if (target.length() > 3) {
                  if (target.endsWith(".er")) { // Experiment directory
                    profile = false;
                  }
                  if (target.endsWith("/")) { // Experiment directory
                    profile = false;
                  }
                  if (target.endsWith(".erg")) { // Experiment group
                    profile = false;
                  }
                }
              } else {
                profile = false; // No arguments
              }
              if (profile) { // Clear expList
                String[] noarguments = new String[0];
                expList = AnUtility.getExpList(noarguments);
              }
              boolean cmp = compareMode && (expList.size() > 1);
              anFrame.setVisible(true);

              String configurationPath;
              if (!Analyzer.getInstance().isRemote() && expList.size() > 0) {
                String expPath = expList.get(0);
                if (!expPath.startsWith("/")) {
                  expPath = Analyzer.getInstance().getWorkingDirectory() + "/" + expPath;
                }
                configurationPath = UserPref.getAsWhenClosedConfigPath(expPath);
                if (!new File(configurationPath).exists()) {
                  configurationPath =
                      UserPref.getConfigurationDirPath(expPath)
                          + "/"
                          + UserPref.getDefaultConfigurationName();
                  if (!new File(configurationPath).exists()) {
                    String parent = new AnFile(expPath).getParentFile().getAbsolutePath();
                    configurationPath = parent + "/" + UserPref.getDefaultConfigurationName();
                    if (!new File(configurationPath).exists()) {
                      configurationPath =
                          UserPref.getHomeConfigurationDirPath()
                              + "/"
                              + UserPref.getDefaultConfigurationName();
                    }
                  }
                }
              } else {
                configurationPath = null;
              }
              if (null == AnWindow.getInstance().getExperimentGroups()) { // first time here
                anFrame
                    .getWindow()
                    .loadExperimentList(expList, cmp, null, true, configurationPath, false);
              }
              if (profile) {
                AnWindow.getInstance()
                    .getProfileApplicationAction()
                    .setEnabled(true); // Not enabled this early!!!!!!
                AnWindow.getInstance().profileApplicationAction(arguments);
              }
              if (errmsg != null) {
                popError(errmsg);
              }
            } catch (Exception e) {
              // IPC didn't run (remote)
              fireConnectionStatus(AnChangeEvent.Type.REMOTE_CONNECTION_CANCELLED_OR_FAILED);
              if (IPC_started) {
                e.printStackTrace();
              }
              try {
                anFrame.getWindow().welcomeViewOnly();
                anFrame.setVisible(true);
                if (System.getProperty("os.name").contains("Mac")) {
                  // A HACK: Resize +1
                  Dimension d0 = anFrame.getSize();
                  Dimension d1 = new Dimension(d0.width + 1, d0.height + 1);
                  anFrame.setSize(d1);
                  anFrame.setSize(d0);
                }
                // Show pop-up
                // String msg = "The Analyzer is now running in remote mode. You can connect to a
                // remote host.";
                // AnUtility.showMessage(anFrame, msg, JOptionPane.INFORMATION_MESSAGE);
              } catch (Exception e2) {
                // Not our day
                e2.printStackTrace();
                // exit?
              }
            }
          }
        });
  }

  /**
   * Main init function. Called from AnMain. Called once. AWT Thread
   *
   * @param args
   * @throws Exception
   */
  public void initAnalyzer(final String[] args) throws Exception {
    AnUtility.checkIfOnAWTThread(true);
    arguments = args;
    anFrame = new AnFrame(this);
    anFrame.initComponents();
    initAnalyzer();
    initAnalyzerIPC();
  }

  private void popError(final String msg) {
    AnWindow.getInstance().getExperimentsView().appendLog(AnLocale.getString("Error: ") + msg);
    AnUtility.showMessage(anFrame, msg, JOptionPane.ERROR_MESSAGE);
  }

  public void endIPC(final String err_msg) {
    popError(err_msg);
    if (!IPC_started) {
      return;
    }
    setIPCStarted(false);
    IPC_session.destroyIPCProc();
  }

  // Experimental code - currently on wrong thread (AWT)
  public void restartEngine() {
    AnWindow aw = AnWindow.getInstance();
    stopConnectionManager();
    try {
      Thread.sleep(100); // A delay in hope all IPC requests finished
      if (isRemote()) {
        Thread.sleep(1000); // An additional delay in hope all IPC requests finished
        // System.err.println("***** NOTE: Waited 1 second before restart engine");
      }
    } catch (Exception e) {
      // nothing
    }
    // Switch IPCReader to restarting mode
    IPC_session.getIPCReader().suspendThread();
    // Restart gp-display-text
    reExec();
    // Wait till gp-display-text is ready. Maximum 10 seconds
    int maxmsec = 10000;
    boolean restarted = false;
    int msec = 0;
    for (; msec < maxmsec; msec++) {
      try {
        if (isRemote()) {
          Thread.sleep(1000); // wait 1000 milliseconds
          msec += 1000;
        } else {
          Thread.sleep(100); // wait 100 milliseconds
          msec += 100;
        }
        if (IPC_session.getIPCReader().isThreadReady()) {
          restarted = true; // Connection established
          // System.err.println("***** NOTE: Connection established");  DEBUG
          break;
        }
      } catch (Exception e) {
        // nothing
      }
    }
    if (restarted == false) {
      // Here we should show an error dialog
      System.err.println("***** ERROR: Cannot restart gp-display-text. Please, reconnect.");
    }
    IPC_session.getIPCReader().runThread();
    // Version Handshake
    int res = versionHandshake();
    // Call initApplication
    String l_fdhome = emptyString;
    String l_licpath = System.getProperty("analyzer.licpath", l_fdhome + "/lib/serial.dat");
    String[] args = new String[1];
    args[0] = er_print;
    String[] license_info = new String[2];
    license_info[0] = "ERROR";
    license_info[1] = "Unknown";
    try {
      license_info = initApplication(false, l_fdhome, l_licpath, args);
    } catch (Exception e) {
      if (null == license_info[0]) {
        license_info[0] = "ERROR";
      }
    }
    // Init view
    aw.initView(0, 0);
    startConnectionManager();
  } // Native methods from liber_dbe.so

  /** Restart gp-display-text */
  public void reExec() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, null);
    ipcHandle.append("reExec");
    IPCResult ipcResult = ipcHandle.sendRequest();
  }

  // Native methods from liber_dbe.so
  private String[] initApplication(
      final boolean in_netbeans, final String fdhome, final String licpath, final String[] args) {
    synchronized (IPC.lock) {
      IPC_session.send("initApplication");
      IPC_session.send(in_netbeans);
      IPC_session.send(fdhome);
      IPC_session.send(licpath);
      IPC_session.send(args);
      return (String[]) IPC_session.recvObject();
    }
  }

  /**
   * Get backend version and compare with frontend version
   *
   * @return 1 if versions do not match, return 0 is versions match
   */
  private int versionHandshake() {
    synchronized (IPC.lock) {
      IPCHandle ipcHandle = IPCHandle.newHandshakeIPCHandle();
      IPCResult ipcResult = ipcHandle.sendRequest(); // no body
      IPCResult.CC cc = ipcResult.getCC(); // blocking
      if (cc == IPCResult.CC.SUCCESS) {
        int er_printVersion = ipcResult.getVersion();
        if (er_printVersion != IPCProtocol.version) {
          System.err.println("Frontend/backend protocol version mis-match:");
          System.err.println("GUI version: " + IPCProtocol.version);
          System.err.println("gp-display-text version: " + er_printVersion);
          return (1);
        }
      }
    }
    return (0);
  }

  private int syncTime() {
    synchronized (IPC.lock) {
      IPC_session.send("syncTime");
      IPC_session.send(IPCLogger.getTimeStamp());
      IPC_session.recvString(); // blocking
    }
    return (0);
  }

  /**
   * Get backend locale
   *
   * @return result : current locale
   */
  final String getLocale() {
    synchronized (IPC.lock) {
      IPC_session.send("getLocale");
      return IPC_session.recvString(); // blocking
    }
  }

  /**
   * Set backend locale
   *
   * @return result : current locale
   */
  private String setLocale(final String locale) {
    synchronized (IPC.lock) {
      IPC_session.send("setLocale");
      IPC_session.send(locale);
      return IPC_session.recvString(); // blocking
    }
  }

  /**
   * Initialize backend application
   *
   * @param win_id
   * @param clone_id
   * @return String
   */
  public String IPC_initView(final int win_id, final int clone_id) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("initView");
    ipcHandle.append(win_id);
    ipcHandle.append(clone_id);
    IPCResult ipcResult = ipcHandle.sendRequest();
    String result = ipcResult.getString(); // blocking
    return result;
  }

  /**
   * Get current directory
   *
   * @return result : full path
   */
  final String getCurrentDirectory() {
    synchronized (IPC.lock) {
      IPC_session.send("getCurrentDirectory");
      return IPC_session.recvString();
    }
  }

  /**
   * Set current directory
   *
   * @param dir_name
   * @return result : 0 done, -1 error
   */
  final int setCurrentDirectory(final String dir_name) {
    synchronized (IPC.lock) {
      IPC_session.send("setCurrentDirectory");
      IPC_session.send(dir_name);
      return IPC_session.recvInt();
    }
  }

  public static String getAnalyzerReleaseName() {
    return PERFORMANCE_ANALYZER_RELEASE_NAME;
  }

  /** Update IPC connection status */
  public void setIPCStarted(boolean started) {
    IPC_started = started;
  }

  private void fireConnectionStatus(AnChangeEvent.Type type) {
    AnChangeEvent anChangeEvent = new AnChangeEvent(this, type);
    AnEventManager.getInstance().fireAnChangeEvent(anChangeEvent);
  }
}
