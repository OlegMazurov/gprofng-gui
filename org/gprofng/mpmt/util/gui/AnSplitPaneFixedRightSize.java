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

import static org.gprofng.mpmt.util.gui.AnSplitPane.defaultDividerSize;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class AnSplitPaneFixedRightSize extends AnSplitPane {

  private int sizeInPixels;
  private final int defaultSize;
  private boolean hidden = false;
  private int sizeInPixelsHidden = -1;

  public AnSplitPaneFixedRightSize(
      int newOrientation,
      Component newLeftComponent,
      Component newRightComponent,
      int rightSizeInPixels,
      int rightDefaultSize) {
    super(newOrientation, newLeftComponent, newRightComponent, 0);
    setOneTouchExpandable(true);
    this.sizeInPixels = rightSizeInPixels;
    this.defaultSize = rightDefaultSize;
    setResizeWeight(1);

    addComponentListener(
        new ComponentListener() {
          @Override
          public void componentShown(ComponentEvent e) {}

          @Override
          public void componentResized(ComponentEvent e) {
            if (!isHidden()) {
              setDivider();
            }
          }

          @Override
          public void componentMoved(ComponentEvent e) {}

          @Override
          public void componentHidden(ComponentEvent e) {}
        });
  }

  private void setDivider() {
    int location;
    if (getOrientation() == HORIZONTAL_SPLIT) {
      location = getWidth() - sizeInPixels;
    } else {
      location = getHeight() - sizeInPixels;
    }
    if (location != super.getDividerLocation()) {
      super.setDividerLocation(location);
    }
  }

  @Override
  public void setDividerLocation(int location) {
    // Only react to this if it is from a direct user action like dragging or clicking
    if (isDraggingDivider() || clickedOneTouchButton()) {
      setDividerLocationInternal(location);
    }
  }

  public void setDividerLocationInternal(int location) {
    if (getOrientation() == HORIZONTAL_SPLIT) {
      sizeInPixels = getWidth() - location;
    } else {
      sizeInPixels = getHeight() - location;
    }
    super.setDividerLocation(location);
  }

  @Override
  public int getLastDividerLocation() {
    int ldl = super.getLastDividerLocation();
    int size;
    if (getOrientation() == HORIZONTAL_SPLIT) {
      size = getWidth();
    } else {
      size = getHeight();
    }
    if (ldl <= 8 || ldl > (size - 8)) {
      int location = size - defaultSize;
      ldl = location;
    }
    return ldl;
  }

  /*
   * Hack......
   */
  private boolean clickedOneTouchButton() {
    StackTraceElement[] es = Thread.currentThread().getStackTrace();
    int n = 0;
    for (StackTraceElement e : es) {
      String s = e.getClassName();
      if (s.contains("OneTouchAction")) { // NOI18N
        return true;
      }
      if (n++ > 3) {
        break;
      }
    }
    return false;
  }

  /**
   * @return the widthInPixels
   */
  public int getSizeInPixels() {
    if (hidden) {
      return sizeInPixelsHidden;
    } else {
      return sizeInPixels;
    }
  }

  /**
   * @param sizeInPixels
   */
  public void setSizeInPixels(int sizeInPixels) {
    this.sizeInPixels = sizeInPixels;
    setDivider();
  }

  /**
   * @param hide
   */
  @Override
  public void setHidden(boolean hide) {
    if (hide) {
      if (!hidden) {
        hidden = true;
        sizeInPixelsHidden = sizeInPixels;
        sizeInPixels = 0;
        setDividerSize(0);
        super.setDividerLocation(10000);
      }
    } else {
      if (hidden) {
        hidden = false;
        sizeInPixels = sizeInPixelsHidden;
        sizeInPixelsHidden = -1;
        setDividerSize(defaultDividerSize);
        setDivider();
      }
    }
  }

  /**
   * @return
   */
  @Override
  public boolean isHidden() {
    return hidden;
  }
}
