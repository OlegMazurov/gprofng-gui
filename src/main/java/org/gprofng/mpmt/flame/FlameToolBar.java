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

package org.gprofng.mpmt.flame;

import org.gprofng.mpmt.AnDisplay;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.settings.MetricsSetting;
import org.gprofng.mpmt.settings.MetricsSetting.MetricCheckBox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class FlameToolBar extends JToolBar {

  private final FlameView flameView;
  private AnMetric[] availableMetrics;
  private JComboBox metricComboBox;
  private JSpinner zoomSpinner;
  private final int defaultZoomLevel;
  private JPopupMenu metricsSelectorpopup = null;

  private Stack<FlameBlock> setBaseStack = new Stack<FlameBlock>();
  private int setBaseStackpointer = -1;
  private int comboboxSelectedIndex = -1;

  protected FlameToolBar(final FlameView flameView) {
    this.flameView = flameView;
    setOpaque(false);
    setBorder(null);

    defaultZoomLevel = getFont().getSize();
    zoomSpinner = new JSpinner(new SpinnerNumberModel(0, -defaultZoomLevel - 3, 99, 1));
    zoomSpinner.setBorder(BorderFactory.createLineBorder(new Color(122, 138, 153), 1));
    JLabel zoomSpinnerlabel = new JLabel(AnLocale.getString("Zoom:"));
    zoomSpinnerlabel.setToolTipText(AnLocale.getString("Vertical zoom"));
    zoomSpinnerlabel.setDisplayedMnemonic(AnLocale.getString('Z', "FlameToolBar_ZoomLabel"));
    zoomSpinnerlabel.setLabelFor(zoomSpinner);

    zoomSpinner.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            int zoomLevel = (int) zoomSpinner.getValue();
            flameView.getFlamePanel().setZoomLevel(zoomLevel);
          }
        });
    add(zoomSpinnerlabel);
    add(zoomSpinner);

    add(getFiller(6));
    add(new JSeparator(JSeparator.VERTICAL));
    add(getFiller(4));

    add(flameView.getUndoSetBaseAction().createActionButton());
    add(flameView.getRedoSetBaseAction().createActionButton());
    add(flameView.getResetBaseAction().createActionButton());
    add(flameView.getSetBaseAction().createActionButton());

    add(getFiller(4));
    add(new JSeparator(JSeparator.VERTICAL));
    add(getFiller(4));

    add(flameView.getUpOneAction().createActionButton());
    add(flameView.getDownOneAction().createActionButton());
    add(flameView.getLeftOneAction().createActionButton());
    add(flameView.getRightOneAction().createActionButton());

    add(getFiller(4));
    add(new JSeparator(JSeparator.VERTICAL));
    add(getFiller(4));

    add(AnWindow.getInstance().getFunctionColorsAction().createActionButton());

    add(getFiller(4));
    add(new JSeparator(JSeparator.VERTICAL));
    add(getFiller(8));

    JLabel metricLabel = new JLabel(AnLocale.getString("Metric: "));
    metricLabel.setToolTipText(AnLocale.getString("Selected metric"));
    metricLabel.setDisplayedMnemonic(AnLocale.getString('e', "FlameViewMetricLabel"));
    add(metricLabel);
    metricComboBox = new JComboBox<AnMetric>();
    metricLabel.setLabelFor(metricComboBox);
    metricComboBox.setToolTipText(AnLocale.getString("Selected metric"));
    metricComboBox.addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            comboboxSelectedIndex = metricComboBox.getSelectedIndex();
          }

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            if (metricComboBox.getSelectedItem() instanceof AnMetric) {
              flameView.setMetric((AnMetric) metricComboBox.getSelectedItem());
            } else {
              if (comboboxSelectedIndex >= 0
                  && comboboxSelectedIndex < metricComboBox.getItemCount()) {
                metricComboBox.setSelectedIndex(comboboxSelectedIndex);
              }
              SwingUtilities.invokeLater(
                  new Runnable() {
                    @Override
                    public void run() {
                      showMetricsConfigurationPopup(
                          metricComboBox, 0, metricComboBox.getHeight(), AnDisplay.DSP_CallFlame);
                    }
                  });
            }
          }

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    add(metricComboBox);

    setMinimumSize(new Dimension(10, 10));

    getAccessibleContext().setAccessibleName(AnLocale.getString("Flame Chart Toolbar"));
    getAccessibleContext().setAccessibleDescription(AnLocale.getString("Flame Chart Toolbar"));
  }

  public void showMetricsConfigurationPopup(Component component, int x, int y, int dtype) {
    metricsSelectorpopup = getmetricsSelectorPopup(dtype);
    metricsSelectorpopup.show(component, x, y);
  }

  private JPopupMenu getmetricsSelectorPopup(int dtype) {
    JPopupMenu popup = new JPopupMenu();
    AnWindow.getInstance()
        .getSettings()
        .getMetricsSetting()
        .addMetricsPopupSelector(popup, dtype, false);
    // Customize: Slecting an already enabled metric will just use that metric (and not unselect
    // it).
    for (Component component : popup.getComponents()) {
      if (component instanceof MetricCheckBox) {
        final MetricCheckBox metricCheckBox = (MetricCheckBox) component;
        if (metricCheckBox.isSelected()) {
          //                    component.setEnabled(false);
          ActionListener[] als = metricCheckBox.getActionListeners();
          for (ActionListener al : als) {
            metricCheckBox.removeActionListener(al);
          }
          metricCheckBox.addActionListener(
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  flameView.setMetric(metricCheckBox.getAnMetric());
                }
              });
        }
      }
    }
    return popup;
  }

  protected void resetZoom() {
    zoomSpinner.setValue(0);
  }

  protected void resetMetrics(AnMetric anMetric) {
    metricComboBox.removeAllItems();
    availableMetrics =
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .getMetricListByMType(MetricsSetting.MET_CALL_AGR);
    if (availableMetrics != null) {
      int selectedIndex = -1;
      int index = 0;
      for (AnMetric am : availableMetrics) {
        if (am.isNameMetric() || am.isStatic()) {
          continue;
        }
        metricComboBox.addItem(am);
        if (anMetric.getUserName().equals(am.getUserName())) {
          selectedIndex = index;
        }
        index++;
      }
      if (selectedIndex >= 0) {
        metricComboBox.setSelectedIndex(selectedIndex);
      } else {
        metricComboBox.addItem(null);
        metricComboBox.setSelectedIndex(metricComboBox.getItemCount() - 1);
      }
      metricComboBox.addItem(AnLocale.getString("More Metrics..."));
    }
  }

  private JComponent getFiller(int x) {
    Dimension dim = new Dimension(x, 0);
    Box.Filler filler = new Box.Filler(dim, dim, dim);
    return filler;
  }

  protected void initSetBaseStack() {
    setBaseStack = new Stack<FlameBlock>();
    setBaseStackpointer = -1;
  }

  protected void pushToSetBaseStack(FlameBlock fb) {
    while (setBaseStack.size() > 0 && (setBaseStack.size() - 1) > setBaseStackpointer) {
      setBaseStack.pop();
    }
    if (setBaseStack.empty() || setBaseStack.peek() != fb) {
      setBaseStack.push(fb);
      setBaseStackpointer = setBaseStack.size() - 1;
    }
  }

  protected void undoSetBaseStack() {
    if (canUndoSetBaseStack()) {
      FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
      setBaseStackpointer--;
      FlameBlock flameBlock = setBaseStack.get(setBaseStackpointer);
      flameView.getFlamePanel().setBaseFlameBlock(flameBlock, false);
      flameView.getFlameData().setSelectedFlameBlock(selectedFlameBlock);
    }
    flameView.updateActionsEnableStatus();
  }

  protected void redoSetBaseStack() {
    if (canRedoSetBaseStack()) {
      FlameBlock selectedFlameBlock = flameView.getFlameData().getSelectedFlameBlock();
      setBaseStackpointer++;
      FlameBlock flameBlock = setBaseStack.get(setBaseStackpointer);
      flameView.getFlamePanel().setBaseFlameBlock(flameBlock, false);
      flameView.getFlameData().setSelectedFlameBlock(selectedFlameBlock);
    }
    flameView.updateActionsEnableStatus();
  }

  protected boolean canUndoSetBaseStack() {
    return setBaseStackpointer > 0;
  }

  protected boolean canRedoSetBaseStack() {
    return setBaseStackpointer < setBaseStack.size() - 1;
  }

  protected boolean canZoom() {
    FlameData flameData = flameView.getFlameData();
    return !(flameData.getFlameRows().length == 0
        || flameData.getFlameRows()[0] == null
        || flameData.getFlameRows()[0].getFlameBlocks().isEmpty());
  }

  protected void setControilsEnabled(boolean enabled) {
    if (enabled) {
      zoomSpinner.setEnabled(canZoom());
    } else {
      zoomSpinner.setEnabled(false);
    }
  }
}
