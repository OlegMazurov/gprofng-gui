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


package org.gprofng.mpmt.metrics;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.overview.AnBarTree;
import org.gprofng.mpmt.overview.AnBarTree.BarNode;
import org.gprofng.mpmt.overview.Bar;
import org.gprofng.mpmt.overview.Bar.ValueColor;
import org.gprofng.mpmt.overview.BarPanel;
import org.gprofng.mpmt.overview.CheckBoxBarPanel;
import org.gprofng.mpmt.overview.Overview;
import org.gprofng.mpmt.overview.TurnerLabelPanel;
import org.gprofng.mpmt.overview.TurnerPanel;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;

public class MetricsGUI {
  public static final boolean SHOW_ATTRIBUTES_IN_OVERVIEW = false;
  private int metricDisplayNameBaseWidth = 500;
  private int metricCheckBoxesPosition = 425;
  private Image exclusiveIcon = AnUtility.excl_icon.getImage();
  private Image inclusiveIcon = AnUtility.incl_icon.getImage();
  private static MetricsGUI instance = null;
  private MetricNode rootNode;
  private AnWindow window;
  private JTree tree;
  private JPanel rootPanel;

  public MetricsGUI(MetricNode rootNode, AnWindow window) {
    instance = this;
    this.rootNode = rootNode;
    this.window = window;
  }

  public static MetricsGUI getInstance() {
    return instance;
  }

  public int getMetricDisplayNameBaseWidth() {
    //        int longestName = longestName(rootNode, 0);
    return metricDisplayNameBaseWidth;
  }

  public void refreshMetricsPanel() {
    //        tree.validate();
    //        tree.repaint(100);
    if (tree != null) {
      for (int i = 0; i < tree.getRowCount(); i++) { // FIXUP: hack.....
        if (tree.isExpanded(i)) {
          tree.collapseRow(i);
          tree.expandRow(i);
        } else {
          tree.expandRow(i);
          tree.collapseRow(i);
        }
      }
    }
    rootPanel.repaint();
  }

  public void setTreeNodeStateSelected(
      SelectableMetricNode selectableMetricNode, boolean selected) {
    if (tree != null) {
      ((AnBarTree) tree).setTreeNodeStateSelected(selectableMetricNode, selected);
    }
  }

  public void expandAllNodes() {
    if (tree != null) {
      for (int i = 0; i < tree.getRowCount(); i++) {
        tree.expandRow(i);
      }
    }
  }

  public void collapseAllNodes() {
    if (tree != null) {
      for (int i = tree.getRowCount() - 1; i >= 0; i--) {
        tree.collapseRow(i);
      }
    }
  }

  public JPanel createAvailableMetricsPanel() {
    rootPanel = new PanelWithHeaders();
    rootPanel.setLayout(new GridBagLayout());
    rootPanel.setBackground(Color.WHITE);

    JLabel titleLabel = new JLabel(AnLocale.getString("Available Metrics"));
    AnUtility.setAccessibleContext(titleLabel.getAccessibleContext(), titleLabel.getText());
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    rootPanel.add(titleLabel, gridBagConstraints);

    if (!SHOW_ATTRIBUTES_IN_OVERVIEW) {
      int titleLabelWidth = titleLabel.getPreferredSize().width;
      int fontSize = new JLabel().getFont().getSize();

      int xOffset = 550 - titleLabelWidth;
      if (window.getSettings().getCompareModeSetting().comparingExperiments()) {
        xOffset += 100;
      }
      if (fontSize <= 12) {
      } else {
        xOffset += (fontSize - 12) * 25;
      }
      JPanel buttonPanel = createButtonPanel();
      xOffset = xOffset - (buttonPanel.getPreferredSize().width / 2);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(0, xOffset, 0, 0);
      gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
      rootPanel.add(buttonPanel, gridBagConstraints);
    }

    JPanel barPanel = createBarPanel(rootNode);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    rootPanel.add(barPanel, gridBagConstraints);

    //        panel.setBorder(new LineBorder(Color.green));
    return rootPanel;
  }

  private class PanelWithHeaders extends JPanel {
    private final int fontHeight = getFontMetrics(getFont()).getHeight();
    private int selectedLabelX = -1;
    private final int selectedLabelWidth = 60;
    private final int exclusiveLabelX = 82;
    private final int exclusiveLabelWidth = 80;
    private final int inclusiveLabelX = 102;
    private final int inclusiveLabelWidth = 80;
    private final int labelsY = 30;

    private int getFirstLabelX() {
      if (selectedLabelX <= 0) {
        int fontSize = new JLabel().getFont().getSize();
        selectedLabelX =
            window.getSettings().getCompareModeSetting().comparingExperiments()
                ? getMetricDisplayNameBaseWidth() + 126
                : getMetricDisplayNameBaseWidth() + 25;
        if (fontSize <= 12) {
        } else {
          selectedLabelX += (fontSize - 12) * 25;
        }
      }
      return selectedLabelX;
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);

      if (SHOW_ATTRIBUTES_IN_OVERVIEW) {
        int x;
        int y = labelsY;
        g.setFont(
            g.getFont()
                .deriveFont((float) 12)); // Always use size 12 font to paint headers (for now!)
        g.setColor(Color.BLACK);

        x = getFirstLabelX();
        x = x - 20;
        //            g.drawString(AnLocale.getString("Show in views"), x, y);
        g.drawString(AnLocale.getString("Show in views"), x, y + 14);
        x += exclusiveLabelX;
        x = x + 20;
        g.drawImage(exclusiveIcon, x, y - exclusiveIcon.getHeight(null) + 2, null);
        g.drawString(AnLocale.getString("Exclusive"), x + exclusiveIcon.getHeight(null) + 2, y);
        g.drawString(AnLocale.getString("Time  Value  %"), x - 5, y + 14);

        x += inclusiveLabelX;
        g.drawImage(inclusiveIcon, x, y - exclusiveIcon.getHeight(null) + 2, null);
        g.drawString(AnLocale.getString("Inclusive"), x + exclusiveIcon.getHeight(null) + 2, y);
        g.drawString(AnLocale.getString("Time  Value  %"), x - 5, y + 14);

        addMouseMotionListener(
            new MouseAdapter() {
              @Override
              public void mouseMoved(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if (x >= getFirstLabelX()
                    && x <= (getFirstLabelX() + selectedLabelWidth)
                    && y >= (labelsY - fontHeight)
                    && y <= labelsY + fontHeight) {
                  setToolTipText(AnLocale.getString("Choose metrics to be shown in data views"));
                } else if (x >= getFirstLabelX() + exclusiveLabelX
                    && x <= (getFirstLabelX() + exclusiveLabelX + exclusiveLabelWidth)
                    && y >= (labelsY - fontHeight)
                    && y <= labelsY + fontHeight) {
                  setToolTipText(
                      AnLocale.getString(
                          "Exclusive metrics do not include activity in called functions"));
                } else if (x >= getFirstLabelX() + exclusiveLabelX + inclusiveLabelX
                    && x
                        <= (getFirstLabelX()
                            + exclusiveLabelX
                            + inclusiveLabelX
                            + inclusiveLabelWidth)
                    && y >= (labelsY - fontHeight)
                    && y <= labelsY + fontHeight) {
                  setToolTipText(
                      AnLocale.getString("Inclusive metrics include activity in called functions"));
                } else {
                  setToolTipText(null);
                }
              }
            });
      }
    }
  }

  public JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(Color.WHITE);
    buttonPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints;
    gridBagConstraints = new GridBagConstraints();
    int gridx = 0;

    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    JButton hotButton = new OverviewButton(AnLocale.getString("Hot"));
    hotButton.setMnemonic(AnLocale.getString('o', "OverviewHotButtonMN"));
    hotButton.setIcon(AnUtility.hot_icon);
    hotButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().getSettings().getMetricsSetting().hotAction();
          }
        });
    hotButton.setToolTipText(
        AnLocale.getString(
            "Select only metrics with the highest activity levels (marked check boxes)"));
    //        JPanel hotPanel = new JPanel();
    //        hotPanel.setLayout(new GridBagLayout());
    //        hotPanel.setBackground(AnEnvironment.METRIC_HOT_HIGHLIGHT);
    //        hotPanel.setPreferredSize(new Dimension(hotButton.getPreferredSize().width+4,
    // hotButton.getPreferredSize().height+4));
    //        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
    //        gridBagConstraints2.anchor = GridBagConstraints.CENTER;
    //        hotPanel.add(hotButton, gridBagConstraints2);
    buttonPanel.add(hotButton, gridBagConstraints);

    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    JButton defaultButton = new OverviewButton(AnLocale.getString("Reset"));
    defaultButton.setMnemonic(AnLocale.getString('R', "OverviewResetButtonMN"));
    defaultButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().getSettings().getMetricsSetting().resetAction();
          }
        });
    defaultButton.setToolTipText(AnLocale.getString("Select the default set of metrics"));
    gridBagConstraints.insets = new Insets(0, 6, 0, 0);
    buttonPanel.add(defaultButton, gridBagConstraints);

    gridBagConstraints.gridx = gridx++;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 6, 0, 0);
    gridBagConstraints.weightx = 1.0;
    JButton clearAllButton = new OverviewButton(AnLocale.getString("Clear All"));
    clearAllButton.setMnemonic(AnLocale.getString('A', "OverviewClearAllButtonMN"));
    clearAllButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            AnWindow.getInstance().getSettings().getMetricsSetting().clearAction();
          }
        });
    clearAllButton.setToolTipText(AnLocale.getString("Deselect all metrics"));
    buttonPanel.add(clearAllButton, gridBagConstraints);

    //        buttonPanel.setBorder(new LineBorder(Color.green));
    return buttonPanel;
  }

  class OverviewButton extends JButton {
    public OverviewButton(String text) {
      super(text);
      setText(text);
      setFont(Overview.defaultPlainFont);
      setMargin(new Insets(0, 4, 0, 4));
      setOpaque(false);
      setBackground(new Color(237, 240, 244));
    }

    @Override
    protected void paintComponent(Graphics g) {
      g.setColor(Color.red);
      g.fillRect(0, 0, getWidth(), getHeight());

      Graphics2D g2d = (Graphics2D) g;
      int x = 15;
      int y = 25;
      Color topColor = new Color(240, 245, 239 + 15);
      Color bottomColor = new Color(176 + 20, 189 + 20, 201 + 20);
      GradientPaint gp;

      gp = new GradientPaint(0, 0, bottomColor, 0, getHeight() / 2, topColor);
      g2d.setPaint(gp);
      g2d.fillRect(0, 0, getWidth(), getHeight() / 2);

      gp = new GradientPaint(0, getHeight() / 2, topColor, 0, getHeight(), bottomColor);
      g2d.setPaint(gp);
      g2d.fillRect(0, getHeight() / 2, getWidth(), getHeight());

      super.paintComponent(g);
    }
  }

  //    private int longestName(MetricNode node, int max) {
  //        int len = 0;
  //        if (node == null || node.getMetricBasic() == null ||
  // node.getMetricBasic().getDisplayName() == null) {
  //            len = 0;
  //        }
  //        else {
  //            len = node.getMetricBasic().getDisplayName().length();
  //        }
  //        System.out.println(node.getMetricBasic().getDisplayName() + " " + len);
  //        if (len > max) {
  //            max = len;
  //        }
  //        for (MetricNode child : node.getChildren()) {
  //            max = longestName(child, max);
  //        }
  //        return max;
  //    }

  private JPanel createBarPanel(MetricNode node) {
    JPanel metricsBottomComponent = new JPanel();
    metricsBottomComponent.setOpaque(false);
    metricsBottomComponent.setLayout(new GridBagLayout());

    int gridy = 0;
    if (node == null) {
      return metricsBottomComponent;
    }
    TurnerPanel hwCounterMetricTurnerPanel = null;
    for (MetricNode child : node.getChildren()) {
      JPanel metricsPanel = null;
      if (child.getMetricType() == MetricNode.MetricType.AGGREGATE_TREE_ROOT_CATEGORY) {
        TurnerLabelPanel metric1TopComponent =
            new TurnerLabelPanel(child.getMetricBasic().getDisplayName(), formatValueLabel(child));
        metric1TopComponent.setOpaque(false);
        JPanel metric1BottomComponent = aggregateTreeRootCategory(child);
        metricsPanel =
            new TurnerPanel(
                metric1TopComponent,
                metric1BottomComponent,
                4,
                true,
                true,
                Overview.METRICS_BACKGROUND);
      } else if (child.getMetricType() == MetricNode.MetricType.AGGREGATE_LIST_CATEGORY
          || child.getMetricType()
              == MetricNode.MetricType
                  .AGGREGATE_TREE_CATEGORY) { // FIXUP: may not always be correct. Workaround for
                                              // Linux HWC (16575950). Perhaps th tree itself should
                                              // be changed.
        TurnerLabelPanel metric1TopComponent =
            new TurnerLabelPanel(child.getMetricBasic().getDisplayName(), formatValueLabel(child));
        metric1TopComponent.setOpaque(false);
        JPanel metric1BottomComponent = aggregateListCategory(child);
        metricsPanel =
            new TurnerPanel(
                metric1TopComponent,
                metric1BottomComponent,
                4,
                true,
                true,
                Overview.METRICS_BACKGROUND);
      } else if (child.getMetricType() == MetricNode.MetricType.PLAIN_LIST_CATEGORY) {
        if (!child.isHiddenInOverview()) {
          TurnerLabelPanel metric1TopComponent =
              new TurnerLabelPanel(
                  child.getMetricBasic().getDisplayName(), formatValueLabel(child));
          metric1TopComponent.setOpaque(false);
          JPanel metric1BottomComponent = createBarPanel(child); // recursive!
          metricsPanel =
              new TurnerPanel(
                  metric1TopComponent,
                  metric1BottomComponent,
                  4,
                  true,
                  true,
                  Overview.METRICS_BACKGROUND);
        }
      } else if (child.getMetricType() == MetricNode.MetricType.PLAIN
          || child.getMetricType() == MetricNode.MetricType.PLAIN_ROOT
          || child.getMetricType() == MetricNode.MetricType.PLAIN_CHILD) {
        if (!child.isHiddenInOverview()) {
          String name = child.getMetricBasic().getName();
          String displayName = child.getMetricBasic().getDisplayName();
          String displayNameValue = displayName;
          String tooltip = displayNameValue;
          Bar bar = null;
          if (child instanceof ValueMetricNode) {
            ValueMetricNode childValueMetricNode = (ValueMetricNode) child;
            displayNameValue = displayName + ": " + formatValueUnit(childValueMetricNode);

            // "Bugs 16624403 and 19539622" (leave this string intact for searches)
            if (displayName.equals("Block Covered %") || displayName.equals("Instr Covered %")) {
              displayNameValue = displayName; // revert to name without value
            }

            tooltip = displayNameValue;
            CompareBarData[] compareBars = null;
            compareBars =
                getCompareValueColors(childValueMetricNode, childValueMetricNode.getValue(0));
            if (compareBars != null && compareBars.length > 0) {
              tooltip = formatToolTip(childValueMetricNode);
              int percent = childValueMetricNode.getValue(0).gtZero() ? 100 : 0;
              bar = new Bar(MetricColors.getColor(name), percent, compareBars);
            }
          }
          int maxWidth = maxWidthInPixelsOfChildren(node) + 50;
          int nodeLevel = nodeLevel(child);
          if (child instanceof SelectableMetricNode) {
            if (child.getMetricType() == MetricNode.MetricType.PLAIN_ROOT
                || child.getMetricType() == MetricNode.MetricType.PLAIN_CHILD) {
              nodeLevel++;
            }
            CheckBoxBarPanel checkBoxBarPanel =
                new CheckBoxBarPanel(
                    child,
                    child.isSelected(),
                    child.isHot(),
                    displayNameValue,
                    tooltip,
                    bar,
                    maxWidth,
                    null,
                    -1,
                    nodeLevel); // FIXUP: add bar when we have tables....
            if (child.getMetricType() == MetricNode.MetricType.PLAIN_ROOT) {
              hwCounterMetricTurnerPanel =
                  new TurnerPanel(checkBoxBarPanel, null, 0, true, false, Color.white);
              metricsPanel = hwCounterMetricTurnerPanel;
            } else if (child.getMetricType() == MetricNode.MetricType.PLAIN_CHILD) {
              if (hwCounterMetricTurnerPanel != null) {
                hwCounterMetricTurnerPanel.setBottomPanelComponent(checkBoxBarPanel);
              }
              hwCounterMetricTurnerPanel = null;
            } else {
              metricsPanel = checkBoxBarPanel;
            }
          } else {
            metricsPanel =
                new BarPanel(
                    child,
                    child.isSelected(),
                    child.isHot(),
                    displayNameValue,
                    tooltip,
                    bar,
                    maxWidth,
                    null,
                    -1,
                    nodeLevel); // FIXUP: add bar when we have tables....
          }
        }
      } else {
        System.err.println("MetricsPanel:getMetricsPanels() not handling " + child.getMetricType());
        assert false;
      }
      if (metricsPanel != null) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        //                gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        metricsBottomComponent.add(metricsPanel, gridBagConstraints);
      }
    }

    return metricsBottomComponent;
  }

  private JPanel aggregateListCategory(MetricNode node) {
    MetricValue totalValueNode = null;
    String totalValueNodeText = null;
    if (node instanceof ValueMetricNode && ((ValueMetricNode) node).hasValues()) {
      ValueMetricNode parentValueMetricNode = (ValueMetricNode) node;
      totalValueNode = parentValueMetricNode.getValue(0);
      totalValueNodeText = parentValueMetricNode.getMetricBasic().getDisplayName();
    }

    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new GridBagLayout());
    int gridy = 0;
    for (MetricNode child : node.getChildren()) {
      if (child.getMetricType() == MetricNode.MetricType.PLAIN) {
        int percent = 0;
        CompareBarData[] compareBars = null;
        String toolTipText = null;

        if (child instanceof ValueMetricNode && totalValueNode != null) {
          ValueMetricNode childValueMetricNode = (ValueMetricNode) child;
          if (childValueMetricNode.hasValues()) {
            percent = childValueMetricNode.getValue(0).percentOf(totalValueNode);
            compareBars = getCompareValueColors(childValueMetricNode, totalValueNode);
            toolTipText = formatToolTip(childValueMetricNode, totalValueNode, totalValueNodeText);
          }
        }

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;

        ValueColor[] valueColor =
            new Bar.ValueColor[] {
              new Bar.ValueColor(percent, MetricColors.getColor(child.getMetricBasic().getName()))
            };
        Bar bar = new Bar(valueColor, percent, compareBars);
        CheckBoxBarPanel barPanel =
            new CheckBoxBarPanel(
                child,
                child.isSelected(),
                child.isHot(),
                child.getMetricBasic().getDisplayName(),
                toolTipText,
                bar,
                31,
                null,
                -2,
                3);
        panel.add(barPanel, gridBagConstraints);
      }
    }

    return panel;
  }

  private String formatToolTip(ValueMetricNode valueMetricNode) {
    return formatToolTip(valueMetricNode, null, null);
  }

  private String formatToolTip(
      ValueMetricNode valueMetricNode, MetricValue refMetric, String refMetricName) {
    String tt = "";
    if (valueMetricNode.getValues().size() > 0) {
      String metricDisplayName = valueMetricNode.getMetricBasic().getDisplayName();
      if (valueMetricNode.getValues().size() == 1) {
        tt =
            metricDisplayName
                + ": "
                + formatMetricValue(valueMetricNode.getValue(0), refMetric, refMetricName);
      } else {
        StringBuilder buf = new StringBuilder();
        buf.append("<html>");
        buf.append("<b>" + metricDisplayName + "</b>");
        int n = 0;
        for (MetricValue metricValue : valueMetricNode.getValues()) {
          buf.append("<br>");
          if (n == 0) {
            String valueTxt = formatMetricValue(metricValue, refMetric, refMetricName);
            buf.append("Baseline");
            for (int i = 0; i < 19; i++) {
              buf.append("&nbsp;");
            }
            if (valueMetricNode.getValues().size() > 2) {
              for (int i = 0; i < 3; i++) {
                buf.append("&nbsp;");
              }
            }
            buf.append(": " + valueTxt);
          } else {
            String valueTxt =
                formatMetricValue(metricValue, valueMetricNode.getValues().get(0), null);
            if (valueMetricNode.getValues().size() == 2) {
              buf.append("Comparison Group: " + valueTxt);
            } else {
              buf.append("Comparison Group " + n + ": " + valueTxt);
            }
          }
          n++;
        }
        buf.append("</html>");
        tt = buf.toString();
      }
    }
    return tt;
  }

  private String formatMetricValue(
      MetricValue metricValue, MetricValue refMetricValue, String refMetricName) {
    String valueUnit = metricValue.formatValueUnit();
    if (metricValue.getValueType() == MetricValue.ValueType.LABEL_ONLY || refMetricValue == null) {
      // No comparison to a reference
      return valueUnit;
    }

    String compareText = "";
    int percent = metricValue.percentOf(refMetricValue);
    String percentString;
    if (percent == MetricValue.PERCENT_NAN) {
      percentString = "+INF%";
    } else {
      percentString = new Formatter().format("%d%%", percent).toString();
    }
    if (refMetricName != null) {
      // show percent of some other total
      compareText =
          new Formatter()
              .format(", " + AnLocale.getString("%s of %s"), percentString, refMetricName)
              .toString();
    } else {
      // show delta from base
      MetricValue diffValue = metricValue.clone();
      diffValue.subtract(refMetricValue);
      String diffValueString = diffValue.formatValue();
      if (!diffValue.ltZero()) {
        diffValueString = "+" + diffValueString;
      }
      compareText = new Formatter().format("  (%s, %s)", diffValueString, percentString).toString();
    }
    String txt = new Formatter().format("%s%s", valueUnit, compareText).toString();
    return txt;
  }

  private String formatValueLabel(MetricNode node) {
    String label = null;
    if (node instanceof ValueMetricNode) {
      ValueMetricNode valueMetricNode = (ValueMetricNode) node;
      if (valueMetricNode.hasValues()) {
        MetricValue metricValue = valueMetricNode.getValue(0);
        if (metricValue.getValueType() == MetricValue.ValueType.LABEL_ONLY) {
          label = metricValue.getLabel();
        } else {
          label =
              new Formatter()
                  .format(
                      "%s: %s",
                      valueMetricNode.getValue(0).getLabel(),
                      valueMetricNode.getValue(0).formatValueUnit())
                  .toString();
        }
      }
    }
    return label;
  }

  private String formatValueUnit(ValueMetricNode valueMetricNode) {
    String valueUnit = "";
    if (valueMetricNode.hasValues()) {
      valueUnit = valueMetricNode.getValue(0).formatValueUnit();
    }
    return valueUnit;
  }

  private static int stringWidthInPixels(String s) {
    int width = new JLabel(s).getPreferredSize().width;
    return width + 30;
  }

  private static int maxWidthInPixelsOfChildren(MetricNode node) {
    int maxWidth = 0;
    {
      for (MetricNode child : node.getChildren()) {
        String displayName = child.getMetricBasic().getDisplayName();
        int width = stringWidthInPixels(displayName);
        if (width > maxWidth) {
          maxWidth = width;
        }
      }
    }
    return maxWidth;
  }

  private static void maxWidthInPixelsOfChildren(
      MetricNode node, int level, int maxWidthAtLevel[]) {
    for (MetricNode child : node.getChildren()) {
      String displayName = child.getMetricBasic().getDisplayName();
      int width = stringWidthInPixels(displayName);
      if (width > maxWidthAtLevel[level]) {
        maxWidthAtLevel[level] = width;
      }
      if (!child.getChildren().isEmpty()) {
        maxWidthInPixelsOfChildren(child, level + 1, maxWidthAtLevel);
      }
    }
  }

  private static int maxWidthInPixels(int maxWidthAtLevel[]) {
    int max = 0;
    for (int i = 0; i < maxWidthAtLevel.length; i++) {
      if (maxWidthAtLevel[i] > max) {
        max = maxWidthAtLevel[i];
      }
    }
    return max;
  }

  private JPanel aggregateTreeRootCategory(MetricNode node) {
    List<Object> rootNodes = new ArrayList<Object>();
    int maxWidthAtLevel[] = new int[10];
    int level = 0;
    maxWidthInPixelsOfChildren(node, level + 1, maxWidthAtLevel);

    for (MetricNode child : node.getChildren()) {
      if (child.getMetricType() == MetricNode.MetricType.AGGREGATE_TREE_CATEGORY) {
        Vector<AnBarTree.BarNode> treeNode =
            aggregateTreeCategory(node, child, maxWidthAtLevel, level + 1);
        rootNodes.add(treeNode);
      } else if (child.getMetricType() == MetricNode.MetricType.PLAIN) {
        //                Vector<AnBarTree.BarNode> treeNode = aggregateTreeCategory(node, child,
        // maxWidthAtLevel, level+1);
        MetricValue totalValueNode = null;
        String totalValueNodeText = null;
        if (node instanceof ValueMetricNode && ((ValueMetricNode) node).hasValues()) {
          totalValueNode = ((ValueMetricNode) node).getValue(0);
          totalValueNodeText = node.getMetricBasic().getDisplayName();
        }
        int percent = 0;
        CompareBarData[] compareBars = null;
        String toolTipText = null;
        if (child instanceof ValueMetricNode) {
          ValueMetricNode nodeValueMetricNode = (ValueMetricNode) child;
          if (nodeValueMetricNode.hasValues() && totalValueNode != null) {
            percent = nodeValueMetricNode.getValue(0).percentOf(totalValueNode);
            compareBars = getCompareValueColors(nodeValueMetricNode, totalValueNode);
            toolTipText = formatToolTip(nodeValueMetricNode, totalValueNode, totalValueNodeText);
          }
        }
        int maxWidth = maxWidthInPixelsOfChildren(node);
        List<Bar.ValueColor> barValueColors = new ArrayList<Bar.ValueColor>();
        barValueColors.add(
            new Bar.ValueColor(percent, MetricColors.getColor(child.getMetricBasic().getName())));
        Bar cpuBar = new Bar(barValueColors, percent, compareBars);
        Vector<AnBarTree.BarNode> plainNode =
            new AnBarTree.BarNode<AnBarTree.BarNode>(
                child, false, toolTipText, cpuBar, maxWidth, 0);

        rootNodes.add(plainNode);
      } else {
        System.err.println(
            "MetricsPanel:aggregateTreeRootCategory() not handling " + child.getMetricType());
        assert false;
      }
    }
    Vector<Object> rootVector =
        new AnBarTree.BarNode<Object>(
            node, true, null, null, rootNodes, maxWidthInPixels(maxWidthAtLevel), level);

    // GUI
    tree = new AnBarTree(rootVector);
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints;

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    panel.add(tree, gridBagConstraints);

    //        expandBranchesWithSelections(node, tree);
    // Hack to get rid of all turners on leaf nodes
    expandAllNodes();
    collapseAllNodes();
    // Expand only root node
    tree.expandRow(0);

    return panel;
  }

  //    private void expandBranchesWithSelections(MetricNode rootNode, JTree tree) {
  //        if (tree == null) {
  //            return;
  //        }
  //        Stack<Integer> levels = new Stack<Integer>();
  //        levels.push(-1);
  //        expandBranchesWithSelectionsOrIsHot(levels, rootNode, tree);
  //    }

  private void expandBranchesWithSelectionsOrIsHot(
      Stack<Integer> levels, MetricNode node, JTree tree) {
    if (node instanceof SelectableMetricNode) {
      boolean hasSelectionsOrHot = node.isSelected() || node.isHot();
      if (hasSelectionsOrHot) {
        Iterator<Integer> iterator = levels.iterator();
        while (iterator.hasNext()) {
          int level = iterator.next().intValue();
          if (iterator.hasNext() && level >= 0) {
            tree.expandRow(level);
          }
        }
      }
    }
    int currentLevel = levels.peek().intValue();
    levels.push(currentLevel + 1);
    for (MetricNode child : node.getChildren()) {
      expandBranchesWithSelectionsOrIsHot(levels, child, tree);
    }
    levels.pop();
  }

  private int nodeLevel(MetricNode targetNode) {
    int level = nodeLevel(rootNode, targetNode, 0);
    return level;
  }

  private int nodeLevel(MetricNode node, MetricNode targetNode, int level) {
    for (MetricNode child : node.getChildren()) {
      if (child == targetNode) {
        return level + 1;
      }
      int l = nodeLevel(child, targetNode, level + 1);
      if (l > 0) {
        return l;
      }
    }
    return -1;
  }

  private CompareBarData[] getCompareValueColors(ValueMetricNode node, MetricValue totalValueNode) {
    if (node.hasValues()) {
      if (node.getValues().size() <= 1) {
        return null;
      } else {
        CompareBarData[] compareValueColors = new CompareBarData[node.getValues().size() - 1];

        for (int valueNo = 1; valueNo < node.getValues().size(); valueNo++) {
          MetricValue metricValue = node.getValues().get(valueNo);
          int percent = metricValue.percentOf(totalValueNode);

          List<ValueColor> valueColorList = new ArrayList<ValueColor>();
          if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (MetricNode child : node.getChildren()) {
              if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                for (MetricNode grandchildren :
                    child
                        .getChildren()) { // handles User/System/trap CPU time //YM: Should probably
                                          // be recursive
                  MetricValue metricValue2 =
                      ((ValueMetricNode) grandchildren).getValues().get(valueNo);
                  int percent2 = metricValue2.percentOf(node.getValue(valueNo));
                  ValueColor valueColor =
                      new ValueColor(
                          percent2,
                          MetricColors.getColor(grandchildren.getMetricBasic().getName()));
                  valueColorList.add(valueColor);
                }
              } else {
                MetricValue metricValue2 = ((ValueMetricNode) child).getValues().get(valueNo);
                int percent2 = metricValue2.percentOf(node.getValue(valueNo));
                ValueColor valueColor =
                    new ValueColor(
                        percent2, MetricColors.getColor(child.getMetricBasic().getName()));
                valueColorList.add(valueColor);
              }
            }
          } else {
            ValueColor valueColor =
                new ValueColor(100, MetricColors.getColor(node.getMetricBasic().getName()));
            valueColorList.add(valueColor);
          }

          compareValueColors[valueNo - 1] = new CompareBarData(percent, valueColorList);
        }
        //                dumpCompareValueColors(node, compareValueColors);
        return compareValueColors;

        //                for (int i = 1; i < node.getValues().size(); i++) {
        //                    MetricValue metricValue = node.getValues().get(i);
        //                    int percent = metricValue.percentOf(totalValueNode);
        //
        //                    List<ValueColor> valueColorList = new ArrayList<ValueColor>();
        //                    ValueColor valueColor = new ValueColor(50, Bar.BAR_COMPARE_COLORS[i %
        // 2]);
        //                    valueColorList.add(valueColor);
        //                    valueColor = new ValueColor(50, Bar.BAR_COMPARE_COLORS[(i+1) % 2]);
        //                    valueColorList.add(valueColor);
        //
        //                    compareValueColors[i - 1] = new CompareBarData(percent,
        // valueColorList);
        //                }
        //                dumpCompareValueColors(node, compareValueColors);
        //                return compareValueColors;
      }
    }
    return null;
  }

  private void dumpCompareValueColors(ValueMetricNode node, CompareBarData[] compareValueColors) {
    System.out.println("compareValueColors: " + node.getMetricBasic().getDisplayName());
    for (CompareBarData cbd : compareValueColors) {
      System.out.print("CompareBarData:" + cbd.getPercentOfTotal());
      for (ValueColor vc : cbd.getValueColors()) {
        System.out.print(" [");
        System.out.print(vc.getValue() + " " + vc.getColor());
        System.out.print("]");
      }
      System.out.println();
    }
  }

  private Vector<AnBarTree.BarNode> aggregateTreeCategory(
      MetricNode rootNode, MetricNode node, int maxWidthAtLevel[], int level) {
    MetricValue totalValueNode = null;
    String totalValueNodeText = null;
    if (rootNode instanceof ValueMetricNode && ((ValueMetricNode) rootNode).hasValues()) {
      totalValueNode = ((ValueMetricNode) rootNode).getValue(0);
      totalValueNodeText = rootNode.getMetricBasic().getDisplayName();
    }

    List<BarNode> barNodes = new ArrayList<BarNode>();
    List<Bar.ValueColor> barValueColors = new ArrayList<Bar.ValueColor>();
    List<MetricNode> children = node.getChildren();
    for (MetricNode child : children) {
      if (child.getMetricType() == MetricNode.MetricType.PLAIN) {
        int percent = 0;
        CompareBarData[] compareBars = null;
        String toolTipText = null;
        if (child instanceof ValueMetricNode) {
          ValueMetricNode childValueMetricNode = (ValueMetricNode) child;
          if (childValueMetricNode.hasValues()) {
            percent = childValueMetricNode.getValue(0).percentOf(totalValueNode);
            compareBars = getCompareValueColors(childValueMetricNode, totalValueNode);
            toolTipText = formatToolTip(childValueMetricNode, totalValueNode, totalValueNodeText);
          }
        }
        barNodes.add(
            new BarNode(
                child,
                child.isSelected(),
                toolTipText,
                new Bar(
                    new Bar.ValueColor[] {
                      new Bar.ValueColor(
                          percent, MetricColors.getColor(child.getMetricBasic().getName()))
                    },
                    percent,
                    compareBars),
                maxWidthInPixels(maxWidthAtLevel),
                level + 1));
        barValueColors.add(
            new Bar.ValueColor(percent, MetricColors.getColor(child.getMetricBasic().getName())));
      } else if (child.getMetricType() == MetricNode.MetricType.AGGREGATE_TREE_CATEGORY) {
        int percent = 0;
        CompareBarData[] compareBars = null;
        String toolTipText = null;
        if (child instanceof ValueMetricNode) {
          ValueMetricNode childValueMetricNode = (ValueMetricNode) child;
          if (childValueMetricNode.hasValues()) {
            percent = childValueMetricNode.getValue(0).percentOf(totalValueNode);
            compareBars = getCompareValueColors(childValueMetricNode, totalValueNode);
            toolTipText = formatToolTip(childValueMetricNode, totalValueNode, totalValueNodeText);
          }
        }
        Vector<AnBarTree.BarNode> vector =
            aggregateTreeCategory(rootNode, child, maxWidthAtLevel, level + 1);
        List<Bar.ValueColor> barValueColorsThisBar = new ArrayList<Bar.ValueColor>();
        for (AnBarTree.BarNode barNode : vector) {
          List<ValueColor> valueColorList = barNode.getBar().getValueColor();
          for (ValueColor valueColor : valueColorList) {
            barValueColors.add(valueColor);
            barValueColorsThisBar.add(valueColor);
          }
        }
        barNodes.add(
            new BarNode(
                child,
                child.isSelected(),
                toolTipText,
                new Bar(barValueColorsThisBar, percent, compareBars),
                vector,
                maxWidthInPixels(maxWidthAtLevel),
                level + 1));
      }
    }
    int percent = 0;
    CompareBarData[] compareBars = null;
    String toolTipText = null;
    if (node instanceof ValueMetricNode) {
      ValueMetricNode nodeValueMetricNode = (ValueMetricNode) node;
      if (nodeValueMetricNode.hasValues() && totalValueNode != null) {
        percent = nodeValueMetricNode.getValue(0).percentOf(totalValueNode);
        compareBars = getCompareValueColors(nodeValueMetricNode, totalValueNode);
        toolTipText = formatToolTip(nodeValueMetricNode, totalValueNode, totalValueNodeText);
      }
    }
    Bar cpuBar = new Bar(barValueColors, percent, compareBars);
    Vector<AnBarTree.BarNode> cpuNode =
        new AnBarTree.BarNode<AnBarTree.BarNode>(
            node,
            node.isSelected(),
            toolTipText,
            cpuBar,
            barNodes,
            maxWidthInPixels(maxWidthAtLevel),
            level);
    return cpuNode;
  }
}
