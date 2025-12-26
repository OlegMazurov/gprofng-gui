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
import org.gprofng.mpmt.AnWindow;
import java.util.ArrayList;
import java.util.List;

public class MetricNode {
  public static final int ALL_LIST_ID = 0;
  public static final int IO_LIST_ID = 16;
  public static final int HEAP_LIST_ID = 43;
  public static final int DATASPACE_HWC_LIST_ID = 72;

  public enum MetricType {
    PLAIN_CATEGORY,
    PLAIN_LIST_CATEGORY,
    AGGREGATE_LIST_CATEGORY,
    PLAIN_TREE_CATEGORY,
    AGGREGATE_TREE_ROOT_CATEGORY,
    AGGREGATE_TREE_CATEGORY,
    PLAIN,
    PLAIN_ROOT,
    PLAIN_CHILD
  };

  private final BasicMetric metricBasic;
  private MetricType metricType;
  private final List<MetricNode> children;
  private boolean hiddenInOverview; // never show this in overview

  public MetricNode(BasicMetric metricBasic, MetricType metricType) {
    this.metricBasic = metricBasic;
    this.metricType = metricType;
    this.children = new ArrayList<>();
    this.hiddenInOverview = false;
  }

  public void setHiddenInOverview(boolean hiddenInOverview) {
    this.hiddenInOverview = hiddenInOverview;
  }

  /**
   * @return the hiddenInOverview
   */
  public boolean isHiddenInOverview() {
    return hiddenInOverview;
  }

  public void addChild(MetricNode metricNode) {
    children.add(metricNode);
  }

  public List<MetricNode> getChildren() {
    return children;
  }

  public BasicMetric getMetricBasic() {
    return metricBasic;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  public List<MetricNode> findByName(String name) {
    List<MetricNode> list = new ArrayList<>();
    findByName(name, list);
    return list;
  }

  private void findByName(String name, List<MetricNode> list) {
    if (getMetricBasic().getName() != null && getMetricBasic().getName().equals(name)) {
      list.add(this);
    }
    for (MetricNode child : getChildren()) {
      child.findByName(name, list);
    }
  }

  public void postProcessRoot() { // only call this on root!
    for (MetricNode child : getChildren()) {
      postProcess(child);
    }
  }

  private void postProcess(MetricNode node) {
    //        System.out.println("MetricNode.postProcess " + node.getMetricType() + " " +
    // node.getMetricBasic().getName());
    // HACK BEGIN: AGGREGATE_TREE_ROOT_CATEGORY has the wrong value. should have tsame value as
    // first AGGREGATE_TREE_CATEGORY
    if (node.getMetricType() == MetricNode.MetricType.AGGREGATE_TREE_ROOT_CATEGORY) {
      for (MetricNode child : node.getChildren()) {
        if (child.getMetricType() == MetricNode.MetricType.AGGREGATE_TREE_CATEGORY
            && child instanceof ValueMetricNode
            && ((ValueMetricNode) child).hasValues()) {
          double totalValue = ((ValueMetricNode) child).getValue(0).getDValue();
          ((ValueMetricNode) node).getValue(0).setDValue(totalValue);
        }
      }
    }
    // HACK END

    // Insert child node in those AGGREGATE_TREE_CATEGORY that don't have children
    if (node.getMetricType() == MetricNode.MetricType.AGGREGATE_TREE_CATEGORY) {
      if (node.getChildren() == null || node.getChildren().isEmpty()) {
        if (node instanceof SelectableMetricNode) {
          // Add identical node as a MetricType.PLAIN child
          SelectableMetricNode selectableMetricNode = (SelectableMetricNode) node;
          BasicMetric basicMetricClone = node.getMetricBasic().clone();
          MetricOption exclusiveClone = selectableMetricNode.getExclusive().clone();
          MetricOption inclusiveClone = selectableMetricNode.getInclusive().clone();
          SelectableMetricNode child =
              new SelectableMetricNode(
                  basicMetricClone,
                  MetricNode.MetricType.PLAIN,
                  exclusiveClone,
                  inclusiveClone,
                  selectableMetricNode.getValueType());
          node.addChild(child);
          // Set the same value
          List<MetricValue> list = new ArrayList<>();
          for (MetricValue metricValue : selectableMetricNode.getValues()) {
            MetricValue metricValueClone = metricValue.clone();
            list.add(metricValueClone);
          }
          child.setValues(list);
        }
      }
    }

    // Transfer 'hot' to AnMetrics so others can know
    if (node instanceof SelectableMetricNode) {
      SelectableMetricNode smnode = (SelectableMetricNode) node;
      AnWindow.getInstance()
          .getSettings()
          .getMetricsSetting()
          .setHotMetric(smnode.getMetricBasic().getName(), smnode.isHot());
    }

    // Look for HW Counter pairs pairs. They come in pairs like "tcycles"/"cycles". Mark them as
    // PLAIN_ROOT/PLAIN_CHILD.
    AnMetric[] availableMetrics =
        AnWindow.getInstance().getSettings().getMetricsSetting().getAvailableAnMetrics();
    if (node.getMetricType() == MetricNode.MetricType.PLAIN_LIST_CATEGORY) { // FIXUP
      List<MetricNode> children = node.getChildren();
      for (int i = 0; i < children.size(); i++) {
        String name = children.get(i).getMetricBasic().getName();

        AnMetric anMetric =
            AnWindow.getInstance().getSettings().getMetricsSetting().getAvailableAnMetric(name);
        if (anMetric != null) {
          String aux = anMetric.getAux();
          if (aux != null && !name.equals(aux)) {
            for (int j = 0; j < children.size(); j++) {
              if (children.get(j).getMetricBasic().getName().equals(aux)) {
                children.get(i).setMetricType(MetricType.PLAIN_ROOT);
                children.get(j).setMetricType(MetricType.PLAIN_CHILD);
                break;
              }
            }
          }
        }

        //                if (name.startsWith("t") && i < (children.size()-1) &&
        // name.substring(1).equals(children.get(i+1).getMetricBasic().getName())) {
        //                    children.get(i).setMetricType(MetricType.PLAIN_ROOT);
        //                    children.get(i+1).setMetricType(MetricType.PLAIN_CHILD);
        //                    i++;
        //                }
      }
    }

    for (MetricNode child : node.getChildren()) {
      postProcess(child);
    }
  }

  public boolean hasSelections() {
    boolean hasSelections = false;
    if (this instanceof SelectableMetricNode) {
      SelectableMetricNode selectableMetricNode = (SelectableMetricNode) this;
      hasSelections = selectableMetricNode.hasSelections();
    }
    return hasSelections;
  }

  public boolean isSelected() {
    boolean selected = false;
    if (this instanceof SelectableMetricNode) {
      SelectableMetricNode selectableMetricNode = (SelectableMetricNode) this;
      selected = selectableMetricNode.isSelected();
    }
    return selected;
  }

  public boolean isHot() {
    boolean isHot = false;
    if (this instanceof SelectableMetricNode) {
      ValueMetricNode valueMetricNode = (ValueMetricNode) this;
      List<MetricValue> values = valueMetricNode.getValues();
      for (MetricValue value : values) {
        if (value.getHighlight()) {
          isHot = true;
          break;
        }
      }
    }
    return isHot;
  }
}
