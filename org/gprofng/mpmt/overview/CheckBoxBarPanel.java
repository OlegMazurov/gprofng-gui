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

package org.gprofng.mpmt.overview;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.metrics.MetricNode;
import org.gprofng.mpmt.metrics.MetricsGUI;
import org.gprofng.mpmt.metrics.SelectableMetricNode;
import org.gprofng.mpmt.settings.MetricNameSelected;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CheckBoxBarPanel extends javax.swing.JPanel {

  private static final int BAR_PANEL_HEIGHT = 16;
  private final JCheckBox checkBox;
  private final MetricNode metricNode;
  private Insets insets = null;
  private String toolTipText;
  private static int checkboxFixedPosition = -1;

  private static List<CheckBoxBarPanel> checkBoxBarPanelList;
  private static Object selectionsLock = new Object();
  private static Thread selectionsThread;
  private boolean hasFocus;

  public CheckBoxBarPanel(
      final MetricNode metricNode,
      boolean isSelected,
      boolean hot,
      String text,
      String toolTipText,
      Bar bar,
      int maxLength,
      List<Color> keyColorList,
      int treeLevel,
      int nodeLevel) {
    if (metricNode != null) {
      //            System.out.println(metricNode.getMetricBasic().getName() + ": " + isSelected);
      checkBoxBarPanelList.add(this);
    }

    this.metricNode = metricNode;
    this.toolTipText = toolTipText;
    initComponents();

    setOpaque(false);
    setLayout(new GridBagLayout());

    checkBox = new BarCheckBox(hot);
    checkBox.setSelected(isSelected);
    checkBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            //                metricNodeClicked(false);
            //                AnWindow.getInstance().getSettings().getMetricsSetting().set(this,
            // metricNode.getMetricBasic().getName(), isSelected());
            //
            // AnWindow.getInstance().getSettings().getMetricsSetting().set(AnWindow.getInstance().getOverviewView(), metricNode.getMetricBasic().getName(), isSelected());
            //                System.out.println("----------------------------------");
            ((SelectableMetricNode) metricNode).setSelected(checkBox.isSelected(), false);
            processSelections();
            //                List<MetricNameSelected> metricNameSelected = new
            // ArrayList<MetricNameSelected>();
            //                for (CheckBoxBarPanel cbbp : checkBoxBarPanelList) {
            ////                    System.out.println(cbbp.metricNode.getMetricBasic().getName() +
            // " " + cbbp.checkBox.isSelected());
            //                    metricNameSelected.add(new
            // MetricNameSelected(cbbp.metricNode.getMetricBasic().getName(),
            // cbbp.checkBox.isSelected()));
            //                }
            //
            // AnWindow.getInstance().getSettings().getMetricsSetting().setList(AnWindow.getInstance().getOverviewView(), metricNameSelected);
          }
        });

    JLabel label = new JLabel(text);
    AnUtility.setAccessibleContext(label.getAccessibleContext(), label.getText());
    label.setFont(Overview.defaultPlainFont);
    label.setOpaque(false);
    setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    int x = 0;
    gridBagConstraints.gridy = 0;

    if (keyColorList != null) {
      Component colorKeyComponent = new BarColorKey(keyColorList);
      gridBagConstraints.gridx = x++;
      gridBagConstraints.insets = new Insets(0, 0, 0, 4);
      add(colorKeyComponent, gridBagConstraints);
    } else {
      maxLength += BarColorKey.barWidth + 4; // make up for not painting color key
    }

    gridBagConstraints.gridx = x++;
    gridBagConstraints.insets = new Insets(1, 0, 0, 0);
    add(label, gridBagConstraints);

    int labelLength = getCheckboxFixedPosition();
    if (treeLevel >= 1) {
      labelLength -= 14;
      labelLength -= treeLevel * 20;
    }
    if (nodeLevel >= 3) {
      labelLength -= (nodeLevel - 2) * 14;
    }
    if (bar != null) {
      labelLength -= Bar.barWidth;
    }
    if (keyColorList != null) {
      labelLength -= (BarColorKey.barWidth + 4);
    }
    text = text + "    ";
    label.setText(text);
    while (label.getPreferredSize().width < labelLength - 20) {
      text += "  .";
      label.setText(text);
    }
    int w = label.getPreferredSize().width;
    int hwMetricOffset = 0;
    if (metricNode != null && metricNode.getMetricBasic() != null) {
      if (metricNode.getMetricType() == MetricNode.MetricType.PLAIN_ROOT
          || metricNode.getMetricType() == MetricNode.MetricType.PLAIN_CHILD) {
        hwMetricOffset = 4;
      }
    }
    Dimension dim = new Dimension(labelLength - w - hwMetricOffset, BAR_PANEL_HEIGHT);
    Box.Filler filler = new Box.Filler(dim, dim, dim);
    gridBagConstraints.gridx = x++;
    add(filler, gridBagConstraints);

    setToolTip();
    if (bar != null && bar.getValue() >= 0) {
      gridBagConstraints.gridx = x++;
      add(bar, gridBagConstraints);
    }

    gridBagConstraints.gridx = x++;
    int offSet = 10;
    if (bar != null && bar.noCompareBars() > 0) {
      offSet = Bar.BAR_MAX_PERCENT * Bar.barWidth / 100 - bar.getPanelWidth() + 10;
    }
    gridBagConstraints.insets = new Insets(0, offSet, 0, 0);
    JLabel hotMarker = new JLabel();
    AnUtility.setAccessibleContext(hotMarker.getAccessibleContext(), AnLocale.getString("Hot"));
    if (hot) {
      hotMarker.setIcon(AnUtility.hot_icon);
    } else {
      hotMarker.setPreferredSize(
          new Dimension(AnUtility.hot_icon.getIconWidth(), AnUtility.hot_icon.getIconHeight()));
    }
    add(hotMarker, gridBagConstraints);

    gridBagConstraints.gridx = x++;
    gridBagConstraints.insets = new Insets(0, 2, 0, 5);
    add(checkBox, gridBagConstraints);

    if (metricNode != null && metricNode instanceof SelectableMetricNode) {
      ((SelectableMetricNode) metricNode)
          .addChangeListener(
              new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                  Object o = e.getSource();
                  if (o instanceof SelectableMetricNode) {
                    boolean selected = ((SelectableMetricNode) o).isSelected();
                    getCheckBox().setSelected(selected);
                  }
                }
              });
    }
    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
              checkBox.doClick();
            }
          }
        });
  }

  private static void processSelections() {
    synchronized (selectionsLock) {
      if (selectionsThread != null && selectionsThread.isAlive()) {
        selectionsThread.interrupt();
      }
      selectionsThread =
          AnUtility.dispatchOnAWorkerThread(
              new Runnable() {
                @Override
                public void run() {
                  boolean interrupted = false;
                  try {
                    Thread.sleep(1000);
                  } catch (InterruptedException ie) {
                    interrupted = true;
                  }
                  ;
                  if (!interrupted) {
                    List<MetricNameSelected> metricNameSelected =
                        new ArrayList<MetricNameSelected>();
                    for (CheckBoxBarPanel cbbp : checkBoxBarPanelList) {
                      metricNameSelected.add(
                          new MetricNameSelected(
                              cbbp.metricNode.getMetricBasic().getName(),
                              cbbp.checkBox.isSelected()));
                    }
                    AnWindow.getInstance()
                        .getSettings()
                        .getMetricsSetting()
                        .setList(AnWindow.getInstance().getOverviewView(), metricNameSelected);
                  }
                }
              },
              "processSelections");
    }
  }

  protected static void resetCheckBoxBarPanelList() {
    checkBoxBarPanelList = new ArrayList<CheckBoxBarPanel>();
  }

  /**
   * @return the checkboxFixedPosition
   */
  public static int getCheckboxFixedPosition() {
    if (checkboxFixedPosition < 0) {
      int fontSize = new JLabel().getFont().getSize();
      if (fontSize <= 12) {
        checkboxFixedPosition = MetricsGUI.getInstance().getMetricDisplayNameBaseWidth();
      } else {
        checkboxFixedPosition =
            MetricsGUI.getInstance().getMetricDisplayNameBaseWidth() + (fontSize - 12) * 25;
      }
    }
    //        System.out.println(checkboxFixedPosition);
    return checkboxFixedPosition;
  }

  private void setToolTip() {
    if (toolTipText != null) {
      if (metricNode.getMetricBasic().getShortDescription() != null) {
        StringBuilder buf = new StringBuilder();
        buf.append("<html>");
        buf.append(toolTipText);
        buf.append("<br>");
        buf.append(metricNode.getMetricBasic().getShortDescription());
        buf.append("<html>");
        setToolTipText(buf.toString());
      } else {
        setToolTipText(toolTipText);
      }
    }
  }

  @Override
  public Insets getInsets() {
    if (insets != null) {
      return insets;
    } else {
      return super.getInsets();
    }
  }

  public void setInsets(Insets insets) {
    this.insets = insets;
  }

  /**
   * @return the checkBox
   */
  public JCheckBox getCheckBox() {
    return checkBox;
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setSelected(boolean selected) {
    checkBox.setSelected(selected);
    setToolTip();
  }

  /**
   * @param hasFocus the hasFocus to set
   */
  public void setHasFocus(boolean hasFocus) {
    this.hasFocus = hasFocus;
  }

  /**
   * @return the hasFocus
   */
  public boolean isHasFocus() {
    return hasFocus;
  }

  class BarCheckBox extends JCheckBox {

    public BarCheckBox(boolean hot) {
      setOpaque(false);
      setMargin(new Insets(0, 0, 0, 0));
      setBorder(null);
      setPreferredSize(new Dimension(17, 17));
      AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Metric selector"));
    }

    @Override
    public Insets getInsets() {
      return new Insets(0, 2, 0, 0);
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      if (isFocusOwner() || hasFocus) {
        // For a11y...
        g.setColor(new Color(122, 138, 153));
        g.drawLine(16, 2, 16, 14);
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

    setLayout(new java.awt.GridBagLayout());
  } // </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
}
