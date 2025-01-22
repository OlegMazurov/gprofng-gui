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

import org.gprofng.mpmt.AnWindow;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SelectableMetricNode extends ValueMetricNode {
  private final MetricOption exclusive; // presentation's exclusive checkboxes
  private final MetricOption inclusive; // presentation's inclusive checkboxes
  private final int valueType; // YXXX placeholder.  For now, AnVariable.VT_INT, etc.
  private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

  public SelectableMetricNode(
      BasicMetric metricBasic,
      MetricType metricsGroupType,
      MetricOption exclusive,
      MetricOption inclusive,
      int valueType) {
    super(metricBasic, metricsGroupType);
    this.exclusive = exclusive;
    this.inclusive = inclusive;
    this.valueType = valueType;
  }

  /**
   * @return the exclusive
   */
  public MetricOption getExclusive() {
    return exclusive;
  }

  /**
   * @return the inclusive
   */
  public MetricOption getInclusive() {
    return inclusive;
  }

  /**
   * @return the hasSelections
   */
  @Override
  public boolean hasSelections() {
    boolean exclIsSelected = exclusive != null ? exclusive.hasSelections() : false;
    boolean inclIsSelected = inclusive != null ? inclusive.hasSelections() : false;
    return exclIsSelected || inclIsSelected;
  }

  /**
   * @return true if selected
   */
  @Override
  public boolean isSelected() {
    boolean selected =
        AnWindow.getInstance()
            .getSettings()
            .getMetricsSetting()
            .isSelected(getMetricBasic().getName());
    return selected;
  }

  public int getValueType() {
    return valueType;
  }

  private boolean[] internalSetSelected(boolean detailsCheckBox, boolean forceDefaults) {
    //        System.out.println("setSelected:" + selected + " " + apply + " " + detailsCheckBox);
    //        dumpAttr(this);
    //        AnWindow anWindow = AnWindow.getInstance();

    boolean eT = exclusive.getCapable().isTime() && exclusive.getSelected().isTime();
    boolean eV = exclusive.getCapable().isValue() && exclusive.getSelected().isValue();
    boolean eP = exclusive.getCapable().isPercent() && exclusive.getSelected().isPercent();
    boolean iT = inclusive.getCapable().isTime() && inclusive.getSelected().isTime();
    boolean iV = inclusive.getCapable().isValue() && inclusive.getSelected().isValue();
    boolean iP = inclusive.getCapable().isPercent() && inclusive.getSelected().isPercent();

    if (!detailsCheckBox) {
      if (forceDefaults || !(eT || eV || eP || iT || iV || iP)) {
        // nothing selected, fall back to saved defaults
        eT = exclusive.getCapable().isTime() && exclusive.getDefaultAttrs().isTime();
        eV = exclusive.getCapable().isValue() && exclusive.getDefaultAttrs().isValue();
        eP = exclusive.getCapable().isPercent() && exclusive.getDefaultAttrs().isPercent();
        iT = inclusive.getCapable().isTime() && inclusive.getDefaultAttrs().isTime();
        iV = inclusive.getCapable().isValue() && inclusive.getDefaultAttrs().isValue();
        iP = inclusive.getCapable().isPercent() && inclusive.getDefaultAttrs().isPercent();
      }
      if (!(eT || eV || eP || iT || iV || iP)) {
        // still nothing, select something using heuristics
        eT = exclusive.getCapable().isTime();
        if (!eT) {
          eV = exclusive.getCapable().isValue();
        }
        if (!eT && !eV) {
          eP = exclusive.getCapable().isPercent();
        }
        //            iT = inclusive.getCapable().isTime();
        //            if (!iT) {
        //                iV = inclusive.getCapable().isValue();
        //            }
      }
    }
    // Save the changes
    exclusive.getSelected().setAttrs(false, eT, eV, eP);
    inclusive.getSelected().setAttrs(false, iT, iV, iP);

    boolean[] exclusiveIncluseValues = new boolean[] {eT, eV, eP, iT, iV, iP};
    return exclusiveIncluseValues;
  }

  public boolean[] setSelected(boolean selected, boolean detailsCheckBox) {
    return internalSetSelected(detailsCheckBox, false);
  }

  public boolean[] setDefaults() {
    return internalSetSelected(false, true);
  }

  public void addChangeListener(ChangeListener changeListener) {
    changeListeners.add(changeListener);
  }

  public void removeChangeListener(ChangeListener changeListener) {
    changeListeners.remove(changeListener);
  }

  public void fireChange() {
    for (ChangeListener changeListener : changeListeners) {
      ChangeEvent changeEvent = new ChangeEvent(this);
      changeListener.stateChanged(changeEvent);
    }
  }

  private static void dumpAttr(SelectableMetricNode selectableMetricNode) {
    System.err.println(
        selectableMetricNode.getMetricBasic().getDisplayName()
            + " - "
            + selectableMetricNode.getMetricBasic().getDisplayName());
    System.err.println("     Exclusive        Inclusive");
    System.err.println("     Time Value  %    Time Value  %   ");
    dumpAttr(
        "Sel",
        selectableMetricNode.getExclusive().getSelected(),
        selectableMetricNode.getInclusive().getSelected());
    dumpAttr(
        "Cap",
        selectableMetricNode.getExclusive().getCapable(),
        selectableMetricNode.getInclusive().getCapable());
  }

  private static void dumpAttr(String what, MetricAttr exEtricAttr, MetricAttr inEtricAttr) {
    System.err.print(what);
    System.err.print("    " + dumpAttr(exEtricAttr.isTime()));
    System.err.print("    " + dumpAttr(exEtricAttr.isValue()));
    System.err.print("    " + dumpAttr(exEtricAttr.isPercent()));
    System.err.print("  ");
    System.err.print("    " + dumpAttr(inEtricAttr.isTime()));
    System.err.print("    " + dumpAttr(inEtricAttr.isValue()));
    System.err.print("    " + dumpAttr(inEtricAttr.isPercent()));
    System.err.println();
  }

  private static String dumpAttr(boolean b) {
    return b ? "x" : " ";
  }
}
