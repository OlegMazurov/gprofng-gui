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

package org.gprofng.mpmt.util.gui;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollBarUI;

public class AnScrollBarUI extends MetalScrollBarUI {

  private static final int gap = 4;
  private Color backgroundColor;
  private static JComponent mouseInsideComponent = null;

  public static ComponentUI createUI(final JComponent c) {
    c.addMouseListener(
        new MouseListener() {
          @Override
          public void mouseEntered(MouseEvent e) {
            mouseInsideComponent = c;
            c.repaint();
          }

          @Override
          public void mouseExited(MouseEvent e) {
            mouseInsideComponent = null;
            c.repaint();
          }

          @Override
          public void mouseClicked(MouseEvent e) {}

          @Override
          public void mousePressed(MouseEvent e) {}

          @Override
          public void mouseReleased(MouseEvent e) {}
        });
    return new AnScrollBarUI();
  }

  @Override
  protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
    backgroundColor = g.getColor();

    // Fill
    g.setColor(AnEnvironment.SCROLLBAR_TRACK_COLOR);
    g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

    // Border
    g.setColor(AnEnvironment.SCROLLBAR_BORDER_COLOR);
    if (trackBounds.width < trackBounds.height) {
      g.drawLine(trackBounds.x, trackBounds.y, 0, trackBounds.height + 20); // Why 20 extra?
    } else {
      g.drawLine(
          trackBounds.x, trackBounds.y, trackBounds.width + 20, trackBounds.y); // Why 20 extra?
    }
  }

  @Override
  protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
    // Knob
    if (mouseInsideComponent == c) {
      g.setColor(AnEnvironment.SCROLLBAR_THUMB_FOCUSED_COLOR);
    } else {
      g.setColor(AnEnvironment.SCROLLBAR_THUMB_COLOR);
    }
    g.fillRect(
        thumbBounds.x + gap,
        thumbBounds.y + gap,
        thumbBounds.width - 2 * gap + 1,
        thumbBounds.height - 2 * gap + 1);

    // Round the corners
    //        g.setColor(AnEnvironment.SCROLLBAR_TRACK_COLOR);
    g.setColor(backgroundColor);
    int x = thumbBounds.x;
    int y = thumbBounds.y;
    int width = thumbBounds.width - 2 * gap;
    int height = thumbBounds.height - 2 * gap;

    // More rounded corners
    //        g.drawLine(x + gap, y + gap, x + gap + 1, y + gap);
    //        g.drawLine(x + gap, y + gap, x + gap, y + gap + 1);
    //        g.drawLine(x + gap + width - 2, y + gap, x + gap + width - 1, y + gap);
    //        g.drawLine(x + gap + width - 1, y + gap, x + gap + width - 1, y + gap + 1);
    //        g.drawLine(x + gap, y + gap + height - 2, x + gap, y + gap + height - 1);
    //        g.drawLine(x + gap, y + gap + height - 1, x + gap + 1, y + gap + height - 1);
    //        g.drawLine(x + gap + width - 2, y + gap + height - 1, x + gap + width - 1, y + gap +
    // height - 1);
    //        g.drawLine(x + gap + width - 1, y + gap + height - 2, x + gap + width - 1, y + gap +
    // height - 1);
    // One pixel rounded corners
    g.drawLine(x + gap, y + gap, x + gap, y + gap);
    g.drawLine(x + gap + width, y + gap, x + gap + width, y + gap);
    g.drawLine(x + gap, y + gap + height, x + gap, y + gap + height);
    g.drawLine(x + gap + width, y + gap + height, x + gap + width, y + gap + height);
  }

  @Override
  protected JButton createDecreaseButton(int orientation) {
    JButton button = new Button(orientation);
    return button;
  }

  @Override
  protected JButton createIncreaseButton(int orientation) {
    JButton button = new Button(orientation);
    return button;
  }

  @Override
  protected Dimension getMinimumThumbSize() {
    return new Dimension(28, 28);
  }

  class Button extends JButton {
    private static final int buttonSize = 14;
    private int orientation;
    private boolean mouseInsideButton = false;

    public Button(int orientation) {
      this.orientation = orientation;
      setMaximumSize(new Dimension(buttonSize, buttonSize));
      setMinimumSize(new Dimension(buttonSize, buttonSize));
      setPreferredSize(new Dimension(buttonSize, buttonSize));

      addMouseListener(
          new MouseListener() {

            @Override
            public void mouseEntered(MouseEvent e) {
              mouseInsideButton = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
              mouseInsideButton = false;
            }

            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}
          });
    }

    @Override
    public void paint(Graphics g) {
      int w = getWidth();
      int h = getHeight();

      // Fill the area with track color
      g.setColor(AnEnvironment.SCROLLBAR_TRACK_COLOR);
      g.fillRect(0, 0, w, h);

      // paint Increment/Decrement buttons if thumb is vivible
      Rectangle rec = getThumbBounds();
      if (rec.height > 0) {
        if (mouseInsideButton) {
          g.setColor(AnEnvironment.SCROLLBAR_INC_DEC_FOCUSED_COLOR);
        } else {
          g.setColor(AnEnvironment.SCROLLBAR_INC_DEC_COLOR);
        }
        if (orientation == MetalScrollBarUI.NORTH) {
          g.drawLine(7, 4, 7, 4);
          g.drawLine(6, 5, 8, 5);
          g.drawLine(5, 6, 9, 6);
          g.drawLine(4, 7, 10, 7);
          g.setColor(AnEnvironment.SCROLLBAR_BORDER_COLOR);
          g.drawLine(0, 0, 0, h - 1);
          g.drawLine(0, h - 1, w, h - 1);
        } else if (orientation == MetalScrollBarUI.SOUTH) {
          g.drawLine(4, 5, 10, 5);
          g.drawLine(5, 6, 9, 6);
          g.drawLine(6, 7, 8, 7);
          g.drawLine(7, 8, 7, 8);
          g.setColor(AnEnvironment.SCROLLBAR_BORDER_COLOR);
          g.drawLine(0, 0, 0, h - 1);
          g.drawLine(0, 0, w, 0);
          g.drawLine(0, h - 1, w, h - 1);
        } else if (orientation == MetalScrollBarUI.WEST) {
          g.drawLine(4, 7, 4, 7);
          g.drawLine(5, 6, 5, 8);
          g.drawLine(6, 5, 6, 9);
          g.drawLine(7, 4, 7, 10);
          g.setColor(AnEnvironment.SCROLLBAR_BORDER_COLOR);
          g.drawLine(0, 0, w - 1, 0);
          g.drawLine(w - 1, 0, w - 1, h - 1);
        } else if (orientation == MetalScrollBarUI.EAST) {
          g.drawLine(5, 4, 5, 10);
          g.drawLine(6, 5, 6, 9);
          g.drawLine(7, 6, 7, 8);
          g.drawLine(8, 7, 8, 7);
          g.setColor(AnEnvironment.SCROLLBAR_BORDER_COLOR);
          g.drawLine(0, 0, w - 1, 0);
          g.drawLine(0, 0, 0, h - 1);
          g.drawLine(w - 1, 0, w - 1, h - 1);
        }
      }
    }
  }
}
