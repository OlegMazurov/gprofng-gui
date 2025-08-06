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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.ipc.IPCResult;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.statecolors.StackState;
import org.gprofng.mpmt.timeline.events.*;
import org.gprofng.mpmt.timeline2.TL2DataSnapshot.*;
import org.gprofng.mpmt.timeline2.cursorevent.*;
import org.gprofng.mpmt.timeline_common.*;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.ruler.valuetypes.ValuesLong;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.SwingUtilities;

// This class' public methods MUST only be accessed from AWT event thread

public final class TL2Drawer implements TimelineDrawer {
  // params
  private TimelinePanel timelinePanel;
  private final TL2DataFetcher tl2DataFetcher;

  // image data
  private final CoordCalcTimeMaster timeAxisMaster;
  private final CoordCalcTimeReader timeAxisReader;
  private final CoordCalcDataMaster dataAxisMaster;
  private final CoordCalcDataReader dataAxisReader;

  public TimelineDraw event_drawer;

  // listeners
  private ArrayList<TimelineSelectionListener> selection_listeners;

  // cursor location (most recent location of user focus)
  private RowGeometry cursor_rowGeometry = null;
  private RowGeometry cursor_rowGeometry_alt = null;

  // cursor movement event queue
  private final LinkedBlockingQueue<CursorEvent>
      cursorEventQ; // Use queue because up/down/left/right need IPC
  private final Object cursorEventQLock = new Object(); // used for syncing peek/take combos

  // selection of row blocks (entities/samples for use in filters)
  private TreeSet<Integer> selected_row_blocks; // indices for selected_row_blocks_info
  private List<EntityDefinitions> selected_entity_info;
  private int selected_row_blocks_anchor = -1; // for shift-click multiselect

  // zoom
  private long old_time_range = 0;

  // components
  // private RangeRuler verticalRuler;
  private IconRuler verticalRuler;

  // fetching
  private long data_time_start;
  private long data_bin_time;
  private int data_time_nbins;
  private int data_row_start;
  private int data_row_end;

  // options

  public TL2Drawer( // Called even before TL tab is popped to top
      final TL2DataFetcher tl2DataFetcher) {
    this.tl2DataFetcher = tl2DataFetcher;

    // placeholder initialization; real init is later via TL2DataFetchListener
    timeAxisMaster = new CoordCalcTimeImpl(TimelineDraw.HMARGIN);
    timeAxisReader = timeAxisMaster;

    dataAxisMaster = new TL2CoordCalcData(0);
    dataAxisMaster.setExpandToFill(false);
    dataAxisReader = dataAxisMaster;

    verticalRuler = new IconRuler(new ValuesLong(0, 0));
    selection_listeners = new ArrayList<>();
    cursorEventQ = new LinkedBlockingQueue();
    cursorEventQueueInit();

    resetSelRowBlocks();
    resetFetchHistory();

    event_drawer =
        new TimelineDraw(
            tl2DataFetcher,
            tl2DataFetcher.anWindow.getColorChooser().getColorMap(),
            verticalRuler,
            timeAxisReader,
            dataAxisMaster);
    tl2DataFetcher.addTL2DataFetchListener(
        new TL2DataFetcher.TL2DataFetchListener() {
          public void not_edt_newDataSnapshot() {
            AnUtility.dispatchOnSwingThread(
                new Runnable() {
                  public void run() {
                    TL2DataSnapshot tl2DataSnapshot = tl2DataFetcher.getTL2DataSnapshot();
                    event_drawer.edt_processDataSnapshot(tl2DataSnapshot, true, null);
                    if (timelinePanel != null) {
                      long new_range = event_drawer.getAbsoluteTimeEnd();
                      if (new_range != 0 && old_time_range != new_range) {
                        // YXXX should we only do this once?
                        old_time_range = new_range;
                        timelinePanel.edt_resetTLRanges();
                        //                                timelinePanel.zoomHistoryAdd();
                        attemptToSelectVisibleStackEvent();
                      } else {
                        // for example, filter set
                        remapSelections();
                      }

                      timelinePanel.edt_revalidateAll(
                          true); // init timeAxisCalculator, dataAxisCalculator
                    }
                  }
                });
          }
        });
  }

  public void edt_resetAll() {
    old_time_range = 0;
    resetFetchHistory();
    event_drawer.edt_resetAll();
    cursorEventQueueClear();
    edt_processTimelineSelectionEvent_internal(
        null, true); // sends TimelineSelectionEvent notifications
  }

  private void resetFetchHistory() {
    data_time_start = 0;
    data_bin_time = 0;
    data_time_nbins = 0;
    data_row_start = 0;
    data_row_end = 0;
  }

  public void setParent(TimelinePanel parent) {
    this.timelinePanel = parent;
  }

  public CoordCalcTimeMaster getTimeAxisMaster() {
    return timeAxisMaster;
  }

  public CoordCalcDataMaster getDataAxisMaster() {
    return dataAxisMaster;
  }

  public CoordCalcTimeReader getTimeAxisReader() {
    return timeAxisReader;
  }

  public CoordCalcDataReader getDataAxisReader() {
    return dataAxisReader;
  }

  public VerticalRowRuler getVerticalRuler() {
    return verticalRuler;
  }

  public boolean getEnableZoomVertical() {
    return false; // disable 2-d drag zoom and the overlay version of the zoom slider
  }

  public boolean getEnableZoomOverlays() {
    return false; // disable the overlay version of the zoom sliders
  }

  public boolean getEnableUnalignedRows() {
    return true;
  }

  public boolean getEnableTimeCaliper() {
    return true;
  }

  // ------ get X & Y max dimensions

  public long getAbsoluteTimeStart() {
    return event_drawer.getAbsoluteTimeStart();
  }

  public long getAbsoluteTimeEnd() {
    return event_drawer.getAbsoluteTimeEnd();
  }

  public long getAbsoluteTimeDuration() {
    return getAbsoluteTimeEnd() - getAbsoluteTimeStart() + 1;
  }

  public int getAbsoluteRowEnd() {
    return dataAxisReader.getAbsRowEnd(); // YXXX shouldn't we get this from event_drawer?
  }

  public int getAbsoluteRowCount() {
    return dataAxisReader.getAbsRowCount(); // YXXX shouldn't we get this from event_drawer?
  }

  public void zoomCenterSelection() {
    // no state to change
  }

  public boolean selectionActive() {
    return event_drawer.selectionActive();
  }

  public long getSelectionTimeCenter() {
    long center =
        (event_drawer.getSelectionTimeEnd() + event_drawer.getSelectionTimeStart())
            / 2; // YXXX perhaps not the same formula used for zoom or dbe
    return center;
  }

  public long getSelectionTimeStart() {
    return event_drawer.getSelectionTimeStart();
  }

  public long getSelectionTimeEnd() {
    return event_drawer.getSelectionTimeEnd();
  }

  public double getSelectionYCenter() {
    return event_drawer.getSelectionYCenter();
  }

  // --- "state" selection via cursor

  public boolean getSelectionNavigationEnabled() {
    //        if(event_drawer.getVisibleSelection()==null){
    if (event_drawer.getSelection() == null) {
      return false;
    }
    return true;
  }

  // --- combined selection and selRowBlock ops

  private void remapSelections() { // AWT thread only
    TL2DataSnapshot tl2DataSnapshot = event_drawer.getDataSnapshot();
    if (tl2DataSnapshot == null) {
      return; // refetch in progress; don't check or update old selections
    }
    remapSelRowBlocks();

    TimelineSelectionEvent oldEvt = event_drawer.getRecentSelection();
    if (oldEvt == null) { // no previous selection to remap
      edt_processTimelineSelectionEvent(null); // IPC not needed to clear selections, do it now
      return;
    }
    final RowGeometry newRow = remapRow(oldEvt.getRowGeometry());
    final CursorSetEvent newEvent;
    if (newRow == null) {
      // row could not be remapped, select an event near that row if possible
      int rowNum = oldEvt.rowNum;
      if (rowNum > event_drawer.getRowGeometry().size() - 1) {
        rowNum = event_drawer.getRowGeometry().size() - 1;
      }
      if (rowNum < 0) {
        // don't even have a row to select
        edt_processTimelineSelectionEvent(null); // IPC not needed to clear selections, do it now
        return;
      }
      RowGeometry row = event_drawer.getRowGeometry().get(rowNum);
      row = row.getMasterRow();
      newEvent = new CursorSetEvent(row, oldEvt.clickTime, false, false, false, false);
    } else {
      newEvent =
          new CursorSetEvent(
              newRow,
              oldEvt.clickTime,
              false,
              false,
              //                    oldEvt.eventIdx,  // filters may have changed...
              -1, // ... so search by time
              oldEvt.eventTimeStart,
              oldEvt.eventTimeEnd,
              false,
              false,
              false);
    }
    cursorEventQueueSet(newEvent);
    // To avoid delayed Y scrolling while waiting for selection details...
    // ... we could generate a new kind of no-ipc Y selection event
  }

  public boolean attemptToSelectVisibleStackEvent() {
    TimelineSelectionEvent selection_event = event_drawer.getSelection();
    if (selection_event != null) {
      return false; // selection already exists, don't undo it
    }

    // no row selected, try to find a row with a callstack.
    double pct_start = dataAxisReader.getVisibleStart();
    int row_start = dataAxisReader.getRowNearPercent(pct_start);
    RowGeometry rowGeometry;
    rowGeometry = getNearbyRowForEventSelection(row_start, true);
    if (rowGeometry == null) {
      rowGeometry = getNearbyRowForEventSelection(row_start, false);
    }
    if (rowGeometry != null) {
      long start_time = timeAxisReader.getTimeStart();
      long end_time = timeAxisReader.getTimeEnd();
      long mid_time = (start_time + end_time) / 2;
      cursorEventQueueAdd(new CursorSetEvent(rowGeometry, mid_time, false, false, true, true));
      return true;
    }
    //          processTimelineSelectionEvent(null);// force unselect; call directly, no IPC needed
    return false;
  }

  // --- multi-selection of row blocks (i.e. entities + samples)

  private void resetSelRowBlocks() {
    selected_row_blocks = new TreeSet();
    selected_row_blocks_anchor = -1;
    selected_entity_info = null;
    cursor_rowGeometry = null;
    cursor_rowGeometry_alt = null;
  }

  private void updateSelRowBlocks(
      TL2DataSnapshot dataSnapshot, int blockNum, boolean ctrl, boolean shift) {
    if (blockNum < 0) {
      int foo = 1; // when resetting blocks
    } else if ((!ctrl && !shift) || (shift && selected_row_blocks_anchor == -1)) {
      selected_row_blocks.clear();
      selected_row_blocks.add(blockNum);
      selected_row_blocks_anchor = blockNum;
    } else if (ctrl) {
      if (!selected_row_blocks.remove(blockNum)) {
        selected_row_blocks.add(blockNum);
        selected_row_blocks_anchor = blockNum;
      }
    } else if (shift) {
      selected_row_blocks.clear();
      final int lo, hi;
      if (selected_row_blocks_anchor < blockNum) {
        lo = selected_row_blocks_anchor;
        hi = blockNum;
      } else {
        lo = blockNum;
        hi = selected_row_blocks_anchor;
      }
      for (int ii = lo; ii <= hi; ii++) {
        selected_row_blocks.add(ii);
      }
    }
    // save the mapping of blocks
    final TimelineSelectionRowEvent rowEvent;
    if (dataSnapshot == null) {
      rowEvent = new TimelineSelectionRowEvent(false);
    } else {
      selected_entity_info = dataSnapshot.getEntityDefinitions();
      rowEvent = new TimelineSelectionRowEvent(!selected_row_blocks.isEmpty());
    }
    event_drawer.setSelectedRows(selected_row_blocks); // YXXX could we refactor this?
    genericNotifyTimelineSelectionListeners(rowEvent);
  }

  private void remapSelRowBlocks() {
    TL2DataSnapshot tl2DataSnapshot = event_drawer.getDataSnapshot();
    if (tl2DataSnapshot == null) {
      return; // refetch in progress; don't check or update old selections
    }
    if (selected_entity_info == null) {
      selected_row_blocks = new TreeSet();
      return; // no history
    }
    TreeSet<Integer> newBlockSelList = new TreeSet();
    for (Integer oldBlockNum : selected_row_blocks) {
      if (oldBlockNum >= selected_entity_info.size()) {
        return; // weird
      }
      RowDefinition rowDef0 = selected_entity_info.get(oldBlockNum).entityRows.get(0);
      List<EntityDefinitions> newBlocks = tl2DataSnapshot.getEntityDefinitions();
      int newBlockNum = 0;
      for (EntityDefinitions rowBlock : newBlocks) {
        RowDefinition tmpRowDef = rowBlock.entityRows.get(0);
        if (rowDef0.matches(-1, tmpRowDef, -1)) {
          newBlockSelList.add(newBlockNum);
          break;
        }
        newBlockNum++;
      }
    }
    selected_row_blocks = newBlockSelList;
    selected_entity_info = tl2DataSnapshot.getEntityDefinitions();
    TimelineSelectionRowEvent rowEvent =
        new TimelineSelectionRowEvent(!selected_row_blocks.isEmpty());
    event_drawer.setSelectedRows(selected_row_blocks); // YXXX could we refactor this?
    genericNotifyTimelineSelectionListeners(rowEvent);
  }

  private RowGeometry remapRow(RowGeometry oldGeometry) {
    if (oldGeometry == null) {
      return null;
    }
    List<RowGeometry> currentRows = event_drawer.getRowGeometry();
    int oldChartPropIdx = oldGeometry.chartPropIdx;
    RowDefinition oldRowDef = oldGeometry.rowData.getRowDef();
    for (RowGeometry curGeo : currentRows) {
      int curChartPropIdx = curGeo.chartPropIdx;
      RowDefinition currRowDef = curGeo.rowData.getRowDef();
      if (currRowDef.matches(curChartPropIdx, oldRowDef, oldChartPropIdx)) {
        return curGeo;
      }
    }
    return null;
  }

  // --- event selection
  private RowGeometry getAdjacentRowGeo(int rowNum) {
    List<RowGeometry> tmpGeoList = event_drawer.getRowGeometry();
    for (int kk = rowNum + 1, direction = 1; ; kk += direction) {
      if (kk >= tmpGeoList.size()) {
        // hit last row; start at middle and go backwards
        kk = rowNum - 1;
        direction = -1;
      }
      if (kk < 0 || kk >= tmpGeoList.size()) {
        break;
      }
      int blkNum = event_drawer.rowNum2EntityNum(kk);
      if (!selected_row_blocks.contains(blkNum)) {
        // row is not part of current block of rows
        return tmpGeoList.get(kk);
      }
    }
    return null;
  }

  private RowGeometry getNearbyRowForEventSelection(int rowNum, boolean onlyWithStacks) {
    List<RowGeometry> tmpGeoList = event_drawer.getRowGeometry();
    for (int kk = rowNum, direction = 1; ; kk += direction) {
      if (kk >= tmpGeoList.size()) {
        // hit last row; start at middle and go backwards
        kk = rowNum - 1;
        direction = -1;
      }
      if (kk < 0 || kk >= tmpGeoList.size()) {
        break;
      }
      RowGeometry row = tmpGeoList.get(kk);
      if (row.isChartData()) {
        // this row is a data chart
        continue;
      }
      Settings.TLData_type tlData_type =
          row.rowData.rowDefinition.getDataDescriptor().getTLDataType();
      if (onlyWithStacks
          && (tlData_type.equals(Settings.TLData_type.TL_SAMPLE)
              || tlData_type.equals(Settings.TLData_type.TL_HEAPSZ)
              || // YXXX add property for no stacks?
              tlData_type.equals(Settings.TLData_type.TL_GCEVENT))) { // CXXX Bug 20801848
        // ignore samples and heapsz when searching for a good row for initial selection
        continue;
      }
      return row;
    }
    return null;
  }

  private TimelineSelectionEvent
      ipcNewSelectionEvent( // not AWT thread.  Must only access final data.
          RowGeometry rowGeometry, // must be valid
          long clickTime, // must be valid
          boolean ctrlKey,
          boolean shiftKey,
          TimelineSelectionEvent.CursorAxis axis,
          long eventIdx, // may be -1
          boolean requestCaliperRefresh,
          boolean updateRowSelections) {

    RowDefinition rowDef = rowGeometry.rowData.rowDefinition;
    final IPCResult leftResult, rightResult;
    if (eventIdx == TimelineSelectionEvent.EVT_IDX_INVALID) {
      leftResult = rightResult = null;
    } else {
      leftResult = ipcStart_getTLEventCenterTime(rowDef, eventIdx, -1);
      rightResult = ipcStart_getTLEventCenterTime(rowDef, eventIdx, 1);
    }

    int rowNum = rowGeometry.rowNum;
    final EventDetail details;
    if (eventIdx == TimelineSelectionEvent.EVT_IDX_INVALID) {
      details = null;
    } else {
      details =
          DetailsIPC.ipcGetTLDetails(
              this.tl2DataFetcher.anWindow,
              rowDef.getExpID(),
              rowDef.getExpName(),
              rowDef.getDataDescriptor().getDataId(),
              rowDef.getDataDescriptor().getTLDataType(),
              rowDef.getEntity().getPropId(),
              eventIdx,
              rowDef.getCpuFreq(),
              rowDef.getTimeOrigin());
    }

    final Vector<StackState> stackFrames;
    if (details == null || details.getStackStates() == null) {
      stackFrames = null;
    } else {
      stackFrames = new Vector(details.getStackStates());
    }
    final boolean left =
        (leftResult != null && ipcFinish_getTLEventCenterTime(leftResult, rowDef) != null);
    final boolean right =
        (rightResult != null && ipcFinish_getTLEventCenterTime(rightResult, rowDef) != null);
    final boolean up;
    final boolean down;
    {
      int lastIdx = rowGeometry.parent.size() - 1;
      int masterOfLastRow = rowGeometry.parent.get(lastIdx).getMasterRow().rowNum;
      int thisMaster = rowGeometry.getMasterRow().rowNum;
      up = (rowNum < 1) ? false : true;
      down = (masterOfLastRow == thisMaster) ? false : true;
    }

    final long eventStartTime, eventEndTime;
    if (details != null) {
      eventEndTime = details.getTimestamp();
      if (details instanceof DurationEvent) {
        long duration = ((DurationEvent) details).getDuration();
        eventStartTime = eventEndTime - duration;
      } else {
        eventStartTime = eventEndTime;
      }
    } else {
      eventStartTime = eventEndTime = clickTime;
    }
    TimelineSelectionEvent selEvent =
        new TimelineSelectionEvent(
            rowGeometry,
            clickTime, // must be valid
            ctrlKey,
            shiftKey,
            axis,
            details,
            stackFrames,
            left,
            right,
            up,
            down,
            eventIdx,
            eventStartTime,
            eventEndTime,
            requestCaliperRefresh,
            updateRowSelections);

    // make sure this stack is known by tl2DataFetcher
    if (details instanceof ExtendedEvent) {
      ExtendedEvent extEvent = (ExtendedEvent) details;
      tl2DataFetcher.ipcSetStackFuncArray(extEvent.getStack()); // IPC!!
    }
    return selEvent;
  }

  private void edt_processTimelineSelectionEvent(TimelineSelectionEvent selEvent) {
    edt_processTimelineSelectionEvent_internal(selEvent, false);
  }

  private void edt_processTimelineSelectionEvent_internal(
      TimelineSelectionEvent selEvent, boolean forceRows) {
    do {
      if (selEvent == null) {
        selEvent = new TimelineSelectionEvent();
      }

      TL2DataSnapshot tl2DataSnapshot = event_drawer.getDataSnapshot();
      if (selEvent.rowNum == -1) {
        // row number unknown
        if (selEvent.updateRowSelections || forceRows) {
          resetSelRowBlocks();
          updateSelRowBlocks(tl2DataSnapshot, -1, false, false);
        }
        break; // go to notify
      }

      if (selEvent.updateRowSelections) {
        int tmpRowNum = limitRowRange(selEvent.rowNum); // is this needed?
        int blockNum = event_drawer.rowNum2EntityNum(tmpRowNum); // may be -1
        updateSelRowBlocks(tl2DataSnapshot, blockNum, selEvent.ctrlKey, selEvent.shiftKey);
      } else {
        int ii = 0; // for breakpoint
      }

      // set global state

      // save definition of current row so we can find it after filter
      cursor_rowGeometry = selEvent.getRowGeometry();
      // search for alternate cursor in case this one is filtered out
      cursor_rowGeometry_alt = getAdjacentRowGeo(selEvent.rowNum);
    } while (false);

    event_drawer.setSelection(selEvent);

    timelinePanel.repaint();
    timelinePanel
        .requestFocus(); // YXXX steals focus from other applications, is there a better way?
    genericNotifyTimelineSelectionListeners(selEvent);
  }

  private void genericNotifyTimelineSelectionListeners(TimelineSelectionGenericEvent ev) {
    for (TimelineSelectionListener listener : selection_listeners) {
      listener.valueChanged(ev);
    }
  }

  // --- used by TL2ControlPanel
  public List<EntityDefinitions> getRowDefsByBlock() {
    TL2DataSnapshot tl2DataSnapshot = event_drawer.getDataSnapshot();
    if (tl2DataSnapshot == null) {
      return null;
    }
    return tl2DataSnapshot.getEntityDefinitions();
  }

  public List<Integer> getRowBlockSelectList() {
    // returns a sorted list of selected row blocks (i.e. threads,samples...).
    // The list consists of indices for use with getRowDefsByBlock()
    return new ArrayList<>(selected_row_blocks);
  }

  // utilities
  private int limitRowRange(int rowNum) {
    // dataAxisCalculator should be in sync with rowDefs
    int lastRow = dataAxisReader.getAbsRowEnd();
    if (rowNum < 0) {
      return 0;
    }
    if (rowNum > lastRow) {
      return lastRow;
    }
    return rowNum;
  }

  private TimelineSelectionEvent ipcGetSelEvent(
      RowGeometry rowGeometry,
      long clickTime, // may be updated below
      final boolean ctrlKey,
      final boolean shiftKey,
      final TimelineSelectionEvent.CursorAxis axis,
      final long oldEventIdx,
      final long moveCount,
      final boolean requestCaliperUpdate,
      final boolean updateRowSelections) {
    if (rowGeometry == null) {
      return null;
    }
    RowDefinition rowDef = rowGeometry.rowData.rowDefinition;

    long eventIdx = oldEventIdx;
    if (eventIdx != TimelineSelectionEvent.EVT_IDX_INVALID) {
      long[] idx_and_time = ipcGetTLEventCenterTime(rowDef, oldEventIdx, moveCount);
      if (idx_and_time == null) { // event not found
        eventIdx = TimelineSelectionEvent.EVT_IDX_INVALID; // try again by time, below
      } else { // event found
        eventIdx = idx_and_time[0];
        long event_ts = idx_and_time[1];
        if (clickTime == TimelineDrawer.TIME_INVALID) {
          clickTime = event_ts; // update clickTime if not previously set
        }
      }
    }
    // note: eventIdx may be updated above
    if (eventIdx == TimelineSelectionEvent.EVT_IDX_INVALID
        && clickTime == TimelineDrawer.TIME_INVALID) {
      return null; // event cannot be found.
    }

    if (eventIdx == TimelineSelectionEvent.EVT_IDX_INVALID) {
      eventIdx = ipcGetTLEventIdxNearTime(rowDef, clickTime);
    }
    TimelineSelectionEvent selEvent =
        ipcNewSelectionEvent(
            rowGeometry,
            clickTime,
            ctrlKey,
            shiftKey,
            axis,
            eventIdx,
            requestCaliperUpdate,
            updateRowSelections);
    return selEvent;
  }

  // --- CursorEventQueue events (e.g. left/right/up/down) handled on worker thread

  private void cursorEventQueueInit() {
    AnUtility.dispatchOnAWorkerThread(
        new Runnable() {
          @Override
          public void run() {
            while (true) {
              cursorEventQueueTake();
            }
          }
        },
        "TL2CursorEventHandler");
  }

  private void cursorEventQueueTake() { // NOT on awt thread!
    try {
      final CursorEvent genericEvent = cursorEventQ.take();
      if (genericEvent instanceof CursorSetEvent) {
        CursorSetEvent head = (CursorSetEvent) genericEvent;
        not_edt_processCursorSetEvent(head);
      } else if (genericEvent instanceof CursorMoveEvent) {
        CursorMoveEvent head = (CursorMoveEvent) genericEvent;
        // look ahead to consolidate like-events
        int count = head.count;
        do {
          CursorMoveEvent anotherMove = null;
          synchronized (cursorEventQLock) {
            // synchronized with clear so that peek/take grab same value
            try {
              CursorEvent tmp = cursorEventQ.peek();
              if (tmp instanceof CursorMoveEvent && ((CursorMoveEvent) tmp).axis == head.axis) {
                anotherMove = (CursorMoveEvent) cursorEventQ.take();
              } else {
                break;
              }
            } catch (InterruptedException ex) {
              ; // do nothing
            }
          }
          count += anotherMove.count;
        } while (true);
        if (count != head.count) {
          head = new CursorMoveEvent(head.axis, count, head.ctrlKey, head.shiftKey);
        }
        not_edt_processCursorMoveEvent(head);
      }
    } catch (InterruptedException ex) {
      ; // do nothing
    }
  }

  private void not_edt_processCursorSetEvent(CursorSetEvent event) { // NOT awt thread
    // do ipc (calls below should not access globals)
    final TimelineSelectionEvent selEvent;
    final long eventIdx;
    if (event.directHit) {
      eventIdx = event.eventIdx;
    } else {
      eventIdx = TimelineSelectionEvent.EVT_IDX_INVALID;
    }
    selEvent =
        ipcGetSelEvent(
            event.rowGeometry,
            event.clickTime,
            event.ctrlKey,
            event.shiftKey,
            TimelineSelectionEvent.CursorAxis.NA,
            eventIdx,
            0,
            event.requestRefreshCaliper,
            event.updateRowSelections);
    if (selEvent == null) {
      return;
    }

    // update swing components
    try {
      SwingUtilities.invokeAndWait(
          new Runnable() {
            public void run() {
              edt_processTimelineSelectionEvent(selEvent);
            }
          });
    } catch (Exception e) {
      ; // weird?  // e.printStackTrace();
    }
  }

  private void not_edt_processCursorMoveEvent(final CursorMoveEvent event) { // NOT awt thread
    final int n = event.count;
    if (n == 0) {
      return;
    }

    // compute new state on EDT
    class AWTStateCapture {
      public RowGeometry newRow;
      public long clickTime;
      public long oldEventIdx;
      public long moveCount;
      public boolean requestCaliperUpdate;

      public AWTStateCapture() {
        this.newRow = null;
        this.clickTime = TimelineDrawer.TIME_INVALID;
        this.oldEventIdx = TimelineSelectionEvent.EVT_IDX_INVALID;
        this.moveCount = 0;
        this.requestCaliperUpdate = false;
      }
    }
    ;
    final AWTStateCapture saved = new AWTStateCapture();
    try {
      SwingUtilities.invokeAndWait(
          new Runnable() {
            public void run() {
              // Calculate the next location on EDT
              // Set newRow=null if next location can't be calculated.
              TimelineSelectionEvent prevEvent = event_drawer.getSelection();
              if (prevEvent == null
                  || prevEvent.getRowGeometry() == null
                  || prevEvent.rowNum == -1) {
                return;
              }

              if (event.axis == TimelineSelectionEvent.CursorAxis.LEFT_RIGHT) {
                if (prevEvent.eventIdx == TimelineSelectionEvent.EVT_IDX_INVALID) {
                  return;
                }
                saved.newRow = prevEvent.getRowGeometry(); // will not be null, see above
                saved.clickTime = TimelineDrawer.TIME_INVALID; // will be determined later
                saved.oldEventIdx = prevEvent.eventIdx;
                saved.moveCount = n;
                saved.requestCaliperUpdate = true;
                return;
              }

              if (event.axis == TimelineSelectionEvent.CursorAxis.UP_DOWN) {
                final int n_abs = Math.abs(n);
                RowGeometry newRow = event_drawer.rowNum2RowGeometry(prevEvent.rowNum);
                if (newRow == null) {
                  return;
                }
                for (int ii = 0; ii < n_abs; ii++) {
                  final int nn;
                  if (n < 0) {
                    nn = newRow.rowNum - newRow.subrowNum - 1;
                  } else {
                    nn = newRow.rowNum - newRow.subrowNum + newRow.nSubrows;
                  }
                  RowGeometry nextRow = event_drawer.rowNum2RowGeometry(nn);
                  if (nextRow == null) {
                    break;
                  }
                  newRow = nextRow;
                }
                newRow = newRow.getMasterRow();
                saved.newRow = newRow;
                saved.clickTime = prevEvent.clickTime;
                saved.oldEventIdx = TimelineSelectionEvent.EVT_IDX_INVALID;
                saved.moveCount = 0; // not used
                saved.requestCaliperUpdate = false;
                return;
              }
            }
          });
    } catch (Exception e) {
      ; // weird?  // e.printStackTrace();
    }

    // do ipc (calls below should not access globals)
    final TimelineSelectionEvent selEvent;
    if (saved.newRow == null) {
      return;
    }
    selEvent =
        ipcGetSelEvent(
            saved.newRow,
            saved.clickTime,
            event.ctrlKey,
            event.shiftKey,
            event.axis,
            saved.oldEventIdx,
            saved.moveCount,
            saved.requestCaliperUpdate,
            true);
    if (selEvent == null) {
      return;
    }

    // update swing components
    try {
      SwingUtilities.invokeAndWait(
          new Runnable() {
            public void run() {
              edt_processTimelineSelectionEvent(selEvent);
            }
          });
    } catch (Exception e) {
      ; // weird?  // e.printStackTrace();
    }
  }

  private void cursorEventQueueClear() {
    synchronized (cursorEventQLock) { // synchronized to simplify peek
      cursorEventQ.clear();
    }
  }

  private void cursorEventQueueAdd(CursorEvent event) {
    cursorEventQ.add(event);
  }

  private void cursorEventQueueSet(CursorEvent event) {
    //        edt_processTimelineSelectionEvent(null);// clears queue, resets RHS
    cursorEventQueueClear();
    cursorEventQueueAdd(event);
  }

  public void selectFirst() {
    int rowNum = dataAxisReader.getRowStart();
    RowGeometry rowGeometry = event_drawer.rowNum2RowGeometry(rowNum);
    long time = timeAxisReader.getTimeStart();
    cursorEventQueueSet(new CursorSetEvent(rowGeometry, time, false, false, true, true));
  }

  @Override
  public void selectLeft() {
    selectLeft(false, false);
  }

  public void selectRight() {
    selectRight(false, false);
  }

  public void selectUp() {
    selectUp(false, false);
  }

  public void selectDown() {
    selectDown(false, false);
  }

  public void selectLeft(boolean ctrl, boolean shift) {
    cursorEventQueueAdd(
        new CursorMoveEvent(TimelineSelectionEvent.CursorAxis.LEFT_RIGHT, -1, ctrl, shift));
  }

  public void selectRight(boolean ctrl, boolean shift) {
    cursorEventQueueAdd(
        new CursorMoveEvent(TimelineSelectionEvent.CursorAxis.LEFT_RIGHT, 1, ctrl, shift));
  }

  public void selectUp(boolean ctrl, boolean shift) {
    cursorEventQueueAdd(
        new CursorMoveEvent(TimelineSelectionEvent.CursorAxis.UP_DOWN, -1, ctrl, shift));
  }

  public void selectDown(boolean ctrl, boolean shift) {
    cursorEventQueueAdd(
        new CursorMoveEvent(TimelineSelectionEvent.CursorAxis.UP_DOWN, 1, ctrl, shift));
  }

  public TimelineCaliper selectNearXY(int x, int y, boolean ctrl, boolean shift) {
    //        System.out.println(event_drawer.dumpXY(event_drawer.findNearXY(x,y,false))); // DUMP
    //        System.out.println(event_drawer.exportAsText(0));
    // Returns time dimensions of event only if mouse directly over event.
    // Additionally, queues the selection event.
    TimelineDraw.FindNearXYResult result = event_drawer.findNearXY(x, y, true);
    if (result == null) {
      return null;
    }
    final CursorSetEvent event;
    if (result.event != null) {
      event =
          new CursorSetEvent(
              result.rowGeometry,
              result.clickTime,
              ctrl,
              shift,
              result.event.eventIdx,
              result.event.timeStart,
              result.event.timeEnd,
              result.directTimeHit,
              false,
              true);
    } else {
      event = new CursorSetEvent(result.rowGeometry, result.clickTime, ctrl, shift, false, true);
    }

    cursorEventQueueSet(event);
    if (!result.directTimeHit) {
      return null;
    }
    return new TimelineCaliper(result.event.timeStart, result.event.timeEnd);
  }

  public void notifyCaliperChange(TimelineCaliper caliper) {
    event_drawer.setCaliper(caliper);
    genericNotifyTimelineSelectionListeners(
        // Refactor?  It may be bit odd doing this here
        // as the owner of calipers is TimelinePanel, not this class.
        new TimelineSelectionCaliperEvent(caliper));
    timelinePanel.repaint();
  }

  public void notifyZoomHistoryChange(TimelineSelectionHistoryEvent e) {
    genericNotifyTimelineSelectionListeners(e);
  }

  public void addSelectionListener(TimelineSelectionListener listener) { // AWT thread only
    selection_listeners.add(listener);
  }

  public boolean edt_fetchData(final boolean forceReload) { // AWT thread only!
    TL2DataSnapshot drawerSnapshot = event_drawer.getDataSnapshot();
    // Note: event_drawer.getDataSnapshot() is only updated on the EDT; we know it won't change
    // while in this method
    if (drawerSnapshot != tl2DataFetcher.getTL2DataSnapshot()) {
      return false; // shortcut the process, the event_drawer is already obsolete
    }

    boolean refetchNeeded = false;

    if (forceReload) {
      // external entity (e.g. filter) is forcing refetch
      refetchNeeded = true;
    }

    // X: align according to bin size
    long tstart = timeAxisReader.getTimeStartAligned();
    long binTime = timeAxisReader.getTimePerBin();
    int binCount = timeAxisReader.getNumBins();

    // Y
    int pstart = dataAxisReader.getRowStart();
    int pend = dataAxisReader.getRowEnd();

    //        if(!initialImageReady){
    //        if(!initialImageReady){//TBR
    //            // no image yet, we need to refetch
    //            refetchNeeded = true;
    //        }else{
    {
      // check old data to see if we really need to refetch

      if (tstart != data_time_start || binTime != data_bin_time || binCount != data_time_nbins) {
        refetchNeeded = true;
      }

      if (pstart < data_row_start || pend > data_row_end) {
        refetchNeeded = true;
      }
    }

    if (!refetchNeeded && !forceReload) {
      return false; // not updated
    }

    final boolean loadingProcessed;
    if (event_drawer.getDataSnapshot() == null) {
      // no data to fetch, but still need to update the screen
      loadingProcessed = true;
    } else {
      RowGeometry rstart = event_drawer.rowNum2RowGeometry(pstart);
      RowGeometry rend = event_drawer.rowNum2RowGeometry(pend);
      if (null == rstart || null == rend) {
        loadingProcessed = false; // happens when null snapshot is pushed to clear screen
      } else {
        RowGeometry wide_rstart = event_drawer.rowNum2RowGeometry(0);
        RowGeometry wide_rend = event_drawer.rowNum2RowGeometry(dataAxisReader.getAbsRowEnd());
        long overshoot = timeAxisReader.getTimeDuration(); // how much extra to buffer
        long estStart = timeAxisReader.getTimeStart() - overshoot;
        long wide_tstart = timeAxisReader.getTimeStartAligned(estStart);
        long estEnd = timeAxisReader.getTimeEnd() + overshoot;
        if (estEnd > event_drawer.getAbsoluteTimeEnd()) {
          estEnd = event_drawer.getAbsoluteTimeEnd();
        }
        long duration = estEnd - wide_tstart;
        int wide_binCount = (int) (duration / binTime) + 1;
        RowDataRequestParams newReq =
            new RowDataRequestParams(
                drawerSnapshot,
                forceReload,
                binTime,
                wide_tstart,
                wide_binCount, // grab more width than requested
                wide_rstart.rowData.idx,
                wide_rend.rowData.idx, // ask for all rows
                timeAxisReader.getTimeStart(),
                timeAxisReader.getTimeEnd(),
                rstart.rowData.idx,
                rend.rowData.idx);
        loadingProcessed = tl2DataFetcher.edt_fetchTLRowDataInitiate(newReq);
      }
    }
    if (!loadingProcessed) {
      return false; // not updated (e.g. snapshot changed underneath us)
    }

    //        initialImageReady = true;
    data_time_start = tstart;
    data_bin_time = binTime;
    data_time_nbins = binCount;
    data_row_start = pstart;
    data_row_end = pend;

    //        if(forceReload){
    //            // for example, filter set
    //            recenterVerticallyOnCursor();
    //            remapSelections();
    //        }

    return true; // updated
  }

  public boolean drawData(Graphics g) { // AWT thread only!
    final boolean painted;
    //          if(initialImageReady){
    if (event_drawer.getDataSnapshot() == tl2DataFetcher.getTL2DataSnapshot()) {
      event_drawer.drawIt(g);
      painted = true;
    } else {
      painted = false;
    }
    return painted;
  }

  private long ipcGetTLEventIdxNearTime(RowDefinition rowDef, long ts) {
    if (rowDef == null) {
      return TimelineSelectionEvent.EVT_IDX_INVALID;
    }
    int searchDirection = 0; // search forward and backwards for nearest event
    {
      Settings.TLData_type tlData_type = rowDef.getDataDescriptor().getTLDataType();
      if (tlData_type.equals(Settings.TLData_type.TL_HEAPSZ)) {
        searchDirection = -1; // only search backwards in time
      }
    }
    long adjust = rowDef.getTimeOrigin();
    long unadjusted_ts = ts + adjust;
    long eventIdx =
        tl2DataFetcher.ipcGetTLEventIdxNearTime(
            rowDef.getExpID(),
            rowDef.getDataDescriptor().getDataId(),
            rowDef.getEntity().getPropId(),
            rowDef.getEntity().getPropValue(),
            rowDef.getAux(),
            searchDirection,
            unadjusted_ts);
    return eventIdx; // SELECTION_IDX_INVALID if row has no events
  }

  private long[] ipcGetTLEventCenterTime(RowDefinition rowDef, long oldEventIdx, long moveCount) {
    if (rowDef == null) {
      return null;
    }
    IPCResult ipcResult = ipcStart_getTLEventCenterTime(rowDef, oldEventIdx, moveCount);
    return ipcFinish_getTLEventCenterTime(ipcResult, rowDef);
  }

  private IPCResult ipcStart_getTLEventCenterTime(
      RowDefinition rowDef, long oldEventIdx, long moveCount) {
    return tl2DataFetcher.ipcStart_getTLEventCenterTime(
        null,
        null,
        rowDef.getExpID(),
        rowDef.getDataDescriptor().getDataId(),
        rowDef.getEntity().getPropId(),
        rowDef.getEntity().getPropValue(),
        rowDef.getAux(),
        oldEventIdx,
        moveCount);
  }

  private long[] ipcFinish_getTLEventCenterTime(IPCResult ipcResult, RowDefinition rowDef) {
    if (ipcResult == null) {
      return null;
    }
    long[] idx_and_time = (long[]) ipcResult.getObject();
    long ts = TimelineDrawer.TIME_INVALID;
    if (idx_and_time != null) {
      long unadjusted_ts = idx_and_time[1];
      long adjust = rowDef.getTimeOrigin();
      ts = unadjusted_ts - adjust; // should not be -1
      if (ts < 0) {
        return null; // weird
      }
      idx_and_time[1] = ts;
    }
    return idx_and_time; // null if no such event
  }
}
