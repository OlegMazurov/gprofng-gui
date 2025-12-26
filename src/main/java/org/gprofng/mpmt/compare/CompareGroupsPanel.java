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

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.ActionTextField;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class CompareGroupsPanel extends javax.swing.JPanel {

  private JLabel[] labels;
  private ActionTextField[] textFields;

  public CompareGroupsPanel() {
    String[][] groups = AnWindow.getInstance().getExperimentGroups();
    initComponents();
    setLayout(new GridBagLayout());
    //        setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1,
    // AnEnvironment.FILTER_STATUS_BORDER_COLOR));
    setBackground(AnEnvironment.FILTER_TOOLBAR_BACKGROUND1_COLOR);
    if (groups == null) {
      return;
    }
    if (groups.length == 1) {
      return;
    }

    labels = new JLabel[groups.length];
    textFields = new ActionTextField[groups.length];
    for (int n = 0; n < groups.length; n++) {
      String groupText = "";
      String groupTTText = "";
      GridBagConstraints gridBagConstraints;
      if (n == 0) {
        labels[n] = new JLabel(AnLocale.getString("Baseline:"));
        groupTTText = AnLocale.getString("Comparison Baseline: ");
      } else {
        String text;
        String ttText;

        if (AnWindow.getInstance().isExperimentGroupsSimple()) {
          text = AnLocale.getString("Exp.");
          ttText = AnLocale.getString("Comparison Experiment");
        } else {
          text = AnLocale.getString("Group");
          ttText = AnLocale.getString("Comparison Group");
        }
        labels[n] = new JLabel(text + " " + n + ":");
        groupTTText = ttText + " " + n + ": ";
      }
      for (int i = 0; i < groups[n].length; i++) {
        if (i > 0) {
          groupText = groupText + ", ";
          groupTTText = groupTTText + ", ";
        }
        groupText = groupText + AnUtility.basename(groups[n][i]);
        groupTTText = groupTTText + groups[n][i];
      }
      labels[n].setToolTipText(groupTTText);
      textFields[n] = new ActionTextField(groupText);
      textFields[n].setEditable(false);
      textFields[n].setToolTipText(groupTTText);
      textFields[n].setBorder(
          BorderFactory.createLineBorder(AnEnvironment.FILTER_TOOLBAR_BACKGROUND2_COLOR, 1));
      textFields[n].setBackground(new Color(243, 243, 243));
      textFields[n].addActionListener(new TextFieldRemoveAction(n));
      textFields[n].setActionToolTipText(AnLocale.getString("Remove this experiment/group"));

      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = n;
      gridBagConstraints.anchor = GridBagConstraints.WEST;
      gridBagConstraints.insets = new Insets(0, 2, 2, 2);
      add(labels[n], gridBagConstraints);

      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = n;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.insets = new Insets(2, 2, 2, 2);
      add(textFields[n], gridBagConstraints);
    }
    setBorder(
        BorderFactory.createMatteBorder(0, 1, 1, 1, AnEnvironment.FILTER_STATUS_BORDER_COLOR));
  }

  class TextFieldRemoveAction implements ActionListener {

    private int groupNo;

    public TextFieldRemoveAction(int groupNo) {
      this.groupNo = groupNo;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

      AnWindow anWindow = AnWindow.getInstance();
      String groups[][] = anWindow.getExperimentGroups();
      int nGroups = groups.length;

      String newGroups[][] = new String[nGroups - 1][];
      int index = 0;
      for (int i = 0; i < nGroups; i++) {
        if (i == groupNo) {
          i++; // Skip the deleted one
          if (i == nGroups) {
            break;
          }
        }
        newGroups[index++] = groups[i];
      }
      anWindow.loadExperimentGroups(newGroups, null, !anWindow.experimentsLoaded(), null, false);
    }
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout
            .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE));
    layout.setVerticalGroup(
        layout
            .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE));
  } // </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
}
