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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnDialog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.persistence.UserPref.ConnectionProperties;
import org.gprofng.mpmt.picklist.StringPickListElement;
import org.gprofng.mpmt.statuspanel.StatusLabelValueHandle;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class ConnectionDialog extends AnDialog implements ItemListener {

  public static final String help_id = AnVariable.HELP_ConnectRemoteHost;
  private static String startDir = null;
  private JComboBox hostNameComboBox;
  private JTextField file;
  private JTextField usernameTextField;
  private JTextField solstudioPathTextField;
  private JTextField connectCommandTextField;
  private JLabel statusValueLabel;
  private JTextArea messageTextArea;
  private AnWindow m_window;
  private JProgressBar progressBar = null;
  public boolean cancelRequest = false;
  private final StatusLabelValueHandle connectedStatusHandle;
  private Thread threadConnector = null;
  private JButton was_ok, was_apply, was_close;
  private static final String connecting = AnLocale.getString("connecting...");
  private static final String connected = AnLocale.getString("connected");
  protected static final String notConnected = AnLocale.getString("not connected");
  private static final String disconnected = AnLocale.getString("connection failed");
  private static final String local_host = "localhost";
  private ConnectionPanel connectionPanel;
  private String defaultSolstudioPath = null;
  private DocumentListener comboboxDocumentListener = null;
  private List<Authentication> authentications = null;

  // Constructor
  public ConnectionDialog(final AnWindow window, final Frame frame,
      StatusLabelValueHandle connectedStatusHandle) {
    super(window, frame, AnLocale.getString("Connect to Remote Host"), false,
        null, null, help_id, false);
    this.connectedStatusHandle = connectedStatusHandle;
    m_window = window;
    initComponents();
    updateStatus();
  }

  // Initialize GUI components
  private void initComponents() {
    getContentPane().setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    connectionPanel = new ConnectionPanel(this);
    hostNameComboBox = connectionPanel.getHostNameComboBox();
    String os_name = Analyzer.getInstance().os_name;
    String lh = local_host;
    if (null != os_name) {
      if ((!os_name.equals("SunOS")) && (!os_name.equals("Linux"))) {
        lh = "";
      }
    }
    hostNameComboBox.addItem(lh);
    int count = 0;
    for (StringPickListElement hostElement :
        UserPref.getInstance().getHostNamePicklist().getStringElements()) {
      hostNameComboBox.addItem(hostElement.getString());
      count = count + 1;
    }
    if (count > 0) {
      hostNameComboBox.setSelectedIndex(1);
    }    
    else {
      hostNameComboBox.setSelectedIndex(0);
    }
    hostNameComboBox.setEditable(true);
    ((JTextField) hostNameComboBox.getEditor().getEditorComponent())
        .getDocument()
        .addDocumentListener(comboboxDocumentListener = new ComboBoxDocumentListener());

    usernameTextField = connectionPanel.getUserNameTextField();
    solstudioPathTextField = connectionPanel.getSolstudioPathTextField();
    connectCommandTextField = connectionPanel.getConnectCommandTextField();
    statusValueLabel = connectionPanel.getConnectionStatusValueLabel();
    statusValueLabel.setForeground(Color.BLACK);
    messageTextArea = connectionPanel.getMessageTextArea();
    String status = notConnected;
    Analyzer an = Analyzer.getInstance();
    if (an.IPC_started) {
      if (an.remoteConnection == null) {
        status = an.localHost;
      } else {
        status = an.remoteHost;
      }
    }
    statusValueLabel.setText(status);
    progressBar = connectionPanel.getConnectionProgressBar();
    connectCommandTextField.setText(an.remoteShell);

    // Change buttons
    JButton[] buttons = getDefaultButtons();
    if (buttons.length > 2) {
      was_ok = buttons[0];
      was_ok.setText(AnLocale.getString("Connect"));
      was_ok.setEnabled(true);
      close_on_enter = false;
      was_apply = buttons[1];
      was_apply.setText(AnLocale.getString("Cancel"));
      was_apply.setEnabled(false);
      was_close = buttons[2];
      was_close.setEnabled(true);
      was_close.setMnemonic(AnLocale.getString('l', "RemoteConnectDialogCloseButtonMN"));
    }

    setAccessory(connectionPanel);
  }

  protected List<Authentication> getAuthenticationsCopy() {
    List<Authentication> copy = new ArrayList<Authentication>();
    for (Authentication auth : authentications) {
      Authentication authCopy = auth.copy();
      copy.add(authCopy);
    }
    return copy;
  }

  protected void setAuthentications(List<Authentication> authentications) {
    this.authentications = authentications;
  }

  class ComboBoxDocumentListener implements DocumentListener {

    @Override
    public void changedUpdate(DocumentEvent e) {
      updateStatus();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      updateStatus();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      updateStatus();
    }
  }

  private void updateStatus() {
    AnUtility.checkIfOnAWTThread(true);
    String hostName = ((JTextField) hostNameComboBox.getEditor().getEditorComponent()).getText();
    String ssh_cmd = window.getAnalyzer().remoteShell;
    String install_dir = "/usr/bin";
    String user_name = AnUtility.getenv("USER");
    if (user_name == null) {
      user_name = "";
    }

    ConnectionProperties connectionProperties =
        UserPref.getInstance().getConnectionPropertiesMap().get(hostName);
    if (connectionProperties != null) {
      String s = connectionProperties.getConnectCommand();
      if (s != null && s.length() > 0) {
        ssh_cmd = s;
      }
      s = connectionProperties.getPath();
      if (s != null && s.length() > 0) {
        install_dir = s;
      }
      s = connectionProperties.getUserName();
      if (s != null && s.length() > 0) {
        user_name = s;
      }
    }
    connectionPanel.setLoginFieldsEnabled(!hostName.equals(local_host));
    connectCommandTextField.setText(ssh_cmd);
    solstudioPathTextField.setText(install_dir);
  }

  // Set visible
  public void setVisible(final boolean set, final int active_id) {
    super.setVisible(set);
  }

  /*
   * Listener to switch between file/printer
   */
  @Override
  public void itemStateChanged(final ItemEvent event) {
    //    final Object source = event.getItemSelectable();
  }

  /*
   * Connect to remote host
   */
  public String connectToRemoteHost(int timeout) {
    try {
      if (!isVisible()) {
        setVisible(true);
        // progressBar.setIndeterminate(true);
      }
      String host = hostNameComboBox.getSelectedItem().toString();
      if (host != null) {
        host = host.trim();
      }
      if ((host == null) || (host.length() < 1)) {
        return (AnLocale.getString("Error: Remote Host Name is not specified."));
      }
      String un = usernameTextField.getText();
      if (un != null) {
        un = un.trim();
      }
      char[] p = null;
      /*
      char[] p = passwordField.getPassword();
      if (p.length <= 0) {
        p = null;
      }
      */
      String connectCommand = connectCommandTextField.getText().trim();
      if (connectCommand.length() < 1) {
        return (AnLocale.getString("Error: connection command is not specified."));
      }
      connectCommand += " -o PasswordAuthentication=no " + 
          "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no";
      String path = solstudioPathTextField.getText().trim();
      if (path.length() < 1) {
        return (AnLocale.getString("Error: remote gprofng path is not specified."));
      }
      String appName = Analyzer.DisplayAppName;
      if (!path.endsWith("/" + appName)) {
        if (path.endsWith("/bin")) {
          path = path + "/" + appName;
        } else if (path.endsWith("/bin/")) {
          path = path + appName;
        } else if (path.endsWith("/")) {
          path = path + "bin/" + appName;
        } else {
          path = path + "/bin/" + appName;
        }
      }
      window.getAnalyzer().remoteGprofngPath = path.substring(0, path.length() - 16);
      String s = m_window.getAnalyzer().createNewIPC(this, host, un, p, connectCommand, path);
      if (s != null) {
        return (s);
      }
    } catch (Exception e) {
      return ("Exception during connecting."); // Cancel
    }
    return null; // Successfully connected
  }

  /**
   * Action performed
   *
   * @param event
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    final String str_file;
    String cmd = event.getActionCommand();
    if (cmd.equals(AnLocale.getString("Browse..."))) {
      if ((str_file = getFile()) != null) {
        file.setText(str_file);
      }
      return;
    }
    if (cmd.equals(AnLocale.getString("OK")) || cmd.equals(AnLocale.getString("Connect"))) {
      String hostName = hostNameComboBox.getSelectedItem().toString();
      hostName = hostName.trim().toLowerCase();

      final ConnectionProperties connectionProperties;
      //            if (hostName.equals(local_host)) {
      Map<String, ConnectionProperties> map = UserPref.getInstance().getConnectionPropertiesMap();
      connectionProperties =
          new ConnectionProperties(
              solstudioPathTextField.getText(), connectCommandTextField.getText(), usernameTextField.getText(), authentications);
      map.put(hostName, connectionProperties);
      if (!(hostName.equals(local_host))) {
          UserPref.getInstance().getHostNamePicklist().addElement(hostName);
      }
      // System.out.println("Connect to host: " + hostName);
      if (comboboxDocumentListener != null) {
        ((JTextField) hostNameComboBox.getEditor().getEditorComponent())
            .getDocument()
            .removeDocumentListener(comboboxDocumentListener);
      }
      hostNameComboBox.removeAllItems();
      hostNameComboBox.addItem(local_host);
      for (StringPickListElement hostElement :
          UserPref.getInstance().getHostNamePicklist().getStringElements()) {
	// System.out.println("past remote host: " + hostElement.getString());
        hostNameComboBox.addItem(hostElement.getString());
      }
      hostNameComboBox.setSelectedItem(hostName);
      if (comboboxDocumentListener != null) {
        ((JTextField) hostNameComboBox.getEditor().getEditorComponent())
            .getDocument()
            .addDocumentListener(comboboxDocumentListener);
      }
      //            } else {
      //                connectionProperties = null;
      //            }
      connectionStarted();
      // Connect to remote host
      Thread worker =
          new Thread(/*m_window.getAnalyzer().tgroup, */ "Connection_thread") {

            final int timeout = 3;

            @Override
            public void run() {
              /* synchronized (AnVariable.worker_lock) */ {
                threadConnector = this;
                cancelRequest = false;
                String res = connectToRemoteHost(timeout);
                if (cancelRequest) {
                  cancelRequest = false;
                  res += ". Connection canceled";
                }
                connectionFinished(res);
              }
            }
          };
      worker.start();
    } else if (cmd.equals(AnLocale.getString("Apply"))
        || cmd.equals(AnLocale.getString("Cancel"))) {
      if (connectedStatusHandle.get().equals(connecting)) {
        cancelRequest = true;
        if (threadConnector != null) {
          try {
            threadConnector.interrupt();
            connectionFinished("Connection canceled");
          } catch (Exception e) {
            // thread may already had gone
          }
          threadConnector = null;
        }
      } else {
        setVisible(false);
      }
    }
  }

  /**
   * Gets host name
   *
   * @return String host name
   */
  //    public String getHost() {
  //        String host = fld_host.getSelectedItem().toString();
  //        return host;
  //    }
  /** Sets buttons and fields states as they should be during connecting */
  private void connectionStarted() {
    Analyzer.getInstance().connectingToRemoteHost = true;
    //        progressBar.setVisible(true);
    connectedStatusHandle.update(connecting);
    progressBar.setIndeterminate(true);
    was_ok.setEnabled(false);
    was_apply.setEnabled(true);
    hostNameComboBox.setEditable(false);
    // passwordField.setEditable(false);
    usernameTextField.setEditable(false);
    solstudioPathTextField.setEditable(false);
    connectCommandTextField.setEditable(false);
    // fld_status.setFont() // bold
    statusValueLabel.setForeground(Color.black);
    statusValueLabel.setText(connecting);
    messageTextArea.setText(AnLocale.getString("Connecting..."));
    //        pack();
  }

  /** Sets buttons and fields states as they should be after connecting */
  private void connectionFinished(String result) {
    //        progressBar.setMaximum(10);
    progressBar.setValue(0);
    progressBar.setIndeterminate(false);
    //        progressBar.setVisible(false);

    was_ok.setEnabled(true);
    was_apply.setEnabled(false);
    hostNameComboBox.setEditable(true);
    // passwordField.setEditable(true);
    usernameTextField.setEditable(true);
    solstudioPathTextField.setEditable(true);
    connectCommandTextField.setEditable(true);
    if (result == null) {
      String hostName = hostNameComboBox.getSelectedItem().toString();
      //            if (hostName.equals(local_host)) {
      //                connectedStatusHandle.update(hostName, StatusLabelValueHandle.Mode.DEFAULT);
      //                statusValueLabel.setForeground(Color.BLACK);
      //                statusValueLabel.setText(local_host);
      //                statusValueLabel.setToolTipText(local_host);
      //            } else {
      connectedStatusHandle.update(
          AnUtility.getShortString(hostName, 30), StatusLabelValueHandle.Mode.SET);
      statusValueLabel.setForeground(new Color(44, 121, 6));
      result = connected + " to " + hostName;
      statusValueLabel.setText(result);
      statusValueLabel.setToolTipText(result);
      //            }
      setVisible(false);
      messageTextArea.setText("");
    } else {
      connectedStatusHandle.update(
          AnLocale.getString("connection failed"), StatusLabelValueHandle.Mode.ERROR);
      statusValueLabel.setForeground(Color.red);
      // statusValueLabel.setText(disconnected);
      statusValueLabel.setText(result);
      statusValueLabel.setToolTipText(result);
      //            setVisible(true); // Show error
      messageTextArea.append("\n");
      messageTextArea.append(AnLocale.getString("Connection failed: "));
      String s =
          Analyzer.getInstance().explainConnectionProblem(result, usernameTextField.getText());
      messageTextArea.append(AnLocale.getString(s));
      // Check if this is CR 22373059:
      // Is it MacOS?
      // Do we use java 7?
      // If "yes" and "yes" show a message and restart remote analyzer client with another java
    }
    Analyzer.getInstance().connectingToRemoteHost = false;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            AnWindow.getInstance().enableSessionActions(false, false);
          }
        });
    //        pack();
  }

  /**
   * Updates Connection Status
   *
   * @param status
   */
  public void updateConnectionStatus(String status) {
    statusValueLabel.setForeground(Color.black);
    statusValueLabel.setText(status);
    messageTextArea.append("\n");
    messageTextArea.append(AnLocale.getString(status));
  }

  /** Set file name by interpreting ~ */
  private static String getFullPath(final String fname) {
    String fullpath = null;

    if (fname != null) {
      if (fname.startsWith("~/")) {
        fullpath = fname.replaceFirst("~/", Analyzer.home_dir + "/");
      } else if (fname.startsWith("~")) {
        fullpath = fname.replaceFirst("~", "/home/");
      } else {
        fullpath = fname;
      }
    }
    return fullpath;
  }

  /** Get file name from the file chooser */
  private String getFile() {
    final JFileChooser filechooser;
    final File prn_file;
    final String path;

    if (startDir == null) {
      startDir = Analyzer.getInstance().getWorkingDirectory();
    }
    filechooser = new JFileChooser(startDir);
    filechooser.setFileHidingEnabled(false);
    filechooser.setDialogTitle(AnLocale.getString("SolStudio Location"));
    filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

    if ((filechooser.showDialog(dialog, AnLocale.getString("Set")) == JFileChooser.APPROVE_OPTION)
        && ((prn_file = filechooser.getSelectedFile()) != null)) {
      path = prn_file.getAbsolutePath();
    } else {
      path = null;
    }

    startDir = filechooser.getCurrentDirectory().getAbsolutePath();

    return path;
  }
}
