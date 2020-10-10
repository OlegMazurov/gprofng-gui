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


package org.gprofng.mpmt.settings;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnChooser;
import org.gprofng.mpmt.AnFile;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;

public class PathmapsPanel extends javax.swing.JPanel {
  private static Color shadedBackground = new Color(249, 249, 249);
  private static Color listLabelBackground = new Color(241, 241, 241);
  private List<String> dataFrom;
  private List<String> dataTo;
  private PMListSelectionListener fromListSelectionListener;
  private PMDocumentListener pmDocumentListener;
  private Settings settings;

  public PathmapsPanel(Settings settings) {
    this.settings = settings;
    initComponents();
    setBorder(BorderFactory.createMatteBorder(12, 12, 12, 10, (Color) null));
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    AnUtility.setTextAndAccessibleContext(topLabel, AnLocale.getString("Map the following path:"));
    AnUtility.setTextAndAccessibleContext(fromLabel, AnLocale.getString("From Path:"));
    fromLabel.setDisplayedMnemonic(AnLocale.getString('F', "PathmapFromPathLabelMnemonic"));
    fromLabel.setLabelFor(fromTextField);
    AnUtility.setTextAndAccessibleContext(toLabel, AnLocale.getString("To Path:"));
    toLabel.setDisplayedMnemonic(AnLocale.getString('T', "PathmapToPathLabelMnemonic"));
    toLabel.setLabelFor(toTextField);
    appendButton.setText(AnLocale.getString("Append"));
    appendButton.setToolTipText(AnLocale.getString("Append the mapping to the pathmap list"));
    appendButton.setMnemonic(AnLocale.getString('P', "PathMapPanelBrowseAppend_MN"));
    updateButton.setText(AnLocale.getString("Update"));
    updateButton.setMnemonic(AnLocale.getString('e', "PathMapPanelBrowseUpdate_MN"));
    updateButton.setToolTipText(AnLocale.getString("Update the selected pathmap with the changes"));
    upButton.setText(AnLocale.getString("Up"));
    upButton.setMnemonic(AnLocale.getString('u', "PathMapPanelBrowseUp_MN"));
    upButton.setToolTipText(AnLocale.getString("Move the selected pathmap up"));
    downButton.setText(AnLocale.getString("Down"));
    downButton.setMnemonic(AnLocale.getString('d', "PathMapPanelBrowseDown_MN"));
    downButton.setToolTipText(AnLocale.getString("Move theselected pathmap down"));
    removeButton.setText(AnLocale.getString("Remove"));
    removeButton.setMnemonic(AnLocale.getString('r', "PathMapPanelBrowseRemove_MN"));
    removeButton.setToolTipText(AnLocale.getString("Remove the selected pathmap"));
    fromButton.setToolTipText(AnLocale.getString("Browse for From Path (ALT B)"));
    fromButton.setMnemonic(AnLocale.getString('B', "PathMapPanelBrowseFromPathButtonMN"));
    toButton.setToolTipText(AnLocale.getString("Browse for To Path (ALT W)"));
    toButton.setMnemonic(AnLocale.getString('W', "PathMapPanelBrowsToPathButtonMN"));
    AnUtility.setTextAndAccessibleContext(listLabel, AnLocale.getString("Pathmaps:"));
    listLabel.setDisplayedMnemonic(AnLocale.getString('m', "PathmapPathmapsLabelMnemonic"));
    listLabel.setLabelFor(listTo);
    AnUtility.setTextAndAccessibleContext(fromListLabel, AnLocale.getString("From Path"));
    fromListLabel.setHorizontalAlignment(JLabel.CENTER);
    AnUtility.setTextAndAccessibleContext(toListLabel, AnLocale.getString("To Path"));
    toListLabel.setHorizontalAlignment(JLabel.CENTER);
    listPanelInner.setBackground(listLabelBackground);
    listPanel.setBorder(
        BorderFactory.createLineBorder(AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR, 1));
    fromListLabel.setBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR));
    listFromScrollPane.setBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR));
    listFrom.setCellRenderer(new LCR());
    AnUtility.setTTAndAccessibleContext(listFrom, AnLocale.getString("Pathmap list"));
    listTo.setCellRenderer(new LCR());

    fromListSelectionListener = new PMListSelectionListener();
    pmDocumentListener = new PMDocumentListener();

    listFrom.addListSelectionListener(fromListSelectionListener);
    listTo.addListSelectionListener(fromListSelectionListener);

    fromTextField.getDocument().addDocumentListener(pmDocumentListener);
    toTextField.getDocument().addDocumentListener(pmDocumentListener);

    updateStates();
  }

  private class LCR extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      JLabel label =
          (JLabel)
              super.getListCellRendererComponent(
                  list,
                  value,
                  index,
                  isSelected,
                  cellHasFocus); // To change body of generated methods, choose Tools | Templates.
      label.setText(" " + label.getText());
      if (index % 2 == 1 && !isSelected) {
        label.setBackground(shadedBackground);
      }
      return label;
    }
  }

  private void refreshPanel(int selectedIndex) {
    listFrom.setListData(dataFrom.toArray());
    listTo.setListData(dataTo.toArray());
    if (dataFrom.size() == 0) {
      return;
    }

    if (selectedIndex >= 0 && selectedIndex <= dataFrom.size()) {
      listFrom.setSelectedIndex(selectedIndex);
      listFrom.ensureIndexIsVisible(selectedIndex);

      listTo.setSelectedIndex(selectedIndex);
      listTo.ensureIndexIsVisible(selectedIndex);
    }
  }

  /** Called when there are changes to pathmaps settings */
  public void updateGUI() {
    String[] from = AnWindow.getInstance().getSettings().getPathMapSetting().getPathMapFrom();
    String[] to = AnWindow.getInstance().getSettings().getPathMapSetting().getPathMapTo();
    dataFrom = new ArrayList(Arrays.asList(from));
    dataTo = new ArrayList(Arrays.asList(to));
    refreshPanel(0);
  }

  private void madeChanges() {
    settings.setMessageAreaVisible(true);
    settings.stateChanged(null);
  }

  /** Called from Save button in Settings panel */
  public void checkPathMapChanged(Object originalSource) {
    String[] from = (String[]) dataFrom.toArray(new String[dataFrom.size()]);
    String[] to = (String[]) dataTo.toArray(new String[dataTo.size()]);
    String[][] pathMap = new String[2][];
    pathMap[0] = from;
    pathMap[1] = to;
    AnWindow.getInstance().getSettings().getPathMapSetting().set(originalSource, pathMap);
  }

  class PMDocumentListener implements DocumentListener {

    @Override
    public void removeUpdate(DocumentEvent e) {
      updateStates();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      updateStates();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      updateStates();
    }
  }

  class PMListSelectionListener implements javax.swing.event.ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent event) {
      if (event.getValueIsAdjusting()) {
        return;
      }

      // Synchronize selection
      listFrom.removeListSelectionListener(this);
      listTo.removeListSelectionListener(this);
      if (event.getSource() == listFrom) {
        listTo.setSelectedIndex(listFrom.getSelectedIndex());
      } else {
        listFrom.setSelectedIndex(listTo.getSelectedIndex());
      }
      listFrom.addListSelectionListener(this);
      listTo.addListSelectionListener(this);

      fromTextField.setText((String) listFrom.getSelectedValue());
      toTextField.setText((String) listTo.getSelectedValue());

      updateStates();
    }
  }

  private void updateStates() {
    appendButton.setEnabled(false);
    updateButton.setEnabled(false);
    upButton.setEnabled(false);
    downButton.setEnabled(false);
    removeButton.setEnabled(false);

    if (dataFrom != null && dataFrom.size() > 0) {
      if (listFrom.getSelectedIndex() >= 0) {
        removeButton.setEnabled(true);
      }
      if (listFrom.getSelectedIndex() > 0) {
        upButton.setEnabled(true);
      }
      if (listFrom.getSelectedIndex() < dataFrom.size() - 1) {
        downButton.setEnabled(true);
      }
    }

    if (fromTextField.getText().length() > 0 && toTextField.getText().length() > 0) {
      appendButton.setEnabled(true);
      if (dataFrom != null && dataFrom.size() > 0) {
        int index = listFrom.getSelectedIndex();
        if (index >= 0) {
          String from = dataFrom.get(index);
          String to = dataTo.get(index);
          if (!from.equals(fromTextField.getText()) || !to.equals(toTextField.getText())) {
            updateButton.setEnabled(true);
          }
        }
      }
    }
  }

  private String browse(String feed, String title) {
    String filePath = null;
    if (!Analyzer.getInstance().isRemote()) {
      // Local
      File feedFile = new File(feed);
      if (!feedFile.exists()) {
        feedFile = null;
      }

      JFileChooser fileChooser = new JFileChooser(); // Always local file chooser
      fileChooser.setDialogTitle(title);
      if (feedFile != null) {
        fileChooser.setCurrentDirectory(feedFile);
      }
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      int ret = fileChooser.showOpenDialog(this);
      if (ret == JFileChooser.APPROVE_OPTION) {
        filePath = fileChooser.getSelectedFile().getAbsolutePath();
      }
    } else {
      // Remote
      AnFile feedFile = new AnFile(feed);
      if (!feedFile.exists()) {
        feedFile = null;
      }

      AnChooser fileChooser =
          AnWindow.getInstance()
              .getAnChooser(AnLocale.getString("To path"), AnChooser.DIR_FILE_CHOOSER, feed);
      //            JFileChooser fileChooser = new JFileChooser(); // Always local file chooser
      if (feedFile != null) {
        fileChooser.setCurrentDirectory(feedFile);
      }
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      int ret = fileChooser.showOpenDialog(this);
      if (ret == JFileChooser.APPROVE_OPTION) {
        filePath = fileChooser.getSelectedFile().getAbsolutePath();
      }
    }
    return filePath;
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    topLabel = new javax.swing.JLabel();
    fromLabel = new javax.swing.JLabel();
    fromTextField = new javax.swing.JTextField();
    fromButton = new javax.swing.JButton();
    toLabel = new javax.swing.JLabel();
    toTextField = new javax.swing.JTextField();
    toButton = new javax.swing.JButton();
    listLabel = new javax.swing.JLabel();
    listPanel = new javax.swing.JPanel();
    listPanelInner = new javax.swing.JPanel();
    fromListLabel = new javax.swing.JLabel();
    listFromScrollPane = new javax.swing.JScrollPane();
    listFrom = new javax.swing.JList();
    toListLabel = new javax.swing.JLabel();
    listToScrollPane = new javax.swing.JScrollPane();
    listTo = new javax.swing.JList();
    topButtonPanel = new javax.swing.JPanel();
    appendButton = new javax.swing.JButton();
    updateButton = new javax.swing.JButton();
    buttomButtonPanel = new javax.swing.JPanel();
    upButton = new javax.swing.JButton();
    downButton = new javax.swing.JButton();
    removeButton = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    topLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    add(topLabel, gridBagConstraints);

    fromLabel.setText("From:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(fromLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 0);
    add(fromTextField, gridBagConstraints);

    fromButton.setText("...");
    fromButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            fromButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 0);
    add(fromButton, gridBagConstraints);

    toLabel.setText("To:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
    add(toLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(toTextField, gridBagConstraints);

    toButton.setText("...");
    toButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            toButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(toButton, gridBagConstraints);

    listLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(listLabel, gridBagConstraints);

    listPanel.setLayout(new java.awt.GridBagLayout());

    listPanelInner.setLayout(new java.awt.GridBagLayout());

    fromListLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    listPanelInner.add(fromListLabel, gridBagConstraints);

    listFromScrollPane.setBorder(null);
    listFromScrollPane.setViewportView(listFrom);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.weighty = 1.0;
    listPanelInner.add(listFromScrollPane, gridBagConstraints);

    toListLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    listPanelInner.add(toListLabel, gridBagConstraints);

    listToScrollPane.setBorder(null);
    listToScrollPane.setViewportView(listTo);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.weighty = 1.0;
    listPanelInner.add(listToScrollPane, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    listPanel.add(listPanelInner, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(listPanel, gridBagConstraints);

    topButtonPanel.setLayout(new java.awt.GridBagLayout());

    appendButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            appendButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    topButtonPanel.add(appendButton, gridBagConstraints);

    updateButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            updateButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    topButtonPanel.add(updateButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 0);
    add(topButtonPanel, gridBagConstraints);

    buttomButtonPanel.setLayout(new java.awt.GridBagLayout());

    upButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            upButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    buttomButtonPanel.add(upButton, gridBagConstraints);

    downButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            downButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    buttomButtonPanel.add(downButton, gridBagConstraints);

    removeButton.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            removeButtonActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    buttomButtonPanel.add(removeButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    add(buttomButtonPanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void appendButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_appendButtonActionPerformed
    dataFrom.add(fromTextField.getText());
    dataTo.add(toTextField.getText());
    refreshPanel(dataFrom.size() - 1);
    madeChanges();
  } // GEN-LAST:event_appendButtonActionPerformed

  private void updateButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_updateButtonActionPerformed
    int index = listFrom.getSelectedIndex();
    if (index >= 0) {
      dataFrom.remove(index);
      dataTo.remove(index);
      dataFrom.add(index, fromTextField.getText());
      dataTo.add(index, toTextField.getText());

      refreshPanel(index);
      madeChanges();
    }
  } // GEN-LAST:event_updateButtonActionPerformed

  private void removeButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_removeButtonActionPerformed
    int index = listFrom.getSelectedIndex();
    if (index >= 0) {
      dataFrom.remove(index);
      dataTo.remove(index);

      if (index < dataFrom.size()) {
        refreshPanel(index);
      } else {
        refreshPanel(index - 1);
      }
      madeChanges();
    }
  } // GEN-LAST:event_removeButtonActionPerformed

  private void upButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_upButtonActionPerformed
    int index = listFrom.getSelectedIndex();
    if (index > 0) {
      String from = dataFrom.get(index);
      String to = dataTo.get(index);
      dataFrom.remove(index);
      dataTo.remove(index);
      dataFrom.add(index - 1, from);
      dataTo.add(index - 1, to);
      refreshPanel(index - 1);
      madeChanges();
    }
  } // GEN-LAST:event_upButtonActionPerformed

  private void downButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_downButtonActionPerformed
    int index = listFrom.getSelectedIndex();
    if (index < dataFrom.size()) {
      String from = dataFrom.get(index);
      String to = dataTo.get(index);
      dataFrom.remove(index);
      dataTo.remove(index);
      dataFrom.add(index + 1, from);
      dataTo.add(index + 1, to);
      refreshPanel(index + 1);
      madeChanges();
    }
  } // GEN-LAST:event_downButtonActionPerformed

  private void fromButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_fromButtonActionPerformed
    String feed = fromTextField.getText();
    String res = browse(feed, AnLocale.getString("From Path"));
    fromTextField.setText(res);
  } // GEN-LAST:event_fromButtonActionPerformed

  private void toButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_toButtonActionPerformed
    String feed = toTextField.getText();
    String res = browse(feed, AnLocale.getString("To Path"));
    toTextField.setText(res);
  } // GEN-LAST:event_toButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton appendButton;
  private javax.swing.JPanel buttomButtonPanel;
  private javax.swing.JButton downButton;
  private javax.swing.JButton fromButton;
  private javax.swing.JLabel fromLabel;
  private javax.swing.JLabel fromListLabel;
  private javax.swing.JTextField fromTextField;
  private javax.swing.JList listFrom;
  private javax.swing.JScrollPane listFromScrollPane;
  private javax.swing.JLabel listLabel;
  private javax.swing.JPanel listPanel;
  private javax.swing.JPanel listPanelInner;
  private javax.swing.JList listTo;
  private javax.swing.JScrollPane listToScrollPane;
  private javax.swing.JButton removeButton;
  private javax.swing.JButton toButton;
  private javax.swing.JLabel toLabel;
  private javax.swing.JLabel toListLabel;
  private javax.swing.JTextField toTextField;
  private javax.swing.JPanel topButtonPanel;
  private javax.swing.JLabel topLabel;
  private javax.swing.JButton upButton;
  private javax.swing.JButton updateButton;
  // End of variables declaration//GEN-END:variables
}
