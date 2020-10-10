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
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

public final class CompareAdvancedDialog extends JDialog implements ItemListener, ActionListener {

  private AnWindow window;
  private final Frame frame;
  private final boolean compare_mode;
  private String[][] experimentGrops = null;
  private List<ExperimentGroupsPanel> expGrops;
  private JPanel workPanel;
  private JScrollPane scrollPane;
  private int maxGroups;
  private JPanel warningPanel = null;

  // Constructor
  public CompareAdvancedDialog(
      final AnWindow window, final Frame frame, final boolean compare_mode, String title) {
    super(frame, title, true);
    this.window = window;
    this.frame = frame;
    this.compare_mode = compare_mode;
    maxGroups = compare_mode ? CompareSimplePanel.MAX_GROUPS : 1;

    experimentGrops = window.getExperimentGroups();
    initComponents();
    pack();
    setLocationRelativeTo(frame);

    // close/dispose when ESC
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
    getRootPane()
        .getActionMap()
        .put(
            "esc",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                setVisible(false);
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "F1");
    getRootPane()
        .getActionMap()
        .put(
            "F1",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                showHelp();
              }
            });
  }

  private javax.swing.JButton btnOk;
  private javax.swing.JButton btnCancel;
  private javax.swing.JButton btnAddGrp;
  private javax.swing.JButton btnHelp;

  // Initialize GUI components
  private void initComponents() {
    expGrops = new ArrayList();
    String groupTitle;
    String groupToolTip;
    char mnemonic;
    if (compare_mode) {
      groupTitle = AnLocale.getString("Baseline:");
      groupToolTip = AnLocale.getString("Experiment(s) to be compared aginst");
      mnemonic = AnLocale.getString('B', "CompareDialogBaselineMN");
    } else {
      groupTitle = AnLocale.getString("Experiments:");
      groupToolTip = AnLocale.getString("List of experiments to aggregate");
      mnemonic = AnLocale.getString('E', "CompareDialogExperimentMN");
    }
    expGrops.add(new ExperimentGroupsPanel(window, frame, groupTitle, groupToolTip, mnemonic));
    if (compare_mode) {
      mnemonic = AnLocale.getString('C', "CompareDialogComparisonGroupMN");
      expGrops.add(
          new ExperimentGroupsPanel(
              window,
              frame,
              AnLocale.getString("Comparison Group:"),
              AnLocale.getString("Exeriment(s) to compare against baseline experiments(s)"),
              mnemonic));
    } else if (experimentGrops != null && experimentGrops.length > 1) {
      List<String> lst = new ArrayList();
      for (String[] arr : experimentGrops) {
        for (String s : arr) {
          lst.add(s);
        }
      }
      experimentGrops = new String[1][];
      experimentGrops[0] = (String[]) lst.toArray(new String[lst.size()]);
    }
    if (experimentGrops != null) {
      String fmt = AnLocale.getString("Comparison Group %d");
      String toolTip = AnLocale.getString("Exeriment(s) to compare against baseline experiment(s)");
      for (int i = 2; i < experimentGrops.length; i++) {
        expGrops.add(new ExperimentGroupsPanel(window, frame, String.format(fmt, i), toolTip, ' '));
        //                expGrops.add(new ExpGrpJPanel(window, frame, String.format(fmt)));
      }
      for (int i = 0; i < experimentGrops.length; i++) {
        ExperimentGroupsPanel grp = expGrops.get(i);
        String[] names = experimentGrops[i];
        List<String> lst = new ArrayList<String>();
        for (String s : names) {
          grp.add_exp_or_grp(lst, s);
        }
        grp.update_exps_list(lst);
      }
    }

    workPanel = new JPanel();
    workPanel.setLayout(new BoxLayout(workPanel, BoxLayout.Y_AXIS));
    for (ExperimentGroupsPanel pnl : expGrops) {
      workPanel.add(pnl);
    }

    btnOk = newButton(AnLocale.getString("OK"), "btnOk");
    btnCancel = newButton(AnLocale.getString("Cancel"), "btnCancel");
    btnAddGrp = newButton(AnLocale.getString("Add More"), "btnAddGrp");
    btnAddGrp.setToolTipText(AnLocale.getString("Add a group"));
    btnHelp = newButton(AnLocale.getString("Help"), "btnHelp");
    btnHelp.setMnemonic(AnLocale.getString('H', "CompareDialogHelpButtonMN"));

    getRootPane().setDefaultButton(btnOk); // default action (Return key)

    JPanel btnPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();

    final JCheckBox showWarningCheckBox = new JCheckBox(AnLocale.getString("Show warning"));
    AnUtility.setAccessibleContext(
        showWarningCheckBox.getAccessibleContext(), showWarningCheckBox.getText());
    showWarningCheckBox.setMnemonic(AnLocale.getString('S', "CompareDialogShowWarningButtonMN"));
    showWarningCheckBox.setSelected(UserPref.getInstance().showCompareSourceWarning());
    showWarningCheckBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            UserPref.getInstance().setShowCompareSourceWarning(showWarningCheckBox.isSelected());
            warningPanel.setVisible(showWarningCheckBox.isSelected());
            pack();
          }
        });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    btnPanel.add(showWarningCheckBox, gridBagConstraints);

    JLabel warningIconLabel = new JLabel();
    warningIconLabel.setIcon(AnUtility.warningIcon);
    JTextArea textArea =
        new JTextArea(
            AnLocale.getString(
                "To compare sources in experiments, first archive the sources in each experiment"
                    + " using the command \"er_archive -s all <experiment-name>\". See er_archive"
                    + " man page for details."));
    textArea.getAccessibleContext().setAccessibleName(AnLocale.getString("Text Area"));
    textArea.getAccessibleContext().setAccessibleDescription(AnLocale.getString("Text Area"));
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setEditable(false);
    //        textArea.setRows(4);
    textArea.setOpaque(false);
    warningPanel = new JPanel();
    warningPanel.setBorder(BorderFactory.createLineBorder(AnEnvironment.ABOUT_BOX_BORDER_COLOR));
    warningPanel.setLayout(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    warningPanel.add(warningIconLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 0);
    warningPanel.add(textArea, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.insets = new Insets(4, 8, 0, 8);
    btnPanel.add(warningPanel, gridBagConstraints);
    warningPanel.setVisible(showWarningCheckBox.isSelected());

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    // gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(12, 8, 8, 0);
    btnPanel.add(btnAddGrp, gridBagConstraints);
    btnAddGrp.setVisible(expGrops.size() < maxGroups);
    // btnAddGrp.setEnabled(expGrops.size() < Max_Exp_Groups);

    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    btnPanel.add(new JLabel(""), gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 8, 0);
    btnPanel.add(btnOk, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(12, 6, 8, 0);
    btnPanel.add(btnCancel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(12, 6, 8, 8);
    btnPanel.add(btnHelp, gridBagConstraints);

    scrollPane = new AnJScrollPane(workPanel);
    scrollPane.setBorder(new EmptyBorder(0, 4, 0, 4));
    scrollPane.setPreferredSize(new Dimension(AnVariable.WIN_SIZE));

    final Container cnt = getContentPane();
    cnt.setLayout(new BorderLayout());
    cnt.add(scrollPane, BorderLayout.CENTER);
    cnt.add(btnPanel, BorderLayout.SOUTH);
    pack();
  }

  private JButton newButton(String txt, String cmd) {
    JButton btn = new JButton();
    btn.setText(txt);
    AnUtility.setAccessibleContext(btn.getAccessibleContext(), txt);
    btn.setActionCommand(cmd);
    btn.addActionListener(this);
    return btn;
  }

  // Set visible
  public void setVisible(final boolean set, final int active_id) {
    CompareAdvancedDialog.super.setVisible(set);
    //        SwingUtilities.invokeLater(new Runnable() {
    //            @Override
    //            public void run() {
    //                CompareDialog.super.setVisible(set);
    //            }
    //        });
    //        if (UserPref.getInstance().showCompareSourceWarning()) {
    //            showSourceWarningDialog();
    //        }
  }

  /** Listener to switch between file/printer */
  @Override
  public void itemStateChanged(final ItemEvent event) {
    //    final Object source = event.getItemSelectable();
  }

  /**
   * Action performed
   *
   * @param event
   */
  // @Override
  public void actionPerformed(final ActionEvent event) {
    String cmd = event.getActionCommand();
    if (cmd.equals("btnAddGrp")) {
      int sz = expGrops.size();
      String nm =
          sz > 1
              ? String.format(AnLocale.getString("Comparison Group %d:"), sz)
              : //                String.format(AnLocale.getString("And Compare Baseline to:")) :
              AnLocale.getString("Compare Group:");
      ExperimentGroupsPanel pnl =
          new ExperimentGroupsPanel(
              window, frame, nm, "Experiment(s) to compare against baseline experiment(s)", ' ');
      expGrops.add(pnl);
      int ind = workPanel.getComponentZOrder(expGrops.get(sz - 1));
      workPanel.add(pnl, ind + 1);
      btnAddGrp.setVisible(expGrops.size() < maxGroups);
      pack();
    } else if (cmd.equals("btnOk")) {
      setExperimentsGroups();
      setVisible(false);
    } else if (cmd.equals("btnCancel")) {
      setVisible(false);
    } else if (cmd.equals("btnHelp")) {
      showHelp();
    }
  }

  private void showHelp() {
    if (compare_mode) {
      Analyzer.showHelp(AnVariable.HELP_Compare);
    } else {
      Analyzer.showHelp(AnVariable.HELP_Aggregate);
    }
  }

  private void setExperimentsGroups() {
    int cnt = 0;
    for (ExperimentGroupsPanel expGroup : expGrops) {
      if (!expGroup.listAbsPathes.isEmpty()) {
        cnt++;
      }
    }
    String[][] arr = new String[cnt][];
    cnt = 0;
    for (ExperimentGroupsPanel expGroup : expGrops) {
      List<String> lst = expGroup.listAbsPathes;
      if (!lst.isEmpty()) {
        arr[cnt] = lst.toArray(new String[lst.size()]);
        cnt++;
      }
    }
    window.loadExperimentGroups(arr, null, !window.experimentsLoaded(), null, false);
  }
}
