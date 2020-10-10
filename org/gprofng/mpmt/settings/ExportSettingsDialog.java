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

package org.gprofng.mpmt.settings;

import static org.gprofng.mpmt.AnWindow.copyToRemote;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.Analyzer;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.Frame;
import java.util.List;

public class ExportSettingsDialog extends AnDialog2 {

  private ExportSettingsPanel saveSettingsPanel;

  public ExportSettingsDialog(Frame owner) {
    super(owner, owner, AnLocale.getString("Export Settings"));
    saveSettingsPanel = new ExportSettingsPanel(this);
    setCustomPanel(saveSettingsPanel);
    setHelpTag(AnVariable.HELP_ExportSettings);
    getOKButton().setText(AnLocale.getString("Export"));
    //        getOKButton().setMnemonic(AnLocale.getString('x', "EXPORT_SETTINGS_DIALOG_SAVE_MN"));
    getOKButton().setToolTipText(AnLocale.getString("Export selected settings"));
    //        getHelpButton().setEnabled(false);
  }

  @Override
  protected void setStatus(Status status) {
    super.setStatus(status);
    if (status == Status.OK) {
      String configurationPath = saveSettingsPanel.getConfigurationPath();
      List<UserPref.What> what = saveSettingsPanel.getWhat();

      String localPath = configurationPath;
      boolean homeDefaultConfiguration =
          configurationPath.startsWith(UserPref.getAnalyzerDirPath());
      if (Analyzer.getInstance().isRemote() && !homeDefaultConfiguration) {
        localPath = AnWindow.tempFile().getAbsolutePath();
      }
      UserPref.getInstance().save(localPath, what);
      if (Analyzer.getInstance().isRemote() && !homeDefaultConfiguration) {
        int res = copyToRemote(localPath, configurationPath);
        if (res < 0) {
          System.err.println("SaveSettingsDialog:copyToRemote failed: " + res);
        }
      }

      UserPref.getInstance().setLastExportImportConfPath(configurationPath);
    }
  }
}
