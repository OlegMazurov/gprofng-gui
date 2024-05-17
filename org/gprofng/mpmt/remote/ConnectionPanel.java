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
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.util.gui.AnDialog2;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Font;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
// import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ConnectionPanel extends JPanel {

  private final ConnectionDialog connectionDialog;

  /** Creates new form ConnectionPanel */
  public ConnectionPanel(ConnectionDialog connectionDialog) {
    this.connectionDialog = connectionDialog;
    initComponents();
    setOpaque(false);
    AnUtility.setAccessibleContext(
        messageTextArea.getAccessibleContext(), AnLocale.getString("Information Area"));
    messageTextArea.setBackground(AnEnvironment.DEFAULT_TEXT_PANE_RO_BACKGROUND);

    hostNameComboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    hostNameComboBox.setFont(hostNameComboBox.getFont().deriveFont(Font.PLAIN));

    hostNameLabel.setText(AnLocale.getString("Remote Host:"));
    hostNameLabel.setToolTipText(AnLocale.getString("Name of the remote system"));
    hostNameLabel.setDisplayedMnemonic(AnLocale.getString('o', "RemoteDialogHostNameLabelMN"));
    hostNameLabel.setLabelFor(hostNameComboBox);

    /*
    authenticationLabel.setText(AnLocale.getString("Authentication:"));
    authenticationLabel.setToolTipText(AnLocale.getString("Autentication methods"));

    authenticationManageButton.setText(AnLocale.getString("Manage"));
    authenticationManageButton.setToolTipText(AnLocale.getString("Manage authentication methods"));
    authenticationManageButton.setMnemonic(AnLocale.getString('M', "AuthenticationManageButton"));
    */
    
    connectCommandLabel.setText(AnLocale.getString("Connect Command:"));
    connectCommandLabel.setToolTipText(
        AnLocale.getString(
            "Connect command for gprofng remote system"));
    connectCommandLabel.setDisplayedMnemonic(AnLocale.getString('I', "RemoteDialogConnectCommandLabelMN"));
    connectCommandLabel.setLabelFor(connectCommandTextField);

    userNameLabel.setText(AnLocale.getString("User Name:"));
    userNameLabel.setToolTipText(
        AnLocale.getString("User name of the account to log in to the remote system"));
    userNameLabel.setDisplayedMnemonic(AnLocale.getString('u', "RemoteDialogUserNameLabelMN"));
    userNameLabel.setLabelFor(userNameTextField);

    /*
    passWordLabel.setText(AnLocale.getString("Password:"));
    passWordLabel.setToolTipText(
        AnLocale.getString("Password for the account to log in to the remote system"));
    passWordLabel.setDisplayedMnemonic(AnLocale.getString('p', "RemoteDialogPasswordLabelMN"));
    passWordLabel.setLabelFor(passwordField);
    */

    solstudioPathLabel.setText(AnLocale.getString("Remote gprofng Path:"));
    solstudioPathLabel.setToolTipText(
        AnLocale.getString(
            "Full path to the gprofng installation accessible on the remote system"));
    solstudioPathLabel.setDisplayedMnemonic(AnLocale.getString('I', "RemoteDialogPathLabelMN"));
    solstudioPathLabel.setLabelFor(solstudioPathTextField);

    connectionStatusLabel.setText(AnLocale.getString("Connection Status:"));
    connectionStatusLabel.setToolTipText(AnLocale.getString("Status of connection"));

    connectionStatusValueLabel.setFont(connectionStatusValueLabel.getFont().deriveFont(Font.PLAIN));

    /*
    AnUtility.setAccessibleContext(
        authenticationTextField.getAccessibleContext(), AnLocale.getString("Authentications"));
    */
    AnUtility.setAccessibleContext(
        connectionStatusValueLabel.getAccessibleContext(),
        AnLocale.getString("Status of connection"));
    AnUtility.setAccessibleContext(
        connectionProgressBar.getAccessibleContext(), AnLocale.getString("Progress"));

    // For now
    userNameTextField.setText(AnUtility.getenv("USER"));
    // passwordField.setText("");
  }

  public void setLoginFieldsEnabled(boolean remote) {

    if (!remote) { // local host
      // authenticationLabel.setEnabled(false);
      solstudioPathLabel.setEnabled(false);
      solstudioPathTextField.setEnabled(false);
      connectCommandLabel.setEnabled(false);
      connectCommandTextField.setEnabled(false);
    } else {
      // authenticationLabel.setEnabled(true);
      solstudioPathLabel.setEnabled(true);
      solstudioPathTextField.setEnabled(true);
      connectCommandLabel.setEnabled(true);
      connectCommandTextField.setEnabled(true);
    }
  }

  public JProgressBar getConnectionProgressBar() {
    return connectionProgressBar;
  }

  public JTextArea getMessageTextArea() {
    return messageTextArea;
  }

  public JLabel getConnectionStatusValueLabel() {
    return connectionStatusValueLabel;
  }

  public JComboBox getHostNameComboBox() {
    return hostNameComboBox;
  }

  /*
  public JLabel getPasswordLabel() {
    return passWordLabel;
  }

  public JPasswordField getPasswordField() {
    return passwordField;
  }
  */

  public JTextField getSolstudioPathTextField() {
    return solstudioPathTextField;
  }

    public JTextField getConnectCommandTextField() {
    return connectCommandTextField;
  }

  /*
  public JLabel getAuthenticationLabel() {
    return authenticationLabel;
  }

  public JTextField getAuthenticationTextField() {
    return authenticationTextField;
  }

  public JButton getAuthenticationManageButton() {
    return authenticationManageButton;
  }
  */

  public JTextField getUserNameTextField() {
    return userNameTextField;
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        hostNameLabel = new javax.swing.JLabel();
        hostNameComboBox = new javax.swing.JComboBox();
        solstudioPathLabel = new javax.swing.JLabel();
        solstudioPathTextField = new javax.swing.JTextField();
        userNameLabel = new javax.swing.JLabel();
        userNameTextField = new javax.swing.JTextField();
        connectCommandLabel = new javax.swing.JLabel();
        connectCommandTextField = new javax.swing.JTextField();
        connectionStatusLabel = new javax.swing.JLabel();
        connectionStatusValueLabel = new javax.swing.JLabel();
        messageTextScrollPane = new javax.swing.JScrollPane();
        messageTextArea = new javax.swing.JTextArea();
        connectionProgressBar = new javax.swing.JProgressBar();

        setLayout(new java.awt.GridBagLayout());

        hostNameLabel.setText("Host name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        add(hostNameLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        add(hostNameComboBox, gridBagConstraints);

        solstudioPathLabel.setText("Remote SolStudio Path:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        add(solstudioPathLabel, gridBagConstraints);

        solstudioPathTextField.setColumns(40);
        solstudioPathTextField.setMinimumSize(new java.awt.Dimension(64, 26));
        solstudioPathTextField.setPreferredSize(new java.awt.Dimension(64, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        add(solstudioPathTextField, gridBagConstraints);

        userNameLabel.setText("User name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        add(userNameLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        add(userNameTextField, gridBagConstraints);

        connectCommandLabel.setText("Connect Cmd:");
        connectCommandLabel.setMaximumSize(new java.awt.Dimension(152, 20));
        connectCommandLabel.setMinimumSize(new java.awt.Dimension(152, 20));
        connectCommandLabel.setPreferredSize(new java.awt.Dimension(152, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        add(connectCommandLabel, gridBagConstraints);
        connectCommandLabel.getAccessibleContext().setAccessibleDescription("");

        connectCommandTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectCommandTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        add(connectCommandTextField, gridBagConstraints);

        connectionStatusLabel.setText("Connection Status:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        add(connectionStatusLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 0);
        add(connectionStatusValueLabel, gridBagConstraints);

        messageTextArea.setEditable(false);
        messageTextArea.setLineWrap(true);
        messageTextArea.setRows(6);
        messageTextArea.setWrapStyleWord(true);
        messageTextScrollPane.setViewportView(messageTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        add(messageTextScrollPane, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(connectionProgressBar, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void connectCommandTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectCommandTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_connectCommandTextFieldActionPerformed

  /*
  private void authenticationManageButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_authenticationManageButtonActionPerformed
    AnDialog2 dialog =
        new AnDialog2(connectionDialog, this, AnLocale.getString("Manage Authentications"));
    AuthenticationPanel authenticationsPanel =
        new AuthenticationPanel(dialog, connectionDialog.getAuthenticationsCopy());
    dialog.setCustomPanel(authenticationsPanel);
    dialog.setHelpTag(AnVariable.HELP_ConnectAuthentication);
    dialog.setVisible(true);
    if (dialog.getStatus() == AnDialog2.Status.OK) { // Not Cancel
      List<Authentication> authentications = authenticationsPanel.getAuthentications();
      connectionDialog.setAuthentications(authentications);
    }
  } // GEN-LAST:event_authenticationManageButtonActionPerformed
  */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel connectCommandLabel;
    private javax.swing.JTextField connectCommandTextField;
    private javax.swing.JProgressBar connectionProgressBar;
    private javax.swing.JLabel connectionStatusLabel;
    private javax.swing.JLabel connectionStatusValueLabel;
    private javax.swing.JComboBox hostNameComboBox;
    private javax.swing.JLabel hostNameLabel;
    private javax.swing.JTextArea messageTextArea;
    private javax.swing.JScrollPane messageTextScrollPane;
    private javax.swing.JLabel solstudioPathLabel;
    private javax.swing.JTextField solstudioPathTextField;
    private javax.swing.JLabel userNameLabel;
    private javax.swing.JTextField userNameTextField;
    // End of variables declaration//GEN-END:variables
}
