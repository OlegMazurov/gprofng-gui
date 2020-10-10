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

// This class is intended to be immutable.
//
// PropDescriptor is a container to hold the DBE description of a metric
// to be d[isplayed.  (e.g. CPU Cycles)
public final class PropDescriptor {
  private final String name; // identifier name
  private final String uname; // printable name
  private final int prop_id; // Dbe PROP_* value
  private final String stateNames[];
  private final String stateUNames[];

  public PropDescriptor(
      final String name,
      final String uname,
      int prop_id,
      final String stateNames[],
      final String stateUNames[]) {
    // used for HWC data, where aux is needed to specify counter
    this.name = name;
    this.uname = uname;
    this.prop_id = prop_id;
    this.stateNames = stateNames;
    this.stateUNames = stateUNames;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return uname;
  }

  public int getPropId() {
    return prop_id;
  }

  public String[] getStateNames() { // may return null
    return stateNames;
  }

  public String[] getStateUNames() { // may return null
    return stateUNames;
  }
}
