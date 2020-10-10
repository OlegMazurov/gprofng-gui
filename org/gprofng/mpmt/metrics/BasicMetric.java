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

public class BasicMetric {
  private final String name; // BaseMetric.cc:cmd (aux for hwcs) example: "datapagefault", not I18N
  private final String
      shortDescription; // BaseMetricTreeNode.cc:<paragraph description>  May be null.
  private final String displayName; // BaseMetric.cc:user_name "Data Page Fault"

  public BasicMetric(String name, String description, String displayName) {
    this.name = name;
    this.shortDescription = description;
    this.displayName = displayName;
  }

  public String getName() {
    return name;
  }

  public String getShortDescription() { // may return null
    return shortDescription;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public BasicMetric clone() {
    BasicMetric clone = new BasicMetric(getName(), getShortDescription(), getDisplayName());
    return clone;
  }
}
