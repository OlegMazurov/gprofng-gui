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

package org.gprofng.mpmt.event;

import org.gprofng.mpmt.settings.AnSettingChangeEvent;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author tpreisle
 */
public class AnEventManager {

  private boolean logEvents = false;
  private static AnEventManager instance = null;
  private List<AnChangeListener> listenerList = null;

  private AnEventManager() {
    listenerList = new ArrayList<>();
  }

  /**
   * @return
   */
  public static AnEventManager getInstance() {
    if (instance == null) {
      instance = new AnEventManager();
    }
    return instance;
  }

  /**
   * @param listener
   */
  public synchronized void addListener(AnChangeListener listener) {
    listenerList.add(listener);
  }

  /**
   * @param listener
   */
  public synchronized void removeListener(AnChangeListener listener) {
    listenerList.remove(listener);
  }

  /**
   * Fire an AnChangeEvent. Events are always dispatched on AWT thread. Requirements to the event
   * handler: o When it done handling the event and returns, all the views states need to be
   * *consistent* and *well-defined*. It cannot dispatch that or part of that work to a different
   * (worker) thread and not wait for it to finish. o It cannot spend significant time handling the
   * event. Significant work like doCompute needs to be dispatched to a worker thread.
   *
   * @param changeEvent the event
   */
  public void fireAnChangeEvent(final AnChangeEvent changeEvent) {
    long start = 0;
    if (logEvents) {
      start = new Date().getTime();
      String type = changeEvent.getType().toString();
      if (changeEvent.getType() == AnChangeEvent.Type.SETTING_CHANGED
          || changeEvent.getType() == AnChangeEvent.Type.SETTING_CHANGING) {
        AnSettingChangeEvent anSettingChangeEvent = (AnSettingChangeEvent) changeEvent.getSource();
        type += " " + anSettingChangeEvent.getType();
      }
      System.out.println("AnEventManager.fireChangeEventInternal begin: " + type);
    }
    AnUtility.dispatchOnSwingThread(
        new Runnable() { // REFACTOR: should this be non-blocking?
          //                AnUtility.invokeLaterOnSwingThread(new Runnable() {// REFACTOR: should
          // this be non-blocking?
          @Override
          public void run() {
            fireAnChangeEventInternal(changeEvent);
          }
        });
    if (logEvents) {
      String type = changeEvent.getType().toString();
      System.out.println(
          "AnEventManager.fireChangeEventInternal end:   "
              + type
              + " ms="
              + (new Date().getTime() - start));
    }
  }

  private synchronized void fireAnChangeEventInternal(final AnChangeEvent changeEvent) {
    for (AnChangeListener listener : listenerList) {
      listener.stateChanged(changeEvent);
    }
  }
}
