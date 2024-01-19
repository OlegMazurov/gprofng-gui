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
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;

public class ViewModeSetting extends Setting {
  private static final ViewMode defaultViewMode = ViewMode.USER;

  public enum ViewMode {
    MACHINE(0, AnLocale.getString("Machine")),
    USER(1, AnLocale.getString("User")),
    EXPERT(2, AnLocale.getString("Expert"));
    private final int value;
    private final String name;

    private ViewMode(int value, String name) {
      this.value = value;
      this.name = name;
    }

    public int value() {
      return value;
    }

    public static ViewMode fromValue(int val) {
      for (ViewMode rt : ViewMode.values()) {
        if (rt.value() == val) {
          return rt;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return name;
    }
  };

  private ViewMode viewMode = ViewMode.USER; // Default

  public ViewModeSetting() {}

  public void init(Object originalSource, ViewMode newViewMode) {
    viewMode = newViewMode;
    fireChangeEvent(originalSource, newViewMode);
  }

  public void set(Object originalSource, ViewMode newViewMode) {
    if (newViewMode != viewMode) {
      setValueAndFireChangeEvent(originalSource, this, newViewMode);
    }
  }

  // View Mode
  public ViewMode get() {
    return viewMode;
  }

  public String getStackName() {
    if (get() == ViewMode.MACHINE) {
      return "MSTACK";
    }
    if (get() == ViewMode.EXPERT) {
      return "XSTACK";
    }
    return "USTACK";
  }

  public static ViewMode getDefaultViewMode() {
    return defaultViewMode;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.VIEW_MODE;
  }

  @Override
  Object getValue() {
    return viewMode;
  }

  @Override
  void setValue(Object newValue) {
    ViewMode newViewMode = (ViewMode) newValue;
    this.viewMode = newViewMode;
    setViewModeIPC(newViewMode.value()); // IPC
  }

  public static int getViewModeIPC() {
    synchronized (IPC.lock) {
      int viewMode;
      AnWindow.getInstance().IPC().send("getViewMode");
      AnWindow.getInstance().IPC().send(0);
      viewMode = AnWindow.getInstance().IPC().recvInt();
      return viewMode;
    }
  }
  /**
   * Send request to get View Mode. Non-blocking IPC call. Caller should call ipcResult.getInt() to
   * get the result
   *
   * @return IPCResult
   */
  public static IPCResult getViewModeIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getViewMode");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int result = ipcResult.getInt() // blocking
    return ipcResult;
  }

  /**
   * Set View Mode
   *
   * @param set 0 = Machine, 1 = User, 2 = Expert
   */
  private static void setViewModeIPC(final int set) {
    synchronized (IPC.lock) {
      //            System.out.println("setViewModeIPC: " + set);
      AnWindow.getInstance().IPC().send("setViewMode");
      AnWindow.getInstance().IPC().send(0);
      AnWindow.getInstance().IPC().send(set);
      AnWindow.getInstance().IPC().recvString();
    }
  }
}
