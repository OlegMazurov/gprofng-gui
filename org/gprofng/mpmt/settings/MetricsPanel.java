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

package org.gprofng.mpmt.settings;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnCheckBox;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author tpreisle
 */
public class MetricsPanel extends javax.swing.JPanel {

  private int gridY = 0;
  private int maxMetricNameLengthInPixels = 0;
  private HashMap<Integer, MetricGUIEntry> metricHashMap = new HashMap<Integer, MetricGUIEntry>();
  private List<JCheckBox> showCheckBoxes;
  private List<JCheckBox> eTimeCheckBoxes;
  private List<JCheckBox> eValueCheckBoxes;
  private List<JCheckBox> ePercentCheckBoxes;
  private List<JCheckBox> iTimeCheckBoxes;
  private List<JCheckBox> iValueCheckBoxes;
  private List<JCheckBox> iPercentCheckBoxes;

  private JCheckBox allShowCheckBox;
  private JCheckBox eAllPercentCheckBox;
  private JCheckBox eAllTimeCheckBox;
  private JCheckBox eAllValueCheckBox;
  private JCheckBox iAllPercentCheckBox;
  private JCheckBox iAllTimeCheckBox;
  private JCheckBox iAllValueCheckBox;

  /** Creates new form MetricsPanel */
  public MetricsPanel() {
    initComponents();
    setPreferredSize(new Dimension(850, 550));
  }

  private void setTopLabelAttr(JLabel label) {
    label.setFont(showLabel1.getFont().deriveFont(Font.BOLD));
    label.setOpaque(true);
    label.setBackground(new Color(225, 225, 225));
    label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
  }

  private void initComponents2() {
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    innerPanel.setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    scrollPane.setBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, AnEnvironment.SCROLLBAR_BORDER_COLOR));

    setTopLabelAttr(showLabel1);
    setTopLabelAttr(exclusiveLabel);
    setTopLabelAttr(inclusiveLabel);

    //        showLabel1.setOpaque(true);
    AnUtility.setTextAndAccessibleContext(showLabel1, AnLocale.getString("Show in Views"));
    AnUtility.setTextAndAccessibleContext(exclusiveLabel, AnLocale.getString("EXCLUSIVE"));
    AnUtility.setTextAndAccessibleContext(inclusiveLabel, AnLocale.getString("INCLUSIVE"));

    eTimeLabel.setText(null);
    eTimeLabel.setIcon(AnUtility.timeIcon);
    AnUtility.setTTAndAccessibleContext(eTimeLabel, AnLocale.getString("Time"));

    eValueLabel.setText(null);
    eValueLabel.setIcon(AnUtility.numberIcon);
    AnUtility.setTTAndAccessibleContext(eValueLabel, AnLocale.getString("Value"));

    ePercentLabel.setText(null);
    ePercentLabel.setIcon(AnUtility.percentIcon);
    AnUtility.setTTAndAccessibleContext(ePercentLabel, AnLocale.getString("Percent"));

    iTimeLabel.setText(null);
    iTimeLabel.setIcon(AnUtility.timeIcon);
    AnUtility.setTTAndAccessibleContext(iTimeLabel, AnLocale.getString("Time"));

    iValueLabel.setText(null);
    iValueLabel.setIcon(AnUtility.numberIcon);
    AnUtility.setTTAndAccessibleContext(iValueLabel, AnLocale.getString("Value"));

    iPercentLabel.setText(null);
    iPercentLabel.setIcon(AnUtility.percentIcon);
    AnUtility.setTTAndAccessibleContext(iPercentLabel, AnLocale.getString("Percent"));

    hotButton.setText(AnLocale.getString("Hot"));
    hotButton.setToolTipText(
        AnLocale.getString(
            "Select only metrics with the highest activity levels (marked check boxes)"));
    hotButton.setMnemonic(AnLocale.getString('t', "SettingsMetricsHotButtonMN"));
    hotButton.setMargin(new Insets(0, 4, 0, 4));
    resetButton.setText(AnLocale.getString("Reset"));
    resetButton.setToolTipText(AnLocale.getString("Select the default set of metrics"));
    resetButton.setMnemonic(AnLocale.getString('R', "SettingsMetricsResetButtonMN"));
    resetButton.setMargin(new Insets(0, 4, 0, 4));
    clearAllButton.setText(AnLocale.getString("Clear All"));
    clearAllButton.setToolTipText(AnLocale.getString("Deselect all metrics"));
    clearAllButton.setMnemonic(AnLocale.getString('c', "SettingsMetricsClearAllButtonMN"));
    clearAllButton.setMargin(new Insets(0, 4, 0, 4));

    allShowCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(allShowCheckBox, AnLocale.getString("Show all metrics"));

    eAllTimeCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(eAllTimeCheckBox, AnLocale.getString("All EXCLUSIVE time"));
    eAllValueCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(
        eAllValueCheckBox, AnLocale.getString("All EXCLUSIVE value"));
    eAllPercentCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(
        eAllPercentCheckBox, AnLocale.getString("All EXCLUSIVE percent"));

    iAllTimeCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(iAllTimeCheckBox, AnLocale.getString("All INCLUSIVE time"));
    iAllValueCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(
        iAllValueCheckBox, AnLocale.getString("All INCLUSIVE value"));
    iAllPercentCheckBox = new AnCheckBox();
    AnUtility.setTTAndAccessibleContext(
        iAllPercentCheckBox, AnLocale.getString("All INCLUSIVE percent"));

    GridBagConstraints gridBagConstraints;
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.insets = new Insets(0, 12, 0, 0);
    innerPanel.add(allShowCheckBox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 3;
    innerPanel.add(eAllTimeCheckBox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 3;
    innerPanel.add(eAllValueCheckBox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 3;
    innerPanel.add(eAllPercentCheckBox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 3;
    innerPanel.add(iAllTimeCheckBox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 10;
    gridBagConstraints.gridy = 3;
    innerPanel.add(iAllValueCheckBox, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 11;
    gridBagConstraints.gridy = 3;
    innerPanel.add(iAllPercentCheckBox, gridBagConstraints);

    filler0.setPreferredSize(new Dimension(0, 3));
    filler1.setPreferredSize(new Dimension(16, 3));
    filler2.setPreferredSize(new Dimension(16, 3));

    hotButton.setIcon(AnUtility.hot_icon);
    hotButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Collection<MetricGUIEntry> collection = metricHashMap.values();
            Iterator<MetricGUIEntry> iterator = collection.iterator();
            while (iterator.hasNext()) {
              MetricGUIEntry metricGUIEntry = iterator.next();
              metricGUIEntry.hot();
            }
            refreshAllCheckBoxes();
            markChanged();
          }
        });
    resetButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Collection<MetricGUIEntry> collection = metricHashMap.values();
            Iterator<MetricGUIEntry> iterator = collection.iterator();
            while (iterator.hasNext()) {
              MetricGUIEntry metricGUIEntry = iterator.next();
              metricGUIEntry.factorySetting();
            }
            refreshAllCheckBoxes();
            markChanged();
          }
        });
    clearAllButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Collection<MetricGUIEntry> collection = metricHashMap.values();
            Iterator<MetricGUIEntry> iterator = collection.iterator();
            while (iterator.hasNext()) {
              MetricGUIEntry metricGUIEntry = iterator.next();
              metricGUIEntry.clear();
            }
            refreshAllCheckBoxes();
            markChanged();
          }
        });

    exclusiveLabel.setIcon(AnUtility.excl_icon);
    inclusiveLabel.setIcon(AnUtility.incl_icon);

    int checkBoxWidth = 14;
    int iconLabelWidth = (exclusiveLabel.getPreferredSize().width - checkBoxWidth) / 2;
    Dimension labelDim1 = new Dimension(iconLabelWidth, 13);
    Dimension labelDim2 = new Dimension(16, 13);
    eTimeLabel.setPreferredSize(labelDim1);
    eValueLabel.setPreferredSize(labelDim2);
    ePercentLabel.setPreferredSize(labelDim1);
    iTimeLabel.setPreferredSize(labelDim1);
    iValueLabel.setPreferredSize(labelDim2);
    iPercentLabel.setPreferredSize(labelDim1);
  }

  private void markChanged() {
    AnWindow.getInstance().getSettings().stateChanged(null); // Will enable Apply/OK
  }

  private class AllAttributeActionListener implements ActionListener {

    private JCheckBox allCheckBox;
    private List<JCheckBox> checkBoxes;

    public AllAttributeActionListener(JCheckBox allCheckBox, List<JCheckBox> checkBoxes) {
      this.checkBoxes = checkBoxes;
      this.allCheckBox = allCheckBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      boolean on = allCheckBox.isSelected();
      for (JCheckBox checkBox : checkBoxes) {
        if (checkBox.isEnabled()) {
          checkBox.setSelected(on);
        }
      }

      Collection<MetricGUIEntry> collection = metricHashMap.values();
      Iterator<MetricGUIEntry> iterator = collection.iterator();
      while (iterator.hasNext()) {
        MetricGUIEntry metricGUIEntry = iterator.next();
        metricGUIEntry.updateViewCheckBoxes();
      }
      markChanged();
    }
  }

  private class AllShowActionListener implements ActionListener {

    private JCheckBox allCheckBox;
    private List<JCheckBox> checkBoxes;

    public AllShowActionListener(JCheckBox allCheckBox, List<JCheckBox> checkBoxes) {
      this.checkBoxes = checkBoxes;
      this.allCheckBox = allCheckBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      boolean on = allCheckBox.isSelected();
      for (JCheckBox checkBox : checkBoxes) {
        if (checkBox.isEnabled()) {
          checkBox.setSelected(on);
        }
      }

      Collection<MetricGUIEntry> collection = metricHashMap.values();
      Iterator<MetricGUIEntry> iterator = collection.iterator();
      while (iterator.hasNext()) {
        MetricGUIEntry metricGUIEntry = iterator.next();
        metricGUIEntry.updateAttributes();
      }
      markChanged();
    }
  }

  protected void updateGUIValues() {
    List<MetricState> metricStateList =
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .getMetricStates()
            .getMetricStateList();
    updateGUIValues(metricStateList);
  }

  protected void updateGUIValues(List<MetricState> metricStateList) {
    for (MetricState metricState : metricStateList) {
      AnMetric anMetric = metricState.getAnMetric();
      MetricGUIEntry metricsGUIEntry = metricHashMap.get(anMetric.getComd().hashCode());
      if (metricsGUIEntry != null) {
        metricsGUIEntry.showCheckBox.setSelected(metricState.getSelection().isSelected());
        if (!metricState.getAnMetric().isStatic()) {
          metricsGUIEntry.eTimeCheckBox.setSelected(false);
          metricsGUIEntry.eValueCheckBox.setSelected(false);
          metricsGUIEntry.ePercentCheckBox.setSelected(false);
          metricsGUIEntry.iTimeCheckBox.setSelected(false);
          metricsGUIEntry.iValueCheckBox.setSelected(false);
          metricsGUIEntry.iPercentCheckBox.setSelected(false);

          if (metricState.getSelection().isSelected()) {
            if (metricState.getCapable().isETime()) {
              metricsGUIEntry.eTimeCheckBox.setSelected(
                  metricState.getSelection().getAttributes().isETime());
            }
            if (metricState.getCapable().isEValue()) {
              metricsGUIEntry.eValueCheckBox.setSelected(
                  metricState.getSelection().getAttributes().isEValue());
            }
            if (metricState.getCapable().isEPercent()) {
              metricsGUIEntry.ePercentCheckBox.setSelected(
                  metricState.getSelection().getAttributes().isEPercent());
            }
            if (metricState.getCapable().isITime()) {
              metricsGUIEntry.iTimeCheckBox.setSelected(
                  metricState.getSelection().getAttributes().isITime());
            }
            if (metricState.getCapable().isIValue()) {
              metricsGUIEntry.iValueCheckBox.setSelected(
                  metricState.getSelection().getAttributes().isIValue());
            }
            if (metricState.getCapable().isIPercent()) {
              metricsGUIEntry.iPercentCheckBox.setSelected(
                  metricState.getSelection().getAttributes().isIPercent());
            }
          }
        }
      }
    }
    refreshAllCheckBoxes();
  }

  private void refreshAllCheckBoxes() {
    refreshAllCheckBox(allShowCheckBox, showCheckBoxes);
    refreshAllCheckBox(eAllTimeCheckBox, eTimeCheckBoxes);
    refreshAllCheckBox(eAllValueCheckBox, eValueCheckBoxes);
    refreshAllCheckBox(eAllPercentCheckBox, ePercentCheckBoxes);
    refreshAllCheckBox(iAllTimeCheckBox, iTimeCheckBoxes);
    refreshAllCheckBox(iAllValueCheckBox, iValueCheckBoxes);
    refreshAllCheckBox(iAllPercentCheckBox, iPercentCheckBoxes);
  }

  private void refreshAllCheckBox(JCheckBox checkBox, List<JCheckBox> checkBoxes) {
    if (!allDisabled(checkBoxes)) {
      checkBox.setEnabled(true);
      checkBox.setSelected(allSelected(checkBoxes));
    } else {
      checkBox.setEnabled(false);
    }
  }

  private boolean allSelected(List<JCheckBox> checkBoxes) {
    boolean ret = true;
    for (JCheckBox checkBox : checkBoxes) {
      if (checkBox.isEnabled() && !checkBox.isSelected()) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  private boolean allDisabled(List<JCheckBox> checkBoxes) {
    boolean ret = true;
    for (JCheckBox checkBox : checkBoxes) {
      if (checkBox.isEnabled()) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  protected void updateGUIElements() {
    removeAll();
    gridY = 4;
    initComponents();
    initComponents2();

    showCheckBoxes = new ArrayList<JCheckBox>();
    allShowCheckBox.addActionListener(new AllShowActionListener(allShowCheckBox, showCheckBoxes));

    eTimeCheckBoxes = new ArrayList<JCheckBox>();
    eAllTimeCheckBox.addActionListener(
        new AllAttributeActionListener(eAllTimeCheckBox, eTimeCheckBoxes));

    eValueCheckBoxes = new ArrayList<JCheckBox>();
    eAllValueCheckBox.addActionListener(
        new AllAttributeActionListener(eAllValueCheckBox, eValueCheckBoxes));

    ePercentCheckBoxes = new ArrayList<JCheckBox>();
    eAllPercentCheckBox.addActionListener(
        new AllAttributeActionListener(eAllPercentCheckBox, ePercentCheckBoxes));

    iTimeCheckBoxes = new ArrayList<JCheckBox>();
    iAllTimeCheckBox.addActionListener(
        new AllAttributeActionListener(iAllTimeCheckBox, iTimeCheckBoxes));

    iValueCheckBoxes = new ArrayList<JCheckBox>();
    iAllValueCheckBox.addActionListener(
        new AllAttributeActionListener(iAllValueCheckBox, iValueCheckBoxes));

    iPercentCheckBoxes = new ArrayList<JCheckBox>();
    iAllPercentCheckBox.addActionListener(
        new AllAttributeActionListener(iAllPercentCheckBox, iPercentCheckBoxes));

    List<MetricState> metricStateList =
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .getMetricStates()
            .getMetricStateList();
    String listName = null;
    // Find longest metric name
    int longestLabelLengthInPixels = 0;
    for (MetricState metricState : metricStateList) {
      AnMetric anMetric = metricState.getAnMetric();
      JLabel label = new JLabel(anMetric.getUserName());
      int labelLengthInPixels = label.getPreferredSize().width;
      if (labelLengthInPixels > longestLabelLengthInPixels) {
        longestLabelLengthInPixels = labelLengthInPixels;
      }
    }
    if (longestLabelLengthInPixels < 200) {
      maxMetricNameLengthInPixels = 200;
    } else if (longestLabelLengthInPixels > 600) {
      maxMetricNameLengthInPixels = 600;
    } else {
      maxMetricNameLengthInPixels = longestLabelLengthInPixels + 20;
    }

    for (MetricState metricState : metricStateList) {
      AnMetric anMetric = metricState.getAnMetric();
      if (anMetric.isNameMetric()) {
        continue;
      }
      String ln = metricState.getListName();
      if (ln != null && !ln.equals(listName)) {
        //                System.out.println(ln);
        listName = ln;
        addGroupEntry(listName);
      }
      addMetricEntry(metricState);
    }

    addLastEntry();

    // Update the values
    updateGUIValues(metricStateList);
  }

  private void addMetricEntry(MetricState metricState) {
    AnMetric anMetric = metricState.getAnMetric();
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridY;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 16, 0, 0);
    JLabel label = new JLabel(getMetricName(anMetric));
    //        if (anMetric.isHot()) {
    //            label.setIcon(AnUtility.hot_icon);
    //            gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    //        } else {
    //            gridBagConstraints.insets = new Insets(0, 16, 0, 0);
    //        }
    label.setToolTipText(anMetric.getUserName());
    label.setPreferredSize(
        new Dimension(maxMetricNameLengthInPixels + 17, label.getPreferredSize().height));
    innerPanel.add(label, gridBagConstraints);

    //        if (anMetric.isHot()) {
    //            gridBagConstraints = new GridBagConstraints();
    //            gridBagConstraints.gridx = 2;
    //            gridBagConstraints.gridy = gridY;
    //            JLabel hotLabel = new JLabel();
    //            hotLabel.setIcon(AnUtility.hot_icon);
    //            innerPanel.add(hotLabel, gridBagConstraints);
    //        }

    MetricGUIEntry metricGUIEntry = new MetricGUIEntry(metricState);
    metricHashMap.put(metricState.getAnMetric().getComd().hashCode(), metricGUIEntry);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = gridY;
    JPanel showPanel = new JPanel(new GridBagLayout());
    showPanel.setOpaque(false);
    Insets insets;
    if (anMetric.isHot()) {
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      JLabel hotLabel = new JLabel();
      hotLabel.setOpaque(false);
      hotLabel.setIcon(AnUtility.hot_icon);
      showPanel.add(hotLabel, gridBagConstraints);
      insets = new Insets(0, 0, 0, 0);
    } else {
      insets = new Insets(0, 12, 0, 0);
    }
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    showPanel.add(metricGUIEntry.showCheckBox, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.insets = insets;
    //        gridBagConstraints.anchor = GridBagConstraints.WEST;
    innerPanel.add(showPanel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridy = gridY;
    if (!anMetric.isStatic()) {
      gridBagConstraints.gridx = 5;
      metricGUIEntry.eTimeCheckBox.setEnabled(metricState.getCapable().isETime());
      innerPanel.add(metricGUIEntry.eTimeCheckBox, gridBagConstraints);
      gridBagConstraints.gridx = 6;
      metricGUIEntry.eValueCheckBox.setEnabled(metricState.getCapable().isEValue());
      innerPanel.add(metricGUIEntry.eValueCheckBox, gridBagConstraints);
      gridBagConstraints.gridx = 7;
      metricGUIEntry.ePercentCheckBox.setEnabled(metricState.getCapable().isEPercent());
      innerPanel.add(metricGUIEntry.ePercentCheckBox, gridBagConstraints);
      gridBagConstraints.gridx = 9;
      metricGUIEntry.iTimeCheckBox.setEnabled(metricState.getCapable().isITime());
      innerPanel.add(metricGUIEntry.iTimeCheckBox, gridBagConstraints);
      gridBagConstraints.gridx = 10;
      metricGUIEntry.iValueCheckBox.setEnabled(metricState.getCapable().isIValue());
      innerPanel.add(metricGUIEntry.iValueCheckBox, gridBagConstraints);
      gridBagConstraints.gridx = 11;
      metricGUIEntry.iPercentCheckBox.setEnabled(metricState.getCapable().isIPercent());
      innerPanel.add(metricGUIEntry.iPercentCheckBox, gridBagConstraints);
    }

    gridY++;
  }

  private class MetricGUIEntry {

    AnMetric anMetric;

    AnCheckBox showCheckBox;
    AnCheckBox eTimeCheckBox;
    AnCheckBox eValueCheckBox;
    AnCheckBox ePercentCheckBox;
    AnCheckBox iTimeCheckBox;
    AnCheckBox iValueCheckBox;
    AnCheckBox iPercentCheckBox;

    public MetricGUIEntry(MetricState metricState) {
      this.anMetric = metricState.getAnMetric();
      ActionListener showActionListener =
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              updateAttributes();
              refreshAllCheckBoxes();
              markChanged();
            }
          };
      showCheckBox = new AnCheckBox();
      String txt = AnLocale.getString("Show this metric in views");
      showCheckBox.setToolTipText(txt);
      AnUtility.setAccessibleContext(showCheckBox.getAccessibleContext(), txt);
      showCheckBox.addActionListener(showActionListener);
      showCheckBoxes.add(showCheckBox);

      if (!metricState.getAnMetric().isStatic()) {
        ActionListener attributeActionListener =
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                showCheckBox.setSelected(anySelected());
                refreshAllCheckBoxes();
                markChanged();
              }
            };
        eTimeCheckBox = new AnCheckBox();
        txt = AnLocale.getString("Show EXCLUSIVE Time in views");
        eTimeCheckBox.setToolTipText(txt);
        AnUtility.setAccessibleContext(eTimeCheckBox.getAccessibleContext(), txt);
        eTimeCheckBox.addActionListener(attributeActionListener);
        eTimeCheckBoxes.add(eTimeCheckBox);

        eValueCheckBox = new AnCheckBox();
        txt = AnLocale.getString("Show EXCLUSIVE Value in views");
        eValueCheckBox.setToolTipText(txt);
        AnUtility.setAccessibleContext(eValueCheckBox.getAccessibleContext(), txt);
        eValueCheckBox.addActionListener(attributeActionListener);
        eValueCheckBoxes.add(eValueCheckBox);

        ePercentCheckBox = new AnCheckBox();
        txt = AnLocale.getString("Show EXCLUSIVE Percent in views");
        ePercentCheckBox.setToolTipText(txt);
        AnUtility.setAccessibleContext(ePercentCheckBox.getAccessibleContext(), txt);
        ePercentCheckBox.addActionListener(attributeActionListener);
        ePercentCheckBoxes.add(ePercentCheckBox);

        iTimeCheckBox = new AnCheckBox();
        txt = AnLocale.getString("Show INCLUSIVE Time in views");
        iTimeCheckBox.setToolTipText(txt);
        AnUtility.setAccessibleContext(iTimeCheckBox.getAccessibleContext(), txt);
        iTimeCheckBox.addActionListener(attributeActionListener);
        iTimeCheckBoxes.add(iTimeCheckBox);

        iValueCheckBox = new AnCheckBox();
        txt = AnLocale.getString("Show INCLUSIVE Value in views");
        iValueCheckBox.setToolTipText(txt);
        AnUtility.setAccessibleContext(iValueCheckBox.getAccessibleContext(), txt);
        iValueCheckBox.addActionListener(attributeActionListener);
        iValueCheckBoxes.add(iValueCheckBox);

        iPercentCheckBox = new AnCheckBox();
        txt = AnLocale.getString("Show INCLUSIVE Percent in views");
        iPercentCheckBox.setToolTipText(txt);
        AnUtility.setAccessibleContext(iPercentCheckBox.getAccessibleContext(), txt);
        iPercentCheckBox.addActionListener(attributeActionListener);
        iPercentCheckBoxes.add(iPercentCheckBox);
      }
    }

    public void updateAttributes() {
      MetricState metricState =
          AnWindow.getInstance()
              .getSettings()
              .getMetricsSetting()
              .getMetricStates()
              .findMetricStateByName(anMetric.getComd());
      if (!metricState.getAnMetric().isStatic()) {
        if (showCheckBox.isSelected()) {
          eTimeCheckBox.setSelected(metricState.getSelection().getAttributes().isETime());
          eValueCheckBox.setSelected(metricState.getSelection().getAttributes().isEValue());
          ePercentCheckBox.setSelected(metricState.getSelection().getAttributes().isEPercent());
          iTimeCheckBox.setSelected(metricState.getSelection().getAttributes().isITime());
          iValueCheckBox.setSelected(metricState.getSelection().getAttributes().isIValue());
          iPercentCheckBox.setSelected(metricState.getSelection().getAttributes().isIPercent());
          if (!anySelected()) {
            eTimeCheckBox.setSelected(metricState.getFactory().getAttributes().isETime());
            eValueCheckBox.setSelected(metricState.getFactory().getAttributes().isEValue());
            ePercentCheckBox.setSelected(metricState.getFactory().getAttributes().isEPercent());
            iTimeCheckBox.setSelected(metricState.getFactory().getAttributes().isITime());
            iValueCheckBox.setSelected(metricState.getFactory().getAttributes().isIValue());
            iPercentCheckBox.setSelected(metricState.getFactory().getAttributes().isIPercent());
          }
          if (!anySelected()) {
            // FIXUP: need to do anything here?
            System.out.println("MetricsPanel:showCheckBox: no attributes selected");
          }
        } else {
          eTimeCheckBox.setSelected(false);
          eValueCheckBox.setSelected(false);
          ePercentCheckBox.setSelected(false);
          iTimeCheckBox.setSelected(false);
          iValueCheckBox.setSelected(false);
          iPercentCheckBox.setSelected(false);
        }
      }
    }

    public void updateViewCheckBoxes() {
      if (!anMetric.isStatic()) {
        showCheckBox.setSelected(anySelected());
      }
    }

    public void hot() {
      showCheckBox.setSelected(anMetric.isHot());
      updateAttributes();
    }

    public void clear() {
      showCheckBox.setSelected(false);
      updateAttributes();
    }

    public void factorySetting() {
      MetricState metricState =
          AnWindow.getInstance()
              .getSettings()
              .getMetricsSetting()
              .getMetricStates()
              .findMetricStateByName(anMetric.getComd());
      showCheckBox.setSelected(metricState.getFactory().isSelected());
      if (!metricState.getAnMetric().isStatic()) {
        if (showCheckBox.isSelected()) {
          eTimeCheckBox.setSelected(metricState.getFactory().getAttributes().isETime());
          eValueCheckBox.setSelected(metricState.getFactory().getAttributes().isEValue());
          ePercentCheckBox.setSelected(metricState.getFactory().getAttributes().isEPercent());
          iTimeCheckBox.setSelected(metricState.getFactory().getAttributes().isITime());
          iValueCheckBox.setSelected(metricState.getFactory().getAttributes().isIValue());
          iPercentCheckBox.setSelected(metricState.getFactory().getAttributes().isIPercent());
        } else {
          eTimeCheckBox.setSelected(false);
          eValueCheckBox.setSelected(false);
          ePercentCheckBox.setSelected(false);
          iTimeCheckBox.setSelected(false);
          iValueCheckBox.setSelected(false);
          iPercentCheckBox.setSelected(false);
        }
      }
    }

    public boolean anySelected() {
      return eTimeCheckBox.isSelected()
          || eValueCheckBox.isSelected()
          || ePercentCheckBox.isSelected()
          || iTimeCheckBox.isSelected()
          || iValueCheckBox.isSelected()
          || iPercentCheckBox.isSelected();
    }
  }

  private String getMetricName(AnMetric anMetric) {
    StringBuilder buf = new StringBuilder();
    buf.append(anMetric.getUserName());
    JLabel label = new JLabel(buf.toString());

    if (label.getPreferredSize().width < maxMetricNameLengthInPixels) {
      while (label.getPreferredSize().width < maxMetricNameLengthInPixels) {
        buf.append("  .");
        label.setText(buf.toString());
      }
    }

    return buf.toString();
  }

  private void addGroupEntry(String groupName) {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridY++;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 0, 0, 0);
    JLabel label = new JLabel();
    AnUtility.setTextAndAccessibleContext(label, groupName);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.setForeground(AnEnvironment.METRIC_MENU_SECTION_COLOR);
    innerPanel.add(label, gridBagConstraints);
  }

  private void addLastEntry() {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridY++;
    gridBagConstraints.weighty = 1.0;
    innerPanel.add(new JLabel(""), gridBagConstraints);
  }

  public List<MetricNameSelection> getMetricNameSelections() {
    AnMetric[] availableMetrics =
        AnWindow.getInstance().getSettings().getMetricsSetting().getAvailableAnMetrics();
    List<MetricNameSelection> list = new ArrayList<MetricNameSelection>();
    for (int index = 0; index < availableMetrics.length; index++) {
      AnMetric anMetric = availableMetrics[index];
      MetricGUIEntry metricGUIEntry = metricHashMap.get(anMetric.getComd().hashCode());
      if (anMetric.isNameMetric()) {
        MetricAttributes metricAttributes =
            new MetricAttributes(
                false, false, false, false, false, false); // compatible with previous version
        MetricSelection metricSelection =
            new MetricSelection(false, metricAttributes); // compatible with previous version
        MetricNameSelection metricNameSelection =
            new MetricNameSelection(anMetric.getComd(), metricSelection);
        list.add(metricNameSelection);
      } else {
        if (metricGUIEntry != null) {
          MetricAttributes metricAttributes;
          if (!anMetric.isStatic()) {
            metricAttributes =
                new MetricAttributes(
                    metricGUIEntry.eTimeCheckBox.isSelected(),
                    metricGUIEntry.eValueCheckBox.isSelected(),
                    metricGUIEntry.ePercentCheckBox.isSelected(),
                    metricGUIEntry.iTimeCheckBox.isSelected(),
                    metricGUIEntry.iValueCheckBox.isSelected(),
                    metricGUIEntry.iPercentCheckBox.isSelected());
          } else {
            metricAttributes = new MetricAttributes(false, true, false, false, false, false);
          }
          MetricSelection metricSelection =
              new MetricSelection(metricGUIEntry.showCheckBox.isSelected(), metricAttributes);
          MetricNameSelection metricNameSelection =
              new MetricNameSelection(anMetric.getComd(), metricSelection);
          list.add(metricNameSelection);
        } else {
          System.err.println(
              "MetricsPanel.getMetricValues: Error: cannot find: " + anMetric.getComd());
        }
      }
    }
    return list;
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    scrollPane = new javax.swing.JScrollPane();
    innerPanel = new javax.swing.JPanel();
    showLabel1 = new javax.swing.JLabel();
    exclusiveLabel = new javax.swing.JLabel();
    inclusiveLabel = new javax.swing.JLabel();
    showLabel2 = new javax.swing.JLabel();
    eTimeLabel = new javax.swing.JLabel();
    eValueLabel = new javax.swing.JLabel();
    ePercentLabel = new javax.swing.JLabel();
    iTimeLabel = new javax.swing.JLabel();
    iValueLabel = new javax.swing.JLabel();
    iPercentLabel = new javax.swing.JLabel();
    separator = new javax.swing.JSeparator();
    filler0 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(12, 12),
            new java.awt.Dimension(12, 12),
            new java.awt.Dimension(12, 12));
    filler1 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(16, 12),
            new java.awt.Dimension(16, 12),
            new java.awt.Dimension(16, 12));
    filler2 =
        new javax.swing.Box.Filler(
            new java.awt.Dimension(12, 12),
            new java.awt.Dimension(12, 12),
            new java.awt.Dimension(12, 12));
    buttonPanel = new javax.swing.JPanel();
    hotButton = new javax.swing.JButton();
    resetButton = new javax.swing.JButton();
    clearAllButton = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    scrollPane.setBorder(null);
    scrollPane.setOpaque(false);

    innerPanel.setLayout(new java.awt.GridBagLayout());

    showLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    showLabel1.setText("Show in Views");
    showLabel1.setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    innerPanel.add(showLabel1, gridBagConstraints);

    exclusiveLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    exclusiveLabel.setText("EXCLUSIVE");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    innerPanel.add(exclusiveLabel, gridBagConstraints);

    inclusiveLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    inclusiveLabel.setText("INCLUSIVE");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    innerPanel.add(inclusiveLabel, gridBagConstraints);

    showLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    showLabel2.setToolTipText("");
    showLabel2.setOpaque(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.ipadx = 12;
    innerPanel.add(showLabel2, gridBagConstraints);

    eTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    eTimeLabel.setText("xx");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    innerPanel.add(eTimeLabel, gridBagConstraints);

    eValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    eValueLabel.setText("yy");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    innerPanel.add(eValueLabel, gridBagConstraints);

    ePercentLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    ePercentLabel.setText("zz");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    innerPanel.add(ePercentLabel, gridBagConstraints);

    iTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    iTimeLabel.setText("xx");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    innerPanel.add(iTimeLabel, gridBagConstraints);

    iValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    iValueLabel.setText("yy");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 10;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    innerPanel.add(iValueLabel, gridBagConstraints);

    iPercentLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    iPercentLabel.setText("zz");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 11;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    innerPanel.add(iPercentLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    innerPanel.add(separator, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    innerPanel.add(filler0, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    innerPanel.add(filler1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    innerPanel.add(filler2, gridBagConstraints);

    scrollPane.setViewportView(innerPanel);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 6);
    add(scrollPane, gridBagConstraints);

    buttonPanel.setLayout(new java.awt.GridBagLayout());

    hotButton.setText("Hot");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    buttonPanel.add(hotButton, gridBagConstraints);

    resetButton.setText("Reset");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    buttonPanel.add(resetButton, gridBagConstraints);

    clearAllButton.setText("Clear All");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    buttonPanel.add(clearAllButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 0);
    add(buttonPanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JButton clearAllButton;
  private javax.swing.JLabel ePercentLabel;
  private javax.swing.JLabel eTimeLabel;
  private javax.swing.JLabel eValueLabel;
  private javax.swing.JLabel exclusiveLabel;
  private javax.swing.Box.Filler filler0;
  private javax.swing.Box.Filler filler1;
  private javax.swing.Box.Filler filler2;
  private javax.swing.JButton hotButton;
  private javax.swing.JLabel iPercentLabel;
  private javax.swing.JLabel iTimeLabel;
  private javax.swing.JLabel iValueLabel;
  private javax.swing.JLabel inclusiveLabel;
  private javax.swing.JPanel innerPanel;
  private javax.swing.JButton resetButton;
  private javax.swing.JScrollPane scrollPane;
  private javax.swing.JSeparator separator;
  private javax.swing.JLabel showLabel1;
  private javax.swing.JLabel showLabel2;
  // End of variables declaration//GEN-END:variables
}
