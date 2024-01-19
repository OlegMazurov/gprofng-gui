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

package org.gprofng.mpmt.control_panel;

import java.awt.event.ActionEvent;

public interface CPToolbarHandler {
  public void selectLeft();

  public void selectRight();

  public void selectDown();

  public void selectUp();

  public void selectFind(); // TBR?

  public void selectFirst();

  public void toggleMouseMode();

  public int getZoomLevelMax();

  public void genericZoomToLevel(int n);

  public void genericZoomIn();

  public void genericZoomOut();

  public void genericVZoomToLevel(int n);

  public void genericVZoomIn();

  public void genericVZoomOut();

  public void resetZoom();

  public void undoZoom();

  public void redoZoom();

  public void zoomToCaliper();

  public void chooseColors();

  public void showContextMenu(ActionEvent ev);
}
