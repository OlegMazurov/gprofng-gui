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


package org.gprofng.mpmt.overview;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.analyzer.AnLog;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.metrics.IPCMetricsAPI;
import org.gprofng.mpmt.metrics.MetricAttr;
import org.gprofng.mpmt.metrics.MetricNode;
import org.gprofng.mpmt.metrics.MetricValue;
import org.gprofng.mpmt.metrics.MetricsGUI;
import org.gprofng.mpmt.metrics.SelectableMetricNode;
import org.gprofng.mpmt.metrics.ValueMetricNode;
import org.gprofng.mpmt.settings.MetricAttributes;
import org.gprofng.mpmt.settings.MetricState;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnSplitPane;
import org.gprofng.mpmt.util.gui.AnSplitPaneFixedRightSize;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.gui.ToolTipPopup;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class OverviewPanel extends javax.swing.JPanel {

  private static OverviewPanel instance = null;
  protected static final Color borderColor = AnEnvironment.SPLIT_PANE_BORDER_COLOR;
  private final AnWindow window;
  private OverviewView overviewDisp;
  private OuterPanel statusOuterPanel = null;
  private TurnerPanel statusTurnerPanel = null;
  private JLabel statusTopLabel = null;
  private JTextPane statusBottomTextPane = null;
  private JScrollPane statusBottomScrollPane = null;
  private ProgressBarPanel metricsProgressBarPanel;
  private OuterPanel metricsOuterPanel = null;
  private JPanel metricsOuterPanel2 = null;
  //    private ProgressBarPanel viewsProgressBarPanel;
  //    private JPanel viewsTurnerPanel = null;
  //    private OuterPanel viewsOuterPanel = null;
  //    private MetricNode metricsRootNode = null;
  private ToolTipPopup previewToolTipPopup = null;
  private boolean previewToolTipPopupNew = true;
  private JPanel outerScrollPanePanel;
  private AnSplitPane splitPane;
  private JPanel previewPanel;

  /** Creates new form WelcomePanel */
  public OverviewPanel(final AnWindow window, OverviewView overviewDisp) {
    instance = this;
    this.window = window;
    this.overviewDisp = overviewDisp;

    initComponents();
    statusLabel.setText(AnLocale.getString("Experiment(s)"));
    statusLabel.setDisplayedMnemonic(AnLocale.getString('E', "MN_Overview_Experiments"));
    AnUtility.setAccessibleContext(statusLabel.getAccessibleContext(), statusLabel.getText());
    metricsLabel.setText(AnLocale.getString("Metrics"));
    //        metricsLabel.setDisplayedMnemonic(AnLocale.getString('s', "MN_Overview_Metrics"));
    AnUtility.setAccessibleContext(metricsLabel.getAccessibleContext(), metricsLabel.getText());
    viewsLabel.setText(AnLocale.getString("Shown Metrics"));
    AnUtility.setAccessibleContext(viewsLabel.getAccessibleContext(), viewsLabel.getText());

    statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
    metricsLabel.setFont(metricsLabel.getFont().deriveFont(Font.BOLD));
    innerPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    metricsPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    viewsPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    setBackground(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);

    // Status
    statusTopLabel = new JLabel();
    AnUtility.setAccessibleContext(
        statusTopLabel.getAccessibleContext(), AnLocale.getString("Status"));
    statusTopLabel.setOpaque(false);
    statusBottomTextPane = new JTextPane();
    statusBottomTextPane.setBackground(Color.WHITE);
    statusBottomTextPane.setEditable(false);
    statusBottomScrollPane = new AnJScrollPane();
    statusBottomScrollPane.setHorizontalScrollBarPolicy(
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    statusBottomScrollPane.setVerticalScrollBarPolicy(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    statusBottomScrollPane.setPreferredSize(new Dimension(100, 80));
    statusBottomScrollPane.setViewportView(statusBottomTextPane);
    statusBottomScrollPane.setBorder(null);
    statusTurnerPanel =
        new TurnerPanel(
            statusTopLabel, statusBottomScrollPane, 4, true, false, Overview.STATUS_BACKGROUND);
    statusLabel.setLabelFor(statusTurnerPanel.getTurnerButton());
    statusOuterPanel = new OuterPanel();
    statusOuterPanel.add(statusTurnerPanel, BorderLayout.CENTER);
    statusPanel.add(statusOuterPanel);
    statusPanel.setBorder(new LineBorder(borderColor));
    statusTurnerPanel.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            Dimension dim = statusPanel.getPreferredSize();
            //                System.out.println("statusPanel: " + dim);
            statusPanel.setMinimumSize(dim);
            innerPanel.validate();
            innerPanel.repaint();
          }
        });

    // Metrics
    metricsOuterPanel = new OuterPanel();
    metricsProgressBarPanel = new ProgressBarPanel();
    metricsOuterPanel.add(metricsProgressBarPanel, BorderLayout.WEST);
    metricsPanel.add(metricsOuterPanel, BorderLayout.CENTER);
    metricsPanel.setBorder(new LineBorder(borderColor));

    // Views
    //        viewsOuterPanel = new OuterPanel();
    //        viewsProgressBarPanel = new ProgressBarPanel();
    //        viewsOuterPanel.add(viewsProgressBarPanel, BorderLayout.WEST);
    //        viewsPanel.add(viewsOuterPanel, BorderLayout.CENTER);
    //        viewsPanel.setBorder(new LineBorder(borderColor));
    //
    // FIXUP: make Views tables invisible for now
    viewsLabel.setVisible(false);
    viewsPanel.setVisible(false);
  }

  @Override
  public void requestFocus() {
    statusTurnerPanel
        .getTurnerButton()
        .requestFocus(); // To change body of generated methods, choose Tools | Templates.
  }

  private void constructMetricsPanel() {
    CheckBoxBarPanel.resetCheckBoxBarPanelList();

    if (metricsOuterPanel2 != null) {
      metricsOuterPanel.remove(metricsOuterPanel2);
    }

    metricsOuterPanel2 = new JPanel();
    metricsOuterPanel2.setBackground(Color.WHITE);
    metricsOuterPanel2.setLayout(new BorderLayout());

    JPanel metricsOuterPanel2a = new JPanel();
    metricsOuterPanel2a.setBackground(Color.WHITE);
    metricsOuterPanel2a.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    int gridy = 0;

    String infoTxt =
        AnLocale.getString(
            "Select the metrics to display in the data views, then click a data view in the"
                + " navigation panel on the left.");
    JLabel infoLabel = new JLabel(infoTxt);
    AnUtility.setAccessibleContext(infoLabel.getAccessibleContext(), infoLabel.getText());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 4, 0, 4);
    metricsOuterPanel2a.add(infoLabel, gridBagConstraints);

    MetricsGUI metricsGUI =
        new MetricsGUI(
            window.getSettings().getMetricsSetting().getMetricStates().getMetricsRootNode(),
            window);

    if (MetricsGUI.SHOW_ATTRIBUTES_IN_OVERVIEW) {
      JPanel buttonPanel = metricsGUI.createButtonPanel();
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridy = gridy++;
      gridBagConstraints.insets = new Insets(12, 4, 0, 4);
      gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
      gridBagConstraints.weightx = 1.0;
      metricsOuterPanel2a.add(buttonPanel, gridBagConstraints);
    }

    JPanel metricsBarPanel = metricsGUI.createAvailableMetricsPanel();
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(12, 4, 0, 4);
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.gridy = gridy++;
    metricsOuterPanel2a.add(metricsBarPanel, gridBagConstraints);

    JScrollPane availableMetrcisScrollPane = new AnJScrollPane();
    availableMetrcisScrollPane.getVerticalScrollBar().setUnitIncrement(16);
    availableMetrcisScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
    availableMetrcisScrollPane.setBorder(null);
    availableMetrcisScrollPane.setViewportView(metricsOuterPanel2a);

    outerScrollPanePanel = new JPanel();
    outerScrollPanePanel.setBackground(Color.WHITE);
    outerScrollPanePanel.setLayout(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 0, 4, 0);
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    outerScrollPanePanel.add(availableMetrcisScrollPane, gridBagConstraints);

    previewPanel = constructPreviewPanel();
    previewPanel.setBorder(new MatteBorder(1, 0, 0, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR));

    int fontSize = previewPanel.getFont().getSize();
    int previewPanelSize = 60 + 4 * fontSize;
    splitPane =
        new AnSplitPaneFixedRightSize(
            AnSplitPane.VERTICAL_SPLIT,
            outerScrollPanePanel,
            previewPanel,
            previewPanelSize,
            previewPanelSize);
    splitPane.setDividerSize(1);
    splitPane.setOneTouchExpandable(false);
    metricsOuterPanel2.add(splitPane, BorderLayout.CENTER);
    //        metricsOuterPanel2.add(previewPanel, BorderLayout.SOUTH);
    metricsOuterPanel.add(metricsOuterPanel2, BorderLayout.CENTER);
  }

  private JPanel constructPreviewPanel() {
    //        overviewDisp.getMiniFuncListDisp().computeOnWorkerThread();

    JPanel miniFuncListPanel = new JPanel();
    miniFuncListPanel.setOpaque(false);
    miniFuncListPanel.setBorder(new MatteBorder(1, 1, 1, 0, AnEnvironment.SPLIT_PANE_BORDER_COLOR));
    miniFuncListPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    miniFuncListPanel.add(overviewDisp.getMiniFuncListDisp(), gridBagConstraints);

    JPanel previewPanel = new JPanel();
    previewPanel.setBackground(Color.WHITE);
    previewPanel.setLayout(new GridBagLayout());

    JLabel titleLabel = new JLabel(AnLocale.getString("Metrics Preview"));
    AnUtility.setAccessibleContext(titleLabel.getAccessibleContext(), titleLabel.getText());
    titleLabel.setToolTipText(AnLocale.getString("Preview of Functions and other views"));
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(4, 4, 0, 4);
    previewPanel.add(titleLabel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.insets = new Insets(0, 4, 0, 4);
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    previewPanel.add(miniFuncListPanel, gridBagConstraints);

    JPanel ttPanel = new JPanel(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    ttPanel.setOpaque(false);
    JLabel ttLabel1 = new JLabel(AnLocale.getString("Metrics Preview"));
    AnUtility.setAccessibleContext(ttLabel1.getAccessibleContext(), ttLabel1.getText());
    //        ttLabel1.setForeground(AnEnvironment.TOOLTIP_POPUP_FOREGROUND_COLOR);
    ttLabel1.setForeground(Color.BLACK);
    ttLabel1.setFont(ttLabel1.getFont().deriveFont(Font.BOLD));
    gridBagConstraints.gridx = 0;
    ttPanel.add(ttLabel1, gridBagConstraints);
    JLabel ttLabel2 = new JLabel(AnLocale.getString(" changed"));
    AnUtility.setAccessibleContext(ttLabel2.getAccessibleContext(), ttLabel2.getText());
    ttLabel2.setForeground(AnEnvironment.TOOLTIP_POPUP_FOREGROUND_COLOR);
    gridBagConstraints.gridx = 1;
    ttPanel.add(ttLabel2, gridBagConstraints);
    previewToolTipPopup =
        new ToolTipPopup(miniFuncListPanel, ttPanel, ToolTipPopup.Location.NORTH, true);
    previewToolTipPopupNew = true;
    return previewPanel;
  }

  public void previewChanged() {
    if (previewToolTipPopupNew) {
      previewToolTipPopupNew = false;
    } else if (previewToolTipPopup != null) {
      previewToolTipPopup.show(0, 3000);
    }
  }

  /*
   * Called from presentation with updates to attributes
   */
  protected void updateMetricTree(List<MetricState> metricStateList) {
    for (MetricState metricSetting : metricStateList) {
      if (metricSetting.getAnMetric().isNameMetric()) {
        continue;
      }
      String metricName = metricSetting.getAnMetric().getComd();
      boolean selected = metricSetting.getSelection().isSelected();
      MetricAttributes metricAttributes = metricSetting.getSelection().getAttributes();

      if (window.getSettings().getMetricsSetting().getMetricStates().getMetricsRootNode() == null) {
        AnLog.log("Error: updateMetricTree: " + " metricsRootNode == null");
        return;
      }
      List<MetricNode> nodes =
          window
              .getSettings()
              .getMetricsSetting()
              .getMetricStates()
              .getMetricsRootNode()
              .findByName(metricName);
      if (nodes.isEmpty()) {
        AnLog.log("Error: updateMetricTree: " + metricName + " not found under metricsRootNode");
        return;
      }
      for (MetricNode node : nodes) {
        if (node != null && node instanceof SelectableMetricNode) {
          // Update data
          SelectableMetricNode selectableMetricNode = (SelectableMetricNode) node;
          boolean isHidden = !selected;
          selectableMetricNode
              .getExclusive()
              .setSelected(
                  new MetricAttr(
                      isHidden,
                      metricAttributes.isETime(),
                      metricAttributes.isEValue(),
                      metricAttributes.isEPercent()));
          selectableMetricNode
              .getInclusive()
              .setSelected(
                  new MetricAttr(
                      isHidden,
                      metricAttributes.isITime(),
                      metricAttributes.isIValue(),
                      metricAttributes.isIPercent()));

          if (!selectableMetricNode.hasSelections()) {
            // should un-select metric in overview // FIXUP
          }
          // Update GUI
          selectableMetricNode.fireChange();
        }
      }
    }
  }

  //    private TurnerPanel dummyViewsPanel() {
  //        GridBagConstraintsnew JLabel gridBagConstraints;
  //
  //        JComponent viewsTopComponent;
  //        JPanel viewsBottomComponent;
  //
  //        TurnerPanel view1TurnerPanel;
  //        JComponent view1TopComponent;
  //        JComponent view1BottomComponent;
  //        TurnerPanel view2TurnerPanel;
  //        JComponent view2TopComponent;
  //        JComponent view2BottomComponent;
  //
  //        view1TopComponent = new JLabel("Functions");
  //        view1TopComponent.setOpaque(false);
  //        view1BottomComponent = new JLabel();
  //        view1TurnerPanel = new TurnerPanel(view1TopComponent, view1BottomComponent, false, true,
  // Overview.VIEWS_BACKGROUND);
  //        //
  //        view2TopComponent = new JLabel("Lines");
  //        view2TopComponent.setOpaque(false);
  //        view2BottomComponent = new JLabel();
  //        view2TurnerPanel = new TurnerPanel(view2TopComponent, view2BottomComponent, false, true,
  // Overview.VIEWS_BACKGROUND);
  //        //
  //        viewsTopComponent = new TopButtonPanel("Selected Views", "All Views...", new
  // ActionListener() {
  ////            @Override
  //            @Override
  //            public void actionPerformed(ActionEvent e) {
  //                window.getSettings().showDialog(7);
  //            }
  //        });
  //        viewsTopComponent.setOpaque(false);
  //        viewsBottomComponent = new JPanel();
  //        viewsBottomComponent.setOpaque(false);
  //        viewsBottomComponent.setLayout(new GridBagLayout());
  //        gridBagConstraints = new GridBagConstraints();
  //        gridBagConstraints.gridx = 0;
  //        gridBagConstraints.gridy = 0;
  //        gridBagConstraints.weightx = 1;
  //        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
  //        viewsBottomComponent.add(view1TurnerPanel, gridBagConstraints);
  //        gridBagConstraints.gridx = 0;
  //        gridBagConstraints.gridy = 1;
  //        gridBagConstraints.weightx = 1;
  //        gridBagConstraints.insets = new java.awt.Insets(4, 0, 6, 0);
  //        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
  //        viewsBottomComponent.add(view2TurnerPanel, gridBagConstraints);
  //
  //        TurnerPanel turnerPanel = new TurnerPanel(viewsTopComponent, viewsBottomComponent, true,
  // true, Overview.VIEWS_BACKGROUND);
  //        return turnerPanel;
  //    }

  private void initExperimentInfo(String[][] groups) {
    if (groups == null || groups.length == 0 || groups[0] == null) {
      return;
    }
    StringBuilder buf = new StringBuilder();
    int got = 0;
    outer:
    for (int i = 0; i < groups.length; i++) {
      for (int j = 0; j < groups[i].length; j++) {
        if (groups[i][j] != null) {
          if (got > 0) {
            buf.append(", ");
          }
          if (got >= 2) {
            buf.append("...");
            break outer;
          }
          buf.append(AnUtility.basename(groups[i][j]));
          got++;
        }
      }
    }
    String labelText = buf.toString();
    statusTopLabel.setText(labelText);
    statusBottomTextPane.setText(AnLocale.getString("Calculating..."));
    statusOuterPanel.setMinimumSize(
        statusOuterPanel.getPreferredSize()); // <=== ugly, but what else to do?
  }

  /**
   * Ready to load new experiments
   *
   * @param expList experiments list
   */
  protected void initLoadingExperiments(final String[][] groups, final boolean newExperiment) {
    //        this.groups = groups;
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            //                if (newExperiment) {
            //                    metricsRootNode = null;
            //                }
            // Status
            initExperimentInfo(groups);

            innerPanel.remove(metricsPanel);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
            innerPanel.add(metricsPanel, gridBagConstraints);

            // Metrics
            if (metricsOuterPanel2 != null) {
              metricsOuterPanel.remove(metricsOuterPanel2);
              metricsOuterPanel2 = null;
            }
            metricsProgressBarPanel.setVisible(true);
            // Views
            //                if (viewsTurnerPanel != null) {
            //                    viewsOuterPanel.remove(viewsTurnerPanel);
            //                    viewsTurnerPanel = null;
            //                }
            //                viewsProgressBarPanel.setVisible(true);
          }
        });
  }

  /** Loading of new experiments completed Always called from AWT thread */
  private void completedLoadingExperiments() {
    AnUtility.checkIfOnAWTThread(true);
    innerPanel.remove(metricsPanel);
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
    innerPanel.add(metricsPanel, gridBagConstraints);

    constructMetricsPanel();
    metricsProgressBarPanel.setVisible(false);
    //        metricsBarPanel.setVisible(true);
    //        constructViews();
    //        viewsProgressBarPanel.setVisible(false);
    //        viewsTurnerPanel.setVisible(true);
  }

  public static OverviewPanel getInstance() {
    return instance;
  }

  /*
   * Should be called if there are changes to the tree
   */
  protected void refreshMetricTree() {
    AnUtility.checkIfOnAWTThread(false); // Don't do this on AWT thread
    createAndUpdateMetricTree(); // IPC
    final String[] preview = getOverviewText(0); // IPC
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            fillStatusPanel(preview);
            completedLoadingExperiments();
          }
        });
  }

  private void createAndUpdateMetricTree() {
    if (window.getSettings().getMetricsSetting().getMetricStates().getMetricsRootNode() != null) {
      List<ValueMetricNode> nodeList =
          getAllValueMetricNodes(
              window.getSettings().getMetricsSetting().getMetricStates().getMetricsRootNode(),
              new ArrayList<ValueMetricNode>());
      IPCMetricsAPI.updateMetricValues(0, nodeList);
      window
          .getSettings()
          .getMetricsSetting()
          .getMetricStates()
          .getMetricsRootNode()
          .postProcessRoot();
    }
  }

  private List<ValueMetricNode> getAllValueMetricNodes(
      MetricNode parentNode, List<ValueMetricNode> list) {
    if (parentNode instanceof ValueMetricNode) {
      list.add((ValueMetricNode) parentNode);
    }
    for (MetricNode childNode : parentNode.getChildren()) {
      getAllValueMetricNodes(childNode, list); // recursive
    }

    return list;
  }

  /** Always executed on AWT thread */
  private void fillStatusPanel(String[] preview) {
    AnUtility.checkIfOnAWTThread(true);
    //      dumpExpStatus(preview);
    if (preview != null) {
      StringBuilder buf = new StringBuilder();
      for (String s : preview) {
        buf.append(s);
        buf.append("\n");
      }
      // Use Monospaced font to make the info more readable
      Font f = statusBottomTextPane.getFont();
      Font mf = new Font("Monospaced", Font.PLAIN, f.getSize());
      statusBottomTextPane.setFont(mf);
      statusBottomTextPane.setText(buf.toString());
      statusBottomTextPane.setCaretPosition(0);
    } else {
      statusBottomTextPane.setText(AnLocale.getString("Cancelled or error..."));
    }
  }

  private void dumpExpStatus(String[] preview) {
    if (false) {
      return;
    }
    for (int ii = 0; ii < preview.length; ii++) {
      System.out.printf("%s\n", preview[ii]); // DUMP
    }
  }

  /**
   * Get experiments preview. Use system progressbar and make it cancellable.
   *
   * @param expName
   * @return
   */
  private static String[] getOverviewText(int dbevindex) {
    IPCContext ipcContext =
        new IPCContext(
            AnLocale.getString("Experiment Preview"),
            IPCContext.Scope.SESSION,
            true,
            true,
            IPCContext.Request.SINGLE,
            1,
            null);
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, ipcContext);
    ipcHandle.append("getOverviewText");
    ipcHandle.append(dbevindex);
    IPCResult ipcResult = ipcHandle.sendRequest();
    if (ipcResult.getCC() == IPCResult.CC.SUCCESS) { // blocking
      String[] result = (String[]) ipcResult.getObject();
      return result;
    } else if (ipcResult.getCC() == IPCResult.CC.CANCELED) {
      return null;
    }
    return null;
  }

  public String dump() {
    return dump(window.getSettings().getMetricsSetting().getMetricStates().getMetricsRootNode());
  }

  public static String dump(MetricNode metricsRootNode) {
    StringBuffer buf = new StringBuffer();
    buf.append("metricsRootNode dump:\n");
    if (metricsRootNode != null) {
      dumpMetricTree(buf, metricsRootNode);
    }
    return buf.toString();
  }

  private static void dumpMetricTree(StringBuffer buf, MetricNode node) {
    dumpMetricTree(buf, node, 0);
  }

  private static void dumpMetricTree(StringBuffer buf, MetricNode node, int level) {
    buf.append("                             ".substring(0, 2 * level)); // DUMP
    if (node instanceof SelectableMetricNode) {
      SelectableMetricNode sMetricNode = (SelectableMetricNode) node;
      if (sMetricNode.isSelected()) {
        buf.append("[X]"); // DUMP
      } else {
        buf.append("[ ]"); // DUMP
      }
    }
    if (node.isHiddenInOverview()) {
      buf.append("HIDDEN "); // DUMP
    }
    buf.append(node.getMetricBasic().getDisplayName() + " - " + node.getMetricType()); // DUMP
    if (node instanceof ValueMetricNode) {
      ValueMetricNode valueMetricNode = (ValueMetricNode) node;
      List<MetricValue> mvals = valueMetricNode.getValues();
      if (!mvals.isEmpty()) {
        MetricValue mval0 = mvals.get(0);
        buf.append(" " + mval0.getLabel() + " Unit=" + mval0.getUnit() + ":");
        for (MetricValue mval : mvals) {
          String hilite = mval.getHighlight() ? "*" : "";
          buf.append(" [" + hilite + mval.formatValue() + "]");
        }
      }
    }
    buf.append("\n");
    String short_desc = node.getMetricBasic().getShortDescription();
    if (short_desc != null) {
      buf.append("        -'" + short_desc + "'\n");
    } // DUMP
    if (!node.getChildren().isEmpty()) {
      level++;
      for (MetricNode child : node.getChildren()) {
        dumpMetricTree(buf, child, level);
      }
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
    statusLabel = new javax.swing.JLabel();
    statusPanel = new javax.swing.JPanel();
    metricsLabel = new javax.swing.JLabel();
    metricsPanel = new javax.swing.JPanel();
    viewsLabel = new javax.swing.JLabel();
    viewsPanel = new javax.swing.JPanel();

    setLayout(new java.awt.GridBagLayout());

    innerPanel.setLayout(new java.awt.GridBagLayout());

    statusLabel.setText("Experiment(s)");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
    innerPanel.add(statusLabel, gridBagConstraints);

    statusPanel.setBackground(new java.awt.Color(255, 255, 255));
    statusPanel.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
    innerPanel.add(statusPanel, gridBagConstraints);

    metricsLabel.setText("Metrics");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
    innerPanel.add(metricsLabel, gridBagConstraints);

    metricsPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    metricsPanel.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
    innerPanel.add(metricsPanel, gridBagConstraints);

    viewsLabel.setText("Shown Metrics");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
    innerPanel.add(viewsLabel, gridBagConstraints);

    viewsPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    viewsPanel.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
    innerPanel.add(viewsPanel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(innerPanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel innerPanel;
  private javax.swing.JLabel metricsLabel;
  private javax.swing.JPanel metricsPanel;
  private javax.swing.JLabel statusLabel;
  private javax.swing.JPanel statusPanel;
  private javax.swing.JLabel viewsLabel;
  private javax.swing.JPanel viewsPanel;
  // End of variables declaration//GEN-END:variables
}
