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

public interface CoordCalcTimeReader {

  // pixels
  public int getMargin();

  public int getCanvasPixels();

  public int getAvailPixels(); // pixels available for data

  // time
  public long getTimeStart();

  public long getTimeEnd();

  public long getTimeDuration();

  // bin-related
  public int getNumBins();

  public long getTimePerBin();

  public long getTimeStartAligned();

  public long getTimeStartAligned(long time); // return is >= 0

  public long getTimeEndAligned();

  // conversions
  public int getCoordAtTime(long time);

  public int getLowCoordForBin(int bin);

  public long getTimeAtCoord(int xx); // returns -1 if coord out of range

  public long getTimeNearCoord(int xx); // always returns a valid time

  public int getBinAtTime(long time);
}
