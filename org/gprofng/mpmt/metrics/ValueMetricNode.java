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


package org.gprofng.mpmt.metrics;

import java.util.ArrayList;
import java.util.List;

public class ValueMetricNode extends MetricNode {
  private List<MetricValue> values;
  private String unit;
  private String unit_uname;

  public ValueMetricNode(
      BasicMetric metricBasic, MetricType metricsGroupType, String unit, String unit_uname) {
    super(metricBasic, metricsGroupType);
    this.values = new ArrayList<>();
    this.unit = unit;
    this.unit_uname = unit_uname;
  }

  public ValueMetricNode(BasicMetric metricBasic, MetricType metricsGroupType) {
    super(metricBasic, metricsGroupType);
    this.values = new ArrayList<>();
    this.unit = null;
    this.unit_uname = null;
  }

  public String getUnit() {
    return unit;
  }

  public String getUnitUname() {
    return unit_uname;
  }

  /**
   * @return the value
   */
  public MetricValue getValue(int no) {
    if (no < values.size()) {
      return values.get(no);
    } else {
      return null;
    }
  }

  /**
   * @return whether it has the value set
   */
  public boolean hasValues() {
    return !values.isEmpty();
  }

  /**
   * @return the value
   */
  public List<MetricValue> getValues() {
    return values;
  }

  /**
   * @param value the value to set
   */
  public void setValues(List<MetricValue> values) {
    this.values = values;
  }
}
