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


package org.gprofng.mpmt.statuspanel;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class StatusLabelValueHandle {
  public enum Mode {
    DEFAULT,
    SET,
    ERROR
  };

  public static final Color DEFAULT_COLOR = Color.BLACK;
  public static final Color SET_COLOR = new Color(44, 121, 6);
  public static final Color ERROR_COLOR = Color.RED;

  private StatusLabel statusLabel;

  protected StatusLabelValueHandle(
      AnWindow anWindow,
      ImageIcon icon,
      String labelTxt,
      String toolTipText,
      ActionListener actionListener) {
    String txt = null;
    if (labelTxt != null) {
      txt = labelTxt + ": ";
    }
    statusLabel = anWindow.getStatusPanel().addStatusLabel(icon, txt, toolTipText, actionListener);
  }

  public String get() {
    return statusLabel.getValueLabel().getText();
  }

  public void update(String text) {
    update(text, Mode.DEFAULT);
  }

  public void updateToolTip(String text) {
    statusLabel.getTextLabel().setToolTipText(text);
    statusLabel.getValueLabel().setToolTipText(text);
  }

  public void update(String text, Mode mode) {
    Color color = DEFAULT_COLOR;
    if (mode == Mode.DEFAULT) {
      color = DEFAULT_COLOR;
    } else if (mode == Mode.SET) {
      color = SET_COLOR;
    } else if (mode == Mode.ERROR) {
      color = ERROR_COLOR;
    }
    update(text, color);
  }

  private void update(final String valueText, final Color color) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            statusLabel.getValueLabel().setForeground(color);
            statusLabel.getValueLabel().setText(valueText);
          }
        });
  }

  public void update(final ImageIcon icon) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            statusLabel.setTextLabelIcon(icon);
          }
        });
  }

  public void setVisible(boolean visible) {
    statusLabel.setVisible(visible);
  }

  public void setEnabled(final boolean enabled) {
    statusLabel.setEnabled(enabled);
  }

  public boolean isVisible() {
    return statusLabel.getValueLabel().isVisible();
  }

  /**
   * @return the text label
   */
  public JLabel getTextLabel() {
    return statusLabel.getTextLabel();
  }

  /**
   * @return the value label
   */
  public JLabel getValueLabel() {
    return statusLabel.getValueLabel();
  }
}
