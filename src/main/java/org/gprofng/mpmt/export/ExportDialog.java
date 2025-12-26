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

package org.gprofng.mpmt.export;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.Frame;

public class ExportDialog extends AnDialog2 {

  private final ExportPanel exportPanel;

  public ExportDialog(Frame owner, ExportSupport exportSupport) {
    super(owner, owner, AnLocale.getString("Export"));
    exportPanel = new ExportPanel(this, exportSupport);
    setCustomPanel(exportPanel);
    setHelpTag(AnVariable.HELP_Export);
    getOKButton().setText(AnLocale.getString("Export"));
    //        getOKButton().setMnemonic(AnLocale.getString('E', "EXPORT_DIALOG_SAVE_MN")); );
    getOKButton().setToolTipText(AnLocale.getString("Save the file"));
  }

  public String getOutputFilePath() {
    return exportPanel.getOutputFilePath();
  }

  public ExportSupport.ExportFormat getExportFormat() {
    return exportPanel.getExportFormat();
  }

  public Character getDelimiter() {
    return exportPanel.getDelimiter();
  }

  public Integer getLimit() {
    return exportPanel.getLimit();
  }

  public Boolean getIncludeSubviews() {
    return exportPanel.getIncludeSubviews();
  }

  @Override
  protected void setStatus(Status status) {
    super.setStatus(status);
    if (status == Status.OK) {
      exportPanel.saveStates();
    }
  }
}
