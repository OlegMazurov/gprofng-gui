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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.experiment_props.PropDescriptor;
import org.gprofng.mpmt.settings.Settings;
import org.gprofng.mpmt.statecolors.StateColorMap;
import org.gprofng.mpmt.timeline.events.*;
import org.gprofng.mpmt.timeline2.TL2DataSnapshot.*;
import org.gprofng.mpmt.timeline2.data.*;
import org.gprofng.mpmt.timeline_common.CoordCalcDataMaster;
import org.gprofng.mpmt.timeline_common.CoordCalcDataReader;
import org.gprofng.mpmt.timeline_common.CoordCalcTimeReader;
import org.gprofng.mpmt.timeline_common.TimelineCaliper;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.swing.*;

public final class TimelineDraw {
  public static final int STANDARD_WHITESPACE_PIXELS = 6;
  public static final int HMARGIN = STANDARD_WHITESPACE_PIXELS + 2; // +2 for border

  // set via constructor args
  private final TL2DataFetcher tl2DataFetcher; // for getStackFuncArray()
  private final IconRuler iconRuler;
  private final CoordCalcTimeReader timeAxisCalculator;
  private final CoordCalcDataReader dataAxisCalculator;
  private final CoordCalcDataMaster dataAxisMaster;
  private final StateColorMap colorMap;
  private StateColorMap.ColorMapSnapshot stateColorSnapshot;

  // settings
  private static final int FATTENED_EVENT_WIDTH = 5;
  // maximum pixel width for zero-duration events
  public static final int CLICK_PIXEL_TOLERANCE = FATTENED_EVENT_WIDTH / 2 + 4;

  private final int MIN_SAMPLE_3D_WIDTH = 3;
  // min width of sample before divider will show
  private final int DEFAULT_SAMPLE_HEIGHT = 20;
  private final int MAX_SAMPLE_HEIGHT = 50;
  private final int DEFAULT_HEAPSZ_HEIGHT = 60;
  private final int MAX_HEAPSZ_HEIGHT = 180;
  // vertical pixels for showing sample data
  private final int MIN_CHART_HEIGHT = 9;
  private final int MAX_CHART_HEIGHT = 9;
  // vertical pixels for showing chart data
  private final int EVENT_TICK_HEIGHT = 4;
  // pixel height of stack event tick marks
  public static final Color TICK_ONE_PIXEL_WIDE_COLOR = new Color(84, 83, 84);
  public static final Color TICK_WITH_STACK_COLOR = new Color(84, 83, 84);
  public static final Color TICK_STANDALONE_COLOR = new Color(87, 86, 87);

  private final int SAMPLE_BORDER_HEIGHT = 0; // 0 is allowed
  // height of border above/below samples
  public static final Color STANDARD_DIVIDER_COLOR =
      Color.LIGHT_GRAY; // entity dividers and subdividers
  public static final Color EXPERIMENT_DIVIDER_COLOR =
      STANDARD_DIVIDER_COLOR; // between experiments
  public static final Color COMPARE_GRP_DIVIDER_COLOR =
      EXPERIMENT_DIVIDER_COLOR; // between compare groups
  public static final Color GENERIC_SELECTION_COLOR = new Color(189, 207, 231);

  private final Color CROSSHAIR_COLOR = Color.DARK_GRAY;
  private final Color CROSSHAIR_FILL_BORDER_COLOR = Color.DARK_GRAY;
  public static final Color SELECTION_BACKGROUND_COLOR =
      new Color(209, 229, 255); // light GENERIC_SELECTION_COLOR
  private final Color CROSSHAIR_FILL_COLOR = SELECTION_BACKGROUND_COLOR;
  private final Color SAMPLE_BORDER_COLOR = Color.LIGHT_GRAY;
  private final Color SAMPLE_VDIVIDER_COLOR = Color.GRAY;

  private final Color CHART_BORDER_COLOR = Color.LIGHT_GRAY;
  private final Color CHART_BACKGROUND_COLOR = new Color(238, 238, 238);
  private final Color CHART_BACKGROUND_SEL_COLOR = GENERIC_SELECTION_COLOR;
  private final Color CHART_GRAPH_COLOR = Color.BLUE;
  private final Color CHART_GRAPH_COLOR_MAX = Color.RED;
  private final Color CHART_NUMERIC_TEXT_COLOR = new Color(0x33, 0x33, 0x33);

  private final double NS_PER_SEC = 1000000000.0;

  private class FontInfo {
    final int ascent;
    final int descent;
    final int height;
    final Font font;

    public FontInfo(Graphics g, Font font) {
      this.font = font;
      FontMetrics fm = g.getFontMetrics(font);
      if (fm != null) {
        ascent = fm.getAscent() - 1; // YXXX bogus adjust for looks
        descent = fm.getDescent() - 2; // YXXX bogus adjust for looks
        height = ascent + descent;
      } else {
        ascent = 0;
        descent = 0;
        height = 0;
      }
    }
  }

  private static final Font standard_font = (new JLabel()).getFont();
  private FontInfo fontInfoSmallBold = null;
  private FontInfo fontInfoSmallNormal = null;

  // presentation options
  private Settings.TLStack_align stack_align; // stack settings
  private int vzoom_level; // stack settings
  private int stack_frame_pixels = 3; // stack settings
  private int stackRowHeight;
  private int nominalSampleHeight;
  private int nominalHeapszHeight;
  private static final int NUM_VZOOM_MACRO_LEVELS =
      4; // vzoom_level = [0, NUM_VZOOM_MACRO_LEVELS] for zoom
  public static final int TIMELINE_MAX_VZOOM_LEVEL = 56 + NUM_VZOOM_MACRO_LEVELS;
  public static final int TIMELINE_DEFAULT_VZOOM_LEVEL = 8;
  private static final int MAX_STACK_PIXELS = 900;
  private final int VZOOM_BASE_PIXEL_STEP =
      5; // initial pixel step for vzoom>NUM_VZOOM_MACRO_LEVELS

  // user options and events
  private boolean idle_color_set; // use a special color for idle cpu
  private Color idle_color; // color for idle cpu

  // selected item
  private TimelineCaliper caliper;
  private int caliper_x1, caliper_x2;
  private TimelineSelectionEvent selection_event = null; // if non-null, rowNum must be valid
  private TimelineSelectionEvent recent_selection = null; // if non-null, rowNum must be valid

  // selected item's boundaries (x,y,h,w)
  private int sel_x, sel_y, sel_w, sel_h;
  private boolean sel_set = false;

  // selected rows
  private TreeSet<Integer> multiselect_rows;
  private List<Integer> rowNumToEntityNum; // map from rowNum to entityNum

  // geometry
  private int usedHeight; // virtual vertical pixels that
  // would be required to draw all rows

  // row data snapshot
  private List<RowGeometry> rowGeometry;
  private List<List<RowGeometry>> rowGeometryByEntity;
  private TL2DataSnapshot recentDataSnapshot;
  private List<Integer> rowOffsets;
  // Notes: there is a 1-1 mapping between rowGeometry and subrows
  // visible on timeline.
  // "rowNum" is the typical name for an index into rowGeometry.
  // "rowNum" also is used by dataAxisCalculator.
  //
  // Do NOT use rowNum to index into rowData or rowDefinition arrays!
  // This is because a single rowDefinition defines both the event data row
  // and any charts rows generated for that event data.  In other words,
  // a single rowDefinition may map to several rowGeometry elements.

  // screen state snapshot
  private int currRowFirst; // first visible row
  private int currRowLast; // first non-visible row
  private int currScrollY;
  private int currVisibleHeight;
  private boolean forceFullRepaint; // all rows must be redrawn
  private boolean forceRowGeometry; // row geometry has changed
  private final int INVALID_BIN = Integer.MIN_VALUE;

  public TimelineDraw(
      TL2DataFetcher tl2DataFetcher,
      StateColorMap colorMap,
      IconRuler iconRuler,
      CoordCalcTimeReader ccx,
      CoordCalcDataMaster ccy) {
    this.tl2DataFetcher = tl2DataFetcher;
    this.iconRuler = iconRuler;
    this.timeAxisCalculator = ccx;
    this.dataAxisCalculator = ccy;
    this.dataAxisMaster = ccy;
    this.colorMap = colorMap;
    stateColorSnapshot = null;

    rowGeometry = new ArrayList();
    rowGeometryByEntity = new ArrayList();
    multiselect_rows = new TreeSet();
    rowNumToEntityNum = new ArrayList();

    // options
    setVZoomLevel(13); // YXXX default, should be set somewhere else?
    this.stack_align = Settings.TLStack_align.TLSTACK_ALIGN_ROOT;

    // colors
    idle_color_set = false;
    idle_color = null;

    // screen info
    currRowFirst = currRowLast = 0;
    currScrollY = 0;
    currVisibleHeight = 0;
    forceFullRepaint = forceRowGeometry = false;

    // if x2 < x1, caliper is not visible:
    caliper_x1 = Integer.MAX_VALUE;
    caliper_x2 = Integer.MIN_VALUE;
  }

  private FontInfo getSmallFont(final Graphics g) {
    if (fontInfoSmallNormal != null) {
      return fontInfoSmallNormal;
    }
    if (g == null) {
      return null; // yuck
    }
    Font font = new Font("SansSerif", Font.PLAIN, standard_font.getSize() - 1);
    fontInfoSmallNormal = new FontInfo(g, font);
    return fontInfoSmallNormal;
  }

  private FontInfo getSmallBoldFont(final Graphics g) {
    if (fontInfoSmallBold != null) {
      return fontInfoSmallBold;
    }
    if (g == null) {
      return null; // yuck
    }
    Font font = new Font("SansSerif", Font.BOLD, standard_font.getSize() - 1);
    fontInfoSmallBold = new FontInfo(g, font);
    return fontInfoSmallBold;
  }

  public void setCPUIdleColor(final boolean set, final Color color) {
    if (idle_color_set != set || idle_color != color) {
      idle_color_set = set;
      idle_color = color;
      forceFullRepaint = true;
    }
  }

  public void setCaliper(TimelineCaliper newCaliper) {
    caliper = newCaliper; // this is a frozen snapshot
  }

  public void setSelection(TimelineSelectionEvent selEvent) {
    if (selEvent == null || selEvent.rowNum == -1 || selEvent.getRowGeometry() == null) {
      selection_event = null;
    } else {
      selection_event = selEvent;
      recent_selection = selEvent;
    }
  }

  public TimelineSelectionEvent getSelection() {
    return selection_event;
  }

  public void edt_resetAll() { // edt only
    recent_selection = null;
    // setSelection() & notification performed in other places
  }

  public TimelineSelectionEvent getRecentSelection() {
    if (selection_event != null) {
      return selection_event;
    }
    return recent_selection;
  }

  public void setSelectedRows(TreeSet<Integer> selected_entities) {
    multiselect_rows = new TreeSet();
    if (recentDataSnapshot == null) {
      // with current design shouldn't happen.  If needed, we could save a copy of selected_entities
      return; // experiment load in progress
    }
    int numBlks = rowGeometryByEntity.size();
    for (Integer blkIdx : selected_entities) {
      if (blkIdx < 0 || blkIdx >= numBlks) {
        continue; // weird
      }
      List<RowGeometry> entityGeos = rowGeometryByEntity.get(blkIdx);
      for (RowGeometry rowGeo : entityGeos) {
        multiselect_rows.add(rowGeo.rowNum);
      }
    }
  }

  public List<RowGeometry> getEntityRows(int entityNum) {
    if (entityNum < 0 || entityNum >= rowGeometryByEntity.size()) {
      return null;
    }
    return rowGeometryByEntity.get(entityNum);
  }

  public void setVZoomLevel(int new_vzoom_level) {
    if (new_vzoom_level < 0) {
      new_vzoom_level = 0;
    } else if (new_vzoom_level >= TIMELINE_MAX_VZOOM_LEVEL) {
      new_vzoom_level = TIMELINE_MAX_VZOOM_LEVEL;
    }
    if (this.vzoom_level != new_vzoom_level) {
      this.vzoom_level = new_vzoom_level;
      forceRowGeometry = true;
    }
  }

  public void setStackFramePixels(int stack_frame_pixels) {
    if (stack_frame_pixels < 1) {
      stack_frame_pixels = 1; // weird
    } else if (stack_frame_pixels > Settings.TIMELINE_MAX_STACK_FRAME_PIXELS) {
      stack_frame_pixels = Settings.TIMELINE_MAX_STACK_FRAME_PIXELS; // weird
    }
    if (this.stack_frame_pixels != stack_frame_pixels) {
      this.stack_frame_pixels = stack_frame_pixels;
      forceFullRepaint = true;
    }
  }

  public void setStackAlign(final Settings.TLStack_align stack_align) {
    if (this.stack_align != stack_align) {
      this.stack_align = stack_align;
      forceFullRepaint = true;
    }
  }

  public void edt_recalcRowOffsets() {
    // must be called from awt event thread
    edt_processDataSnapshot(recentDataSnapshot, true, null);
  }

  public TL2DataSnapshot getDataSnapshot() {
    return recentDataSnapshot;
  }

  // Main drawing routine for timeline
  public void drawIt(final Graphics g) {
    drawData(g);
    drawSelection(g);
    drawBorder(g);
  }

  private void drawData(final Graphics g) {
    TL2DataSnapshot dataSnapshot = recentDataSnapshot;
    boolean forceRedraw = false;
    if (fontInfoSmallBold == null || fontInfoSmallNormal == null) { // yuck
      getSmallFont(g);
      getSmallBoldFont(g);
      forceRedraw = true;
    }
    edt_processDataSnapshot(dataSnapshot, forceRedraw, g);

    final int visibleHeight = dataAxisCalculator.getCanvasPixels();
    final int visibleWidth = timeAxisCalculator.getCanvasPixels();

    // erase everything
    {
      Graphics2D g2 = (Graphics2D) g;
      Rectangle drawHere = g.getClipBounds();
      g.setColor(Color.white);
      g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);
    }

    if (dataSnapshot == null || dataSnapshot.getRowData().isEmpty()) {
      final String message;
      if (dataSnapshot == null) {
        message = AnLocale.getString("Experiment load in progress...");
      } else {
        message = AnLocale.getString("Data not available for this filter selection");
      }
      drawCenteredMessage(g, message, visibleWidth / 2, visibleHeight / 2);
      rowGeometry = new ArrayList();
      rowGeometryByEntity = new ArrayList();
      rowNumToEntityNum = new ArrayList();
      iconRuler.setRowGeometry(rowGeometry, 0, -1, -1, -1, multiselect_rows);
      return;
    }

    stateColorSnapshot = colorMap.checkForColorUpdates(stateColorSnapshot);

    final int scrollY = calcScrollY();
    setScrollY(rowGeometry, scrollY, visibleHeight); // might be optimized...

    forceFullRepaint = false; // YXXX all rows updated for now, optimize later
    // in theory, could repaint only rows that have been updated or damaged

    // draw selection crosshair
    final long maxVisTime = timeAxisCalculator.getTimeEnd();
    final long minVisTime = timeAxisCalculator.getTimeStart();
    final int x1;
    final int x2;
    final long c_high, c_low;
    if (caliper != null
        && (c_high = caliper.getHighTime()) >= minVisTime
        && (c_low = caliper.getLowTime()) <= maxVisTime) {
      // caliper is visible
      if (c_low >= minVisTime) {
        int binStart = timeAxisCalculator.getBinAtTime(c_low);
        x1 = timeAxisCalculator.getLowCoordForBin(binStart);
      } else {
        x1 = Integer.MIN_VALUE;
      }
      if (c_high <= maxVisTime) {
        int binEnd = timeAxisCalculator.getBinAtTime(c_high);
        x2 = timeAxisCalculator.getLowCoordForBin(binEnd + 1) - 1;
      } else {
        x2 = Integer.MAX_VALUE;
      }
    } else {
      // caliper is not visible
      x1 = Integer.MAX_VALUE;
      x2 = Integer.MIN_VALUE;
    }
    caliper_x1 = x1;
    caliper_x2 = x2;
    drawCaliper(g, 0, visibleHeight - 1, CROSSHAIR_FILL_COLOR); // caliper fill

    drawVisibleDividers(g, rowGeometry, visibleWidth);
    drawVisibleRows(g, rowGeometry, visibleWidth); // sets stackColorMap

    drawCaliper(g, 0, visibleHeight - 1, null); // caliper border

    // YXXX could/should validate that event is still current
    int selectedRow = (selection_event == null) ? -1 : selection_event.rowNum;

    iconRuler.setRowGeometry(
        rowGeometry, scrollY, currRowFirst, currRowLast, selectedRow, multiselect_rows);
    // YXXX canvas.setRepainting(false);
  }

  // Selected object highlighting
  private GenericEvent getVisibleSelection() {
    if (selection_event == null || selection_event.eventIdx == -1) {
      return null;
    }
    if (selection_event.eventDetail == null) {
      return null; // weird?
    }
    // check if row is visible
    final int selection_row = selection_event.rowNum;
    if (selection_row < currRowFirst || selection_row >= currRowLast) {
      return null;
    }
    if (selection_row >= rowGeometry.size()) {
      return null; // typically, when rows are cleared pending new data
    }
    final EventDetail details = selection_event.eventDetail;
    long sel_time_start = details.getTimestamp();
    long sel_time_end = sel_time_start;
    if (details instanceof DurationEvent) {
      DurationEvent devent = (DurationEvent) details;
      sel_time_start -= devent.getDuration();
    }

    final GenericEvent evt;
    evt = eventDetail2GenericEvent(details, sel_time_start, sel_time_end, selection_event.eventIdx);
    return evt;
  }

  private GenericEvent eventDetail2GenericEvent(
      EventDetail details, long sel_time_start, long sel_time_end, long evtIdx) {
    final GenericEvent genEvent;
    final long delta = timeAxisCalculator.getTimeDuration();
    final long timeWindowLow = timeAxisCalculator.getTimeStart() - delta;
    final long timeWindowHigh = timeAxisCalculator.getTimeEnd() + delta;
    // wider window used: the event highlight may be visible even if event is off screen
    if (sel_time_end < timeWindowLow || sel_time_start > timeWindowHigh) {
      // it's way off the screen
      return null;
    }
    long drawTimeStart = sel_time_start;
    long drawTimeEnd = sel_time_end;
    // limit draw range so that long calculations don't overflow:
    if (drawTimeStart < timeWindowLow) {
      drawTimeStart = timeWindowLow;
    }
    if (drawTimeEnd > timeWindowHigh) {
      drawTimeEnd = timeWindowHigh;
    }
    final int binStart = timeAxisCalculator.getBinAtTime(drawTimeStart);
    final int binEnd = timeAxisCalculator.getBinAtTime(drawTimeEnd);
    final int binCount = binEnd - binStart + 1;
    if (details instanceof ExtendedEvent) {
      int state = -1;
      if (details instanceof StateEvent) {
        state = ((StateEvent) details).getState();
      }
      genEvent =
          new StackEvent(
              evtIdx, binStart, binCount, sel_time_start, sel_time_end, details.getStack(), state);
    } else if (details instanceof Sample) {
      Sample sample = (Sample) details;
      genEvent =
          new SampleEvent(
              evtIdx, binStart, binCount, sel_time_start, sel_time_end, sample.getMStates());
    } else {
      genEvent = null; // weird
    }
    return genEvent;
  }

  private void setSelectionBounds(int x, int y, int w, int h) {
    sel_x = x;
    sel_y = y;
    sel_w = w;
    sel_h = h;
    sel_set = true;
  }

  private void drawSelection(Graphics g) {
    GenericEvent event = getVisibleSelection();
    if (event == null) {
      return;
    }

    sel_set = false; // may be set to true by drawVisibleRows()
    if (selection_event == null) {
      return;
    }
    int selRowNum = selection_event.rowNum;
    if (selRowNum >= rowGeometry.size()) {
      return; // weird?
    }
    RowGeometry selRow = rowGeometry.get(selRowNum);
    drawSelection(g, selRow, event);

    if (!(event instanceof StackEvent)) {
      return;
    }

    // identify rows that display event's state
    RowGeometry statesRow = null;
    for (int ii = 0; ii < selRow.nSubrows; ii++) {
      int row_idx = ii - selRow.subrowNum;
      if (row_idx == 0) {
        continue;
      }
      row_idx += selRowNum;
      RowGeometry tmpRow = rowGeometry.get(row_idx);
      // YXXX there must be a better way to accurately determine if a row is for states
      final RowData dataCopy =
          tmpRow.rowData.grabEventData(); // row.rowData is volatile, get snapshot
      final Object vals = dataCopy.getChartData(tmpRow.chartPropIdx);
      if (vals instanceof SampleEvent[]) {
        statesRow = tmpRow;
        StackEvent stackEvt = (StackEvent) event;
        int state = stackEvt.mState;
        if (state < 0) {
          continue; // weird
        }
        long[] propVals = new long[state + 1];
        propVals[state] = 1;
        SampleEvent sampleEvt =
            new SampleEvent(
                event.eventIdx,
                event.binStart,
                event.binCount,
                event.timeStart,
                event.timeEnd,
                propVals);
        drawSelection(g, statesRow, sampleEvt);
      }
    }
  }

  private void drawSelection(Graphics g, RowGeometry row, GenericEvent event) {
    GenericEvent[] events = new GenericEvent[1];
    events[0] = event;

    drawRowEvents(g, row, events, 0, 0, true, false);
    if (!sel_set) {
      return; // off screen
    }
    drawHighlight(g, sel_x, sel_y, sel_w, sel_h);
  }

  private void drawHighlight(Graphics g, int x, int y, int w, int h) {
    Color c[] = {Color.YELLOW, Color.YELLOW, Color.RED};
    for (int i = 0; i < c.length; i++) {
      g.setColor(c[i]);
      g.drawRect(x - i - 1, y - i - 1, w + 2 * i + 1, h + 2 * i + 1);
    }
  }

  private void drawBorder(final Graphics g) {
    final int surface_width = timeAxisCalculator.getCanvasPixels();
    final int surface_height = dataAxisCalculator.getCanvasPixels();
    final int xMargin = timeAxisCalculator.getMargin();
    final int yMargin = dataAxisCalculator.getMargin();
    final Graphics2D g2 = (Graphics2D) g;
    if (yMargin > 1 || xMargin > 1) {
      g2.setColor(STANDARD_DIVIDER_COLOR);
      g2.drawLine(0, 0, 0, surface_height - 1); // vertical, to right of ruler
      g2.setColor(Color.BLACK);
      g2.drawRect(-1, 0, surface_width + 1, surface_height - 1);
    } else {
      g2.setColor(Color.BLACK);
      g2.drawRect(0, 0, surface_width, surface_height - 1);
    }
  }

  private void drawRowVBorders(
      final Graphics g,
      final int rowStartY,
      final int rowEndY,
      final int dataStartY,
      final int dataEndY) {
    final int xMargin = timeAxisCalculator.getMargin();
    if (xMargin <= 1) {
      return;
    }
    // erase vertical margins
    final int surface_width = timeAxisCalculator.getCanvasPixels();
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(Color.WHITE);
    final int h = rowEndY - rowStartY + 1;
    g2.fillRect(0, rowStartY, xMargin, h);
    g2.fillRect(surface_width - xMargin, rowStartY, xMargin, h);

    if (dataEndY != -1) {
      // add vertical border
      g2.setColor(STANDARD_DIVIDER_COLOR);
      int xx = xMargin - 1;
      g2.drawLine(xx, dataStartY, xx, dataEndY);
      xx = surface_width - xMargin;
      g2.drawLine(xx, dataStartY, xx, dataEndY);
    }
  }

  private int calcScrollY() {
    double startPercent = dataAxisCalculator.getVisibleStart();
    int scrollY = (int) (usedHeight * startPercent) - dataAxisCalculator.getMargin();
    return scrollY;
  }

  private void drawCenteredMessage(Graphics g, String msg, int screenX, int screenY) {
    final FontMetrics fm = g.getFontMetrics();
    g.setFont(standard_font);
    g.setColor(Color.LIGHT_GRAY);
    g.drawString(msg, screenX - fm.stringWidth(msg) / 2, screenY + fm.getHeight() / 2);
  }

  private void drawCaliper(final Graphics g, final int y1, final int y2, final Color doFill) {
    final int x1_orig = caliper_x1;
    final int x2_orig = caliper_x2;
    if (x2_orig < x1_orig || y2 < y1) {
      return; // not visible
    }
    final int visibleWidth = timeAxisCalculator.getCanvasPixels();
    final int xMargin = timeAxisCalculator.getMargin();
    final int x1 = x1_orig < xMargin ? xMargin : x1_orig;
    final int x2 = x2_orig > visibleWidth - xMargin ? visibleWidth - xMargin : x2_orig;
    final int w = x2 - x1;
    if (doFill != null) {
      if (w > 1) {
        g.setColor(doFill);
        g.fillRect(x1, y1, w, y2 - y1 + 1);
      }
      return;
    }
    if (w > 1) {
      g.setColor(CROSSHAIR_FILL_BORDER_COLOR);
    } else {
      g.setColor(CROSSHAIR_COLOR);
    }
    if (x1 == x1_orig) {
      g.drawLine(x1, y1, x1, y2);
    }
    if (x2 == x2_orig && x2 != x1) {
      g.drawLine(x2, y1, x2, y2);
    }
    //        if (x1_orig == x2_orig) {
    //            // fatten caliper if original was only 1 pixel wide
    //            g.drawLine(x1 + 1, y1, x1 + 1, y2);
    //        }
  }

  private int calcRowUpperPadding(
      Graphics g,
      final RowDefinition rowDef,
      final int chartIdx,
      final int subrowNum,
      final int numCharts) {
    Settings.TLData_type dataType = rowDef.getTLDataType();

    if (chartIdx == -1
        && (dataType.equals(Settings.TLData_type.TL_SAMPLE)
            || dataType.equals(Settings.TLData_type.TL_GCEVENT))) {
      // sample event data
      // GC event data
      return chart_padding();
    }

    if (dataType.equals(Settings.TLData_type.TL_HEAPSZ)) {
      // heapsize event data and chart
      if (chartIdx == -1) { // event data
        int padding = chart_padding();
        if (padding < STANDARD_WHITESPACE_PIXELS) {
          padding++;
        }
        return padding;
      }
      // chart data
      if (rowDef.getChartProperties().get(chartIdx).getName().equals("HTYPE")) {
        return 1; // state color chart
      }
      final FontInfo fi =
          getSmallBoldFont(g); // warning: see hack to init this at the top of drawData()
      if (fi != null) {
        return fi.height + chart_padding();
      }
      return chart_padding();
    }

    return 0;
  }

  private int calcRowLowerPadding(
      final Graphics g,
      final RowDefinition rowDef,
      final int chartIdx,
      final int subrowNum,
      final int numCharts) {
    Settings.TLData_type dataType = rowDef.getTLDataType();

    if (chartIdx == -1 && dataType.equals(Settings.TLData_type.TL_HEAPSZ)) {
      // heapsz event data
      return 0;
    }

    if (chartIdx == -1
        && !dataType.equals(Settings.TLData_type.TL_SAMPLE)
        && !dataType.equals(Settings.TLData_type.TL_GCEVENT)) {
      // callstack event data
      if (numCharts == 0) { // standalone event
        if (vzoom_level <= 2) {
          return 0;
        }
        if (vzoom_level <= 4) {
          return 1;
        }
        return 2;
      } else {
        // a chart follows
        int padding = chart_padding();
        if (padding <= 1) {
          return padding;
        }
        if (subrowNum == numCharts) { // last row
          return 1;
        }
        return padding - 1; // -1 because stacks typically show "short" tick-marks
      }
    }

    // chart data
    return chart_padding();
  }

  private int chart_padding() {
    // whitespace between charts
    if (vzoom_level <= 1) {
      return 0;
    }
    if (vzoom_level <= 4) {
      return 1;
    }
    {
      int pixels = 4 * stackRowHeight / STANDARD_WHITESPACE_PIXELS / 3;
      if (pixels < STANDARD_WHITESPACE_PIXELS) {
        return pixels;
      }
    }
    return STANDARD_WHITESPACE_PIXELS;
  }

  private int calcRowHeight(
      final Graphics g,
      final RowDefinition rowDef,
      final int chartIdx,
      final int subrowNum,
      final int nCharts) {
    final int stackBaseline = 18;
    Settings.TLData_type dataType = rowDef.getTLDataType();

    if (chartIdx == -1 && dataType.equals(Settings.TLData_type.TL_SAMPLE)) {
      // sample event data
      if (vzoom_level <= NUM_VZOOM_MACRO_LEVELS) {
        int sz = DEFAULT_SAMPLE_HEIGHT * (vzoom_level + 1) / (NUM_VZOOM_MACRO_LEVELS + 2);
        return sz;
      }
      return nominalSampleHeight;
    }

    if (chartIdx == -1 && dataType.equals(Settings.TLData_type.TL_GCEVENT)) { // CXXX Bug 20801848
      // gc event data
      if (vzoom_level <= NUM_VZOOM_MACRO_LEVELS) {
        int sz = DEFAULT_SAMPLE_HEIGHT * (vzoom_level + 1) / (NUM_VZOOM_MACRO_LEVELS + 2);
        return sz;
      }
      return nominalSampleHeight;
    }

    if (dataType.equals(Settings.TLData_type.TL_HEAPSZ)) {
      // heapsize event data and chart
      if (chartIdx == -1) { // event data
        if (vzoom_level <= 1) {
          return 2;
        }
        return EVENT_TICK_HEIGHT;
      }
      // chart data
      if (rowDef.getChartProperties().get(chartIdx).getName().equals("HTYPE")) {
        return MIN_CHART_HEIGHT;
      }
      if (vzoom_level <= NUM_VZOOM_MACRO_LEVELS) {
        int sz = DEFAULT_HEAPSZ_HEIGHT * (vzoom_level + 1) / (NUM_VZOOM_MACRO_LEVELS + 2);
        return sz;
      }
      return nominalHeapszHeight;
    }

    if (chartIdx == -1) { // event data
      if (nCharts == 0) { // standalone event
        if (vzoom_level <= 0) {
          return 1;
        }
        if (vzoom_level <= 1) {
          return 2;
        }
        if (vzoom_level <= 2) {
          return 3;
        }
        if (vzoom_level <= 3) {
          return 4;
        }
        //                if (vzoom_level<=4) {
        //                    return EVENT_TICK_HEIGHT;
        //                }
        return stackRowHeight;
      } else {
        // charts follow this event row
        if (vzoom_level <= 1) {
          return 0;
        }
        if (vzoom_level <= 2) {
          return 2;
        }
        if (vzoom_level <= 3) {
          return 3;
        }
        //                if (vzoom_level<=4) {
        //                    return EVENT_TICK_HEIGHT;
        //                }
        return stackRowHeight;
      }
    }

    // chart data
    if (stackRowHeight > stackBaseline) {
      int chartHeight = MIN_CHART_HEIGHT;
      chartHeight += (stackRowHeight - stackBaseline) / 15;
      if (chartHeight > MAX_CHART_HEIGHT) {
        chartHeight = MAX_CHART_HEIGHT;
      }
      return chartHeight;
    }
    if (vzoom_level <= 0) {
      return 1;
    }
    if (vzoom_level <= 1) {
      return 2;
    }
    if (vzoom_level <= 2) {
      return 3;
    }
    if (vzoom_level <= 3) {
      return 5;
    }
    return MIN_CHART_HEIGHT;
  }

  public int rowNum2EntityNum(int rowNum) {
    if (rowNum < 0 || rowNum >= rowNumToEntityNum.size()) {
      return -1; // weird
    }
    int entityNum = rowNumToEntityNum.get(rowNum);
    return entityNum;
  }

  public RowGeometry rowNum2RowGeometry(int rowNum) {
    if (rowNum < 0 || rowNum >= rowGeometry.size()) {
      return null; // weird;
    }
    RowGeometry rowGeo = rowGeometry.get(rowNum); // YXXX sync rowGeometry?
    return rowGeo;
  }

  public List<RowGeometry> getRowGeometry() {
    return rowGeometry;
  }

  public void edt_processDataSnapshot(
      TL2DataSnapshot dataSnapshot, boolean forceRecalc, Graphics g /* may be null */) {
    if (!forceRecalc && recentDataSnapshot == dataSnapshot && !forceRowGeometry) {
      return;
    }
    recentDataSnapshot = dataSnapshot;
    rowGeometry = new ArrayList();
    rowGeometryByEntity = new ArrayList();
    rowNumToEntityNum = new ArrayList();
    usedHeight = 0; // # of pixels used on virtual vertical axis

    { // set globals used in selecting spacing
      int zero_based_zoom = vzoom_level - NUM_VZOOM_MACRO_LEVELS;
      if (zero_based_zoom < 0) {
        zero_based_zoom = 0;
      }
      int stepFactor =
          MAX_STACK_PIXELS / (TIMELINE_MAX_VZOOM_LEVEL - NUM_VZOOM_MACRO_LEVELS)
              - VZOOM_BASE_PIXEL_STEP;
      double zoomPixelStep =
          VZOOM_BASE_PIXEL_STEP
              + (double) stepFactor
                  * zero_based_zoom
                  / (TIMELINE_MAX_VZOOM_LEVEL - NUM_VZOOM_MACRO_LEVELS);
      int pixelsForCallstacks = (int) (zoomPixelStep * zero_based_zoom);
      stackRowHeight = pixelsForCallstacks + EVENT_TICK_HEIGHT;
      nominalSampleHeight = DEFAULT_SAMPLE_HEIGHT + pixelsForCallstacks / 6;
      if (nominalSampleHeight > MAX_SAMPLE_HEIGHT) {
        nominalSampleHeight = MAX_SAMPLE_HEIGHT;
      }
      nominalHeapszHeight = DEFAULT_HEAPSZ_HEIGHT + pixelsForCallstacks / 2;
      if (nominalHeapszHeight > MAX_HEAPSZ_HEIGHT) {
        nominalHeapszHeight = MAX_HEAPSZ_HEIGHT;
      }
    }

    List<ExperimentData> expList;
    final int overallEntityCount;
    if (dataSnapshot == null) {
      expList = new ArrayList();
      overallEntityCount = 0;
    } else {
      expList = dataSnapshot.getExperimentData();
      overallEntityCount = dataSnapshot.getEntityData().size();
    }
    int currentY = 1; // upper border is 1 pixel wide
    int rowGeometryNum = 0;

    // First scan rows to determine where dividers are needed:
    List<Boolean> entityNeedsSubDividers = new ArrayList(overallEntityCount);
    List<Boolean> entityNeedsDividers = new ArrayList(overallEntityCount);
    boolean someEntityHasSubDividers = false;
    boolean someEntityHasDividers = false;
    boolean someEntityIsSampleOrHeapsz = false;
    int maxEntities = 0;
    for (ExperimentData expData : expList) {
      final int numEntities = expData.experimentEntities.size();
      if (maxEntities < numEntities) {
        maxEntities = numEntities;
      }
      for (int kk = 0; kk < numEntities; kk++) {
        EntityData entityData = expData.experimentEntities.get(kk);
        // determine if sub-divider lines are needed within this entity
        boolean needsSubDividers = false;
        boolean needsDivider = false;
        boolean hasSampleOrHeapsz = false;
        // see if we need subDividers:
        int numEntityDataTypes = entityData.entityRows.size();
        for (RowData rowData : entityData.entityRows) {
          RowDefinition rowDef = rowData.rowDefinition;
          final int numCharts = rowDef.getChartCount();
          Settings.TLData_type dataType = rowDef.getTLDataType();
          if (dataType.equals(Settings.TLData_type.TL_HEAPSZ)
              || dataType.equals(Settings.TLData_type.TL_SAMPLE)) {
            hasSampleOrHeapsz = true;
            if (numEntityDataTypes > 1) {
              needsSubDividers = true;
            }
          }
          if (numCharts > 0 && numEntityDataTypes > 1) { // has multiple events and charts
            if (stackRowHeight > 8) {
              needsSubDividers = true;
            }
            if (vzoom_level >= 3) {
              needsSubDividers = true;
            }
          }
        }
        final boolean isExperimentEnd = (kk == numEntities - 1);
        // see if we need dividers:
        if (!isExperimentEnd) {
          if (needsSubDividers) {
            needsDivider = true;
          }
          if (vzoom_level >= 2) {
            needsDivider = true;
          }
          if (hasSampleOrHeapsz) {
            needsDivider = true;
          }
        }
        entityNeedsDividers.add(needsDivider);
        entityNeedsSubDividers.add(needsSubDividers);
        if (needsSubDividers) {
          someEntityHasSubDividers = true;
        }
        if (needsDivider) {
          someEntityHasDividers = true;
        }
        if (hasSampleOrHeapsz) {
          someEntityIsSampleOrHeapsz = true;
        }
      }
    }
    if (entityNeedsDividers.size() != overallEntityCount) {
      int xx = 1; // weird!
    }

    final boolean procDividersNeeded;
    if (someEntityHasSubDividers
        || someEntityHasDividers
        || someEntityIsSampleOrHeapsz
        || vzoom_level >= 3
        || maxEntities > 1) {
      procDividersNeeded = true;
    } else {
      procDividersNeeded = false;
    }
    final int subDividerThickness = someEntityHasSubDividers ? 1 : 0;
    final int entityDividerThickness =
        someEntityHasDividers ? subDividerThickness + 1 : subDividerThickness;
    final int procDividerThickness = procDividersNeeded ? entityDividerThickness + 1 : 0;
    final int expGroupDividerThickness = 5;

    final int numExperiments = expList.size();
    for (int kk = 0; kk < numExperiments; kk++) {
      ExperimentData expData = expList.get(kk);
      final int numEntities = expData.experimentEntities.size();
      for (int jj = 0; jj < numEntities; jj++) {
        int overallEntityNum = rowGeometryByEntity.size();
        EntityData entityData = expData.experimentEntities.get(jj);
        boolean isEntityStart = true;
        boolean isEntityEnd = false;
        int numEntityDataTypes = entityData.entityRows.size();
        List<RowGeometry> entityGeos = new ArrayList();

        boolean needsSubDividers = entityNeedsSubDividers.get(overallEntityNum);
        for (int ii = 0; ii < numEntityDataTypes; ii++) {
          // row within entity (entity's DATA* type)
          RowData rowData = entityData.entityRows.get(ii);
          // For each rowData element, there is one row of data events and N charts
          RowDefinition rowDef = rowData.rowDefinition;
          final int numCharts = rowDef.getChartCount();

          int postRowDividerHeight = 0;
          RowGeometry masterRow = null;
          final int startSubrow = rowGeometryNum;
          for (int subrowNum = 0; subrowNum <= numCharts; subrowNum++) {
            final int chartIdx = subrowNum - 1; // -1 is for "events"

            final int startY = currentY;
            currentY += calcRowUpperPadding(g, rowDef, chartIdx, subrowNum, numCharts);
            final int dataStartY = currentY;
            currentY += calcRowHeight(g, rowDef, chartIdx, subrowNum, numCharts);
            final int dataEndY = currentY - 1;

            boolean isExperimentEnd = false;
            boolean isCompareGroupEnd = false;

            if (subrowNum == numCharts) {
              // last row for metric
              if (ii == numEntityDataTypes - 1) {
                // last row of entity
                isEntityEnd = true;
                isExperimentEnd = (jj == numEntities - 1);
                if (isExperimentEnd) {

                  // determine if isCompareGroupEnd:
                  if (kk == numExperiments - 1 && rowDef.getComparisonOn()) {
                    isCompareGroupEnd = true;
                  } else if (kk < numExperiments - 1) {
                    ExperimentData nextExpData = expList.get(kk + 1);
                    int nextGroupId =
                        nextExpData
                            .experimentEntities
                            .get(0)
                            .entityRows
                            .get(0)
                            .getRowDef()
                            .getExpGroupId();
                    int groupId = rowDef.getExpGroupId();
                    if (groupId != nextGroupId) {
                      isCompareGroupEnd = true;
                    }
                  }

                  if (isCompareGroupEnd) {
                    postRowDividerHeight = expGroupDividerThickness;
                  } else {
                    postRowDividerHeight = procDividerThickness;
                  }
                } else if (entityNeedsDividers.get(overallEntityNum)) {
                  postRowDividerHeight = entityDividerThickness;
                }
              } else {
                // not last row of entity
                if (needsSubDividers) {
                  postRowDividerHeight = subDividerThickness;
                }
              }
            }

            currentY += calcRowLowerPadding(g, rowDef, chartIdx, subrowNum, numCharts);
            final int endY = currentY - 1;

            final String propName, propUName;
            if (chartIdx == -1) {
              propName = "MSTATE"; // YXXX actually, only needed for samples
              propUName = "Process Microstates";
            } else {
              List<PropDescriptor> chartProps = rowDef.getChartProperties();
              propName = chartProps.get(chartIdx).getName();
              propUName = chartProps.get(chartIdx).getDescription();
            }
            RowGeometry subRow =
                new RowGeometry(
                    rowGeometryNum,
                    rowGeometry,
                    numCharts + 1,
                    subrowNum,
                    rowData, // note: rowData is not fully populated yet
                    chartIdx,
                    propName,
                    propUName,
                    startY,
                    endY,
                    dataStartY,
                    dataEndY,
                    isEntityStart,
                    isEntityEnd,
                    isExperimentEnd,
                    isCompareGroupEnd,
                    postRowDividerHeight);
            if (chartIdx == -1) {
              masterRow = subRow;
            }
            rowGeometryNum++;
            rowNumToEntityNum.add(overallEntityNum);
            rowGeometry.add(subRow);
            entityGeos.add(subRow);
            isEntityStart = false;
          } // numCharts
          int tmpRowNum = startSubrow;
          for (int subrowNum = 0; subrowNum <= numCharts; subrowNum++) {
            RowGeometry tmp = rowGeometry.get(tmpRowNum);
            tmp.setMasterRow(masterRow);
            tmpRowNum++;
          }
          currentY += postRowDividerHeight;
        } // data types
        rowGeometryByEntity.add(entityGeos);
      } // entities
    } // experiments

    usedHeight = currentY;

    if (rowGeometryByEntity.size() != overallEntityCount) {
      int xx = 1; // weird!
    }

    // used by coord calculator
    ArrayList<Integer> tmpOffsets = new ArrayList();
    int ypos = 0;
    for (RowGeometry row : rowGeometry) {
      int delta = row.endY - ypos;
      tmpOffsets.add(delta);
      ypos = row.endY;
    }
    rowOffsets = tmpOffsets;

    forceRowGeometry = false;
    forceFullRepaint = true;

    dataAxisMaster.setAbsRowMinHeights(tmpOffsets);
    // YXXX check above call to see if it can cause feedback loop
  }

  private int getBinStartOffset(RowData dataCopy) {
    // Inputs: dataCopy is stable snapshot returned from RowData.grabEventData()
    // Returns: the # of bins from start of data in dataCopy to first bin visible on screen
    //     or INVALID_BIN if no data is available for visible portion of screen.
    // Example:  Suppose the TL view is 1000 pixels wide, and dataCopy has 2000 pixels
    // of valid data with 300 pixels off screen on the left and 700 off screen on the right.
    // The return value for this function would be 300, the first visible data bin.
    int offset = 0;
    if (!dataCopy.eventDataIsValid()) {
      return INVALID_BIN;
    }
    RowDataRequestParams params = dataCopy.getParams();
    if (params == null) {
      return INVALID_BIN; // weird
    }
    long binTime = timeAxisCalculator.getTimePerBin();
    if (binTime != params.binTime) {
      return INVALID_BIN; // zooming, data is stale
    }
    long fetchStart = params.alignedTimeStart;
    long visibleStart = timeAxisCalculator.getTimeStartAligned();
    long delta = visibleStart - fetchStart;
    offset = (int) (delta / binTime);
    if (binTime * offset != delta) {
      return INVALID_BIN; // weird, not aligned
    }
    if (offset < 0) { // data is shifted so far right, there's a hole
      int ii = 0; // for breakpoint
    }
    return offset;
  }

  private void setScrollY(List<RowGeometry> rowGeometry, int scrollY, int visibleHeight) {
    if (currScrollY != scrollY || currVisibleHeight != visibleHeight) {
      currScrollY = scrollY;
      currVisibleHeight = visibleHeight;
      forceFullRepaint = true;
    }
    int rowCount = rowGeometry.size();
    int newRowFirst = rowCount;
    int newRowLast = rowCount;
    for (int ii = 0; ii < rowCount; ii++) { // YXXX slow...
      RowGeometry row = rowGeometry.get(ii);
      int virtualY = row.endY - scrollY;
      if (newRowFirst == rowCount && virtualY > 0) {
        newRowFirst = ii;
      }
      virtualY = row.startY - scrollY;
      if (virtualY > visibleHeight) {
        newRowLast = ii;
        break;
      }
    }
    if (currRowFirst != newRowFirst || currRowLast != newRowLast) {
      currRowFirst = newRowFirst;
      currRowLast = newRowLast;
      forceFullRepaint = true;
    }
  }

  private void drawVisibleRows(
      final Graphics g, final List<RowGeometry> rowGeometry, final int visibleWidth) {
    for (int ii = currRowFirst; ii < currRowLast; ii++) {
      final RowGeometry row = rowGeometry.get(ii);
      final RowData dataCopy = row.rowData.grabEventData(); // row.rowData is volatile, get snapshot
      final int binStartOffset = getBinStartOffset(dataCopy);

      if (binStartOffset == INVALID_BIN) {
        // offset could not be determined
        int rowCenter = (row.dataEndY + row.dataStartY) / 2 - currScrollY;
        drawCenteredMessage(g, AnLocale.getString("Fetching Data..."), visibleWidth / 2, rowCenter);
        continue;
      }

      if (!row.isChartData()) {
        // specifies the events themselves
        GenericEvent[] events = dataCopy.getEvents();
        drawRowEvents(g, row, events, binStartOffset, visibleWidth, false, true);
        continue;
      }

      // specifies a chart
      final Object vals = dataCopy.getChartData(row.chartPropIdx);
      if (vals instanceof long[]) {
        long longVals[] = (long[]) vals;
        Settings.TLData_type dataType = row.rowData.rowDefinition.getTLDataType();
        final boolean preferZeroYOrigin =
            dataType.equals(Settings.TLData_type.TL_HEAPSZ) ? false : true;
        //                final boolean preferZeroYOrigin = true;
        drawRowChartValues(
            g, row, dataCopy, longVals, binStartOffset, visibleWidth, preferZeroYOrigin);
      } else if (vals instanceof SampleEvent[]) {
        SampleEvent sampleEvents[] = (SampleEvent[]) vals;
        drawRowEvents(g, row, sampleEvents, binStartOffset, visibleWidth, false, false);
      } else {
        continue; // weird, vals is unexpected type
      }
    }
  }

  private void drawVisibleDividers(
      final Graphics g, final List<RowGeometry> rowGeometry, final int visibleWidth) {
    int x1 = 0;
    int x2 = visibleWidth;
    if (currScrollY == 0) {
      drawSolidDivider(g, STANDARD_DIVIDER_COLOR, x1, x2, 0, 1);
    }
    for (int ii = currRowFirst; ii < currRowLast; ii++) {
      RowGeometry row = rowGeometry.get(ii);
      if (row.rowDividerHeight > 0) {
        // divider below this row
        int y = row.endY - currScrollY + 1;

        if (row.isCompareGroupEnd) {
          drawDashedDivider(g, COMPARE_GRP_DIVIDER_COLOR, x1, x2, y, row.rowDividerHeight);
        } else if (row.isExperimentEnd) {
          drawSolidDivider(g, EXPERIMENT_DIVIDER_COLOR, x1, x2, y, row.rowDividerHeight);
        } else if (row.isEntityEnd) {
          drawSolidDivider(g, STANDARD_DIVIDER_COLOR, x1, x2, y, row.rowDividerHeight);
        } else {
          drawSolidDivider(g, STANDARD_DIVIDER_COLOR, x1, x2, y, row.rowDividerHeight);
        }
      }
    }
  }

  public static void drawSolidDivider(
      Graphics g, Color color, final int x1, final int x2, final int y, final int thickness) {
    g.setColor(color);
    for (int ii = 0; ii < thickness; ii++) {
      g.drawLine(x1, y + ii, x2, y + ii);
    }
  }

  public static void drawDashedDivider(
      Graphics g, Color color, final int x1, final int x2, int y, int thickness) {
    g.setColor(color);
    if (thickness > 2) {
      int ii = 0;
      g.drawLine(x1, y + ii, x2, y + ii);
      ii += thickness - 1;
      g.drawLine(x1, y + ii, x2, y + ii);
      thickness -= 2;
      y += 1;
    }
    for (int ii = 0; ii < thickness; ii++) {
      int x = x1 - (2 * ii);
      int step = 3;
      final int space = (thickness > 1) ? 6 : 2;
      while (x < x2) {
        if (step > x2 - x) {
          step = x2 - x;
        }
        g.drawLine(x, y + ii, x + step - 1, y + ii);
        x += step + space;
      }
    }
  }

  private void drawRowEvents(
      Graphics g,
      final RowGeometry row,
      final GenericEvent[] events,
      final int binStartOffset,
      final int visibleWidth,
      final boolean drawingSelectedItem,
      final boolean showDivisions) {

    if (events == null) {
      return;
    }
    if (events.length == 0) {
      return;
    }
    final int rowStartY = row.startY - currScrollY;
    final int rowEndY = row.endY - currScrollY;
    final int dataStartY = row.dataStartY - currScrollY;
    final int dataEndY = row.dataEndY - currScrollY;

    final TimelineVariable.StatesDisplayInfo stateInfo;
    if (events[0] instanceof SampleEvent) { // see TLExperiment.getChartProps()
      stateInfo = TimelineVariable.getStateInfo(row.propName);
    } else {
      stateInfo = null;
    }

    boolean forceToSingleBin = false;
    RowDefinition rowDef = row.rowData.rowDefinition;
    if (!row.isChartData() && rowDef.getTLDataType().equals(Settings.TLData_type.TL_CLOCK)) {
      // Hack to show clock profiling callstacks as a point in time
      // ... but show microstate bar for the full duration
      forceToSingleBin = true;
    }

    // repaint row
    int prev_event_x = 0; // remember where previous event ended
    int prev_event_x_tick = -2; // remember previous event tick end
    for (int ii = 0; ii < events.length; ii++) {
      final GenericEvent event = events[ii];
      final int eventbinStart = event.binStart - binStartOffset;
      final int nbins = timeAxisCalculator.getNumBins();

      final int binStart, binCount;
      if (forceToSingleBin && event.binCount > 1) {
        binStart = eventbinStart + event.binCount - 1;
        binCount = 1;
      } else {
        binStart = eventbinStart;
        binCount = event.binCount;
      }

      if (binStart > nbins + FATTENED_EVENT_WIDTH) {
        break;
      }
      if (binStart + binCount < 0) {
        continue;
      }
      if (binCount < 1) {
        continue; // happens when an event end overlaps with an event start, needs fix in DBE
      }

      int w;
      int x;
      int xLeftEdge, xRightEdge;
      {
        int binEnd = binStart + binCount - 1;
        int xStart = timeAxisCalculator.getLowCoordForBin(binStart);
        int xEnd = timeAxisCalculator.getLowCoordForBin(binEnd + 1) - 1;
        w = xEnd - xStart + 1;
        x = xStart;
        xLeftEdge = xStart;
        xRightEdge = xEnd;
      }
      boolean shortTick = true;
      boolean standaloneTick = true;
      if (w < FATTENED_EVENT_WIDTH
          && !row.rowData.rowDefinition.getTLDataType().equals(Settings.TLData_type.TL_GCEVENT)) {
        // attempt to fatten event
        int extension = (FATTENED_EVENT_WIDTH - w) / 2;
        // how much to add to each side

        // widen right side first
        int adjustedW;
        final int next_xStart;
        if (ii + 1 < events.length) {
          // check distance to event on right
          final GenericEvent nextEvent = events[ii + 1];
          final int nextEventbinStart = nextEvent.binStart - binStartOffset;

          final int nextBinStart;
          if (forceToSingleBin && nextEvent.binCount > 1) {
            nextBinStart = nextEventbinStart + nextEvent.binCount - 1;
          } else {
            nextBinStart = nextEventbinStart;
          }

          next_xStart = timeAxisCalculator.getLowCoordForBin(nextBinStart);
          int avail = (next_xStart - (x + w)) / 2;
          if (avail < 0) {
            // overlapping events
            // note: intentinally not merged with next case
            adjustedW = w + avail; // shorten this event
            if (adjustedW < 1) {
              adjustedW = 1; // weird, not expected
            }
          } else if (avail < extension) {
            // not overlapping, but cramped
            adjustedW = w + avail;
          } else {
            // enough space for full expansion
            adjustedW = w + extension;
          }
        } else {
          adjustedW = w + extension;
          next_xStart = -1;
        }

        // now widen left side
        int adjustedX;
        if (prev_event_x < x - extension) {
          adjustedX = x - extension;
        } else {
          adjustedX = prev_event_x + 1;
        }
        adjustedW += x - adjustedX;

        // make tick marks of different heights (visual cue to zoom to see details)
        if (xLeftEdge == next_xStart - 1 && xLeftEdge == prev_event_x_tick + 1) {
          // touching both previous and next ticks
          //                    if(event.binNSamples>1){
          // there are hidden events
          if ((xLeftEdge % 4) == 0) {
            // add special jagged edge
            // only use tall ticks for every third adjacent tick
            shortTick = false;
            //                        }
          }
        }
        if (xLeftEdge == next_xStart - 1 || xLeftEdge == prev_event_x_tick + 1) {
          standaloneTick = false;
        }

        w = adjustedW;
        x = adjustedX;
      }
      int x2 = x + w - 1;
      prev_event_x = x2;
      prev_event_x_tick = xRightEdge;

      if (event instanceof SampleEvent) { // YXXX how inefficient is this?
        SampleEvent sampleEvt = (SampleEvent) event;
        drawSampleEvent(
            g,
            sampleEvt.propValues,
            stateInfo,
            x,
            x2,
            dataStartY,
            dataEndY,
            drawingSelectedItem,
            showDivisions);
      } else if (event instanceof StackEvent) {
        StackEvent stackEvt = (StackEvent) event;
        drawStackEvent(
            g,
            stackEvt,
            x,
            x2,
            dataStartY,
            dataEndY,
            xLeftEdge,
            xRightEdge,
            drawingSelectedItem,
            shortTick,
            standaloneTick);
      } else if (event
          instanceof GenericEvent) { // CXXX Bug 20801848 - maybe add a function drawGCEvent()
        drawGenericEvent(g, x, x2, dataStartY, dataEndY, drawingSelectedItem, showDivisions);
      } else {
        int foo = 0; // weird
      }
    }
    if (!drawingSelectedItem) {
      drawRowVBorders(g, rowStartY, rowEndY, -1, -1);
    }
  }

  private int[] getRowVisibleBinMinMax(final RowData dataCopy) {
    // return minimum and maximum bin for valid data
    // if no valid data is visible, returns (Integer.MAX_VALUE, Integer.MIN_VALUE)
    final int[] minMaxNA = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    if (dataCopy == null || !dataCopy.eventDataIsValid()) {
      return minMaxNA;
    }
    int lowBinLimit = 0;
    int highBinLimit = timeAxisCalculator.getNumBins() - 1;
    { // limit chart to experiment's time boundaries
      final long timeOffset = dataCopy.rowDefinition.getTimeOrigin();
      final long wallTimeStart = dataCopy.rowDefinition.getDataTimeStart() - timeOffset;
      final long wallTimeEnd = dataCopy.rowDefinition.getDataTimeEnd() - timeOffset;
      final long visibleStart = timeAxisCalculator.getTimeStart();
      final long visibleEnd = timeAxisCalculator.getTimeEnd();
      if (wallTimeStart > visibleStart) {
        if (wallTimeStart > visibleEnd) {
          return minMaxNA;
        }
        lowBinLimit = timeAxisCalculator.getBinAtTime(wallTimeStart);
      }
      if (wallTimeEnd < visibleEnd) {
        if (wallTimeEnd < visibleStart) {
          return minMaxNA;
        }
        highBinLimit = timeAxisCalculator.getBinAtTime(wallTimeEnd);
      }
    }
    int[] loHi = {lowBinLimit, highBinLimit};
    return loHi;
  }

  private void drawRowChartValues(
      Graphics g,
      final RowGeometry row,
      final RowData dataCopy,
      final long longVals[],
      int binStartOffset,
      final int visibleWidth,
      final boolean preferZeroYOrigin) {
    final int rowStartY = row.startY - currScrollY;
    final int rowEndY = row.endY - currScrollY;
    final int dataStartY = row.dataStartY - currScrollY;
    final int dataEndY = row.dataEndY - currScrollY;
    final int rowHeight = dataEndY - dataStartY + 1;

    final int[] binMinMax = getRowVisibleBinMinMax(dataCopy);
    final int lowBinLimit = binMinMax[0];
    final int highBinLimit = binMinMax[1] + 1; // +1 is fudge for missing pixel at some zooms

    int borderHeight = rowHeight < 6 ? 0 : 1;
    final int yOrigin = dataEndY - borderHeight; // -1 for bottom border
    final int lineThickness = rowHeight < 6 ? 1 : 2;
    final int dataHeight = rowHeight - 2 * borderHeight - (lineThickness - 1); // -1 for highlight

    // compute vertical scale
    long min_val = Long.MAX_VALUE;
    long max_val = Long.MIN_VALUE;
    int startBin = binStartOffset >= 0 ? binStartOffset : 0;
    for (int bin = startBin; bin < longVals.length; bin++) {
      final int visibleBin = bin - binStartOffset;
      if (visibleBin > highBinLimit) {
        break;
      }
      if (visibleBin < lowBinLimit) {
        continue;
      }
      // only consider visible time when computing y range
      long val = longVals[bin];
      if (min_val > val) {
        min_val = val;
      }
      if (max_val < val) {
        max_val = val;
      }
    }
    if (min_val > 0 && preferZeroYOrigin) {
      min_val = 0; // zero Y origin unless minimum is negative
    }

    if (min_val == Long.MAX_VALUE) {
      min_val = 0;
    }
    if (max_val == Long.MIN_VALUE) {
      max_val = 0;
    }
    final long range = max_val - min_val;
    final double scale;
    if (range == 0) {
      scale = 0;
    } else {
      scale = (double) (dataHeight - lineThickness) / range;
    }

    // fill background
    {
      int xMargin = timeAxisCalculator.getMargin();
      int x1 = xMargin;
      int x2 = visibleWidth - xMargin;
      drawHorizontalBand(
          g, x1, dataStartY, x2, dataEndY, CHART_BORDER_COLOR, CHART_BACKGROUND_COLOR);
      drawCaliper(g, dataStartY + 1, dataEndY - 1, CHART_BACKGROUND_SEL_COLOR);
    }

    // draw line
    g.setColor(CHART_GRAPH_COLOR);
    int peakStart = -1;
    int peakY = 0;
    int prevX = 0;
    int prevY = 0;
    long prevVal = 0;
    boolean firstTime = true;
    boolean lastTime = false;
    for (int bin = startBin; bin < longVals.length; bin++) {
      final int visibleBin = bin - binStartOffset;
      if (visibleBin == highBinLimit || bin == longVals.length - 1) {
        lastTime = true;
      }
      if (visibleBin > highBinLimit) {
        break;
      }
      if (visibleBin < lowBinLimit) {
        continue;
      }
      // calc vertical
      final Long curVal = longVals[bin] - min_val;
      final int h = (int) Math.round(scale * curVal);
      final int currY = yOrigin - h; // just above top of grey bar

      // calc horizonal
      final int x1 = timeAxisCalculator.getLowCoordForBin(visibleBin);
      final int x2 = timeAxisCalculator.getLowCoordForBin(visibleBin + 1) - 1;
      int centerX = (x1 + x2) / 2;

      if (firstTime) {
        prevY = currY;
        prevVal = curVal;
        prevX = x1;
        firstTime = false;
      }

      if (dataHeight > 3) {
        g.drawLine(prevX, prevY, centerX, currY);
        if (lineThickness != 1) {
          if (!(curVal == 0 && prevVal == 0)) {
            // only show non-baseline values thicker
            g.drawLine(prevX, prevY - 1, centerX, currY - 1);
          }
        }
      } else {
        if (!(curVal == 0 && prevVal == 0)) {
          // only show non-baseline values
          g.drawLine(prevX, prevY, centerX, currY);
        }
      }

      // highlight peaks
      boolean isPeak = (max_val == longVals[bin] && max_val != min_val);
      if (max_val == 1) {
        // don't highlight bins with just one event
        // note: won't work if values are based on HWC counts or time.
        isPeak = false;
      }
      if (isPeak) {
        if (peakStart == -1) {
          peakStart = centerX;
        }
        peakY = currY;
      }
      if (peakStart != -1 && (!isPeak || lastTime)) {
        // we know the end of the peak; let's draw the highlight
        int peakEnd = centerX;
        if (!isPeak) {
          peakEnd--; // we passed the peak
        }
        int delta = peakEnd - peakStart;
        final int min_peak_width = 4;
        if (delta < min_peak_width) {
          int fatten = (min_peak_width - delta + 1) / 2;
          peakEnd += fatten;
          peakStart -= fatten;
        }
        g.setColor(CHART_GRAPH_COLOR_MAX);
        int yOffset = peakY - lineThickness - borderHeight + 1;
        g.drawLine(peakStart, yOffset, peakEnd, yOffset);
        if (lineThickness != 1) {
          g.drawLine(peakStart, yOffset - 1, peakEnd, yOffset - 1);
        }
        g.setColor(CHART_GRAPH_COLOR);
        peakStart = -1;
      }
      prevY = currY;
      prevVal = curVal;
      prevX = centerX;
    }

    FontInfo title_font = getSmallFont(g);
    FontInfo value_font = getSmallBoldFont(g);
    if (title_font != null
        && value_font != null
        && value_font.height - 3 <= dataStartY - rowStartY) {
      // add Labels
      int xMargin = timeAxisCalculator.getMargin();
      int xMax = timeAxisCalculator.getCanvasPixels() - xMargin;
      int xMin = xMargin;
      int textStartY = dataStartY - 1 - title_font.height + title_font.ascent;

      final String chartTitle = row.propUName;
      int chartTitleWidth = stringWidth(g, title_font.font, chartTitle);

      String yValue = getString(max_val);
      int yValueWidth = stringWidth(g, value_font.font, yValue);

      // Chart Title
      if (chartTitleWidth + yValueWidth < xMax - xMin - 10) {
        g.setColor(Color.BLACK);
        g.setFont(title_font.font);
        g.drawString(chartTitle, xMin + 1, textStartY);
      }

      // Y low and high values
      g.setColor(CHART_NUMERIC_TEXT_COLOR);
      g.setFont(value_font.font);
      if (yValueWidth < xMax - xMin) {
        g.drawString(yValue, xMax - yValueWidth - 1, textStartY);
      }
      if (value_font.height + 2 < dataHeight) {
        yValue = getString(min_val);
        yValueWidth = stringWidth(g, value_font.font, yValue);
        textStartY = yOrigin - value_font.height + value_font.ascent;
        g.drawString(yValue, xMax - yValueWidth - 1, textStartY);
      }
    }

    drawRowVBorders(g, rowStartY, rowEndY, dataStartY, dataEndY);
  }

  private void drawHorizontalBand(
      final Graphics g,
      final int x1,
      final int y1,
      final int x2,
      final int y2,
      final Color border,
      final Color fill) {
    g.setColor(fill);
    g.fillRect(x1, y1, x2 - x1, y2 - y1);
    g.setColor(border);
    g.drawLine(x1, y1, x2, y1);
    g.drawLine(x1, y2, x2, y2);
    g.setColor(Color.BLACK);
  }

  private String getString(long val) {
    Long lval = new Long(val);
    return lval.toString();
  }

  private int stringWidth(final Graphics g, Font font, String stmp) {
    int strPixels = g.getFontMetrics(font).stringWidth(stmp);
    return strPixels;
  }

  private void drawStackEvent(
      Graphics g,
      final StackEvent thisEvent,
      final int startX,
      final int endX,
      final int startY,
      final int endY,
      final int xLeftEdge,
      final int xRightEdge,
      final boolean doSetSelected,
      boolean shortTick,
      boolean standaloneTick) {
    final int x = startX;
    final int wid = endX - startX + 1;
    final int height = endY - startY + 1;
    if (doSetSelected && height < 1) {
      setSelectionBounds(startX, startY, wid, height);
      return;
    }
    if (height < 1) {
      return;
    }

    final int nominalSegmentHeight = stack_frame_pixels;

    // check user's "cpu idle color" setting
    boolean useIdleColor = false;
    if (idle_color_set && thisEvent.mState > 1) { // YXXX yuck!
      if (idle_color == null) {
        return;
      }
      useIdleColor = true;
    }

    // determine stack alignment based on user settings
    final int numFrames;
    final int startFrame;
    final int frameInc;
    final int anchorY;
    final int tickY;
    final int topEdgeOfStack;
    final long[] stackFuncs = tl2DataFetcher.getStackFuncArray(thisEvent.stackId);

    final int tick_height = height >= EVENT_TICK_HEIGHT ? EVENT_TICK_HEIGHT : height;
    final int nominalHeight = tick_height + stack_frame_pixels * stackFuncs.length;
    final boolean truncated = nominalHeight > (endY - startY + 1);
    final int framesThatFit = (height - tick_height + stack_frame_pixels - 1) / stack_frame_pixels;

    if (stackFuncs.length > framesThatFit) {
      numFrames = framesThatFit;
    } else {
      numFrames = stackFuncs.length;
    }
    if (stack_align == Settings.TLStack_align.TLSTACK_ALIGN_ROOT) {
      startFrame = stackFuncs.length - 1; // main
      frameInc = -1;
      anchorY = 1 + endY - tick_height - nominalSegmentHeight;
      tickY = 1 + endY - tick_height;
      int _topEdgeOfStack = 1 + endY - tick_height - numFrames * nominalSegmentHeight;
      if (_topEdgeOfStack < startY) {
        topEdgeOfStack = startY;
      } else {
        topEdgeOfStack = _topEdgeOfStack;
      }
    } else {
      startFrame = 0; // leaf
      frameInc = 1;
      anchorY = startY + tick_height;
      tickY = startY;
      topEdgeOfStack = anchorY;
    }

    // draw stack boundary tick marks
    if (standaloneTick) {
      g.setColor(TICK_ONE_PIXEL_WIDE_COLOR);
    } else if (numFrames > 0) {
      g.setColor(TICK_WITH_STACK_COLOR);
    } else {
      g.setColor(TICK_STANDALONE_COLOR);
    }
    if (tick_height < 2) {
      // row is squashed to very few pixels, widen event to make it visible
      if (xLeftEdge != xRightEdge) {
        g.drawLine(xRightEdge - 1, tickY, xRightEdge, tickY);
      }
      g.drawLine(xLeftEdge - 1, tickY, xLeftEdge, tickY);
    } else {
      int tickYStart = tickY;
      int tickYEnd = tickY + tick_height - 1;
      if (shortTick && tick_height > 2) {
        tickYEnd -= 1;
      }
      if (xLeftEdge != xRightEdge) {
        g.drawLine(xRightEdge, tickYStart, xRightEdge, tickYEnd);
      }
      g.drawLine(xLeftEdge, tickYStart, xLeftEdge, tickYEnd);
      //            if (thisEvent.binNSamples > 1) {
      //                g.drawLine(xLeftEdge - 1, tickYStart, xLeftEdge - 1, tickYEnd);
      //            }
    }

    // draw the stack
    Color color = idle_color;
    int stackHeight = 0;
    for (int ii = 0, frame = startFrame; ii < numFrames; ii++, frame += frameInc) {
      int segmentHeight = nominalSegmentHeight;
      if (!useIdleColor) {
        if (doSetSelected || stateColorSnapshot == null) {
          // selected objects are fetched later, color may not be in stateColorSnapshot
          color = colorMap.getFuncColor(stackFuncs[frame]); // slower, requires lock
        } else {
          color = stateColorSnapshot.getColorMap().get(stackFuncs[frame]);
        }
        if (color == null) {
          color = Color.LIGHT_GRAY;
        }
      }
      int yPos = anchorY + ii * segmentHeight * frameInc;
      if (yPos < startY) {
        segmentHeight -= (startY - yPos);
        yPos = startY;
      }
      int tmpEndY = yPos + segmentHeight - 1;
      if (tmpEndY > endY) {
        segmentHeight -= (tmpEndY - endY);
      }
      if (segmentHeight <= 0) {
        continue; // weird
      }
      stackHeight += segmentHeight;
      g.setColor(color);
      g.fillRect(x, yPos, wid, segmentHeight);
      if (ii == numFrames - 1 && truncated) {
        int y = (frameInc > 0) ? yPos + segmentHeight - 1 : yPos;
        Color darker = color.darker();
        g.setColor(darker);
        g.drawLine(x, y, x + wid - 1, y);
      }
    }
    if (doSetSelected) {
      setSelectionBounds(x, topEdgeOfStack, wid, stackHeight);
    }
  }

  private void drawSampleEvent(
      final Graphics g,
      final long stateVals[],
      TimelineVariable.StatesDisplayInfo stateInfo,
      final int startX,
      final int endX,
      final int startY,
      final int endY,
      final boolean doSetSelected,
      boolean showDivisions) {
    final long propValues[] = stateInfo.raw2displayValues(stateVals);
    if (propValues == null || propValues.length == 0) {
      return;
    }
    final int event_w = endX - startX + 1;
    final int sampleHeight = endY - startY + 1;

    // calc total
    double total = 0;
    for (long val : propValues) {
      total += val;
    }
    if (total == 0) {
      return;
    }

    final int x = startX;
    final int w = event_w;

    // draw sample colors
    long sum_time = 0;
    int yPrev = startY + SAMPLE_BORDER_HEIGHT;
    int dataHeight = sampleHeight - SAMPLE_BORDER_HEIGHT * 2;

    for (int i = 0; i < propValues.length; i++) {
      sum_time += propValues[i];
      double percent = sum_time / total;
      int height = (int) Math.round(percent * dataHeight);
      int yNew = startY + height + SAMPLE_BORDER_HEIGHT;
      if (yNew > yPrev) {
        final Color color = stateInfo.getColorByDisplayIdx(i);
        g.setColor(color);
        g.fillRect(x, yPrev, w, yNew - yPrev);
      }
      yPrev = yNew;
    }

    // vertical dividers
    if (showDivisions && w >= MIN_SAMPLE_3D_WIDTH) {
      final int y1 = startY + sampleHeight - 1 - SAMPLE_BORDER_HEIGHT;
      final int y2 = startY + SAMPLE_BORDER_HEIGHT;
      final int x1 = x;
      final int x2 = x + w - 1;
      // g.setColor(Color.LIGHT_GRAY);
      // g.drawLine(x1, y1, x1, y2);
      g.setColor(SAMPLE_VDIVIDER_COLOR);
      g.drawLine(x2, y1, x2, y2);
    }

    if (SAMPLE_BORDER_HEIGHT > 0 && SAMPLE_BORDER_COLOR != null) {
      final int y1 = startY + sampleHeight - 1;
      final int y2 = startY;
      final int x1 = x;
      final int x2 = x + w - 1;
      g.setColor(SAMPLE_BORDER_COLOR);
      g.drawLine(x1, y1, x2, y1);
      g.drawLine(x1, y2, x2, y2);
    }

    if (doSetSelected) {
      setSelectionBounds(x, startY, w, sampleHeight);
    }
  }

  // CXXX Bug 20801848 - maybe change to drawGCEvent()
  private void drawGenericEvent(
      final Graphics g,
      final int startX,
      final int endX,
      final int startY,
      final int endY,
      final boolean doSetSelected,
      boolean showDivisions) {
    final int event_w = endX - startX + 1;
    final int sampleHeight = endY - startY + 1;

    final int x = startX;
    final int w = event_w;

    int yPrev = startY + SAMPLE_BORDER_HEIGHT;
    int dataHeight = sampleHeight - SAMPLE_BORDER_HEIGHT * 2;

    g.setColor(Color.GRAY);
    int yNew = startY + dataHeight + SAMPLE_BORDER_HEIGHT;
    g.fillRect(x, yPrev, w, yNew - yPrev);

    // vertical dividers
    if (showDivisions && w >= MIN_SAMPLE_3D_WIDTH) {
      final int y1 = startY + sampleHeight - 1 - SAMPLE_BORDER_HEIGHT;
      final int y2 = startY + SAMPLE_BORDER_HEIGHT;
      final int x1 = x;
      final int x2 = x + w - 1;
      // g.setColor(Color.LIGHT_GRAY);
      // g.drawLine(x1, y1, x1, y2);
      g.setColor(SAMPLE_VDIVIDER_COLOR);
      g.drawLine(x2, y1, x2, y2);
    }

    if (SAMPLE_BORDER_HEIGHT > 0 && SAMPLE_BORDER_COLOR != null) {
      final int y1 = startY + sampleHeight - 1;
      final int y2 = startY;
      final int x1 = x;
      final int x2 = x + w - 1;
      g.setColor(SAMPLE_BORDER_COLOR);
      g.drawLine(x1, y1, x2, y1);
      g.drawLine(x1, y2, x2, y2);
    }

    if (doSetSelected) {
      setSelectionBounds(x, startY, w, sampleHeight);
    }
  }
  //     private String getDescription(final MouseEvent evt) { //YXXX unused
  //        final RowGeometry           cachedRow;
  //        cachedRow = screenY2RowGeometry(evt.getY());
  //	if ( cachedRow == null ){
  //	    return null;
  //        }
  //	return cachedRow.rowData.rowDefinition.getLongDescription();
  //    }

  public int getVZoomLevel() {
    return vzoom_level;
  }

  public int getStackFramePixels() {
    return stack_frame_pixels;
  }

  public Settings.TLStack_align getStackAlign() {
    return stack_align;
  }

  public long getAbsoluteTimeStart() {
    if (recentDataSnapshot == null) {
      return 0;
    }
    return recentDataSnapshot.getAbsoluteTimeStart();
  }

  public long getAbsoluteTimeEnd() {
    if (recentDataSnapshot == null) {
      return 0;
    }
    return recentDataSnapshot.getAbsoluteTimeEnd();
  }

  public boolean selectionActive() {
    if (selection_event == null) {
      return false;
    }
    return true;
  }

  public long getSelectionTimeEnd() {
    if (selection_event == null) {
      return 0;
    }
    return selection_event.eventTimeEnd;
  }

  public long getSelectionTimeStart() {
    if (selection_event == null) {
      return 0;
    }
    return selection_event.eventTimeStart;
  }

  public double getSelectionYCenter() {
    if (selection_event == null) {
      return 0;
    }
    int ycoord = dataAxisCalculator.getCenterCoordForRow(selection_event.rowNum);
    double selection_ypct_center = dataAxisCalculator.getPercentNearCoord(ycoord);
    return selection_ypct_center;
  }

  public class FindNearXYResult {
    public final RowGeometry rowGeometry;
    public final long clickTime;
    public final GenericEvent event;
    public final Object chartData; // YXXX would prefer to use GenericEvent as type
    public final boolean directTimeHit;
    public final int x, y;

    public FindNearXYResult(
        int x,
        int y,
        RowGeometry rowGeometry,
        long clickTime,
        GenericEvent event,
        Object chartData,
        boolean directTimeHit) {
      this.x = x;
      this.y = y;
      this.rowGeometry = rowGeometry;
      this.clickTime = clickTime;
      this.event = event;
      this.chartData = chartData;
      this.directTimeHit = directTimeHit;
    }
  }

  public FindNearXYResult findNearXY(
      final int xx, final int yy, final boolean searchOnlyMasterRows) {
    // xx: either Integer.MIN_VALUE to use previous selection click time, or x coordinate
    final TimelineSelectionEvent recentSel = getRecentSelection();
    final int y;
    final int rowNum;
    if (yy == Integer.MIN_VALUE) {
      if (recentSel != null) {
        rowNum = recentSel.rowNum;
      } else {
        rowNum = dataAxisCalculator.getRowStart();
      }
      y = dataAxisCalculator.getCenterCoordForRow(rowNum);
    } else {
      y = yy;
      rowNum = dataAxisCalculator.getRowAtCoord(y);
    }
    RowGeometry rowGeometry = rowNum2RowGeometry(rowNum);
    if (rowGeometry == null) {
      return null;
    }
    final RowGeometry row;
    if (searchOnlyMasterRows) {
      // redirect any clicks on charts to master row
      row = rowGeometry.getMasterRow();
    } else {
      row = rowGeometry;
    }

    final long clickTime;
    final int x;
    boolean inMargin = false;
    if (xx == Integer.MIN_VALUE) {
      if (recentSel != null) {
        clickTime = recentSel.clickTime;
      } else {
        clickTime = timeAxisCalculator.getTimeStart();
      }
      x = timeAxisCalculator.getCoordAtTime(clickTime);
    } else {
      x = xx;
      int margin = timeAxisCalculator.getMargin();
      int availWidth = timeAxisCalculator.getAvailPixels();
      if (x < margin || x > availWidth + margin) {
        inMargin = true;
      }
      clickTime = timeAxisCalculator.getTimeNearCoord(x);
    }
    final RowData dataCopy = row.rowData.grabEventData(); // row.rowData is volatile, get snapshot
    Object chartData = null;
    if (row.isChartData()) {
      final Object vals = dataCopy.getChartData(row.chartPropIdx);
      do {
        // chart events are based on pixel-sized bins (not event time info)
        final int binStartOffset = getBinStartOffset(dataCopy);
        if (binStartOffset == INVALID_BIN) {
          break; // weird
        }
        final int clickBin = timeAxisCalculator.getBinAtTime(clickTime);
        if (vals instanceof SampleEvent[]) {
          // SampleEvent[]-style data is sparse; only bins with data are stored
          final SampleEvent[] events = (SampleEvent[]) vals;
          if (events.length == 0) {
            break;
          }
          final int virtualDataBin = clickBin + binStartOffset;
          final int srchIdx = searchEventsByBin(events, virtualDataBin);
          final SampleEvent event = events[srchIdx];
          final int binEnd = event.binStart + event.binCount - 1;
          if (event.binStart > virtualDataBin || binEnd < virtualDataBin) {
            break; // not in data set
          }
          chartData = event;
        } else if (vals instanceof long[]) {
          // long[]-style data has a value for every bin
          final int[] binMinMax = getRowVisibleBinMinMax(dataCopy);
          final int lowBinLimit = binMinMax[0];
          final int highBinLimit = binMinMax[1];
          if (clickBin < lowBinLimit || clickBin > highBinLimit) {
            break; // click is outside of chart's valid data
          }
          final int dataBin = clickBin + binStartOffset;
          final long[] events = (long[]) vals;
          if (dataBin < 0 || dataBin >= events.length) {
            break; // weird
          }
          chartData = events[dataBin];
        } else {
          int ii = 0; // weird, for breakpoint
          break;
        }
      } while (false);
    }

    GenericEvent[] events = dataCopy.getEvents();
    if (!dataCopy.eventDataIsValid() || events == null || events.length == 0) {
      return new FindNearXYResult(x, y, row, clickTime, null, chartData, false);
    }

    // The events are ordered by timeEnd, but they might overlap
    // See Dbe.cc dbeGetTLEventIdxNearTime() for algorithm.

    // We'll do a binary search of visible (painted) events.
    // If searchEventsByTime() can't find an exact match, the result
    // will be after clickTime except if it is the last item.
    final int srchIdx = searchEventsByTime(events, clickTime);
    final long srchEndTime = events[srchIdx].timeEnd;
    int winner = -1;
    final long winnerDelta;
    do {
      if (clickTime == srchEndTime) { // exact
        winner = srchIdx;
        winnerDelta = 0;
        break;
      }

      final long srchTimeStart = events[srchIdx].timeStart;
      final long srchTimeDelta = distanceToEvent(clickTime, srchTimeStart, srchEndTime);
      if (srchEndTime < clickTime // hit rightmost max
          || srchIdx == 0) { // hit leftmost max
        winner = srchIdx;
        winnerDelta = srchTimeDelta;
        break;
      }

      // need to choose
      final int prevIdx = srchIdx - 1;
      final long prevTimeDelta =
          distanceToEvent(clickTime, events[prevIdx].timeStart, events[prevIdx].timeEnd);
      if (srchTimeDelta < prevTimeDelta) {
        winner = srchIdx;
        winnerDelta = srchTimeDelta;
        break;
      } else {
        winner = prevIdx;
        winnerDelta = prevTimeDelta;
        break;
      }
    } while (false);

    final GenericEvent event = events[winner];
    final long timeTolerance = timeAxisCalculator.getTimePerBin() * CLICK_PIXEL_TOLERANCE;
    final boolean directTimeHit = !inMargin && (winnerDelta < timeTolerance);
    return new FindNearXYResult(x, y, row, clickTime, event, chartData, directTimeHit);
  }

  // binary search.  If no exact match, the result's timeEnd
  // will be greater than clickTime unless the result is the last item.
  private int searchEventsByTime(GenericEvent[] events, long clickTime) {
    int lo = 0;
    int hi = events.length - 1;
    while (lo < hi) {
      int md = (hi + lo) / 2;
      if (clickTime > events[md].timeEnd) {
        lo = md + 1;
      } else {
        hi = md;
      }
    }
    return lo;
  }

  // binary search.  If no exact match, the result's timeEnd
  // will be greater than clickTime unless the result is the last item.
  private int searchEventsByBin(GenericEvent[] events, int clickBin) {
    int lo = 0;
    int hi = events.length - 1;
    while (lo < hi) {
      int md = (hi + lo) / 2;
      int binEnd = events[md].binStart + events[md].binCount - 1;
      if (clickBin > binEnd) {
        lo = md + 1;
      } else {
        hi = md;
      }
    }
    return lo;
  }

  private long distanceToEvent(long refTime, long eventStart, long eventEnd) {
    if (refTime < eventStart) {
      return eventStart - refTime;
    }
    if (eventEnd < refTime) {
      return refTime - eventEnd;
    }
    return 0;
  }

  // ===== text dump of timeline data for debugging and automated GUI testing =====

  public String dumpXY(final FindNearXYResult result) {
    if (result == null) {
      return "null";
    }
    StringBuilder buf = new StringBuilder();
    final RowGeometry row = result.rowGeometry;
    final RowDefinition rowDef = row.rowData.rowDefinition;
    buf.append("Timeline Data Near (x=" + result.x + ", y=" + result.y + "):");
    buf.append(" rowNum=" + row.rowNum);
    buf.append(" clickTime=" + result.clickTime / NS_PER_SEC);
    buf.append(" directTimeHit=" + result.directTimeHit);
    buf.append("\n");
    buf.append(" rowLabel=" + dumpRowInfo(row));
    buf.append(
        " procTimeRange=[" + (rowDef.getDataTimeStart() - rowDef.getTimeOrigin()) / NS_PER_SEC);
    buf.append(", " + (rowDef.getDataTimeEnd() - rowDef.getTimeOrigin()) / NS_PER_SEC + "]");
    buf.append("\n");
    buf.append(" rowToolTip='" + rowDef.getLongDescription() + "'");
    buf.append(dumpGenericEvent(result.event, row));
    if (row.isChartData()) {
      buf.append("\n Chart='" + row.propUName + "'");
      if (result.chartData != null) {
        if (result.chartData instanceof StackEvent) {
          buf.append("Unexected Chart Data");
        } else if (result.chartData instanceof SampleEvent) {
          final SampleEvent event = (SampleEvent) result.chartData;
          buf.append(dumpSampleEvent(event, row.propName));
        } else if (result.chartData instanceof Long) {
          final Long val = (Long) result.chartData;
          buf.append(" Value=" + val);
        } else {
          buf.append("UNKNOWN Chart Data!");
        }
      }
    }
    buf.append("\n");
    return buf.toString();
  }

  private String dumpGenericEvent(GenericEvent evt, RowGeometry row) {
    if (evt != null) {
      StringBuilder buf = new StringBuilder();
      buf.append("\n Event: ");
      buf.append(evt.toString());
      if (evt instanceof SampleEvent) {
        buf.append("\n Sample:");
        final SampleEvent event = (SampleEvent) evt;
        final String propName = row.getMasterRow().propName;
        buf.append(dumpSampleEvent(event, propName));
      }
      return buf.toString();
    }
    return "";
  }

  private String dumpSampleEvent(final SampleEvent sampleEvent, final String propName) {
    if (sampleEvent == null) {
      return "";
    }
    StringBuilder buf = new StringBuilder();

    final TimelineVariable.StatesDisplayInfo stateInfo;
    stateInfo = TimelineVariable.getStateInfo(propName);
    if (stateInfo.statePropName.isEmpty()) {
      int ii = 0; // weird
      return "";
    }

    final long stateVals[] = stateInfo.raw2displayValues(sampleEvent.propValues);
    long total = 0;
    for (int ii = 0; ii < stateVals.length; ii++) {
      total += stateVals[ii];
    }
    final long binDuration = sampleEvent.timeEnd - sampleEvent.timeStart + 1;
    if (binDuration > 0) {
      final double nthreads = 1.0 * total / binDuration;
      buf.append(String.format(" nthreads=%.1f", nthreads));
    } else {
      buf.append("<unknown sample duration>");
    }
    buf.append(" (totalSampleTime=" + total / NS_PER_SEC + ")");
    for (int ii = 0; ii < stateVals.length; ii++) {
      if (stateVals[ii] == 0) {
        continue;
      }
      final String state = stateInfo.getNameByDisplayIdx(ii);
      final double percent = 100.0 * stateVals[ii] / total;
      buf.append(String.format("\n  %.2f%%", percent));
      buf.append(" state='" + state + "'");
      buf.append(" color=" + stateInfo.getColorByDisplayIdx(ii));
    }
    return buf.toString();
  }

  /**
   * @param limit limit in number of lines. 0 means all lines.
   * @return formatted text
   */
  public String exportAsText(Integer limit) {
    TL2DataSnapshot dataSnapshot = recentDataSnapshot;
    if (dataSnapshot == null || dataSnapshot.getRowData().isEmpty()) {
      final String message;
      if (dataSnapshot == null) {
        message = AnLocale.getString("Experiment load in progress...");
      } else {
        message = AnLocale.getString("Data not available for this filter selection");
      }
      return message;
    }
    StringBuilder buf = new StringBuilder();
    GenericEvent gevent = getVisibleSelection();
    buf.append("\n============ TIMELINE DUMP ============");
    buf.append("\n=== Selection ===");
    if (gevent != null) {
      RowGeometry selRow = rowGeometry.get(selection_event.rowNum);
      buf.append("\n Timeline " + dumpRowInfo(selRow));
      buf.append(dumpGenericEvent(gevent, selRow));
    } else {
      buf.append("\n <no event selected>");
    }
    if (caliper != null) {
      buf.append(
          "\n Time selection range: [" + caliper.getLowTime() + ", " + caliper.getHighTime() + "]");
    }
    if (rowGeometry == null) {
      buf.append("\nWARNING: timeline panel has not been drawn yet");
      return buf.toString();
    }
    int nRows = rowGeometry.size();
    if (currRowFirst > nRows || currRowLast > nRows) {
      buf.append("\nWARNING: timeline data row count is inconsistent");
      return buf.toString();
    }
    for (int ii = currRowFirst; ii < currRowLast; ii++) {
      final RowGeometry row = rowGeometry.get(ii);
      final RowData dataCopy = row.rowData.grabEventData(); // row.rowData is volatile, get snapshot
      final int binStartOffset = getBinStartOffset(dataCopy);
      buf.append("\n=== Timeline " + dumpRowInfo(row) + " ===");
      if (binStartOffset == INVALID_BIN) {
        buf.append("\n Fetching Data...");
        continue;
      }
      if (!row.isChartData()) {
        // specifies the events themselves
        GenericEvent[] events = dataCopy.getEvents();
        buf.append(dumpRowEvents(row, events, binStartOffset));
        continue;
      }

      // specifies a chart
      final Object vals = dataCopy.getChartData(row.chartPropIdx);
      if (vals instanceof long[]) {
        long longVals[] = (long[]) vals;
        buf.append(dumpRowChartValues(dataCopy, longVals, binStartOffset));
      } else if (vals instanceof SampleEvent[]) {
        SampleEvent sampleEvents[] = (SampleEvent[]) vals;
        buf.append(dumpRowEvents(row, sampleEvents, binStartOffset));
      } else {
        buf.append("\nERROR: unexpected data type");
        continue; // weird, vals is unexpected type
      }
    }
    return buf.toString();
  }

  private String dumpRowInfo(RowGeometry row) {
    final RowDefinition rowDef = row.rowData.rowDefinition;
    StringBuilder buf = new StringBuilder();
    buf.append(
        "'"
            + rowDef.getShortLabel()
            + "'"
            //                + " " + rowDef.getExpName()
            + " "
            + rowDef.getDataDescUName()
            + " ");
    if (row.subrowNum == 0) {
      buf.append("Events");
    } else {
      buf.append(row.propUName);
    }
    return buf.toString();
  }

  private String dumpRowEvents(
      final RowGeometry row, final GenericEvent[] events, final int binStartOffset) {

    StringBuilder buf = new StringBuilder();
    ;
    if (events == null) {
      buf.append("\n <no data>");
      return buf.toString();
    }
    if (events.length == 0) {
      buf.append("\n <no events>");
      return buf.toString();
    }

    final TimelineVariable.StatesDisplayInfo stateInfo;
    if (events[0] instanceof SampleEvent) { // see TLExperiment.getChartProps()
      stateInfo = TimelineVariable.getStateInfo(row.propName);
    } else {
      stateInfo = null;
    }

    boolean forceToSingleBin = false;
    RowDefinition rowDef = row.rowData.rowDefinition;
    if (!row.isChartData() && rowDef.getTLDataType().equals(Settings.TLData_type.TL_CLOCK)) {
      // Hack to show clock profiling callstacks as a point in time
      // ... but show microstate bar for the full duration
      forceToSingleBin = true;
    }

    for (int ii = 0; ii < events.length; ii++) {
      final GenericEvent event = events[ii];
      final int eventbinStart = event.binStart - binStartOffset;
      final int nbins = timeAxisCalculator.getNumBins();

      final int binStart, binCount;
      if (forceToSingleBin && event.binCount > 1) {
        binStart = eventbinStart + event.binCount - 1;
        binCount = 1;
      } else {
        binStart = eventbinStart;
        binCount = event.binCount;
      }

      if (binStart > nbins + FATTENED_EVENT_WIDTH) {
        break;
      }
      if (binStart + binCount < 0) {
        continue;
      }
      {
        int binEnd = binStart + binCount - 1;
        int xStart = timeAxisCalculator.getLowCoordForBin(binStart);
        int xEnd = timeAxisCalculator.getLowCoordForBin(binEnd + 1) - 1;
        buf.append(dumpGenericEvent(event, row));
        break; // only print the first visible event for now
      }
    }
    return buf.toString();
  }

  private String dumpRowChartValues(
      final RowData dataCopy, final long longVals[], int binStartOffset) {
    StringBuilder buf = new StringBuilder();
    ;

    final int[] binMinMax = getRowVisibleBinMinMax(dataCopy);
    final int lowBinLimit = binMinMax[0];
    final int highBinLimit = binMinMax[1] + 1; // +1 is fudge for missing pixel at some zooms

    int startBin = binStartOffset >= 0 ? binStartOffset : 0;
    int maxPrintValues = 10;
    buf.append("\n Leftmost non-zero chart values: ");
    for (int bin = startBin; bin < longVals.length; bin++) {
      final int visibleBin = bin - binStartOffset;
      if (visibleBin > highBinLimit) {
        break;
      }
      if (visibleBin < lowBinLimit) {
        continue;
      }
      long val = longVals[bin];
      if (val != 0) {
        if (maxPrintValues == 0) {
          break;
        }
        maxPrintValues--;
        buf.append(" " + val + "@" + bin);
      }
    }
    return buf.toString();
  }
}
