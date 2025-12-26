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

package org.gprofng.mpmt.util.gui;

import org.gprofng.analyzer.AnEnvironment;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class AnSplitPane extends JSplitPane {

  private boolean hidden = false;
  private boolean isDragging = false;
  public static final int defaultDividerSize = 6;
  public static final int defaultDividerSizeSmall = 2;

  private int locationWhenShowing = 0;
  private int dividerSizeWhenShowing = 0;

  static {
    UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder()); // NOI18N
  }
  ;

  public AnSplitPane(int orientation, Component leftComponent, Component rightComponent) {
    this(orientation, leftComponent, rightComponent, -1);
  }

  public AnSplitPane(
      int orientation, Component leftComponent, Component rightComponent, int location) {
    super(orientation, leftComponent, rightComponent);
    init();
    if (location >= 0) {
      setDividerLocation(location);
    }
    setBackground(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);
  }

  private void init() {
    setContinuousLayout(true);
    setDividerSize(defaultDividerSize);
    //        setOneTouchExpandable(true);
    setUI(
        new BasicSplitPaneUI() {
          private final int oneTouchSize = 5;

          @Override
          public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
              private boolean mouseInsideRight;
              private boolean mouseInsideLeft;

              @Override
              public void setBorder(Border b) {}

              @Override
              public void paint(Graphics g) {
                g.setColor(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);
                super.paint(g);
              }

              /**
               * Creates and return an instance of JButton that can be used to collapse the left
               * component in the split pane.
               */
              /*
               * Code copied from BasicSplitPaneDivider.java
               */
              @Override
              protected JButton createLeftOneTouchButton() {
                JButton b =
                    new JButton() {
                      @Override
                      public void setBorder(Border b) {}

                      @Override
                      public void paint(Graphics g) {
                        if (splitPane != null) {
                          int[] xs = new int[3];
                          int[] ys = new int[3];
                          int blockSize;

                          // Fill the background first ......
                          g.setColor(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);
                          g.fillRect(0, 0, this.getWidth(), this.getHeight());

                          // ... then draw the arrow.
                          if (mouseInsideLeft) {
                            g.setColor(AnEnvironment.SPLIT_PANE_ONE_TOUCH_BUTTON_FOCUSED_COLOR);
                          } else {
                            g.setColor(AnEnvironment.SPLIT_PANE_ONE_TOUCH_BUTTON_COLOR);
                          }
                          if (orientation == JSplitPane.VERTICAL_SPLIT) {
                            blockSize = Math.min(getHeight(), oneTouchSize);
                            xs[0] = blockSize;
                            xs[1] = 0;
                            xs[2] = blockSize << 1;
                            ys[0] = 0;
                            ys[1] = blockSize;
                            ys[2] = blockSize;
                            g.drawPolygon(xs, ys, 3);
                          } else {
                            blockSize = Math.min(getWidth(), oneTouchSize);
                            xs[0] = blockSize;
                            xs[1] = 0;
                            xs[2] = blockSize;
                            ys[0] = 0;
                            ys[1] = blockSize;
                            ys[2] = blockSize << 1;
                          }
                          g.fillPolygon(xs, ys, 3);

                          g.setColor(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);
                          // g.setColor(Color.red);
                          if (orientation == JSplitPane.VERTICAL_SPLIT) {
                            g.drawLine(
                                0, this.getHeight() - 1, this.getWidth() - 1, this.getHeight() - 1);
                          } else {
                            g.drawLine(
                                this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight() - 1);
                          }
                        }
                      }
                      // Don't want the button to participate in focus traversable.

                      @Override
                      public boolean isFocusTraversable() {
                        return false;
                      }
                    };
                b.setMinimumSize(new Dimension(oneTouchSize, oneTouchSize));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                b.setRequestFocusEnabled(false);
                b.addMouseListener(
                    new MouseListener() {
                      @Override
                      public void mouseClicked(MouseEvent e) {}

                      @Override
                      public void mousePressed(MouseEvent e) {}

                      @Override
                      public void mouseReleased(MouseEvent e) {}

                      @Override
                      public void mouseEntered(MouseEvent e) {
                        mouseInsideLeft = true;
                        repaint();
                      }

                      @Override
                      public void mouseExited(MouseEvent e) {
                        mouseInsideLeft = false;
                        repaint();
                      }
                    });
                return b;
              }

              /**
               * Creates and return an instance of JButton that can be used to collapse the right
               * component in the split pane.
               */
              /*
               * Code copied from BasicSplitPaneDivider.java
               */
              @Override
              protected JButton createRightOneTouchButton() {
                JButton b =
                    new JButton() {
                      @Override
                      public void setBorder(Border border) {}

                      @Override
                      public void paint(Graphics g) {
                        if (splitPane != null) {
                          int[] xs = new int[3];
                          int[] ys = new int[3];
                          int blockSize;

                          // Fill the background first ...
                          g.setColor(AnEnvironment.SPLIT_PANE_DIVIDER_BACKGROUND_COLOR);
                          g.fillRect(0, 0, this.getWidth(), this.getHeight());

                          // ... then draw the arrow.
                          if (orientation == JSplitPane.VERTICAL_SPLIT) {
                            blockSize = Math.min(getHeight(), oneTouchSize);
                            xs[0] = blockSize - 1;
                            xs[1] = (blockSize << 1) - 1;
                            xs[2] = 0;
                            ys[0] = blockSize + 1;
                            ys[1] = 0 + 1;
                            ys[2] = 0 + 1;
                          } else {
                            blockSize = Math.min(getWidth(), oneTouchSize);
                            xs[0] = 0 + 1;
                            xs[1] = blockSize + 1;
                            xs[2] = 0 + 1;
                            ys[0] = 0;
                            ys[1] = blockSize;
                            ys[2] = blockSize << 1;
                          }
                          if (mouseInsideRight) {
                            g.setColor(AnEnvironment.SPLIT_PANE_ONE_TOUCH_BUTTON_FOCUSED_COLOR);
                          } else {
                            g.setColor(AnEnvironment.SPLIT_PANE_ONE_TOUCH_BUTTON_COLOR);
                          }
                          g.fillPolygon(xs, ys, 3);
                        }
                      }
                      // Don't want the button to participate in focus traversable.

                      @Override
                      public boolean isFocusTraversable() {
                        return false;
                      }
                    };
                b.setMinimumSize(new Dimension(oneTouchSize, oneTouchSize));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                b.setRequestFocusEnabled(false);
                b.addMouseListener(
                    new MouseListener() {
                      @Override
                      public void mouseClicked(MouseEvent e) {}

                      @Override
                      public void mousePressed(MouseEvent e) {}

                      @Override
                      public void mouseReleased(MouseEvent e) {}

                      @Override
                      public void mouseEntered(MouseEvent e) {
                        mouseInsideRight = true;
                        repaint();
                      }

                      @Override
                      public void mouseExited(MouseEvent e) {
                        mouseInsideRight = false;
                        repaint();
                      }
                    });
                return b;
              }
            };
          }

          @Override
          protected void dragDividerTo(int location) {
            isDragging = true;
            super.dragDividerTo(location);
            isDragging = false;
          }
        });
  }

  protected boolean isDraggingDivider() {
    return isDragging;
  }

  public void setHidden(boolean hide) {
    if (hide) {
      if (!hidden) {
        locationWhenShowing = getDividerLocation();
        dividerSizeWhenShowing = getDividerSize();
        setDividerLocation(0);
        setDividerSize(0);
        hidden = true;
      }
    } else if (hidden) {
      hidden = false;
      setDividerLocation(locationWhenShowing);
      setDividerSize(dividerSizeWhenShowing);
    }
  }

  public boolean isHidden() {
    return hidden;
  }

  public int getDividerLocationWhenShowing() {
    if (isHidden()) {
      return locationWhenShowing;
    } else {
      return getDividerLocation();
    }
  }
}
