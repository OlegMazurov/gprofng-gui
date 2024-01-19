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


package org.gprofng.mpmt.timeline2;

import org.gprofng.mpmt.AnLocale;
import org.gprofng.mpmt.experiment_props.*;
import org.gprofng.mpmt.settings.Settings;
import java.util.List;

// This class is intended to be immutable so that it can be accessed from
// the AWT thread without worry that inner details may change.
// (Changeable data should go into RowData.java w/ appropriate synchronization)
//
// Each Entity may correspond to several rows on timeline (RowDefinitions).
// The number of rows on the timeline is determined by the number of
// DataDescriptors specified for the particular experiment.

public final class RowDefinition {
  // experiment
  private final int exp_id;
  private final int user_exp_id;

  // data_id within experiment
  private final DataDescriptor data_descriptor; // immutable
  private final int aux; // HWC value

  // entity within data_id
  private final Entity entity; // immutable

  // specification for additional chart properties
  private final List<PropDescriptor> chartProps;
  private final String chartPropNames[];

  // descriptive strings
  private final String exp_name;
  private final int exp_group_id;
  private final boolean compare_on;
  private final long data_start_hrt; // hr time at data start
  private final long data_end_hrt; // hr time at data end
  private final long time_origin_hrt; // hr time at "0" point of timeline
  private final String longDescription;
  private final String shortLabel;
  private final int cpuFreq;

  public RowDefinition(
      final ExperimentProperties experiment,
      Entity entity,
      final DataDescriptor d_descriptor,
      final int aux,
      final List<PropDescriptor> chartProps,
      final long time_adjust,
      final boolean compare_on) {
    this.compare_on = compare_on;
    this.exp_id = experiment.getID();
    this.user_exp_id = experiment.getUserID();
    this.data_descriptor = d_descriptor;
    this.aux = aux;
    if (entity == null) {
      entity =
          new Entity(
              Settings.PROP_NONE, // ent_prop_id (experiment-wide)
              "", // don't care
              0, // ent_prop_value (0 is value for any undefined prop)
              d_descriptor.getLongUName(aux),
              null,
              null);
    }
    this.entity = entity;

    this.exp_name = experiment.getName();
    this.exp_group_id = experiment.getGroupId();
    this.data_start_hrt = experiment.getStartTime();
    this.data_end_hrt = experiment.getEndTime();
    this.time_origin_hrt = time_adjust;

    this.chartProps = chartProps;
    if (chartProps == null) {
      this.chartPropNames = null;
    } else {
      this.chartPropNames = new String[chartProps.size()];
      int ii = 0;
      for (PropDescriptor prop : chartProps) {
        String propName = prop.getName();
        this.chartPropNames[ii++] = propName;
      }
    }

    // other fields must be set before creating labels:
    this.longDescription = createLongDescription();
    this.shortLabel = createShortLable();
    this.cpuFreq = experiment.getCpuFreq();
  }

  public DataDescriptor getDataDescriptor() {
    return data_descriptor;
  }

  public Entity getEntity() {
    return entity;
  }

  public String getExpName() {
    return exp_name;
  }

  public int getExpID() {
    return exp_id;
  }

  public int getUserExpID() {
    return user_exp_id;
  }

  public int getExpGroupId() {
    return exp_group_id;
  }

  public int getAux() {
    return aux;
  }

  public Settings.TLData_type getTLDataType() {
    return data_descriptor.getTLDataType();
  }

  public String getDataDescUName() {
    return data_descriptor.getLongUName(aux);
  }

  public long getTimeOrigin() { // hr time at timeline origin (nanoseconds)
    return time_origin_hrt;
  }

  public long getDataTimeStart() {
    return data_start_hrt;
  }

  public long getDataTimeEnd() {
    return data_end_hrt;
  }

  public String getLongDescription() {
    return longDescription;
  }

  public boolean getComparisonOn() {
    return compare_on;
  }

  public int getChartCount() {
    if (chartProps == null) {
      return 0;
    } else {
      return chartProps.size();
    }
  }

  public List<PropDescriptor> getChartProperties() {
    return chartProps;
  }

  public String[] getChartPropNames() {
    return chartPropNames;
  }

  public boolean matches(int thisChartPropIdx, RowDefinition thatRowDef, int thatChartPropIdx) {
    // check the basics
    if (user_exp_id != thatRowDef.user_exp_id) {
      return false;
    } else if (aux != thatRowDef.aux) {
      return false;
    } else if (entity.getPropId() != thatRowDef.entity.getPropId()) {
      return false;
    } else if (entity.getPropValue() != thatRowDef.entity.getPropValue()) {
      return false;
    }

    // check for data vs chart
    boolean thisIsData = (thisChartPropIdx == -1);
    boolean thatIsData = (thatChartPropIdx == -1);
    if (thisIsData != thatIsData) {
      return false; // only one is data
    } else if (thisIsData && thatIsData) {
      return true; // both are data
    }

    // were dealing with two charts
    if (chartProps == null || thisChartPropIdx >= chartProps.size()) {
      return false; // weird
    } else if (thatRowDef.getChartProperties() == null
        || thatChartPropIdx >= thatRowDef.getChartProperties().size()) {
      return false; // weird
    }
    PropDescriptor thisPropDescriptor = chartProps.get(thisChartPropIdx);
    PropDescriptor thatPropDescriptor = thatRowDef.getChartProperties().get(thatChartPropIdx);
    if (thisPropDescriptor.getPropId() != thatPropDescriptor.getPropId()) {
      return false;
    }

    return true;
  }

  private String createLongDescription() {
    RowDefinition rowDefinition = this;
    String desc = "<HTML>";

    String moreInfo = "";
    Settings.TLData_type tltype = data_descriptor.getTLDataType();
    if (!tltype.equals(Settings.TLData_type.TL_SAMPLE)
        && !tltype.equals(Settings.TLData_type.TL_HEAPSZ)
        && // YXXX where to decide when to show stacks?
        !tltype.equals(Settings.TLData_type.TL_GCEVENT)) { // CXXX Bug 20801848
      moreInfo = AnLocale.getString(" Call Stacks");
    }

    String experimentInfo =
        AnLocale.getString("<HR>Process: ")
            + "<B>"
            + user_exp_id
            + "</B>"
            + " ("
            + "<B>"
            + exp_name
            + "</B>"
            + ")";

    if (Settings.PROP_NONE == rowDefinition.getEntity().getPropId())
      desc += rowDefinition.getDataDescUName() + moreInfo + "<BR>" + experimentInfo;
    else {
      final String num, thr_name, thr_g_name, thr_p_name;
      final long entityNum;
      entityNum = rowDefinition.getEntity().getPropValue();
      thr_name = rowDefinition.getEntity().getThreadName();
      thr_g_name = rowDefinition.getEntity().getJThreadGroupName();
      thr_p_name = rowDefinition.getEntity().getJThreadParentName();

      //	    int tl_data_mode = rowDefinition.getEntity().getPropId();

      String str;
      if (thr_g_name != null) {
        str =
            "<BR>"
                + AnLocale.getString("Thread name: ")
                + "<B>"
                + thr_name
                + "</B>"
                + "<BR>"
                + AnLocale.getString("Thread group: ")
                + "<B>"
                + thr_g_name
                + "</B>"
                + "<BR>"
                + AnLocale.getString("Thread parent: ")
                + "<B>"
                + thr_p_name
                + "</B>";
      } else if (thr_name != null) {
        str =
            "<BR>"
                + AnLocale.getString("Thread name: ") // YXXX TBD
                + "<B>"
                + thr_name
                + "</B>";
      } else {
        str = "";
      }

      num = Long.toString(entityNum);

      String entMode = entity.getPropIdName();

      desc +=
          rowDefinition.getDataDescUName()
              + moreInfo
              + experimentInfo
              + "<BR>"
              + entMode
              + ": "
              + "<B>"
              + num
              + "</B>"
              + str;
    }
    if (this.compare_on) {
      final String group_name;
      if (exp_group_id <= 1) {
        group_name = "Baseline";
      } else {
        group_name = "Comparison Group " + (exp_group_id - 1);
      }
      desc += "<BR>" + AnLocale.getString("Compare Group: ") + "<B>" + group_name + "</B>";
    }
    return desc;
  }

  public String getShortLabel() {
    return shortLabel;
  }

  private String createShortLable() {
    RowDefinition rowDefinition = this;
    String userExpId = Integer.toString(rowDefinition.getUserExpID());
    String rowLabel = new String();
    long entityPropId = rowDefinition.getEntity().getPropId();
    String entityPropName = rowDefinition.getEntity().getPropIdName();

    rowLabel += userExpId;
    if (entityPropId != Settings.PROP_NONE) {
      // everything besides sample rows
      rowLabel +=
          " "
              + entityPropName.toUpperCase().charAt(0)
              + ":"
              + Long.toString(rowDefinition.getEntity().getPropValue());
    } else {
      if (compare_on) {
        if (exp_group_id <= 1) {
          rowLabel += " (Base)";
        } else {
          //                    rowLabel += " (G" + (exp_group_id-1) + ")";
        }
      }
    }
    return rowLabel;
  }

  public int getCpuFreq() {
    return cpuFreq;
  }
}
