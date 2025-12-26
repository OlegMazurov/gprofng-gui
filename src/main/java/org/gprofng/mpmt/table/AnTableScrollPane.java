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

package org.gprofng.mpmt.table;

import org.gprofng.analyzer.AnEnvironment;
import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnWindow;
import org.gprofng.mpmt.util.gui.AnJScrollPane;
import org.gprofng.mpmt.util.gui.AnUtility;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.MouseInputAdapter;

public abstract class AnTableScrollPane extends AnJScrollPane {

  private Image addtablecolumnsImage = AnUtility.addColumnIcon.getImage();
  private static final String metricsIconToolTip =
      AnLocale.getString("Click to select colums to display...");
  private static int imageXGap = -1;
  private static final int imageYGap = 2;
  private boolean hasColumnButton;
  private int dtype;

  private JPopupMenu metricsSelectorpopup = null;

  public AnTableScrollPane(int dtype, boolean hasColumnButton) {
    this.dtype = dtype;
    this.hasColumnButton = hasColumnButton;
    init();
  }

  protected void init() {
    if (hasColumnButton) {
      addMouseListener(
          new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              super.mouseClicked(e);
              if (!e.isPopupTrigger()) {
                int startX = e.getX();
                int startY = e.getY();

                // Handle mouse click over data presentation icon
                AnWindow anWindow = AnWindow.getInstance();
                if (anWindow.getSettingsAction().isEnabled()) {
                  int width = getSize().width;
                  if (startX >= width - (addtablecolumnsImage.getWidth(null) + imageXGap)
                      && startX <= (width - imageXGap)
                      && startY >= imageYGap
                      && startY <= (addtablecolumnsImage.getHeight(null) + imageYGap)) {
                    showMetricsConfigurationPopup(
                        AnTableScrollPane.this, startX + 5, startY + 5, dtype);
                  }
                }
              }
            }
          });
      addMouseMotionListener(
          new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
              super.mouseMoved(e);
              int startX = e.getX();
              int startY = e.getY();

              // Handle tooltip data presentation icon
              AnWindow anWindow = AnWindow.getInstance();
              if (anWindow.getSettingsAction().isEnabled()) {
                int width = getSize().width;
                if (startX >= width - (addtablecolumnsImage.getWidth(null) + imageXGap)
                    && startX <= (width - imageXGap)
                    && startY >= imageYGap
                    && startY <= (addtablecolumnsImage.getHeight(null) + imageYGap)) {
                  setToolTipText(metricsIconToolTip);
                } else {
                  setToolTipText(null);
                }
              }
            }
          });
    }
  }

  public void showMetricsConfigurationPopup(Component component, int x, int y, int dtype) {
    metricsSelectorpopup = getmetricsSelectorPopup(dtype);
    metricsSelectorpopup.show(component, x, y);
  }

  private JPopupMenu getmetricsSelectorPopup(int dtype) {
    JPopupMenu popup = new JPopupMenu();
    AnWindow.getInstance()
        .getSettings()
        .getMetricsSetting()
        .addMetricsPopupSelector(popup, dtype, true);
    return popup;
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    int vsbw = getVerticalScrollBar().getWidth(); // 17
    int vsbh = getVerticalScrollBar().getHeight(); // many+
    boolean vsbv = getVerticalScrollBar().isVisible();
    int hsbw = getHorizontalScrollBar().getWidth(); // many+
    int hsbh = getHorizontalScrollBar().getHeight(); // 17
    boolean hsbv = getHorizontalScrollBar().isVisible();
    g.setColor(AnEnvironment.SCROLLBAR_TRACK_COLOR);
    g.fillRect(getWidth() - vsbw, 0, vsbw, getHeight() - vsbh - (hsbv ? hsbh : 0));

    g.setColor(AnEnvironment.SPLIT_PANE_BORDER_COLOR);
    if (hsbv) {
      g.drawLine(getWidth() - vsbw, 0, getWidth() - vsbw, getHeight() - vsbh - hsbh - 1);
      g.drawLine(
          getWidth() - vsbw,
          getHeight() - vsbh - hsbh - 1,
          getWidth(),
          getHeight() - vsbh - hsbh - 1);
    } else {
      g.drawLine(getWidth() - vsbw, getHeight() - vsbh - 1, getWidth(), getHeight() - vsbh - 1);
    }

    if (hasColumnButton && getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_ALWAYS) {
      g.drawImage(
          addtablecolumnsImage,
          getWidth() - (addtablecolumnsImage.getHeight(null) + imageXGap),
          imageYGap,
          null);
    }
  }
}
