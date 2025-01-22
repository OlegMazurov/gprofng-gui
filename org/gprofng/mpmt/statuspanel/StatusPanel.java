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


package org.gprofng.mpmt.statuspanel;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class StatusPanel extends JPanel {
  public enum Orientation {
    LEFT,
    RIGHT
  };

  private int gridxLeft = 0;
  private int gridxCenter = 50;
  private int gridxRight = 100;

  public StatusPanel() {
    initComponents();
    //        setBackground(AnEnvironment.STATUS_PANEL_BACKGROUND_COLOR);
    //        setTopBorderColor(AnEnvironment.STATUS_PANEL_BORDER_COLOR);
    //        setTopBorderColor(Color.red);
    setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AnEnvironment.STATUS_PANEL_BORDER_COLOR));
    addCenterFill();
  }

  @Override
  protected void paintComponent(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    Graphics2D g2d = (Graphics2D) g;
    GradientPaint gp;
    gp =
        new GradientPaint(
            0,
            0,
            AnEnvironment.STATUS_PANEL_BACKGROUND_COLOR1,
            0,
            height,
            AnEnvironment.STATUS_PANEL_BACKGROUND_COLOR2);
    g2d.setPaint(gp);
    g2d.fillRect(0, 0, width, height);
  }

  private final void addCenterFill() {
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridxCenter;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    add(new JLabel(""), gridBagConstraints);
  }

  protected final StatusComponent addStatusComponent(
      JComponent component, Orientation orientation) {
    StatusComponent statusComponent = new StatusComponent();
    statusComponent.setComponent(component);
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    if (orientation == Orientation.LEFT) {
      if (gridxLeft > 0) {
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridxLeft++;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(6, 4, 4, 4);
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        statusComponent.setSeparator(separator);
        add(separator, gridBagConstraints);
      }
      gridBagConstraints.gridx = gridxLeft++;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(4, 4, 2, 8);
      gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    } else {
      gridBagConstraints.gridx = gridxRight--;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(2, 4, 0, gridxRight == 99 ? 18 : 4);
      gridBagConstraints.anchor = GridBagConstraints.LINE_END;
    }
    add(component, gridBagConstraints);
    return statusComponent;
  }

  protected final StatusLabel addStatusLabel(
      ImageIcon icon, String txt, String toolTipText, ActionListener actionListener) {
    StatusLabel statusLabel = new StatusLabel();
    //        String formattedTxt = txt;
    //        int i = txt.indexOf(":");
    //        if (i > 0 ) {
    //            formattedTxt = "<html>" + txt.substring(0, i+1) + "<font color=#2c7906>" +
    // txt.substring(i+1);
    //        }
    JLabel txtLabel = new JLabel();
    if (txt != null) {
      txtLabel.setText(txt);
    }
    if (icon != null) {
      txtLabel.setIcon(icon);
    }
    statusLabel.setTextLabel(txtLabel);
    txtLabel.setFont(txtLabel.getFont().deriveFont(Font.PLAIN));
    if (toolTipText != null) {
      txtLabel.setToolTipText(toolTipText);
    }

    JLabel valueLabel = new JLabel();
    statusLabel.setValueLabel(valueLabel);
    valueLabel.setFont(txtLabel.getFont().deriveFont(Font.PLAIN));
    if (toolTipText != null) {
      valueLabel.setToolTipText(toolTipText);
    }

    GridBagConstraints gridBagConstraints;
    if (gridxLeft > 0) {
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = gridxLeft++;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(6, 4, 4, 4);
      gridBagConstraints.fill = GridBagConstraints.VERTICAL;
      gridBagConstraints.anchor = GridBagConstraints.LINE_START;
      JSeparator separator = new JSeparator(JSeparator.VERTICAL);
      statusLabel.setSeparator(separator);
      add(separator, gridBagConstraints);
    }

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridxLeft++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(4, 6, 2, 0);
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    add(txtLabel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = gridxLeft++;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(4, 2, 2, 8);
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    add(valueLabel, gridBagConstraints);

    if (actionListener != null) {
      statusLabel.addActionListener(actionListener);
    }
    return statusLabel;
  }

  public final void removeStatusComponent(JComponent component) {
    remove(component);
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
