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

package org.gprofng.mpmt.remote;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

public class CheckBoxList extends JList<JCheckBox> {

  private DefaultListModel<JCheckBox> model;

  public CheckBoxList() {
    model = new DefaultListModel<JCheckBox>();
    setModel(model);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setCellRenderer(new CellRenderer());
    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyTyped(KeyEvent evt) {
            super.keyTyped(evt);
            if (evt.getKeyChar() == ' ') {
              JCheckBox checkBox = getSelectedValue();
              if (checkBox != null) {
                checkBox.setSelected(!checkBox.isSelected());
                repaint(100);
              }
            }
          }
        });
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent evt) {
            if (evt.getX() < 20) {
              int row = locationToIndex(evt.getPoint());
              JCheckBox checkBox = model.getElementAt(row);
              if (checkBox != null) {
                checkBox.setSelected(!checkBox.isSelected());
                repaint(100);
              }
            }
          }
        });
  }

  private class CellRenderer implements ListCellRenderer<JCheckBox> {

    @Override
    public Component getListCellRendererComponent(
        JList<? extends JCheckBox> list,
        JCheckBox checkBox,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      if (isSelected) {
        checkBox.setBackground(getSelectionBackground());
        checkBox.setForeground(getSelectionForeground());
      } else {
        checkBox.setBackground(getBackground());
        checkBox.setForeground(getForeground());
      }
      return checkBox;
    }
  }

  public void addElement(JCheckBox checkBox) {
    model.addElement(checkBox);
  }
}
