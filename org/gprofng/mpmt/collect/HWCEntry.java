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

package org.gprofng.mpmt.collect;

import org.gprofng.mpmt.AnLocale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HWCEntry {

  private int no; // Just an entry counter
  private final boolean set; // Is a set ('default', ...)
  private final String i18n; // some printable string, does not include short_desc text
  private final String name; // alias name, or raw hwc name
  private final String intName; // hwc specification
  private final String metric; // very short description for std ctrs (aliases)
  private final long val; // default overflow rate
  private final int timecvt; // 0:not cycles.  !=0:cycles that can be converted to time
  private final int memop; // hwcentry.h enum describing capabilities (like memspace)
  private final String shortDesc; // one-liner describing counter (or null)
  private final int[] regList; // which registers support this counter
  private final boolean supportsAttrs; // supports attributes
  private final boolean supportsMemspace; // supports memoryspace profiling
  private final boolean recommended; // Recommended (std) counter
  private String collectorString;

  HWCEntry(
      boolean set,
      String i18n,
      String name,
      String intName,
      String metric,
      long val,
      int timecvt,
      int memop,
      String shortDesc,
      int[] regList,
      boolean supportsAttrs,
      boolean supportsMemspace,
      boolean recommended) {
    this.set = set;
    this.i18n = i18n;
    this.name = name;
    this.intName = intName;
    this.metric = metric;
    this.val = val;
    this.timecvt = timecvt;
    this.memop = memop;
    this.shortDesc = shortDesc;
    this.regList = regList;
    this.supportsAttrs = supportsAttrs;
    this.supportsMemspace = supportsMemspace;
    this.recommended = recommended;
    collectorString = null;
  }

  public HWCEntry copy() {
    int[] regListDeepCopy = null;
    if (regList != null) {
      regListDeepCopy = new int[regList.length];
      for (int i = 0; i < regList.length; i++) {
        regListDeepCopy[i] = regList[i];
      }
    }
    HWCEntry copy =
        new HWCEntry(
            set,
            i18n,
            name,
            intName,
            metric,
            val,
            timecvt,
            memop,
            shortDesc,
            regListDeepCopy,
            supportsAttrs,
            supportsMemspace,
            recommended);
    copy.setNo(no);
    copy.setCollectorString(collectorString);

    return copy;
  }

  public String getI18n() {
    return i18n;
  }

  public String getName() {
    return name;
  }

  public String getIntName() {
    return intName;
  }

  public String getMetric() {
    return metric;
  }

  public long getVal() {
    return val;
  }

  public int getTimecvt() {
    return timecvt;
  }

  public int getMemop() {
    return memop;
  }

  public String getShortDesc() {
    return shortDesc;
  }

  public int[] getRegList() {
    return regList;
  }

  public boolean supportsAttrs() {
    return supportsAttrs;
  }

  public boolean supportsMemspace(Map<String, HWCEntry> hwcNameMap) {
    if (!isSet() || hwcNameMap == null) {
      return supportsMemspace;
    } else {
      boolean supportsMemspace = false;
      List<String> setNames = getSetNames();
      for (String name : setNames) {
        HWCEntry entry = hwcNameMap.get(name);
        if (entry != null) {
          if (entry.supportsMemspace(hwcNameMap)) {
            supportsMemspace = true;
            break;
          }
        }
      }
      return supportsMemspace;
    }
  }

  public boolean isRecommended() {
    return recommended;
  }

  public boolean supportsTime(Map<String, HWCEntry> hwcNameMap) {
    if (!isSet() || hwcNameMap == null) {
      return timecvt != 0;
    } else {
      boolean supportsTime = false;
      List<String> setNames = getSetNames();
      for (String name : setNames) {
        HWCEntry entry = hwcNameMap.get(name);
        if (entry != null) {
          if (entry.supportsTime(hwcNameMap)) {
            supportsTime = true;
            break;
          }
        }
      }
      return supportsTime;
    }
  }

  public boolean isRaw() {
    return !isRecommended();
  }

  public int getNo() {
    return no;
  }

  public void setNo(int no) {
    this.no = no;
  }

  public boolean isSet() {
    return set;
  }

  public List<String> getSetNames() {
    List<String> list = new ArrayList<String>();

    if (isSet()) {
      // Parse
      String[] names = getIntName().split(",,");
      int index = 0;
      for (String name : names) {
        if (name.startsWith("+")) {
          names[index] = name.substring(1);
        }
        index++;
      }
      list = Arrays.asList(names);
    }

    return list;
  }

  @Override
  public String toString() {
    return getI18n() + "  [" + getCollectorString() + "]";
  }

  /**
   * @return the string (including attributes, ...) used by collector
   */
  public String getCollectorString() {
    if (collectorString != null) {
      return collectorString;
    } else {
      return name;
    }
  }

  public void setCollectorString(String collectorString) {
    this.collectorString = collectorString;
  }

  public String getMetricText() {
    if (isSet()) {
      return AnLocale.getString("Default Set");
    }
    if (isRecommended()) {
      return getI18n();
    }
    if (isRaw()) {
      return getI18n();
    }
    return "???";
  }

  public String getCounterText() {
    if (isSet()) {
      return getIntName();
    }
    if (isRecommended()) {
      String format = "%s  [%s]";
      return String.format(format, getName(), getIntName());
    }
    if (isRaw()) {
      return getName();
    }
    return "???";
  }

  public String getDescriptionText(Map<String, HWCEntry> hwcNameMap) {
    if (isSet()) {
      return AnLocale.getString("Default set of counters");
    }
    if (isRecommended()) {
      if (getShortDesc() != null) {
        return getShortDesc();
      } else {
        HWCEntry mappedEntry = hwcNameMap.get(getIntName());
        if (mappedEntry != null && mappedEntry.getShortDesc() != null) {
          return mappedEntry.getShortDesc();
        } else {
          return getI18n();
        }
      }
    }
    if (isRaw()) {
      return getShortDesc() != null ? getShortDesc() : "";
    }
    return "???";
  }
}
