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

public interface CoordCalcDataReader {
  // pixels
  public int getMargin();

  public int getCanvasPixels();

  public int getAvailPixels(); // pixels available for data

  public int getTotalPixels(); // virtual # of pixels if everything drawn

  public int getMinPixelsForAllRows();

  public int getMinPixelsRequired(int row_lo, int row_hi); // YXXX unused

  // rows
  public int getRowStart();

  public int getRowEnd();

  public int getRowCount();

  public int getAbsRowEnd(); // last valid row; zero if getRowCount()=0

  public int getAbsRowCount();

  // percent visible (used with smooth scrolling)
  public double getVisibleStart();

  public double getVisibleEnd();

  public double getVisiblePercent();

  // conversions
  public int getLowCoordForRow(int yrow);

  public int getCenterCoordForRow(int yrow);

  public int getHighCoordForRow(int yrow);

  public double getCenterPercentForRow(int yrow);

  public double getPercentNearCoord(int canvasY);

  public double getRowPercentOfTotal(int yrow);

  public int getRowAtCoord(int canvasY);

  public int getRowNearCoord(int canvasY);

  public int getRowNearPercent(double percent); // always returns a valid row
}
