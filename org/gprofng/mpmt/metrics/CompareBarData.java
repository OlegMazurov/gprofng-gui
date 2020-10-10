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


package org.gprofng.mpmt.metrics;

import org.gprofng.mpmt.overview.Bar.ValueColor;
import java.util.List;

/**
 * @author tpreisle
 */
public class CompareBarData {
  private int percentOfTotal; //
  private List<ValueColor> valueColors;

  public CompareBarData(int percent, List<ValueColor> valueColors) {
    this.percentOfTotal = percent;
    this.valueColors = valueColors;
  }

  /**
   * @return the percent
   */
  public int getPercentOfTotal() {
    return percentOfTotal;
  }

  /**
   * @return the valueColors
   */
  public List<ValueColor> getValueColors() {
    return valueColors;
  }
}
