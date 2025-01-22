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


package org.gprofng.mpmt.settings;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnChooser;
import org.gprofng.mpmt.AnFile;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author tpreisle
 */
public class ExportSettingsPanel extends javax.swing.JPanel {
  private static final Color errorColor = Color.red;
  private static final Color warningColor = new Color(255, 150, 0);
  ExportSettingsDialog dialog = null;
  DocListener docListener = new DocListener();
  private List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

  /** Creates new form SaveSettingsPanel */
  public ExportSettingsPanel(ExportSettingsDialog dialog) {
    this.dialog = dialog;
    initComponents();
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    ActionListener checkBoxActionListener = new CheckBoxActionListener();

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

    AnUtility.setTTAndAccessibleContext(allCheckBox, AnLocale.getString("Select all"));
    AnUtility.setTextAndAccessibleContext(
        infoLabel,
        AnLocale.getString(
            "Export settings to a configuration file. Select which options to save and where to"
                + " save them. Saved configuration files can be imported and also selected when"
                + " opening an experiment."));
    AnUtility.setTextAndAccessibleContext(whatLabel, AnLocale.getString("Options to export:"));
    whatLabel.setDisplayedMnemonic(
        AnLocale.getString('O', "ExportSettingsOptionsToExportLabelMnemonics"));
    whatLabel.setLabelFor(viewsCheckBox);
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

    allCheckBox.addActionListener(new AllCheckBoxActionListener());
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.addActionListener(checkBoxActionListener);
    }

    metricsCheckBox.setSelected(true);
    viewsCheckBox.setSelected(true);
    formatsCheckBox.setSelected(true);
    callTreeCheckBox.setSelected(true);
    sourceDisassemblyCheckBox.setSelected(true);
    functionColorsCheckBox.setSelected(true);
    timelineCheckBox.setSelected(true);
    searchPathCheckBox.setSelected(true);
    pathmapCheckBox.setSelected(true);
    libraryClassVisibilityCheckBox.setSelected(true);
    miscCheckBox.setSelected(true);

    fullPathTextField.setEditable(false);

    AnUtility.setTextAndAccessibleContext(
        locationOptionsLabel, AnLocale.getString("Where to save the configuration file:"));
    locationOptionsLabel.setDisplayedMnemonic(
        AnLocale.getString('W', "ExportSettingsWhereToLabelMnemonics"));
    locationOptionsLabel.setLabelFor(experimentFolderRadioButton);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(experimentFolderRadioButton);
    buttonGroup.add(experimetParentRadioButton);
    buttonGroup.add(homeRadioButton);
    buttonGroup.add(otherRadioButton);
    experimentFolderRadioButton.setText(AnLocale.getString("Experiment"));
    experimentFolderRadioButton.setToolTipText(
        AnLocale.getString("Save settings inside the experiment (private to this experiment)"));
    experimetParentRadioButton.setText(AnLocale.getString("Experiment's parent folder"));
    experimetParentRadioButton.setToolTipText(
        AnLocale.getString(
            "Save settings in the parent folder of the experiment (shared between experiments in"
                + " this folder)"));
    homeRadioButton.setText(AnLocale.getString("User Directory"));
    homeRadioButton.setToolTipText(
        AnLocale.getString(
            "Save settings in the user directory "
                + UserPref.getAnalyzerDirPath()
                + "  (shared between all experiments)"));
    otherRadioButton.setText(AnLocale.getString("Other"));
    otherRadioButton.setToolTipText(AnLocale.getString("Save settings in another location..."));
    RadioButtonActionListener radioButtonActionListener = new RadioButtonActionListener();
    experimentFolderRadioButton.addActionListener(radioButtonActionListener);
    experimetParentRadioButton.addActionListener(radioButtonActionListener);
    homeRadioButton.addActionListener(radioButtonActionListener);
    otherRadioButton.addActionListener(radioButtonActionListener);
    //        AnUtility.setTextAndAccessibleContext(locationLabel, AnLocale.getString("Location:"));
    locationLabel.setDisplayedMnemonic(
        AnLocale.getString('L', "ExportSettingsLocationLabelMnemonic"));
    locationLabel.setLabelFor(pathTextField);
    //        AnUtility.setTTAndAccessibleContext(pathTextField, locationLabel.getText());
    browseButton.setToolTipText(AnLocale.getString("Browse for Location (ALT B)"));
    browseButton.setMnemonic(AnLocale.getString('B', "ExportBrowseButtonMnemonic"));
    experimentFolderRadioButton.setSelected(true);
    //        radioButtonActionListener.actionPerformed(null);
    //        pathTextField.getDocument().addDocumentListener(docListener);

    AnUtility.setTextAndAccessibleContext(
        configurationNameLabel, AnLocale.getString("Configuration Name:"));
    configurationNameLabel.setDisplayedMnemonic(
        AnLocale.getString('g', "ExportSettingsConfigurationNameLabelMnemonic"));
    configurationNameLabel.setLabelFor(configurationNameTextField);
    AnUtility.setTTAndAccessibleContext(
        configurationNameTextField, configurationNameLabel.getText());
    AnUtility.setTextAndAccessibleContext(
        defaultNameCheckBox, AnLocale.getString("Save as Default configuration"));
    defaultNameCheckBox.setMnemonic(AnLocale.getString('S', "ExportSettingsSaveCheckboxMN"));
    defaultNameCheckBox.setSelected(false);
    defaultNameCheckBoxActionPerformed(null);
    configurationNameTextField.getDocument().addDocumentListener(docListener);

    //        AnUtility.setTextAndAccessibleContext(fullPathLabel1, AnLocale.getString("Full
    // Path:"));
    //        AnUtility.setTTAndAccessibleContext(fullPathTextField, fullPathLabel1.getText());

    linePanel.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, AnEnvironment.ABOUT_BOX_BORDER_COLOR));

    updateRadioButtons();
    updateStates();
  }

  public String getConfigurationPath() {
    return fullPathTextField.getText();
  }

  public List<UserPref.What> getWhat() {
    List<UserPref.What> what = new ArrayList<UserPref.What>();
    if (viewsCheckBox.isSelected()) {
      what.add(UserPref.What.VIEWS);
    }
    if (metricsCheckBox.isSelected()) {
      what.add(UserPref.What.METRICS);
    }
    if (timelineCheckBox.isSelected()) {
      what.add(UserPref.What.TIMELINE);
    }
    if (sourceDisassemblyCheckBox.isSelected()) {
      what.add(UserPref.What.SOURCEDISASSEMBLY);
    }
    if (callTreeCheckBox.isSelected()) {
      what.add(UserPref.What.CALLTREE);
    }
    if (formatsCheckBox.isSelected()) {
      what.add(UserPref.What.FORMATS);
    }
    if (searchPathCheckBox.isSelected()) {
      what.add(UserPref.What.SEARCHPATH);
    }
    if (pathmapCheckBox.isSelected()) {
      what.add(UserPref.What.PATHMAP);
    }
    if (functionColorsCheckBox.isSelected()) {
      what.add(UserPref.What.FUNCTIONCOLORS);
    }
    if (libraryClassVisibilityCheckBox.isSelected()) {
      what.add(UserPref.What.LIBRARYVISIBILITY);
    }
    if (miscCheckBox.isSelected()) {
      what.add(UserPref.What.MISC);
    }
    return what;
  }

  private class RadioButtonActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateRadioButtons();
      updateStates();
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

  private class CheckBoxActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateStates();
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

  private void updateRadioButtons() {
    String FS = "";
    String remoteFS = "";
    String localFS = "";
    if (Analyzer.getInstance().isRemote()) {
      remoteFS = " (" + AnLocale.getString("remote") + ")";
      localFS = " (" + AnLocale.getString("local") + ")";
    }

    if (experimentFolderRadioButton.isSelected()) {
      String experiment = AnUtility.toFullPath(AnWindow.getInstance().getExperimentGroups()[0][0]);
      pathTextField.setText(UserPref.getConfigurationDirPath(experiment));
      FS = remoteFS;
    } else if (experimetParentRadioButton.isSelected()) {
      String experiment = AnUtility.toFullPath(AnWindow.getInstance().getExperimentGroups()[0][0]);
      String parentDir = AnUtility.dirname(experiment);
      pathTextField.setText(parentDir);
      FS = remoteFS;
    } else if (homeRadioButton.isSelected()) {
      pathTextField.setText(UserPref.getHomeConfigurationDirPath());
      FS = localFS;
    } else if (otherRadioButton.isSelected()) {
      String last = UserPref.getInstance().getLastExportImportConfPath();
      if (last != null) {
        pathTextField.setText(AnUtility.dirname(last));
      }
      FS = remoteFS;
    }
    AnUtility.setTextAndAccessibleContext(locationLabel, AnLocale.getString("Location") + FS + ":");
    AnUtility.setTTAndAccessibleContext(pathTextField, locationLabel.getText());
    AnUtility.setTextAndAccessibleContext(
        fullPathLabel1, AnLocale.getString("Full Path") + FS + ":");
    AnUtility.setTTAndAccessibleContext(fullPathTextField, fullPathLabel1.getText());
  }

  private void updateStates() {
    pathTextField.getDocument().removeDocumentListener(docListener);
    pathTextField.setEnabled(otherRadioButton.isSelected());
    browseButton.setEnabled(otherRadioButton.isSelected());
    configurationNameTextField.setEnabled(!defaultNameCheckBox.isSelected());

    if (pathTextField.isEnabled()) {
      pathTextField.getDocument().addDocumentListener(docListener);
    }

    fullPathTextField.setText(
        pathTextField.getText()
            + "/"
            + configurationNameTextField.getText()
            + UserPref.configurationSuffix);

    String errorText = null;
    String warningText = null;
    if (configurationNameTextField.getText().length() == 0) {
      errorText = AnLocale.getString("A configuration must have a name");
    } else {
      errorText = checkCkeckBoxesStates();
      if (errorText == null) {
        String confPath = fullPathTextField.getText();
        boolean homeDefaultConfiguration = confPath.startsWith(UserPref.getAnalyzerDirPath());
        File confFile = homeDefaultConfiguration ? new File(confPath) : new AnFile(confPath);
        if (confFile.exists()) {
          warningText = AnLocale.getString("Configuration already exists");
        }
      }
    }

    allCheckBox.setSelected(false);
    if (errorText == null) {
      boolean allCheckBoxSelectedState = true;
      for (JCheckBox checkBox : checkBoxes) {
        if (checkBox.isEnabled() && !checkBox.isSelected()) {
          allCheckBoxSelectedState = false;
          break;
        }
      }
      allCheckBox.setSelected(allCheckBoxSelectedState);
    } else {
      allCheckBox.setSelected(false);
    }

    handleErrorWarning(errorText, warningText);
  }

  private void handleErrorWarning(String errorText, String warningText) {
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
      allCheckBox.setSelected(allCheckBoxSelectedState);
    } else {
      allCheckBox.setSelected(false);
    }

    return errorText;
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
    locationOptionsLabel = new javax.swing.JLabel();
    experimentFolderRadioButton = new javax.swing.JRadioButton();
    experimetParentRadioButton = new javax.swing.JRadioButton();
    homeRadioButton = new javax.swing.JRadioButton();
    otherRadioButton = new javax.swing.JRadioButton();
    pathTextField = new javax.swing.JTextField();
    browseButton = new javax.swing.JButton();
    locationLabel = new javax.swing.JLabel();
    namePanel = new javax.swing.JPanel();
    configurationNameLabel = new javax.swing.JLabel();
    configurationNameTextField = new javax.swing.JTextField();
    defaultNameCheckBox = new javax.swing.JCheckBox();
    fullPathPanel = new javax.swing.JPanel();
    fullPathLabel1 = new javax.swing.JLabel();
    fullPathTextField = new javax.swing.JTextField();
    errorLabel = new javax.swing.JLabel();
    allCheckBox = new javax.swing.JCheckBox();
    linePanel = new javax.swing.JPanel();

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
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
    add(whatLabel, gridBagConstraints);

    viewsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(viewsCheckBox, gridBagConstraints);

    metricsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(metricsCheckBox, gridBagConstraints);

    timelineCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(timelineCheckBox, gridBagConstraints);

    sourceDisassemblyCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(sourceDisassemblyCheckBox, gridBagConstraints);

    callTreeCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(callTreeCheckBox, gridBagConstraints);

    formatsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(formatsCheckBox, gridBagConstraints);

    searchPathCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(searchPathCheckBox, gridBagConstraints);

    pathmapCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(pathmapCheckBox, gridBagConstraints);

    functionColorsCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(functionColorsCheckBox, gridBagConstraints);

    libraryClassVisibilityCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(libraryClassVisibilityCheckBox, gridBagConstraints);

    miscCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(miscCheckBox, gridBagConstraints);

    locationOptionsLabel.setText("Where...:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
    add(locationOptionsLabel, gridBagConstraints);

    experimentFolderRadioButton.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 17;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(experimentFolderRadioButton, gridBagConstraints);

    experimetParentRadioButton.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 18;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(experimetParentRadioButton, gridBagConstraints);

    homeRadioButton.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 19;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(homeRadioButton, gridBagConstraints);

    otherRadioButton.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 20;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(otherRadioButton, gridBagConstraints);

    pathTextField.setColumns(70);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 22;
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
    gridBagConstraints.gridy = 22;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(browseButton, gridBagConstraints);

    locationLabel.setText("Location:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 21;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    add(locationLabel, gridBagConstraints);

    namePanel.setOpaque(false);
    namePanel.setLayout(new java.awt.GridBagLayout());

    configurationNameLabel.setText("Configuration Name:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    namePanel.add(configurationNameLabel, gridBagConstraints);

    configurationNameTextField.setColumns(30);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    namePanel.add(configurationNameTextField, gridBagConstraints);

    defaultNameCheckBox.setText("Save as...");
    defaultNameCheckBox.setOpaque(false);
    defaultNameCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            defaultNameCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    namePanel.add(defaultNameCheckBox, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 23;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
    add(namePanel, gridBagConstraints);

    fullPathPanel.setOpaque(false);
    fullPathPanel.setLayout(new java.awt.GridBagLayout());

    fullPathLabel1.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    fullPathPanel.add(fullPathLabel1, gridBagConstraints);

    fullPathTextField.setEditable(false);
    fullPathTextField.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    fullPathPanel.add(fullPathTextField, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 24;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
    add(fullPathPanel, gridBagConstraints);

    errorLabel.setForeground(new java.awt.Color(255, 0, 51));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 25;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
    add(errorLabel, gridBagConstraints);

    allCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(allCheckBox, gridBagConstraints);

    linePanel.setMaximumSize(new java.awt.Dimension(20, 20));
    linePanel.setMinimumSize(new java.awt.Dimension(10, 30));
    linePanel.setOpaque(false);
    linePanel.setPreferredSize(new java.awt.Dimension(200, 2));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
    add(linePanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void browseButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_browseButtonActionPerformed
    String feed = pathTextField.getText();
    File feedFile = new File(feed);
    if (!feedFile.exists() || !feedFile.isDirectory()) {
      feedFile = null;
    }

    JFileChooser fileChooser = AnWindow.getInstance().getAnChooser("", AnChooser.DIR_CHOOSER, null);
    if (feedFile != null) {
      fileChooser.setCurrentDirectory(feedFile);
    }
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int ret = fileChooser.showOpenDialog(this);
    if (ret == JFileChooser.APPROVE_OPTION) {
      String filePath = fileChooser.getSelectedFile().getAbsolutePath();
      pathTextField.setText(filePath);
    }
    updateStates();
  } // GEN-LAST:event_browseButtonActionPerformed

  private void defaultNameCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_defaultNameCheckBoxActionPerformed
    if (defaultNameCheckBox.isSelected()) {
      configurationNameTextField.setText(UserPref.configDefaultName);
    } else {
      configurationNameTextField.setText("name");
    }
    updateStates();
  } // GEN-LAST:event_defaultNameCheckBoxActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox allCheckBox;
  private javax.swing.JButton browseButton;
  private javax.swing.JCheckBox callTreeCheckBox;
  private javax.swing.JLabel configurationNameLabel;
  private javax.swing.JTextField configurationNameTextField;
  private javax.swing.JCheckBox defaultNameCheckBox;
  private javax.swing.JLabel errorLabel;
  private javax.swing.JRadioButton experimentFolderRadioButton;
  private javax.swing.JRadioButton experimetParentRadioButton;
  private javax.swing.JCheckBox formatsCheckBox;
  private javax.swing.JLabel fullPathLabel1;
  private javax.swing.JPanel fullPathPanel;
  private javax.swing.JTextField fullPathTextField;
  private javax.swing.JCheckBox functionColorsCheckBox;
  private javax.swing.JRadioButton homeRadioButton;
  private javax.swing.JLabel infoLabel;
  private javax.swing.JCheckBox libraryClassVisibilityCheckBox;
  private javax.swing.JPanel linePanel;
  private javax.swing.JLabel locationLabel;
  private javax.swing.JLabel locationOptionsLabel;
  private javax.swing.JCheckBox metricsCheckBox;
  private javax.swing.JCheckBox miscCheckBox;
  private javax.swing.JPanel namePanel;
  private javax.swing.JRadioButton otherRadioButton;
  private javax.swing.JTextField pathTextField;
  private javax.swing.JCheckBox pathmapCheckBox;
  private javax.swing.JCheckBox searchPathCheckBox;
  private javax.swing.JCheckBox sourceDisassemblyCheckBox;
  private javax.swing.JCheckBox timelineCheckBox;
  private javax.swing.JCheckBox viewsCheckBox;
  private javax.swing.JLabel whatLabel;
  // End of variables declaration//GEN-END:variables
}
