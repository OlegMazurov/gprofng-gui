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

package org.gprofng.mpmt.compare;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnChooser;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.ExperimentPickListElement;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnGradientPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.MouseInputAdapter;

/**
 * @author tpreisle
 */
public class CompareSimplePanel extends javax.swing.JPanel {
  protected static final int MAX_GROUPS = 20;
  private CompareSimpleDialog compareSimpleDialog;
  private int gridy;

  private JPanel buttonPanel;
  private JButton addExperimentChooserButton;
  private JButton reverseButton;
  private int noExperimentChoosers;

  private ExperimentComboBox[] experimentChooserComboBoxes;
  private String[] experimentChooserLabelTexts;
  private char[] experimentChooserLabelMN;

  public CompareSimplePanel(CompareSimpleDialog compareSimpleDialog) {
    this.compareSimpleDialog = compareSimpleDialog;
    initComponents();
    setLayout(new GridBagLayout());
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    experimentChooserComboBoxes = new ExperimentComboBox[MAX_GROUPS];
    experimentChooserLabelTexts = new String[MAX_GROUPS];
    experimentChooserLabelMN = new char[MAX_GROUPS];

    for (int i = 0; i < MAX_GROUPS; i++) {
      experimentChooserComboBoxes[i] = new ExperimentComboBox();
      if (i == 0) {
        experimentChooserLabelTexts[i] = AnLocale.getString("Baseline Experiment:");
        experimentChooserLabelMN[i] =
            AnLocale.getString('B', "CompareSimpleDialog_BaselineExperiment");
      } else {
        experimentChooserLabelTexts[i] =
            AnLocale.getString("Comparison Experiment") + " " + i + ":";
        if (i < 10) {
          experimentChooserLabelMN[i] = ("" + i).charAt(0);
        }
      }
    }

    gridy = 0;
    addGuidanceText();
    addButtonPanel();

    noExperimentChoosers = 0;

    String[][] groups = AnWindow.getInstance().getExperimentGroups();
    addExperimentChooser();
    addExperimentChooser();
    for (int i = 2; i < groups.length; i++) {
      addExperimentChooser();
    }

    if (groups.length >= 1) {
      experimentChooserComboBoxes[0].setSelectedItem(groups[0][0]);
    }
    if (groups.length >= 2) {
      experimentChooserComboBoxes[1].setSelectedItem(groups[1][0]);
    }

    for (int i = 2; i < groups.length; i++) {
      experimentChooserComboBoxes[i].setSelectedItem(groups[i][0]);
    }
    addFiller();
  }

  private void addButtonPanel() {
    GridBagConstraints gridBagConstraints;

    buttonPanel =
        new AnGradientPanel(
            AnEnvironment.FILTER_TOOLBAR_BACKGROUND1_COLOR,
            AnEnvironment.FILTER_TOOLBAR_BACKGROUND2_COLOR);
    buttonPanel.setLayout(new GridBagLayout());

    addExperimentChooserButton = new JButton();
    addExperimentChooserButton.setText(AnLocale.getString("More"));
    addExperimentChooserButton.setIcon(AnUtility.moreIcon);
    addExperimentChooserButton.setBorderPainted(false);
    addExperimentChooserButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
    addExperimentChooserButton.setOpaque(false);
    addExperimentChooserButton.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    addExperimentChooserButton.setToolTipText(AnLocale.getString("Add a Comparison Experiment"));
    addExperimentChooserButton.setMnemonic(AnLocale.getString('M', "CompareSimpleDialog_More"));
    addExperimentChooserButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            addExperimentChooser();
            compareSimpleDialog.pack();
          }
        });
    addExperimentChooserButton.addMouseListener(
        new MouseInputAdapter() {
          @Override
          public void mouseEntered(MouseEvent evt) {
            JButton btn = (JButton) evt.getSource();
            if (btn.isEnabled()) {
              btn.setBorderPainted(true);
            }
          }

          @Override
          public void mouseExited(MouseEvent evt) {
            JButton btn = (JButton) evt.getSource();
            if (btn.isEnabled()) {
              btn.setBorderPainted(false);
            }
          }
        });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 2, 2, 0);
    buttonPanel.add(addExperimentChooserButton, gridBagConstraints);

    reverseButton = new JButton();
    reverseButton.setText(AnLocale.getString("Reverse"));
    reverseButton.setIcon(AnUtility.compareReverseIcon);
    reverseButton.setBorderPainted(false);
    reverseButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
    reverseButton.setOpaque(false);
    reverseButton.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    reverseButton.setToolTipText(AnLocale.getString("Reverse comparison order"));
    reverseButton.setMnemonic(AnLocale.getString('R', "CompareSimpleDialog_ReverseButton"));
    reverseButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String[] selected = new String[MAX_GROUPS];
            for (int i = 0; i < MAX_GROUPS; i++) {
              selected[i] = (String) experimentChooserComboBoxes[i].getSelectedItem();
            }
            for (int i = 0; i < noExperimentChoosers; i++) {
              experimentChooserComboBoxes[i].setSelectedItem(
                  selected[noExperimentChoosers - i - 1]);
            }
          }
        });
    reverseButton.addMouseListener(
        new MouseInputAdapter() {
          @Override
          public void mouseEntered(MouseEvent evt) {
            JButton btn = (JButton) evt.getSource();
            if (btn.isEnabled()) {
              btn.setBorderPainted(true);
            }
          }

          @Override
          public void mouseExited(MouseEvent evt) {
            JButton btn = (JButton) evt.getSource();
            if (btn.isEnabled()) {
              btn.setBorderPainted(false);
            }
          }
        });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 0, 2, 2);
    buttonPanel.add(reverseButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.insets = new Insets(5, 0, 0, 0);
    add(buttonPanel, gridBagConstraints);
  }

  private void addExperimentChooser() {
    JLabel label = new JLabel();
    AnUtility.setTextAndAccessibleContext(label, experimentChooserLabelTexts[noExperimentChoosers]);
    addExperimentChooserGUI(label, experimentChooserComboBoxes[noExperimentChoosers]);
    label.setLabelFor(experimentChooserComboBoxes[noExperimentChoosers]);
    label.setDisplayedMnemonic(experimentChooserLabelMN[noExperimentChoosers]);
    noExperimentChoosers++;
    updateState();
  }

  private void updateState() {
    addExperimentChooserButton.setEnabled(noExperimentChoosers < MAX_GROUPS);
  }

  private void addFiller() {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 20;
    gridBagConstraints.gridy = 20;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 0, 16, 0);
    add(new JLabel(), gridBagConstraints);
  }

  private void addGuidanceText() {
    JTextArea textArea = new JTextArea();
    AnUtility.setAccessibleContext(
        textArea.getAccessibleContext(), AnLocale.getString("Infomation area"));
    textArea.setEditable(false);
    textArea.setBorder(null);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    textArea.setText(
        AnLocale.getString(
            "Comparing experiments. The Comparison experiment(s) are compared against the Baseline"
                + " experiment. Most data views support comparing experiments. When you compare"
                + " experiments, gprofng GUI displays data from the experiments or groups"
                + " in adjacent columns. The Advanced option allows grouping (aggregating) of"
                + " experiments before comparing them."));

    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 0, 6, 0);
    add(textArea, gridBagConstraints);
  }

  private void addExperimentChooserGUI(JLabel label, final ExperimentComboBox combobox) {
    GridBagConstraints gridBagConstraints;
    combobox.setPreferredSize(new Dimension(600, combobox.getPreferredSize().height));

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.insets = new Insets(10, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    add(label, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    add(combobox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.gridx = 1;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    JButton selectExperimentButton = new JButton("...");
    selectExperimentButton.setToolTipText(AnLocale.getString("Browse for experiment..."));
    selectExperimentButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnChooser ac =
                AnWindow.getInstance()
                    .getAnChooser(
                        AnLocale.getString("Select Experiment"), AnChooser.EXP_CHOOSER, null);
            //                ac.setMultiSelectionEnabled(false); // Doesn't work ?????
            if (ac.showDialog(AnWindow.getInstance().getFrame(), null)
                == AnChooser.APPROVE_OPTION) {
              File file = ac.getSelectedAnFile();
              String path = file.getAbsolutePath();
              combobox.setSelectedItem(path);
            }
          }
        });
    add(selectExperimentButton, gridBagConstraints);
  }

  public String[][] getExperimentGroups() {
    List<String> experiments = new ArrayList<String>();
    for (int i = 0; i < noExperimentChoosers; i++) {
      String experimentPath = (String) experimentChooserComboBoxes[i].getSelectedItem();
      if (experimentPath != null && experimentPath.length() > 0) {
        experiments.add(experimentPath.trim());
      }
    }

    String[][] groups = new String[experiments.size()][];
    for (int i = 0; i < experiments.size(); i++) {
      groups[i] = new String[1];
      groups[i][0] = experiments.get(i);
    }
    return groups;
  }

  private class ExperimentComboBox extends JComboBox {

    public ExperimentComboBox() {
      setEditable(true);

      addItem("");
      List<ExperimentPickListElement> paths =
          UserPref.getInstance().getExperimentsPicklists().getPicklist().getRecentExperiments(20);
      for (ExperimentPickListElement eple : paths) {
        addItem(eple.getPath());
      }
      setSelectedItem("");
    }

    @Override
    public void setSelectedItem(Object anObject) {
      super.setSelectedItem(
          anObject); // To change body of generated methods, choose Tools | Templates.
      setToolTipText(anObject.toString());
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
