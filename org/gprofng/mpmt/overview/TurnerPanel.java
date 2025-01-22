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


package org.gprofng.mpmt.overview;

import org.gprofng.mpmt.AnIconButton;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box.Filler;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TurnerPanel extends javax.swing.JPanel {
  private boolean expanded = true;
  private Filler filler;
  private static final int fillerWith = 14;
  private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
  private JButton turnerButton;

  public TurnerPanel(
      JComponent topComponent,
      JComponent bottomComponent,
      int turnerButtonInset,
      boolean align,
      boolean expanded,
      Color background) {
    initComponents();

    turnerButton = new AnIconButton(AnUtility.hollowArrowRightIcon);
    turnerButton.setBorder(null);
    turnerButton.setBorderPainted(false);
    //        turnerButton.addActionListener(new java.awt.event.ActionListener() {
    //            @Override
    //            public void actionPerformed(java.awt.event.ActionEvent evt) {
    //                TurnerPanel.this.expanded = !TurnerPanel.this.expanded;
    //                updateState();
    //            }
    //        });
    turnerButton.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            TurnerPanel.this.expanded = !TurnerPanel.this.expanded;
            updateState();
          }
        });
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, turnerButtonInset, 0, 1);
    add(turnerButton, gridBagConstraints);

    topComponent.setFont(topComponent.getFont().deriveFont(Font.BOLD));
    setBackground(background);
    turnerButton.setBackground(background);
    turnerButton.setOpaque(false);
    topPanel.setOpaque(false);
    topPanel.add(topComponent);

    bottomPanel.setOpaque(false);
    if (bottomComponent != null) {
      bottomPanel.add(bottomComponent);
      Dimension dim = new Dimension(align ? fillerWith : 3, 0);
      filler = new javax.swing.Box.Filler(dim, dim, dim);
      bottomPanel.add(filler, BorderLayout.WEST);
    }

    this.expanded = expanded;
    updateState();

    turnerButton.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            //                System.out.println("turnerButton: " + e);
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
              TurnerPanel.this.expanded = true;
              updateState();
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
              TurnerPanel.this.expanded = false;
              updateState();
            }
          }
        });
  }

  public void setBottomPanelComponent(JComponent bottomComponent) {
    bottomPanel.add(bottomComponent);
    Dimension dim = new Dimension(18, 0);
    filler = new javax.swing.Box.Filler(dim, dim, dim);
    bottomPanel.add(filler, BorderLayout.WEST);
  }

  public JButton getTurnerButton() {
    return turnerButton;
  }

  private void updateState() {
    if (expanded) {
      turnerButton.setToolTipText(AnLocale.getString("Collapse"));
      turnerButton.setIcon(AnUtility.hollowArrowDownIcon);
    } else {
      turnerButton.setToolTipText(AnLocale.getString("Expand"));
      turnerButton.setIcon(AnUtility.hollowArrowRightIcon);
    }
    bottomPanel.setVisible(expanded);
    fireChange();
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void addChangeListener(ChangeListener changeListener) {
    changeListeners.add(changeListener);
  }

  public void removeChangeListener(ChangeListener changeListener) {
    changeListeners.remove(changeListener);
  }

  private void fireChange() {
    ChangeEvent changeEvent = new ChangeEvent(this);
    for (ChangeListener changeListener : changeListeners) {
      changeListener.stateChanged(changeEvent);
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

    topPanel = new javax.swing.JPanel();
    bottomPanel = new javax.swing.JPanel();

    setLayout(new java.awt.GridBagLayout());

    topPanel.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(topPanel, gridBagConstraints);

    bottomPanel.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    add(bottomPanel, gridBagConstraints);
  } // </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel bottomPanel;
  private javax.swing.JPanel topPanel;
  // End of variables declaration//GEN-END:variables
}
