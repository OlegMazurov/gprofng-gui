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
package org.gprofng.mpmt.timeline_common;

import java.util.ArrayList;

public abstract class CoordCalcDataMaster implements CoordCalcDataReader {

  public abstract void setCanvasPixels(int visiblePixels);

  public abstract void edt_revalidate(); // must be called from event dispatch thread

  public abstract boolean isValid(); // must be called from event dispatch thread

  public abstract boolean isInitialized();

  public abstract void setAbsRowMinHeights(ArrayList<Integer> mins);

  public abstract void setAbsRowMinHeights(int rowCount, int minPixels);

  public abstract void setExpandToFill(boolean expands);

  public abstract void setRowRange(int startrow, int endrow);

  public abstract void setVisibleRange(double start, double end);
}
