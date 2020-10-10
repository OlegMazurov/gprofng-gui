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

package org.gprofng.mpmt.timeline_common;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.timeline2.TimelineDraw;
import org.gprofng.mpmt.timeline2.TimelineSelectionEvent;
import org.gprofng.mpmt.util.gui.AnLongScrollBar;
import org.gprofng.mpmt.util.ruler.RangeRuler;
import org.gprofng.mpmt.util.ruler.valuetypes.ValuesNanoseconds;
import org.gprofng.mpmt.util.zoomruler.OverlayMouseHandler;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerEvent;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerListener;
import org.gprofng.mpmt.util.zoomruler.ZoomRulerOverlay;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class provides a JComponent to display a timeline. The timeline shows several processes or
 * threads of execution simultaneously. Each will be represented by a horizontal bar with the color
 * of the bar indicating the state at each moment in time. The raw data is provided by a
 * TimelineProvider object with display options specified by a TimlineOptions objects.
 *
 * <p>The timeline provides panning and zooming capabilities. In addition to the state information,
 * optional message lines can be displayed to show communication between processes.
 *
 * <p>To minimize refetches of data from DBE, this class may draw a subset of the data that has been
 * fetched from DBE and stored in the TimelineProcessStates array.
 *
 * <p>With the current implementation, the time interval displayed is exactly what has been fetched
 * from dbe. In the process direction, however, zoom is allowed to show a subset of the data.
 */
public class TimelinePanel extends JPanel {

  // image data
  private TimelineDrawer tldrawer;
  private CoordCalcTimeMaster timeAxisCalculator;
  private CoordCalcDataMaster dataAxisCalculator;

  // listeners
  private Vector<ChangeListener> change_listeners;
  private boolean forceDataReload;

  // options
  private final boolean enable_vertical_zoom;
  private final boolean enable_zoom_overlays;
  private final boolean enable_vertical_row_snap;
  private final boolean enable_time_caliper;

  // zoom and pan history
  private class ZoomHistoryEvent {
    public final long timeStart;
    public final long timeEnd;
    public final double ypctStart; // used by MPI timeline
    public final double ypctEnd; // used by MPI timeline
    public final long timeFocus;
    public final double timeFocusScreenPercent;
    public final double ypctFocus;
    public final double ypctFocusScreenPercent;
    public final int vZoomLevel; // used by regular timeline

    public ZoomHistoryEvent(
        long timeStart,
        long timeEnd,
        double ypctStart,
        double ypctEnd,
        long timeFocus,
        double timeFocusScreenPercent,
        double ypctFocus,
        double ypctFocusScreenPercent,
        int vZoomLevel) {
      this.timeStart = timeStart;
      this.timeEnd = timeEnd;
      this.ypctStart = ypctStart;
      this.ypctEnd = ypctEnd;
      this.timeFocus = timeFocus;
      this.timeFocusScreenPercent = timeFocusScreenPercent;
      this.ypctFocus = ypctFocus;
      this.ypctFocusScreenPercent = ypctFocusScreenPercent;
      this.vZoomLevel = vZoomLevel;
    }

    public boolean equals(ZoomHistoryEvent e) {
      if (e.timeStart != this.timeStart
          || e.timeEnd != this.timeEnd
          || e.vZoomLevel != this.vZoomLevel) {
        return false;
      }
      double epsilon = 0.0001;
      if (Math.abs(e.ypctStart - this.ypctStart) > epsilon) {
        return false;
      }
      if (Math.abs(e.ypctEnd - this.ypctEnd) > epsilon) {
        return false;
      }
      return true;
    }

    public String toString() {
      ZoomHistoryEvent e = this;
      String s =
          String.format(
              "[t=(%.1f, %.1f), v=(%.0f, %.0f) z=%d]",
              e.timeStart / 10000000f,
              e.timeEnd / 10000000f,
              e.ypctStart * 100,
              e.ypctEnd * 100,
              e.vZoomLevel);
      return s;
    }
  }

  private List<ZoomHistoryEvent> zoomHistory = new ArrayList();
  private int zoomHistoryIdx = 0; // position of next add, can't undo from here
  private long visible_time_start = 0; // first time visible on screen
  private long visible_time_end = 0; // last  time visible on screen
  private double visible_ypct_start = 0; // y axis visible start (% of total)
  private double visible_ypct_end = 0; // y axis visible end   (% of total)
  private boolean zoom_ypct_at_max; // true when at max vertical zoom out
  private ZoomHistoryEvent referenceEvent;

  // zoom center
  private long zoom_time_center; // should be renamed to time_focus
  private double zoom_time_center_screen_percent; // should be renamed to time_focus_screen_percent
  private double ypct_focus; // y axis zoom center (% of total)
  private double ypct_focus_screen_percent; // screen center of time zoom

  // calipers
  private TimelineCaliper timeCaliper;

  /* scrolling state */
  private boolean set_vscroll = false;
  private boolean set_hscroll = false;
  private boolean hscroll_adjusting = false;
  private boolean yscroll_adjusting = false;
  ZoomHistoryEvent scroll_adjusting_snapshot;

  /* variables for rubberband zoom rectangle */
  private static int RUBBERBAND_WIDTH = 2;
  private boolean rubberbandIs2D = false;

  /* mouse settings */
  private final int MOUSE_PRESS_DRAG_DELAY = 400; // ms before enabled
  private final int AUTO_PAN_REPEAT_TIME = 100; // ms between updates
  private final int AUTO_PAN_REGION_PIXELS = 4; // margin where activated
  private final double AUTO_PAN_RELOCATION_PERCENT = 0.25; // new location percent of screen
  private final int MOUSE_DRAG_MIN_PIXELS = 2; // pixels before enabled
  private final int CALIPER_CLICK_PIXEL_TOLERANCE =
      TimelineDraw.CLICK_PIXEL_TOLERANCE + 0; // yuck, crosses interface

  public static enum MouseDragMode {
    MOUSE_RUBBERBANDING_MODE, // zoom
    MOUSE_GRABBING_MODE, // pan
    MOUSE_SELECTION_MODE // set/expand selection
  };

  private static enum MouseSubmode {
    MM_UNKNOWN,
    MM_HOVER_CALIPER,
    MM_HOVER_SELECTION,
    MM_HOVER_GRAB,
    MM_HOVER_RUBBERBAND,
    MM_DRAG_CALIPER, // drag-to-adjust caliper
    MM_DRAG_SELECTION, // drag-to-adjust selection region
    MM_DRAG_GRAB, // drag-to-pan
    MM_DRAG_RUBBERBAND, // drag to zoom
    MM_OVERLAY // nop, pass to overlay
  };

  private MouseDragMode mouse_drag_mode = MouseDragMode.MOUSE_GRABBING_MODE;
  private boolean global_mouse_drag_mode_changed = true;
  private final int SMALL_PAN_STEP_FRACTION = 40; // 1/40th of screen step
  private final int LARGE_PAN_STEP_FRACTION = 5; // 1/5th of screen step
  private static final boolean MOUSE_WHEEL_ZOOMS = false; // true=>hzoom, false=>vscroll

  // components
  private JPanel southPanel;
  private AnLongScrollBar horScroll;
  private JScrollBar verScroll;
  private static final int VERSCROLL_TO_PERCENT = 100000;
  private static final double PERCENT_TOLERANCE = 0.000001; // YXXX need to restructure
  private static final double HUNDRED_PERCENT_ROUNDUP = 1.0 - PERCENT_TOLERANCE;
  private static final double ROW_SNAP_TOLERANCE = 0.01;
  private JPanel southEastSpacer;
  private JPanel southWestSpacer;
  private TimelineCanvas timelineCanvas;
  private TimelineCursor handcursors;
  private RangeRuler globalRuler;
  private RangeRuler localRuler;
  private VerticalRowRuler verticalRuler;
  private final MouseDragMode mainPanelDefaultMouseMode;
  private final MouseDragMode hRulerDefaultMouseMode;
  private final MouseDragMode vRulerDefaultMouseMode;

  // zoom overlays
  private ZoomRulerOverlay timeOverlay = null;
  private ZoomRulerOverlay processOverlay = null;
  private OverlayMouseHandler defaultOverlayMouseHandler =
      new OverlayMouseHandler() {
        @Override
        public boolean inOverlay(int x, int y) {
          return false;
        }

        @Override
        public void deactivate() {}

        @Override
        public void activate() {}

        @Override
        public Cursor getCursor() {
          return null;
        }
      };
  private OverlayMouseHandler overlayMouseHandler = defaultOverlayMouseHandler;

  /** Creates a new instance of TimelinePanel. Call this from AWT thread only */
  public TimelinePanel(
      final TimelineDrawer tldrawer,
      final MouseDragMode mainPanelMode,
      final MouseDragMode hRulerMode,
      final MouseDragMode vRulerMode) {
    super();

    this.tldrawer = tldrawer;
    mainPanelDefaultMouseMode = mainPanelMode;
    hRulerDefaultMouseMode = hRulerMode;
    vRulerDefaultMouseMode = vRulerMode;

    this.enable_vertical_zoom = tldrawer.getEnableZoomVertical();
    this.enable_zoom_overlays = tldrawer.getEnableZoomOverlays();
    this.enable_vertical_row_snap = !tldrawer.getEnableUnalignedRows();
    this.enable_time_caliper = tldrawer.getEnableTimeCaliper();

    // sync with calculators
    initAxisCalculators();

    change_listeners = new Vector();
    handcursors = new TimelineCursor();

    timeCaliper = new TimelineCaliper(0, 0);

    initializeComponents();
    //        zoomHistoryAdd();
  }

  private void initAxisCalculators() {
    this.timeAxisCalculator = tldrawer.getTimeAxisMaster();
    long zoom_time_start_prev = tldrawer.getAbsoluteTimeStart();
    long zoom_time_end_prev = tldrawer.getAbsoluteTimeEnd();
    zoom_time_center = (zoom_time_start_prev + zoom_time_end_prev + 1) / 2;
    zoom_time_center_screen_percent = 0.5; // zoom focus at middle
    timeAxisCalculator.setTimeRange(zoom_time_start_prev, zoom_time_end_prev);
    timeAxisCalculator.edt_revalidate();

    this.dataAxisCalculator = tldrawer.getDataAxisMaster();
    double zoom_ypct_start_prev = 0.0; /* display from 0% */
    double zoom_ypct_end_prev = 1.0; /* display to 100% */
    ypct_focus = 0.0; /* center at 0% */
    ypct_focus_screen_percent = 0.0; // focus at top
    zoom_ypct_at_max = true;
    dataAxisCalculator.setVisibleRange( // order matters, do this last
        zoom_ypct_start_prev, zoom_ypct_end_prev);
    dataAxisCalculator.edt_revalidate();
  }

  public void edt_resetAll() { // edt only
    initAxisCalculators(); // Depends on TL2DataSnapshot being reset first
    zoomHistoryReset();
  }

  public void edt_resetExperiments() { // edt only
    int ii = 0;
  }

  public void edt_resetTLRanges() {
    zoomYToPercent(1.0);

    zoomTimeToDuration(tldrawer.getAbsoluteTimeDuration());
    ZoomHistoryEvent latest = zoomHistoryCreateEvent();
    if (referenceEvent != null && !referenceEvent.equals(latest)) {
      zoomHistoryReset();
    }
    referenceEvent = latest;
  }

  // ----- stuff used by right-hand-tab filter selector
  public long getFilterTimeStart() {
    return timeAxisCalculator.getTimeStart();
  }

  public long getFilterTimeEnd() {
    return timeAxisCalculator.getTimeEnd();
  }

  /** Returns the center time at the current pan offset */
  public int getFilterProcStart() {
    int p;
    if (zoom_ypct_at_max) {
      p = 0;
    } else {
      p = dataAxisCalculator.getRowStart();
    }
    return p;
  }

  public int getFilterProcEnd() {
    int p;
    if (zoom_ypct_at_max) {
      p = tldrawer.getAbsoluteRowEnd();
    } else {
      p = dataAxisCalculator.getRowEnd();
    }
    return p;
  }

  /** Force a data refresh and a display update (used by unfilter) */
  public void edt_refetchAndRepaint() { // AWT thread only
    forceDataReload = true; // AWT thread only
    repaint();
  }

  public void edt_revalidateAll(boolean _forceDataReload) { // AWT thread only
    if (_forceDataReload) {
      forceDataReload = true;
    }
    //        zoomHistoryAdd();
    updateY(true);
    updateX(true);
  }

  // ---- listeners for context menu

  /** Adds a MouseListener to the timeline display */
  public void addMouseListener(MouseListener l) {
    timelineCanvas.addMouseListener(l);
    verticalRuler.addMouseListener(l);
    globalRuler.addMouseListener(l);
    localRuler.addMouseListener(l);
  }

  /** Adds a MouseMotionListener to the timeline display */
  public void addMouseMotionListener(MouseMotionListener l) {
    timelineCanvas.addMouseMotionListener(l);
  }

  // ----- Zoom to endpoints

  /** sets zoom endpoints and center.. e.g. rubberband */
  private void zoomTimeToRange(long t1, long t2, boolean isPan) {
    long newstart = t1;
    long newend = t2;

    // swap if needed
    if (newstart > newend) {
      long tmp = newstart;
      newstart = newend;
      newend = tmp;
    }
    long delta = newend - newstart;

    long absEnd = tldrawer.getAbsoluteTimeEnd();
    long absStart = tldrawer.getAbsoluteTimeStart();
    if (newend > absEnd) {
      newend = absEnd;
      if (isPan) {
        newstart = newend - delta;
      }
    }
    if (newstart < absStart) {
      newstart = absStart;
      if (isPan) {
        newend = newstart + delta;
      }
      if (newend > absEnd) {
        newend = absEnd;
      }
    }

    zoom_time_center = (newstart + newend + 1) / 2;
    zoom_time_center_screen_percent = 0.5;
    timeAxisCalculator.setTimeRange(newstart, newend);
    updateX(false);
  }

  /** sets zoom endpoints and center. e.g. rubberband */
  private void zoomProcessToRange(int start_p, int end_p, boolean isPan) {
    int newstart = start_p;
    int newend = end_p;

    // swap if needed
    if (newstart > newend) { // swap if needed
      int tmp = newstart;
      newstart = newend;
      newend = tmp;
    }
    int delta = newend - newstart;

    if (!isPan) { // for zooms, update zoom_proc_at_max
      int range = delta + 1;
      zoom_ypct_at_max = (range >= tldrawer.getAbsoluteRowCount());
    }

    if (newend > tldrawer.getAbsoluteRowEnd()) {
      newend = tldrawer.getAbsoluteRowEnd();
      if (isPan) {
        newstart = newend - delta;
      }
    }
    if (newstart < 0) {
      newstart = 0;
      if (isPan) {
        newend = newstart + delta;
      }
      if (newend > tldrawer.getAbsoluteRowEnd()) {
        newend = tldrawer.getAbsoluteRowEnd();
      }
    }

    ypct_focus = (newstart + newend + 1) / 2;
    ypct_focus_screen_percent = 0.5;
    dataAxisCalculator.setRowRange(newstart, newend);
    updateY(false);
  }

  /** sets zoom endpoints and center. e.g. rubberband */
  private void zoomYToRange(double start_pct, double end_pct, boolean isPan) {
    double newstart = start_pct;
    double newend = end_pct;

    // swap if needed
    if (newstart > newend) {
      double tmp = newstart;
      newstart = newend;
      newend = tmp;
    }
    double delta = newend - newstart;

    if (!isPan) { // for zooms, update zoom_proc_at_max
      zoom_ypct_at_max = (delta >= HUNDRED_PERCENT_ROUNDUP);
    }

    if (enable_vertical_row_snap) {
      int start_p, end_p;
      if (isPan) {
        int num_p = dataAxisCalculator.getRowCount();
        double slop = 1.0 / (dataAxisCalculator.getAbsRowEnd() + 1) * ROW_SNAP_TOLERANCE;
        double start_pct_rounded = newstart + slop;
        start_p = dataAxisCalculator.getRowNearPercent(start_pct_rounded);
        end_p = start_p + num_p - 1;
      } else {
        start_p = dataAxisCalculator.getRowNearPercent(newstart);
        end_p = dataAxisCalculator.getRowNearPercent(newend);
      }
      zoomProcessToRange(start_p, end_p, isPan);
      return;
    }

    // contrain the zoom level
    if (!isPan) {
      double min_percent = 1.0 / (dataAxisCalculator.getAbsRowEnd() + 1);
      if (delta < min_percent) {
        delta = min_percent;
        double center = (newstart + newend) / 2;
        newstart = center - delta / 2;
        newend = center + delta / 2;
      }
    }

    if (newend > HUNDRED_PERCENT_ROUNDUP) {
      newend = 1.0;
      if (isPan) {
        newstart = newend - delta;
      }
    }
    if (newstart < 0.0) {
      newstart = 0.0;
      if (isPan) {
        newend = newstart + delta;
      }
      if (newend > HUNDRED_PERCENT_ROUNDUP) {
        newend = 1.0;
      }
    }

    ypct_focus = (newstart + newend) / 2;
    ypct_focus_screen_percent = 0.5;
    dataAxisCalculator.setVisibleRange(newstart, newend);
    updateY(false);
  }

  // --- zoom to a fixed scale using existing zoom center

  /** zoom to a fixed scale using existing zoom center, e.g. zoom slider */
  private void zoomTimeToDuration(long duration) {
    if (duration < 1) {
      duration = 1;
    } else if (duration > tldrawer.getAbsoluteTimeDuration()) {
      duration = tldrawer.getAbsoluteTimeDuration();
    }
    long newstart = zoom_time_center - (long) (zoom_time_center_screen_percent * duration);
    if (newstart < tldrawer.getAbsoluteTimeStart()) newstart = tldrawer.getAbsoluteTimeStart();
    long newend = newstart + (duration - 1);
    if (newend > tldrawer.getAbsoluteTimeEnd()) {
      newend = tldrawer.getAbsoluteTimeEnd();
      newstart = newend - (duration - 1);
    }

    timeAxisCalculator.setTimeRange(newstart, newend);
    updateX(false);
  }

  /** zoom to a fixed scale using existing zoom center, e.g. zoom slider */
  private void zoomProcessToCount(int procCount) {
    zoom_ypct_at_max = (procCount >= tldrawer.getAbsoluteRowCount());

    if (procCount < 1) {
      procCount = 1;
    } else if (procCount > tldrawer.getAbsoluteRowCount()) {
      procCount = tldrawer.getAbsoluteRowCount();
    }
    int zoom_proc_center = dataAxisCalculator.getRowNearPercent(ypct_focus);
    int newstart =
        zoom_proc_center
            - procCount / 2; // YXXX probably should be something like below, but needs testing
    //                - (long)(ypct_focus_screen_percent * procCount);
    if (newstart < 0) newstart = 0;
    int newend = newstart + (procCount - 1);
    if (newend > tldrawer.getAbsoluteRowEnd()) {
      newend = tldrawer.getAbsoluteRowEnd();
      newstart = newend - (procCount - 1);
    }

    dataAxisCalculator.setRowRange(newstart, newend);
    updateY(false);
  }

  /** zoom to a fixed scale using existing zoom center, e.g. zoom slider */
  private void zoomYToPercent(double percent) {
    zoom_ypct_at_max = (percent >= HUNDRED_PERCENT_ROUNDUP);

    if (enable_vertical_row_snap) {
      int procCount = (int) (dataAxisCalculator.getAbsRowCount() * (percent + PERCENT_TOLERANCE));
      if (procCount < 1) {
        procCount = 1;
      }
      zoomProcessToCount(procCount);
      return;
    }

    // contrain the zoom level
    double min_percent = 1.0 / (dataAxisCalculator.getAbsRowEnd() + 1);
    if (percent < min_percent) {
      percent = min_percent;
    } else if (percent >= HUNDRED_PERCENT_ROUNDUP) {
      percent = 1.0;
    }

    double newstart = ypct_focus - ypct_focus_screen_percent * percent;
    if (newstart < 0.0) {
      newstart = 0.0;
    }
    double newend = newstart + percent;
    if (newend >= HUNDRED_PERCENT_ROUNDUP) {
      newend = 1.0;
      newstart = newend - percent;
    }

    dataAxisCalculator.setVisibleRange(newstart, newend);
    updateY(false);
  }

  // --- zoom by step

  /** Zoom in by a factor of 2 */
  public void zoomTimeIn() {
    zoomHistoryAdd();
    long duration = timeAxisCalculator.getTimeDuration() / 2;
    zoomTimeToDuration(duration);
  }
  /** Zoom out by a factor of 2 */
  public void zoomTimeOut() {
    zoomHistoryAdd();
    long duration = timeAxisCalculator.getTimeDuration() * 2;
    zoomTimeToDuration(duration);
  }

  /** Zoom in by a factor of 2 */
  public void zoomProcessIn() {
    zoomHistoryAdd();
    double range = dataAxisCalculator.getVisiblePercent() / 2;
    zoomYToPercent(range);
  }
  /** Zoom out by a factor of 2 */
  public void zoomProcessOut() {
    zoomHistoryAdd();
    double range = dataAxisCalculator.getVisiblePercent() * 2;
    zoomYToPercent(range);
  }

  // -----Compute Zoom levels used by Zoom slider widgets

  public int getZoomTimeLevelMax() {
    return getZoomLevel(tldrawer.getAbsoluteTimeDuration(), 0);
  }

  public int getZoomProcessLevelMax() {
    return getZoomLevel(tldrawer.getAbsoluteRowCount(), 0);
  }

  public int getZoomTimeLevel() {
    long absolute = tldrawer.getAbsoluteTimeDuration();
    long current = timeAxisCalculator.getTimeDuration();
    int level = getZoomLevel(absolute, current);
    return level;
  }

  public int getZoomProcessLevel() {
    long totalnp = tldrawer.getAbsoluteRowCount();
    long current;
    if (zoom_ypct_at_max) {
      current = totalnp;
    } else {
      current = (long) (totalnp * dataAxisCalculator.getVisiblePercent());
    }
    int level = getZoomLevel(totalnp, current);
    return level;
  }

  /** Compute the nearest level for a segment within max */
  private int getZoomLevel(long max, long segment) {
    int i = 0;
    long lt = max;
    while (lt > segment) {
      lt >>= 1;
      i++;
    }
    return i;
  }

  private long getZoomMultiplier(int lev) {
    long mult = 1L << lev;
    return mult;
  }

  // --- zoom to level

  public void zoomTimeToLevel(int lev) {
    zoomHistoryAdd();
    long multiplier = getZoomMultiplier(lev);
    long abs_count = tldrawer.getAbsoluteTimeDuration();
    long desired_count = abs_count / multiplier;
    zoomTimeToDuration(desired_count);
  }

  public void zoomProcessToLevel(int lev) {
    zoomHistoryAdd();
    long multiplier = getZoomMultiplier(lev);
    double percent = 1.0 / multiplier;
    zoomYToPercent(percent);
  }

  // --- reset and revert zoom

  /** Reset zoom to show entire time and process range */
  public void resetZoom() {
    zoomHistoryAdd();
    zoomTimeToDuration(tldrawer.getAbsoluteTimeDuration());
    zoomYToPercent(1.0);
    // YXXX does double paint
  }

  public void zoomCenterSelection() {
    if (tldrawer.selectionActive()) {
      zoomHistoryAdd();
      tldrawer.zoomCenterSelection();
      ypct_focus = tldrawer.getSelectionYCenter();
      ypct_focus_screen_percent = 0.5;
      zoom_time_center = tldrawer.getSelectionTimeCenter();
      zoom_time_center_screen_percent = 0.5;
      zoomTimeToDuration(timeAxisCalculator.getTimeDuration());
      zoomYToPercent(dataAxisCalculator.getVisiblePercent());
    }
  }

  private double calcTimeScreenPercent(long tstamp) {
    final double deltat = tstamp - timeAxisCalculator.getTimeStart();
    final long duration = timeAxisCalculator.getTimeDuration();
    if (duration < 1) {
      return 0; // weird
    }
    double screenPercent = deltat / duration;
    if (screenPercent > 1.0) {
      screenPercent = 1.0; // weird
    } else if (screenPercent < 0.0) {
      screenPercent = 0.0; // weird
    }
    return screenPercent;
  }

  private double calcYpctScreenPercent(double ypct) {
    final double deltat = ypct - dataAxisCalculator.getVisibleStart();
    final double visible = dataAxisCalculator.getVisiblePercent();
    if (visible == 0) {
      return 0; // weird
    }
    double screenPercent = deltat / visible;
    if (screenPercent > 1.0) {
      screenPercent = 1.0; // weird
    } else if (screenPercent < 0.0) {
      screenPercent = 0.0; // weird
    }
    return screenPercent;
  }

  // --- zoomHistory internal and external functions
  private static boolean dumpHistory = false;

  private void zoomHistoryReset() {
    zoomHistory.clear();
    zoomHistoryIdx = 0;
    zoomHistoryNotifyListeners();
  }

  public boolean zoomHistoryAtMax() {
    validate_zoomHistoryIdx();
    if (zoomHistoryIdx >= zoomHistory.size() - 1) {
      return true;
    }
    return false;
  }

  public boolean zoomHistoryAtMin() {
    validate_zoomHistoryIdx();
    if (zoomHistoryIdx == 0) {
      return true;
    }
    return false;
  }

  public void zoomHistoryAdd() {
    ZoomHistoryEvent e = zoomHistoryCreateEvent();
    zoomHistoryAdd(e);
  }

  private void zoomHistoryAdd(ZoomHistoryEvent e) {
    validate_zoomHistoryIdx();
    for (int ii = zoomHistory.size() - 1; ii >= zoomHistoryIdx; ii--) {
      zoomHistory.remove(ii);
    }
    int prev_frame = zoomHistoryIdx - 1;
    if (prev_frame >= 0 && prev_frame < zoomHistory.size()) {
      ZoomHistoryEvent old_e = zoomHistory.get(prev_frame);
      if (e.equals(old_e)) {
        if (dumpHistory)
          System.out.printf(
              "HistoryAdd@%d: skipping - exact match: %s\n", zoomHistoryIdx, e.toString()); // debug
        zoomHistoryNotifyListeners();
        return;
      }
    }
    zoomHistory.add(e);
    zoomHistoryIdx = zoomHistory.size();
    if (dumpHistory)
      System.out.printf("HistoryAdd@%d: %s\n", zoomHistoryIdx, e.toString()); // debug
    //
    // if(debugHistory)System.out.printf("HistoryAdd@%d:\n%s",zoomHistoryIdx,zoomHistoryDump().toString()); // debug
    zoomHistoryNotifyListeners();
  }

  public void undoZoom() {
    if (zoomHistoryAtMin()) {
      return;
    }
    // remember where we came from:
    ZoomHistoryEvent e = zoomHistoryCreateEvent();
    if (zoomHistoryIdx > zoomHistory.size() - 1) {
      zoomHistory.add(e);
    } else {
      zoomHistory.set(zoomHistoryIdx, e);
    }
    if (dumpHistory)
      System.out.printf(
          "HistoryUndo@%d: updated %d: %s\n",
          zoomHistoryIdx - 1, zoomHistoryIdx, e.toString()); // debug
    // restore zoom
    e = zoomHistory.get(--zoomHistoryIdx);
    //
    // if(debugHistory)System.out.printf("HistoryUndo@%d:\n%s",zoomHistoryIdx,zoomHistoryDump().toString()); // debug
    zoomHistoryDoZoom(e);
    zoomHistoryNotifyListeners();
  }

  public void redoZoom() {
    if (zoomHistoryAtMax()) {
      return;
    }
    // remember where we came from:
    ZoomHistoryEvent e = zoomHistoryCreateEvent();
    zoomHistory.set(zoomHistoryIdx, e);
    if (dumpHistory)
      System.out.printf(
          "HistoryRedo@%d: updated %d: %s\n",
          zoomHistoryIdx, zoomHistoryIdx, e.toString()); // debug
    // restore zoom
    e = zoomHistory.get(++zoomHistoryIdx);
    //
    // if(debugHistory)System.out.printf("HistoryRedo@%d:\n%s",zoomHistoryIdx,zoomHistoryDump().toString()); // debug
    zoomHistoryDoZoom(e);
    zoomHistoryNotifyListeners();
  }

  private void validate_zoomHistoryIdx() {
    if (zoomHistoryIdx > zoomHistory.size()) {
      if (dumpHistory)
        System.out.printf(
            "HistoryValidate fail: idx=%d size=@%d\n", zoomHistoryIdx, zoomHistory.size()); // debug
      zoomHistoryIdx = zoomHistory.size(); // weird
    } else if (zoomHistoryIdx < 0) {
      if (dumpHistory)
        System.out.printf(
            "HistoryValidate fail: idx=%d size=@%d\n", zoomHistoryIdx, zoomHistory.size()); // debug
      zoomHistoryIdx = 0; // weird
    }
  }

  private ZoomHistoryEvent zoomHistoryCreateEvent() {
    ZoomHistoryEvent e =
        new ZoomHistoryEvent(
            timeAxisCalculator.getTimeStart(),
            timeAxisCalculator.getTimeEnd(),
            dataAxisCalculator.getVisibleStart(),
            dataAxisCalculator.getVisibleEnd(),
            zoom_time_center,
            zoom_time_center_screen_percent,
            ypct_focus,
            ypct_focus_screen_percent,
            AnWindow.getInstance().getSettings().getTimelineSetting().getTLStackDepth());
    return e;
  }

  private StringBuffer zoomHistoryDump() {
    StringBuffer sb = new StringBuffer();
    for (int ii = 0; ii < zoomHistory.size(); ii++) {
      if (ii == zoomHistoryIdx) {
        sb.append("  *");
      } else {
        sb.append("   ");
      }
      sb.append(String.format("%2d: %s\n", ii, zoomHistory.get(ii).toString()));
    }
    return sb;
  }

  private void zoomHistoryNotifyListeners() {
    TimelineSelectionHistoryEvent e =
        new TimelineSelectionHistoryEvent(zoomHistoryAtMin(), zoomHistoryAtMax());
    tldrawer.notifyZoomHistoryChange(e);
  }

  private void zoomHistoryDoZoom(ZoomHistoryEvent e) {
    zoom_time_center = e.timeFocus;
    zoom_time_center_screen_percent = e.timeFocusScreenPercent;
    ypct_focus = e.ypctFocus;
    ypct_focus_screen_percent = e.ypctFocusScreenPercent;
    AnWindow.getInstance()
        .getSettings()
        .getTimelineSetting()
        .setTLStackDepth(TimelinePanel.this, e.vZoomLevel); // delayed :(
    zoomTimeToRange(e.timeStart, e.timeEnd, false);
    zoomYToRange(e.ypctStart, e.ypctEnd, false);
    zoom_time_center = e.timeFocus;
    zoom_time_center_screen_percent = e.timeFocusScreenPercent;
    ypct_focus = e.ypctFocus;
    ypct_focus_screen_percent = e.ypctFocusScreenPercent;
    // YXXX does double paint
  }

  // --- mouse and rubberbanding modes

  /**
   * Set Mouse Mode to MOUSE_GRABBING_MODE or MOUSE_RUBBERBANDING_MODE The modes differ in how to
   * process mouse drags. MOUSE_GRABBING_MODE - a drag pans MOUSE_RUBBERBANDING_MODE - a drag draws
   * a rectangle for zooming Both modes select objects with a single click and zoom and center on a
   * double click.
   */
  public void setMouseMode(MouseDragMode mode) {
    if (mouse_drag_mode != mode) {
      mouse_drag_mode = mode;
      global_mouse_drag_mode_changed = true;
    }
  }

  /** Return mouse mode */
  public MouseDragMode getMouseMode() {
    return mouse_drag_mode;
  }

  /** Set type of rubberbanding to use */
  public void setRubberbandIs2D(boolean is2d) {
    rubberbandIs2D = is2d;
  }

  /** Add a listener for changes to the range of the display */
  public void addChangeListener(ChangeListener listener) {
    change_listeners.add(listener);
  }

  /** Notify change listeners of a zoom or pan */
  private void notifyListeners() {
    ChangeEvent event = new ChangeEvent(this);
    for (ChangeListener listener : change_listeners) {
      listener.stateChanged(event);
    }
  }

  private int limitCoord(int initialCoord, int max) {
    int coord = initialCoord;
    if (coord < 0) {
      coord = 0;
    } else if (coord >= max) {
      coord = max - 1;
    }
    return coord;
  }

  /**
   * Main routine to layout the timeline with the following components. timelineCanvas - central
   * drawing area to display the timeline globalRuler - ruler showing the global range on the bottom
   * of the timeline. localRuler - ruler that shows the width of the current display. verticalRuler
   * - ruler with the process labels. horScroll - time Scrollbar on the bottom of the timeline.
   * verScroll - process scrollbar on the right side of the display.
   */
  private void initializeComponents() {
    this.setLayout(new BorderLayout());

    southPanel = new JPanel();
    southPanel.setLayout(new BorderLayout());
    this.add(southPanel, BorderLayout.SOUTH);

    /* south: graph's horizontal scrollbar */
    horScroll = new AnLongScrollBar(JScrollBar.HORIZONTAL);
    horScroll.addLongAdjustmentListener(
        new AnLongScrollBar.LongAdjustmentListener() {
          public void adjustmentValueChanged(AnLongScrollBar.LongAdjustmentEvent adjustmentEvent) {
            if (set_hscroll) {
              /* ignore events while adjusting geometry */
              return;
            }
            {
              if (scroll_adjusting_snapshot == null) {
                scroll_adjusting_snapshot = zoomHistoryCreateEvent();
              }

              hscroll_adjusting = true;
              long newstart = adjustmentEvent.getValue();
              panTimeTo(newstart);
              hscroll_adjusting = false;

              if (!adjustmentEvent.getValueIsAdjusting()) {
                zoomHistoryAdd(scroll_adjusting_snapshot);
                scroll_adjusting_snapshot = null;
              }
            }
          }
        });
    southPanel.add(BorderLayout.CENTER, horScroll);

    /* east: scroll bar for processes */
    verScroll = new JScrollBar(JScrollBar.VERTICAL);
    verScroll.addAdjustmentListener(
        new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
            if (set_vscroll) {
              /* ignore events while adjusting geometry */
              return;
            }
            {
              if (scroll_adjusting_snapshot == null) {
                scroll_adjusting_snapshot = zoomHistoryCreateEvent();
              }

              yscroll_adjusting = true;
              double newstart;
              if (enable_vertical_row_snap) {
                int proc = verScroll.getValue();
                int yy = dataAxisCalculator.getLowCoordForRow(proc);
                newstart = dataAxisCalculator.getPercentNearCoord(yy);
              } else {
                newstart = verScroll.getValue();
                newstart /= VERSCROLL_TO_PERCENT;
              }
              panProcTo(newstart);
              yscroll_adjusting = false;

              if (!adjustmentEvent.getValueIsAdjusting()) {
                zoomHistoryAdd(scroll_adjusting_snapshot);
                scroll_adjusting_snapshot = null;
              }
            }
          }
        });
    this.add(verScroll, BorderLayout.EAST);

    /* north: time ruler */
    ValuesNanoseconds vtype =
        new ValuesNanoseconds(timeAxisCalculator.getTimeStart(), timeAxisCalculator.getTimeEnd());
    globalRuler = new RangeRuler(vtype, RangeRuler.HORIZONTAL_ORIENTATION);
    globalRuler.setTitlePlacement(localRuler.PLACEMENT_WITH_LABELS);
    globalRuler.setTitle(AnLocale.getString("Time"));
    globalRuler.setTickPlacement(RangeRuler.TICKS_ON_THE_BOTTOM);
    globalRuler.setUseTickCenterForUnitSteps(true);
    TimelineMouseAdapter globalRulerMouseAdapter =
        new TimelineMouseAdapter(
            globalRuler,
            MousePanelType.MP_X_RULER,
            hRulerDefaultMouseMode,
            MouseDragMode.MOUSE_RUBBERBANDING_MODE);
    globalRuler.addMouseWheelListener(globalRulerMouseAdapter);
    globalRuler.addMouseMotionListener(globalRulerMouseAdapter);
    globalRuler.addMouseListener(globalRulerMouseAdapter);
    this.add(globalRuler, BorderLayout.NORTH);

    /* south panel's relative scale ruler */
    vtype = new ValuesNanoseconds(0, timeAxisCalculator.getTimeDuration());
    localRuler = new RangeRuler(vtype, RangeRuler.HORIZONTAL_ORIENTATION);
    localRuler.setTitlePlacement(localRuler.PLACEMENT_WITH_LABELS);
    localRuler.setTitle(AnLocale.getString("Relative"));
    localRuler.setTickPlacement(RangeRuler.TICKS_ON_THE_TOP);
    localRuler.setUseTickCenterForUnitSteps(false);
    TimelineMouseAdapter localRulerMouseAdapter =
        new TimelineMouseAdapter(
            localRuler,
            MousePanelType.MP_X_RULER,
            hRulerDefaultMouseMode,
            MouseDragMode.MOUSE_RUBBERBANDING_MODE);
    localRuler.addMouseWheelListener(localRulerMouseAdapter);
    localRuler.addMouseMotionListener(localRulerMouseAdapter);
    localRuler.addMouseListener(localRulerMouseAdapter);
    southPanel.add(BorderLayout.NORTH, localRuler);

    /* south: spacer to east of scrollbar */
    southEastSpacer = new JPanel();
    southPanel.add(BorderLayout.EAST, southEastSpacer);

    /* south: spacer to west of scrollbar */
    southWestSpacer = new JPanel();
    southPanel.add(BorderLayout.WEST, southWestSpacer);

    /* The canvas is where the timeline is drawn */
    timelineCanvas = new TimelineCanvas();
    { // YXXX is this the right place vs. AnWindow or TL2Disp/MPITLDisp?
      AccessibleContext ac = timelineCanvas.getAccessibleContext();
      if (ac != null) {
        String accessibleName = AnLocale.getString("Timeline"); // YXXX what should text be?
        String accessibleDescription =
            AnLocale.getString("Timeline shows experiment activity vs. time");
        ac.setAccessibleName(accessibleName);
        ac.setAccessibleDescription(accessibleName);
      }
    }
    TimelineMouseAdapter timelineMouseAdapter =
        new TimelineMouseAdapter(timelineCanvas, MousePanelType.MP_MAIN_PANEL,
            mainPanelDefaultMouseMode, MouseDragMode.MOUSE_RUBBERBANDING_MODE);
    timelineCanvas.addMouseWheelListener(timelineMouseAdapter);
    timelineCanvas.addMouseMotionListener(timelineMouseAdapter);
    timelineCanvas.addMouseListener(timelineMouseAdapter);

    // zoom sliders.  Dimensions are used for timelineCanvas
    if (enable_zoom_overlays) {
      initZoomOverlays(timelineCanvas);
      int overlaySz = timeOverlay.getWidth();
      timelineCanvas.setPreferredSize(new Dimension(overlaySz, overlaySz));
      timelineCanvas.setMinimumSize(new Dimension(overlaySz, overlaySz));
    }

    this.add(timelineCanvas, BorderLayout.CENTER);

    /* west: vertical ruler for processes */
    verticalRuler = tldrawer.getVerticalRuler();
    TimelineMouseAdapter vRulerMouseAdapter =
        new TimelineMouseAdapter(
            verticalRuler, MousePanelType.MP_Y_RULER, vRulerDefaultMouseMode, null);
    verticalRuler.addMouseWheelListener(vRulerMouseAdapter);
    verticalRuler.addMouseMotionListener(vRulerMouseAdapter);
    verticalRuler.addMouseListener(vRulerMouseAdapter);
    this.add(verticalRuler, BorderLayout.WEST);

    tldrawer.addSelectionListener(
        new TimelineSelectionListener() {
          public void valueChanged(TimelineSelectionGenericEvent ge) {
            if (ge instanceof TimelineSelectionEvent) {
              // update caliper
              TimelineSelectionEvent rowTimeEvent =
                  (ge instanceof TimelineSelectionEvent) ? (TimelineSelectionEvent) ge : null;
              boolean shift = false;
              boolean updateCaliper = true;
              if (rowTimeEvent != null) {
                if (rowTimeEvent.shiftKey) {
                  shift = true;
                }
                if (!rowTimeEvent.requestCaliperUpdate) {
                  updateCaliper = false;
                }
                if (rowTimeEvent.moveAxis == rowTimeEvent.moveAxis.LEFT_RIGHT) {
                  long eventTimeStart = tldrawer.getSelectionTimeStart();
                  long eventTimeEnd = tldrawer.getSelectionTimeEnd();
                  panLeftRightToMakeVisible(eventTimeStart, eventTimeEnd);
                } else if (rowTimeEvent.moveAxis == rowTimeEvent.moveAxis.UP_DOWN) {
                  double yCenter = tldrawer.getSelectionYCenter();
                  panUpDownToMakeVisible(yCenter);
                } else {
                  // direct or restored selection (not cursor move)
                  if (tldrawer.selectionActive()) {
                    double yCenter = tldrawer.getSelectionYCenter();
                    panUpDownToMakeVisible(yCenter);
                  } else {
                    int ii = 1; // weird
                  }
                }
              }
              if (updateCaliper) {
                long timeLow = tldrawer.getSelectionTimeStart();
                long timeHigh = tldrawer.getSelectionTimeEnd();
                caliperUtilSetAndNotify(timeLow, timeHigh, shift);
              }
            } else {
              int ii = 1; // for breakpoint
            }
          }
        });

    this.revalidate(); // needed after adding components
  }

  private void panUpDownToMakeVisible(double yCenter) {
    ZoomHistoryEvent e = zoomHistoryCreateEvent();

    final double recenterPercentOfScreen = AUTO_PAN_RELOCATION_PERCENT;
    // Y axis
    ypct_focus = yCenter;
    final double visibleStart = dataAxisCalculator.getVisibleStart();
    final double visibleEnd = dataAxisCalculator.getVisibleEnd();
    final double vizPercent = dataAxisCalculator.getVisiblePercent();
    if (ypct_focus <= visibleStart) {
      zoomHistoryAdd(e);
      ypct_focus_screen_percent = recenterPercentOfScreen;
      double newStart = ypct_focus - vizPercent * recenterPercentOfScreen;
      panProcTo(newStart);
    } else if (ypct_focus >= visibleEnd) {
      zoomHistoryAdd(e);
      ypct_focus_screen_percent = 1 - recenterPercentOfScreen;
      double newStart = ypct_focus - vizPercent * (1 - recenterPercentOfScreen);
      panProcTo(newStart);
    } else {
      ypct_focus_screen_percent = calcYpctScreenPercent(ypct_focus);
    }
  }

  private void panLeftRightToMakeVisible(long eventTimeStart, long eventTimeEnd) {
    ZoomHistoryEvent e = zoomHistoryCreateEvent();

    double recenterPercentOfScreen = AUTO_PAN_RELOCATION_PERCENT;
    // Time axis
    zoom_time_center = (eventTimeStart + eventTimeEnd) / 2;
    final long visibleTimeStart = timeAxisCalculator.getTimeStart();
    final long visibleTimeEnd = timeAxisCalculator.getTimeEnd();
    final long visibleDuration = timeAxisCalculator.getTimeDuration();
    if (visibleDuration < 5) {
      // with very few ns of time, percentage doesn't work, pick middle.
      recenterPercentOfScreen = 0.51;
    }

    if (eventTimeEnd < visibleTimeStart) {
      zoomHistoryAdd(e);
      zoom_time_center_screen_percent = recenterPercentOfScreen;
      zoomTimeToDuration(visibleDuration);
    } else if (eventTimeStart > visibleTimeEnd) {
      zoomHistoryAdd(e);
      zoom_time_center_screen_percent = 1 - recenterPercentOfScreen;
      zoomTimeToDuration(visibleDuration);
    } else {
      zoom_time_center_screen_percent = calcTimeScreenPercent(zoom_time_center);
    }
  }

  // === Mouse handlers
  private static enum MousePanelType {
    MP_MAIN_PANEL,
    MP_X_RULER,
    MP_Y_RULER
  };

  private class TimelineMouseAdapter extends MouseAdapter {

    /* mouse dragging */
    private ZoomHistoryEvent history_start_drag;
    private int x_start_drag = 0;
    private int y_start_drag = 0;
    private int caliperStepX = 0;
    private int caliperX = 0;
    private int recent_mouse_button = -1;
    private long x_start_drag_timeOffset;
    private double y_start_drag_percentOffset;
    private Timer mousePressedTimer = null;
    private Timer autoScrollTimer;
    private final MousePanelType parentPanelType;
    private final JComponent parentPanel;
    private MouseSubmode mouseSubmode = MouseSubmode.MM_UNKNOWN;
    private final MouseDragMode altDragAction;
    private final MouseDragMode dragAction; // if null, use global mouse_mode
    private final boolean x_is_live, y_is_live;

    public TimelineMouseAdapter(
        JComponent _parentPanel,
        MousePanelType _mmode,
        MouseDragMode _dragAction,
        MouseDragMode _altDragAction) {
      parentPanel = _parentPanel;
      parentPanelType = _mmode;
      dragAction = _dragAction;
      altDragAction = _altDragAction;
      x_is_live =
          (parentPanelType == MousePanelType.MP_MAIN_PANEL
              || parentPanelType == MousePanelType.MP_X_RULER);
      y_is_live =
          (parentPanelType == MousePanelType.MP_MAIN_PANEL
              || parentPanelType == MousePanelType.MP_Y_RULER);
      autoScrollTimer =
          new Timer(
              AUTO_PAN_REPEAT_TIME,
              new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                  // user timer to scroll
                  if (x_is_live) {
                    long curStart = timeAxisCalculator.getTimeStart();
                    long binTime = timeAxisCalculator.getTimePerBin();
                    long newTime = curStart + binTime * caliperStepX / 3;
                    panTimeTo(newTime);
                    switch (getMouseSubmode()) {
                      case MM_DRAG_CALIPER:
                      case MM_DRAG_SELECTION:
                        {
                          long time = timeAxisCalculator.getTimeNearCoord(caliperX);
                          timeCaliper.setActiveCaliperTime(time);
                          caliperUtilNotify(timeCaliper);
                          break;
                        }
                      default:
                        int ii = 1; // weird
                        break;
                    }
                  }
                }
              });
      autoScrollTimer.setRepeats(true);
    }

    private boolean killMousePressedTimer() {
      if (mousePressedTimer != null) {
        mousePressedTimer.stop();
        mousePressedTimer = null;
        return true;
      }
      return false;
    }

    private void checkAutoScrollTimer(MouseEvent e) {
      int scrollzonePixels = AUTO_PAN_REGION_PIXELS;
      int rightScrollzonePixels = timeAxisCalculator.getAvailPixels() - scrollzonePixels;
      caliperStepX = 0;
      final int ix = translateX(e);
      if (ix > rightScrollzonePixels) {
        caliperStepX = ix - rightScrollzonePixels;
      } else if (scrollzonePixels > ix) {
        caliperStepX = -(scrollzonePixels - ix);
      }
      if (caliperStepX != 0) {
        // auto-pan needed
        caliperX = ix;
        if (!autoScrollTimer.isRunning()) {
          autoScrollTimer.start();
        }
      } else {
        // turn off auto-pan
        autoScrollTimer.stop();
      }
    }

    // === track ctrl-key events to change mouse state
    // couldn't figure out which JComponent's getInputMap() is needed:
    //            private boolean ctrlKeyDown = false;
    //            private void initCtrlKeyListener(){
    //                String ks_string;
    //                KeyStroke ks_def;
    //                ks_string = "_CONTROLPRESSED";
    //                ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL,
    //                        InputEvent.CTRL_DOWN_MASK, false);
    //                parentPanel.getInputMap(JComponent.WHEN_FOCUSED).put(ks_def, ks_string);
    //                parentPanel.getActionMap().put(ks_string, new AbstractAction() {
    //                    @Override
    //                    public void actionPerformed(ActionEvent ev) {
    //                        ctrlKeyDown = true;
    //
    // parentPanel.setCursor(handcursors.getCursor(TimelineCursor.OPEN_HAND_CURSOR));
    //                    }
    //                });
    //
    //                ks_string = "_CONTROLRELEASED";
    //                ks_def = KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL,
    //                        InputEvent.CTRL_DOWN_MASK, true);
    //                parentPanel.getInputMap(JComponent.WHEN_FOCUSED).put(ks_def, ks_string);
    //                parentPanel.getActionMap().put(ks_string, new AbstractAction() {
    //                    @Override
    //                    public void actionPerformed(ActionEvent ev) {
    //                        ctrlKeyDown = false;
    //
    // parentPanel.setCursor(handcursors.getCursor(TimelineCursor.CLOSED_HAND_CURSOR));
    //                    }
    //                });
    //            }

    // === translate coordinates from rulers to pmain panel
    private int translateX(MouseEvent e) {
      // translate to main panel coordinates
      if (!x_is_live) {
        return Integer.MIN_VALUE;
      }
      // YXXX ugly switch on global object
      final int x_offset;
      if (e.getComponent() == globalRuler) {
        x_offset = globalRuler.getPaddingLow() - timeAxisCalculator.getMargin();
      } else if (e.getComponent() == localRuler) {
        x_offset = localRuler.getPaddingLow() - timeAxisCalculator.getMargin();
      } else if (e.getComponent() == verticalRuler) {
        x_offset = 0;
      } else {
        x_offset = 0;
      }
      final int ix = e.getX() - x_offset;
      return ix;
    }

    private int translateY(MouseEvent e) {
      // translate to main panel coordinates
      if (!y_is_live) {
        return Integer.MIN_VALUE;
      }
      return e.getY();
    }

    private boolean coord_is_live(int ix) {
      if (ix == Integer.MIN_VALUE) {
        return false;
      }
      return true;
    }

    // === timeline mouse modes, Submodes
    private MouseSubmode getMouseSubmode() {
      return mouseSubmode;
    }

    private void setMouseSubmode(final MouseSubmode newMode) {
      // sets mouseSubmode and panel's cursor
      if (mouseSubmode == newMode && !global_mouse_drag_mode_changed) {
        return;
      }
      global_mouse_drag_mode_changed = false;
      mouseSubmode = newMode;
      parentPanel.setCursor(getSubmodeCursor(newMode));
    }

    private Cursor getSubmodeCursor(MouseSubmode submode) {
      switch (submode) {
        case MM_OVERLAY:
          return overlayMouseHandler.getCursor();
        case MM_DRAG_RUBBERBAND:
        case MM_HOVER_RUBBERBAND:
          return handcursors.getCursor(Cursor.TEXT_CURSOR);
        case MM_DRAG_GRAB:
          return handcursors.getCursor(TimelineCursor.CLOSED_HAND_CURSOR);
        case MM_HOVER_GRAB:
          return handcursors.getCursor(Cursor.DEFAULT_CURSOR);
        case MM_DRAG_SELECTION:
        case MM_HOVER_SELECTION:
          return handcursors.getCursor(Cursor.CROSSHAIR_CURSOR);
        case MM_DRAG_CALIPER:
        case MM_HOVER_CALIPER:
          return handcursors.getCursor(Cursor.E_RESIZE_CURSOR);
      }
      return handcursors.getCursor(Cursor.DEFAULT_CURSOR); // weird, should not happen
    }

    private MouseSubmode convertHoverToDrag(MouseSubmode hoverMode) {
      switch (hoverMode) {
        case MM_HOVER_RUBBERBAND:
          return MouseSubmode.MM_DRAG_RUBBERBAND;
        case MM_HOVER_GRAB:
          return MouseSubmode.MM_DRAG_GRAB;
        case MM_HOVER_SELECTION:
          return MouseSubmode.MM_DRAG_SELECTION;
        case MM_HOVER_CALIPER:
          return MouseSubmode.MM_DRAG_CALIPER;
      }
      return hoverMode; // weird, should not happen
    }

    private void setMouseDragMode() {
      MouseSubmode newMode = convertHoverToDrag(getMouseSubmode());
      setMouseSubmode(newMode);
    }

    // === other listener actions
    public void mouseWheelMoved(MouseWheelEvent e) {
      int notches = e.getWheelRotation();
      if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
        // e.getScrollAmount() // unit increments per notch
        // e.getUnitsToScroll() // unit increments
        // scrollPane.getVerticalScrollBar().getUnitIncrement(1)  // Vertical unit increment pixels
      } else { // scroll type == MouseWheelEvent.WHEEL_BLOCK_SCROLL
        // scrollPane.getVerticalScrollBar().getBlockIncrement(1) // Vertical block increment pixels
      }
      if (MOUSE_WHEEL_ZOOMS) {
        // zoom X
        Double zoomFactor = notches * 1.5;
        final int ix = translateX(e);
        long mouseTime =
            coord_is_live(ix) ? timeAxisCalculator.getTimeAtCoord(ix) : zoom_time_center;
        long duration;
        if (zoomFactor < 0) {
          // zoom in
          Double dd = timeAxisCalculator.getTimeDuration() / (-zoomFactor);
          duration = dd.longValue();
        } else {
          // zoom out
          Double dd = timeAxisCalculator.getTimeDuration() * (zoomFactor);
          duration = dd.longValue();
          if (duration < 2) {
            duration = 2; // deal with rounding at maximum zoom
          }
        }
        zoomTimeToDuration(duration); // sets the duration
        recalc_time_focus(mouseTime);
        zoomTimeToDuration(duration); // pans to updated center
      } else {
        // vertical scrolling
        final int visiblePixels = dataAxisCalculator.getAvailPixels();
        if (visiblePixels <= 0) {
          return;
        }
        final int pixelsPerNotch = 50;
        final double percentPerPixel = dataAxisCalculator.getVisiblePercent() / visiblePixels;
        Double panPercent = (notches * pixelsPerNotch) * percentPerPixel;
        panProcBy(panPercent);
      }
    }

    public void mouseDragged(MouseEvent e) {
      if (recent_mouse_button != e.BUTTON1) {
        return;
      }
      final MouseSubmode submode = getMouseSubmode();
      switch (submode) {
        case MM_OVERLAY:
          break;
        case MM_DRAG_RUBBERBAND:
          processRubberband(e, false);
          break;
        case MM_DRAG_GRAB:
          if (x_is_live) {
            final int ix = translateX(e);
            int deltaX = ix - x_start_drag;
            long deltatime =
                timeAxisCalculator.getTimeDuration() * deltaX / timeAxisCalculator.getAvailPixels();
            long start = x_start_drag_timeOffset - deltatime;
            panTimeTo(start);
          }
          if (y_is_live) {
            final int iy = translateY(e);
            int deltaY = iy - y_start_drag;
            double deltaPercent =
                dataAxisCalculator.getVisiblePercent()
                    * deltaY
                    / dataAxisCalculator.getAvailPixels();
            double startYPercent = y_start_drag_percentOffset - deltaPercent;
            panProcTo(startYPercent);
          }
          break;
        case MM_DRAG_SELECTION:
        case MM_DRAG_CALIPER:
          if (x_is_live) {
            checkAutoScrollTimer(e);
            final int ix = translateX(e);
            long time = timeAxisCalculator.getTimeNearCoord(ix);
            timeCaliper.setActiveCaliperTime(time);
            caliperUtilNotify(timeCaliper);
          } else {
            int ii = 1; // weird, not supported yet
          }
          break;
        case MM_HOVER_RUBBERBAND:
        case MM_HOVER_GRAB:
        case MM_HOVER_SELECTION:
        case MM_HOVER_CALIPER:
          final int xDist = x_is_live ? Math.abs(x_start_drag - translateX(e)) : 0;
          final int yDist = y_is_live ? Math.abs(y_start_drag - translateY(e)) : 0;
          if (mousePressedTimer != null
              && (xDist < MOUSE_DRAG_MIN_PIXELS && yDist < MOUSE_DRAG_MIN_PIXELS)) {
            // for the initial period, don't pan/rubberband for small movements
            int foo = 1; // for breakpoint
          } else {
            // either timer expired or cursor moved significantly
            killMousePressedTimer();
            setMouseDragMode();
          }
          break;
        default:
          int xx = 1; // weird
          break;
      }
    }

    private void mouseDraggedEnd(MouseEvent e) {
      // called by mouseReleased().  Cleans up any state used by mouseDragged()
      switch (getMouseSubmode()) {
        case MM_DRAG_RUBBERBAND:
          processRubberband(e, true);
          break;
        default:
          // Java appears to provide same X/Y to mouseDragged()
          // and mouseReleased(), no need to do anything else here
          break;
      }
    }

    public void mouseMoved(MouseEvent e) {
      selectHoverState(e, false);
    }

    private void selectHoverState(final MouseEvent e, final boolean checkAltMode) {
      /* Set the cursor and mouse MM_HOVER_* state  */
      if (parentPanelType == MousePanelType.MP_MAIN_PANEL
          && overlayMouseHandler.inOverlay(e.getX(), e.getY())) {
        // for now, only support zoom overlay in main panel
        setMouseSubmode(MouseSubmode.MM_OVERLAY);
        overlayMouseHandler.activate();
        return;
      }
      overlayMouseHandler.deactivate();

      if (enable_time_caliper) {
        // check if over a caliper
        int idx = caliperUtilSelectNearX(translateX(e));
        if (idx != -1) {
          // we're hovering over a caliper
          if (idx == 0) { // selected caliper arm is the anchor
            timeCaliper.swapAnchor(); // make the other the anchor
          }
          timeCaliper.setActiveIdx(1);
          setMouseSubmode(MouseSubmode.MM_HOVER_CALIPER);
          return;
        }
      }

      final MouseDragMode defaultDragAction;
      if (checkAltMode && e.isControlDown() && altDragAction != null) {
        // toggle to alternate mode
        defaultDragAction = altDragAction;
      } else {
        defaultDragAction = dragAction != null ? dragAction : mouse_drag_mode;
      }
      final MouseSubmode newMode;
      switch (defaultDragAction) {
        default:
        case MOUSE_GRABBING_MODE:
          newMode = MouseSubmode.MM_HOVER_GRAB;
          break;
        case MOUSE_SELECTION_MODE:
          newMode = MouseSubmode.MM_HOVER_SELECTION;
          break;
        case MOUSE_RUBBERBANDING_MODE:
          newMode = MouseSubmode.MM_HOVER_RUBBERBAND;
          break;
      }
      setMouseSubmode(newMode);
    }

    public void mouseClicked(MouseEvent e) {
      if (getMouseSubmode() == MouseSubmode.MM_OVERLAY) {
        return;
      }

      if (e.getButton() == e.BUTTON1) {
        if (e.getClickCount() == 2) {
          doubleClickZoomIn(e);
        }
      }
    }

    private void doubleClickZoomIn(final MouseEvent e) {
      zoomHistoryAdd();
      if (x_is_live) {
        // reset zoom X center
        final int x = translateX(e);

        zoom_time_center = timeAxisCalculator.getTimeNearCoord(x);
        zoom_time_center_screen_percent = calcTimeScreenPercent(zoom_time_center);
        // zoom X
        long duration = timeAxisCalculator.getTimeDuration() / 4;
        zoomTimeToDuration(duration);
      }
      if (y_is_live) {
        final int y = translateY(e);
        // reset zoom Y center //YXXX mouse up already sets the center??
        ypct_focus = dataAxisCalculator.getPercentNearCoord(y);
        ypct_focus_screen_percent = calcYpctScreenPercent(ypct_focus);

        // zoom Y
        if (parentPanelType == MousePanelType.MP_MAIN_PANEL || enable_vertical_zoom) {
          double vis_percent = dataAxisCalculator.getVisiblePercent();
          int yproc = dataAxisCalculator.getRowNearPercent(ypct_focus);
          double three_rows_pct = dataAxisCalculator.getRowPercentOfTotal(yproc) * 3;
          if (vis_percent > three_rows_pct) {
            vis_percent /= 2;
            if (vis_percent < three_rows_pct) {
              vis_percent = three_rows_pct;
            }
            zoomYToPercent(vis_percent);
          }
        } else if (parentPanelType == MousePanelType.MP_Y_RULER) {
          int xx = 1; // no action implemented yet
        }
      }
    }

    public void mousePressed(final MouseEvent e) {
      selectHoverState(e, e.isControlDown());
      if (getMouseSubmode() == MouseSubmode.MM_OVERLAY) {
        return;
      }
      int button = e.getButton();
      recent_mouse_button = button;
      if (button != e.BUTTON1) {
        return;
      }

      /* This is where a mouse drag starts (though it's also called for
       * a click), there is not a call for MouseDragged with the initial
       * coordinates */
      {
        mousePressedTimer =
            new Timer(
                MOUSE_PRESS_DRAG_DELAY,
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent ae) {
                    setMouseDragMode(/*e*/ );
                    mousePressedTimer = null;
                  }
                });
        mousePressedTimer.setRepeats(false);
        mousePressedTimer.start();
      }
      x_start_drag = translateX(e);
      y_start_drag = translateY(e);
      x_start_drag_timeOffset = timeAxisCalculator.getTimeStart();
      y_start_drag_percentOffset = dataAxisCalculator.getVisibleStart();
      history_start_drag = zoomHistoryCreateEvent();

      if (getMouseSubmode() == MouseSubmode.MM_HOVER_SELECTION && !e.isShiftDown()) {
        // always resets the selection, give feedback right away
        long time = timeAxisCalculator.getTimeNearCoord(x_start_drag);
        timeCaliper.setActiveIdx(1);
        timeCaliper.setTime(0, time);
        timeCaliper.setTime(1, time);
        caliperUtilNotify(timeCaliper);
      }
    }

    private void processRubberband(final MouseEvent e, final boolean isDragEnd) {
      final int xx = translateX(e);
      final int yy = translateY(e);
      boolean yzoom = true, xzoom = true;

      if (!coord_is_live(xx)) {
        xzoom = false;
      } else if (!coord_is_live(yy) || !enable_vertical_zoom) {
        yzoom = false;
      } else if (!rubberbandIs2D) {
        // determine the primary direction of drag
        int dx = Math.abs(xx - x_start_drag);
        int dy = Math.abs(yy - y_start_drag);
        if (dx < dy) {
          xzoom = false;
        } else {
          yzoom = false;
        }
      }

      final int x1, y1, x2, y2;
      if (xzoom) {
        //                    x1 = timeAxisCalculator.getCoordAtTime(x_start_drag_time);
        x1 = x_start_drag;
        x2 = limitCoord(xx, timelineCanvas.getWidth());
      } else {
        x1 = 0;
        x2 = timelineCanvas.getWidth() - 1;
      }

      if (yzoom) {
        y1 = y_start_drag;
        y2 = limitCoord(yy, timelineCanvas.getHeight());
      } else {
        y1 = 0;
        y2 = timelineCanvas.getHeight() - 1;
      }

      if (!isDragEnd) {
        setRubberband(x1, y1, x2, y2);
        repaint();
      } else {
        clearRubberband();
        repaint();
        {
          if (yzoom) {
            double p1 = dataAxisCalculator.getPercentNearCoord(y1);
            double p2 = dataAxisCalculator.getPercentNearCoord(y2);
            zoomYToRange(p1, p2, false);
          }
          if (xzoom) {
            long t1 = timeAxisCalculator.getTimeNearCoord(x1);
            long t2 = timeAxisCalculator.getTimeNearCoord(x2);
            zoomTimeToRange(t1, t2, false);
          }
        }
      }
    }

    /* The drag is complete */
    public void mouseReleased(MouseEvent e) {
      if (getMouseSubmode() == MouseSubmode.MM_OVERLAY) {
        selectHoverState(e, false);
        autoScrollTimer.stop();
        killMousePressedTimer();
        return;
      }
      recent_mouse_button = -1;
      int button = e.getButton();
      if (button != e.BUTTON1) {
        return;
      }
      autoScrollTimer.stop();
      boolean preTimeout = killMousePressedTimer();
      final int xx = x_start_drag; // may be MIN_VALUE
      final int yy = y_start_drag; // may be MIN_VALUE
      final boolean isClick = preTimeout;
      if (isClick) {
        // we consider this to be a click
        final TimelineCaliper selected;
        if (coord_is_live(yy) || getMouseSubmode() == MouseSubmode.MM_HOVER_GRAB) {
          // vRuler or main panel
          // check for event hit:
          selected = tldrawer.selectNearXY(xx, yy, e.isControlDown(), e.isShiftDown());
        } else {
          // hRuler
          // don't modify event selection:
          selected = null;
        }
        // update calipers
        if (selected != null) {
          // x-axis click hit an event, update caliper to match:
          caliperUtilSetAndNotify(selected.getLowTime(), selected.getHighTime(), e.isShiftDown());
        } else {
          // x-axis click did not update event selection
          if (coord_is_live(xx)) {
            // hRuler or main panel, put caliper at click location:
            long time = timeAxisCalculator.getTimeNearCoord(xx);
            caliperUtilSetAndNotify(time, time, e.isShiftDown());
          }
        }
      } else {
        // end of drag action
        mouseDraggedEnd(e);
      }
      if (coord_is_live(yy)) {
        // y-axis click, update zoom center
        double clickYPercent = dataAxisCalculator.getPercentNearCoord(yy);
        recalc_ypct_focus(clickYPercent, !isClick);
      }
      ZoomHistoryEvent updated = zoomHistoryCreateEvent();
      if (!updated.equals(history_start_drag)) {
        zoomHistoryAdd(history_start_drag);
      }
      history_start_drag = null;
      selectHoverState(e, false);
    }
  }

  private boolean isTimeVisible(long time) {
    final long start = timeAxisCalculator.getTimeStart();
    final long end = timeAxisCalculator.getTimeEnd();
    //        if( end-start<=1 && time==start ){
    //            // special rule when only two ticks visible on screen
    //            return true;
    //        }
    //        return start<time && time<end;
    return start <= time && time <= end;
  }

  private boolean isYpctVisible(double ypct) {
    final double start = dataAxisCalculator.getVisibleStart();
    final double end = dataAxisCalculator.getVisibleEnd();
    return start < ypct && ypct < end;
  }

  private void recalc_time_focus(long mouseTime) {
    // This method updates zoom_time_center and zoom_time_center_screen_percent
    // There are many options for what zoom should use as the focal point...
    long new_zoom_center = -1;

    //        final int maxSnapToCaliperPixels = 20;
    //        // if a caliper line is near mouse, use it
    //        if (new_zoom_center == -1) {
    //            if (timeCaliper != null){
    //                long candidateTime = -1;
    //                int bestDist = Integer.MAX_VALUE;
    //                for (int ii = 0; ii < 2; ii++) {
    //                    long ctime = timeCaliper.getTime(ii);
    //                    if ( isVisible(ctime) ) {
    //                        // caliper is on screen
    //                        int caliperX = timeAxisCalculator.getCoordAtTime(ctime);
    //                        int dist = Math.abs(caliperX - mouseX);
    //                        if( maxSnapToCaliperPixels > dist &&
    //                                bestDist> dist){
    //                            bestDist = dist;
    //                            candidateTime = ctime;
    //                        }
    //                    }
    //                }
    //                if(candidateTime != -1){
    //                    new_zoom_center = candidateTime;
    //                }
    //            }
    //        }
    //
    //
    //        // try active caliper
    //        if (new_zoom_center == -1) {
    //            if (timeCaliper != null) {
    //                final long caliperTime = timeCaliper.getActiveCaliperTime();
    //                if ( caliperTime!=-1 &&
    //                        isVisible(caliperTime) ) {
    //                    new_zoom_center = caliperTime;
    //                }
    //            }
    //        }
    //
    //        // if a single caliper line is visible, use it
    //        if (new_zoom_center == -1) {
    //            if (timeCaliper != null){
    //                int num_visible = 0;
    //                long candidateTime = -1;
    //                for (int ii = 0; ii < 2; ii++) {
    //                    long ctime = timeCaliper.getTime(ii);
    //                    if ( isVisible(ctime) ) {
    //                        candidateTime = ctime;
    //                        num_visible++;
    //                    }
    //                }
    //                if (num_visible == 1) {
    //                    new_zoom_center = candidateTime;
    //                }
    //            }
    //        }

    // if any caliper line is visible, pick the closest one
    if (new_zoom_center == -1) {
      if (timeCaliper != null) {
        long candidateTime = Long.MAX_VALUE;
        int num_visible = 0;
        long minDist = Long.MAX_VALUE;
        for (int ii = 0; ii < 2; ii++) {
          long ctime = timeCaliper.getTime(ii);
          if (isTimeVisible(ctime)) {
            num_visible++;
            long dist = Math.abs(mouseTime - ctime);
            if (minDist > dist) {
              minDist = dist;
              candidateTime = ctime;
            }
          }
        }
        //                if( num_visible == 2){
        {
          // consider midpoint as well
          long ctime = (timeCaliper.getTime(0) + timeCaliper.getTime(1)) / 2;
          long dist = Math.abs(mouseTime - ctime);
          if (isTimeVisible(ctime)) {
            if (minDist > dist) {
              minDist = dist;
              candidateTime = ctime;
            }
          }
        }
        if (candidateTime != Long.MAX_VALUE) {
          new_zoom_center = candidateTime;
        }
      }
    }

    //        // try selection center
    //        if (new_zoom_center == -1) {
    //            long selectionTime = tldrawer.getSelectionTimeCenter();
    //            if (tldrawer.selectionActive() && isVisible(selectionTime)) {
    //                // if selection is visible, use it as the selection center
    //                new_zoom_center = selectionTime;
    //            }
    //        }

    // use the mouse location
    if (new_zoom_center == -1) {
      new_zoom_center = mouseTime;
    }

    boolean needPercentCalc = false;
    if (zoom_time_center != new_zoom_center) {
      zoom_time_center = new_zoom_center;
      needPercentCalc = true;
    }
    if (timeAxisCalculator.getTimeStart() == tldrawer.getAbsoluteTimeStart()
        || timeAxisCalculator.getTimeEnd() == tldrawer.getAbsoluteTimeEnd()) {
      // maxxed out at one edge
      needPercentCalc = true;
    }
    if (timeAxisCalculator.getTimeDuration() < 10) {
      // using percentage when there are only a few ns doesn't work.  Pick middle
      zoom_time_center_screen_percent = 0.49;
    } else if (needPercentCalc) {
      double newPercent = calcTimeScreenPercent(zoom_time_center);
      zoom_time_center_screen_percent = newPercent;
    }
  }

  private boolean double_equals(double a, double b) {
    final double sigma = 0.0001;
    return Math.abs(a - b) < sigma;
  }

  private void recalc_ypct_focus(double clickYPercent, boolean isDrag) {
    // try selection center
    double new_zoom_center = -1;
    final double visEnd = dataAxisCalculator.getVisibleEnd();
    final double visStart = dataAxisCalculator.getVisibleStart();
    final double midScreen = (visEnd + visStart) / 2;
    if (isDrag) {
      double sel = tldrawer.getSelectionYCenter();
      if (tldrawer.selectionActive() && isYpctVisible(sel)) {
        // if selection is visible, use it as the selection center
        new_zoom_center = sel;
      } else {
        new_zoom_center = midScreen;
      }
    }

    // use the mouse location
    if (new_zoom_center < 0) {
      if (isYpctVisible(clickYPercent)) {
        new_zoom_center = clickYPercent;
      } else {
        new_zoom_center = midScreen;
      }
    }

    boolean needPercentCalc = false;
    if (!double_equals(ypct_focus, new_zoom_center)) {
      ypct_focus = new_zoom_center;
      needPercentCalc = true;
    } else if (double_equals(visStart, 0) || double_equals(visEnd, 1)) {
      // maxxed out at one edge
      needPercentCalc = true;
    }
    if (needPercentCalc) {
      double newPercent = calcYpctScreenPercent(ypct_focus);
      ypct_focus_screen_percent = newPercent;
    }
  }

  public TimelineCaliper getCaliperClone() {
    if (timeCaliper == null) {
      return null;
    }
    return timeCaliper.clone();
  }

  public void zoomToCaliper() {
    if (timeCaliper == null || timeCaliper.isSingleEvent()) {
      return;
    }
    zoomHistoryAdd();
    zoomTimeToRange(timeCaliper.getLowTime(), timeCaliper.getHighTime(), true);
  }

  private void caliperUtilNotify(TimelineCaliper caliper) {
    timeCaliper = caliper;
    recalc_time_focus(zoom_time_center);
    tldrawer.notifyCaliperChange(getCaliperClone());
    updateLocalRuler();
    if (hRulerDefaultMouseMode == TimelinePanel.MouseDragMode.MOUSE_SELECTION_MODE) {
      updateHRulerCalipers();
    }
  }

  private void caliperUtilSetTimeAndNotify(long anchor, long caliper) {
    TimelineCaliper newCaliper = new TimelineCaliper(anchor, caliper);
    caliperUtilNotify(newCaliper);
  }

  private int caliperUtilSelectNearX(int ix) { // returns index of caliper or -1
    if (timeCaliper == null) {
      return -1;
    }
    if (ix == Integer.MIN_VALUE) {
      return -1;
    }
    int closestDist = CALIPER_CLICK_PIXEL_TOLERANCE;
    TimelineCaliper closestCaliper = null;
    int closestTimeIdx = -1;
    { // originally designed to loop through multiple calipers
      TimelineCaliper caliper = timeCaliper;
      for (int timeIdx = 0; timeIdx < caliper.getSize(); timeIdx++) {
        long time = caliper.getTime(timeIdx);
        if (caliper.getHighTime() == time // is right side
            && !(caliper.isSingleEvent() && timeIdx == 0)) { // is not left side
          time += 1; // +1 because all events are shown fattened by 1 ns
        }
        int x = timeAxisCalculator.getCoordAtTime(time);
        final int xDist = Math.abs(x - ix);
        if (closestDist > xDist) {
          closestDist = xDist;
          closestCaliper = caliper;
          closestTimeIdx = timeIdx;
        }
      }
    }
    if (closestCaliper == null) {
      return -1; // nothing found within caliper_max_grab_distance
    }
    return closestTimeIdx;
  }

  private void caliperUtilSetAndNotify(long timeLow, long timeHigh, boolean shift) {
    final long anchorTime, caliper;
    if (timeCaliper != null && shift) {
      anchorTime = timeCaliper.getTime(0);
      if (timeLow >= anchorTime) {
        caliper = timeHigh;
      } else if (timeHigh <= anchorTime) {
        caliper = timeLow;
      } else {
        // new event straddles anchor, ambigous
        if (Math.abs(anchorTime - timeHigh) > Math.abs(anchorTime - timeLow)) {
          caliper = timeHigh;
        } else {
          caliper = timeLow;
        }
      }
    } else {
      anchorTime = timeLow;
      caliper = timeHigh;
    }
    caliperUtilSetTimeAndNotify(anchorTime, caliper);
  }

  // requires timelineCanvas
  private void initZoomOverlays(JComponent parentCanvas) {
    timeOverlay =
        new ZoomRulerOverlay(
            parentCanvas,
            2,
            2,
            ZoomRulerOverlay.HORIZONTAL_ORIENTATION,
            ZoomRulerOverlay.RULER_LD_DEFAULT,
            false,
            true,
            false,
            false);
    timeOverlay.setNumberOfLevels(getZoomTimeLevelMax());
    timeOverlay.addZoomRulerListener(
        new ZoomRulerListener() {
          public void zoomChanged(ZoomRulerEvent event) {
            if (event.isUndo()) {
              undoZoom();
            } else if (event.isRedo()) {
              redoZoom();
            } else if (event.isLevelChange()) {
              int lev = event.getZoomLevel();
              zoomTimeToLevel(lev);
            } else if (event.isLevelPlus()) {
              zoomTimeIn();
            } else if (event.isLevelMinus()) {
              zoomTimeOut();
            } else if (event.isReset()) {
              resetZoom();
            } else if (event.isPan()) {
              if (event.getDirection() == ZoomRulerEvent.PAN_UP) panUp();
              else if (event.getDirection() == ZoomRulerEvent.PAN_RIGHT) panRight();
              else if (event.getDirection() == ZoomRulerEvent.PAN_DOWN) panDown();
              else // PAN_LEFT
              panLeft();
            }
          }
        });
    if (enable_vertical_zoom) {
      processOverlay =
          new ZoomRulerOverlay(
              parentCanvas,
              2,
              timeOverlay.getHeight() + 1,
              ZoomRulerOverlay.VERTICAL_ORIENTATION,
              ZoomRulerOverlay.RULER_LD_DEFAULT,
              false,
              false,
              false,
              false);
      processOverlay.setNumberOfLevels(getZoomProcessLevelMax());
      processOverlay.addZoomRulerListener(
          new ZoomRulerListener() {
            public void zoomChanged(ZoomRulerEvent event) {
              if (event.isLevelChange()) {
                int lev = event.getZoomLevel();
                zoomProcessToLevel(lev);
              } else if (event.isLevelPlus()) {
                zoomProcessIn();
              } else if (event.isLevelMinus()) {
                zoomProcessOut();
              }
            }
          });
    }

    this.overlayMouseHandler =
        new OverlayMouseHandler() {
          private boolean active = false;

          public boolean inOverlay(int x, int y) {
            return timeOverlay.withinBounds(x, y)
                || (enable_vertical_zoom && processOverlay.withinBounds(x, y));
          }

          public void activate() {
            if (active) {
              return;
            }
            active = true;
            timeOverlay.activateMouseHandling();
            if (enable_vertical_zoom) {
              processOverlay.activateMouseHandling();
            }
          }

          public void deactivate() {
            if (!active) {
              return;
            }
            active = false;
            timeOverlay.deactivateMouseHandling();
            if (enable_vertical_zoom) {
              processOverlay.deactivateMouseHandling();
            }
          }

          public Cursor getCursor() {
            return overlaycursor;
          }
        };
  }

  // --- X & Y update

  private void updateX(boolean force) {
    updateXCalc();
    if (force
        || timeAxisCalculator.getTimeStart() != visible_time_start
        || timeAxisCalculator.getTimeEnd() != visible_time_end) {
      updateXWidgetsRanges();
      repaint();
      return;
    }
    return;
  }

  private void updateY(boolean force) {
    updateYCalc();
    if (force
        || dataAxisCalculator.getVisibleStart() != visible_ypct_start
        || dataAxisCalculator.getVisibleEnd() != visible_ypct_end) {
      updateYWidgetsRanges();
      repaint();
      return;
    }
    return;
  }

  // Update X widgets.  X-axis TimelineOptions must already be set.
  // Should not modify TimelineOptions
  private void updateXWidgetsRanges() {
    long start_ts = timeAxisCalculator.getTimeStart();
    long duration = timeAxisCalculator.getTimeDuration();

    updateXRulers();

    // horizontal scroll bar
    if (!hscroll_adjusting) {
      set_hscroll = true;
      horScroll.setValues(
          start_ts, duration, tldrawer.getAbsoluteTimeStart(), tldrawer.getAbsoluteTimeEnd() + 1);
      set_hscroll = false;
    }

    int zlvl = getZoomTimeLevel();
    if (timeOverlay != null) {
      timeOverlay.setZoomLevel(zlvl);
    }
  }

  private void updateXRulers() {
    long start_ts = timeAxisCalculator.getTimeStart();
    long end_ts = timeAxisCalculator.getTimeEnd();

    globalRuler.setRange(start_ts, end_ts);
    updateLocalRuler();
  }

  private void updateLocalRuler() {
    long start_ts = timeAxisCalculator.getTimeStart();
    long duration = timeAxisCalculator.getTimeDuration();
    final long centerTime;
    TimelineCaliper caliper = timeCaliper;
    if (caliper != null) {
      centerTime = caliper.getLowTime();
    } else if (tldrawer.selectionActive()) {
      centerTime = tldrawer.getSelectionTimeStart();
    } else {
      centerTime = zoom_time_center;
    }
    long relStart = start_ts - centerTime;
    long relEnd = relStart + duration - 1;
    localRuler.setRange(relStart, relEnd);
  }

  private void updateHRulerCalipers() {
    List<Long> valueList = new ArrayList();
    List<RangeRuler.TabStopTypes> typeList = new ArrayList();
    TimelineCaliper caliper = timeCaliper;
    if (caliper != null) {
      valueList.add(caliper.getLowTime());
      valueList.add(caliper.getHighTime());
      typeList.add(RangeRuler.TabStopTypes.TABSTOP_LEFT);
      typeList.add(RangeRuler.TabStopTypes.TABSTOP_RIGHT);
    }
    globalRuler.setTabStops(valueList, typeList, timeAxisCalculator);
    localRuler.setTabStops(valueList, typeList, timeAxisCalculator);
  }

  // Update Y widgets.  Y-axis TimelineOptions must already be set.
  // Should not modify TimelineOptions
  private void updateYWidgetsRanges() {
    // process ruler
    int start_p = dataAxisCalculator.getRowStart();
    int end_p = dataAxisCalculator.getRowEnd();
    verticalRuler.setRange(start_p, end_p);

    // process scrollbar.  Scrollbar range is based ranks.
    if (!yscroll_adjusting) {
      set_vscroll = true;

      int scroll_lo;
      int scroll_hi;
      int scroll_abs_start;
      int scroll_abs_end;
      if (enable_vertical_row_snap) {
        scroll_lo = start_p;
        scroll_hi = end_p;
        scroll_abs_start = 0;
        scroll_abs_end = tldrawer.getAbsoluteRowEnd();
      } else {
        double start_pct = dataAxisCalculator.getVisibleStart();
        double end_pct = dataAxisCalculator.getVisibleEnd();
        scroll_lo = (int) (start_pct * VERSCROLL_TO_PERCENT);
        scroll_hi = (int) (end_pct * VERSCROLL_TO_PERCENT);
        scroll_abs_start = 0;
        scroll_abs_end = VERSCROLL_TO_PERCENT;
      }
      int scroll_visible = scroll_hi - scroll_lo + 1;
      verScroll.setValues(scroll_lo, scroll_visible, scroll_abs_start, scroll_abs_end + 1);
      int scrollBlockStep = scroll_visible / 2;
      if (scrollBlockStep <= 0) scrollBlockStep = 1;
      verScroll.setBlockIncrement(scrollBlockStep);
      int scrollUnitStep = scrollBlockStep / SMALL_PAN_STEP_FRACTION;
      if (scrollUnitStep <= 0) scrollUnitStep = 1;
      verScroll.setUnitIncrement(scrollUnitStep);

      set_vscroll = false;
    }

    if (enable_vertical_zoom && processOverlay != null) {
      int plvl = getZoomProcessLevel();
      processOverlay.setZoomLevel(plvl);
    }
  }

  // --- "state" selection via cursor

  // --- pan commands

  // internal

  private void panTimeTo(long start) {
    long duration = timeAxisCalculator.getTimeDuration();
    zoomTimeToRange(start, start + duration - 1, true);
  }

  public void panProcTo(double start) { // YXXX public, hack for TL2 remap center
    double proc_cnt = dataAxisCalculator.getVisiblePercent();
    zoomYToRange(start, start + proc_cnt, true);
  }

  private void panTimeBy(long step) {
    panTimeTo(timeAxisCalculator.getTimeStart() + step);
  }

  private void panProcBy(double step) {
    zoomHistoryAdd();
    panProcTo(dataAxisCalculator.getVisibleStart() + step);
  }

  // external

  public void pageUp() {
    zoomHistoryAdd();
    panProcBy(-dataAxisCalculator.getVisiblePercent());
  }

  public void pageDown() {
    zoomHistoryAdd();
    panProcBy(dataAxisCalculator.getVisiblePercent());
  }

  public void panUp() {
    zoomHistoryAdd();
    panProcBy(-dataAxisCalculator.getVisiblePercent() / LARGE_PAN_STEP_FRACTION);
  }

  public void panDown() {
    zoomHistoryAdd();
    panProcBy(dataAxisCalculator.getVisiblePercent() / LARGE_PAN_STEP_FRACTION);
  }

  public void stepUp() {
    zoomHistoryAdd();
    panProcBy(-dataAxisCalculator.getVisiblePercent() / SMALL_PAN_STEP_FRACTION);
  }

  public void stepDown() {
    zoomHistoryAdd();
    panProcBy(dataAxisCalculator.getVisiblePercent() / SMALL_PAN_STEP_FRACTION);
  }

  public void pageLeft() {
    zoomHistoryAdd();
    panTimeBy(-timeAxisCalculator.getTimeDuration());
  }

  public void pageRight() {
    zoomHistoryAdd();
    panTimeBy(timeAxisCalculator.getTimeDuration());
  }

  public void panLeft() {
    zoomHistoryAdd();
    panTimeBy(-(timeAxisCalculator.getTimeDuration() / LARGE_PAN_STEP_FRACTION + 1));
  }

  public void panRight() {
    zoomHistoryAdd();
    panTimeBy((timeAxisCalculator.getTimeDuration() / LARGE_PAN_STEP_FRACTION + 1));
  }

  public void stepLeft() {
    zoomHistoryAdd();
    panTimeBy(-(timeAxisCalculator.getTimeDuration() / SMALL_PAN_STEP_FRACTION + 1));
  }

  public void stepRight() {
    zoomHistoryAdd();
    panTimeBy((timeAxisCalculator.getTimeDuration() / SMALL_PAN_STEP_FRACTION + 1));
  }

  private int findTimeMarkerIndex = 0;

  public void findTimeMarker() {
    if (timeCaliper == null) {
      return;
    }
    zoomHistoryAdd();
    long newTime = timeCaliper.getTime(findTimeMarkerIndex);
    findTimeMarkerIndex = (findTimeMarkerIndex + 1) & 1;

    //        zoom_time_center = newTime;
    //        zoom_time_center_screen_percent = 0.5;
    //        zoomTimeToDuration(timeAxisCalculator.getTimeDuration());
    panLeftRightToMakeVisible(newTime, newTime);
  }

  public void setTimeMarker() {
    long anchor = (timeAxisCalculator.getTimeStart() + timeAxisCalculator.getTimeEnd()) / 2;
    caliperUtilSetTimeAndNotify(anchor, anchor);
  }

  private long limitTime(long inTime) {
    if (inTime < tldrawer.getAbsoluteTimeStart()) {
      inTime = tldrawer.getAbsoluteTimeStart();
    } else if (inTime > tldrawer.getAbsoluteTimeEnd()) {
      inTime = tldrawer.getAbsoluteTimeEnd();
    }
    return inTime;
  }

  private void moveTimeMarker(long delta) {
    if (timeCaliper == null) {
      return;
    }
    long newTime = limitTime(timeCaliper.getTime(1) + delta);
    caliperUtilSetTimeAndNotify(newTime, newTime);
    panLeftRightToMakeVisible(newTime, newTime);
  }

  private void moveTimeSelectionMarker(long delta) {
    if (timeCaliper == null) {
      return;
    }
    long tmp;
    do {
      tmp = timeCaliper.getTime(1);
      if (isTimeVisible(tmp)
          ||
          // is maxxed out
          (tmp == tldrawer.getAbsoluteTimeStart() && tmp == timeAxisCalculator.getTimeStart())
          || (tmp == tldrawer.getAbsoluteTimeEnd() && tmp == timeAxisCalculator.getTimeEnd())) {
        break;
      }
      tmp = timeCaliper.getTime(0);
      if (isTimeVisible(tmp)) {
        timeCaliper.swapAnchor(); // make the other the anchor
        break;
      }
      tmp = timeCaliper.getTime(1);
    } while (false);
    long anchor = timeCaliper.getTime(0);
    long newTime = limitTime(tmp + delta);
    caliperUtilSetTimeAndNotify(anchor, newTime);
    panLeftRightToMakeVisible(newTime, newTime);
  }

  public void moveTimeMarkerLeft() {
    long step = timeAxisCalculator.getTimeDuration() / SMALL_PAN_STEP_FRACTION + 1;
    moveTimeMarker(-step);
  }

  public void moveTimeMarkerRight() {
    long step = timeAxisCalculator.getTimeDuration() / SMALL_PAN_STEP_FRACTION + 1;
    moveTimeMarker(+step);
  }

  public void moveTimeRangeToLeft() {
    long step = timeAxisCalculator.getTimeDuration() / SMALL_PAN_STEP_FRACTION + 1;
    moveTimeSelectionMarker(-step);
  }

  public void moveTimeRangeToRight() {
    long step = timeAxisCalculator.getTimeDuration() / SMALL_PAN_STEP_FRACTION + 1;
    moveTimeSelectionMarker(+step);
  }

  // --- image update utilites

  /* Bar heights are calculated on image initialization, process zoom, and
   * component resizes */
  private void updateYCalc() {
    int canvasHeight = timelineCanvas.getHeight();
    if (dataAxisCalculator.getCanvasPixels() != canvasHeight) {
      // must update setCanvasPixels() before calling doCompute()
      dataAxisCalculator.setCanvasPixels(canvasHeight);
      if (zoom_ypct_at_max) {
        // attempt to resize to full height
        dataAxisCalculator.setVisibleRange(0.0, 1.0);
      }
    }

    int avail = dataAxisCalculator.getAvailPixels();
    int min_pixels = dataAxisCalculator.getMinPixelsForAllRows();
    double desired_pct = dataAxisCalculator.getVisiblePercent();
    int desired_min = (int) (min_pixels * desired_pct);

    boolean percentAdjustNeeded = false;
    if (!enable_vertical_zoom) {
      // vertical size is set externally
      percentAdjustNeeded = true;
    } else {
      // has local control of vertical zoom
      if (avail < desired_min) {
        // not enough canvas real estate for all procs
        double percentAvail = (double) avail / min_pixels;
        if (enable_vertical_row_snap) {
          int visibleProcs =
              (int) (tldrawer.getAbsoluteRowCount() * (percentAvail + PERCENT_TOLERANCE));
          if (visibleProcs < 1) {
            visibleProcs = 1;
          }
          int absStartProc = 0;
          int absEndProc = tldrawer.getAbsoluteRowEnd();
          int zoom_proc_center = dataAxisCalculator.getRowNearPercent(ypct_focus);
          int newStartProc = zoom_proc_center - visibleProcs / 2;
          if (newStartProc < absStartProc) {
            newStartProc = absStartProc;
          }
          int newEndProc = newStartProc + (visibleProcs - 1);
          if (newEndProc > absEndProc) {
            newEndProc = absEndProc;
            newStartProc = newEndProc - (visibleProcs - 1);
          }
          dataAxisCalculator.setRowRange(newStartProc, newEndProc);
          dataAxisCalculator.edt_revalidate();
          return;
        } else {
          percentAdjustNeeded = true;
        }
      }
    }
    if (percentAdjustNeeded && min_pixels > 0) {
      double percentAvail = (double) avail / min_pixels;
      double newStartPct = ypct_focus - ypct_focus_screen_percent * percentAvail;
      if (newStartPct < 0.0) {
        newStartPct = 0.0;
      }
      double newEndPct = newStartPct + percentAvail;
      if (newEndPct > 1.0) {
        newEndPct = 1.0;
        newStartPct = newEndPct - percentAvail;
      }
      dataAxisCalculator.setVisibleRange(newStartPct, newEndPct);
    }

    dataAxisCalculator.edt_revalidate();
  }

  private void updateXCalc() {
    int canvasWidth = timelineCanvas.getWidth();
    timeAxisCalculator.setCanvasPixels(canvasWidth + 1); // +1: scrollbar acts like border
    timeAxisCalculator.edt_revalidate(); // set alignment
  }

  // rubberband
  private int rb_x1, rb_x2, rb_y1, rb_y2;
  private boolean rb_enable = false;

  private void setRubberband(int x1, int y1, int x2, int y2) {
    int tmp;
    if (x2 < x1) {
      tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    if (y2 < y1) {
      tmp = y1;
      y1 = y2;
      y2 = tmp;
    }
    rb_x1 = x1;
    rb_x2 = x2;
    rb_y1 = y1;
    rb_y2 = y2;
    rb_enable = true;
  }

  private void clearRubberband() {
    rb_enable = false;
  }

  private void drawRubberband(final Graphics g) {
    if (!rb_enable) {
      return;
    }
    g.setXORMode(Color.YELLOW);
    int w = rb_x2 - rb_x1;
    int h = rb_y2 - rb_y1;
    for (int ii = 0; ii < RUBBERBAND_WIDTH; ii++) {
      g.drawRect(rb_x1 + ii, rb_y1 + ii, w - 2 * ii, h - 2 * ii);
    }
  }

  /* This is the component where the timeline process bars and message lines
   * are rendered */
  private class TimelineCanvas extends javax.swing.JComponent implements Accessible {
    private boolean firstPaint = true;

    public TimelineCanvas() {
      super();
      initialize();
    }

    private void initialize() {
      this.addComponentListener(
          new ComponentListener() {
            public void componentResized(ComponentEvent componentEvent) {
              if (dataAxisCalculator.getCanvasPixels() != timelineCanvas.getHeight()) {
                updateY(true); // visible proc range may change
                verticalRuler.repaint();
              }
              if (timeAxisCalculator.getCanvasPixels()
                  != timelineCanvas.getWidth() + 1) { // +1: see setCanvasPixels()
                updateX(false);
                globalRuler.repaint();
                localRuler.repaint();
              }
              repaint(); // may be redundant, tbd
            }

            public void componentMoved(ComponentEvent componentEvent) {}

            public void componentShown(ComponentEvent componentEvent) {}

            public void componentHidden(ComponentEvent componentEvent) {}
          });
    }

    @Override
    public void setCursor(Cursor cursor) {
      if (super.getCursor() != cursor) {
        super.setCursor(cursor); // YXXX see if this is fast enough...
      }
    }

    // Notify timeline PaintListeners
    @Override
    public void paintComponent(Graphics g) {
      myPaint(g);
      if (timeOverlay != null) {
        timeOverlay.paintRuler(g);
      }
      if (enable_vertical_zoom && processOverlay != null) {
        processOverlay.paintRuler(g);
      }
      drawRubberband(g);
    }

    /* Get new data from the image provide and request that the state and
     * message drawers redraw the timeline image */

    private void myPaint(Graphics g) {
      //           javax.swing.JOptionPane.showMessageDialog(null, "Updating");
      boolean notify = false;
      if (dataAxisCalculator.getCanvasPixels() != this.getHeight()) {
        return; // let componentResized handle it
      }
      if (timeAxisCalculator.getCanvasPixels()
          != this.getWidth() + 1) { // +1: see setCanvasPixels()
        return; // let componentResized handle it
      }

      if (dataAxisCalculator.isInitialized() && timeAxisCalculator.isInitialized()) {
        if (tldrawer.edt_fetchData(forceDataReload)) {
          // fetch started
          //                    notify = true;
          forceDataReload = false;
        } else {
          int ii = 0; // for breakpoint
        }
      } else {
        int ii = 0; // for breakpoint
      }

      if (visible_ypct_start != dataAxisCalculator.getVisibleStart()
          || visible_ypct_end != dataAxisCalculator.getVisibleEnd()
          || visible_time_start != timeAxisCalculator.getTimeStart()
          || visible_time_end != timeAxisCalculator.getTimeEnd()) {
        // data range change

        visible_ypct_start = dataAxisCalculator.getVisibleStart();
        visible_ypct_end = dataAxisCalculator.getVisibleEnd();
        visible_time_start = timeAxisCalculator.getTimeStart();
        visible_time_end = timeAxisCalculator.getTimeEnd();

        if (dataAxisCalculator.getAbsRowCount()
            != 0) { // YXXX should probably look at Time data too?
          // legit new data
          notify = true;
        }
      }

      if (firstPaint) { // YXXX some of the following may not be kosher from paintComponent()?
        // adjust rulers for margins
        int vRulerWidth = verticalRuler.getWidth(); // not known until 1st update
        int xMargin = timeAxisCalculator.getMargin(); // currently, static
        int xPaddingLo = xMargin + vRulerWidth;
        int vScrollWidth = verScroll.getWidth();
        int xPaddingHi = xMargin + vScrollWidth;
        globalRuler.setPadding(xPaddingLo, xPaddingHi);
        localRuler.setPadding(xPaddingLo, xPaddingHi);

        globalRuler.lockRulerThickness(g, true);
        localRuler.lockRulerThickness(g, true);
        verticalRuler.lockRulerThickness(g, true);

        // repaint rulers.  Why? Initially, rulers may know their range,
        //   but not geometry, so they wouldn't have painted at the
        //   resize event.
        globalRuler.repaint();
        verticalRuler.repaint();

        southWestSpacer.setPreferredSize(new Dimension(vRulerWidth, 0));
        southEastSpacer.setPreferredSize(new Dimension(vScrollWidth, 0));
        southPanel.revalidate();
        firstPaint = false;
      }

      if (tldrawer.drawData(g)) {
        //                notify = true;
      }

      if (notify) { // YXXX some of the listeners may not be kosher from paintComponent()?
        notifyListeners();
      }
    }

    ///////////////////////////
    // Accessibility support //
    ///////////////////////////
    /**
     * Gets the AccessibleContext associated with this JPanel. For MPITimelineCanvas, the
     * AccessibleContext takes the form of an AccessibleMPITimelineCanvas. A new AccessibleJPanel
     * instance is created if necessary.
     *
     * @return an AccessibleJPanel that serves as the AccessibleContext of this JPanel
     */
    public AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleMPITimelineCanvas();
      }
      return accessibleContext;
    }

    /**
     * This class implements accessibility support for the <code>MPITimelineCanvas</code> class. It
     * provides an implementation of the Java Accessibility API appropriate to panel user-interface
     * elements.
     */
    protected class AccessibleMPITimelineCanvas extends AccessibleJComponent {
      /**
       * Get the role of this object.
       *
       * @return an instance of AccessibleRole describing the role of the object
       */
      public AccessibleRole getAccessibleRole() {
        return AccessibleRole.PANEL;
      }
    }
  }
}
