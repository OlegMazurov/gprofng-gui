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


package org.gprofng.mpmt.experiment_props;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class is intended to be immutable
//
// Mirrors back-end Experiment DataDescriptor and PropDescr information
public class ExperimentProperties {
  public static class BasicInfo {
    private final int exp_id; // dbe Experiment handle
    private final int
        founder_exp_id; // dbe handle of founder experiment (same as exp_id if standalone)
    private final int user_exp_id; // filter experiment id
    private final int exp_group_id; // dbe experiment group id
    private final String exp_name;
    private final long offset_time, start_time, end_time;
    private final long start_wall_secs; // wall time-of-day seconds at start_time
    private final String hostname;
    private final int cpu_freq;

    public BasicInfo(
        final int exp_id,
        final int founder_exp_id,
        final int user_exp_id,
        final String exp_name,
        final int group_id,
        final long offset_time,
        final long start_time,
        final long end_time,
        final long start_wall_secs,
        final String hostname,
        final int cpu_freq) {
      this.exp_id = exp_id;
      this.founder_exp_id = founder_exp_id;
      this.user_exp_id = user_exp_id;
      this.exp_name = exp_name;
      this.exp_group_id = group_id;
      this.offset_time = offset_time;
      this.start_time = start_time;
      this.end_time = end_time;
      this.start_wall_secs = start_wall_secs;
      this.hostname = hostname;
      this.cpu_freq = cpu_freq;
    }
  }

  private final BasicInfo basic_info;
  private final List<DataDescriptor> data_desc;
  private final List<DataDescriptor> time_data_desc;
  private final Map<String, DataDescriptor> desc_map;

  public ExperimentProperties(final BasicInfo basic_info, List<DataDescriptor> data_desc) {
    this.basic_info = basic_info;
    this.data_desc = data_desc;
    this.time_data_desc = timeDataDescriptions(data_desc);
    this.desc_map = new HashMap();
  }

  public final int getID() {
    return basic_info.exp_id;
  }

  public final int getFounderID() {
    return basic_info.founder_exp_id;
  }

  public final int getUserID() {
    return basic_info.user_exp_id;
  }

  public final long getTimeOffset() {
    return basic_info.offset_time;
  }

  public final long getStartTime() {
    return basic_info.start_time;
  }

  public final long getEndTime() {
    return basic_info.end_time;
  }

  public final long getStartWallSeconds() {
    return basic_info.start_wall_secs;
  }

  public final String getHostname() {
    return basic_info.hostname;
  }

  public final int getCpuFreq() {
    return basic_info.cpu_freq;
  }

  public List<DataDescriptor> getDataDescriptors() {
    return data_desc;
  }

  public List<DataDescriptor> getTstampDataDescriptors() {
    return time_data_desc;
  }

  public DataDescriptor findDataDescriptor(final String name) {
    if (desc_map.isEmpty()) {
      if (data_desc == null) {
        return null;
      }
      for (DataDescriptor desc : data_desc) {
        desc_map.put(desc.getName(), desc);
      }
    }
    return desc_map.get(name);
  }

  public String getName() {
    return basic_info.exp_name;
  }

  public int getGroupId() {
    return basic_info.exp_group_id;
  }

  private List<DataDescriptor> timeDataDescriptions(List<DataDescriptor> dataDescriptors) {
    ArrayList<DataDescriptor> tlDataDescriptors = new ArrayList();
    if (dataDescriptors == null) { // experiment dropped?
      return tlDataDescriptors;
    }
    for (DataDescriptor ddscr : dataDescriptors) {
      if (ddscr.findProp("TSTAMP") != null) {
        tlDataDescriptors.add(ddscr);
      }
    }
    return tlDataDescriptors;
  }
}
