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

package org.gprofng.mpmt.util.zoomruler;

import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The ZoomrulerOverlay class draws the ZoomRuler on top of a component. It registers a
 * MouseListener and MouseMotionListener on that component and handles mouse events that occur
 * within the overlay. It generates ZoomRoulerEvent objects when the user requests a change in zoom.
 * Along with the ruler to display the levels, a reset button and a revert button can be used. There
 * is also some support for a pan button but it is not fully supported at this point. The overlay
 * does require the component to interact with it. The overlay doesn't not know when to repaint. So
 * the component must tell it to repaint. Additionally MouseHandling usually should be coordinated
 * through the OverlayMouseHandler class.
 *
 * @author blewis
 */
public class ZoomRulerOverlay {
  // geometry (LD == long direction, SD == short direction)
  // distance from canvas edge
  private static final int BORDER = 1;
  // size of +/- and arrow boxes
  private static final int ANCHOR_SIZE = 14;
  // panning arrows
  private static final int ARROW_WIDTH = 4;
  private static final int ARROW_LENGTH = 4;
  // slider
  private static final int MARKER_LD = 6;
  private static final int MARKER_SD = 14;
  // slider bar (ruler)
  public static final int RULER_LD_DEFAULT = 50 + MARKER_LD; // 10e15/log(2)=50
  // whitespace between anchor border and +/- labels
  private static final int LABEL_BORDER = 3;

  // colors and shape
  private static final Color borderColor = new Color(0x44, 0x44, 0x44);
  private static final Color buttonFillColor = new Color(0xf4, 0xf4, 0xf4);
  private static final Color labelShadowColor = new Color(0xcc, 0xcc, 0xcc);
  private static final Color shadowColor = new Color(0x99, 0x99, 0x99);
  private static final boolean useLabelShadow = true;
  private static final int rectRadius = 3;

  // components and listeners
  private Vector<ZoomRulerListener> listeners;
  private JComponent component;
  private static final ImageIcon revertImg = AnUtility.undo_icon;
  private static final ImageIcon resetImg = AnUtility.rset_icon;

  // levels, modified at runtime
  private int nlevels; // total levels
  private int ilevel; // current level

  // state
  private boolean active = true;
  private boolean dragging = false;
  private int recent_mouse_button = -1;

  // set by user at or near creation
  private static final int RULER_SD = 4;
  private final int RULER_LD;

  // full ruler including +/- boxes
  private static final int TOTAL_SD = 2 * BORDER + ANCHOR_SIZE;
  private final int TOTAL_LD;

  // +/- box far from origin
  private static final int ANCHOR2_OFFSET_SD = BORDER;
  private final int ANCHOR2_OFFSET_LD;
  // +/- box near origin
  private static final int ANCHOR1_OFFSET_SD = BORDER;
  private final int ANCHOR1_OFFSET_LD;
  // reset box
  private static final int RESET_OFFSET_SD = BORDER;
  private final int RESET_OFFSET_LD;
  // revert box
  private static final int REVERT_OFFSET_SD = BORDER;
  private final int REVERT_OFFSET_LD;

  private final boolean hasSlider;
  private final int orientation;
  private final boolean revertable;
  private final boolean PANNING = false; // enable panning mode
  private final boolean resetable1d;
  private final boolean resetable2d;
  private final boolean autoRepeat;

  private int xoff;
  private int yoff;
  private boolean paintBackground;
  private boolean isEnabled;

  // user-accessible enums
  public static final int VERTICAL_ORIENTATION = 1;
  public static final int HORIZONTAL_ORIENTATION = 2;

  /**
   * Instantiate a new overlay object that will draw the ruler on the given component, with nlevels,
   * starting at coordinate(x,y) with the specified orientation. Usage of Resetable and Revertable
   * buttons are also specified.
   */
  public ZoomRulerOverlay(
      JComponent component,
      int x,
      int y,
      final int orientation,
      final int slider_length,
      final boolean resetable1d,
      final boolean resetable2d,
      final boolean revertable,
      final boolean autoRepeat) {
    if (slider_length < MARKER_LD * 2) {
      hasSlider = false;
    } else {
      hasSlider = true;
    }
    RULER_LD = slider_length;

    // precalculate offsets on the long axis
    RESET_OFFSET_LD = BORDER;
    if (resetable2d) {
      if (orientation == this.HORIZONTAL_ORIENTATION) {
        ANCHOR1_OFFSET_LD = RESET_OFFSET_LD + resetImg.getIconWidth() + 1;
      } else {
        ANCHOR1_OFFSET_LD = RESET_OFFSET_LD + resetImg.getIconHeight() + 1;
      }
    } else if (resetable1d) {
      ANCHOR1_OFFSET_LD = RESET_OFFSET_LD + ANCHOR_SIZE + 2;
    } else {
      ANCHOR1_OFFSET_LD = RESET_OFFSET_LD;
    }
    ANCHOR2_OFFSET_LD = ANCHOR1_OFFSET_LD + ANCHOR_SIZE + RULER_LD;
    REVERT_OFFSET_LD = ANCHOR2_OFFSET_LD + ANCHOR_SIZE + 1;

    int ld_total = REVERT_OFFSET_LD;
    if (orientation == this.HORIZONTAL_ORIENTATION) {
      if (revertable) ld_total += revertImg.getIconWidth();
    } else {
      if (revertable) ld_total += revertImg.getIconHeight();
    }
    TOTAL_LD = ld_total;

    this.component = component;
    this.nlevels = 0;
    this.ilevel = 0;
    this.xoff = x;
    this.yoff = y;
    this.orientation = orientation;

    this.resetable1d = resetable1d;
    this.resetable2d = resetable2d;

    this.revertable = revertable;
    this.autoRepeat = autoRepeat;
    listeners = new Vector();
    initializeMouseListeners();
    isEnabled = true;
  }

  /** Get the height of the overlay */
  public int getHeight() {
    if (orientation == HORIZONTAL_ORIENTATION) return TOTAL_SD + 1;
    else {
      return TOTAL_LD + 1;
    }
  }

  /** Get the widht of the overlay */
  public int getWidth() {
    if (orientation == HORIZONTAL_ORIENTATION) {
      return TOTAL_LD + 1;
    } else return TOTAL_SD + 1;
  }

  /** Get the horizontal offset in the component where the overlay is drawn */
  public int getX() {
    return xoff;
  }

  /** Get the vertical offset in the component where the overlay is drawn */
  public int getY() {
    return yoff;
  }

  /** Set the number of zoom levels to display */
  public void setNumberOfLevels(int n) {
    if (n != nlevels && n >= 0) {
      nlevels = n;
      doRepaint();
    }
  }

  /** Get the the number of possible levels */
  public int getNumberOfLevels() {
    return nlevels;
  }

  /** quietly changes the level without notify listeners */
  public void setZoomLevel(int il) {
    if (il < 0) {
      il = 0; // weird
    } else if (il >= nlevels) {
      il = nlevels - 1; // weird
    }
    if (ilevel != il && il < nlevels) {
      ilevel = il; // YXXX this might cause lack of paints
      doRepaint();
    }
  }

  /** sets the current level and notifies listeners */
  private void setCurrentLevel(int il) {
    setZoomLevel(il);
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.SET_LEVEL, ilevel));
  }

  /** sets the current level and notifies listeners */
  private void setNotifyPlus() {
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.LEVEL_PLUS, 0));
  }

  /** sets the current level and notifies listeners */
  private void setNotifyMinus() {
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.LEVEL_MINUS, 0));
  }

  /** Notify listeners of a revert event */
  private void undo() {
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.UNDO, 0));
  }

  private void redo() {
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.REDO, 0));
  }

  /** Notify listeners of a pan event */
  private void pan(int idir) {
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.PAN, idir));
  }

  /** Notify listeners of a rest event */
  private void reset() {
    notifyListeners(new ZoomRulerEvent(ZoomRulerEvent.RESET, 0));
  }

  /** Notify listeners of a ZoomRulerEvent */
  private void notifyListeners(ZoomRulerEvent event) {
    for (ZoomRulerListener listener : listeners) {
      listener.zoomChanged(event);
    }
  }

  private boolean isInComponent(int x, int y) {
    if (x > xoff && x < xoff + getWidth() && y > yoff && y < yoff + getHeight()) {
      return true;
    } else {
      return false;
    }
  }

  private class TimelineMouseAdapter extends MouseAdapter {
    private Timer autoScrollTimer;
    private final int AUTO_PLUS_MINUS_REPEAT_TIME = 120; // ms between repeats
    private final int AUTO_PLUS_MINUS_REPEAT_INITIAL = 360; // ms before first repeat
    private int clickType = ZoomRulerEvent.UNKNOWN;

    public TimelineMouseAdapter() {
      if (autoRepeat) {
        autoScrollTimer =
            new Timer(
                AUTO_PLUS_MINUS_REPEAT_TIME,
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent ae) {
                    if (clickType == ZoomRulerEvent.LEVEL_MINUS) {
                      setNotifyMinus();
                    } else if (clickType == ZoomRulerEvent.LEVEL_PLUS) {
                      setNotifyPlus();
                    } else {
                      int ii = 1; // weird
                      autoScrollTimer.stop();
                    }
                  }
                });
        autoScrollTimer.setInitialDelay(AUTO_PLUS_MINUS_REPEAT_INITIAL);
        autoScrollTimer.setRepeats(true);
      }
    }

    public void mousePressed(MouseEvent event) {
      int button = event.getButton();
      recent_mouse_button = button;
      if (button != event.BUTTON1) {
        return;
      }
      if (!active) return;
      int x = event.getX();
      int y = event.getY();
      clickType = getClickType(x, y);
      if (clickType == ZoomRulerEvent.LEVEL_MINUS || clickType == ZoomRulerEvent.LEVEL_PLUS) {
        if (autoScrollTimer != null && !autoScrollTimer.isRunning()) {
          autoScrollTimer.start();
        }
      }
    }

    public void mouseReleased(MouseEvent event) {
      recent_mouse_button = -1;
      int button = event.getButton();
      if (button != event.BUTTON1) {
        return;
      }
      if (autoScrollTimer != null) {
        autoScrollTimer.stop();
      }

      if (!active && !dragging) return;
      int x = event.getX();
      int y = event.getY();
      // clickType<0
      if (dragging) {
        processDrag(x, y);
        setCurrentLevel(ilevel); // notifies listeners
        dragging = false;
        return;
      }
      int releaseClickType = getClickType(x, y);
      if (releaseClickType >= 0) {
        if (releaseClickType != clickType) {
          // mouse drifted across buttons, do nothing
          return;
        }
        switch (releaseClickType) {
          case ZoomRulerEvent.LEVEL_MINUS:
            setNotifyMinus();
            break;
          case ZoomRulerEvent.LEVEL_PLUS:
            setNotifyPlus();
            break;
          case ZoomRulerEvent.PAN_RIGHT:
          case ZoomRulerEvent.PAN_LEFT:
          case ZoomRulerEvent.PAN_UP:
          case ZoomRulerEvent.PAN_DOWN:
            pan(releaseClickType);
            break;
          case ZoomRulerEvent.REDO:
          case ZoomRulerEvent.UNDO:
            undo();
            break;
          case ZoomRulerEvent.RESET:
            reset();
            break;
          case ZoomRulerEvent.UNKNOWN:
          default:
            {
              int ii = 0; // weird
            }
        }
        return;
      } else { // negative value
        int il = (-releaseClickType) - 1;
        setCurrentLevel(il);
      }
    }

    public void mouseDragged(MouseEvent event) {
      if (recent_mouse_button != event.BUTTON1) {
        return;
      }

      if (!active) return;
      if (!hasSlider) return;
      if (clickType >= 0) return; // initial click was not on ruler
      int x = event.getX();
      int y = event.getY();

      /* process if the coordinate is in the boundaries of the overaly */
      if (dragging || isInComponent(x, y)) {
        processDrag(x, y);
        dragging = true;
      }
    }

    public void mouseMoved(MouseEvent e) {}

    public void mouseClicked(MouseEvent event) {}
  }

  /** Add listeners to the component to handle mouse events in the overlay */
  private void initializeMouseListeners() {
    if (component == null) {
      return;
    }
    TimelineMouseAdapter timelineMouseAdapter = new TimelineMouseAdapter();
    component.addMouseWheelListener(timelineMouseAdapter);
    component.addMouseMotionListener(timelineMouseAdapter);
    component.addMouseListener(timelineMouseAdapter);
  }

  /** Add ZoomRulerListener */
  public void addZoomRulerListener(ZoomRulerListener listener) {
    listeners.add(listener);
  }

  /** Draw the Ruler */
  public void paintRuler(Graphics g) {
    if (!isEnabled) {
      return;
    }
    if (component == null) {
      return;
    }
    if (paintBackground) {
      g.setColor(component.getBackground());
      g.fillRect(xoff, yoff, getWidth(), getHeight());
    }

    //        Graphics2D g2d = (Graphics2D) g;
    //        RenderingHints rhints = g2d.getRenderingHints();
    //        boolean antialiasOn = rhints.containsValue(RenderingHints.VALUE_ANTIALIAS_ON);
    //        if(!antialiasOn){
    //            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //                    RenderingHints.VALUE_ANTIALIAS_ON);
    //        }

    paintRuler(g, xoff, yoff);
    if (PANNING) {
      paintPanButtons(g, xoff, yoff);
    }

    //        if(!antialiasOn){
    //            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //                    RenderingHints.VALUE_ANTIALIAS_OFF);
    //        }
  }

  private void paintPanButtons(Graphics g, final int xstart, final int ystart) {
    // pan buttons would need work to make them usable
    final int sz = ANCHOR1_OFFSET_LD;
    g.setColor(borderColor);
    g.drawRect(BORDER, 1, ANCHOR_SIZE, ANCHOR_SIZE);
    g.drawRect(1, BORDER, ANCHOR_SIZE, ANCHOR_SIZE);
    g.drawRect(BORDER, BORDER, ANCHOR_SIZE, ANCHOR_SIZE);
    g.drawRect(BORDER + BORDER, BORDER, ANCHOR_SIZE, ANCHOR_SIZE);
    g.drawRect(BORDER, BORDER + BORDER, ANCHOR_SIZE, ANCHOR_SIZE);
    // g.fillRect(xstart, ystart, (2 * BORDER + ANCHOR_SIZE), (2 * BORDER + ANCHOR_SIZE));

    int ps = revertImg.getIconHeight();
    int pb = (sz - ps) / 2;
    g.setColor(buttonFillColor);
    g.fillOval(xstart + pb, ystart + pb, ps, ps);
    g.setColor(borderColor);
    //            g.drawOval(xstart+pb, ystart+pb, ps, ps);
    int xend = xstart + sz - pb;
    int yend = ystart + sz - pb;
    int xmid = xstart + pb + ps / 2;
    int ymid = ystart + pb + ps / 2;
    g.drawLine(xstart + pb, ymid, xend, ymid);
    g.drawLine(xmid, ystart + pb, xmid, yend);

    int xps[] = new int[3];
    int yps[] = new int[3];
    xps[0] = xmid;
    xps[1] = xmid + ARROW_WIDTH;
    xps[2] = xmid - ARROW_WIDTH;
    yps[0] = ystart + pb;
    yps[1] = ystart + pb + ARROW_LENGTH;
    yps[2] = ystart + pb + ARROW_LENGTH;
    g.fillPolygon(xps, yps, 3);

    xps[0] = xmid;
    xps[1] = xmid + ARROW_WIDTH;
    xps[2] = xmid - ARROW_WIDTH;
    yps[0] = yend;
    yps[1] = yend - ARROW_LENGTH;
    yps[2] = yend - ARROW_LENGTH;
    g.fillPolygon(xps, yps, 3);

    xps[0] = xstart + pb;
    xps[1] = xstart + pb + ARROW_LENGTH;
    xps[2] = xstart + pb + ARROW_LENGTH;
    yps[0] = ymid;
    yps[1] = ymid + ARROW_WIDTH;
    yps[2] = ymid - ARROW_WIDTH;
    g.fillPolygon(xps, yps, 3);

    xps[0] = xend;
    xps[1] = xend - ARROW_LENGTH;
    xps[2] = xend - ARROW_LENGTH;
    yps[0] = ymid;
    yps[1] = ymid + ARROW_WIDTH;
    yps[2] = ymid - ARROW_WIDTH;
    g.fillPolygon(xps, yps, 3);
  }

  private void paintRuler(Graphics g, final int inX, final int inY) {
    for (int ii = 0; ii < 4; ii++) {
      if (!useLabelShadow && ii == 2) {
        continue;
      }
      final int xOffset, yOffset; // includes shadow shift
      //            final int xstart, ystart;
      final Color lineColor;
      final Color fillColor;
      final boolean doLabel;
      final boolean doFill;
      final boolean doIcons;
      final boolean doBorder;
      switch (ii) {
        case 0:
          // border shadow
          doFill = true;
          doBorder = true;
          doLabel = false;
          doIcons = false;
          lineColor = shadowColor;
          fillColor = shadowColor;
          xOffset = inX + 1;
          yOffset = inY + 1;
          break;
        case 1:
          // border main
          doFill = true;
          doBorder = true;
          doLabel = false;
          doIcons = true;
          lineColor = borderColor;
          fillColor = buttonFillColor;
          xOffset = inX;
          yOffset = inY;
          break;
        case 2:
          // label shadow
          doFill = false;
          doBorder = false;
          doLabel = true;
          doIcons = false;
          lineColor = labelShadowColor;
          fillColor = labelShadowColor;
          xOffset = inX + 1;
          yOffset = inY + 1;
          break;
        case 3:
        default:
          // label main
          doFill = false;
          doBorder = false;
          doLabel = true;
          doIcons = false;
          lineColor = borderColor;
          fillColor = buttonFillColor;
          xOffset = inX;
          yOffset = inY;
          break;
      }

      // Draw anchors and main bar
      {
        final int ix1, iy1;
        final int ix2, iy2;
        final int sx, sy, sl, sw;
        if (orientation == this.HORIZONTAL_ORIENTATION) {
          ix1 = xOffset + ANCHOR1_OFFSET_LD;
          iy1 = yOffset + ANCHOR1_OFFSET_SD;
          ix2 = xOffset + ANCHOR2_OFFSET_LD;
          iy2 = yOffset + ANCHOR2_OFFSET_SD;
          sx = ix1 + ANCHOR_SIZE;
          sy = iy1 + ANCHOR_SIZE / 2 - 2;
          sw = RULER_LD;
          sl = RULER_SD;
        } else {
          ix1 = xOffset + ANCHOR1_OFFSET_SD;
          iy1 = yOffset + ANCHOR1_OFFSET_LD;
          ix2 = xOffset + ANCHOR2_OFFSET_SD;
          iy2 = yOffset + ANCHOR2_OFFSET_LD;
          sx = ix1 + ANCHOR_SIZE / 2 - 2;
          sy = iy1 + ANCHOR_SIZE;
          sw = RULER_SD;
          sl = RULER_LD;
        }
        if (doFill) {
          g.setColor(fillColor);
          myFillRect(g, ix1, iy1, ANCHOR_SIZE, ANCHOR_SIZE);
          myFillRect(g, ix2, iy2, ANCHOR_SIZE, ANCHOR_SIZE);
          if (hasSlider) {
            myFillRect(g, sx, sy, sw, sl);
          }
        }
        if (doBorder) {
          g.setColor(lineColor);
          myDrawRect(g, ix1, iy1, ANCHOR_SIZE, ANCHOR_SIZE);
          myDrawRect(g, ix2, iy2, ANCHOR_SIZE, ANCHOR_SIZE);
          if (hasSlider) {
            myDrawRect(g, sx, sy, sw, sl);
          }
        }
        if (doLabel) {
          g.setColor(lineColor);
          if (orientation == this.HORIZONTAL_ORIENTATION) {
            drawMinusLabel(g, ix1, iy1, ANCHOR_SIZE, ANCHOR_SIZE);
            drawPlusLabel(g, ix2, iy2, ANCHOR_SIZE, ANCHOR_SIZE);
          } else {
            drawPlusLabel(g, ix1, iy1, ANCHOR_SIZE, ANCHOR_SIZE);
            drawMinusLabel(g, ix2, iy2, ANCHOR_SIZE, ANCHOR_SIZE);
          }
        }

        if (hasSlider && nlevels > 0) {
          final float wp = pixelsPerLevel();
          final int ix, iy, length, width;
          if (orientation == HORIZONTAL_ORIENTATION) {
            iy = iy1 + ANCHOR_SIZE / 2 - MARKER_SD / 2;
            ix = sx + Math.round(wp * ilevel);
            length = MARKER_LD;
            width = MARKER_SD;
          } else {
            iy = sy + Math.round(wp * (nlevels - 1 - ilevel));
            ix = ix1 + ANCHOR_SIZE / 2 - MARKER_SD / 2;
            width = MARKER_LD;
            length = MARKER_SD;
          }
          if (doFill) {
            g.setColor(fillColor);
            myFillRect(g, ix, iy, length, width);
          }
          if (doBorder) {
            g.setColor(lineColor);
            myDrawRect(g, ix, iy, length, width);
          }
        }
      }

      if (resetable1d || resetable2d) {
        final int resetX, resetY;
        if (orientation == this.HORIZONTAL_ORIENTATION) {
          resetX = xOffset + RESET_OFFSET_LD;
          resetY = yOffset + RESET_OFFSET_SD;
        } else {
          resetX = xOffset + RESET_OFFSET_SD;
          resetY = yOffset + RESET_OFFSET_LD;
        }
        if (resetable1d) {
          if (doFill) {
            g.setColor(fillColor);
            myFillRect(g, resetX, resetY, ANCHOR_SIZE, ANCHOR_SIZE);
          }
          if (doBorder) {
            g.setColor(lineColor);
            myDrawRect(g, resetX, resetY, ANCHOR_SIZE, ANCHOR_SIZE);
          }
          if (doLabel) {
            g.setColor(lineColor);
            drawResetLeftLabel(g, resetX, resetY, ANCHOR_SIZE, ANCHOR_SIZE);
          }
        } else if (resetable2d) {
          if (doIcons) {
            final int rx, ry;
            if (orientation == HORIZONTAL_ORIENTATION) {
              int offset = (ANCHOR_SIZE - resetImg.getIconWidth()) / 2;
              rx = resetX + BORDER;
              ry = resetY + offset + BORDER;
            } else {
              int offset = (ANCHOR_SIZE - resetImg.getIconHeight()) / 2;
              rx = resetX + offset + BORDER;
              ry = resetY + BORDER;
            }
            g.drawImage(resetImg.getImage(), rx, ry, null);
          }
        }
      }

      if (revertable) {
        if (doIcons) {
          final int rx, ry;
          if (orientation == HORIZONTAL_ORIENTATION) {
            int iy = (REVERT_OFFSET_SD - revertImg.getIconHeight()) / 2 + 2;
            rx = xOffset + REVERT_OFFSET_LD;
            ry = yOffset + iy;
          } else {
            int ix = (REVERT_OFFSET_SD - revertImg.getIconWidth()) / 2 + 1;
            rx = xOffset + ix;
            ry = yOffset + REVERT_OFFSET_LD;
          }
          g.drawImage(revertImg.getImage(), rx, ry, null);
        }
      }
    }
  }

  private void myDrawRect(Graphics g, int x1, int y1, int x2, int y2) {
    if (rectRadius > 1) {
      g.drawRoundRect(x1, y1, x2, y2, rectRadius, rectRadius);
    } else {
      g.drawRect(x1, y1, x2, y2);
    }
  }

  private void myFillRect(Graphics g, int x1, int y1, int w, int h) {
    if (rectRadius > 1) {
      g.fillRoundRect(x1, y1, w, h, rectRadius, rectRadius);
    } else {
      g.fillRect(x1, y1, w, h);
    }
  }

  private void drawMinusLabel(Graphics g, final int x, final int y, int width, int height) {
    final int x1 = x + LABEL_BORDER;
    final int y1 = y + height / 2;
    final int x2 = x + width - LABEL_BORDER;
    final int y2 = y1;
    g.drawLine(x1, y1, x2, y2);
  }

  private void drawResetLeftLabel(Graphics g, final int x, final int y, int width, int height) {
    final int x1 = x + (width - ARROW_LENGTH) / 2;
    final int yBorder = (height - ARROW_WIDTH * 2) / 2 + 1;
    final int y1 = y + yBorder;
    final int x2 = x1;
    final int y2 = y + width - yBorder;
    g.drawLine(x1, y1, x2, y2);
    // triangle:
    {
      int xps[] = new int[3];
      int yps[] = new int[3];
      final int ymid = y + height / 2;
      final int xstart = x1 + 1;
      xps[0] = xstart;
      xps[1] = xstart + ARROW_LENGTH;
      xps[2] = xstart + ARROW_LENGTH;
      yps[0] = ymid;
      yps[1] = ymid + ARROW_WIDTH;
      yps[2] = ymid - ARROW_WIDTH;
      g.fillPolygon(xps, yps, 3);
    }
  }

  private void drawPlusLabel(Graphics g, final int x, final int y, int width, int height) {
    drawMinusLabel(g, x, y, width, height);
    final int x1 = x + width / 2;
    final int y1 = y + LABEL_BORDER;
    final int x2 = x1;
    final int y2 = y + width - LABEL_BORDER;
    g.drawLine(x1, y1, x2, y2);
  }

  /** Handle a mouse click at (xin, yin) */
  // NOTE: this method should not change class state.
  private int getClickType(final int _mouseX, final int _mouseY) {
    final int mouseX = _mouseX - 1;
    final int mouseY = _mouseY - 1;

    /* create local x and y relative to the Overlay */
    if (!active) return ZoomRulerEvent.UNKNOWN;
    if (!isInComponent(mouseX, mouseY)) return ZoomRulerEvent.UNKNOWN;

    final int xl, yl;
    if (orientation == this.VERTICAL_ORIENTATION) {
      /* the logic is set up for a horizontal orientation, swap the
      values */
      xl = mouseY - yoff;
      yl = mouseX - xoff;
    } else {
      xl = mouseX - xoff;
      yl = mouseY - yoff;
    }

    /* if PANNING is supported, handle it here */
    if (nlevels <= 0)
      return ZoomRulerEvent.UNKNOWN;

    /* check for click on ZoomOut Anchor */
    if (xl >= ANCHOR1_OFFSET_LD
        && xl <= ANCHOR1_OFFSET_LD + ANCHOR_SIZE
        && yl >= ANCHOR1_OFFSET_SD
        && yl <= ANCHOR1_OFFSET_SD + ANCHOR_SIZE) {
      if (orientation == this.VERTICAL_ORIENTATION) {
        if (ilevel + 1 < nlevels) {
          return ZoomRulerEvent.LEVEL_PLUS;
        }
      } else {
        if (ilevel > 0) {
          return ZoomRulerEvent.LEVEL_MINUS;
        }
      }
      return ZoomRulerEvent.UNKNOWN;
    }
    /* check for a click on the ZoomIn Anchor */
    if (xl >= ANCHOR2_OFFSET_LD
        && xl <= ANCHOR2_OFFSET_LD + ANCHOR_SIZE
        && yl >= ANCHOR2_OFFSET_SD
        && yl <= ANCHOR2_OFFSET_SD + ANCHOR_SIZE) {
      if (orientation == this.VERTICAL_ORIENTATION) {
        if (ilevel > 0) {
          return ZoomRulerEvent.LEVEL_MINUS;
        }
      } else {
        if (ilevel + 1 < nlevels) {
          return ZoomRulerEvent.LEVEL_PLUS;
        }
      }
      return ZoomRulerEvent.UNKNOWN;
    }
    /* check for a click on the ruler */
    if (hasSlider
        && xl > ANCHOR1_OFFSET_LD + ANCHOR_SIZE
        && xl < ANCHOR2_OFFSET_LD
        && yl > ANCHOR1_OFFSET_SD
        && yl < ANCHOR1_OFFSET_SD + ANCHOR_SIZE) {
      final int levelValue = getLevel(_mouseX, _mouseY);
      return -(levelValue + 1);
    }
    /* check for a click on the 1d reset button*/
    if (resetable1d) {
      int s1 = RESET_OFFSET_LD;
      int s2 = RESET_OFFSET_SD;
      int h = ANCHOR_SIZE;
      int w = ANCHOR_SIZE;
      if (xl > s1 && xl < s1 + w && yl > s2 && yl < s2 + h) {
        return ZoomRulerEvent.RESET;
      }
    }
    /* check for a click on the 2d reset button*/
    if (resetable2d) {
      final int h, w;
      if (orientation == this.VERTICAL_ORIENTATION) {
        // reversed
        w = resetImg.getIconHeight();
        h = resetImg.getIconWidth();
      } else {
        h = resetImg.getIconHeight();
        w = resetImg.getIconWidth();
      }
      final int s1 = RESET_OFFSET_LD;
      final int s2 = RESET_OFFSET_SD + (ANCHOR_SIZE - h) / 2;
      if (xl > s1 && xl < s1 + w && yl > s2 && yl < s2 + h) {
        return ZoomRulerEvent.RESET;
      }
    }
    /* check for a click on the revert button*/
    if (revertable) {
      final int h, w;
      if (orientation == this.VERTICAL_ORIENTATION) {
        // reversed
        w = revertImg.getIconHeight();
        h = revertImg.getIconWidth();
      } else {
        h = revertImg.getIconHeight();
        w = revertImg.getIconWidth();
      }
      int s1 = REVERT_OFFSET_LD;
      final int s2 = RESET_OFFSET_SD + (ANCHOR_SIZE - h) / 2;
      if (xl > s1 && xl < s1 + w && yl > s2 && yl < s2 + h) {
        return ZoomRulerEvent.UNDO;
      }
    }
    return ZoomRulerEvent.UNKNOWN;
  }

  /** Process a mouse drag at (x, y). */
  private void processDrag(final int _mouseX, final int _mouseY) {
    if (!active && !dragging) return;
    if (nlevels <= 0) return;
    if (!hasSlider) return;
    final int levelValue = getLevel(_mouseX, _mouseY);
    setZoomLevel(levelValue);
  }

  private float pixelsPerLevel() {
    return (float) (RULER_LD - MARKER_LD) / (nlevels - 1);
  }

  private int getLevel(final int _mouseX, final int _mouseY) {
    final int mouseX = _mouseX - 1;
    final int mouseY = _mouseY - 1;
    final int sliderX;
    if (orientation == this.VERTICAL_ORIENTATION) {
      /* vertical ruler level decreases as y values increase, so invert
       * coordiantes on the ruler, if outside the ruler it may be on the
       * revert button */
      int offsetAdjusted = mouseY - yoff;
      sliderX = (ANCHOR2_OFFSET_LD - MARKER_LD / 2) - offsetAdjusted - 1;
    } else {
      int offsetAdjusted = mouseX - xoff;
      sliderX = offsetAdjusted - (ANCHOR1_OFFSET_LD + ANCHOR_SIZE + MARKER_LD / 2);
    }

    /* subtract of pan area */
    final float pixels_per_level = pixelsPerLevel();
    int levelValue = Math.round(sliderX / pixels_per_level);
    if (levelValue < 0) {
      levelValue = 0;
    }
    if (levelValue > nlevels - 1) {
      levelValue = nlevels - 1;
    }
    return levelValue;
  }

  /** Check if a (x,y) coordinate is with the overlay. */
  public boolean withinBounds(int x, int y) {
    x -= xoff;
    y -= yoff;
    int w = getWidth();
    if (x < 0 || x > w) {
      return false;
    }
    int h = getHeight();
    if (y < 0 || y > h) {
      return false;
    }
    return true;
  }

  /** Turn on mouse handling */
  public void activateMouseHandling() {
    active = true;
  }

  /** Turn off mouse handling */
  public void deactivateMouseHandling() {
    active = false;
  }

  /** Set the component that the overlay is drawn on */
  public void setComponent(JComponent component) {
    this.component = component;
    initializeMouseListeners();
  }

  /** Determine if a mouse drag is occuring */
  public boolean isDragging() {
    return dragging;
  }

  /** Return dimensions of the overlay */
  public static int getDefaultDimension() {
    return TOTAL_SD;
  }

  /** Set the offset at which to draw the overlay */
  public void setOffsets(int x, int y) {
    if (this.xoff != x || this.yoff != y) {
      this.xoff = x;
      this.yoff = y;
      doRepaint();
    }
  }

  /*
   * Set the boolean to indicate wether to paint a solid background or let the
   * underlying image show through
   */
  public void setPaintBackground(boolean pb) {
    this.paintBackground = pb;
  }

  public void show(boolean show) {
    if (isEnabled != show) {
      isEnabled = show;
      dragging = false;
      active = isEnabled; // YXXX not sure if this is right
      doRepaint();
    }
  }

  private void doRepaint() {
    if (component != null) {
      component.repaint();
    }
  }
}
