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

package org.gprofng.mpmt.compare;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public class CompareSimpleDialog extends AnDialog2 {
  private CompareSimplePanel compareSimplePanel;

  public CompareSimpleDialog(Frame owner) {
    super(owner, owner, AnLocale.getString("Compare Experiments"));
    setHelpTag(AnVariable.HELP_Compare);
    compareSimplePanel = new CompareSimplePanel(this);
    setCustomPanel(compareSimplePanel);
    JButton advancedButton = new JButton(AnLocale.getString("Advanced"));
    advancedButton.setMnemonic(AnLocale.getString('D', "CompareSimpleDialog_Advanced"));
    advancedButton.setToolTipText(AnLocale.getString("Advanced options (aggregating experiments)"));
    advancedButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
            AnWindow.getInstance().compareExperimentsAdvancedDialog();
          }
        });
    addAuxButton(advancedButton);
    pack();
  }

  @Override
  protected void setStatus(Status status) {
    super.setStatus(status);
    if (status == Status.OK) {
      String[][] groups = compareSimplePanel.getExperimentGroups();
      AnWindow anWindow = AnWindow.getInstance();
      anWindow.loadExperimentGroups(groups, null, !anWindow.experimentsLoaded(), null, false);
    }
  }
}
