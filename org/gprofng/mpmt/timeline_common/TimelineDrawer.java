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

package org.gprofng.mpmt.timeline_common;

import java.awt.Graphics;

public interface TimelineDrawer {
  // consts
  public static final long TIME_INVALID = -1; // used by IPC

  public void setParent(TimelinePanel parent); // YXXX yuck, use interface

  // ------ get components
  public CoordCalcTimeMaster getTimeAxisMaster();

  public CoordCalcDataMaster getDataAxisMaster();

  public VerticalRowRuler getVerticalRuler();

  // ------ get options
  public boolean getEnableZoomVertical();

  public boolean getEnableZoomOverlays();

  public boolean getEnableUnalignedRows();

  public boolean getEnableTimeCaliper();

  // ------ get X & Y max dimensions
  public long getAbsoluteTimeStart();

  public long getAbsoluteTimeEnd();

  public long getAbsoluteTimeDuration();

  public int getAbsoluteRowCount();

  public int getAbsoluteRowEnd(); // last valid row, or 0 if no rows.

  // ------ zoom
  public void zoomCenterSelection();

  // ------ selection (cursor)
  public boolean selectionActive();

  public long getSelectionTimeCenter();

  public long getSelectionTimeStart();

  public long getSelectionTimeEnd();

  public double getSelectionYCenter();

  public boolean getSelectionNavigationEnabled();

  public void selectFirst();

  public void selectLeft();

  public void selectRight();

  public void selectUp();

  public void selectDown();

  public void selectLeft(boolean ctrl, boolean shift);

  public void selectRight(boolean ctrl, boolean shift);

  public void selectUp(boolean ctrl, boolean shift);

  public void selectDown(boolean ctrl, boolean shift);

  public TimelineCaliper selectNearXY(
      int x, int y, boolean ctrl, boolean shift); // return TimelineCaliper only on precise hit

  public void notifyCaliperChange(TimelineCaliper caliper);

  public void notifyZoomHistoryChange(TimelineSelectionHistoryEvent e);

  public void addSelectionListener(TimelineSelectionListener listener);

  // --- fetch of data
  public boolean edt_fetchData(boolean forceRefetch);

  public boolean drawData(Graphics g);
}
