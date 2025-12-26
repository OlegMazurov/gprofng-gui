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


package org.gprofng.mpmt.settings;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;

public class CompareModeSetting extends Setting {
  private CompareMode compareMode = CompareMode.CMP_DISABLE; // Default

  public enum CompareMode {
    CMP_DISABLE(0, AnLocale.getString("Disable")),
    CMP_ENABLE(1, AnLocale.getString("Enable")),
    CMP_RATIO(2, AnLocale.getString("Ratio")),
    CMP_DELTA(4, AnLocale.getString("Delta"));

    private final int value;
    private final String name;

    private CompareMode(int value, String name) {
      this.value = value;
      this.name = name;
    }

    public int value() {
      return value;
    }

    public static CompareMode fromValue(int val) {
      for (CompareMode rt : CompareMode.values()) {
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

  public CompareModeSetting() {}

  public static CompareMode getDefaultCompareMode() {
    return CompareMode.CMP_DISABLE;
  }

  public void init(Object originalSource, CompareMode mode) {
    if (mode != compareMode) {
      this.compareMode = mode;
      fireChangeEvent(originalSource, mode);
    }
  }

  public void set(Object originalSource, CompareMode mode) {
    if (mode != compareMode) {
      setValueAndFireChangeEvent(originalSource, this, mode);
    }
  }

  public CompareMode get() {
    return compareMode;
  }

  public boolean comparingExperiments() {
    return compareMode != CompareMode.CMP_DISABLE;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.COMPARE_MODE;
  }

  @Override
  Object getValue() {
    return compareMode;
  }

  @Override
  void setValue(Object newValue) {
    CompareMode newMode = (CompareMode) newValue;
    this.compareMode = newMode;
    setCompareModeV2IPC(newMode); // IPC
  }

  private static void setCompareModeV2IPC(final CompareMode mode) {
    synchronized (IPC.lock) {
      final IPC ipc = AnWindow.getInstance().IPC();
      ipc.send("setCompareModeV2");
      ipc.send(0);
      ipc.send(mode.value());
      ipc.recvVoid();
    }
  }

  public static CompareMode getCompareModeV2IPC() {
    synchronized (IPC.lock) {
      final IPC ipc = AnWindow.getInstance().IPC();
      ipc.send("getCompareModeV2");
      ipc.send(0);
      int mode = ipc.recvInt();
      CompareMode compareMode = CompareMode.fromValue(mode);
      return compareMode;
    }
  }
  /**
   * Send request to get Compare Mode. Non-blocking IPC call. Caller should call ipcResult.getInt()
   * to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getCompareModeV2IPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getCompareModeV2");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // CompareMode result = ipcResult.getInt() // blocking
    return ipcResult;
  }
}
