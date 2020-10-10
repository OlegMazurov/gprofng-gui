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

import org.gprofng.mpmt.AnMetric;

public class MetricOption {
  private final MetricAttr capable; // static capability
  private MetricAttr selected; // frontend sets these
  private final MetricAttr defaultAttrs;

  public MetricOption(MetricAttr capable, MetricAttr defaultValues) {
    this.capable = capable;
    this.selected = defaultValues.clone();
    this.defaultAttrs = defaultValues;
    validateSelected();
  }

  private void validateSelected() {
    int selectedBits = selected.getVisibleAttrs();
    int allowedBits = selectedBits & capable.getVisibleAttrs();
    if (selectedBits != allowedBits) {
      // illegal selected bits!
      // for time values, meaning of .er.rc dmetrics command's . and + symbols is swapped.
      if (capable.isTime() && selected.isValue()) {
        selected.setAttrs(allowedBits | AnMetric.VAL_TIMEVAL); // YXXX lame
      } else {
        selected.setAttrs(allowedBits); // unexpected, should bail out here?
      }
    }
  }

  /**
   * @return the capable
   */
  public MetricAttr getCapable() {
    return capable;
  }

  /**
   * @return the selected
   */
  public MetricAttr getSelected() {
    return selected;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(MetricAttr selected) {
    this.selected = selected;
  }

  /**
   * @return the selected
   */
  public boolean hasSelections() {
    if (selected == null) {
      return false;
    }
    return selected.hasVisibleAttrs();
  }

  /**
   * @return the userSavedValues
   */
  public MetricAttr getDefaultAttrs() {
    return defaultAttrs;
  }

  public MetricOption clone() {
    MetricAttr capableClone = getCapable().clone();
    //        MetricAttr selectedClone = getSelected().clone();
    MetricAttr defaultAttrsClone = defaultAttrs.clone();
    MetricOption clone = new MetricOption(capableClone, defaultAttrsClone);
    return clone;
  }
}
