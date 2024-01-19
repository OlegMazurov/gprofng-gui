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


package org.gprofng.mpmt.settings;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnChooser;
import org.gprofng.mpmt.AnFile;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.persistence.UserPrefPersistence;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

/**
 * @author tpreisle
 */
public class ImportSettingsPanel extends javax.swing.JPanel {
  private static final Color errorColor = Color.red;
  private static final Color warningColor = new Color(255, 150, 0);
  private final ImportSettingsDialog dialog;
  private final DocListener docListener = new DocListener();
  private UserPref userPref;
  private List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

  /** Creates new form SaveSettingsPanel */
  public ImportSettingsPanel(ImportSettingsDialog dialog) {
    this.dialog = dialog;
    initComponents();
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    ActionListener checkBoxActionListener = new CheckBoxActionListener();

    AnUtility.setTextAndAccessibleContext(
        infoLabel, AnLocale.getString("Import settings from a saved configuration file."));
    AnUtility.setTextAndAccessibleContext(whatLabel, AnLocale.getString("Options to import:"));
    whatLabel.setDisplayedMnemonic(
        AnLocale.getString('O', "ExportSettingsOptionsToExportLabelMnemonics"));
    whatLabel.setLabelFor(viewsCheckBox);

    checkBoxes.add(metricsCheckBox);
    checkBoxes.add(viewsCheckBox);
    checkBoxes.add(formatsCheckBox);
    checkBoxes.add(callTreeCheckBox);
    checkBoxes.add(sourceDisassemblyCheckBox);
    checkBoxes.add(functionColorsCheckBox);
    checkBoxes.add(timelineCheckBox);
    checkBoxes.add(searchPathCheckBox);
    checkBoxes.add(pathmapCheckBox);
    checkBoxes.add(libraryClassVisibilityCheckBox);
    checkBoxes.add(miscCheckBox);

    metricsCheckBox.setText(AnLocale.getString("Metrics"));
    viewsCheckBox.setText(AnLocale.getString("Views"));
    formatsCheckBox.setText(AnLocale.getString("Formats"));
    callTreeCheckBox.setText(AnLocale.getString("Call Tree"));
    sourceDisassemblyCheckBox.setText(AnLocale.getString("Source/Disassembly"));
    functionColorsCheckBox.setText(AnLocale.getString("Function Colors"));
    timelineCheckBox.setText(AnLocale.getString("Timeline"));
    searchPathCheckBox.setText(AnLocale.getString("Search Path"));
    pathmapCheckBox.setText(AnLocale.getString("Pathmaps"));
    libraryClassVisibilityCheckBox.setText(AnLocale.getString("Library and Class Visibility"));
    miscCheckBox.setText(AnLocale.getString("Miscellaneous"));

    metricsCheckBox.setToolTipText(
        AnLocale.getString(
            "Selected metrics")); // getString("Selected metrics, order, and sort metric"));
    viewsCheckBox.setToolTipText(AnLocale.getString("Selected views and order"));
    formatsCheckBox.setToolTipText(AnLocale.getString("Formats settings"));
    callTreeCheckBox.setToolTipText(AnLocale.getString("Call Tree settings"));
    sourceDisassemblyCheckBox.setToolTipText(AnLocale.getString("Source/Disassembly settings"));
    functionColorsCheckBox.setToolTipText(AnLocale.getString("Function color settings"));
    timelineCheckBox.setToolTipText(AnLocale.getString("Timeline settings"));
    searchPathCheckBox.setToolTipText(AnLocale.getString("Search Path settings"));
    pathmapCheckBox.setToolTipText(AnLocale.getString("Pathmap settings"));
    libraryClassVisibilityCheckBox.setToolTipText(
        AnLocale.getString("Library and Class Visibility settings"));
    miscCheckBox.setToolTipText(AnLocale.getString("Find history, ..."));

    AnUtility.setTTAndAccessibleContext(allCheckBox, AnLocale.getString("Select all"));
    allCheckBox.addActionListener(new AllCheckBoxActionListener());
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.addActionListener(checkBoxActionListener);
    }

    AnUtility.setTextAndAccessibleContext(locationLabel, AnLocale.getString("Configuration File:"));
    locationLabel.setDisplayedMnemonic(
        AnLocale.getString('C', "ImportSettingsConfigurationFileMnemonic"));
    locationLabel.setLabelFor(pathTextField);
    browseButton.setToolTipText(AnLocale.getString("Browse for Location (ALT B)"));
    browseButton.setMnemonic(AnLocale.getString('B', "ExportBrowseButtonMnemonic"));

    if (UserPref.getInstance().getLastExportImportConfPath() != null) {
      pathTextField.setText(UserPref.getInstance().getLastExportImportConfPath());
    }

    pathTextField.getDocument().addDocumentListener(docListener);

    linePanel.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, AnEnvironment.ABOUT_BOX_BORDER_COLOR));

    updateStates();
  }

  public String getConfigurationPath() {
    return pathTextField.getText();
  }

  public List<UserPref.What> getWhat() {
    List<UserPref.What> what = new ArrayList<UserPref.What>();
    if (metricsCheckBox.isSelected()) {
      what.add(UserPref.What.METRICS);
    }
    if (formatsCheckBox.isSelected()) {
      what.add(UserPref.What.FORMATS);
    }
    if (viewsCheckBox.isSelected()) {
      what.add(UserPref.What.VIEWS);
    }
    if (callTreeCheckBox.isSelected()) {
      what.add(UserPref.What.CALLTREE);
    }
    if (sourceDisassemblyCheckBox.isSelected()) {
      what.add(UserPref.What.SOURCEDISASSEMBLY);
    }
    if (functionColorsCheckBox.isSelected()) {
      what.add(UserPref.What.FUNCTIONCOLORS);
    }
    if (miscCheckBox.isSelected()) {
      what.add(UserPref.What.MISC);
    }
    if (timelineCheckBox.isSelected()) {
      what.add(UserPref.What.TIMELINE);
    }
    if (libraryClassVisibilityCheckBox.isSelected()) {
      what.add(UserPref.What.LIBRARYVISIBILITY);
    }
    if (searchPathCheckBox.isSelected()) {
      what.add(UserPref.What.SEARCHPATH);
    }
    if (pathmapCheckBox.isSelected()) {
      what.add(UserPref.What.PATHMAP);
    }
    return what;
  }

  private class CheckBoxActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      String errorText = checkCkeckBoxesStates();
      handleErrorWarning(errorText, null);
    }
  }

  private class AllCheckBoxActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      boolean sel = allCheckBox.isSelected();
      for (JCheckBox checkBox : checkBoxes) {
        if (checkBox.isEnabled()) {
          checkBox.setSelected(sel);
        }
      }
      String errorText = checkCkeckBoxesStates();
      handleErrorWarning(errorText, null);
    }
  }

  class DocListener implements DocumentListener {
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
  }

  private void updateStates() {
    String errorText = null;
    String warningText = null;

    allCheckBox.setEnabled(false);
    allCheckBox.setSelected(false);

    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setEnabled(false);
      checkBox.setSelected(false);
    }

    if (pathTextField.getText().length() == 0) {
      errorText = AnLocale.getString("Specify a configuration file to import settings from");
    } else if (!pathTextField.getText().endsWith(UserPref.configurationSuffix)) {
      errorText = AnLocale.getString("File is not a configuration file");
    } else if (!new AnFile(pathTextField.getText()).exists()) {
      errorText = AnLocale.getString("Configuration file doesn't exixts");
    }

    if (errorText == null) {
      // Process config file and enable only available options.
      userPref = new UserPref();
      String configFilePath = getConfigurationPath();

      if (Analyzer.getInstance().isRemote()) {
        configFilePath = AnWindow.copyFromRemote(configFilePath);
      }

      new UserPrefPersistence().restoreSettings(configFilePath, userPref);
      if (userPref.getVersion() >= 10) {
        List<UserPref.What> whatList = userPref.getWhatList();
        if (whatList.contains(UserPref.What.METRICS)) {
          metricsCheckBox.setEnabled(true);
          metricsCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.VIEWS)) {
          viewsCheckBox.setEnabled(true);
          viewsCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.FORMATS)) {
          formatsCheckBox.setEnabled(true);
          formatsCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.CALLTREE)) {
          callTreeCheckBox.setEnabled(true);
          callTreeCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.SOURCEDISASSEMBLY)) {
          sourceDisassemblyCheckBox.setEnabled(true);
          sourceDisassemblyCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.TIMELINE)) {
          timelineCheckBox.setEnabled(true);
          timelineCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.SEARCHPATH)) {
          searchPathCheckBox.setEnabled(true);
          searchPathCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.PATHMAP)) {
          pathmapCheckBox.setEnabled(true);
          pathmapCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.MISC)) {
          miscCheckBox.setEnabled(true);
          miscCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.LIBRARYVISIBILITY)) {
          libraryClassVisibilityCheckBox.setEnabled(true);
          libraryClassVisibilityCheckBox.setSelected(true);
        }
        if (whatList.contains(UserPref.What.FUNCTIONCOLORS)) {
          functionColorsCheckBox.setEnabled(true);
          functionColorsCheckBox.setSelected(true);
        }
      } else {
        userPref = null;
        errorText =
            AnLocale.getString(
                "Configuration file is an older version. Import from older version configuration"
                    + " files is not supported.");
      }
    }
    if (errorText == null) {
      errorText = checkCkeckBoxesStates();
    }
    handleErrorWarning(errorText, warningText);
  }

  private String checkCkeckBoxesStates() {
    String errorText = null;
    if (errorText == null) {
      if (!anyCheckBoxesSelected()) {
        errorText = AnLocale.getString("At least one option needs to be selected");
      }
    }

    if (errorText == null) {
      boolean allCheckBoxSelectedState = true;
      for (JCheckBox checkBox : checkBoxes) {
        if (checkBox.isEnabled() && !checkBox.isSelected()) {
          allCheckBoxSelectedState = false;
          break;
        }
      }
      allCheckBox.setEnabled(true);
      allCheckBox.setSelected(allCheckBoxSelectedState);
    } else {
      allCheckBox.setSelected(false);
    }
    return errorText;
  }

  private void handleErrorWarning(String errorText, String warningText) {
    if (errorText == null) {
      boolean allCheckBoxSelectedState = true;
      for (JCheckBox checkBox : checkBoxes) {
        if (checkBox.isEnabled() && !checkBox.isSelected()) {
          allCheckBoxSelectedState = false;
          break;
        }
      }
      allCheckBox.setEnabled(true);
      allCheckBox.setSelected(allCheckBoxSelectedState);
    } else {
      allCheckBox.setSelected(false);
    }

    if (errorText != null) {
      dialog.getOKButton().setEnabled(false);
      setError(errorText);
    } else if (warningText != null) {
      dialog.getOKButton().setEnabled(true);
      setWarning(warningText);
    } else {
      dialog.getOKButton().setEnabled(true);
      resetErrorWarning();
    }
  }

  private boolean anyCheckBoxesSelected() {
    boolean ret = false;
    for (JCheckBox checkBox : checkBoxes) {
      if (checkBox.isSelected()) {
        ret = true;
        break;
      }
    }
    return ret;
  }

  private void setError(String text) {
    errorLabel.setForeground(errorColor);
    errorLabel.setText(AnLocale.getString("Error: ") + text);
  }

  private void setWarning(String text) {
    errorLabel.setForeground(warningColor);
    errorLabel.setText(AnLocale.getString("Warning: ") + text);
  }

  private void resetErrorWarning() {
    errorLabel.setText(" ");
  }

  public UserPref getUserPref() {
    return userPref;
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
    whatLabel = new javax.swing.JLabel();
    allCheckBox = new javax.swing.JCheckBox();
    linePanel = new javax.swing.JPanel();
    viewsCheckBox = new javax.swing.JCheckBox();
    metricsCheckBox = new javax.swing.JCheckBox();
    timelineCheckBox = new javax.swing.JCheckBox();
    sourceDisassemblyCheckBox = new javax.swing.JCheckBox();
    callTreeCheckBox = new javax.swing.JCheckBox();
    formatsCheckBox = new javax.swing.JCheckBox();
    searchPathCheckBox = new javax.swing.JCheckBox();
    pathmapCheckBox = new javax.swing.JCheckBox();
    functionColorsCheckBox = new javax.swing.JCheckBox();
    libraryClassVisibilityCheckBox = new javax.swing.JCheckBox();
    miscCheckBox = new javax.swing.JCheckBox();
    pathTextField = new javax.swing.JTextField();
    browseButton = new javax.swing.JButton();
    locationLabel = new javax.swing.JLabel();
    errorLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    infoLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    add(infoLabel, gridBagConstraints);

    whatLabel.setText("Options...:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(whatLabel, gridBagConstraints);

    allCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(allCheckBox, gridBagConstraints);

    linePanel.setMaximumSize(new java.awt.Dimension(20, 20));
    linePanel.setMinimumSize(new java.awt.Dimension(10, 30));
    linePanel.setOpaque(false);
    linePanel.setPreferredSize(new java.awt.Dimension(200, 2));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
    add(linePanel, gridBagConstraints);

    viewsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(viewsCheckBox, gridBagConstraints);

    metricsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(metricsCheckBox, gridBagConstraints);

    timelineCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(timelineCheckBox, gridBagConstraints);

    sourceDisassemblyCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(sourceDisassemblyCheckBox, gridBagConstraints);

    callTreeCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(callTreeCheckBox, gridBagConstraints);

    formatsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(formatsCheckBox, gridBagConstraints);

    searchPathCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(searchPathCheckBox, gridBagConstraints);

    pathmapCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(pathmapCheckBox, gridBagConstraints);

    functionColorsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 17;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(functionColorsCheckBox, gridBagConstraints);

    libraryClassVisibilityCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 18;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(libraryClassVisibilityCheckBox, gridBagConstraints);

    miscCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 19;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(miscCheckBox, gridBagConstraints);

    pathTextField.setColumns(70);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    add(pathTextField, gridBagConstraints);

    browseButton.setText("...");
    browseButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            browseButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(browseButton, gridBagConstraints);

    locationLabel.setText("Location:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(locationLabel, gridBagConstraints);

    errorLabel.setForeground(new java.awt.Color(255, 0, 51));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 22;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
    add(errorLabel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void browseButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_browseButtonActionPerformed
    String feed = pathTextField.getText();
    File feedFile = null;
    if (feed.length() > 0) {
      feedFile = new File(feed).getParentFile();
      if (feedFile != null && (!feedFile.exists() || !feedFile.isDirectory())) {
        feedFile = null;
      }
    }

    JFileChooser fileChooser =
        AnWindow.getInstance().getAnChooser("", AnChooser.DIR_FILE_CHOOSER, null);
    if (feedFile != null) {
      fileChooser.setCurrentDirectory(feedFile);
    }
    fileChooser.setFileFilter(new ConfFileFileFilter());
    int ret = fileChooser.showOpenDialog(this);
    if (ret == JFileChooser.APPROVE_OPTION) {
      String filePath = fileChooser.getSelectedFile().getAbsolutePath();
      pathTextField.setText(filePath);
    }
    updateStates();
  } // GEN-LAST:event_browseButtonActionPerformed

  private class ConfFileFileFilter extends FileFilter {

    @Override
    public String getDescription() {
      return UserPref.configurationSuffix;
    }

    @Override
    public boolean accept(File f) {
      if (f.isDirectory()) {
        return true;
      } else if (f.getName().endsWith(UserPref.configurationSuffix)) {
        return true;
      } else {
        return false;
      }
    }
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox allCheckBox;
  private javax.swing.JButton browseButton;
  private javax.swing.JCheckBox callTreeCheckBox;
  private javax.swing.JLabel errorLabel;
  private javax.swing.JCheckBox formatsCheckBox;
  private javax.swing.JCheckBox functionColorsCheckBox;
  private javax.swing.JLabel infoLabel;
  private javax.swing.JCheckBox libraryClassVisibilityCheckBox;
  private javax.swing.JPanel linePanel;
  private javax.swing.JLabel locationLabel;
  private javax.swing.JCheckBox metricsCheckBox;
  private javax.swing.JCheckBox miscCheckBox;
  private javax.swing.JTextField pathTextField;
  private javax.swing.JCheckBox pathmapCheckBox;
  private javax.swing.JCheckBox searchPathCheckBox;
  private javax.swing.JCheckBox sourceDisassemblyCheckBox;
  private javax.swing.JCheckBox timelineCheckBox;
  private javax.swing.JCheckBox viewsCheckBox;
  private javax.swing.JLabel whatLabel;
  // End of variables declaration//GEN-END:variables
}
