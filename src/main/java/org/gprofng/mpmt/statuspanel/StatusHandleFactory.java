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
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

public class StatusHandleFactory {
  public static StatusLabelValueHandle createStatusLabelValue(
      AnWindow anWindow,
      ImageIcon icon,
      String labelTxt,
      String labelToolTip,
      ActionListener actionListener) {
    return new StatusLabelValueHandle(anWindow, icon, labelTxt, labelToolTip, actionListener);
  }

  public static StatusLabelHandle createStatusLabel(
      AnWindow anWindow,
      String labelTxt,
      String labelToolTip,
      StatusPanel.Orientation orientation) {
    return new StatusLabelHandle(anWindow, labelTxt, labelToolTip, orientation);
  }

  public static StatusComponentHandle createStatusComponent(
      AnWindow anWindow, JComponent component, StatusPanel.Orientation orientation) {
    return new StatusComponentHandle(anWindow, component, orientation);
  }
}
