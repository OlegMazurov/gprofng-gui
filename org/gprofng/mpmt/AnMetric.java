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

package org.gprofng.mpmt;

import org.gprofng.mpmt.settings.CompareModeSetting.CompareMode;
import java.awt.Color;
import java.util.Hashtable;
import java.util.List;

// See BaseMetric.h and Metric.h for definitions of members
public final class AnMetric {

  // BaseMetric.h:

  public String expr_spec, aux, legend;
  private final String comd, user_name, data_type_name, data_type_uname;
  //    private final int id
  private final int valtype, clock, type, flavors;
  private final int value_styles;
  private int sub_type, vis_bits;
  // Metric.h:
  private final String name, abbr, unit /*aka abbr_unit*/;
  // MetricList.h::get_sort_ref_index()
  private final String short_desc; // one-liner description of metric. May be NULL

  private boolean sorted;

  public static final Color defaultBackground = new Color(0xef, 0xef, 0xef);
  private static final Color[] compareColors = {
    new Color(0xce, 0xce, 0xce),
    defaultBackground,
    Color.WHITE,
    new Color(0xd0, 0xfc, 0xff),
    new Color(0xff, 0xfa, 0xd0),
    new Color(0xea, 0xd0, 0xff),
  };
  private final Color backgroundColor;
  private boolean hot = false;

  // Constructor
  public AnMetric(AnMetric bm) {
    type = bm.type;
    sub_type = bm.sub_type;
    clock = bm.clock;
    flavors = bm.flavors;
    value_styles = bm.value_styles;
    user_name = bm.user_name;
    legend = bm.legend;
    expr_spec = bm.expr_spec;
    aux = bm.aux;
    name = bm.name;
    abbr = bm.abbr;
    comd = bm.comd;
    unit = bm.unit;
    vis_bits = bm.vis_bits;
    valtype = bm.valtype;
    data_type_name = bm.data_type_name;
    data_type_uname = bm.data_type_uname;
    sorted = false;
    backgroundColor = getMetricBackgroundInternal(expr_spec);
    short_desc = bm.short_desc;
  }

  public AnMetric(
      int type,
      int sub_type,
      int clock,
      int flavors,
      int value_styles,
      String user_name,
      String expr_spec,
      String aux,
      String name,
      String abbr,
      String comd,
      String unit,
      String legend,
      int vis_bits,
      boolean sorted,
      int valtype,
      String data_type_name,
      String data_type_uname,
      String short_desc) {
    this.type = type;
    this.sub_type = sub_type;
    this.clock = clock;
    this.flavors = flavors;
    this.value_styles = value_styles;
    this.user_name = user_name;
    this.legend = legend;
    this.expr_spec = expr_spec;
    this.aux = aux;
    this.name = name;
    this.abbr = abbr;
    this.comd = comd;
    this.unit = unit;
    this.vis_bits = vis_bits;
    this.sorted = sorted;
    this.valtype = valtype;
    this.data_type_name = data_type_name;
    this.data_type_uname = data_type_uname;
    this.short_desc = short_desc;
    backgroundColor = getMetricBackgroundInternal(expr_spec);
  }

  //    public int      get_id()        { return id; }
  public int getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getUserName() {
    return user_name == null ? name : user_name;
  }

  public String getDataTypeName() {
    return data_type_name;
  }

  public String getDataTypeUserName() {
    return data_type_uname;
  }

  public String getShortDesc() {
    return short_desc;
  }

  public String getAbbr() {
    return abbr;
  }

  public String getComd() {
    return comd;
  }

  public String getAux() {
    return aux;
  }

  public String getUnit() {
    return unit;
  }

  public int getSubType() {
    return sub_type;
  }

  public int get_valtype() {
    return valtype;
  }

  public int getClock() {
    return clock;
  }

  public int get_vis_bits() {
    return vis_bits;
  }

  public boolean isSorted() {
    return sorted;
  }

  public boolean isTimeMetric() {
    return (value_styles & VAL_TIMEVAL) != 0;
  }

  public boolean isValMetric() {
    return (value_styles & VAL_VALUE) != 0;
  }

  public boolean isNameMetric() {
    return (type == METRIC_TYPE_ONAME);
  }

  public boolean isStatic() {
    return (flavors & STATIC) != 0;
  }

  public boolean canExclusive() {
    return (flavors & EXCLUSIVE) != 0;
  }

  public boolean canInclusive() {
    return (flavors & INCLUSIVE) != 0;
  }

  public boolean canPercent() {
    return (value_styles & VAL_PERCENT) != 0;
  }

  @Override
  public String toString() {
    return getUserName();
  }

  //    public void     set_id(int _id)         { id = _id; }
  public void setSubType(int t) {
    sub_type = t;
  }

  public void setSorted(boolean set) {
    sorted = set;
  }

  public boolean isHidden() {
    return (vis_bits & VAL_HIDE_ALL) != 0;
  }

  public void set_hidden(boolean hide_all) {
    vis_bits &= ~(VAL_HIDE_ALL);
    if (hide_all) {
      vis_bits |= VAL_HIDE_ALL;
    }
  }

  public boolean isPVisible() {
    if (isHidden()) {
      return false;
    }
    return (vis_bits & VAL_PERCENT) != 0;
  }

  public boolean isVVisible() {
    if (isHidden()) {
      return false;
    }
    if (is_time_val()) {
      return (vis_bits & VAL_VALUE) != 0;
    } else if (isValMetric()) {
      return (vis_bits & (VAL_VALUE | VAL_TIMEVAL)) != 0;
    }
    return false;
  }

  public boolean isTVisible() {
    if (isHidden()) {
      return false;
    }
    if (is_time_val()) {
      return (vis_bits & VAL_TIMEVAL) != 0;
    } else if (isTimeMetric()) {
      return (vis_bits & (VAL_VALUE | VAL_TIMEVAL)) != 0;
    }
    return false;
  }

  public void set_vis_bits(boolean tvis, boolean vvis, boolean pvis) {
    int v = 0;
    if (is_time_val()) {
      if (tvis) {
        v |= VAL_TIMEVAL;
      }
      if (vvis) {
        v |= VAL_VALUE;
      }
    } else if ((vvis || tvis)) {
      v |= VAL_VALUE;
    }
    if (pvis && canPercent()) {
      v |= VAL_PERCENT;
    }
    vis_bits &= ~(VAL_VALUE | VAL_TIMEVAL | VAL_PERCENT);
    vis_bits |= v;
  }

  public boolean is_time_val() {
    final int v = VAL_TIMEVAL | VAL_VALUE;
    return (value_styles & v) == v;
  }

  private static Hashtable simulatedGroupIds = new Hashtable();

  private Color getMetricBackgroundInternal(
      String expr_spec) { // YXXX should use group id value from dbe
    if (expr_spec == null) {
      return defaultBackground;
    }
    Integer simulatedGroupId;
    if (!simulatedGroupIds.containsKey(expr_spec)) {
      simulatedGroupId = simulatedGroupIds.size() + 1;
      simulatedGroupIds.put(expr_spec, simulatedGroupId);
    } else {
      simulatedGroupId = (Integer) simulatedGroupIds.get(expr_spec);
    }
    return getMetricBackground(simulatedGroupId);
  }

  public Color getMetricBackground() {
    return backgroundColor;
  }

  public static Color getMetricBackground(int groupId) {
    if (groupId < 1) {
      return defaultBackground;
    }
    int ii = (groupId - 1) % compareColors.length;
    return compareColors[ii];
  }

  private int compareTo(String a, String b) {
    if (a == null) {
      return b == null ? 0 : -1;
    } else {
      return b == null ? 1 : a.compareTo(b);
    }
  }

  public boolean equals(AnMetric m) {
    if (m == null) {
      return false;
    }
    return (getType() == m.getType())
        && (getSubType() == m.getSubType())
        && (compareTo(getUserName(), m.getUserName()) == 0)
        && (compareTo(expr_spec, m.expr_spec) == 0);
  }

  public int indexIn(List<AnMetric> mlist) {
    for (int i = 0; i < mlist.size(); i++) {
      if (equals(mlist.get(i))) {
        return i;
      }
    }
    return -1;
  }

  public CompareMode getCompareMode() {
    if ((expr_spec == null) || expr_spec.equalsIgnoreCase("EXPGRID==1")) {
      return CompareMode.CMP_DISABLE;
    }
    CompareMode mode = AnWindow.getInstance().getSettings().getCompareModeSetting().get();
    return mode;
  }

  // flavors bits: (should match BaseMetric.h)
  public static final int STATIC = 1;
  public static final int EXCLUSIVE = 2;
  public static final int INCLUSIVE = 4;

  // ValueTag (should match dbe_structs.h and AnVariable.java)
  //    public final static int VT_SHORT = 1;  // unused
  public static final int VT_INT = 2;
  public static final int VT_LLONG = 3;
  //    public final static int VT_FLOAT = 4;  // unused
  public static final int VT_DOUBLE = 5;
  //    public final static int VT_HRTIME = 6; // unused
  public static final int VT_LABEL = 7;
  public static final int VT_ADDRESS = 8; // unused
  //    public final static int VT_OFFSET = 9; // unused
  public static final int VT_ULLONG = 10;

  // vis_bits (should match enums.h)
  public static final int VAL_NA = 0;
  public static final int VAL_TIMEVAL = 1;
  public static final int VAL_VALUE = 2;
  public static final int VAL_PERCENT = 4;
  public static final int VAL_DELTA = 8;
  public static final int VAL_RATIO = 16;
  public static final int VAL_HIDE_ALL = 64;

  // metric types (should match BaseMetric.h) //YXXX TBR, yuck
  public static final int METRIC_TYPE_ONAME = 1;
  public static final int METRIC_TYPE_USER_CPU_TIME = 9;
  public static final int METRIC_TYPE_LOCK_WAIT_TIME = 19; // aka "LWT"

  public boolean isHot() {
    return hot;
  }

  public void setHot(boolean hot) {
    this.hot = hot;
  }
}
