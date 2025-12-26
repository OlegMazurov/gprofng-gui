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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;

public class SourceDisassemblySetting extends Setting {
  private static final int settingsSource = 0x7ff; // 2147483647;
  private static final int settingsDisassembly = 0x7ff; // 2147483647;
  private static final int thresholdSource = 75;
  private static final int thresholdDisassembly = 75;
  private static final int sourceCode = 1;
  private static final int metricsForSourceLines = 0;
  private static final int hexadecimalInstructions = 0;
  private static final int onlyShowDataOfCurrentFunction = 0;
  private static final int showCompilerCommandLlineFlags = 0;
  private static final int showFunctionBeginningLine = 1;
  private static final int[] defaultSettings =
      new int[] {
        settingsSource,
        settingsDisassembly,
        thresholdSource,
        thresholdDisassembly,
        sourceCode,
        metricsForSourceLines,
        hexadecimalInstructions,
        showCompilerCommandLlineFlags,
        onlyShowDataOfCurrentFunction,
        showFunctionBeginningLine
      };

  private int[] settings = defaultSettings;

  public SourceDisassemblySetting() {}

  public static int[] getDefaultSourceDisassemblySetting() {
    return defaultSettings;
  }

  public void init(Object originalSource, int[] newSettings) {
    newSettings[0] = 0x7ff & newSettings[0];
    newSettings[1] = 0x7ff & newSettings[1];
    boolean changed = changed(settings, newSettings);
    if (changed) {
      this.settings = newSettings;
      fireChangeEvent(originalSource, newSettings);
    }
  }

  public void set(Object originalSource, int[] newSettings) {
    newSettings[0] = 0x7ff & newSettings[0];
    newSettings[1] = 0x7ff & newSettings[1];
    boolean changed = changed(settings, newSettings);
    if (changed) {
      setValueAndFireChangeEvent(originalSource, this, newSettings);
    }
  }

  public static boolean changed(int[] oldSettings, int[] newSettings) {
    boolean changed = false;
    // check if any changes
    if (oldSettings.length != newSettings.length) {
      changed = true;
    } else {
      for (int i = 0; i < oldSettings.length; i++) {
        if (oldSettings[i] != newSettings[i]) {
          changed = true;
          break;
        }
      }
    }
    return changed;
  }

  public int[] get() {
    return settings;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.SRC_DIS;
  }

  @Override
  Object getValue() {
    return settings;
  }

  @Override
  void setValue(Object newValue) {
    int[] newSetting = (int[]) newValue;
    this.settings = newSetting;
    setAnoValueIPC(newSetting); // IPC
  }

  public static int[] getAnoValueIPC() {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("getAnoValue");
      window.IPC().send(0);
      return (int[]) window.IPC().recvObject();
    }
  }
  /**
   * Send request to get AnoValue. Non-blocking IPC call. Caller should call ipcResult.getInts() to
   * get the result
   *
   * @return IPCResult
   */
  public static IPCResult getAnoValueIPCRequest() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getAnoValue");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int[] result = ipcResult.getInts() // blocking
    return ipcResult;
  }

  private static void setAnoValueIPC(final int[] set) {
    synchronized (IPC.lock) {
      AnWindow window = AnWindow.getInstance();
      window.IPC().send("setAnoValue");
      window.IPC().send(0);
      window.IPC().send(set);
      window.IPC().recvString(); // synchronize
    }
  }
}
