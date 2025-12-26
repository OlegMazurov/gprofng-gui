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


package org.gprofng.mpmt.util.ruler;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.timeline_common.CoordCalcTimeReader;
import org.gprofng.mpmt.timeline_common.VerticalRowRuler;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.ruler.valuetypes.ValuesGeneric;
import org.gprofng.mpmt.util.zoomruler.PaintListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JTable;

// import org.gprofng.mpmt.GraphControls;

/**
 * A component that is suitable to show the labels for an axis of the graph which has a range of
 * values. Special processing is made to for nano second values to show the time units.
 */
public class RangeRuler extends VerticalRowRuler implements Ruler, Accessible {
  // user settings
  private ValuesGeneric valueType;
  private LabelCoordinateCalculator coordCalculator;
  private boolean isContinuous;
  private int orientation; // see enum above
  private int paddingLow;
  private int paddingHigh;
  private int tickPlacement; // see enum above
  private int titlePlacement = PLACEMENT_NONE; // see enum above
  private boolean inverseOrder = false;
  private boolean useTickCenterForUnitSteps;

  // ticks geometry
  private final int MIN_PIXELS_BETWEEN_TICKS = 4;
  private final int SMALL_TICK_LENGTH = 2;
  private final int MAXTICKLEN = SMALL_TICK_LENGTH * 4;

  // font settings
  private final int MIN_PIXELS_BETWEEN_LABELS = 30;
  private static final double RADIANS_90DEGREES = Math.PI / 2;
  private int MARGIN = 2;

  // font state
  private static Font TEXT_FONT;
  private int FONT_HEIGHT = 0;
  private int FONT_ASCENT = 0;
  private int FONT_DESCENT = 0;

  // state
  private boolean staggerHorizontalLabels;
  private int labelBoundaryCoord = 0; // coordinate of edge of title
  private String title;
  private ArrayList<PaintListener> paintListeners = new ArrayList<>();
  private boolean lockPreferredSize = false;
  private Dimension preferredD = new Dimension(1, 1); // non-zero for layout mgr

  // TabStops // TBR: eventually move to new class that implements RangeRuler
  public enum TabStopTypes {
    TABSTOP_GENERIC,
    TABSTOP_LEFT,
    TABSTOP_RIGHT
  };

  private List<TabStopTypes> tabStopTypes;
  private List<Long> tabStopValues;
  private CoordCalcTimeReader tabStopCalc;
  private final int TABSTOP_WIDTH = 5;
  private final int TABSTOP_LENGTH = MAXTICKLEN - 1;
  private final Color tabStopColorLeft = new Color(0, 32, 255); // Microstate Sleep blue
  private final Color tabStopColorRight = new Color(255, 16, 0); // Microstate Stopped red

  /** Instantiate a RangeRuler with a specified orientation */
  public RangeRuler(ValuesGeneric type, int orientation) {
    super();
    initRangeRuler(type, orientation, null, true);
  }

  static {
    JTable jtable = new JTable();
    Font org_font = jtable.getFont();
    TEXT_FONT =
        new Font(
            "SansSerif",
            org_font.getStyle(), // NOI18N
            org_font.getSize());
  }

  public RangeRuler(
      ValuesGeneric type,
      int orientation,
      LabelCoordinateCalculator coordCalculator,
      boolean continuous) {
    super();
    initRangeRuler(type, orientation, coordCalculator, continuous);
  }

  private void initRangeRuler(
      ValuesGeneric type,
      int orientation,
      LabelCoordinateCalculator coordCalculator,
      boolean isContinuous) {
    this.valueType = type;
    this.paddingLow = 0;
    this.paddingHigh = 0;
    this.orientation = orientation;
    if (coordCalculator == null) {
      this.coordCalculator = new LocalCalculator();
      isContinuous = true;
    } else {
      this.coordCalculator = coordCalculator;
    }
    this.isContinuous = isContinuous;
    this.useTickCenterForUnitSteps = true;
    this.tickPlacement = TICKS_ON_THE_BOTTOM;
    this.title = ""; // NOI18N
    if (this.orientation == this.VERTICAL_ORIENTATION) {
      inverseOrder = !inverseOrder;
    }
    this.tabStopTypes = new ArrayList<>();
    this.tabStopValues = new ArrayList<>();
    String aText =
        orientation == RangeRuler.HORIZONTAL_ORIENTATION
            ? AnLocale.getString("Horizontal Ruler")
            : AnLocale.getString("Vertical Ruler");
    AnUtility.setAccessibleContext(getAccessibleContext(), aText);
  }

  private boolean isFontInitialized() {
    if (FONT_HEIGHT != 0) {
      return true;
    }
    if (TEXT_FONT != null) {
      FontMetrics fm = this.getFontMetrics(TEXT_FONT);
      if (fm != null) {
        FONT_ASCENT = fm.getAscent() - 1; // YXXX bogus adjust for looks
        FONT_DESCENT = fm.getDescent() - 2; // YXXX bogus adjust for looks
        FONT_HEIGHT = FONT_ASCENT + FONT_DESCENT;
        return true;
      }
    }
    return false;
  }

  /**
   * Set a spacing to align component padLo is at the "0" end of the graphics' coordinate system
   * padHi is at the opposite end
   */
  public void setPadding(int padLo, int padHi) {
    if (paddingLow != padLo || paddingHigh != padHi) {
      paddingLow = padLo;
      paddingHigh = padHi;
      repaint(); // should we do this elsewhere, as well?
    }
  }

  public int getPaddingLow() {
    return paddingLow;
  }

  public int getPaddingHigh() {
    return paddingHigh;
  }

  public void setUseTickCenterForUnitSteps(boolean center) {
    useTickCenterForUnitSteps = center;
  }

  /** set direction of ruler w.r.t. coordinate system */
  public void setOrderReverse(boolean reverse) {
    this.inverseOrder = reverse;
  }

  /** Set title for the axis */
  public void setTitle(String title) {
    this.title = title;
    if (title != null && titlePlacement == PLACEMENT_NONE) {
      setTitlePlacement(PLACEMENT_TITLE_LEVEL);
    }
  }

  /** Set to TICKS_ON_THE_BOTTOM or TICKS_ON_THE_TOP */
  public void setTickPlacement(int p) {
    tickPlacement = p;
  }

  public void setTitlePlacement(int placement) {
    titlePlacement = placement;
    repaint();
  }

  public void addPaintLister(PaintListener paintListener) {
    paintListeners.add(paintListener);
  }

  private void notifyPaintListeners(Graphics g) {
    for (PaintListener l : paintListeners) {
      l.timelinePainted(g);
    }
  }

  // if the LabelCoordinateCalculator stays the same, you can call this
  // WARNING: Modifies values in the global variable that was passed in!
  public final void setRange(long lo, long hi) {
    if (valueType.getLowValue() != lo || valueType.getHighValue() != hi) {
      valueType.setRange(lo, hi);
      repaint();
    }
  }

  // --- accessability
  public final AccessibleContext getAccessibleContext() {
    // variable accessibleContext is protected in superclass
    if (accessibleContext == null) {
      accessibleContext = new AccessibleRuler();
    }
    return accessibleContext;
  }

  final class AccessibleRuler extends AccessibleJComponent {

    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.CANVAS;
    }
  }

  // --- calculators
  private class LocalCalculator implements LabelCoordinateCalculator {
    public int getCenterCoordForValue(long vv) {
      int absoluteCoord;
      // pretty inefficient, could cache some of these values
      int pixelsAvailForData = getDataPixels();
      long dataHi = valueType.getHighValue();
      long dataLo = valueType.getLowValue();
      long dataRange = dataHi - dataLo + 1;
      double pixelsPerVal = (double) pixelsAvailForData / dataRange;
      double relOffset = vv - dataLo;
      if (useTickCenterForUnitSteps) {
        relOffset += 0.5;
      }
      int tmp = (int) (pixelsPerVal * relOffset);
      if (inverseOrder) {
        absoluteCoord = pixelsAvailForData - tmp;
      } else {
        absoluteCoord = tmp;
      }
      return absoluteCoord;
    }

    public int getDataPixels() {
      int availPixels;
      if (orientation == HORIZONTAL_ORIENTATION) {
        availPixels = getWidth() - paddingLow - paddingHigh;
      } else {
        availPixels = getHeight() - paddingLow - paddingHigh;
      }
      return availPixels;
    }
  }

  private boolean setPreferredForVertical(final Graphics g) {
    if (!isFontInitialized()) {
      return false;
    }
    int w = calcWidestLabel(g, valueType.getLowValue(), valueType.getHighValue(), 1);
    int width = w + MAXTICKLEN + MARGIN;
    if (titlePlacement != PLACEMENT_NONE) {
      width += FONT_HEIGHT + MARGIN;
    }

    boolean changed = internalSetPreferredSize(width, 0);
    return changed;
  }

  private boolean setPreferredForHorizontal(final Graphics g) {
    if (!isFontInitialized()) {
      return false;
    }
    int level = 1; // basic labels
    if (false) {
      // YXXX maybe the right thing to do, but I want to minimize resizes
      setStaggerHorizontalLabels(g); // compute staggerHorizontalLabels
      if (staggerHorizontalLabels) {
        level++;
      }
    } else {
      if (!isContinuous) {
        level++;
      }
    }
    if (titlePlacement == PLACEMENT_TITLE_LEVEL) {
      level++;
    }
    int height = computeDistanceFromHorizontalRuler(level);
    height += MARGIN;

    boolean changed = internalSetPreferredSize(0, height);
    return changed;
  }

  private int computeYCoordForHorizontalLabel(int level) {
    int coord = computeDistanceFromHorizontalRuler(level);
    if (tickPlacement == TICKS_ON_THE_BOTTOM) {
      coord += FONT_DESCENT;
      coord = preferredD.height - coord;
    } else {
      coord += FONT_ASCENT;
    }
    return coord;
  }

  // note: modifies staggerHorizontalLabels
  private int setStaggerHorizontalLabels(final Graphics g) {
    int availPixels = coordCalculator.getDataPixels();
    if (availPixels <= 0) {
      return 0;
    }
    long lo = valueType.getLowValue();
    long hi = valueType.getHighValue();
    long dataRange = hi - lo + 1; // must be positive
    long estDataStep = dataRange / 80; // worst case, say 80 labels
    int maxLabelPixels; // max number of pixels per label, on-axis direction

    // # of allowable offsets (e.g. 2 for alternating) for tick labels
    boolean doStagger = false;
    maxLabelPixels = calcWidestLabel(g, lo, hi, estDataStep);
    maxLabelPixels += FONT_HEIGHT / 2; // add horzontal space based on font
    int estLabelCount = availPixels / maxLabelPixels;
    if (estLabelCount < dataRange && !isContinuous) {
      // labels don't all fit, use multi level
      doStagger = true;
      // two levels of labels -> divide by 2
      maxLabelPixels = maxLabelPixels / 2 + 1;
    }

    staggerHorizontalLabels = doStagger;
    return maxLabelPixels; // largest size allowed for a single label
  }

  private int computeDistanceFromHorizontalRuler(int level) {
    int coord = MAXTICKLEN;
    coord += level * (FONT_HEIGHT);
    return coord;
  }

  // --- paint functions
  private void paintHorizontal(final Graphics g) {
    g.setFont(TEXT_FONT);
    g.setColor(Color.black);

    // direction-specific geometry
    int originX = 0;
    int originY;
    if (tickPlacement == TICKS_ON_THE_BOTTOM) {
      originY = this.getHeight();
    } else {
      originY = 0;
    }
    int maxLabelPixels = setStaggerHorizontalLabels(g); // sets staggerHorizontalLabels

    // draw title
    int title_x;
    int title_y;
    String lbl = title;
    String units = valueType.getUnitString();
    if (units != null && !units.equals("")) {
      lbl += "(" + units + ")"; // NOI18N
    }
    if (titlePlacement == PLACEMENT_TITLE_LEVEL) {
      title_x = coordCalculator.getDataPixels() / 2 + paddingLow;
      int level = 1;
      if (staggerHorizontalLabels) {
        level++;
      }
      title_y = computeYCoordForHorizontalLabel(level);
      drawCenteredString(g, lbl, title_x, title_y, 0); // YXXX add right bound later
      labelBoundaryCoord = MARGIN;
    } else if (titlePlacement == PLACEMENT_WITH_LABELS) {
      title_x = MARGIN;
      title_y = computeYCoordForHorizontalLabel(0);
      g.drawString(lbl, title_x, title_y);
      int titleWidth = g.getFontMetrics(TEXT_FONT).stringWidth(lbl);
      labelBoundaryCoord = title_x + titleWidth + MARGIN;
    }

    // draw ticks
    drawAllTicks(g, maxLabelPixels, originX, originY);
    drawAllTabStops(g, originX, originY);
  }

  private void paintVertical(final Graphics g) {
    g.setFont(TEXT_FONT);
    g.setColor(Color.black);

    // direction-specific geometry
    int originY = this.getHeight();
    int originX;
    if (tickPlacement == TICKS_ON_THE_BOTTOM) { // right
      originX = this.getWidth();
    } else {
      originX = 0;
    }
    int maxLabelPixels = FONT_HEIGHT + 1;

    // draw title
    if (titlePlacement == PLACEMENT_NONE) {
      labelBoundaryCoord = MARGIN;
    } else {
      int title_x = FONT_ASCENT + MARGIN;
      int title_y;
      String lbl = title;
      String units = valueType.getUnitString();
      if (units != null && !units.equals("")) {
        lbl += "(" + units + ")"; // NOI18N
      }
      int w = g.getFontMetrics(TEXT_FONT).stringWidth(lbl);
      title_y = (coordCalculator.getDataPixels() + w) / 2 + paddingLow;

      Graphics2D g2 = (Graphics2D) g;
      g2.rotate(-RADIANS_90DEGREES);
      g2.drawString(lbl, -title_y, title_x);
      g2.rotate(RADIANS_90DEGREES);

      labelBoundaryCoord = MARGIN + FONT_HEIGHT + MARGIN;
    }

    // draw ticks
    drawAllTicks(g, maxLabelPixels, originX, originY);
  }

  // color must already be set
  private void drawAllTicks(
      final Graphics g,
      int maxLabelPixels, // pixels to allocate per label
      int originX,
      int originY) {

    g.setColor(Color.black);

    // estimate label sizes
    int availPixels = coordCalculator.getDataPixels();
    long lo = valueType.getLowValue();
    long hi = valueType.getHighValue();
    long dataRange = hi - lo + 1; // must be positive

    // get array which specifies tick spacing
    long[] tickStep = calcTickSteps(availPixels, maxLabelPixels, dataRange);

    int tickLen = SMALL_TICK_LENGTH;
    // loop thru each level of tick spacing
    for (int ii = 0; ii < tickStep.length; ii++) {
      boolean showLabels = false;
      if (ii == tickStep.length - 1) {
        // last element in list specifies Labels
        showLabels = true;
      }
      drawTicks(g, originX, originY, tickStep[ii], tickLen, showLabels);
      tickLen += SMALL_TICK_LENGTH;
    }
  }

  // color must already be set
  private boolean drawTicks(
      final Graphics g, int originX, int originY, long dataStep, int tickLen, boolean showLabels) {

    if (dataStep == 0) {
      // no ticks at this dataStep level
      return false;
    }

    int pixelsAvailForData = coordCalculator.getDataPixels();
    long dataHi = valueType.getHighValue();
    long dataLo = valueType.getLowValue();
    long dataRange = dataHi - dataLo + 1;
    double nsteps = (double) dataRange / dataStep;

    long pixelsPerTick = (long) (pixelsAvailForData / nsteps);
    if (pixelsPerTick < MIN_PIXELS_BETWEEN_TICKS) {
      // ticks too close together for screen
      if (showLabels) {
        return false; // gets here when there's no space for labels
      }
      return false;
    }

    // calculate loop start and end
    long alignedStart; // first value >= dataLo that is aligned to dataStep
    long alignedEnd;
    if (dataLo >= 0) {
      alignedStart = (dataLo + dataStep - 1) / dataStep * dataStep;
    } else {
      alignedStart = (dataLo) / dataStep * dataStep;
    }
    if (useTickCenterForUnitSteps) {
      alignedEnd = dataHi;
    } else {
      alignedEnd = dataHi + 1; // show most extreme tickmark
    }

    int labelOffsetLevel = 0;
    if (staggerHorizontalLabels) {
      long topDigit = alignedStart / dataStep;
      labelOffsetLevel = (int) (topDigit % 2);
    }
    // step thru range by tick step
    for (long vv = alignedStart; vv <= alignedEnd; vv += dataStep) {
      int x, y; // tick x and y

      // compute pixel location in axis direction
      int tickLocation;
      tickLocation = coordCalculator.getCenterCoordForValue(vv);
      tickLocation += paddingLow;

      if (orientation == HORIZONTAL_ORIENTATION) {
        x = tickLocation;
        if (showLabels) {
          int labelTickLen = computeDistanceFromHorizontalRuler(labelOffsetLevel);

          // draw extended ticks to handle staggered labels
          if (tickPlacement == TICKS_ON_THE_BOTTOM) {
            y = originY - labelTickLen;
          } else {
            y = originY + labelTickLen;
          }
          g.drawLine(x, originY, x, y);

          int lx = x;
          int ly = computeYCoordForHorizontalLabel(labelOffsetLevel);
          String label = valueType.getFormattedValue(vv, dataStep);
          int labelRightLimit = lx + (int) pixelsPerTick / 2;
          drawCenteredString(g, label, lx, ly, labelRightLimit);
          if (staggerHorizontalLabels) {
            labelOffsetLevel = (labelOffsetLevel + 1) % 2;
          }
        } else {
          if (tickPlacement == TICKS_ON_THE_BOTTOM) {
            y = originY - tickLen;
          } else {
            y = originY + tickLen;
          }
          g.drawLine(x, originY, x, y);
        }
      } else { // VERTICAL_ORIENTATION
        y = tickLocation;
        if (tickPlacement == TICKS_ON_THE_BOTTOM) { // on the right
          x = originX - tickLen;
          if (x < labelBoundaryCoord) {
            // no space to draw
            continue;
          }
        } else {
          x = tickLen;
          if (x > labelBoundaryCoord) {
            // no space to draw
            continue;
          }
        }
        if (showLabels) {
          String label = valueType.getFormattedValue(vv, dataStep);
          int lx = x;
          int ly = y - FONT_HEIGHT / 2 + FONT_ASCENT;
          if (tickPlacement == TICKS_ON_THE_BOTTOM) { // on the right
            drawRightAdjustedString(g, label, lx, ly);
          } else {
            drawLeftAdjustedString(g, label, lx, ly);
          }
        } else {
          g.drawLine(originX, y, x, y);
        }
      }
    }
    return true;
  }

  // tabStop code to show caliper "handles" // TBR, refactor to class that implements RangeRuler
  public final void setTabStops(
      List<Long> vals, List<TabStopTypes> types, CoordCalcTimeReader calc) {
    final int valuesSize = (vals != null) ? vals.size() : 0;
    final int typesSize = (types != null) ? types.size() : 0;
    boolean needsUpdate = false;
    tabStopCalc = calc; // TBR after refactoring tabStop* into separate class
    if (tabStopValues.size() != valuesSize || tabStopTypes.size() != typesSize) {
      needsUpdate = true;
    }
    for (int ii = 0; !needsUpdate && ii < valuesSize; ii++) {
      if (tabStopValues.get(ii) != vals.get(ii)) {
        needsUpdate = true;
        break;
      }
      if (tabStopTypes.get(ii) != types.get(ii)) {
        needsUpdate = true;
        break;
      }
    }
    if (!needsUpdate) {
      return;
    }
    tabStopValues.clear();
    tabStopTypes.clear();
    for (int ii = 0; ii < valuesSize; ii++) {
      tabStopValues.add(vals.get(ii));
      TabStopTypes newType = TabStopTypes.TABSTOP_GENERIC;
      if (types != null && ii < types.size()) {
        newType = types.get(ii);
      }
      tabStopTypes.add(newType);
    }
    repaint();
  }

  private void drawAllTabStops(final Graphics g, int originX, int originY) {

    if (tabStopValues.size() != 2) { // only handle caliper pairs for now
      return;
    }
    {
      int bin;
      int cx;
      final int margin = tabStopCalc.getMargin();

      int ii = 0;
      bin = tabStopCalc.getBinAtTime(tabStopValues.get(ii));
      cx = tabStopCalc.getLowCoordForBin(bin) - margin;
      drawTabStop(g, originX, originY, margin, cx, tabStopTypes.get(ii));

      ii = 1;
      bin = tabStopCalc.getBinAtTime(tabStopValues.get(ii));
      cx = (tabStopCalc.getLowCoordForBin(bin + 1) - 1) - margin;
      drawTabStop(g, originX, originY, margin, cx, tabStopTypes.get(ii));
    }
  }

  private void drawTabStop(
      final Graphics g, int originX, int originY, int margin, int coord, TabStopTypes type) {
    if (coord < -margin || coord > coordCalculator.getDataPixels() + margin) {
      return;
    }
    int tickLen = TABSTOP_LENGTH;
    int x, y; // tick x and y
    // compute pixel location in axis direction
    int tickLocation = coord + paddingLow;
    if (orientation == HORIZONTAL_ORIENTATION) {
      x = tickLocation;
      int midY;
      if (tickPlacement == TICKS_ON_THE_BOTTOM) {
        y = originY - tickLen;
        midY = originY - TABSTOP_WIDTH;
      } else {
        y = originY + tickLen;
        midY = originY + TABSTOP_WIDTH;
      }
      int w = 0;
      Color c = Color.black;
      switch (type) {
        case TABSTOP_LEFT:
          w = -TABSTOP_WIDTH;
          c = tabStopColorLeft;
          break;
        case TABSTOP_RIGHT:
          w = TABSTOP_WIDTH;
          c = tabStopColorRight;
          break;
        default:
        case TABSTOP_GENERIC:
          g.setColor(Color.red);
          g.drawLine(x, originY, x, y);
          break;
      }
      if (w != 0) {
        Polygon p;
        {
          int xx[] = {x, x, x + w, x + w, x};
          int yy[] = {originY, y, y, midY, originY};
          p = new Polygon(xx, yy, xx.length); // This polygon represents a triangle with the above
        }
        g.setColor(c);
        g.fillPolygon(p);
        g.setColor(Color.DARK_GRAY);
        g.drawPolygon(p);
      }
    }
  }

  // --- string utilites
  private void drawRightAdjustedString(Graphics g, String label, int rightBoundary, int ycoord) {
    if (rightBoundary <= labelBoundaryCoord) {
      // right of string is already infringing on title
      return;
    }
    int xlabelsz = g.getFontMetrics(TEXT_FONT).stringWidth(label);
    int startx = rightBoundary - xlabelsz;
    if (startx >= labelBoundaryCoord) {
      g.drawString(label, startx, ycoord);
    } else {
      int availWidth = rightBoundary - labelBoundaryCoord;
      String printString = squishString(g, label, availWidth);
      if (printString != null) {
        xlabelsz = g.getFontMetrics(TEXT_FONT).stringWidth(printString);
        startx = rightBoundary - xlabelsz;
        g.drawString(printString, startx, ycoord);
      }
    }
  }

  private void drawLeftAdjustedString(Graphics g, String label, int leftBoundary, int ycoord) {
    g.drawString(label, leftBoundary, ycoord);
    // YXXX implement limited print later
  }

  private void drawCenteredString(
      Graphics g, String label, int labelCenter, int ycoord, int rightBoundary) {
    if (labelCenter <= labelBoundaryCoord) {
      // middle of string is already infringing on title
      return;
    }
    int xlabelsz = g.getFontMetrics(TEXT_FONT).stringWidth(label);
    int startx = labelCenter - xlabelsz / 2;
    if (startx >= labelBoundaryCoord) {
      g.drawString(label, startx, ycoord);
    } else {
      int availWidth = rightBoundary - labelBoundaryCoord;
      String printString = squishString(g, label, availWidth);
      if (printString != null) {
        g.drawString(printString, labelBoundaryCoord, ycoord);
      }
    }
  }

  private String squishString(Graphics g, String label, int availWidth) {
    int strlen;
    strlen = g.getFontMetrics(TEXT_FONT).stringWidth(label);
    if (strlen <= availWidth) {
      return label;
    }
    String newString = "..";
    strlen = g.getFontMetrics(TEXT_FONT).stringWidth(newString);
    if (strlen > availWidth) {
      return null;
    }
    String currString = newString;
    int ii = 0;
    if (false) {
      ii += 1; // show first N characters then ".."
      newString = label.substring(0, ii) + currString;
      strlen = g.getFontMetrics(TEXT_FONT).stringWidth(newString);
      if (strlen > availWidth) {
        return currString;
      }
      ii += 1; // skip N characters
      currString = newString;
      int max = label.length();
      // find largest tail segment that fits
      for (; ii < max; ii++) {
        newString = currString + label.substring(ii);
        strlen = g.getFontMetrics(TEXT_FONT).stringWidth(newString);
        if (strlen < availWidth) {
          return newString;
        }
      }
    } else {
      // chop off tailing characters and end with ".."
      int max = label.length();
      currString = newString;
      // find largest head segment that fits
      for (ii = 0; ii < max; ii++) {
        newString = label.substring(0, max - ii - 1) + "..";
        strlen = g.getFontMetrics(TEXT_FONT).stringWidth(newString);
        if (strlen < availWidth) {
          return newString;
        }
      }
    }
    return currString;
  }

  // --- return array which specifies tick spacing
  private long[] calcTickSteps(int pixelsAvailForData, int maxLabelSize, long dataRange) {

    // how far apart should labels be?
    int strPixels = maxLabelSize;
    if (isContinuous && strPixels < MIN_PIXELS_BETWEEN_LABELS) {
      // for continuous axes, require more space between labels
      strPixels = MIN_PIXELS_BETWEEN_LABELS;
    }
    if (strPixels <= 0) {
      strPixels = 1;
    }

    // estimate max # of labels based on estimated label size
    int maxNumLabels;
    maxNumLabels = pixelsAvailForData / strPixels;
    if (maxNumLabels < 1) {
      // one label may not fit!
      maxNumLabels = 1;
    }

    // types of steps to calculate
    long labelStep; // value step between labels
    long wholeStep; // value step for Ones
    long midStep; // value step for Halves
    long tenthStep; // value step for 10ths

    // determine labelStep
    double d_minValueStep; // smallest step allowed based on label size
    d_minValueStep = (double) dataRange / maxNumLabels;
    long multipleOfTen = getMultipleOfTen((long) d_minValueStep);
    double d_minValStepMSD; // most significant digit of minValueStep
    d_minValStepMSD = d_minValueStep / multipleOfTen;

    int labelMSD; // significant digit of step for Labels
    if (isContinuous) {
      // show a sparse # of labels & let the ticks show intermediate vals
      if (d_minValStepMSD <= 1.0) {
        labelMSD = 1;
      } else {
        labelMSD = 10; // try 10x first
        long estNumLabels = dataRange / (labelMSD * multipleOfTen);
        if (estNumLabels < 2 && d_minValStepMSD <= 5) {
          // label half-steps to increase # of labels
          labelMSD = 5;
          estNumLabels = dataRange / (labelMSD * multipleOfTen);
          if (estNumLabels < 2 && d_minValStepMSD <= 2) {
            // label half-steps to increase # of labels
            labelMSD = 2;
          }
        }
      }
    } else { // !isContinous
      // show as many labels as possible
      if (d_minValStepMSD <= 1.0) {
        labelMSD = 1;
      } else if (d_minValStepMSD <= 2) {
        labelMSD = 2;
      } else if (d_minValStepMSD <= 5) {
        labelMSD = 5;
      } else {
        labelMSD = 10;
      }
    }
    labelStep = labelMSD * multipleOfTen;

    // decide step for ticks
    if (d_minValStepMSD <= 1.0) {
      wholeStep = multipleOfTen;
    } else {
      wholeStep = multipleOfTen * 10;
    }
    midStep = wholeStep / 2;
    tenthStep = wholeStep / 10;
    if (labelMSD == 2) {
      midStep = 0; // don't emphasize "5" ticks if labels are at steps of 2
    }

    long rc[] = new long[4];
    // smallest tick first, label last
    rc[0] = tenthStep;
    rc[1] = midStep;
    rc[2] = wholeStep;
    rc[3] = labelStep;
    return rc;
  }

  private long getMultipleOfTen(long value) {
    long d = value;
    long multipleOfTen = 1;
    while (d >= 10) {
      d /= 10;
      multipleOfTen *= 10;
    }
    return multipleOfTen;
  }

  private int calcWidestLabel(final Graphics g, long lo, long hi, long dataStep) {
    if (!isFontInitialized()) {
      return 0; // YXXX or could return a guess
    }
    int strPixels = 0;
    long step = 1;
    if (isContinuous) {
      // just the endpoints are OK
      step = hi - lo;
      if (step < 1) {
        step = 1;
      }
    }
    for (long ii = lo; ii <= hi; ii += step) {
      String stmp = valueType.getFormattedValue(ii, dataStep);
      int itmp = g.getFontMetrics(TEXT_FONT).stringWidth(stmp);
      if (strPixels < itmp) {
        strPixels = itmp;
      }
    }
    return strPixels;
  }

  /** Recompute preferred size */
  private boolean recomputePreferredSize(Graphics g) {
    boolean changed;
    if (orientation == HORIZONTAL_ORIENTATION) {
      changed = setPreferredForHorizontal(g);
    } else {
      changed = setPreferredForVertical(g);
    }
    return changed;
  }

  public void lockRulerThickness(Graphics g, boolean lock) {
    /*
     * YXXX not sure if it ok to use g from another context, but
     * font size is needed to set preferred ruler size, and
     * I don't know how else to get font size without g.
     * Maybe we should never attempt to lock ruler thickness?
     * Unfortunately, changing thickness might unexpectedly change
     * data range that can be shown on center panel.
     * For now, workaround is to provide g.
     */
    if (!lockPreferredSize && lock) {
      recomputePreferredSize(g);
    }
    lockPreferredSize = lock;
  }

  // override parent
  public Dimension getPreferredSize() {
    return preferredD;
  }

  private boolean internalSetPreferredSize(int width, int height) {
    boolean changed = false;
    if (preferredD.width != width || preferredD.height != height) {
      preferredD.width = width;
      preferredD.height = height;
      changed = true;
    }
    return changed;
  }

  protected void paintComponent(Graphics g) {
    if (!isFontInitialized()) {
      // should never happen since <g> should allow font to be set
      return;
    }
    if (!lockPreferredSize) {
      boolean changed = recomputePreferredSize(g);
      if (changed) {
        revalidate(); // YXXX not supposed to revalidate from paint
      }
    }
    if (orientation == HORIZONTAL_ORIENTATION) {
      paintHorizontal(g);
    } else {
      paintVertical(g);
    }
    this.notifyPaintListeners(g);
  }
}
