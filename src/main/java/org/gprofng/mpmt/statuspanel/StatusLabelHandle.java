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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnUtility;
import javax.swing.JLabel;

public class StatusLabelHandle {
  private JLabel label;
  private StatusComponent statusComponent;

  protected StatusLabelHandle(
      AnWindow anWindow, String labelTxt, String toolTipText, StatusPanel.Orientation orientation) {
    label = new JLabel(labelTxt);
    label.setToolTipText(toolTipText);
    statusComponent = anWindow.getStatusPanel().addStatusComponent(label, orientation);
  }

  public void setText(String text) {
    label.setText(text);
  }

  public String getText() {
    return label.getText();
  }

  public void update(final String text) {
    AnUtility.dispatchOnSwingThread(
        new Runnable() {
          @Override
          public void run() {
            label.setText(text);
          }
        });
  }

  public void updateToolTip(String text) {
    label.setToolTipText(text);
  }

  public void setVisible(boolean visible) {
    statusComponent.getComponent().setVisible(visible);
    if (statusComponent.getSeparator() != null) {
      statusComponent.getSeparator().setVisible(visible);
    }
  }

  public void setEnabled(final boolean enabled) {
    statusComponent.getComponent().setEnabled(enabled);
    if (statusComponent.getSeparator() != null) {
      statusComponent.getSeparator().setEnabled(enabled);
    }
  }

  public boolean isVisible() {
    return statusComponent.getComponent().isVisible();
  }
}
