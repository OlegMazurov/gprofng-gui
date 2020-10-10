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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.IPC;
import org.gprofng.mpmt.ipc.IPCContext;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCListener;
import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.timeline.events.DetailsIPC;
import org.gprofng.mpmt.timeline2.data.GenericEvent;
import org.gprofng.mpmt.timeline2.data.SampleEvent;
import org.gprofng.mpmt.timeline2.data.StackEvent;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Fetches requested rows (RowDataRequestParams) from er_print and places
// row data in TL2DataSnapshot.  Data fetch is asynchronous to the awt thread.
public final class TL2DataFetcher {
  // passed in on contstruction
  public final AnWindow anWindow; // for IPC  //YXXX make private
  private final TimelineView timelineView; // for doRepaint()

  // storage for timeline data
  private boolean needsFetchAllStackStates;
  private final HashMap<Long, long[]>
      stack_htable; // unique stacks; key=stack_id, value=array of function IDs
  private final HashSet<Long> stacks_pending; // unique stacks; key=stack_id
  private final BlockingQueue<Boolean> stackFetcherQ;

  // use the following lock with members below
  private final Object dataRequestLock;
  private RowDataRequestParams dataRequestOnDeck; // on deck
  private RowDataRequestParams dataRequestHead; // in progress

  // use the following lock with members below
  private final Object snapshotLock;
  private TL2DataSnapshot tl2DataSnapshot; // info for all rows

  private final ArrayList<TL2DataFetchListener> fetchTLRowDataListeners;
  private final IPCContext ipcContext;

  public TL2DataFetcher(TimelineView tl2Disp, final AnWindow awindow) {
    this.anWindow = awindow;
    this.timelineView = tl2Disp;

    needsFetchAllStackStates = true;
    stack_htable = new HashMap(1024);
    stacks_pending = new HashSet();
    stackFetcherQ = new LinkedBlockingQueue<>();

    dataRequestLock = new Object();
    dataRequestOnDeck = null;
    dataRequestHead = null;

    snapshotLock = new Object();
    tl2DataSnapshot = null;

    fetchTLRowDataListeners = new ArrayList();

    ipcContext =
        IPCContext.newDBEIPCContext(
            "TL2DataFetcher",
            IPCContext.Scope.SESSION,
            false,
            false /* true*/,
            IPCContext.Request.GROUP,
            anWindow);

    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            try {
              while (true) {
                stackFetcherQ.take();
                if (needsFetchAllStackStates) {
                  int numAdded = anWindow.getColorChooser().getColorMap().updateFullColorMapIPC();
                  needsFetchAllStackStates = false;
                }
                ipcAddStackIds();
              }
            } catch (InterruptedException ex) {
              int ii = 1;
            }
          }
        },
        "TL2StackFetcherThread");
  }

  // ---- called from non-awt threads
  public void resetAll() {
    /* start over including colors, selection */
    synchronized (stack_htable) {
      stack_htable.clear();
      stacks_pending.clear();
    }
    needsFetchAllStackStates = true;

    anWindow
        .getColorChooser()
        .getColorMap()
        .reset(); // YXXX  should instead be called from AnWindow or something?
    resetSnapshot();
  }

  public void resetSnapshot() {
    /* clear screen but keep selection, colors */
    setTL2DataSnapshot(null);
    enqueueRequest(null);
  }

  public void setTL2DataSnapshot(TL2DataSnapshot snapshot) {
    boolean updated = false;
    synchronized (snapshotLock) {
      if (tl2DataSnapshot != snapshot) {
        updated = true;
        tl2DataSnapshot = snapshot;
        // YXXX cancel old IPC when that becomes possible
      }
    }
    if (updated) {
      notifyNewDataSnapshot();
    }
  }

  public TL2DataSnapshot getTL2DataSnapshot() {
    synchronized (snapshotLock) {
      return tl2DataSnapshot;
    }
  }

  // ---- listeners
  public interface TL2DataFetchListener {
    public void not_edt_newDataSnapshot();
  }

  public void addTL2DataFetchListener(TL2DataFetchListener listener) {
    fetchTLRowDataListeners.add(listener); // assumes single threaded init
  }

  private void notifyNewDataSnapshot() {
    for (TL2DataFetchListener listener : fetchTLRowDataListeners) {
      listener.not_edt_newDataSnapshot();
    }
  }

  // ---- request queue (2 deep.  If full, previous on-deck request discarded)
  private int enqueueRequest(RowDataRequestParams newReq) {
    final int newSize;
    synchronized (dataRequestLock) {
      if (dataRequestHead == null) {
        dataRequestHead = newReq;
        newSize = 1;
      } else {
        // replace any old request with new request (intentionally lossy)
        dataRequestOnDeck = newReq;
        dataRequestHead.cancel(); // aborts loop in fetchTLRows()
        newSize = 2;
      }
    }
    return newSize;
  }

  private RowDataRequestParams nextRequest() {
    synchronized (dataRequestLock) {
      dataRequestHead = dataRequestOnDeck;
      dataRequestOnDeck = null;
      return dataRequestHead;
    }
  }

  private RowDataRequestParams headRequest() {
    synchronized (dataRequestLock) {
      return dataRequestHead;
    }
  }

  // ---- called from AWT thread:
  public boolean edt_fetchTLRowDataInitiate(final RowDataRequestParams newReq) {

    if (newReq.snapshot == null) {
      return false; // probably waiting for experiment to load
    }
    synchronized (snapshotLock) {
      if (newReq.snapshot != tl2DataSnapshot) {
        return false; // request is out of date.
      }
    }

    int newSize = enqueueRequest(newReq);
    if (newSize > 1) {
      return true; // request is not first in queue, loop below will get to it eventually
    }

    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            RowDataRequestParams currReq = headRequest();
            while (currReq != null) {
              not_edt_fetchTLRows(currReq);
              currReq = nextRequest();
            }
          }
        },
        "TL2DataFetcherThread");
    return true;
  }

  private boolean subsetOf(RowDataRequestParams master, RowDataRequestParams request) {
    if (master == null || request == null) {
      return false;
    }
    if (master.binTime == request.binTime
        && master.alignedTimeStart <= request.required_timeStart
        && master.timeEnd >= request.required_timeEnd
        && master.snapshot == request.snapshot) {
      return true;
    }
    return false;
  }

  private void invalidateRowData(TL2DataSnapshot tl2DataSnapshot) {
    List<RowData> rowDataArray = tl2DataSnapshot.getRowData();
    for (RowData rowData : rowDataArray) {
      rowData.invalidateEventData();
      // YXXX cancel old IPC?
    }
  }

  private void invalidateRowsWithChanges(RowDataRequestParams request) {
    // Causes row.getEvents() and row.getChartData()
    //   to return null until data is refreshed.
    List<RowData> rowDataArray = request.snapshot.getRowData();
    int nRows = rowDataArray.size();
    // rowDataArray defines "all rows" at full zoom out
    for (int i = request.rowStart; i <= request.rowEnd; i++) {
      if (i >= nRows) {
        break; // weird
      }
      RowData row = rowDataArray.get(i);
      RowDataRequestParams oldParams = row.getParams(); // params mutated by not_edt_fetchTLRows()
      if (request.forceLoad || !subsetOf(oldParams, request)) {
        row.invalidateEventData();
      }
    }
  }

  private synchronized void not_edt_fetchTLRows(final RowDataRequestParams request) {
    // note: above "synchronized" not needed if calls never overlap
    {
      if (request.snapshot != getTL2DataSnapshot()) {
        // request is no longer valid
        //    ...change in [exp add/drop, filters, grouping, mode]
        invalidateRowData(request.snapshot);
        // YXXX do we need to free old IPC Context here? THOMAS?
        return;
      }

      invalidateRowsWithChanges(request); // doesn't need to be sync'd

      // start fetching new data.
      List<RowData> rowDataArray = request.snapshot.getRowData();

      List<RowData> fetchRows = new ArrayList();
      // add required rows
      for (int i = request.required_rowStart; i <= request.required_rowEnd; i++) {
        RowData row = rowDataArray.get(i);
        fetchRows.add(row);
      }
      // add prefetch rows, starting with those nearest required rows
      int lo = request.required_rowStart - 1;
      int hi = request.required_rowEnd + 1;
      while (lo >= request.rowStart || hi <= request.rowEnd) {
        if (lo >= request.rowStart) {
          RowData row = rowDataArray.get(lo);
          fetchRows.add(row);
        }
        if (hi <= request.rowEnd) {
          RowData row = rowDataArray.get(hi);
          fetchRows.add(row);
        }
        hi++;
        lo--;
      }
      Queue<IPCResult> ipcResultQ = new LinkedList<>();
      Queue<RowData> rowQ = new LinkedList<>();
      for (RowData row : fetchRows) {
        if (request.isCancelled()) {
          // fetchTLRowDataInitiate() called, new TLDataSnapshot not required (e.g. for pan, zoom)
          //                    ipcContext.cancel(); // YXXX IPC is not complete here.  Should
          // probably cancel ipcContext? // THOMAS FIXME
          break;
        }
        if (row.eventDataIsValid()) {
          // old data is fine; skip fetch
          continue;
        }
        IPCResult ipcResult =
            ipcStart_getTLData(ipcContext, null, row, request); // non-blocking ipc start

        ipcResultQ.add(ipcResult);
        rowQ.add(row);

        if (ipcResultQ.size() > 5) { // because cancel isn't implemented right, need to cap this
          process_fetchTLRows(request, rowQ, ipcResultQ);
        }
      }
      // last rows
      process_fetchTLRows(request, rowQ, ipcResultQ);
    }
  }

  private void process_fetchTLRows(
      final RowDataRequestParams request, Queue<RowData> rows, Queue<IPCResult> ipcResults) {
    while (ipcResults.size() > 0) {
      IPCResult savedIpcResult = ipcResults.remove();
      RowData savedRow = rows.remove();
      Object[] data = (Object[]) savedIpcResult.getObject();
      if (request.isCancelled()) {
        continue;
      }
      if (data != null) {
        ipcFinish_getTLData(data, savedRow, request);
      }
    }
  }

  // ---- from MetaExperiment.java

  public long[] getStackFuncArray(final long stackId) { // YXXX slow
    long[] stackFuncs;
    Long stackIdL = stackId;
    synchronized (stack_htable) {
      stackFuncs = stack_htable.get(stackIdL);
    }
    if (stackFuncs == null) {
      stackFuncs = new long[0];
      // System.err.println("XXXmpview Experiment.java::getStackFuncArray("+stackId+") returning
      // null");
    }
    return stackFuncs;
  }

  public void ipcSetStackFuncArray(final long stackId) { // IPC!!
    long stackIds[] = new long[1];
    stackIds[0] = stackId;
    detectNewStackIds(stackIds);
  }

  // ---IPC--- (Native methods from liber_dbe.so)

  private IPCResult ipcStart_getTLData(
      final IPCContext ipcContext,
      final IPCListener ipcListener,
      final RowData rowData,
      final RowDataRequestParams request) {
    final RowDefinition rowDef = rowData.rowDefinition;
    long current_time_adjust = rowDef.getTimeOrigin();
    long startt = request.alignedTimeStart + current_time_adjust;

    IPCResult ipcResult =
        ipcStart_getTLData(
            ipcContext,
            ipcListener,
            rowDef.getExpID(),
            rowDef.getDataDescriptor().getDataId(),
            rowDef.getEntity().getPropId(),
            rowDef.getEntity().getPropValue(),
            rowDef.getAux(),
            startt,
            request.binTime,
            request.nbins,
            true,
            rowDef.getChartPropNames());
    return ipcResult;
  }

  private void ipcFinish_getTLData(
      Object data[],
      final RowData rowData, // IPC!!
      final RowDataRequestParams request) {
    Object representatives[] = (Object[]) data[0];
    final GenericEvent[] events;
    if (representatives == null) {
      events = null;
    } else {
      // each of the following is the same length:
      int startbins[] = (int[]) representatives[0];
      int eventbins[] = (int[]) representatives[1];
      long event_ids[] = (long[]) representatives[2]; // DataView indexes
      long stack_ids[] = (long[]) representatives[3];
      int mstate[] = (int[]) representatives[4];
      long sampleVals[][] = (long[][]) representatives[5];
      long timeStart[] = (long[]) representatives[6];
      long timeEnd[] = (long[]) representatives[7];

      long timeAdjust = rowData.rowDefinition.getTimeOrigin();

      if (rowData.getRowDef().getTLDataType().equals(Settings.TLData_type.TL_GCEVENT)) {
        // CXXX Bug 20801848 - maybe create GCEvent instead of GenericEvent
        events = new GenericEvent[event_ids.length];
        for (int k = 0; k < event_ids.length; k++) {
          events[k] =
              new GenericEvent(
                  event_ids[k],
                  startbins[k],
                  eventbins[k],
                  timeStart[k] - timeAdjust,
                  timeEnd[k] - timeAdjust);
        }
      } else if (sampleVals != null) {
        // if (tltype == Presentation.TLData_type.TL_SAMPLE }) {
        events = new SampleEvent[event_ids.length];
        for (int k = 0; k < event_ids.length; k++) {
          events[k] =
              new SampleEvent(
                  event_ids[k],
                  startbins[k],
                  eventbins[k],
                  timeStart[k] - timeAdjust,
                  timeEnd[k] - timeAdjust,
                  sampleVals[k]);
        }
      } else {
        events = new StackEvent[event_ids.length];
        for (int k = 0; k < event_ids.length; k++) {
          final long stack_id;
          if (stack_ids == null) {
            stack_id = DetailsIPC.INVALID_STACK_ID;
          } else {
            stack_id = stack_ids[k];
          }
          events[k] =
              new StackEvent(
                  event_ids[k],
                  startbins[k],
                  eventbins[k],
                  timeStart[k] - timeAdjust,
                  timeEnd[k] - timeAdjust,
                  stack_id,
                  mstate != null ? mstate[k] : -1);
        }
      }

      detectNewStackIds(stack_ids);
    }

    final Object chartPropVals[] = (Object[]) data[1];
    if (chartPropVals != null) {
      for (int propNum = 0; propNum < chartPropVals.length; propNum++) {
        Object thisProp = chartPropVals[propNum];
        if (thisProp == null) {
          continue;
        }
        if (thisProp instanceof long[][]) {
          // thisProp represents multiple states; convert to Events
          long perBinStates[][] = (long[][]) thisProp;

          // sparsely populated; determine how many actual samples
          int nSamples = 0;
          for (Object binData : perBinStates) {
            if (binData != null) {
              nSamples++;
            }
          }
          SampleEvent sampleEvents[] = new SampleEvent[nSamples];
          int sampleIdx = 0;
          // for now, charts binTimeStart/binTimeEnd not used; use request duration
          final long binTimeStart = 0;
          final long binTimeEnd = request.binTime - 1;
          for (int bin = 0; bin < perBinStates.length; bin++) {
            long stateVals[] = (long[]) perBinStates[bin];
            if (stateVals == null) {
              continue;
            }
            sampleEvents[sampleIdx++] =
                new SampleEvent(-1, bin, 1, binTimeStart, binTimeEnd, stateVals);
          }
          chartPropVals[propNum] = sampleEvents; // replace w/ Events
        }
      }
    }
    { // IPC: if needed, code from here down could be done in separate thread
      rowData.setEventData(request, events, chartPropVals);
      timelineView.repaintTLDrawer();
    }
  }

  public void detectNewStackIds(long stack_ids[]) {
    if (stack_ids == null || stack_ids.length == 0) {
      return;
    }
    boolean missing = false;
    synchronized (stack_htable) {
      for (long stack_id : stack_ids) {
        if (!stack_htable.containsKey(stack_id)) {
          stacks_pending.add(stack_id);
          missing = true;
        }
      }
    }
    if (missing) {
      try {
        stackFetcherQ.put(true);
      } catch (InterruptedException ex) {
        int ii = 1;
      } // send wakeup
    }
  }

  private void ipcAddStackIds() { // IPC!!
    final int sz;
    final long newStackIdsArray[];
    synchronized (stack_htable) {
      if (stacks_pending == null || stacks_pending.isEmpty()) {
        return;
      }
      sz = stacks_pending.size();
      newStackIdsArray = new long[sz];
      int ii = 0;
      for (Long stackId : stacks_pending) {
        newStackIdsArray[ii++] = stackId.longValue();
      }
    }

    Object[] stacks = anWindow.getStacksFunctions(newStackIdsArray); // IPC!!
    HashSet<Long> newFunctions = new HashSet();
    synchronized (stack_htable) {
      for (int ii = 0; ii < sz; ii++) {
        long stack_id = newStackIdsArray[ii];
        long[] stack = (long[]) stacks[ii];
        long[] old = stack_htable.put(stack_id, stack);
        if (old == null) {
          // new stack detected
          boolean changed = stacks_pending.remove(stack_id);
          if (!changed) {
            int iii = 1; // weird
          }
          // store unique functions
          for (long function : stack) {
            newFunctions.add(new Long(function));
          }
        }
      }
      if (stacks_pending.size() > 0) {
        int ii = 1; // for breakpoint.  could happen
      }
    }
    anWindow.getColorChooser().getColorMap().addFunctionsIPC(newFunctions);
    timelineView.repaintTLDrawer();
  }

  private IPCResult ipcStart_getTLData(
      IPCContext ipcContext,
      IPCListener ipcListener,
      int exp_id,
      int data_id,
      int entity_prop_id,
      int entity_prop_val,
      int hwctag,
      long start_ts,
      long delta,
      int ndelta,
      boolean getRepresentatives,
      final String[] chartProperties) {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, ipcContext);
    {
      ipcHandle.append("getTLData");
      ipcHandle.append(0);
      ipcHandle.append(exp_id);
      ipcHandle.append(data_id);
      ipcHandle.append(entity_prop_id);
      ipcHandle.append(entity_prop_val);
      ipcHandle.append(hwctag);
      ipcHandle.append(start_ts);
      ipcHandle.append(delta);
      ipcHandle.append(ndelta);
      ipcHandle.append(getRepresentatives);
      ipcHandle.append(chartProperties);
    }
    if (ipcListener != null) {
      ipcHandle.addIPCListener(ipcListener);
    }
    IPCResult ipcResult = ipcHandle.sendRequest();
    return ipcResult;
  }

  public IPCResult ipcStart_getTLEventCenterTime(
      IPCContext ipcContext,
      IPCListener ipcListener,
      int exp_id,
      int data_id,
      int entity_prop_id,
      int entity_prop_val,
      int hwctag,
      long event_idx,
      long move_count) {
    if (ipcContext == null) {
      ipcContext = this.ipcContext;
    }
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE, ipcContext);
    {
      ipcHandle.append("getTLEventCenterTime");
      ipcHandle.append(0);
      ipcHandle.append(exp_id);
      ipcHandle.append(data_id);
      ipcHandle.append(entity_prop_id);
      ipcHandle.append(entity_prop_val);
      ipcHandle.append(hwctag);
      ipcHandle.append(event_idx);
      ipcHandle.append(move_count);
    }
    if (ipcListener != null) {
      ipcHandle.addIPCListener(ipcListener);
    }
    IPCResult ipcResult = ipcHandle.sendRequest();
    return ipcResult;
  }

  public long ipcGetTLEventIdxNearTime(
      int exp_id,
      int data_id,
      int entity_prop_id,
      int entity_prop_val,
      int hwctag,
      int searchDirection,
      long ts) {
    synchronized (IPC.lock) {
      anWindow.IPC().send("getTLEventIdxNearTime");

      anWindow.IPC().send(0);
      anWindow.IPC().send(exp_id);
      anWindow.IPC().send(data_id);
      anWindow.IPC().send(entity_prop_id);
      anWindow.IPC().send(entity_prop_val);
      anWindow.IPC().send(hwctag);
      anWindow.IPC().send(searchDirection);
      anWindow.IPC().send(ts);
      return anWindow.IPC().recvLong();
    }
  }
}
