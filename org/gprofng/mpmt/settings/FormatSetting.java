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

public class FormatSetting extends Setting {
  private static final Style defaultStyle = Style.LONG;
  private static final boolean defaultAppendSoName = false;

  public enum Style {
    LONG(1, AnLocale.getString("Long")),
    SHORT(2, AnLocale.getString("Short")),
    MANGLED(3, AnLocale.getString("Mangled"));
    private final int value;
    private final String name;

    private Style(int value, String name) {
      this.value = value;
      this.name = name;
    }

    public int value() {
      return value;
    }

    public static Style fromValue(int val) {
      for (Style rt : Style.values()) {
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

  public class Format {
    public Style style = defaultStyle;
    public boolean appendSOName = defaultAppendSoName;
  }

  private Format format = new Format();

  public static Style getDefaultStyle() {
    return defaultStyle;
  }

  public static boolean getDefaultAppendSoName() {
    return defaultAppendSoName;
  }

  public FormatSetting() {}

  public FormatSetting(Style style, boolean appendSOName) {
    format.style = style;
    format.appendSOName = appendSOName;
  }

  public void init(Object originalSource, Style style, boolean appendSoName) {
    format.style = style;
    format.appendSOName = appendSoName;
    setNameFormatIPC(format.style.value(), format.appendSOName); // IPC
    fireChangeEvent(originalSource, this);
  }

  public void set(Object originalSource, Style style, boolean appendSoName) {
    if (style != format.style || appendSoName != format.appendSOName) {
      FormatSetting newFormatSetting = new FormatSetting(style, appendSoName);
      setValueAndFireChangeEvent(originalSource, this, newFormatSetting);
    }
  }

  public Style getStyle() {
    return format.style;
  }

  public boolean getAppendSoName() {
    return format.appendSOName;
  }

  public void setAppendSoNames(Object originalSource, boolean appendSoName) {
    if (appendSoName != format.appendSOName) {
      setValueAndFireChangeEvent(originalSource, this, format);
    }
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.FORMAT;
  }

  @Override
  Object getValue() {
    return format;
  }

  @Override
  void setValue(Object newValue) {
    FormatSetting newFormatSetting = (FormatSetting) newValue;
    format.style = newFormatSetting.format.style;
    format.appendSOName = newFormatSetting.format.appendSOName;
    setNameFormatIPC(format.style.value(), format.appendSOName); // IPC
  }

  public static int getNameFormatIPC() {
    synchronized (IPC.lock) {
      AnWindow.getInstance().IPC().send("getNameFormat");
      AnWindow.getInstance().IPC().send(0);
      return AnWindow.getInstance().IPC().recvInt();
    }
  }
  /**
   * Send request to get Name Format. Non-blocking IPC call. Caller should call ipcResult.getInt()
   * to get the result
   *
   * @return IPCResult
   */
  public static IPCResult SendRequest_getNameFormatIPC() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getNameFormat");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // int result = ipcResult.getInt() // blocking
    return ipcResult;
  }

  public static boolean getSoNameIPC() {
    synchronized (IPC.lock) {
      AnWindow.getInstance().IPC().send("getSoName");
      AnWindow.getInstance().IPC().send(0);
      return AnWindow.getInstance().IPC().recvBoolean();
    }
  }
  /**
   * Send request to get SoName. Non-blocking IPC call. Caller should call ipcResult.getBoolean() to
   * get the result
   *
   * @return IPCResult
   */
  public static IPCResult SendRequest_getSoNameIPC() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getSoName");
    ipcHandle.append(0);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // boolean result = ipcResult.getBoolean() // blocking
    return ipcResult;
  }

  private static void setNameFormatIPC(final int set, final boolean so_name) {
    synchronized (IPC.lock) {
      AnWindow.getInstance().IPC().send("setNameFormat");
      AnWindow.getInstance().IPC().send(0);
      AnWindow.getInstance().IPC().send(set);
      AnWindow.getInstance().IPC().send(so_name);
      AnWindow.getInstance().IPC().recvString(); // synchronize
    }
  }
}
