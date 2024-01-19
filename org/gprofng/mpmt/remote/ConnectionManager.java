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

package org.gprofng.mpmt.remote;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCLogger;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * ConnectionManager thread creates and manages ConnectionChecker thread, which sends
 * "checkConnection" IPC calls.
 *
 * <p>Sets interface variables doCheck and defaultTimeOut.
 *
 * <p>Uses interface variables connectionStatus and connectionCheckerState.
 */
public class ConnectionManager extends Thread {

  int CC_defaultTimeOut = 2000; // 2 seconds
  int CM_defaultTimeOut = 10000; // 10 seconds
  int CONNECTION_UNKNOWN = 0;
  int CONNECTION_OK = 1;
  int CONNECTION_LOST = 2;
  int CONNECTION_CHECK = 3;
  volatile int connectionStatus = CONNECTION_UNKNOWN;
  int CM_NO_CHECK = 0;
  int CM_CHECK = 1;
  volatile int connectionManagerStatus = CM_CHECK;
  ConnectionChecker cc = null;
  Boolean doCheck = false;
  String threadName = "Connection Manager";
  JDialog CM_warn_dialog = null;

  /** Default ConnectionManager constructor. */
  public ConnectionManager() {
    this.setName(threadName);
  }

  /** Main method. */
  public void run() {
    AnWindow anWindow = AnWindow.getInstance();
    String err_cc_msg = "ERROR: Cannot create ConnectionChecker";
    String warn_msg = AnLocale.getString("Lost connection to remote host ");
    String warn_title = AnLocale.getString("Warning: Connection is lost");
    String conf_msg = AnLocale.getString("Are you sure you want to exit gprofng GUI");
    String conf_title = AnLocale.getString("Exit gprofng GUI?");
    // System.err.println("ConnectionManager started"); // DEBUG
    try {
      String str = AnUtility.getenv("SP_ANALYZER_HEARTBEAT_DISABLE");
      if (str != null) {
        // System.err.println("analyzer: SP_ANALYZER_HEARTBEAT_DISABLE is set");  DEBUG
        IPCLogger.logTrace("\n" + "analyzer: SP_ANALYZER_HEARTBEAT_DISABLE is set");
        connectionManagerStatus = CM_NO_CHECK;
      }
      str = AnUtility.getenv("SP_ANALYZER_HEARTBEAT_MSEC");
      if (str != null) { // set timeout value
        // System.err.println("analyzer: SP_ANALYZER_HEARTBEAT_MSEC=" + str);  DEBUG
        IPCLogger.logTrace("\n" + "analyzer: SP_ANALYZER_HEARTBEAT_MSEC=" + str);
        Integer tm = new Integer(str);
        setTimeOut(tm.intValue());
      }
      long ts_start_checking = 0;
      while (CC_defaultTimeOut > 0) {
        if ((connectionManagerStatus != CM_NO_CHECK) && (Analyzer.getInstance().isRemote())) {
          if (null != cc) {
            if (cc.connectionCheckerState == cc.CHECKER_FINISHED) {
              // System.err.println("ConnectionChecker finished"); // DEBUG
              cc.join();
              // System.err.println("ConnectionChecker joined"); // DEBUG
              cc = new ConnectionChecker();
              if (null == cc) {
                Logger.getLogger(ConnectionManager.class.getName())
                    .log(Level.SEVERE, null, err_cc_msg);
                // System.err.println(err_cc_msg); // DEBUG
                return;
              }
              cc.start();
              // System.err.println("ConnectionChecker started"); // DEBUG
            }
          } else {
            cc = new ConnectionChecker();
            if (null == cc) {
              Logger.getLogger(ConnectionManager.class.getName())
                  .log(Level.SEVERE, null, err_cc_msg);
              // System.err.println(err_cc_msg); // DEBUG
              return;
            }
            cc.start();
            // System.err.println("ConnectionChecker started"); // DEBUG
          }
          if (connectionStatus == CONNECTION_LOST) {
            if (cc.connectionCheckerState == cc.CHECKER_FINISHED) {
              // System.err.println("ConnectionChecker finished"); // DEBUG
              cc.join();
              // System.err.println("ConnectionChecker joined"); // DEBUG
              cc = null;
            }
            // Show error dialog
            String sWait = AnLocale.getString("Wait");
            String sExit = AnLocale.getString("Exit");
            String host = Analyzer.getInstance().getRemoteHost();
            int at = host.indexOf("@");
            if (at >= 0) {
              host = host.substring(at + 1);
            }
            Object[] options = {sWait, sExit};
            JOptionPane pane =
                new JOptionPane(
                    warn_msg + host,
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,
                    options,
                    options[0]);
            // Use CM_warn_dialog to allow ConnectionChecker thread to close it
            CM_warn_dialog = pane.createDialog(anWindow.getFrame(), warn_title);
            CM_warn_dialog.setVisible(true);
            Object selectedValue = pane.getValue();
            CM_warn_dialog = null; // Sync with ConnectionChecker
            // There are 2 choices: "Wait" and "Exit"
            // System.err.println("selectedValue:" + selectedValue); // DEBUG
            int choice = 0; // Cancel means Wait
            if (selectedValue instanceof Integer) {
              choice = ((Integer) selectedValue).intValue();
              if (choice != 1) choice = 0; // Wait
            }
            if (selectedValue instanceof String) {
              if (sExit.equals(selectedValue)) choice = 1; // Exit
            }
            // Exit - stop ConnectionChecker and exit
            if (sExit.equals(options[choice])) {
              // Show confirmation dialog
              selectedValue =
                  JOptionPane.showConfirmDialog(
                      anWindow.getFrame(), conf_msg, conf_title, JOptionPane.YES_NO_OPTION);
              // System.err.println("selectedValue:" + selectedValue); // DEBUG
              if (selectedValue instanceof Integer) {
                choice = ((Integer) selectedValue).intValue();
              }
              if (choice == 0) { // YES
                stopChecking();
                if (null != cc) {
                  cc.interrupt();
                  cc.join(CC_defaultTimeOut * 2);
                  // System.err.println("ConnectionChecker joined."); // DEBUG
                  cc = null;
                }
                // System.err.println("Exiting..."); // DEBUG
                AnUtility.dispatchOnSwingThread(
                    new Runnable() {
                      @Override
                      public void run() {
                        AnWindow.getInstance().exitAction();
                      }
                    });
                return;
              }
            }
            // Restore connectionStatus to CONNECTION_CHECK
            connectionStatus = CONNECTION_CHECK;
            // Wait
            if (null == cc) {
              cc = new ConnectionChecker();
              if (null == cc) {
                Logger.getLogger(ConnectionManager.class.getName())
                    .log(Level.SEVERE, null, err_cc_msg);
                // System.err.println(err_cc_msg); // DEBUG
                return;
              }
              cc.start();
            }
            // Wait for next timeout
            ts_start_checking = System.currentTimeMillis();
            // System.err.println("Waiting..."); // DEBUG
            startChecking();
          }
          if (connectionStatus == CONNECTION_CHECK) {
            if (ts_start_checking != 0) {
              long ts = System.currentTimeMillis();
              long dts = ts - ts_start_checking;
              if (dts > CM_defaultTimeOut) {
                connectionStatus = CONNECTION_LOST;
                continue;
              }
            } else {
              ts_start_checking = System.currentTimeMillis();
            }
          }
          if (connectionStatus == CONNECTION_OK) {
            ts_start_checking = 0;
          }
        }
        try {
          sleep(CC_defaultTimeOut / 4); // Check more often
        } catch (InterruptedException ex) {
          // Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      if (null != cc) {
        // System.err.println("ConnectionChecker interrupted"); // DEBUG
        cc.interrupt();
        cc.join(CC_defaultTimeOut * 2);
        cc = null;
        // System.err.println("ConnectionChecker joined"); // DEBUG
      }
    } catch (InterruptedException ex) {
      Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
    }
    // System.err.println("ConnectionManager finished"); // DEBUG
  }

  /**
   * Sets default timeout value. Minimum: 100 milliseconds
   *
   * @param timeout
   * @return defaultTimeOut
   */
  public int setTimeOut(int timeout) {
    if (timeout < 100) {
      timeout = 100; // not less than 100 milliseconds
    }
    CC_defaultTimeOut = timeout;
    return CC_defaultTimeOut;
  }

  /**
   * Gets default timeout value
   *
   * @return defaultTimeOut
   */
  public int getTimeOut() {
    return CC_defaultTimeOut;
  }

  /** Sets "Start Checking" flag. */
  public void startChecking() {
    // System.err.println("startChecking"); // DEBUG
    doCheck = true;
  }

  /** Sets "Stop Checking" flag. */
  public void stopChecking() {
    // System.err.println("stopChecking"); // DEBUG
    doCheck = false;
  }

  /**
   * Thread ConnectionChecker sends "checkConnection" IPC calls. Uses interface variables doCheck
   * and defaultTimeOut. Sets interface variables connectionStatus and connectionCheckerState.
   */
  private class ConnectionChecker extends Thread {

    int CHECKER_NOT_IN_IPC = 0; // Not in IPC call
    int CHECKER_IN_IPC = 1; // In IPC call
    int CHECKER_FINISHED = 2;
    volatile int connectionCheckerState = CHECKER_NOT_IN_IPC;
    String threadName = "Connection Checker";

    /** Default ConnectionChecker constructor. */
    public ConnectionChecker() {
      this.setName(threadName);
    }

    /**
     * Check Connection
     *
     * @return res
     */
    private int checkConnection() {
      int res = 0;
      long ts0, ts1;
      long n = 1;
      AnWindow anWindow = AnWindow.getInstance();
      while (CC_defaultTimeOut > 0) {
        ts0 = System.currentTimeMillis();
        ts1 = ts0 + CC_defaultTimeOut;
        try {
          sleep(CC_defaultTimeOut);
        } catch (InterruptedException ex) {
          // Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (doCheck != true) {
          continue; // Do not check connection
        }
        // Ping remote host - this thread can hang here
        String s = "Check:" + n;
        // System.err.println("ConnectionChecker sent ping: " + s + "  ts=" + ts1); // DEBUG
        connectionStatus = CONNECTION_CHECK;
        connectionCheckerState = CHECKER_IN_IPC;
        String r = null;
        int ver = 2;
        if (1 == ver) { // use Sync IPC
          r = anWindow.checkConnection(s); // Sync IPC call
        } else { // use Async IPC
          //                    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.NON_DBE);
          //                    ipcHandle.append("checkConnection");
          //                    ipcHandle.append(s);
          //                    ipcHandle.append(0);
          //                    IPCResult ipcResult = ipcHandle.sendRequest();
          //                    r = ipcResult.getString(); // blocking
          // Temporary use Handshake request - it is really async
          r = s; // fake reply
          IPCHandle ipcHandle = IPCHandle.newHandshakeIPCHandle();
          IPCResult ipcResult = ipcHandle.sendRequest(); // no body
          IPCResult.CC cc = ipcResult.getCC(); // blocking
        }
        connectionCheckerState = CHECKER_NOT_IN_IPC;
        if (!s.equals(r)) {
          // System.err.println("ConnectionChecker got wrong reply: " + r + " instead of " + s); //
          // DEBUG
          res = 1;
          break;
        }
        n++;

        // Debugging code to simulate lost connection
        //                int debug = 1;
        //                if (debug > 0) {
        //                    File f = new File("/tmp/nm_remote_analyzer_check_100"); // DEBUG
        //                    if (f.exists()) {
        //                        f.delete();
        //                        setTimeOut(100);
        //                    }
        //                    f = new File("/tmp/nm_remote_analyzer_check_2000"); // DEBUG
        //                    if (f.exists()) {
        //                        f.delete();
        //                        setTimeOut(2000);
        //                    }
        //                    f = new File("/tmp/nm_remote_analyzer_stop_checking"); // DEBUG
        //                    if (f.exists()) {
        //                        f.delete();
        //                        res = 2;
        //                        break;
        //                    }
        //                    f = new File("/tmp/nm_remote_analyzer_hang_checking"); // DEBUG
        //                    if (f.exists()) {
        //                        //f.delete();
        //                        connectionStatus = CONNECTION_CHECK;
        //                        continue;
        //                    }
        //                }

        connectionStatus = CONNECTION_OK;
        // Close warning dialog if necessary - HACK and NO SYNC
        final JDialog d = CM_warn_dialog;
        if (d != null) {
          // System.err.println("Closing dialog..."); // DEBUG
          AnUtility.dispatchOnSwingThread(
              new Runnable() {
                @Override
                public void run() {
                  d.dispose();
                }
              });
        }
      }
      return res;
    }

    /** Main method Calls checkConnection() */
    public void run() {
      int res = checkConnection();
      if (res != 0) {
        connectionStatus = CONNECTION_LOST;
      }
      connectionCheckerState = CHECKER_FINISHED; // finished
    }
  }
}
