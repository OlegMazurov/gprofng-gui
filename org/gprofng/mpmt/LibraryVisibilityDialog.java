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

package org.gprofng.mpmt;

import org.gprofng.mpmt.picklist.StringPickList;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.Frame;
import java.util.List;

public class LibraryVisibilityDialog extends AnDialog2 {

  private final LibraryVisibilityPanel libraryVisibilityPanel;

  public LibraryVisibilityDialog(Frame owner) {
    super(owner, owner, AnLocale.getString("Library and Class Visibility"));
    libraryVisibilityPanel = new LibraryVisibilityPanel(this);
    setHelpTag(AnVariable.HELP_ShowHideFunctions);
    setCustomPanel(libraryVisibilityPanel);
    getHelpButton().setVisible(true);
  }

  @Override
  public void setVisible(boolean b) {
    if (b) {
      libraryVisibilityPanel.reset();
    }
    super.setVisible(b);
  }

  @Override
  protected void setStatus(Status status) {
    super.setStatus(status);
    if (status == Status.OK) {
      libraryVisibilityPanel.save();
    }
  }

  public void initStates(
      boolean java,
      List<String> initStates,
      StringPickList hideFuncsIncludePickList,
      String currentIncludeFilter,
      StringPickList hideFuncsExcludePickList,
      String currentExcludeFilter) {
    libraryVisibilityPanel.initStates(
        java,
        hideFuncsIncludePickList,
        currentIncludeFilter,
        hideFuncsExcludePickList,
        currentExcludeFilter);
  }

  // For persistance
  public boolean isJava() {
    return libraryVisibilityPanel.isJava();
  }
}
