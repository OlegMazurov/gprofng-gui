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

import org.gprofng.mpmt.AnMetric;

public class MetricAttr { // AKA DBE ValueStyle bitmask

  private int enabledAttrs; // bitmask, e.g. AnMetric.VAL_TIMEVAL

  public MetricAttr(boolean is_hidden, boolean time, boolean value, boolean percent) {
    setAttrs(is_hidden, time, value, percent);
  }

  public MetricAttr(int value_styles) {
    enabledAttrs = value_styles;
  }

  /**
   * @return set the enabledAttrs
   */
  public void setAttrs(int value_styles) {
    enabledAttrs = value_styles;
  }

  public void setAttrs(boolean is_hidden, boolean time, boolean value, boolean percent) {
    enabledAttrs = 0;
    if (is_hidden) {
      enabledAttrs |= AnMetric.VAL_HIDE_ALL;
    }
    if (time) {
      enabledAttrs |= AnMetric.VAL_TIMEVAL;
    }
    if (value) {
      enabledAttrs |= AnMetric.VAL_VALUE;
    }
    if (percent) {
      enabledAttrs |= AnMetric.VAL_PERCENT;
    }
  }

  public void setTime(boolean time) {
    setAttrs(isHidden(), time, isValue(), isPercent());
  }

  public void setValue(boolean value) {
    setAttrs(isHidden(), isTime(), value, isPercent());
  }

  public void setPercent(boolean percent) {
    setAttrs(isHidden(), isTime(), isValue(), percent);
  }

  /**
   * @return the hidden
   */
  public boolean isHidden() {
    boolean hidden = (enabledAttrs & AnMetric.VAL_HIDE_ALL) != 0;
    return hidden;
  }

  /**
   * @return the time
   */
  public boolean isTime() {
    boolean time = (enabledAttrs & AnMetric.VAL_TIMEVAL) != 0;
    return time;
  }

  /**
   * @return the value
   */
  public boolean isValue() {
    boolean value = (enabledAttrs & AnMetric.VAL_VALUE) != 0;
    return value;
  }

  /**
   * @return the percent
   */
  public boolean isPercent() {
    boolean percent = (enabledAttrs & AnMetric.VAL_PERCENT) != 0;
    return percent;
  }

  /**
   * @return true if any Attrs enabled
   */
  public boolean hasVisibleAttrs() {
    int visibleAttrs = getVisibleAttrs();
    boolean enabled = (visibleAttrs != 0);
    return enabled;
  }

  /**
   * @return the enabledAttrs
   */
  public int getVisibleAttrs() {
    if (isHidden()) {
      return AnMetric.VAL_NA;
    }
    int visibleAttrs = (enabledAttrs & ~AnMetric.VAL_HIDE_ALL);
    return visibleAttrs;
  }

  public MetricAttr clone() {
    MetricAttr clone = new MetricAttr(enabledAttrs);
    return clone;
  }
}
