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

package org.gprofng.mpmt.timeline2;

// This class is intended to be immutable.
//
// Entity is populated by DBE and describes how the TL rows are organized.
// It specifies one of [LWP, THREAD, CPU],
//
// Each Entity may correspond to several rows on timeline (RowDefinition).
// The number of rows on the timeline is determined by the number of
// DataDescriptors specified for the particular experiment.

public final class Entity {
  // data from ipcGetEntities()
  private final int entity_prop_id; // property used to segregate data
  private final String entity_prop_id_name;
  private final int entity_prop_value;

  // if "force_java_threads == true", set these:
  private final String name; // jthread entname
  private final String g_name; // jthread group name
  private final String p_name; // jthread parent name

  public Entity(
      final int entity_prop_id,
      final String entity_prop_id_name,
      final int entity_prop_value,
      final String name,
      final String g_name,
      final String p_name) {
    this.entity_prop_id = entity_prop_id;
    this.entity_prop_id_name = entity_prop_id_name;
    this.entity_prop_value = entity_prop_value;
    this.name = name;
    this.g_name = g_name;
    this.p_name = p_name;
  }

  public int getPropValue() {
    return entity_prop_value;
  }

  public int getPropId() {
    return entity_prop_id;
  }

  public String getPropIdName() {
    return entity_prop_id_name;
  }

  public String getThreadName() {
    return name;
  }

  public String getJThreadGroupName() {
    return g_name;
  }

  public String getJThreadParentName() {
    return p_name;
  }
}
