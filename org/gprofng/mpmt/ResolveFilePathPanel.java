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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ResolveFilePathPanel extends javax.swing.JPanel {
  private final ResolveFilePathDialog resolveFilePathDialog;
  private final String notFoundFilePath;
  private final String notFoundFileName;
  private String resolvedAsPath;
  private String filePath;
  private static boolean archiveCheckBoxState = true;
  private static boolean archiveThisFileOnlyState = true;
  private static String feed = "";

  public ResolveFilePathPanel(
      ResolveFilePathDialog resolveFilePathDialog, String notFoundFilePath) {
    this.resolveFilePathDialog = resolveFilePathDialog;
    this.notFoundFilePath = notFoundFilePath;
    notFoundFileName = AnUtility.basename(notFoundFilePath);
    initComponents();
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    infoLabel.setText(AnLocale.getString("Locate the source file below and click OK."));
    AnUtility.setAccessibleContext(infoLabel.getAccessibleContext(), infoLabel.getText());
    infoLabel.setDisplayedMnemonic(AnLocale.getString('L', "ResolvePanelLocateLabelMnemonic"));
    infoLabel.setLabelFor(pathTextField);
    AnUtility.setAccessibleContext(
        pathTextField.getAccessibleContext(), AnLocale.getString("Path"));
    String unResolvedTT = AnLocale.getString("Can not find source file") + " " + notFoundFilePath;
    unResolvedLabel.setIcon(AnUtility.fileUnResolved);
    unResolvedLabel.setText("");
    unResolvedLabel.setToolTipText(unResolvedTT);
    AnUtility.setAccessibleContext(
        unResolvedLabel.getAccessibleContext(), AnLocale.getString("Unresolved Path"));
    unResolvedTextField.setText(notFoundFilePath + "  " + AnLocale.getString("(not found)"));
    unResolvedTextField.setToolTipText(unResolvedTT);
    AnUtility.setAccessibleContext(
        unResolvedTextField.getAccessibleContext(), AnLocale.getString("Unresolved Path"));
    AnUtility.setAccessibleContext(
        resolvedTextField.getAccessibleContext(), AnLocale.getString("Rresolved Path"));
    browseButton.setMnemonic(AnLocale.getString('B', "ResolvePanelBrowseButtonMN"));
    browseButton.setToolTipText(AnLocale.getString("Browse for file (ALT B)"));
    thisFileOnlyCheckBox.setText(AnLocale.getString("Archive this file only"));
    thisFileOnlyCheckBox.setMnemonic(AnLocale.getString('R', "ResolvePanelThisFileOnly heckboxMN"));
    thisFileOnlyCheckBox.setToolTipText(AnLocale.getString("Only this file will be resolved"));
    thisFileOnlyCheckBox.setOpaque(false);
    thisFileOnlyCheckBox.setSelected(archiveThisFileOnlyState);
    archiveCheckBox.setText(AnLocale.getString("Archive source file(s) inside experiment"));
    archiveCheckBox.setToolTipText(
        AnLocale.getString(
            "Use Archive feature to save a copy of the file(s) inside the experiment"));
    archiveCheckBox.setMnemonic(AnLocale.getString('A', "ResolvePanelArchiveCheckkboxMN"));
    archiveCheckBox.setOpaque(false);
    archiveCheckBox.setSelected(archiveCheckBoxState);
    errorLabel.setForeground(Color.red);
    if (feed != null) {
      pathTextField.setText(feed);
    }
    pathTextField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void changedUpdate(DocumentEvent e) {
                updateStates();
              }

              @Override
              public void insertUpdate(DocumentEvent e) {
                updateStates();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                updateStates();
              }
            });
    updateStatesInternal();
  }

  private void updateStates() {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            updateStatesInternal();
          }
        },
        "updateStates");
  }

  private void updateStatesInternal() {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            errorLabel.setText(" ");
            resolveFilePathDialog.getOKButton().setEnabled(false);
            resolvedLabel.setIcon(null);
            resolvedTextField.setText("");
            resolvedLabel.setToolTipText(null);
            resolvedTextField.setToolTipText(null);
            filePath = pathTextField.getText();
          }
        });

    if (filePath.equals("/net") || filePath.equals("/net/")) {
      return; // optimization
    }
    String fileName = AnUtility.basename(filePath);
    String error = null;
    resolvedAsPath = null;

    if (filePath.length() > 0 && !filePath.startsWith("/")) {
      error = AnLocale.getString("Use absolute path");
    }

    if (error == null) {
      if (notFoundFilePath.startsWith("/")) {
        // Native absolute path
        File filePathFile = new AnFile(filePath);
        if (filePathFile.exists()) {
          if (filePathFile.isDirectory()) {
            String tryThis = filePathFile.getAbsoluteFile() + "/" + notFoundFileName;
            if (new AnFile(tryThis).exists()) {
              resolvedAsPath = tryThis;
            }
          } else if (notFoundFileName.equals(fileName)) {
            resolvedAsPath = filePath;
          }
        } else {
          if (fileName.equals(notFoundFileName)) {
            error = AnLocale.getString("File doesn't exists");
          }
        }
      } else {
        // Java relative path
        File filePathFile = new AnFile(filePath);
        if (filePathFile.exists()) {
          if (filePathFile.isDirectory()) {
            String tryThis = filePathFile.getAbsoluteFile() + "/" + notFoundFilePath;
            if (new AnFile(tryThis).exists()) {
              resolvedAsPath = tryThis;
            }
          } else {
            // Compare file names
            String fName = AnUtility.basename(filePath);
            String nfName = AnUtility.basename(notFoundFilePath);
            if (nfName.equals(fName)) {
              resolvedAsPath = filePath;
            }
          }
        } else {
          if (fileName.equals(notFoundFileName)) {
            error = AnLocale.getString("File doesn't exists");
          }
        }
      }
    }

    final String errorMsg = error;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            if (errorMsg != null) {
              errorLabel.setText(errorMsg);
            } else if (resolvedAsPath != null) {
              resolveFilePathDialog.getOKButton().setEnabled(true);
              resolvedLabel.setIcon(AnUtility.fileResolved);
              resolvedTextField.setText(resolvedAsPath + "  " + AnLocale.getString("(resolved)"));
              String tt = AnLocale.getString("File resolved as ") + resolvedAsPath;
              resolvedLabel.setToolTipText(tt);
              resolvedTextField.setToolTipText(tt);
            }
          }
        });
  }

  private void updateCheckBoxStates() {
    thisFileOnlyCheckBox.setEnabled(archiveCheckBox.isSelected());
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    infoLabel = new javax.swing.JLabel();
    unResolvedLabel = new javax.swing.JLabel();
    unResolvedTextField = new javax.swing.JTextField();
    resolvedLabel = new javax.swing.JLabel();
    resolvedTextField = new javax.swing.JTextField();
    pathTextField = new javax.swing.JTextField();
    browseButton = new javax.swing.JButton();
    archiveCheckBox = new javax.swing.JCheckBox();
    thisFileOnlyCheckBox = new javax.swing.JCheckBox();
    errorLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
    add(infoLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(unResolvedLabel, gridBagConstraints);

    unResolvedTextField.setEditable(false);
    unResolvedTextField.setText("222");
    unResolvedTextField.setBorder(null);
    unResolvedTextField.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 0);
    add(unResolvedTextField, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    add(resolvedLabel, gridBagConstraints);

    resolvedTextField.setEditable(false);
    resolvedTextField.setText("111");
    resolvedTextField.setBorder(null);
    resolvedTextField.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(6, 4, 0, 0);
    add(resolvedTextField, gridBagConstraints);

    pathTextField.setColumns(50);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    add(pathTextField, gridBagConstraints);

    browseButton.setText("...");
    browseButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            browseButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    add(browseButton, gridBagConstraints);

    archiveCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            archiveCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(archiveCheckBox, gridBagConstraints);

    thisFileOnlyCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            thisFileOnlyCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(2, 17, 0, 0);
    add(thisFileOnlyCheckBox, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    add(errorLabel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void browseButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_browseButtonActionPerformed
    // TODO add your handling code here:
    if (!Analyzer.getInstance().isRemote()) {
      // Local
      feed = pathTextField.getText();
      File feedFile = new File(feed);
      if (!feedFile.exists()) {
        feed = Analyzer.getInstance().getWorkingDirectory();
        feedFile = new File(feed);
        if (!feedFile.exists()) {
          feedFile = null;
        }
      }

      JFileChooser fileChooser = new JFileChooser(); // Always local file chooser
      fileChooser.setDialogTitle(AnLocale.getString("Resolve Source File"));
      if (feedFile != null) {
        fileChooser.setCurrentDirectory(feedFile);
      }
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      int ret = fileChooser.showOpenDialog(this);
      if (ret == JFileChooser.APPROVE_OPTION) {
        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        pathTextField.setText(filePath);
        feed = pathTextField.getText();
      }
      updateStates();
    } else {
      // Remote
      feed = pathTextField.getText();
      AnFile feedFile = new AnFile(feed);
      if (!feedFile.exists()) {
        feed = Analyzer.getInstance().getWorkingDirectory();
        feedFile = new AnFile(feed);
        if (!feedFile.exists()) {
          feedFile = null;
        }
      }

      AnChooser fileChooser =
          AnWindow.getInstance()
              .getAnChooser(
                  AnLocale.getString("Resolve Source File"), AnChooser.DIR_FILE_CHOOSER, feed);
      //            JFileChooser fileChooser = new JFileChooser(); // Always local file chooser
      if (feedFile != null) {
        fileChooser.setCurrentDirectory(feedFile);
      }
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      int ret = fileChooser.showOpenDialog(this);
      if (ret == JFileChooser.APPROVE_OPTION) {
        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        pathTextField.setText(filePath);
        feed = pathTextField.getText();
      }
      updateStates();
    }
  } // GEN-LAST:event_browseButtonActionPerformed

  private void archiveCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_archiveCheckBoxActionPerformed
    archiveCheckBoxState = archiveCheckBox.isSelected();
    if (archiveCheckBox.isSelected()) {
      thisFileOnlyCheckBox.setSelected(archiveThisFileOnlyState);
    } else {
      thisFileOnlyCheckBox.setSelected(false);
    }
    updateCheckBoxStates();
  } // GEN-LAST:event_archiveCheckBoxActionPerformed

  private void thisFileOnlyCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_thisFileOnlyCheckBoxActionPerformed
    archiveThisFileOnlyState = thisFileOnlyCheckBox.isSelected();
    updateCheckBoxStates();
  } // GEN-LAST:event_thisFileOnlyCheckBoxActionPerformed

  protected String getResolvedPath() {
    return resolvedAsPath;
  }

  protected boolean resolveThisFileOnly() {
    return thisFileOnlyCheckBox.isSelected();
  }

  protected boolean archiveFiles() {
    return archiveCheckBox.isSelected();
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox archiveCheckBox;
  private javax.swing.JButton browseButton;
  private javax.swing.JLabel errorLabel;
  private javax.swing.JLabel infoLabel;
  private javax.swing.JTextField pathTextField;
  private javax.swing.JLabel resolvedLabel;
  private javax.swing.JTextField resolvedTextField;
  private javax.swing.JCheckBox thisFileOnlyCheckBox;
  private javax.swing.JLabel unResolvedLabel;
  private javax.swing.JTextField unResolvedTextField;
  // End of variables declaration//GEN-END:variables
}
