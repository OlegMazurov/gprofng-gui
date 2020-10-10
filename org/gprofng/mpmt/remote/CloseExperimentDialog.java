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

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.Frame;

public class CloseExperimentDialog extends AnDialog2 {

  public CloseExperimentDialog(Frame owner) {
    super(owner, owner, AnLocale.getString("Connect to Remote Host"));
    CloseExperimentPanel panel = new CloseExperimentPanel();
    setCustomPanel(panel);
    getOKButton().setText(AnLocale.getString("Close"));
    getOKButton().setToolTipText(AnLocale.getString("Close experiment(s) before continuing"));
  }
}
