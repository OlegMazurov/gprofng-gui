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

package org.gprofng.mpmt.settings;

public class MetricAttributes {

  private boolean eTime;
  private boolean eValue;
  private boolean ePercent;
  private boolean iTime;
  private boolean iValue;
  private boolean iPercent;

  public MetricAttributes(
      boolean eTime,
      boolean eValue,
      boolean ePercent,
      boolean iTime,
      boolean iValue,
      boolean iPercent) {
    this.eTime = eTime;
    this.eValue = eValue;
    this.ePercent = ePercent;
    this.iTime = iTime;
    this.iValue = iValue;
    this.iPercent = iPercent;
  }

  public boolean isETime() {
    return eTime;
  }

  public void setETime(boolean eTime) {
    this.eTime = eTime;
  }

  public boolean isEValue() {
    return eValue;
  }

  /**
   * @param eValue the eValue to set
   */
  public void setEValue(boolean eValue) {
    this.eValue = eValue;
  }

  public boolean isEPercent() {
    return ePercent;
  }

  public void setEPercent(boolean ePercent) {
    this.ePercent = ePercent;
  }

  public boolean isITime() {
    return iTime;
  }

  public void setITime(boolean iTime) {
    this.iTime = iTime;
  }

  public boolean isIValue() {
    return iValue;
  }

  public void setIValue(boolean iValue) {
    this.iValue = iValue;
  }

  public boolean isIPercent() {
    return iPercent;
  }

  public void setIPercent(boolean iPercent) {
    this.iPercent = iPercent;
  }

  public boolean isVTime() {
    if (eTime || eValue || ePercent) {
      return eTime;
    } else {
      return iTime;
    }
  }

  public void setVTime(boolean time) {
    if (eTime || eValue || ePercent) {
      eTime = time;
    } else {
      iTime = time;
    }
  }

  public boolean isVValue() {
    if (eTime || eValue || ePercent) {
      return eValue;
    } else {
      return iValue;
    }
  }

  public void setVValue(boolean value) {
    if (eTime || eValue || ePercent) {
      eValue = value;
    } else {
      iValue = value;
    }
  }

  public boolean isVPercent() {
    if (eTime || eValue || ePercent) {
      return ePercent;
    } else {
      return iPercent;
    }
  }

  public void setVPercent(boolean percent) {
    if (eTime || eValue || ePercent) {
      ePercent = percent;
    } else {
      iPercent = percent;
    }
  }

  /**
   * @return deep copy
   */
  public MetricAttributes copy() {
    return new MetricAttributes(eTime, eValue, ePercent, iTime, iValue, iPercent);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MetricAttributes) {
      MetricAttributes ma = (MetricAttributes) obj;
      return ma.isETime() == eTime
          && ma.isEValue() == eValue
          && ma.isEPercent() == ePercent
          && ma.isITime() == iTime
          && ma.isIValue() == iValue
          && ma.isIPercent() == iPercent;
    } else {
      return super.equals(obj); // To change body of generated methods, choose Tools | Templates.
    }
  }

  public boolean anyExclusiveSelections() {
    return eTime || eValue || ePercent;
  }

  public boolean anyInclusiveSelections() {
    return iTime || iValue || iPercent;
  }

  public boolean anySelections() {
    return anyExclusiveSelections() || anyInclusiveSelections();
  }
}
