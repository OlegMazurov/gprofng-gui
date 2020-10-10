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

package org.gprofng.mpmt.settings;

public class MetricNameSelected {
  private String name;
  private boolean selected;

  public MetricNameSelected(String name, boolean selected) {
    this.name = name;
    this.selected = selected;
  }

  public String getName() {
    return name;
  }

  public boolean isSelected() {
    return selected;
  }
}
