/* Copyright (C) 2022 Free Software Foundation

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

import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;

/**
 * @author tpreisle
 */
public class ConfigurationPanel extends javax.swing.JPanel {
  private File expFile = null;

  /** Creates new form ExtraPanel */
  public ConfigurationPanel(boolean includeWorkingDir) {
    initComponents();

    useConfCheckBox.setText(AnLocale.getString("Use this Configuration"));
    useConfCheckBox.setToolTipText(
        AnLocale.getString(
            "If checked, the experiment will be opened using the specified Configuration"));
    useConfCheckBox.setMnemonic(
        AnLocale.getString('U', "OpenExperimentDialogUseThisConfigurationCheckBox"));
    AnUtility.setAccessibleContext(
        useConfCheckBox.getAccessibleContext(), AnLocale.getString("Use this Configuration"));
    confComboBox.setRenderer(new ComboBoxRenderer());
    AnUtility.setAccessibleContext(
        confComboBox.getAccessibleContext(), AnLocale.getString("Use this Configuration"));
    confBrowseButton.setToolTipText(AnLocale.getString("Browse for Configuration File"));
    alwaysCheckBox.setText(
        AnLocale.getString("Always re-open this experiment with this Configuration"));
    alwaysCheckBox.setToolTipText(
        AnLocale.getString(
            "If checked, the experiment will be opened using this Configuration when re-opened"));
    alwaysCheckBox.setMnemonic(AnLocale.getString('A', "OpenExperimentDialogAlwaysReopenCheckBox"));
    AnUtility.setAccessibleContext(
        alwaysCheckBox.getAccessibleContext(),
        AnLocale.getString("Always re-open this experiment with this Configuration"));

    if (includeWorkingDir) {
      workingDirCheckBox.setText(AnLocale.getString("Change Working Directory"));
      workingDirCheckBox.setSelected(true);
      workingDirCheckBox.setMnemonic(
          AnLocale.getString('W', "OpenExperimentDialogChangeWorkingDirectoryCheckBox"));
      workingDirLabel.setText(AnLocale.getString("Directory:"));
      wdBrowseButton.setToolTipText(AnLocale.getString("Browse for Working Directory"));
      AnUtility.setAccessibleContext(
          workingDirCheckBox.getAccessibleContext(),
          AnLocale.getString("Change Working Directory"));
      AnUtility.setAccessibleContext(
          wdBrowseButton.getAccessibleContext(),
          AnLocale.getString("Browse for Working Directory"));
      AnUtility.setAccessibleContext(
          workingDirLabel.getAccessibleContext(), AnLocale.getString("Directory:"));
      AnUtility.setAccessibleContext(
          workingDirTextField.getAccessibleContext(), AnLocale.getString("Working Directory"));
    } else {
      workingDirCheckBox.setVisible(false);
      workingDirLabel.setVisible(false);
      workingDirTextField.setVisible(false);
      wdBrowseButton.setVisible(false);
    }
    updateStates();
  }

  public class ComboBoxRenderer implements ListCellRenderer {
    private final ListCellRenderer originalRenderer;

    public ComboBoxRenderer() {
      originalRenderer = confComboBox.getRenderer();
    }

    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component component =
          originalRenderer.getListCellRendererComponent(
              list, value, index, isSelected, cellHasFocus);
      if (addSeparator(index)) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JSeparator(), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
      } else {
        return component;
      }
    }

    private boolean addSeparator(int index) {
      if (confComboBox.getItemCount() <= 1) {
        return false;
      }
      if (index < 1) {
        return false;
      }
      String previousPath = ((ConfName) confComboBox.getItemAt(index - 1)).getPath();
      String previousDir = AnUtility.dirname(previousPath);
      String thisPath = ((ConfName) confComboBox.getItemAt(index)).getPath();
      String thisDir = AnUtility.dirname(thisPath);
      return !previousDir.equals(thisDir);
    }
  }

  private void updateStates() {
    boolean UseOn = useConfCheckBox.isSelected();
    confBrowseButton.setEnabled(UseOn);
    confComboBox.setEnabled(UseOn);
    alwaysCheckBox.setEnabled(UseOn);
    boolean wdOn = workingDirCheckBox.isSelected();
    workingDirLabel.setEnabled(wdOn);
    workingDirTextField.setEnabled(wdOn);
    wdBrowseButton.setEnabled(wdOn);
  }

  public void initConfigurationChoices(File file) {
    this.expFile = file;
    List<ConfName> list = new ArrayList<ConfName>();
    File confDir;
    // look inside experiment
    confDir =
        new AnFile(UserPref.getConfigurationDirPath(file.getAbsolutePath())); // Local or remote
    addConfigFile(confDir, list);
    // look in parent folder
    confDir = new AnFile(file.getParentFile().getAbsolutePath()); // Local or remote
    addConfigFile(confDir, list);
    // look in Home folder
    confDir = new File(UserPref.getHomeConfigurationDirPath()); // Always local
    addConfigFile(confDir, list);

    // Add As Last Time This Experiment Closed
    String asClosedPath = null;
    asClosedPath = UserPref.getAsWhenClosedConfigPath(file.getAbsolutePath());
    if (asClosedPath != null && new File(asClosedPath).exists()) {
      list.add(
          new ConfName(asClosedPath, AnLocale.getString("As This Experiment Closed Last Time")));
    }
    // Add As Current Experiment
    String[][] experimentsGroups = AnWindow.getInstance().getExperimentGroups();
    if (experimentsGroups != null && experimentsGroups.length > 0) {
      String asPreviousClosedPath = UserPref.getAsWhenClosedConfigPath(experimentsGroups[0][0]);
      String previousDisplayName = AnLocale.getString("As Current Experiment");
      list.add(new ConfName(asPreviousClosedPath, previousDisplayName));
    }
    // Add As Previous Experiment Closed
    else if (UserPref.getInstance().getLastClosedExpConfPath() != null
        && UserPref.getInstance().getLastClosedExpConfPath().length() > 0) {
      String asPreviousClosedPath = UserPref.getInstance().getLastClosedExpConfPath();
      String previousDisplayName = AnLocale.getString("As Previous Experiment Closed");
      list.add(new ConfName(asPreviousClosedPath, previousDisplayName));
    }

    confComboBox.removeAllItems();
    for (ConfName confName : list) {
      confComboBox.addItem(confName);
    }

    useConfCheckBox.setSelected(false);
    if (confComboBox.getItemCount() == 0) {
      useConfCheckBox.setEnabled(false);
    }
    // Look for default
    boolean defaultFound = false;
    for (int i = 0; i < confComboBox.getItemCount(); i++) {
      ConfName confName = (ConfName) confComboBox.getItemAt(i);
      if (confName.isDefault()) {
        confComboBox.setSelectedItem(confName);
        useConfCheckBox.setSelected(true);
        defaultFound = true;
        break;
      }
    }
    if (!defaultFound) {
      // Look for As Last Experiment Closed
      for (int i = 0; i < confComboBox.getItemCount(); i++) {
        ConfName confName = (ConfName) confComboBox.getItemAt(i);
        if (confName.isAsLast()) {
          confComboBox.setSelectedItem(confName);
          //                    useConfCheckBox.setSelected(true); // Don't select it by default.
          // It's cpnfusing!
          break;
        }
      }
    }
    updateStates();
  }

  private void addConfigFile(File dir, List<ConfName> list) {
    if (!dir.exists() || !dir.isDirectory()) {
      return;
    }
    File[] found = dir.listFiles();
    if (found != null) {
      for (File f : found) {
        if (f.getName().endsWith(UserPref.configurationSuffix)
            && !f.getName().contains(UserPref.configAsWhenClosedName)) {
          list.add(new ConfName(f.getAbsolutePath(), null));
        }
      }
    }
  }

  public void setWorkingDirectory(String wd) {
    workingDirTextField.setText(wd);
    workingDirTextField.setCaretPosition(wd.length());
  }

  public String getWorkingDirectory() {
    String wd = null;
    if (workingDirCheckBox.isSelected()) {
      wd = workingDirTextField.getText();
      if (wd.length() == 0) {
        wd = null;
      }
    }
    return wd;
  }

  public String getConfiguration() {
    String configuration = null;
    if (useConfCheckBox.isSelected()) {
      ConfName confName = (ConfName) confComboBox.getSelectedItem();
      if (confName != null) {
        configuration = confName.getPath();
      }
    }
    return configuration;
  }

  public boolean alwaysUseThisConfiguration() {
    boolean always = alwaysCheckBox.isSelected();
    return always;
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    useConfCheckBox = new javax.swing.JCheckBox();
    confComboBox = new javax.swing.JComboBox();
    confBrowseButton = new javax.swing.JButton();
    alwaysCheckBox = new javax.swing.JCheckBox();
    workingDirCheckBox = new javax.swing.JCheckBox();
    workingDirLabel = new javax.swing.JLabel();
    workingDirTextField = new javax.swing.JTextField();
    wdBrowseButton = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    useConfCheckBox.setText("NOI18N");
    useConfCheckBox.setOpaque(false);
    useConfCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            useConfCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    add(useConfCheckBox, gridBagConstraints);

    confComboBox.setEditable(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 21, 0, 0);
    add(confComboBox, gridBagConstraints);

    confBrowseButton.setText("...");
    confBrowseButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            confBrowseButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(confBrowseButton, gridBagConstraints);

    alwaysCheckBox.setOpaque(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 18, 0, 0);
    add(alwaysCheckBox, gridBagConstraints);

    workingDirCheckBox.setText("NOI18N");
    workingDirCheckBox.setOpaque(false);
    workingDirCheckBox.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            workingDirCheckBoxActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(workingDirCheckBox, gridBagConstraints);

    workingDirLabel.setText("NOI18N");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 21, 0, 0);
    add(workingDirLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(workingDirTextField, gridBagConstraints);

    wdBrowseButton.setText("...");
    wdBrowseButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            wdBrowseButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(wdBrowseButton, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void wdBrowseButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_wdBrowseButtonActionPerformed
    String currentWD = workingDirTextField.getText();
    AnChooser chooser =
        AnWindow.getInstance()
            .getAnChooser(
                AnLocale.getString("Working Directory"), AnChooser.DIR_CHOOSER, currentWD);
    int res = chooser.showDialog(AnWindow.getInstance().getFrame(), null);
    if (res == AnChooser.APPROVE_OPTION) {
      File wdFile = chooser.getSelectedAnFile();
      String wd = wdFile.getAbsolutePath();
      workingDirTextField.setText(wd);
    }
  } // GEN-LAST:event_wdBrowseButtonActionPerformed

  private void workingDirCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_workingDirCheckBoxActionPerformed
    updateStates();
  } // GEN-LAST:event_workingDirCheckBoxActionPerformed

  private void useConfCheckBoxActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_useConfCheckBoxActionPerformed
    updateStates();
  } // GEN-LAST:event_useConfCheckBoxActionPerformed

  private void confBrowseButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_confBrowseButtonActionPerformed
    AnChooser chooser =
        AnWindow.getInstance()
            .getAnChooser(
                AnLocale.getString("Configurations"),
                AnChooser.DIR_FILE_CHOOSER,
                expFile.getParent());
    //        JFileChooser chooser = new JFileChooser(expFile.getParent());
    chooser.setFileFilter(new ConfigFilter());
    chooser.setFileHidingEnabled(false); // FIXUP: ????
    int res = chooser.showDialog(AnWindow.getInstance().getFrame(), null);
    if (res == AnChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      String path = file.getAbsolutePath();
      ConfName confName = new ConfName(path, null);
      confComboBox.addItem(confName);
      confComboBox.setSelectedItem(confName);
    }
  } // GEN-LAST:event_confBrowseButtonActionPerformed

  private class ConfigFilter extends FileFilter {
    @Override
    public String getDescription() {
      return UserPref.configurationSuffix;
    }

    @Override
    public boolean accept(File f) {
      boolean accept =
          f.isDirectory()
              || (f.getName().endsWith(UserPref.configurationSuffix)
                  && !f.getName().startsWith(UserPref.getAsWhenClosedConfigName()));
      return accept;
    }
  }

  private class ConfName {
    private String path;
    private String displayName;
    private boolean isDefault;
    private boolean isAsLast;

    public ConfName(String path, String displayName) {
      this.path = path;
      isDefault = false;
      isAsLast = false;

      String baseName = AnUtility.basename(path);
      if (path.contains("/" + UserPref.configAsWhenClosedName + "/")) {
        isAsLast = true;
      } else {
        if (baseName.equals(UserPref.getDefaultConfigurationName())) {
          isDefault = true;
        }
      }

      if (displayName != null) {
        this.displayName = displayName;
      } else {
        this.displayName = defaultDisplayName(baseName);
      }
    }

    private String defaultDisplayName(String baseName) {
      String defaultDisplayName;
      if (path.contains("/" + UserPref.configAsWhenClosedName + "/")) {
        defaultDisplayName = AnLocale.getString("As Last Experiment Closed");
      } else {
        String confName = baseName;
        if (confName.endsWith(UserPref.configurationSuffix)) {
          confName =
              baseName.substring(0, confName.length() - UserPref.configurationSuffix.length());
        }

        String displayPath = path;
        int n = displayPath.length();
        if (n > 70) {
          displayPath = displayPath.substring(0, 25) + "....." + displayPath.substring(n - 45, n);
        }

        defaultDisplayName = confName + "  -  (" + displayPath + ")";
      }
      return defaultDisplayName;
    }

    @Override
    public String toString() {
      return displayName;
    }

    public boolean isDefault() {
      return isDefault;
    }

    public boolean isAsLast() {
      return isAsLast;
    }

    public String getPath() {
      return path;
    }
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox alwaysCheckBox;
  private javax.swing.JButton confBrowseButton;
  private javax.swing.JComboBox confComboBox;
  private javax.swing.JCheckBox useConfCheckBox;
  private javax.swing.JButton wdBrowseButton;
  private javax.swing.JCheckBox workingDirCheckBox;
  private javax.swing.JLabel workingDirLabel;
  private javax.swing.JTextField workingDirTextField;
  // End of variables declaration//GEN-END:variables
}
