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

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.persistence.UserPref;
import org.gprofng.mpmt.util.gui.AnDialog2;
import java.awt.Frame;
import java.util.List;

public class ImportSettingsDialog extends AnDialog2 {

  ImportSettingsPanel importSettingsPanel;

  public ImportSettingsDialog(Frame owner) {
    super(owner, owner, AnLocale.getString("Import Settings"));
    importSettingsPanel = new ImportSettingsPanel(this);
    setCustomPanel(importSettingsPanel);
    //        setHelpTag(AnVariable.HELP_ImportSettings);
    getOKButton().setText(AnLocale.getString("Import"));
    getOKButton().setToolTipText(AnLocale.getString("Import selected settings"));
  }

  @Override
  protected void setStatus(Status status) {
    super.setStatus(status);
    if (status == Status.OK) {
      UserPref existingUserpref = UserPref.getInstance();
      UserPref userPref = importSettingsPanel.getUserPref();
      Settings settings = AnWindow.getInstance().getSettings();
      List<UserPref.What> whatList = importSettingsPanel.getWhat();

      if (whatList.contains(UserPref.What.VIEWS)) {
        AnWindow.getInstance()
            .getSettings()
            .getViewsSetting()
            .set(
                this,
                userPref.getViewPanelOrder(),
                userPref.getMachineModel(),
                userPref.getCustomIndexObjects(),
                userPref.getCustomMemoryObjects());
      }
      if (whatList.contains(UserPref.What.METRICS)) {
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .set(
                this,
                userPref.getMetricSelectionList(),
                userPref.getMetricReversedSort(),
                userPref.getMetricSortByMTypeList(),
                userPref.getMetricOrderLists());
      }
      if (whatList.contains(UserPref.What.TIMELINE)) {
        AnWindow.getInstance()
            .getSettings()
            .getTimelineSetting()
            .set(
                this,
                userPref.getTimelineGroupDataByButtonName(),
                userPref.getTimelineStackAlign(),
                userPref.getTimelineStackDepth(),
                userPref.getTimelineStackFramePixels(),
                userPref.getTimelineShowEventStates(),
                userPref.getTimelineShowEventDensity());
      }
      if (whatList.contains(UserPref.What.SOURCEDISASSEMBLY)) {
        settings.getSourceDisassemblySetting().set(this, userPref.getSourceDisassemblySettings());
      }
      if (whatList.contains(UserPref.What.CALLTREE)) {
        settings.getCallTreeSetting().setThreshold(this, userPref.getCallStackThreshold());
      }
      if (whatList.contains(UserPref.What.FORMATS)) {
        settings
            .getViewModeSetting()
            .set(this, ViewModeSetting.ViewMode.fromValue(userPref.getFormatsViewMode()));
        settings
            .getFormatSetting()
            .set(
                this,
                FormatSetting.Style.fromValue(userPref.getFormatsStyle()),
                userPref.getFormatsAppendSoName());
        settings
            .getCompareModeSetting()
            .set(this, CompareModeSetting.CompareMode.fromValue(userPref.getFormatsCompareMode()));
        settings.getTableSettings().setWrapMetricNames(this, userPref.wrapMetricNamesInTables());
      }
      if (whatList.contains(UserPref.What.SEARCHPATH)) {
        settings
            .getSearchPathSetting()
            .set(
                this,
                userPref.getSearchPath().toArray(new String[userPref.getSearchPath().size()]));
      }
      if (whatList.contains(UserPref.What.PATHMAP)) {
        settings.getPathMapSetting().set(this, userPref.getPathmap());
      }
      if (whatList.contains(UserPref.What.FUNCTIONCOLORS)) {
        AnWindow.getInstance()
            .getColorChooser()
            .getColorMap()
            .initRules(userPref.getActiveColorRules(), true);
      }
      if (whatList.contains(UserPref.What.LIBRARYVISIBILITY)) {
        settings
            .getLibraryVisibilitySetting()
            .setFromStrings(this, userPref.getLibraryVisibilitySettings());
        AnWindow.getInstance()
            .getLibraryVisibilityDialog()
            .initStates(
                userPref.getLibraryVisibilityJava(),
                userPref.getLibraryVisibilitySettings(),
                userPref.getLibraryVisibilityIncludePickList(),
                userPref.getLibraryVisibilityIncludeFilter(),
                userPref.getLibraryVisibilityExcludePickList(),
                userPref.getCurrenLibraryVisibilityExcludeFilter());
      }
      if (whatList.contains(UserPref.What.MISC)) {
        existingUserpref.setFindPickList(userPref.getFindPickList());
        AnWindow.getInstance()
            .getToolBarPanel()
            .getFindTextPanel()
            .initializeFindTexts(userPref.getFindPickList(), null); // Find text
      }

      // Save configuration file path
      existingUserpref.setLastExportImportConfPath(importSettingsPanel.getConfigurationPath());
    }
  }
}
