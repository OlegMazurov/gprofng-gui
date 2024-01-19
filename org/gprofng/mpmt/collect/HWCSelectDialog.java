/* Copyright (C) 2022-2024 Free Software Foundation

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

package org.gprofng.mpmt.collect;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.util.gui.AnDialog2;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;

public class HWCSelectDialog extends AnDialog2 {
  private CollectPanel collectPanel;
  private HWCSelectPanel hwcSelectPanel;

  public HWCSelectDialog(
      Dialog dialog,
      CollectPanel collectPanel,
      List<HWCEntry> hwcFlatList,
      Map<String, HWCEntry> hwcNameMap) {
    super(dialog, collectPanel, AnLocale.getString("Select Hardware Counters"));
    this.collectPanel = collectPanel;
    hwcSelectPanel = new HWCSelectPanel(this, hwcFlatList, hwcNameMap);
    setHelpTag(AnVariable.HELP_CollectDialog);
    setCustomPanel(hwcSelectPanel);
    getHelpButton().setVisible(true);
    getOKButton().setText(AnLocale.getString("Add"));
    getOKButton().setMnemonic((AnLocale.getString('A', "HWCSelectDialog.Add")));
    setModal(false);

    JButton HWCHelpButton = new JButton(AnLocale.getString("Hardware Counter Descriptions"));
    AnUtility.setAccessibleContext(
        HWCHelpButton.getAccessibleContext(), AnLocale.getString("Hardware Counter Help"));
    HWCHelpButton.setMnemonic((AnLocale.getString('C', "HWCSelectDialog.availableCountersButton")));
    HWCHelpButton.setToolTipText(AnLocale.getString("Hardware Counter Help"));
    HWCHelpButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            HWCSelectDialog.this.collectPanel.showAvailableCountersDialog();
          }
        });
    addAuxButton(HWCHelpButton);
  }

  @Override
  public void setVisible(boolean b) {
    if (b) {
      hwcSelectPanel.resetGUI();
    }
    super.setVisible(b);
  }

  protected List<HWCEntry> getSelectedEntries() {
    return hwcSelectPanel.getSelectedEntries();
  }

  @Override
  protected void setStatus(Status status) {
    super.setStatus(status);
    if (getStatus() == AnDialog2.Status.OK) {
      List<HWCEntry> selectedList = getSelectedEntries();
      collectPanel.addHWCEntry(selectedList);
    }
  }
}
