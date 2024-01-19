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

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;

public class PathMapSetting extends Setting {
  private String[][] pathMap = null;

  public PathMapSetting() {
    pathMap = new String[2][]; // Default
    pathMap[0] = new String[0];
    pathMap[1] = new String[0];
  }

  public void set(Object originalSource, String[][] newPathMap) {
    boolean changed = false;
    if (pathMap == null) {
      changed = true;
    } else {
      assert pathMap.length == 2 && newPathMap.length == 2;
      String[] oldTo = pathMap[0];
      String[] oldFrom = pathMap[1];
      String[] newTo = newPathMap[0];
      String[] newFrom = newPathMap[1];
      assert oldTo.length == oldFrom.length;
      assert newTo.length == newFrom.length;

      if (oldTo.length != newTo.length) {
        changed = true;
      } else {
        for (int i = 0; i < oldTo.length; i++) {
          if (!oldTo[i].equals(newTo[i])) {
            changed = true;
            break;
          }
        }
        for (int i = 0; i < oldFrom.length; i++) {
          if (!oldFrom[i].equals(newFrom[i])) {
            changed = true;
            break;
          }
        }
      }
    }
    if (changed) {
      setValueAndFireChangeEvent(originalSource, this, newPathMap);
    }
  }

  public void addPathMap(Object originalSource, String from, String to) {
    String[][] newPathMap = new String[pathMap.length][];
    String fromPaths[] = new String[pathMap[0].length + 1];
    String toPaths[] = new String[pathMap[1].length + 1];
    for (int i = 0; i < pathMap[0].length; i++) {
      fromPaths[i] = pathMap[0][i];
      toPaths[i] = pathMap[1][i];
    }
    fromPaths[pathMap[0].length] = from;
    toPaths[pathMap[0].length] = to;

    newPathMap[0] = fromPaths;
    newPathMap[1] = toPaths;

    set(originalSource, newPathMap);
  }

  public void init(Object originalSource, String[][] pathMap) {
    setValue(pathMap);
    fireChangeEvent(originalSource, pathMap);
  }

  public static String[][] getDefaultPathmap() {
    String[][] pathMap = new String[2][]; // Default
    pathMap[0] = new String[0];
    pathMap[1] = new String[0];
    return pathMap;
  }

  public static boolean isDefault(String[][] pathmap) {
    String[][] def = getDefaultPathmap();
    boolean ret = true;
    if (def.length != pathmap.length) {
      ret = false;
    } else {
      String[] deffrom = def[0];
      String[] defto = def[1];
      String[] pathmapfrom = pathmap[0];
      String[] pathmapto = pathmap[1];
      if (deffrom.length != pathmapfrom.length) {
        ret = false;
      } else if (defto.length != pathmapto.length) {
        ret = false;
      } else {
        for (int i = 0; i < deffrom.length; i++) {
          if (!deffrom[i].equals(pathmapfrom[i])) {
            ret = false;
            break;
          }
        }
        for (int i = 0; i < defto.length; i++) {
          if (!defto[i].equals(pathmapto[i])) {
            ret = false;
            break;
          }
        }
      }
    }
    return ret;
  }

  public String[][] get() {
    return pathMap;
  }

  public String[] getPathMapFrom() {
    return pathMap[0];
  }

  public String[] getPathMapTo() {
    return pathMap[1];
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.PATH_MAP;
  }

  @Override
  Object getValue() {
    return pathMap;
  }

  @Override
  void setValue(Object newValue) {
    Object[] newPathMap = (Object[]) newValue;
    pathMap = (String[][]) newPathMap;
    setPathMapIPC(pathMap); // IPC
  }

  public static String[][] getPathMapsIPC() {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("getPathmaps");
      Object[] ret = (Object[]) anWindow.IPC().recvObject();
      String[][] retS = new String[2][];
      retS[0] = (String[]) ret[0];
      retS[1] = (String[]) ret[1];
      return retS;
    }
  }

  private static String addPathMapIPC(String from, final String to) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("addPathmap");
      anWindow.IPC().send(from);
      anWindow.IPC().send(to);
      return anWindow.IPC().recvString();
    }
  }

  private static void setPathMapIPC(String[][] pathMap) {
    AnWindow anWindow = AnWindow.getInstance();
    synchronized (IPC.lock) {
      anWindow.IPC().send("setPathmaps");
      anWindow.IPC().send(pathMap[0]);
      anWindow.IPC().send(pathMap[1]);
      anWindow.IPC().recvString(); // synchronize
    }
  }
}
