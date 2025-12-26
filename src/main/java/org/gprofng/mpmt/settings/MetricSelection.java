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

public class MetricSelection {
  private boolean selected;
  private MetricAttributes attributes;

  public MetricSelection(boolean selected, MetricAttributes attributes) {
    this.selected = selected;
    this.attributes = attributes;
  }

  public void set(MetricSelection metricSelection) {
    selected = metricSelection.isSelected();
    attributes = metricSelection.getAttributes();
  }

  /**
   * @return deep copy
   */
  public MetricSelection copy() {
    return new MetricSelection(isSelected(), getAttributes().copy());
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public void setAttributes(MetricAttributes attributes) {
    this.attributes = attributes;
  }

  public MetricAttributes getAttributes() {
    return attributes;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MetricSelection) {
      MetricSelection metricSelection = (MetricSelection) obj;
      if (selected != metricSelection.isSelected()) {
        return false;
      } else {
        return attributes.equals(metricSelection.getAttributes());
      }
    } else {
      return super.equals(obj); // To change body of generated methods, choose Tools | Templates.
    }
  }
}
