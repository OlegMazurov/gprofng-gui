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


package org.gprofng.mpmt.export;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.export.ExportSupport.ExportFormat;
import org.gprofng.mpmt.mainview.Subview;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author tpreisle
 */
public class ExportPanel extends javax.swing.JPanel {
  private static final Color errorColor = Color.red;
  private static final Color warningColor = new Color(255, 150, 0);
  private ExportDialog exportDialog;
  private JFileChooser fileChooser;
  private ExportSupport exportSupport;

  private static Integer limitSaved = null;
  private static ExportFormat formatSaved = null;
  private static File directorySaved = null;
  private static String fileNameSaved = "output";
  private static Character delimiterSaved = ',';
  private static Boolean includeSubviews = false;
  /** Creates new form ExportPanel */
  public ExportPanel(ExportDialog exportDialog, ExportSupport exportSupport) {
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    this.exportDialog = exportDialog;
    this.exportSupport = exportSupport;
    List<ExportSupport.ExportFormat> formats = exportSupport.getSupportedExportFormats();
    initComponents();
    infoScrollPane.setBorder(null);
    //        infoTextArea.setOpaque(false);
    infoTextArea.setEditable(false);
    infoTextArea.setRows(1);
    infoTextArea.setWrapStyleWord(true);
    infoTextArea.setLineWrap(true);
    infoTextArea.setText(
        AnLocale.getString(
            "Save the data shown in the current view to a file of the specified format."));
    infoTextArea.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    AnUtility.setAccessibleContext(
        infoTextArea.getAccessibleContext(), AnLocale.getString("Info area"));
    //        fileBrowseButton.setIcon(AnUtility.brws_icon);
    //        fileBrowseButton.setToolTipText(AnLocale.getString("Browse for file"));
    //        ToolBarButton.setBehaviour(fileBrowseButton);
    //        errorLabel.setFont(exportDialog.getDialogFont());
    //        fileBrowseButton.setFont(exportDialog.getDialogFont());
    //        dirLabel.setFont(exportDialog.getDialogFont());
    //        dirTextFeild.setFont(exportDialog.getDialogFont());
    //        formatComboBox.setFont(exportDialog.getDialogFont());
    //        delimiterLabel.setFont(exportDialog.getDialogFont());
    //        delimiterTextField.setFont(exportDialog.getDialogFont());
    //        formatLabel.setFont(exportDialog.getDialogFont());
    //        limitLabel.setFont(exportDialog.getDialogFont());
    //        limitTextField.setFont(exportDialog.getDialogFont());
    //        pathLabelExt.setFont(exportDialog.getDialogFont());

    pathLabelExt.setForeground(new Color(150, 150, 150));

    nameLabel.setText(AnLocale.getString("File Name:"));
    String outputFileTT = AnLocale.getString("Name of output file");
    nameLabel.setToolTipText(outputFileTT);
    nameTextField.setToolTipText(outputFileTT);
    nameLabel.setDisplayedMnemonic(AnLocale.getString('F', "ExportFileNameLabelMnemonic"));
    nameLabel.setLabelFor(nameTextField);
    nameTextField.setToolTipText(outputFileTT);

    dirLabel.setText(AnLocale.getString("Directory:"));
    dirLabel.setDisplayedMnemonic(AnLocale.getString('D', "ExportDirectoryLabelMemonic"));
    dirLabel.setLabelFor(dirTextField);
    String dirTT = AnLocale.getString("Directory where to store output file");
    dirLabel.setToolTipText(dirTT);
    dirTextField.setToolTipText(dirTT);

    dirBrowseButton.setToolTipText(AnLocale.getString("Browse for Directory (ALT B)"));
    dirBrowseButton.setMnemonic(AnLocale.getString('B', "ExportBrowseButtonMnemonic"));

    formatLabel.setText(AnLocale.getString("Export as:"));
    formatLabel.setDisplayedMnemonic(AnLocale.getString('X', "ExportFormatLabelMnemonic"));
    formatLabel.setLabelFor(formatComboBox);
    String formatTT = AnLocale.getString("Format of the exported data or image");
    formatLabel.setToolTipText(formatTT);
    formatComboBox.setToolTipText(formatTT);

    subviewsCheckBox.setText(AnLocale.getString("Include subviews"));
    subviewsCheckBox.setMnemonic(AnLocale.getString('I', "ExportIncludeSubviewsMnemonic"));
    subviewsCheckBox.setSelected(includeSubviews);

    limitLabel.setText(AnLocale.getString("Maximum:"));
    String limitTT =
        AnLocale.getString("Number of lines to export beginning from the top of the data display");
    limitLabel.setToolTipText(limitTT);
    limitTextField.setToolTipText(limitTT);

    formatComboBox.removeAllItems();
    for (ExportFormat format : formats) {
      formatComboBox.addItem(format);
    }

    delimiterLabel.setText(AnLocale.getString("Delimiter:"));
    String delimiterTT = AnLocale.getString("Character used to separate values");
    delimiterLabel.setToolTipText(delimiterTT);
    delimiterTextField.setToolTipText(delimiterTT);
    delimiterTextField.setHorizontalAlignment(JTextField.CENTER);
    if (delimiterSaved != null) {
      delimiterTextField.setText("" + delimiterSaved);
    } else {
      delimiterTextField.setText("" + ',');
    }

    // Restore saved states
    if (limitSaved == null) {
      limitTextField.setText("");
    } else {
      limitTextField.setText("" + limitSaved.intValue());
    }
    if (formatSaved != null) {
      for (int i = 0; i < formatComboBox.getItemCount(); i++) {
        if (formatComboBox.getItemAt(i) == formatSaved) {
          formatComboBox.setSelectedIndex(i);
          break;
        }
      }
    }

    if (directorySaved == null) {
      directorySaved = new File(Analyzer.getInstance().getWorkingDirectory());
    }
    dirTextField.setText(directorySaved.getAbsolutePath());

    String defaultName = fileNameSaved;
    nameTextField.setText(defaultName);
    if (getFilePathWithExtension().exists()) {
      for (int i = 1; i < 999; i++) {
        nameTextField.setText(defaultName + "_" + i);
        if (!getFilePathWithExtension().exists()) {
          break;
        }
      }
    }

    validateInput();

    nameTextField.getDocument().addDocumentListener(new DocListener());
    dirTextField.getDocument().addDocumentListener(new DocListener());
    delimiterTextField.getDocument().addDocumentListener(new DocListener());
    limitTextField.getDocument().addDocumentListener(new DocListener());

    dirBrowseButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            browse();
          }
        });

    formatComboBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            validateInput();
          }
        });
  }

  class DocListener implements DocumentListener {
    @Override
    public void changedUpdate(DocumentEvent e) {
      validateInput();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      validateInput();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      validateInput();
    }
  }

  private void browse() {
    fileChooser = new JFileChooser();
    File fp = getFilePathWithExtension();
    fileChooser.setCurrentDirectory(fp.getParentFile());
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    int ret = fileChooser.showOpenDialog(this);
    if (ret == JFileChooser.APPROVE_OPTION) {
      File selFile = fileChooser.getSelectedFile();
      if (selFile.isDirectory()) {
        dirTextField.setText(selFile.getAbsolutePath());
      } else {
        nameTextField.setText(selFile.getName());
        dirTextField.setText(selFile.getParentFile().getAbsolutePath());
      }
    }
    validateInput();
  }

  private File getFilePathWithExtension() {
    String fileName = nameTextField.getText();
    String dirName = dirTextField.getText();
    String filePath = dirName + "/" + fileName;
    String ext = null;
    String newExt = null;
    int i = fileName.lastIndexOf('.');
    if (i >= 0 && i < (fileName.length() - 1)) {
      ext = fileName.substring(i + 1);
    }
    if (ext == null || ext.length() == 0) {
      ExportFormat format = (ExportFormat) formatComboBox.getSelectedItem();
      if (format == ExportFormat.CSV) {
        newExt = "csv";
      } else if (format == ExportFormat.HTML) {
        newExt = "html";
      } else if (format == ExportFormat.JPG) {
        newExt = "jpg";
      } else if (format == ExportFormat.TEXT) {
        newExt = "txt";
      } else {
        assert true;
      }
    }
    if (ext != null && ext.length() > 0) {
      //
    } else if (filePath.endsWith(".")) {
      filePath = filePath + newExt;
    } else {
      filePath = filePath + "." + newExt;
    }
    return new File(filePath);
  }

  public Character getDelimiter() {
    Character delim = ',';
    String delimText = delimiterTextField.getText();
    if (delimText.length() == 1) {
      delim = delimText.charAt(0);
    }
    if (delimText.length() >= 2) {
      delim = null;
    }
    return delim;
  }

  private Integer getLineLimit() throws NumberFormatException {
    String limit = limitTextField.getText();
    if (limit.length() == 0) {
      return null;
    }
    return new Integer(limit).intValue();
  }

  private void validateInput() {
    ExportFormat format = (ExportFormat) formatComboBox.getSelectedItem();
    List<Subview> subviews =
        AnWindow.getInstance().getViews().getCurrentViewDisplay().getVisibleSubviews();
    if (format == ExportFormat.JPG && subviews.size() > 0) {
      subviewsCheckBox.setVisible(true);
    } else {
      subviewsCheckBox.setVisible(false);
    }
    if (format == ExportFormat.JPG || !exportSupport.exportLimitSupported()) {
      if (limitLabel.isVisible()) {
        limitLabel.setVisible(false);
        limitTextField.setVisible(false);
      }
    } else {
      if (!limitLabel.isVisible()) {
        limitLabel.setVisible(true);
        limitTextField.setVisible(true);
      }
    }
    if (format == ExportFormat.CSV) {
      if (!delimiterLabel.isVisible()) {
        delimiterLabel.setVisible(true);
        delimiterTextField.setVisible(true);
      }
    } else {
      if (delimiterLabel.isVisible()) {
        delimiterLabel.setVisible(false);
        delimiterTextField.setVisible(false);
      }
    }
    AnUtility.setTextAndAccessibleContext(pathLabelExt, AnLocale.getString("Path used: "));

    errorLabel.setText(" ");
    exportDialog.getOKButton().setEnabled(true);

    String outputFileName = nameTextField.getText();
    if (outputFileName.length() == 0) {
      updateError(AnLocale.getString("File name missing"));
      return;
    }
    File fullPathFile = getFilePathWithExtension();
    if (fullPathFile.exists() && fullPathFile.isDirectory()) {
      updateError(AnLocale.getString("File name missing"));
      return;
    }
    File outputFileParent = fullPathFile.getParentFile();
    if (outputFileParent != null && !outputFileParent.exists()) {
      updateError(AnLocale.getString("Folder doesn't exist"));
      return;
    }
    pathLabelExt.setText(AnLocale.getString("Path used: ") + fullPathFile.getAbsolutePath());

    if (fullPathFile.exists()) {
      updateWarning(AnLocale.getString("File name already exists"));
    }

    if (format == ExportFormat.CSV) {
      Character delim = getDelimiter();
      if (delim == null) {
        updateError("Invalid delimiter character");
        return;
      }
    }

    exportDialog.pack();

    Integer limit = null;
    if (limitTextField.isVisible()) {
      try {
        limit = getLineLimit();
      } catch (NumberFormatException nfe) {
        updateError(AnLocale.getString("Max Lines is not a valid number"));
        return;
      }
    }
    if (limit != null && limit <= 0) {
      updateError(AnLocale.getString("Max Lines should be empty or greater than 0"));
      return;
    }
  }

  private void updateWarning(String warningText) {
    errorLabel.setForeground(warningColor);
    errorLabel.setText(AnLocale.getString("Warning: ") + warningText);
  }

  private void updateError(String errorText) {
    errorLabel.setForeground(errorColor);
    errorLabel.setText(AnLocale.getString("Error: ") + errorText);
    exportDialog.getOKButton().setEnabled(false);
  }

  public String getOutputFilePath() {
    return getFilePathWithExtension().getAbsolutePath();
  }

  public ExportSupport.ExportFormat getExportFormat() {
    return (ExportFormat) formatComboBox.getSelectedItem();
  }

  public Integer getLimit() {
    return getLineLimit();
  }

  public Boolean getIncludeSubviews() {
    return subviewsCheckBox.isSelected();
  }

  protected void saveStates() {
    ExportFormat format = (ExportFormat) formatComboBox.getSelectedItem();

    formatSaved = getExportFormat();
    if (format != ExportFormat.JPG) {
      limitSaved = getLimit();
    }
    if (format == ExportFormat.JPG) {
      includeSubviews = getIncludeSubviews();
    }
    File outputFile = getFilePathWithExtension();
    directorySaved = outputFile.getParentFile();
    fileNameSaved = nameTextField.getText();
    fileNameSaved = fileNameSaved.replaceAll("\\..*$", "");
    fileNameSaved = fileNameSaved.replaceAll("_\\d*$", "");
    if (format == ExportFormat.CSV) {
      delimiterSaved = getDelimiter();
    }
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    infoScrollPane = new javax.swing.JScrollPane();
    infoTextArea = new javax.swing.JTextArea();
    nameLabel = new javax.swing.JLabel();
    nameTextField = new javax.swing.JTextField();
    dirLabel = new javax.swing.JLabel();
    dirTextField = new javax.swing.JTextField();
    dirBrowseButton = new javax.swing.JButton();
    formatLabel = new javax.swing.JLabel();
    formatComboBox = new javax.swing.JComboBox();
    delimiterLabel = new javax.swing.JLabel();
    delimiterTextField = new javax.swing.JTextField();
    subviewsCheckBox = new javax.swing.JCheckBox();
    limitLabel = new javax.swing.JLabel();
    limitTextField = new javax.swing.JTextField();
    errorLabel = new javax.swing.JLabel();
    pathLabelExt = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    infoTextArea.setColumns(20);
    infoTextArea.setRows(5);
    infoScrollPane.setViewportView(infoTextArea);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    add(infoScrollPane, gridBagConstraints);

    nameLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(nameLabel, gridBagConstraints);

    nameTextField.setColumns(20);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 4, 0, 0);
    add(nameTextField, gridBagConstraints);

    dirLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(dirLabel, gridBagConstraints);

    dirTextField.setColumns(60);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 4, 0, 0);
    add(dirTextField, gridBagConstraints);

    dirBrowseButton.setText("...");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 4, 0, 0);
    add(dirBrowseButton, gridBagConstraints);

    formatLabel.setText("jLabel2");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(formatLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 4, 0, 0);
    add(formatComboBox, gridBagConstraints);

    delimiterLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.insets = new java.awt.Insets(10, 8, 0, 0);
    add(delimiterLabel, gridBagConstraints);

    delimiterTextField.setColumns(2);
    delimiterTextField.setText(",");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 4, 0, 0);
    add(delimiterTextField, gridBagConstraints);

    subviewsCheckBox.setText("jCheckBox1");
    subviewsCheckBox.setOpaque(false);
    subviewsCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            subviewsCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.insets = new java.awt.Insets(10, 8, 0, 0);
    add(subviewsCheckBox, gridBagConstraints);

    limitLabel.setText("jLabel3");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(limitLabel, gridBagConstraints);

    limitTextField.setColumns(10);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 4, 0, 0);
    add(limitTextField, gridBagConstraints);

    errorLabel.setForeground(new java.awt.Color(255, 0, 51));
    errorLabel.setText("error");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(errorLabel, gridBagConstraints);

    pathLabelExt.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(pathLabelExt, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void subviewsCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_subviewsCheckBoxActionPerformed
    // TODO add your handling code here:
  } // GEN-LAST:event_subviewsCheckBoxActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel delimiterLabel;
  private javax.swing.JTextField delimiterTextField;
  private javax.swing.JButton dirBrowseButton;
  private javax.swing.JLabel dirLabel;
  private javax.swing.JTextField dirTextField;
  private javax.swing.JLabel errorLabel;
  private javax.swing.JComboBox formatComboBox;
  private javax.swing.JLabel formatLabel;
  private javax.swing.JScrollPane infoScrollPane;
  private javax.swing.JTextArea infoTextArea;
  private javax.swing.JLabel limitLabel;
  private javax.swing.JTextField limitTextField;
  private javax.swing.JLabel nameLabel;
  private javax.swing.JTextField nameTextField;
  private javax.swing.JLabel pathLabelExt;
  private javax.swing.JCheckBox subviewsCheckBox;
  // End of variables declaration//GEN-END:variables
}
