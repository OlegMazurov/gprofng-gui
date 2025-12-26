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
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSeparator;

class StatusLabel {

  private JSeparator separator;
  private JLabel textLabel;
  private JLabel valueLabel;
  private ActionListener actionListener;
  private MouseListener mouseListener = null;
  private KeyboardListener keyboardListener = null;

  /**
   * @return the separator
   */
  protected JSeparator getSeparator() {
    return separator;
  }

  /**
   * @param separator the separator to set
   */
  protected void setSeparator(JSeparator separator) {
    this.separator = separator;
  }

  /**
   * @return the labelTxt
   */
  protected JLabel getTextLabel() {
    return textLabel;
  }

  protected void setTextLabelIcon(ImageIcon icon) {
    textLabel.setIcon(icon);
  }

  /**
   * @param textLabel the labelTxt to set
   */
  protected void setTextLabel(final JLabel textLabel) {
    this.textLabel = textLabel;
    if (textLabel != null) {
      textLabel.addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
              textLabel.setBorder(
                  BorderFactory.createLineBorder(AnEnvironment.STATUS_SELECTED_BORDER_COLOR));
            }

            @Override
            public void focusLost(FocusEvent e) {
              textLabel.setBorder(null);
            }
          });
    }
  }

  /**
   * @return the labelValue
   */
  protected JLabel getValueLabel() {
    return valueLabel;
  }

  /**
   * @param valueLabel the labelValue to set
   */
  protected void setValueLabel(final JLabel valueLabel) {
    this.valueLabel = valueLabel;
    if (valueLabel != null) {
      valueLabel.addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
              valueLabel.setBorder(
                  BorderFactory.createLineBorder(AnEnvironment.STATUS_SELECTED_BORDER_COLOR));
            }

            @Override
            public void focusLost(FocusEvent e) {
              valueLabel.setBorder(null);
            }
          });
    }
  }

  public void setEnabled(final boolean enabled) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            if (enabled && getValueLabel().isEnabled()) {
              return;
            }
            if (!enabled && !getValueLabel().isEnabled()) {
              return;
            }
            if (!enabled && getMouseListener() != null) {
              textLabel.removeMouseListener(getMouseListener());
              valueLabel.removeMouseListener(getMouseListener());
            }
            //        getTextLabel().setEnabled(enabled);
            getValueLabel().setEnabled(enabled);
            if (getSeparator() != null) {
              getSeparator().setEnabled(enabled);
            }
            if (enabled && getMouseListener() != null) {
              textLabel.addMouseListener(getMouseListener());
              valueLabel.addMouseListener(getMouseListener());
            }
          }
        });
  }

  public void setVisible(final boolean visible) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            getTextLabel().setVisible(visible);
            getValueLabel().setVisible(visible);
            if (getSeparator() != null) {
              getSeparator().setVisible(visible);
            }
          }
        });
  }

  protected void addActionListener(ActionListener actionListener) {
    this.actionListener = actionListener;
    if (actionListener != null) {
      textLabel.addMouseListener(getMouseListener());
      textLabel.addKeyListener(getKeyboardListener());
      valueLabel.addMouseListener(getMouseListener());
      valueLabel.addKeyListener(getKeyboardListener());
    }
  }

  private MouseListener getMouseListener() {
    if (mouseListener == null) {
      mouseListener = new MouseListener();
    }
    return mouseListener;
  }

  class MouseListener extends MouseAdapter {
    private Cursor oldCursor = null;

    // @Override
    @Override
    public void mouseEntered(MouseEvent e) {
      oldCursor = textLabel.getCursor();
      textLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
      valueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // @Override
    @Override
    public void mouseExited(MouseEvent e) {
      textLabel.setCursor(oldCursor);
      valueLabel.setCursor(oldCursor);
    }

    // @Override
    @Override
    public void mouseClicked(MouseEvent e) {
      if (actionListener != null) {
        actionListener.actionPerformed(null);
      }
    }
  }

  private KeyboardListener getKeyboardListener() {
    if (keyboardListener == null) {
      keyboardListener = new KeyboardListener();
    }
    return keyboardListener;
  }

  class KeyboardListener extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyChar() == ' ' && actionListener != null) {
        actionListener.actionPerformed(null);
      }
    }
  }
}
