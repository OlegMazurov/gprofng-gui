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


package org.gprofng.mpmt.timeline.events;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.statecolors.StackViewState;
import java.util.ArrayList;
import java.util.List;

// contents moved here from timeline.Experiment.java
// Ideally, IPC related to TL details should be updated to use properties...

public class DetailsIPC {
  public static final long INVALID_STACK_ID = -1;

  public static EventDetail ipcGetTLDetails(
      AnWindow m_window,
      int exp_id,
      String exp_name,
      int data_id,
      Settings.TLData_type tl_type,
      int entity_prop_id,
      long event_id,
      int cpu_freq,
      long time_adjust) {

    final EventDetail[] events;
    if (tl_type.equals(Settings.TLData_type.TL_SAMPLE)) {
      // YXXX samples has separate processing. Fix when old TL removed
      events = ipcGetSampleObjs(m_window, exp_id, event_id, event_id, exp_name, time_adjust);
    } else if (tl_type.equals(Settings.TLData_type.TL_GCEVENT)) { // CXXX Bug 20801848
      events = ipcGetGCEventObjs(m_window, exp_id, event_id, event_id, exp_name, time_adjust);
    } else {
      final Object[] detailVals;
      detailVals = ipcGetTLDetails(m_window, exp_id, data_id, entity_prop_id, event_id);
      if (detailVals == null) {
        return null;
      }

      final long[] values = (long[]) detailVals[0];
      final long[] funcs = (long[]) detailVals[1];
      final String[] funcNames = (String[]) detailVals[2];
      final long[] pcs = (long[]) detailVals[3];
      final String[] pcNames = (String[]) detailVals[4];

      List<StackState> stackStates = new ArrayList<StackState>();
      for (int i = 0; i < funcs.length; i++) {
        StackViewState svstate = new StackViewState(funcNames[i], pcNames[i], funcs[i], pcs[i]);
        stackStates.add(svstate);
      }

      // To avoid changing legacy code in ipcFinish_getTLDetailsEvents(), set up arrays of details
      // here:
      final long[][] list = new long[1][];
      list[0] = values;
      final List<List<StackState>> stackStatesList = new ArrayList<>();
      stackStatesList.add(stackStates);
      events =
          ipcFinish_getTLDetailsEvents(
              m_window, list, stackStatesList, tl_type, cpu_freq, time_adjust);
    }

    if (events == null || events.length == 0) {
      return null;
    }
    return events[0];
  }

  // TLData_type is defined in Presentation.java. Make sure
  // to update TLData_type if there is a new TL_?? type. Also,
  // The new TL_?? type has to match with the type that is
  // returned by getTLDetailValues(). This method is defined
  // in Dbe.cc.
  private static EventDetail[] ipcFinish_getTLDetailsEvents(
      AnWindow m_window,
      final long[][] list,
      List<List<StackState>> stackStatesList,
      final Settings.TLData_type tl_type,
      final int cpu_freq,
      final long time_adjust) {
    long[] value;
    final int size = list.length;
    final EventDetail events[] = new EventDetail[size];

    switch (tl_type) {
      case TL_CLOCK:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new ProfileEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  (int) value[6],
                  value[7],
                  (int) value[8],
                  value[9],
                  time_adjust);
        }
        break;
      case TL_SYNC:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new SyncEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  value[6],
                  value[7],
                  time_adjust);
        }
        break;
      case TL_HWC:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new HWCEvent(
                  m_window,
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  value[6],
                  value[7],
                  value[8],
                  value[9],
                  value[10],
                  cpu_freq,
                  time_adjust);
        }
        break;
      case TL_HEAP:
      case TL_HEAPSZ:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new HeapEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  (int) value[6],
                  value[7],
                  value[8],
                  value[9],
                  value[10],
                  value[11],
                  value[12],
                  value[13],
                  time_adjust);
        }
        break;
      case TL_IOTRACE:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new IOEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  (int) value[6],
                  (int) value[7],
                  value[8],
                  value[9],
                  time_adjust);
        }
        break;
      case TL_RACES:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new RaceEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  (int) value[6],
                  (int) value[7],
                  value[8],
                  time_adjust);
        }
        break;
      case TL_DLCK:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new DeadlockEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  (int) value[6],
                  (int) value[7],
                  (int) value[8],
                  value[9],
                  time_adjust);
        }
        break;
      case TL_MPI:
        for (int i = 0; i < size; i++) {
          value = list[i];
          events[i] =
              new MPIEvent(
                  (int) value[0],
                  (int) value[1],
                  (int) value[2],
                  value[3],
                  value[4],
                  value[5],
                  stackStatesList.get(i),
                  value[6],
                  (int) value[7],
                  (int) value[8],
                  value[9],
                  time_adjust);
        }
        break;
      default:
        break; // weird
    }

    return events;
  }

  private static Sample[] ipcGetSampleObjs(
      AnWindow m_window, int exp_id, long lo_idx, long hi_idx, String exp_name, long time_adjust) {
    Sample[] event;

    final Object[] objs = ipcGetSamples(m_window, exp_id, lo_idx, hi_idx);
    if (objs != null) {
      final long[][] array = (long[][]) objs[0];
      final long[] starts = (long[]) objs[1];
      final long[] ends = (long[]) objs[2];
      final long[] rtimes = (long[]) objs[3];
      final String[] start_label = (String[]) objs[4];
      final String[] end_label = (String[]) objs[5];
      final int[] samp_id = (int[]) objs[6];

      int nsmp = array.length;

      event = new Sample[nsmp];
      for (int i = 0; i < nsmp; i++) {
        long[] list = array[i];
        long[] mstates = list.clone();
        Sample samp =
            new Sample(
                samp_id[i],
                start_label[i],
                end_label[i],
                starts[i],
                ends[i],
                rtimes[i],
                mstates,
                exp_name,
                time_adjust);
        event[i] = samp;
      }
    } else {
      event = new Sample[0];
    }
    return event;
  }

  // CXXX Bug 20801848 - return type may not be Sample[]
  private static Sample[] ipcGetGCEventObjs(
      AnWindow m_window, int exp_id, long lo_idx, long hi_idx, String exp_name, long time_adjust) {
    Sample[] event;

    final Object[] objs = ipcGetGCEvents(m_window, exp_id, lo_idx, hi_idx);
    if (objs != null) {
      final long[] starts = (long[]) objs[0];
      final long[] ends = (long[]) objs[1];
      final int[] gcevent_id = (int[]) objs[2];

      int nsmp = gcevent_id.length;

      event = new Sample[nsmp];
      long[] mstates = new long[0];
      for (int i = 0; i < nsmp; i++) {
        Sample gcevent =
            new Sample(
                gcevent_id[i],
                null,
                null,
                starts[i],
                ends[i],
                ends[i] - starts[i],
                mstates,
                exp_name,
                time_adjust);
        event[i] = gcevent;
      }
    } else {
      event = new Sample[0];
    }

    return event;
  }

  private static Object[] ipcGetTLDetails(
      AnWindow m_window,
      final int exp_id,
      final int data_id,
      final int entity_prop_id,
      final long event_id) {
    synchronized (IPC.lock) {
      m_window.IPC().send("getTLDetails"); // NOI18N
      m_window.IPC().send(0);
      m_window.IPC().send(exp_id);
      m_window.IPC().send(data_id);
      m_window.IPC().send(entity_prop_id);
      m_window.IPC().send(event_id);
      return (Object[]) m_window.IPC().recvObject();
    }
  }

  private static Object[] ipcGetSamples(
      AnWindow m_window, final int exp_id, long hi_idx, long lo_idx) {
    synchronized (IPC.lock) {
      m_window.IPC().send("getSamples"); // NOI18N
      m_window.IPC().send(0);
      m_window.IPC().send(exp_id);
      m_window.IPC().send(hi_idx);
      m_window.IPC().send(lo_idx);
      return (Object[]) m_window.IPC().recvObject();
    }
  }

  private static Object[] ipcGetGCEvents(
      AnWindow m_window, final int exp_id, long hi_idx, long lo_idx) {
    synchronized (IPC.lock) {
      m_window.IPC().send("getGCEvents"); // NOI18N
      m_window.IPC().send(0);
      m_window.IPC().send(exp_id);
      m_window.IPC().send(hi_idx);
      m_window.IPC().send(lo_idx);
      return (Object[]) m_window.IPC().recvObject();
    }
  }
}
