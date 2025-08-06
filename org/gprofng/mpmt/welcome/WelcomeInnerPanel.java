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

package org.gprofng.mpmt.welcome;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnAction;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.ExperimentPickListElement;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box.Filler;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class WelcomeInnerPanel extends javax.swing.JPanel {

  private final int MOST_RECENT_EXP_LIST_SIZE = 9;
  private static final Font font12 = new JLabel().getFont().deriveFont(12f);
  private static final Font bold12Font = font12.deriveFont(Font.BOLD);
  private static final Font plain12Font = font12.deriveFont(Font.PLAIN);
  private static final Font bold16Font = bold12Font.deriveFont(16f);
  private static final Font bold17Font = bold12Font.deriveFont(17f);
  private static final Font bold18Font = bold12Font.deriveFont(18f);
  private static final Font bold22Font = bold12Font.deriveFont(22f);
  private static final Font plain16Font = plain12Font.deriveFont(16f);
  private static final Font plain17Font = plain12Font.deriveFont(17f);
  private static final Font plain18Font = plain12Font.deriveFont(18f);
  private static final Font plain19Font = plain12Font.deriveFont(19f);
  private static final Font plain22Font = plain12Font.deriveFont(22f);

  private JPanel leftPanel;
  private JPanel rightPanel;
  private ImageIcon backgroundImageIcon;

  private JLabel createExperimentTitleLabel;
  private JLabel viewExperimentTitleLabel;
  private WelcomeButton profileAppButton;
  private WelcomeButton profileRunningAppButton;
  private WelcomeButton openExistingExperimentButton;
  private WelcomeButton openRecentExperimentButton;
  private List<WelcomeButton> recentExperimentsButtons;
  private WelcomeButton compareExperimentsButton;
  private WelcomeButton connectButton;

  public WelcomeInnerPanel() {
    backgroundImageIcon =
        new ImageIcon(
//          getClass().getResource("/org/gprofng/mpmt/icons/welcomeScreenNoDivider.png"));			
          getClass().getResource("/org/gprofng/mpmt/icons/welcomeBackgroundShadow.png"));
//          getClass().getResource("/org/gprofng/mpmt/icons/welcomeAlternate.png"));
    initComponents();
    buildForeground();
    buildBackground();
  }

  @Override
  public void requestFocus() {
    profileAppButton.requestFocus(); // First button
  }

  protected void enableActions(boolean enable) {
    if (!enable) {
      setConnected(false);
      connectButton.setEnabled(false);
    } else {
      setConnected(Analyzer.getInstance().isConnected());
      connectButton.setEnabled(true);
    }
  }

  private void setConnected(boolean connected) {
    createExperimentTitleLabel.setEnabled(connected);
    viewExperimentTitleLabel.setEnabled(connected);
    profileAppButton.setEnabled(connected);
    profileRunningAppButton.setEnabled(false);
    //        profileKernelButton.setEnabled(connected &&
    // Analyzer.getInstance().isKernelProfilingEnabled());
    openExistingExperimentButton.setEnabled(connected);
    if (openRecentExperimentButton != null) {
      openRecentExperimentButton.setEnabled(connected);
    }
    if (recentExperimentsButtons != null) {
      for (WelcomeButton welcomeButton : recentExperimentsButtons) {
        welcomeButton.setEnabled(connected);
      }
    }
    compareExperimentsButton.setEnabled(connected);
  }

  private void buildBackground() {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    JLabel iconLabel = new JLabel();
    AnUtility.setAccessibleContext(iconLabel.getAccessibleContext(), " ");
    iconLabel.setIcon(backgroundImageIcon);
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.gridheight = 1;
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    add(iconLabel, gridBagConstraints);
  }

  private void buildForeground() {
    GridBagConstraints gridBagConstraints;

    JPanel foregroundPanel = new JPanel();
    Dimension dim =
        new Dimension(backgroundImageIcon.getIconWidth(), backgroundImageIcon.getIconHeight());
    foregroundPanel.setMinimumSize(dim);
    foregroundPanel.setPreferredSize(dim);
    foregroundPanel.setMaximumSize(dim);
    foregroundPanel.setOpaque(false);
    foregroundPanel.setLayout(new GridBagLayout());

    buildLabelsAndPanels(foregroundPanel);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    add(foregroundPanel, gridBagConstraints);
  }

  private void buildLabelsAndPanels(JPanel foregroundPanel) {
    GridBagConstraints gridBagConstraints;
    JPanel topLabelPanel = new JPanel();
    topLabelPanel.setLayout(new GridBagLayout());
    topLabelPanel.setOpaque(false);

    JLabel label = new JLabel(" ");
    AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
    label.setForeground(AnEnvironment.WELCOME_MAIN_LABEL_COLOR);
    label.setFont(bold16Font);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    topLabelPanel.add(label, gridBagConstraints);

    JLabel label2 = new JLabel(" ");
    AnUtility.setAccessibleContext(label2.getAccessibleContext(), label2.getText());
    label2.setForeground(AnEnvironment.WELCOME_MAIN_LABEL_COLOR);
    label2.setFont(plain16Font);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    topLabelPanel.add(label2, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.insets = new Insets(60, 200, 0, 0);
    foregroundPanel.add(topLabelPanel, gridBagConstraints);

    JLabel label3 = new JLabel(AnLocale.getString("Welcome"));
    AnUtility.setAccessibleContext(label3.getAccessibleContext(), label3.getText());
    label3.setForeground(AnEnvironment.WELCOME_MAIN_LABEL_COLOR);
    label3.setFont(bold22Font);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(20, 75, 0, 0);
    foregroundPanel.add(label3, gridBagConstraints);

    leftPanel = new JPanel();
    leftPanel.setLayout(new GridBagLayout());
    buildLeftPanel(leftPanel);
    leftPanel.setOpaque(false);
    //        leftPanel.setBackground(Color.red);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(16, 122, 0, 0);
    foregroundPanel.add(leftPanel, gridBagConstraints);

    rightPanel = new JPanel();
    rightPanel.setLayout(new GridBagLayout());
    buildRightPanel(rightPanel);
    rightPanel.setOpaque(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(16, 40, 0, 0);
    foregroundPanel.add(rightPanel, gridBagConstraints);
  }

  private void buildLeftPanel(JPanel panel) {
    AnAction action;
    panel.removeAll();
    int gridy = 0;

    createExperimentTitleLabel =
        addSectionTitleLabel(panel, AnLocale.getString("Create Experiments"), gridy++);

    action = AnWindow.getInstance().getProfileApplicationAction();
    profileAppButton =
        addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);

    action = AnWindow.getInstance().getProfileRunningProcessAction();
    profileRunningAppButton =
        addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);

    addSpace(panel, 14, gridy++);
    viewExperimentTitleLabel =
        addSectionTitleLabel(panel, AnLocale.getString("View Experiments"), gridy++);

    action = AnWindow.getInstance().getOpenExperimentAction();
    openExistingExperimentButton =
        addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);

    List<ExperimentPickListElement> recentExperiments =
        UserPref.getInstance()
            .getExperimentsPicklists()
            .getPicklist()
            .getRecentExperiments(MOST_RECENT_EXP_LIST_SIZE);
    if (!recentExperiments.isEmpty()) {
      String buttonText = AnLocale.getString("Open Recent Experiment");
      String buttonTT = AnLocale.getString("Click an experiment name below to reopen it");
      openRecentExperimentButton = addActionButton(panel, gridy++, buttonText, buttonTT, null);
      if (Analyzer.getInstance().isConnected()) {
        recentExperimentsButtons = new ArrayList<>();
        for (ExperimentPickListElement experimentElement : recentExperiments) {  
	  String displayName = AnUtility.basename(experimentElement.getPath());
          int length = displayName.length();
          if (length > 25) {
		  // 16
            displayName = displayName.substring(0, 54) + "..." + displayName.substring(length - 7);
          }
          recentExperimentsButtons.add(
              addExperimentButton(
                  panel,
                  gridy++,
                  displayName,
                  experimentElement.getToolTip(),
                  new OpenExistingExperimentAction(experimentElement)));
        }
      }
    }

    action = AnWindow.getInstance().getCompareExperimentsAction();
    compareExperimentsButton =
        addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);

    addSpace(panel, 14, gridy++);
	
    addSectionTitleLabel(panel, AnLocale.getString("Remote"), gridy++);
    action = AnWindow.getInstance().getConnectAction();
    connectButton =
        addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);

    addButtomFill(panel, gridy++);
    addRightFill(panel);
  }

  private void buildRightPanel(JPanel panel) {
    AnAction action;
    int gridy = 0;

    /*
    addSectionTitleLabel(panel, AnLocale.getString("Remote"), gridy++);
    action = AnWindow.getInstance().getConnectAction();
    connectButton =
        addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);
    */

    addSpace(panel, 14, gridy++);

    /*
    addSectionTitleLabel(panel, AnLocale.getString("Learn More"), gridy++);

    action = AnWindow.getInstance().getHelpAnalyzerAction();
    addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);
    action = AnWindow.getInstance().getHelpNewFeaturesAction();
    addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);
    action = AnWindow.getInstance().getHelpInformationMapAction();
    addActionButton(panel, gridy++, action.getTextNoDots(), action.getTooltipText(), action);
    */
    addButtomFill(panel, gridy++);
  }

  /*
   * Picklist changes
   */
  public void doCompute() {
    //        System.out.println("WelcomeInnerPanel doCompute ********************");
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            buildLeftPanel(leftPanel);
            setConnected(Analyzer.getInstance().isConnected());
            validate();
            repaint();
          }
        });
  }

  private void addSpace(JPanel panel, int space, int gridy) {
    Dimension dim = new Dimension(space, space);
    Filler filler = new Filler(dim, dim, dim);
    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
    panel.add(filler, gridBagConstraints);
  }

  private JLabel addSectionTitleLabel(JPanel panel, String title, int gridy) {
    JLabel label = new JLabel(title);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
    label.setFont(bold18Font);
    label.setForeground(AnEnvironment.WELCOME_MAIN_LABEL_COLOR);
    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    panel.add(label, gridBagConstraints);
    return label;
  }

  private WelcomeButton addActionButton(
      JPanel panel, int gridy, String labelTxt, String labelTT, Action action) {
    WelcomeButton button =
        new WelcomeButton(
            labelTxt, labelTT, action, 5, AnEnvironment.WELCOME_BUTTON_ACTION_COLOR, Font.BOLD);

    JLabel label = new JLabel();
    label.setIcon(new ImageIcon(getClass().getResource("/org/gprofng/mpmt/icons/tab.png")));
    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    panel.add(label, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    panel.add(button, gridBagConstraints);
    return button;
  }

  private WelcomeButton addExperimentButton(
      JPanel panel, int gridy, String labelTxt, String labelTT, Action action) {
    WelcomeButton button =
        new WelcomeButton(
            labelTxt, labelTT, action, 4, AnEnvironment.WELCOME_BUTTON_EXPERIMENT_COLOR, Font.BOLD);

    JLabel label = new JLabel();
    if (labelTxt.endsWith("erg")) {
      label.setIcon(
          new ImageIcon(getClass().getResource("/org/gprofng/mpmt/icons/expgroup.png")));
    } else {
      label.setIcon(
          new ImageIcon(getClass().getResource("/org/gprofng/mpmt/icons/experiment.png")));
    }
    AnUtility.setAccessibleContext(label.getAccessibleContext(), labelTxt);
    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 0);
    panel.add(label, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 0);
    panel.add(button, gridBagConstraints);

    return button;
  }

  private void addButtomFill(JPanel panel, int gridy) {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = gridy;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    panel.add(new JLabel(), gridBagConstraints);
  }

  private void addRightFill(JPanel panel) {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 100;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.insets = new Insets(0, 0, 0, 300);
    Dimension dim = new Dimension(0, 0);
    Filler filler = new Filler(dim, dim, dim);
    panel.add(filler, gridBagConstraints);
  }

  class OpenExperimentAction implements ActionListener {

    // @Override
    @Override
    public void actionPerformed(ActionEvent e) {
      AnWindow.getInstance().openExperimentAction();
    }
  }

  class ConnectRemoteHostAction implements ActionListener {

    // @Override
    @Override
    public void actionPerformed(ActionEvent e) {
      AnWindow.getInstance().connectAction();
    }
  }

  class CompareExperimentsAction implements ActionListener {

    // @Override
    @Override
    public void actionPerformed(ActionEvent e) {
      AnWindow.getInstance().compareExperimentsAction();
    }
  }

  class OpenExistingExperimentAction extends AbstractAction {

    private ExperimentPickListElement experimentElement;

    public OpenExistingExperimentAction(ExperimentPickListElement experimentElement) {
      this.experimentElement = experimentElement;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      ArrayList<String> list = new ArrayList<>();
      list.add(experimentElement.getPath());
      String confPath;
      boolean always;
      if (experimentElement.getConfPath() != null) {
        confPath = experimentElement.getConfPath();
        always = true;
      } else {
        confPath = UserPref.getAsWhenClosedConfigPath(experimentElement.getPath());
        always = false;
      }
      AnWindow.getInstance()
          .loadExperimentList(
              list, false, experimentElement.getWorkingDirectory(), true, confPath, always);
    }
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    setLayout(new java.awt.GridBagLayout());
  } // </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
}
