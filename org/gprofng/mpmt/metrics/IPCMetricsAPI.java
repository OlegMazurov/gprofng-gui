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

package org.gprofng.mpmt.metrics;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.AnMetric;
import org.gprofng.mpmt.ipc.IPCHandle;
import org.gprofng.mpmt.ipc.IPCResult;
import java.util.ArrayList;
import java.util.List;

public class IPCMetricsAPI {

  public static boolean isJavaEnabled = false;

  public static boolean getJavaEnableIPC() {
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getJavaEnable"); // New ipc request!
    IPCResult ipcResult = ipcHandle.sendRequest();
    if (ipcResult.getCC() == IPCResult.CC.SUCCESS) { // blocking
      // Things are fine
      isJavaEnabled = ipcResult.getBoolean();
      return isJavaEnabled;
    }
    return false;
  }

  public static MetricNode getAllAvailableMetricsIPC(int dbevindex) {
    // Getting list of metrics from backend
    boolean include_unregistered_metrics = false;
    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getRefMetricTree"); // New ipc request!
    ipcHandle.append(dbevindex);
    ipcHandle.append(include_unregistered_metrics);
    IPCResult ipcResult = ipcHandle.sendRequest();
    if (ipcResult.getCC() == IPCResult.CC.SUCCESS) { // blocking
      // Things are fine
      Object[] data = (Object[]) ipcResult.getObject();
      MetricNode nodeTree = parseRefMetricTreeIpcData(null, data, 0);
      return nodeTree;
    } else {
      int i = 0; // for breakpoint
      // error handling
    }
    return null;
  }

  private static MetricNode parseRefMetricTreeIpcData(String parentName, Object[] data, int level) {
    Object fields[] = (Object[]) data[0];
    Object children[] = (Object[]) data[1];

    String name = ((String[]) fields[0])[0];
    String username = ((String[]) fields[1])[0];
    String description = ((String[]) fields[2])[0];
    int flavors = ((int[]) fields[3])[0]; // bitmask e.g. EXCLUSIVE
    int vtype = ((int[]) fields[4])[0]; // e.g.AnVariable:VT_INT
    int vstyles_capable = ((int[]) fields[5])[0]; // bitmask e.g.VAL_TIMEVAL

    int vstyles_e_default_values = ((int[]) fields[6])[0]; // excl defaults: VAL_TIME, etc.
    int vstyles_i_default_values = ((int[]) fields[7])[0]; // incl defaults: VAL_TIME, etc.

    boolean registered = ((boolean[]) fields[8])[0];
    boolean aggregation = ((boolean[]) fields[9])[0];
    boolean has_value = ((boolean[]) fields[10])[0];
    String unit = ((String[]) fields[11])[0];
    String unit_uname = ((String[]) fields[12])[0];

    if (name != null && name.equals("name")) { // YXXX where should we put this special case?
      return null;
    }

    boolean is_total_time = false; // YXXX Yuck.  where should we put Thomas' special case?
    if (name != null) {
      if (name.equals("total")) {
        is_total_time = true;
      }
      if (name.equals("totalcpu")
          && parentName != null
          && !parentName.equals(
              "total")) { // See 18378636 - Overview is insufficient to pick metrics for er_kernel
                          // experiments
        is_total_time = true;
      }
    }

    boolean isHiddenInOverview = ((flavors & AnMetric.STATIC) != 0);
    if (name != null && name.equals("PROFDATA_TYPE_STATIC")) {
      isHiddenInOverview = true;
    }
    if (!isJavaEnabled && name != null && name.equals("PROFDATA_TYPE_GCDURATION")) {
      isHiddenInOverview = true;
    }

    boolean selectable = (vstyles_capable != 0) ? true : false;
    MetricNode.MetricType mtype;
    if (name == null) { // unnamed node
      mtype = MetricNode.MetricType.PLAIN_CATEGORY;
    } else if (aggregation) {
      mtype = MetricNode.MetricType.AGGREGATE_TREE_CATEGORY;
    } else if (children.length > 0) {
      mtype = MetricNode.MetricType.PLAIN_LIST_CATEGORY;
    } else {
      //            System.out.println("=================== " + vtype + " " + name);
      mtype = MetricNode.MetricType.PLAIN;
    }

    BasicMetric basicMetric = new BasicMetric(name, description, username);

    MetricNode node;
    if (selectable) {
      MetricOption exclusive =
          new MetricOption(
              new MetricAttr(vstyles_capable), new MetricAttr(vstyles_e_default_values));
      MetricOption inclusive =
          new MetricOption(
              new MetricAttr(vstyles_capable), new MetricAttr(vstyles_i_default_values));
      node = new SelectableMetricNode(basicMetric, mtype, exclusive, inclusive, vtype);
    } else if (has_value) {
      node = new ValueMetricNode(basicMetric, mtype, unit, unit_uname);
    } else {
      node = new MetricNode(basicMetric, mtype);
    }
    node.setHiddenInOverview(isHiddenInOverview);

    // YXXX yuck: if is_total, insert a dummy "total thread time" node
    MetricNode parentNode = node;
    if (is_total_time && aggregation) {
      BasicMetric totalBMetric =
          new BasicMetric("YXXX_TOTAL_TIME_PLUS_THREADS", description, username);
      parentNode =
          new ValueMetricNode(totalBMetric, MetricNode.MetricType.AGGREGATE_TREE_ROOT_CATEGORY);
      parentNode.addChild(node);
    }

    // debug printing
    if (false) {
      for (int jj = 0; jj < level; jj++) {
        System.out.printf(" ");
      }
      System.out.printf(
          "%c L%d, %s, %s, %s\n", registered ? '*' : ' ', level, username, name, mtype);
      System.out.printf(
          "\taggregation=%d\thas_val=%d\tflavors=%d\t"
              + "vtype=%d\tviz_capable=%d\te_viz=%d\ti_viz=%d\n",
          aggregation ? 1 : 0,
          has_value ? 1 : 0,
          flavors,
          vtype,
          vstyles_capable,
          vstyles_e_default_values,
          vstyles_i_default_values);
    }

    for (int ii = 0; ii < children.length; ii++) {
      Object[] child_data = (Object[]) children[ii];
      MetricNode child = parseRefMetricTreeIpcData(name, child_data, level + 1);
      if (child != null) { // child may decide it is invisible
        node.addChild(child);
      }
    }
    return parentNode;
  }

  public static boolean updateMetricValues(int dbevindex, List<ValueMetricNode> valueMetricList) {
    //        System.out.println("updateMetricValues:");

    // split into lists of nodes w/ metrics and nodes that just have values
    List<SelectableMetricNode> selectableMetrics = new ArrayList<SelectableMetricNode>();
    List<ValueMetricNode> valueMetricsOnly = new ArrayList<ValueMetricNode>();
    for (ValueMetricNode valueMetricNode : valueMetricList) {
      if (valueMetricNode instanceof SelectableMetricNode) {
        selectableMetrics.add((SelectableMetricNode) valueMetricNode);
      } else {
        valueMetricsOnly.add(valueMetricNode);
      }
    }

    boolean rc;
    rc = ipcGetRefMetricTreeValues(dbevindex, selectableMetrics, valueMetricsOnly);
    return rc;
  }

  private static boolean ipcGetRefMetricTreeValues(
      int dbevindex, List<SelectableMetricNode> metlist, List<ValueMetricNode> nonmetlist) {
    if (metlist.isEmpty() && nonmetlist.isEmpty()) {
      return false;
    }

    String mcmds[] = new String[metlist.size()];
    int ii = 0;
    for (SelectableMetricNode item : metlist) {
      String mcmd = item.getMetricBasic().getName();
      mcmds[ii++] = mcmd;
    }

    String nmcmds[] = new String[nonmetlist.size()];
    ii = 0;
    for (ValueMetricNode item : nonmetlist) {
      String mcmd = item.getMetricBasic().getName();
      nmcmds[ii++] = mcmd;
    }

    IPCHandle ipcHandle = new IPCHandle(IPCHandle.RequestKind.DBE);
    ipcHandle.append("getRefMetricTreeValues"); // New ipc request!
    ipcHandle.append(dbevindex);
    ipcHandle.append(mcmds);
    ipcHandle.append(nmcmds);
    IPCResult ipcResult = ipcHandle.sendRequest();
    if (ipcResult.getCC() == IPCResult.CC.SUCCESS) { // blocking
      // Things are fine
      Object[] data = (Object[]) ipcResult.getObject();
      return parseRefMetricValuesIpcData(data, metlist, nonmetlist);
    } else {
      int i = 0; // for breakpoint
      // error handling
    }
    return true; // YXXX not sure the meaning of this
  }

  private static boolean parseRefMetricValuesIpcData(
      final Object[] rawData,
      List<SelectableMetricNode> metlist,
      List<ValueMetricNode> nonmetlist) {
    int nc = metlist.size() + nonmetlist.size(); // num columns (one for each value)

    Object[] valueColumns = (Object[]) rawData[0];
    Object[] highlightColumns = (Object[]) rawData[1];
    if (nc != valueColumns.length || nc != highlightColumns.length) {
      return true; // YXXX weird, broken
    }

    int metIndex = 0;
    int nonMetIndex = 0;
    for (int i = 0; i < nc; i++) {
      ValueMetricNode node;
      ArrayList<MetricValue> mvlist = new ArrayList();
      String label;
      if (i < metlist.size()) {
        node = metlist.get(metIndex++);
        label = node.getMetricBasic().getDisplayName();
      } else {
        node = nonmetlist.get(nonMetIndex++);
        label = node.getMetricBasic().getDisplayName();
      }
      String unit = null;
      if (node instanceof SelectableMetricNode) {
        MetricOption excl = ((SelectableMetricNode) node).getExclusive();
        if (excl.getCapable().isTime()) {
          unit = AnLocale.getString("Seconds");
        }
      } else if (node instanceof ValueMetricNode) {
        String vmunit = ((ValueMetricNode) node).getUnit();
        if (vmunit != null && vmunit.compareTo("SECONDS") == 0) {
          unit = AnLocale.getString("Seconds");
        }
      }
      if (valueColumns[i] instanceof double[]) {
        double[] dd = (double[]) valueColumns[i];
        for (int jj = 0; jj < dd.length; jj++) {
          mvlist.add(new MetricValue(label, dd[jj], MetricValue.ValueType.DOUBLE, unit));
        }
      } else if (valueColumns[i] instanceof int[]) {
        int[] ii = (int[]) valueColumns[i];
        for (int jj = 0; jj < ii.length; jj++) {
          mvlist.add(new MetricValue(label, ii[jj], MetricValue.ValueType.LONG, unit));
        }
      } else if (valueColumns[i] instanceof long[]) {
        long[] ii = (long[]) valueColumns[i];
        for (int jj = 0; jj < ii.length; jj++) {
          mvlist.add(new MetricValue(label, ii[jj], MetricValue.ValueType.LONG, unit));
        }
        // May need to handle AnAddress here
      } else if (valueColumns[i] instanceof String[]) {
        String[] ii = (String[]) valueColumns[i];
        for (int jj = 0; jj < ii.length; jj++) {
          mvlist.add(new MetricValue(ii[jj], MetricValue.ValueType.LABEL_ONLY));
        }
      } else {
        int jj = 0; // happens if there are no experiments loaded
      }

      for (int jj = 0; jj < mvlist.size(); jj++) {
        boolean[] highlights = (boolean[]) highlightColumns[i];
        MetricValue mv = mvlist.get(jj);
        boolean highlight = highlights[jj];
        mv.setHighlight(highlight);
      }
      node.setValues(mvlist);
    }
    return false;
  }
}
