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


package org.gprofng.mpmt.settings;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnVariable;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;
import org.gprofng.mpmt.util.gui.AnUtility;

public abstract class Setting {
  abstract void setValue(Object newValue);

  abstract Object getValue();

  abstract Type getType();

  public void setValueAndFireChangeEvent(
      final Object originalSource, final Setting setting, final Object newValue) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (AnVariable.mainFlowLock) {
              //                    System.out.println("Setting.setValueAndFireChangeEvent: " +
              // getType() + " " + originalSource + " " + Setting.this);
              IPCContext.newCurrentContext(
                  AnLocale.getString("Changing settings..."),
                  IPCContext.Scope.SESSION,
                  false,
                  AnWindow.getInstance());
              Object progressBarHandle =
                  AnWindow.getInstance()
                      .getSystemProgressPanel()
                      .progressBarStart(AnLocale.getString("Changing Settings"));
              final Object oldValue = setting.getValue();
              final AnSettingChangeEvent anPropertyChangeEvent =
                  new AnSettingChangeEvent(
                      originalSource, Setting.this, getType(), oldValue, newValue);
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(
                          anPropertyChangeEvent, AnChangeEvent.Type.SETTING_CHANGING));
              setValue(newValue); // possible IPC
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(anPropertyChangeEvent, AnChangeEvent.Type.SETTING_CHANGED));
              AnWindow.getInstance().getSystemProgressPanel().progressBarStop(progressBarHandle);
              IPCContext.newCurrentContext(
                  null, IPCContext.Scope.SESSION, false, AnWindow.getInstance());
            }
          }
        },
        "Setting Thread");
  }

  public void fireChangeEvent(final Object originalSource, final Object newValue) {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            synchronized (AnVariable.mainFlowLock) {
              //                    System.out.println("Setting.fireChangeEvent: " + getType() + " "
              // + originalSource + " " + Setting.this);
              IPCContext.newCurrentContext(
                  AnLocale.getString("Changing settings..."),
                  IPCContext.Scope.SESSION,
                  false,
                  AnWindow.getInstance());
              Object progressBarHandle =
                  AnWindow.getInstance()
                      .getSystemProgressPanel()
                      .progressBarStart(AnLocale.getString("Changing Settings"));
              final AnSettingChangeEvent anPropertyChangeEvent =
                  new AnSettingChangeEvent(originalSource, Setting.this, getType(), null, newValue);
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(
                          anPropertyChangeEvent, AnChangeEvent.Type.SETTING_CHANGING));
              AnEventManager.getInstance()
                  .fireAnChangeEvent(
                      new AnChangeEvent(anPropertyChangeEvent, AnChangeEvent.Type.SETTING_CHANGED));
              AnWindow.getInstance().getSystemProgressPanel().progressBarStop(progressBarHandle);
              IPCContext.newCurrentContext(
                  null, IPCContext.Scope.SESSION, false, AnWindow.getInstance());
            }
          }
        },
        "Setting Thread");
  }
}
