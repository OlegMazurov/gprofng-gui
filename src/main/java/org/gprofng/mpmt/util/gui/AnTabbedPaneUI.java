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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public class AnTabbedPaneUI extends BasicTabbedPaneUI {
  private static int tabHeight = -1;

  @Override
  protected void installDefaults() {
    super.installDefaults();

    tabAreaInsets = new Insets(0, 0, 0, 0);
    tabInsets = new Insets(0, 0, 0, 1);

    TabbedPaneMouseListener mouseListener = new TabbedPaneMouseListener();
    tabPane.addMouseListener(mouseListener);
    tabPane.addMouseMotionListener(mouseListener);
  }

  @Override
  protected JButton createScrollButton(int direction) {
    JButton button =
        super.createScrollButton(
            direction); // To change body of generated methods, choose Tools | Templates.
    button.getAccessibleContext().setAccessibleDescription("");
    return button;
  }

  @Override
  protected Insets getContentBorderInsets(int tabPlacement) {
    return new Insets(0, 0, 0, 0);
  }

  @Override
  protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
    return getTabHeight();
  }

  @Override
  protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
    // Make the width 10 pixels wider. May have to make room for cancel image.
    return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 10;
  }

  @Override
  protected int calculateMaxTabHeight(int tabPlacement) {
    return getTabHeight();
  }

  @Override
  protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
    Color topColor = AnEnvironment.TABBED_PANE_TAB_AREA_TOP_COLOR;
    Color bottomColor = AnEnvironment.TABBED_PANE_TAB_AREA_BOTTOM_COLOR;
    Graphics2D g2d = (Graphics2D) g;
    GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getTabHeight(), bottomColor);
    g2d.setPaint(gp);
    g2d.fillRect(0, 0, tabPane.getWidth(), getTabHeight());

    super.paintTabArea(g, tabPlacement, selectedIndex);

    //        g.setColor(AnEnvironment.TABBED_PANE_LINE_COLOR);
    //        g.setColor(Color.red);
    //        g.drawLine(0, getTabHeight() - 1, tabPane.getWidth() - 1, getTabHeight() - 1);
    //        g.drawLine(0, getTabHeight() - 0, tabPane.getWidth() - 1, getTabHeight() - 0);
  }

  @Override
  protected void paintTabBackground(
      Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
    Color topColor;
    Color bottomColor;
    if (isSelected) {
      topColor = AnEnvironment.TABBED_PANE_SELECTED_TAB_COLOR;
      bottomColor = AnEnvironment.TABBED_PANE_SELECTED_TAB_COLOR;
    } else if (getRolloverTab() == tabIndex) {
      topColor = AnEnvironment.TABBED_PANE_FOCUSED_TAB_TOP_COLOR;
      bottomColor = AnEnvironment.TABBED_PANE_FOCUSED_TAB_BOTTOM_COLOR;
    } else {
      topColor = AnEnvironment.TABBED_PANE_DESELECTED_TAB_TOP_COLOR;
      bottomColor = AnEnvironment.TABBED_PANE_DESELECTED_TAB_BOTTOM_COLOR;
    }

    Graphics2D g2d = (Graphics2D) g;
    GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getTabHeight(), bottomColor);
    g2d.setPaint(gp);
    g2d.fillRect(x, 0, w, h);

    //        g.setColor(AnEnvironment.TABBED_PANE_LINE_COLOR);
    //        g.setColor(Color.red);
    //        g.drawLine(x, h - 1, x + w - 1, h - 1);
    //        g.drawLine(x, h - 0, x + w - 1, h - 0);
  }

  @Override
  protected void paintTabBorder(
      Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
    g.setColor(AnEnvironment.TABBED_PANE_DIVIDER_COLOR);
    g.drawLine(x + w - 1, 0, x + w - 1, h - 1);
  }

  @Override
  protected void paintContentBorderTopEdge(
      Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

  @Override
  protected void paintContentBorderRightEdge(
      Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

  @Override
  protected void paintContentBorderLeftEdge(
      Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

  //    @Override
  @Override
  protected void paintContentBorderBottomEdge(
      Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

  @Override
  protected void paintText(
      Graphics g,
      int tabPlacement,
      Font font,
      FontMetrics metrics,
      int tabIndex,
      String title,
      Rectangle textRect,
      boolean isSelected) {
    if (!isSelected) {
      g.setFont(font);
      g.setColor(AnEnvironment.TABBED_PANE_DESELECTED_FORGROUND_COLOR);
      g.drawString(title, textRect.x, textRect.height - 2);
    } else {
      super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
    }
  }

  @Override
  protected void paintFocusIndicator(
      Graphics g,
      int tabPlacement,
      Rectangle[] rects,
      int tabIndex,
      Rectangle iconRect,
      Rectangle textRect,
      boolean isSelected) {
    //        super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect,
    // isSelected);
    Rectangle tabRect = rects[tabIndex];
    if (tabPane.hasFocus() && isSelected) {
      int x, y, w, h;
      g.setColor(AnEnvironment.TABBED_PANE_TAB_FOCUS_COLOR);
      x = tabRect.x + 3;
      y = tabRect.y + 1;
      w = tabRect.width - 6;
      h = tabRect.height - 3;
      g.drawRect(x, y, w, h);
    }
  }

  @Override
  protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
    return 0;
  }

  private int getTabHeight() {
    if (tabHeight < 0) {
      tabHeight = tabPane.getFontMetrics(tabPane.getFont()).getHeight() + 2;
    }
    return tabHeight;
  }

  private int lastRoolOverTab = -1;

  private class TabbedPaneMouseListener implements MouseListener, MouseMotionListener {
    @Override
    public void mouseExited(MouseEvent e) {
      repaint();
      lastRoolOverTab = -1;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if (lastRoolOverTab != getRolloverTab()) {
        repaint();
        lastRoolOverTab = getRolloverTab();
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      if (lastRoolOverTab != getRolloverTab()) {
        repaint();
        lastRoolOverTab = getRolloverTab();
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    private void repaint() {
      tabPane.repaint(new Rectangle(0, 0, tabPane.getWidth(), getTabHeight() - 1));
    }
  }
}
