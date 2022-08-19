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

import org.gprofng.mpmt.settings.Settings;
import java.util.ArrayList;

// This class is intended to be immutable.
//
// DataDescriptor is a container to hold the DBE description of a metric
// to be displayed.  (e.g. CPU Cycles)
public final class DataDescriptor {
  private final String name;
  private final String uname;
  private final Settings.TLData_type tldata_type; // enum/bitmask TBR?
  private final int data_id; // Dbe DATA_* value
  private final ArrayList<PropDescriptor> props;
  private final int aux_prop_id; // e.g. PROP_HWCTAG
  private final int aux_count; // number of values for aux_prop_id
  private final boolean has_callstacks;

  public DataDescriptor(
      final String name,
      final String uname,
      final int data_id,
      final ArrayList<PropDescriptor> props,
      final int aux_prop_id) {
    this.name = name;
    this.uname = uname;
    this.tldata_type = Settings.TLData_type.find(name);
    this.data_id = data_id;
    this.props = props;
    this.aux_prop_id = aux_prop_id;
    PropDescriptor prop = findProp(aux_prop_id);
    if (prop != null && prop.getStateNames() != null) {
      aux_count = prop.getStateNames().length;
    } else {
      aux_count = 0;
    }
    if (findProp("FRINFO") != null) {
      has_callstacks = true;
    } else {
      has_callstacks = false;
    }
  }

  public String getName() {
    return name;
  }

  public String getAuxName(int aux) { // gp-display-text metrics command for HWCs
    if (aux_count == 0) {
      return null;
    }
    PropDescriptor prop = findProp(aux_prop_id);
    if (aux < 0 || aux >= aux_count || prop == null) {
      return null; // unexpected
    }
    String auxName = prop.getStateNames()[aux];
    return auxName;
  }

  public String getAuxUName(int aux) { // gp-display-text metrics command for HWCs' description
    if (aux_count == 0) {
      return null;
    }
    PropDescriptor prop = findProp(aux_prop_id);
    if (aux < 0 || aux >= aux_count || prop == null) {
      return null; // unexpected
    }
    String auxName = prop.getStateUNames()[aux];
    return auxName;
  }

  public String getErRcTLDataCmdName(int aux) { // tldata .gprofng.rc command
    String cmd = Settings.getErRcTLDataCmdName(tldata_type, getAuxName(aux));
    return cmd;
  }

  public String getLongUName(int aux) { // long user-visible name
    String longUName = Settings.getErRcTLDataLongUName(uname, getAuxUName(aux));
    return longUName;
  }

  public Settings.TLData_type getTLDataType() { // TBR?
    return tldata_type;
  }

  public int getDataId() {
    return data_id;
  }

  public final ArrayList<PropDescriptor> getProps() {
    return props;
  }

  public final PropDescriptor findProp(int prop_id) {
    if (props == null) {
      return null;
    }
    for (PropDescriptor prop : props) {
      if (prop_id == prop.getPropId()) {
        return prop;
      }
    }
    return null;
  }

  public final PropDescriptor findProp(String propName) {
    if (props == null) {
      return null;
    }
    for (PropDescriptor prop : props) {
      if (propName.equals(prop.getName())) {
        return prop;
      }
    }
    return null;
  }

  public int getAuxPropId() {
    return aux_prop_id;
  }

  public int getAuxCount() {
    return aux_count;
  }

  public boolean hasCallstacks() {
    return has_callstacks;
  }
}
