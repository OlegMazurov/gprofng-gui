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
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchPathSetting extends Setting {
  private String[] searchPath = new String[0]; // Default

  public SearchPathSetting() {}

  public void set(Object originalSource, String[] newSearch_path) {
    boolean changed = false;
    if (searchPath.length != newSearch_path.length) {
      changed = true;
    } else {
      for (int i = 0; i < searchPath.length; i++) {
        if (!searchPath[i].equals(newSearch_path[i])) {
          changed = true;
          break;
        }
      }
    }
    if (changed) {
      setValueAndFireChangeEvent(originalSource, this, newSearch_path);
    }
  }

  public void add(Object originalSource, String path) {
    String[] newSearchPath = new String[searchPath.length + 1];
    for (int i = 0; i < searchPath.length; i++) {
      newSearchPath[i] = searchPath[i];
    }
    newSearchPath[searchPath.length] = path;
    set(originalSource, newSearchPath);
  }

  public void init(Object originalSource, List<String> searchPath) {
    String[] arr = searchPath.toArray(new String[searchPath.size()]);
    setValue(arr);
    fireChangeEvent(originalSource, searchPath);
  }

  public static List<String> getDefaultSearchPath() {
    List<String> list = new ArrayList<>();
    list.add("$expts");
    list.add(".");
    return list;
  }

  public static boolean isDefault(List<String> list) {
    List<String> def = getDefaultSearchPath();
    boolean ret = true;
    if (def.size() != list.size()) {
      ret = false;
    } else {
      for (int i = 0; i < def.size(); i++) {
        if (!def.get(i).equals(list.get(i))) {
          ret = false;
          break;
        }
      }
    }
    return ret;
  }

  public List<String> get() {
    List<String> list = new ArrayList<>();
    for (String s : searchPath) {
      list.add(s);
    }
    return list;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.SEARCH_PATH;
  }

  @Override
  Object getValue() {
    return searchPath;
  }

  @Override
  void setValue(Object newValue) {
    String[] newSearchPath = (String[]) newValue;
    searchPath = newSearchPath;
    setSearchPathIPC(searchPath); // IPC
  }

  public static String[] getSearchPathIPC() {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getSearchPath");
      return (String[]) anWindow.IPC().recvObject();
    }
  }

  private static void setSearchPathIPC(String[] path) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("setSearchPath");
      anWindow.IPC().send(path);
      anWindow.IPC().recvString(); // synchronize
    }
  }
}
