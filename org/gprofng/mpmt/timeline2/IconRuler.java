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

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.timeline_common.VerticalRowRuler;
import org.gprofng.mpmt.util.gui.AnUtility;
import org.gprofng.mpmt.util.ruler.Ruler;
import org.gprofng.mpmt.util.ruler.valuetypes.ValuesGeneric;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.ImageIcon;
import javax.swing.JTable;

public final class IconRuler extends VerticalRowRuler implements Ruler, Accessible {
  private final int ICON_WIDTH = 16;
  private final int ICON_HEIGHT = 16;
  private final int ICON_HPAD = 3;
  private final int CURSOR_HPAD = 7;

  private List<RowGeometry> rowGeometry;
  private int rowFirst; // first visible row
  private int rowLast; // first non-Visible row
  private int eventSelectionRow; // selected event's row
  private TreeSet<Integer> selectedRows; // highlighted rows
  private int scrollY;

  // font state
  private static Font TEXT_FONT;
  private int FONT_HEIGHT = 0;
  private int FONT_ASCENT = 0;
  private int FONT_DESCENT = 0;
  private int rulerWidth = 0;

  // drawer settings
  private ValuesGeneric valueType;
  private int paddingLow;
  private int paddingHigh;
  private final Color backColor = Color.WHITE;
  private final Color selectionBackColor = TimelineDraw.GENERIC_SELECTION_COLOR;
  private final Color selectionTextColor = Color.BLACK;
  private boolean comparisonEnabled;

  // preferred size
  private boolean lockPreferredSize = false;
  private Dimension preferredD = new Dimension(1, 1); // non-zero for layout mgr
  private int actualWidth;

  static {
    JTable jtable = new JTable();
    Font org_font = jtable.getFont();
    TEXT_FONT = new Font("SansSerif", org_font.getStyle(), org_font.getSize());
  }

  public IconRuler(ValuesGeneric type) { // YXXX icon ruler shouldn't take value type?
    super();
    this.valueType = type; // YXXX this should be a local copy
    this.paddingLow = 0;
    this.paddingHigh = 0;

    setToolTipText("");
    reset();
    AnUtility.setAccessibleContext(getAccessibleContext(), AnLocale.getString("Vertical Ruler"));
  }

  private void reset() {
    rowGeometry = new ArrayList();
    rowFirst = -1;
    rowLast = -1;
    eventSelectionRow = -1;
    selectedRows = new TreeSet();
    comparisonEnabled = false;
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

  public void setRowGeometry(
      List<RowGeometry> geometry,
      int scrollY,
      int firstVisibleRow,
      int lastVisibleRow,
      int selectedRow,
      TreeSet<Integer> multiselect_rows) {
    this.rowGeometry = geometry;
    this.scrollY = scrollY;
    this.rowFirst = firstVisibleRow;
    this.rowLast = lastVisibleRow;
    this.eventSelectionRow = selectedRow;
    this.selectedRows = multiselect_rows;
    this.comparisonEnabled = false;
    if (geometry.size() > 1) {
      int grp0 = geometry.get(0).rowData.rowDefinition.getExpGroupId();
      int grpLast = geometry.get(geometry.size() - 1).rowData.rowDefinition.getExpGroupId();
      if (grpLast != grp0) {
        comparisonEnabled = true;
      }
    }
    repaint();
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

  /** set direction of ruler w.r.t. coordinate system */
  public void setOrderReverse(boolean reverse) {}

  // if the LabelCoordinateCalculator stays the same, you can call this
  // WARNING: Modifies values in the global variable that was passed in!
  @Override
  public final void setRange(long lo, long hi) { // YXXX only used to determine repaints
    if (valueType.getLowValue() != lo || valueType.getHighValue() != hi) {
      valueType.setRange(lo, hi);
      repaint();
    }
  }

  @Override
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

  private int calcWidth(final Graphics g, String stmp) {
    if (!isFontInitialized()) {
      return 0; // YXXX or could return a guess
    }
    int strPixels = g.getFontMetrics(TEXT_FONT).stringWidth(stmp);
    return strPixels;
  }

  /** Recompute preferred size */
  private boolean recomputePreferredSize(Graphics g) {
    if (!isFontInitialized()) {
      return false;
    }
    int textWidth = calcWidth(g, "99 (Base)"); // YXXX should use real labels?

    rulerWidth = textWidth + ICON_WIDTH + ICON_HPAD + CURSOR_HPAD;
    boolean changed = internalSetPreferredSize(rulerWidth, 0);
    return changed;
  }

  // override parent
  @Override
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

  @Override
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
    myPaintComponent(g);
  }

  private int screenY(int virtualY) {
    return virtualY - scrollY;
  }

  // Paints the ruler
  private void myPaintComponent(final Graphics gr) {
    Graphics2D g = (Graphics2D) gr;
    Rectangle drawHere = g.getClipBounds();
    actualWidth = drawHere.width;

    // erase entire ruler
    g.setColor(backColor);
    g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

    if (rowGeometry.isEmpty()) {
      drawBorders(g, drawHere.height);
      return;
    }

    // Do the ruler labels in a small font that's black.
    g.setFont(TEXT_FONT);

    int lastVisibleRow = rowLast;

    // draw horizontal lines, selected rows' background
    for (int ii = rowFirst; ii < lastVisibleRow; ii++) {
      RowGeometry row = rowGeometry.get(ii);
      int groupId = row.rowData.rowDefinition.getExpGroupId();
      if (selectedRows.contains(ii)) {
        g.setColor(selectionBackColor);
      } else if (comparisonEnabled) {
        g.setColor(AnMetric.getMetricBackground(groupId));
      } else {
        g.setColor(backColor);
      }
      {
        int startY = screenY(row.startY);
        int height = screenY(row.endY) - startY + 1 + 1; // overshoot by 1
        int width = drawHere.width;
        g.fillRect(drawHere.x, startY, width, height);
      }
      if (row.rowDividerHeight > 0) {
        // divider below this row
        int y = screenY(row.endY + 1);
        if (row.isCompareGroupEnd) {
          TimelineDraw.drawDashedDivider(
              g, TimelineDraw.COMPARE_GRP_DIVIDER_COLOR, 0, actualWidth, y, row.rowDividerHeight);
        } else if (row.isExperimentEnd) {
          TimelineDraw.drawSolidDivider(
              g, TimelineDraw.EXPERIMENT_DIVIDER_COLOR, 0, actualWidth, y, row.rowDividerHeight);
        } else if (row.isEntityEnd) {
          TimelineDraw.drawSolidDivider(
              g, TimelineDraw.STANDARD_DIVIDER_COLOR, 0, actualWidth, y, row.rowDividerHeight);
        } else {
          TimelineDraw.drawDashedDivider(
              g, TimelineDraw.STANDARD_DIVIDER_COLOR, 0, actualWidth, y, row.rowDividerHeight);
        }
      }
      if (screenY(row.startY) > drawHere.height) {
        lastVisibleRow = ii;
        break;
      }
    }

    // draw icons
    for (int ii = rowFirst; ii < lastVisibleRow; ) {
      RowGeometry masterRow = rowGeometry.get(ii).getMasterRow();
      final int firstSubrow = masterRow.rowNum - masterRow.subrowNum;
      final int lastSubrow = firstSubrow + masterRow.nSubrows - 1;
      ii = lastSubrow + 1;
      ImageIcon icon = masterRow.rowData.rowDefinition.getTLDataType().getIcon();
      if (icon == null) {
        continue;
      }
      int block_startY = rowGeometry.get(firstSubrow).startY;
      int block_endY = rowGeometry.get(lastSubrow).endY;
      int row_height = block_endY - block_startY + 1;
      if (ICON_HEIGHT > row_height) {
        continue;
      }

      {
        int centerY = screenY((block_endY + block_startY + 1) / 2);
        int startY = centerY - ICON_HEIGHT / 2;
        icon.paintIcon(this, g, actualWidth - ICON_WIDTH - ICON_HPAD - CURSOR_HPAD, startY);
      }
    }

    // draw text
    for (int ii = rowFirst; ii < lastVisibleRow; ii++) {
      RowGeometry row = rowGeometry.get(ii);
      if (ii == eventSelectionRow) {
        RowGeometry masterRow = rowGeometry.get(ii).getMasterRow();
        final int firstSubrow = masterRow.rowNum - masterRow.subrowNum;
        final int lastSubrow = firstSubrow + masterRow.nSubrows - 1;
        int block_startY = rowGeometry.get(firstSubrow).startY;
        int block_endY = rowGeometry.get(lastSubrow).endY;
        if (block_endY - block_startY > 5) {
          block_endY--;
          block_startY++;
        }
        if (block_endY - block_startY > 7) {
          block_endY--;
          block_startY++;
        }
        drawHighlight(g, block_startY, block_endY);
      }
      if (!row.isEntityStart) {
        continue;
      }
      String str = row.rowData.rowDefinition.getShortLabel();
      if (str == null) {
        continue;
      }

      // scan ahead for entity end to get height
      int entity_startY = row.startY;
      int entity_endY = row.endY;
      for (int jj = ii; jj < lastVisibleRow; jj++) {
        RowGeometry tmp_row = rowGeometry.get(jj);
        if (tmp_row.isEntityEnd) {
          entity_endY = tmp_row.endY;
          break;
        }
      }
      int row_height = entity_endY - entity_startY + 1;
      int fudge_factor = 2;
      if (FONT_HEIGHT - fudge_factor > row_height) {
        continue;
      }
      int textStartY = screenY(entity_startY);
      if (FONT_HEIGHT > row_height) {
        textStartY -= (FONT_HEIGHT - row_height + 3) / 2;
      }

      boolean is_selected = selectedRows.contains(ii);
      int groupId = row.rowData.rowDefinition.getExpGroupId();

      // clear text background
      {
        Rectangle r = TEXT_FONT.getStringBounds(str, g.getFontRenderContext()).getBounds();
        if (is_selected) {
          g.setColor(selectionBackColor);
        } else if (comparisonEnabled) {
          g.setColor(AnMetric.getMetricBackground(groupId));
        } else {
          g.setColor(backColor);
        }
        g.fillRect(0, textStartY + 1, r.width, FONT_HEIGHT - 1);
      }

      if (is_selected) {
        g.setColor(selectionTextColor);
      } else {
        g.setColor(Color.black);
      }
      g.drawString(str, 0, textStartY + FONT_ASCENT);
    }
    drawBorders(g, drawHere.height);
  }

  private void drawHighlight(Graphics g, int dataStartY, int dataEndY) {
    Color c[] = {Color.YELLOW, Color.YELLOW, Color.RED};
    final int borderSz = c.length;
    int startY = dataStartY + borderSz;
    int endY = dataEndY - borderSz;

    int y = screenY(startY);
    int h = endY - startY + 1;
    int x = actualWidth - CURSOR_HPAD + 3;
    int w = -1;
    for (int i = 0; i < c.length; i++) {
      g.setColor(c[i]);
      g.drawRect(x - i - 1, y - i - 1, w + 2 * i + 1, h + 2 * i + 1);
    }
  }

  private void drawBorders(Graphics g, int height) {
    g.setColor(Color.black);
    g.drawLine(0, 0, actualWidth, 0);
    g.drawLine(0, height - 1, actualWidth, height - 1);
  }

  private RowGeometry findRow(int screenY) {
    int virtualY = screenY + scrollY;
    for (int ii = rowFirst; ii < rowLast; ii++) {
      RowGeometry row = rowGeometry.get(ii);
      if (virtualY <= row.endY && virtualY >= row.startY) {
        return row;
      }
    }
    return null;
  }

  // The tooltip to be displayed for any icon inside the ruler, TimelineDraw
  // goes through the list of data to find what is being displayed at that "y"
  // location for the evt and sets the tooltip accordingly
  @Override
  public String getToolTipText(MouseEvent evt) {
    int screenY = evt.getY();
    RowGeometry row = findRow(screenY);
    if (row != null) {
      String rc = row.rowData.rowDefinition.getLongDescription();
      return rc;
    }
    return null;
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    // variable accessibleContext is protected in superclass
    if (accessibleContext == null) {
      accessibleContext = new AccessibleIconRuler();
    }
    return accessibleContext;
  }

  final class AccessibleIconRuler extends AccessibleJComponent {
    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.CANVAS;
    }
  }
}
