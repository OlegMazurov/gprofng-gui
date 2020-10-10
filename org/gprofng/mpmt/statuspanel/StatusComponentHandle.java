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
import javax.swing.JComponent;

public class StatusComponentHandle {
  private StatusComponent statusComponent;

  protected StatusComponentHandle(
      AnWindow anWindow, JComponent component, StatusPanel.Orientation orientation) {
    statusComponent = anWindow.getStatusPanel().addStatusComponent(component, orientation);
  }

  public void setVisible(boolean visible) {
    statusComponent.getComponent().setVisible(visible);
    if (statusComponent.getSeparator() != null) {
      statusComponent.getSeparator().setVisible(visible);
    }
  }
}
