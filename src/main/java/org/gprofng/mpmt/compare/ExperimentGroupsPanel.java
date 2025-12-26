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


package org.gprofng.mpmt.compare;

import org.gprofng.mpmt.AnChooser;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author vmezents
 */
public class ExperimentGroupsPanel extends javax.swing.JPanel implements ActionListener {
  private AnWindow window;
  private Frame frame;
  private String title;
  public List<String> listAbsPathes;
  private static int IdCount = 0;
  private final int id;

  /** Creates new form ExpGrpJPanel */
  public ExperimentGroupsPanel(
      final AnWindow window, final Frame frame, final String title, String toolTip, char mnemonic) {
    this.window = window;
    this.frame = frame;
    this.title = title;
    id = ++IdCount;
    init();
    label.setText(title);
    label.setToolTipText(toolTip);
    if (mnemonic != ' ') {
      label.setDisplayedMnemonic(mnemonic);
      label.setLabelFor(listExpNames);
    }
    listExpNames.setToolTipText(toolTip);
  }

  private void init() {
    initComponents();
    btnRemove.setText(AnLocale.getString("Remove"));
    btnRemove.setToolTipText(AnLocale.getString("Remove experiment from the group"));
    btnRemove.addActionListener(this);
    btnAddExp.setText(AnLocale.getString("Add..."));
    btnAddExp.setToolTipText(AnLocale.getString("Add experiment to the group"));
    btnAddExp.addActionListener(this);
    listExpNames.setDragEnabled(true);
    listExpNames.setTransferHandler(new ListTransferHandler());
    listExpNames.addListSelectionListener(
        new ListSelectionListener() {
          // This method is called each time the user changes the set of selected items
          // @Override
          public void valueChanged(ListSelectionEvent evt) {
            // When the user release the mouse button and completes the selection,
            // getValueIsAdjusting() becomes false
            if (!evt.getValueIsAdjusting()) {
              update_btnRemove();
            }
          }
        });
    update_exps_list(null);
    setVisible(true);
    jScrollPane1.setMinimumSize(new Dimension(10, 10));
    listExpNames.setMinimumSize(new Dimension(10, 10));
    //        setBorder(javax.swing.BorderFactory.createTitledBorder(title));
  }

  public void update_exps_list(final List<String> lst) {
    final List<String> listAbsNames = new ArrayList<>();
    final List<String> listBaseNames = new ArrayList<>();
    L_LOOP:
    for (int i = 0, sz = (lst == null) ? 0 : lst.size(); i < sz; i++) {
      String fname = lst.get(i);
      for (int j = 0; j < i; j++) {
        if (fname.compareTo(lst.get(j)) == 0) {
          continue L_LOOP;
        }
      }
      String bname = AnUtility.basename(fname);
      listBaseNames.add(bname);
      listAbsNames.add(fname);
    }
    listAbsPathes = listAbsNames;
    listExpNames.setListData(listBaseNames.toArray());
    update_btnRemove();
  }

  private void update_btnRemove() {
    btnRemove.setEnabled(listExpNames.getSelectedIndices().length != 0);
  }

  /**
   * Action performed
   *
   * @param event
   */
  // @Override
  public void actionPerformed(final ActionEvent event) {
    String cmd = event.getActionCommand();
    if (cmd.equals("btnAddExp")) {
      AnChooser ac =
          window.getAnChooser(AnLocale.getString("Select Experiment"), AnChooser.EXP_CHOOSER, null);
      if (ac.showDialog(this, null) == AnChooser.APPROVE_OPTION) {
        File files[] = ac.getSelectedAnFiles();
        if (files != null) {
          List<String> lst = new ArrayList<>();
          if (listAbsPathes != null) {
            lst.addAll(listAbsPathes);
          }
          for (File f : files) {
            String path = f.getAbsolutePath();
            add_exp_or_grp(lst, path);
          }
          update_exps_list(lst);
        }
      }
    } else if (cmd.equals("btnRemove")) {
      int[] sel = listExpNames.getSelectedIndices();
      if (sel.length > 0) {
        List<String> lst = new ArrayList<>();
        if (listAbsPathes != null) {
          lst.addAll(listAbsPathes);
        }
        for (int i = sel.length - 1; i >= 0; i--) {
          int ind = sel[i];
          if (ind < lst.size()) {
            lst.remove(ind);
          }
        }
        update_exps_list(lst);
      }
    }
  }

  public void add_exp_or_grp(List<String> lst, String path) {
    if ((path == null) || (path.length() == 0)) {
      return;
    }
    List<String> vec = AnUtility.getGroupList(path);
    for (String s : vec) {
      if (s.length() != 0) {
        lst.add(s);
      }
    }
  }

  // Support Drag and Drop, Copy/Paste
  private class ListTransferHandler extends AnTransferHandler {
    private int[] indices = null;

    // Bundle up the selected items in the list
    // as a single string, for export.
    @Override
    protected String exportString(JComponent c) {
      indices = listExpNames.getSelectedIndices();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < indices.length; i++) {
        if (indices[i] < listAbsPathes.size()) {
          if (sb.length() != 0) {
            sb.append("\n");
          }
          sb.append(listAbsPathes.get(indices[i]));
        }
      }
      String str = sb.toString();
      debug("ListTransferHandler::exportString: " + str);
      return str;
    }

    // Take the incoming string and wherever there is a
    // newline, break it into a separate item in the list.
    @Override
    protected void importString(JComponent c, String str) {
      debug("ListTransferHandler::importString: " + str);
      List<String> lst = new ArrayList<>();
      if (listAbsPathes != null) {
        lst.addAll(listAbsPathes);
      }
      if (indices != null) { // Drag & Drop in the same list
        for (int i = 0; i < indices.length; i++) {
          lst.remove(indices[indices.length - 1 - i]);
          lst.add(listAbsPathes.get(indices[i]));
        }
        indices = null;
      } else {
        for (String s = str; true; ) {
          int ind = s.indexOf("\n");
          if (ind == -1) {
            add_exp_or_grp(lst, s);
            break;
          }
          add_exp_or_grp(lst, s.substring(0, ind));
          s = s.substring(ind + 1);
        }
      }
      update_exps_list(lst);
    }

    // If the remove argument is true, the drop has been
    // successful and it's time to remove the selected items
    // from the list. If the remove argument is false, it
    // was a Copy operation and the original list is left
    // intact.
    @Override
    protected void cleanup(JComponent c, boolean remove) {
      debug("ListTransferHandler::cleanup: " + id + " " + remove);
      if (remove && (indices != null)) {
        List<String> lst = new ArrayList<>();
        if (listAbsPathes != null) {
          lst.addAll(listAbsPathes);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
          debug(
              "ListTransferHandler::cleanup: " + id + " " + indices[i] + " " + lst.get(indices[i]));
          lst.remove(indices[i]);
        }
        update_exps_list(lst);
      }
      indices = null;
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

    innerPanel = new javax.swing.JPanel();
    label = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    listExpNames = new javax.swing.JList();
    btnAddExp = new javax.swing.JButton();
    btnRemove = new javax.swing.JButton();

    setPreferredSize(new java.awt.Dimension(800, 150));
    setLayout(new java.awt.GridBagLayout());

    innerPanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    innerPanel.add(label, gridBagConstraints);

    listExpNames.setVisibleRowCount(3);
    jScrollPane1.setViewportView(listExpNames);
    listExpNames.getAccessibleContext().setAccessibleName("Experiments list");
    listExpNames.getAccessibleContext().setAccessibleDescription("Experiments list");
    listExpNames.getAccessibleContext().setAccessibleParent(listExpNames);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridheight = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.ipadx = 384;
    gridBagConstraints.ipady = 71;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 8, 0);
    innerPanel.add(jScrollPane1, gridBagConstraints);

    btnAddExp.setText("Add...");
    btnAddExp.setActionCommand("btnAddExp");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 4);
    innerPanel.add(btnAddExp, gridBagConstraints);

    btnRemove.setText("Remove");
    btnRemove.setActionCommand("btnRemove");
    btnRemove.addActionListener(
        new java.awt.event.ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnRemoveActionPerformed(evt);
          }
        });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 4);
    innerPanel.add(btnRemove, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(innerPanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  private void btnRemoveActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_btnRemoveActionPerformed
    // TODO add your handling code here:
  } // GEN-LAST:event_btnRemoveActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  public javax.swing.JButton btnAddExp;
  public javax.swing.JButton btnRemove;
  private javax.swing.JPanel innerPanel;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JLabel label;
  public javax.swing.JList listExpNames;
  // End of variables declaration//GEN-END:variables
}
