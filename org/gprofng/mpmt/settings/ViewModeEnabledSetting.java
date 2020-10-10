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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;

public class ViewModeEnabledSetting extends Setting {
  private boolean enabled = false; // Default

  public ViewModeEnabledSetting() {}

  public void init(Object originalSource, boolean newValue) {
    if (enabled != newValue) {
      this.enabled = newValue;
      fireChangeEvent(originalSource, newValue);
    }
  }

  public void init(boolean newValue) {
    enabled = newValue;
  }

  public boolean isViewModeEnabled() {
    return enabled;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.VIEW_MODE_ENABLED;
  }

  @Override
  Object getValue() {
    return enabled;
  }

  @Override
  void setValue(Object newValue) {
    enabled = (Boolean) newValue;
  }

  public static boolean getViewModeEnabledIPC() {
    boolean viewModeEnabled;
    synchronized (IPC.lock) {
      AnWindow.getInstance().IPC().send("getViewModeEnable");
      viewModeEnabled = AnWindow.getInstance().IPC().recvBoolean();
    }
    return viewModeEnabled;
  }
  /**
   * Send request to get View Mode Enabled. Non-blocking IPC call. Caller should call
   * ipcResult.getBoolean() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getViewModeEnabledIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getViewModeEnable");
    IPCResult ipcResult = ipcHandle.sendRequest();
    // boolean result = ipcResult.getBoolean() // blocking
    return ipcResult;
  }
}
