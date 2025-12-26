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
package org.gprofng.mpmt;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Icon;
import javax.swing.JButton;

public class AnIconButton extends JButton implements MouseListener, FocusListener {

  private Insets insets = new Insets(2, 2, 2, 2);

  public AnIconButton(Icon icon) {
    super(icon);
    setBorder(javax.swing.BorderFactory.createLineBorder(new Color(115, 115, 115)));
    setOpaque(false);
    setContentAreaFilled(false);
    setBorderPainted(false);

    addMouseListener(this);
    addFocusListener(this);
  }

  @Override
  public Insets getInsets() {
    return insets;
  }

  @Override
  public void setMargin(Insets m) {
    super.setMargin(m);
    insets = m;
  }

  @Override
  public void mouseExited(MouseEvent e) {
    setBorderPainted(false);
    validate();
    repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {
    setBorderPainted(true);
    validate();
    repaint();
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (isEnabled()) {
      setContentAreaFilled(true);
      validate();
      repaint();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    setContentAreaFilled(false);
    validate();
    repaint();
  }

  @Override
  public void focusGained(FocusEvent e) {
    setBorderPainted(true);
    validate();
    repaint();
  }

  @Override
  public void focusLost(FocusEvent e) {
    setBorderPainted(false);
    validate();
    repaint();
  }
}
