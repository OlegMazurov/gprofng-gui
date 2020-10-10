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

package org.gprofng.mpmt;

import org.gprofng.mpmt.event.AnChangeEvent;
import org.gprofng.mpmt.event.AnChangeListener;
import org.gprofng.mpmt.event.AnEventManager;
import org.gprofng.mpmt.util.gui.AnUtility;

public final class SelectedObject implements AnChangeListener {

  private long selObjectSelectionTime = 0;
  private boolean selObjWasSet = false;
  private long selIndex;
  private int selType;
  private int selSubType;

  public SelectedObject() {
    AnEventManager.getInstance().addListener(this);
  }

  /**
   * AnChangeEvent handler. Events are always dispatched on AWT thread. Requirements to an event
   * handler like this one: o When it done handling the event and returns, all it's states need to
   * be *consistent* and *well-defined*. It cannot dispatch that or part of that work to a different
   * (worker) thread and not wait for it to finish. o It cannot spend significant time handling the
   * event. Significant work like doCompute needs to be dispatched to a worker thread.
   *
   * @param e the event
   */
  @Override
  public void stateChanged(AnChangeEvent e) {
    //        System.out.println("RacesView stateChanged:  " + e.getType());
    switch (e.getType()) {
      case EXPERIMENTS_LOADING_NEW:
        resetSelectedObject();
        break;
      case EXPERIMENTS_LOADING_ADDED_OR_REMOVED:
      case EXPERIMENTS_LOADED_FAILED:
      case EXPERIMENTS_LOADED:
        // Nothing
        break;
      case FILTER_CHANGING:
        resetSelectedObject();
        break;
      case FILTER_CHANGED:
      case REMOTE_CONNECTION_CHANGING:
      case REMOTE_CONNECTION_CANCELLED_OR_FAILED:
      case REMOTE_CONNECTION_CHANGED:
      case MOST_RECENT_EXPERIMENT_LIST_CHANGED:
      case SETTING_CHANGED:
      case SOURCE_FINDING_CHANGING:
      case SOURCE_FINDING_CHANGED:
      case SETTING_CHANGING:
      case SELECTED_OBJECT_CHANGED:
      case SELECTION_CHANGING:
      case SELECTION_CHANGED:
      case SELECTION_UPDATE:
        // Nothing
        break;
      case DEBUG:
        debug();
        break;
      default:
        System.err.println("AnChanged event " + e.getType() + " not handled: " + this);
        break;
    }
  }

  private void debug() {
    System.out.println(this.getClass().getSimpleName());
  }

  public void setSelObjWithEvent(long index, int type, int subtype) {
    setSelObj(index, type, subtype);
    AnEventManager.getInstance()
        .fireAnChangeEvent(new AnChangeEvent(this, AnChangeEvent.Type.SELECTED_OBJECT_CHANGED));
  }

  public void setSelObj(long index, int type, int subtype) {
    //        System.out.println("setSelObj: " + index + " " + type + " " + subtype);
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      getWindow().IPC().send("setSelObj");
      getWindow().IPC().send(0);
      getWindow().IPC().send(index);
      getWindow().IPC().send(type);
      getWindow().IPC().send(subtype);
      getWindow().IPC().recvString(); // synchronize

      // store the selection
      selObjectSelectionTime = System.currentTimeMillis();
      selObjWasSet = true;
      selIndex = index;
      selType = type;
      selSubType = subtype;
      AnUtility.checkIPCOnWrongThread(true);
    }
  }

  public boolean wasASelDone() {
    return selObjWasSet;
  }

  private void resetSelectedObject() {
    selObjWasSet = false;
  }

  public long getSelIndex() {
    return selIndex;
  }

  public int getSelType() {
    return selType;
  }

  public int getSelSubType() {
    return selSubType;
  }

  public long getSelObj(int type, int subtype) {
    synchronized (IPC.lock) {
      getWindow().IPC().send("getSelObj");
      getWindow().IPC().send(0);
      getWindow().IPC().send(type);
      getWindow().IPC().send(subtype);
      long l = getWindow().IPC().recvLong();
      //            System.out.println("getSelObj: " + l);
      return l;
    }
  }

  public int getSelIndex(long sel_obj, int type, int subtype) {
    synchronized (IPC.lock) {
      getWindow().IPC().send("getSelIndex");
      getWindow().IPC().send(0);
      getWindow().IPC().send(sel_obj);
      getWindow().IPC().send(type);
      getWindow().IPC().send(subtype);
      return getWindow().IPC().recvInt();
    }
  }

  /**
   * @return the selObjectSelectionTime
   */
  public long getSelObjectSelectionTime() {
    return selObjectSelectionTime;
  }

  /**
   * Get Selected Object. New API "V2" (version 2). Based on unique id.
   *
   * @param ConvertTo String "FUNCTION"
   * @return ID
   */
  public long getSelObjV2(String ConvertTo) {
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      getWindow().IPC().send("getSelObjV2");
      getWindow().IPC().send(0);
      getWindow().IPC().send(ConvertTo);
      long res = getWindow().IPC().recvLong();
      AnUtility.checkIPCOnWrongThread(true);
      return res;
    }
  }

  public void setSelObjV2WithEvent(long id) {
    setSelObjV2(id);
    AnEventManager.getInstance()
        .fireAnChangeEvent(new AnChangeEvent(this, AnChangeEvent.Type.SELECTED_OBJECT_CHANGED));
  }

  public void setSelObjV2(long id) {
    //        System.out.println("setSelObjV2: " + id);
    AnUtility.checkIPCOnWrongThread(false);
    synchronized (IPC.lock) {
      getWindow().IPC().send("setSelObjV2");
      getWindow().IPC().send(0);
      getWindow().IPC().send(id);
      getWindow().IPC().recvString(); // synchronize

      // store the selection
      selObjectSelectionTime = System.currentTimeMillis();
      selObjWasSet = true;
    }
    AnUtility.checkIPCOnWrongThread(true);
  }

  public long[] getSelObjIO(long id, int type) {
    synchronized (IPC.lock) {
      final IPC ipc = getWindow().IPC();
      ipc.send("getSelObjIO");
      ipc.send(0);
      ipc.send(id);
      ipc.send(type);
      return (long[]) ipc.recvObject();
    }
  }

  public long[] getSelObjsIO(final long[] ids, int type) {
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      final IPC ipc = getWindow().IPC();
      ipc.send("getSelObjsIO");
      ipc.send(0);
      ipc.send(ids);
      ipc.send(type);
      long[] ll = (long[]) ipc.recvObject();
      AnUtility.checkIPCOnWrongThread(true);
      return ll;
    }
  }

  public long getSelObjHeapTimestamp(long id) {
    synchronized (IPC.lock) {
      final IPC ipc = getWindow().IPC();
      ipc.send("getSelObjHeapTimestamp");
      ipc.send(0);
      ipc.send(id);
      return (long) ipc.recvLong();
    }
  }

  public int getSelObjHeapUserExpId(long id) {
    synchronized (IPC.lock) {
      final IPC ipc = getWindow().IPC();
      ipc.send("getSelObjHeapUserExpId");
      ipc.send(0);
      ipc.send(id);
      return (int) ipc.recvInt();
    }
  }

  public long getObject(final long sel_func, final long sel_pc) {
    synchronized (IPC.lock) {
      AnUtility.checkIPCOnWrongThread(false);
      getWindow().IPC().send("getObject");
      getWindow().IPC().send(0);
      getWindow().IPC().send(sel_func);
      getWindow().IPC().send(sel_pc);
      long l = getWindow().IPC().recvLong();
      AnUtility.checkIPCOnWrongThread(true);
      return l;
    }
  }

  private AnWindow getWindow() {
    return AnWindow.getInstance();
  }
}
