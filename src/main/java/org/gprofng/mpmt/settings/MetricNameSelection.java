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

package org.gprofng.mpmt.settings;

/**
 * @author tpreisle
 */
public class MetricNameSelection {
  private String name;
  private MetricSelection metricSelection;

  public MetricNameSelection(String name, boolean selected, boolean[] exclusiveInclusive) {
    this.name = name;
    MetricAttributes metricAttributes =
        new MetricAttributes(
            exclusiveInclusive[0],
            exclusiveInclusive[1],
            exclusiveInclusive[2],
            exclusiveInclusive[3],
            exclusiveInclusive[4],
            exclusiveInclusive[5]);
    this.metricSelection = new MetricSelection(selected, metricAttributes);
  }

  public MetricNameSelection(String name, MetricSelection metricSelection) {
    this.name = name;
    this.metricSelection = metricSelection;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the selected
   */
  public boolean isSelected() {
    return getMetricSelection().isSelected();
  }

  /**
   * @return the metricSelection
   */
  public MetricSelection getMetricSelection() {
    return metricSelection;
  }
}
