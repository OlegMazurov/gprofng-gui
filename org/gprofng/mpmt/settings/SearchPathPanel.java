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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;

public class SearchPathPanel extends javax.swing.JPanel {
  private static Color shadedBackground = new Color(246, 246, 246);
  private List<String> data = new ArrayList<String>();
  private PMListSelectionListener fromListSelectionListener;
  private PMDocumentListener pmDocumentListener;
  private Settings settings;

  public SearchPathPanel(Settings settings) {
    this.settings = settings;
    initComponents();
    setBorder(BorderFactory.createMatteBorder(12, 12, 12, 10, (Color) null));
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    AnUtility.setTextAndAccessibleContext(topLabel, AnLocale.getString("Search Path:"));
    AnUtility.setTextAndAccessibleContext(pathLabel, AnLocale.getString("Path:"));
    pathLabel.setDisplayedMnemonic(AnLocale.getString('P', "SearchPathPathLabelMnemonic"));
    pathLabel.setLabelFor(pathTextField);
    AnUtility.setTTAndAccessibleContext(pathTextField, AnLocale.getString("Search path"));
    appendButton.setText(AnLocale.getString("Append"));
    appendButton.setToolTipText(AnLocale.getString("Append the Search Path to the list"));
    appendButton.setMnemonic(AnLocale.getString('e', "SearchPathPanelAppend_MN"));
    updateButton.setText(AnLocale.getString("Update"));
    updateButton.setMnemonic(AnLocale.getString('t', "SearchPathPanelUpdate_MN"));
    updateButton.setToolTipText(
        AnLocale.getString("Update the selected search path with the changes"));
    upButton.setText(AnLocale.getString("Up"));
    upButton.setMnemonic(AnLocale.getString('u', "SearchPathPanelUp_MN"));
    upButton.setToolTipText(AnLocale.getString("Move the selected search path up"));
    downButton.setText(AnLocale.getString("Down"));
    downButton.setMnemonic(AnLocale.getString('d', "SearchPathPanelDown_MN"));
    downButton.setToolTipText(AnLocale.getString("Move theselected search path down"));
    removeButton.setText(AnLocale.getString("Remove"));
    removeButton.setMnemonic(AnLocale.getString('r', "SearchPathPanelRemove_MN"));
    removeButton.setToolTipText(AnLocale.getString("Remove the selected search path"));
    browseButton.setToolTipText(AnLocale.getString("Browse for Path (ALT B)"));
    browseButton.setMnemonic(AnLocale.getString('B', "SearchPathPanelBrowseButtonMN"));
    AnUtility.setTextAndAccessibleContext(listLabel, AnLocale.getString("Search Paths:"));
    listLabel.setDisplayedMnemonic(AnLocale.getString('S', "SearchPathSearchPathsLabelMnemonic"));
    listLabel.setLabelFor(list);
    listPanel.setBorder(
        BorderFactory.createLineBorder(AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR, 1));
    //        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
    // AnEnvironment.NAVIGATION_PANEL_BORDER_COLOR));
    list.setCellRenderer(new LCR());
    AnUtility.setTTAndAccessibleContext(list, AnLocale.getString("Search path list"));

    fromListSelectionListener = new PMListSelectionListener();
    pmDocumentListener = new PMDocumentListener();

    list.addListSelectionListener(fromListSelectionListener);

    pathTextField.getDocument().addDocumentListener(pmDocumentListener);

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
    list.setListData(data.toArray());
    //        appendButton.setText("dsa");
    //        appendButton.validate();
    //        appendButton.repaint();
    if (data.size() == 0) {
      return;
    }

    if (selectedIndex >= 0 && selectedIndex <= data.size()) {
      list.setSelectedIndex(selectedIndex);
      list.ensureIndexIsVisible(selectedIndex);
    }
  }

  private void madeChanges() {
    settings.setMessageAreaVisible(true);
    settings.stateChanged(null);
  }

  /** Called when there are changes to pathmaps settings */
  public void updateGUI() {
    final int size;
    int i;

    data.clear();

    List<String> searchPath = AnWindow.getInstance().getSettings().getSearchPathSetting().get();
    size = searchPath.size();

    for (i = 0; i < size; i++) {
      data.add(searchPath.get(i));
    }

    list.setListData(data.toArray());

    if (size == 0) {
      return;
    }

    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);

    refreshPanel(0);
  }

  public void checkSearchPathChanged(Object originalSource) {
    String[] searchPath = data.toArray(new String[data.size()]);
    AnWindow.getInstance().getSettings().getSearchPathSetting().set(originalSource, searchPath);
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
      pathTextField.setText((String) list.getSelectedValue());
      updateStates();
    }
  }

  private void updateStates() {
    appendButton.setEnabled(false);
    updateButton.setEnabled(false);
    upButton.setEnabled(false);
    downButton.setEnabled(false);
    removeButton.setEnabled(false);

    if (data != null && data.size() > 0) {
      if (list.getSelectedIndex() >= 0) {
        removeButton.setEnabled(true);
      }
      if (list.getSelectedIndex() > 0) {
        upButton.setEnabled(true);
      }
      if (list.getSelectedIndex() < data.size() - 1) {
        downButton.setEnabled(true);
      }
    }

    if (pathTextField.getText().length() > 0) {
      appendButton.setEnabled(true);
      if (data != null && data.size() > 0) {
        int index = list.getSelectedIndex();
        if (index >= 0) {
          String from = data.get(index);
          if (!from.equals(pathTextField.getText())) {
            updateButton.setEnabled(true);
          }
        }
      }
    }
  }

  private String browse(String feed) {
    String filePath = null;
    if (!Analyzer.getInstance().isRemote()) {
      // Local
      File feedFile = new File(feed);
      if (!feedFile.exists()) {
        feedFile = null;
      }

      JFileChooser fileChooser = new JFileChooser(); // Always local file chooser
      fileChooser.setDialogTitle(AnLocale.getString("From path"));
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
    pathLabel = new javax.swing.JLabel();
    pathTextField = new javax.swing.JTextField();
    browseButton = new javax.swing.JButton();
    listLabel = new javax.swing.JLabel();
    listPanel = new javax.swing.JPanel();
    listPanelInner = new javax.swing.JPanel();
    scrollPane = new javax.swing.JScrollPane();
    list = new javax.swing.JList();
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

    pathLabel.setText("From:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(pathLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 0);
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
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 0);
    add(browseButton, gridBagConstraints);

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

    scrollPane.setBorder(null);
    scrollPane.setViewportView(list);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.weighty = 1.0;
    listPanelInner.add(scrollPane, gridBagConstraints);

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
    data.add(pathTextField.getText());
    refreshPanel(data.size() - 1);
    madeChanges();
  } // GEN-LAST:event_appendButtonActionPerformed

  private void updateButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_updateButtonActionPerformed
    int index = list.getSelectedIndex();
    if (index >= 0) {
      data.remove(index);
      data.add(index, pathTextField.getText());

      refreshPanel(index);
      madeChanges();
    }
  } // GEN-LAST:event_updateButtonActionPerformed

  private void removeButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_removeButtonActionPerformed
    int index = list.getSelectedIndex();
    if (index >= 0) {
      data.remove(index);
      if (index < data.size()) {
        refreshPanel(index);
      } else {
        refreshPanel(index - 1);
      }
      madeChanges();
    }
  } // GEN-LAST:event_removeButtonActionPerformed

  private void upButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_upButtonActionPerformed
    int index = list.getSelectedIndex();
    if (index > 0) {
      String from = data.get(index);
      data.remove(index);
      data.add(index - 1, from);
      refreshPanel(index - 1);
      madeChanges();
    }
  } // GEN-LAST:event_upButtonActionPerformed

  private void downButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_downButtonActionPerformed
    int index = list.getSelectedIndex();
    if (index < data.size()) {
      String from = data.get(index);
      data.remove(index);
      data.add(index + 1, from);
      refreshPanel(index + 1);
      madeChanges();
    }
  } // GEN-LAST:event_downButtonActionPerformed

  private void browseButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_browseButtonActionPerformed
    String feed = pathTextField.getText();
    String res = browse(feed);
    pathTextField.setText(res);
  } // GEN-LAST:event_browseButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton appendButton;
  private javax.swing.JButton browseButton;
  private javax.swing.JPanel buttomButtonPanel;
  private javax.swing.JButton downButton;
  private javax.swing.JList list;
  private javax.swing.JLabel listLabel;
  private javax.swing.JPanel listPanel;
  private javax.swing.JPanel listPanelInner;
  private javax.swing.JLabel pathLabel;
  private javax.swing.JTextField pathTextField;
  private javax.swing.JButton removeButton;
  private javax.swing.JScrollPane scrollPane;
  private javax.swing.JPanel topButtonPanel;
  private javax.swing.JLabel topLabel;
  private javax.swing.JButton upButton;
  private javax.swing.JButton updateButton;
  // End of variables declaration//GEN-END:variables
}
