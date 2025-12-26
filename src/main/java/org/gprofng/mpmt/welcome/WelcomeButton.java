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

package org.gprofng.mpmt.welcome;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.UIManager;

public class WelcomeButton extends JButton {

  private final Color foreground;

  public WelcomeButton(
      String text, String tooltip, Action action, int txtSize, Color foreground, int style) {
    super(action);
    setText(text);
    setToolTipText(tooltip);
    setIcon(null);
    AnUtility.setAccessibleContext(getAccessibleContext(), text);
    this.foreground = foreground;
    setBorder(null);
    setBorderPainted(false);
    setOpaque(false);
    setBackground(AnEnvironment.DEFAULT_PANEL_BACKGROUND);
    setForeground(foreground);
    setFont(
        getFont()
            .deriveFont(12f)); // Use font size 12 for now. Doesn't resize with --fontsize option
    setFont(getFont().deriveFont(style));
    setFont(getFont().deriveFont((float) getFont().getSize() + txtSize));
    if (action != null) {
      addMouseListener(
          new MouseAdapter() {
            private Cursor oldCursor = null;
            // @Override

            @Override
            public void mouseEntered(MouseEvent e) {
              oldCursor = getCursor();
              setCursor(new Cursor(Cursor.HAND_CURSOR));
              setForeground(AnEnvironment.WELCOME_BUTTON_FOCUSED_COLOR);
            }

            // @Override
            @Override
            public void mouseExited(MouseEvent e) {
              setCursor(oldCursor);
              setForeground(WelcomeButton.this.foreground);
            }
          });
      addKeyListener(
          new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
              if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                actionListener.actionPerformed(null);
              }
            }
          });
    }
  }

  @Override
  public void paint(Graphics g) {
    if (!isEnabled()) {
      Color saveColor = (Color) UIManager.getDefaults().get("Button.disabledText");
      UIManager.getDefaults()
          .put("Button.disabledText", AnEnvironment.WELCOME_BUTTON_DISABLED_COLOR);
      super.paint(g);
      UIManager.getDefaults().put("Button.disabledText", saveColor);
    } else {
      super.paint(g);
      if (hasFocus()) {
        g.setColor(new Color(200, 200, 200));
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
      }
    }
  }
}
