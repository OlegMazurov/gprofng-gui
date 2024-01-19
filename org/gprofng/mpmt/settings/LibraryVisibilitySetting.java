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
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.AnSettingChangeEvent.Type;
import java.util.ArrayList;
import java.util.List;

public class LibraryVisibilitySetting extends Setting {
  private List<Entry> settings; // Complete list in original order

  public LibraryVisibilitySetting() {}

  public void set(Object originalSource, List<Entry> newSettings) {
    boolean changed = false;
    for (int i = 0; i < settings.size(); i++) {
      Entry oldEntry = settings.get(i);
      Entry newEntry = newSettings.get(i);
      if (oldEntry.getState() != newEntry.getState()) {
        changed = true;
        break;
      }
    }
    if (changed) {
      setValueAndFireChangeEvent(originalSource, this, newSettings);
    }
  }

  public void setFromStrings(Object originalSource, List<String> initStates) {
    List<Entry> newSettings = new ArrayList<Entry>();
    for (Entry entry : settings) {
      newSettings.add(entry.copy());
    }

    if (initStates != null && !initStates.isEmpty()) {
      for (String s : initStates) {
        int stateValue = new Integer(s.substring(0, 1));
        String path = s.substring(2);
        for (Entry entry : newSettings) {
          if (entry.getPath().equals(path)) {
            entry.setState(stateValue);
          }
        }
      }
    } else {
      for (Entry entry : newSettings) {
        entry.setState(0);
      }
    }

    set(originalSource, newSettings);
  }

  public void init(Object originalSource, Object[] custom_object, List<String> initStates) {
    if (custom_object == null) {
      return;
    }
    String[] names = (String[]) custom_object[0];
    int[] states = (int[]) custom_object[1];
    int[] indices = (int[]) custom_object[2];
    String[] paths = (String[]) custom_object[3];
    int[] java = (int[]) custom_object[4];

    if (initStates != null && !initStates.isEmpty()) {
      for (String s : initStates) {
        int stateValue = new Integer(s.substring(0, 1));
        String path = s.substring(2);
        for (int i = 0; i < paths.length; i++) {
          if (paths[i].equals(path)) {
            states[i] = stateValue;
            break;
          }
        }
      }
      setLoadObjectStateIPC(0, states); // IPC
    }

    settings = new ArrayList<Entry>();
    for (int i = 0; i < names.length; i++) {
      Entry entry = new Entry(names[i], paths[i], states[i], indices[i], java[i] == 1);
      settings.add(entry);
    }
  }

  public List<Entry> get() {
    return settings;
  }

  @Override
  Type getType() {
    return AnSettingChangeEvent.Type.LIBRARY_VISIBILITY;
  }

  @Override
  Object getValue() {
    return settings;
  }

  @Override
  void setValue(Object newValue) {
    List<Entry> newSettings = (List<Entry>) newValue;
    settings = newSettings;

    int[] newStates = new int[settings.size()];
    int i = 0;
    for (Entry entry : settings) {
      newStates[i++] = entry.getState();
    }
    setLoadObjectStateIPC(0, newStates); // IPC
  }

  // For persistance
  public List<String> getStates() {
    List<String> list = new ArrayList<String>();
    for (Entry entry : settings) {
      if (entry.getState() != 0) {
        list.add("" + entry.getState() + ":" + entry.getPath());
      }
    }
    return list;
  }

  public static class Entry {
    private String name;
    private String path;
    private int state;
    private int index;
    private boolean java;

    public Entry(String name, String path, int state, int index, boolean java) {
      this.name = name;
      this.path = path;
      this.state = state;
      this.index = index;
      this.java = java;
    }

    /**
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the path
     */
    public String getPath() {
      return path;
    }

    /**
     * @return the state
     */
    public int getState() {
      return state;
    }

    /**
     * @return the index
     */
    public int getIndex() {
      return index;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
      this.state = state;
    }

    public Entry copy() {
      return new Entry(name, path, state, index, isJava());
    }

    /**
     * @return the java
     */
    public boolean isJava() {
      return java;
    }
  }

  // Native methods from liber_dbe.so
  public static Object[] getLoadObjectListIPC(int w_id) {
    AnWindow m_window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      m_window.IPC().send("getLoadObjectList");
      m_window.IPC().send(w_id);
      return (Object[]) m_window.IPC().recvObject();
    }
  }
  /**
   * Send request to get Load Object List. Non-blocking IPC call. Caller should call
   * ipcResult.getObjects() to get the result
   *
   * @return IPCResult
   */
  public static IPCResult getLoadObjectListIPCRequest(int w_id) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getLoadObjectList");
    ipcHandle.append(w_id);
    IPCResult ipcResult = ipcHandle.sendRequest();
    // Object[] result = ipcResult.getObjects() // blocking
    return ipcResult;
  }

  public static int[] getLoadObjectStateIPC(int w_id) {
    AnWindow m_window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      m_window.IPC().send("getLoadObjectState");
      m_window.IPC().send(w_id);
      return (int[]) m_window.IPC().recvObject();
    }
  }

  public static void setLoadObjectStateIPC(final int w_id, final int[] state) {
    AnWindow m_window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      m_window.IPC().send("setLoadObjectState");
      m_window.IPC().send(w_id);
      m_window.IPC().send(state);
      m_window.IPC().recvString(); // synchronize
    }
  }

  public static void setLoadObjectDefaultsIPC(final int w_id) {
    AnWindow m_window = AnWindow.getInstance();
    synchronized (IPC.lock) {
      m_window.IPC().send("setLoadObjectDefaults");
      m_window.IPC().send(w_id);
      m_window.IPC().recvString(); // synchronize
    }
  }
}
