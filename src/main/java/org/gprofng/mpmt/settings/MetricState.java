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

import org.gprofng.mpmt.AnMetric;

public class MetricState {
  private String listName;
  private int listId;
  private AnMetric anMetric;
  private MetricAttributes capable;
  private MetricSelection factory;
  private MetricSelection selection;

  public MetricState(
      String listName,
      int listId,
      AnMetric anMetric,
      MetricAttributes capable,
      MetricSelection factory,
      MetricSelection selection) {
    this.listName = listName;
    this.listId = listId;
    this.anMetric = anMetric;
    this.capable = capable;
    this.factory = factory;
    this.selection = selection;
  }

  public void dump() {
    System.out.print(String.format("%-20s", anMetric.getComd()));
    dumpMetricAttributes(capable);
    System.out.print("   ");
    dumpMetricAttributes(factory);
    System.out.print("   ");
    dumpMetricAttributes(getSelection());
    System.out.println();
  }

  private void dumpMetricAttributes(MetricSelection metricSelection) {
    System.out.print(metricSelection.isSelected() ? "X" : " ");
    dumpMetricAttributes(metricSelection.getAttributes());
  }

  private void dumpMetricAttributes(MetricAttributes metricAttributes) {
    System.out.print(metricAttributes.isETime() ? "+" : "-");
    System.out.print(metricAttributes.isEValue() ? "+" : "-");
    System.out.print(metricAttributes.isEPercent() ? "+" : "-");
    System.out.print(" ");
    System.out.print(metricAttributes.isITime() ? "+" : "-");
    System.out.print(metricAttributes.isIValue() ? "+" : "-");
    System.out.print(metricAttributes.isIPercent() ? "+" : "-");
  }

  /**
   * @return the listName
   */
  public String getListName() {
    return listName;
  }

  /**
   * @return the anMetric
   */
  public AnMetric getAnMetric() {
    return anMetric;
  }

  /**
   * @return the selection
   */
  public MetricSelection getSelection() {
    return selection;
  }

  /**
   * @return a deep copy (except for static states
   */
  public MetricState copy() {
    MetricState copy =
        new MetricState(listName, listId, anMetric, capable, factory, selection.copy());
    return copy;
  }

  /**
   * @return the factory
   */
  public MetricSelection getFactory() {
    return factory;
  }

  /**
   * @return the capable
   */
  public MetricAttributes getCapable() {
    return capable;
  }

  /**
   * @return the listId
   */
  public int getListId() {
    return listId;
  }
}
