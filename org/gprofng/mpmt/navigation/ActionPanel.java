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


package org.gprofng.mpmt.navigation;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class ActionPanel extends javax.swing.JPanel implements MouseListener, MouseMotionListener {
  private ActionListener actionListener;
  private boolean focused = false;
  private boolean selected = false;
  private boolean keyboardFocus = false;

  public ActionPanel(String name, String tooltip, ActionListener actionListener) {
    initComponents();
    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
    nameLabel.setText(name);
    AnUtility.setAccessibleContext(nameLabel.getAccessibleContext(), nameLabel.getText());
    setToolTipText(tooltip);
    setBorder(null);
    setBackground(
        AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR /*AnEnvironment.DEFAULT_PANEL_BACKGROUND*/);
    addMouseListener(this);
    addMouseMotionListener(this);
    this.actionListener = actionListener;
  }

  // @Override
  @Override
  public void mouseClicked(MouseEvent me) {
    ActionEvent ae = new ActionEvent(me, 0, null);
    actionListener.actionPerformed(ae);
  }

  // @Override
  @Override
  public void mouseDragged(MouseEvent me) {}

  // @Override
  @Override
  public void mouseEntered(MouseEvent me) {
    focused = true;
    refresh();
  }

  // @Override
  @Override
  public void mouseExited(MouseEvent me) {
    focused = false;
    refresh();
  }

  // @Override
  @Override
  public void mouseReleased(MouseEvent me) {
    selected = false;
    refresh();
  }

  // @Override
  @Override
  public void mousePressed(MouseEvent me) {
    selected = true;
    refresh();
  }

  // @Override
  @Override
  public void mouseMoved(MouseEvent me) {}

  public void setSelected(boolean selected) {
    this.selected = selected;
    refresh();
  }

  private void refresh() {
    // Set background
    Color backgroundColor;
    if (isSelected()) {
      backgroundColor = AnEnvironment.VIEW_SELECTED_BACKGROUND_COLOR;
    } else if (focused) {
      backgroundColor = AnEnvironment.VIEW_FOCUSED_BACKGROUND_COLOR;
    } else {
      backgroundColor = AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR;
    }
    setBackground(backgroundColor);

    repaint();
  }

  @Override
  public void paint(Graphics g) {
    if (selected || focused) {
      Graphics2D g2d = (Graphics2D) g;
      Color topColor =
          selected ? AnEnvironment.VIEW_SELECTED_TOP_COLOR : AnEnvironment.VIEW_FOCUSED_TOP_COLOR;
      Color bottomColor =
          selected
              ? AnEnvironment.VIEW_SELECTED_BOTTOM_COLOR
              : AnEnvironment.VIEW_FOCUSED_BOTTOM_COLOR;
      GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
      g2d.setPaint(gp);
      g2d.fillRect(0, 0, getWidth(), getHeight());

      // Set border
      Color borderColor;
      if (selected || focused) {
        borderColor = AnEnvironment.VIEW_SELECTED_BORDER_COLOR;
      } else {
        borderColor = AnEnvironment.NAVIGATION_PANEL_BACKGROUND_COLOR;
      }
      g.setColor(borderColor);
      g.drawLine(0, 0, getWidth(), 0);
      g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

      setOpaque(false);
    } else {
      setOpaque(true);
    }
    super.paint(g);
    if (keyboardFocus) {
      g.setColor(new Color(180, 180, 180));
      g.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
    }
  }

  public void performAction() {
    actionListener.actionPerformed(new ActionEvent(this, 0, null));
  }

  /**
   * @return the keyboardFocus
   */
  public boolean hasKeyboardFocus() {
    return keyboardFocus;
  }

  /**
   * @param keyboardFocused the keyboardFocus to set
   */
  public void setKeyboardFocused(boolean keyboardFocused) {
    this.keyboardFocus = keyboardFocused;
  }

  // @Override
  @Override
  public String toString() {
    return nameLabel.getText();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    nameLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    nameLabel.setText("NOI18N");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 0);
    add(nameLabel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel nameLabel;
  // End of variables declaration//GEN-END:variables

  /**
   * @return the selected
   */
  public boolean isSelected() {
    return selected;
  }
}
