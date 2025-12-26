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


package org.gprofng.mpmt.filter;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

/**
 * @author tpreisle
 */
public class CustomFilterAction extends AbstractAction {
  public JMenuItem jmi = null;

  public CustomFilterAction() {
    super(AnLocale.getString("Add Filter: Advanced Custom Filter..."));
  }

  public CustomFilterAction(String name) {
    super(AnLocale.getString(name));
  }

  public void actionPerformed(ActionEvent ev) {
    AnWindow anWindow = AnWindow.getInstance();
    anWindow.getFilters().showCustomFilterDialog();
  }
}
